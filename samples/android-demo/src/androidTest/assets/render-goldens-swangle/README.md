# 3D-demo render goldens, CI profile

The references the `demo-render-goldens` CI job compares against
(`goldenSet=render-goldens-swangle`, #3554). Same test, same contract and same
tolerances as `../render-goldens/` (read that README first); only the profile
differs, so these are never interchangeable with the hardware-GPU set.

## Profile

`ubuntu-latest` hosted runner, API 30 `google_apis` x86_64, **`-gpu
swangle_indirect`** (ANGLE over SwiftShader-Vulkan), 1080x2400 @ 420 dpi, light
mode. The test crops the 96 px status bar, so the PNGs are **1080x2304**.

## Readiness

On this rasteriser the first frame takes tens of seconds, and the scaffold's own
loading states (the M3 loading indicator, the "Still loading…" card after 12 s)
are non-flat content. The test therefore captures only once the viewport is named
"Scene ready", no stall card is up and no `Loading…`/`Streaming…` scrim is
visible, then waits `READY_TAIL_MS`. The readiness budget,
`SOFTWARE_MAX_SETTLE_MS`, is about twice the slowest measured case; every run
logs `first-frame slug=… elapsedMs=…` in the job's `logcat.txt`.

## Recording

1. Remove the case from `SWANGLE_BASELINED_GOLDENS` (and its PNG here), push, and
   dispatch `render-tests.yml` on the branch. The case takes the first-run path
   and saves `<name>_first_run.png`.
2. Download the `demo-render-golden-captures` artifact and **look at every
   image**: model present, colours right, no loading indicator or card, chrome
   intact. A capture that is wrong is not a reference.
3. Copy the good ones here as `<name>.png` and add them back to
   `SWANGLE_BASELINED_GOLDENS` in the same commit.

## Left out of the gate

None today. `splatpreview_default` was out until #4459: its picture moved from one
run to the next (54.7 % of pixels against its recording in run 36416690269, 54.9 %
between the two attempts of run 38044621943).

### What moved the Splat Preview picture (#4459)

Not the framing: the three CI captures of commit `4140018cb` hold the same
geometry and differ in how the translucent points are blended, mostly in the
foreground ground — 0.49 % of pixels between runs 1 and 3, 13.6 % and 13.8 %
between run 2 and the two others. The captures were taken 15.3 s, 7.8 s and
8.5 s after "Scene ready".

The cause was the order the screen opened in. It drew an empty stage on the
default camera while the file decoded, then swapped the camera onto the scan,
which the SDK turns into a 0.6 s glide. `SplatNode` sorts its points back to
front for the camera on a background thread, one sort at a time, and only
starts another when the camera has moved more than 1 % of the scan's radius.
So the sorts chased the glide: "Scene ready" could be announced on an order
computed for a pose the camera had already left, replaced seconds later on a
software rasteriser, and the last order kept was sorted for a pose merely
*near* the final one, which one depending on frame timing.

Measured on `emulator-5554` (host GPU, status bar cropped, 3 450 880 px),
captures at +0.5 s, +2 s, +5 s and +15 s after "Scene ready":

| | Within one cold start | Between cold starts |
| --- | --- | --- |
| Before | 0 px at all four delays | 0.30 % and 0.03 % — three starts, three pictures |
| Before, order forced to the one sorted for the default camera | 0 px over 15 s | 20.4 % against a normal start |
| After | 0 px at all four delays | 0 px across five cold starts |

The screen now draws the stage only once the file is open, with the camera
already on the scan (no glide, one sort, for the exact home pose), and keeps
its cover up until that sort is on screen. On the emulator the scan appears
about 0.6 s later than before, because the surface is no longer created while
the file decodes.

On CI (SwiftShader), three runs of the fix — 38090189401, 38090195575,
38090201359, 2026-10-10 — gave the same picture: 42, 51 and 16 pixels of
2 488 320 differ between them by more than 8 on a channel (largest difference
34), against 0.49 % to 13.8 % before. The reference here is the first of the
three. The default budget (2 % of pixels, 8 per channel) is kept.

## "Scene ready" came before the models (#4448, closed by #4459)

Seven runs of 2026-10-10 on the same code gave **two pictures** for several
screens, by runner speed. On the fast runners the capture holds the whole scene;
on the slow ones it is taken before a model, or the environment, is in it:

| Case | What the early capture lacks |
| --- | --- |
| `cameragestures_default` | all three models (4 runs of 7) |
| `secondarycamera_default` | the helmet in both views (3 of 7), or only the environment's shading (1 of 7) |
| `lighting_default` | the helmet and the sky (2 of 7), or the sky alone (1 of 7) |
| `fog_default`, `lightinglab_default` | the helmet (5 of 7) — both slugs opened the same Lighting Lab screen since #4282; both cases retired by #4603, which folded the lab into `lighting` |
| `customgeometry_default`, `geometry_default` | nothing visible to the eye: only the shading of the shapes differs, by 11.5 % and 5.4 % of pixels (4 runs of 7 in the early state) |
| `materials_default` | the blurred sky behind the spheres (1 of 7) |

The references here are the complete picture, so a run that captures early goes
red, and it should: the picture it took is not the screen. The cause was the
readiness signal, not the renderer — "Scene ready" followed the first presented
frame (and, on the screens that pass `sceneReady`, the HDR), never the models.

Since #4459 each of these screens holds "Scene ready" for what its picture
needs — the model instances, their textures (`ModelLoader.isLoading`), the
environment, the decoded splat — and, when any of it landed after a frame had
already been presented, for one backend drain behind the first frame that
carries it. A load that reports a failure shows a "could not load" card on the
stage, and the test fails on that card by name; a load that is merely slow shows
"Still loading part of this scene…" after 30 s and is never captured as ready.

First CI run with #4459 (38082396646, 2026-10-10): 13 of the 14 gated cases
matched. One more run is not a proof of stability; read the next ones.

- `geometry_default` was the early state. With #4459 the capture carries the
  environment's highlights on the shapes and differed from the old reference by
  5.38 %, as predicted; the reference here is that run's capture.
  `customgeometry_default` matched as it was.
- `splatpreview_default` now waits for the decoded scan, which removes the
  empty-stage capture; what made it differ between runs was the sort order, see
  "What moved the Splat Preview picture" above.

## Debug Overlay: the reference was the early picture (#4448)

After #4459 one case kept going red, about three runs in ten:
`debugoverlay_default`, 3.49 % of pixels (86 774 to 86 803), always the same
ones — the sphere's shading and the floor's grid lines, nothing in the chrome.

Debug Overlay was not among the screens #4459 rewired: it named itself "Scene
ready" on its first frame and never waited for its studio HDR. The capture was
therefore taken before the HDR on a slow runner and after it on a fast one, and
the reference recorded in #4458 was the slow runner's picture — the scene lit by
the neutral fallback, with white grid lines. The red runs were the ones that
showed the real screen.

The screen now holds "Scene ready" for the HDR, and so does Picking & Collision,
which had the same unheld environment behind a wider budget. With the hold, three
runs on the same commit (38394278289, 38394282012, 38394285785) all held for 15 to
28 s and all produced the HDR-lit picture, 86 774 px from the old reference each
time. The reference here is the first of the three.

## The emulator is gone when the suite ends (#4448)

`adb: device offline` in this job is not a case that kills the emulator. In the
eight nightlies read for #4448 and in every run since, all 15 cases execute; the
emulator process dies **after** the last one, within half a second of the
framework killing the instrumented process ("Force stopping … finished inst").
The test runner has already finished the activity and the Filament engine is
already destroyed at that point, so a live viewport is not what it trips on.
The three jobs of the same workflow that run `-gpu swiftshader_indirect` end
with "OK: killing emulator"; only this one, on `swangle_indirect`, does not.

What it costs: Gradle cannot uninstall the APKs (hidden by `|| true`), the final
`adb pull` finds no device, and whatever a case wrote after the harvest loop's
last 20 s pass is not in the artifact. Verdicts are not affected — the JUnit XML
is complete.
