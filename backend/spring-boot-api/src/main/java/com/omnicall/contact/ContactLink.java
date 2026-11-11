package com.omnicall.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacts")
public class ContactLink {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private UUID contactUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    public static ContactLink invite(UUID ownerId, UUID contactUserId) {
        ContactLink link = new ContactLink();
        link.id = UUID.randomUUID();
        link.ownerId = ownerId;
        link.contactUserId = contactUserId;
        link.status = ContactStatus.INVITED;
        link.createdAt = Instant.now();
        return link;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getContactUserId() {
        return contactUserId;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public void setStatus(ContactStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
