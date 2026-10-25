import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../controllers/library_controller.dart';
import '../models/song.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _search = TextEditingController();
  int _tab = 0;

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<LibraryController>();
    return Scaffold(
      appBar: AppBar(
        title: const Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('LIBRARY', style: TextStyle(fontSize: 12, letterSpacing: 2)),
          Text('cyb-mus', style: TextStyle(fontWeight: FontWeight.bold)),
        ]),
        actions: [
          IconButton(onPressed: controller.loadLocal, icon: const Icon(Icons.refresh), tooltip: 'Scan local music'),
          IconButton(
            onPressed: () => controller.setOnline(!controller.onlineMode),
            icon: Icon(controller.onlineMode ? Icons.cloud_off : Icons.cloud),
            tooltip: controller.onlineMode ? 'Switch to offline mode' : 'Switch to online mode',
          ),
        ],
      ),
      body: IndexedStack(index: _tab, children: [
        _library(context, controller),
        _player(context, controller),
        _settings(controller),
      ]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (value) => setState(() => _tab = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.library_music), label: 'Library'),
          NavigationDestination(icon: Icon(Icons.equalizer), label: 'Player'),
          NavigationDestination(icon: Icon(Icons.settings), label: 'Settings'),
        ],
      ),
    );
  }

  Widget _library(BuildContext context, LibraryController controller) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
      child: Column(children: [
        if (controller.onlineMode)
          Row(children: [
            Expanded(child: TextField(controller: _search, onSubmitted: controller.searchOnline, decoration: const InputDecoration(labelText: 'Song or artist'))),
            const SizedBox(width: 8),
            FilledButton(onPressed: () => controller.searchOnline(_search.text), child: const Text('Search')),
          ])
        else
          const Align(alignment: Alignment.centerLeft, child: Text('LOCAL MUSIC', style: TextStyle(letterSpacing: 1.5))),
        const SizedBox(height: 12),
        if (controller.loading) const LinearProgressIndicator(),
        if (controller.error.isNotEmpty) Padding(padding: const EdgeInsets.symmetric(vertical: 12), child: Text(controller.error, style: TextStyle(color: Theme.of(context).colorScheme.error))),
        Expanded(
          child: controller.songs.isEmpty && !controller.loading
              ? Center(child: Text(controller.onlineMode ? 'Search for music online.' : 'No local songs found. Grant permission and scan.'))
              : ListView.separated(
                  itemCount: controller.songs.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 8),
                  itemBuilder: (_, index) => _songTile(controller, controller.songs[index]),
                ),
        ),
      ]),
    );
  }

  Widget _songTile(LibraryController controller, Song song) {
    return Card(
      child: ListTile(
        onTap: () => controller.play(song),
        leading: CircleAvatar(child: Icon(song.isOnline ? Icons.cloud : Icons.music_note)),
        title: Text(song.title, maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Text('${song.artist}  •  ${song.durationLabel}'),
        trailing: const Icon(Icons.play_arrow),
      ),
    );
  }

  Widget _player(BuildContext context, LibraryController controller) {
    final song = controller.current;
    return Center(child: song == null ? const Text('Select a song from the library.') : Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      const Icon(Icons.album, size: 96),
      const SizedBox(height: 20),
      Text(song.title, style: Theme.of(context).textTheme.headlineSmall),
      Text(song.artist),
      const SizedBox(height: 24),
      StreamBuilder<bool>(stream: controller.service.player.playingStream, builder: (_, snapshot) => IconButton(iconSize: 64, onPressed: () => snapshot.data == true ? controller.service.player.pause() : controller.service.player.play(), icon: Icon(snapshot.data == true ? Icons.pause_circle : Icons.play_circle))),
    ]));
  }

  Widget _settings(LibraryController controller) => const Center(child: Text('Settings will be migrated next.'));

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }
}
