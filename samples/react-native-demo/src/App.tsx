import React, { useState } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  Switch,
  TouchableOpacity,
  FlatList,
} from 'react-native';
import { SceneView, ARSceneView, type ModelNode } from 'react-native-sceneview';

// ---------------------------------------------------------------------------
// Model catalog — all assets live in assets/models/ and assets/environments/
// ---------------------------------------------------------------------------

interface ModelEntry {
  label: string;
  node: ModelNode;
}

const MODELS: ModelEntry[] = [
  {
    label: 'Robot',
    node: {
      src: 'models/robot.glb',
      position: [0, 0, -2],
      scale: [0.5, 0.5, 0.5],
      animation: 'Idle',
    },
  },
  {
    label: 'Damaged Helmet',
    node: {
      src: 'models/damaged_helmet.glb',
      position: [0, 0, -2],
      scale: [1, 1, 1],
    },
  },
  {
    label: 'Cyberpunk Car',
    node: {
      src: 'models/cyberpunk_car.glb',
      position: [0, -0.5, -3],
      scale: [0.5, 0.5, 0.5],
    },
  },
  {
    label: 'Fox',
    node: {
      src: 'models/fox.glb',
      position: [0, -0.5, -2],
      scale: [0.02, 0.02, 0.02],
      animation: 'Survey',
    },
  },
  {
    label: 'Butterfly',
    node: {
      src: 'models/animated_butterfly.glb',
      position: [0, 0, -1.5],
      scale: [0.3, 0.3, 0.3],
      animation: 'ArmatureAction',
    },
  },
];

const ENVIRONMENT = 'environments/studio_small.hdr';

export default function App() {
  const [arMode, setArMode] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);

  const currentModel = MODELS[selectedIndex];

  return (
    <SafeAreaView style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>SceneView</Text>
        <View style={styles.toggle}>
          <Text style={styles.toggleLabel}>3D</Text>
          <Switch
            value={arMode}
            onValueChange={setArMode}
            trackColor={{ false: '#555', true: '#4a90d9' }}
            thumbColor="#fff"
          />
          <Text style={styles.toggleLabel}>AR</Text>
        </View>
      </View>

      {/* Scene */}
      {arMode ? (
        <ARSceneView
          style={styles.scene}
          planeDetection
          modelNodes={[currentModel.node]}
          onTap={(e) => {
            console.log('AR tap:', e.nativeEvent);
          }}
          onPlaneDetected={(e) => {
            console.log('Plane detected:', e.nativeEvent);
          }}
        />
      ) : (
        <SceneView
          style={styles.scene}
          environment={ENVIRONMENT}
          modelNodes={[currentModel.node]}
          cameraOrbit
          onTap={(e) => {
            console.log('3D tap:', e.nativeEvent);
          }}
        />
      )}

      {/* Model picker */}
      <View style={styles.picker}>
        <FlatList
          data={MODELS}
          horizontal
          showsHorizontalScrollIndicator={false}
          keyExtractor={(_, i) => String(i)}
          contentContainerStyle={styles.pickerContent}
          renderItem={({ item, index }) => (
            <TouchableOpacity
              style={[
                styles.chip,
                index === selectedIndex && styles.chipSelected,
              ]}
              onPress={() => setSelectedIndex(index)}
              activeOpacity={0.7}
            >
              <Text
                style={[
                  styles.chipText,
                  index === selectedIndex && styles.chipTextSelected,
                ]}
              >
                {item.label}
              </Text>
            </TouchableOpacity>
          )}
        />
      </View>
    </SafeAreaView>
  );
}

// ---------------------------------------------------------------------------
// Styles
// ---------------------------------------------------------------------------

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1a1a2e',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  title: {
    color: '#fff',
    fontSize: 22,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  toggle: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  toggleLabel: {
    color: '#ccc',
    fontSize: 14,
    fontWeight: '500',
  },
  scene: {
    flex: 1,
  },
  picker: {
    paddingVertical: 12,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#333',
  },
  pickerContent: {
    paddingHorizontal: 12,
    gap: 8,
  },
  chip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#2a2a4e',
    borderWidth: 1,
    borderColor: '#444',
  },
  chipSelected: {
    backgroundColor: '#4a90d9',
    borderColor: '#4a90d9',
  },
  chipText: {
    color: '#aaa',
    fontSize: 14,
    fontWeight: '500',
  },
  chipTextSelected: {
    color: '#fff',
  },
});
