# 🌿 KrishiScan

**Offline crop-disease detection and advisory for smallholder farmers.**

KrishiScan identifies crop diseases from a leaf photo and gives the farmer actionable treatment advice — **entirely on-device, with no internet connection**. It is built for rural communities where connectivity is limited or absent, and for the low-end hardware farmers actually own.

> Built for Problem Statement 1 (offline crop-disease / pest detection & advisory).

---

## 📦 What's in this repo

| Folder | What it is | Status |
|---|---|---|
| [`web-demo/`](web-demo/) | Fully-offline in-browser demo: upload/camera → on-device inference → diagnosis + advisory | ✅ Working |
| [`android-app/`](android-app/) | The main deliverable: native Android app — vision via **ExecuTorch**, advisory generated on-device by **Llama 3.2 1B** | 🚧 In development |

## 🚧 Android app (main deliverable, in development)

Two on-device pipelines, documented separately:

1. **Vision pipeline** — the distilled TinyCNN student, exported to ExecuTorch (`.pte`), runs on the phone's Arm CPU (XNNPACK): leaf photo → disease classification.
2. **Advisory pipeline** — Llama 3.2 1B-Instruct (quantized), running on-device through the ExecuTorch LLM runner: takes the diagnosis, crop, severity and region, and generates a grounded, multilingual advisory. If generation is unavailable, the app falls back to the verified static advisory — it never shows "no advisory."

Supporting pieces: an OpenCV lesion-coverage step for severity levels (mild / moderate / severe), region selector (West Bengal first), and English / Hindi / Bengali UI + advisory languages. Everything runs offline after a one-time model setup.

## 🌐 Web demo (working today)

A zero-install mirror of the vision pipeline that runs in any modern browser:

- Upload, drag-and-drop, or phone-camera capture
- Preprocessing in-page (cover-crop, 224×224, ImageNet normalisation)
- Inference by **ONNX Runtime Web (WebAssembly)** — model and runtime are bundled locally, **no CDN, no server**
- Diagnosis + confidence + top-3 alternatives + a per-disease advisory card with severity badge

Run it locally:

```bash
cd web-demo
python3 -m http.server 8000
# open http://localhost:8000
```

(`file://` will not work — the model is fetched over HTTP.) On GitHub Pages it is served at `<your-username>.github.io/<repo>/web-demo/`.

**Offline proof:** load the page once, switch the browser to Offline mode (DevTools → Network), and scan again — it still works.

## 📊 Measured results (v1 — tomato, PlantVillage)

Trained and evaluated on the PlantVillage tomato subset (10 classes, stratified 80/10/10 split):

| Model | Role | Test accuracy | Size |
|---|---|---|---|
| MobileNetV2 | Baseline | 99.95% | 9.2 MB |
| EfficientNet-B0 | Teacher | 99.95% | 16.4 MB |
| TinyCNN (0.39M params) | Student, distilled (T=4, α=0.7) | 99.34% | — |
| TinyCNN INT8 (static) | Deployment model | 99.40% | **0.41 MB**, 12.6 ms/image (CPU) |

Grad-CAM sanity check: 6/6 correct predictions with heatmaps localising on disease lesions.

> ⚠️ **Honest note:** PlantVillage images are controlled, laboratory-condition photos. These numbers do **not** establish field accuracy — measuring and closing the lab-to-field gap is an explicit part of the roadmap below.

## 🗺️ Roadmap

- **Multi-crop v2 model** — 5 crops, 25 classes (tomato, potato, cotton, maize, grape), trained with field-condition images and field-simulation augmentation; per-crop accuracy reported, lab and field side by side
- **Android app** — ExecuTorch vision + on-device Llama advisory (above)
- **Severity levels** — OpenCV lesion-coverage estimation, validated against hand-labelled photos
- **Pest detection** — a subset of common pests (IP102) as a gated stretch goal

## 👥 Team

Indrajit Biswas (lead) · Parinidhi Jain · Soumyajit Chatterjee
MCKV Institute of Engineering, Liluah, Howrah, West Bengal, India

## 📄 License

MIT
