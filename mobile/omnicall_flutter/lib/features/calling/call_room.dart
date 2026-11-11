import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:livekit_client/livekit_client.dart';
import 'package:omnicall_flutter/data/models.dart';
import 'package:permission_handler/permission_handler.dart';

class CallRoomState {
  const CallRoomState({
    this.connected = false,
    this.muted = false,
    this.speakerOn = true,
    this.status = 'Connecting',
    this.subtitle,
    this.failure,
  });

  final bool connected;
  final bool muted;
  final bool speakerOn;
  final String status;
  final SubtitleCue? subtitle;
  final String? failure;

  CallRoomState copyWith({bool? connected, bool? muted, bool? speakerOn, String? status, SubtitleCue? subtitle, String? failure}) {
    return CallRoomState(
      connected: connected ?? this.connected,
      muted: muted ?? this.muted,
      speakerOn: speakerOn ?? this.speakerOn,
      status: status ?? this.status,
      subtitle: subtitle ?? this.subtitle,
      failure: failure,
    );
  }
}

class CallRoomController extends Notifier<CallRoomState> {
  Room? _room;
  EventsListener<RoomEvent>? _listener;
  static const _channel = MethodChannel('com.omnicall/call_service');

  @override
  CallRoomState build() {
    ref.onDispose(hangupLocal);
    return const CallRoomState();
  }

  Future<void> connect({
    required String url,
    required String token,
    required String myLanguage,
    required bool subtitles,
    required bool originalAudio,
    required String peerName,
  }) async {
    final mic = await Permission.microphone.request();
    if (!mic.isGranted) {
      state = state.copyWith(failure: 'Microphone permission is required for a translated call.', status: 'Microphone blocked');
      return;
    }
    try {
      await _channel.invokeMethod('start', {'text': 'Translated call with $peerName'});
    } catch (_) {}
    final room = Room(
      roomOptions: const RoomOptions(adaptiveStream: true, dynacast: true, defaultAudioPublishOptions: AudioPublishOptions(name: 'microphone')),
    );
    _room = room;
    _listener = room.createListener()
      ..on<TrackPublishedEvent>((event) {
        _route(event.publication, myLanguage, originalAudio);
      })
      ..on<DataReceivedEvent>((event) {
        if (!subtitles || event.topic != 'omnicall.subtitle') return;
        final json = jsonDecode(utf8.decode(event.data)) as Map<String, dynamic>;
        final cue = SubtitleCue.fromJson(json);
        state = state.copyWith(subtitle: cue, status: _statusLabel(cue.status));
      })
      ..on<RoomDisconnectedEvent>((_) {
        state = state.copyWith(connected: false, status: 'Disconnected');
      });
    await room.prepareConnection(url, token);
    await room.connect(url, token);
    await room.localParticipant?.setMicrophoneEnabled(true);
    try {
      await _channel.invokeMethod('setSpeaker', {'on': true});
    } catch (_) {}
    state = state.copyWith(connected: true, status: 'Listening', failure: null);
  }

  void _route(RemoteTrackPublication publication, String myLanguage, bool originalAudio) {
    final name = publication.name;
    final translated = name.startsWith('tx:');
    final hear = translated ? name.substring(3) == myLanguage : originalAudio;
    if (hear) {
      publication.subscribe();
    } else {
      publication.unsubscribe();
    }
  }

  Future<void> setMuted(bool muted) async {
    await _room?.localParticipant?.setMicrophoneEnabled(!muted);
    state = state.copyWith(muted: muted, status: muted ? 'Muted' : 'Listening');
  }

  Future<void> setSpeaker(bool on) async {
    try {
      await _channel.invokeMethod('setSpeaker', {'on': on});
    } catch (_) {}
    state = state.copyWith(speakerOn: on);
  }

  Future<void> repeat(String targetLanguage) async {
    final payload = utf8.encode(jsonEncode({'action': 'repeat', 'targetLanguage': targetLanguage}));
    await _room?.localParticipant?.publishData(payload, reliable: true, topic: 'omnicall.control');
  }

  Future<void> hangupLocal() async {
    await _listener?.dispose();
    await _room?.disconnect();
    _room = null;
    try {
      await _channel.invokeMethod('stop');
    } catch (_) {}
  }

  String _statusLabel(String status) {
    return switch (status) {
      'TRANSLATED' => 'Speaking translation',
      'TRANSLATION_FAILED' => 'Translation unavailable',
      'NO_SPEECH' => 'Listening',
      _ => 'Translating',
    };
  }
}

final callRoomProvider = NotifierProvider<CallRoomController, CallRoomState>(CallRoomController.new);
