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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
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
    onTouchStart: (() -> Unit)? = null,
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
                        onTouchStart?.invoke()

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
    val strings = GameStrings.current()
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
                        strings.ctrlRocket
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
            text = if (inSafeZone) strings.hudSafeZone
            else if (isReady) strings.ctrlReady else strings.ctrlReload,
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
                    text = if (isAutoFireEnabled && !inSafeZone) GameStrings.current().ctrlLaserOn else GameStrings.current().ctrlFire,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inSafeZone) TacticalMuted else Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val strings = GameStrings.current()
        val statusText = when {
            inSafeZone -> strings.hudSafeZone
            !hasTarget -> strings.ctrlNoTarget
            targetInRange -> strings.ctrlInRange
            else -> strings.ctrlOutOfRange
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
    targetSectorId: String?,
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
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = GameStrings.current().ctrlJump,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            if (!targetSectorId.isNullOrBlank()) {
                Text(
                    text = targetSectorId,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE0F2FE),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun StationInteractButton(
    stationKind: StationKind,
    stationName: String,
    isBlackMarket: Boolean,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = GameStrings.current()
    val transition = rememberInfiniteTransition(label = "station_dock_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "station_dock_alpha"
    )
    val gradientColors = when {
        stationKind == StationKind.REFINERY -> listOf(Color(0xEEFB923C), Color(0xDD9A3412), Color(0xCC2A1205))
        stationKind == StationKind.CRAFTING -> listOf(Color(0xEE34D399), Color(0xDD047857), Color(0xCC06281E))
        isBlackMarket -> listOf(Color(0xEEFACC15), Color(0xDDB45309), Color(0xCC271A04))
        else -> listOf(Color(0xEE38BDF8), Color(0xDD0369A1), Color(0xCC0A192F))
    }
    val subLabel = when {
        stationKind == StationKind.REFINERY -> strings.ctrlRefineryShort
        stationKind == StationKind.CRAFTING -> strings.ctrlCraftingShort
        isBlackMarket -> strings.ctrlBlackMarketShort
        else -> strings.ctrlMarketShort
    }
    val iconVector = when (stationKind) {
        StationKind.REFINERY -> Icons.Default.Build
        StationKind.CRAFTING -> Icons.Default.Handyman
        StationKind.LOCAL_MARKET -> Icons.Default.Storefront
    }
    val tag = when (stationKind) {
        StationKind.REFINERY -> "dock_refinery_button"
        StationKind.CRAFTING -> "dock_crafting_button"
        StationKind.LOCAL_MARKET -> "dock_market_button"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = gradientColors))
            .border(
                width = (2.2f + 1.3f * pulse).dp,
                color = Color.White.copy(alpha = 0.65f + 0.35f * pulse),
                shape = CircleShape
            )
            .clickable(onClick = onInteract)
            .testTag(tag)
            .semantics { contentDescription = "${strings.ctrlDock} $stationName" }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = strings.ctrlDock,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF8FAFC),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CircularFormationButton(
    activeFormation: DroneFormation,
    ownedFormationsSet: Set<DroneFormation>,
    cooldownRemaining: Float,
    onSelectFormation: (DroneFormation) -> Unit,
    modifier: Modifier = Modifier
) {
    val fmDesc = stringResource(R.string.cd_cycle_formation)
    val fmColor = Color(activeFormation.accentHex)
    val fmOnCooldown = cooldownRemaining > 0.05f
    var isFormationMenuOpen by remember { mutableStateOf(false) }
    val ownedFormations = remember(ownedFormationsSet) {
        DroneFormation.entries.filter { it in ownedFormationsSet }
    }

    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = when {
                            isFormationMenuOpen -> listOf(Color(0xEE0B2A46), Color(0xDD061325))
                            fmOnCooldown -> listOf(Color(0xCC1A2233), Color(0xCC0A101E))
                            else -> listOf(fmColor.copy(alpha = 0.28f), Color(0xDD081122))
                        }
                    )
                )
                .border(
                    width = if (isFormationMenuOpen) 2.2.dp else 1.6.dp,
                    color = when {
                        isFormationMenuOpen -> CyanLaser
                        fmOnCooldown -> TacticalMuted.copy(alpha = 0.55f)
                        else -> fmColor.copy(alpha = 0.88f)
                    },
                    shape = CircleShape
                )
                .clickable { isFormationMenuOpen = !isFormationMenuOpen }
                .testTag("formation_menu_button")
                .semantics { contentDescription = fmDesc }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = activeFormation.shortCode,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (fmOnCooldown) TacticalMuted else fmColor,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = if (fmOnCooldown) {
                        "${ceil(cooldownRemaining.toDouble()).toInt()}s"
                    } else {
                        "${ownedFormations.size}/${DroneFormation.entries.size}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    color = if (fmOnCooldown) AmberCredits else TacticalMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        DropdownMenu(
            expanded = isFormationMenuOpen,
            onDismissRequest = { isFormationMenuOpen = false },
            containerColor = Color(0xF2071020),
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
                        text = "CD ${ceil(cooldownRemaining.toDouble()).toInt()}s",
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
                    val isActive = formation == activeFormation
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
}

