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
    val camDistFromOrigin = hypot(camX, camY)

    // 1. Deep void space background
    drawRect(color = SpaceVoid, size = size)

    // 2. World-space entities, culled starfield, concentric danger zones & combat effects
    withTransform({
        translate(left = viewportW * 0.5f, top = viewportH * 0.5f)
        scale(scaleX = worldScale, scaleY = worldScale, pivot = Offset.Zero)
        translate(left = -camX, top = -camY)
    }) {
        drawWorldStarfield(engine, minX, maxX, minY, maxY)
        drawNebulae(engine, minX, maxX, minY, maxY)
        drawSectorGridAndZoneRings(camDistFromOrigin, halfDiag)

        if (camDistFromOrigin <= GameConfig.SAFE_ZONE_RADIUS + halfDiag + 140f) {
            drawSafeZoneAndStation(engine.elapsedTime)
        }

        if (camDistFromOrigin + halfDiag >= GameConfig.MAP_RADIUS - 160f) {
            drawRadiationBorder(engine)
        }

        drawLootBoxes(engine, minX, maxX, minY, maxY)

        if (engine.player.isAlive) {
            drawTacticalOverlays(engine)
        }

        // Draw aliens (viewport culled) with distinct Canvas shapes per tier
        val lockedId = engine.lockedAlienId
        for (alien in engine.aliens) {
            if (alien.x < minX || alien.x > maxX || alien.y < minY || alien.y > maxY) {
                continue
            }
            if (alien.id == lockedId) {
                drawTargetLockReticle(alien, engine.elapsedTime)
            }
            drawAlienShip(alien)
            val barWidth = when (alien.tier) {
                AlienTier.DREADNOUGHT_BOSS -> 78f
                AlienTier.DESTROYER -> 56f
                AlienTier.HUNTER -> 46f
                AlienTier.SCOUT -> 38f
            }
            val barHeight = if (alien.tier == AlienTier.DREADNOUGHT_BOSS) 7f else 5.5f
            val tierFill = Color(alien.tier.primaryColorHex)
            val tierGlow = Color(alien.tier.accentColorHex)
            drawHealthBar(
                centerX = alien.x,
                topY = alien.y - alien.radius - 16f,
                width = barWidth,
                height = barHeight,
                ratio = (alien.hp / alien.maxHp).coerceIn(0f, 1f),
                fillColor = tierGlow,
                glowColor = tierFill
            )
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
        }

        drawEffects(engine, popupPaint, minX, maxX, minY, maxY)
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

    // Subtle intermediate navigation rings every 1000 units (culled by camera ring distance)
    val minVisibleR = (camDistFromOrigin - halfDiag - 60f).coerceAtLeast(0f)
    val maxVisibleR = camDistFromOrigin + halfDiag + 60f

    var r = 1000f
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
        r += 1000f
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

    if (player.shieldHitFlash > 0.05f) {
        drawCircle(
            color = CyanThruster.copy(alpha = 0.28f * player.shieldHitFlash),
            radius = hullR + 10f,
            center = center
        )
        drawCircle(
            color = CyanLaser.copy(alpha = 0.8f * player.shieldHitFlash),
            radius = hullR + 10f,
            center = center,
            style = Stroke(width = 2.2f)
        )
    }

    if (player.inSafeZone && (player.hp < player.maxHp || player.shield < player.maxShield)) {
        val repairPulse = 0.45f + 0.55f * sin(time * 8f)
        drawCircle(
            color = EmeraldHullBright.copy(alpha = 0.35f * repairPulse),
            radius = hullR + 8f,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    val hit = player.hitFlash > 0.2f
    val hullFill = if (hit) Color(0xFFFF99AA) else Color(ship.primaryColorHex)
    val trimColor = if (hit) Color.White else Color(ship.accentColorHex)

    rotate(degrees = degrees, pivot = center) {
        val flicker = 0.8f + 0.35f * sin(player.thrusterPulse)

        when (ship) {
            ShipClass.INTERCEPTOR -> {
                // Sleek high-speed Cyan razor-dart starfighter
                if (player.isMoving) {
                    val flameLen = 19f * flicker
                    drawThrusterFlame(player.x - 13f, player.y, flameLen, 5.5f, CyanThruster)
                }

                val hullPath = Path().apply {
                    moveTo(player.x + 24f, player.y)
                    lineTo(player.x + 5f, player.y - 8f)
                    lineTo(player.x - 11f, player.y - 19f)
                    lineTo(player.x - 16f, player.y - 11f)
                    lineTo(player.x - 10f, player.y - 4f)
                    lineTo(player.x - 15f, player.y)
                    lineTo(player.x - 10f, player.y + 4f)
                    lineTo(player.x - 16f, player.y + 11f)
                    lineTo(player.x - 11f, player.y + 19f)
                    lineTo(player.x + 5f, player.y + 8f)
                    close()
                }
                drawPath(path = hullPath, color = hullFill)
                drawPath(path = hullPath, color = trimColor, style = Stroke(width = 1.9f))

                val spinePath = Path().apply {
                    moveTo(player.x + 20f, player.y)
                    lineTo(player.x - 7f, player.y - 5.5f)
                    lineTo(player.x - 12f, player.y)
                    lineTo(player.x - 7f, player.y + 5.5f)
                    close()
                }
                drawPath(path = spinePath, color = Color(0xFF64748B))

                val cockpit = Path().apply {
                    moveTo(player.x + 12f, player.y)
                    lineTo(player.x + 1f, player.y - 3.6f)
                    lineTo(player.x - 4f, player.y)
                    lineTo(player.x + 1f, player.y + 3.6f)
                    close()
                }
                drawPath(path = cockpit, color = CyanLaser)

                drawLine(
                    color = CyanThruster,
                    start = Offset(player.x - 3f, player.y - 13f),
                    end = Offset(player.x + 11f, player.y - 13f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = CyanThruster,
                    start = Offset(player.x - 3f, player.y + 13f),
                    end = Offset(player.x + 11f, player.y + 13f),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round
                )
            }

            ShipClass.BATTLECRUISER -> {
                // Balanced Cobalt & Gold twin-prow cruiser with dual engine pods
                if (player.isMoving) {
                    val flameLen = 17f * flicker
                    drawThrusterFlame(player.x - 18f, player.y - 8f, flameLen, 4.8f, AmberCredits)
                    drawThrusterFlame(player.x - 18f, player.y + 8f, flameLen, 4.8f, AmberCredits)
                }

                val cruiserHull = Path().apply {
                    moveTo(player.x + 27f, player.y - 5f)
                    lineTo(player.x + 14f, player.y - 11f)
                    lineTo(player.x + 4f, player.y - 23f)
                    lineTo(player.x - 14f, player.y - 21f)
                    lineTo(player.x - 20f, player.y - 12f)
                    lineTo(player.x - 17f, player.y - 5f)
                    lineTo(player.x - 20f, player.y)
                    lineTo(player.x - 17f, player.y + 5f)
                    lineTo(player.x - 20f, player.y + 12f)
                    lineTo(player.x - 14f, player.y + 21f)
                    lineTo(player.x + 4f, player.y + 23f)
                    lineTo(player.x + 14f, player.y + 11f)
                    lineTo(player.x + 27f, player.y + 5f)
                    lineTo(player.x + 19f, player.y)
                    close()
                }
                drawPath(path = cruiserHull, color = hullFill)
                drawPath(path = cruiserHull, color = trimColor, style = Stroke(width = 2.2f))

                val deckPlate = Path().apply {
                    moveTo(player.x + 18f, player.y)
                    lineTo(player.x + 2f, player.y - 11f)
                    lineTo(player.x - 13f, player.y - 8f)
                    lineTo(player.x - 13f, player.y + 8f)
                    lineTo(player.x + 2f, player.y + 11f)
                    close()
                }
                drawPath(path = deckPlate, color = Color(0xFF1E3A8A))
                drawPath(path = deckPlate, color = Color(0xFF60A5FA), style = Stroke(width = 1.2f))

                // Glowing amber-gold bridge & twin forward laser rails
                drawCircle(color = AmberCredits, radius = 4.6f, center = Offset(player.x + 4f, player.y))
                drawCircle(color = Color.White, radius = 2.0f, center = Offset(player.x + 4f, player.y))
                drawLine(
                    color = trimColor,
                    start = Offset(player.x - 4f, player.y - 17f),
                    end = Offset(player.x + 15f, player.y - 17f),
                    strokeWidth = 2.6f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = trimColor,
                    start = Offset(player.x - 4f, player.y + 17f),
                    end = Offset(player.x + 15f, player.y + 17f),
                    strokeWidth = 2.6f,
                    cap = StrokeCap.Round
                )
            }

            ShipClass.DREADNOUGHT -> {
                // Heavy Emerald/Titanium Tank Fortress with quad sponsons & triple engines
                if (player.isMoving) {
                    val flameLen = 16f * flicker
                    drawThrusterFlame(player.x - 24f, player.y - 12f, flameLen * 0.85f, 4.4f, EmeraldHullBright)
                    drawThrusterFlame(player.x - 26f, player.y, flameLen * 1.1f, 5.8f, CyanThruster)
                    drawThrusterFlame(player.x - 24f, player.y + 12f, flameLen * 0.85f, 4.4f, EmeraldHullBright)
                }

                val dreadHull = Path().apply {
                    moveTo(player.x + 31f, player.y - 8f)
                    lineTo(player.x + 18f, player.y - 15f)
                    lineTo(player.x + 12f, player.y - 28f)
                    lineTo(player.x - 14f, player.y - 29f)
                    lineTo(player.x - 25f, player.y - 18f)
                    lineTo(player.x - 23f, player.y - 8f)
                    lineTo(player.x - 27f, player.y)
                    lineTo(player.x - 23f, player.y + 8f)
                    lineTo(player.x - 25f, player.y + 18f)
                    lineTo(player.x - 14f, player.y + 29f)
                    lineTo(player.x + 12f, player.y + 28f)
                    lineTo(player.x + 18f, player.y + 15f)
                    lineTo(player.x + 31f, player.y + 8f)
                    lineTo(player.x + 22f, player.y)
                    close()
                }
                drawPath(path = dreadHull, color = hullFill)
                drawPath(path = dreadHull, color = trimColor, style = Stroke(width = 2.6f))

                // Reinforced octagonal citadel core
                val citadel = Path().apply {
                    moveTo(player.x + 19f, player.y - 6f)
                    lineTo(player.x + 8f, player.y - 16f)
                    lineTo(player.x - 12f, player.y - 16f)
                    lineTo(player.x - 18f, player.y - 6f)
                    lineTo(player.x - 18f, player.y + 6f)
                    lineTo(player.x - 12f, player.y + 16f)
                    lineTo(player.x + 8f, player.y + 16f)
                    lineTo(player.x + 19f, player.y + 6f)
                    close()
                }
                drawPath(path = citadel, color = Color(0xFF0F2F28))
                drawPath(path = citadel, color = trimColor.copy(alpha = 0.85f), style = Stroke(width = 1.6f))

                // Quad heavy broadside cannon rails
                for (wy in floatArrayOf(-23f, -15f, 15f, 23f)) {
                    drawLine(
                        color = trimColor,
                        start = Offset(player.x - 6f, player.y + wy),
                        end = Offset(player.x + 18f, player.y + wy),
                        strokeWidth = 2.6f,
                        cap = StrokeCap.Round
                    )
                }

                // Pulsing Emerald Reactor Core
                drawCircle(color = trimColor.copy(alpha = 0.35f), radius = 10f, center = Offset(player.x + 2f, player.y))
                drawCircle(color = trimColor, radius = 5.5f, center = Offset(player.x + 2f, player.y))
                drawCircle(color = Color.White, radius = 2.4f, center = Offset(player.x + 2f, player.y))
            }
        }
    }
}

private fun DrawScope.drawOrbitingVants(engine: SpaceGameEngine) {
    val vants = engine.ownedVants
    if (vants.isEmpty()) return

    val player = engine.player
    val orbitRadius = player.shipClass.hullRadius + 24f
    val count = vants.size
    val baseAngle = engine.elapsedTime * 1.45f
    val pulse = 0.75f + 0.25f * sin(engine.elapsedTime * 6.5f)

    // Subtle holographic orbital track ring
    drawCircle(
        color = CyanLaser.copy(alpha = 0.12f),
        radius = orbitRadius,
        center = Offset(player.x, player.y),
        style = Stroke(
            width = 1.0f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), engine.elapsedTime * 14f)
        )
    )

    for (i in 0 until count) {
        val vant = vants[i]
        val theta = baseAngle + (i * (2f * PI.toFloat() / count))
        val vx = player.x + cos(theta) * orbitRadius
        val vy = player.y + sin(theta) * orbitRadius
        val center = Offset(vx, vy)
        val facingDeg = Math.toDegrees((theta + PI * 0.5).toDouble()).toFloat()

        rotate(degrees = facingDeg, pivot = center) {
            when (vant.tier) {
                VantTier.FLAX -> {
                    // Sleek silver/cyan mini-drone
                    drawCircle(
                        color = CyanLaser.copy(alpha = 0.22f * pulse),
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
                    // Golden wing accent stripes
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

private fun DrawScope.drawThrusterFlame(
    baseX: Float,
    baseY: Float,
    length: Float,
    halfWidth: Float,
    outerColor: Color
) {
    val outer = Path().apply {
        moveTo(baseX, baseY - halfWidth)
        lineTo(baseX - length, baseY)
        lineTo(baseX, baseY + halfWidth)
        close()
    }
    drawPath(path = outer, color = outerColor.copy(alpha = 0.82f))
    val inner = Path().apply {
        moveTo(baseX + 1f, baseY - halfWidth * 0.52f)
        lineTo(baseX - length * 0.6f, baseY)
        lineTo(baseX + 1f, baseY + halfWidth * 0.52f)
        close()
    }
    drawPath(path = inner, color = Color.White.copy(alpha = 0.92f))
}

private fun DrawScope.drawAlienShip(alien: AlienEntity) {
    val center = Offset(alien.x, alien.y)
    val degrees = Math.toDegrees(alien.angleRad.toDouble()).toFloat()
    val corePulse = 0.72f + 0.28f * sin(alien.pulsePhase)
    val primary = Color(alien.tier.primaryColorHex)
    val accent = Color(alien.tier.accentColorHex)
    val hit = alien.hitFlash > 0.22f
    val trim = if (hit) Color.White else primary

    rotate(degrees = degrees, pivot = center) {
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
