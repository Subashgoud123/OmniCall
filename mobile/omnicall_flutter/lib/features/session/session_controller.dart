import 'dart:async';
import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:omnicall_flutter/core/api_client.dart';
import 'package:omnicall_flutter/core/config.dart';
import 'package:omnicall_flutter/data/api_repository.dart';
import 'package:omnicall_flutter/data/models.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class SessionState {
  const SessionState({this.user, this.ready = false, this.incoming});
  final UserProfile? user;
  final bool ready;
  final CallSession? incoming;

  SessionState copyWith({UserProfile? user, bool? ready, CallSession? incoming, bool clearIncoming = false, bool clearUser = false}) {
    return SessionState(
      user: clearUser ? null : user ?? this.user,
      ready: ready ?? this.ready,
      incoming: clearIncoming ? null : incoming ?? this.incoming,
    );
  }
}

class SessionController extends Notifier<SessionState> {
  WebSocketChannel? _socket;
  StreamSubscription<dynamic>? _subscription;

  @override
  SessionState build() {
    Future.microtask(restore);
    ref.onDispose(() {
      _subscription?.cancel();
      _socket?.sink.close();
    });
    return const SessionState();
  }

  ApiRepository get _api => ref.read(apiRepositoryProvider);

  Future<void> restore() async {
    final tokens = ref.read(tokenStoreProvider);
    await tokens.load();
    if (tokens.accessToken == null) {
      state = state.copyWith(ready: true, clearUser: true);
      return;
    }
    try {
      final user = await _api.me();
      state = state.copyWith(user: user, ready: true);
      _connectSocket();
    } catch (_) {
      await tokens.clear();
      state = state.copyWith(ready: true, clearUser: true);
    }
  }

  Future<void> verify(String phone, String otp, String purpose) async {
    final result = await _api.verify(phone, otp, purpose);
    await ref.read(tokenStoreProvider).save(result.access, result.refresh);
    final profile = await _api.me();
    state = state.copyWith(user: profile, ready: true);
    _connectSocket();
  }

  Future<void> logout() async {
    try {
      await _api.logout(ref.read(tokenStoreProvider).refreshToken);
    } catch (_) {}
    await ref.read(tokenStoreProvider).clear();
    _subscription?.cancel();
    _socket?.sink.close();
    state = const SessionState(ready: true);
  }

  void clearIncoming() => state = state.copyWith(clearIncoming: true);

  void updateUser(UserProfile user) => state = state.copyWith(user: user);

  void _connectSocket() {
    final token = ref.read(tokenStoreProvider).accessToken;
    if (token == null) return;
    _subscription?.cancel();
    _socket?.sink.close();
    _socket = WebSocketChannel.connect(Uri.parse('${AppConfig.wsBase}?token=$token'));
    _subscription = _socket!.stream.listen((event) async {
      final message = jsonDecode(event as String) as Map<String, dynamic>;
      if (message['type'] == 'INCOMING_CALL') {
        final call = await _api.call(message['callId'].toString());
        state = state.copyWith(incoming: call);
      }
    });
  }
}

final sessionProvider = NotifierProvider<SessionController, SessionState>(SessionController.new);
