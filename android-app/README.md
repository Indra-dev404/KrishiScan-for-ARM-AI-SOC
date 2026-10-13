# KrishiScan — Android App

🚧 **In development.** This is the main deliverable of the project.

## Planned architecture

Two on-device pipelines (documented separately, as the problem statement requires):

1. **Vision pipeline** — distilled TinyCNN student exported to ExecuTorch (`.pte`), running on the Arm CPU via XNNPACK.
   Leaf photo → preprocessing → disease classification (25 classes, 5 crops in v2).
2. **Advisory pipeline** — Llama 3.2 1B-Instruct (quantized) running on-device via the ExecuTorch LLM runner.
   Diagnosis + crop + severity + region + language → grounded advisory, generated locally.
   Falls back to the verified static advisory database if generation is unavailable.

Supporting pieces:

- **Severity** — OpenCV lesion-coverage estimate → mild / moderate / severe (bands under validation)
- **Region** — West Bengal first; advisory content localised per region
- **Languages** — English / Hindi / Bengali
- **Offline** — after a one-time model setup, the app needs no network at all

## Status

Sprint in progress (Oct–Nov 2026). The working browser version of the vision pipeline lives in [`../web-demo/`](../web-demo/).
