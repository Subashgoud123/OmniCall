import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:omnicall_flutter/core/api_client.dart';
import 'package:omnicall_flutter/data/models.dart';

final apiRepositoryProvider = Provider((ref) => ApiRepository(ref.watch(dioProvider)));

class ApiRepository {
  ApiRepository(this._dio);
  final Dio _dio;

  Future<void> register({
    required String displayName,
    required String phoneNumber,
    required String preferredLanguage,
    required bool acceptedTerms,
    required bool translationConsent,
  }) async {
    await _dio.post('/api/auth/register', data: {
      'displayName': displayName,
      'phoneNumber': phoneNumber,
      'preferredLanguage': preferredLanguage,
      'acceptedTerms': acceptedTerms,
      'translationConsent': translationConsent,
    });
  }

  Future<void> login(String phoneNumber) async {
    await _dio.post('/api/auth/login', data: {'phoneNumber': phoneNumber});
  }

  Future<({String access, String refresh, UserProfile user})> verify(String phone, String otp, String purpose) async {
    final response = await _dio.post('/api/auth/verify-otp', data: {'phoneNumber': phone, 'otp': otp, 'purpose': purpose});
    return _auth(response.data as Map<String, dynamic>);
  }

  Future<UserProfile> me() async {
    final response = await _dio.get('/api/users/me');
    return UserProfile.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<LanguageOption>> languages({bool includeUpcoming = true}) async {
    final response = await _dio.get('/api/languages', queryParameters: {'includeUpcoming': includeUpcoming});
    return (response.data as List).map((item) => LanguageOption.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<UserProfile> updatePreferences(Map<String, dynamic> body) async {
    final response = await _dio.put('/api/users/preferences', data: body);
    return UserProfile.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<PublicUser>> search(String query) async {
    final response = await _dio.get('/api/users/search', queryParameters: {'query': query});
    return (response.data as List).map((item) => PublicUser.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<List<ContactEntry>> contacts() async {
    final response = await _dio.get('/api/contacts');
    return (response.data as List).map((item) => ContactEntry.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<void> invite(String userId) async {
    await _dio.post('/api/contacts/invite', data: {'userId': userId});
  }

  Future<void> acceptContact(String userId) async {
    await _dio.post('/api/contacts/$userId/accept');
  }

  Future<void> removeContact(String userId) async {
    await _dio.delete('/api/contacts/$userId');
  }

  Future<CallSession> startCall(String receiverId) async {
    final response = await _dio.post('/api/calls', data: {'receiverId': receiverId});
    return CallSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<CallSession> acceptCall(String callId) async {
    final response = await _dio.post('/api/calls/$callId/accept');
    return CallSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<CallSession> rejectCall(String callId) async {
    final response = await _dio.post('/api/calls/$callId/reject');
    return CallSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<CallSession> endCall(String callId) async {
    final response = await _dio.post('/api/calls/$callId/end');
    return CallSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<CallSession> call(String callId) async {
    final response = await _dio.get('/api/calls/$callId');
    return CallSession.fromJson(response.data as Map<String, dynamic>);
  }

  Future<List<CallSession>> history() async {
    final response = await _dio.get('/api/calls/history');
    return (response.data as List).map((item) => CallSession.fromJson(item as Map<String, dynamic>)).toList();
  }

  Future<CallCredentials> token(String callId) async {
    final response = await _dio.get('/api/calls/$callId/token');
    return CallCredentials.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> registerDevice(String token, String platform) async {
    await _dio.post('/api/devices', data: {'token': token, 'platform': platform});
  }

  Future<void> logout(String? refresh) async {
    await _dio.post('/api/auth/logout', data: {'refreshToken': refresh});
  }

  Future<void> uploadAvatar(String path) async {
    await _dio.post('/api/users/me/avatar', data: FormData.fromMap({'file': await MultipartFile.fromFile(path)}));
  }

  Future<void> deleteAccount() async {
    await _dio.delete('/api/users/me', data: {'confirmation': 'DELETE'});
  }

  ({String access, String refresh, UserProfile user}) _auth(Map<String, dynamic> json) {
    return (
      access: json['accessToken'] as String,
      refresh: json['refreshToken'] as String,
      user: UserProfile.fromJson(json['user'] as Map<String, dynamic>),
    );
  }
}
