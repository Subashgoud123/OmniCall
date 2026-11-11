import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:omnicall_flutter/core/api_client.dart';
import 'package:omnicall_flutter/core/theme.dart';
import 'package:omnicall_flutter/data/api_repository.dart';
import 'package:omnicall_flutter/data/models.dart';
import 'package:omnicall_flutter/features/session/session_controller.dart';

class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Spacer(),
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(color: AppTheme.navy, borderRadius: BorderRadius.circular(22)),
                child: const Icon(Icons.translate, color: Colors.white, size: 36),
              ),
              const SizedBox(height: 28),
              Text('OmniCall', style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              Text('Speak your language. Understand everyone.', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 16),
              const Text('A call between Hindi and Telugu is translated in both directions while you talk. OmniCall processes the audio to do that.'),
              const Spacer(),
              FilledButton(onPressed: () => context.go('/register'), child: const Text('Create account')),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: () => context.go('/login'), child: const Text('I already have an account')),
            ],
          ),
        ),
      ),
    );
  }
}

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _name = TextEditingController();
  final _phone = TextEditingController();
  String _language = 'hi-IN';
  bool _terms = false;
  bool _consent = false;
  bool _busy = false;
  List<LanguageOption> _languages = [];

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  Future<void> _load() async {
    try {
      final languages = await ref.read(apiRepositoryProvider).languages();
      if (mounted) setState(() => _languages = languages);
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final enabled = _languages.where((language) => language.enabled).toList();
    return Scaffold(
      appBar: AppBar(title: const Text('Create account')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          TextField(controller: _name, decoration: const InputDecoration(labelText: 'Display name')),
          const SizedBox(height: 12),
          TextField(controller: _phone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Phone number', hintText: '+91...')),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _language,
            decoration: const InputDecoration(labelText: 'Preferred language'),
            items: (enabled.isEmpty ? [LanguageOption(code: 'hi-IN', englishName: 'Hindi', nativeName: 'हिन्दी', enabled: true), LanguageOption(code: 'te-IN', englishName: 'Telugu', nativeName: 'తెలుగు', enabled: true)] : enabled)
                .map((language) => DropdownMenuItem(value: language.code, child: Text('${language.nativeName} · ${language.englishName}')))
                .toList(),
            onChanged: (value) => setState(() => _language = value ?? _language),
          ),
          CheckboxListTile(value: _terms, onChanged: (value) => setState(() => _terms = value ?? false), title: const Text('I accept the terms'), contentPadding: EdgeInsets.zero),
          CheckboxListTile(
            value: _consent,
            onChanged: (value) => setState(() => _consent = value ?? false),
            title: const Text('I agree that call audio is processed by OmniCall for translation. Calls are not end-to-end encrypted.'),
            contentPadding: EdgeInsets.zero,
          ),
          const SizedBox(height: 12),
          FilledButton(onPressed: _busy ? null : _submit, child: Text(_busy ? 'Sending code' : 'Send verification code')),
        ],
      ),
    );
  }

  Future<void> _submit() async {
    setState(() => _busy = true);
    try {
      await ref.read(apiRepositoryProvider).register(
            displayName: _name.text.trim(),
            phoneNumber: _phone.text.trim(),
            preferredLanguage: _language,
            acceptedTerms: _terms,
            translationConsent: _consent,
          );
      if (mounted) context.go('/verify?phone=${Uri.encodeComponent(_phone.text.trim())}&purpose=REGISTER');
    } catch (error) {
      _show(apiError(error).message);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _show(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }
}

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _phone = TextEditingController();
  bool _busy = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Sign in')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            TextField(controller: _phone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Phone number')),
            const SizedBox(height: 16),
            FilledButton(onPressed: _busy ? null : _submit, child: const Text('Send code')),
          ],
        ),
      ),
    );
  }

  Future<void> _submit() async {
    setState(() => _busy = true);
    try {
      await ref.read(apiRepositoryProvider).login(_phone.text.trim());
      if (!mounted) return;
      context.go('/verify?phone=${Uri.encodeComponent(_phone.text.trim())}&purpose=LOGIN');
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(apiError(error).message)));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }
}

class VerifyScreen extends ConsumerStatefulWidget {
  const VerifyScreen({super.key, required this.phone, required this.purpose});
  final String phone;
  final String purpose;

  @override
  ConsumerState<VerifyScreen> createState() => _VerifyScreenState();
}

class _VerifyScreenState extends ConsumerState<VerifyScreen> {
  final _code = TextEditingController();
  bool _busy = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Enter code')),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('We sent a code to ${widget.phone}.'),
            const SizedBox(height: 16),
            TextField(controller: _code, keyboardType: TextInputType.number, decoration: const InputDecoration(labelText: '6-digit code')),
            const SizedBox(height: 16),
            FilledButton(onPressed: _busy ? null : _submit, child: const Text('Verify')),
          ],
        ),
      ),
    );
  }

  Future<void> _submit() async {
    setState(() => _busy = true);
    try {
      await ref.read(sessionProvider.notifier).verify(widget.phone, _code.text.trim(), widget.purpose);
      if (!mounted) return;
      context.go('/home');
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(apiError(error).message)));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }
}
