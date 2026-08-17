# 3D Frame Try-On Device Matrix

**Status:** Primary POCO run recorded; additional-device coverage pending

**Recorded:** 2026-08-18
**Scope:** One validated physical round-frame variant. The rectangular and
cat-eye/browline assets are deferred until suitable physical frames are
available; this record does not claim three-variant coverage.

## Automated build evidence

| Check | Result |
| --- | --- |
| `testDebugUnitTest` | PASS |
| `lintDebug` | PASS |
| `assembleDebug` | PASS |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| APK SHA-256 | `CFDC6BE69EA27D89C5EC75BB870A03E371FC5C23015786FB6BDDA5843CC7FD37` |
| APK size | 112,768,674 bytes |
| App version | `1.0` (`versionCode=1`) |
| Source checkpoint | `f891dd5` (`fix: enforce three-frame reservation limit`) plus other uncommitted session changes |

## Primary device

| Field | Recorded value |
| --- | --- |
| Manufacturer/model | Xiaomi POCO X8 Pro (`2511FPC34G`) |
| Android/API | Android 16 / API 36 |
| Total memory | 7,497,916 kB (`/proc/meminfo`) |
| GPU/graphics | ARM Mali-G720 MC8, OpenGL ES 3.2 |
| Asset | Bundled textured round-frame GLB (`models/round_frame_textured.glb`) |
| Asset evidence | [`3d-frame-try-on-feasibility.md`](3d-frame-try-on-feasibility.md) |

## Current-build manual run

The current-build POCO run was reported as usable. The earlier feasibility gate
is recorded separately and is not silently reused as a current-build
measurement.

| Observation | Result | Notes |
| --- | --- | --- |
| App opens and camera permission flow works | **PASS** | Reported usable on the POCO. |
| Round GLB loads over the front camera | **PASS** | The textured round frame rendered with the expected label. |
| Face loss/reacquisition has no blocking or stuck loading state | **PASS with limitation** | A fast upward head tilt can hide the model for about one second; it returns when MediaPipe reacquires the face. |
| Normal head movement, distance, yaw, pitch, and roll remain usable | **PASS with limitation** | Normal movement was usable; the fast upward-tilt reacquisition gap is recorded above. |
| Image fallback and reservation actions remain available | **PASS** | No failure was reported during the manual run. |
| 60-second normal-use run has no crash/reset | **PASS** | No crash, renderer reset, or blocking failure was reported. |
| FPS/load-time summary captured from this run | **PASS with measurement note** | `gfxinfo` after the run: 11,013 frames, 12 ms 50th percentile (~83 FPS), 2 janky frames (0.02%), 1 missed vsync. Statistics were not reset immediately before a stopwatch-controlled 60-second sample. |

The fast-tilt gap is consistent with the current tracking policy: a transient
`NoFace` result clears the last pose immediately, and the renderer hides the
model until the next valid transformation matrix arrives. This is a recovery
limitation, not an asset download or backend failure. Do not claim another
device or a three-variant result without running it and recording its facts.

## Additional devices

No additional device has been tested for this slice. Unsupported or untested
device classes must remain explicitly unclaimed.
