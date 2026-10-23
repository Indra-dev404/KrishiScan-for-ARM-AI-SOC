# KrishiScan — Hardware Inventory

*Collected 2026-10-23 (POC sprint Day 2). Feeds the hardware gate: one target device is picked from this list, and all Llama/vision performance measurements are taken on that device only.*

| Device | Yours / Borrowed | RAM | Android / OS | API | ABI | Free storage |
|---|---|---|---|---|---|---|
| Redmi 15 5G | Mine | 8 GB physical (+4 GB virtual) | Android 16 | 36 | arm64-v8a ✅ (confirmed 2026-10-23) | 136.4 GB |

## Gate assessment (plan pass marks)

- **ABI arm64-v8a** — required for ExecuTorch Android. ✅ Confirmed 2026-10-23 (`getprop ro.product.cpu.abi` → `arm64-v8a`).
- **API ≥ 26** — ✅ API 36 (Android 16), well above the floor.
- **RAM ≥ 6 GB preferred** — ✅ 8 GB physical. (The "+4 GB" is storage-backed virtual RAM expansion; it does not help hold the model — the physical 8 GB is what counts, and it is comfortably above the ~1.9–2.3 GB resident size published for quantised Llama 3.2 1B under ExecuTorch, plus vision model + app.)
- **Free storage ≥ ~3 GB** — ✅ 136.4 GB (models total ~1.2 GB).

**Verdict: PASS.** This phone is the target device; no borrowed hardware or Raspberry Pi needed for the sprint.
