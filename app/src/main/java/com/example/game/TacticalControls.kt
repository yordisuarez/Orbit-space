package com.example.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AlienCoreOrange
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.CyanThruster
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.SpaceHudPanel
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun VirtualJoystick(
    label: String,
    accentColor: Color,
    tag: String,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    onMove: (Float, Float) -> Unit
) {
    var knobOffsetX by remember { mutableFloatStateOf(0f) }
    var knobOffsetY by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .testTag(tag)
                .semantics { contentDescription = label }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        down.consume()
                        isPressed = true

                        val centerX = this.size.width * 0.5f
                        val centerY = this.size.height * 0.5f
                        val maxRadius = min(centerX, centerY) * 0.64f

                        fun updateFromPosition(pos: Offset) {
                            val dx = pos.x - centerX
                            val dy = pos.y - centerY
                            val dist = hypot(dx, dy)
                            val clamped = min(dist, maxRadius)
                            if (dist > 0.001f) {
                                knobOffsetX = (dx / dist) * clamped
                                knobOffsetY = (dy / dist) * clamped
                                onMove(knobOffsetX / maxRadius, knobOffsetY / maxRadius)
                            } else {
                                knobOffsetX = 0f
                                knobOffsetY = 0f
                                onMove(0f, 0f)
                            }
                        }

                        updateFromPosition(down.position)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                break
                            }
                            change.consume()
                            updateFromPosition(change.position)
                        }

                        isPressed = false
                        knobOffsetX = 0f
                        knobOffsetY = 0f
                        onMove(0f, 0f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
                val baseRadius = this.size.minDimension * 0.45f
                val knobRadius = this.size.minDimension * 0.19f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x880B1324), Color(0x440B1324)),
                        center = center,
                        radius = baseRadius
                    ),
                    radius = baseRadius,
                    center = center
                )
                drawCircle(
                    color = accentColor.copy(alpha = if (isPressed) 0.78f else 0.38f),
                    radius = baseRadius,
                    center = center,
                    style = Stroke(width = if (isPressed) 2.6f else 1.8f)
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.2f),
                    radius = baseRadius * 0.56f,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
                for (i in 0 until 4) {
                    val angle = i * (Math.PI.toFloat() * 0.5f)
                    val inner = baseRadius * 0.78f
                    val outer = baseRadius * 0.94f
                    drawLine(
                        color = accentColor.copy(alpha = 0.45f),
                        start = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                        end = Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer),
                        strokeWidth = 2f
                    )
                }

                val knobCenter = Offset(center.x + knobOffsetX, center.y + knobOffsetY)
                drawCircle(
                    color = accentColor.copy(alpha = if (isPressed) 0.34f else 0.18f),
                    radius = knobRadius * 1.32f,
                    center = knobCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0.55f)),
                        center = knobCenter,
                        radius = knobRadius
                    ),
                    radius = knobRadius,
                    center = knobCenter
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = knobRadius * 0.42f,
                    center = knobCenter
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor.copy(alpha = 0.85f)
        )
    }
}

@Composable
fun RocketFireButton(
    cooldownRemaining: Float,
    inSafeZone: Boolean,
    onFireRocket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = !inSafeZone && cooldownRemaining <= 0.05f
    val rocketDesc = stringResource(R.string.cd_fire_rocket)
    val bgBrush = when {
        inSafeZone -> Brush.radialGradient(listOf(Color(0x99111C2D), Color(0x99070D19)))
        isReady -> Brush.radialGradient(listOf(Color(0xE6FB923C), Color(0xCC9A3412)))
        else -> Brush.radialGradient(listOf(Color(0xAA1E293B), Color(0xAA0F172A)))
    }
    val borderColor = when {
        inSafeZone -> SpaceHudBorder
        isReady -> AmberCredits
        else -> AlienCoreOrange.copy(alpha = 0.45f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(brush = bgBrush)
                .border(width = if (isReady) 2.4.dp else 1.5.dp, color = borderColor, shape = CircleShape)
                .testTag("rocket_fire_button")
                .semantics { contentDescription = rocketDesc }
                .clickable(enabled = isReady, onClick = onFireRocket)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = if (isReady) Color.White else TacticalMuted,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (cooldownRemaining > 0.05f) {
                        "${ceil(cooldownRemaining.toDouble()).toInt()}s"
                    } else {
                        stringResource(R.string.action_rocket)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isReady) Color.White else TacticalMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (inSafeZone) stringResource(R.string.hud_weapons_locked)
            else if (isReady) "READY" else "RELOAD",
            style = MaterialTheme.typography.labelSmall,
            color = if (isReady) AmberCredits else TacticalMuted
        )
    }
}

@Composable
fun AttackLaserButton(
    isAutoFireEnabled: Boolean,
    inSafeZone: Boolean,
    hasTarget: Boolean,
    targetInRange: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_alpha"
    )

    val activeBorder = when {
        inSafeZone -> SpaceHudBorder
        isAutoFireEnabled -> CrimsonDanger.copy(alpha = pulseAlpha)
        else -> CyanLaser.copy(alpha = 0.55f)
    }
    val activeBg = when {
        inSafeZone -> Brush.radialGradient(
            colors = listOf(Color(0x99111C2D), Color(0x99070D19))
        )
        isAutoFireEnabled -> Brush.radialGradient(
            colors = listOf(Color(0xE6FF2E4C), Color(0xCC7F1D1D))
        )
        else -> Brush.radialGradient(
            colors = listOf(Color(0xDD13233C), Color(0xCC091224))
        )
    }

    val laserDesc = stringResource(R.string.cd_laser_toggle)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(brush = activeBg)
                .border(
                    width = if (isAutoFireEnabled && !inSafeZone) 3.dp else 2.dp,
                    color = activeBorder,
                    shape = CircleShape
                )
                .testTag("laser_fire_button")
                .semantics { contentDescription = laserDesc }
                .clickable(enabled = !inSafeZone, onClick = onToggle)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (inSafeZone) Icons.Default.Security else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = when {
                        inSafeZone -> CyanLaser.copy(alpha = 0.5f)
                        isAutoFireEnabled -> Color.White
                        else -> CyanLaser
                    },
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = stringResource(
                        if (isAutoFireEnabled && !inSafeZone) R.string.action_laser_on else R.string.action_laser_off
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inSafeZone) TacticalMuted else Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val statusText = when {
            inSafeZone -> stringResource(R.string.hud_weapons_locked)
            !hasTarget -> stringResource(R.string.hud_no_target)
            targetInRange -> stringResource(R.string.hud_in_range)
            else -> stringResource(R.string.hud_out_of_range)
        }
        val statusColor = when {
            inSafeZone -> CyanLaser
            !hasTarget -> TacticalMuted
            targetInRange -> EmeraldHull
            else -> AmberCredits
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor
        )
    }
}

