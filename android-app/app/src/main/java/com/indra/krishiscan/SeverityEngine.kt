// SeverityEngine.kt — OpenCV severity v2 (KrishiScan, Nov 2026)
// Mirrors severity_prototype_v2.py EXACTLY: same HSV ranges, same morphology,
// same bands.
//
// v2 changes vs v1 (20 Nov evening, from the 15-photo validation — v1 agreed
// 5/15 and systematically read HIGH): the dark lesion range is now restricted
// to brown hues (H 5-35) instead of any hue, so shadowed dark-green tissue
// stops counting as lesion (healthy photo 10: 9.8% -> 2.4%); the yellow-brown
// saturation floor is raised 50 -> 65; the dark V cap is 70 -> 50.
// v2 re-run on the same 15 photos: agreement 10/15 (was 5/15). Synthetic
// accuracy unchanged (within ~1pp). KNOWN v2 LIMIT: small dark-spot diseases
// (bacterial spot) read near 0% — the spots share the leaf's hue and are only
// darker, so no hue range separates them; needs a texture/brightness-contrast
// channel in v3. Bands stay "proposed".
//
// What it does (v4 plan, Phase R3): after VisionEngine returns a diagnosis,
// segment the leaf, segment the lesion inside it, and report
//     coverage = lesion pixels / leaf pixels
// Bands are PROPOSED until the 15-photo validation count exists — the
// result screen and the prompt must both show "(proposed)":
//     Mild < 5%  |  Moderate 5-25%  |  Severe > 25%
//
// Replaces the FIXED per-disease severity in PromptBuilder (e.g. "High"
// for bacterial spot): that label describes the disease, not this photo.
// After this step, MainActivity passes result.bandForPrompt into the
// prompt instead of the fixed label, and shows the measured coverage
// on the result screen. See severity-v1-notes.md for the wiring snippet.
//
// HOW TO USE:
// 1. Add OpenCV to the app (NOT yet verified in this project — confirm the
//    version in Android Studio before building):
//      - Download the OpenCV Android SDK (4.x) from opencv.org/releases,
//        File > New > Import Module, pick the sdk/java folder, then in
//        app/build.gradle.kts: implementation(project(":opencv")).
//      - In MainActivity.onCreate, before first use:
//          OpenCVLoader.initLocal()  (or OpenCVLoader.initDebug())
//    If the SDK import fights you, do the PC prototype validation first —
//    the Android wiring is 10 minutes once OpenCV is in the project.
// 2. Copy this file next to VisionEngine.kt. PACKAGE LINE — must match
//    your MainActivity.kt (com.indra.krishiscan in the Day-4 project).
// 3. Call from the capture flow (see severity-v1-notes.md).
//
// Tested: the Python twin was run 20 Nov 2026 on synthetic leaves with
// known lesion coverage — Mild 3.9% (true 4.0), Moderate 15.6% (true 15.9),
// Severe 35.1% (true 35.6), healthy 0.0%; v2 re-check on red-brown,
// orange-brown and yellow-brown synthetic discs stayed within ~1pp.
// v2 validation on 15 real PlantVillage photos (hand-labelled first):
// agreement 10/15 (v1: 5/15); healthy leaves 2.4% / 0.0%. Known v2 limit:
// bacterial spot reads near 0% (spots share the leaf's hue). This Kotlin
// file was NOT run on-device (no OpenCV in the workspace project yet); the
// algorithm is identical, but the first on-phone run must be treated as a
// test, not a verified result.

package com.indra.krishiscan

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

data class SeverityResult(
    val coveragePercent: Double,   // lesion pixels / leaf pixels * 100
    val band: String,              // "Mild" | "Moderate" | "Severe"
    val leafPixels: Int,
    val lesionPixels: Int,
    val leafFound: Boolean,        // false = full-frame fallback was used
    val overlay: Bitmap,           // leaf outlined green, lesions tinted red
) {
    // What goes on the result screen and into the Llama prompt.
    // "(proposed)" stays until the 15-photo validation count exists.
    val displayText: String
        get() = "Severity: $band (proposed) — %.1f%% of leaf".format(coveragePercent)

    // What replaces the fixed per-disease label in PromptBuilder.
    val bandForPrompt: String
        get() = "$band (proposed, %.1f%% leaf coverage)".format(coveragePercent)
}

object SeverityEngine {

    // ---- Tunable constants — keep IDENTICAL in severity_prototype.py ----
    // OpenCV HSV: H 0-179, S 0-255, V 0-255.
    private val LEAF_LO = Scalar(35.0, 40.0, 40.0)   // healthy green
    private val LEAF_HI = Scalar(85.0, 255.0, 255.0)
    private val LESION_YB_LO = Scalar(5.0, 65.0, 40.0)  // yellow / brown (v2: S floor 50 -> 65)
    private val LESION_YB_HI = Scalar(35.0, 255.0, 255.0)
    private const val LESION_DARK_V_MAX = 50.0        // very dark necrotic (v2: 70 -> 50)
    private const val LESION_DARK_S_MIN = 30.0
    private const val MIN_LEAF_FRACTION = 0.05        // below this: full-frame fallback

    fun bandFor(coverage: Double): String = when {
        coverage < 5.0 -> "Mild"
        coverage < 25.0 -> "Moderate"
        else -> "Severe"
    }

