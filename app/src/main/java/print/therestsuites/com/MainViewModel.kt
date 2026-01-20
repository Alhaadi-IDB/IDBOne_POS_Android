package print.therestsuites.com

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import print.therestsuites.com.model.PrintUiState
import print.therestsuites.com.printing.PrintJobRunner
import print.therestsuites.com.printing.PrintStage
import print.therestsuites.com.printing.UrlPrintService
import print.therestsuites.com.settings.SettingsRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val printJobRunner = PrintJobRunner(application)

    private val _uiState = MutableStateFlow(PrintUiState())
    val uiState: StateFlow<PrintUiState> = _uiState.asStateFlow()

    val settingsFlow = settingsRepository.settingsFlow

    private var pendingPdfId: String? = null

    fun printFromPdfId(pdfId: String) {
        if (pdfId.isBlank()) return
        pendingPdfId = pdfId
        viewModelScope.launch {
            startPrint(pdfId)
        }
    }

    fun onBluetoothPermissionGranted() {
        val pdfId = pendingPdfId ?: return
        viewModelScope.launch {
            startPrint(pdfId)
        }
    }

    fun clearStatus() {
        _uiState.value = PrintUiState()
    }

    fun printFromUrl(address: String, pdfUrl: String, type: String) {
        if (address.isBlank() || pdfUrl.isBlank()) {
            _uiState.value = PrintUiState(
                status = PrintStatus.Error,
                message = "Printer address and PDF URL are required."
            )
            return
        }
        viewModelScope.launch {
            startPrintFromUrl(address, pdfUrl, type)
        }
    }

    private suspend fun startPrint(pdfId: String) {
        val settings = settingsRepository.settingsFlow.first()
        if (!isBluetoothAvailable()) {
            _uiState.value = PrintUiState(
                status = PrintStatus.Error,
                message = "Bluetooth is unavailable or disabled.",
                pdfId = pdfId
            )
            return
        }
        if (!hasBluetoothPermission()) {
            _uiState.value = PrintUiState(
                status = PrintStatus.WaitingForPermission,
                message = "Bluetooth permission required to print.",
                pdfId = pdfId
            )
            return
        }
        if (settings.printerAddress.isNullOrBlank()) {
            _uiState.value = PrintUiState(
                status = PrintStatus.Error,
                message = "No printer configured. Select a paired printer first.",
                pdfId = pdfId
            )
            return
        }

        _uiState.value = PrintUiState(
            status = PrintStatus.Downloading,
            message = "Downloading PDF...",
            pdfId = pdfId
        )

        try {
            withContext(Dispatchers.IO) {
                printJobRunner.run(
                    pdfId = pdfId,
                    settings = settings,
                    onProgress = { stage -> updateProgress(pdfId, stage) }
                )
            }
            _uiState.value = PrintUiState(
                status = PrintStatus.Success,
                message = "Print job sent.",
                pdfId = pdfId
            )
        } catch (e: Exception) {
            _uiState.value = PrintUiState(
                status = PrintStatus.Error,
                message = e.message ?: "Printing failed.",
                pdfId = pdfId
            )
        }
    }

    private suspend fun startPrintFromUrl(address: String, pdfUrl: String, type: String) {
        val settings = settingsRepository.settingsFlow.first()
        val needsBluetooth = !UrlPrintService.isEthernet(type, address)
        if (needsBluetooth && !isBluetoothAvailable()) {
            _uiState.value = PrintUiState(
                status = PrintStatus.Error,
                message = "Bluetooth is unavailable or disabled."
            )
            return
        }
        if (needsBluetooth && !hasBluetoothPermission()) {
            _uiState.value = PrintUiState(
                status = PrintStatus.WaitingForPermission,
                message = "Bluetooth permission required to print."
            )
            return
        }

        _uiState.value = PrintUiState(
            status = PrintStatus.Downloading,
            message = "Downloading PDF..."
        )

        try {
            withContext(Dispatchers.IO) {
                UrlPrintService.printPdfUrl(
                    context = getApplication(),
                    settings = settings,
                    address = address,
                    type = type,
                    pdfUrl = pdfUrl
                )
            }
            _uiState.value = PrintUiState(
                status = PrintStatus.Success,
                message = "Print job sent."
            )
        } catch (e: Exception) {
            _uiState.value = PrintUiState(
                status = PrintStatus.Error,
                message = e.message ?: "Printing failed."
            )
        }
    }

    private fun updateProgress(pdfId: String, stage: PrintStage) {
        val status = when (stage) {
            PrintStage.Downloading -> PrintStatus.Downloading
            PrintStage.Rendering,
            PrintStage.Connecting,
            PrintStage.Sending -> PrintStatus.Printing
        }
        val message = when (stage) {
            PrintStage.Downloading -> "Downloading PDF..."
            PrintStage.Rendering -> "Rendering PDF..."
            PrintStage.Connecting -> "Connecting to printer..."
            PrintStage.Sending -> "Sending data to printer..."
        }
        _uiState.value = PrintUiState(
            status = status,
            message = message,
            pdfId = pdfId
        )
    }

    private fun isBluetoothAvailable(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false
        return adapter.isEnabled
    }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val permission = android.Manifest.permission.BLUETOOTH_CONNECT
        return ContextCompat.checkSelfPermission(getApplication(), permission) ==
            PackageManager.PERMISSION_GRANTED
    }
}

enum class PrintStatus {
    Idle,
    WaitingForPermission,
    Downloading,
    Printing,
    Success,
    Error
}
