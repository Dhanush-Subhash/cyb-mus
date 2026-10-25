class Song {
  const Song({
    required this.id,
    required this.title,
    required this.artist,
    required this.duration,
    required this.uri,
    this.artwork,
    this.fileType = 'audio',
    this.isOnline = false,
  });

  final String id;
  final String title;
  final String artist;
  final Duration duration;
  final String uri;
  final String? artwork;
  final String fileType;
  final bool isOnline;

  String get durationLabel {
    final seconds = duration.inSeconds;
    return '${seconds ~/ 60}:${(seconds % 60).toString().padLeft(2, '0')}';
  }
}
