# shared

Source-of-truth for validator helpers shared across the vertical SceneView MCP packages
(`interior`, `gaming`, `healthcare`, `automotive`).

## What lives here

`src/deprecated-api-check.ts` exports two pure helpers:

- `checkDeprecatedApi(code)` — detects pre-v3.6 `Scene { }` / `ARScene { }`, pre-3.0
  Sceneform calls (`ArSceneView()`, `loadModelAsync`, `Engine.create`), and `import com.google.ar.sceneform.*` lines.
- `checkMissingSceneViewImports(code)` — detects usage of `SceneView { }` /
  `ARSceneView { }` without the matching `import io.github.sceneview...` line.

Both return `ValidationIssue[]` that the caller merges into its own issues list.

## How it's wired

This is **not a published npm package**. Each consuming sub-package's `tsconfig.json`
declares:

```json
"rootDirs": ["src", "../shared/src"]
```

Which makes TypeScript treat `../shared/src/*` as if it lived in the package's own
`src/`. The validator imports it via `import { checkDeprecatedApi, checkMissingSceneViewImports } from "./deprecated-api-check.js"`.

When the sub-package runs `tsc`, the shared file is compiled into the sub-package's
own `dist/` directory alongside its other modules, and gets shipped to npm via the
sub-package's `files:` array. There is no `node_modules` resolution at runtime — the
file is fully bundled into each sub-package's published artifact.

## Why this layout

1. Single source of truth — fix once, every package picks it up on next build
2. Each sub-package stays self-contained on npm (no cross-package `file:` deps that break on publish)
3. No build step in `shared/` itself — `noEmit: true` keeps it as a pure source root
4. Editors get full type-checking via the local `tsconfig.json`

## Adding more shared helpers

1. Drop a new `.ts` file in `src/`
2. Each sub-package can immediately import it via `./<filename>.js`
3. Add the compiled `dist/<filename>.js` to each sub-package's `files:` array in `package.json`
