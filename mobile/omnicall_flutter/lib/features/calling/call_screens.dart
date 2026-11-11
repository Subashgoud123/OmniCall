import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:omnicall_flutter/core/api_client.dart';
import 'package:omnicall_flutter/core/format.dart';
import 'package:omnicall_flutter/core/theme.dart';
import 'package:omnicall_flutter/data/api_repository.dart';
import 'package:omnicall_flutter/data/models.dart';
import 'package:omnicall_flutter/features/calling/call_room.dart';
import 'package:omnicall_flutter/features/session/session_controller.dart';

class OutgoingCallScreen extends ConsumerStatefulWidget {
  const OutgoingCallScreen({super.key, required this.callId});
  final String callId;

  @override
  ConsumerState<OutgoingCallScreen> createState() => _OutgoingCallScreenState();
}

class _OutgoingCallScreenState extends ConsumerState<OutgoingCallScreen> {
  Timer? _timer;
  CallSession? _call;

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 2), (_) => _poll());
    _poll();
  }

  Future<void> _poll() async {
    try {
      final call = await ref.read(apiRepositoryProvider).call(widget.callId);
      if (!mounted) return;
      setState(() => _call = call);
      if (call.status == 'ACTIVE') {
        context.go('/call/active/${call.id}');
      } else if (call.status == 'REJECTED' || call.status == 'MISSED' || call.status == 'ENDED' || call.status == 'FAILED') {
        context.go('/home');
      }
    } catch (_) {}
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final call = _call;
    return Scaffold(
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const CircleAvatar(radius: 48, child: Icon(Icons.person, size: 40)),
              const SizedBox(height: 16),
              Text(call?.receiverName ?? 'Calling', style: Theme.of(context).textTheme.headlineSmall),
              Text(call == null ? '' : '${languageLabel(call.callerLanguage)} → ${languageLabel(call.receiverLanguage)}'),
              const SizedBox(height: 8),
              const Text('Ringing'),
              const SizedBox(height: 32),
              IconButton.filled(
                style: IconButton.styleFrom(backgroundColor: Colors.red),
                onPressed: () async {
                  await ref.read(apiRepositoryProvider).endCall(widget.callId);
                  if (context.mounted) context.go('/home');
                },
                icon: const Icon(Icons.call_end),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class IncomingCallScreen extends ConsumerWidget {
  const IncomingCallScreen({super.key, required this.callId});
  final String callId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      body: FutureBuilder(
        future: ref.read(apiRepositoryProvider).call(callId),
        builder: (context, snapshot) {
          final call = snapshot.data;
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(32),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(call?.callerName ?? 'Incoming call', style: Theme.of(context).textTheme.headlineSmall),
                  Text(call == null ? '' : 'Speaks ${languageLabel(call.callerLanguage)}'),
                  const SizedBox(height: 8),
                  const Text('Audio will be translated. This is not an end-to-end encrypted call.'),
                  const SizedBox(height: 32),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      IconButton.filled(
                        style: IconButton.styleFrom(backgroundColor: Colors.red),
                        onPressed: () async {
                          await ref.read(apiRepositoryProvider).rejectCall(callId);
                          ref.read(sessionProvider.notifier).clearIncoming();
                          if (context.mounted) context.go('/home');
                        },
                        icon: const Icon(Icons.call_end),
                      ),
                      const SizedBox(width: 24),
                      IconButton.filled(
                        style: IconButton.styleFrom(backgroundColor: Colors.green),
                        onPressed: () async {
                          await ref.read(apiRepositoryProvider).acceptCall(callId);
                          ref.read(sessionProvider.notifier).clearIncoming();
                          if (context.mounted) context.go('/call/active/$callId');
                        },
                        icon: const Icon(Icons.call),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class ActiveCallScreen extends ConsumerStatefulWidget {
  const ActiveCallScreen({super.key, required this.callId});
  final String callId;

  @override
  ConsumerState<ActiveCallScreen> createState() => _ActiveCallScreenState();
}

class _ActiveCallScreenState extends ConsumerState<ActiveCallScreen> {
  CallSession? _call;
  Timer? _clock;
  int _seconds = 0;
  String? _error;

  @override
  void initState() {
    super.initState();
    _start();
    _clock = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted && ref.read(callRoomProvider).connected) setState(() => _seconds += 1);
    });
  }

  Future<void> _start() async {
    try {
      final api = ref.read(apiRepositoryProvider);
      final call = await api.call(widget.callId);
      final credentials = await api.token(widget.callId);
      final user = ref.read(sessionProvider).user!;
      await ref.read(callRoomProvider.notifier).connect(
            url: credentials.url,
            token: credentials.token,
            myLanguage: user.preferredLanguage,
            subtitles: user.subtitleEnabled,
            originalAudio: user.originalAudioEnabled,
            peerName: call.otherName(user.id),
          );
      if (mounted) setState(() => _call = call);
    } catch (error) {
      if (mounted) setState(() => _error = apiError(error).message);
    }
  }

  @override
  void dispose() {
    _clock?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final room = ref.watch(callRoomProvider);
    final user = ref.watch(sessionProvider).user;
    final call = _call;
    final peer = user == null || call == null ? 'Connecting' : call.otherName(user.id);
    final cue = room.subtitle;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(room.connected ? 'Connected' : room.status),
                  Text(formatDuration(_seconds)),
                ],
              ),
              const Spacer(),
              CircleAvatar(radius: 52, backgroundColor: AppTheme.navy, child: Text(peer.isEmpty ? '?' : peer.characters.first, style: const TextStyle(color: Colors.white, fontSize: 32))),
              const SizedBox(height: 12),
              Text(peer, style: Theme.of(context).textTheme.headlineSmall),
              if (call != null) Text('${languageLabel(call.callerLanguage)} ↔ ${languageLabel(call.receiverLanguage)}'),
              const SizedBox(height: 8),
              Text(room.status),
              if (_error != null || room.failure != null) Text(_error ?? room.failure!, textAlign: TextAlign.center),
              const Spacer(),
              if (user?.subtitleEnabled != false)
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(cue == null ? 'Waiting for speech' : (cue.partial ? 'Hearing…' : 'Translated')),
                        const SizedBox(height: 8),
                        Text(cue?.originalText.isNotEmpty == true ? cue!.originalText : 'Original speech appears here'),
                        const SizedBox(height: 8),
                        Text(
                          cue == null
                              ? ''
                              : cue.status == 'TRANSLATION_FAILED'
                                  ? 'Translated audio is temporarily unavailable.'
                                  : cue.translatedText,
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                        const SizedBox(height: 4),
                        const Text('AI-generated voice', style: TextStyle(fontSize: 12)),
                      ],
                    ),
                  ),
                ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  IconButton(onPressed: () => ref.read(callRoomProvider.notifier).setMuted(!room.muted), icon: Icon(room.muted ? Icons.mic_off : Icons.mic)),
                  IconButton(onPressed: () => ref.read(callRoomProvider.notifier).setSpeaker(!room.speakerOn), icon: Icon(room.speakerOn ? Icons.volume_up : Icons.hearing)),
                  IconButton(
                    onPressed: user == null ? null : () => ref.read(callRoomProvider.notifier).repeat(user.preferredLanguage),
                    icon: const Icon(Icons.replay),
                  ),
                  IconButton.filled(
                    style: IconButton.styleFrom(backgroundColor: Colors.red),
                    onPressed: () async {
                      await ref.read(apiRepositoryProvider).endCall(widget.callId);
                      await ref.read(callRoomProvider.notifier).hangupLocal();
                      if (context.mounted) context.go('/home');
                    },
                    icon: const Icon(Icons.call_end),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
