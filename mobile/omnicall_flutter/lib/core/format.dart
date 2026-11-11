class AppException implements Exception {
  AppException(this.message, {this.code});
  final String message;
  final String? code;

  @override
  String toString() => message;
}

String formatDuration(int? seconds) {
  final value = seconds ?? 0;
  final minutes = value ~/ 60;
  final rest = value % 60;
  return '${minutes.toString().padLeft(2, '0')}:${rest.toString().padLeft(2, '0')}';
}

String languageLabel(String code) {
  return switch (code) {
    'hi-IN' => 'Hindi',
    'te-IN' => 'Telugu',
    'en-IN' => 'English',
    'ta-IN' => 'Tamil',
    'kn-IN' => 'Kannada',
    'ml-IN' => 'Malayalam',
    'mr-IN' => 'Marathi',
    'bn-IN' => 'Bengali',
    'gu-IN' => 'Gujarati',
    'pa-IN' => 'Punjabi',
    _ => code,
  };
}
