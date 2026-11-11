package com.omnicall.livekit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicall.common.ApiException;
import com.omnicall.config.OmniCallProperties;
import com.omnicall.user.UserAccount;
import com.omnicall.user.UserPreferences;
import io.livekit.server.AccessToken;
import io.livekit.server.AgentDispatchServiceClient;
import io.livekit.server.CanPublish;
import io.livekit.server.CanPublishData;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.okhttp.OkHttpFactory;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LiveKitGateway {

    private final OmniCallProperties properties;
    private final ObjectMapper objectMapper;
    private final RoomServiceClient rooms;
    private final AgentDispatchServiceClient dispatch;

    public LiveKitGateway(OmniCallProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        String host = properties.getLivekit().getHost();
        String key = properties.getLivekit().getApiKey();
        String secret = properties.getLivekit().getApiSecret();
        OkHttpFactory http = new OkHttpFactory();
        this.rooms = RoomServiceClient.createClient(host, key, secret, http, false);
        this.dispatch = AgentDispatchServiceClient.createClient(host, key, secret, http, false);
    }

    public void createRoom(String roomName, String metadata) {
        try {
            var response = rooms.createRoom(roomName, 300, 8, null, metadata, null, null, null, 30).execute();
            if (!response.isSuccessful()) {
                throw unavailable("LiveKit rejected room creation (" + response.code() + ").");
            }
        } catch (IOException exception) {
            throw unavailable("The calling service could not be reached.");
        }
    }

    public String dispatchTranslator(String roomName, String metadata) {
        try {
            var response = dispatch.createDispatch(roomName, properties.getLivekit().getAgentName(), metadata).execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw unavailable("LiveKit could not start the translator.");
            }
            return response.body().getId();
        } catch (IOException exception) {
            throw unavailable("The translator could not be reached.");
        }
    }

    public void closeRoom(String roomName) {
        try {
            rooms.deleteRoom(roomName).execute();
        } catch (IOException exception) {
            // The call record is still closed. A leftover empty room expires on its own.
        }
    }

    public String participantToken(UserAccount user, UserPreferences preferences, String roomName) {
        AccessToken token = new AccessToken(properties.getLivekit().getApiKey(), properties.getLivekit().getApiSecret());
        token.setIdentity(user.getId().toString());
        token.setName(user.getDisplayName());
        token.setMetadata(participantMetadata(user, preferences));
        token.setTtl(properties.getLivekit().getTokenTtl().toMillis());
        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName),
                new CanPublish(true),
                new CanSubscribe(true),
                new CanPublishData(true));
        return token.toJwt();
    }

    public String roomMetadata(UUID callId, UserAccount caller, UserPreferences callerPrefs, UserAccount receiver, UserPreferences receiverPrefs) {
        return json(Map.of(
                "callId", callId.toString(),
                "participants", Map.of(
                        caller.getId().toString(), participantView(caller, callerPrefs),
                        receiver.getId().toString(), participantView(receiver, receiverPrefs))));
    }

    private String participantMetadata(UserAccount user, UserPreferences preferences) {
        return json(participantView(user, preferences));
    }

    private Map<String, Object> participantView(UserAccount user, UserPreferences preferences) {
        return Map.of(
                "userId", user.getId().toString(),
                "displayName", user.getDisplayName(),
                "language", user.getPreferredLanguage(),
                "voice", preferences.getPreferredVoice() == null ? "" : preferences.getPreferredVoice(),
                "voiceGender", preferences.getVoiceGender(),
                "translationSpeed", preferences.getTranslationSpeed(),
                "historyEnabled", preferences.isTranslationHistoryEnabled());
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "METADATA_FAILED", "Could not prepare the call.");
        }
    }

    private ApiException unavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "LIVEKIT_UNAVAILABLE", message);
    }
}
