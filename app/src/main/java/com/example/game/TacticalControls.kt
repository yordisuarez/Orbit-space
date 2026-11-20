package com.example.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
fun TacticalMinimap(
    engine: SpaceGameEngine,
    frameTick: Long,
    modifier: Modifier = Modifier
) {
    val tickMarker = frameTick

    Box(
        modifier = modifier
            .size(134.dp)
            .clip(CircleShape)
            .background(SpaceHudPanel)
            .border(1.8.dp, CrimsonDanger.copy(alpha = 0.7f), CircleShape)
            .padding(6.dp)
            .testTag("tactical_minimap")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_EXPRESSION")
            tickMarker

            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val radarRadius = size.minDimension * 0.5f
            val usableRadius = radarRadius - 4f
            val scale = usableRadius / GameConfig.MAP_RADIUS

            val gridColor = Color(0x2238BDF8)
            drawLine(
                color = gridColor,
                start = Offset(center.x - radarRadius, center.y),
                end = Offset(center.x + radarRadius, center.y),
                strokeWidth = 0.9f
            )
            drawLine(
                color = gridColor,
                start = Offset(center.x, center.y - radarRadius),
                end = Offset(center.x, center.y + radarRadius),
                strokeWidth = 0.9f
            )

            drawCircle(
                color = Color(0x664ADE80),
                radius = GameConfig.ZONE_1_MAX_RADIUS * scale,
                center = center,
                style = Stroke(width = 1.0f)
            )
            drawCircle(
                color = Color(0x66FACC15),
                radius = GameConfig.ZONE_2_MAX_RADIUS * scale,
                center = center,
                style = Stroke(width = 1.0f)
            )
            drawCircle(
                color = Color(0x66FB923C),
                radius = GameConfig.ZONE_3_MAX_RADIUS * scale,
                center = center,
                style = Stroke(width = 1.0f)
            )
            drawCircle(
                color = CrimsonDanger.copy(alpha = 0.65f),
                radius = radarRadius - 1f,
                center = center,
                style = Stroke(width = 1.6f)
            )

            val safeZoneRadarR = (GameConfig.SAFE_ZONE_RADIUS * scale).coerceAtLeast(4.5f)
            drawCircle(
                color = CyanLaser.copy(alpha = 0.22f),
                radius = safeZoneRadarR,
                center = center
            )
            drawCircle(
                color = CyanLaser.copy(alpha = 0.85f),
                radius = safeZoneRadarR,
                center = center,
                style = Stroke(width = 1.2f)
            )
            drawCircle(
                color = CyanLaser,
                radius = 2.4f,
                center = center
            )

            for (box in engine.lootBoxes) {
                val bx = center.x + box.x * scale
                val by = center.y + box.y * scale
                drawCircle(
                    color = if (box.kind == LootBoxKind.CARGO_BOX) AmberCredits else Color(0xBBFACC15),
                    radius = if (box.kind == LootBoxKind.CARGO_BOX) 2.0f else 1.4f,
                    center = Offset(bx, by)
                )
            }

            val lockedId = engine.lockedAlienId
            for (alien in engine.aliens) {
                val ax = center.x + alien.x * scale
                val ay = center.y + alien.y * scale
                val tierColor = Color(alien.tier.primaryColorHex)
                if (alien.id == lockedId) {
                    drawCircle(
                        color = CrimsonDanger.copy(alpha = 0.7f),
                        radius = alien.tier.minimapDotRadius + 3.2f,
                        center = Offset(ax, ay),
                        style = Stroke(width = 1.4f)
                    )
                }
                if (alien.tier == AlienTier.DREADNOUGHT_BOSS) {
                    drawCircle(
                        color = tierColor.copy(alpha = 0.32f),
                        radius = alien.tier.minimapDotRadius + 2.2f,
                        center = Offset(ax, ay)
                    )
                }
                drawCircle(
                    color = tierColor,
                    radius = alien.tier.minimapDotRadius,
                    center = Offset(ax, ay)
                )
            }

            val px = center.x + engine.player.x * scale
            val py = center.y + engine.player.y * scale
            val shipAccent = Color(engine.player.shipClass.accentColorHex)
            drawCircle(
                color = shipAccent.copy(alpha = 0.35f),
                radius = 7.0f,
                center = Offset(px, py)
            )
            drawCircle(
                color = shipAccent,
                radius = 3.8f,
                center = Offset(px, py)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.85f),
                start = Offset(px, py),
                end = Offset(
                    px + cos(engine.player.angleRad) * 7.2f,
                    py + sin(engine.player.angleRad) * 7.2f
                ),
                strokeWidth = 1.5f
            )
        }
    }
}

@Composable
fun TopTacticalHud(
    hud: HudSnapshot,
    onOpenHangar: () -> Unit,
    onToggleConfig: () -> Unit,
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
                    .width(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpaceHudPanel)
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .testTag("vitals_hud_panel")
            ) {
                val shdRatio = (hud.playerShield.toFloat() / hud.playerMaxShield.coerceAtLeast(1)).coerceIn(0f, 1f)
                val hpRatio = (hud.playerHp.toFloat() / hud.playerMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)

                HudVitalBarRow(
                    label = "${stringResource(R.string.hud_shield_label)} ${hud.playerShield}/${hud.playerMaxShield}",
                    ratio = shdRatio,
                    barColor = CyanThruster
                )
                HudVitalBarRow(
                    label = "${stringResource(R.string.hud_hull_label)} ${hud.playerHp}/${hud.playerMaxHp}",
                    ratio = hpRatio,
                    barColor = if (hpRatio < 0.35f) CrimsonDanger else EmeraldHull
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

        // Row 2: Dynamic Zone Indicator Badge, Locked Target, Radiation Warning, Coords
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val zoneLabel = when (hud.currentZone) {
                SpaceZone.SAFE_ZONE -> stringResource(R.string.hud_safe_zone)
                SpaceZone.ZONE_1_ALPHA -> stringResource(R.string.hud_zone_alpha)
                SpaceZone.ZONE_2_BETA -> stringResource(R.string.hud_zone_beta)
                SpaceZone.ZONE_3_GAMMA -> stringResource(R.string.hud_zone_gamma)
                SpaceZone.ZONE_4_OMEGA -> stringResource(R.string.hud_zone_omega)
            }
            val zoneColor = when (hud.currentZone) {
                SpaceZone.SAFE_ZONE -> CyanLaser
                SpaceZone.ZONE_1_ALPHA -> Color(0xFF4ADE80)
                SpaceZone.ZONE_2_BETA -> Color(0xFFFACC15)
                SpaceZone.ZONE_3_GAMMA -> Color(0xFFFB923C)
                SpaceZone.ZONE_4_OMEGA -> Color(0xFFC084FC)
            }
            val zoneBg = when (hud.currentZone) {
                SpaceZone.SAFE_ZONE -> Color(0xCC062838)
                SpaceZone.ZONE_1_ALPHA -> Color(0xCC072415)
                SpaceZone.ZONE_2_BETA -> Color(0xCC2A2006)
                SpaceZone.ZONE_3_GAMMA -> Color(0xCC2C1306)
                SpaceZone.ZONE_4_OMEGA -> Color(0xCC280A2E)
            }
            val zoneIcon = when (hud.currentZone) {
                SpaceZone.SAFE_ZONE -> Icons.Default.Security
                SpaceZone.ZONE_4_OMEGA -> Icons.Default.Warning
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
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = barColor
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF060C18))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}
