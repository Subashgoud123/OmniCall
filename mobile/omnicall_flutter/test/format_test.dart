import 'package:flutter_test/flutter_test.dart';
import 'package:omnicall_flutter/core/format.dart';
import 'package:omnicall_flutter/domain/call_direction.dart';

void main() {
  test('formats call duration as minutes and seconds', () {
    expect(formatDuration(65), '01:05');
    expect(formatDuration(null), '00:00');
  });

  test('labels the initial languages', () {
    expect(languageLabel('hi-IN'), 'Hindi');
    expect(languageLabel('te-IN'), 'Telugu');
  });

  test('translation follows the listener language', () {
    expect(CallDirection.needsTranslation('hi-IN', 'te-IN'), isTrue);
    expect(CallDirection.targetFor('te-IN'), 'te-IN');
    expect(CallDirection.needsTranslation('hi-IN', 'hi-IN'), isFalse);
  });
}
