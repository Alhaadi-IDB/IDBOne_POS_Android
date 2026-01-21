package com.idb.idbonepos.printing

import android.content.Context
import com.idb.idbonepos.model.PrintSettings

class PrintJobRunner(private val context: Context) {
    private val pdfDownloader = PdfDownloader()
    private val printer = BluetoothPrinter()

    fun run(pdfId: String, settings: PrintSettings, onProgress: (PrintStage) -> Unit) {
        onProgress(PrintStage.Downloading)
        val pdfFile = pdfDownloader.downloadPdf(context, pdfId)

        onProgress(PrintStage.Rendering)
        val bitmap = pdfFile.inputStream().use {
            PdfRendererHelper.renderFirstPage(it, settings.widthDots)
        }
        val raster = EscPosRasterizer.bitmapToRasterBytes(bitmap)
        bitmap.recycle()

        val initCommands = EscPosCommands.parseHexString(settings.initialCommands)
        val cutterCommands = EscPosCommands.parseHexString(settings.cutterCommands)
        val drawerCommands = EscPosCommands.parseHexString(settings.drawerCommands)

        onProgress(PrintStage.Connecting)
        val address = settings.printerAddress
            ?: throw IllegalStateException("Printer address missing")
        val chunks = listOf(initCommands, raster, cutterCommands, drawerCommands)
        onProgress(PrintStage.Sending)
        printer.print(address, chunks)
    }
}
