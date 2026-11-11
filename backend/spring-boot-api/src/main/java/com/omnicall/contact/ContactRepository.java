package com.omnicall.contact;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<ContactLink, UUID> {

    List<ContactLink> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<ContactLink> findByOwnerIdAndContactUserId(UUID ownerId, UUID contactUserId);

    void deleteByOwnerIdOrContactUserId(UUID ownerId, UUID contactUserId);
}
