# 3D Frame Try-On Feasibility Gate

**Decision:** PASS

**Gate date:** 2026-08-16

**Scope:** One locally bundled, textured round-frame GLB on the POCO X8 Pro.
This gate does not claim that the remote catalog contract, backend publication,
multi-variant loading, or clinical fit is complete.

## Recorded build and asset

| Field | Recorded value |
| --- | --- |
| Android branch | `feature/3d-frame-try-on-impl` |
| 3D calibration checkpoint | `0c6e175` (`fix: nudge round frame bridge upward`) |
| Build | Debug APK; `assembleDebug` passed |
| APK SHA-256 | `D10ED78F1A4333E7DB369BE8620F43FD07E3DD91545E25976D1C27215D6FA27E` |
| Device | POCO X8 Pro, model `2511FPC34G`, Android 16 |
| Bundled asset | `models/round_frame_textured.glb` |
| Asset version | `bundled-feasibility-v1` (local fixture; no remote catalog version) |
| GLB size | 5,256,552 bytes |
| GLB SHA-256 | `0D0A198A22FD526EEE994E31F7E45148F09D1757554636D8CBBF5D5E306381C8` |
| Initialization | Observed within the 3-second target during the manual run; no stopwatch or in-app timer was available |
| Elapsed project time | Gate completed on the second calendar day after implementation began; focused hours were not separately tracked |

## Performance evidence

After the stakeholder's manual run, `adb shell dumpsys gfxinfo com.eyecare.app`
reported:

- 4,256 rendered frames;
- 50th-percentile frame time: 11 ms, approximately 91 FPS;
- 0.47% janky frames; and
- 7 missed-vsync frames.

The Android graphics statistics window was approximately 239 seconds and was
not reset immediately before a stopwatch-controlled 60-second sample. The
median frame-time result is nevertheless above the approved 24 FPS threshold;
this limitation is recorded so the result is not presented as a laboratory
benchmark.

## Manual gate result

The stakeholder reported that the full POCO check was good. The observed flow
covered the live front-camera preview, face acquisition and loss/reacquisition,
normal movement and distance changes, variant controls, image fallback access,
and repeated try-on use. No crash, renderer reset, blocking error, or
out-of-memory failure was observed.

| Gate | Result | Evidence |
| --- | --- | --- |
| Live GLB over the front camera | PASS | Model rendered over CameraX on the POCO |
| Median performance at least 24 FPS | PASS | 11 ms 50th-percentile frame time (~91 FPS) from `gfxinfo` |
| Recognizable stable attachment | PASS | Manual front-view and movement review; final calibration accepted |
| No crash/reset/blocking failure | PASS | Manual lifecycle and recovery review; no failure observed |

## Accepted prototype limitations

- The current transparent SceneView composition has no face-depth or
  segmentation occlusion, so a temple can remain visible over the side of the
  face at oblique angles.
- Pose smoothing uses a bounded response time, so the model can trail very
  fast face motion slightly. This was accepted for the feasibility slice.
- The GLB lens material remains an asset-authoring concern; the Android path
  renders the supplied asset as authored.
- The current calibration is a visual preview only. Final fit is confirmed at
  the clinic.

## Next branch

Proceed to PASS-branch Task P1: add the typed nullable `ar` asset contract and
repository-boundary mapping. Keep the bundled GLB explicitly as the
feasibility/demo fallback until a backend response fixture and approved remote
asset publication path exist.
