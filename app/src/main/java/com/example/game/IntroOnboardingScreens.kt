package com.example.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.CyanThruster
import com.example.ui.theme.SpaceDeepSurface
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class IntroStar(
    val x: Float,
    val y: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float,
    val tint: Color
)

@Composable
fun AnimatedSpaceBackdrop(
    accentColor: Color = CyanLaser,
    modifier: Modifier = Modifier
) {
    val stars = remember {
        val rng = Random(4207)
        val palette = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFBAE6FD),
            Color(0xFFC7D2FE),
            Color(0xFFFDE68A),
            Color(0xFF7DD3FC)
        )
        List(140) { idx ->
            val layer = idx % 3
            IntroStar(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                speed = when (layer) {
                    0 -> 0.012f
                    1 -> 0.028f
                    else -> 0.056f
                },
                radius = when (layer) {
                    0 -> 1.1f
                    1 -> 1.8f
                    else -> 2.7f
                },
                alpha = when (layer) {
                    0 -> 0.35f + rng.nextFloat() * 0.25f
                    1 -> 0.55f + rng.nextFloat() * 0.3f
                    else -> 0.78f + rng.nextFloat() * 0.22f
                },
                tint = palette[rng.nextInt(palette.size)]
            )
        }
    }

    var animTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    animTime += dt
                }
                lastNanos = frameNanos
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030612))
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        // Deep cosmic radial gradient + accent nebula glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.14f),
                    Color(0xFF0A142D).copy(alpha = 0.72f),
                    Color(0xFF030612)
                ),
                center = Offset(cx, cy * 0.85f),
                radius = maxOf(w, h) * 0.68f
            ),
            size = size
        )

        // Subtle secondary nebula pocket
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF6366F1).copy(alpha = 0.13f),
                    Color.Transparent
                ),
                center = Offset(w * 0.18f, h * 0.22f),
                radius = h * 0.65f
            ),
            radius = h * 0.65f,
            center = Offset(w * 0.18f, h * 0.22f)
        )

        // Drifting parallax starfield (diagonal drift left-to-right & gently downward)
        for (star in stars) {
            val sx = ((star.x - animTime * star.speed) % 1f + 1f) % 1f * w
            val sy = ((star.y + animTime * star.speed * 0.24f) % 1f + 1f) % 1f * h
            val twinkle = 0.78f + 0.22f * sin(animTime * 2.8f + star.x * 31f)
            drawCircle(
                color = star.tint.copy(alpha = (star.alpha * twinkle).coerceIn(0.12f, 1f)),
                radius = star.radius,
                center = Offset(sx, sy)
            )
            if (star.radius > 2.2f) {
                drawLine(
                    color = star.tint.copy(alpha = 0.18f * twinkle),
                    start = Offset(sx, sy),
                    end = Offset(sx + star.radius * 5.5f, sy - star.radius * 1.3f),
                    strokeWidth = 1.1f
                )
            }
        }

        // Slow rotating orbital radar rings behind UI
        val ringR = minOf(w, h) * 0.42f
        drawCircle(
            color = accentColor.copy(alpha = 0.08f),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.4f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.05f),
            radius = ringR * 1.35f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.0f)
        )
        rotate(degrees = animTime * 8f, pivot = Offset(cx, cy)) {
            for (i in 0 until 4) {
                val a = (i * 90.0) * PI / 180.0
                val p1 = Offset(cx + cos(a).toFloat() * (ringR - 8f), cy + sin(a).toFloat() * (ringR - 8f))
                val p2 = Offset(cx + cos(a).toFloat() * (ringR + 10f), cy + sin(a).toFloat() * (ringR + 10f))
                drawLine(
                    color = accentColor.copy(alpha = 0.26f),
                    start = p1,
                    end = p2,
                    strokeWidth = 2f
                )
            }
        }
    }
}

