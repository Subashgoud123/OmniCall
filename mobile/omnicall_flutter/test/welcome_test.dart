import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:omnicall_flutter/features/auth/auth_screens.dart';

void main() {
  testWidgets('welcome screen states the product', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: WelcomeScreen()));
    expect(find.text('OmniCall'), findsOneWidget);
    expect(find.text('Speak your language. Understand everyone.'), findsOneWidget);
    expect(find.text('Create account'), findsOneWidget);
  });
}
