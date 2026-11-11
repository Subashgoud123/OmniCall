import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:omnicall_flutter/core/config.dart';
import 'package:omnicall_flutter/core/format.dart';

final secureStorageProvider = Provider((ref) => const FlutterSecureStorage());

final tokenStoreProvider = Provider((ref) => TokenStore(ref.watch(secureStorageProvider)));

class TokenStore {
  TokenStore(this._storage);
  final FlutterSecureStorage _storage;
  String? accessToken;
  String? refreshToken;

  Future<void> load() async {
    accessToken = await _storage.read(key: 'access_token');
    refreshToken = await _storage.read(key: 'refresh_token');
  }

  Future<void> save(String access, String refresh) async {
    accessToken = access;
    refreshToken = refresh;
    await _storage.write(key: 'access_token', value: access);
    await _storage.write(key: 'refresh_token', value: refresh);
  }

  Future<void> clear() async {
    accessToken = null;
    refreshToken = null;
    await _storage.deleteAll();
  }
}

final dioProvider = Provider<Dio>((ref) {
  final tokens = ref.watch(tokenStoreProvider);
  final dio = Dio(BaseOptions(baseUrl: AppConfig.apiBase, connectTimeout: const Duration(seconds: 15)));
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) {
      final token = tokens.accessToken;
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    },
    onError: (error, handler) async {
      if (error.response?.statusCode == 401 && tokens.refreshToken != null && error.requestOptions.path != '/api/auth/refresh') {
        try {
          final refresh = await Dio(BaseOptions(baseUrl: AppConfig.apiBase)).post('/api/auth/refresh', data: {'refreshToken': tokens.refreshToken});
          final access = refresh.data['accessToken'] as String;
          final nextRefresh = refresh.data['refreshToken'] as String;
          await tokens.save(access, nextRefresh);
          final request = error.requestOptions;
          request.headers['Authorization'] = 'Bearer $access';
          final response = await dio.fetch(request);
          handler.resolve(response);
          return;
        } catch (_) {
          await tokens.clear();
        }
      }
      handler.next(error);
    },
  ));
  return dio;
});

AppException apiError(Object error) {
  if (error is DioException) {
    final data = error.response?.data;
    if (data is Map && data['message'] is String) {
      return AppException(data['message'] as String, code: data['code'] as String?);
    }
    if (error.type == DioExceptionType.connectionError) {
      return AppException('OmniCall cannot reach the server. Check the connection and API address.');
    }
  }
  return AppException('Something went wrong. Try again.');
}
