package com.omnicall.user;

import com.omnicall.common.ApiException;
import com.omnicall.common.PhoneNumbers;
import com.omnicall.config.OmniCallProperties;
import com.omnicall.contact.ContactRepository;
import com.omnicall.language.LanguageCatalog;
import com.omnicall.notification.DeviceTokenRepository;
import com.omnicall.privacy.ConsentRecord;
import com.omnicall.privacy.ConsentRecordRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private static final Pattern VOICE = Pattern.compile("^[a-z]{2}-[A-Z]{2}[A-Za-z0-9-]*$");

    private final UserAccountRepository users;
    private final UserPreferencesRepository preferences;
    private final LanguageCatalog languages;
    private final ContactRepository contacts;
    private final DeviceTokenRepository devices;
    private final com.omnicall.call.TranslationEventRepository translations;
    private final ConsentRecordRepository consents;
    private final OmniCallProperties properties;

    public UserService(
            UserAccountRepository users,
            UserPreferencesRepository preferences,
            LanguageCatalog languages,
            ContactRepository contacts,
            DeviceTokenRepository devices,
            com.omnicall.call.TranslationEventRepository translations,
            ConsentRecordRepository consents,
            OmniCallProperties properties) {
        this.users = users;
        this.preferences = preferences;
        this.languages = languages;
        this.contacts = contacts;
        this.devices = devices;
        this.translations = translations;
        this.consents = consents;
        this.properties = properties;
    }

    public Profile me(UUID userId) {
        return profile(require(userId), preferences(userId));
    }

    public List<PublicUser> search(UUID self, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();
        if (trimmed.startsWith("+")) {
            String phone = PhoneNumbers.normalize(trimmed);
            return users.findByPhoneNumber(phone)
                    .filter(user -> user.getDeletedAt() == null && !user.getId().equals(self))
                    .map(user -> List.of(PublicUser.from(user)))
                    .orElse(List.of());
        }
        if (trimmed.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SEARCH_TOO_SHORT", "Enter at least 3 letters, or a full phone number.");
        }
        return users.searchByName(self, trimmed.toLowerCase(Locale.ROOT)).stream().map(PublicUser::from).toList();
    }

    public PublicUser get(UUID userId) {
        return PublicUser.from(require(userId));
    }

    @Transactional
    public Profile updatePreferences(UUID userId, PreferenceUpdate update) {
        UserAccount user = require(userId);
        if (update.preferredLanguage() != null) {
            languages.requireEnabled(update.preferredLanguage());
            user.setPreferredLanguage(update.preferredLanguage());
        }
        if (update.displayName() != null && !update.displayName().isBlank()) {
            user.setDisplayName(update.displayName().trim());
        }
        if (update.translationConsent() != null) {
            user.setTranslationConsent(update.translationConsent());
            consents.save(ConsentRecord.of(
                    userId,
                    "TRANSLATION_PROCESSING",
                    update.translationConsent(),
                    properties.getPrivacy().getPolicyVersion()));
        }
        users.save(user);
        UserPreferences prefs = preferences(userId);
        if (update.preferredVoice() != null) {
            if (!update.preferredVoice().isBlank() && !VOICE.matcher(update.preferredVoice()).matches()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VOICE_INVALID", "Choose a voice from the language's voice list.");
            }
            prefs.setPreferredVoice(update.preferredVoice().isBlank() ? null : update.preferredVoice());
        }
        if (update.voiceGender() != null) {
            String gender = update.voiceGender().toUpperCase(Locale.ROOT);
            if (!List.of("FEMALE", "MALE", "NEUTRAL").contains(gender)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VOICE_GENDER_INVALID", "Voice gender must be female, male, or neutral.");
            }
            prefs.setVoiceGender(gender);
        }
        if (update.subtitleEnabled() != null) {
            prefs.setSubtitleEnabled(update.subtitleEnabled());
        }
        if (update.originalAudioEnabled() != null) {
            prefs.setOriginalAudioEnabled(update.originalAudioEnabled());
        }
        if (update.translationSpeed() != null) {
            if (update.translationSpeed() < 0.75 || update.translationSpeed() > 1.25) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "SPEED_INVALID", "Translation speed must stay between 0.75 and 1.25.");
            }
            prefs.setTranslationSpeed(update.translationSpeed());
        }
        if (update.notificationsEnabled() != null) {
            prefs.setNotificationsEnabled(update.notificationsEnabled());
        }
        if (update.translationHistoryEnabled() != null) {
            prefs.setTranslationHistoryEnabled(update.translationHistoryEnabled());
            consents.save(ConsentRecord.of(
                    userId,
                    "TRANSLATION_HISTORY",
                    update.translationHistoryEnabled(),
                    properties.getPrivacy().getPolicyVersion()));
        }
        preferences.save(prefs);
        return profile(user, prefs);
    }

    public void saveAvatar(UUID userId, MultipartFile file) {
        require(userId);
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_EMPTY", "Choose a profile image.");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        String extension = switch (type) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "AVATAR_TYPE", "Use a JPEG or PNG image.");
        };
        try {
            Path directory = Path.of(properties.getMedia().getDirectory());
            Files.createDirectories(directory);
            Files.write(directory.resolve(userId + "." + extension), file.getBytes());
            UserAccount user = require(userId);
            user.setProfileImageUrl("/api/users/" + userId + "/avatar");
            users.save(user);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_FAILED", "The profile image could not be saved.");
        }
    }

    public Path avatar(UUID userId) {
        Path directory = Path.of(properties.getMedia().getDirectory());
        Path jpeg = directory.resolve(userId + ".jpg");
        if (Files.exists(jpeg)) {
            return jpeg;
        }
        Path png = directory.resolve(userId + ".png");
        if (Files.exists(png)) {
            return png;
        }
        throw new ApiException(HttpStatus.NOT_FOUND, "AVATAR_MISSING", "No profile image is set.");
    }

    @Transactional
    public void deleteAccount(UUID userId, String confirmation) {
        if (!"DELETE".equals(confirmation)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DELETE_UNCONFIRMED", "Type DELETE to confirm account deletion.");
        }
        UserAccount user = require(userId);
        translations.deleteBySpeakerId(userId);
        contacts.deleteByOwnerIdOrContactUserId(userId, userId);
        devices.deleteByUserId(userId);
        preferences.findByUserId(userId).ifPresent(preferences::delete);
        user.setDisplayName("Deleted user");
        user.setPhoneNumber("deleted-" + userId.toString().replace("-", "").substring(0, 20));
        user.setProfileImageUrl(null);
        user.setTranslationConsent(false);
        user.setStatus(AccountStatus.DELETED);
        user.setDeletedAt(java.time.Instant.now());
        users.save(user);
    }

    private UserAccount require(UUID userId) {
        return users.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "That person is not on OmniCall."));
    }

    private UserPreferences preferences(UUID userId) {
        return preferences.findByUserId(userId).orElseGet(() -> preferences.save(UserPreferences.defaults(userId)));
    }

    private Profile profile(UserAccount user, UserPreferences prefs) {
        return new Profile(
                user.getId(),
                user.getDisplayName(),
                user.getPhoneNumber(),
                user.getPreferredLanguage(),
                user.isTranslationConsent(),
                user.getProfileImageUrl(),
                prefs.getPreferredVoice(),
                prefs.getVoiceGender(),
                prefs.isSubtitleEnabled(),
                prefs.isOriginalAudioEnabled(),
                prefs.getTranslationSpeed(),
                prefs.isNotificationsEnabled(),
                prefs.isTranslationHistoryEnabled());
    }

    public record PreferenceUpdate(
            String displayName,
            String preferredLanguage,
            Boolean translationConsent,
            String preferredVoice,
            String voiceGender,
            Boolean subtitleEnabled,
            Boolean originalAudioEnabled,
            Double translationSpeed,
            Boolean notificationsEnabled,
            Boolean translationHistoryEnabled) {
    }

    public record Profile(
            UUID id,
            String displayName,
            String phoneNumber,
            String preferredLanguage,
            boolean translationConsent,
            String profileImageUrl,
            String preferredVoice,
            String voiceGender,
            boolean subtitleEnabled,
            boolean originalAudioEnabled,
            double translationSpeed,
            boolean notificationsEnabled,
            boolean translationHistoryEnabled) {
    }

    public record PublicUser(UUID id, String displayName, String preferredLanguage, String profileImageUrl) {
        public static PublicUser from(UserAccount user) {
            return new PublicUser(user.getId(), user.getDisplayName(), user.getPreferredLanguage(), user.getProfileImageUrl());
        }
    }

    public record DeleteAccountRequest(String confirmation) {
    }
}
