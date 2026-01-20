package print.therestsuites.com.model

import print.therestsuites.com.PrintStatus
import print.therestsuites.com.settings.PrintMode
import kotlin.math.roundToInt

enum class PrinterType {
    BLUETOOTH,
    ETHERNET
}

enum class PrinterProfileType {
    ORDER,
    RECEIPT
}

data class PrintSettings(
    val printerName: String? = null,
    val printerAddress: String? = null,
    val printMode: PrintMode = PrintMode.GRAPHIC,
    val printWidthMm: Int = 48,
    val printResolutionDpi: Int = 203,
    val initialCommands: String = "",
    val cutterCommands: String = "1D,56,42,00",
    val drawerCommands: String = "1B,70,00,19,FA"
) {
    val dotsPerMm: Int
        get() = when (printResolutionDpi) {
            203 -> 8
            300 -> 12
            else -> (printResolutionDpi / 25.4f).roundToInt().coerceAtLeast(1)
        }

    val widthDots: Int
        get() = (printWidthMm * dotsPerMm).coerceAtLeast(1)
}

data class PrintUiState(
    val status: PrintStatus = PrintStatus.Idle,
    val message: String = "",
    val pdfId: String? = null
)

data class PrinterOption(
    val name: String,
    val address: String
)

data class PrinterProfile(
    val id: String,
    val name: String,
    val address: String,
    val type: PrinterType,
    val printMode: PrintMode,
    val printWidthMm: Int,
    val printResolutionDpi: Int,
    val initialCommands: String,
    val cutterCommands: String,
    val drawerCommands: String,
    val graphicTestUrl: String = "",
    val profileType: PrinterProfileType = PrinterProfileType.ORDER
)

data class ResolutionOption(
    val dpi: Int,
    val dotsPerMm: Int
) {
    val label: String = "$dpi dpi ($dotsPerMm dots/mm)"
}

data class PrintReport(
    val success: Boolean,
    val message: String
)
