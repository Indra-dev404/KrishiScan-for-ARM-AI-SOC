# KrishiScan — Web Demo

A fully-offline, in-browser version of KrishiScan's vision pipeline. This is a **bonus mirror** of the project; the main deliverable is the native Android app (see the repo root README).

## How it works

```
photo (upload / drag-drop / camera)
  → preprocessing in-page (cover-crop, resize 224×224, ImageNet mean/std normalisation)
  → ONNX Runtime Web (WebAssembly, bundled locally — no CDN)
  → distilled TinyCNN student (student.onnx, input [1,3,224,224] → 10 class logits)
  → diagnosis + confidence + top-3 + advisory card (advisories.js)
```

Everything — model, runtime, advisories — is served from these files. After the first load, the demo works with the network completely off.

## Files

| File | Purpose |
|---|---|
| `index.html` | The app page |
| `style.css` | Styling (mobile-first, farmer-simple) |
| `app.js` | Model loading, preprocessing, inference, UI |
| `labels.js` | The 10 tomato class names, in training order |
| `advisories.js` | Per-disease advisory database (symptoms, actions, severity) |
| `ort.min.js` + `ort-wasm-simd.wasm` | ONNX Runtime Web 1.17, bundled locally |
| `student.onnx` | Trained distilled student model (FP32 export) |

## Run it

```bash
cd web-demo
python3 -m http.server 8000
# open http://localhost:8000
```

Do not open `index.html` via `file://` — browsers block the model fetch. On GitHub Pages, this folder is served at `<your-username>.github.io/<repo>/web-demo/`.

## Notes

- **Single-threaded WASM on purpose:** `ort.env.wasm.numThreads = 1` in `app.js`, because GitHub Pages does not send the COOP/COEP headers that multi-threaded WebAssembly requires. The model is tiny, so single-thread inference is still fast.
- **Scope:** v1 recognises 10 tomato classes from the PlantVillage dataset (laboratory-condition images). Field photos may be less accurate — see the roadmap in the root README.
