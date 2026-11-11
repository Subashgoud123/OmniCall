package com.omnicall.language;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LanguageController {

    private final LanguageCatalog catalog;

    public LanguageController(LanguageCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/api/languages")
    public List<LanguageResponse> list(@RequestParam(defaultValue = "false") boolean includeUpcoming) {
        return catalog.list(includeUpcoming).stream().map(LanguageResponse::from).toList();
    }

    public record LanguageResponse(
            String code,
            String englishName,
            String nativeName,
            boolean speechEnabled,
            boolean translationEnabled,
            boolean ttsEnabled,
            boolean enabled) {

        static LanguageResponse from(LanguageDefinition language) {
            return new LanguageResponse(
                    language.getCode(),
                    language.getEnglishName(),
                    language.getNativeName(),
                    language.isSpeechEnabled(),
                    language.isTranslationEnabled(),
                    language.isTtsEnabled(),
                    language.isEnabled());
        }
    }
}
