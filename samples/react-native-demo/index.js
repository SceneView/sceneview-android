/**
 * SceneView React Native demo entry point.
 * Registers the root App component with React Native's AppRegistry.
 */
import { AppRegistry } from 'react-native';
import App from './src/App';
import { name as appName } from './app.json';

AppRegistry.registerComponent(appName, () => App);
