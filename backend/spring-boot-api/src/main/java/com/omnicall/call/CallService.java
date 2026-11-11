package com.omnicall.call;

import com.omnicall.common.ApiException;
import com.omnicall.config.OmniCallProperties;
import com.omnicall.livekit.LiveKitGateway;
import com.omnicall.notification.AppNotification;
import com.omnicall.notification.DeviceTokenRepository;
import com.omnicall.notification.NotificationRepository;
import com.omnicall.notification.PushSender;
import com.omnicall.signaling.CallSignalingHandler;
import com.omnicall.user.UserAccount;
import com.omnicall.user.UserAccountRepository;
import com.omnicall.user.UserPreferences;
import com.omnicall.user.UserPreferencesRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CallService {

    private static final List<CallStatus> OPEN = List.of(CallStatus.RINGING, CallStatus.ACTIVE);

    private final CallSessionRepository calls;
    private final CallParticipantRepository participants;
    private final TranslationEventRepository translations;
    private final UserAccountRepository users;
    private final UserPreferencesRepository preferences;
    private final LiveKitGateway liveKit;
    private final UsageLedger usage;
    private final CallSignalingHandler signaling;
    private final NotificationRepository notifications;
    private final DeviceTokenRepository devices;
    private final PushSender push;
    private final OmniCallProperties properties;

    public CallService(
            CallSessionRepository calls,
            CallParticipantRepository participants,
            TranslationEventRepository translations,
            UserAccountRepository users,
            UserPreferencesRepository preferences,
            LiveKitGateway liveKit,
            UsageLedger usage,
            CallSignalingHandler signaling,
            NotificationRepository notifications,
            DeviceTokenRepository devices,
            PushSender push,
            OmniCallProperties properties) {
        this.calls = calls;
        this.participants = participants;
        this.translations = translations;
        this.users = users;
        this.preferences = preferences;
        this.liveKit = liveKit;
        this.usage = usage;
        this.signaling = signaling;
        this.notifications = notifications;
        this.devices = devices;
        this.push = push;
        this.properties = properties;
    }

    public CallView start(UUID callerId, UUID receiverId) {
        if (callerId.equals(receiverId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CALL_SELF", "Choose another person to call.");
        }
        UserAccount caller = requireActive(callerId);
        UserAccount receiver = requireActive(receiverId);
        requireConsent(caller);
        requireConsent(receiver);
        if (!calls.findOpenForUser(callerId, OPEN).isEmpty() || !calls.findOpenForUser(receiverId, OPEN).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "CALL_BUSY", "One of you is already in a call.");
        }
        usage.assertCanStart(callerId);
        UserPreferences callerPrefs = preferences.findByUserId(callerId).orElseGet(() -> preferences.save(UserPreferences.defaults(callerId)));
        UserPreferences receiverPrefs = preferences.findByUserId(receiverId).orElseGet(() -> preferences.save(UserPreferences.defaults(receiverId)));
        CallSession call = calls.save(CallSession.ringing(callerId, receiverId, caller.getPreferredLanguage(), receiver.getPreferredLanguage()));
        participants.save(CallParticipant.create(call.getId(), callerId, ParticipantRole.CALLER));
        participants.save(CallParticipant.create(call.getId(), receiverId, ParticipantRole.RECEIVER));
        String metadata = liveKit.roomMetadata(call.getId(), caller, callerPrefs, receiver, receiverPrefs);
        try {
            liveKit.createRoom(call.getRoomName(), metadata);
            String dispatchId = liveKit.dispatchTranslator(call.getRoomName(), metadata);
            call.setDispatchId(dispatchId);
            calls.save(call);
        } catch (RuntimeException exception) {
            call.setStatus(CallStatus.FAILED);
            call.setEndReason("LIVEKIT");
            call.setEndedAt(Instant.now());
            calls.save(call);
            liveKit.closeRoom(call.getRoomName());
            throw exception;
        }
        notify(receiverId, "INCOMING_CALL", Map.of(
                "type", "INCOMING_CALL",
                "callId", call.getId().toString(),
                "callerId", callerId.toString(),
                "callerName", caller.getDisplayName(),
                "callerLanguage", caller.getPreferredLanguage(),
                "receiverLanguage", receiver.getPreferredLanguage()));
        pushIncoming(receiverId, caller.getDisplayName(), caller.getPreferredLanguage(), call.getId());
        return view(call);
    }

    @Transactional
    public CallView accept(UUID userId, UUID callId) {
        CallSession call = require(callId);
        if (!call.getReceiverId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CALL_FORBIDDEN", "Only the person being called can accept.");
        }
        if (call.getStatus() != CallStatus.RINGING) {
            throw new ApiException(HttpStatus.CONFLICT, "CALL_NOT_RINGING", "This call is no longer ringing.");
        }
        call.setStatus(CallStatus.ACTIVE);
        call.setStartedAt(Instant.now());
        calls.save(call);
        markJoined(callId, userId);
        notify(call.getCallerId(), "CALL_ACCEPTED", Map.of(
                "type", "CALL_ACCEPTED",
                "callId", call.getId().toString()));
        return view(call);
    }

    @Transactional
    public CallView reject(UUID userId, UUID callId) {
        CallSession call = require(callId);
        if (!call.getReceiverId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CALL_FORBIDDEN", "Only the person being called can decline.");
        }
        if (call.getStatus() != CallStatus.RINGING) {
            throw new ApiException(HttpStatus.CONFLICT, "CALL_NOT_RINGING", "This call is no longer ringing.");
        }
        finish(call, CallStatus.REJECTED, "REJECTED");
        notify(call.getCallerId(), "CALL_REJECTED", Map.of("type", "CALL_REJECTED", "callId", call.getId().toString()));
        return view(call);
    }

    @Transactional
    public CallView end(UUID userId, UUID callId) {
        CallSession call = require(callId);
        if (!call.involves(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CALL_FORBIDDEN", "You are not part of this call.");
        }
        if (call.getStatus() == CallStatus.ENDED || call.getStatus() == CallStatus.REJECTED || call.getStatus() == CallStatus.MISSED) {
            return view(call);
        }
        finish(call, CallStatus.ENDED, "HANGUP");
        notify(call.otherParty(userId), "CALL_ENDED", Map.of(
                "type", "CALL_ENDED",
                "callId", call.getId().toString(),
                "durationSeconds", call.getDurationSeconds() == null ? 0 : call.getDurationSeconds()));
        return view(call);
    }

    public CallToken token(UUID userId, UUID callId) {
        CallSession call = require(callId);
        if (!call.involves(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CALL_FORBIDDEN", "You are not part of this call.");
        }
        if (call.getStatus() != CallStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "CALL_NOT_ACTIVE", "The call is not active.");
        }
        UserAccount user = requireActive(userId);
        UserPreferences prefs = preferences.findByUserId(userId).orElseGet(() -> UserPreferences.defaults(userId));
        markJoined(callId, userId);
        return new CallToken(
                properties.getLivekit().getUrl(),
                liveKit.participantToken(user, prefs, call.getRoomName()),
                call.getRoomName(),
                call.getId().toString());
    }

    public CallView get(UUID userId, UUID callId) {
        CallSession call = require(callId);
        if (!call.involves(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "CALL_FORBIDDEN", "You are not part of this call.");
        }
        return view(call);
    }

    public List<CallView> history(UUID userId) {
        return calls.findByCallerIdOrReceiverIdOrderByCreatedAtDesc(userId, userId).stream().map(this::view).toList();
    }

    @Transactional
    public void expireRinging() {
        Instant cutoff = Instant.now().minus(properties.getPrivacy().getRingTimeout());
        for (CallSession call : calls.findByStatusAndCreatedAtBefore(CallStatus.RINGING, cutoff)) {
            finish(call, CallStatus.MISSED, "NO_ANSWER");
            notify(call.getCallerId(), "CALL_ENDED", Map.of("type", "CALL_ENDED", "callId", call.getId().toString(), "reason", "NO_ANSWER"));
        }
    }

    @Transactional
    public void recordTranslation(UUID callId, TranslationReport report) {
        CallSession call = require(callId);
        if (call.getStatus() != CallStatus.ACTIVE && call.getStatus() != CallStatus.ENDED) {
            return;
        }
        UserPreferences speakerPrefs = preferences.findByUserId(report.speakerId()).orElse(null);
        if (speakerPrefs != null && speakerPrefs.isTranslationHistoryEnabled() && !report.partial()) {
            translations.save(TranslationEvent.create(
                    callId,
                    report.speakerId(),
                    report.sourceLanguage(),
                    report.targetLanguage(),
                    report.originalText(),
                    report.translatedText(),
                    report.status()));
        }
        Map<String, Object> event = Map.of(
                "type", "SUBTITLE",
                "callId", callId.toString(),
                "speakerId", report.speakerId().toString(),
                "sourceLanguage", report.sourceLanguage(),
                "targetLanguage", report.targetLanguage(),
                "originalText", report.originalText() == null ? "" : report.originalText(),
                "translatedText", report.translatedText() == null ? "" : report.translatedText(),
                "status", report.status(),
                "partial", report.partial(),
                "utteranceId", report.utteranceId() == null ? "" : report.utteranceId());
        signaling.send(call.getCallerId(), event);
        signaling.send(call.getReceiverId(), event);
    }

    private void finish(CallSession call, CallStatus status, String reason) {
        Instant ended = Instant.now();
        if (call.getStartedAt() != null) {
            long seconds = Math.max(0, ended.getEpochSecond() - call.getStartedAt().getEpochSecond());
            call.setDurationSeconds((int) seconds);
            usage.add(call.getCallerId(), seconds);
            usage.add(call.getReceiverId(), seconds);
        }
        call.setStatus(status);
        call.setEndReason(reason);
        call.setEndedAt(ended);
        calls.save(call);
        for (CallParticipant participant : participants.findByCallId(call.getId())) {
            participant.setConnectionStatus("LEFT");
            participant.setLeftAt(ended);
            participants.save(participant);
        }
        liveKit.closeRoom(call.getRoomName());
    }

    private void markJoined(UUID callId, UUID userId) {
        for (CallParticipant participant : participants.findByCallId(callId)) {
            if (participant.getUserId().equals(userId) && participant.getJoinedAt() == null) {
                participant.setJoinedAt(Instant.now());
                participant.setConnectionStatus("JOINED");
                participants.save(participant);
            }
        }
    }

    private void notify(UUID userId, String type, Map<String, Object> payload) {
        signaling.send(userId, payload);
        notifications.save(AppNotification.pending(userId, type, payload.toString()));
    }

    private void pushIncoming(UUID userId, String callerName, String language, UUID callId) {
        for (var device : devices.findByUserId(userId)) {
            push.sendIncomingCall(
                    device.getToken(),
                    "Incoming OmniCall",
                    callerName + " is calling in " + language,
                    Map.of("type", "INCOMING_CALL", "callId", callId.toString()));
        }
    }

    private CallSession require(UUID callId) {
        return calls.findById(callId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CALL_NOT_FOUND", "That call does not exist."));
    }

    private UserAccount requireActive(UUID userId) {
        return users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "That person is not on OmniCall."));
    }

    private void requireConsent(UserAccount user) {
        if (!user.isTranslationConsent()) {
            throw new ApiException(HttpStatus.CONFLICT, "CONSENT_REQUIRED", user.getDisplayName() + " has not agreed to AI translation.");
        }
    }

    private CallView view(CallSession call) {
        UserAccount caller = users.findById(call.getCallerId()).orElseThrow();
        UserAccount receiver = users.findById(call.getReceiverId()).orElseThrow();
        return new CallView(
                call.getId(),
                call.getCallerId(),
                call.getReceiverId(),
                caller.getDisplayName(),
                receiver.getDisplayName(),
                caller.getProfileImageUrl(),
                receiver.getProfileImageUrl(),
                call.getCallerLanguage(),
                call.getReceiverLanguage(),
                call.getStatus().name(),
                call.getStartedAt(),
                call.getEndedAt(),
                call.getDurationSeconds(),
                call.getRoomName());
    }

    public record CallView(
            UUID id,
            UUID callerId,
            UUID receiverId,
            String callerName,
            String receiverName,
            String callerImageUrl,
            String receiverImageUrl,
            String callerLanguage,
            String receiverLanguage,
            String status,
            Instant startedAt,
            Instant endedAt,
            Integer durationSeconds,
            String roomName) {
    }

    public record CallToken(String url, String token, String roomName, String callId) {
    }

    public record StartCallRequest(@jakarta.validation.constraints.NotNull UUID receiverId) {
    }

    public record TranslationReport(
            UUID speakerId,
            String sourceLanguage,
            String targetLanguage,
            String originalText,
            String translatedText,
            String status,
            boolean partial,
            String utteranceId) {
    }
}
