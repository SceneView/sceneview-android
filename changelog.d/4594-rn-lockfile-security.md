<!-- category: Security -->
- **React Native lockfiles no longer pull in `sprintf-js` ([#4594](https://github.com/sceneview/sceneview/pull/4594)).** The advisory GHSA-hp3w-g68c-fv3c has no patched release, so a scoped npm override moves js-yaml's CLI-only `argparse` dependency to 2.0.1, which drops `sprintf-js` from both the package and the demo dependency trees. No package version change.
