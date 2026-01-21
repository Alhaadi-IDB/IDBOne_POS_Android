package print.therestsuites.com.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import print.therestsuites.com.model.PrintSettings
import print.therestsuites.com.model.PrinterProfile
import print.therestsuites.com.model.PrinterProfileType
import print.therestsuites.com.model.PrinterType
import print.therestsuites.com.model.ResolutionOption
import print.therestsuites.com.settings.PrintMode
import print.therestsuites.com.settings.SettingsRepository
import print.therestsuites.com.ui.components.ModernToolbar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSelectionScreen(
    hasBluetoothPermission: Boolean,
    needsBluetoothPermission: Boolean,
    onRequestBluetoothPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settingsFlow.collectAsState(initial = PrintSettings())
    val savedPrinters by repository.printersFlow.collectAsState(initial = emptyList())
    val selectedPrinterId by repository.selectedPrinterIdFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var ipAddress by remember { mutableStateOf("") }
    var ethernetInput by remember { mutableStateOf("") }
    var printModeInput by remember { mutableStateOf(settings.printMode) }
    var printWidthInput by remember { mutableStateOf(settings.printWidthMm) }
    var printResolutionInput by remember { mutableStateOf(settings.printResolutionDpi) }
    var printerNameInput by remember { mutableStateOf("Ethernet Printer") }
    var addPrinterError by remember { mutableStateOf<String?>(null) }
    var showAddPrinterDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val printModes = listOf(PrintMode.GRAPHIC, PrintMode.TEXT)
    val printWidths = listOf(48, 58, 64, 72, 80)
    val printResolutions = listOf(
        ResolutionOption(203, 8),
        ResolutionOption(300, 12)
    )
    val selectedSavedPrinter = remember(savedPrinters, selectedPrinterId) {
        savedPrinters.firstOrNull { it.id == selectedPrinterId }
    }

    LaunchedEffect(hasBluetoothPermission) {
        if (!needsBluetoothPermission || hasBluetoothPermission) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null) {
                errorMessage = "Bluetooth is not available on this device."
                devices = emptyList()
            } else {
                devices = adapter.bondedDevices?.toList()?.sortedBy { it.name ?: it.address } ?: emptyList()
            }
        }
    }

    LaunchedEffect(Unit) {
        ipAddress = repository.getEthernetPrinterIP()
        ethernetInput = ipAddress
    }
    LaunchedEffect(showAddPrinterDialog) {
        if (showAddPrinterDialog) {
            ethernetInput = ipAddress
            printerNameInput = "Ethernet Printer"
            printModeInput = settings.printMode
            printWidthInput = settings.printWidthMm
            printResolutionInput = settings.printResolutionDpi
            addPrinterError = null
        }
    }

    AppScaffold(
        topBar = {
            ModernToolbar(
                title = "Manage printers",
                showBack = true,
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPrinterDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add printer"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { errorMessage = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }

            if (needsBluetoothPermission && !hasBluetoothPermission) {
                Text(
                    text = "Bluetooth permission is required to list paired printers.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = onRequestBluetoothPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Grant Bluetooth Permission", color = MaterialTheme.colorScheme.onPrimary)
                }
                return@Column
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Saved printers", style = MaterialTheme.typography.titleMedium)
                    if (savedPrinters.isEmpty()) {
                        Text(
                            text = "No saved printers yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        savedPrinters.forEach { printer ->
                            SavedPrinterRow(
                                printer = printer,
                                onEdit = {
                                    scope.launch {
                                        repository.selectPrinter(printer)
                                        onOpenSettings()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            if (devices.isNotEmpty())  {
                devices.forEach { device ->
                    PrinterRow(
                        name = device.name ?: "Unknown",
                        address = device.address,
                        onClick = {
                            scope.launch {
                                val existing = savedPrinters.firstOrNull { it.address == device.address }
                                val profile = PrinterProfile(
                                    id = existing?.id ?: device.address,
                                    name = device.name ?: "Unknown",
                                    address = device.address,
                                    type = PrinterType.BLUETOOTH,
                                    printMode = settings.printMode,
                                    printWidthMm = settings.printWidthMm,
                                    printResolutionDpi = settings.printResolutionDpi,
                                    initialCommands = settings.initialCommands,
                                    cutterCommands = settings.cutterCommands,
                                    drawerCommands = settings.drawerCommands,
                                    graphicTestUrl = ""
                                )
                                repository.upsertPrinter(profile, select = true)
                                onOpenSettings()
                            }
                        },
                        actionLabel = "Configure"
                    )
                }
            }
        }
    }

    if (showAddPrinterDialog) {
        AlertDialog(
            onDismissRequest = { showAddPrinterDialog = false },
            title = { Text("Add printer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the Ethernet printer IP address and default configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = printerNameInput,
                        onValueChange = { printerNameInput = it },
                        label = { Text("Printer name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ethernetInput,
                        onValueChange = {
                            ethernetInput = it
                            addPrinterError = null
                        },
                        label = { Text("IP address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    if (addPrinterError != null) {
                        Text(
                            text = addPrinterError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    DialogDropdownSetting(
                        label = "Print mode",
                        value = printModeInput.name.lowercase().replaceFirstChar { it.uppercase() },
                        options = printModes.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                        onOptionSelected = { index -> printModeInput = printModes[index] }
                    )
                    DialogDropdownSetting(
                        label = "Print width",
                        value = "${printWidthInput} mm",
                        options = printWidths.map { "$it mm" },
                        onOptionSelected = { index -> printWidthInput = printWidths[index] }
                    )
                    val currentResolution = printResolutions.firstOrNull { it.dpi == printResolutionInput }
                    DialogDropdownSetting(
                        label = "Print resolution",
                        value = currentResolution?.label ?: "${printResolutionInput} dpi",
                        options = printResolutions.map { it.label },
                        onOptionSelected = { index -> printResolutionInput = printResolutions[index].dpi }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = ethernetInput.trim()
                        if (value.isBlank()) {
                            addPrinterError = "Please enter a valid IP address."
                            return@Button
                        }
                        scope.launch {
                            val profile = PrinterProfile(
                                id = UUID.randomUUID().toString(),
                                name = printerNameInput.ifBlank { "Ethernet Printer" },
                                address = value,
                                type = PrinterType.ETHERNET,
                                printMode = printModeInput,
                                printWidthMm = printWidthInput,
                                printResolutionDpi = printResolutionInput,
                                initialCommands = settings.initialCommands,
                                cutterCommands = settings.cutterCommands,
                                drawerCommands = settings.drawerCommands,
                                graphicTestUrl = ""
                            )
                            repository.upsertPrinter(profile, select = true)
                            repository.saveEthernetPrinterIP(value)
                            ipAddress = value
                            showAddPrinterDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPrinterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
private fun PrinterRow(
    name: String,
    address: String,
    onClick: () -> Unit,
    actionLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(text = address, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(actionLabel, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun SavedPrinterRow(
    printer: PrinterProfile,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (printer.profileType) {
                        PrinterProfileType.ORDER -> MaterialTheme.colorScheme.primaryContainer
                        PrinterProfileType.RECEIPT -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = printer.profileType.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(text = printer.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = printer.address, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = printer.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Manage", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogDropdownSetting(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onOptionSelected(index)
                    }
                )
            }
        }
    }
}