    fun analyse(bitmap: Bitmap): SeverityResult {
        val tmp = Mat()
        Utils.bitmapToMat(bitmap, tmp)
        // Camera bitmaps are ARGB_8888 (4 channels); the HSV step needs 3.
        // Without this, cvtColor throws "invalid number of channels" on-device.
        val src = Mat()
        if (tmp.channels() == 4) {
            Imgproc.cvtColor(tmp, src, Imgproc.COLOR_RGBA2RGB)
        } else {
            tmp.copyTo(src)
        }
        tmp.release()
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV)

        val leafMask = segmentLeaf(hsv)
        var leafPixels = Core.countNonZero(leafMask)
        val totalPixels = src.rows() * src.cols()
        var leafFound = leafPixels >= MIN_LEAF_FRACTION * totalPixels
        val leaf = if (leafFound) {
            leafMask
        } else {
            // Lab photo where the leaf fills the frame but the green test
            // failed (e.g. heavy yellowing): measure against the full
            // frame and flag it, rather than returning a nonsense 0%.
            leafFound = false
            Mat(src.rows(), src.cols(), CvType.CV_8UC1, Scalar(255.0)).also {
                leafPixels = totalPixels
            }
        }

        val lesionMask = segmentLesion(hsv, leaf)
        val lesionPixels = Core.countNonZero(lesionMask)
        val coverage = 100.0 * lesionPixels / leafPixels.coerceAtLeast(1)

        val overlay = overlayBitmap(src, leaf, lesionMask)
        src.release(); hsv.release(); leafMask.release()
        lesionMask.release()
        return SeverityResult(
            coveragePercent = coverage,
            band = bandFor(coverage),
            leafPixels = leafPixels,
            lesionPixels = lesionPixels,
            leafFound = leafFound,
            overlay = overlay,
        )
    }

    private fun segmentLeaf(hsv: Mat): Mat {
        // Healthy-green mask. CLOSE only, no OPEN: opening erodes the leaf
        // edge before the contour is taken and shrank the leaf area ~11%
        // in the synthetic test. Largest contour wins; its CONVEX HULL is
        // filled, because a lesion touching the leaf edge indents the green
        // boundary and filling the raw contour cuts off that chunk of leaf
        // (synthetic severe case went from 28.9% to 35.1% measured, true
        // 35.6%, after switching to the hull). Tomato leaves are roughly
        // convex, so the hull only fills indentations.
        val mask = Mat()
        Core.inRange(hsv, LEAF_LO, LEAF_HI, mask)
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, Size(5.0, 5.0)
        )
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel)
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            mask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )
        val filled = Mat.zeros(mask.rows(), mask.cols(), CvType.CV_8UC1)
        if (contours.isNotEmpty()) {
            val biggest = contours.maxByOrNull { Imgproc.contourArea(it) }!!
            // OpenCV 4.11 Java has exactly ONE convexHull overload (verified
            // in the official 4.11.0 javadoc):
            //   convexHull(MatOfPoint points, MatOfInt hull)
            // The hull comes back as INDICES into the contour, so map them
            // back to points for drawContours.
            val hullIndices = MatOfInt()
            Imgproc.convexHull(biggest, hullIndices)
            val points = biggest.toArray()
            val hull = MatOfPoint(*hullIndices.toArray().map { points[it] }.toTypedArray())
            Imgproc.drawContours(filled, listOf(hull), -1, Scalar(255.0), -1)
            hullIndices.release(); hull.release()
        } else {
            mask.copyTo(filled)
        }
        contours.forEach { it.release() }
        mask.release(); kernel.release()
        return filled
    }

    private fun segmentLesion(hsv: Mat, leafMask: Mat): Mat {
        // Lesion = yellow-brown OR dark pixels, strictly inside the leaf.
        // v2: the dark range is restricted to brown hues (H 5-35) — v1 accepted
        // any hue, so shadowed dark-green leaf tissue counted as lesion
        // (a healthy leaf read 9.8% Moderate in v1; 2.4% in v2).
        val yellowBrown = Mat()
        Core.inRange(hsv, LESION_YB_LO, LESION_YB_HI, yellowBrown)
        val dark = Mat()
        Core.inRange(
            hsv,
            Scalar(5.0, LESION_DARK_S_MIN, 0.0),
            Scalar(35.0, 255.0, LESION_DARK_V_MAX),
            dark,
        )
        val lesion = Mat()
        Core.bitwise_or(yellowBrown, dark, lesion)
        Core.bitwise_and(lesion, lesion, lesion, leafMask)
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0)
        )
        Imgproc.morphologyEx(lesion, lesion, Imgproc.MORPH_OPEN, kernel)
        yellowBrown.release(); dark.release(); kernel.release()
        return lesion
    }

    private fun overlayBitmap(src: Mat, leafMask: Mat, lesionMask: Mat): Bitmap {
        val out = Mat()
        src.copyTo(out)
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(
            leafMask, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )
        Imgproc.drawContours(out, contours, -1, Scalar(0.0, 255.0, 0.0), 2)
        contours.forEach { it.release() }
        // Tint lesions red, blended over the original.
        val red = Mat(src.rows(), src.cols(), src.type(), Scalar(0.0, 0.0, 255.0))
        val lesionBgr = Mat()
        Core.bitwise_and(red, red, lesionBgr, lesionMask)
        Core.addWeighted(out, 1.0, lesionBgr, 0.45, 0.0, out)
        red.release(); lesionBgr.release()
        val bmp = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(out, bmp)
        out.release()
        return bmp
    }
}
