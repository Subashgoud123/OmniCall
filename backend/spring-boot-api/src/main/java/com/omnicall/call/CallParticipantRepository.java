package com.omnicall.call;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallParticipantRepository extends JpaRepository<CallParticipant, UUID> {

    List<CallParticipant> findByCallId(UUID callId);
}