@Composable
fun JumpGateButton(
    gateLabel: String,
    onJump: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "jump_gate_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(560),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jump_gate_alpha"
    )
    val jumpDesc = stringResource(R.string.cd_jump_gate)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xEE00E5FF),
                        Color(0xDD0284C7),
                        Color(0xCC1E1B4B)
                    )
                )
            )
            .border(
                width = (2.2f + 1.4f * pulse).dp,
                color = Color.White.copy(alpha = 0.65f + 0.35f * pulse),
                shape = CircleShape
            )
            .clickable(onClick = onJump)
            .testTag("jump_gate_button")
            .semantics { contentDescription = "$jumpDesc ($gateLabel)" }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.action_jump_gate),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun TacticalMinimap(
    engine: SpaceGameEngine,
    frameTick: Long,
    onExpandMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tickMarker = frameTick
    val mapDesc = stringResource(R.string.cd_open_tactical_map)
    val isMapX1 = engine.activeMap == GameMap.MAP_X1

    Box(
        modifier = modifier
            .size(134.dp)
            .clip(CircleShape)
            .background(if (isMapX1) Color(0xDC080720) else SpaceHudPanel)
            .border(1.8.dp, if (isMapX1) Color(0xCC818CF8) else CyanLaser.copy(alpha = 0.72f), CircleShape)
            .clickable(onClick = onExpandMap)
            .padding(4.dp)
            .testTag("tactical_minimap")
            .semantics { contentDescription = mapDesc }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_EXPRESSION")
            tickMarker

            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val radarRadius = size.minDimension * 0.5f
            val viewRadius = GameConfig.HUD_MINIMAP_VIEW_RADIUS
            val scale = (radarRadius - 2f) / viewRadius
            val pulse = 0.55f + 0.45f * sin(engine.elapsedTime * 6.5f)

            val playerX = engine.player.x
            val playerY = engine.player.y
            val playerOriginDist = hypot(playerX, playerY)
            val baseCenter = Offset(
                center.x - playerX * scale,
                center.y - playerY * scale
            )

            val clipCircle = Path().apply {
                addOval(
                    Rect(
                        left = center.x - radarRadius,
                        top = center.y - radarRadius,
                        right = center.x + radarRadius,
                        bottom = center.y + radarRadius
                    )
                )
            }

            clipPath(clipCircle) {
                // Local radar range ring (half-range = 2,250 units) & crosshairs
                val gridColor = Color(0x2638BDF8)
                drawCircle(
                    color = gridColor,
                    radius = radarRadius * 0.5f,
                    center = center,
                    style = Stroke(width = 0.9f)
                )
                drawLine(
                    color = gridColor,
                    start = Offset(center.x - radarRadius, center.y),
                    end = Offset(center.x + radarRadius, center.y),
                    strokeWidth = 0.85f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(center.x, center.y - radarRadius),
                    end = Offset(center.x, center.y + radarRadius),
                    strokeWidth = 0.85f
                )

                if (engine.activeMap == GameMap.MAP_X1) {
                    // Straight red square edge walls & outer radiation fill when approaching MAP X-1 borders
                    val leftWallX = center.x + (0f - playerX) * scale
                    val rightWallX = center.x + (GameConfig.MAP_X1_SIZE - playerX) * scale
                    val topWallY = center.y + (0f - playerY) * scale
                    val bottomWallY = center.y + (GameConfig.MAP_X1_SIZE - playerY) * scale

                    val wallRadFill = Color(0x36FF1744)
                    if (leftWallX > 0f) {
                        drawRect(
                            color = wallRadFill,
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(leftWallX.coerceAtMost(size.width), size.height)
                        )
                    }
                    if (rightWallX < size.width) {
                        val rx = rightWallX.coerceAtLeast(0f)
                        drawRect(
                            color = wallRadFill,
                            topLeft = Offset(rx, 0f),
                            size = androidx.compose.ui.geometry.Size((size.width - rx).coerceAtLeast(0f), size.height)
                        )
                    }
                    if (topWallY > 0f) {
                        drawRect(
                            color = wallRadFill,
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width, topWallY.coerceAtMost(size.height))
                        )
                    }
                    if (bottomWallY < size.height) {
                        val by = bottomWallY.coerceAtLeast(0f)
                        drawRect(
                            color = wallRadFill,
                            topLeft = Offset(0f, by),
                            size = androidx.compose.ui.geometry.Size(size.width, (size.height - by).coerceAtLeast(0f))
                        )
                    }

                    // Draw crisp straight red border walls
                    val borderRectLeft = leftWallX
                    val borderRectTop = topWallY
                    val borderRectSize = GameConfig.MAP_X1_SIZE * scale
                    drawRect(
                        color = CrimsonDanger.copy(alpha = 0.94f),
                        topLeft = Offset(borderRectLeft, borderRectTop),
                        size = androidx.compose.ui.geometry.Size(borderRectSize, borderRectSize),
                        style = Stroke(width = 2.4f)
                    )

                    // Top-left Corner Safe Zone & Return Portal (1200, 1200) when within local radar range
                    val dxSafe = GameConfig.MAP_X1_SAFE_ZONE_X - playerX
                    val dySafe = GameConfig.MAP_X1_SAFE_ZONE_Y - playerY
                    if (hypot(dxSafe, dySafe) <= viewRadius + GameConfig.MAP_X1_SAFE_ZONE_RADIUS + 120f) {
                        val safeCenter = Offset(center.x + dxSafe * scale, center.y + dySafe * scale)
                        val safeR = GameConfig.MAP_X1_SAFE_ZONE_RADIUS * scale
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.22f),
                            radius = safeR,
                            center = safeCenter
                        )
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.9f),
                            radius = safeR,
                            center = safeCenter,
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.45f + 0.45f * pulse),
                            radius = 4.6f,
                            center = safeCenter,
                            style = Stroke(width = 1.4f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.2f,
                            center = safeCenter
                        )
                    }
                } else {
                    // Curved arcs of concentric Sector rings & outer red border when within local radar range
                    val ringDash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    if (abs(playerOriginDist - GameConfig.ZONE_1_MAX_RADIUS) <= viewRadius + 250f) {
                        drawCircle(
                            color = Color(0x994ADE80),
                            radius = GameConfig.ZONE_1_MAX_RADIUS * scale,
                            center = baseCenter,
                            style = Stroke(width = 1.4f, pathEffect = ringDash)
                        )
                    }
                    if (abs(playerOriginDist - GameConfig.ZONE_2_MAX_RADIUS) <= viewRadius + 250f) {
                        drawCircle(
                            color = Color(0x99FACC15),
                            radius = GameConfig.ZONE_2_MAX_RADIUS * scale,
                            center = baseCenter,
                            style = Stroke(width = 1.4f, pathEffect = ringDash)
                        )
                    }
                    if (abs(playerOriginDist - GameConfig.ZONE_3_MAX_RADIUS) <= viewRadius + 250f) {
                        drawCircle(
                            color = Color(0x99FB923C),
                            radius = GameConfig.ZONE_3_MAX_RADIUS * scale,
                            center = baseCenter,
                            style = Stroke(width = 1.4f, pathEffect = ringDash)
                        )
                    }
                    if (abs(playerOriginDist - GameConfig.MAP_RADIUS) <= viewRadius + 250f) {
                        drawCircle(
                            color = CrimsonDanger.copy(alpha = 0.9f),
                            radius = GameConfig.MAP_RADIUS * scale,
                            center = baseCenter,
                            style = Stroke(width = 2.4f)
                        )
                    }

                    // Central Safe Zone Base & Jump Gate Portal (220, 0) when within local radar range
                    if (playerOriginDist <= viewRadius + GameConfig.SAFE_ZONE_RADIUS + 120f) {
                        val safeR = GameConfig.SAFE_ZONE_RADIUS * scale
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.22f),
                            radius = safeR,
                            center = baseCenter
                        )
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.9f),
                            radius = safeR,
                            center = baseCenter,
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.8f,
                            center = baseCenter
                        )
                        val gatePos = Offset(
                            center.x + (GameConfig.MAIN_JUMP_GATE_X - playerX) * scale,
                            center.y + (GameConfig.MAIN_JUMP_GATE_Y - playerY) * scale
                        )
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.55f + 0.4f * pulse),
                            radius = 3.8f,
                            center = gatePos,
                            style = Stroke(width = 1.3f)
                        )
                    }
                }

                // Nearby Bonus & Cargo Boxes within 4,500-unit radar radius
                val boxCutoffSq = (viewRadius + 150f) * (viewRadius + 150f)
                for (box in engine.lootBoxes) {
                    val dx = box.x - playerX
                    val dy = box.y - playerY
                    if (dx * dx + dy * dy > boxCutoffSq) continue
                    val bx = center.x + dx * scale
                    val by = center.y + dy * scale
                    val isCargo = box.kind == LootBoxKind.CARGO_BOX
                    drawCircle(
                        color = if (isCargo) AmberCredits else Color(0xCCFACC15),
                        radius = if (isCargo) 2.7f else 1.9f,
                        center = Offset(bx, by)
                    )
                }

                // Local Radar Detection Range: only show Normal & BOSS Aliens within 2,800 world units
                val alienCutoffSq = GameConfig.ALIEN_RADAR_DETECTION_RANGE * GameConfig.ALIEN_RADAR_DETECTION_RANGE
                val lockedId = engine.lockedAlienId
                for (alien in engine.aliens) {
                    val dx = alien.x - playerX
                    val dy = alien.y - playerY
                    if (dx * dx + dy * dy > alienCutoffSq) continue
                    val ax = center.x + dx * scale
                    val ay = center.y + dy * scale
                    val tierColor = Color(alien.tier.primaryColorHex)

                    if (alien.id == lockedId) {
                        drawCircle(
                            color = CrimsonDanger.copy(alpha = 0.88f),
                            radius = if (alien.isBoss) alien.tier.minimapDotRadius + 6.0f else alien.tier.minimapDotRadius + 4.0f,
                            center = Offset(ax, ay),
                            style = Stroke(width = 1.5f)
                        )
                    }
                    if (alien.isBoss) {
                        drawCircle(
                            color = AmberCredits.copy(alpha = 0.28f + 0.40f * pulse),
                            radius = alien.tier.minimapDotRadius + 4.0f + 2.0f * pulse,
                            center = Offset(ax, ay)
                        )
                        drawCircle(
                            color = CrimsonDanger,
                            radius = alien.tier.minimapDotRadius + 2.0f,
                            center = Offset(ax, ay)
                        )
                        drawCircle(
                            color = AmberCredits,
                            radius = alien.tier.minimapDotRadius * 0.78f,
                            center = Offset(ax, ay)
                        )
                    } else {
                        drawCircle(
                            color = tierColor,
                            radius = (alien.tier.minimapDotRadius * 1.15f).coerceAtLeast(2.7f),
                            center = Offset(ax, ay)
                        )
                    }
                }

                // Active Autopilot dashed trajectory line from center toward waypoint (or edge beacon if > 4,500u)
                val apX = engine.autopilotTargetX
                val apY = engine.autopilotTargetY
                if (apX != null && apY != null) {
                    val dx = apX - playerX
                    val dy = apY - playerY
                    val worldDist = hypot(dx, dy)
                    if (worldDist > 1f) {
                        val ux = dx / worldDist
                        val uy = dy / worldDist
                        val withinRadar = worldDist <= viewRadius
                        val endOffset = if (withinRadar) {
                            Offset(center.x + dx * scale, center.y + dy * scale)
                        } else {
                            Offset(
                                center.x + ux * (radarRadius - 4f),
                                center.y + uy * (radarRadius - 4f)
                            )
                        }
                        drawLine(
                            color = CyanLaser.copy(alpha = 0.80f),
                            start = center,
                            end = endOffset,
                            strokeWidth = 1.6f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                        )
                        if (withinRadar) {
                            drawCircle(
                                color = CyanLaser.copy(alpha = 0.5f + 0.45f * pulse),
                                radius = 5.0f + 1.5f * pulse,
                                center = endOffset,
                                style = Stroke(width = 1.4f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.0f,
                                center = endOffset
                            )
                        } else {
                            // Edge directional indicator pointing toward off-radar waypoint
                            val edgeTip = Offset(
                                center.x + ux * (radarRadius - 1.5f),
                                center.y + uy * (radarRadius - 1.5f)
                            )
                            val edgeLeft = Offset(
                                center.x + ux * (radarRadius - 8.5f) - uy * 4.2f,
                                center.y + uy * (radarRadius - 8.5f) + ux * 4.2f
                            )
                            val edgeRight = Offset(
                                center.x + ux * (radarRadius - 8.5f) + uy * 4.2f,
                                center.y + uy * (radarRadius - 8.5f) - ux * 4.2f
                            )
                            val beacon = Path().apply {
                                moveTo(edgeTip.x, edgeTip.y)
                                lineTo(edgeLeft.x, edgeLeft.y)
                                lineTo(edgeRight.x, edgeRight.y)
                                close()
                            }
                            drawPath(path = beacon, color = CyanLaser.copy(alpha = 0.65f + 0.35f * pulse))
                        }
                    }
                }

                // Crisp bright cyan/white directional player arrow locked at exact center
                val angle = engine.player.angleRad
                val cosA = cos(angle)
                val sinA = sin(angle)
                drawCircle(
                    color = CyanLaser.copy(alpha = 0.28f),
                    radius = 12.5f,
                    center = center
                )
                val arrow = Path().apply {
                    moveTo(center.x + cosA * 10.5f, center.y + sinA * 10.5f)
                    lineTo(center.x + (-cosA * 6.5f - sinA * 6.5f), center.y + (-sinA * 6.5f + cosA * 6.5f))
                    lineTo(center.x - cosA * 2.5f, center.y - sinA * 2.5f)
                    lineTo(center.x + (-cosA * 6.5f + sinA * 6.5f), center.y + (-sinA * 6.5f - cosA * 6.5f))
                    close()
                }
                drawPath(path = arrow, color = CyanLaser)
                drawPath(path = arrow, color = Color.White, style = Stroke(width = 1.6f))
            }

            // Crisp radar rim
            drawCircle(
                color = CyanLaser.copy(alpha = 0.45f),
                radius = radarRadius - 0.8f,
                center = center,
                style = Stroke(width = 1.2f)
            )
        }
    }
}

