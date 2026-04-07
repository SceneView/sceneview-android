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
  {
    label: 'Shelby Cobra',
    node: {
      src: 'models/shelby_cobra.glb',
      position: [0, -0.5, -3],
      scale: [0.5, 0.5, 0.5],
    },
  },
  {
    label: 'Audi TT',
    node: {
      src: 'models/audi_tt.glb',
      position: [0, -0.5, -2.5],
      scale: [0.6, 0.6, 0.6],
    },
  },
  {
    label: 'Earthquake',
    node: {
      src: 'models/earthquake_california.glb',
      position: [0, -1, -4],
      scale: [0.3, 0.3, 0.3],
    },
  },
  {
    label: 'Lamborghini',
    node: {
      src: 'models/lamborghini_countach.glb',
      position: [0, -0.5, -3],
      scale: [0.4, 0.4, 0.4],
    },
  },
  {
    label: 'Nike Jordan',
    node: {
      src: 'models/nike_air_jordan.glb',
      position: [0, 0, -1.5],
      scale: [0.8, 0.8, 0.8],
    },
  },
  {
    label: 'Ferrari F40',
    node: {
      src: 'models/ferrari_f40.glb',
      position: [0, -0.5, -3],
      scale: [0.5, 0.5, 0.5],
    },
  },
  {
    label: 'PS5 Controller',
    node: {
      src: 'models/ps5_dualsense.glb',
      position: [0, 0, -2],
      scale: [0.7, 0.7, 0.7],
    },
  },
  {
    label: 'Cybertruck',
    node: {
      src: 'models/tesla_cybertruck.glb',
      position: [0, -0.5, -3],
      scale: [0.5, 0.5, 0.5],
    },
  },
  {
    label: 'Switch',
    node: {
      src: 'models/nintendo_switch.glb',
      position: [0, 0, -1.5],
      scale: [0.8, 0.8, 0.8],
    },
  },
  {
    label: 'BMW M3 E30',
    node: {
      src: 'models/bmw_m3_e30.glb',
      position: [0, -0.5, -3],
      scale: [0.5, 0.5, 0.5],
    },
  },
  {
    label: 'Koi Fish',
    node: {
      src: 'models/koi_fish.glb',
      position: [0, 0, -2.5],
      scale: [0.5, 0.5, 0.5],
    },
  },
  {
    label: 'Toon Cat',
    node: {
      src: 'models/toon_cat.glb',
      position: [0, 0, -2],
      scale: [0.8, 0.8, 0.8],
    },
  },
  {
    label: 'Elephant',
    node: {
      src: 'models/animated_elephant.glb',
      position: [0, -0.5, -3],
      scale: [0.4, 0.4, 0.4],
    },
  },
  {
    label: 'Trumpet',
    node: {
      src: 'models/trumpet.glb',
      position: [0, 0, -2],
      scale: [0.7, 0.7, 0.7],
    },
  },
  {
    label: 'Night City',
    node: {
      src: 'models/night_city.glb',
      position: [0, -0.5, -4],
      scale: [0.3, 0.3, 0.3],
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
            trackColor={{ false: '#555', true: '#005bc1' }}
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
    backgroundColor: '#111318',
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
    borderTopColor: '#434750',
  },
  pickerContent: {
    paddingHorizontal: 12,
    gap: 8,
  },
  chip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: '#1d2027',
    borderWidth: 1,
    borderColor: '#434750',
  },
  chipSelected: {
    backgroundColor: '#005bc1',
    borderColor: '#005bc1',
  },
  chipText: {
    color: '#c3c6cf',
    fontSize: 14,
    fontWeight: '500',
  },
  chipTextSelected: {
    color: '#fff',
  },
});
