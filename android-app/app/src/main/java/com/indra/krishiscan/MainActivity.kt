package com.indra.krishiscan

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The two models live here, ONCE per app process — not once per screen.
 * Before this, every time the screen was re-created (back + reopen, rotate,
 * dark-mode switch) a SECOND 1.1 GB Llama was loaded next to the first one.
 * [busy] makes sure only one scan / one Llama generation runs at a time:
 * LlmModule must never be asked to generate twice at once.
 */
object Engines {
    @Volatile var vision: VisionEngine? = null
    @Volatile var advisory: AdvisoryEngine? = null
    val busy = AtomicBoolean(false)
    private val started = AtomicBoolean(false)

    fun ensureLoading(appContext: Context) {
        if (!started.compareAndSet(false, true)) return
        Thread {
            try {
                vision = VisionEngine(appContext)
                Log.d("KrishiScan", "Vision engine ready")
            } catch (t: Throwable) {
                Log.e("KrishiScan", "Vision engine failed to load", t)
            }
        }.start()
        Thread {
            try {
                val adv = AdvisoryEngine(appContext)
                adv.load()
                advisory = adv
                Log.d("KrishiScan", "Advisory engine ready in ${adv.loadTimeMs} ms")
            } catch (t: Throwable) {
                // Llama files missing (e.g. a phone that only got the APK) or
                // load failed: the app keeps working on verified static advice.
                Log.e("KrishiScan", "Advisory engine failed to load", t)
            }
        }.start()
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultLabel: TextView
    private lateinit var resultDetails: TextView
    private lateinit var capturedImage: ImageView
    private lateinit var advisoryText: TextView
    private lateinit var severityChip: TextView
    private lateinit var regionSpinner: Spinner
    private lateinit var languageSpinner: Spinner
    private lateinit var btnCapture: Button

    private var imageCapture: ImageCapture? = null

    // Selector state — fed into PromptBuilder on every scan.
    private var selectedRegion = "West Bengal"
    private var selectedLanguage = "English"

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(
            this, "Camera permission is needed to scan leaves", Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashReporter()
        setContentView(R.layout.activity_main)
        applySystemBarInsets()
        showLastCrashIfAny()

        previewView = findViewById(R.id.previewView)
        resultLabel = findViewById(R.id.resultLabel)
        resultDetails = findViewById(R.id.resultDetails)
        capturedImage = findViewById(R.id.capturedImage)
        advisoryText = findViewById(R.id.advisoryText)
        severityChip = findViewById(R.id.severityChip)
        regionSpinner = findViewById(R.id.regionSpinner)
        languageSpinner = findViewById(R.id.languageSpinner)
        btnCapture = findViewById(R.id.btnCapture)

        setupSelectors()

        // Load both models once per process, in the background.
        Engines.ensureLoading(applicationContext)

        btnCapture.setOnClickListener { captureAndDiagnose() }

        // Start the camera now if permission is already granted, else ask for it.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Android 15+ draws apps edge-to-edge: without this, the header slides
     * under the status bar and the Scan button under the navigation buttons.
     * This pads the screen by exactly the size of the system bars / notch on
     * whatever phone the app runs on.
     */
    private fun applySystemBarInsets() {
        val root = findViewById<View>(R.id.root)
        val base = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(
                base + bars.left, base + bars.top, base + bars.right, base + bars.bottom
            )
            insets
        }
    }

    // ---- DEBUG ONLY: crash reporter. Remove before the 27 Nov code freeze. ----
    // Saves the real error to a file when the app dies, and shows it on the
    // next launch — so a crash tells us its cause instead of just closing.
    // (It cannot catch crashes inside native code or out-of-memory kills.)
    private fun crashFile() = File(filesDir, "last_crash.txt")

    private fun installCrashReporter() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is CrashSaver) return
        Thread.setDefaultUncaughtExceptionHandler(CrashSaver(crashFile(), previous))
    }

    private class CrashSaver(
        private val file: File,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                file.writeText("Thread: ${t.name}\n\n$sw")
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(t, e)
        }
    }

    private fun showLastCrashIfAny() {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val f = crashFile()
        if (!debuggable || !f.exists()) return
        val text = f.readText().take(4000)
        f.delete()
        AlertDialog.Builder(this)
            .setTitle("Last crash (debug)")
            .setMessage(text)
            .setPositiveButton("OK", null)
            .show()
    }
    // ---- end DEBUG ONLY ----

    /**
     * Decodes the camera JPEG at a small size. The camera hands over a
     * 12-50 megapixel photo; decoding that in full costs 50-200 MB per scan
     * (and several times that again inside the severity step), which is how a
     * phone that already holds a 1.1 GB Llama runs out of memory. The model
     * only needs 224x224 and the severity step was validated on 256x256
     * photos, so the longest side is brought down to 512 px.
     */
    private fun decodeSmall(bytes: ByteArray, maxSide: Int = 512): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            ?: throw IllegalStateException("Could not read the photo")
        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= maxSide) return decoded
        val scale = maxSide.toFloat() / longest
        val small = Bitmap.createScaledBitmap(
            decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true
        )
        if (small !== decoded) decoded.recycle()
        return small
    }

    private fun finishScan() {
        Engines.busy.set(false)
        runOnUiThread {
            btnCapture.isEnabled = true
            btnCapture.text = "Scan Leaf"
        }
    }

    /**
     * Region + language dropdowns. Region only changes the prompt framing
     * ("advice for farmers in X") — the verified facts stay identical, by
     * design (grounding). Language is passed straight to the prompt; Llama
     * generates the reply in it. Defaults: West Bengal + English.
     */
    private fun setupSelectors() {
        val regions = listOf(
            "West Bengal", "Odisha", "Bihar", "Jharkhand",
            "Uttar Pradesh", "Maharashtra", "Punjab", "Tamil Nadu",
        )
        regionSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, regions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        regionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long,
            ) {
                selectedRegion = regions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val languages = listOf("English", "Hindi", "Bengali")
        languageSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, languages
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long,
            ) {
                selectedLanguage = languages[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                Log.e("KrishiScan", "Camera bind failed", e)
                Toast.makeText(this, "Camera failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndDiagnose() {
        val capture = imageCapture ?: run {
            Toast.makeText(this, "Camera not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val vision = Engines.vision ?: run {
            Toast.makeText(this, "Model still loading, try again in a second", Toast.LENGTH_SHORT).show()
            return
        }
        // One scan at a time — the button stays locked until the advice is
        // finished too, so Llama is never asked to generate twice at once.
        if (!Engines.busy.compareAndSet(false, true)) {
            Toast.makeText(this, "Still working on the last scan", Toast.LENGTH_SHORT).show()
            return
        }
        btnCapture.isEnabled = false
        btnCapture.text = "Analysing…"
        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap: Bitmap = try {
                        // ImageCapture returns JPEG bytes — decode them SMALL.
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        decodeSmall(bytes)
                    } catch (t: Throwable) {
                        Log.e("KrishiScan", "Photo decode failed", t)
                        Toast.makeText(
                            this@MainActivity, "Photo failed: ${t.message}", Toast.LENGTH_LONG
                        ).show()
                        finishScan()
                        return
                    } finally {
                        image.close()
                    }
                    capturedImage.setImageBitmap(bitmap)
                    Thread {
                        try {
                            val (label, conf, ms) = vision.predict(bitmap)
                            // Severity (v2): measure the lesion coverage on THIS leaf. Wrapped so a
                            // severity failure never breaks the diagnosis.
                            val severity = try {
                                SeverityEngine.analyse(bitmap).also { it.overlay.recycle() }
                            } catch (t: Throwable) {
                                Log.e("KrishiScan", "Severity failed", t)
                                null
                            }
                            runOnUiThread {
                                resultLabel.text = label
                                    .replace("___", " — ")
                                    .replace('_', ' ')
                                resultDetails.text =
                                    "Confidence: ${(conf * 100).toInt()}%  •  ${ms}ms"
                                // Severity chip: green / orange / red by band. Hidden for
                                // healthy leaves (the diagnosis says "healthy") and when
                                // the measurement failed.
                                if (severity != null && label != "Tomato___healthy") {
                                    severityChip.visibility = View.VISIBLE
                                    severityChip.text = "${severity.band} • ${"%.1f".format(severity.coveragePercent)}%"
                                    val chipColor = when (severity.band) {
                                        "Mild" -> 0xFF2E7D32.toInt()
                                        "Moderate" -> 0xFFEF6C00.toInt()
                                        else -> 0xFFC62828.toInt()
                                    }
                                    severityChip.background = GradientDrawable().apply {
                                        shape = GradientDrawable.RECTANGLE
                                        cornerRadius = 48f
                                        setColor(chipColor)
                                    }
                                } else {
                                    severityChip.visibility = View.GONE
                                }
                                btnCapture.text = "Writing advice…"
                                Log.d("KrishiScan", "Vision: $label conf=$conf ms=$ms")
                            }
                            // Advisory: grounded Llama generation, static fallback on failure.
                            runAdvisory(label, conf, severity)
                        } catch (t: Throwable) {
                            Log.e("KrishiScan", "Vision failed", t)
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity, "Scan failed: ${t.message}", Toast.LENGTH_LONG
                                ).show()
                            }
                        } finally {
                            finishScan()
                        }
                    }.start()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("KrishiScan", "Capture failed", exception)
                    Toast.makeText(
                        this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_LONG
                    ).show()
                    finishScan()
                }
            }
        )
    }

    /**
     * Runs the advisory on the calling (background) thread.
     *
     * Bengali/Hindi: the verified TRANSLATED static advice is shown directly —
     * Llama 1B's generation in these languages tested garbled on-device
     * (20 Nov 2026), and it is also instant (no ~13 s generation wait).
     *
     * English: grounded Llama generation, capped at 120 new tokens (~13 s
     * worst case at the measured 9.4 tok/s). If Llama isn't loaded yet or
     * generation fails, the verified static advice is shown instead — the
     * farmer never gets an empty screen.
     */
    private fun runAdvisory(label: String, confidence: Float, severity: SeverityResult? = null) {
        if (selectedLanguage != "English") {
            runOnUiThread {
                advisoryText.text = PromptBuilder.staticAdvisory(label, selectedLanguage)
            }
            return
        }
        val adv = Engines.advisory
        if (adv == null) {
            runOnUiThread {
                advisoryText.text = PromptBuilder.staticAdvisory(label, selectedLanguage) +
                    "\n\n(Adviser still loading — showing verified advice.)"
            }
            return
        }
        runOnUiThread { advisoryText.text = "Writing advice…" }
        try {
            val prompt = PromptBuilder.buildPrompt(
                label, confidence, selectedRegion, selectedLanguage,
                severityOverride = severity?.bandForPrompt
            )
            val result = adv.generate(prompt, maxNewTokens = 120)
            runOnUiThread {
                advisoryText.text = result.text.ifBlank {
                    PromptBuilder.staticAdvisory(label, selectedLanguage)
                }
                Log.d(
                    "KrishiScan",
                    "Advisory: ${result.outputTokens} tokens in ${result.totalMs} ms",
                )
            }
        } catch (e: Throwable) {
            Log.e("KrishiScan", "Advisory failed, using static advice", e)
            runOnUiThread {
                advisoryText.text = PromptBuilder.staticAdvisory(label, selectedLanguage) +
                    "\n\n(Adviser unavailable — showing verified advice.)"
            }
        }
    }
}
