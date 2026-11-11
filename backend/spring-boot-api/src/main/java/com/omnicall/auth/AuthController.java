package com.omnicall.auth;

import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/register")
    public Map<String, Object> register(@Valid @RequestBody AuthService.RegisterRequest request) {
        authService.register(request);
        return Map.of("status", "OTP_SENT", "expiresInSeconds", 300);
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@Valid @RequestBody AuthService.LoginRequest request) {
        authService.login(request.phoneNumber());
        return Map.of("status", "OTP_SENT", "expiresInSeconds", 300);
    }

    @PostMapping("/api/auth/verify-otp")
    public AuthService.AuthResponse verify(@Valid @RequestBody AuthService.VerifyRequest request) {
        return authService.verify(request);
    }

    @PostMapping("/api/auth/refresh")
    public AuthService.AuthResponse refresh(@Valid @RequestBody AuthService.RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/api/auth/logout")
    public Map<String, String> logout(@RequestBody(required = false) AuthService.LogoutRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return Map.of("status", "SIGNED_OUT");
    }
}
