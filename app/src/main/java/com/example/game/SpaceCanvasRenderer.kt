package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonRadiation
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.CyanThruster
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.EmeraldHullBright
import com.example.ui.theme.SpaceVoid
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun computeWorldScale(viewportHeightPx: Float): Float {
    return (viewportHeightPx / 540f).coerceIn(0.85f, 2.2f)
}

fun createPopupTextPaint(): Paint {
    return Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
        isFakeBoldText = true
        textSize = 14f
    }
}

fun DrawScope.renderSpaceWorld(
    engine: SpaceGameEngine,
    popupPaint: Paint
) {
    val viewportW = size.width
    val viewportH = size.height
    if (viewportW <= 0f || viewportH <= 0f) return

    val worldScale = computeWorldScale(viewportH)
    val shakeX = if (engine.screenShake > 0.1f) {
        sin(engine.elapsedTime * 57f) * engine.screenShake
    } else 0f
    val shakeY = if (engine.screenShake > 0.1f) {
        cos(engine.elapsedTime * 63f) * engine.screenShake
    } else 0f

    val camX = engine.cameraX + shakeX
    val camY = engine.cameraY + shakeY

    // Viewport world-space bounds for 60 FPS culling across the 8000-radius universe
    val halfWorldW = (viewportW * 0.5f) / worldScale
    val halfWorldH = (viewportH * 0.5f) / worldScale
    val halfDiag = hypot(halfWorldW, halfWorldH)
    val cullMargin = 110f
    val minX = camX - halfWorldW - cullMargin
    val maxX = camX + halfWorldW + cullMargin
    val minY = camY - halfWorldH - cullMargin
    val maxY = camY + halfWorldH + cullMargin
    val sector = engine.currentSector
    val mapSize = sector.mapSize

    // 1. Deep void space background tinted to the active sector's zone risk profile
    drawRect(color = sector.zoneType.spaceBackgroundColor, size = size)

    // 2. World-space entities, culled starfield, sector layout, Jump Gates & combat effects
    withTransform({
        translate(left = viewportW * 0.5f, top = viewportH * 0.5f)
        scale(scaleX = worldScale, scaleY = worldScale, pivot = Offset.Zero)
        translate(left = -camX, top = -camY)
    }) {
        drawWorldStarfield(engine, minX, maxX, minY, maxY)
        drawNebulae(engine, minX, maxX, minY, maxY)

        drawSquareSectorGridAndBorder(
            engine = engine,
            mapSize = mapSize,
            accentColor = sector.nodeColor,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY
        )

        if (sector.hasCentralSafeZone && sector.safeZoneRadius > 0f) {
            val (sx, sy) = engine.activeSafeZoneCenter()
            val distToSafe = hypot(camX - sx, camY - sy)
            if (distToSafe <= sector.safeZoneRadius + halfDiag + 160f) {
                drawCentralSectorSafeZone(
                    center = Offset(sx, sy),
                    safeR = sector.safeZoneRadius,
                    sector = sector,
                    time = engine.elapsedTime,
                    popupPaint = popupPaint
                )
            }
        }

        drawSectorJumpGatePortals(engine, popupPaint, minX, maxX, minY, maxY)
        drawLootBoxes(engine, minX, maxX, minY, maxY)

        if (engine.player.isAlive) {
            drawTacticalOverlays(engine)
        }

        // Draw aliens (viewport culled) with distinct Canvas shapes per tier + BOSS variants
        val lockedId = engine.lockedAlienId
        val nativeCanvas = drawContext.canvas.nativeCanvas
        for (alien in engine.aliens) {
            if (alien.x < minX || alien.x > maxX || alien.y < minY || alien.y > maxY) {
                continue
            }
            if (alien.id == lockedId) {
                drawTargetLockReticle(alien, engine.elapsedTime)
            }
            drawAlienShip(alien, engine.elapsedTime)
            val baseBarWidth = when (alien.tier) {
                AlienTier.DREADNOUGHT_BOSS -> 78f
                AlienTier.DESTROYER -> 56f
                AlienTier.HUNTER -> 46f
                AlienTier.SCOUT -> 38f
            }
            val barWidth = if (alien.isBoss) baseBarWidth * 1.32f else baseBarWidth
            val barHeight = if (alien.isBoss || alien.tier == AlienTier.DREADNOUGHT_BOSS) 7.5f else 5.5f
            val tierFill = if (alien.isBoss) AmberCredits else Color(alien.tier.primaryColorHex)
            val tierGlow = if (alien.isBoss) CrimsonDanger else Color(alien.tier.accentColorHex)
            val barTopY = alien.y - alien.radius - 16f
            drawHealthBar(
                centerX = alien.x,
                topY = barTopY,
                width = barWidth,
                height = barHeight,
                ratio = (alien.hp / alien.maxHp).coerceIn(0f, 1f),
                fillColor = tierGlow,
                glowColor = tierFill
            )
            if (alien.isBoss) {
                popupPaint.textSize = 13f
                popupPaint.color = android.graphics.Color.argb(220, 4, 7, 17)
                nativeCanvas.drawText("BOSS", alien.x + 1f, barTopY - 5f, popupPaint)
                popupPaint.color = android.graphics.Color.rgb(251, 191, 36)
                nativeCanvas.drawText("BOSS", alien.x, barTopY - 6f, popupPaint)
            }
        }

        // Draw laser beams & homing rockets (viewport culled)
        drawLasers(engine, minX, maxX, minY, maxY)
        drawRockets(engine, minX, maxX, minY, maxY)

        // Draw player ship class silhouette + orbiting VANT drones + Shield/Hull bars
        if (engine.player.isAlive) {
            val hullR = engine.player.shipClass.hullRadius
            drawOrbitingVants(engine)
            drawPlayerShip(engine.player, engine.elapsedTime)
            drawHealthBar(
                centerX = engine.player.x,
                topY = engine.player.y - hullR - 18f,
                width = 56f,
                height = 5.5f,
                ratio = (engine.player.hp / engine.player.maxHp).coerceIn(0f, 1f),
                fillColor = EmeraldHull,
                glowColor = EmeraldHullBright
            )
            drawHealthBar(
                centerX = engine.player.x,
                topY = engine.player.y - hullR - 26f,
                width = 56f,
                height = 5f,
                ratio = (engine.player.shield / engine.player.maxShield).coerceIn(0f, 1f),
                fillColor = Color(0xFF0284C7),
                glowColor = CyanThruster
            )

            // Pilot [TAG] Callsign neatly below ship tinted with Company color
            val pilotLabel = engine.pilotName?.let { name ->
                val tag = engine.pilotCompany?.tag
                if (tag != null) "[$tag] $name" else name
            }
            if (!pilotLabel.isNullOrBlank()) {
                val rgb = engine.pilotCompany?.rgbInt ?: 0x00E5FF
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                val labelY = engine.player.y + hullR + 22f
                val nativeCanvas = drawContext.canvas.nativeCanvas
                popupPaint.textSize = 12.5f
                popupPaint.color = android.graphics.Color.argb(235, 3, 6, 18)
                nativeCanvas.drawText(pilotLabel, engine.player.x + 1.2f, labelY + 1.2f, popupPaint)
                popupPaint.color = android.graphics.Color.rgb(r, g, b)
                nativeCanvas.drawText(pilotLabel, engine.player.x, labelY, popupPaint)
            }
        }

        drawEffects(engine, popupPaint, minX, maxX, minY, maxY)
    }

    // 3. Screen-space Cyan Warp Flash overlay (~0.4s smooth fade upon Jump Gate transit)
    if (engine.warpFlashTimer > 0f) {
        val ratio = (engine.warpFlashTimer / GameConfig.WARP_FLASH_DURATION_SEC).coerceIn(0f, 1f)
        val alpha = (ratio * ratio * 0.85f).coerceIn(0f, 0.88f)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = (alpha * 0.92f).coerceIn(0f, 0.92f)),
                    CyanLaser.copy(alpha = alpha),
                    Color(0xFF0284C7).copy(alpha = alpha * 0.72f)
                ),
                center = Offset(viewportW * 0.5f, viewportH * 0.5f),
                radius = hypot(viewportW, viewportH) * 0.65f
            ),
            size = size
        )
    }
}

private fun DrawScope.drawWorldStarfield(
    engine: SpaceGameEngine,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    val t = engine.elapsedTime
    for (star in engine.stars) {
        if (star.x < minX || star.x > maxX || star.y < minY || star.y > maxY) {
            continue
        }
        val baseColor = when (star.tintType) {
            1 -> Color(0xFF93C5FD)
            2 -> Color(0xFFFDE68A)
            else -> Color(0xFFE2E8F0)
        }
        val twinkle = 0.84f + 0.16f * sin(t * 2.2f + star.x * 0.02f)
        drawCircle(
            color = baseColor.copy(alpha = (star.alpha * twinkle).coerceIn(0.12f, 1f)),
            radius = star.radius,
            center = Offset(star.x, star.y)
        )
    }
}

private fun DrawScope.drawNebulae(
    engine: SpaceGameEngine,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    for (cloud in engine.nebulae) {
        if (cloud.x + cloud.radius < minX || cloud.x - cloud.radius > maxX ||
            cloud.y + cloud.radius < minY || cloud.y - cloud.radius > maxY
        ) {
            continue
        }
        val center = Offset(cloud.x, cloud.y)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(cloud.colorHex), Color.Transparent),
                center = center,
                radius = cloud.radius
            ),
            radius = cloud.radius,
            center = center
        )
    }
}

private fun DrawScope.drawSectorGridAndZoneRings(
    camDistFromOrigin: Float,
    halfDiag: Float
) {
    val maxR = GameConfig.MAP_RADIUS
    val gridColor = Color(0x1238BDF8)

    // Subtle intermediate navigation rings every 2500 units (culled by camera ring distance)
    val minVisibleR = (camDistFromOrigin - halfDiag - 60f).coerceAtLeast(0f)
    val maxVisibleR = camDistFromOrigin + halfDiag + 60f

    var r = 2500f
    while (r < maxR) {
        val isZoneBorder = r == GameConfig.ZONE_1_MAX_RADIUS ||
            r == GameConfig.ZONE_2_MAX_RADIUS ||
            r == GameConfig.ZONE_3_MAX_RADIUS
        if (!isZoneBorder && r in minVisibleR..maxVisibleR) {
            drawCircle(
                color = gridColor,
                radius = r,
                center = Offset.Zero,
                style = Stroke(width = 1.1f)
            )
        }
        r += 2500f
    }

    // 8 subtle radial sector spokes
    for (i in 0 until 8) {
        val angle = i * (PI.toFloat() * 0.25f)
        drawLine(
            color = gridColor,
            start = Offset.Zero,
            end = Offset(cos(angle) * maxR, sin(angle) * maxR),
            strokeWidth = 1.0f
        )
    }

    // Zone 1 -> Zone 2 subtle green dashed ring border at radius 2000
    if (GameConfig.ZONE_1_MAX_RADIUS in minVisibleR..maxVisibleR) {
        drawZoneBorderRing(
            radius = GameConfig.ZONE_1_MAX_RADIUS,
            color = Color(0xFF4ADE80)
        )
    }
    // Zone 2 -> Zone 3 subtle yellow dashed ring border at radius 4000
    if (GameConfig.ZONE_2_MAX_RADIUS in minVisibleR..maxVisibleR) {
        drawZoneBorderRing(
            radius = GameConfig.ZONE_2_MAX_RADIUS,
            color = Color(0xFFFACC15)
        )
    }
    // Zone 3 -> Zone 4 subtle orange dashed ring border at radius 6000
    if (GameConfig.ZONE_3_MAX_RADIUS in minVisibleR..maxVisibleR) {
        drawZoneBorderRing(
            radius = GameConfig.ZONE_3_MAX_RADIUS,
            color = Color(0xFFFB923C)
        )
    }
}

private fun DrawScope.drawZoneBorderRing(
    radius: Float,
    color: Color
) {
    // Soft low-opacity ambient band
    drawCircle(
        color = color.copy(alpha = 0.11f),
        radius = radius,
        center = Offset.Zero,
        style = Stroke(width = 16f)
    )
    // Crisp dashed tactical perimeter
    drawCircle(
        color = color.copy(alpha = 0.56f),
        radius = radius,
        center = Offset.Zero,
        style = Stroke(
            width = 2.6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 18f), 0f)
        )
    )
}

