package com.example.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite
import kotlinx.coroutines.delay

/**
 * Interactive Station Overlay for:
 * 1. Central Refining Station (enforces Clockwise City Refining Loop: C1->C2->C3->C1 & Yellow Crystal->0-0)
 * 2. Strictly Isolated Local Market / Black Market (independent stock & prices per sectorId)
 * 3. Capital City Crafting Station (crafts consumables, chips & gear from refined resources)
 */
@Composable
fun StationOverlay(
    hud: HudSnapshot,
    lastCraftOutcome: CraftOutcome?,
    onSwitchStationTab: (StationKind) -> Unit,
    onRefineResource: (ResourceType, Int) -> Unit,
    onBuyMarketResource: (ResourceType, Boolean) -> Unit,
    onSellMarketResource: (ResourceType, Boolean, Boolean) -> Unit,
    onBuyMarketEquipment: (EquipmentItemId) -> Unit,
    onBuyBasicVendorItem: (BasicVendorItemId) -> Unit,
    onCraftStationRecipe: (StationCraftRecipeId) -> Unit,
    onDismissCraftOutcome: () -> Unit,
    onOpenSkills: () -> Unit,
    onClose: () -> Unit
) {
    val kind = hud.activeStationOverlay ?: return
    val lang = LocalGameLanguage.current
    val strings = GameStrings.of(lang)
    val sector = GalaxyMapGraph.sectorOf(hud.currentSectorId)
    val accent = when {
        kind == StationKind.REFINERY -> Color(0xFFFB923C)
        kind == StationKind.CRAFTING -> Color(0xFF34D399)
        sector.isNucleus -> AmberCredits
        else -> sector.nodeColor
    }
    val headerIcon = when (kind) {
        StationKind.REFINERY -> Icons.Default.Build
        StationKind.CRAFTING -> Icons.Default.Handyman
        StationKind.LOCAL_MARKET -> Icons.Default.Storefront
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9040711))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("station_overlay")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceDeepSurface)
                .border(1.5.dp, accent.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = GameStrings.stationHeaderTitle(kind, sector, lang),
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = GameStrings.stationHeaderSubtitle(kind, sector, lang),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC060C18))
                            .border(1.dp, AmberCredits.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = AmberCredits,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${"%,d".format(hud.credits)} CR",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberCredits,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC0A182B))
                            .border(1.dp, CyanLaser.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenSkills)
                            .padding(horizontal = 9.dp)
                            .testTag("station_skills_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = strings.hudSkills,
                            tint = CyanLaser,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = strings.hudSkills,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanLaser,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Button(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonDanger,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("station_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.stationUndock,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Capital City Station Tab Bar (Refinery • Crafting • Market)
            if (sector.zoneType == GalaxyZoneType.CAPITAL_CITY) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("station_service_tabs")
                ) {
                    StationServiceTabButton(
                        label = strings.stationTabRefinery,
                        icon = Icons.Default.Build,
                        selected = kind == StationKind.REFINERY,
                        accent = Color(0xFFFB923C),
                        tag = "station_tab_refinery",
                        modifier = Modifier.weight(1f),
                        onClick = { onSwitchStationTab(StationKind.REFINERY) }
                    )
                    StationServiceTabButton(
                        label = strings.stationTabCrafting,
                        icon = Icons.Default.Handyman,
                        selected = kind == StationKind.CRAFTING,
                        accent = Color(0xFF34D399),
                        tag = "station_tab_crafting",
                        modifier = Modifier.weight(1f),
                        onClick = { onSwitchStationTab(StationKind.CRAFTING) }
                    )
                    StationServiceTabButton(
                        label = strings.stationTabMarket,
                        icon = Icons.Default.Storefront,
                        selected = kind == StationKind.LOCAL_MARKET,
                        accent = sector.nodeColor,
                        tag = "station_tab_market",
                        modifier = Modifier.weight(1f),
                        onClick = { onSwitchStationTab(StationKind.LOCAL_MARKET) }
                    )
                }
            }

            when (kind) {
                StationKind.REFINERY -> {
                    val refiningProgress = hud.pilotSkills[SkillType.REFINING] ?: SkillProgress()
                    RefineryContent(
                        sectorId = hud.currentSectorId,
                        credits = hud.credits,
                        refiningLevel = refiningProgress.level,
                        rawCargo = hud.rawCargo,
                        refinedCargo = hud.refinedCargo,
                        isAdminMode = hud.isAdminMode,
                        lang = lang,
                        strings = strings,
                        onRefine = onRefineResource,
                        modifier = Modifier.weight(1f)
                    )
                }

                StationKind.CRAFTING -> {
                    CraftingContent(
                        hud = hud,
                        lastCraftOutcome = lastCraftOutcome,
                        lang = lang,
                        strings = strings,
                        onCraft = onCraftStationRecipe,
                        onDismissOutcome = onDismissCraftOutcome,
                        modifier = Modifier.weight(1f)
                    )
                }

                StationKind.LOCAL_MARKET -> {
                    val market = hud.activeLocalMarket
                    if (market != null) {
                        LocalMarketContent(
                            market = market,
                            credits = hud.credits,
                            rawCargo = hud.rawCargo,
                            refinedCargo = hud.refinedCargo,
                            ownedItemCounts = hud.equipment.ownedItemCounts,
                            basicVendorItemCounts = hud.equipment.basicVendorItemCounts,
                            isAdminMode = hud.isAdminMode,
                            accent = accent,
                            lang = lang,
                            strings = strings,
                            onBuyResource = onBuyMarketResource,
                            onSellResource = onSellMarketResource,
                            onBuyEquipment = onBuyMarketEquipment,
                            onBuyBasicVendorItem = onBuyBasicVendorItem,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StationServiceTabButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color(0x99060C18))
            .border(
                width = if (selected) 1.3.dp else 1.dp,
                color = if (selected) accent else SpaceHudBorder,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) accent else TacticalMuted,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) accent else TacticalMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CraftingContent(
    hud: HudSnapshot,
    lastCraftOutcome: CraftOutcome?,
    lang: GameLanguage,
    strings: GameText,
    onCraft: (StationCraftRecipeId) -> Unit,
    onDismissOutcome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDiscipline by remember { mutableStateOf(CraftingDiscipline.NAVAL_ENGINEERING) }

    // Auto-dismiss craft result banner after 3.8s
    LaunchedEffect(lastCraftOutcome?.id) {
        if (lastCraftOutcome != null) {
            delay(3800L)
            onDismissOutcome()
        }
    }

    val activeSkill = selectedDiscipline.skill
    val activeSkillLevel = (hud.pilotSkills[activeSkill] ?: SkillProgress()).level
    val activeAccent = Color(selectedDiscipline.accentHex)
    val qualityOdds = remember(activeSkillLevel) { craftQualityOdds(activeSkillLevel) }
    val recipes = remember(selectedDiscipline) {
        StationCraftRecipeId.forDiscipline(selectedDiscipline)
    }

    val perkText = when (selectedDiscipline) {
        CraftingDiscipline.NAVAL_ENGINEERING,
        CraftingDiscipline.GEAR_MANUFACTURING -> {
            val pct = Math.round(craftingMaterialReturnRate(activeSkillLevel) * 100f)
            "${strings.craftMaterialReturnPrefix}: $pct%"
        }
        CraftingDiscipline.SYNTHESIS -> {
            val pct = Math.round(synthesisDoubleOutputChance(activeSkillLevel) * 100f)
            "${strings.craftDoubleChancePrefix}: $pct%"
        }
    }

    val ownedCountLabel = when (selectedDiscipline) {
        CraftingDiscipline.NAVAL_ENGINEERING ->
            "${strings.craftHangarShipsOwnedPrefix}: ${hud.craftedHangarShips.size}"
        CraftingDiscipline.GEAR_MANUFACTURING,
        CraftingDiscipline.SYNTHESIS -> {
            val craftedCount = hud.craftedInventoryItems.sumOf { it.quantity }
            "${strings.craftInventoryItemsOwnedPrefix}: $craftedCount"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 3 Sub-Tabs matching the 3 Crafting Skills
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("crafting_discipline_tabs")
        ) {
            CraftingDisciplineSubTab(
                label = strings.craftSubTabNaval,
                icon = Icons.Default.RocketLaunch,
                selected = selectedDiscipline == CraftingDiscipline.NAVAL_ENGINEERING,
                accent = Color(CraftingDiscipline.NAVAL_ENGINEERING.accentHex),
                tag = "craft_subtab_naval",
                modifier = Modifier.weight(1f),
                onClick = { selectedDiscipline = CraftingDiscipline.NAVAL_ENGINEERING }
            )
            CraftingDisciplineSubTab(
                label = strings.craftSubTabGear,
                icon = Icons.Default.PrecisionManufacturing,
                selected = selectedDiscipline == CraftingDiscipline.GEAR_MANUFACTURING,
                accent = Color(CraftingDiscipline.GEAR_MANUFACTURING.accentHex),
                tag = "craft_subtab_gear",
                modifier = Modifier.weight(1f),
                onClick = { selectedDiscipline = CraftingDiscipline.GEAR_MANUFACTURING }
            )
            CraftingDisciplineSubTab(
                label = strings.craftSubTabSynthesis,
                icon = Icons.Default.AutoAwesome,
                selected = selectedDiscipline == CraftingDiscipline.SYNTHESIS,
                accent = Color(CraftingDiscipline.SYNTHESIS.accentHex),
                tag = "craft_subtab_synthesis",
                modifier = Modifier.weight(1f),
                onClick = { selectedDiscipline = CraftingDiscipline.SYNTHESIS }
            )
        }

        // Skill Level, Perk & Quality Odds Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xB3071522))
                .border(1.dp, activeAccent.copy(alpha = 0.52f), RoundedCornerShape(10.dp))
                .padding(horizontal = 11.dp, vertical = 7.dp)
                .testTag("crafting_skill_banner")
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${GameStrings.craftingDisciplineName(selectedDiscipline, lang)} • ${strings.hudLevelPrefix} $activeSkillLevel/$PILOT_SKILL_MAX_LEVEL • $perkText",
                    style = MaterialTheme.typography.labelMedium,
                    color = activeAccent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${strings.craftQualityOddsPrefix}: +0% (${qualityOdds.standardPct}%) • +5% (${qualityOdds.reinforcedPct}%) • +10% (${qualityOdds.excellentPct}%) • +15% (${qualityOdds.masterworkPct}%)",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TacticalMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = ownedCountLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TacticalWhite,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        // Visual Craft Success Toast / Banner with Color-Coded Quality Badge
        AnimatedVisibility(
            visible = lastCraftOutcome != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }
        ) {
            if (lastCraftOutcome != null) {
                CraftOutcomeBanner(
                    outcome = lastCraftOutcome,
                    lang = lang,
                    strings = strings,
                    onDismiss = onDismissOutcome
                )
            }
        }

        // Recipe List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(bottom = 4.dp)
        ) {
            items(recipes, key = { it.name }) { recipe ->
                val canCraft = hud.isAdminMode || recipe.ingredients.all { cost ->
                    val owned = if (cost.isRefined) {
                        hud.refinedCargo[cost.resource] ?: 0
                    } else {
                        hud.rawCargo[cost.resource] ?: 0
                    }
                    owned >= cost.amount
                }
                val itemAccent = Color(recipe.accentHex)

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xCC0A1324))
                        .border(
                            width = 1.dp,
                            color = itemAccent.copy(alpha = if (canCraft) 0.72f else 0.25f),
                            shape = RoundedCornerShape(11.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 9.dp)
                        .testTag("craft_card_${recipe.name.lowercase()}")
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
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(itemAccent)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = GameStrings.stationCraftRecipeName(recipe, lang),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TacticalWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${GameStrings.stationCraftRecipeSummary(recipe, lang)} • +${recipe.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.5.sp,
                                    color = TacticalMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onCraft(recipe) },
                            enabled = canCraft,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeAccent,
                                contentColor = SpaceVoid,
                                disabledContainerColor = Color(0xFF1A2438),
                                disabledContentColor = TacticalMuted
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("craft_${recipe.name.lowercase()}")
                        ) {
                            Text(
                                text = strings.craftingButton,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Required vs Owned Materials Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (cost in recipe.ingredients) {
                            val owned = if (cost.isRefined) {
                                hud.refinedCargo[cost.resource] ?: 0
                            } else {
                                hud.rawCargo[cost.resource] ?: 0
                            }
                            val hasEnough = hud.isAdminMode || owned >= cost.amount
                            val matLabel = GameStrings.craftMaterialLabel(cost, lang)
                            val chipColor = if (hasEnough) EmeraldHull else Color(0xFFF87171)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(chipColor.copy(alpha = 0.12f))
                                    .border(
                                        1.dp,
                                        chipColor.copy(alpha = if (hasEnough) 0.45f else 0.35f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(cost.resource.colorHex))
                                )
                                Text(
                                    text = "$matLabel: $owned/${cost.amount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasEnough) Color(0xFFD1FAE5) else Color(0xFFFECACA),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CraftingDisciplineSubTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) accent else Color(0xCC070E1C))
            .border(
                width = 1.dp,
                color = if (selected) accent else accent.copy(alpha = 0.35f),
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) SpaceVoid else accent,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) SpaceVoid else TacticalWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CraftOutcomeBanner(
    outcome: CraftOutcome,
    lang: GameLanguage,
    strings: GameText,
    onDismiss: () -> Unit
) {
    val qualityColor = craftQualityBadgeColor(outcome.quality)
    val qualityLabel = GameStrings.craftQualityName(outcome.quality, lang).uppercase()
    val bonusPctInt = Math.round(outcome.quality.bonusPct * 100f)
    val itemName = GameStrings.stationCraftRecipeName(outcome.recipeId, lang)
    val destText = if (outcome.recipeId.outputKind == CraftOutputKind.INDUSTRIAL_SHIP) {
        strings.craftBannerAddedHangar
    } else {
        strings.craftBannerAddedInventory
    }

    val extraNotes = buildList {
        add(outcome.statSummary)
        add(destText)
        if (outcome.refundedUnits > 0) {
            add("+${outcome.refundedUnits} ${strings.craftBannerRefundedSuffix}")
        }
        add("+${outcome.xpAwarded} XP")
    }.joinToString(" • ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xEB0B1528))
            .border(1.4.dp, qualityColor.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 11.dp, vertical = 8.dp)
            .testTag("craft_outcome_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = qualityColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${outcome.quantityCrafted}× $itemName",
                        style = MaterialTheme.typography.labelLarge,
                        color = TacticalWhite,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Color-Coded Quality Badge: Standard (White/Gray), Reinforced (Green), Excellent (Purple), Masterwork (Gold)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(qualityColor.copy(alpha = 0.20f))
                            .border(1.dp, qualityColor, RoundedCornerShape(5.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("craft_quality_badge_${outcome.quality.name.lowercase()}")
                    ) {
                        Text(
                            text = "$qualityLabel +$bonusPctInt%",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = qualityColor,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (outcome.doubledOutput) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0x33E879F9))
                                .border(1.dp, Color(0xFFE879F9), RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = strings.craftBannerDoubleBonus,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF0ABFC),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                Text(
                    text = extraNotes,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = TacticalMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Distinct color-coding for CraftQuality badges:
 * - STANDARD: White/Gray
 * - REINFORCED: Green
 * - EXCELLENT: Blue/Purple
 * - MASTERWORK: Gold
 */
private fun craftQualityBadgeColor(quality: CraftQuality): Color = when (quality) {
    CraftQuality.STANDARD -> Color(0xFFE2E8F0)
    CraftQuality.REINFORCED -> Color(0xFF4ADE80)
    CraftQuality.EXCELLENT -> Color(0xFFA855F7)
    CraftQuality.MASTERWORK -> Color(0xFFFACC15)
}

@Composable
private fun RefineryContent(
    sectorId: String,
    credits: Int,
    refiningLevel: Int,
    rawCargo: Map<ResourceType, Int>,
    refinedCargo: Map<ResourceType, Int>,
    isAdminMode: Boolean,
    lang: GameLanguage,
    strings: GameText,
    onRefine: (ResourceType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ownedRawEntries: List<Pair<ResourceType, Int>> = if (isAdminMode) {
        ResourceType.entries
            .map { it to (rawCargo[it] ?: 0) }
            .sortedWith(
                compareByDescending<Pair<ResourceType, Int>> { it.first.canRefineIn(sectorId) }
                    .thenBy { it.first.isDirectUse }
                    .thenBy { it.first.ordinal }
            )
    } else {
        rawCargo.entries
            .filter { (_, qty) -> qty > 0 }
            .map { it.key to it.value }
            .sortedWith(
                compareByDescending<Pair<ResourceType, Int>> { it.first.canRefineIn(sectorId) }
                    .thenBy { it.first.isDirectUse }
                    .thenBy { it.first.ordinal }
            )
    }

    val acceptedSummary = GameStrings.refineryAcceptedSummary(sectorId, lang)
    val feeDiscountPct = Math.round(refiningFeeDiscountPct(refiningLevel) * 100f)
    val bonusChancePct = Math.round(refiningBonusOutputChance(refiningLevel) * 100f)
    val acceptedTypesHere = remember(sectorId) {
        ResourceType.entries.filter { it.canRefineIn(sectorId) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Refinery Loop & REFINING Skill Bonus Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xB319110A))
                .border(1.dp, Color(0x99FB923C), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = acceptedSummary,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFFDBA74),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${GameStrings.skillName(SkillType.REFINING, lang)} ${strings.hudLevelPrefix} $refiningLevel/$PILOT_SKILL_MAX_LEVEL • ${strings.refineryFeeDiscountPrefix} -$feeDiscountPct% • ${strings.refineryBonusChancePrefix} +$bonusChancePct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanLaser,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${ResourceType.RAW_REQUIRED_PER_REFINE} → 1",
                style = MaterialTheme.typography.labelMedium,
                color = AmberCredits,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (ownedRawEntries.isEmpty()) {
            // Minimalist dark empty state with accepted-here preview
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x99060C18))
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(12.dp))
                    .padding(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = Color(0x99FB923C),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.refineryEmptyCargo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TacticalMuted
                )
                if (acceptedTypesHere.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val acceptedNames = acceptedTypesHere.joinToString(" • ") {
                        GameStrings.resourceName(it, lang)
                    }
                    Text(
                        text = acceptedNames,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDBA74),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(ownedRawEntries, key = { it.first.name }) { (res, rawQty) ->
                    val refQty = refinedCargo[res] ?: 0
                    val canRefineHere = isAdminMode || res.canRefineIn(sectorId)
                    val feePerUnit = if (isAdminMode) 0 else effectiveRefiningFeePerUnit(res, refiningLevel)
                    val maxByRaw = rawQty / ResourceType.RAW_REQUIRED_PER_REFINE
                    val maxByCredits = if (feePerUnit > 0) credits / feePerUnit else maxByRaw
                    val maxRefinable = if (isAdminMode) maxOf(10, maxByRaw) else minOf(maxByRaw, maxByCredits)
                    val resColor = Color(res.colorHex)
                    val rawLabel = GameStrings.resourceName(res, lang)
                    val refinedLabel = GameStrings.refinedNameFor(res, lang)
                    val catLabel = GameStrings.resourceCategoryName(res.category, lang).uppercase()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (canRefineHere) Color(0xCC0A1324) else Color(0x99070D19))
                            .border(
                                1.dp,
                                if (canRefineHere) {
                                    resColor.copy(alpha = if (maxRefinable >= 1) 0.72f else 0.30f)
                                } else {
                                    SpaceHudBorder
                                },
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("refinery_item_${res.name.lowercase()}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(if (canRefineHere) resColor else resColor.copy(alpha = 0.45f))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                val titleText = if (res.isDirectUse) {
                                    "$rawLabel [$catLabel]"
                                } else {
                                    "$rawLabel → $refinedLabel"
                                }
                                Text(
                                    text = titleText,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (canRefineHere) TacticalWhite else TacticalWhite.copy(alpha = 0.72f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (canRefineHere) {
                                    Text(
                                        text = "${strings.refineryRawOwned}: $rawQty • ${strings.refineryRefinedOwned}: $refQty • ${strings.refineryFeePrefix}: $feePerUnit CR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (maxByCredits < 1 && maxByRaw >= 1) CrimsonDanger else TacticalMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    val destLabel = GameStrings.refinesInLabel(res, lang)
                                    Text(
                                        text = "${strings.refineryRawOwned}: $rawQty • $destLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberCredits,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (canRefineHere) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Button(
                                    onClick = { onRefine(res, 1) },
                                    enabled = maxRefinable >= 1,
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFB923C),
                                        contentColor = SpaceVoid,
                                        disabledContainerColor = Color(0xFF1E293B),
                                        disabledContentColor = TacticalMuted
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("refine_x1_${res.name.lowercase()}")
                                ) {
                                    Text(
                                        text = "x1",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onRefine(res, 10) },
                                    enabled = maxRefinable >= 10,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFFDBA74),
                                        disabledContentColor = TacticalMuted.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("refine_x10_${res.name.lowercase()}")
                                ) {
                                    Text(
                                        text = "x10",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onRefine(res, maxRefinable) },
                                    enabled = maxRefinable >= 1,
                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = AmberCredits,
                                        disabledContentColor = TacticalMuted.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("refine_max_${res.name.lowercase()}")
                                ) {
                                    Text(
                                        text = strings.refineryMax,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        } else {
                            // Wrong city / non-refinable: disabled refine button + clear localized destination badge
                            val destBadge = GameStrings.refinesInLabel(res, lang)
                            Button(
                                onClick = {},
                                enabled = false,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = Color(0xFF131C2E),
                                    disabledContentColor = AmberCredits.copy(alpha = 0.85f)
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("refine_locked_${res.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = destBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalMarketContent(
    market: LocalMarketSnapshot,
    credits: Int,
    rawCargo: Map<ResourceType, Int>,
    refinedCargo: Map<ResourceType, Int>,
    ownedItemCounts: Map<EquipmentItemId, Int>,
    basicVendorItemCounts: Map<BasicVendorItemId, Int>,
    isAdminMode: Boolean,
    accent: Color,
    lang: GameLanguage,
    strings: GameText,
    onBuyResource: (ResourceType, Boolean) -> Unit,
    onSellResource: (ResourceType, Boolean, Boolean) -> Unit,
    onBuyEquipment: (EquipmentItemId) -> Unit,
    onBuyBasicVendorItem: (BasicVendorItemId) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = SELL CARGO, 1 = BUY RESOURCES, 2 = BASIC VENDOR (C1/C2/C3) or BLACK MARKET GEAR (0-0)
    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketTabPill(
                label = strings.marketTabSell,
                selected = tab == 0,
                accent = accent,
                tag = "market_tab_sell",
                modifier = Modifier.weight(1f)
            ) { tab = 0 }
            MarketTabPill(
                label = strings.marketTabBuy,
                selected = tab == 1,
                accent = accent,
                tag = "market_tab_buy",
                modifier = Modifier.weight(1f)
            ) { tab = 1 }
            MarketTabPill(
                label = if (market.isBlackMarket) strings.marketTabBlackGear else strings.marketTabBasicVendor,
                selected = tab == 2,
                accent = accent,
                tag = if (market.isBlackMarket) "market_tab_black_gear" else "market_tab_basic_vendor",
                modifier = Modifier.weight(1f)
            ) { tab = 2 }
        }

        when (tab) {
            0 -> {
                val ownedListings = market.resourceListings.filter { listing ->
                    val qty = if (listing.isRefined) refinedCargo[listing.resourceType] ?: 0 else rawCargo[listing.resourceType] ?: 0
                    qty > 0
                }
                if (ownedListings.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x99060C18))
                            .border(1.dp, SpaceHudBorder, RoundedCornerShape(12.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = strings.marketEmptyCargo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TacticalMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ownedListings, key = { "sell_${it.resourceType.name}_${it.isRefined}" }) { listing ->
                            val qty = if (listing.isRefined) {
                                refinedCargo[listing.resourceType] ?: 0
                            } else {
                                rawCargo[listing.resourceType] ?: 0
                            }
                            val res = listing.resourceType
                            val itemName = if (listing.isRefined) {
                                GameStrings.refinedNameFor(res, lang)
                            } else {
                                "${GameStrings.resourceName(res, lang)} (${strings.marketRawTag})"
                            }
                            val color = Color(res.colorHex)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xCC0A1324))
                                    .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "T${res.tier} $itemName • ${strings.marketCargoPrefix}: ×$qty",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TacticalWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${market.sectorId} ${strings.marketBidPrefix}: ${listing.sellPrice} CR • ${strings.marketLocalStockPrefix}: ${listing.localStock}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberCredits
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onSellResource(res, listing.isRefined, false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldHull,
                                            contentColor = SpaceVoid
                                        ),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(strings.marketSellOne, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                                    }
                                    if (qty > 1) {
                                        OutlinedButton(
                                            onClick = { onSellResource(res, listing.isRefined, true) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldHull),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("${strings.refineryAll} (${listing.sellPrice * qty})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(market.resourceListings, key = { "buy_${it.resourceType.name}_${it.isRefined}" }) { listing ->
                        val res = listing.resourceType
                        val itemName = if (listing.isRefined) {
                            GameStrings.refinedNameFor(res, lang)
                        } else {
                            "${GameStrings.resourceName(res, lang)} (${strings.marketRawTag})"
                        }
                        val canBuy = isAdminMode || (listing.localStock > 0 && credits >= listing.buyPrice)
                        val color = Color(res.colorHex)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xCC0A1324))
                                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "T${res.tier} $itemName",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TacticalWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                val refineNote = if (!listing.isRefined) {
                                    " • ${GameStrings.refinesInLabel(res, lang)}"
                                } else {
                                    " • ${strings.marketRefinedTag}"
                                }
                                Text(
                                    text = "${market.sectorId} ${strings.marketLocalStockPrefix}: ${listing.localStock}$refineNote",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalMuted
                                )
                            }
                            Button(
                                onClick = { onBuyResource(res, listing.isRefined) },
                                enabled = canBuy,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent,
                                    contentColor = SpaceVoid,
                                    disabledContainerColor = Color(0xFF1E293B),
                                    disabledContentColor = TacticalMuted
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = when {
                                        isAdminMode -> "${strings.marketBuyPrefix} 0 CR"
                                        listing.localStock <= 0 -> strings.marketOut
                                        else -> "${strings.marketBuyPrefix} ${listing.buyPrice} CR"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                if (!market.isBlackMarket) {
                    // Capital City (C1, C2, C3) NPC Basic Vendor: strictly Tier 1 starter gear & repair kit
                    Column(
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("basic_vendor_panel")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xB3071526))
                                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = strings.basicVendorBanner,
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accent.copy(alpha = 0.18f))
                                    .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = strings.basicVendorUnlimitedTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(BasicVendorItemId.entries, key = { "basic_${it.name}" }) { item ->
                                val itemColor = Color(item.accentHex)
                                val ownedCount = item.equipmentId?.let { ownedItemCounts[it] ?: 0 }
                                    ?: (basicVendorItemCounts[item] ?: 0)
                                val canBuy = isAdminMode || credits >= item.priceCredits

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xCC0A1324))
                                        .border(1.dp, itemColor.copy(alpha = 0.52f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                        .testTag("basic_vendor_card_${item.name.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(itemColor)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = GameStrings.basicVendorItemName(item, lang),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = TacticalWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Text(
                                                    text = "${strings.basicVendorOwnedPrefix}: $ownedCount",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    color = CyanLaser,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = GameStrings.basicVendorItemDesc(item, lang),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TacticalMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { onBuyBasicVendorItem(item) },
                                        enabled = canBuy,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = itemColor,
                                            contentColor = SpaceVoid,
                                            disabledContainerColor = Color(0xFF1E293B),
                                            disabledContentColor = TacticalMuted
                                        ),
                                        modifier = Modifier
                                            .height(38.dp)
                                            .testTag("basic_vendor_buy_${item.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = if (isAdminMode) {
                                                "${strings.marketBuyPrefix} 0 CR"
                                            } else {
                                                "${strings.marketBuyPrefix} ${item.priceCredits} CR"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(market.equipmentListings, key = { "eq_${it.itemId.name}" }) { gear ->
                            val item = gear.itemId
                            val canBuy = isAdminMode || (gear.localStock > 0 && credits >= gear.buyPrice)
                            val itemColor = Color(item.accentHex)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xCC0A1324))
                                    .border(1.dp, itemColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.displayName} (${item.statSummary()})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = itemColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${market.sectorId} ${strings.marketLocalStockPrefix}: ${gear.localStock} ${strings.marketExclusiveStockSuffix}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TacticalMuted
                                    )
                                }
                                Button(
                                    onClick = { onBuyEquipment(item) },
                                    enabled = canBuy,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accent,
                                        contentColor = SpaceVoid,
                                        disabledContainerColor = Color(0xFF1E293B),
                                        disabledContentColor = TacticalMuted
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isAdminMode -> "${strings.marketBuyPrefix} 0 CR"
                                            gear.localStock <= 0 -> strings.marketSoldOut
                                            else -> "${strings.marketBuyPrefix} ${gear.buyPrice} CR"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketTabPill(
    label: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.24f) else Color(0xAA060C18))
            .border(1.2.dp, if (selected) accent else SpaceHudBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accent else TacticalMuted,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
