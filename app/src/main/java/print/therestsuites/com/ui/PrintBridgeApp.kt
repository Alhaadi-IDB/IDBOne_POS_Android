package print.therestsuites.com.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import print.therestsuites.com.MainViewModel
import print.therestsuites.com.model.PrintSettings

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun PrintBridgeApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState(initial = PrintSettings())

    val needsBluetoothPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var hasBluetoothPermission by remember {
        mutableStateOf(
            !needsBluetoothPermission ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetoothPermission = granted
        if (granted) {
            viewModel.onBluetoothPermissionGranted()
        }
    }
    val shouldRequestBluetoothPermission = needsBluetoothPermission && !hasBluetoothPermission
    LaunchedEffect(shouldRequestBluetoothPermission) {
        if (shouldRequestBluetoothPermission) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenConfiguration = { navController.navigate("settings") },
                onOpenDefaultSettings = { navController.navigate("default-settings") },
                onOpenPrinter = { navController.navigate("printer") },
                onOpenWebView = { navController.navigate("webview") }
            )
        }
        composable("main") {
            MainScreen(
                uiState = uiState,
                settings = settings,
                hasBluetoothPermission = hasBluetoothPermission,
                needsBluetoothPermission = needsBluetoothPermission,
                onRequestBluetoothPermission = {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                },
                onSelectPrinter = { navController.navigate("printer") },
                onOpenSettings = { navController.navigate("settings") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("default-settings") {
            DefaultSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("printer") {
            PrinterSelectionScreen(
                hasBluetoothPermission = hasBluetoothPermission,
                needsBluetoothPermission = needsBluetoothPermission,
                onRequestBluetoothPermission = {
                    permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                },
                onOpenSettings = { navController.navigate("settings") },
                onOpenDefaultSettings = { navController.navigate("default-settings") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("webview") {
            WebViewScreen(
                onBack = { navController.popBackStack() },
                onTriggerPrint = { pdfId ->
                    viewModel.printFromPdfId(pdfId)
                },
                onSendPrintToAndroid = { address, pdfUrl, type ->
                    viewModel.printFromUrl(address, pdfUrl, type)
                },
                uiState = uiState,
                onDismissStatus = { viewModel.clearStatus() }
            )
        }
    }
}
