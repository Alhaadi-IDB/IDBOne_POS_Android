package print.therestsuites.com.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import print.therestsuites.com.model.PrintSettings
import print.therestsuites.com.model.ResolutionOption
import print.therestsuites.com.settings.PrintMode
import print.therestsuites.com.settings.SettingsRepository
import print.therestsuites.com.ui.components.ModernToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settingsFlow.collectAsState(initial = PrintSettings())
    val scope = rememberCoroutineScope()

    var editMode by remember { mutableStateOf(settings.printMode) }
    var editWidth by remember { mutableStateOf(settings.printWidthMm) }
    var editResolution by remember { mutableStateOf(settings.printResolutionDpi) }
    var editInitialCommands by remember { mutableStateOf(settings.initialCommands) }
    var editCutterCommands by remember { mutableStateOf(settings.cutterCommands) }
    var editDrawerCommands by remember { mutableStateOf(settings.drawerCommands) }

    val printModes = listOf(PrintMode.GRAPHIC, PrintMode.TEXT)
    val printWidths = listOf(48, 58, 64, 72, 80)
    val printResolutions = listOf(
        ResolutionOption(203, 8),
        ResolutionOption(300, 12)
    )

    LaunchedEffect(settings) {
        editMode = settings.printMode
        editWidth = settings.printWidthMm
        editResolution = settings.printResolutionDpi
        editInitialCommands = settings.initialCommands
        editCutterCommands = settings.cutterCommands
        editDrawerCommands = settings.drawerCommands
    }

    AppScaffold(
        topBar = {
            ModernToolbar(
                title = "Default settings",
                showBack = true,
                onBack = onBack
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
                Text(
                    text = "These settings apply as the default configuration for all printers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                DropdownSetting(
                    label = "Print mode",
                    value = editMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    options = printModes.map { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } },
                    onOptionSelected = { index -> editMode = printModes[index] }
                )
            }

            item {
                DropdownSetting(
                    label = "Print width",
                    value = "${editWidth} mm",
                    options = printWidths.map { "$it mm" },
                    onOptionSelected = { index -> editWidth = printWidths[index] }
                )
            }

            val currentResolution = printResolutions.firstOrNull { it.dpi == editResolution }
            item {
                DropdownSetting(
                    label = "Print resolution",
                    value = currentResolution?.label ?: "${editResolution} dpi",
                    options = printResolutions.map { it.label },
                    onOptionSelected = { index -> editResolution = printResolutions[index].dpi }
                )
            }

            item {
                CommandField(
                    label = "Initial ESC/POS commands",
                    value = editInitialCommands,
                    onValueChange = { editInitialCommands = it }
                )
            }

            item {
                CommandField(
                    label = "Cutter ESC/POS commands",
                    value = editCutterCommands,
                    onValueChange = { editCutterCommands = it }
                )
            }

            item {
                CommandField(
                    label = "Drawer ESC/POS commands",
                    value = editDrawerCommands,
                    onValueChange = { editDrawerCommands = it }
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.updatePrintMode(editMode)
                                repository.updatePrintWidth(editWidth)
                                repository.updatePrintResolution(editResolution)
                                repository.updateInitialCommands(editInitialCommands)
                                repository.updateCutterCommands(editCutterCommands)
                                repository.updateDrawerCommands(editDrawerCommands)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save default configuration")
                    }
                }
            }
        }
    }
}
