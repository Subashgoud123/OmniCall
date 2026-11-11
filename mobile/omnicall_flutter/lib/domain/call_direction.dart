class CallDirection {
  static bool needsTranslation(String speakerLanguage, String listenerLanguage) {
    return speakerLanguage != listenerLanguage;
  }

  static String targetFor(String listenerLanguage) => listenerLanguage;
}
