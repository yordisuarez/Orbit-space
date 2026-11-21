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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.SpaceDeepSurface
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite

/**
 * Dark, minimalist, mobile-friendly vertical Command Menu overlay with 5 destinations:
 * 1. Skills  2. Hangar  3. Inventory  4. Info / Game Guide  5. Settings
 */
@Composable
fun UnifiedMenuOverlay(
    hud: HudSnapshot,
    currentLanguage: GameLanguage,
    onOpenSkills: () -> Unit,
    onOpenHangar: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val strings = GameStrings.of(currentLanguage)
    val totalSkillLevels = SkillType.entries.sumOf { hud.pilotSkills[it]?.level ?: PILOT_SKILL_MIN_LEVEL }
    val rawTotal = hud.rawCargo.values.sum()
    val refinedTotal = hud.refinedCargo.values.sum()
    val usedCargo = rawTotal + refinedTotal
    val cargoCap = hud.equipment.totalCargoCapacity.coerceAtLeast(1)
    val hangarHasAlert = hud.mission.isReadyToClaim || hud.equipment.hasAnyReadyFusion
    val hangarSubtitle = if (hud.inSafeZone) strings.menuHangarDockedHint else strings.menuHangarSpaceHint
    val hangarBadge = if (!hud.inSafeZone) strings.menuHangarReadOnlyTag else null

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD803060E))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("unified_menu_overlay")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .widthIn(min = 300.dp, max = 440.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A1324), SpaceDeepSurface)
                    )
                )
                .border(1.2.dp, CyanLaser.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(CyanLaser.copy(alpha = 0.14f))
                            .border(1.dp, CyanLaser.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            tint = CyanLaser,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.menuTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TacticalWhite,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.menuSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        .testTag("menu_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.actionClose,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.actionClose,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 5 Vertical Menu Destinations
            MenuDestinationRow(
                icon = Icons.Default.MilitaryTech,
                title = strings.menuSkills,
                subtitle = strings.menuSkillsHint,
                accent = Color(0xFFC084FC),
                badgeText = "${strings.hudLevelPrefix} $totalSkillLevels",
                showAlertDot = false,
                tag = "menu_item_skills",
                onClick = onOpenSkills
            )

            MenuDestinationRow(
                icon = Icons.Default.RocketLaunch,
                title = strings.menuHangar,
                subtitle = hangarSubtitle,
                accent = CyanLaser,
                badgeText = hangarBadge,
                badgeColor = AmberCredits,
                showAlertDot = hangarHasAlert,
                tag = "menu_item_hangar",
                onClick = onOpenHangar
            )

            MenuDestinationRow(
                icon = Icons.Default.Inventory2,
                title = strings.menuInventory,
                subtitle = strings.menuInventoryHint,
                accent = AmberCredits,
                badgeText = "$usedCargo/$cargoCap",
                showAlertDot = false,
                tag = "menu_item_inventory",
                onClick = onOpenInventory
            )

            MenuDestinationRow(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = strings.menuInfo,
                subtitle = strings.menuInfoHint,
                accent = EmeraldHull,
                badgeText = "${GameGuideCatalog.entries.size} ${strings.guideTopicsSuffix}",
                showAlertDot = false,
                tag = "menu_item_info",
                onClick = onOpenGuide
            )

            MenuDestinationRow(
                icon = Icons.Default.Settings,
                title = strings.menuSettings,
                subtitle = strings.menuSettingsHint,
                accent = Color(0xFF94A3B8),
                badgeText = currentLanguage.code.uppercase(),
                showAlertDot = false,
                tag = "menu_item_settings",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun MenuDestinationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    badgeText: String?,
    showAlertDot: Boolean,
    tag: String,
    badgeColor: Color = accent,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xCC070F1E))
            .border(1.dp, accent.copy(alpha = 0.36f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(9.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showAlertDot) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldHull)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = TacticalMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.14f))
                    .border(1.dp, badgeColor.copy(alpha = 0.42f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TacticalMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Dark, minimalist, mobile-friendly Cargo Hold & Stored Modules overlay.
 */
@Composable
fun InventoryOverlay(
    hud: HudSnapshot,
    onClose: () -> Unit
) {
    val lang = LocalGameLanguage.current
    val strings = GameStrings.of(lang)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Cargo Hold, 1 = Stored Modules

    val rawEntries = remember(hud.rawCargo) {
        ResourceType.entries.mapNotNull { res ->
            val qty = hud.rawCargo[res] ?: 0
            if (qty > 0) res to qty else null
        }
    }
    val refinedEntries = remember(hud.refinedCargo) {
        ResourceType.entries.mapNotNull { res ->
            val qty = hud.refinedCargo[res] ?: 0
            if (qty > 0) res to qty else null
        }
    }
    val groupedModules = remember(hud.equipment.inventory) {
        hud.equipment.inventory
            .groupBy { it.type }
            .entries
            .sortedBy { it.key.ordinal }
            .map { it.key to it.value.size }
    }

    val usedCargo = rawEntries.sumOf { it.second } + refinedEntries.sumOf { it.second }
    val cargoCap = hud.equipment.totalCargoCapacity.coerceAtLeast(1)
    val cargoRatio = (usedCargo.toFloat() / cargoCap.toFloat()).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD03060E))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("inventory_overlay")
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
                .border(1.2.dp, AmberCredits.copy(alpha = 0.62f), RoundedCornerShape(16.dp))
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
                            .background(AmberCredits.copy(alpha = 0.16f))
                            .border(1.dp, AmberCredits.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = AmberCredits,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.inventoryTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = TacticalWhite,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${hud.activeShipClass.displayName} • ${strings.inventorySubtitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        .testTag("inventory_close_button")
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

            // Cargo Capacity Strip + Tab Switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF060C18))
                        .border(1.dp, SpaceHudBorder, RoundedCornerShape(9.dp))
                        .padding(3.dp)
                ) {
                    InventoryTabChip(
                        label = "${strings.inventoryTabCargo} ($usedCargo)",
                        selected = selectedTab == 0,
                        tag = "inventory_tab_cargo",
                        onClick = { selectedTab = 0 }
                    )
                    InventoryTabChip(
                        label = "${strings.inventoryTabModules} (${hud.equipment.inventory.size})",
                        selected = selectedTab == 1,
                        tag = "inventory_tab_modules",
                        onClick = { selectedTab = 1 }
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${strings.inventoryCapacityPrefix}: $usedCargo / $cargoCap",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberCredits,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${"%,d".format(hud.credits)} CR",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF040812))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(cargoRatio)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (cargoRatio >= 0.9f) Color(0xFFFF5252) else AmberCredits
                                )
                        )
                    }
                }
            }

            // Content Area
            if (selectedTab == 0) {
                if (rawEntries.isEmpty() && refinedEntries.isEmpty() && hud.overchargedAmmo <= 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x99060C18))
                            .border(1.dp, SpaceHudBorder, RoundedCornerShape(12.dp))
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = TacticalMuted.copy(alpha = 0.65f),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = strings.inventoryEmptyCargo,
                                style = MaterialTheme.typography.bodySmall,
                                color = TacticalMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        if (rawEntries.isNotEmpty()) {
                            item(key = "hdr_raw") {
                                InventorySectionHeader(
                                    label = strings.inventoryRawSection,
                                    accent = Color(0xFF38BDF8)
                                )
                            }
                            items(rawEntries, key = { "raw_${it.first.name}" }) { (res, qty) ->
                                CargoResourceRow(
                                    name = GameStrings.resourceName(res, lang),
                                    tagText = GameStrings.resourceCategoryName(res.category, lang),
                                    detailText = GameStrings.refinesInLabel(res, lang),
                                    qty = qty,
                                    accent = Color(res.colorHex)
                                )
                            }
                        }

                        if (refinedEntries.isNotEmpty()) {
                            item(key = "hdr_refined") {
                                InventorySectionHeader(
                                    label = strings.inventoryRefinedSection,
                                    accent = EmeraldHull
                                )
                            }
                            items(refinedEntries, key = { "ref_${it.first.name}" }) { (res, qty) ->
                                CargoResourceRow(
                                    name = GameStrings.refinedNameFor(res, lang),
                                    tagText = strings.marketRefinedTag,
                                    detailText = "T${res.tier}",
                                    qty = qty,
                                    accent = EmeraldHull
                                )
                            }
                        }

                        if (hud.overchargedAmmo > 0) {
                            item(key = "hdr_consumables") {
                                InventorySectionHeader(
                                    label = strings.inventoryConsumablesSection,
                                    accent = AmberCredits
                                )
                            }
                            item(key = "ammo_plasma") {
                                CargoResourceRow(
                                    name = strings.craftingAmmoStock,
                                    tagText = "+35% DMG",
                                    detailText = strings.hudOverchargePrefix,
                                    qty = hud.overchargedAmmo,
                                    accent = AmberCredits
                                )
                            }
                        }
                    }
                }
            } else {
                if (groupedModules.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x99060C18))
                            .border(1.dp, SpaceHudBorder, RoundedCornerShape(12.dp))
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = TacticalMuted.copy(alpha = 0.65f),
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = strings.inventoryEmptyModules,
                                style = MaterialTheme.typography.bodySmall,
                                color = TacticalMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        contentPadding = PaddingValues(bottom = 4.dp)
                    ) {
                        items(groupedModules, key = { "mod_${it.first.name}" }) { (itemId, count) ->
                            CargoResourceRow(
                                name = itemId.displayName,
                                tagText = itemId.shortCode,
                                detailText = itemId.statSummary(),
                                qty = count,
                                accent = Color(itemId.accentHex)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryTabChip(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) AmberCredits else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) SpaceVoid else TacticalMuted,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun InventorySectionHeader(label: String, accent: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        color = accent,
        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
    )
}

@Composable
private fun CargoResourceRow(
    name: String,
    tagText: String,
    detailText: String,
    qty: Int,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xCC070E1C))
            .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
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
                    .background(accent)
            )
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
                    text = "$tagText • $detailText",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.5.sp,
                    color = TacticalMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(7.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Text(
                text = "×$qty",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
        }
    }
}
