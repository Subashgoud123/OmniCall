import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:omnicall_flutter/core/api_client.dart';
import 'package:omnicall_flutter/core/format.dart';
import 'package:omnicall_flutter/data/api_repository.dart';
import 'package:omnicall_flutter/features/session/session_controller.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(sessionProvider).user;
    if (user == null) return const Scaffold(body: Center(child: CircularProgressIndicator()));
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        children: [
          ListTile(title: const Text('Preferred language'), subtitle: Text(languageLabel(user.preferredLanguage)), onTap: () => _language(context, ref)),
          ListTile(title: const Text('Voice'), subtitle: Text('${user.voiceGender} · ${user.preferredVoice ?? 'Default'}'), onTap: () => _voice(context, ref)),
          ListTile(
            title: const Text('Translation speed'),
            subtitle: Text(user.translationSpeed.toStringAsFixed(2)),
            onTap: () => _speed(context, ref),
          ),
          SwitchListTile(
            title: const Text('Subtitles'),
            value: user.subtitleEnabled,
            onChanged: (value) => _save(ref, {'subtitleEnabled': value}),
          ),
          SwitchListTile(
            title: const Text('Also hear original audio'),
            subtitle: const Text('Off by default, so you hear the translation rather than both voices.'),
            value: user.originalAudioEnabled,
            onChanged: (value) => _save(ref, {'originalAudioEnabled': value}),
          ),
          SwitchListTile(
            title: const Text('Save translation history'),
            subtitle: const Text('Off by default. Raw audio is never stored.'),
            value: user.translationHistoryEnabled,
            onChanged: (value) => _save(ref, {'translationHistoryEnabled': value}),
          ),
          SwitchListTile(
            title: const Text('Notifications'),
            value: user.notificationsEnabled,
            onChanged: (value) => _save(ref, {'notificationsEnabled': value}),
          ),
          ListTile(title: const Text('Call history'), onTap: () => context.go('/history')),
          ListTile(title: const Text('Privacy'), onTap: () => context.go('/privacy')),
          ListTile(
            title: const Text('Log out'),
            onTap: () async {
              await ref.read(sessionProvider.notifier).logout();
              if (context.mounted) context.go('/welcome');
            },
          ),
        ],
      ),
    );
  }

  Future<void> _save(WidgetRef ref, Map<String, dynamic> body) async {
    final profile = await ref.read(apiRepositoryProvider).updatePreferences(body);
    ref.read(sessionProvider.notifier).updateUser(profile);
  }

  Future<void> _language(BuildContext context, WidgetRef ref) async {
    final languages = await ref.read(apiRepositoryProvider).languages();
    if (!context.mounted) return;
    final code = await showModalBottomSheet<String>(
      context: context,
      builder: (context) => ListView(
        children: languages
            .map((language) => ListTile(
                  title: Text(language.nativeName),
                  subtitle: Text(language.enabled ? language.englishName : '${language.englishName} · coming soon'),
                  enabled: language.enabled,
                  onTap: () => Navigator.pop(context, language.code),
                ))
            .toList(),
      ),
    );
    if (code != null) await _save(ref, {'preferredLanguage': code});
  }

  Future<void> _voice(BuildContext context, WidgetRef ref) async {
    final gender = await showModalBottomSheet<String>(
      context: context,
      builder: (context) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          ListTile(title: const Text('Female'), onTap: () => Navigator.pop(context, 'FEMALE')),
          ListTile(title: const Text('Male'), onTap: () => Navigator.pop(context, 'MALE')),
          ListTile(title: const Text('Neutral'), onTap: () => Navigator.pop(context, 'NEUTRAL')),
        ],
      ),
    );
    if (gender != null) await _save(ref, {'voiceGender': gender});
  }

  Future<void> _speed(BuildContext context, WidgetRef ref) async {
    final speed = await showModalBottomSheet<double>(
      context: context,
      builder: (context) => Column(
        mainAxisSize: MainAxisSize.min,
        children: [0.85, 1.0, 1.15]
            .map((value) => ListTile(title: Text(value.toStringAsFixed(2)), onTap: () => Navigator.pop(context, value)))
            .toList(),
      ),
    );
    if (speed != null) await _save(ref, {'translationSpeed': speed});
  }
}

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(sessionProvider).user;
    return Scaffold(
      appBar: AppBar(title: const Text('Profile')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(user?.displayName ?? '', style: Theme.of(context).textTheme.headlineSmall),
            Text(user?.phoneNumber ?? ''),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: () async {
                final image = await ImagePicker().pickImage(source: ImageSource.gallery, maxWidth: 1024);
                if (image == null) return;
                try {
                  await ref.read(apiRepositoryProvider).uploadAvatar(image.path);
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Profile image saved')));
                  }
                } catch (error) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(apiError(error).message)));
                  }
                }
              },
              child: const Text('Choose profile image'),
            ),
          ],
        ),
      ),
    );
  }
}

class PrivacyScreen extends ConsumerWidget {
  const PrivacyScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('Privacy')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('OmniCall translates a live call by sending each speaker’s audio to the translation service. The service recognizes speech, translates the sentence, and generates a new voice.'),
          const SizedBox(height: 12),
          const Text('Because the service has to hear the audio, these calls are not end-to-end encrypted. Media is still encrypted on the network between your phone, the call server, and the translator.'),
          const SizedBox(height: 12),
          const Text('Raw call audio is not stored. Translation text is stored only when you turn on translation history, and it is deleted on a retention schedule or when you delete your account.'),
          const SizedBox(height: 12),
          const Text('Deleting your account removes your profile, contacts, device notifications, and saved translations. Call records for the other person remain, with your name replaced.'),
          const SizedBox(height: 24),
          OutlinedButton(
            onPressed: () async {
              try {
                await ref.read(apiRepositoryProvider).deleteAccount();
                await ref.read(sessionProvider.notifier).logout();
                if (context.mounted) context.go('/welcome');
              } catch (error) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(apiError(error).message)));
                }
              }
            },
            child: const Text('Delete account'),
          ),
        ],
      ),
    );
  }
}
