package print.therestsuites.com.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import print.therestsuites.com.PrintStatus
import print.therestsuites.com.model.PrintReport
import print.therestsuites.com.model.PrintUiState
import print.therestsuites.com.settings.SettingsRepository
import print.therestsuites.com.ui.components.ModernToolbar
import print.therestsuites.com.ui.components.RobotLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    onBack: () -> Unit,
    onTriggerPrint: (String) -> Unit,
    onSendPrintToAndroid: (String, String, String) -> Unit,
    uiState: PrintUiState,
    onDismissStatus: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }

    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var currentTitle by remember { mutableStateOf("Web View") }
    var isFullscreen by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var pendingPrintReport by remember { mutableStateOf<PrintReport?>(null) }
    var lastSentPrintReport by remember { mutableStateOf<PrintReport?>(null) }

    LaunchedEffect(Unit) {
        url = repository.getWebUrl()
    }

    BackHandler {
        val webView = webViewRef
        if (webView?.canGoBack() == true) {
            webView.goBack()
        } else if (isFullscreen) {
            isFullscreen = false
        } else {
            onBack()
        }
    }

    val view = LocalView.current
    DisposableEffect(isFullscreen) {
        val activity = view.context as? Activity
        val window = activity?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
            val controller = WindowInsetsControllerCompat(window, view)
            if (isFullscreen) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    AppScaffold(
        topBar = {
            if (!isFullscreen) {
                ModernToolbar(
                    title = currentTitle,
                    subtitle = currentUrl,
                    showBack = true,
                    onBack = {
                        val webView = webViewRef
                        // If WebView has history, go back in WebView history
                        if (webView?.canGoBack() == true) {
                            try {
                                webView.goBack()
                            } catch (e: Exception) {
                                Log.e("WebViewScreen", "Error going back in WebView: ${e.message}", e)
                                // If goBack fails, navigate to main page
                                onBack()
                            }
                        } else {
                            // If no WebView history, go back to main page
                            onBack()
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else padding)
        ) {
            if (url.isBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "No URL saved yet.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ElevatedButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Go Back", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            } else {
                if (isFullscreen) {
                    WebViewContent(
                        url = url,
                        onWebViewReady = { webViewRef = it },
                        onLoading = { isLoading = it },
                        onError = {
                            //errorMessage = it
                            Log.d(Log.DEBUG.toString(), "Error loading URL: $it")
                                  },
                        onUrlChange = {
                            currentUrl = it
                            canGoBack = webViewRef?.canGoBack() == true
                        },
                        onTitleChange = { currentTitle = it.ifBlank { "Web View" } },
                        onTriggerPrint = { payload ->
                            if (payload.isBlank()) {
                                errorMessage = "Print request is missing a PDF ID."
                            } else {
                                onTriggerPrint(payload)
                            }
                        },
                        onSendPrintToAndroid = { address, pdfUrl, type ->
                            if (address.isBlank() || pdfUrl.isBlank()) {
                                errorMessage = "Printer address and PDF URL are required."
                            } else {
                                onSendPrintToAndroid(address, pdfUrl, type)
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 900.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                .clip(RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            WebViewContent(
                                url = url,
                                onWebViewReady = { webViewRef = it },
                                onLoading = { isLoading = it },
                                onError = { errorMessage = it },
                                onUrlChange = {
                                    currentUrl = it
                                    canGoBack = webViewRef?.canGoBack() == true
                                },
                                onTitleChange = { currentTitle = it.ifBlank { "Web View" } },
                                onTriggerPrint = { payload ->
                                    if (payload.isBlank()) {
                                        errorMessage = "Print request is missing a PDF ID."
                                    } else {
                                        onTriggerPrint(payload)
                                    }
                                },
                                onSendPrintToAndroid = { address, pdfUrl, type ->
                                    if (address.isBlank() || pdfUrl.isBlank()) {
                                        errorMessage = "Printer address and PDF URL are required."
                                    } else {
                                        onSendPrintToAndroid(address, pdfUrl, type)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RobotLoadingIndicator(
                                isLoading = true,
                                size = 120.dp
                            )
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
            ) {
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Could not load the page",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { errorMessage = null },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                            )
                                ElevatedButton(
                                onClick = {
                                    try {
                                        errorMessage = null
                                        webViewRef?.reload()
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to reload: ${e.message}"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Retry", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    }
                }
            }

            FilledIconButton(
                onClick = { 
                    try {
                        isFullscreen = !isFullscreen
                    } catch (e: Exception) {
                        errorMessage = "Failed to toggle fullscreen: ${e.message}"
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isFullscreen) {
                        Icons.Default.ArrowDropDown
                    } else {
                        Icons.Default.KeyboardArrowUp
                    },
                    contentDescription = if (isFullscreen) {
                        "Exit Fullscreen"
                    } else {
                        "Enter Fullscreen"
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (uiState.status != PrintStatus.Idle || uiState.message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = onDismissStatus,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = { 
                Text(
                    "Print status",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    if (uiState.status == PrintStatus.Downloading ||
                        uiState.status == PrintStatus.Printing ||
                        uiState.status == PrintStatus.WaitingForPermission
                    ) {
                        RobotLoadingIndicator(
                            isLoading = true,
                            size = 80.dp
                        )
                    } else if (uiState.status == PrintStatus.Error) {
                        RobotLoadingIndicator(
                            isLoading = false,
                            size = 80.dp
                        )
                    }
                    val statusLabel = when (uiState.status) {
                        PrintStatus.Idle -> ""
                        PrintStatus.WaitingForPermission -> "Waiting for permission..."
                        PrintStatus.Downloading -> "Downloading..."
                        PrintStatus.Printing -> "Printing..."
                        PrintStatus.Success -> "Success"
                        PrintStatus.Error -> "Error"
                    }
                    if (statusLabel.isNotBlank()) {
                        Text(
                            statusLabel, 
                            style = MaterialTheme.typography.titleMedium,
                            color = when (uiState.status) {
                                PrintStatus.Success -> MaterialTheme.colorScheme.primary
                                PrintStatus.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    if (uiState.message.isNotBlank()) {
                        Text(
                            uiState.message, 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                ElevatedButton(
                    onClick = onDismissStatus,
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

    LaunchedEffect(uiState.status, uiState.message) {
        if (uiState.status == PrintStatus.Success || uiState.status == PrintStatus.Error) {
            val report = PrintReport(
                success = uiState.status == PrintStatus.Success,
                message = uiState.message
            )
            if (report != lastSentPrintReport) {
                pendingPrintReport = report
            }
        }
    }

    LaunchedEffect(webViewRef, pendingPrintReport) {
        val report = pendingPrintReport
        val webView = webViewRef
        if (webView != null && report != null) {
            sendPrintStatusToWebView(webView, report.success, report.message)
            lastSentPrintReport = report
            pendingPrintReport = null
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewContent(
    url: String,
    onWebViewReady: (WebView) -> Unit,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onTriggerPrint: (String) -> Unit,
    onSendPrintToAndroid: (String, String, String) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                CookieManager.getInstance().setAcceptCookie(true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.textZoom = 100
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(
                    PrintBridgeJsInterface(onTriggerPrint, onSendPrintToAndroid, onError),
                    "AndroidBridge"
                )
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView, title: String?) {
                        onTitleChange(title.orEmpty())
                    }

                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                        val message = consoleMessage.message()
                        if (consoleMessage.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR ||
                            isLikelyJavaScriptError(message)
                        ) {
                            onError("JavaScript error: ${consoleMessage.message()}")
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }

                    override fun onJsAlert(
                        view: WebView,
                        url: String,
                        message: String,
                        result: JsResult
                    ): Boolean {
                        onError("JavaScript alert: $message")
                        result.confirm()
                        return true
                    }

                    override fun onJsConfirm(
                        view: WebView,
                        url: String,
                        message: String,
                        result: JsResult
                    ): Boolean {
                        onError("JavaScript confirm: $message")
                        result.confirm()
                        return true
                    }

                    override fun onJsPrompt(
                        view: WebView,
                        url: String,
                        message: String,
                        defaultValue: String,
                        result: JsPromptResult
                    ): Boolean {
                        onError("JavaScript prompt: $message")
                        result.confirm(defaultValue)
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                        try {
                            onError(null)
                            onLoading(true)
                            onUrlChange(url.orEmpty())
                        } catch (e: Exception) {
                            Log.e("WebViewScreen", "Error in onPageStarted: ${e.message}", e)
                            onError("Error starting page load: ${e.message}")
                        }
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        try {
                            onLoading(false)
                            onUrlChange(url.orEmpty())
                            injectJavaScriptErrorHandler(view)
                        } catch (e: Exception) {
                            Log.e("WebViewScreen", "Error in onPageFinished: ${e.message}", e)
                            onError("Error finishing page load: ${e.message}")
                            onLoading(false)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
                        try {
                            if (request.isForMainFrame) {
                                val description = error.description?.toString() ?: "Failed to load page."
                                val cleartextDetected = description.contains("CLEARTEXT", ignoreCase = true)
                                val message = if (cleartextDetected) {
                                    "Cleartext HTTP is blocked. Use https or allow cleartext traffic."
                                } else {
                                    description
                                }
                                onError(message)
                                onLoading(false)
                            }
                        } catch (e: Exception) {
                            Log.e("WebViewScreen", "Error in onReceivedError: ${e.message}", e)
                            onError("Network error: ${e.message}")
                            onLoading(false)
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        return try {
                            val targetUrl = request.url?.toString().orEmpty()
                            val isHttp = targetUrl.startsWith("http://") || targetUrl.startsWith("https://")
                            if (isHttp) {
                                false
                            } else {
                                onError("Unsupported URL scheme.")
                                true
                            }
                        } catch (e: Exception) {
                            Log.e("WebViewScreen", "Error in shouldOverrideUrlLoading: ${e.message}", e)
                            false
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse
                    ) {
                        try {
                            if (request.isForMainFrame) {
                                val statusCode = errorResponse.statusCode
                                val errorMsg = when (statusCode) {
                                    404 -> "Page not found (404)"
                                    500 -> "Server error (500)"
                                    403 -> "Access forbidden (403)"
                                    else -> "HTTP error: $statusCode"
                                }
                                onError(errorMsg)
                                onLoading(false)
                            }
                        } catch (e: Exception) {
                            Log.e("WebViewScreen", "Error in onReceivedHttpError: ${e.message}", e)
                            onError("HTTP error occurred")
                            onLoading(false)
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView,
                        handler: SslErrorHandler,
                        error: SslError
                    ) {
                        try {
                            val errorMsg = when (error.primaryError) {
                                android.net.http.SslError.SSL_UNTRUSTED -> "Certificate is not trusted"
                                android.net.http.SslError.SSL_EXPIRED -> "Certificate has expired"
                                android.net.http.SslError.SSL_IDMISMATCH -> "Certificate hostname mismatch"
                                android.net.http.SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
                                else -> "SSL error: ${error.primaryError}"
                            }
                            onError(errorMsg)
                            onLoading(false)
                            handler.cancel()
                        } catch (e: Exception) {
                            Log.e("WebViewScreen", "Error in onReceivedSslError: ${e.message}", e)
                            onError("SSL error occurred")
                            onLoading(false)
                            handler.cancel()
                        }
                    }

                    override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                        try {
                            val crashed = detail.didCrash()
                            val message = if (crashed) {
                                "Web content crashed. Please reload the page."
                            } else {
                                "Web content terminated. Please reload the page."
                            }
                            onError(message)
                            onLoading(false)
                            return true
                        } catch (e: Exception) {
                            Log.e("WebViewScreen", "Error in onRenderProcessGone: ${e.message}", e)
                            onError("Web content error occurred")
                            onLoading(false)
                            return true
                        }
                    }
                }
                try {
                    loadUrl(url)
                    onUrlChange(url)
                    onWebViewReady(this)
                } catch (e: Exception) {
                    Log.e("WebViewScreen", "Error loading URL: ${e.message}", e)
                    onError("Failed to load URL: ${e.message}")
                }
            }
        },
        update = { view ->
            try {
                if (view.url != url) {
                    view.loadUrl(url)
                }
            } catch (e: Exception) {
                Log.e("WebViewScreen", "Error updating URL: ${e.message}", e)
                onError("Failed to update URL: ${e.message}")
            }
        }
    )
}

private fun isLikelyJavaScriptError(message: String): Boolean {
    val lower = message.lowercase()
    return lower.contains("uncaught") ||
        lower.contains("typeerror") ||
        lower.contains("referenceerror") ||
        lower.contains("syntaxerror") ||
        lower.contains("is not a function")
}

private fun injectJavaScriptErrorHandler(webView: WebView) {
    val script = """
        (function() {
          if (window.xMainEntry?.__printBridgeErrorsInstalled) return;
          window.xMainEntry?.__printBridgeErrorsInstalled = true;
          window.xMainEntry?.addEventListener('error', function(event) {
            var message = event && event.message ? event.message : 'Unknown JS error';
            if (window.xMainEntry?.AndroidBridge && window.xMainEntry?.AndroidBridge.reportError) {
              window.xMainEntry?.AndroidBridge.reportError(message);
            }
          });
          window.xMainEntry?.addEventListener('unhandledrejection', function(event) {
            var reason = event && event.reason ? event.reason : 'Unhandled promise rejection';
            var message = typeof reason === 'string' ? reason : (reason && reason.message) || 'Unhandled promise rejection';
            if (window.xMainEntry?.AndroidBridge && window.xMainEntry?.AndroidBridge.reportError) {
              window.xMainEntry?.AndroidBridge.reportError(message);
            }
          });
        })();
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}

///mobile call this function when finish the printing job.
private fun sendPrintStatusToWebView(webView: WebView, success: Boolean, message: String) {
    val safeMessage = escapeForJs(message)
    val script = """
        if (window.xMainEntry?.mobilePrintingStatus) {
          window.xMainEntry?.mobilePrintingStatus(${success.toString()}, '$safeMessage');
        }
    """.trimIndent()
    webView.evaluateJavascript(script, null)
}

private fun escapeForJs(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "")
}


private class PrintBridgeJsInterface(
    private val onTriggerPrint: (String) -> Unit,
    private val onSendPrintToAndroid: (String, String, String) -> Unit,
    private val onError: (String?) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(payload: String?) {
        handlePrint(payload)
    }

    @JavascriptInterface
    fun triggerPrint(payload: String?) {
        handlePrint(payload)
    }

    @JavascriptInterface
    fun print(payload: String?) {
        handlePrint(payload)
    }

    @JavascriptInterface
    fun sendPrintToAndroid(ipAddress: String?, pdfUrl: String?, type: String?) {
        try {
            val addressValue = ipAddress?.trim().orEmpty()
            val urlValue = pdfUrl?.trim().orEmpty()
            val typeValue = type?.trim().orEmpty()
            Log.println(Log.DEBUG,"sendPrintToAndroid=> ","$addressValue :: $urlValue ::  $typeValue")
            mainHandler.post {
                try {
                    if (addressValue.isBlank() || urlValue.isBlank()) {
                        onError("Printer address and PDF URL are required.")
                    } else {
                        onSendPrintToAndroid(addressValue, urlValue, typeValue)
                    }
                } catch (e: Exception) {
                    Log.e("PrintBridgeJsInterface", "Error in sendPrintToAndroid: ${e.message}", e)
                    onError("Failed to process print request: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("PrintBridgeJsInterface", "Error in sendPrintToAndroid: ${e.message}", e)
            mainHandler.post {
                onError("Failed to handle print request: ${e.message}")
            }
        }
    }

//    @JavascriptInterface
//    fun senfPrintToAndroid(ipAddress: String?, pdfUrl: String?, type: String?) {
//        sendPrintToAndroid(ipAddress, pdfUrl, type)
//    }

    @JavascriptInterface
    fun sendOrderFromAndroid() {
        // ✅ called when button pressed in WebView
        println("sendOrderFromAndroid() called from WebView")
        // 👉 process on Android here
    }

    @JavascriptInterface
    fun reportError(message: String?) {
        try {
            val value = message?.trim().orEmpty()
            mainHandler.post {
                try {
                    if (value.isNotBlank()) {
                        onError("JavaScript error: $value")
                    }
                } catch (e: Exception) {
                    Log.e("PrintBridgeJsInterface", "Error in reportError: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PrintBridgeJsInterface", "Error in reportError: ${e.message}", e)
        }
    }

    private fun handlePrint(payload: String?) {
        try {
            val value = payload?.trim().orEmpty()
            mainHandler.post {
                try {
                    if (value.isBlank()) {
                        onError("Print request is missing a PDF ID.")
                    } else {
                        onTriggerPrint(value)
                    }
                } catch (e: Exception) {
                    Log.e("PrintBridgeJsInterface", "Error in handlePrint: ${e.message}", e)
                    onError("Failed to process print: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("PrintBridgeJsInterface", "Error in handlePrint: ${e.message}", e)
            mainHandler.post {
                onError("Failed to handle print request: ${e.message}")
            }
        }
    }
}