private fun DrawScope.drawSquareSectorGridAndBorder(
    engine: SpaceGameEngine,
    mapSize: Float,
    accentColor: Color,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    val step = 1000f

    val startGx = (kotlin.math.floor((minX.coerceAtLeast(0f)) / step) * step).coerceAtLeast(0f)
    val endGx = (kotlin.math.ceil((maxX.coerceAtMost(mapSize)) / step) * step).coerceAtMost(mapSize)
    val startGy = (kotlin.math.floor((minY.coerceAtLeast(0f)) / step) * step).coerceAtLeast(0f)
    val endGy = (kotlin.math.ceil((maxY.coerceAtMost(mapSize)) / step) * step).coerceAtMost(mapSize)

    val clipTop = minY.coerceAtLeast(0f)
    val clipBottom = maxY.coerceAtMost(mapSize)
    val clipLeft = minX.coerceAtLeast(0f)
    val clipRight = maxX.coerceAtMost(mapSize)

    if (clipBottom > clipTop) {
        var gx = startGx
        while (gx <= endGx) {
            if (gx > 0f && gx < mapSize) {
                val isMajor = ((gx / step).toInt() % 2 == 0)
                drawLine(
                    color = accentColor.copy(alpha = if (isMajor) 0.14f else 0.07f),
                    start = Offset(gx, clipTop),
                    end = Offset(gx, clipBottom),
                    strokeWidth = if (isMajor) 1.5f else 1.0f
                )
            }
            gx += step
        }
    }

    if (clipRight > clipLeft) {
        var gy = startGy
        while (gy <= endGy) {
            if (gy > 0f && gy < mapSize) {
                val isMajor = ((gy / step).toInt() % 2 == 0)
                drawLine(
                    color = accentColor.copy(alpha = if (isMajor) 0.14f else 0.07f),
                    start = Offset(clipLeft, gy),
                    end = Offset(clipRight, gy),
                    strokeWidth = if (isMajor) 1.5f else 1.0f
                )
            }
            gy += step
        }
    }

    // Outer Crimson Radiation Zone shading beyond the 0..mapSize square edges
    val radBand = GameConfig.MAP_X1_RADIATION_OUTER_BUFFER + 640f
    val radFill = Color(0x26FF1744)
    if (minY < 0f) {
        drawRect(color = radFill, topLeft = Offset(-radBand, -radBand), size = Size(mapSize + radBand * 2f, radBand))
    }
    if (maxY > mapSize) {
        drawRect(color = radFill, topLeft = Offset(-radBand, mapSize), size = Size(mapSize + radBand * 2f, radBand))
    }
    if (minX < 0f) {
        drawRect(color = radFill, topLeft = Offset(-radBand, 0f), size = Size(radBand, mapSize))
    }
    if (maxX > mapSize) {
        drawRect(color = radFill, topLeft = Offset(mapSize, 0f), size = Size(radBand, mapSize))
    }

    val pulse = 0.65f + 0.35f * sin(engine.elapsedTime * 5.2f)
    drawRect(
        color = CrimsonRadiation.copy(alpha = 0.24f * pulse),
        topLeft = Offset.Zero,
        size = Size(mapSize, mapSize),
        style = Stroke(width = 24f)
    )
    drawRect(
        color = CrimsonDanger.copy(alpha = 0.92f),
        topLeft = Offset.Zero,
        size = Size(mapSize, mapSize),
        style = Stroke(width = 5f)
    )
    drawRect(
        color = Color(0xFFFF8A80).copy(alpha = 0.7f * pulse),
        topLeft = Offset(14f, 14f),
        size = Size(mapSize - 28f, mapSize - 28f),
        style = Stroke(
            width = 2.4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 18f), engine.elapsedTime * 42f)
        )
    )
}

private fun DrawScope.drawCentralSectorSafeZone(
    center: Offset,
    safeR: Float,
    sector: SectorNode,
    time: Float,
    popupPaint: Paint
) {
    val pulse = 0.72f + 0.28f * sin(time * 3.0f)
    val ringColor = if (sector.isNucleus) AmberCredits else sector.nodeColor

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                CyanLaser.copy(alpha = 0.14f),
                ringColor.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = center,
            radius = safeR
        ),
        radius = safeR,
        center = center
    )
    drawCircle(
        color = ringColor.copy(alpha = 0.22f * pulse),
        radius = safeR,
        center = center,
        style = Stroke(width = 16f)
    )
    drawCircle(
        color = CyanLaser.copy(alpha = 0.80f),
        radius = safeR,
        center = center,
        style = Stroke(
            width = 3.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 14f), time * 26f)
        )
    )
    if (sector.isNucleus) {
        drawCircle(
            color = AmberCredits.copy(alpha = 0.65f),
            radius = safeR + 24f,
            center = center,
            style = Stroke(width = 2.4f)
        )
    }

    // Rotating central station hub
    rotate(degrees = time * 11f, pivot = center) {
        drawCircle(color = Color(0xFF16243D), radius = 78f, center = center, style = Stroke(width = 12f))
        drawCircle(color = ringColor.copy(alpha = 0.8f), radius = 84f, center = center, style = Stroke(width = 2.0f))
        for (i in 0 until 4) {
            val a = i * (PI.toFloat() * 0.5f)
            val p1 = Offset(center.x + cos(a) * 34f, center.y + sin(a) * 34f)
            val p2 = Offset(center.x + cos(a) * 108f, center.y + sin(a) * 108f)
            drawLine(Color(0xFF334155), p1, p2, strokeWidth = 10f, cap = StrokeCap.Round)
            drawLine(ringColor.copy(alpha = 0.85f), p1, p2, strokeWidth = 2.2f, cap = StrokeCap.Round)
            drawCircle(ringColor, radius = 4.2f, center = p2)
        }
    }
    drawCircle(color = Color(0xFF0F1C33), radius = 40f, center = center)
    drawCircle(color = ringColor, radius = 40f, center = center, style = Stroke(width = 2.2f))
    drawCircle(color = CyanLaser, radius = 12f * (0.9f + 0.1f * pulse), center = center)
    drawCircle(color = Color.White, radius = 5f, center = center)

    val stationTitle = when {
        sector.isNucleus -> "BLACK MARKET • SAFE ZONE"
        sector.isCapitalCity -> "${sector.id} CAPITAL BASE • MARKET • VAULT"
        else -> "${sector.id} SAFE ZONE OUTPOST"
    }
    val nativeCanvas = drawContext.canvas.nativeCanvas
    popupPaint.textSize = 15f
    popupPaint.color = android.graphics.Color.argb(220, 4, 7, 17)
    nativeCanvas.drawText(stationTitle, center.x + 1.2f, center.y + 134f, popupPaint)
    popupPaint.color = if (sector.isNucleus) {
        android.graphics.Color.rgb(251, 191, 36)
    } else {
        android.graphics.Color.rgb(56, 189, 248)
    }
    nativeCanvas.drawText(stationTitle, center.x, center.y + 133f, popupPaint)
}

private fun DrawScope.drawSectorJumpGatePortals(
    engine: SpaceGameEngine,
    popupPaint: Paint,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    val nearest = engine.nearestJumpPortal()
    val nextHop = engine.nextRouteHopSectorId
    val t = engine.elapsedTime
    val pulse = 0.72f + 0.28f * sin(t * 4.4f)
    val outerR = 46f
    val innerR = 32f
    val nativeCanvas = drawContext.canvas.nativeCanvas

    for (portal in engine.activePortals) {
        val gx = portal.worldX
        val gy = portal.worldY
        if (gx + 130f < minX || gx - 130f > maxX || gy + 130f < minY || gy - 130f > maxY) {
            continue
        }

        val center = Offset(gx, gy)
        val nearGate = nearest?.targetSectorId == portal.targetSectorId
        val isRouteTarget = nextHop == portal.targetSectorId
        val accent = if (isRouteTarget) CyanLaser else portal.accentColor

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = if (nearGate || isRouteTarget) 0.50f else 0.30f),
                    Color(0xFF6366F1).copy(alpha = if (nearGate) 0.26f else 0.14f),
                    Color.Transparent
                ),
                center = center,
                radius = if (nearGate || isRouteTarget) 104f else 82f
            ),
            radius = if (nearGate || isRouteTarget) 104f else 82f,
            center = center
        )

        if (nearGate || isRouteTarget) {
            drawCircle(
                color = accent.copy(alpha = 0.40f * pulse),
                radius = GameConfig.JUMP_GATE_INTERACT_RADIUS,
                center = center,
                style = Stroke(
                    width = if (isRouteTarget) 2.2f else 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), -t * 36f)
                )
            )
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f * pulse),
                    accent.copy(alpha = 0.65f),
                    Color(0xFF4F46E5).copy(alpha = 0.45f),
                    Color(0xFF0A1026).copy(alpha = 0.72f)
                ),
                center = center,
                radius = innerR
            ),
            radius = innerR,
            center = center
        )

        rotate(degrees = t * 34f, pivot = center) {
            drawCircle(color = Color(0xFF1E293B), radius = outerR, center = center, style = Stroke(width = 7.5f))
            drawCircle(color = Color(0xFF94A3B8).copy(alpha = 0.78f), radius = outerR + 3.2f, center = center, style = Stroke(width = 1.4f))
            drawCircle(color = accent.copy(alpha = 0.82f), radius = outerR - 3.6f, center = center, style = Stroke(width = 1.5f))
            val arcTopLeft = Offset(gx - outerR, gy - outerR)
            val arcSize = Size(outerR * 2f, outerR * 2f)
            for (i in 0 until 3) {
                drawArc(
                    color = accent.copy(alpha = 0.90f),
                    startAngle = i * 120f + 12f,
                    sweepAngle = 72f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )
            }
            for (i in 0 until 6) {
                val a = i * (PI.toFloat() / 3f)
                val px = gx + cos(a) * outerR
                val py = gy + sin(a) * outerR
                drawCircle(color = Color(0xFF0F172A), radius = 5.4f, center = Offset(px, py))
                drawCircle(
                    color = if (i % 2 == 0) accent else Color(0xFFA78BFA),
                    radius = 3.0f,
                    center = Offset(px, py)
                )
            }
        }

        rotate(degrees = -t * 58f, pivot = center) {
            drawCircle(
                color = (if (portal.isShortcut) EmeraldHull else Color(0xFFA78BFA)).copy(alpha = 0.82f),
                radius = innerR + 3f,
                center = center,
                style = Stroke(
                    width = 2.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
            )
            val innerArcTopLeft = Offset(gx - (innerR - 6f), gy - (innerR - 6f))
            val innerArcSize = Size((innerR - 6f) * 2f, (innerR - 6f) * 2f)
            for (i in 0 until 2) {
                drawArc(
                    color = Color.White.copy(alpha = 0.85f * pulse),
                    startAngle = i * 180f,
                    sweepAngle = 82f,
                    useCenter = false,
                    topLeft = innerArcTopLeft,
                    size = innerArcSize,
                    style = Stroke(width = 2.0f, cap = StrokeCap.Round)
                )
            }
        }

        val label = if (isRouteTarget) "★ ${portal.gateLabel}" else portal.gateLabel
        popupPaint.textSize = 13.5f
        popupPaint.color = android.graphics.Color.argb(215, 4, 7, 17)
        nativeCanvas.drawText(label, gx + 1f, gy + outerR + 22f, popupPaint)
        popupPaint.color = when {
            isRouteTarget -> android.graphics.Color.rgb(0, 229, 255)
            nearGate -> android.graphics.Color.rgb(224, 247, 250)
            portal.isShortcut -> android.graphics.Color.rgb(52, 211, 153)
            else -> android.graphics.Color.argb(230, 186, 230, 253)
        }
        nativeCanvas.drawText(label, gx, gy + outerR + 21f, popupPaint)
    }
}

