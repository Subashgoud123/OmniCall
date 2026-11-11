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
@Table(name = "call_participants")
public class CallParticipant {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID callId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 20)
    private ParticipantRole participantRole;

    @Column(nullable = false, length = 20)
    private String connectionStatus;

    private Instant joinedAt;
    private Instant leftAt;

    public static CallParticipant create(UUID callId, UUID userId, ParticipantRole role) {
        CallParticipant participant = new CallParticipant();
        participant.id = UUID.randomUUID();
        participant.callId = callId;
        participant.userId = userId;
        participant.participantRole = role;
        participant.connectionStatus = "INVITED";
        return participant;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCallId() {
        return callId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ParticipantRole getParticipantRole() {
        return participantRole;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(Instant leftAt) {
        this.leftAt = leftAt;
    }
}