@Composable
fun ExpandableTacticalMapOverlay(
    engine: SpaceGameEngine,
    frameTick: Long,
    onSetAutopilot: (Float, Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tickMarker = frameTick
    val labelPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            textAlign = AndroidPaint.Align.CENTER
            textSize = 26f
            isFakeBoldText = true
        }
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x6E020612))
            .pointerInput(Unit) {
                detectTapGestures { onClose() }
            }
            .testTag("tactical_map_overlay")
    ) {
        val isMapX1 = engine.activeMap == GameMap.MAP_X1
        // ~78% of screen height (capped by width so map never clips)
        val mapDiameter = min(maxHeight.value * 0.78f, maxWidth.value * 0.78f).coerceAtLeast(220f).dp
        val mapShape = if (isMapX1) RoundedCornerShape(14.dp) else CircleShape
        val mapBgColor = if (isMapX1) Color(0xD407081F) else Color(0xC4050C1B)
        val mapBorderColor = if (isMapX1) Color(0xCC6366F1) else CyanLaser.copy(alpha = 0.8f)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.pointerInput(Unit) {
                // Absorb taps on the header/controls area so only outside-map backdrop closes
                detectTapGestures { }
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xD6071122))
                    .border(1.dp, CyanLaser.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = CyanLaser,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(
                        if (isMapX1) R.string.tactical_map_x1_title else R.string.tactical_map_title
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanLaser,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "• ${stringResource(R.string.tactical_map_hint)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalMuted,
                    maxLines = 1,
                    softWrap = false
                )
                Button(
                    onClick = onClose,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonDanger,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("tactical_map_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.action_close),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(mapDiameter)
                    .clip(mapShape)
                    .background(mapBgColor)
                    .border(2.2.dp, mapBorderColor, mapShape)
                    .pointerInput(isMapX1) {
                        detectTapGestures { tapOffset ->
                            if (isMapX1) {
                                val pad = 14f
                                val usableW = (size.width - pad * 2f).coerceAtLeast(1f)
                                val usableH = (size.height - pad * 2f).coerceAtLeast(1f)
                                val normX = ((tapOffset.x - pad) / usableW).coerceIn(0f, 1f)
                                val normY = ((tapOffset.y - pad) / usableH).coerceIn(0f, 1f)
                                val wx = (normX * GameConfig.MAP_X1_SIZE)
                                    .coerceIn(GameConfig.MAP_X1_AUTOPILOT_MIN, GameConfig.MAP_X1_AUTOPILOT_MAX)
                                val wy = (normY * GameConfig.MAP_X1_SIZE)
                                    .coerceIn(GameConfig.MAP_X1_AUTOPILOT_MIN, GameConfig.MAP_X1_AUTOPILOT_MAX)
                                onSetAutopilot(wx, wy)
                            } else {
                                val centerX = size.width * 0.5f
                                val centerY = size.height * 0.5f
                                val usableRadius = min(centerX, centerY) - 10f
                                val dx = tapOffset.x - centerX
                                val dy = tapOffset.y - centerY
                                val tapDist = hypot(dx, dy)
                                if (tapDist <= usableRadius + 8f && usableRadius > 1f) {
                                    val worldScale = GameConfig.MAP_RADIUS / usableRadius
                                    val wx = dx * worldScale
                                    val wy = dy * worldScale
                                    onSetAutopilot(wx, wy)
                                } else {
                                    onClose()
                                }
                            }
                        }
                    }
                    .testTag("tactical_map_circle")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION")
                    tickMarker

                    val pulse = 0.55f + 0.45f * sin(engine.elapsedTime * 6.5f)
                    val pad = 14f
                    val mapSpanPx = (size.minDimension - pad * 2f).coerceAtLeast(1f)
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                    val mapPxRadius = size.minDimension * 0.5f - 10f
                    val scale = if (isMapX1) {
                        mapSpanPx / GameConfig.MAP_X1_SIZE
                    } else {
                        mapPxRadius / GameConfig.MAP_RADIUS
                    }

                    fun worldToCanvas(wx: Float, wy: Float): Offset {
                        return if (isMapX1) {
                            Offset(pad + wx * scale, pad + wy * scale)
                        } else {
                            Offset(center.x + wx * scale, center.y + wy * scale)
                        }
                    }

                    if (isMapX1) {
                        // Classic square 14000 x 14000 coordinate grid (every 2000 units)
                        val gridSteps = 7
                        for (i in 1 until gridSteps) {
                            val offsetPx = pad + (mapSpanPx * i / gridSteps)
                            drawLine(
                                color = Color(0x28818CF8),
                                start = Offset(offsetPx, pad),
                                end = Offset(offsetPx, pad + mapSpanPx),
                                strokeWidth = 1.1f
                            )
                            drawLine(
                                color = Color(0x28818CF8),
                                start = Offset(pad, offsetPx),
                                end = Offset(pad + mapSpanPx, offsetPx),
                                strokeWidth = 1.1f
                            )
                        }

                        // Glowing red square Radiation Zone perimeter
                        drawRect(
                            color = CrimsonDanger.copy(alpha = 0.25f),
                            topLeft = Offset(pad, pad),
                            size = androidx.compose.ui.geometry.Size(mapSpanPx, mapSpanPx),
                            style = Stroke(width = 6f)
                        )
                        drawRect(
                            color = CrimsonDanger.copy(alpha = 0.90f),
                            topLeft = Offset(pad, pad),
                            size = androidx.compose.ui.geometry.Size(mapSpanPx, mapSpanPx),
                            style = Stroke(width = 2.2f)
                        )

                        // Top-left Corner Safe Zone & Return Portal (1200, 1200)
                        val safeCenter = worldToCanvas(
                            GameConfig.MAP_X1_SAFE_ZONE_X,
                            GameConfig.MAP_X1_SAFE_ZONE_Y
                        )
                        val safeR = (GameConfig.MAP_X1_SAFE_ZONE_RADIUS * scale).coerceAtLeast(10f)
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.22f),
                            radius = safeR,
                            center = safeCenter
                        )
                        drawCircle(
                            color = CyanLaser,
                            radius = safeR,
                            center = safeCenter,
                            style = Stroke(width = 1.8f)
                        )
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.55f + 0.40f * pulse),
                            radius = 6.0f + 1.5f * pulse,
                            center = safeCenter,
                            style = Stroke(width = 1.8f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.8f,
                            center = safeCenter
                        )

                        drawIntoCanvas { canvas ->
                            labelPaint.textSize = (size.minDimension * 0.036f).coerceIn(15f, 24f)
                            labelPaint.color = 0xFF38BDF8.toInt()
                            canvas.nativeCanvas.drawText(
                                "GATE: BASE",
                                safeCenter.x + safeR + 28f,
                                safeCenter.y + 6f,
                                labelPaint
                            )
                            labelPaint.textSize = (size.minDimension * 0.044f).coerceIn(18f, 28f)
                            labelPaint.color = 0x55818CF8
                            canvas.nativeCanvas.drawText(
                                "X-1",
                                center.x,
                                center.y + labelPaint.textSize * 0.35f,
                                labelPaint
                            )
                        }
                    } else {
                        // Subtle polar crosshairs
                        val gridColor = Color(0x2838BDF8)
                        drawLine(
                            color = gridColor,
                            start = Offset(center.x - mapPxRadius, center.y),
                            end = Offset(center.x + mapPxRadius, center.y),
                            strokeWidth = 1.2f
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(center.x, center.y - mapPxRadius),
                            end = Offset(center.x, center.y + mapPxRadius),
                            strokeWidth = 1.2f
                        )

                        // 4 Concentric Sector Rings
                        val ringDash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        drawCircle(
                            color = Color(0x994ADE80),
                            radius = GameConfig.ZONE_1_MAX_RADIUS * scale,
                            center = center,
                            style = Stroke(width = 1.6f, pathEffect = ringDash)
                        )
                        drawCircle(
                            color = Color(0x99FACC15),
                            radius = GameConfig.ZONE_2_MAX_RADIUS * scale,
                            center = center,
                            style = Stroke(width = 1.6f, pathEffect = ringDash)
                        )
                        drawCircle(
                            color = Color(0x99FB923C),
                            radius = GameConfig.ZONE_3_MAX_RADIUS * scale,
                            center = center,
                            style = Stroke(width = 1.6f, pathEffect = ringDash)
                        )
                        drawCircle(
                            color = CrimsonDanger.copy(alpha = 0.85f),
                            radius = mapPxRadius,
                            center = center,
                            style = Stroke(width = 2.4f)
                        )

                        // Sector Labels S1..S4 along top vertical axis
                        drawIntoCanvas { canvas ->
                            labelPaint.textSize = (size.minDimension * 0.042f).coerceIn(18f, 30f)
                            val sectors = listOf(
                                Triple("S1", 2800f, 0xFF4ADE80.toInt()),
                                Triple("S2", 7500f, 0xFFFACC15.toInt()),
                                Triple("S3", 12500f, 0xFFFB923C.toInt()),
                                Triple("S4", 17500f, 0xFFC084FC.toInt())
                            )
                            for ((name, worldR, colorInt) in sectors) {
                                labelPaint.color = colorInt
                                canvas.nativeCanvas.drawText(
                                    name,
                                    center.x,
                                    center.y - worldR * scale + labelPaint.textSize * 0.35f,
                                    labelPaint
                                )
                            }
                        }

                        // Central Safe Zone Base (Cyan) & Jump Gate indicator
                        val safeR = (GameConfig.SAFE_ZONE_RADIUS * scale).coerceAtLeast(8f)
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.24f),
                            radius = safeR,
                            center = center
                        )
                        drawCircle(
                            color = CyanLaser,
                            radius = safeR,
                            center = center,
                            style = Stroke(width = 1.8f)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.2f,
                            center = center
                        )
                        val gatePt = worldToCanvas(GameConfig.MAIN_JUMP_GATE_X, GameConfig.MAIN_JUMP_GATE_Y)
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.65f + 0.35f * pulse),
                            radius = 3.4f,
                            center = gatePt,
                            style = Stroke(width = 1.4f)
                        )
                    }

                    // Bonus & Cargo Boxes
                    for (box in engine.lootBoxes) {
                        val bPos = worldToCanvas(box.x, box.y)
                        val isCargo = box.kind == LootBoxKind.CARGO_BOX
                        drawCircle(
                            color = if (isCargo) AmberCredits else Color(0xBBFACC15),
                            radius = if (isCargo) 3.4f else 2.2f,
                            center = bPos
                        )
                    }

                    val playerPos = worldToCanvas(
                        if (isMapX1) engine.player.x.coerceIn(0f, GameConfig.MAP_X1_SIZE) else engine.player.x,
                        if (isMapX1) engine.player.y.coerceIn(0f, GameConfig.MAP_X1_SIZE) else engine.player.y
                    )
                    val px = playerPos.x
                    val py = playerPos.y

                    // Faint translucent cyan 2,800-unit active ship sensor scanner range circle
                    val scannerRadiusPx = GameConfig.ALIEN_RADAR_DETECTION_RANGE * scale
                    drawCircle(
                        color = CyanLaser.copy(alpha = 0.065f),
                        radius = scannerRadiusPx,
                        center = playerPos
                    )
                    drawCircle(
                        color = CyanLaser.copy(alpha = 0.34f),
                        radius = scannerRadiusPx,
                        center = playerPos,
                        style = Stroke(width = 1.35f)
                    )

                    // Aliens (normal & pulsing BOSS) — ONLY rendered within 2,800-unit ship sensor range
                    val alienDetectSq = GameConfig.ALIEN_RADAR_DETECTION_RANGE * GameConfig.ALIEN_RADAR_DETECTION_RANGE
                    val lockedId = engine.lockedAlienId
                    for (alien in engine.aliens) {
                        val adx = alien.x - engine.player.x
                        val ady = alien.y - engine.player.y
                        if (adx * adx + ady * ady > alienDetectSq) continue

                        val aPos = worldToCanvas(alien.x, alien.y)
                        val tierColor = Color(alien.tier.primaryColorHex)

                        if (alien.id == lockedId) {
                            drawCircle(
                                color = CrimsonDanger,
                                radius = if (alien.isBoss) 11f else 8f,
                                center = aPos,
                                style = Stroke(width = 1.8f)
                            )
                        }

                        if (alien.isBoss) {
                            drawCircle(
                                color = AmberCredits.copy(alpha = 0.25f + 0.38f * pulse),
                                radius = 7.5f + 3.2f * pulse,
                                center = aPos
                            )
                            drawCircle(
                                color = CrimsonDanger.copy(alpha = 0.95f),
                                radius = 5.4f,
                                center = aPos
                            )
                            drawCircle(
                                color = AmberCredits,
                                radius = 2.6f,
                                center = aPos
                            )
                        } else {
                            drawCircle(
                                color = tierColor,
                                radius = (alien.tier.minimapDotRadius * 1.15f).coerceAtLeast(2.8f),
                                center = aPos
                            )
                        }
                    }

                    // Active Autopilot Dashed Route & Crosshair
                    val apX = engine.autopilotTargetX
                    val apY = engine.autopilotTargetY
                    if (apX != null && apY != null) {
                        val tPos = worldToCanvas(apX, apY)
                        val tx = tPos.x
                        val ty = tPos.y
                        drawLine(
                            color = CyanLaser.copy(alpha = 0.72f),
                            start = playerPos,
                            end = tPos,
                            strokeWidth = 2.0f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                        )
                        val wpR = 7.5f + 3f * pulse
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.45f + 0.45f * pulse),
                            radius = wpR,
                            center = tPos,
                            style = Stroke(width = 1.8f)
                        )
                        drawLine(
                            color = CyanLaser,
                            start = Offset(tx - wpR - 3f, ty),
                            end = Offset(tx + wpR + 3f, ty),
                            strokeWidth = 1.4f
                        )
                        drawLine(
                            color = CyanLaser,
                            start = Offset(tx, ty - wpR - 3f),
                            end = Offset(tx, ty + wpR + 3f),
                            strokeWidth = 1.4f
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = tPos
                        )
                    }

                    // High-visibility glowing Player directional chevron on full Tactical Map
                    drawCircle(
                        color = CyanLaser.copy(alpha = 0.16f + 0.14f * pulse),
                        radius = 20f + 4.5f * pulse,
                        center = playerPos
                    )
                    drawCircle(
                        color = CyanLaser.copy(alpha = 0.40f),
                        radius = 13.5f,
                        center = playerPos
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f + 0.35f * pulse),
                        radius = 16.5f + 2.5f * pulse,
                        center = playerPos,
                        style = Stroke(width = 1.4f)
                    )
                    val angle = engine.player.angleRad
                    val cosA = cos(angle)
                    val sinA = sin(angle)
                    val tip = Offset(px + cosA * 15f, py + sinA * 15f)
                    val left = Offset(px + (-cosA * 9f - sinA * 9f), py + (-sinA * 9f + cosA * 9f))
                    val right = Offset(px + (-cosA * 9f + sinA * 9f), py + (-sinA * 9f - cosA * 9f))
                    val chevron = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(left.x, left.y)
                        lineTo(px - cosA * 3.2f, py - sinA * 3.2f)
                        lineTo(right.x, right.y)
                        close()
                    }
                    drawPath(path = chevron, color = CyanLaser)
                    drawPath(path = chevron, color = Color.White, style = Stroke(width = 2.0f))
                }
            }
        }
    }
}

