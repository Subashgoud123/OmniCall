package com.omnicall.call;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {

    @Query("""
            select call from CallSession call
            where call.status in :statuses
              and (call.callerId = :userId or call.receiverId = :userId)
            """)
    List<CallSession> findOpenForUser(UUID userId, Collection<CallStatus> statuses);

    List<CallSession> findByCallerIdOrReceiverIdOrderByCreatedAtDesc(UUID callerId, UUID receiverId);

    List<CallSession> findByStatusAndCreatedAtBefore(CallStatus status, Instant createdAt);
}
