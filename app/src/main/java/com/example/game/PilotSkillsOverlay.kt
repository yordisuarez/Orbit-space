package com.example.game

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.SpaceDeepSurface
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite

/**
 * Dark, minimalist, mobile-friendly scrollable panel displaying all 9 Pilot Skills (Lv 1–99),
 * XP progress bars, and live active profession bonuses.
 */
@Composable
fun PilotSkillsOverlay(
    pilotName: String?,
    pilotCompany: PilotCompany?,
    skills: Map<SkillType, SkillProgress>,
    isAdminMode: Boolean = false,
    onAdminCycleSkill: (SkillType) -> Unit = {},
    onAdminSetSkill: (SkillType, Int) -> Unit = { _, _ -> },
    onAdminSetAllSkills: (Int) -> Unit = {},
    onClose: () -> Unit
) {
    val lang = LocalGameLanguage.current
    val strings = GameStrings.of(lang)
    val totalSkillLevels = SkillType.entries.sumOf { skills[it]?.level ?: PILOT_SKILL_MIN_LEVEL }
    val maxTotalSkillLevels = SkillType.entries.size * PILOT_SKILL_MAX_LEVEL
    val headerAccent = pilotCompany?.let { Color(it.primaryColorHex) } ?: CyanLaser

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD03060E))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("pilot_skills_overlay")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A1222), SpaceDeepSurface)
                    )
                )
                .border(1.2.dp, headerAccent.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Minimalist Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(headerAccent.copy(alpha = 0.16f))
                            .border(1.dp, headerAccent.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = headerAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        val titleLine = if (!pilotName.isNullOrBlank()) {
                            "${strings.skillsTitle} • ${pilotName.uppercase()}"
                        } else {
                            strings.skillsTitle
                        }
                        Text(
                            text = titleLine,
                            style = MaterialTheme.typography.titleMedium,
                            color = TacticalWhite,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.skillsSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC060C18))
                            .border(1.dp, headerAccent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${strings.skillsTotalLevelPrefix} $totalSkillLevels/$maxTotalSkillLevels",
                            style = MaterialTheme.typography.labelSmall,
                            color = headerAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A253B),
                            contentColor = TacticalWhite
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("pilot_skills_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.actionClose,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.actionClose,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Admin Mode Instant Skill Level Testing Strip (only visible when isAdminMode == true)
            if (isAdminMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0x2E34D399))
                        .border(1.dp, AmberCredits.copy(alpha = 0.72f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("admin_skills_quick_bar")
                ) {
                    Text(
                        text = if (lang == GameLanguage.ES) "ADMIN • TODOS:" else "ADMIN • SET ALL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "LV 1", 33 to "LV 33", 66 to "LV 66", 99 to "MAX 99").forEach { (lv, label) ->
                            val isMax = lv == 99
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMax) AmberCredits.copy(alpha = 0.25f) else Color(0xFF102038))
                                    .border(
                                        1.dp,
                                        if (isMax) AmberCredits else EmeraldHull.copy(alpha = 0.75f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onAdminSetAllSkills(lv) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("admin_all_skills_lv_$lv")
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    color = if (isMax) Color(0xFFFEF08A) else Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // Scrollable 9-Skill List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(SkillType.entries, key = { it.name }) { skill ->
                    val progress = skills[skill] ?: SkillProgress()
                    SkillProgressCard(
                        skill = skill,
                        progress = progress,
                        isAdminMode = isAdminMode,
                        onAdminCycle = { onAdminCycleSkill(skill) },
                        onAdminMax = { onAdminSetSkill(skill, PILOT_SKILL_MAX_LEVEL) },
                        lang = lang,
                        strings = strings
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillProgressCard(
    skill: SkillType,
    progress: SkillProgress,
    isAdminMode: Boolean,
    onAdminCycle: () -> Unit,
    onAdminMax: () -> Unit,
    lang: GameLanguage,
    strings: GameText
) {
    val accent = skillAccentColor(skill)
    val icon = skillIcon(skill)
    val name = GameStrings.skillName(skill, lang)
    val activeBonus = GameStrings.skillActiveBonus(skill, progress.level, lang)
    val nextXp = progress.xpToNextLevel
    val xpLabel = if (progress.isMaxLevel || nextXp <= 0L) {
        strings.skillsMaxLabel
    } else {
        "${"%,d".format(progress.xp)} / ${"%,d".format(nextXp)} XP"
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xCC070E1C))
            .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .testTag("skill_row_${skill.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accent.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        color = TacticalWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeBonus,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAdminMode) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3334D399))
                            .border(1.dp, EmeraldHull.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .clickable(onClick = onAdminCycle)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("admin_skill_cycle_${skill.name.lowercase()}")
                    ) {
                        Text(
                            text = "+LV",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color(0xFFA7F3D0),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33F59E0B))
                            .border(1.dp, AmberCredits.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                            .clickable(onClick = onAdminMax)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("admin_skill_max_${skill.name.lowercase()}")
                    ) {
                        Text(
                            text = "MAX 99",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color(0xFFFEF08A),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${strings.hudLevelPrefix} ${progress.level} / $PILOT_SKILL_MAX_LEVEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (progress.isMaxLevel) AmberCredits else TacticalWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = xpLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = TacticalMuted
                    )
                }
            }
        }

        // Compact XP Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF040812))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.progressFraction.coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accent.copy(alpha = 0.72f), accent)
                        )
                    )
            )
        }
    }
}

private fun skillAccentColor(skill: SkillType): Color = when (skill) {
    SkillType.MINING -> Color(0xFF38BDF8)
    SkillType.SALVAGING -> Color(0xFFC084FC)
    SkillType.REFINING -> Color(0xFFFB923C)
    SkillType.NAVAL_ENGINEERING -> EmeraldHull
    SkillType.GEAR_MANUFACTURING -> Color(0xFF2DD4BF)
    SkillType.SYNTHESIS -> Color(0xFFE879F9)
    SkillType.WEAPONRY -> CrimsonDanger
    SkillType.SHIELDS -> CyanLaser
    SkillType.PILOTING -> AmberCredits
}

private fun skillIcon(skill: SkillType): ImageVector = when (skill) {
    SkillType.MINING -> Icons.Default.Hardware
    SkillType.SALVAGING -> Icons.Default.Handyman
    SkillType.REFINING -> Icons.Default.LocalFireDepartment
    SkillType.NAVAL_ENGINEERING -> Icons.Default.Build
    SkillType.GEAR_MANUFACTURING -> Icons.Default.PrecisionManufacturing
    SkillType.SYNTHESIS -> Icons.Default.AutoAwesome
    SkillType.WEAPONRY -> Icons.Default.GpsFixed
    SkillType.SHIELDS -> Icons.Default.Shield
    SkillType.PILOTING -> Icons.Default.Explore
}
