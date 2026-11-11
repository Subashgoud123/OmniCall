import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:omnicall_flutter/features/auth/auth_screens.dart';
import 'package:omnicall_flutter/features/calling/call_screens.dart';
import 'package:omnicall_flutter/features/home/home_screens.dart';
import 'package:omnicall_flutter/features/session/session_controller.dart';
import 'package:omnicall_flutter/features/settings/settings_screens.dart';

final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<int>(0);
  ref.listen(sessionProvider, (_, __) => refresh.value++);
  ref.onDispose(refresh.dispose);
  return GoRouter(
    initialLocation: '/welcome',
    refreshListenable: refresh,
    redirect: (context, state) {
      final session = ref.read(sessionProvider);
      final path = state.uri.path;
      if (!session.ready) return path == '/splash' ? null : '/splash';
      final signedIn = session.user != null;
      final public = path == '/welcome' || path == '/login' || path == '/register' || path.startsWith('/verify');
      if (!signedIn && !public) return '/welcome';
      if (signedIn && public) return '/home';
      if (signedIn && session.incoming != null && !path.startsWith('/call/')) return '/call/incoming/${session.incoming!.id}';
      return null;
    },
    routes: [
      GoRoute(path: '/splash', builder: (_, __) => const Scaffold(body: Center(child: CircularProgressIndicator()))),
      GoRoute(path: '/welcome', builder: (_, __) => const WelcomeScreen()),
      GoRoute(path: '/register', builder: (_, __) => const RegisterScreen()),
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(
        path: '/verify',
        builder: (_, state) => VerifyScreen(
          phone: state.uri.queryParameters['phone'] ?? '',
          purpose: state.uri.queryParameters['purpose'] ?? 'LOGIN',
        ),
      ),
      GoRoute(path: '/home', builder: (_, __) => const HomeScreen()),
      GoRoute(path: '/search', builder: (_, __) => const SearchScreen()),
      GoRoute(path: '/contacts', builder: (_, __) => const ContactsScreen()),
      GoRoute(path: '/history', builder: (_, __) => const HistoryScreen()),
      GoRoute(path: '/settings', builder: (_, __) => const SettingsScreen()),
      GoRoute(path: '/profile', builder: (_, __) => const ProfileScreen()),
      GoRoute(path: '/privacy', builder: (_, __) => const PrivacyScreen()),
      GoRoute(path: '/call/outgoing/:id', builder: (_, state) => OutgoingCallScreen(callId: state.pathParameters['id']!)),
      GoRoute(path: '/call/incoming/:id', builder: (_, state) => IncomingCallScreen(callId: state.pathParameters['id']!)),
      GoRoute(path: '/call/active/:id', builder: (_, state) => ActiveCallScreen(callId: state.pathParameters['id']!)),
    ],
  );
});
