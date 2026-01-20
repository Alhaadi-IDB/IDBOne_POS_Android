package print.therestsuites.com

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import print.therestsuites.com.ui.PrintBridgeApp
import print.therestsuites.com.ui.theme.PrintBridgeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        setContent {
            PrintBridgeTheme {
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
        if (data.scheme != "https" || data.host != "app.mycompany.com") return null
        val segments = data.pathSegments
        if (segments.isNotEmpty() && segments[0] == "orders") {
            return segments.getOrNull(1) ?: data.lastPathSegment
        }
        return data.getQueryParameter("pdf_id")
    }
}
