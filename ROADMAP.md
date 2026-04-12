# Roadmap

> Ship fast, ship often. Every feature = a release.

## Current: v4.0.0-rc (April 2026)

**AI-first SDK** — 9 platforms, MCP monetization live, Rerun.io debug integration.

| What | Status |
|---|---|
| Android SDK (Filament + Compose) | Stable |
| iOS / macOS / visionOS (RealityKit + SwiftUI) | Alpha |
| Web (Filament.js + WebXR) | Alpha |
| Desktop (Compose Desktop) | Placeholder |
| Android TV | Alpha |
| Flutter bridge | Alpha |
| React Native bridge | Alpha |
| MCP on official registry | Live (4.0.0-rc.3 @latest) |
| MCP Gateway (Stripe billing) | Live (first paying customer) |
| Hub MCP (11 libs, 52 tools) | Live (hub-mcp@0.1.0) |
| Telemetry Worker | Live |
| Rerun.io debug integration | Shipped (Android + iOS + Python) |
| Play Store demo app | Deployed |
| App Store demo app | TestFlight |

---

## Next: v4.0.0 (stable)

### API
- [x] Unify naming: `SceneView {}` / `ARSceneView {}` (v3.6)
- [x] Separate modules kept intentionally: `sceneview` (3D-only, no ARCore) + `arsceneview` (opt-in AR)

### Platform maturity
- [ ] Filament JNI for Desktop (hardware 3D, replace placeholder)
- [ ] Android XR module (Jetpack XR SceneCore)
- [ ] visionOS spatial features (immersive spaces, hand tracking)
- [ ] sceneview-core WASM target (when kotlin-math supports wasmJs)

### Quality
- [x] Render tests CI (SwiftShader, 4 test classes)
- [x] NodeAnimator bug fix (#388)
- [ ] Visual regression testing across platforms
- [ ] Performance benchmarks (FPS, memory, load time)

### AI & monetization
- [x] MCP Pro with Stripe Billing (free/pro/team tiers, gateway live)
- [x] Hub MCP (11 vertical libraries bundled)
- [x] Anonymous telemetry (Cloudflare Worker + D1)
- [x] Claude playground on website
- [ ] First external paying customer (Thomas = dogfooding)

---

## Future

### v4.1+
- Compose Multiplatform renderer (shared Compose UI across Android + Desktop + Web)
- Filament 2.x migration (when available)
- Scene graph serialization (save/load scenes as files)

### Exploration
- Android Auto / AAOS (when custom 3D views are supported)
- Wear OS (Canvas-based 3D for watch faces)
- Cloud rendering API (server-side Filament)
- Real-time multiplayer scenes (WebSocket sync)
