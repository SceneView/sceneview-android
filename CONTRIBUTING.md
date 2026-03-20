# How to contribute

## AI-assisted workflow (recommended)

SceneView ships with a full Claude Code setup so you can contribute with AI assistance
from the first keystroke — no context-gathering needed.

### Quick start

1. Install [Claude Code](https://claude.ai/code)
2. Clone the repo and open it: `claude` inside the project root
3. Run `/contribute` — Claude walks you through the entire workflow

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

## Issues

Use the issue templates on GitHub. For questions, open a [Discussion](https://github.com/SceneView/sceneview/discussions) instead.

Feature requests are welcomed too!

## Discussions

You can create a discussion if you have a question rather than a problem report. You should also copy your questions and provided answers from the Discord server if you feel that other developers can benefit from them in future.

## Pull requests

You can create a pull request if you know how to fix a problem, improve the documentation or implement a new feature. You just need to fork the repository, commit your changes and create a pull request from there. After the changes are merged the Discord bot will award you the **Contributor** role :tada:

### Title

You should start the title of the pull request with an uppercase letter.

### Description

You should provide a short description of your changes so other contributors can better understand them.

### Author

You need to make sure that you use the same name and email as in your GitHub account when committing changes. Otherwise, the Discord bot may have difficulties with awarding you the **Contributor** role.

### Code Style

We use the official [Kotlin style guide](https://developer.android.com/kotlin/style-guide) in the project. The code style is stored in the repository so everything should be configured automatically when you open the project in Android Studio.

### Changes in source code

You should keep changes as minimal as possible, however, you can fix obvious mistakes in the source code, formatting or documentation.

### Changes in Filament materials

You should recompile the Filament materials using the [current Filament version](https://github.com/google/filament/releases) if you make any changes to them. The recommended way to do that is to enable the [Filament plugin](https://github.com/SceneView/sceneview/blob/main/gradle.properties) and build the project.