private fun DrawScope.drawSafeZoneAndStation(time: Float) {
    val safeR = GameConfig.SAFE_ZONE_RADIUS
    val pulse = 0.7f + 0.3f * sin(time * 3.0f)

    // Soft cyan Safe Zone dome field
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                CyanLaser.copy(alpha = 0.12f),
                CyanLaser.copy(alpha = 0.05f),
                CyanLaser.copy(alpha = 0.02f)
            ),
            center = Offset.Zero,
            radius = safeR
        ),
        radius = safeR,
        center = Offset.Zero
    )

    // Outer glowing cyan Safe Zone ring (radius 400)
    drawCircle(
        color = CyanLaser.copy(alpha = 0.22f * pulse),
        radius = safeR,
        center = Offset.Zero,
        style = Stroke(width = 14f)
    )
    drawCircle(
        color = CyanLaser.copy(alpha = 0.85f),
        radius = safeR,
        center = Offset.Zero,
        style = Stroke(width = 3f)
    )
    // Rotating dashed inner barrier ring
    drawCircle(
        color = CyanThruster.copy(alpha = 0.45f),
        radius = safeR - 14f,
        center = Offset.Zero,
        style = Stroke(
            width = 1.4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), -time * 24f)
        )
    )

    // Sci-Fi Central Space Station at (0, 0)
    rotate(degrees = (time * 9f) % 360f, pivot = Offset.Zero) {
        drawCircle(
            color = Color(0xFF16243D),
            radius = 78f,
            center = Offset.Zero,
            style = Stroke(width = 12f)
        )
        drawCircle(
            color = CyanLaser.copy(alpha = 0.7f),
            radius = 84f,
            center = Offset.Zero,
            style = Stroke(width = 1.8f)
        )
        drawCircle(
            color = CyanThruster.copy(alpha = 0.45f),
            radius = 72f,
            center = Offset.Zero,
            style = Stroke(width = 1.4f)
        )

        for (i in 0 until 4) {
            val angle = i * (PI.toFloat() * 0.5f)
            val cosA = cos(angle)
            val sinA = sin(angle)
            drawLine(
                color = Color(0xFF334155),
                start = Offset(cosA * 34f, sinA * 34f),
                end = Offset(cosA * 106f, sinA * 106f),
                strokeWidth = 10f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = CyanLaser.copy(alpha = 0.75f),
                start = Offset(cosA * 40f, sinA * 40f),
                end = Offset(cosA * 104f, sinA * 104f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
            val tip = Offset(cosA * 108f, sinA * 108f)
            drawCircle(color = CyanLaser.copy(alpha = 0.3f * pulse), radius = 10f, center = tip)
            drawCircle(color = CyanLaser, radius = 4f, center = tip)
        }
    }

    val hubPath = Path().apply {
        val sides = 8
        val hubR = 42f
        for (i in 0 until sides) {
            val a = i * (2f * PI.toFloat() / sides) + PI.toFloat() / 8f
            val x = cos(a) * hubR
            val y = sin(a) * hubR
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path = hubPath, color = Color(0xFF0F1C33))
    drawPath(path = hubPath, color = CyanLaser, style = Stroke(width = 2.2f))

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(CyanLaser.copy(alpha = 0.85f * pulse), Color.Transparent),
            center = Offset.Zero,
            radius = 28f
        ),
        radius = 28f,
        center = Offset.Zero
    )
    drawCircle(color = CyanLaser, radius = 12f * (0.9f + 0.1f * pulse), center = Offset.Zero)
    drawCircle(color = Color.White, radius = 5f, center = Offset.Zero)
}

private fun DrawScope.drawLootBoxes(
    engine: SpaceGameEngine,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    for (box in engine.lootBoxes) {
        if (box.x < minX || box.x > maxX || box.y < minY || box.y > maxY) {
            continue
        }
        val pulse = 0.72f + 0.28f * sin(box.pulsePhase)
        val center = Offset(box.x, box.y)

        if (box.kind == LootBoxKind.BONUS_BOX) {
            val primaryColor = if (box.shieldReward > 0f) CyanLaser else Color(0xFFFACC15)
            drawCircle(
                color = primaryColor.copy(alpha = 0.2f * pulse),
                radius = 18f * pulse,
                center = center
            )
            rotate(degrees = (box.pulsePhase * 24f) % 360f, pivot = center) {
                val half = 8.5f
                drawRoundRect(
                    color = Color(0xDD0B1324),
                    topLeft = Offset(box.x - half, box.y - half),
                    size = Size(half * 2f, half * 2f),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(box.x - half, box.y - half),
                    size = Size(half * 2f, half * 2f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 2.1f)
                )
                drawCircle(color = primaryColor, radius = 3.2f, center = center)
            }
        } else {
            drawCircle(
                color = AmberCredits.copy(alpha = 0.26f * pulse),
                radius = 20f * pulse,
                center = center
            )
            val w = 18f
            val h = 14f
            drawRoundRect(
                color = Color(0xEE2B1606),
                topLeft = Offset(box.x - w * 0.5f, box.y - h * 0.5f),
                size = Size(w, h),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawRoundRect(
                color = AmberCredits,
                topLeft = Offset(box.x - w * 0.5f, box.y - h * 0.5f),
                size = Size(w, h),
                cornerRadius = CornerRadius(3f, 3f),
                style = Stroke(width = 2.2f)
            )
            drawLine(
                color = AmberCredits.copy(alpha = 0.85f),
                start = Offset(box.x - w * 0.45f, box.y),
                end = Offset(box.x + w * 0.45f, box.y),
                strokeWidth = 1.6f
            )
            drawCircle(color = Color.White, radius = 2.5f, center = center)
        }
    }
}

private fun DrawScope.drawRadiationBorder(engine: SpaceGameEngine) {
    val mapR = GameConfig.MAP_RADIUS
    val radBandWidth = 520f

    drawCircle(
        color = Color(0x2AE11D48),
        radius = mapR + radBandWidth * 0.5f,
        center = Offset.Zero,
        style = Stroke(width = radBandWidth)
    )

    val pulse = 0.65f + 0.35f * sin(engine.elapsedTime * 4.5f)
    drawCircle(
        color = CrimsonRadiation.copy(alpha = 0.30f * pulse),
        radius = mapR,
        center = Offset.Zero,
        style = Stroke(width = 24f)
    )
    drawCircle(
        color = CrimsonDanger.copy(alpha = 0.86f + 0.14f * pulse),
        radius = mapR,
        center = Offset.Zero,
        style = Stroke(width = 4.5f)
    )
    drawCircle(
        color = CrimsonDanger.copy(alpha = 0.45f),
        radius = mapR - 20f,
        center = Offset.Zero,
        style = Stroke(
            width = 1.6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(26f, 18f), engine.elapsedTime * 32f)
        )
    )
}

private fun DrawScope.drawTacticalOverlays(engine: SpaceGameEngine) {
    val player = engine.player

    if (player.isAutoFireEnabled && !player.inSafeZone) {
        drawCircle(
            color = CyanLaser.copy(alpha = 0.14f),
            radius = GameConfig.PLAYER_ATTACK_RANGE,
            center = Offset(player.x, player.y),
            style = Stroke(
                width = 1.3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            )
        )
    }

    // Glowing semi-transparent 56-degree Radar Cone ONLY while right joystick is actively tilted
    if (engine.isAiming) {
        val aimAngleRad = atan2(engine.aimInputY, engine.aimInputX)
        val aimDeg = Math.toDegrees(aimAngleRad.toDouble()).toFloat()
        val sweepDeg = GameConfig.RADAR_CONE_SWEEP_DEG
        val halfSweepDeg = sweepDeg * 0.5f
        val startDeg = aimDeg - halfSweepDeg
        val coneRange = GameConfig.RADAR_CONE_RANGE
        val topLeft = Offset(player.x - coneRange, player.y - coneRange)
        val arcSize = Size(coneRange * 2f, coneRange * 2f)

        drawArc(
            brush = Brush.radialGradient(
                colors = listOf(
                    CrimsonDanger.copy(alpha = 0.24f),
                    CrimsonDanger.copy(alpha = 0.12f),
                    CrimsonDanger.copy(alpha = 0.04f)
                ),
                center = Offset(player.x, player.y),
                radius = coneRange
            ),
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = true,
            topLeft = topLeft,
            size = arcSize
        )

        val midRange = coneRange * 0.58f
        drawArc(
            color = CrimsonDanger.copy(alpha = 0.22f),
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = Offset(player.x - midRange, player.y - midRange),
            size = Size(midRange * 2f, midRange * 2f),
            style = Stroke(
                width = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        )

        val leftEdgeRad = aimAngleRad - GameConfig.RADAR_CONE_HALF_ANGLE_RAD
        val rightEdgeRad = aimAngleRad + GameConfig.RADAR_CONE_HALF_ANGLE_RAD
        val edgeColor = Color(0xCCFF4D6D)
        drawLine(
            color = edgeColor,
            start = Offset(player.x, player.y),
            end = Offset(
                player.x + cos(leftEdgeRad) * coneRange,
                player.y + sin(leftEdgeRad) * coneRange
            ),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = edgeColor,
            start = Offset(player.x, player.y),
            end = Offset(
                player.x + cos(rightEdgeRad) * coneRange,
                player.y + sin(rightEdgeRad) * coneRange
            ),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )

        drawArc(
            color = CrimsonDanger.copy(alpha = 0.35f),
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xEEFF5C77),
            startAngle = startDeg,
            sweepAngle = sweepDeg,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = 2.2f, cap = StrokeCap.Round)
        )
    }

    val locked = engine.getLockedAlien()
    if (locked != null && player.isAutoFireEnabled && !player.inSafeZone) {
        val dist = hypot(locked.x - player.x, locked.y - player.y)
        if (dist <= GameConfig.PLAYER_ATTACK_RANGE) {
            drawLine(
                color = CrimsonDanger.copy(alpha = 0.22f),
                start = Offset(player.x, player.y),
                end = Offset(locked.x, locked.y),
                strokeWidth = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
            )
        }
    }

    // Active Autopilot dashed vector + pulsing cyan crosshair in world space
    val apX = engine.autopilotTargetX
    val apY = engine.autopilotTargetY
    if (apX != null && apY != null) {
        drawLine(
            color = CyanLaser.copy(alpha = 0.30f),
            start = Offset(player.x, player.y),
            end = Offset(apX, apY),
            strokeWidth = 1.6f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), -engine.elapsedTime * 36f)
        )
        val apPulse = 0.65f + 0.35f * sin(engine.elapsedTime * 6f)
        val apCenter = Offset(apX, apY)
        drawCircle(
            color = CyanLaser.copy(alpha = 0.22f * apPulse),
            radius = 22f * apPulse,
            center = apCenter
        )
        drawCircle(
            color = CyanLaser.copy(alpha = 0.85f),
            radius = 12f,
            center = apCenter,
            style = Stroke(width = 1.8f)
        )
        drawLine(
            color = CyanLaser,
            start = Offset(apX - 18f, apY),
            end = Offset(apX + 18f, apY),
            strokeWidth = 1.4f
        )
        drawLine(
            color = CyanLaser,
            start = Offset(apX, apY - 18f),
            end = Offset(apX, apY + 18f),
            strokeWidth = 1.4f
        )
    }
}

