package com.omnicall.call;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TranslationEventRepository extends JpaRepository<TranslationEvent, UUID> {

    @Modifying
    @Query("delete from TranslationEvent event where event.createdAt < :cutoff")
    int deleteOlderThan(Instant cutoff);

    void deleteBySpeakerId(UUID speakerId);
}
