package com.qb.secondbrain.context

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ScreenCaptureProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var rootInActiveWindowProvider: (() -> AccessibilityNodeInfo?)? = null

    fun setRootInActiveWindowProvider(callback: () -> AccessibilityNodeInfo?) {
        rootInActiveWindowProvider = callback
    }

    suspend fun captureScreen(): String? = withContext(Dispatchers.IO) {
        try {
            val rootNode = rootInActiveWindowProvider?.invoke()
                ?: return@withContext null

            val screenshotDir = File(context.cacheDir, "screenshots")
            if (!screenshotDir.exists()) {
                screenshotDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "screenshot_$timestamp.jpg"
            val outputFile = File(screenshotDir, fileName)

            val bounds = Rect()
            rootNode.getBoundsInScreen(bounds)
            val width = bounds.width().coerceAtLeast(1)
            val height = bounds.height().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            drawNodeToCanvas(rootNode, canvas)

            FileOutputStream(outputFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.flush()
            }

            bitmap.recycle()
            outputFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun drawNodeToCanvas(node: AccessibilityNodeInfo, canvas: Canvas) {
        if (node.isVisibleToUser) {
            val nodeBounds = Rect()
            node.getBoundsInScreen(nodeBounds)
            val text = node.text?.toString() ?: node.contentDescription?.toString()
            if (!text.isNullOrEmpty()) {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 24f
                    isAntiAlias = true
                }
                canvas.drawText(text, nodeBounds.left.toFloat() + 8f, nodeBounds.top.toFloat() + 24f, paint)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                drawNodeToCanvas(child, canvas)
            }
        }
    }
}
