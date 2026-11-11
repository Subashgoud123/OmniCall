package com.omnicall.contact;

import com.omnicall.common.ApiException;
import com.omnicall.notification.AppNotification;
import com.omnicall.notification.NotificationRepository;
import com.omnicall.signaling.CallSignalingHandler;
import com.omnicall.user.UserAccount;
import com.omnicall.user.UserAccountRepository;
import com.omnicall.user.UserService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private final ContactRepository contacts;
    private final UserAccountRepository users;
    private final NotificationRepository notifications;
    private final CallSignalingHandler signaling;

    public ContactService(
            ContactRepository contacts,
            UserAccountRepository users,
            NotificationRepository notifications,
            CallSignalingHandler signaling) {
        this.contacts = contacts;
        this.users = users;
        this.notifications = notifications;
        this.signaling = signaling;
    }

    public List<ContactView> list(UUID ownerId) {
        return contacts.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::view).toList();
    }

    @Transactional
    public ContactView invite(UUID ownerId, UUID contactUserId) {
        if (ownerId.equals(contactUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONTACT_SELF", "You are already you.");
        }
        UserAccount owner = require(ownerId);
        require(contactUserId);
        contacts.findByOwnerIdAndContactUserId(ownerId, contactUserId).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "CONTACT_EXISTS", "That person is already in your contacts.");
        });
        ContactLink link = contacts.save(ContactLink.invite(ownerId, contactUserId));
        signaling.send(contactUserId, Map.of(
                "type", "CONTACT_INVITE",
                "contactId", link.getId().toString(),
                "fromUserId", ownerId.toString(),
                "fromName", owner.getDisplayName()));
        notifications.save(AppNotification.pending(contactUserId, "CONTACT_INVITE", owner.getDisplayName()));
        return view(link);
    }

    @Transactional
    public ContactView accept(UUID userId, UUID ownerId) {
        ContactLink incoming = contacts.findByOwnerIdAndContactUserId(ownerId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTACT_NOT_FOUND", "That invitation does not exist."));
        incoming.setStatus(ContactStatus.ACCEPTED);
        contacts.save(incoming);
        contacts.findByOwnerIdAndContactUserId(userId, ownerId).orElseGet(() -> {
            ContactLink reciprocal = ContactLink.invite(userId, ownerId);
            reciprocal.setStatus(ContactStatus.ACCEPTED);
            return contacts.save(reciprocal);
        });
        return view(incoming);
    }

    @Transactional
    public void remove(UUID ownerId, UUID contactUserId) {
        ContactLink link = contacts.findByOwnerIdAndContactUserId(ownerId, contactUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CONTACT_NOT_FOUND", "That contact does not exist."));
        contacts.delete(link);
    }

    private ContactView view(ContactLink link) {
        UserAccount person = require(link.getContactUserId());
        return new ContactView(link.getId(), UserService.PublicUser.from(person), link.getStatus().name());
    }

    private UserAccount require(UUID userId) {
        return users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "That person is not on OmniCall."));
    }

    public record ContactView(UUID id, UserService.PublicUser user, String status) {
    }

    public record InviteRequest(@jakarta.validation.constraints.NotNull UUID userId) {
    }
}
