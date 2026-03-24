import React, { useState } from 'react';
import { SafeAreaView, StyleSheet, Text, View, Switch } from 'react-native';
import { SceneView, ARSceneView, type ModelNode } from 'react-native-sceneview';

const ROBOT_MODEL: ModelNode = {
  src: 'models/robot.glb',
  position: [0, 0, -2],
  scale: [0.5, 0.5, 0.5],
  animation: 'Idle',
};

export default function App() {
  const [arMode, setArMode] = useState(false);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>SceneView Example</Text>
        <View style={styles.toggle}>
          <Text>3D</Text>
          <Switch value={arMode} onValueChange={setArMode} />
          <Text>AR</Text>
        </View>
      </View>

      {arMode ? (
        <ARSceneView
          style={styles.scene}
          planeDetection
          modelNodes={[ROBOT_MODEL]}
          onTap={(e) => {
            console.log('Tapped:', e.nativeEvent);
          }}
          onPlaneDetected={(e) => {
            console.log('Plane detected:', e.nativeEvent);
          }}
        />
      ) : (
        <SceneView
          style={styles.scene}
          environment="environments/studio_small.hdr"
          modelNodes={[ROBOT_MODEL]}
          cameraOrbit
          onTap={(e) => {
            console.log('Tapped:', e.nativeEvent);
          }}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1a1a2e',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
  },
  title: {
    color: '#fff',
    fontSize: 20,
    fontWeight: '600',
  },
  toggle: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  scene: {
    flex: 1,
  },
});
