package com.idb.idbonepos

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
        
        // Set status bar color to match toolbar gradient (primary color from theme)
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
        // Make status bar content dark (since gradient background is dark purple)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = true
        
        handleDeepLink(intent)
        setContent {
            PrintBridgeTheme() {
                PrintBridgeApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
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
