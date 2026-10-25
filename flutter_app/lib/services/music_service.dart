import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:just_audio/just_audio.dart';
import 'package:on_audio_query/on_audio_query.dart';
import 'package:permission_handler/permission_handler.dart';

import '../models/song.dart';

class MusicService {
  final AudioPlayer player = AudioPlayer();
  final OnAudioQuery _audioQuery = OnAudioQuery();

  Future<List<Song>> scanLocalSongs() async {
    if (!await _requestAudioPermission()) return const [];
    final rows = await _audioQuery.querySongs(
      sortType: SongSortType.TITLE,
      orderType: OrderType.ASC_OR_SMALLER,
      uriType: UriType.EXTERNAL,
      ignoreCase: true,
    );
    return rows
        .where((row) => row.uri != null && row.title.trim().isNotEmpty)
        .map((row) => Song(
              id: 'local:${row.id}',
              title: row.title,
              artist: row.artist ?? 'Unknown artist',
              duration: Duration(milliseconds: row.duration ?? 0),
              uri: row.uri!,
              fileType: _extension(row.uri!),
            ))
        .toList();
  }

  Future<List<Song>> searchOnline(String query) async {
    final uri = Uri.https('saavn.dev', '/api/search/songs', {
      'query': query.trim(),
      'limit': '25',
    });
    final response = await http.get(uri).timeout(const Duration(seconds: 20));
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw Exception('Online service returned ${response.statusCode}');
    }
    final root = jsonDecode(response.body) as Map<String, dynamic>;
    final results = ((root['data'] as Map?)?['results'] as List?) ?? const [];
    return results.map((raw) {
      final item = raw as Map<String, dynamic>;
      final downloads = (item['downloadUrl'] as List?) ?? const [];
      final selected = _bestUrl(downloads);
      if (selected == null) return null;
      final images = (item['image'] as List?) ?? const [];
      final image = images.isNotEmpty ? (images.last as Map?)?['url'] as String? : null;
      return Song(
        id: 'online:${item['id'] ?? item['name']}',
        title: item['name'] as String? ?? 'Unknown title',
        artist: item['primaryArtists'] as String? ?? 'Unknown artist',
        duration: Duration(seconds: int.tryParse('${item['duration'] ?? 0}') ?? 0),
        uri: selected,
        artwork: image,
        fileType: 'online',
        isOnline: true,
      );
    }).whereType<Song>().toList();
  }

  Future<void> play(Song song) async {
    await player.setAudioSource(AudioSource.uri(Uri.parse(song.uri)));
    await player.play();
  }

  Future<bool> _requestAudioPermission() async {
    final permission = await Permission.audio.request();
    if (permission.isGranted) return true;
    final legacy = await Permission.storage.request();
    return legacy.isGranted;
  }

  String _extension(String uri) {
    final path = Uri.parse(uri).path;
    final extension = path.contains('.') ? path.split('.').last : 'audio';
    return extension.toLowerCase();
  }

  String? _bestUrl(List<dynamic> downloads) {
    for (final quality in ['320kbps', '160kbps', '96kbps', '48kbps']) {
      for (final raw in downloads) {
        final item = raw as Map?;
        if (item?['quality'] == quality && item?['url'] is String) return item!['url'] as String;
      }
    }
    for (final raw in downloads) {
      final url = (raw as Map?)?['url'];
      if (url is String && url.isNotEmpty) return url;
    }
    return null;
  }

  void dispose() => player.dispose();
}
