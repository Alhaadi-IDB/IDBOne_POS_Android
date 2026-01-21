package com.idb.idbonepos.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.idb.idbonepos.settings.PrintMode
import com.idb.idbonepos.printing.downloadAndRenderPdfPreview
import com.idb.idbonepos.printing.printGraphicTestPage
import com.idb.idbonepos.printing.printGraphicTestPageFromUrl
import com.idb.idbonepos.printing.printTestPage
import com.idb.idbonepos.model.PrintSettings
import com.idb.idbonepos.model.PrinterProfile
import com.idb.idbonepos.model.PrinterProfileType
import com.idb.idbonepos.model.PrinterType
import com.idb.idbonepos.model.ResolutionOption
import com.idb.idbonepos.settings.SettingsRepository
import com.idb.idbonepos.ui.components.ModernToolbar
import com.idb.idbonepos.ui.components.RobotLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settingsFlow.collectAsState(initial = PrintSettings())
    val savedPrinters by repository.printersFlow.collectAsState(initial = emptyList())
    val selectedPrinterId by repository.selectedPrinterIdFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var printerExpanded by remember { mutableStateOf(false) }
    var pdfUrl by remember { mutableStateOf("") }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showPreview by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("Processing...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(settings.printMode) }
    var editWidth by remember { mutableStateOf(settings.printWidthMm) }
    var editResolution by remember { mutableStateOf(settings.printResolutionDpi) }
    var editInitialCommands by remember { mutableStateOf(settings.initialCommands) }
    var editCutterCommands by remember { mutableStateOf(settings.cutterCommands) }
    var editDrawerCommands by remember { mutableStateOf(settings.drawerCommands) }
    var editGraphicUrl by remember { mutableStateOf("") }
    var editProfileType by remember { mutableStateOf(PrinterProfileType.ORDER) }
    var editError by remember { mutableStateOf<String?>(null) }

    val printModes = listOf(PrintMode.GRAPHIC, PrintMode.TEXT)
    val profileTypes = listOf(PrinterProfileType.ORDER,PrinterProfileType.RECEIPT)
    val printWidths = listOf(48, 58, 64, 72, 80)
    val printResolutions = listOf(
        ResolutionOption(203, 8),
        ResolutionOption(300, 12)
    )
    val selectedPrinter = remember(savedPrinters, selectedPrinterId) {
        savedPrinters.firstOrNull { it.id == selectedPrinterId } ?: savedPrinters.firstOrNull()
    }

    LaunchedEffect(Unit) {
        pdfUrl = repository.getPdfUrl()
    }
    LaunchedEffect(selectedPrinter) {
        val printer = selectedPrinter
        if (printer != null) {
            editName = printer.name
            editAddress = printer.address
            editMode = printer.printMode
            editWidth = printer.printWidthMm
            editResolution = printer.printResolutionDpi
            editInitialCommands = printer.initialCommands
            editCutterCommands = printer.cutterCommands
            editDrawerCommands = printer.drawerCommands
            editGraphicUrl = printer.graphicTestUrl
            editProfileType = printer.profileType
            editError = null
        }
    }

    AppScaffold(
        topBar = {
            ModernToolbar(
                title = "Printer configuration",
                showBack = true,
                onBack = onBack,
                actions = {
                    if (selectedPrinter != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete printer",
                                tint = Color.Red
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { error ->
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
                                    onClick = { errorMessage = null },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        .clip(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Printer configuration", style = MaterialTheme.typography.titleMedium)
                        if (savedPrinters.isEmpty()) {
                            Text(
                                text = "No saved printers yet. Add one from Manage printers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            /*ExposedDropdownMenuBox(
                                expanded = printerExpanded && savedPrinters.size > 1,
                                onExpandedChange = {
                                    if (savedPrinters.size > 1) {
                                        printerExpanded = !printerExpanded
                                    }
                                }
                            ) {
                                val selection = selectedPrinter ?: savedPrinters.first()
                                OutlinedTextField(
                                    value = selection.name,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Selected printer") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = printerExpanded)
                                    },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = printerExpanded && savedPrinters.size > 1,
                                    onDismissRequest = { printerExpanded = false }
                                ) {
                                    savedPrinters.forEach { printer ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(printer.name) },
                                            onClick = {
                                                printerExpanded = false
                                                scope.launch {
                                                    repository.selectPrinter(printer)
                                                }
                                            }
                                        )
                                    }
                                }
                            }*/

                            val selected = selectedPrinter ?: savedPrinters.first()
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Printer name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownSetting(
                                label = "Print type",
                                value = editProfileType.name.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                options = profileTypes.map {
                                    it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
                                },
                                onOptionSelected = { index -> editProfileType = profileTypes[index] }
                            )
                            if (editProfileType == PrinterProfileType.RECEIPT) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(
                                            elevation = 2.dp,
                                            shape = RoundedCornerShape(12.dp),
                                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Info",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                        Text(
                                            text = "Shared printers are not supported for this print type.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = editAddress,
                                onValueChange = {
                                    editAddress = it
                                    editError = null
                                },
                                label = { Text("Printer address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = selected.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Printer type") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (editError != null) {
                                Text(
                                    text = editError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            DropdownSetting(
                                label = "Print mode",
                                value = editMode.name.lowercase().replaceFirstChar { it.uppercase() },
                                options = printModes.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                                onOptionSelected = { index -> editMode = printModes[index] }
                            )
                            DropdownSetting(
                                label = "Print width",
                                value = "${editWidth} mm",
                                options = printWidths.map { "$it mm" },
                                onOptionSelected = { index -> editWidth = printWidths[index] }
                            )
                            val currentResolution = printResolutions.firstOrNull { it.dpi == editResolution }
                            DropdownSetting(
                                label = "Print resolution",
                                value = currentResolution?.label ?: "${editResolution} dpi",
                                options = printResolutions.map { it.label },
                                onOptionSelected = { index -> editResolution = printResolutions[index].dpi }
                            )
                            CommandField(
                                label = "Initial ESC/POS commands",
                                value = editInitialCommands,
                                onValueChange = { editInitialCommands = it }
                            )
                            CommandField(
                                label = "Cutter ESC/POS commands",
                                value = editCutterCommands,
                                onValueChange = { editCutterCommands = it }
                            )
                            CommandField(
                                label = "Drawer ESC/POS commands",
                                value = editDrawerCommands,
                                onValueChange = { editDrawerCommands = it }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val printer = selected
                                        val previewProfile = printer.copy(
                                            name = editName.ifBlank { printer.name },
                                            address = editAddress.ifBlank { printer.address },
                                            printMode = editMode,
                                            printWidthMm = editWidth,
                                            printResolutionDpi = editResolution,
                                            initialCommands = editInitialCommands,
                                            cutterCommands = editCutterCommands,
                                            drawerCommands = editDrawerCommands,
                                            profileType = editProfileType
                                        )
                                        val name = if (previewProfile.type == PrinterType.ETHERNET) {
                                            "Ethernet Printer"
                                        } else {
                                            previewProfile.name
                                        }
                                        scope.launch {
                                            val error = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                                runCatching {
                                                    printTestPage(name, previewProfile.address)
                                                }.exceptionOrNull()
                                            }
                                            if (error != null) {
                                                errorMessage = "Failed to send test page."
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Print test")
                                }
                                Button(
                                    onClick = {
                                        val printer = selected
                                        val previewProfile = printer.copy(
                                            name = editName.ifBlank { printer.name },
                                            address = editAddress.ifBlank { printer.address },
                                            printMode = editMode,
                                            printWidthMm = editWidth,
                                            printResolutionDpi = editResolution,
                                            initialCommands = editInitialCommands,
                                            cutterCommands = editCutterCommands,
                                            drawerCommands = editDrawerCommands,
                                            profileType = editProfileType
                                        )
                                        val name = if (previewProfile.type == PrinterType.ETHERNET) {
                                            "Ethernet Printer"
                                        } else {
                                            previewProfile.name
                                        }
                                        val printerSettings = settingsFromProfile(previewProfile, settings)
                                        scope.launch {
                                            val error = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                                runCatching {
                                                    printGraphicTestPage(
                                                        context,
                                                        printerSettings,
                                                        name,
                                                        printer.address
                                                    )
                                                }.exceptionOrNull()
                                            }
                                            if (error != null) {
                                                errorMessage = "Failed to send graphic test page."
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                ) {
                                    Text("Graphic test", color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }

                            HorizontalDivider()

                            Text(text = "Graphic test from URL", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(
                                value = editGraphicUrl.ifBlank { pdfUrl },
                                onValueChange = { editGraphicUrl = it },
                                label = { Text("PDF URL") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            showLoading = true
                                            loadingMessage = "Loading preview..."
                                            val graphicUrl = editGraphicUrl.ifBlank { pdfUrl }
                                            val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                                runCatching {
                                            val previewProfile = selected.copy(
                                                name = editName.ifBlank { selected.name },
                                                address = editAddress.ifBlank { selected.address },
                                                printMode = editMode,
                                                printWidthMm = editWidth,
                                                printResolutionDpi = editResolution,
                                                initialCommands = editInitialCommands,
                                                cutterCommands = editCutterCommands,
                                                drawerCommands = editDrawerCommands,
                                                graphicTestUrl = graphicUrl,
                                                profileType = editProfileType
                                            )
                                                    downloadAndRenderPdfPreview(
                                                        settingsFromProfile(previewProfile, settings),
                                                        graphicUrl
                                                    )
                                                }.getOrNull()
                                            }
                                            showLoading = false
                                            if (result == null) {
                                                errorMessage = "Failed to load PDF preview."
                                            } else {
                                                previewBitmap = result
                                                showPreview = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = (editGraphicUrl.ifBlank { pdfUrl }).isNotBlank() && !showLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                ) {
                                    Text("Preview", color = MaterialTheme.colorScheme.onTertiary)
                                }
                                Button(
                                    onClick = {
                                        scope.launch {
                                            repository.savePdfUrl(editGraphicUrl.ifBlank { pdfUrl })
                                        }
                                        scope.launch {
                                            showLoading = true
                                            loadingMessage = "Connecting to printer..."
                                            val previewProfile = selected.copy(
                                                name = editName.ifBlank { selected.name },
                                                address = editAddress.ifBlank { selected.address },
                                                printMode = editMode,
                                                printWidthMm = editWidth,
                                                printResolutionDpi = editResolution,
                                                initialCommands = editInitialCommands,
                                                cutterCommands = editCutterCommands,
                                                drawerCommands = editDrawerCommands,
                                                graphicTestUrl = editGraphicUrl.ifBlank { pdfUrl },
                                                profileType = editProfileType
                                            )
                                            val name = if (previewProfile.type == PrinterType.ETHERNET) {
                                                "Ethernet Printer"
                                            } else {
                                                previewProfile.name
                                            }
                                            val graphicUrl = editGraphicUrl.ifBlank { pdfUrl }
                                            val success = kotlinx.coroutines.withContext(Dispatchers.IO) {
                                                printGraphicTestPageFromUrl(
                                                    context,
                                                    settingsFromProfile(previewProfile, settings),
                                                    name,
                                                    previewProfile.address,
                                                    graphicUrl
                                                )
                                            }
                                            if (!success) {
                                                errorMessage = "Failed to idbonepos from URL."
                                            } else {
                                                loadingMessage = "Printing..."
                                                kotlinx.coroutines.delay(1000)
                                            }
                                            showLoading = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = (editGraphicUrl.ifBlank { pdfUrl }).isNotBlank() && !showLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Print from URL", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }

                            HorizontalDivider()

                            Button(
                                onClick = {
                                    val address = editAddress.trim()
                                    if (address.isBlank()) {
                                        editError = "Printer address is required."
                                        return@Button
                                    }
                                    val updated = selected.copy(
                                        name = editName.ifBlank { selected.name },
                                        address = address,
                                        printMode = editMode,
                                        printWidthMm = editWidth,
                                        printResolutionDpi = editResolution,
                                        initialCommands = editInitialCommands,
                                        cutterCommands = editCutterCommands,
                                        drawerCommands = editDrawerCommands,
                                        graphicTestUrl = editGraphicUrl,
                                        profileType = editProfileType
                                    )
                                    scope.launch {
                                        repository.upsertPrinter(updated, select = true)
                                        if (updated.type == PrinterType.ETHERNET) {
                                            repository.saveEthernetPrinterIP(updated.address)
                                        }
                                        // Navigate back after saving
                                        onBack()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Save configuration", color = MaterialTheme.colorScheme.onPrimary)
                            }

                        }
                    }
                }
            }

        }

        if (showPreview && previewBitmap != null) {
            AlertDialog(
                onDismissRequest = { showPreview = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                title = { 
                    Text(
                        "PDF Preview",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                },
                confirmButton = {
                    ElevatedButton(
                        onClick = { showPreview = false },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Close", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }

        if (showLoading) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                title = { 
                    Text(
                        "Processing",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RobotLoadingIndicator(
                            isLoading = true,
                            size = 100.dp
                        )
                        Text(
                            loadingMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {}
            )
        }

        if (showDeleteDialog && selectedPrinter != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                title = { 
                    Text(
                        "Delete printer",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                text = {
                    Text(
                        "Remove ${selectedPrinter.name} from saved printers?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    ElevatedButton(
                        onClick = {
                            try {
                                val printerId = selectedPrinter.id
                                scope.launch {
                                    try {
                                        repository.deletePrinter(printerId)
                                        showDeleteDialog = false
                                        onBack()
                                    } catch (e: Exception) {
                                        Log.e("SettingsScreen", "Error deleting printer: ${e.message}", e)
                                        errorMessage = "Failed to delete printer: ${e.message}"
                                        showDeleteDialog = false
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("SettingsScreen", "Error in delete action: ${e.message}", e)
                                errorMessage = "Failed to delete printer: ${e.message}"
                                showDeleteDialog = false
                            }
                        },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun settingsFromProfile(profile: PrinterProfile, fallback: PrintSettings): PrintSettings {
    return fallback.copy(
        printerName = profile.name,
        printerAddress = profile.address,
        printMode = profile.printMode,
        printWidthMm = profile.printWidthMm,
        printResolutionDpi = profile.printResolutionDpi,
        initialCommands = profile.initialCommands,
        cutterCommands = profile.cutterCommands,
        drawerCommands = profile.drawerCommands
    )
}
