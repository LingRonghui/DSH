package com.dsh.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DshApp()
        }
    }
}

private val BrandBlue = Color(0xFF4D6BFE)
private val BrandBlueBright = Color(0xFF8DA2FF)
private val NightBg = Color(0xFF0E1220)
private val NightSurface = Color(0xFF171C2E)
private val NightOnBg = Color(0xFFE9ECF8)

@Composable
private fun Modifier.dshEnter(visible: Boolean): Modifier {
    val t by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(480, easing = FastOutSlowInEasing),
        label = "dshEnter"
    )
    return graphicsLayer {
        alpha = t
        translationY = (1f - t) * 30f
    }
}

private const val UUID_POLYFILL_JS =
    "if(!(window.crypto&&typeof window.crypto.randomUUID==='function')){try{" +
        "window.crypto.randomUUID=function(){return('10000000-1000-4000-8000-100000000000')" +
        ".replace(/[018]/g,function(c){var r=window.crypto.getRandomValues(new Uint8Array(1))[0];" +
        "return((+c)^(r&15>>(c/4))).toString(16);});};}catch(e){}}"

private const val ADAPT_CSS_ASSET = "mobile_adapt.css"
private const val ADAPT_STYLE_ID = "dsh-mobile-adapt"

@Volatile
private var adaptCssCache: String? = null

@Volatile
private var adaptCssLoaded = false

private fun loadAdaptCss(context: Context): String? {
    if (!adaptCssLoaded) {
        adaptCssLoaded = true
        adaptCssCache = try {
            context.assets.open(ADAPT_CSS_ASSET).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
    return adaptCssCache
}

private fun adaptInjectJs(css: String): String =
    "(function(){if(document.getElementById('" + ADAPT_STYLE_ID + "'))return;" +
        "var s=document.createElement('style');s.id='" + ADAPT_STYLE_ID + "';" +
        "s.textContent=" + org.json.JSONObject.quote(css) + ";" +
        "document.head.appendChild(s);})();"

@Composable
private fun DshApp() {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val view = LocalView.current
    val scheme = if (dark) {
        darkColorScheme(
            primary = BrandBlue,
            background = NightBg,
            surface = NightSurface,
            surfaceVariant = NightSurface,
            onBackground = NightOnBg,
            onSurface = NightOnBg
        )
    } else {
        lightColorScheme(primary = BrandBlue)
    }
    var connectedUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var servers by remember { mutableStateOf(ServerStore.load(context)) }

    DisposableEffect(dark, connectedUrl) {
        val window = (context as Activity).window
        val inWeb = connectedUrl != null
        val barColor = if (inWeb || !dark) Color.White else NightBg
        window.statusBarColor = barColor.toArgb()
        window.navigationBarColor = barColor.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            val darkIcons = if (inWeb) true else !dark
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
        onDispose { }
    }

    MaterialTheme(colorScheme = scheme) {
        if (connectedUrl == null) {
            ConnectScreen(
                servers = servers,
                onConnect = { url ->
                    ServerStore.save(context, url)
                    servers = ServerStore.load(context)
                    connectedUrl = url
                },
                onRemove = { url ->
                    ServerStore.remove(context, url)
                    servers = ServerStore.load(context)
                }
            )
        } else {
            WebScreen(url = connectedUrl!!, onExit = { connectedUrl = null })
        }
    }
}

object ServerStore {
    private const val PREF = "dsh_servers"
    private const val KEY_LIST = "list"
    private const val KEY_LAST = "last"

    private val UrlPattern = Regex("^https?://[A-Za-z0-9._\\-\\[\\]:]+$")

    fun normalize(raw: String): String {
        var s = raw.trim().trimEnd('/')
        if (s.isNotEmpty() && !s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }
        return s
    }

    fun isValid(url: String): Boolean = url.length >= 11 && UrlPattern.matches(url)

    fun load(context: Context): List<String> =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getStringSet(KEY_LIST, emptySet())
            ?.sorted()
            ?: emptyList()

    fun last(context: Context): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_LAST, null)

    fun save(context: Context, url: String) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = LinkedHashSet(sp.getStringSet(KEY_LIST, emptySet()) ?: emptySet())
        set.remove(url)
        set.add(url)
        sp.edit().putStringSet(KEY_LIST, set).putString(KEY_LAST, url).apply()
    }

    fun remove(context: Context, url: String) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = LinkedHashSet(sp.getStringSet(KEY_LIST, emptySet()) ?: emptySet())
        set.remove(url)
        val editor = sp.edit().putStringSet(KEY_LIST, set)
        if (url == sp.getString(KEY_LAST, null)) {
            val fallback = set.minOrNull()
            if (fallback != null) editor.putString(KEY_LAST, fallback) else editor.remove(KEY_LAST)
        }
        editor.apply()
    }
}