@Composable
fun TopTacticalHud(
    hud: HudSnapshot,
    onOpenHangar: () -> Unit,
    onToggleConfig: () -> Unit,
    onSelectFormation: (DroneFormation) -> Unit,
    onCancelAutopilot: () -> Unit,
    onClaimMission: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "hud_glow")
    val glowPulse by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(720),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_pulse_alpha"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
    ) {
        // Row 1: Credits/Kills/Level, Shield+Hull, Config, Hangar, Pause
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpaceHudPanel)
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("credits_hud_panel")
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = AmberCredits,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.hud_credits_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalMuted
                    )
                    Text(
                        text = "%,d".format(hud.credits),
                        style = MaterialTheme.typography.titleMedium,
                        color = AmberCredits
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(SpaceHudBorder)
                )

                Column {
                    Text(
                        text = stringResource(R.string.hud_kills_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalMuted
                    )
                    Text(
                        text = hud.kills.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TacticalWhite
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(SpaceHudBorder)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.width(62.dp)
                ) {
                    val xpRatio = (hud.playerXp.toFloat() / hud.playerXpToNext.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Text(
                        text = "${stringResource(R.string.hud_level_prefix)} ${hud.playerLevel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFC084FC)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF060C18))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(xpRatio)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFC084FC))
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .width(150.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpaceHudPanel)
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 5.dp)
                    .testTag("vitals_hud_panel")
            ) {
                val shdRatio = (hud.playerShield.toFloat() / hud.playerMaxShield.coerceAtLeast(1)).coerceIn(0f, 1f)
                val hpRatio = (hud.playerHp.toFloat() / hud.playerMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)

                HudVitalBarRow(
                    label = "${stringResource(R.string.hud_shield_label)}: ${hud.playerShield} / ${hud.playerMaxShield}",
                    ratio = shdRatio,
                    barColor = CyanThruster,
                    tag = "hud_shield_bar"
                )
                HudVitalBarRow(
                    label = "${stringResource(R.string.hud_hp_label)}: ${hud.playerHp} / ${hud.playerMaxHp}",
                    ratio = hpRatio,
                    barColor = if (hpRatio < 0.35f) CrimsonDanger else EmeraldHull,
                    tag = "hud_hp_bar"
                )
            }

            val cfgDesc = stringResource(R.string.cd_switch_config)
            val cfgColor = if (hud.activeConfig == 1) CyanLaser else AmberCredits
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpaceHudPanel)
                    .border(1.4.dp, cfgColor.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleConfig)
                    .padding(horizontal = 10.dp)
                    .testTag("config_toggle_button")
                    .semantics { contentDescription = cfgDesc }
            ) {
                Text(
                    text = stringResource(
                        if (hud.activeConfig == 1) R.string.hud_config_1 else R.string.hud_config_2
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = cfgColor
                )
            }

            // Interactive VANT Formation Dropdown Submenu Button
            val fmDesc = stringResource(R.string.cd_cycle_formation)
            val fmColor = Color(hud.activeFormation.accentHex)
            val fmOnCooldown = hud.formationCooldownRemaining > 0.05f
            var isFormationMenuOpen by remember { mutableStateOf(false) }
            val ownedFormations = remember(hud.equipment.ownedFormations) {
                DroneFormation.entries.filter { it in hud.equipment.ownedFormations }
            }

            Box {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isFormationMenuOpen -> Color(0xE60B2239)
                                fmOnCooldown -> Color(0xCC0D1628)
                                else -> SpaceHudPanel
                            }
                        )
                        .border(
                            width = if (isFormationMenuOpen) 1.8.dp else 1.4.dp,
                            color = when {
                                isFormationMenuOpen -> CyanLaser
                                fmOnCooldown -> TacticalMuted.copy(alpha = 0.5f)
                                else -> fmColor.copy(alpha = 0.8f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { isFormationMenuOpen = !isFormationMenuOpen }
                        .padding(horizontal = 9.dp)
                        .testTag("formation_menu_button")
                        .semantics { contentDescription = fmDesc }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = hud.activeFormation.shortCode,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (fmOnCooldown) TacticalMuted else fmColor,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (isFormationMenuOpen) CyanLaser else fmColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (fmOnCooldown) {
                            "${ceil(hud.formationCooldownRemaining.toDouble()).toInt()}s"
                        } else {
                            "FMN ${ownedFormations.size}/${DroneFormation.entries.size}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (fmOnCooldown) AmberCredits else TacticalMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                DropdownMenu(
                    expanded = isFormationMenuOpen,
                    onDismissRequest = { isFormationMenuOpen = false },
                    containerColor = Color(0xF0071020),
                    border = BorderStroke(
                        1.3.dp,
                        if (fmOnCooldown) AmberCredits.copy(alpha = 0.85f) else CyanLaser.copy(alpha = 0.78f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .testTag("formation_dropdown_menu")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (fmOnCooldown) {
                            Text(
                                text = "CD ${ceil(hud.formationCooldownRemaining.toDouble()).toInt()}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberCredits,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x33FBBF24))
                                    .border(1.dp, AmberCredits.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            )
                        }

                        for (formation in ownedFormations) {
                            val isActive = formation == hud.activeFormation
                            val accent = Color(formation.accentHex)
                            val canActivate = !isActive && !fmOnCooldown

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(
                                        if (isActive) Color(0x3800E5FF) else Color(0xB30D182C)
                                    )
                                    .border(
                                        width = if (isActive) 1.8.dp else 1.dp,
                                        color = if (isActive) CyanLaser else SpaceHudBorder,
                                        shape = RoundedCornerShape(17.dp)
                                    )
                                    .clickable {
                                        if (canActivate) {
                                            onSelectFormation(formation)
                                        }
                                        isFormationMenuOpen = false
                                    }
                                    .padding(horizontal = 10.dp)
                                    .testTag("formation_option_${formation.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Text(
                                    text = formation.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isActive -> Color.White
                                        fmOnCooldown -> TacticalMuted
                                        else -> accent
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CyanLaser,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val hangarDesc = stringResource(R.string.cd_hangar_open)
            val hasAlert = hud.mission.isReadyToClaim || hud.equipment.hasAnyReadyFusion
            val hangarBg = if (hud.inSafeZone) Color(0xCC0A2E42) else SpaceHudPanel
            val hangarBorder = when {
                hud.mission.isReadyToClaim -> EmeraldHull.copy(alpha = glowPulse)
                hud.inSafeZone -> CyanLaser.copy(alpha = glowPulse)
                else -> SpaceHudBorder
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(hangarBg)
                    .border(
                        width = if (hud.inSafeZone || hasAlert) 1.8.dp else 1.dp,
                        color = hangarBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(onClick = onOpenHangar)
                    .padding(horizontal = 11.dp)
                    .testTag("hangar_button")
                    .semantics { contentDescription = hangarDesc }
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = if (hud.inSafeZone) CyanLaser else AmberCredits,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.action_hangar),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hud.inSafeZone) CyanLaser else TacticalWhite
                )
                if (hasAlert) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (hud.mission.isReadyToClaim) EmeraldHull else AmberCredits)
                    )
                }
            }

            val pauseDesc = stringResource(R.string.cd_pause_game)
            IconButton(
                onClick = onTogglePause,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpaceHudPanel)
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = if (hud.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = pauseDesc,
                    tint = CyanLaser
                )
            }
        }

        // Row 2: Active Map Badge, Dynamic Zone Indicator Badge, Locked Target, Radiation Warning, Coords
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val isMapX1 = hud.activeMap == GameMap.MAP_X1
            val mapBadgeColor = if (isMapX1) Color(0xFFA5B4FC) else CyanLaser
            val mapBadgeBg = if (isMapX1) Color(0xCC1E1B4B) else Color(0xCC0A192F)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(mapBadgeBg)
                    .border(1.dp, mapBadgeColor.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("map_name_badge")
            ) {
                Text(
                    text = stringResource(
                        if (isMapX1) R.string.map_name_x1 else R.string.map_name_main
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = mapBadgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            val zoneLabel = if (isMapX1) {
                when {
                    hud.inSafeZone -> stringResource(R.string.hud_safe_zone)
                    hud.isTakingZoneRadiation -> stringResource(R.string.hud_radiation_warning)
                    else -> stringResource(R.string.hud_zone_x1_sector)
                }
            } else {
                when (hud.currentZone) {
                    SpaceZone.SAFE_ZONE -> stringResource(R.string.hud_safe_zone)
                    SpaceZone.ZONE_1_ALPHA -> stringResource(R.string.hud_zone_alpha)
                    SpaceZone.ZONE_2_BETA -> stringResource(R.string.hud_zone_beta)
                    SpaceZone.ZONE_3_GAMMA -> stringResource(R.string.hud_zone_gamma)
                    SpaceZone.ZONE_4_OMEGA -> stringResource(R.string.hud_zone_omega)
                }
            }
            val zoneColor = if (isMapX1) {
                when {
                    hud.inSafeZone -> CyanLaser
                    hud.isTakingZoneRadiation -> CrimsonDanger
                    else -> Color(0xFF818CF8)
                }
            } else {
                when (hud.currentZone) {
                    SpaceZone.SAFE_ZONE -> CyanLaser
                    SpaceZone.ZONE_1_ALPHA -> Color(0xFF4ADE80)
                    SpaceZone.ZONE_2_BETA -> Color(0xFFFACC15)
                    SpaceZone.ZONE_3_GAMMA -> Color(0xFFFB923C)
                    SpaceZone.ZONE_4_OMEGA -> Color(0xFFC084FC)
                }
            }
            val zoneBg = if (isMapX1) {
                when {
                    hud.inSafeZone -> Color(0xCC062838)
                    hud.isTakingZoneRadiation -> Color(0xCC3B0A14)
                    else -> Color(0xCC131338)
                }
            } else {
                when (hud.currentZone) {
                    SpaceZone.SAFE_ZONE -> Color(0xCC062838)
                    SpaceZone.ZONE_1_ALPHA -> Color(0xCC072415)
                    SpaceZone.ZONE_2_BETA -> Color(0xCC2A2006)
                    SpaceZone.ZONE_3_GAMMA -> Color(0xCC2C1306)
                    SpaceZone.ZONE_4_OMEGA -> Color(0xCC280A2E)
                }
            }
            val zoneIcon = when {
                hud.inSafeZone -> Icons.Default.Security
                hud.isTakingZoneRadiation || hud.currentZone == SpaceZone.ZONE_4_OMEGA -> Icons.Default.Warning
                else -> Icons.Default.GpsFixed
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(zoneBg)
                    .border(1.dp, zoneColor.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
                    .testTag("zone_indicator_badge")
            ) {
                Icon(
                    imageVector = zoneIcon,
                    contentDescription = null,
                    tint = zoneColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = zoneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = zoneColor
                )
            }

            if (hud.lockedTargetCallsign != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC2A0B14))
                        .border(1.dp, CrimsonDanger.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("locked_target_chip")
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = CrimsonDanger,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${hud.lockedTargetCallsign}  ${hud.lockedTargetHp}/${hud.lockedTargetMaxHp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalWhite
                    )
                }
            }

            if (hud.inRadiationWarning) {
                val radText = if (hud.isTakingZoneRadiation && hud.requiredChipWarning != null) {
                    "RADIATION • NEED ${hud.requiredChipWarning}"
                } else {
                    stringResource(R.string.hud_radiation_warning)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xDD7F1D1D))
                        .border(1.dp, CrimsonDanger, RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("radiation_warning_chip")
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberCredits,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = radText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            if (hud.isAutopilotActive) {
                val apCancelDesc = stringResource(R.string.cd_cancel_autopilot)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC062838))
                        .border(1.2.dp, CyanLaser.copy(alpha = glowPulse), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCancelAutopilot)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("autopilot_badge")
                        .semantics { contentDescription = apCancelDesc }
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = CyanLaser,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.hud_autopilot_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanLaser,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = "X:${hud.coordX} Y:${hud.coordY}",
                style = MaterialTheme.typography.labelSmall,
                color = TacticalMuted
            )
        }

        // Row 3: Compact Active Mission Tracker Widget (top-left HUD)
        HudMissionTracker(
            mission = hud.mission,
            glowPulse = glowPulse,
            onClaimMission = onClaimMission
        )
    }
}

@Composable
private fun HudMissionTracker(
    mission: MissionSnapshot,
    glowPulse: Float,
    onClaimMission: () -> Unit
) {
    val active = mission.activeMission
    val claimDesc = stringResource(R.string.cd_claim_mission)

    if (active == null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC06281A))
                .border(1.dp, EmeraldHull.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("hud_mission_tracker")
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = EmeraldHull,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.hud_missions_complete),
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldHull
            )
        }
        return
    }

    when {
        mission.isReadyToClaim -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xDD06351E))
                    .border(1.8.dp, EmeraldHull.copy(alpha = glowPulse), RoundedCornerShape(9.dp))
                    .clickable(onClick = onClaimMission)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("hud_mission_claim_button")
                    .semantics { contentDescription = claimDesc }
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldHull,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${active.code}: ${active.description} (${active.targetCount}/${active.targetCount})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(EmeraldHull)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hud_mission_claim),
                        style = MaterialTheme.typography.labelSmall,
                        color = SpaceVoid,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        mission.isLevelLocked -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC1B1626))
                    .border(1.dp, Color(0x88C084FC), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("hud_mission_tracker")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFC084FC),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${active.code} LOCKED — REACH LEVEL ${active.requiredLevel} TO UNLOCK",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE9D5FF),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        else -> {
            val ratio = (mission.currentMissionProgress.toFloat() / active.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpaceHudPanel)
                    .border(
                        1.dp,
                        if (active.isSectorFinal) AmberCredits.copy(alpha = 0.7f) else SpaceHudBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("hud_mission_tracker")
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = if (active.isSectorFinal) AmberCredits else CyanLaser,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${stringResource(R.string.hud_mission_prefix)}: ${active.description} (${mission.currentMissionProgress}/${active.targetCount})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalWhite,
                    maxLines = 1,
                    softWrap = false
                )
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF060C18))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active.isSectorFinal) AmberCredits else CyanLaser)
                    )
                }
            }
        }
    }
}

@Composable
private fun HudVitalBarRow(
    label: String,
    ratio: Float,
    barColor: Color,
    tag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(17.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF060C18))
            .border(0.8.dp, barColor.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(17.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor.copy(alpha = 0.88f))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                shadow = Shadow(
                    color = Color(0xEB000000),
                    offset = Offset(1f, 1.2f),
                    blurRadius = 2.5f
                )
            ),
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
