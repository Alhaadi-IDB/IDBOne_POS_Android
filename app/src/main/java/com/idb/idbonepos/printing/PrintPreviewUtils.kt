package com.idb.idbonepos.printing

import android.content.Context
import com.idb.idbonepos.model.PrintSettings
import java.io.ByteArrayInputStream
import java.net.URL

fun printGraphicTestPageFromUrl(
    context: Context,
    settings: PrintSettings,
    name: String,
    address: String,
    pdfUrl: String
): Boolean {
    return try {
        val url = URL(pdfUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val inputStream = connection.getInputStream()
        val pdfBytes = inputStream.readBytes()
        inputStream.close()

        val pdfInputStream = ByteArrayInputStream(pdfBytes)
        val bitmap = PdfRendererHelper.renderFirstPage(pdfInputStream, settings.widthDots)
        val initCommands = EscPosCommands.parseHexString(settings.initialCommands)
        val cutterCommands = EscPosCommands.parseHexString(settings.cutterCommands)
        val rasterChunks = EscPosRasterizer.bitmapToRasterChunks(bitmap)
        //val chunks = listOf(initCommands) + rasterChunks//todo -> disable cut paper
        val chunks = listOf(initCommands) + rasterChunks + listOf(cutterCommands)

        if (name == "Ethernet Printer") {
            EthernetPrinter().print(address, chunks)
        } else {
            BluetoothPrinter().print(address, chunks)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun downloadAndRenderPdfPreview(settings: PrintSettings, pdfUrl: String): android.graphics.Bitmap? {
    return try {
        val url = URL(pdfUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val inputStream = connection.getInputStream()
        val pdfBytes = inputStream.readBytes()
        inputStream.close()

        val pdfInputStream = ByteArrayInputStream(pdfBytes)
        PdfRendererHelper.renderFirstPage(pdfInputStream, settings.widthDots)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
