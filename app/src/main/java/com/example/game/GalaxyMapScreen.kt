package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GalaxyMapOverlay(
    currentSectorId: String,
    plottedRoute: List<String>,
    onPlotRoute: (List<String>) -> Unit,
    onClearRoute: () -> Unit,
    onEngageAutopilotToNextGate: () -> Unit,
    onSwitchToLocalMap: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "galaxy_map_anim")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "galaxy_phase"
    )

    var selectedSectorId by remember(currentSectorId) { mutableStateOf(currentSectorId) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val selectedSector = GalaxyMapGraph.sectorOf(selectedSectorId)
    val previewRoute = remember(currentSectorId, selectedSectorId) {
        if (selectedSectorId == currentSectorId) {
            emptyList()
        } else {
            GalaxyMapGraph.findShortestRoute(currentSectorId, selectedSectorId)
        }
    }
    val activeRoute = if (plottedRoute.size >= 2) plottedRoute else previewRoute

    val backdropStars = remember {
        val rng = Random(204209L)
        List(140) {
            Triple(
                Offset(rng.nextFloat(), rng.nextFloat()),
                0.7f + rng.nextFloat() * 1.6f,
                0.22f + rng.nextFloat() * 0.65f
            )
        }
    }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF5040814))
            .testTag("galaxy_map_overlay")
    ) {
        // 1. Deep space starfield + nebula + HUD radar scopes backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGalaxyBackdrop(backdropStars, animPhase)
        }

        // 2. Interactive pan/zoom 25-sector Galactic Map canvas
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val mapSide = (min(widthPx, heightPx) * 0.90f).coerceAtLeast(300f)
            val mapOriginX = (widthPx - mapSide) * 0.5f
            val mapOriginY = (heightPx - mapSide) * 0.5f + with(density) { 12.dp.toPx() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("galaxy_map_canvas")
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextZoom = (zoomScale * zoom).coerceIn(0.75f, 2.6f)
                            zoomScale = nextZoom
                            val maxPan = mapSide * 0.45f * nextZoom
                            panOffset = Offset(
                                x = (panOffset.x + pan.x).coerceIn(-maxPan, maxPan),
                                y = (panOffset.y + pan.y).coerceIn(-maxPan, maxPan)
                            )
                        }
                    }
                    .pointerInput(widthPx, heightPx) {
                        detectTapGestures { tap ->
                            val cx = widthPx * 0.5f
                            val cy = heightPx * 0.5f
                            val unpannedX = (tap.x - cx - panOffset.x) / zoomScale + cx
                            val unpannedY = (tap.y - cy - panOffset.y) / zoomScale + cy

                            var bestId: String? = null
                            var bestDist = (mapSide * 0.075f).coerceIn(30f, 56f)
                            for (node in GalaxyMapGraph.sectors.values) {
                                val nx = mapOriginX + node.position.x * mapSide
                                val ny = mapOriginY + node.position.y * mapSide
                                val bonus = when {
                                    node.isNucleus -> 26f
                                    node.isCapitalCity -> 16f
                                    else -> 4f
                                }
                                val d = hypot(unpannedX - nx, unpannedY - ny)
                                if (d <= bestDist + bonus && d < bestDist + 26f) {
                                    bestDist = d
                                    bestId = node.id
                                }
                            }
                            if (bestId != null) {
                                selectedSectorId = bestId
                            }
                        }
                    }
            ) {
                val pivot = Offset(size.width * 0.5f, size.height * 0.5f)
                withTransform({
                    translate(left = panOffset.x, top = panOffset.y)
                    scale(scaleX = zoomScale, scaleY = zoomScale, pivot = pivot)
                }) {
                    drawGalaxyGraph(
                        mapOrigin = Offset(mapOriginX, mapOriginY),
                        mapSide = mapSide,
                        currentSectorId = currentSectorId,
                        selectedSectorId = selectedSectorId,
                        highlightedRoute = activeRoute,
                        animPhase = animPhase,
                        labelPaint = labelPaint
                    )
                }
            }
        }

        // 3. HUD Chrome: Top Header, Bottom Legend, Active Route Banner, Sector Info Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xD9071022))
                        .border(1.2.dp, CyanLaser.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = CyanLaser,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.galaxy_map_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE0F2FE),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "CURRENT: ${GalaxyMapGraph.sectorOf(currentSectorId).id} • ${GalaxyMapGraph.sectorOf(currentSectorId).zoneType.shortLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GalaxyMapGraph.sectorOf(currentSectorId).zoneType.badgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                GalaxyHeaderChip(
                    icon = Icons.Default.Radar,
                    label = stringResource(R.string.action_local_map),
                    accent = Color(0xFF38BDF8),
                    tag = "galaxy_to_local_button",
                    onClick = onSwitchToLocalMap
                )

                GalaxyHeaderChip(
                    icon = Icons.Default.CenterFocusStrong,
                    label = stringResource(R.string.action_reset_zoom),
                    accent = Color(0xFF94A3B8),
                    tag = "galaxy_reset_zoom_button",
                    onClick = {
                        zoomScale = 1f
                        panOffset = Offset.Zero
                    }
                )

                Button(
                    onClick = onClose,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonDanger,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("galaxy_map_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_close),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Bottom-left compact Legend
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC060C1B))
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                LegendEntry(Color(0xFF38BDF8), "C1 Blue • C2 Green • C3 Red (Safe Cities)")
                LegendEntry(Color(0xFF4ADE80), "Outer Ring R1–R3 (Tier 1–2)")
                LegendEntry(Color(0xFFFB923C), "Inner Ring 1-2/2-2/3-2 (Tier 3–4)")
                LegendEntry(Color(0xFFFACC15), "Yellow Ring & 0-0 Nucleus (Tier 4–5)")
                LegendEntry(Color(0xFF34D399), "Dashed Links = Border Shortcuts")
            }

            // Active plotted route strip (top-left below header)
            if (plottedRoute.size >= 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xDC081B2C))
                        .border(1.2.dp, CyanLaser.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("galaxy_route_banner")
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        tint = CyanLaser,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(modifier = Modifier.widthIn(max = 220.dp)) {
                        Text(
                            text = "ROUTE (${plottedRoute.size - 1} JUMPS) • NEXT: ${plottedRoute[1]}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanLaser,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = plottedRoute.joinToString(" → "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(CyanLaser)
                            .clickable(onClick = onEngageAutopilotToNextGate)
                            .padding(horizontal = 9.dp)
                            .testTag("galaxy_fly_to_gate_button")
                    ) {
                        Text(
                            text = stringResource(R.string.action_fly_to_gate),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF040A16),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color(0x55EF4444))
                            .clickable(onClick = onClearRoute)
                            .testTag("galaxy_clear_route_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear plotted route",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Bottom-right Compact Sector Info Card
            SectorInfoCard(
                sector = selectedSector,
                isCurrentSector = selectedSector.id == currentSectorId,
                routeToSelected = previewRoute,
                isRoutePlotted = plottedRoute.size >= 2 && plottedRoute.last() == selectedSector.id,
                onPlotRoute = {
                    if (previewRoute.size >= 2) {
                        onPlotRoute(previewRoute)
                    }
                },
                onFlyToNextGate = onEngageAutopilotToNextGate,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .widthIn(min = 240.dp, max = 300.dp)
            )
        }
    }
}

