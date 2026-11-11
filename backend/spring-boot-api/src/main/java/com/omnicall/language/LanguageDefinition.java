package com.omnicall.language;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "languages")
public class LanguageDefinition {

    @Id
    @Column(length = 16)
    private String code;

    @Column(nullable = false, length = 64)
    private String englishName;

    @Column(nullable = false, length = 64)
    private String nativeName;

    @Column(nullable = false, length = 16)
    private String translationCode;

    @Column(nullable = false)
    private boolean speechEnabled;

    @Column(nullable = false)
    private boolean translationEnabled;

    @Column(nullable = false)
    private boolean ttsEnabled;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int sortOrder;

    public String getCode() {
        return code;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getNativeName() {
        return nativeName;
    }

    public String getTranslationCode() {
        return translationCode;
    }

    public boolean isSpeechEnabled() {
        return speechEnabled;
    }

    public boolean isTranslationEnabled() {
        return translationEnabled;
    }

    public boolean isTtsEnabled() {
        return ttsEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
