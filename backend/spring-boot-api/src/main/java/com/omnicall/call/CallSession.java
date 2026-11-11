package com.omnicall.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions")
public class CallSession {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID callerId;

    @Column(nullable = false)
    private UUID receiverId;

    @Column(nullable = false, length = 16)
    private String callerLanguage;

    @Column(nullable = false, length = 16)
    private String receiverLanguage;

    @Column(nullable = false, unique = true, length = 80)
    private String roomName;

    @Column(length = 80)
    private String dispatchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallStatus status;

    private Instant startedAt;
    private Instant endedAt;
    private Integer durationSeconds;

    @Column(length = 40)
    private String endReason;

    @Column(nullable = false)
    private Instant createdAt;

    public static CallSession ringing(UUID callerId, UUID receiverId, String callerLanguage, String receiverLanguage) {
        CallSession call = new CallSession();
        call.id = UUID.randomUUID();
        call.callerId = callerId;
        call.receiverId = receiverId;
        call.callerLanguage = callerLanguage;
        call.receiverLanguage = receiverLanguage;
        call.roomName = "omnicall-" + call.id;
        call.status = CallStatus.RINGING;
        call.createdAt = Instant.now();
        return call;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCallerId() {
        return callerId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public String getCallerLanguage() {
        return callerLanguage;
    }

    public String getReceiverLanguage() {
        return receiverLanguage;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(String dispatchId) {
        this.dispatchId = dispatchId;
    }

    public CallStatus getStatus() {
        return status;
    }

    public void setStatus(CallStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getEndReason() {
        return endReason;
    }

    public void setEndReason(String endReason) {
        this.endReason = endReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean involves(UUID userId) {
        return callerId.equals(userId) || receiverId.equals(userId);
    }

    public UUID otherParty(UUID userId) {
        return callerId.equals(userId) ? receiverId : callerId;
    }

    public String languageOf(UUID userId) {
        return callerId.equals(userId) ? callerLanguage : receiverLanguage;
    }
}
