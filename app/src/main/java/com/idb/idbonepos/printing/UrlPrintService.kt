package com.idb.idbonepos.printing

import android.content.Context
import com.idb.idbonepos.model.PrintSettings
import java.io.ByteArrayInputStream
import java.net.URL

object UrlPrintService {
    fun printPdfUrl(
        context: Context,
        settings: PrintSettings,
        address: String,
        type: String,
        pdfUrl: String
    ) {
        require(address.isNotBlank()) { "Printer address is required." }
        require(pdfUrl.isNotBlank()) { "PDF URL is required." }

      //val effectiveSettings = adjustSettingsForType(settings, type)---> to override setting
        val effectiveSettings = settings
        val pdfBytes = downloadPdf(pdfUrl)
        val pdfInputStream = ByteArrayInputStream(pdfBytes)
        val bitmap = PdfRendererHelper.renderFirstPage(pdfInputStream, effectiveSettings.widthDots)
        val rasterChunks = EscPosRasterizer.bitmapToRasterChunks(bitmap)
        val initCommands = EscPosCommands.parseHexString(effectiveSettings.initialCommands)
        val cutterCommands = EscPosCommands.parseHexString(settings.cutterCommands)
        //val chunks = listOf(initCommands) + rasterChunks//todo -> disable cut paper
        val chunks = listOf(initCommands) + rasterChunks + listOf(cutterCommands)

        if (isEthernet(type, address)) {
            EthernetPrinter().print(address, chunks)
        } else {
            BluetoothPrinter().print(address, chunks)
        }
    }

    private fun downloadPdf(pdfUrl: String): ByteArray {
        val url = URL(pdfUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.getInputStream().use { inputStream ->
            return inputStream.readBytes()
        }
    }

    fun isEthernet(type: String, address: String): Boolean {
        val normalized = type.trim().lowercase()
        return when {
            normalized == "ethernet" || normalized == "network" || normalized == "ip" || normalized == "ethernet printer"-> true
            normalized == "bluetooth" || normalized == "bt" -> false
            normalized.isBlank() -> !address.contains(":")
            else -> !address.contains(":")
        }
    }

    private fun adjustSettingsForType(settings: PrintSettings, type: String): PrintSettings {
        return when (type.trim().lowercase()) {
            "receipt" -> settings.copy(printResolutionDpi = 300, printWidthMm = 48)
            "order" -> settings.copy(printResolutionDpi = 203, printWidthMm = 64)
            else -> settings
        }
    }
}
