package com.idb.idbonepos.printing

import android.content.Context
import com.idb.idbonepos.model.PrintSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun printTestPage(name: String, address: String) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val content = "Receipt $timestamp\n"
    val chunks = listOf(content.toByteArray())
    if (name == "Ethernet Printer") {
        EthernetPrinter().print(address, chunks)
    } else {
        BluetoothPrinter().print(address, chunks)
    }
}

fun printGraphicTestPage(context: Context, settings: PrintSettings, name: String, address: String) {
    val inputStream = context.assets.open("sample_orderchit.pdf")
    val bitmap = PdfRendererHelper.renderFirstPage(inputStream, settings.widthDots)
    val initCommands = EscPosCommands.parseHexString(settings.initialCommands)
    val cutterCommands = EscPosCommands.parseHexString(settings.cutterCommands)
    val rasterChunks = EscPosRasterizer.bitmapToRasterChunks(bitmap)
    val chunks = listOf(initCommands) + rasterChunks + listOf(cutterCommands)
    if (name == "Ethernet Printer") {
        EthernetPrinter().print(address, chunks)
    } else {
        BluetoothPrinter().print(address, chunks)
    }
}