@Composable
fun TacticalMinimap(
    engine: SpaceGameEngine,
    frameTick: Long,
    onExpandMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tickMarker = frameTick
    val strings = GameStrings.current()
    val mapDesc = stringResource(R.string.cd_open_tactical_map)
    val sector = engine.currentSector
    val mapSize = sector.mapSize

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onExpandMap)
            .padding(2.dp)
            .testTag("tactical_minimap")
            .semantics { contentDescription = mapDesc }
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(Color(0xE0080D1E))
                .border(1.8.dp, sector.nodeColor.copy(alpha = 0.85f), CircleShape)
                .padding(3.dp)
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

                // Straight red square edge walls & outer radiation fill when approaching borders
                val leftWallX = center.x + (0f - playerX) * scale
                val rightWallX = center.x + (mapSize - playerX) * scale
                val topWallY = center.y + (0f - playerY) * scale
                val bottomWallY = center.y + (mapSize - playerY) * scale

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

                val borderRectSize = mapSize * scale
                drawRect(
                    color = CrimsonDanger.copy(alpha = 0.94f),
                    topLeft = Offset(leftWallX, topWallY),
                    size = androidx.compose.ui.geometry.Size(borderRectSize, borderRectSize),
                    style = Stroke(width = 2.4f)
                )

                // Central Safe Zone (Cities, Level 1, and Nucleus Black Market)
                if (sector.hasCentralSafeZone && sector.safeZoneRadius > 0f) {
                    val (sx, sy) = engine.activeSafeZoneCenter()
                    val dxSafe = sx - playerX
                    val dySafe = sy - playerY
                    if (hypot(dxSafe, dySafe) <= viewRadius + sector.safeZoneRadius + 160f) {
                        val safeCenter = Offset(center.x + dxSafe * scale, center.y + dySafe * scale)
                        val safeR = sector.safeZoneRadius * scale
                        drawCircle(
                            color = (if (sector.isNucleus) AmberCredits else CyanLaser).copy(alpha = 0.22f),
                            radius = safeR,
                            center = safeCenter
                        )
                        drawCircle(
                            color = if (sector.isNucleus) AmberCredits else CyanLaser,
                            radius = safeR,
                            center = safeCenter,
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                // Active bidirectional Jump Gate Portals within radar range
                val nextHop = engine.nextRouteHopSectorId
                for (portal in engine.activePortals) {
                    val pdx = portal.worldX - playerX
                    val pdy = portal.worldY - playerY
                    if (hypot(pdx, pdy) > viewRadius + 160f) continue
                    val pOffset = Offset(center.x + pdx * scale, center.y + pdy * scale)
                    val isRouteTarget = nextHop == portal.targetSectorId
                    val pColor = if (isRouteTarget) CyanLaser else portal.accentColor
                    drawCircle(
                        color = pColor.copy(alpha = 0.55f + 0.45f * pulse),
                        radius = if (isRouteTarget) 5.5f else 4.2f,
                        center = pOffset,
                        style = Stroke(width = 1.5f)
                    )
                    drawCircle(color = Color.White, radius = 2.0f, center = pOffset)
                }

                // Central Interactive Stations (Refinery & Isolated Local / Black Market)
                for (station in engine.activeStations) {
                    val sdx = station.worldX - playerX
                    val sdy = station.worldY - playerY
                    if (hypot(sdx, sdy) > viewRadius + 160f) continue
                    val sOffset = Offset(center.x + sdx * scale, center.y + sdy * scale)
                    val sColor = when {
                        station.kind == StationKind.REFINERY -> Color(0xFFFB923C)
                        station.kind == StationKind.CRAFTING -> Color(0xFF34D399)
                        sector.isNucleus -> AmberCredits
                        else -> CyanLaser
                    }
                    drawRect(
                        color = sColor,
                        topLeft = Offset(sOffset.x - 3.6f, sOffset.y - 3.6f),
                        size = androidx.compose.ui.geometry.Size(7.2f, 7.2f)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(sOffset.x - 1.5f, sOffset.y - 1.5f),
                        size = androidx.compose.ui.geometry.Size(3.0f, 3.0f)
                    )
                }

                // Nearby Mineable Resource Nodes within radar radius
                val nodeCutoffSq = (viewRadius + 150f) * (viewRadius + 150f)
                for (node in engine.resourceNodes) {
                    val ndx = node.x - playerX
                    val ndy = node.y - playerY
                    if (ndx * ndx + ndy * ndy > nodeCutoffSq) continue
                    val nx = center.x + ndx * scale
                    val ny = center.y + ndy * scale
                    val nodeColor = Color(node.resourceType.colorHex)
                    if (node.resourceType.isYellowCrystal) {
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.38f + 0.35f * pulse),
                            radius = 4.6f,
                            center = Offset(nx, ny)
                        )
                    }
                    drawCircle(
                        color = nodeColor,
                        radius = if (node.resourceType.isYellowCrystal) 2.7f else 2.0f,
                        center = Offset(nx, ny)
                    )
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

                // Direct Tap-to-Move faint trajectory line & glowing destination crosshair (or edge beacon if off-radar)
                val target = engine.autoMoveTarget
                if (target != null) {
                    val dx = target.x - playerX
                    val dy = target.y - playerY
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
                            color = CyanLaser.copy(alpha = 0.50f),
                            start = center,
                            end = endOffset,
                            strokeWidth = 1.3f,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(4.5f, 4f),
                                -engine.elapsedTime * 18f
                            )
                        )
                        if (withinRadar) {
                            val ringR = 4.2f + 1.4f * pulse
                            drawCircle(
                                color = CyanLaser.copy(alpha = 0.22f),
                                radius = ringR + 2.2f,
                                center = endOffset
                            )
                            drawCircle(
                                color = CyanLaser.copy(alpha = 0.65f + 0.30f * pulse),
                                radius = ringR,
                                center = endOffset,
                                style = Stroke(width = 1.3f)
                            )
                            drawLine(
                                color = CyanLaser.copy(alpha = 0.9f),
                                start = Offset(endOffset.x - ringR - 2.2f, endOffset.y),
                                end = Offset(endOffset.x + ringR + 2.2f, endOffset.y),
                                strokeWidth = 1.0f
                            )
                            drawLine(
                                color = CyanLaser.copy(alpha = 0.9f),
                                start = Offset(endOffset.x, endOffset.y - ringR - 2.2f),
                                end = Offset(endOffset.x, endOffset.y + ringR + 2.2f),
                                strokeWidth = 1.0f
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.7f,
                                center = endOffset
                            )
                        } else {
                            // Edge directional indicator pointing toward off-radar destination
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

            // Corner expand icon badge directly on the radar rim
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xE6041022))
                    .border(1.dp, CyanLaser.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = null,
                    tint = CyanLaser,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Clear "Tap to Open / Tocar para Abrir" badge below radar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xDE071326))
                .border(1.dp, CyanLaser.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("minimap_tap_to_open_badge")
        ) {
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = null,
                tint = CyanLaser,
                modifier = Modifier.size(9.dp)
            )
            Text(
                text = strings.minimapTapToOpen,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = CyanLaser,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun ExpandableTacticalMapOverlay(
    engine: SpaceGameEngine,
    frameTick: Long,
    onTapMapToMove: (Float, Float) -> Unit,
    onOpenGalaxyMap: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tickMarker = frameTick
    val lang = LocalGameLanguage.current
    val strings = GameStrings.of(lang)
    val sector = engine.currentSector
    val mapSize = sector.mapSize

    val labelPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            textAlign = AndroidPaint.Align.CENTER
            textSize = 24f
            isFakeBoldText = true
        }
    }

    val backdropInteraction = remember { MutableInteractionSource() }
    val contentInteraction = remember { MutableInteractionSource() }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x9E020612))
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onClose
            )
            .testTag("tactical_map_overlay")
    ) {
        val mapDiameter = min(maxHeight.value * 0.72f, maxWidth.value * 0.72f).coerceAtLeast(188f).dp
        val mapShape = RoundedCornerShape(14.dp)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clickable(
                    interactionSource = contentInteraction,
                    indication = null,
                    onClick = {}
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xE6071122))
                    .border(1.2.dp, sector.nodeColor.copy(alpha = 0.78f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = sector.nodeColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${strings.sectorMapTitle} • ${sector.id} (${sector.name.uppercase()})",
                    style = MaterialTheme.typography.labelLarge,
                    color = sector.nodeColor,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Button(
                    onClick = onOpenGalaxyMap,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0EA5E9),
                        contentColor = Color(0xFF040A16)
                    ),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("tactical_to_galaxy_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = strings.hudGalaxy,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
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
                        text = strings.actionClose,
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
                    .background(Color(0xE8060B1A))
                    .border(2.2.dp, sector.nodeColor.copy(alpha = 0.88f), mapShape)
                    .pointerInput(sector.id, mapSize) {
                        detectTapGestures { tapOffset ->
                            val minDim = min(size.width.toFloat(), size.height.toFloat())
                            val pad = (minDim * 0.04f).coerceIn(10f, 18f)
                            val mapSpanPx = (minDim - pad * 2f).coerceAtLeast(1f)
                            val originX = (size.width.toFloat() - mapSpanPx) * 0.5f
                            val originY = (size.height.toFloat() - mapSpanPx) * 0.5f
                            val normX = ((tapOffset.x - originX) / mapSpanPx).coerceIn(0f, 1f)
                            val normY = ((tapOffset.y - originY) / mapSpanPx).coerceIn(0f, 1f)
                            val targetWorldX = (normX * mapSize).coerceIn(120f, mapSize - 120f)
                            val targetWorldY = (normY * mapSize).coerceIn(120f, mapSize - 120f)
                            onTapMapToMove(targetWorldX, targetWorldY)
                        }
                    }
                    .testTag("tactical_map_circle")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    @Suppress("UNUSED_EXPRESSION")
                    tickMarker

                    val pulse = 0.55f + 0.45f * sin(engine.elapsedTime * 6.5f)
                    val minDim = size.minDimension
                    val pad = (minDim * 0.04f).coerceIn(10f, 18f)
                    val mapSpanPx = (minDim - pad * 2f).coerceAtLeast(1f)
                    val originX = (size.width - mapSpanPx) * 0.5f
                    val originY = (size.height - mapSpanPx) * 0.5f
                    val center = Offset(originX + mapSpanPx * 0.5f, originY + mapSpanPx * 0.5f)
                    val scale = mapSpanPx / mapSize

                    fun worldToCanvas(wx: Float, wy: Float): Offset =
                        Offset(originX + wx * scale, originY + wy * scale)

                    val gridSteps = 7
                    for (i in 1 until gridSteps) {
                        val fracPx = mapSpanPx * i / gridSteps
                        drawLine(
                            color = sector.nodeColor.copy(alpha = 0.16f),
                            start = Offset(originX + fracPx, originY),
                            end = Offset(originX + fracPx, originY + mapSpanPx),
                            strokeWidth = 1.1f
                        )
                        drawLine(
                            color = sector.nodeColor.copy(alpha = 0.16f),
                            start = Offset(originX, originY + fracPx),
                            end = Offset(originX + mapSpanPx, originY + fracPx),
                            strokeWidth = 1.1f
                        )
                    }

                    // Glowing red square Radiation Zone perimeter
                    drawRect(
                        color = CrimsonDanger.copy(alpha = 0.25f),
                        topLeft = Offset(originX, originY),
                        size = androidx.compose.ui.geometry.Size(mapSpanPx, mapSpanPx),
                        style = Stroke(width = 6f)
                    )
                    drawRect(
                        color = CrimsonDanger.copy(alpha = 0.90f),
                        topLeft = Offset(originX, originY),
                        size = androidx.compose.ui.geometry.Size(mapSpanPx, mapSpanPx),
                        style = Stroke(width = 2.2f)
                    )

                    // Central Safe Zone (Capital Cities, Level 1 Outposts, Nucleus Black Market)
                    if (sector.hasCentralSafeZone && sector.safeZoneRadius > 0f) {
                        val (sx, sy) = engine.activeSafeZoneCenter()
                        val safeCenter = worldToCanvas(sx, sy)
                        val safeR = (sector.safeZoneRadius * scale).coerceAtLeast(10f)
                        val ringColor = if (sector.isNucleus) AmberCredits else CyanLaser
                        drawCircle(color = ringColor.copy(alpha = 0.20f), radius = safeR, center = safeCenter)
                        drawCircle(color = ringColor, radius = safeR, center = safeCenter, style = Stroke(width = 1.8f))
                        if (sector.isNucleus) {
                            drawCircle(color = CyanLaser, radius = safeR * 0.65f, center = safeCenter, style = Stroke(width = 1.3f))
                        }
                    }

                    // All bidirectional Jump Gate Portals in this sector
                    val nextHop = engine.nextRouteHopSectorId
                    for (portal in engine.activePortals) {
                        val pt = worldToCanvas(portal.worldX, portal.worldY)
                        val isRouteTarget = nextHop == portal.targetSectorId
                        val pColor = if (isRouteTarget) CyanLaser else portal.accentColor
                        drawCircle(
                            color = pColor.copy(alpha = 0.55f + 0.40f * pulse),
                            radius = if (isRouteTarget) 8.5f + 2f * pulse else 6.0f + 1.2f * pulse,
                            center = pt,
                            style = Stroke(width = if (isRouteTarget) 2.2f else 1.7f)
                        )
                        drawCircle(color = Color.White, radius = 2.8f, center = pt)
                        drawIntoCanvas { canvas ->
                            labelPaint.textSize = (size.minDimension * 0.032f).coerceIn(12f, 20f)
                            labelPaint.color = if (isRouteTarget) 0xFF00E5FF.toInt() else 0xFFE2E8F0.toInt()
                            val txt = if (isRouteTarget) "★ ${portal.targetSectorId}" else portal.targetSectorId
                            canvas.nativeCanvas.drawText(txt, pt.x, pt.y + 18f, labelPaint)
                        }
                    }

                    drawIntoCanvas { canvas ->
                        labelPaint.textSize = (size.minDimension * 0.044f).coerceIn(16f, 26f)
                        labelPaint.color = 0x44818CF8
                        canvas.nativeCanvas.drawText(
                            sector.id,
                            center.x,
                            center.y + labelPaint.textSize * 0.35f,
                            labelPaint
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

                    // Central Interactive Stations (Refinery, Crafting Station, Local / Black Market)
                    for (station in engine.activeStations) {
                        val stPos = worldToCanvas(station.worldX, station.worldY)
                        val stColor = when {
                            station.kind == StationKind.REFINERY -> Color(0xFFFB923C)
                            station.kind == StationKind.CRAFTING -> Color(0xFF34D399)
                            sector.isNucleus -> AmberCredits
                            else -> CyanLaser
                        }
                        drawRect(
                            color = stColor,
                            topLeft = Offset(stPos.x - 4.2f, stPos.y - 4.2f),
                            size = androidx.compose.ui.geometry.Size(8.4f, 8.4f)
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(stPos.x - 1.6f, stPos.y - 1.6f),
                            size = androidx.compose.ui.geometry.Size(3.2f, 3.2f)
                        )
                    }

                    val playerPos = worldToCanvas(
                        engine.player.x.coerceIn(0f, mapSize),
                        engine.player.y.coerceIn(0f, mapSize)
                    )
                    val px = playerPos.x
                    val py = playerPos.y

                    // Translucent blue radar circle sized to 1 grid square total diameter (radius = 0.5 grid cell)
                    val gridCellPx = mapSpanPx / gridSteps
                    val scannerRadiusPx = gridCellPx * 0.5f
                    drawCircle(
                        color = CyanLaser.copy(alpha = 0.08f),
                        radius = scannerRadiusPx,
                        center = playerPos
                    )
                    drawCircle(
                        color = CyanLaser.copy(alpha = 0.38f),
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

                    // Direct Tap-to-Move faint trajectory line & clean glowing destination crosshair/ring
                    val moveTarget = engine.autoMoveTarget
                    if (moveTarget != null) {
                        val tPos = worldToCanvas(moveTarget.x, moveTarget.y)
                        val tx = tPos.x
                        val ty = tPos.y
                        drawLine(
                            color = CyanLaser.copy(alpha = 0.45f),
                            start = playerPos,
                            end = tPos,
                            strokeWidth = 1.8f,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(8f, 6f),
                                -engine.elapsedTime * 28f
                            )
                        )
                        val ringR = 7.5f + 2.4f * pulse
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.18f),
                            radius = ringR + 3.5f,
                            center = tPos
                        )
                        drawCircle(
                            color = CyanLaser.copy(alpha = 0.65f + 0.30f * pulse),
                            radius = ringR,
                            center = tPos,
                            style = Stroke(width = 1.7f)
                        )
                        drawLine(
                            color = CyanLaser.copy(alpha = 0.92f),
                            start = Offset(tx - ringR - 3.5f, ty),
                            end = Offset(tx + ringR + 3.5f, ty),
                            strokeWidth = 1.3f
                        )
                        drawLine(
                            color = CyanLaser.copy(alpha = 0.92f),
                            start = Offset(tx, ty - ringR - 3.5f),
                            end = Offset(tx, ty + ringR + 3.5f),
                            strokeWidth = 1.3f
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.4f,
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

            // Minimalist single-line tap hint / active destination coordinates (no Autopilot buttons)
            val activeTarget = engine.autoMoveTarget
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xD6071122))
                    .border(1.dp, CyanLaser.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("tactical_map_hint_strip")
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = CyanLaser,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = if (activeTarget != null) {
                        "→ X:${activeTarget.x.toInt()}  Y:${activeTarget.y.toInt()}"
                    } else {
                        strings.sectorMapTapHint
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (activeTarget != null) CyanLaser else Color(0xFFCBD5E1),
                    fontWeight = if (activeTarget != null) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TopTacticalHud(
    hud: HudSnapshot,
    onOpenMenu: () -> Unit,
    onOpenGalaxyMap: () -> Unit,
    onToggleConfig: () -> Unit,
    onClaimMission: () -> Unit,
    onTogglePause: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalGameLanguage.current
    val strings = GameStrings.of(lang)
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
    val hasMenuAlert = hud.mission.isReadyToClaim || hud.equipment.hasAnyReadyFusion

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(end = 114.dp)
    ) {
        // Row 1: Unified Compact MENU Button + Stats (Credits/Kills/Level) + CFG + Galaxy + Pause
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (hasMenuAlert) Color(0xCC0A2E42) else SpaceHudPanel)
                    .border(
                        width = if (hasMenuAlert) 1.5.dp else 1.2.dp,
                        color = if (hasMenuAlert) CyanLaser.copy(alpha = glowPulse) else CyanLaser.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onOpenMenu)
                    .padding(horizontal = 9.dp)
                    .testTag("hud_menu_button")
                    .semantics { contentDescription = strings.menuTitle }
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = CyanLaser,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.hudMenu,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TacticalWhite,
                    maxLines = 1,
                    softWrap = false
                )
                if (hasMenuAlert) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (hud.mission.isReadyToClaim) EmeraldHull else AmberCredits)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpaceHudPanel)
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp)
                    .testTag("credits_hud_panel")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = strings.hudCredits,
                        tint = AmberCredits,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "%,d".format(hud.credits),
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberCredits,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(SpaceHudBorder)
                )

                Text(
                    text = "${strings.hudKills.take(1)}:${hud.kills}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalWhite,
                    maxLines = 1,
                    softWrap = false
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(SpaceHudBorder)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.width(38.dp)
                ) {
                    val xpRatio = (hud.playerXp.toFloat() / hud.playerXpToNext.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Text(
                        text = "${strings.hudLevelPrefix}${hud.playerLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFC084FC),
                        maxLines = 1,
                        softWrap = false
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF060C18))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(xpRatio)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFC084FC))
                        )
                    }
                }
            }

            val cfgDesc = stringResource(R.string.cd_switch_config)
            val cfgColor = if (hud.activeConfig == 1) CyanLaser else AmberCredits
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpaceHudPanel)
                    .border(1.2.dp, cfgColor.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleConfig)
                    .padding(horizontal = 8.dp)
                    .testTag("config_toggle_button")
                    .semantics { contentDescription = cfgDesc }
            ) {
                Text(
                    text = if (hud.activeConfig == 1) strings.hudConfig1 else strings.hudConfig2,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = cfgColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val galaxyDesc = stringResource(R.string.cd_open_galaxy_map)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0A1C33))
                    .border(
                        width = if (hud.nextRouteHopSectorId != null) 1.5.dp else 1.dp,
                        color = if (hud.nextRouteHopSectorId != null) CyanLaser.copy(alpha = glowPulse) else Color(0xCC38BDF8),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onOpenGalaxyMap)
                    .padding(horizontal = 8.dp)
                    .testTag("galaxy_map_button")
                    .semantics { contentDescription = galaxyDesc }
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = CyanLaser,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = strings.hudGalaxy,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanLaser,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val pauseDesc = stringResource(R.string.cd_pause_game)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SpaceHudPanel)
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(8.dp))
                    .clickable(onClick = onTogglePause)
                    .testTag("pause_button")
                    .semantics { contentDescription = pauseDesc }
            ) {
                Icon(
                    imageVector = if (hud.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = CyanLaser,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Row 2: Compact Sector Code Badge, Dynamic Safe Zone / Risk Badge, Route Hop, Target, Coords
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val activeSector = GalaxyMapGraph.sectorOf(hud.currentSectorId)
            val sectorColor = activeSector.nodeColor
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(activeSector.zoneType.badgeBgColor)
                    .border(1.dp, sectorColor.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenGalaxyMap)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("map_name_badge")
            ) {
                Text(
                    text = activeSector.id,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = sectorColor,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            val zoneLabel = when {
                hud.isTakingZoneRadiation -> strings.hudRadiationWarning
                hud.inSafeZone -> if (activeSector.isNucleus) strings.hudSafeZoneBlackMarket else strings.hudSafeZone
                else -> GameStrings.zoneShortLabel(activeSector.zoneType, lang)
            }
            val zoneColor = when {
                hud.isTakingZoneRadiation -> CrimsonDanger
                hud.inSafeZone -> CyanLaser
                else -> activeSector.zoneType.badgeColor
            }
            val zoneBg = when {
                hud.isTakingZoneRadiation -> Color(0xCC3B0A14)
                hud.inSafeZone -> Color(0xCC062838)
                else -> activeSector.zoneType.badgeBgColor
            }
            val zoneIcon = when {
                hud.inSafeZone -> Icons.Default.Security
                hud.isTakingZoneRadiation || activeSector.zoneType == GalaxyZoneType.NUCLEUS -> Icons.Default.Warning
                else -> Icons.Default.GpsFixed
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(zoneBg)
                    .border(1.dp, zoneColor.copy(alpha = 0.78f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
                    .testTag("zone_indicator_badge")
            ) {
                Icon(
                    imageVector = zoneIcon,
                    contentDescription = null,
                    tint = zoneColor,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = zoneLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = zoneColor
                )
            }

            if (hud.overchargedAmmo > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC1E1B08))
                        .border(1.dp, AmberCredits.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("plasma_ammo_badge")
                ) {
                    Text(
                        text = "${strings.hudOverchargePrefix} ×${hud.overchargedAmmo}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberCredits,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (hud.miningNodeName != null && hud.miningProgress > 0f) {
                val pct = (hud.miningProgress * 100f).toInt().coerceIn(0, 100)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC1E1B08))
                        .border(1.dp, AmberCredits.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("mining_progress_badge")
                ) {
                    Text(
                        text = "${strings.hudMiningPrefix} ${hud.miningNodeName.uppercase()} $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberCredits,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val nextHop = hud.nextRouteHopSectorId
            if (nextHop != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC062838))
                        .border(1.dp, CyanLaser.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenGalaxyMap)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("hud_route_hop_badge")
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = CyanLaser,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${strings.hudRoutePrefix} $nextHop",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanLaser,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                    "${strings.hudRadiationNeedPrefix} ${hud.requiredChipWarning}"
                } else {
                    strings.hudRadiationWarning
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
    val lang = LocalGameLanguage.current
    val strings = GameStrings.of(lang)
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
                text = strings.hudAllMissionsComplete,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldHull
            )
        }
        return
    }

    val activeDesc = GameStrings.missionDescription(active, lang)

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
                    text = "${active.code}: $activeDesc (${active.targetCount}/${active.targetCount})",
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
                        text = strings.hudMissionClaim,
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
                    text = "${active.code} • ${strings.hudNeedLevelPrefix} ${active.requiredLevel}",
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
                    text = "${strings.hudMissionPrefix} $activeDesc (${mission.currentMissionProgress}/${active.targetCount})",
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
