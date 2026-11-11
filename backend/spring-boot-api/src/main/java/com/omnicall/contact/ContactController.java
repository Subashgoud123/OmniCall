package com.omnicall.contact;

import com.omnicall.security.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContactController {

    private final ContactService contacts;

    public ContactController(ContactService contacts) {
        this.contacts = contacts;
    }

    @GetMapping("/api/contacts")
    public List<ContactService.ContactView> list() {
        return contacts.list(AuthContext.current().id());
    }

    @PostMapping("/api/contacts/invite")
    public ContactService.ContactView invite(@Valid @RequestBody ContactService.InviteRequest request) {
        return contacts.invite(AuthContext.current().id(), request.userId());
    }

    @PostMapping("/api/contacts/{userId}/accept")
    public ContactService.ContactView accept(@PathVariable UUID userId) {
        return contacts.accept(AuthContext.current().id(), userId);
    }

    @DeleteMapping("/api/contacts/{userId}")
    public Map<String, String> remove(@PathVariable UUID userId) {
        contacts.remove(AuthContext.current().id(), userId);
        return Map.of("status", "REMOVED");
    }
}