@Composable
fun CompanyEmblemCanvas(
    company: PilotCompany,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val primary = Color(company.primaryColorHex)
    val darkBg = Color(company.darkBgColorHex)

    Canvas(
        modifier = modifier
            .size(size)
            .testTag("company_emblem_${company.id}")
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = minOf(w, h) * 0.46f

        // Ambient radial glow behind emblem
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.34f), Color.Transparent),
                center = Offset(cx, cy),
                radius = r * 1.12f
            ),
            radius = r * 1.12f,
            center = Offset(cx, cy)
        )

        when (company) {
            PilotCompany.SOLARIS -> {
                // Beveled diamond frame + radiant solar starburst
                val diamond = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r, cy)
                    close()
                }
                drawPath(diamond, color = darkBg)
                drawPath(
                    diamond,
                    color = primary,
                    style = Stroke(width = r * 0.11f, join = StrokeJoin.Miter)
                )

                // 8-point blazing solar star
                val star = Path().apply {
                    for (i in 0 until 16) {
                        val rad = if (i % 2 == 0) r * 0.66f else r * 0.28f
                        val a = (i * 22.5 - 90.0) * PI / 180.0
                        val px = cx + cos(a).toFloat() * rad
                        val py = cy + sin(a).toFloat() * rad
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(
                    star,
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD166), primary),
                        center = Offset(cx, cy),
                        radius = r * 0.7f
                    )
                )
                drawCircle(color = Color.White, radius = r * 0.14f, center = Offset(cx, cy))
            }

            PilotCompany.AETHER -> {
                // Precision hexagonal Aegis shield + concentric orbital ring + diamond core
                val hex = Path().apply {
                    for (i in 0 until 6) {
                        val a = (i * 60.0 - 90.0) * PI / 180.0
                        val px = cx + cos(a).toFloat() * r
                        val py = cy + sin(a).toFloat() * r
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(hex, color = darkBg)
                drawPath(
                    hex,
                    color = primary,
                    style = Stroke(width = r * 0.11f, join = StrokeJoin.Miter)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.72f),
                    radius = r * 0.58f,
                    center = Offset(cx, cy),
                    style = Stroke(width = r * 0.08f)
                )
                // Crosshair ticks
                for (i in 0 until 4) {
                    val a = (i * 90.0) * PI / 180.0
                    drawLine(
                        color = primary,
                        start = Offset(cx + cos(a).toFloat() * r * 0.34f, cy + sin(a).toFloat() * r * 0.34f),
                        end = Offset(cx + cos(a).toFloat() * r * 0.76f, cy + sin(a).toFloat() * r * 0.76f),
                        strokeWidth = r * 0.08f,
                        cap = StrokeCap.Round
                    )
                }
                val core = Path().apply {
                    moveTo(cx, cy - r * 0.28f)
                    lineTo(cx + r * 0.28f, cy)
                    lineTo(cx, cy + r * 0.28f)
                    lineTo(cx - r * 0.28f, cy)
                    close()
                }
                drawPath(core, color = Color.White)
            }

            PilotCompany.VERDANT -> {
                // Crest shield + twin ascending wings + central compass spire
                val crest = Path().apply {
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.88f, cy - r * 0.42f)
                    lineTo(cx + r * 0.68f, cy + r * 0.56f)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.68f, cy + r * 0.56f)
                    lineTo(cx - r * 0.88f, cy - r * 0.42f)
                    close()
                }
                drawPath(crest, color = darkBg)
                drawPath(
                    crest,
                    color = primary,
                    style = Stroke(width = r * 0.11f, join = StrokeJoin.Miter)
                )

                // Twin chevron wings
                val wings = Path().apply {
                    moveTo(cx - r * 0.58f, cy + r * 0.06f)
                    lineTo(cx, cy - r * 0.54f)
                    lineTo(cx + r * 0.58f, cy + r * 0.06f)
                    lineTo(cx + r * 0.34f, cy + r * 0.22f)
                    lineTo(cx, cy - r * 0.16f)
                    lineTo(cx - r * 0.34f, cy + r * 0.22f)
                    close()
                }
                drawPath(
                    wings,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF86EFAC), primary),
                        startY = cy - r * 0.6f,
                        endY = cy + r * 0.3f
                    )
                )

                // Lower trade compass crystal
                val crystal = Path().apply {
                    moveTo(cx, cy + r * 0.06f)
                    lineTo(cx + r * 0.24f, cy + r * 0.36f)
                    lineTo(cx, cy + r * 0.66f)
                    lineTo(cx - r * 0.24f, cy + r * 0.36f)
                    close()
                }
                drawPath(crystal, color = Color(0xFFFDE047))
            }
        }
    }
}

