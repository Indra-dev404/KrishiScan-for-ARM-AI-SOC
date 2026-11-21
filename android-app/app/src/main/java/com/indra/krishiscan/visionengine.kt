package com.indra.krishiscan

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import kotlin.math.exp

class VisionEngine(context: Context) {

    // ONNX Runtime needs a real file path, so the asset is copied to internal storage first.
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        assetFilePath(context, "student.onnx"),
        OrtSession.SessionOptions()
    )

    val labels = arrayOf(
        "Tomato___Bacterial_spot", "Tomato___Early_blight", "Tomato___Late_blight",
        "Tomato___Leaf_Mold", "Tomato___Septoria_leaf_spot",
        "Tomato___Spider_mites Two-spotted_spider_mite", "Tomato___Target_Spot",
        "Tomato___Tomato_Yellow_Leaf_Curl_Virus", "Tomato___Tomato_mosaic_virus",
        "Tomato___healthy"
    )

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) return file.absolutePath
        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        return file.absolutePath
    }

    // Returns: predicted label, confidence (0-1), inference milliseconds.
    fun predict(bitmap: Bitmap): Triple<String, Float, Long> {
        val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val pixels = IntArray(224 * 224)
        resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)
        // Same preprocessing as training: RGB 0-1, then ImageNet mean/std, NCHW order.
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        val input = FloatArray(3 * 224 * 224)
        for (i in pixels.indices) {
            val p = pixels[i]
            input[i] = (((p shr 16) and 0xFF) / 255f - mean[0]) / std[0]
            input[224 * 224 + i] = (((p shr 8) and 0xFF) / 255f - mean[1]) / std[1]
            input[2 * 224 * 224 + i] = ((p and 0xFF) / 255f - mean[2]) / std[2]
        }
        val tensor = OnnxTensor.createTensor(env, java.nio.FloatBuffer.wrap(input), longArrayOf(1, 3, 224, 224))
        val start = System.nanoTime()
        val result = session.run(mapOf("input" to tensor))
        val output = (result[0].value as Array<FloatArray>)[0]
        val ms = (System.nanoTime() - start) / 1_000_000
        result.close()
        tensor.close()
        // Softmax for confidence; argmax for the winning class.
        val maxLogit = output.max()
        val exps = output.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sum = exps.sum()
        val top = exps.indices.maxByOrNull { exps[it] } ?: 0
        return Triple(labels[top], exps[top] / sum, ms)
    }
}
