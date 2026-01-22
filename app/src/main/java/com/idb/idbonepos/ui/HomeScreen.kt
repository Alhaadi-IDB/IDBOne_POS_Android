package com.idb.idbonepos.ui

import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.Color
import com.idb.idbonepos.printing.printGraphicTestPage
import com.idb.idbonepos.printing.printTestPage
import com.idb.idbonepos.model.PrintSettings
import com.idb.idbonepos.model.PrinterType
import com.idb.idbonepos.settings.SettingsRepository
import com.idb.idbonepos.ui.components.ModernToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenConfiguration: () -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onOpenPrinter: () -> Unit,
    onOpenWebView: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val settings by repository.settingsFlow.collectAsState(initial = PrintSettings())
    val printerProfiles by repository.printersFlow.collectAsState(initial = emptyList())
    val selectedPrinterId by repository.selectedPrinterIdFlow.collectAsState(initial = null)
    
    // Get app version
    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }
    var savedUrl by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }
    var printerError by remember { mutableStateOf<String?>(null) }
    var ethernetIp by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var optionsExpanded by remember { mutableStateOf(false) }
    var printerExpanded by remember { mutableStateOf(false) }
    var selectedPrinterIndex by remember { mutableStateOf(0) }
    val isUrlValid = remember(urlInput) { normalizeUrl(urlInput) != null }

    LaunchedEffect(Unit) {
        savedUrl = repository.getWebUrl()
        urlInput = savedUrl
        ethernetIp = repository.getEthernetPrinterIP()
    }

    LaunchedEffect(settings.printerAddress) {
        ethernetIp = repository.getEthernetPrinterIP()
    }

    LaunchedEffect(printerProfiles, selectedPrinterId) {
        if (printerProfiles.isNotEmpty()) {
            val index = printerProfiles.indexOfFirst { it.id == selectedPrinterId }
            selectedPrinterIndex = if (index >= 0) index else 0
        }
    }

    AppScaffold(
        topBar = {
            ModernToolbar(
                title = "Home",
                actions = {
                    IconButton(
                        onClick = { optionsExpanded = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Open settings")
                    }
                    DropdownMenu(
                        expanded = optionsExpanded,
                        onDismissRequest = { optionsExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Manage printers") },
                            onClick = {
                                optionsExpanded = false
                                onOpenPrinter()
                            }
                        )
                        /*DropdownMenuItem(
                            text = { Text("Configuration") },
                            onClick = {
                                optionsExpanded = false
                                onOpenConfiguration()
                            }
                        )*/
                        DropdownMenuItem(
                            text = { Text("Default configuration") },
                            onClick = {
                                optionsExpanded = false
                                onOpenDefaultSettings()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedVisibility(
                    visible = printerError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    printerError?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                ElevatedButton(
                                    onClick = { printerError = null },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Load URL", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = {
                                urlInput = it
                                urlError = null
                            },
                            label = { Text("Web URL") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = urlError != null,
                            singleLine = true,
                            placeholder = { Text("https://example.com") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Create, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        if (urlError != null) {
                            Text(
                                text = urlError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ElevatedButton(
                                onClick = {
                                    try {
                                        val normalized = normalizeUrl(urlInput)
                                        if (normalized == null) {
                                            urlError = "Please enter a valid http or https URL."
                                            return@ElevatedButton
                                        }
                                        scope.launch {
                                            try {
                                                repository.saveWebUrl(normalized)
                                                savedUrl = normalized
                                                onOpenWebView()
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "HomeScreen",
                                                    "Error saving URL: ${e.message}",
                                                    e
                                                )
                                                urlError = "Failed to save URL: ${e.message}"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(
                                            "HomeScreen",
                                            "Error normalizing URL: ${e.message}",
                                            e
                                        )
                                        urlError = "Invalid URL format: ${e.message}"
                                    }
                                },
                                enabled = isUrlValid,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Open", color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Button(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                )
                            ) {
                                Text("Reset URL", color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                        if (savedUrl.isNotBlank()) {
                            Text(
                                text = "Current: $savedUrl",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Print Test", style = MaterialTheme.typography.titleMedium)
                        if (printerProfiles.isEmpty()) {
                            Text(
                                text = "No printer selected. Add a printer to continue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onOpenPrinter,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Add Printer", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = printerExpanded && printerProfiles.size > 1,
                                onExpandedChange = {
                                    if (printerProfiles.size > 1) {
                                        printerExpanded = !printerExpanded
                                    }
                                }
                            ) {
                                val selection = printerProfiles.getOrNull(selectedPrinterIndex)
                                OutlinedTextField(
                                    value = selection?.name ?: "Printer",
                                    onValueChange = {},
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Selected printer") },
                                    trailingIcon = {
                                        if (printerProfiles.size > 1) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = printerExpanded
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Send,
                                            contentDescription = null
                                        )
                                    },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = printerExpanded && printerProfiles.size > 1,
                                    onDismissRequest = { printerExpanded = false }
                                ) {
                                    printerProfiles.forEachIndexed { index, printer ->
                                        DropdownMenuItem(
                                            text = { Text(printer.name) },
                                            onClick = {
                                                printerExpanded = false
                                                selectedPrinterIndex = index
                                                scope.launch {
                                                    repository.selectPrinter(printer)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            val selectedPrinter = printerProfiles[selectedPrinterIndex]
                            Text(
                                text = selectedPrinter.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ElevatedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val error = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        val printerName =
                                                            if (selectedPrinter.type == PrinterType.ETHERNET) {
                                                                "Ethernet Printer"
                                                            } else {
                                                                selectedPrinter.name
                                                            }
                                                        printTestPage(
                                                            printerName,
                                                            selectedPrinter.address
                                                        )
                                                    }.exceptionOrNull()
                                                }
                                                if (error != null) {
                                                    Log.e(
                                                        "HomeScreen",
                                                        "Print test error: ${error.message}",
                                                        error
                                                    )
                                                    printerError =
                                                        "Failed to send test page: ${error.message ?: "Unknown error"}"
                                                }
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "HomeScreen",
                                                    "Error in idbonepos test: ${e.message}",
                                                    e
                                                )
                                                printerError =
                                                    "Failed to send test page: ${e.message}"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Print test", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                ElevatedButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val error = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        val printerName =
                                                            if (selectedPrinter.type == PrinterType.ETHERNET) {
                                                                "Ethernet Printer"
                                                            } else {
                                                                selectedPrinter.name
                                                            }
                                                        printGraphicTestPage(
                                                            context,
                                                            settings,
                                                            printerName,
                                                            selectedPrinter.address
                                                        )
                                                    }.exceptionOrNull()
                                                }
                                                if (error != null) {
                                                    Log.e(
                                                        "HomeScreen",
                                                        "Graphic test error: ${error.message}",
                                                        error
                                                    )
                                                    printerError =
                                                        "Failed to send graphic test page: ${error.message ?: "Unknown error"}"
                                                }
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "HomeScreen",
                                                    "Error in graphic test: ${e.message}",
                                                    e
                                                )
                                                printerError =
                                                    "Failed to send graphic test page: ${e.message}"
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                ) {
                                    Text(
                                        "Print Graphic Test",
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // App version at bottom right
            Text(
                text = "V $appVersion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            )
        }
    }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        "Reset URL",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        "Clear the saved URL so you can enter a new one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    ElevatedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    repository.saveWebUrl("")
                                    savedUrl = ""
                                    urlInput = ""
                                    showResetDialog = false
                                } catch (e: Exception) {
                                    Log.e("HomeScreen", "Error resetting URL: ${e.message}", e)
                                    urlError = "Failed to reset URL: ${e.message}"
                                    showResetDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

}
private fun normalizeUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    val uri = Uri.parse(withScheme)
    val scheme = uri.scheme?.lowercase()
    return if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()) {
        withScheme
    } else {
        null
    }
}