@Composable
private fun GalaxyHeaderChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xD90A1428))
            .border(1.2.dp, accent.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp)
            .testTag(tag)
            .semantics { contentDescription = label }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun LegendEntry(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SectorInfoCard(
    sector: SectorNode,
    isCurrentSector: Boolean,
    routeToSelected: List<String>,
    isRoutePlotted: Boolean,
    onPlotRoute: () -> Unit,
    onFlyToNextGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = sector.nodeColor
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xEB071022))
            .border(1.5.dp, accent.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("galaxy_sector_info_card")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent.copy(alpha = 0.22f))
                    .border(1.dp, accent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = sector.id,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sector.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sector.zoneType.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = sector.zoneType.badgeColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isCurrentSector) {
                Text(
                    text = "YOU",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF040A16),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyanLaser)
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        InfoDetailRow(
            icon = Icons.Default.Security,
            iconColor = if (sector.hasCentralSafeZone) CyanLaser else AmberCredits,
            label = sector.safeZoneStatusLabel
        )
        InfoDetailRow(
            icon = Icons.Default.Hub,
            iconColor = accent,
            label = "Resources: ${sector.resourceTiers}"
        )
        InfoDetailRow(
            icon = Icons.Default.Navigation,
            iconColor = TacticalMuted,
            label = "Gates: ${sector.connectedSectorIds.joinToString(", ")}"
        )

        if (!isCurrentSector && routeToSelected.size >= 2) {
            Text(
                text = "Path (${routeToSelected.size - 1} jumps): ${routeToSelected.joinToString(" → ")}",
                style = MaterialTheme.typography.labelSmall,
                color = CyanLaser,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPlotRoute,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRoutePlotted) EmeraldHull else CyanLaser,
                        contentColor = Color(0xFF040A16)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("galaxy_plot_route_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRoutePlotted) "PLOTTED" else stringResource(R.string.action_plot_route),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (isRoutePlotted) {
                    Button(
                        onClick = onFlyToNextGate,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberCredits,
                            contentColor = Color(0xFF040A16)
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("galaxy_card_fly_gate_button")
                    ) {
                        Text(
                            text = "AUTOPILOT",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun DrawScope.drawGalaxyBackdrop(
    stars: List<Triple<Offset, Float, Float>>,
    animPhase: Float
) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF0D1732), Color(0xFF060B1A), Color(0xFF030610)),
            center = Offset(w * 0.5f, h * 0.5f),
            radius = hypot(w, h) * 0.6f
        ),
        size = size
    )

    // Subtle cosmic nebulae tints
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x1C38BDF8), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.16f),
            radius = min(w, h) * 0.34f
        ),
        radius = min(w, h) * 0.34f,
        center = Offset(w * 0.5f, h * 0.16f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0x18FBBF24), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.52f),
            radius = min(w, h) * 0.25f
        ),
        radius = min(w, h) * 0.25f,
        center = Offset(w * 0.5f, h * 0.52f)
    )

    // Subtle sci-fi decorative HUD radar scopes in top-right and bottom-right corners
    val scopeR = (min(w, h) * 0.12f).coerceIn(36f, 68f)
    drawDecorativeRadarScope(Offset(w - scopeR - 22f, scopeR + 62f), scopeR, animPhase)
    drawDecorativeRadarScope(Offset(scopeR + 22f, h - scopeR - 86f), scopeR * 0.78f, -animPhase)

    for ((pos, r, alpha) in stars) {
        val twinkle = 0.78f + 0.22f * sin(animPhase * 2f * PI.toFloat() + pos.x * 19f)
        drawCircle(
            color = Color(0xFFDCE8FF).copy(alpha = (alpha * twinkle).coerceIn(0.12f, 0.92f)),
            radius = r,
            center = Offset(pos.x * w, pos.y * h)
        )
    }
}