private fun DrawScope.drawTargetLockReticle(alien: AlienEntity, time: Float) {
    val pulse = 0.5f + 0.5f * sin(time * 7.5f)
    val radius = alien.radius + 11f + pulse * 5f
    val center = Offset(alien.x, alien.y)

    drawCircle(
        color = CrimsonDanger.copy(alpha = 0.14f + 0.12f * pulse),
        radius = radius + 4f,
        center = center
    )
    drawCircle(
        color = CrimsonDanger.copy(alpha = 0.78f + 0.22f * pulse),
        radius = radius,
        center = center,
        style = Stroke(width = 2.4f)
    )
    rotate(degrees = (time * 55f) % 360f, pivot = center) {
        val tickInner = radius - 4f
        val tickOuter = radius + 6f
        for (i in 0 until 4) {
            val angle = i * (PI.toFloat() * 0.5f)
            drawLine(
                color = Color(0xFFFF6B81),
                start = Offset(center.x + cos(angle) * tickInner, center.y + sin(angle) * tickInner),
                end = Offset(center.x + cos(angle) * tickOuter, center.y + sin(angle) * tickOuter),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawPlayerShip(player: PlayerState, time: Float) {
    val center = Offset(player.x, player.y)
    val degrees = Math.toDegrees(player.angleRad.toDouble()).toFloat()
    val ship = player.shipClass
    val hullR = ship.hullRadius

    if (player.inSafeZone && (player.hp < player.maxHp || player.shield < player.maxShield)) {
        val repairPulse = 0.45f + 0.55f * sin(time * 8f)
        drawCircle(
            color = EmeraldHullBright.copy(alpha = 0.35f * repairPulse),
            radius = hullR + 8f,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    val thrust = player.thrustFactor.coerceIn(0f, 1f)
    val pulse = player.thrusterPulse
    val hitFlash = player.hitFlash.coerceIn(0f, 1f)

    rotate(degrees = degrees, pivot = center) {
        when (ship) {
            ShipClass.INTERCEPTOR -> drawInterceptorShip(player.x, player.y, thrust, pulse, time, hitFlash)
            ShipClass.BATTLECRUISER -> drawBattlecruiserShip(player.x, player.y, thrust, pulse, time, hitFlash)
            ShipClass.DREADNOUGHT -> drawTankCitadelShip(player.x, player.y, thrust, pulse, time, hitFlash)
        }
    }

    // Translucent cyan/blue energy shield bubble flash on Shield Hit (~0.25s smooth fade)
    if (player.shieldHitFlash > 0.01f) {
        val alpha = player.shieldHitFlash.coerceIn(0f, 1f)
        val bubbleR = hullR + 11f + (1f - alpha) * 4f
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.52f to Color(0xFF0284C7).copy(alpha = 0.08f * alpha),
                    0.82f to Color(0xFF00E5FF).copy(alpha = 0.32f * alpha),
                    1.0f to Color(0xFF7DD3FC).copy(alpha = 0.55f * alpha)
                ),
                center = center,
                radius = bubbleR
            ),
            radius = bubbleR,
            center = center
        )
        drawCircle(
            color = Color(0xFF38BDF8).copy(alpha = 0.90f * alpha),
            radius = bubbleR,
            center = center,
            style = Stroke(width = 2.4f)
        )
        drawCircle(
            color = Color(0xFFE0F2FE).copy(alpha = 0.55f * alpha),
            radius = bubbleR - 2.6f,
            center = center,
            style = Stroke(width = 1.0f)
        )
    }
}

private fun DrawScope.drawInterceptorShip(
    cx: Float,
    cy: Float,
    thrust: Float,
    pulse: Float,
    time: Float,
    hitFlash: Float
) {
    // 2 rear thrusters (Vengeance / Pusat twin-engine bank)
    drawPlasmaThrusterFlame(cx - 14.5f, cy - 5.8f, thrust, maxLen = 24f, halfW = 3.8f, pulse = pulse, phaseOffset = 0.0f)
    drawPlasmaThrusterFlame(cx - 14.5f, cy + 5.8f, thrust, maxLen = 24f, halfW = 3.8f, pulse = pulse, phaseOffset = 1.9f)

    // Twin armored exhaust nozzles
    for (sign in floatArrayOf(-1f, 1f)) {
        val ny = cy + sign * 5.8f
        val nozzle = Path().apply {
            moveTo(cx - 10.5f, ny - 2.7f)
            lineTo(cx - 15.2f, ny - 2.2f)
            lineTo(cx - 15.2f, ny + 2.2f)
            lineTo(cx - 10.5f, ny + 2.7f)
            close()
        }
        drawPath(nozzle, Color(0xFF1E293B))
        drawPath(nozzle, Color(0xFF64748B), style = Stroke(width = 0.9f))
    }

    // Swept-back razor delta wing chassis (dark metallic silver/slate linear gradient)
    val mainWings = Path().apply {
        moveTo(cx + 26.5f, cy)
        lineTo(cx + 11f, cy - 5.5f)
        lineTo(cx + 1f, cy - 11.5f)
        lineTo(cx - 11.5f, cy - 21.5f)
        lineTo(cx - 18f, cy - 17.5f)
        lineTo(cx - 12.5f, cy - 10f)
        lineTo(cx - 13.5f, cy - 3.2f)
        lineTo(cx - 10.5f, cy)
        lineTo(cx - 13.5f, cy + 3.2f)
        lineTo(cx - 12.5f, cy + 10f)
        lineTo(cx - 18f, cy + 17.5f)
        lineTo(cx - 11.5f, cy + 21.5f)
        lineTo(cx + 1f, cy + 11.5f)
        lineTo(cx + 11f, cy + 5.5f)
        close()
    }
    drawPath(
        path = mainWings,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF94A3B8), Color(0xFF475569), Color(0xFF1E293B)),
            start = Offset(cx, cy - 22f),
            end = Offset(cx, cy + 22f)
        )
    )
    drawPath(path = mainWings, color = Color(0xFFCBD5E1).copy(alpha = 0.8f), style = Stroke(width = 1.25f))

    // Recessed dark wing intake / mid-wing armor plates
    for (sign in floatArrayOf(-1f, 1f)) {
        val wingPlate = Path().apply {
            moveTo(cx + 4f, cy + sign * 6.5f)
            lineTo(cx - 9.5f, cy + sign * 17.5f)
            lineTo(cx - 13.5f, cy + sign * 15f)
            lineTo(cx - 7.5f, cy + sign * 6.5f)
            close()
        }
        drawPath(wingPlate, Color(0xFF0F172A).copy(alpha = 0.72f))
        drawPath(wingPlate, Color(0xFF64748B).copy(alpha = 0.65f), style = Stroke(width = 0.85f))
    }

    // Raised razor central fuselage spine (bright metallic silver gradient)
    val fuselage = Path().apply {
        moveTo(cx + 26f, cy)
        lineTo(cx + 9f, cy - 5.2f)
        lineTo(cx - 9.5f, cy - 6.6f)
        lineTo(cx - 13.5f, cy - 2.6f)
        lineTo(cx - 11f, cy)
        lineTo(cx - 13.5f, cy + 2.6f)
        lineTo(cx - 9.5f, cy + 6.6f)
        lineTo(cx + 9f, cy + 5.2f)
        close()
    }
    drawPath(
        path = fuselage,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFCBD5E1), Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFF334155)),
            start = Offset(cx + 14f, cy - 7f),
            end = Offset(cx - 10f, cy + 7f)
        )
    )
    drawPath(path = fuselage, color = Color(0xFF0F172A), style = Stroke(width = 1.1f))

    // Crisp sci-fi panel lines along spine & wings
    drawLine(Color(0xFF0F172A), Offset(cx + 15.5f, cy), Offset(cx + 24.5f, cy), strokeWidth = 1.0f)
    drawLine(Color(0xFF1E293B), Offset(cx - 10.5f, cy), Offset(cx - 1f, cy), strokeWidth = 1.1f)
    for (sign in floatArrayOf(-1f, 1f)) {
        drawLine(Color(0xFF0F172A), Offset(cx + 2f, cy + sign * 5.2f), Offset(cx - 10.5f, cy + sign * 12.2f), strokeWidth = 1.0f)
        drawLine(Color(0xFF94A3B8).copy(alpha = 0.55f), Offset(cx + 6f, cy + sign * 4.0f), Offset(cx - 8f, cy + sign * 5.0f), strokeWidth = 0.9f)
    }

    // Bright cyan neon wingtip & leading-edge strips
    val neonGlow = 0.80f + 0.20f * sin(time * 5.5f)
    for (sign in floatArrayOf(-1f, 1f)) {
        // Outer glow
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.38f * neonGlow),
            start = Offset(cx + 0.5f, cy + sign * 11.8f),
            end = Offset(cx - 12.5f, cy + sign * 20.5f),
            strokeWidth = 4.6f,
            cap = StrokeCap.Round
        )
        // Core bright neon cyan strip
        drawLine(
            color = Color(0xFF00F0FF),
            start = Offset(cx + 0.5f, cy + sign * 11.8f),
            end = Offset(cx - 12.5f, cy + sign * 20.5f),
            strokeWidth = 1.9f,
            cap = StrokeCap.Round
        )
        // Trailing tip accent
        drawLine(
            color = Color(0xFF7DD3FC),
            start = Offset(cx - 11.8f, cy + sign * 20.8f),
            end = Offset(cx - 17.0f, cy + sign * 17.6f),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }

    // Glowing cyan faceted cockpit canopy
    val canopy = Path().apply {
        moveTo(cx + 15.5f, cy)
        lineTo(cx + 7.5f, cy - 3.6f)
        lineTo(cx - 1.8f, cy - 2.6f)
        lineTo(cx - 4.2f, cy)
        lineTo(cx - 1.8f, cy + 2.6f)
        lineTo(cx + 7.5f, cy + 3.6f)
        close()
    }
    drawCircle(
        color = Color(0xFF00E5FF).copy(alpha = 0.26f),
        radius = 8.5f,
        center = Offset(cx + 6.5f, cy)
    )
    drawPath(
        path = canopy,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0F2FE), Color(0xFF00E5FF), Color(0xFF0284C7)),
            start = Offset(cx + 14f, cy - 3.5f),
            end = Offset(cx - 3f, cy + 3.5f)
        )
    )
    drawPath(path = canopy, color = Color(0xFF0F172A), style = Stroke(width = 1.1f))
    // Specular glass highlight
    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(cx + 11.5f, cy - 1.1f),
        end = Offset(cx + 4.5f, cy - 1.8f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )

    if (hitFlash > 0.08f) {
        drawPath(mainWings, Color(0xFFFF6D00).copy(alpha = 0.35f * hitFlash))
        drawPath(mainWings, Color(0xFFFFB74D).copy(alpha = 0.8f * hitFlash), style = Stroke(width = 1.8f))
    }
}

