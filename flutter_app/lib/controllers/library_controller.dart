import 'package:flutter/foundation.dart';

import '../models/song.dart';
import '../services/music_service.dart';

class LibraryController extends ChangeNotifier {
  LibraryController(this.service);

  final MusicService service;
  List<Song> songs = const [];
  Song? current;
  bool onlineMode = false;
  bool loading = false;
  String error = '';

  Future<void> loadLocal() async {
    loading = true;
    error = '';
    notifyListeners();
    try {
      songs = await service.scanLocalSongs();
    } catch (e) {
      error = 'Could not scan music: $e';
    } finally {
      loading = false;
      notifyListeners();
    }
  }

  Future<void> setOnline(bool value) async {
    onlineMode = value;
    error = '';
    if (!value) {
      await loadLocal();
    } else {
      songs = const [];
      notifyListeners();
    }
  }

  Future<void> searchOnline(String query) async {
    if (query.trim().isEmpty) return;
    loading = true;
    error = '';
    notifyListeners();
    try {
      songs = await service.searchOnline(query);
    } catch (e) {
      songs = const [];
      error = 'Online search failed: $e';
    } finally {
      loading = false;
      notifyListeners();
    }
  }

  Future<void> play(Song song) async {
    current = song;
    notifyListeners();
    try {
      await service.play(song);
    } catch (e) {
      error = 'Playback failed: $e';
      notifyListeners();
    }
  }

  @override
  void dispose() {
    service.dispose();
    super.dispose();
  }
}
