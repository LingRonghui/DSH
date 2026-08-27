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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
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

// 深海声呐主题色板：启动幕深渊蓝 / 连接页雾蓝 / 主页冰雾底
private val AbyssTop = Color(0xFF070B16)
private val AbyssBottom = Color(0xFF111A36)
private val MistTop = Color(0xFFF7F9FF)
private val MistBottom = Color(0xFFECF1FB)
private val WebTop = Color(0xFFF7F9FE)
private val WebTint = Color(0xFFF5F8FE)

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

// 启动幕：深渊蓝 + 声呐涟漪 + 鲸鱼弹性入场，期间 WebView 在底层并行加载（感知加速）
@Composable
private fun DshSplash(hint: String, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var leaving by remember { mutableStateOf(false) }
    val exit = remember { Animatable(1f) }
    val sonar = rememberInfiniteTransition(label = "splashSonar")
    val rippleT by sonar.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "splashRipple"
    )
    val floatT by sonar.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing)),
        label = "splashFloat"
    )
    val dotT by sonar.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "splashDot"
    )
    val whaleIn by animateFloatAsState(
        targetValue = if (step >= 1) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "whaleIn"
    )

    LaunchedEffect(Unit) {
        delay(90); step = 1
        delay(240); step = 2
        delay(220); step = 3
        val t0 = System.currentTimeMillis()
        while (!leaving && System.currentTimeMillis() - t0 < 1450) delay(40)
        leaving = true
        exit.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = exit.value
                val s = 1f + (1f - exit.value) * 0.05f
                scaleX = s
                scaleY = s
            }
            .background(Brush.verticalGradient(listOf(AbyssTop, AbyssBottom)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { leaving = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(196.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(196.dp)) {
                    val base = size.minDimension / 2f
                    repeat(3) { i ->
                        val p = (rippleT + i / 3f) % 1f
                        drawCircle(
                            color = BrandBlue.copy(alpha = (1f - p) * 0.32f),
                            radius = base * (0.30f + 0.70f * p),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_whale),
                    contentDescription = null,
                    tint = BrandBlueBright,
                    modifier = Modifier
                        .size(74.dp)
                        .graphicsLayer {
                            val t = ((whaleIn - 0.55f) / 0.45f).coerceIn(0f, 1f)
                            scaleX = 0.55f + 0.45f * whaleIn
                            scaleY = 0.55f + 0.45f * whaleIn
                            alpha = t
                            translationY = (floatT - 0.5f) * 16f
                        }
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(
                "DSH",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                color = Color(0xFFF2F5FF),
                modifier = Modifier.dshEnter(step >= 2)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "DEEPSEEK HARNESS",
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = BrandBlueBright.copy(alpha = 0.85f),
                modifier = Modifier.dshEnter(step >= 2)
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 84.dp)
                .dshEnter(step >= 3),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(BrandBlueBright.copy(alpha = 0.25f + 0.7f * ((dotT + i / 3f) % 1f)))
                )
            }
        }
        Text(
            hint,
            fontSize = 12.sp,
            color = Color(0xFF93A1D6),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 58.dp)
                .dshEnter(step >= 3)
        )
    }
}

private const val UUID_POLYFILL_JS =
    "if(!(window.crypto&&typeof window.crypto.randomUUID==='function')){try{" +
        "window.crypto.randomUUID=function(){return('10000000-1000-4000-8000-100000000000')" +
        ".replace(/[018]/g,function(c){var r=window.crypto.getRandomValues(new Uint8Array(1))[0];" +
        "return((+c)^(r&15>>(c/4))).toString(16);});};}catch(e){}}"

