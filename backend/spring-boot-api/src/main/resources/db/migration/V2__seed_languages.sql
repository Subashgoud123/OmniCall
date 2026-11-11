INSERT INTO languages (
    code, english_name, native_name, translation_code,
    speech_enabled, translation_enabled, tts_enabled, enabled, sort_order
) VALUES
    ('hi-IN', 'Hindi', 'हिन्दी', 'hi', TRUE, TRUE, TRUE, TRUE, 1),
    ('te-IN', 'Telugu', 'తెలుగు', 'te', TRUE, TRUE, TRUE, TRUE, 2),
    ('en-IN', 'English', 'English', 'en', TRUE, TRUE, TRUE, FALSE, 3),
    ('ta-IN', 'Tamil', 'தமிழ்', 'ta', TRUE, TRUE, TRUE, FALSE, 4),
    ('kn-IN', 'Kannada', 'ಕನ್ನಡ', 'kn', TRUE, TRUE, TRUE, FALSE, 5),
    ('ml-IN', 'Malayalam', 'മലയാളം', 'ml', TRUE, TRUE, TRUE, FALSE, 6),
    ('mr-IN', 'Marathi', 'मराठी', 'mr', TRUE, TRUE, TRUE, FALSE, 7),
    ('bn-IN', 'Bengali', 'বাংলা', 'bn', TRUE, TRUE, TRUE, FALSE, 8),
    ('gu-IN', 'Gujarati', 'ગુજરાતી', 'gu', TRUE, TRUE, TRUE, FALSE, 9),
    ('pa-IN', 'Punjabi', 'ਪੰਜਾਬੀ', 'pa', TRUE, TRUE, TRUE, FALSE, 10);
