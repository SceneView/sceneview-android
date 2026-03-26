# Contributing to SceneView

Thanks for your interest in contributing! This guide covers everything you need to get started.

---

## Development environment setup

### Prerequisites

- **JDK 17** (for Android/KMP modules)
- **Android Studio** (latest stable recommended)
- **Xcode 15+** (for SceneViewSwift / iOS work only)

### Clone and open

```bash
git clone https://github.com/sceneview/sceneview.git
cd sceneview
```

Open the project in Android Studio. Gradle sync will pull all dependencies automatically.

### Build

```bash
# Android libraries
./gradlew assembleDebug

# Android demo app
./gradlew :samples:android-demo:assembleDebug
```

For iOS (SceneViewSwift), open `SceneViewSwift/Package.swift` in Xcode and build from there.

### Run tests

```bash
# All tests
./gradlew test

# KMP core tests only
./gradlew :sceneview-core:allTests
```

---

## AI-assisted workflow (recommended)

SceneView ships with a full Claude Code setup so you can contribute with AI assistance
from the first keystroke — no context-gathering needed.

### Quick start

1. Install [Claude Code](https://claude.ai/code)
2. Clone the repo and open it: `claude` inside the project root
3. Run `/contribute` — Claude walks you through the entire workflow

See [CLAUDE.md](CLAUDE.md) for the full module map, architecture overview, threading rules, and AI contributor guidelines.

### Available slash commands

| Command | What it does |
|---|---|
| `/contribute` | Full guided workflow from understanding to PR |
| `/review` | Checks threading rules, Compose API, Kotlin style, module boundaries |
| `/document` | Generates/updates KDoc and `llms.txt` for changed APIs |
| `/test` | Audits coverage and generates missing tests |

### MCP server (optional)

If you use Claude Desktop or another MCP-compatible editor, add the SceneView MCP server
for full API context in any chat:

```json
{
  "mcpServers": {
    "sceneview": { "command": "npx", "args": ["-y", "sceneview-mcp"] }
  }
}
```

---

## Pull request guidelines

1. **One feature per PR.** Keep changes focused and reviewable.
2. **Tests required.** Add or update tests for any behavior change.
3. **Follow existing code style.** Match the patterns in the module you are editing.
4. **Describe the why.** PR descriptions should explain the motivation, not just list changed files.
5. **Keep commits clean.** Squash fixups before requesting review.

Contributions to any part of the project are welcome — Android (`sceneview/`, `arsceneview/`), iOS (`SceneViewSwift/`), shared KMP core (`sceneview-core/`), samples, documentation, or the MCP server.

After your changes are merged, the Discord bot will award you the **Contributor** role.

### Code style

- **Kotlin**: follow the official [Kotlin style guide](https://developer.android.com/kotlin/style-guide) and existing Compose API conventions (composable functions, `remember*` helpers, named parameters). The code style is stored in the repository and auto-configured by Android Studio.
- **Swift**: follow the existing SceneViewSwift patterns (builder-style modifiers, RealityKit conventions).
- No wildcard imports. No unused imports.
- Keep changes minimal — you can fix obvious mistakes in formatting or documentation along the way.

### Changes in Filament materials

Recompile Filament materials using the [current Filament version](https://github.com/google/filament/releases) if you modify them. Enable the [Filament plugin](https://github.com/sceneview/sceneview/blob/main/gradle.properties) and build.

---

## Issues and discussions

- **Bug reports**: use the issue templates on [GitHub Issues](https://github.com/sceneview/sceneview/issues). Include platform, SceneView version, minimal reproduction steps, and relevant logs.
- **Questions**: open a [Discussion](https://github.com/sceneview/sceneview/discussions) instead of an issue.
- **Feature requests**: welcomed as issues or discussions.
- **Chat**: join the [Discord](https://discord.gg/UbNDDBTNqb) to talk with the community and maintainers.
