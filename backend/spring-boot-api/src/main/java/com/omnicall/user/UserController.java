package com.omnicall.user;

import com.omnicall.security.AuthContext;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/api/users/me")
    public UserService.Profile me() {
        return users.me(AuthContext.current().id());
    }

    @PutMapping("/api/users/preferences")
    public UserService.Profile preferences(@RequestBody UserService.PreferenceUpdate update) {
        return users.updatePreferences(AuthContext.current().id(), update);
    }

    @GetMapping("/api/users/search")
    public List<UserService.PublicUser> search(@RequestParam("query") String query) {
        return users.search(AuthContext.current().id(), query);
    }

    @GetMapping("/api/users/{userId}")
    public UserService.PublicUser get(@PathVariable java.util.UUID userId) {
        return users.get(userId);
    }

    @PostMapping(value = "/api/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> avatar(@RequestParam("file") MultipartFile file) {
        users.saveAvatar(AuthContext.current().id(), file);
        return Map.of("status", "SAVED");
    }

    @GetMapping("/api/users/{userId}/avatar")
    public ResponseEntity<Resource> avatar(@PathVariable java.util.UUID userId) throws java.io.IOException {
        var path = users.avatar(userId);
        String type = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type))
                .body(new FileSystemResource(path));
    }

    @DeleteMapping("/api/users/me")
    public Map<String, String> delete(@RequestBody UserService.DeleteAccountRequest request) {
        users.deleteAccount(AuthContext.current().id(), request.confirmation());
        return Map.of("status", "DELETED");
    }
}