@Composable
fun IntroTitleScreen(
    savedPilotName: String?,
    savedCompany: PilotCompany?,
    onContinueSaved: () -> Unit,
    onStartNew: () -> Unit
) {
    val hasSavedPilot = !savedPilotName.isNullOrBlank() && savedCompany != null
    val accent = if (hasSavedPilot) Color(savedCompany!!.primaryColorHex) else CyanLaser

    val infiniteTransition = rememberInfiniteTransition(label = "intro_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cta_glow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedSpaceBackdrop(accentColor = accent)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top tactical classification badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0x990A1428))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = stringResource(R.string.intro_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = TacticalMuted,
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glowing Sci-Fi Title Logo
            Box(contentAlignment = Alignment.Center) {
                // Soft glow backing text
                Text(
                    text = stringResource(R.string.intro_title),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.5.sp,
                    color = accent.copy(alpha = 0.25f * glowAlpha),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.scale(1.03f)
                )
                Text(
                    text = stringResource(R.string.intro_title),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.5.sp,
                    style = MaterialTheme.typography.displaySmall.copy(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFE0F2FE),
                                accent
                            )
                        )
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("intro_title_logo")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sci-fi horizontal divider bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, accent.copy(alpha = 0.8f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accent.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Pulsing Primary Action Button
            val buttonLabel = if (hasSavedPilot) {
                "${stringResource(R.string.intro_action_continue_prefix)} [${savedCompany!!.tag}] ${savedPilotName!!}"
            } else {
                stringResource(R.string.intro_action_start)
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 2.dp,
                        color = accent.copy(alpha = glowAlpha),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.28f),
                                accent.copy(alpha = 0.14f),
                                accent.copy(alpha = 0.28f)
                            )
                        )
                    )
                    .clickable(onClick = if (hasSavedPilot) onContinueSaved else onStartNew)
                    .padding(horizontal = 28.dp, vertical = 14.dp)
                    .testTag(if (hasSavedPilot) "intro_continue_button" else "intro_start_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasSavedPilot) {
                        CompanyEmblemCanvas(company = savedCompany!!, size = 26.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = buttonLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CompanySelectionScreen(
    selectedCompany: PilotCompany?,
    onSelectCompany: (PilotCompany) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val accent = selectedCompany?.let { Color(it.primaryColorHex) } ?: CyanLaser

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedSpaceBackdrop(accentColor = accent)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Back button + Step Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalWhite),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("onboarding_company_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_action_back),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_step1_tag),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step1_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TacticalWhite,
                        letterSpacing = 1.2.sp
                    )
                }

                // Visual spacer to balance back button
                Spacer(modifier = Modifier.width(88.dp))
            }

            // 3 Rival Company Cards (side by side for landscape)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp)
            ) {
                for (company in PilotCompany.entries) {
                    CompanyCard(
                        company = company,
                        selected = selectedCompany == company,
                        onClick = { onSelectCompany(company) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            // Bottom CTA bar
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onNext,
                    enabled = selectedCompany != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = SpaceVoid,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = TacticalMuted
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("onboarding_company_next_button")
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_action_next),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyCard(
    company: PilotCompany,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = Color(company.primaryColorHex)
    val darkBg = Color(company.darkBgColorHex)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.24f), darkBg.copy(alpha = 0.94f))
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xD90B1324), Color(0xE6060C18))
                    )
                }
            )
            .border(
                width = if (selected) 2.2.dp else 1.2.dp,
                color = if (selected) primary else primary.copy(alpha = 0.35f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag("company_card_${company.id}")
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            CompanyEmblemCanvas(company = company, size = 56.dp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = company.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TacticalWhite,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(primary.copy(alpha = 0.2f))
                        .border(1.dp, primary.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "[${company.tag}]",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = company.lore,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) TacticalWhite.copy(alpha = 0.92f) else TacticalMuted,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PilotNameRegistrationScreen(
    company: PilotCompany,
    pilotNameDraft: String,
    onPilotNameChange: (String) -> Unit,
    onLaunchShip: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val primary = Color(company.primaryColorHex)
    val trimmed = pilotNameDraft.trim()
    val isValid = trimmed.length in 3..14
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedSpaceBackdrop(accentColor = primary)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalWhite),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("onboarding_name_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.onboarding_action_back),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.onboarding_step2_tag),
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step2_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TacticalWhite,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.width(88.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Registration Card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SpaceDeepSurface.copy(alpha = 0.92f))
                    .border(1.5.dp, primary.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                // Left: Selected Company emblem + live callsign preview
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.width(170.dp)
                ) {
                    CompanyEmblemCanvas(company = company, size = 64.dp)
                    Text(
                        text = company.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TacticalWhite,
                        letterSpacing = 1.2.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(company.darkBgColorHex))
                            .border(1.dp, primary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "[${company.tag}] ${trimmed.ifEmpty { "PILOT" }}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right: Pilot Name TextField + Launch Ship Button
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = pilotNameDraft,
                        onValueChange = { raw ->
                            val sanitized = raw.replace("\n", "").replace("\r", "")
                            if (sanitized.length <= 14) {
                                onPilotNameChange(sanitized)
                            }
                        },
                        singleLine = true,
                        label = {
                            Text(
                                text = stringResource(R.string.onboarding_pilot_name_label),
                                letterSpacing = 1.1.sp
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.onboarding_pilot_name_placeholder),
                                color = TacticalMuted.copy(alpha = 0.7f)
                            )
                        },
                        supportingText = {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.onboarding_pilot_name_hint),
                                    color = if (trimmed.isNotEmpty() && !isValid) Color(0xFFFCA5A5) else TacticalMuted
                                )
                                Text(
                                    text = "${trimmed.length}/14",
                                    color = if (isValid) primary else TacticalMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isValid) onLaunchShip()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary,
                            unfocusedBorderColor = SpaceHudBorder,
                            focusedLabelColor = primary,
                            unfocusedLabelColor = TacticalMuted,
                            focusedTextColor = TacticalWhite,
                            unfocusedTextColor = TacticalWhite,
                            cursorColor = primary,
                            focusedContainerColor = Color(0xFF060C18),
                            unfocusedContainerColor = Color(0xFF060C18)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_pilot_name_input")
                    )

                    Button(
                        onClick = onLaunchShip,
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = SpaceVoid,
                            disabledContainerColor = Color(0xFF1E293B),
                            disabledContentColor = TacticalMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("onboarding_launch_ship_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.onboarding_action_launch),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.4.sp
                        )
                    }
                }
            }
        }
    }
}
