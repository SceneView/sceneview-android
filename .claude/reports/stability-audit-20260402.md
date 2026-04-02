# SceneView Stability Audit — 2026-04-02

## Summary

| Platform | Status | Notes |
|---|---|---|
| Android — sceneview compile | ✅ PASS | BUILD SUCCESSFUL |
| Android — arsceneview compile | ✅ PASS | BUILD SUCCESSFUL |
| Android — sceneview tests | ✅ PASS | 175 tests, 0 failures |
| Android — arsceneview tests | ✅ PASS | 15 tests, 0 failures |
| Android — bundleRelease | ✅ PASS | Signed AAB in 2m6s |
| iOS — Swift files | ✅ PASS | Imports/structs intact |
| iOS — CI workflow | ⏳ Verify on GitHub Actions |
| Web — sceneview.js syntax | ✅ PASS | Valid JS |
| Web — sceneview.github.io | ✅ PASS | HTTP 200 |
| Web — playground.html | ✅ PASS | HTTP 200 |
| KMP Core — Android tests | ✅ PASS | 600 tests, 0 failures |
| KMP Core — JS tests | ✅ PASS | |
| KMP Core — iOS link | ⚠️ LOCAL CACHE | Corrupt klib, not a real bug |
| Flutter plugin | ✅ PASS | v3.6.0, all files present |
| React Native module | ✅ PASS | v3.6.0, all files present |
| MCP version | ✅ PASS | 3.6.0 |
| MCP tests | ✅ PASS | 2360/2360 (8 regressions fixed) |

## Bugs Fixed During Audit
- MCP: 8 test failures in node-reference.test.ts (llms.txt section keys renamed)

## Remaining Issues
- KMP iOS sim link: corrupt local gradle cache — run `rm -rf ~/.gradle/caches/modules-2/files-2.1/dev.romainguy/kotlin-math-iossimulatorarm64`
