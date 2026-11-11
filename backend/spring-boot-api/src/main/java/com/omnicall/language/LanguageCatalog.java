package com.omnicall.language;

import com.omnicall.common.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LanguageCatalog {

    private final LanguageRepository languages;

    public LanguageCatalog(LanguageRepository languages) {
        this.languages = languages;
    }

    public List<LanguageDefinition> list(boolean includeUpcoming) {
        return includeUpcoming ? languages.findAllByOrderBySortOrderAsc() : languages.findByEnabledTrueOrderBySortOrderAsc();
    }

    public LanguageDefinition requireEnabled(String code) {
        LanguageDefinition language = languages.findById(code)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "LANGUAGE_UNKNOWN", "That language is not in the registry."));
        if (!language.isEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LANGUAGE_UNAVAILABLE", language.getEnglishName() + " is not available yet.");
        }
        return language;
    }
}
