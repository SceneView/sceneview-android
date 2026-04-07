import 'package:flutter/material.dart';

/// About tab — version info, architecture diagram, and supported features.
class AboutPage extends StatelessWidget {
  const AboutPage({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('About'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Logo / title
          Card(
            color: theme.colorScheme.primaryContainer,
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  Icon(
                    Icons.view_in_ar,
                    size: 64,
                    color: theme.colorScheme.onPrimaryContainer,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'SceneView Flutter',
                    style: theme.textTheme.headlineMedium?.copyWith(
                      color: theme.colorScheme.onPrimaryContainer,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'v3.6.1',
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: theme.colorScheme.onPrimaryContainer.withValues(alpha: 0.7),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '3D and AR for Flutter using native renderers',
                    style: theme.textTheme.bodyLarge?.copyWith(
                      color: theme.colorScheme.onPrimaryContainer,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Architecture
          Text('Architecture', style: theme.textTheme.titleLarge),
          const SizedBox(height: 8),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _ArchRow(
                    label: 'Flutter (Dart)',
                    detail: 'SceneView / ARSceneView widgets',
                    icon: Icons.flutter_dash,
                    theme: theme,
                  ),
                  const _ArchArrow(),
                  _ArchRow(
                    label: 'PlatformView Bridge',
                    detail: 'MethodChannel for commands and callbacks',
                    icon: Icons.swap_vert,
                    theme: theme,
                  ),
                  const _ArchArrow(),
                  Row(
                    children: [
                      Expanded(
                        child: _ArchRow(
                          label: 'Android',
                          detail: 'Filament (Compose)',
                          icon: Icons.android,
                          theme: theme,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: _ArchRow(
                          label: 'iOS',
                          detail: 'RealityKit (SwiftUI)',
                          icon: Icons.apple,
                          theme: theme,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Supported features
          Text('Supported Features', style: theme.textTheme.titleLarge),
          const SizedBox(height: 8),
          Card(
            child: Column(
              children: [
                _FeatureRow('SceneView (3D)', true, theme),
                _FeatureRow('ARSceneView (AR)', true, theme),
                _FeatureRow('Model loading (GLB/glTF)', true, theme),
                _FeatureRow('Model position, rotation, scale', true, theme),
                _FeatureRow('onTap callback', true, theme),
                _FeatureRow('onPlaneDetected callback', true, theme),
                _FeatureRow('Environment HDR', true, theme),
                _FeatureRow('Camera gestures (pan, pinch)', true, theme),
                _FeatureRow('Geometry nodes (cube, sphere, etc.)', false, theme),
                _FeatureRow('Light configuration', false, theme),
                _FeatureRow('Animation control', false, theme),
                _FeatureRow('Texture swapping', false, theme),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Links
          Text('Links', style: theme.textTheme.titleLarge),
          const SizedBox(height: 8),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.code),
                  title: const Text('GitHub Repository'),
                  subtitle: const Text('github.com/sceneview/sceneview'),
                ),
                ListTile(
                  leading: const Icon(Icons.web),
                  title: const Text('Website'),
                  subtitle: const Text('sceneview.github.io'),
                ),
                ListTile(
                  leading: const Icon(Icons.article),
                  title: const Text('pub.dev Package'),
                  subtitle: const Text('pub.dev/packages/sceneview'),
                ),
                ListTile(
                  leading: const Icon(Icons.description),
                  title: const Text('API Documentation'),
                  subtitle: const Text('sceneview.github.io/docs'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Tech stack
          Text('Tech Stack', style: theme.textTheme.titleLarge),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: const [
              Chip(avatar: Icon(Icons.flutter_dash, size: 18), label: Text('Flutter 3.10+')),
              Chip(avatar: Icon(Icons.android, size: 18), label: Text('Filament')),
              Chip(avatar: Icon(Icons.apple, size: 18), label: Text('RealityKit')),
              Chip(avatar: Icon(Icons.view_in_ar, size: 18), label: Text('ARCore')),
              Chip(avatar: Icon(Icons.camera, size: 18), label: Text('ARKit')),
              Chip(avatar: Icon(Icons.code, size: 18), label: Text('Jetpack Compose')),
              Chip(avatar: Icon(Icons.phone_iphone, size: 18), label: Text('SwiftUI')),
              Chip(avatar: Icon(Icons.design_services, size: 18), label: Text('Material 3')),
            ],
          ),
          const SizedBox(height: 32),

          // Footer
          Center(
            child: Text(
              'Made with SceneView SDK v3.6.1',
              style: theme.textTheme.bodySmall,
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }
}

class _ArchRow extends StatelessWidget {
  final String label;
  final String detail;
  final IconData icon;
  final ThemeData theme;

  const _ArchRow({
    required this.label,
    required this.detail,
    required this.icon,
    required this.theme,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Icon(icon, size: 24, color: theme.colorScheme.primary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: theme.textTheme.titleSmall),
                Text(detail, style: theme.textTheme.bodySmall),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ArchArrow extends StatelessWidget {
  const _ArchArrow();

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.symmetric(vertical: 4),
      child: Center(
        child: Icon(Icons.arrow_downward, size: 20, color: Colors.grey),
      ),
    );
  }
}

Widget _FeatureRow(String name, bool implemented, ThemeData theme) {
  return ListTile(
    dense: true,
    leading: Icon(
      implemented ? Icons.check_circle : Icons.radio_button_unchecked,
      color: implemented ? Colors.green : Colors.grey,
      size: 20,
    ),
    title: Text(name),
    trailing: Text(
      implemented ? 'Ready' : 'Planned',
      style: theme.textTheme.bodySmall?.copyWith(
        color: implemented ? Colors.green : Colors.grey,
      ),
    ),
  );
}
