import 'package:flutter/material.dart';

class AppTheme {
  static const navy = Color(0xFF0B1F3A);
  static const blue = Color(0xFF2F6FED);
  static const teal = Color(0xFF1F8A9E);

  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(seedColor: navy, primary: navy, secondary: teal, brightness: Brightness.light);
    return _theme(scheme);
  }

  static ThemeData dark() {
    final scheme = ColorScheme.fromSeed(seedColor: navy, primary: const Color(0xFF8FB4FF), secondary: teal, brightness: Brightness.dark);
    return _theme(scheme);
  }

  static ThemeData _theme(ColorScheme scheme) {
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scheme.surface,
      appBarTheme: AppBarTheme(backgroundColor: scheme.surface, foregroundColor: scheme.onSurface, elevation: 0),
      cardTheme: CardThemeData(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)), elevation: 0),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))),
      ),
    );
  }
}
