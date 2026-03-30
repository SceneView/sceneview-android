import 'package:flutter/material.dart';
import 'package:sceneview_flutter/sceneview_flutter.dart';

void main() {
  runApp(const SceneViewExampleApp());
}

/// Available 3D models for the demo.
const _models = [
  _ModelInfo(
    name: 'Damaged Helmet',
    path: 'models/damaged_helmet.glb',
    icon: Icons.sports_motorsports,
  ),
  _ModelInfo(
    name: 'Toy Car',
    path: 'models/toy_car.glb',
    icon: Icons.directions_car,
  ),
  _ModelInfo(
    name: 'Sheen Chair',
    path: 'models/sheen_chair.glb',
    icon: Icons.chair,
  ),
  _ModelInfo(
    name: 'Water Bottle',
    path: 'models/water_bottle.glb',
    icon: Icons.water_drop,
  ),
  _ModelInfo(
    name: 'Shelby Cobra',
    path: 'models/shelby_cobra.glb',
    icon: Icons.directions_car,
  ),
  _ModelInfo(
    name: 'Audi TT',
    path: 'models/audi_tt.glb',
    icon: Icons.directions_car_filled,
  ),
  _ModelInfo(
    name: 'Earthquake',
    path: 'models/earthquake_california.glb',
    icon: Icons.public,
  ),
  _ModelInfo(
    name: 'Lamborghini',
    path: 'models/lamborghini_countach.glb',
    icon: Icons.directions_car,
  ),
  _ModelInfo(
    name: 'Nike Jordan',
    path: 'models/nike_air_jordan.glb',
    icon: Icons.sports_basketball,
  ),
  _ModelInfo(
    name: 'Ferrari F40',
    path: 'models/ferrari_f40.glb',
    icon: Icons.directions_car,
  ),
  _ModelInfo(
    name: 'PS5 Controller',
    path: 'models/ps5_dualsense.glb',
    icon: Icons.gamepad,
  ),
  _ModelInfo(
    name: 'Cybertruck',
    path: 'models/tesla_cybertruck.glb',
    icon: Icons.electric_car,
  ),
  _ModelInfo(
    name: 'Nintendo Switch',
    path: 'models/nintendo_switch.glb',
    icon: Icons.videogame_asset,
  ),
  _ModelInfo(
    name: 'BMW M3 E30',
    path: 'models/bmw_m3_e30.glb',
    icon: Icons.directions_car_filled,
  ),
];

class _ModelInfo {
  final String name;
  final String path;
  final IconData icon;

  const _ModelInfo({
    required this.name,
    required this.path,
    required this.icon,
  });
}

class SceneViewExampleApp extends StatelessWidget {
  const SceneViewExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SceneView Flutter Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.dark(
          primary: const Color(0xFFA4C1FF),
          onPrimary: const Color(0xFF002F65),
          primaryContainer: const Color(0xFF00458F),
          onPrimaryContainer: const Color(0xFFD4E3FF),
          secondary: const Color(0xFFBDC7DC),
          onSecondary: const Color(0xFF273141),
          tertiary: const Color(0xFFD2A8FF),
          onTertiary: const Color(0xFF37009B),
          surface: const Color(0xFF111318),
          onSurface: const Color(0xFFE2E2E9),
          error: const Color(0xFFFFB4AB),
          onError: const Color(0xFF690005),
        ),
        useMaterial3: true,
      ),
      home: const ModelViewerPage(),
    );
  }
}

class ModelViewerPage extends StatefulWidget {
  const ModelViewerPage({super.key});

  @override
  State<ModelViewerPage> createState() => _ModelViewerPageState();
}

class _ModelViewerPageState extends State<ModelViewerPage> {
  final _controller = SceneViewController();
  int _selectedModelIndex = 0;
  bool _sceneReady = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SceneView Flutter Demo'),
        actions: [
          IconButton(
            onPressed: () => _controller.clearScene(),
            icon: const Icon(Icons.delete_outline),
            tooltip: 'Clear scene',
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: Stack(
              children: [
                SceneView(
                  controller: _controller,
                  onViewCreated: _onSceneCreated,
                ),
                if (!_sceneReady)
                  const Center(child: CircularProgressIndicator()),
              ],
            ),
          ),
          _buildModelSelector(),
          _buildActionBar(),
        ],
      ),
    );
  }

  Widget _buildModelSelector() {
    return SizedBox(
      height: 80,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 8),
        itemCount: _models.length,
        itemBuilder: (context, index) {
          final model = _models[index];
          final isSelected = index == _selectedModelIndex;
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 8),
            child: ChoiceChip(
              avatar: Icon(model.icon, size: 20),
              label: Text(model.name),
              selected: isSelected,
              onSelected: (_) {
                setState(() => _selectedModelIndex = index);
                _loadModel(index);
              },
            ),
          );
        },
      ),
    );
  }

  Widget _buildActionBar() {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          FilledButton.icon(
            onPressed: _sceneReady ? () => _loadModel(_selectedModelIndex) : null,
            icon: const Icon(Icons.view_in_ar),
            label: const Text('Load Model'),
          ),
          FilledButton.tonalIcon(
            onPressed: _sceneReady ? _addCube : null,
            icon: const Icon(Icons.square_outlined),
            label: const Text('Add Cube'),
          ),
        ],
      ),
    );
  }

  void _onSceneCreated() {
    // Set up default environment and lighting
    _controller.setEnvironment('environments/studio_small.hdr');
    _controller.addLight(const LightNode(
      type: 'directional',
      intensity: 100000,
    ));
    setState(() => _sceneReady = true);
    // Load the first model automatically
    _loadModel(0);
  }

  void _loadModel(int index) {
    _controller.clearScene();
    _controller.loadModel(ModelNode(
      modelPath: _models[index].path,
    ));
  }

  void _addCube() {
    _controller.addGeometry(const GeometryNode(
      type: 'cube',
      size: 0.5,
      color: 0xFF005BC1,
    ));
  }
}
