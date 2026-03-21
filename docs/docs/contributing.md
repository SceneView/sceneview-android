# Contributing to SceneView

We welcome contributions of all kinds — bug fixes, new features, documentation, and samples.

---

## Quick start

```bash
# Fork and clone
git clone https://github.com/YOUR_USERNAME/sceneview-android.git
cd sceneview-android

# Open in Android Studio, build, and run a sample to verify setup
```

## AI-assisted workflow (recommended)

SceneView ships with a full [Claude Code](https://claude.ai/code) setup so you can contribute
with AI assistance from the first keystroke:

```bash
# Install Claude Code, then inside the project root:
claude
```

| Command | What it does |
|---|---|
| `/contribute` | Full guided workflow from understanding to PR |
| `/review` | Checks threading, Compose API, Kotlin style, module boundaries |
| `/document` | Generates/updates KDoc and `llms.txt` for changed APIs |
| `/test` | Audits coverage and generates missing tests |

---

## Code style

We follow the official [Kotlin style guide](https://developer.android.com/kotlin/style-guide).
The code style is stored in the repository — Android Studio picks it up automatically.

Key rules:

- **4-space indentation** (no tabs)
- **Trailing commas** in multi-line parameter lists
- **`internal`** visibility for implementation details
- **No wildcard imports**

---

## Pull request guidelines

1. **Fork → branch → PR** — create a feature branch from `main`
2. **Keep changes minimal** — fix what you came to fix, don't refactor the world
3. **Start PR title with uppercase** — e.g., "Add PhysicsNode collision callbacks"
4. **Describe your changes** — a short summary helps reviewers
5. **Same Git name/email as your GitHub account** — for contributor role attribution

---

## Module structure

| Module | What to change |
|---|---|
| `sceneview/` | Core 3D library — nodes, scene, rendering, materials |
| `arsceneview/` | AR layer — ARCore integration, AR-specific nodes |
| `samples/` | Sample apps — add new samples or improve existing ones |
| `docs/` | This documentation site |

---

## Threading rules

!!! warning "Critical"
    Filament JNI calls **must** run on the main thread. Never call `modelLoader.createModel*`
    or `materialLoader.*` from a background coroutine. Use `rememberModelInstance` in composables
    or `loadModelInstanceAsync` for imperative code.

---

## Filament materials

If you modify `.mat` files, recompile them using the
[current Filament version](https://github.com/google/filament/releases).
Enable the Filament plugin in `gradle.properties` and rebuild.

---

## Issues & discussions

- **Bug reports** → [GitHub Issues](https://github.com/SceneView/sceneview-android/issues) (use the templates)
- **Questions** → [GitHub Discussions](https://github.com/SceneView/sceneview-android/discussions)
- **Chat** → [Discord](https://discord.gg/UbNDDBTNqb)

---

## License

By contributing, you agree that your contributions will be licensed under the
[Apache License 2.0](https://github.com/SceneView/sceneview-android/blob/main/LICENSE).
