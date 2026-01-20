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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import print.therestsuites.com.PrintStatus
import print.therestsuites.com.model.PrintReport
import print.therestsuites.com.model.PrintUiState
import print.therestsuites.com.settings.SettingsRepository
import print.therestsuites.com.ui.components.ModernToolbar

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
                    showBack = canGoBack,
                    onBack = {
                        val webView = webViewRef
                        if (webView?.canGoBack() == true) {
                            webView.goBack()
                        } else {
                            onBack()
                        }
                    },
                    actions = {
                        IconButton(onClick = { webViewRef?.reload() }) {
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No URL saved yet.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            } else {
                if (isFullscreen) {
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
                                .widthIn(max = 900.dp),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Could not load the page.",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = { errorMessage = null }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }


                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(onClick = {
                            errorMessage = null
                            webViewRef?.reload()
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            FilledIconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
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
                    }
                )
            }
        }
    }

    if (uiState.status != PrintStatus.Idle || uiState.message.isNotBlank()) {
        AlertDialog(
            onDismissRequest = onDismissStatus,
            title = { Text("Print status") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.status == PrintStatus.Downloading ||
                        uiState.status == PrintStatus.Printing ||
                        uiState.status == PrintStatus.WaitingForPermission
                    ) {
                        CircularProgressIndicator()
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
                        Text(statusLabel, style = MaterialTheme.typography.titleSmall)
                    }
                    if (uiState.message.isNotBlank()) {
                        Text(uiState.message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismissStatus) {
                    Text("Close")
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
                        onError(null)
                        onLoading(true)
                        onUrlChange(url.orEmpty())
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        onLoading(false)
                        onUrlChange(url.orEmpty())
                        injectJavaScriptErrorHandler(view)
                    }

                    override fun onReceivedError(
                        view: WebView,
                        request: WebResourceRequest,
                        error: WebResourceError
                    ) {
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
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val targetUrl = request.url?.toString().orEmpty()
                        val isHttp = targetUrl.startsWith("http://") || targetUrl.startsWith("https://")
                        return if (isHttp) {
                            false
                        } else {
                            onError("Unsupported URL scheme.")
                            true
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse
                    ) {
                        if (request.isForMainFrame) {
                            onError("HTTP ${errorResponse.statusCode}")
                            onLoading(false)
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView,
                        handler: SslErrorHandler,
                        error: SslError
                    ) {
                        onError("SSL error: ${error.primaryError}")
                        onLoading(false)
                        handler.cancel()
                    }

                    override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
                        onError("Web content crashed. Please reload.")
                        onLoading(false)
                        return true
                    }
                }
                loadUrl(url)
                onUrlChange(url)
                onWebViewReady(this)
            }
        },
        update = { view ->
            if (view.url != url) {
                view.loadUrl(url)
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
        val addressValue = ipAddress?.trim().orEmpty()
        val urlValue = pdfUrl?.trim().orEmpty()
        val typeValue = type?.trim().orEmpty()
        Log.println(Log.DEBUG,"sendPrintToAndroid=> ","$addressValue :: $urlValue ::  $typeValue")
        mainHandler.post {
            if (addressValue.isBlank() || urlValue.isBlank()) {
                onError("Printer address and PDF URL are required.")
            } else {
                onSendPrintToAndroid(addressValue, urlValue, typeValue)
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
        val value = message?.trim().orEmpty()
        mainHandler.post {
            if (value.isNotBlank()) {
                onError("JavaScript error: $value")
            }
        }
    }

    private fun handlePrint(payload: String?) {
        val value = payload?.trim().orEmpty()
        mainHandler.post {
            if (value.isBlank()) {
                onError("Print request is missing a PDF ID.")
            } else {
                onTriggerPrint(value)
            }
        }
    }
}
