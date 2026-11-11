package com.omnicall.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicall.common.ApiException;
import com.omnicall.common.PhoneNumbers;
import com.omnicall.config.OmniCallProperties;
import com.omnicall.language.LanguageCatalog;
import com.omnicall.privacy.ConsentRecord;
import com.omnicall.privacy.ConsentRecordRepository;
import com.omnicall.security.AuthContext;
import com.omnicall.security.JwtService;
import com.omnicall.security.TokenDenylist;
import com.omnicall.user.AccountStatus;
import com.omnicall.user.UserAccount;
import com.omnicall.user.UserAccountRepository;
import com.omnicall.user.UserPreferences;
import com.omnicall.user.UserPreferencesRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final UserPreferencesRepository preferences;
    private final ConsentRecordRepository consents;
    private final LanguageCatalog languages;
    private final OtpService otpService;
    private final AuthRateLimiter rateLimiter;
    private final List<SmsSender> senders;
    private final SessionService sessions;
    private final JwtService jwtService;
    private final TokenDenylist denylist;
    private final ObjectMapper objectMapper;
    private final OmniCallProperties properties;

    public AuthService(
            UserAccountRepository users,
            UserPreferencesRepository preferences,
            ConsentRecordRepository consents,
            LanguageCatalog languages,
            OtpService otpService,
            AuthRateLimiter rateLimiter,
            List<SmsSender> senders,
            SessionService sessions,
            JwtService jwtService,
            TokenDenylist denylist,
            ObjectMapper objectMapper,
            OmniCallProperties properties) {
        this.users = users;
        this.preferences = preferences;
        this.consents = consents;
        this.languages = languages;
        this.otpService = otpService;
        this.rateLimiter = rateLimiter;
        this.senders = senders;
        this.sessions = sessions;
        this.jwtService = jwtService;
        this.denylist = denylist;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void register(RegisterRequest request) {
        if (!request.acceptedTerms() || !request.translationConsent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONSENT_REQUIRED", "Terms and translation consent are required.");
        }
        String phone = PhoneNumbers.normalize(request.phoneNumber());
        languages.requireEnabled(request.preferredLanguage());
        users.findByPhoneNumber(phone).filter(user -> user.getDeletedAt() == null).ifPresent(user -> {
            throw new ApiException(HttpStatus.CONFLICT, "PHONE_IN_USE", "An account already exists for this phone number.");
        });
        rateLimiter.assertOtpAllowed(phone);
        String code = otpService.issue(phone, "REGISTER", new PendingRegistration(
                request.displayName().trim(), phone, request.preferredLanguage(), request.translationConsent()));
        deliver(phone, code);
    }

    public void login(String phoneNumber) {
        String phone = PhoneNumbers.normalize(phoneNumber);
        rateLimiter.assertOtpAllowed(phone);
        var user = users.findByPhoneNumber(phone).filter(account -> account.getDeletedAt() == null);
        if (user.isEmpty()) {
            return;
        }
        String code = otpService.issue(phone, "LOGIN", phone);
        deliver(phone, code);
    }

    @Transactional
    public AuthResponse verify(VerifyRequest request) {
        String phone = PhoneNumbers.normalize(request.phoneNumber());
        String purpose = request.purpose().toUpperCase();
        String payload = otpService.consume(phone, purpose, request.otp());
        UserAccount user = switch (purpose) {
            case "REGISTER" -> createUser(read(payload));
            case "LOGIN" -> users.findByPhoneNumber(phone)
                    .filter(account -> account.getDeletedAt() == null)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "OTP_INVALID", "That code is incorrect or expired."));
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "PURPOSE_INVALID", "Unsupported verification purpose.");
        };
        return response(sessions.issue(user), user);
    }

    public AuthResponse refresh(String refreshToken) {
        var userId = sessions.requireRefresh(refreshToken);
        sessions.revokeRefresh(refreshToken);
        UserAccount user = users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Sign in again."));
        return response(sessions.issue(user), user);
    }

    public void logout(String refreshToken) {
        sessions.revokeRefresh(refreshToken);
        Object credentials = SecurityContextHolder.getContext().getAuthentication().getCredentials();
        if (credentials instanceof String jti) {
            try {
                String header = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes attributes
                        ? attributes.getRequest().getHeader("Authorization")
                        : null;
                if (header != null && header.startsWith("Bearer ")) {
                    denylist.revoke(jti, jwtService.parse(header.substring(7)).expiresAt());
                }
            } catch (RuntimeException ignored) {
                denylist.revoke(jti, java.time.Instant.now().plus(properties.getJwt().getAccessTtl()));
            }
        }
    }

    private UserAccount createUser(PendingRegistration pending) {
        UserAccount user = users.save(UserAccount.create(
                pending.displayName(), pending.phoneNumber(), pending.preferredLanguage(), pending.translationConsent()));
        preferences.save(UserPreferences.defaults(user.getId()));
        String version = properties.getPrivacy().getPolicyVersion();
        consents.save(ConsentRecord.of(user.getId(), "TERMS", true, version));
        consents.save(ConsentRecord.of(user.getId(), "TRANSLATION_PROCESSING", pending.translationConsent(), version));
        return user;
    }

    private void deliver(String phone, String code) {
        boolean delivered = false;
        for (SmsSender sender : senders) {
            sender.send(phone, code);
            delivered = true;
        }
        if (!delivered) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_NOT_CONFIGURED", "SMS delivery is not configured.");
        }
    }

    private PendingRegistration read(String json) {
        try {
            return objectMapper.readValue(json, PendingRegistration.class);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "OTP_INVALID", "That code is incorrect or expired.");
        }
    }

    private AuthResponse response(SessionService.AuthTokens tokens, UserAccount user) {
        return new AuthResponse(tokens.accessToken(), tokens.refreshToken(), UserView.from(user));
    }

    public UserAccount currentUser() {
        return users.findByIdAndDeletedAtIsNull(AuthContext.current().id())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Sign in again."));
    }

    public record RegisterRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String displayName,
            @jakarta.validation.constraints.NotBlank String phoneNumber,
            @jakarta.validation.constraints.NotBlank String preferredLanguage,
            boolean acceptedTerms,
            boolean translationConsent) {
    }

    public record VerifyRequest(
            @jakarta.validation.constraints.NotBlank String phoneNumber,
            @jakarta.validation.constraints.NotBlank String otp,
            @jakarta.validation.constraints.NotBlank String purpose) {
    }

    public record LoginRequest(@jakarta.validation.constraints.NotBlank String phoneNumber) {
    }

    public record RefreshRequest(@jakarta.validation.constraints.NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record PendingRegistration(String displayName, String phoneNumber, String preferredLanguage, boolean translationConsent) {
    }

    public record UserView(String id, String displayName, String phoneNumber, String preferredLanguage, boolean translationConsent, String profileImageUrl) {
        static UserView from(UserAccount user) {
            return new UserView(
                    user.getId().toString(),
                    user.getDisplayName(),
                    user.getPhoneNumber(),
                    user.getPreferredLanguage(),
                    user.isTranslationConsent(),
                    user.getProfileImageUrl());
        }
    }

    public record AuthResponse(String accessToken, String refreshToken, UserView user) {
    }
}
