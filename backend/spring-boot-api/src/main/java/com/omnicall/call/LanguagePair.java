package com.omnicall.call;

public record LanguagePair(String speakerLanguage, String listenerLanguage) {

    public String targetLanguage() {
        return listenerLanguage;
    }

    public boolean translationRequired() {
        return speakerLanguage != null && !speakerLanguage.equals(listenerLanguage);
    }
}
