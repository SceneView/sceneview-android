const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');
const path = require('path');

/**
 * Metro configuration for SceneView RN demo.
 *
 * The demo imports `@sceneview-sdk/react-native` from the linked bridge module
 * at `../../react-native/react-native-sceneview`. We tell Metro to watch that
 * directory so edits to the bridge are hot-reloaded, and we blocklist its
 * duplicate `node_modules/react*` copies to prevent "multiple React" errors.
 *
 * https://facebook.github.io/metro/docs/configuration
 */
const projectRoot = __dirname;
const bridgeRoot = path.resolve(projectRoot, '../../react-native/react-native-sceneview');

const config = {
  watchFolders: [bridgeRoot],
  resolver: {
    // Use the demo's node_modules copy of react/react-native, not the bridge's.
    nodeModulesPaths: [path.resolve(projectRoot, 'node_modules')],
    // Block the bridge's node_modules copies of react* to avoid duplicate
    // registrations of React hook contexts.
    blockList: [
      new RegExp(`${bridgeRoot}/node_modules/react/.*`),
      new RegExp(`${bridgeRoot}/node_modules/react-native/.*`),
    ],
  },
};

module.exports = mergeConfig(getDefaultConfig(projectRoot), config);
