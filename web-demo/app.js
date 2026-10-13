const MODEL_URL = "./student.onnx";
const IMG_SIZE = 224;
const MEAN = [0.485, 0.456, 0.406];
const STD = [0.229, 0.224, 0.225];

let session = null;

const $ = (id) => document.getElementById(id);
const statusText = $("status-text");

function setStatus(html, done = false) {
  statusText.innerHTML = html;
  if (done) $("model-status").classList.add("ready");
}

async function initModel() {
  try {
    ort.env.wasm.wasmPaths = "./";
    // single thread: GitHub Pages doesn't send the COOP/COEP headers
    // that multi-threaded WebAssembly requires
    ort.env.wasm.numThreads = 1;
    session = await ort.InferenceSession.create(MODEL_URL, {
      executionProviders: ["wasm"],
    });
    setStatus("✅ AI model loaded — <strong>fully offline</strong>, ready to scan.", true);
    $("upload-section").classList.remove("hidden");
  } catch (e) {
    console.error(e);
    setStatus("❌ Could not load the AI model. Make sure <code>student.onnx</code> is in the repo root.");
  }
}

function preprocess(img) {
  // cover-fit crop to square, then resize to 224
  const canvas = document.createElement("canvas");
  canvas.width = IMG_SIZE;
  canvas.height = IMG_SIZE;
  const ctx = canvas.getContext("2d");
  const s = Math.max(IMG_SIZE / img.width, IMG_SIZE / img.height);
  const w = img.width * s, h = img.height * s;
  ctx.drawImage(img, (IMG_SIZE - w) / 2, (IMG_SIZE - h) / 2, w, h);
  const px = ctx.getImageData(0, 0, IMG_SIZE, IMG_SIZE).data;
  const out = new Float32Array(3 * IMG_SIZE * IMG_SIZE);
  for (let i = 0; i < IMG_SIZE * IMG_SIZE; i++) {
    out[i] = (px[i * 4] / 255 - MEAN[0]) / STD[0];
    out[IMG_SIZE * IMG_SIZE + i] = (px[i * 4 + 1] / 255 - MEAN[1]) / STD[1];
    out[2 * IMG_SIZE * IMG_SIZE + i] = (px[i * 4 + 2] / 255 - MEAN[2]) / STD[2];
  }
  return new ort.Tensor("float32", out, [1, 3, IMG_SIZE, IMG_SIZE]);
}

function softmax(logits) {
  const m = Math.max(...logits);
  const exps = logits.map((v) => Math.exp(v - m));
  const s = exps.reduce((a, b) => a + b, 0);
  return exps.map((v) => v / s);
}

async function analyze(img, previewURL) {
  $("upload-section").classList.add("hidden");
  $("result-section").classList.remove("hidden");
  $("preview").src = previewURL;
  $("disease-name").textContent = "Analyzing…";
  $("conf-fill").style.width = "0%";
  $("conf-text").textContent = "";
  $("top3").innerHTML = "";
  $("advisory").innerHTML = "";

  // let the UI paint before heavy inference
  await new Promise((r) => setTimeout(r, 60));

  const t0 = performance.now();
  const feeds = {};
  feeds[session.inputNames[0]] = preprocess(img);
  const results = await session.run(feeds);
  const probs = softmax(Array.from(results[session.outputNames[0]].data));
  const ms = (performance.now() - t0).toFixed(0);

  const ranked = probs.map((p, i) => [p, i]).sort((a, b) => b[0] - a[0]);
  const [pTop, iTop] = ranked[0];
  const folder = LABELS[iTop];
  const name = prettyName(folder);
  const adv = ADVISORIES[folder] || { severity: "—", symptoms: "—", actions: [] };

  $("disease-name").textContent = name;
  $("conf-fill").style.width = `${(pTop * 100).toFixed(1)}%`;
  $("conf-text").textContent = `${(pTop * 100).toFixed(1)}% · ${ms} ms on-device`;
  $("top3").innerHTML =
    "<p class='label'>ALSO CONSIDERED</p>" +
    ranked
      .slice(1, 3)
      .map(([p, i]) => `<div class="top3-row"><span>${prettyName(LABELS[i])}</span><span>${(p * 100).toFixed(1)}%</span></div>`)
      .join("");

  const sevClass = adv.severity.toLowerCase().includes("critical")
    ? "sev-critical" : adv.severity.toLowerCase().includes("high") ? "sev-high"
    : adv.severity.toLowerCase().includes("none") ? "sev-none" : "sev-med";

  $("advisory").innerHTML = `
    <div class="adv-head">
      <h3>🌿 Advisory</h3>
      <span class="sev ${sevClass}">${adv.severity}</span>
    </div>
    <p><strong>What it looks like:</strong> ${adv.symptoms}</p>
    <p><strong>What to do now:</strong></p>
    <ul>${adv.actions.map((a) => `<li>${a}</li>`).join("")}</ul>`;
}

function handleFile(file) {
  if (!file || !file.type.startsWith("image/")) return;
  const url = URL.createObjectURL(file);
  const img = new Image();
  img.onload = () => analyze(img, url);
  img.src = url;
}

window.addEventListener("DOMContentLoaded", () => {
  initModel();
  const dz = $("dropzone"), fi = $("file-input");
  dz.addEventListener("click", () => fi.click());
  fi.addEventListener("change", (e) => handleFile(e.target.files[0]));
  ["dragover", "dragenter"].forEach((ev) =>
    dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.add("drag"); }));
  ["dragleave", "drop"].forEach((ev) =>
    dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.remove("drag"); }));
  dz.addEventListener("drop", (e) => handleFile(e.dataTransfer.files[0]));

  const ci = $("camera-input");
  $("camera-btn").addEventListener("click", () => ci.click());
  ci.addEventListener("change", (e) => handleFile(e.target.files[0]));

  $("again-btn").addEventListener("click", () => {
    $("result-section").classList.add("hidden");
    $("upload-section").classList.remove("hidden");
    fi.value = ""; ci.value = "";
  });
});
