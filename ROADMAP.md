# Roadmap

> Ship fast, ship often. Every feature = a release.

## Current: v3.5.2 (March 2026)

**Multi-platform expansion** — 9 platforms, MCP Registry, store-ready.

| What | Status |
|---|---|
| Android SDK (Filament + Compose) | Stable |
| iOS / macOS / visionOS (RealityKit + SwiftUI) | Alpha |
| Web (Filament.js + WebXR) | Alpha |
| Desktop (Compose Desktop) | Alpha |
| Android TV | Alpha |
| Flutter bridge | Alpha |
| React Native bridge | Alpha |
| MCP on official registry | Live |
| Play Store demo app | Deploying |
| App Store demo app | Secrets ready |

---

## Next: v3.6.0

### API simplification
- [ ] Merge sceneview + arsceneview into single dependency (one `implementation` line)
- [ ] Unify naming: `SceneView {}` on all platforms (currently `Scene {}` on Android)

### Platform maturity
- [ ] Filament JNI for Desktop (hardware 3D, replace software renderer)
- [ ] Android XR module (Jetpack XR SceneCore)
- [ ] visionOS spatial features (immersive spaces, hand tracking)
- [ ] sceneview-core WASM target (when kotlin-math supports wasmJs)

### Store releases
- [ ] Play Store: SceneView Demo published
- [ ] App Store: SceneView Demo on TestFlight

### AI & monetization
- [ ] MCP Pro with API key + Stripe Billing
- [ ] Claude playground on website (describe in natural language → 3D code)
- [ ] OpenAI GPT Store entry

### Quality
- [ ] Visual regression testing across platforms
- [ ] Performance benchmarks (FPS, memory, load time)
- [ ] Accessibility audit

---

## Future

### v4.0 (breaking changes)
- Compose Multiplatform renderer (shared Compose UI across Android + Desktop + Web)
- Filament 2.x migration (when available)
- Scene graph serialization (save/load scenes as files)

### Exploration
- Android Auto / AAOS (when custom 3D views are supported)
- Wear OS (Canvas-based 3D for watch faces)
- Cloud rendering API (server-side Filament)
- Real-time multiplayer scenes (WebSocket sync)
