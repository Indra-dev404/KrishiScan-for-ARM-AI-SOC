# KrishiScan — Android App

The main deliverable: a native Android app that diagnoses tomato leaf diseases, estimates severity, and gives treatment advice, fully offline.

**Download:** [KrishiScan Android v1.0 release](https://github.com/Indra-dev404/KrishiScan-for-ARM-AI-SOC/releases/tag/android-v1.0)

## Requirements

- Android 8.0 or newer (minSdk 26)
- arm64-v8a processor (almost every phone from the last several years)
- About 1.2 GB free storage if you want Llama-generated advice

## Install

1. Download the APK from the release page onto the phone.
2. Tap the file. Android asks to allow installs from that app (Chrome, Files or WhatsApp) — allow it, then tap **Install**.
3. Open KrishiScan and allow camera access.

That is enough for diagnosis, severity, and the verified advice in English, Hindi and Bengali.

## One-time Llama setup (for generated advice)

The Llama model is 1.1 GB, so it is not packed inside the APK. Without it the app still works and shows the verified advice. To enable generated advice:

**1. Download the two model files** from [executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET](https://huggingface.co/executorch-community/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET) on Hugging Face:

- `Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte` (1,135,951,488 bytes)
- `tokenizer.model` (2,183,982 bytes)

**2. Push them to the phone** (USB debugging on, phone connected, app already installed):

```
adb shell mkdir -p /data/local/tmp/llama
adb push Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte /data/local/tmp/llama/
adb push tokenizer.model /data/local/tmp/llama/
adb shell "run-as com.indra.krishiscan cp /data/local/tmp/llama/* /data/data/com.indra.krishiscan/files/"
```

Run the `mkdir` line first. If the folder does not exist, `adb push` reports success but the file lands nowhere.

**3. Restart the app.** Llama loads in the background in about 6 seconds. English scans now get generated advice.

## How it works

```
Camera photo ──► VisionEngine (ONNX Runtime) ──► disease class + confidence
      │
      └────────► SeverityEngine (OpenCV) ──────► % of leaf covered by lesions
                                                        │
 diagnosis + severity + region + verified facts ────────┘
                         │
                         ▼
          PromptBuilder ──► AdvisoryEngine (Llama 3.2 1B on ExecuTorch) ──► advice
```

| File | Job |
|---|---|
| `visionengine.kt` | Loads `student.onnx`, resizes to 224×224, applies ImageNet normalisation, returns label, confidence and inference time |
| `SeverityEngine.kt` | Leaf mask (green HSV range, largest contour, convex hull) and lesion mask (yellow-brown and dark-brown HSV ranges); coverage = lesion ÷ leaf |
| `PromptBuilder.kt` | Verified advisory facts for all 10 classes in English, Hindi and Bengali, and the grounded Llama prompt |
| `AdvisoryEngine.kt` | Wraps ExecuTorch's `LlmModule`: load once, generate with temperature 0.3 and a token cap |
| `MainActivity.kt` | CameraX preview and capture, the scan flow, region and language selectors, result screen |

Design choices worth knowing:

- **The two pipelines are separate.** The photo goes to the vision model only. Llama gets text.
- **Grounded generation.** The prompt carries verified facts for the diagnosed disease and tells Llama to use only those. Generation is capped at 120 tokens.
- **Static fallback.** If Llama is missing, still loading, or fails, the verified advice is shown instead.
- **Hindi and Bengali are not generated.** They use translations of the verified advice. Llama 1B's Bengali was garbled in on-device testing.
- **No INTERNET permission** in the manifest. Offline is enforced, not just tested.
- **One scan at a time.** The Scan button stays locked until the advice is finished, and camera photos are downscaled to 512 px before analysis to keep memory low next to a 1.1 GB model.

## Measured on a Redmi 15 5G

Android 16, 8 GB RAM, arm64-v8a.

| What | Measured |
|---|---|
| Llama cold load | 5,709 ms |
| Time to first token | 361 ms |
| Decode speed | 9.4 tokens/s |
| APK size | 81 MB |
| Model storage on the phone | 1.0 GB |
| Airplane-mode scan → advice | Works |

## Build from source

1. Open the `android-app/` folder in Android Studio and let Gradle sync.
2. Connect a phone and press Run, or build an APK from the terminal:

```
.\gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Pinned dependencies: `executorch-android` 1.5.1, `onnxruntime-android` 1.32.0, OpenCV 4.11.0, CameraX 1.7.0. The build is limited to `arm64-v8a`, which brings the APK from 310 MB down to 81 MB.

## Known limitations

- Tomato only, trained on PlantVillage lab photos. Field accuracy is not measured.
- Severity bands are proposed: 10 of 15 agreement with hand labels in a pilot. Bacterial spot under-reads.
- Generated advice is English only. The Bengali text has been checked by a native reader; the Hindi text has not been reviewed yet.
- The vision model in the app is the floating-point student (1.5 MB). The 0.41 MB INT8 version is measured but not yet shipped in the app.

## Attribution

**Built with Llama.** Llama 3.2 is licensed under the Llama 3.2 Community License, Copyright © Meta Platforms, Inc. All Rights Reserved.
