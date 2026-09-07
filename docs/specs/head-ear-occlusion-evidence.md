# Head/Ear Occlusion Evidence

## Model boundary

- Model: MediaPipe SelfieSegmenter, square float16, confidence output.
- Asset: `app/src/main/assets/selfie_segmenter.tflite`
- Size: 249,537 bytes.
- SHA-256: `191AC9529AE506EE0BEEFA6B2C945A172DAB9D07D1E802A290A4E4038226658B`
- Source: https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite
- Documentation: https://developers.google.com/edge/mediapipe/solutions/vision/image_segmenter/android
- Model card: https://storage.googleapis.com/mediapipe-assets/Model%20Card%20MediaPipe%20Selfie%20Segmentation.pdf
- The reviewed model card identifies the model as a person/background segmenter
  intended for on-device AR/mobile use and licensed under Apache-2.0. It also
  documents expected degradation for thin features, poor lighting, and motion;
  those limitations remain part of the device checkpoint.

## Automated evidence

The following commands passed for the fixed-capacity occluder implementation:

```powershell
.\gradlew testDebugUnitTest
.\gradlew assembleDebug
.\gradlew lintDebug
```

Focused coverage includes shared camera timestamp pairing, invalid/out-of-order
result handling, mask dimensions, mirror/aspect-fill mapping, central-face
exclusion, confidence/freshness fallback, and temple-policy regression tests.

## Device checkpoint

Status: pending. The workspace has no `adb` executable or connected POCO X8 Pro,
so visual alignment, layer ordering, FPS, thermal behavior, and side-profile
temple coverage have not been claimed as passing. Install the debug APK on the
POCO and record front, 30–60 degree yaw, side profile, quick turns, face loss,
reacquisition, and a 60-second movement run before any production-hardening
decision.

No segmentation values are persisted, uploaded, logged, or sent to analytics.
