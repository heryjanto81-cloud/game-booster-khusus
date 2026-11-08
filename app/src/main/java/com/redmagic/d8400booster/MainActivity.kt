package com.redmagic.d8400booster

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val CrimsonNeon = Color(0xFFFF003C)
val CyanNeon = Color(0xFF00F0FF)
val GoldNeon = Color(0xFFFFB800)
val GreenNeon = Color(0xFF00FF66)
val DarkBg = Color(0xFF08090F)
val CardBg = Color(0xFF121521)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engine = DimensityEngine(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = DarkBg,
                    surface = CardBg,
                    primary = CrimsonNeon,
                    secondary = CyanNeon
                )
            ) {
                RedDimensityDashboard(engine)
            }
        }
    }
}

@Composable
fun RedDimensityDashboard(engine: DimensityEngine) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var currentMode by remember { mutableStateOf(PerformanceMode.RISE) }
    var telemetry by remember { mutableStateOf(engine.readTelemetry(currentMode)) }

    var mfrcEnabled by remember { mutableStateOf(true) }
    var rayTracingBoost by remember { mutableStateOf(true) }
    var touchBoost by remember { mutableStateOf(true) }
    var nosNetworkBoost by remember { mutableStateOf(true) }
    var bypassCharging by remember { mutableStateOf(false) }
    var scoutAudio by remember { mutableStateOf(false) }

    var showCrosshair by remember { mutableStateOf(false) }
    var crosshairStyle by remember { mutableIntStateOf(0) }
    var crosshairColor by remember { mutableStateOf("#00FF66") }
    var crosshairSize by remember { mutableFloatStateOf(1.0f) }
    var showFpsHud by remember { mutableStateOf(false) }

    var consoleLogs by remember {
        mutableStateOf(
            listOf(
                "[SYSTEM] RedDimensity Space v1.0.0-ULTRA initialized.",
                "[SoC] MediaTek Dimensity 8400-Ultra (MT6899) 8x Cortex-A725 detected.",
                "[GPU] ARM Mali-G720 MC7 @ 1300MHz Ray-Tracing ready."
            )
        )
    }

    LaunchedEffect(currentMode) {
        while (true) {
            telemetry = engine.readTelemetry(currentMode)
            delay(1000)
        }
    }

    fun updateOverlayService() {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Izinkan 'Tampilkan di atas aplikasi lain' terlebih dahulu!", Toast.LENGTH_LONG).show()
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
            return
        }
        val intent = Intent(context, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_SHOW_CROSSHAIR, showCrosshair)
            putExtra(OverlayService.EXTRA_CROSSHAIR_STYLE, crosshairStyle)
            putExtra(OverlayService.EXTRA_CROSSHAIR_COLOR, crosshairColor)
            putExtra(OverlayService.EXTRA_CROSSHAIR_SIZE, crosshairSize)
            putExtra(OverlayService.EXTRA_SHOW_FPS, showFpsHud)
        }
        context.startService(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF280612), CardBg, Color(0xFF061E28))
                    )
                )
                .border(1.dp, CrimsonNeon.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RedDimensityMechaLogo(isDiabloMode = currentMode == PerformanceMode.DIABLO)
                Column {
                    Text(
                        text = "RED DIMENSITY // 8400-ULTRA SPACE",
                        color = CrimsonNeon,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "MT6899 4nm TSMC • 8x Cortex-A725 All-Big-Core • Mali-G720 MC7",
                        color = CyanNeon,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge("🌡️ ${telemetry.tempCelsius}°C", if (telemetry.tempCelsius > 43f) CrimsonNeon else GreenNeon)
                StatusBadge("🔋 ${telemetry.batteryPercent}%", CyanNeon)
                StatusBadge("⚡ ${currentMode.title}", if (currentMode == PerformanceMode.DIABLO) CrimsonNeon else GoldNeon)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                "🔥 ENERGY CUBE",
                "🎯 AIM & PLUGINS",
                "🚀 HYPERENGINE",
                "🎮 GAME LAUNCHER",
                "📟 KERNEL LOGS"
            )
            tabs.forEachIndexed { index, title ->
                val active = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) CrimsonNeon else CardBg)
                        .border(1.dp, if (active) CrimsonNeon else Color.DarkGray, RoundedCornerShape(8.dp))
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (active) Color.White else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> EnergyCubeTab(
                    telemetry = telemetry,
                    currentMode = currentMode,
                    onModeChange = { mode ->
                        currentMode = mode
                        scope.launch {
                            val newLogs = engine.applyPerformanceProfile(
                                mode, mfrcEnabled, rayTracingBoost, bypassCharging, touchBoost, nosNetworkBoost
                            )
                            consoleLogs = consoleLogs + newLogs
                        }
                    },
                    onCleanRam = {
                        scope.launch {
                            val (apps, mb) = engine.boostRamAndCleanCache()
                            consoleLogs = consoleLogs + "[LPDDR5X TURBO] Killed $apps background tasks & freed $mb MB RAM!"
                            Toast.makeText(context, "RAM LPDDR5X Dibersihkan: +$mb MB Bebas!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                1 -> PluginsTab(
                    showCrosshair = showCrosshair,
                    crosshairStyle = crosshairStyle,
                    crosshairColor = crosshairColor,
                    crosshairSize = crosshairSize,
                    showFpsHud = showFpsHud,
                    scoutAudio = scoutAudio,
                    bypassCharging = bypassCharging,
                    onToggleCrosshair = {
                        showCrosshair = it
                        updateOverlayService()
                    },
                    onChangeStyle = {
                        crosshairStyle = it
                        if (showCrosshair) updateOverlayService()
                    },
                    onChangeColor = {
                        crosshairColor = it
                        if (showCrosshair) updateOverlayService()
                    },
                    onChangeSize = {
                        crosshairSize = it
                        if (showCrosshair) updateOverlayService()
                    },
                    onToggleFps = {
                        showFpsHud = it
                        updateOverlayService()
                    },
                    onToggleScoutAudio = {
                        scoutAudio = it
                        val msg = engine.setScoutAudioMode(it)
                        consoleLogs = consoleLogs + "[AUDIO DSP] $msg"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    onToggleBypass = {
                        bypassCharging = it
                        scope.launch {
                            val newLogs = engine.applyPerformanceProfile(
                                currentMode, mfrcEnabled, rayTracingBoost, bypassCharging, touchBoost, nosNetworkBoost
                            )
                            consoleLogs = consoleLogs + newLogs
                        }
                    }
                )
                2 -> HyperEngineTab(
                    mfrcEnabled = mfrcEnabled,
                    rayTracingBoost = rayTracingBoost,
                    touchBoost = touchBoost,
                    nosNetworkBoost = nosNetworkBoost,
                    onUpdateToggles = { mfrc, rt, touch, nos ->
                        mfrcEnabled = mfrc
                        rayTracingBoost = rt
                        touchBoost = touch
                        nosNetworkBoost = nos
                        scope.launch {
                            val newLogs = engine.applyPerformanceProfile(
                                currentMode, mfrcEnabled, rayTracingBoost, bypassCharging, touchBoost, nosNetworkBoost
                            )
                            consoleLogs = consoleLogs + newLogs
                        }
                    }
                )
                3 -> GameLauncherTab(
                    games = engine.presetGames,
                    onLaunchGame = { game ->
                        scope.launch {
                            val (_, freedMB) = engine.boostRamAndCleanCache()
                            val newLogs = engine.applyPerformanceProfile(
                                currentMode, mfrcEnabled, rayTracingBoost, bypassCharging, touchBoost, nosNetworkBoost
                            )
                            consoleLogs = consoleLogs + newLogs + "[LAUNCHER] Boosting ${game.name} (+$freedMB MB RAM Freed)"
                            val launched = engine.launchGame(game.packageName)
                            if (launched) {
                                Toast.makeText(context, "Meluncurkan ${game.name} dalam ${currentMode.title}!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Profil ${currentMode.title} aktif untuk ${game.name}! (Game belum terinstal)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
                4 -> KernelLogsTab(
                    logs = consoleLogs,
                    onClear = { consoleLogs = listOf("[SYSTEM] Log terminal dibersihkan.") }
                )
            }
        }
    }
}

@Composable
fun RedDimensityMechaLogo(isDiabloMode: Boolean) {
    val primaryColor = if (isDiabloMode) CrimsonNeon else CyanNeon
    val coreColor = if (isDiabloMode) GoldNeon else CrimsonNeon

    Canvas(
        modifier = Modifier
            .size(44.dp)
            .padding(2.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(
            color = primaryColor.copy(alpha = 0.22f),
            radius = w * 0.48f
        )

        val wingPath = Path().apply {
            moveTo(w * 0.08f, h * 0.25f)
            lineTo(w * 0.28f, h * 0.12f)
            lineTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.28f, h * 0.88f)
            lineTo(w * 0.08f, h * 0.75f)
            close()

            moveTo(w * 0.92f, h * 0.25f)
            lineTo(w * 0.72f, h * 0.12f)
            lineTo(w * 0.78f, h * 0.50f)
            lineTo(w * 0.72f, h * 0.88f)
            lineTo(w * 0.92f, h * 0.75f)
            close()
        }
        drawPath(wingPath, color = CrimsonNeon)

        val hexPath = Path().apply {
            moveTo(cx, h * 0.16f)
            lineTo(w * 0.74f, h * 0.33f)
            lineTo(w * 0.74f, h * 0.67f)
            lineTo(cx, h * 0.84f)
            lineTo(w * 0.26f, h * 0.67f)
            lineTo(w * 0.26f, h * 0.33f)
            close()
        }
        drawPath(hexPath, color = Color(0xFF101320))
        drawPath(hexPath, color = primaryColor, style = Stroke(width = 3f))

        val boltPath = Path().apply {
            moveTo(cx + 3f, h * 0.26f)
            lineTo(cx - 8f, cy + 2f)
            lineTo(cx + 1f, cy + 2f)
            lineTo(cx - 3f, h * 0.74f)
            lineTo(cx + 8f, cy - 2f)
            lineTo(cx - 1f, cy - 2f)
            close()
        }
        drawPath(boltPath, color = coreColor)
    }
}

@Composable
fun EnergyCubeTab(
    telemetry: SoCTelemetry,
    currentMode: PerformanceMode,
    onModeChange: (PerformanceMode) -> Unit,
    onCleanRam: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📊 TELEMETRI ALL-BIG-CORE CORTEX-A725 & MALI-G720",
                color = CyanNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            ClusterBar("PRIME CORE (1x Cortex-A725 @ 3.25GHz)", "${telemetry.cluster2FreqMHz} MHz", telemetry.cluster2FreqMHz / 3250f, CrimsonNeon)
            ClusterBar("PERF CLUSTER (3x Cortex-A725 @ 3.00GHz)", "${telemetry.cluster1FreqMHz} MHz", telemetry.cluster1FreqMHz / 3000f, GoldNeon)
            ClusterBar("EFF-BIG CLUSTER (4x Cortex-A725 @ 2.10GHz)", "${telemetry.cluster0FreqMHz} MHz", telemetry.cluster0FreqMHz / 2100f, CyanNeon)
            ClusterBar("GPU ARM Mali-G720 MC7 (7-Core RayTracing)", "${telemetry.gpuFreqMHz} MHz (${telemetry.gpuLoadPercent}%)", telemetry.gpuFreqMHz / 1300f, GreenNeon)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LPDDR5X RAM: ${telemetry.ramUsedMB} / ${telemetry.ramTotalMB} MB",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Button(
                    onClick = onCleanRam,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("🚀 BOOST RAM LPDDR5X", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PerformanceMode.entries.forEach { mode ->
                val isSelected = currentMode == mode
                val borderCol = when (mode) {
                    PerformanceMode.DIABLO -> CrimsonNeon
                    PerformanceMode.RISE -> GoldNeon
                    PerformanceMode.BALANCE -> CyanNeon
                    PerformanceMode.ECO -> GreenNeon
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) borderCol.copy(alpha = 0.22f) else CardBg)
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) borderCol else Color.DarkGray, RoundedCornerShape(10.dp))
                        .clickable { onModeChange(mode) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = mode.title,
                            color = if (isSelected) borderCol else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = mode.subtitle,
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = mode.maxClockGHz,
                        color = borderCol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PluginsTab(
    showCrosshair: Boolean,
    crosshairStyle: Int,
    crosshairColor: String,
    crosshairSize: Float,
    showFpsHud: Boolean,
    scoutAudio: Boolean,
    bypassCharging: Boolean,
    onToggleCrosshair: (Boolean) -> Unit,
    onChangeStyle: (Int) -> Unit,
    onChangeColor: (String) -> Unit,
    onChangeSize: (Float) -> Unit,
    onToggleFps: (Boolean) -> Unit,
    onToggleScoutAudio: (Boolean) -> Unit,
    onToggleBypass: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .weight(1.15f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎯 RED MAGIC OVERLAY & CUSTOM CROSSHAIR", color = CrimsonNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)

            PluginToggleRow(
                title = "Crosshair Assist (Touch-Passthrough)",
                desc = "Bidikan tengah layar untuk game FPS",
                checked = showCrosshair,
                onCheckedChange = onToggleCrosshair
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("➕ Cross", "⭕ Sniper", "🟢 Dot", "🔼 Mecha").forEachIndexed { idx, label ->
                    OutlinedButton(
                        onClick = { onChangeStyle(idx) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (crosshairStyle == idx) CyanNeon else Color.Gray
                        )
                    ) {
                        Text(label, color = if (crosshairStyle == idx) CyanNeon else Color.White, fontSize = 10.sp)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Warna & Ukuran:", color = Color.LightGray, fontSize = 11.sp)
                listOf(
                    "#00FF66" to GreenNeon,
                    "#FF003C" to CrimsonNeon,
                    "#00F0FF" to CyanNeon,
                    "#FFB800" to GoldNeon
                ).forEach { (hex, col) ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(col)
                            .border(2.dp, if (crosshairColor == hex) Color.White else Color.Transparent, RoundedCornerShape(11.dp))
                            .clickable { onChangeColor(hex) }
                    )
                }
                Slider(
                    value = crosshairSize,
                    onValueChange = onChangeSize,
                    valueRange = 0.6f..2.0f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⚡ HARDWARE PRO PLUGINS", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)

            PluginToggleRow(
                title = "Floating Real-Time FPS & Clock HUD",
                desc = "Tampilkan FPS Meter & Clock 3.25GHz di atas layar game",
                checked = showFpsHud,
                onCheckedChange = onToggleFps
            )

            PluginToggleRow(
                title = "Scout Mode (Footstep Sound Booster)",
                desc = "Equalizer DSP: Perjelas langkah kaki musuh (2kHz - 4kHz)",
                checked = scoutAudio,
                onCheckedChange = onToggleScoutAudio
            )

            PluginToggleRow(
                title = "Charge Separation (Bypass Charging)",
                desc = "Alirkan listrik langsung ke Motherboard agar baterai dingin",
                checked = bypassCharging,
                onCheckedChange = onToggleBypass
            )
        }
    }
}

@Composable
fun HyperEngineTab(
    mfrcEnabled: Boolean,
    rayTracingBoost: Boolean,
    touchBoost: Boolean,
    nosNetworkBoost: Boolean,
    onUpdateToggles: (Boolean, Boolean, Boolean, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "🚀 MEDIATEK HYPERENGINE & MAGT 3.0 TUNER (DIMENSITY 8400-ULTRA)",
                color = GoldNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        item {
            PluginToggleRow(
                title = "MFRC (MediaTek Frame Rate Converter 120/144 FPS)",
                desc = "Membuka kunci refresh rate 120Hz/144Hz & interpolasi frame halus",
                checked = mfrcEnabled,
                onCheckedChange = { onUpdateToggles(it, rayTracingBoost, touchBoost, nosNetworkBoost) }
            )
        }
        item {
            PluginToggleRow(
                title = "Mali-G720 MC7 Hardware Ray Tracing & SkiaVK Pipeline",
                desc = "Paksa rendering Vulkan (SkiaVK) untuk stabilitas FPS 1% Low",
                checked = rayTracingBoost,
                onCheckedChange = { onUpdateToggles(mfrcEnabled, it, touchBoost, nosNetworkBoost) }
            )
        }
        item {
            PluginToggleRow(
                title = "2160Hz Instant Touch Sampling & Gyroscope Calibrator",
                desc = "Kurangi latensi sentuhan layar & tingkatkan akurasi giroskop",
                checked = touchBoost,
                onCheckedChange = { onUpdateToggles(mfrcEnabled, rayTracingBoost, it, nosNetworkBoost) }
            )
        }
        item {
            PluginToggleRow(
                title = "MediaTek NOS 5G/Wi-Fi Low-Latency & Gaming DNS (1.1.1.1)",
                desc = "Prioritaskan paket data game & cegah lonjakan ping",
                checked = nosNetworkBoost,
                onCheckedChange = { onUpdateToggles(mfrcEnabled, rayTracingBoost, touchBoost, it) }
            )
        }
    }
}

@Composable
fun GameLauncherTab(
    games: List<GameItem>,
    onLaunchGame: (GameItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "🎮 RED MAGIC GAME SPACE LAUNCHER (OPTIMIZED FOR D8400-ULTRA)",
                color = CyanNeon,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        items(games) { game ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1E2E))
                    .border(1.dp, CrimsonNeon.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(game.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text("${game.genre} • Package: ${game.packageName}", color = Color.LightGray, fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusBadge(game.targetFps, GreenNeon)
                    Button(
                        onClick = { onLaunchGame(game) },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonNeon)
                    ) {
                        Text("⚡ BOOST & LAUNCH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun KernelLogsTab(logs: List<String>, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📟 MT6899 SYSFS & HYPERENGINE EXECUTION LOGS", color = GreenNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            OutlinedButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("CLEAR LOGS", color = Color.White, fontSize = 10.sp)
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF050608))
                .border(1.dp, Color.DarkGray, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(logs.reversed()) { log ->
                Text(
                    text = "> $log",
                    color = if (log.contains("DIABLO")) CrimsonNeon else GreenNeon,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun ClusterBar(label: String, valueText: String, progress: Float, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0.05f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.DarkGray
        )
    }
}

@Composable
fun PluginToggleRow(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B1E2E))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(desc, color = Color.LightGray, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = GreenNeon)
        )
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
