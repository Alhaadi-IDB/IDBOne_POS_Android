package com.idb.idbonepos

import android.app.ComponentCaller
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.idb.idbonepos.R
import com.idb.idbonepos.ui.PrintBridgeApp
import com.idb.idbonepos.ui.theme.PrintBridgeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display (required for API 36)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set status bar color to transparent for edge-to-edge
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // Make status bar content light (white icons) since toolbar has dark gradient background
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false
        
        handleDeepLink(intent)
        setContent {
            PrintBridgeTheme() {
                PrintBridgeApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val pdfId = extractPdfId(data) ?: return
        viewModel.printFromPdfId(pdfId)
    }

    private fun extractPdfId(data: Uri): String? {
        if (data.scheme != "https" || data.host != "app.mycompany.idbonepos") return null
        val segments = data.pathSegments
        if (segments.isNotEmpty() && segments[0] == "orders") {
            return segments.getOrNull(1) ?: data.lastPathSegment
        }
        return data.getQueryParameter("pdf_id")
    }
}
