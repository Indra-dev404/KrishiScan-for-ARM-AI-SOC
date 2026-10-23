# KrishiScan — Sprint Log

## Fallback record (Sprint Day 3, logged 2026-10-23)

- **Web POC = frozen fallback / bonus mirror.** The Android app is the deliverable; the web demo is not developed further except as a mirror.
- **Repo:** https://github.com/Indra-dev404/KrishiScan-for-ARM-AI-SOC
- **Live demo:** https://indra-dev404.github.io/KrishiScan-for-ARM-AI-SOC/web-demo/
- **Fallback commit (working demo, incl. the in-app ONNX IR 10→9 patch):**
  `26692debaeb5f82c5d65241a55203093eaf0b4c9` — "Rename app (1).js to app.js", 2026-10-13
- **Git tag to create on that commit:** `web-poc-v1-fallback`
  (GitHub: Releases → Create a new release → new tag `web-poc-v1-fallback`. Or CLI:
  `git tag -a web-poc-v1-fallback -m "Web POC v1 — frozen fallback / bonus mirror"` then `git push origin web-poc-v1-fallback`.)

## Hardware (Sprint Day 2)

Target device: **Redmi 15 5G** — Android 16 (API 36), 8 GB RAM, arm64-v8a, 136.4 GB free.
Full inventory: `hardware-inventory.md`.