@Composable
private fun ConnectScreen(
    servers: List<String>,
    onConnect: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dark = isSystemInDarkTheme()
    var input by rememberSaveable { mutableStateOf(ServerStore.last(context) ?: "") }
    var errorHint by rememberSaveable { mutableStateOf(false) }
    var step by remember { mutableIntStateOf(0) }
    val ripple = rememberInfiniteTransition(label = "sonar")
    val rippleT by ripple.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sonarRipple"
    )

    LaunchedEffect(Unit) {
        delay(60); step = 1
        delay(180); step = 2
        delay(160); step = 3
        delay(240); step = 4
        delay(200); step = 5
    }

    fun submit() {
        val url = ServerStore.normalize(input)
        if (!ServerStore.isValid(url)) {
            errorHint = true
            return
        }
        errorHint = false
        keyboard?.hide()
        onConnect(url)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.16f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .dshEnter(step >= 1),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(132.dp)) {
                    val base = size.minDimension / 2f
                    repeat(3) { i ->
                        val p = (rippleT + i / 3f) % 1f
                        drawCircle(
                            color = BrandBlue.copy(alpha = (1f - p) * 0.28f),
                            radius = base * (0.40f + 0.60f * p),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_whale),
                    contentDescription = null,
                    tint = if (dark) BrandBlueBright else BrandBlue,
                    modifier = Modifier.size(62.dp)
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "DSH",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.dshEnter(step >= 2)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "DEEPSEEK HARNESS",
                fontSize = 12.sp,
                letterSpacing = 2.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.dshEnter(step >= 2)
            )
            Spacer(Modifier.height(40.dp))
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    errorHint = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .dshEnter(step >= 3),
                label = { Text("服务器地址") },
                placeholder = { Text("如 192.168.1.10:3080 或 ds.example.com") },
                leadingIcon = { Icon(Icons.Outlined.Dns, contentDescription = null) },
                isError = errorHint,
                supportingText = if (errorHint) {
                    { Text("请输入有效的服务器地址", color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() })
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .dshEnter(step >= 4),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                enabled = input.isNotBlank()
            ) {
                Text("连接服务器", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            if (servers.isNotEmpty()) {
                Spacer(Modifier.height(36.dp))
                Text(
                    "历史连接",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dshEnter(step >= 5)
                )
                Spacer(Modifier.height(6.dp))
                servers.forEachIndexed { index, server ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val target = ServerStore.normalize(server)
                                if (ServerStore.isValid(target)) {
                                    keyboard?.hide()
                                    onConnect(target)
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 12.dp)
                            .dshEnter(step >= 5),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            server.removePrefix("https://").removePrefix("http://"),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemove(server) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (index < servers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 36.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
            Text(
                "所有会话数据均保存在你自己的服务器上",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .dshEnter(step >= 5)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebScreen(url: String, onExit: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableStateOf(0) }
    var loadingActive by remember { mutableStateOf(false) }
    var frameError by remember { mutableStateOf<String?>(null) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileCallback
        fileCallback = null
        callback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
    }

    BackHandler {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else onExit()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (loadingActive) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = BrandBlue,
                    trackColor = Color.White
                )
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.WHITE)
                        WebView.setWebContentsDebuggingEnabled(true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                            try {
                                WebViewCompat.addDocumentStartJavaScript(
                                    this,
                                    UUID_POLYFILL_JS,
                                    setOf("http://*", "https://*")
                                )
                            } catch (_: Exception) {
                            }
                        }
                        setDownloadListener { link, _, disposition, mime, _ ->
                            try {
                                val name = URLUtil.guessFileName(link, disposition, mime)
                                val request = DownloadManager.Request(Uri.parse(link)).apply {
                                    setTitle(name)
                                    setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                    )
                                    setDestinationInExternalPublicDir(
                                        Environment.DIRECTORY_DOWNLOADS, name
                                    )
                                    CookieManager.getInstance().getCookie(link)?.let {
                                        addRequestHeader("Cookie", it)
                                    }
                                }
                                (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager)
                                    .enqueue(request)
                                Toast.makeText(ctx, "已开始下载 $name", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "下载失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                if (!request.isForMainFrame) return false
                                val scheme = request.url.scheme?.lowercase()
                                if (scheme == "http" || scheme == "https") return false
                                return try {
                                    ctx.startActivity(
                                        Intent(Intent.ACTION_VIEW, request.url)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                    true
                                } catch (e: Exception) {
                                    true
                                }
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                u: String?,
                                favicon: Bitmap?
                            ) {
                                loadingActive = true
                                frameError = null
                                evaluateJavascript(UUID_POLYFILL_JS, null)
                            }

                            override fun onPageFinished(view: WebView?, u: String?) {
                                loadingActive = false
                                view?.let { wv ->
                                    loadAdaptCss(ctx)?.let { css ->
                                        wv.evaluateJavascript(adaptInjectJs(css), null)
                                    }
                                }
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame) {
                                    frameError = error.description?.toString() ?: "网络错误"
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                if (newProgress >= 100) loadingActive = false
                            }

                            override fun onShowFileChooser(
                                wv: WebView?,
                                callback: ValueCallback<Array<Uri>>,
                                params: FileChooserParams
                            ): Boolean {
                                fileCallback?.onReceiveValue(null)
                                fileCallback = callback
                                return try {
                                    fileChooserLauncher.launch(params.createIntent())
                                    true
                                } catch (e: Exception) {
                                    fileCallback = null
                                    false
                                }
                            }
                        }
                        loadUrl(url)
                    }.also { webView = it }
                }
            )
        }

        frameError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("无法连接服务器", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        err,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                    Row {
                        TextButton(onClick = { frameError = null; webView?.reload() }) {
                            Text("重试")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onExit) {
                            Text("更换地址")
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webView?.destroy() }
    }
}
