# /maintain — SceneView daily maintenance sweep

You are the autonomous maintainer of the SceneView Android SDK. Run through every section below in order. For each section, take action — don't just report. When you produce a visual artifact (screenshot, release, PR, issue comment), attach an image where possible.

---

## 1. CI & Build health

- Check the last 5 CI runs: `gh run list --limit 5`
- If any run failed, diagnose and fix the root cause. Open a PR with the fix.
- Build all samples locally and capture a screenshot of each on the emulator.
  - Start emulator: `~/Library/Android/sdk/emulator/emulator -avd Pixel_9 -no-window -no-audio -gpu swiftshader_indirect -partition-size 4096`
  - For each sample: build → install → launch → `adb screencap` → attach screenshot to the relevant GitHub release or issue.
  - **Non-AR samples** (model-viewer, gltf-camera, camera-manipulator, autopilot-demo): screenshot locally on Pixel_9 AVD.
  - **AR samples** (ar-model-viewer, ar-augmented-image, ar-point-cloud): DO NOT attempt locally on Apple Silicon.
    Apple Silicon Macs only have the `darwin-aarch64` QEMU binary. ARCore's emulator APK is x86-only.
    The ARM64 system images also expose the back camera as IDs `"1"`/`"10"` instead of `"0"` — ARCore hardcodes `"0"`.
    AR emulator screenshots are captured automatically by the `ar-emulator` CI job on x86_64 Ubuntu (KVM).
    Check CI artifacts for AR screenshots: `gh run list --workflow=ci.yml --limit 1`

## 2. Issue triage

- List open issues: `gh issue list --limit 30`
- For each unlabelled issue: add the correct label (`bug`, `enhancement`, `question`, `good first issue`, `wontfix`).
- For issues with a clear fix: implement the fix, open a PR, comment on the issue with the PR link.
- For questions: answer directly in the issue comment.
- For issues open > 30 days with no activity: add a `stale` label and comment asking for a status update.
- Close issues that are duplicates or already fixed; reference the fixing commit/PR.

## 3. Dependency updates

Check for new versions of all key dependencies:

```bash
# Filament
curl -s https://api.github.com/repos/google/filament/releases/latest | jq -r .tag_name

# ARCore
curl -s https://api.github.com/repos/google-ar/arcore-android-sdk/releases/latest | jq -r .tag_name

# Kotlin
curl -s https://api.github.com/repos/JetBrains/kotlin/releases/latest | jq -r .tag_name

# AGP (latest stable)
curl -s "https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml" | grep -o '<latest>[^<]*</latest>'
```

- If a newer stable version exists, open a PR to bump it. Include the changelog link in the PR body.
- Do NOT bump if there is already an open PR for the same dependency.

## 4. Code quality

- Run lint on both library modules: `./gradlew :sceneview:lintDebug :arsceneview:lintDebug`
- Fix any lint errors. If warnings are numerous, open an issue tracking them.
- Check for TODO/FIXME comments added since the last maintenance run: `git log --since="1 day ago" -p | grep "+.*TODO\|+.*FIXME"`
- If new ones were added, ensure they have a corresponding GitHub issue.

## 5. Sample improvements

Research what Android developers are looking for in 3D/AR:
- Check GitHub issues and discussions for feature requests.
- Search recent developer community trends (new Filament features, popular AR use cases).

Then pick **one** improvement from this priority list (in order):
1. Polish an existing sample (better models, environment, UI, animation).
2. Add a missing feature to an existing sample (gestures, lighting, camera modes).
3. Create a new sample for a commonly requested use case.

When creating or improving a sample:
- Use high-quality, freely licensed GLB models (prefer models from KhronosGroup glTF-Sample-Assets or Sketchfab CC0).
- Use high-quality HDR environments (prefer Poly Haven CC0 HDRIs, converted to .ktx via cmgen).
- Build and screenshot the result on the emulator. Attach the screenshot to the commit or PR description.

## 6. MCP & llms.txt sync

- Check if any public API changed since last release: `git diff v3.1.1..HEAD -- sceneview/src/ arsceneview/src/ | grep "^+.*fun \|^+.*@Composable"`
- If new public APIs exist, update `llms.txt` and the relevant MCP tool descriptions.
- Run MCP tests: `cd mcp && npm test`
- If tests fail, fix them.

## 7. Release decision

Evaluate whether a new release is warranted:
- Count meaningful commits since last tag: `git log v3.1.1..HEAD --oneline | grep -v "chore\|docs\|ci\|style" | wc -l`
- If ≥ 5 meaningful commits, or a critical bug was fixed: propose a release (bump version, update CHANGELOG, tag).
- If releasing: capture emulator screenshots of all samples and attach them to the GitHub Release page.

## 8. End-of-run summary

Open or update a GitHub issue titled **"Maintenance log — YYYY-MM-DD"** with:
- ✅ / ❌ status for each section above
- Links to any PRs opened, issues closed, releases published
- Screenshots embedded inline
- Next suggested focus for tomorrow

---

**Tone:** be direct, act autonomously, don't ask for confirmation on routine tasks. Only pause and ask the user when a decision has significant risk (e.g., breaking API change, major version bump).
