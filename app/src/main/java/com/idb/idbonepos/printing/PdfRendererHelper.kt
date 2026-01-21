package com.idb.idbonepos.printing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

object PdfRendererHelper {
    fun renderFirstPage(inputStream: InputStream, targetWidthPx: Int): Bitmap {
        val file = File.createTempFile("temp", ".pdf")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(pfd).use { renderer ->
            renderer.openPage(0).use { page ->
                val scale = targetWidthPx.toFloat() / page.width
                val targetHeightPx = (page.height * scale).roundToInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(
                    targetWidthPx.coerceAtLeast(1),
                    targetHeightPx,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                return bitmap
            }
        }
    }
}