internal fun DrawScope.drawBattlecruiserShip(
    cx: Float,
    cy: Float,
    thrust: Float,
    pulse: Float,
    time: Float,
    hitFlash: Float,
    showThrusterFlames: Boolean = true
) {
    val corePulse = 0.78f + 0.22f * sin(time * 6.2f)

    // 1. Double / Twin Main Thrusters venting existing dynamic cyan plasma exhaust
    if (showThrusterFlames) {
        drawPlasmaThrusterFlame(cx - 20.6f, cy - 4.8f, thrust, maxLen = 24f, halfW = 3.8f, pulse = pulse, phaseOffset = 0.0f)
        drawPlasmaThrusterFlame(cx - 20.6f, cy + 4.8f, thrust, maxLen = 24f, halfW = 3.8f, pulse = pulse, phaseOffset = 1.9f)
    }

    // Twin Main Rear Thruster Nozzles (layered dark-steel bells + cyan exhaust collar)
    for (sign in floatArrayOf(-1f, 1f)) {
        val ny = cy + sign * 4.8f
        val nozzle = Path().apply {
            moveTo(cx - 15.2f, ny - 3.3f)
            lineTo(cx - 21.0f, ny - 2.5f)
            lineTo(cx - 21.0f, ny + 2.5f)
            lineTo(cx - 15.2f, ny + 3.3f)
            close()
        }
        drawPath(
            path = nozzle,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF64748B), Color(0xFF1E293B), Color(0xFF090D16)),
                startY = ny - 3.3f,
                endY = ny + 3.3f
            )
        )
        drawPath(path = nozzle, color = Color(0xFFE2E8F0).copy(alpha = 0.80f), style = Stroke(width = 0.85f))
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.72f + 0.28f * thrust),
            start = Offset(cx - 20.4f, ny - 1.8f),
            end = Offset(cx - 20.4f, ny + 1.8f),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
        )
    }

    // 2. Angular "Shoulder" & Winglet Armor Plates (extending outward & slightly backward with stepped-panel layers & bright white highlights)
    val shoulderWingPaths = ArrayList<Path>(2)
    for (sign in floatArrayOf(-1f, 1f)) {
        // Base outer swept angular winglet
        val outerWinglet = Path().apply {
            moveTo(cx - 1.2f, cy + sign * 9.8f)
            lineTo(cx - 3.0f, cy + sign * 21.4f)
            lineTo(cx - 8.2f, cy + sign * 24.8f)
            lineTo(cx - 12.2f, cy + sign * 21.5f)
            lineTo(cx - 10.8f, cy + sign * 19.0f)
            lineTo(cx - 15.4f, cy + sign * 17.6f)
            lineTo(cx - 13.8f, cy + sign * 14.5f)
            lineTo(cx - 16.8f, cy + sign * 12.2f)
            lineTo(cx - 13.2f, cy + sign * 9.2f)
            close()
        }
        shoulderWingPaths.add(outerWinglet)
        drawPath(
            path = outerWinglet,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A)),
                start = Offset(cx - 3f, cy + sign * 24.8f),
                end = Offset(cx - 16f, cy + sign * 9.2f)
            )
        )
        drawPath(
            path = outerWinglet,
            color = Color(0xFFF8FAFC).copy(alpha = 0.90f),
            style = Stroke(width = 1.05f, join = StrokeJoin.Miter)
        )

        // Mid stepped armor layer (steel-gray metallic)
        val midStepPlate = Path().apply {
            moveTo(cx - 3.4f, cy + sign * 11.4f)
            lineTo(cx - 4.8f, cy + sign * 20.4f)
            lineTo(cx - 9.0f, cy + sign * 21.6f)
            lineTo(cx - 12.2f, cy + sign * 16.4f)
            lineTo(cx - 11.4f, cy + sign * 11.2f)
            close()
        }
        drawPath(
            path = midStepPlate,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF64748B), Color(0xFF334155), Color(0xFF111827)),
                start = Offset(cx - 4f, cy + sign * 21.5f),
                end = Offset(cx - 12f, cy + sign * 11.2f)
            )
        )
        drawPath(path = midStepPlate, color = Color(0xFF0F172A), style = Stroke(width = 0.9f))

        // Upper brushed-silver stepped bevel plate with bright white outer trim
        val upperBevelPlate = Path().apply {
            moveTo(cx - 5.4f, cy + sign * 13.2f)
            lineTo(cx - 6.4f, cy + sign * 18.6f)
            lineTo(cx - 9.6f, cy + sign * 18.4f)
            lineTo(cx - 10.6f, cy + sign * 13.6f)
            close()
        }
        drawPath(
            path = upperBevelPlate,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFCBD5E1), Color(0xFF64748B), Color(0xFF334155)),
                start = Offset(cx - 5.5f, cy + sign * 18.6f),
                end = Offset(cx - 10.6f, cy + sign * 13.2f)
            )
        )
        drawPath(path = upperBevelPlate, color = Color(0xFFF8FAFC).copy(alpha = 0.85f), style = Stroke(width = 0.8f))
    }

    // 3. Heavy Tapered Rear Engine Block with Pointed Stabilizers & Layered Armor Casing
    // Twin pointed rear flanking stabilizer fins
    for (sign in floatArrayOf(-1f, 1f)) {
        val sideFin = Path().apply {
            moveTo(cx - 12.0f, cy + sign * 7.6f)
            lineTo(cx - 22.8f, cy + sign * 9.8f) // pointed outer stabilizer fin
            lineTo(cx - 19.6f, cy + sign * 6.0f)
            lineTo(cx - 13.5f, cy + sign * 5.4f)
            close()
        }
        drawPath(
            path = sideFin,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0B1120)),
                start = Offset(cx - 12f, cy + sign * 9.8f),
                end = Offset(cx - 22f, cy + sign * 5.4f)
            )
        )
        drawPath(path = sideFin, color = Color(0xFFE2E8F0).copy(alpha = 0.86f), style = Stroke(width = 0.9f, join = StrokeJoin.Miter))
    }

    // Main tapered rear engine armor casing & pointed central stabilizer spine
    val engineBlock = Path().apply {
        moveTo(cx - 8.5f, cy - 9.4f)
        lineTo(cx - 16.4f, cy - 8.0f)
        lineTo(cx - 19.8f, cy - 6.8f)
        lineTo(cx - 19.8f, cy - 2.6f)
        lineTo(cx - 24.8f, cy) // pointed central rear stabilizer tip
        lineTo(cx - 19.8f, cy + 2.6f)
        lineTo(cx - 19.8f, cy + 6.8f)
        lineTo(cx - 16.4f, cy + 8.0f)
        lineTo(cx - 8.5f, cy + 9.4f)
        close()
    }
    drawPath(
        path = engineBlock,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF475569), Color(0xFF334155), Color(0xFF0F172A)),
            start = Offset(cx - 9f, cy - 9.4f),
            end = Offset(cx - 24f, cy + 9.4f)
        )
    )
    drawPath(path = engineBlock, color = Color(0xFFF1F5F9).copy(alpha = 0.88f), style = Stroke(width = 1.0f, join = StrokeJoin.Round))

    // Layered inner engine armor cowling plate + central stabilizer highlight
    val enginePlate = Path().apply {
        moveTo(cx - 10.6f, cy - 6.0f)
        lineTo(cx - 18.2f, cy - 4.4f)
        lineTo(cx - 19.4f, cy)
        lineTo(cx - 18.2f, cy + 4.4f)
        lineTo(cx - 10.6f, cy + 6.0f)
        close()
    }
    drawPath(
        path = enginePlate,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF64748B), Color(0xFF334155), Color(0xFF1E293B)),
            start = Offset(cx - 11f, cy - 6f),
            end = Offset(cx - 19f, cy + 6f)
        )
    )
    drawPath(path = enginePlate, color = Color(0xFF0F172A), style = Stroke(width = 0.9f))
    drawLine(
        color = Color(0xFFF8FAFC).copy(alpha = 0.92f),
        start = Offset(cx - 13.2f, cy),
        end = Offset(cx - 24.0f, cy),
        strokeWidth = 1.15f,
        cap = StrokeCap.Round
    )

    // 4. Small Forward-Facing Weapon / Laser Barrels on Outer Edges of Pincers
    for (sign in floatArrayOf(-1f, 1f)) {
        // Outer shoulder barrel
        val byOuter = cy + sign * 13.8f
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(cx + 19.2f, byOuter),
            end = Offset(cx + 27.6f, byOuter),
            strokeWidth = 2.3f,
            cap = StrokeCap.Butt
        )
        drawLine(
            color = Color(0xFFF1F5F9).copy(alpha = 0.92f),
            start = Offset(cx + 20.0f, byOuter - sign * 0.55f),
            end = Offset(cx + 27.2f, byOuter - sign * 0.55f),
            strokeWidth = 0.8f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color(0xFF7DD3FC), radius = 1.15f, center = Offset(cx + 27.9f, byOuter))

        // Forward pincer-tip outer edge barrel
        val byTip = cy + sign * 8.8f
        drawLine(
            color = Color(0xFF334155),
            start = Offset(cx + 23.6f, byTip),
            end = Offset(cx + 29.8f, byTip),
            strokeWidth = 1.9f,
            cap = StrokeCap.Butt
        )
        drawLine(
            color = Color.White.copy(alpha = 0.92f),
            start = Offset(cx + 24.2f, byTip - sign * 0.45f),
            end = Offset(cx + 29.5f, byTip - sign * 0.45f),
            strokeWidth = 0.7f,
            cap = StrokeCap.Round
        )
        drawCircle(color = Color(0xFFBAE6FD), radius = 1.0f, center = Offset(cx + 30.1f, byTip))
    }

    // 5. Massive Circular Curved Pincers ("O" / "U" Horseshoe Shape, ~18% larger than Interceptor)
    val pincerPaths = ArrayList<Path>(2)
    for (sign in floatArrayOf(-1f, 1f)) {
        val pincer = Path().apply {
            // Heavy armored root anchored solidly at mid-hull
            moveTo(cx - 3.6f, cy + sign * 17.6f)
            // Sweeping circular outer crescent wrapping boldly forward & inward
            cubicTo(
                cx + 10.6f, cy + sign * 25.4f,
                cx + 26.8f, cy + sign * 19.2f,
                cx + 28.4f, cy + sign * 3.1f // sharp pointed tip with crisp narrow front gap (~6.2 units)
            )
            // Razor beveled inner tip
            lineTo(cx + 25.6f, cy + sign * 4.5f)
            // Smooth circular inner crescent edge enclosing the central needle-nose & cockpit
            cubicTo(
                cx + 22.8f, cy + sign * 13.4f,
                cx + 11.4f, cy + sign * 15.6f,
                cx - 1.0f, cy + sign * 9.0f
            )
            close()
        }
        pincerPaths.add(pincer)

        // Base deep charcoal & dark steel-gray metallic fill
        drawPath(
            path = pincer,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0B1120),
                    Color(0xFF1E293B),
                    Color(0xFF334155),
                    Color(0xFF1E293B),
                    Color(0xFF090D16)
                ),
                start = Offset(cx + 2f, cy + sign * 24f),
                end = Offset(cx + 27f, cy + sign * 3.5f)
            )
        )

        // Intricate Layered Armor Sub-Panel 1: Sculpted Metallic Silver Outer Crescent Plate
        val outerArmorPlate = Path().apply {
            moveTo(cx + 0.6f, cy + sign * 17.8f)
            cubicTo(
                cx + 9.8f, cy + sign * 22.0f,
                cx + 20.6f, cy + sign * 18.6f,
                cx + 25.0f, cy + sign * 10.6f
            )
            lineTo(cx + 22.8f, cy + sign * 9.6f)
            cubicTo(
                cx + 18.8f, cy + sign * 16.0f,
                cx + 9.8f, cy + sign * 18.6f,
                cx + 1.6f, cy + sign * 14.8f
            )
            close()
        }
        drawPath(
            path = outerArmorPlate,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFF475569), Color(0xFF1E293B)),
                start = Offset(cx + 2f, cy + sign * 20f),
                end = Offset(cx + 24f, cy + sign * 10f)
            )
        )
        drawPath(path = outerArmorPlate, color = Color(0xFF0B1120).copy(alpha = 0.90f), style = Stroke(width = 0.85f))

        // Intricate Layered Armor Sub-Panel 2: Inner Dark Steel-Gray Recessed Curve Plate
        val innerArmorPlate = Path().apply {
            moveTo(cx + 2.8f, cy + sign * 13.6f)
            cubicTo(
                cx + 10.2f, cy + sign * 16.4f,
                cx + 17.8f, cy + sign * 14.8f,
                cx + 21.8f, cy + sign * 9.4f
            )
            lineTo(cx + 20.4f, cy + sign * 8.4f)
            cubicTo(
                cx + 16.8f, cy + sign * 13.0f,
                cx + 10.2f, cy + sign * 14.4f,
                cx + 3.4f, cy + sign * 11.8f
            )
            close()
        }
        drawPath(
            path = innerArmorPlate,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF334155), Color(0xFF64748B), Color(0xFF1E293B)),
                start = Offset(cx + 3f, cy + sign * 14f),
                end = Offset(cx + 21f, cy + sign * 8.5f)
            )
        )
        drawPath(path = innerArmorPlate, color = Color(0xFF090D16), style = Stroke(width = 0.75f))

        // Intricate Layered Armor Sub-Panel 3: Pointed Forward Tip Metallic Bevel Cap
        val tipBevel = Path().apply {
            moveTo(cx + 27.5f, cy + sign * 3.8f)
            cubicTo(
                cx + 26.8f, cy + sign * 7.2f,
                cx + 25.4f, cy + sign * 9.6f,
                cx + 23.6f, cy + sign * 11.2f
            )
            lineTo(cx + 22.4f, cy + sign * 9.6f)
            cubicTo(
                cx + 23.8f, cy + sign * 8.2f,
                cx + 24.8f, cy + sign * 6.4f,
                cx + 25.4f, cy + sign * 4.8f
            )
            close()
        }
        drawPath(
            path = tipBevel,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE2E8F0), Color(0xFF64748B)),
                start = Offset(cx + 27.5f, cy + sign * 3.8f),
                end = Offset(cx + 22.4f, cy + sign * 11.0f)
            )
        )

        // Clean recessed dark armor plate divider seams
        drawLine(
            color = Color(0xFF090D16).copy(alpha = 0.90f),
            start = Offset(cx + 8.6f, cy + sign * 21.0f),
            end = Offset(cx + 9.0f, cy + sign * 13.6f),
            strokeWidth = 0.95f
        )
        drawLine(
            color = Color(0xFF090D16).copy(alpha = 0.90f),
            start = Offset(cx + 18.2f, cy + sign * 18.6f),
            end = Offset(cx + 16.4f, cy + sign * 12.6f),
            strokeWidth = 0.95f
        )

        // High-Contrast Bright White / Cyan Neon Edge Highlights (outer & inner crescent border paths)
        drawPath(
            path = pincer,
            color = Color(0xFF00E5FF).copy(alpha = 0.34f * corePulse),
            style = Stroke(width = 2.9f, join = StrokeJoin.Round)
        )
        drawPath(
            path = pincer,
            color = Color(0xFFF0F9FF).copy(alpha = 0.96f),
            style = Stroke(width = 1.15f, join = StrokeJoin.Round)
        )
    }

    // 6. Central Body & Fuselage (Needle-Like Inner Nose Completely Enveloped by Curved Pincers)
    val centralHull = Path().apply {
        moveTo(cx + 16.4f, cy) // streamlined needle-like forward core
        lineTo(cx + 9.8f, cy - 3.6f)
        lineTo(cx + 4.0f, cy - 7.0f)
        lineTo(cx - 4.4f, cy - 9.4f)
        lineTo(cx - 11.8f, cy - 7.4f)
        lineTo(cx - 14.8f, cy - 4.0f)
        lineTo(cx - 14.8f, cy + 4.0f)
        lineTo(cx - 11.8f, cy + 7.4f)
        lineTo(cx - 4.4f, cy + 9.4f)
        lineTo(cx + 4.0f, cy + 7.0f)
        lineTo(cx + 9.8f, cy + 3.6f)
        close()
    }
    drawPath(
        path = centralHull,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF64748B),
                Color(0xFF94A3B8),
                Color(0xFF334155),
                Color(0xFF0F172A)
            ),
            start = Offset(cx + 4f, cy - 9.4f),
            end = Offset(cx - 10f, cy + 9.4f)
        )
    )
    drawPath(
        path = centralHull,
        color = Color(0xFF00E5FF).copy(alpha = 0.22f),
        style = Stroke(width = 2.2f, join = StrokeJoin.Round)
    )
    drawPath(
        path = centralHull,
        color = Color(0xFFF1F5F9).copy(alpha = 0.92f),
        style = Stroke(width = 1.05f, join = StrokeJoin.Round)
    )

    // Sleek Needle-Nose Prow & Fine Metallic Spire centered inside the hollow crescent
    val needleProw = Path().apply {
        moveTo(cx + 21.8f, cy)
        lineTo(cx + 11.2f, cy - 2.0f)
        lineTo(cx + 9.6f, cy)
        lineTo(cx + 11.2f, cy + 2.0f)
        close()
    }
    drawPath(
        path = needleProw,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFF94A3B8), Color(0xFF334155)),
            start = Offset(cx + 21f, cy - 2f),
            end = Offset(cx + 10f, cy + 2f)
        )
    )
    drawPath(path = needleProw, color = Color(0xFF0F172A), style = Stroke(width = 0.75f))
    drawLine(
        color = Color(0xFFE0F2FE),
        start = Offset(cx + 19.6f, cy),
        end = Offset(cx + 23.0f, cy),
        strokeWidth = 1.05f,
        cap = StrokeCap.Round
    )

    // 7. Streamlined Dark Armored Cockpit centered toward the back of the inner section + Glowing GREEN Jewel Canopy/Core
    val cockpitHousing = Path().apply {
        moveTo(cx + 6.8f, cy)
        cubicTo(
            cx + 4.8f, cy - 3.6f,
            cx - 0.6f, cy - 4.8f,
            cx - 6.6f, cy - 3.6f
        )
        lineTo(cx - 8.4f, cy)
        lineTo(cx - 6.6f, cy + 3.6f)
        cubicTo(
            cx - 0.6f, cy + 4.8f,
            cx + 4.8f, cy + 3.6f,
            cx + 6.8f, cy
        )
        close()
    }
    drawPath(
        path = cockpitHousing,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0B1120), Color(0xFF064E3B)),
            start = Offset(cx + 6f, cy - 4.5f),
            end = Offset(cx - 8f, cy + 4.5f)
        )
    )
    drawPath(
        path = cockpitHousing,
        color = Color(0xFFCBD5E1).copy(alpha = 0.85f),
        style = Stroke(width = 0.9f, join = StrokeJoin.Round)
    )

    // Multi-layered Glowing Green Jewel Canopy / Energy Core
    val greenJewel = Path().apply {
        moveTo(cx + 2.8f, cy)
        cubicTo(
            cx + 1.2f, cy - 2.6f,
            cx - 2.4f, cy - 3.1f,
            cx - 5.4f, cy - 2.2f
        )
        lineTo(cx - 6.6f, cy)
        lineTo(cx - 5.4f, cy + 2.2f)
        cubicTo(
            cx - 2.4f, cy + 3.1f,
            cx + 1.2f, cy + 2.6f,
            cx + 2.8f, cy
        )
        close()
    }
    // Soft glowing emerald aura around the jewel core
    drawCircle(
        color = Color(0xFF22C55E).copy(alpha = 0.28f * corePulse),
        radius = 6.2f,
        center = Offset(cx - 2.0f, cy)
    )
    drawPath(
        path = greenJewel,
        color = Color(0xFF4ADE80).copy(alpha = 0.45f * corePulse),
        style = Stroke(width = 2.6f)
    )
    drawPath(
        path = greenJewel,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFECFDF5),
                Color(0xFF4ADE80),
                Color(0xFF16A34A),
                Color(0xFF065F46)
            ),
            center = Offset(cx - 1.8f, cy - 0.5f),
            radius = 5.0f
        )
    )
    drawPath(path = greenJewel, color = Color(0xFFBBF7D0).copy(alpha = 0.92f), style = Stroke(width = 0.85f))
    // Jewel crystal facet highlight & specular glint
    drawLine(
        color = Color.White.copy(alpha = 0.95f),
        start = Offset(cx + 0.8f, cy - 0.9f),
        end = Offset(cx - 3.4f, cy - 1.4f),
        strokeWidth = 1.1f,
        cap = StrokeCap.Round
    )
    drawCircle(color = Color(0xFFDCFCE7), radius = 1.0f, center = Offset(cx - 1.8f, cy))

    if (hitFlash > 0.08f) {
        val flashFill = Color(0xFFFF6D00).copy(alpha = 0.35f * hitFlash)
        val flashStroke = Color(0xFFFFB74D).copy(alpha = 0.80f * hitFlash)
        for (p in pincerPaths) {
            drawPath(p, flashFill)
            drawPath(p, flashStroke, style = Stroke(width = 1.8f))
        }
        drawPath(centralHull, flashFill)
        drawPath(centralHull, flashStroke, style = Stroke(width = 1.8f))
        for (w in shoulderWingPaths) {
            drawPath(w, flashFill)
        }
    }
}