// 触屏去气泡：①原生 title 气泡点击后常驻遮挡输入框 → title 转移到 aria-label 后移除；
// ②页面自实现的 role=tooltip 气泡由 mobile_adapt.css 隐藏。
// 必须在 onPageFinished 注入：onPageStarted 时新 document 可能未就绪，observe 会抛错
private const val TOOLTIP_CLEANUP_JS =
    "(function(){try{" +
        "if(window.__dshTipGuard)return;" +
        "window.__dshTipGuard=true;" +
        "var f=function(root){var t=root.hasAttribute&&root.hasAttribute('title')?[root]:[];" +
        "if(root.querySelectorAll)root.querySelectorAll('[title]').forEach(function(el){t.push(el);});" +
        "t.forEach(function(el){" +
        "if(!el.getAttribute('aria-label'))el.setAttribute('aria-label',el.getAttribute('title'));" +
        "el.removeAttribute('title');});};" +
        "f(document);" +
        "new MutationObserver(function(ms){ms.forEach(function(m){f(m.target);});})" +
        ".observe(document.documentElement," +
        "{childList:true,subtree:true,attributes:true,attributeFilter:['title']});}catch(e){}})();"

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
    // 自动重连：冷启动直接恢复上次成功连接且合法的服务器，配合全局 Cookie 免重复登录
    var connectedUrl by rememberSaveable {
        mutableStateOf(ServerStore.last(context)?.takeIf { ServerStore.isValid(it) })
    }
    var splashShowing by rememberSaveable { mutableStateOf(true) }
    var servers by remember { mutableStateOf(ServerStore.load(context)) }

    DisposableEffect(dark, connectedUrl, splashShowing) {
        val window = (context as Activity).window
        val inWeb = connectedUrl != null
        val barColor = when {
            splashShowing -> AbyssTop
            inWeb -> if (dark) NightBg else WebTop
            else -> if (dark) NightBg else MistTop
        }
        window.statusBarColor = barColor.toArgb()
        window.navigationBarColor = barColor.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            val darkIcons = !splashShowing && !dark
            isAppearanceLightStatusBars = darkIcons
            isAppearanceLightNavigationBars = darkIcons
        }
        onDispose { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaterialTheme(colorScheme = scheme) {
            if (connectedUrl == null) {
                ConnectScreen(
                    servers = servers,
                    lastServer = ServerStore.last(context),
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
        if (splashShowing) {
            DshSplash(
                hint = if (connectedUrl != null) "正在连接上次的服务器…" else "正在唤醒深海工作站…",
                onDone = { splashShowing = false }
            )
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
    lastServer: String?,
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
    val floatT by ripple.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing)),
        label = "sonarFloat"
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
            .background(
                if (dark) Brush.verticalGradient(listOf(NightBg, Color(0xFF131A30)))
                else Brush.verticalGradient(listOf(MistTop, MistBottom))
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-120).dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            BrandBlue.copy(alpha = if (dark) 0.22f else 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-90).dp, y = 130.dp)
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            BrandBlueBright.copy(alpha = if (dark) 0.12f else 0.10f),
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
                    modifier = Modifier
                        .size(62.dp)
                        .graphicsLayer { translationY = (floatT - 0.5f) * 12f }
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
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(BrandBlue, BrandBlueBright)))
                    .dshEnter(step >= 2)
            )
            Spacer(Modifier.height(22.dp))
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
                    .shadow(
                        16.dp,
                        RoundedCornerShape(14.dp),
                        ambientColor = BrandBlue,
                        spotColor = BrandBlue
                    )
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
                Spacer(Modifier.height(10.dp))
                servers.forEachIndexed { _, server ->
                    val isLast = server == lastServer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (dark) NightSurface else Color.White)
                            .border(
                                0.8.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                val target = ServerStore.normalize(server)
                                if (ServerStore.isValid(target)) {
                                    keyboard?.hide()
                                    onConnect(target)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .dshEnter(step >= 5),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BrandBlue.copy(alpha = if (dark) 0.18f else 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = if (dark) BrandBlueBright else BrandBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            server.removePrefix("https://").removePrefix("http://"),
                            fontSize = 14.sp,
                            fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isLast) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandBlue.copy(alpha = if (dark) 0.22f else 0.10f))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "上次",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dark) BrandBlueBright else BrandBlue
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                        }
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
    val dark = isSystemInDarkTheme()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadProgress by remember { mutableStateOf(0) }
    var loadingActive by remember { mutableStateOf(false) }
    var frameError by remember { mutableStateOf<String?>(null) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    // 右缘拉手 → 底部断开面板：随时退出当前服务器、临时切换其他服务器
    var showSheet by remember { mutableStateOf(false) }
    val handleAlpha by animateFloatAsState(
        targetValue = if (loadingActive) 0f else 1f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "handleAlpha"
    )

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
        when {
            showSheet -> showSheet = false
            webView?.canGoBack() == true -> webView?.goBack()
            else -> onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (dark) NightBg else WebTop)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (loadingActive) {
                LinearProgressIndicator(
                    progress = { loadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = BrandBlue,
                    trackColor = Color.Transparent
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
                        setBackgroundColor(0xFFDBE5F4.toInt())
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
                                    // document 就绪后注入，onPageStarted 时机可能未就绪导致 observer 建立失败
                                    wv.evaluateJavascript(TOOLTIP_CLEANUP_JS, null)
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

        // 右缘拉手：轻量入口，唤出断开面板
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-48).dp)
                .graphicsLayer { alpha = handleAlpha * 0.92f }
                .size(width = 24.dp, height = 76.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .background(
                    if (dark) Color.White.copy(alpha = 0.10f)
                    else Color(0xFF0E1220).copy(alpha = 0.08f)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ChevronLeft,
                contentDescription = "断开连接",
                tint = if (dark) Color.White.copy(alpha = 0.65f)
                else Color(0xFF0E1220).copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp)
            )
        }

        // 断开面板：scrim + 底部卡片
        val scrimA by animateFloatAsState(
            targetValue = if (showSheet) 1f else 0f,
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            label = "scrimA"
        )
        if (scrimA > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrimA }
                    .background(Color.Black.copy(alpha = 0.34f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showSheet = false }
            )
        }
        AnimatedVisibility(
            visible = showSheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(if (dark) NightSurface else Color.White)
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp)
            ) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    "断开当前服务器",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (dark) NightBg else MistBottom)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = if (dark) BrandBlueBright else BrandBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        url.removePrefix("https://").removePrefix("http://"),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "断开后可临时切换其他服务器；你的登录状态与数据都保存在服务器上，重新连接即可恢复。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showSheet = false }) {
                        Text("取消")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            showSheet = false
                            onExit()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Icon(
                            Icons.Outlined.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("断开并切换")
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }

        frameError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (dark) NightBg else WebTop),
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
