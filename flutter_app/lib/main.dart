import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'controllers/library_controller.dart';
import 'screens/home_screen.dart';
import 'services/music_service.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(
    ChangeNotifierProvider(
      create: (_) => LibraryController(MusicService())..loadLocal(),
      child: const CybMusApp(),
    ),
  );
}

class CybMusApp extends StatelessWidget {
  const CybMusApp({super.key});

  @override
  Widget build(BuildContext context) {
    const background = Color(0xff090b12);
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'cyb-mus',
      theme: ThemeData.dark(useMaterial3: true).copyWith(
        scaffoldBackgroundColor: background,
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff65fbd2), brightness: Brightness.dark),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: const Color(0xff141823),
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
        ),
      ),
      home: const HomeScreen(),
    );
  }
}