private fun DrawScope.drawTankCitadelShip(
    cx: Float,
    cy: Float,
    thrust: Float,
    pulse: Float,
    time: Float,
    hitFlash: Float
) {
    // 4 heavy rear thrusters (Citadel quad capital-ship engine bank)
    drawPlasmaThrusterFlame(cx - 24.5f, cy - 15.2f, thrust, maxLen = 20f, halfW = 3.8f, pulse = pulse, phaseOffset = 0.4f)
    drawPlasmaThrusterFlame(cx - 26.5f, cy - 6.0f, thrust, maxLen = 24f, halfW = 4.5f, pulse = pulse, phaseOffset = 1.6f)
    drawPlasmaThrusterFlame(cx - 26.5f, cy + 6.0f, thrust, maxLen = 24f, halfW = 4.5f, pulse = pulse, phaseOffset = 2.9f)
    drawPlasmaThrusterFlame(cx - 24.5f, cy + 15.2f, thrust, maxLen = 20f, halfW = 3.8f, pulse = pulse, phaseOffset = 4.1f)

    // Heavy armored thruster shrouds (4 units)
    val thrusterCoords = arrayOf(
        Offset(cx - 25.0f, cy - 15.2f),
        Offset(cx - 27.0f, cy - 6.0f),
        Offset(cx - 27.0f, cy + 6.0f),
        Offset(cx - 25.0f, cy + 15.2f)
    )
    for (pt in thrusterCoords) {
        drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(pt.x, pt.y - 3.4f),
            size = Size(6.5f, 6.8f)
        )
        drawRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(pt.x, pt.y - 3.4f),
            size = Size(6.5f, 6.8f),
            style = Stroke(width = 1.0f)
        )
    }

    // Heavy wide fortified dreadnought base hull with twin reinforced front prows
    val citadelHull = Path().apply {
        moveTo(cx + 32f, cy - 8.5f)
        lineTo(cx + 28f, cy - 17.5f)
        lineTo(cx + 16f, cy - 23f)
        lineTo(cx + 11f, cy - 30f)
        lineTo(cx - 14f, cy - 30.5f)
        lineTo(cx - 24.5f, cy - 21f)
        lineTo(cx - 24.5f, cy - 10.5f)
        lineTo(cx - 26.5f, cy - 9f)
        lineTo(cx - 26.5f, cy + 9f)
        lineTo(cx - 24.5f, cy + 10.5f)
        lineTo(cx - 24.5f, cy + 21f)
        lineTo(cx - 14f, cy + 30.5f)
        lineTo(cx + 11f, cy + 30f)
        lineTo(cx + 16f, cy + 23f)
        lineTo(cx + 28f, cy + 17.5f)
        lineTo(cx + 32f, cy + 8.5f)
        lineTo(cx + 20.5f, cy + 5.5f)
        lineTo(cx + 20.5f, cy - 5.5f)
        close()
    }
    drawPath(
        path = citadelHull,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF475569), Color(0xFF1E293B)),
            start = Offset(cx, cy - 31f),
            end = Offset(cx, cy + 31f)
        )
    )
    drawPath(path = citadelHull, color = Color(0xFFCBD5E1).copy(alpha = 0.85f), style = Stroke(width = 1.7f))

    // Reinforced twin front prow / siege bulwarks (thick layered titanium plates)
    for (sign in floatArrayOf(-1f, 1f)) {
        val prowPlate = Path().apply {
            moveTo(cx + 31f, cy + sign * 8.8f)
            lineTo(cx + 27f, cy + sign * 16.5f)
            lineTo(cx + 11f, cy + sign * 20.5f)
            lineTo(cx + 8f, cy + sign * 11.5f)
            lineTo(cx + 21f, cy + sign * 7.2f)
            close()
        }
        drawPath(
            path = prowPlate,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF475569)),
                start = Offset(cx + 31f, cy + sign * 8f),
                end = Offset(cx + 9f, cy + sign * 20f)
            )
        )
        drawPath(path = prowPlate, color = Color(0xFF0F172A), style = Stroke(width = 1.25f))

        // Outer flank heavy titanium side-skirt armor block
        val flankBlock = Path().apply {
            moveTo(cx + 10f, cy + sign * 28.5f)
            lineTo(cx - 13f, cy + sign * 28.8f)
            lineTo(cx - 21.5f, cy + sign * 20f)
            lineTo(cx - 17f, cy + sign * 15.5f)
            lineTo(cx + 12f, cy + sign * 19.5f)
            close()
        }
        drawPath(
            path = flankBlock,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF475569), Color(0xFF64748B), Color(0xFF1E293B)),
                start = Offset(cx + 10f, cy + sign * 16f),
                end = Offset(cx - 20f, cy + sign * 29f)
            )
        )
        drawPath(path = flankBlock, color = Color(0xFF0F172A), style = Stroke(width = 1.2f))
    }

    // Thick central armored titanium deck tier
    val innerDeck = Path().apply {
        moveTo(cx + 19.5f, cy - 6.5f)
        lineTo(cx + 11f, cy - 16.5f)
        lineTo(cx - 15f, cy - 17.5f)
        lineTo(cx - 22f, cy - 10.5f)
        lineTo(cx - 22f, cy + 10.5f)
        lineTo(cx - 15f, cy + 17.5f)
        lineTo(cx + 11f, cy + 16.5f)
        lineTo(cx + 19.5f, cy + 6.5f)
        close()
    }
    drawPath(
        path = innerDeck,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFF64748B), Color(0xFF334155)),
            start = Offset(cx + 10f, cy - 18f),
            end = Offset(cx - 14f, cy + 18f)
        )
    )
    drawPath(path = innerDeck, color = Color(0xFF0F172A), style = Stroke(width = 1.4f))

    // Structural panel lines & armor ribs
    for (xRib in floatArrayOf(-12f, -2f, 9f)) {
        drawLine(Color(0xFF0F172A).copy(alpha = 0.85f), Offset(cx + xRib, cy - 16.5f), Offset(cx + xRib, cy + 16.5f), strokeWidth = 1.15f)
    }

    // Subtle glowing blue/cyan shield-emitter strips along both flanks & prow edges
    val emitterPulse = 0.72f + 0.28f * sin(time * 5.0f)
    for (sign in floatArrayOf(-1f, 1f)) {
        // Long flank shield emitter bar
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.30f * emitterPulse),
            start = Offset(cx - 12.5f, cy + sign * 24.2f),
            end = Offset(cx + 9.5f, cy + sign * 24.2f),
            strokeWidth = 4.6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.92f),
            start = Offset(cx - 12.5f, cy + sign * 24.2f),
            end = Offset(cx + 9.5f, cy + sign * 24.2f),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
        // Forward bulwark emitter strip
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.28f * emitterPulse),
            start = Offset(cx + 15.5f, cy + sign * 13.5f),
            end = Offset(cx + 26.5f, cy + sign * 12.2f),
            strokeWidth = 3.8f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF7DD3FC).copy(alpha = 0.90f),
            start = Offset(cx + 15.5f, cy + sign * 13.5f),
            end = Offset(cx + 26.5f, cy + sign * 12.2f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
    }

    // Heavily armored elevated bridge citadel
    val bridge = Path().apply {
        moveTo(cx + 10.5f, cy - 5.2f)
        lineTo(cx + 4f, cy - 9.2f)
        lineTo(cx - 10.5f, cy - 9.2f)
        lineTo(cx - 14.5f, cy - 4.8f)
        lineTo(cx - 14.5f, cy + 4.8f)
        lineTo(cx - 10.5f, cy + 9.2f)
        lineTo(cx + 4f, cy + 9.2f)
        lineTo(cx + 10.5f, cy + 5.2f)
        close()
    }
    drawPath(
        path = bridge,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF1E293B)),
            start = Offset(cx, cy - 9.5f),
            end = Offset(cx, cy + 9.5f)
        )
    )
    drawPath(path = bridge, color = Color(0xFF94A3B8), style = Stroke(width = 1.35f))

    // Armored bridge recessed glowing viewport slit
    val bridgeVisor = Path().apply {
        moveTo(cx + 8.6f, cy - 4.2f)
        lineTo(cx + 5.4f, cy - 5.2f)
        lineTo(cx + 4.4f, cy)
        lineTo(cx + 5.4f, cy + 5.2f)
        lineTo(cx + 8.6f, cy + 4.2f)
        lineTo(cx + 7.2f, cy)
        close()
    }
    drawPath(
        path = bridgeVisor,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0284C7)),
            start = Offset(cx + 8.5f, cy - 5f),
            end = Offset(cx + 4.5f, cy + 5f)
        )
    )
    // Shield generator core node on rear bridge deck
    drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.30f * emitterPulse), radius = 6.0f, center = Offset(cx - 4.5f, cy))
    drawCircle(color = Color(0xFF38BDF8), radius = 3.1f, center = Offset(cx - 4.5f, cy))
    drawCircle(color = Color.White, radius = 1.3f, center = Offset(cx - 4.5f, cy))

    if (hitFlash > 0.08f) {
        drawPath(citadelHull, Color(0xFFFF6D00).copy(alpha = 0.35f * hitFlash))
        drawPath(citadelHull, Color(0xFFFFB74D).copy(alpha = 0.8f * hitFlash), style = Stroke(width = 2.2f))
    }
}