private fun DrawScope.drawDecorativeRadarScope(
    center: Offset,
    radius: Float,
    phase: Float
) {
    val lineColor = Color(0x2038BDF8)
    drawCircle(color = lineColor, radius = radius, center = center, style = Stroke(width = 1f))
    drawCircle(color = lineColor, radius = radius * 0.65f, center = center, style = Stroke(width = 0.9f))
    drawCircle(color = lineColor, radius = radius * 0.32f, center = center, style = Stroke(width = 0.8f))
    drawLine(lineColor, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 0.8f)
    drawLine(lineColor, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 0.8f)
    val sweepAngle = phase * 360f
    drawArc(
        color = Color(0x1E00E5FF),
        startAngle = sweepAngle,
        sweepAngle = 42f,
        useCenter = true,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f)
    )
}

private fun DrawScope.drawGalaxyGraph(
    mapOrigin: Offset,
    mapSide: Float,
    currentSectorId: String,
    selectedSectorId: String,
    highlightedRoute: List<String>,
    animPhase: Float,
    labelPaint: Paint
) {
    fun toCanvas(pos: Offset): Offset = Offset(
        x = mapOrigin.x + pos.x * mapSide,
        y = mapOrigin.y + pos.y * mapSide
    )

    val nucleusCenter = toCanvas(GalaxyMapGraph.sectorOf("0-0").position)
    val pulse = 0.55f + 0.45f * sin(animPhase * 2f * PI.toFloat())

    // Build set of route edges for fast lookup
    val routeEdgeKeys = HashSet<String>()
    for (i in 0 until highlightedRoute.size - 1) {
        val a = highlightedRoute[i]
        val b = highlightedRoute[i + 1]
        routeEdgeKeys.add(if (a < b) "$a|$b" else "$b|$a")
    }

    // Subtle decorative concentric orbital guide rings matching the Flight Map aesthetic
    drawCircle(
        color = Color(0x28FF3B5C),
        radius = mapSide * 0.385f,
        center = nucleusCenter,
        style = Stroke(width = 1.6f)
    )
    drawCircle(
        color = Color(0x3638BDF8),
        radius = mapSide * 0.228f,
        center = nucleusCenter,
        style = Stroke(
            width = 1.4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), animPhase * 40f)
        )
    )
    drawCircle(
        color = Color(0x24FACC15),
        radius = mapSide * 0.148f,
        center = nucleusCenter,
        style = Stroke(width = 1.2f)
    )

    // 1. Draw all bidirectional portal connections
    for (link in GalaxyMapGraph.allLinks) {
        val fromNode = GalaxyMapGraph.sectorOf(link.fromId)
        val toNode = GalaxyMapGraph.sectorOf(link.toId)
        val p1 = toCanvas(fromNode.position)
        val p2 = toCanvas(toNode.position)
        val key = if (link.fromId < link.toId) "${link.fromId}|${link.toId}" else "${link.toId}|${link.fromId}"
        val isOnRoute = key in routeEdgeKeys

        val baseColor = when {
            isOnRoute -> CyanLaser
            link.isShortcut -> Color(0xFF34D399)
            link.isInnerRing -> Color(0xFF38BDF8)
            link.isOuterRing -> Color(0xCCEF4444)
            fromNode.isNucleus || toNode.isNucleus -> Color(0xCCFACC15)
            else -> Color(0x9938BDF8)
        }

        if (isOnRoute) {
            drawLine(
                color = CyanLaser.copy(alpha = 0.28f + 0.22f * pulse),
                start = p1,
                end = p2,
                strokeWidth = 7.5f,
                cap = StrokeCap.Round
            )
        }

        drawLine(
            color = baseColor.copy(alpha = if (isOnRoute) 0.98f else 0.72f),
            start = p1,
            end = p2,
            strokeWidth = if (isOnRoute) 3.0f else if (link.isShortcut) 2.0f else 1.7f,
            cap = StrokeCap.Round,
            pathEffect = if (link.isShortcut) {
                PathEffect.dashPathEffect(floatArrayOf(9f, 7f), -animPhase * 32f)
            } else {
                null
            }
        )
    }

    // Animated route travel pulse dots along plotted/preview route
    if (highlightedRoute.size >= 2) {
        for (i in 0 until highlightedRoute.size - 1) {
            val a = toCanvas(GalaxyMapGraph.sectorOf(highlightedRoute[i]).position)
            val b = toCanvas(GalaxyMapGraph.sectorOf(highlightedRoute[i + 1]).position)
            val t = (animPhase * 2f) % 1f
            val dotPos = Offset(
                x = a.x + (b.x - a.x) * t,
                y = a.y + (b.y - a.y) * t
            )
            drawCircle(color = CyanLaser.copy(alpha = 0.45f), radius = 6.5f, center = dotPos)
            drawCircle(color = Color.White, radius = 3.0f, center = dotPos)
        }
    }

    // 2. Draw all 25 sector nodes
    val nativeCanvas = drawContext.canvas.nativeCanvas
    for (node in GalaxyMapGraph.sectors.values) {
        val pt = toCanvas(node.position)
        val isCurrent = node.id == currentSectorId
        val isSelected = node.id == selectedSectorId
        val isOnRoute = node.id in highlightedRoute
        val color = node.nodeColor

        when {
            node.isNucleus -> {
                val r = mapSide * 0.082f
                // Glowing golden halo + tilted orbit ring + central cyan safe zone ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66FBBF24), Color(0x22F59E0B), Color.Transparent),
                        center = pt,
                        radius = r * 1.9f
                    ),
                    radius = r * 1.9f,
                    center = pt
                )
                rotate(degrees = -18f + animPhase * 360f * 0.15f, pivot = pt) {
                    drawOval(
                        color = Color(0xBBFACC15),
                        topLeft = Offset(pt.x - r * 1.55f, pt.y - r * 0.62f),
                        size = Size(r * 3.1f, r * 1.24f),
                        style = Stroke(
                            width = 2.0f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                        )
                    )
                }
                drawCircle(color = Color(0xEE181105), radius = r, center = pt)
                drawCircle(color = Color(0xFFFBBF24), radius = r, center = pt, style = Stroke(width = 3.6f))
                drawCircle(
                    color = CyanLaser.copy(alpha = 0.85f),
                    radius = r * 0.52f,
                    center = pt,
                    style = Stroke(width = 1.6f)
                )

                drawIntoCanvas {
                    labelPaint.textSize = (mapSide * 0.032f).coerceIn(12f, 20f)
                    labelPaint.color = 0xFFFDE68A.toInt()
                    nativeCanvas.drawText("NUCLEUS", pt.x, pt.y - 2f, labelPaint)
                    labelPaint.textSize = (mapSide * 0.020f).coerceIn(9f, 13f)
                    labelPaint.color = 0xFF38BDF8.toInt()
                    nativeCanvas.drawText("0-0 • BLACK MARKET", pt.x, pt.y + labelPaint.textSize * 1.25f, labelPaint)
                }
            }

            node.isCapitalCity -> {
                val r = mapSide * 0.044f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.48f), Color.Transparent),
                        center = pt,
                        radius = r * 2.0f
                    ),
                    radius = r * 2.0f,
                    center = pt
                )
                drawCircle(color = Color(0xEE071224), radius = r, center = pt)
                drawCircle(color = color, radius = r, center = pt, style = Stroke(width = 2.6f))
                drawCircle(color = color.copy(alpha = 0.72f), radius = r * 0.68f, center = pt, style = Stroke(width = 1.8f))
                drawCircle(color = color, radius = r * 0.36f, center = pt)

                val labelOffsetY = if (node.id == "C1") -(r + 12f) else (r + 18f)
                drawIntoCanvas {
                    labelPaint.textSize = (mapSide * 0.028f).coerceIn(11f, 17f)
                    labelPaint.color = color.toArgb()
                    nativeCanvas.drawText(
                        when (node.id) {
                            "C1" -> "CITY 1 (C1)"
                            "C2" -> "CITY 2 (C2)"
                            else -> "CITY 3 (C3)"
                        },
                        pt.x,
                        pt.y + labelOffsetY,
                        labelPaint
                    )
                }
            }

            else -> {
                val r = when {
                    node.isShortcutHub -> mapSide * 0.021f
                    node.zoneType == GalaxyZoneType.HIGH_RISK_YELLOW_RING -> mapSide * 0.017f
                    else -> mapSide * 0.015f
                }
                drawCircle(
                    color = color.copy(alpha = if (isOnRoute) 0.40f else 0.22f),
                    radius = r * 1.9f,
                    center = pt
                )
                drawCircle(color = Color(0xF0071124), radius = r, center = pt)
                drawCircle(
                    color = if (isOnRoute) CyanLaser else color,
                    radius = r,
                    center = pt,
                    style = Stroke(width = if (node.isShortcutHub) 2.2f else 1.7f)
                )
                drawCircle(color = color, radius = r * 0.45f, center = pt)

                drawIntoCanvas {
                    labelPaint.textSize = (mapSide * 0.018f).coerceIn(8.5f, 12.5f)
                    labelPaint.color = if (isOnRoute || isSelected) 0xFFFFFFFF.toInt() else 0xFFCBD5E1.toInt()
                    nativeCanvas.drawText(node.id, pt.x, pt.y + r + labelPaint.textSize * 1.15f, labelPaint)
                }
            }
        }

        // Selection ring
        if (isSelected) {
            val selR = when {
                node.isNucleus -> mapSide * 0.095f
                node.isCapitalCity -> mapSide * 0.056f
                else -> mapSide * 0.029f
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = selR,
                center = pt,
                style = Stroke(
                    width = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), animPhase * 22f)
                )
            )
        }

        // Animated pulsing Current Player Sector beacon
        if (isCurrent) {
            val baseR = when {
                node.isNucleus -> mapSide * 0.104f
                node.isCapitalCity -> mapSide * 0.062f
                else -> mapSide * 0.032f
            }
            val expandR = baseR + (mapSide * 0.022f) * animPhase
            drawCircle(
                color = CyanLaser.copy(alpha = (1f - animPhase) * 0.85f),
                radius = expandR,
                center = pt,
                style = Stroke(width = 2.6f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.6f + 0.4f * pulse),
                radius = baseR,
                center = pt,
                style = Stroke(width = 2.2f)
            )
        }
    }
}
