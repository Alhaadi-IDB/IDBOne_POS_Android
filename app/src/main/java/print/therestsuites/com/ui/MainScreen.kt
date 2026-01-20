package print.therestsuites.com.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import print.therestsuites.com.PrintStatus
import print.therestsuites.com.model.PrintSettings
import print.therestsuites.com.model.PrintUiState
import print.therestsuites.com.ui.components.ModernToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: PrintUiState,
    settings: PrintSettings,
    hasBluetoothPermission: Boolean,
    needsBluetoothPermission: Boolean,
    onRequestBluetoothPermission: () -> Unit,
    onSelectPrinter: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    AppScaffold(
        topBar = {
            ModernToolbar(
                title = "Print Bridge",
                showBack = true,
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PrinterInfoCard(settings = settings)
            StatusCard(uiState = uiState)

            if (needsBluetoothPermission && !hasBluetoothPermission) {
                Button(
                    onClick = onRequestBluetoothPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Bluetooth Permission")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onSelectPrinter,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select Printer")
                }
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Configuration")
                }
            }
        }
    }
}

@Composable
private fun PrinterInfoCard(settings: PrintSettings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Printer", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = settings.printerName ?: "Not selected",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = settings.printerAddress ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCard(uiState: PrintUiState) {
    val statusLabel = when (uiState.status) {
        PrintStatus.Idle -> "Idle"
        PrintStatus.WaitingForPermission -> "Permission required"
        PrintStatus.Downloading -> "Downloading"
        PrintStatus.Printing -> "Printing"
        PrintStatus.Success -> "Success"
        PrintStatus.Error -> "Error"
    }

    val containerColor = when (uiState.status) {
        PrintStatus.Error -> MaterialTheme.colorScheme.errorContainer
        PrintStatus.Success -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (uiState.status) {
        PrintStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        PrintStatus.Success -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Status", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = statusLabel, style = MaterialTheme.typography.bodyLarge)
            if (uiState.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = uiState.message, style = MaterialTheme.typography.bodySmall)
            }
            if (!uiState.pdfId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "PDF ID: ${uiState.pdfId}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