private fun DrawScope.drawOrbitingVants(engine: SpaceGameEngine) {
    val vants = engine.getActiveVants()
    if (vants.isEmpty()) return

    val player = engine.player
    val formation = engine.activeFormation
    val formAccent = Color(formation.accentHex)
    val pulse = 0.75f + 0.25f * sin(engine.elapsedTime * 6.5f)

    // Subtle formation aura ring for RING formation only (WHEEL draws drones only, no extra rings)
    if (formation == DroneFormation.RING) {
        val ringR = player.shipClass.hullRadius + 20f
        drawCircle(
            color = formAccent.copy(alpha = 0.16f),
            radius = ringR,
            center = Offset(player.x, player.y),
            style = Stroke(
                width = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 7f), engine.elapsedTime * 22f)
            )
        )
    }

    for (vant in vants) {
        val vx = if (vant.hasInitializedPosition) vant.worldX else player.x
        val vy = if (vant.hasInitializedPosition) vant.worldY else player.y
        val center = Offset(vx, vy)
        val facingDeg = Math.toDegrees(vant.facingRad.toDouble()).toFloat()

        rotate(degrees = facingDeg, pivot = center) {
            when (vant.tier) {
                VantTier.FLAX -> {
                    // Sleek silver/cyan mini-drone
                    drawCircle(
                        color = formAccent.copy(alpha = 0.22f * pulse),
                        radius = 8.5f,
                        center = center
                    )
                    val hull = Path().apply {
                        moveTo(vx + 7.5f, vy)
                        lineTo(vx - 3.5f, vy - 5.2f)
                        lineTo(vx - 6.0f, vy)
                        lineTo(vx - 3.5f, vy + 5.2f)
                        close()
                    }
                    drawPath(path = hull, color = Color(0xFFCBD5E1))
                    drawPath(path = hull, color = CyanLaser, style = Stroke(width = 1.4f))
                    drawCircle(color = CyanLaser, radius = 2.0f, center = Offset(vx + 1.0f, vy))
                    drawCircle(color = Color.White, radius = 0.9f, center = Offset(vx + 1.0f, vy))
                }

                VantTier.IRIS -> {
                    // Golden/blue winged mini-drone
                    drawCircle(
                        color = AmberCredits.copy(alpha = 0.26f * pulse),
                        radius = 10.5f,
                        center = center
                    )
                    val wings = Path().apply {
                        moveTo(vx + 9.0f, vy)
                        lineTo(vx + 1.5f, vy - 4.0f)
                        lineTo(vx - 4.5f, vy - 9.0f)
                        lineTo(vx - 7.0f, vy - 4.5f)
                        lineTo(vx - 5.0f, vy)
                        lineTo(vx - 7.0f, vy + 4.5f)
                        lineTo(vx - 4.5f, vy + 9.0f)
                        lineTo(vx + 1.5f, vy + 4.0f)
                        close()
                    }
                    drawPath(path = wings, color = Color(0xFF1D4ED8))
                    drawPath(path = wings, color = AmberCredits, style = Stroke(width = 1.6f))
                    drawLine(
                        color = Color(0xFFFDE68A),
                        start = Offset(vx - 3.0f, vy - 6.8f),
                        end = Offset(vx + 3.0f, vy - 2.6f),
                        strokeWidth = 1.3f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFFDE68A),
                        start = Offset(vx - 3.0f, vy + 6.8f),
                        end = Offset(vx + 3.0f, vy + 2.6f),
                        strokeWidth = 1.3f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = CyanThruster, radius = 2.6f, center = Offset(vx + 0.5f, vy))
                    drawCircle(color = Color.White, radius = 1.1f, center = Offset(vx + 0.5f, vy))
                }
            }
        }
    }
}

private fun DrawScope.drawPlasmaThrusterFlame(
    baseX: Float,
    baseY: Float,
    thrust: Float,
    maxLen: Float,
    halfW: Float,
    pulse: Float,
    phaseOffset: Float
) {
    // Organic multi-harmonic plasma flicker
    val flicker = 0.84f + 0.14f * sin(pulse * 2.1f + phaseOffset) + 0.08f * sin(pulse * 4.7f + phaseOffset * 1.7f)
    // Subtle idle pilot glow when stationary -> long intense plasma plume when flying
    val len = (maxLen * (0.24f + 0.88f * thrust)) * flicker
    val w = halfW * (0.68f + 0.36f * thrust)

    // Soft outer blue plasma halo at exhaust nozzle
    drawCircle(
        color = Color(0xFF0284C7).copy(alpha = 0.24f + 0.22f * thrust),
        radius = w * 1.85f,
        center = Offset(baseX - len * 0.16f, baseY)
    )

    // Outer deep-blue / cyan glow plume
    val outer = Path().apply {
        moveTo(baseX + 0.5f, baseY - w * 1.25f)
        lineTo(baseX - len * 1.15f, baseY)
        lineTo(baseX + 0.5f, baseY + w * 1.25f)
        close()
    }
    drawPath(
        path = outer,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.65f + 0.25f * thrust), Color(0xFF0284C7).copy(alpha = 0.12f)),
            start = Offset(baseX, baseY),
            end = Offset(baseX - len * 1.15f, baseY)
        )
    )

    // Mid bright cyan plasma tongue
    val mid = Path().apply {
        moveTo(baseX + 0.5f, baseY - w * 0.82f)
        lineTo(baseX - len * 0.86f, baseY)
        lineTo(baseX + 0.5f, baseY + w * 0.82f)
        close()
    }
    drawPath(
        path = mid,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF7DD3FC).copy(alpha = 0.95f), Color(0xFF00E5FF).copy(alpha = 0.45f)),
            start = Offset(baseX, baseY),
            end = Offset(baseX - len * 0.86f, baseY)
        )
    )

    // Bright white-hot plasma inner core
    val core = Path().apply {
        moveTo(baseX + 0.8f, baseY - w * 0.44f)
        lineTo(baseX - len * 0.52f, baseY)
        lineTo(baseX + 0.8f, baseY + w * 0.44f)
        close()
    }
    drawPath(path = core, color = Color.White.copy(alpha = 0.86f + 0.14f * thrust))
}

