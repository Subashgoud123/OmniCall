import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:omnicall_flutter/core/api_client.dart';
import 'package:omnicall_flutter/core/format.dart';
import 'package:omnicall_flutter/data/api_repository.dart';
import 'package:omnicall_flutter/data/models.dart';
import 'package:omnicall_flutter/features/session/session_controller.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(sessionProvider).user;
    final incoming = ref.watch(sessionProvider).incoming;
    return Scaffold(
      appBar: AppBar(
        title: const Text('OmniCall'),
        actions: [
          IconButton(onPressed: () => context.go('/settings'), icon: const Icon(Icons.settings_outlined)),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          if (incoming != null)
            Card(
              child: ListTile(
                title: Text('${incoming.callerName} is calling'),
                subtitle: Text(languageLabel(incoming.callerLanguage)),
                trailing: FilledButton(onPressed: () => context.go('/call/incoming/${incoming.id}'), child: const Text('Open')),
              ),
            ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(user?.displayName ?? ''),
            subtitle: Text(user == null ? '' : 'You speak ${languageLabel(user.preferredLanguage)}'),
            trailing: IconButton(onPressed: () => context.go('/profile'), icon: const Icon(Icons.person_outline)),
          ),
          const _NetworkStatus(),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: () => context.go('/search'), icon: const Icon(Icons.search), label: const Text('Find someone to call')),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: () => context.go('/contacts'), child: const Text('Contacts')),
          const SizedBox(height: 24),
          Text('Recent calls', style: Theme.of(context).textTheme.titleMedium),
          const _RecentCalls(),
        ],
      ),
    );
  }
}

class _NetworkStatus extends StatefulWidget {
  const _NetworkStatus();

  @override
  State<_NetworkStatus> createState() => _NetworkStatusState();
}

class _NetworkStatusState extends State<_NetworkStatus> {
  String _status = 'Checking network';

  @override
  void initState() {
    super.initState();
    Connectivity().checkConnectivity().then((results) {
      if (!mounted) return;
      final online = results.any((result) => result != ConnectivityResult.none);
      setState(() => _status = online ? 'Online' : 'Offline');
    });
  }

  @override
  Widget build(BuildContext context) => Text(_status);
}

class _RecentCalls extends ConsumerWidget {
  const _RecentCalls();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return FutureBuilder<List<CallSession>>(
      future: ref.read(apiRepositoryProvider).history(),
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) return const Padding(padding: EdgeInsets.all(16), child: CircularProgressIndicator());
        if (snapshot.hasError) return Text(apiError(snapshot.error!).message);
        final calls = snapshot.data ?? [];
        if (calls.isEmpty) return const Padding(padding: EdgeInsets.symmetric(vertical: 16), child: Text('No calls yet.'));
        final userId = ref.watch(sessionProvider).user?.id;
        return Column(
          children: calls.take(8).map((call) {
            return ListTile(
              contentPadding: EdgeInsets.zero,
              title: Text(userId == null ? call.receiverName : call.otherName(userId)),
              subtitle: Text('${call.status} · ${languageLabel(call.callerLanguage)} → ${languageLabel(call.receiverLanguage)}'),
              trailing: Text(formatDuration(call.durationSeconds)),
            );
          }).toList(),
        );
      },
    );
  }
}

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});

  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen> {
  final _query = TextEditingController();
  List<PublicUser> _results = [];
  String? _error;
  bool _busy = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Find a person')),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          TextField(controller: _query, decoration: const InputDecoration(labelText: 'Name or +phone number')),
          const SizedBox(height: 12),
          FilledButton(onPressed: _busy ? null : _search, child: const Text('Search')),
          if (_error != null) Padding(padding: const EdgeInsets.only(top: 12), child: Text(_error!)),
          ..._results.map((person) => ListTile(
                title: Text(person.displayName),
                subtitle: Text(languageLabel(person.preferredLanguage)),
                trailing: Wrap(
                  spacing: 8,
                  children: [
                    IconButton(onPressed: () => _invite(person), icon: const Icon(Icons.person_add_alt)),
                    IconButton(onPressed: () => _call(person), icon: const Icon(Icons.call)),
                  ],
                ),
              )),
        ],
      ),
    );
  }

  Future<void> _search() async {
    setState(() { _busy = true; _error = null; });
    try {
      final results = await ref.read(apiRepositoryProvider).search(_query.text.trim());
      setState(() => _results = results);
    } catch (error) {
      setState(() => _error = apiError(error).message);
    } finally {
      setState(() => _busy = false);
    }
  }

  Future<void> _invite(PublicUser person) async {
    try {
      await ref.read(apiRepositoryProvider).invite(person.id);
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Invitation sent')));
    } catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(apiError(error).message)));
    }
  }

  Future<void> _call(PublicUser person) async {
    try {
      final call = await ref.read(apiRepositoryProvider).startCall(person.id);
      if (mounted) context.go('/call/outgoing/${call.id}');
    } catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(apiError(error).message)));
    }
  }
}

class ContactsScreen extends ConsumerWidget {
  const ContactsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(title: const Text('Contacts')),
      body: FutureBuilder(
        future: ref.read(apiRepositoryProvider).contacts(),
        builder: (context, snapshot) {
          if (!snapshot.hasData) return const Center(child: CircularProgressIndicator());
          final contacts = snapshot.data!;
          if (contacts.isEmpty) return const Center(child: Text('No contacts yet.'));
          return ListView(
            children: contacts.map((contact) {
              return ListTile(
                title: Text(contact.user.displayName),
                subtitle: Text('${contact.status} · ${languageLabel(contact.user.preferredLanguage)}'),
                trailing: contact.status == 'ACCEPTED'
                    ? IconButton(
                        icon: const Icon(Icons.call),
                        onPressed: () async {
                          final call = await ref.read(apiRepositoryProvider).startCall(contact.user.id);
                          if (context.mounted) context.go('/call/outgoing/${call.id}');
                        },
                      )
                    : TextButton(
                        onPressed: () => ref.read(apiRepositoryProvider).acceptContact(contact.user.id),
                        child: const Text('Accept'),
                      ),
              );
            }).toList(),
          );
        },
      ),
    );
  }
}

class HistoryScreen extends ConsumerWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final userId = ref.watch(sessionProvider).user?.id;
    return Scaffold(
      appBar: AppBar(title: const Text('Call history')),
      body: FutureBuilder(
        future: ref.read(apiRepositoryProvider).history(),
        builder: (context, snapshot) {
          if (!snapshot.hasData) return const Center(child: CircularProgressIndicator());
          return ListView(
            children: snapshot.data!.map((call) {
              return ListTile(
                title: Text(userId == null ? call.receiverName : call.otherName(userId)),
                subtitle: Text(call.status),
                trailing: Text(formatDuration(call.durationSeconds)),
              );
            }).toList(),
          );
        },
      ),
    );
  }
}
