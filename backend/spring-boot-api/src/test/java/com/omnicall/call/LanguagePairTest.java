package com.omnicall.call;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LanguagePairTest {

    @Test
    void hindiSpeakerIsTranslatedIntoTheListenersLanguage() {
        LanguagePair pair = new LanguagePair("hi-IN", "te-IN");
        assertEquals("te-IN", pair.targetLanguage());
        assertTrue(pair.translationRequired());
    }

    @Test
    void teluguSpeakerIsTranslatedIntoHindi() {
        LanguagePair pair = new LanguagePair("te-IN", "hi-IN");
        assertEquals("hi-IN", pair.targetLanguage());
        assertTrue(pair.translationRequired());
    }

    @Test
    void sameLanguageDoesNotRequestTranslation() {
        assertFalse(new LanguagePair("hi-IN", "hi-IN").translationRequired());
    }
}
