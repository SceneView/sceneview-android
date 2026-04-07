import 'package:flutter/material.dart';

import 'pages/viewer_page.dart';
import 'pages/ar_page.dart';
import 'pages/features_page.dart';
import 'pages/about_page.dart';

/// SceneView Flutter Demo — showcases all Flutter bridge capabilities.
///
/// Architecture:
/// ```
/// Flutter (Dart)
///   +-- PlatformView --> Android: SceneView (Filament)
///   +-- PlatformView --> iOS: SceneViewSwift (RealityKit)
/// ```
class SceneViewDemoApp extends StatelessWidget {
  const SceneViewDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SceneView Flutter',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorSchemeSeed: const Color(0xFF005BC1),
        useMaterial3: true,
        brightness: Brightness.light,
      ),
      darkTheme: ThemeData(
        colorSchemeSeed: const Color(0xFF005BC1),
        useMaterial3: true,
        brightness: Brightness.dark,
      ),
      themeMode: ThemeMode.system,
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _currentIndex = 0;

  static const _pages = <Widget>[
    ViewerPage(),
    ARPage(),
    FeaturesPage(),
    AboutPage(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _currentIndex,
        children: _pages,
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (i) => setState(() => _currentIndex = i),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.view_in_ar_outlined),
            selectedIcon: Icon(Icons.view_in_ar),
            label: '3D Viewer',
          ),
          NavigationDestination(
            icon: Icon(Icons.camera_outlined),
            selectedIcon: Icon(Icons.camera),
            label: 'AR',
          ),
          NavigationDestination(
            icon: Icon(Icons.auto_awesome_outlined),
            selectedIcon: Icon(Icons.auto_awesome),
            label: 'Features',
          ),
          NavigationDestination(
            icon: Icon(Icons.info_outlined),
            selectedIcon: Icon(Icons.info),
            label: 'About',
          ),
        ],
      ),
    );
  }
}
