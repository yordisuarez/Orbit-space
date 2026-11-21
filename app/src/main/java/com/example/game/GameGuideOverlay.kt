package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite

/**
 * Modular game guide topic entry. Future updates can append new entries here
 * and add matching keys to GameStrings.
 */
data class GuideEntry(
    val id: String,
    val titleKey: String,
    val bodyKey: String
)

object GameGuideCatalog {
    val entries: List<GuideEntry> = listOf(
        GuideEntry(
            id = "capsule_death_loot",
            titleKey = "guide_capsule_title",
            bodyKey = "guide_capsule_body"
        ),
        GuideEntry(
            id = "professions_industrial",
            titleKey = "guide_professions_title",
            bodyKey = "guide_professions_body"
        ),
        GuideEntry(
            id = "resources_refining",
            titleKey = "guide_refining_title",
            bodyKey = "guide_refining_body"
        ),
        GuideEntry(
            id = "pilot_skills",
            titleKey = "guide_skills_title",
            bodyKey = "guide_skills_body"
        ),
        GuideEntry(
            id = "crafting_quality_balance",
            titleKey = "guide_quality_title",
            bodyKey = "guide_quality_body"
        ),
        GuideEntry(
            id = "player_economy",
            titleKey = "guide_economy_title",
            bodyKey = "guide_economy_body"
        )
    )
}

/**
 * Dark, minimalist, mobile-friendly scrollable Info / Game Guide modal
 * rendered from the modular [GameGuideCatalog.entries] list.
 */
@Composable
fun GameGuideOverlay(
    onClose: () -> Unit
) {
    val lang = LocalGameLanguage.current
    val strings = GameStrings.of(lang)
    var expandedIds by remember {
        mutableStateOf(setOf(GameGuideCatalog.entries.firstOrNull()?.id.orEmpty()))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD03060E))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("game_guide_overlay")
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
                        colors = listOf(Color(0xFF0A1424), SpaceDeepSurface)
                    )
                )
                .border(1.2.dp, EmeraldHull.copy(alpha = 0.62f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Header
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
                            .background(EmeraldHull.copy(alpha = 0.16f))
                            .border(1.dp, EmeraldHull.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = EmeraldHull,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.guideTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TacticalWhite,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${strings.guideSubtitle} • ${strings.guideTapToExpand}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onClose,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A253B),
                        contentColor = TacticalWhite
                    ),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("game_guide_close_button")
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

            // Expandable Modular Topics List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp)
            ) {
                itemsIndexed(
                    items = GameGuideCatalog.entries,
                    key = { _, entry -> entry.id }
                ) { index, entry ->
                    val isExpanded = entry.id in expandedIds
                    GuideTopicCard(
                        index = index + 1,
                        entry = entry,
                        accent = guideTopicAccent(index),
                        expanded = isExpanded,
                        lang = lang,
                        onToggle = {
                            expandedIds = if (isExpanded) {
                                expandedIds - entry.id
                            } else {
                                expandedIds + entry.id
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideTopicCard(
    index: Int,
    entry: GuideEntry,
    accent: Color,
    expanded: Boolean,
    lang: GameLanguage,
    onToggle: () -> Unit
) {
    val title = GameStrings.guideString(entry.titleKey, lang)
    val body = GameStrings.guideString(entry.bodyKey, lang)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "guide_chevron_${entry.id}"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (expanded) Color(0xE60B1528) else Color(0xCC070E1C))
            .border(
                width = if (expanded) 1.2.dp else 1.dp,
                color = accent.copy(alpha = if (expanded) 0.65f else 0.28f),
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("guide_card_${entry.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                ) {
                    Text(
                        text = "$index",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (expanded) accent else TacticalMuted,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x88050A14))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFD6E2F0)
                )
            }
        }
    }
}

private fun guideTopicAccent(index: Int): Color = when (index % 6) {
    0 -> CrimsonDanger
    1 -> Color(0xFF38BDF8)
    2 -> Color(0xFFFB923C)
    3 -> Color(0xFFC084FC)
    4 -> EmeraldHull
    else -> AmberCredits
}