private fun DrawScope.drawAlienShip(alien: AlienEntity, elapsedTime: Float) {
    val center = Offset(alien.x, alien.y)
    val degrees = Math.toDegrees(alien.angleRad.toDouble()).toFloat()
    val corePulse = 0.72f + 0.28f * sin(alien.pulsePhase)
    val primary = Color(alien.tier.primaryColorHex)
    val accent = Color(alien.tier.accentColorHex)
    val hit = alien.hitFlash > 0.22f
    val trim = if (hit) Color.White else if (alien.isBoss) AmberCredits else primary

    // Pulsing crimson/gold BOSS aura ring around any Boss variant
    if (alien.isBoss) {
        val bossPulse = 0.55f + 0.45f * sin(elapsedTime * 5.2f + alien.id)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CrimsonDanger.copy(alpha = 0.34f * bossPulse),
                    AmberCredits.copy(alpha = 0.16f * bossPulse),
                    Color.Transparent
                ),
                center = center,
                radius = alien.radius + 18f
            ),
            radius = alien.radius + 18f,
            center = center
        )
        drawCircle(
            color = CrimsonDanger.copy(alpha = 0.78f * bossPulse),
            radius = alien.radius + 7f + 3f * bossPulse,
            center = center,
            style = Stroke(width = 2.4f)
        )
        drawCircle(
            color = AmberCredits.copy(alpha = 0.72f),
            radius = alien.radius + 3f,
            center = center,
            style = Stroke(
                width = 1.6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), elapsedTime * 28f)
            )
        )
    }

    val shipScale = if (alien.isBoss) GameConfig.BOSS_SIZE_MULTIPLIER else 1f
    withTransform({
        scale(scaleX = shipScale, scaleY = shipScale, pivot = center)
        rotate(degrees = degrees, pivot = center)
    }) {
        when (alien.tier) {
            AlienTier.SCOUT -> {
                // Zone 1: Small green/lime sharp dart triangle
                val body = if (hit) Color(0xFFDCFCE7) else Color(0xFF0E291A)
                val tri = Path().apply {
                    moveTo(alien.x + 18f, alien.y)
                    lineTo(alien.x - 13f, alien.y - 13f)
                    lineTo(alien.x - 7f, alien.y)
                    lineTo(alien.x - 13f, alien.y + 13f)
                    close()
                }
                drawPath(path = tri, color = body)
                drawPath(path = tri, color = trim, style = Stroke(width = 1.9f))
                drawCircle(
                    color = accent.copy(alpha = 0.35f * corePulse),
                    radius = 7f,
                    center = Offset(alien.x - 1f, alien.y)
                )
                drawCircle(
                    color = primary,
                    radius = 3.5f * corePulse,
                    center = Offset(alien.x - 1f, alien.y)
                )
            }

            AlienTier.HUNTER -> {
                // Zone 2: Medium yellow/amber diamond interceptor with twin fangs
                val body = if (hit) Color(0xFFFEF9C3) else Color(0xFF2B2108)
                val diamond = Path().apply {
                    moveTo(alien.x + 23f, alien.y)
                    lineTo(alien.x + 2f, alien.y - 17f)
                    lineTo(alien.x - 19f, alien.y)
                    lineTo(alien.x + 2f, alien.y + 17f)
                    close()
                }
                drawPath(path = diamond, color = body)
                drawPath(path = diamond, color = trim, style = Stroke(width = 2.0f))

                // Inner chevron armor accent
                val inner = Path().apply {
                    moveTo(alien.x + 12f, alien.y)
                    lineTo(alien.x - 2f, alien.y - 8f)
                    lineTo(alien.x - 10f, alien.y)
                    lineTo(alien.x - 2f, alien.y + 8f)
                    close()
                }
                drawPath(path = inner, color = accent.copy(alpha = 0.35f))
                drawCircle(
                    color = accent.copy(alpha = 0.4f * corePulse),
                    radius = 9f,
                    center = Offset(alien.x, alien.y)
                )
                drawCircle(
                    color = primary,
                    radius = 4.5f * corePulse,
                    center = Offset(alien.x, alien.y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.8f,
                    center = Offset(alien.x, alien.y)
                )
            }

            AlienTier.DESTROYER -> {
                // Zone 3: Large armored orange/red hexagonal gunship
                val body = if (hit) Color(0xFFFFEDD5) else Color(0xFF2E120B)
                val hexR = 26f
                val hex = Path().apply {
                    for (i in 0 until 6) {
                        val a = i * (PI.toFloat() / 3f)
                        val px = alien.x + cos(a) * hexR
                        val py = alien.y + sin(a) * hexR
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(path = hex, color = body)
                drawPath(path = hex, color = trim, style = Stroke(width = 2.4f))

                // Forward twin heavy cannons
                drawLine(
                    color = accent,
                    start = Offset(alien.x + 10f, alien.y - 12f),
                    end = Offset(alien.x + 27f, alien.y - 12f),
                    strokeWidth = 3.2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(alien.x + 10f, alien.y + 12f),
                    end = Offset(alien.x + 27f, alien.y + 12f),
                    strokeWidth = 3.2f,
                    cap = StrokeCap.Round
                )

                // Inner hex plate & glowing reactor
                val innerHex = Path().apply {
                    for (i in 0 until 6) {
                        val a = i * (PI.toFloat() / 3f)
                        val px = alien.x + cos(a) * 14f
                        val py = alien.y + sin(a) * 14f
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(path = innerHex, color = accent.copy(alpha = 0.28f))
                drawPath(path = innerHex, color = accent.copy(alpha = 0.8f), style = Stroke(width = 1.3f))
                drawCircle(
                    color = accent.copy(alpha = 0.42f * corePulse),
                    radius = 12f,
                    center = center
                )
                drawCircle(
                    color = primary,
                    radius = 6f * corePulse,
                    center = center
                )
                drawCircle(color = Color(0xFFFFF7ED), radius = 2.6f, center = center)
            }

            AlienTier.DREADNOUGHT_BOSS -> {
                // Zone 4: Massive purple/crimson capital boss ship with glowing core
                val body = if (hit) Color(0xFFF3E8FF) else Color(0xFF1F0A2B)

                // Ambient boss aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.30f * corePulse),
                            primary.copy(alpha = 0.14f * corePulse),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 52f
                    ),
                    radius = 52f,
                    center = center
                )

                // Sweeping armored outer capital wings
                val bossHull = Path().apply {
                    moveTo(alien.x + 42f, alien.y - 10f)
                    lineTo(alien.x + 24f, alien.y - 14f)
                    lineTo(alien.x + 12f, alien.y - 36f)
                    lineTo(alien.x - 16f, alien.y - 38f)
                    lineTo(alien.x - 36f, alien.y - 20f)
                    lineTo(alien.x - 28f, alien.y - 8f)
                    lineTo(alien.x - 34f, alien.y)
                    lineTo(alien.x - 28f, alien.y + 8f)
                    lineTo(alien.x - 36f, alien.y + 20f)
                    lineTo(alien.x - 16f, alien.y + 38f)
                    lineTo(alien.x + 12f, alien.y + 36f)
                    lineTo(alien.x + 24f, alien.y + 14f)
                    lineTo(alien.x + 42f, alien.y + 10f)
                    lineTo(alien.x + 30f, alien.y)
                    close()
                }
                drawPath(path = bossHull, color = body)
                drawPath(path = bossHull, color = trim, style = Stroke(width = 2.8f))

                // Crimson inner wing ribs
                drawLine(
                    color = accent.copy(alpha = 0.85f),
                    start = Offset(alien.x - 12f, alien.y - 30f),
                    end = Offset(alien.x + 18f, alien.y - 12f),
                    strokeWidth = 2.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.85f),
                    start = Offset(alien.x - 12f, alien.y + 30f),
                    end = Offset(alien.x + 18f, alien.y + 12f),
                    strokeWidth = 2.8f,
                    cap = StrokeCap.Round
                )

                // Octagonal central citadel armor ring
                val citadel = Path().apply {
                    for (i in 0 until 8) {
                        val a = i * (PI.toFloat() * 0.25f)
                        val px = alien.x + cos(a) * 19f
                        val py = alien.y + sin(a) * 19f
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(path = citadel, color = Color(0xFF340B3B))
                drawPath(path = citadel, color = accent, style = Stroke(width = 2f))

                // Massive pulsing crimson-purple reactor core
                drawCircle(
                    color = accent.copy(alpha = 0.55f * corePulse),
                    radius = 14f * corePulse,
                    center = center
                )
                drawCircle(
                    color = primary,
                    radius = 9f * corePulse,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.2f,
                    center = center
                )
            }
        }
    }
}

private fun DrawScope.drawHealthBar(
    centerX: Float,
    topY: Float,
    width: Float,
    height: Float,
    ratio: Float,
    fillColor: Color,
    glowColor: Color
) {
    val left = centerX - width * 0.5f
    val corner = CornerRadius(3f, 3f)

    drawRoundRect(
        color = Color(0xCC060B14),
        topLeft = Offset(left - 1f, topY - 1f),
        size = Size(width + 2f, height + 2f),
        cornerRadius = corner
    )
    if (ratio > 0f) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(fillColor, glowColor),
                startX = left,
                endX = left + width * ratio
            ),
            topLeft = Offset(left, topY),
            size = Size(width * ratio, height),
            cornerRadius = corner
        )
    }
    drawRoundRect(
        color = Color(0x66FFFFFF),
        topLeft = Offset(left - 1f, topY - 1f),
        size = Size(width + 2f, height + 2f),
        cornerRadius = corner,
        style = Stroke(width = 0.8f)
    )
}

private fun DrawScope.drawLasers(
    engine: SpaceGameEngine,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    for (bolt in engine.lasers) {
        if (bolt.x < minX || bolt.x > maxX || bolt.y < minY || bolt.y > maxY) {
            continue
        }
        val dx = cos(bolt.angleRad)
        val dy = sin(bolt.angleRad)
        val glowColor = Color(bolt.colorHex)

        if (bolt.fromPlayer) {
            // Iconic twin parallel cyan wing-cannon beams
            val len = 24f
            val perpX = -dy * 6f
            val perpY = dx * 6f
            for (side in -1..1 step 2) {
                val ox = perpX * side
                val oy = perpY * side
                val start = Offset(bolt.x + ox - dx * len, bolt.y + oy - dy * len)
                val end = Offset(bolt.x + ox, bolt.y + oy)
                drawLine(
                    color = glowColor.copy(alpha = 0.36f),
                    start = start,
                    end = end,
                    strokeWidth = 6.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = glowColor,
                    start = start,
                    end = end,
                    strokeWidth = 2.8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = start,
                    end = end,
                    strokeWidth = 1.1f,
                    cap = StrokeCap.Round
                )
            }
        } else {
            val isBossBolt = bolt.damage >= 40f
            val len = if (isBossBolt) 28f else 19f
            val start = Offset(bolt.x - dx * len, bolt.y - dy * len)
            val end = Offset(bolt.x, bolt.y)
            drawLine(
                color = glowColor.copy(alpha = 0.42f),
                start = start,
                end = end,
                strokeWidth = if (isBossBolt) 11f else 7.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = glowColor,
                start = start,
                end = end,
                strokeWidth = if (isBossBolt) 4.8f else 3.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = start,
                end = end,
                strokeWidth = if (isBossBolt) 1.8f else 1.2f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawRockets(
    engine: SpaceGameEngine,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    for (rocket in engine.rockets) {
        if (rocket.x < minX || rocket.x > maxX || rocket.y < minY || rocket.y > maxY) {
            continue
        }
        val center = Offset(rocket.x, rocket.y)
        val degrees = Math.toDegrees(rocket.angleRad.toDouble()).toFloat()
        rotate(degrees = degrees, pivot = center) {
            // Rocket exhaust glow
            drawCircle(
                color = Color(0x66FB923C),
                radius = 7.5f,
                center = Offset(rocket.x - 8f, rocket.y)
            )
            // Sleek missile fuselage
            val body = Path().apply {
                moveTo(rocket.x + 11f, rocket.y)
                lineTo(rocket.x + 2f, rocket.y - 3.2f)
                lineTo(rocket.x - 8f, rocket.y - 3.2f)
                lineTo(rocket.x - 11f, rocket.y - 5.5f)
                lineTo(rocket.x - 8f, rocket.y)
                lineTo(rocket.x - 11f, rocket.y + 5.5f)
                lineTo(rocket.x - 8f, rocket.y + 3.2f)
                lineTo(rocket.x + 2f, rocket.y + 3.2f)
                close()
            }
            drawPath(path = body, color = Color(0xFFF8FAFC))
            drawPath(path = body, color = Color(0xFFFB923C), style = Stroke(width = 1.4f))
            drawCircle(color = CrimsonDanger, radius = 2.2f, center = Offset(rocket.x + 7f, rocket.y))
        }
    }
}

private fun DrawScope.drawEffects(
    engine: SpaceGameEngine,
    popupPaint: Paint,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float
) {
    for (ring in engine.shockwaves) {
        if (ring.x + ring.maxRadius < minX || ring.x - ring.maxRadius > maxX ||
            ring.y + ring.maxRadius < minY || ring.y - ring.maxRadius > maxY
        ) {
            continue
        }
        val progress = 1f - (ring.life / ring.maxLife).coerceIn(0f, 1f)
        val radius = 10f + ring.maxRadius * progress
        val alpha = (1f - progress) * 0.8f
        val color = Color(ring.colorHex)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = Offset(ring.x, ring.y),
            style = Stroke(width = (4.2f * (1f - progress)).coerceAtLeast(0.8f))
        )
    }

    for (p in engine.particles) {
        if (p.x < minX || p.x > maxX || p.y < minY || p.y > maxY) {
            continue
        }
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        val color = Color(p.colorHex)
        val r = p.radius * (0.35f + 0.65f * alpha)
        // Soft outer glow + bright core
        drawCircle(
            color = color.copy(alpha = alpha * 0.35f),
            radius = r * 1.9f,
            center = Offset(p.x, p.y)
        )
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = r,
            center = Offset(p.x, p.y)
        )
    }

    val nativeCanvas = drawContext.canvas.nativeCanvas
    for (popup in engine.popups) {
        if (popup.x < minX || popup.x > maxX || popup.y < minY || popup.y > maxY) {
            continue
        }
        val ratio = (popup.life / popup.maxLife).coerceIn(0f, 1f)
        val alpha = (ratio * 255f).toInt().coerceIn(0, 255)
        val r = (popup.colorRgb shr 16) and 0xFF
        val g = (popup.colorRgb shr 8) and 0xFF
        val b = popup.colorRgb and 0xFF
        popupPaint.textSize = popup.textSize
        // Crisp dark shadow offset for instant combat readability
        popupPaint.color = android.graphics.Color.argb((alpha * 0.8f).toInt(), 4, 7, 17)
        nativeCanvas.drawText(popup.text, popup.x + 1.2f, popup.y + 1.2f, popupPaint)
        popupPaint.color = android.graphics.Color.argb(alpha, r, g, b)
        nativeCanvas.drawText(popup.text, popup.x, popup.y, popupPaint)
    }
}
