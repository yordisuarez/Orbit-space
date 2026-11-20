package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AlienCoreOrange
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.CyanThruster
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.SpaceDeepSurface
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite

@Composable
fun HangarShopOverlay(
    hud: HudSnapshot,
    onSwitchConfig: (Int) -> Unit,
    onBuyOrSelectShipClass: (ShipClass) -> Unit,
    onBuyOrSelectFormation: (DroneFormation) -> Unit,
    onBuyEquipment: (EquipmentItemId) -> Unit,
    onBuyFlaxVant: () -> Unit,
    onFuseItems: (EquipmentItemId) -> Unit,
    onFuseFlaxVants: () -> Unit,
    onEquipFromInventory: (Int) -> Unit,
    onEquipItemToVantSlot: (Int, Int) -> Unit,
    onEquipFromInventoryToVant: (Int) -> Unit,
    onUnequipVantSlot: (Int, Int) -> Unit,
    onUnequipLaserSlot: (Int) -> Unit,
    onUnequipShieldSlot: (Int) -> Unit,
    onUnequipSectorChip: () -> Unit,
    onClaimMissionReward: () -> Unit,
    onResetSave: () -> Unit,
    onDebugAddCredits: () -> Unit,
    onDebugAddLevel: () -> Unit,
    onClose: () -> Unit
) {
    // 0 = EQUIPMENT, 1 = VANTS, 2 = SHOP, 3 = FUSE, 4 = MISSIONS
    var selectedTab by remember { mutableIntStateOf(0) }
    val equipment = hud.equipment

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD4040711))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("hangar_shop_overlay")
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceDeepSurface)
                .border(1.5.dp, CyanLaser.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Header: Title, 5 Tabs, Level, Credits, and strictly single-line RESUME button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hangar_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = CyanLaser,
                        maxLines = 1,
                        softWrap = false
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF060C18))
                            .border(1.dp, SpaceHudBorder, RoundedCornerShape(9.dp))
                            .padding(3.dp)
                    ) {
                        HangarTabButton(
                            label = stringResource(R.string.hangar_tab_equipment),
                            selected = selectedTab == 0,
                            showDot = false,
                            tag = "hangar_tab_equipment",
                            onClick = { selectedTab = 0 }
                        )
                        HangarTabButton(
                            label = stringResource(R.string.hangar_tab_vants),
                            selected = selectedTab == 1,
                            showDot = equipment.vants.any { v -> v.activeSlots.any { it == null } } &&
                                equipment.inventory.any {
                                    it.type.category == EquipmentCategory.LASER ||
                                        it.type.category == EquipmentCategory.SHIELD_GENERATOR
                                },
                            dotColor = CyanThruster,
                            tag = "hangar_tab_vants",
                            onClick = { selectedTab = 1 }
                        )
                        HangarTabButton(
                            label = stringResource(R.string.hangar_tab_shop),
                            selected = selectedTab == 2,
                            showDot = false,
                            tag = "hangar_tab_shop",
                            onClick = { selectedTab = 2 }
                        )
                        HangarTabButton(
                            label = stringResource(R.string.hangar_tab_fuse),
                            selected = selectedTab == 3,
                            showDot = equipment.hasAnyReadyFusion,
                            dotColor = AmberCredits,
                            tag = "hangar_tab_fuse",
                            onClick = { selectedTab = 3 }
                        )
                        HangarTabButton(
                            label = stringResource(R.string.hangar_tab_missions),
                            selected = selectedTab == 4,
                            showDot = hud.mission.isReadyToClaim,
                            dotColor = EmeraldHull,
                            tag = "hangar_tab_missions",
                            onClick = { selectedTab = 4 }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF140D26))
                            .border(1.dp, Color(0x88C084FC), RoundedCornerShape(9.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("hangar_level_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${stringResource(R.string.hud_level_prefix)} ${hud.playerLevel}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFE9D5FF),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF071020))
                            .border(1.dp, AmberCredits.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = AmberCredits,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${"%,d".format(hud.credits)} CR",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberCredits,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Button(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanLaser,
                            contentColor = SpaceVoid
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("hangar_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.action_resume),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            // 2. Compact Live Ship Stats Summary Bar + Config 1 / Config 2 Toggle + Dev & Reset Buttons
            LiveStatsSummaryBar(
                equipment = equipment,
                onSwitchConfig = onSwitchConfig,
                onResetSave = onResetSave,
                onDebugAddCredits = onDebugAddCredits,
                onDebugAddLevel = onDebugAddLevel
            )

            // 3. Active Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> EquipmentTabContent(
                        equipment = equipment,
                        onSwitchConfig = onSwitchConfig,
                        onSelectShipClass = onBuyOrSelectShipClass,
                        onEquipFromInventory = onEquipFromInventory,
                        onUnequipLaserSlot = onUnequipLaserSlot,
                        onUnequipShieldSlot = onUnequipShieldSlot,
                        onUnequipSectorChip = onUnequipSectorChip
                    )
                    1 -> VantsTabContent(
                        equipment = equipment,
                        onEquipItemToVantSlot = onEquipItemToVantSlot,
                        onEquipFromInventoryToVant = onEquipFromInventoryToVant,
                        onUnequipVantSlot = onUnequipVantSlot
                    )
                    2 -> ShopTabContent(
                        credits = hud.credits,
                        equipment = equipment,
                        onBuyOrSelectShipClass = onBuyOrSelectShipClass,
                        onBuyOrSelectFormation = onBuyOrSelectFormation,
                        onBuyEquipment = onBuyEquipment,
                        onBuyFlaxVant = onBuyFlaxVant
                    )
                    3 -> FusionWorkshopTabContent(
                        equipment = equipment,
                        onFuseItems = onFuseItems,
                        onFuseFlaxVants = onFuseFlaxVants
                    )
                    else -> MissionsTabContent(
                        playerLevel = hud.playerLevel,
                        mission = hud.mission,
                        onClaimMissionReward = onClaimMissionReward
                    )
                }
            }
        }
    }
}

@Composable
private fun HangarTabButton(
    label: String,
    selected: Boolean,
    showDot: Boolean,
    tag: String,
    dotColor: Color = EmeraldHull,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) CyanLaser else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 6.dp)
            .testTag(tag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) SpaceVoid else TacticalMuted,
            maxLines = 1,
            softWrap = false
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (selected) SpaceVoid else dotColor)
            )
        }
    }
}

@Composable
private fun LiveStatsSummaryBar(
    equipment: EquipmentSnapshot,
    onSwitchConfig: (Int) -> Unit,
    onResetSave: () -> Unit,
    onDebugAddCredits: () -> Unit,
    onDebugAddLevel: () -> Unit
) {
    val shipAccent = Color(equipment.activeShipClass.accentColorHex)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF071020))
            .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("hangar_stats_bar")
    ) {
        // Always-accessible Config 1 / Config 2 Toggle at the top of the Hangar
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF040914))
                .border(1.dp, SpaceHudBorder, RoundedCornerShape(7.dp))
                .padding(2.dp)
        ) {
            HangarTabButton(
                label = stringResource(R.string.hud_config_1),
                selected = equipment.activeConfig == 1,
                showDot = false,
                tag = "hangar_top_config_1",
                onClick = { onSwitchConfig(1) }
            )
            HangarTabButton(
                label = stringResource(R.string.hud_config_2),
                selected = equipment.activeConfig == 2,
                showDot = false,
                tag = "hangar_top_config_2",
                onClick = { onSwitchConfig(2) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(shipAccent.copy(alpha = 0.14f))
                .border(1.dp, shipAccent.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = shipAccent,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = equipment.activeShipClass.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = shipAccent,
                maxLines = 1,
                softWrap = false
            )
        }

        LiveStatPill(
            icon = Icons.Default.Bolt,
            label = stringResource(R.string.stat_dmg),
            value = equipment.totalDamage.toString(),
            accent = CrimsonDanger
        )
        LiveStatPill(
            icon = Icons.Default.Shield,
            label = stringResource(R.string.stat_shd),
            value = equipment.totalMaxShield.toString(),
            accent = CyanThruster
        )
        LiveStatPill(
            icon = Icons.Default.Speed,
            label = stringResource(R.string.stat_spd),
            value = equipment.totalSpeed.toString(),
            accent = AmberCredits
        )
        LiveStatPill(
            icon = Icons.Default.Favorite,
            label = stringResource(R.string.stat_hull),
            value = equipment.totalMaxHull.toString(),
            accent = EmeraldHull
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Temporary Developer / Testing Buttons next to RESET SAVE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x33F59E0B))
                    .border(1.dp, AmberCredits.copy(alpha = 0.85f), RoundedCornerShape(7.dp))
                    .clickable(onClick = onDebugAddCredits)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("hangar_debug_add_credits")
            ) {
                Text(
                    text = stringResource(R.string.action_debug_add_credits),
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberCredits,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x33A855F7))
                    .border(1.dp, Color(0xFFC084FC), RoundedCornerShape(7.dp))
                    .clickable(onClick = onDebugAddLevel)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("hangar_debug_add_level")
            ) {
                Text(
                    text = stringResource(R.string.action_debug_add_level),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE9D5FF),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x28FF2E4C))
                    .border(1.dp, CrimsonDanger.copy(alpha = 0.75f), RoundedCornerShape(7.dp))
                    .clickable(onClick = onResetSave)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("hangar_reset_save_button")
            ) {
                Text(
                    text = stringResource(R.string.action_reset_save),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF6B81),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun LiveStatPill(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = TacticalMuted,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            maxLines = 1,
            softWrap = false
        )
    }
}

// =============================================================================
// TAB 0: EQUIPMENT (Ship Switcher, Config 1 & 2, Sector Chip, Laser & Shield Slots, Inventory)
// =============================================================================
@Composable
private fun EquipmentTabContent(
    equipment: EquipmentSnapshot,
    onSwitchConfig: (Int) -> Unit,
    onSelectShipClass: (ShipClass) -> Unit,
    onEquipFromInventory: (Int) -> Unit,
    onUnequipLaserSlot: (Int) -> Unit,
    onUnequipShieldSlot: (Int) -> Unit,
    onUnequipSectorChip: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Row: Config 1/2 Switcher + Owned Ship Switcher + Dedicated Sector Chip Slot
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF071020))
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 7.dp, vertical = 6.dp)
            ) {
                HangarTabButton(
                    label = stringResource(R.string.hangar_config_1),
                    selected = equipment.activeConfig == 1,
                    showDot = false,
                    tag = "hangar_config_1_button",
                    onClick = { onSwitchConfig(1) }
                )
                HangarTabButton(
                    label = stringResource(R.string.hangar_config_2),
                    selected = equipment.activeConfig == 2,
                    showDot = false,
                    tag = "hangar_config_2_button",
                    onClick = { onSwitchConfig(2) }
                )
            }

            // Quick switch between owned Ship Classes
            if (equipment.ownedShipClasses.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF071020))
                        .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    ShipClass.entries.filter { it in equipment.ownedShipClasses }.forEach { ship ->
                        val active = ship == equipment.activeShipClass
                        val accent = Color(ship.accentColorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) accent else Color.Transparent)
                                .clickable { onSelectShipClass(ship) }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                                .testTag("quick_ship_${ship.name.lowercase()}")
                        ) {
                            Text(
                                text = ship.displayName.take(4).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) SpaceVoid else TacticalMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Dedicated Sector Chip Slot
            val chip = equipment.equippedSectorChip
            val chipAccent = chip?.let { Color(it.type.accentHex) } ?: SpaceHudBorder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (chip != null) chipAccent.copy(alpha = 0.14f) else Color(0xFF071020)
                    )
                    .border(
                        width = if (chip != null) 1.5.dp else 1.dp,
                        color = if (chip != null) chipAccent.copy(alpha = 0.85f) else SpaceHudBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = chip != null, onClick = onUnequipSectorChip)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("sector_chip_slot")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = stringResource(R.string.hangar_sector_chip_slot),
                        tint = if (chip != null) chipAccent else TacticalMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.hangar_sector_chip_slot),
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = chip?.type?.displayName ?: stringResource(R.string.hangar_sector_chip_none),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (chip != null) chipAccent else TacticalWhite,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                if (chip != null) {
                    Text(
                        text = chip.type.statSummary(),
                        style = MaterialTheme.typography.labelSmall,
                        color = chipAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Dynamic Laser Slots (4 / 6 / 8) & Shield Slots (3 / 6 / 10)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val usedLasers = equipment.laserSlots.count { it != null }
            DynamicSlotGridPanel(
                title = "${stringResource(R.string.hangar_laser_slots)} ($usedLasers/${equipment.laserSlots.size})",
                accent = AlienCoreOrange,
                slots = equipment.laserSlots,
                columns = if (equipment.laserSlots.size >= 8) 4 else 3,
                emptyIcon = Icons.Default.Bolt,
                tagPrefix = "laser_slot",
                onSlotTap = onUnequipLaserSlot,
                modifier = Modifier.weight(1f)
            )

            val usedShields = equipment.shieldSlots.count { it != null }
            DynamicSlotGridPanel(
                title = "${stringResource(R.string.hangar_shield_slots)} ($usedShields/${equipment.shieldSlots.size})",
                accent = CyanThruster,
                slots = equipment.shieldSlots,
                columns = if (equipment.shieldSlots.size >= 8) 5 else 3,
                emptyIcon = Icons.Default.Shield,
                tagPrefix = "shield_slot",
                onSlotTap = onUnequipShieldSlot,
                modifier = Modifier.weight(1f)
            )
        }

        // Player Inventory section (unequipped owned items)
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF071020))
                .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${stringResource(R.string.hangar_inventory)} (${equipment.inventory.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalWhite,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = stringResource(R.string.hangar_slot_tap_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (equipment.inventory.isEmpty()) {
                Text(
                    text = stringResource(R.string.hangar_inventory_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TacticalMuted,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    equipment.inventory.forEach { item ->
                        EquipmentSquareTile(
                            item = item,
                            emptyIcon = Icons.Default.Bolt,
                            tag = "inventory_item_${item.instanceId}",
                            onClick = { onEquipFromInventory(item.instanceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicSlotGridPanel(
    title: String,
    accent: Color,
    slots: List<EquipmentItem?>,
    columns: Int,
    emptyIcon: ImageVector,
    tagPrefix: String,
    onSlotTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = (slots.size + columns - 1) / columns
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF071020))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(9.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            maxLines = 1,
            softWrap = false
        )

        for (r in 0 until rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (c in 0 until columns) {
                    val slotIdx = r * columns + c
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (slotIdx < slots.size) {
                            val item = slots[slotIdx]
                            EquipmentSquareTile(
                                item = item,
                                emptyIcon = emptyIcon,
                                tag = "${tagPrefix}_$slotIdx",
                                compact = columns >= 4,
                                onClick = {
                                    if (item != null) onSlotTap(slotIdx)
                                }
                            )
                        } else {
                            Spacer(modifier = Modifier.size(54.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentSquareTile(
    item: EquipmentItem?,
    emptyIcon: ImageVector,
    tag: String,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val accent = item?.let { Color(it.type.accentHex) } ?: SpaceHudBorder
    val bgColor = if (item != null) accent.copy(alpha = 0.14f) else Color(0xFF040914)
    val tileSize = if (compact) 54.dp else 60.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(tileSize)
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .border(
                width = if (item != null) 1.5.dp else 1.dp,
                color = if (item != null) accent.copy(alpha = 0.85f) else SpaceHudBorder.copy(alpha = 0.55f),
                shape = RoundedCornerShape(9.dp)
            )
            .clickable(enabled = item != null, onClick = onClick)
            .testTag(tag)
    ) {
        if (item == null) {
            Icon(
                imageVector = emptyIcon,
                contentDescription = null,
                tint = SpaceHudBorder.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Icon(
                    imageVector = categoryIcon(item.type.category),
                    contentDescription = item.type.displayName,
                    tint = accent,
                    modifier = Modifier.size(if (compact) 15.dp else 17.dp)
                )
                Text(
                    text = item.type.shortCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalWhite,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = item.type.statSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

// =============================================================================
// TAB 1: VANTS (ORBITAL DRONES: FLAX & IRIS WITH CONFIG 1 / 2 UNIVERSAL SLOTS)
// =============================================================================
@Composable
private fun VantsTabContent(
    equipment: EquipmentSnapshot,
    onEquipItemToVantSlot: (Int, Int) -> Unit,
    onEquipFromInventoryToVant: (Int) -> Unit,
    onUnequipVantSlot: (Int, Int) -> Unit
) {
    val compatibleInventory = remember(equipment.inventory) {
        equipment.inventory.filter {
            it.type.category == EquipmentCategory.LASER ||
                it.type.category == EquipmentCategory.SHIELD_GENERATOR
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val totalOwnedVants = equipment.flaxVantCount + equipment.irisVantCount
                val reserveSuffix = if (totalOwnedVants > GameConfig.MAX_VANTS) {
                    " • +${totalOwnedVants - GameConfig.MAX_VANTS} RES"
                } else {
                    ""
                }
                Text(
                    text = "VANTS: ${equipment.vants.size} / ${GameConfig.MAX_VANTS}",
                    style = MaterialTheme.typography.labelLarge,
                    color = CyanLaser,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "FLAX: ${equipment.flaxVantCount} • IRIS: ${equipment.irisVantCount}$reserveSuffix • CFG ${equipment.activeConfig} • ${equipment.activeFormation.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberCredits,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Text(
                text = stringResource(R.string.hangar_vants_hint),
                style = MaterialTheme.typography.labelSmall,
                color = TacticalMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (equipment.vants.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF071020))
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 22.dp)
            ) {
                Text(
                    text = stringResource(R.string.hangar_vants_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TacticalMuted
                )
            }
        } else {
            val rows = (equipment.vants.size + 3) / 4
            for (r in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (c in 0 until 4) {
                        val idx = r * 4 + c
                        if (idx < equipment.vants.size) {
                            val vant = equipment.vants[idx]
                            VantDroneCard(
                                vant = vant,
                                indexLabel = "#${idx + 1}",
                                hasCompatibleInventory = compatibleInventory.isNotEmpty(),
                                onSlotTap = { slotIdx, occupied ->
                                    if (occupied) {
                                        onUnequipVantSlot(vant.id, slotIdx)
                                    } else {
                                        onEquipItemToVantSlot(vant.id, slotIdx)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Compatible Lasers & Shield Generators Inventory for VANT Universal Slots
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF071020))
                .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "LASER & SHIELD INVENTORY (${compatibleInventory.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalWhite,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "Tap item to equip into first empty VANT slot",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (compatibleInventory.isEmpty()) {
                Text(
                    text = "No unequipped Lasers or Shield Generators in Inventory.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TacticalMuted,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    compatibleInventory.forEach { item ->
                        EquipmentSquareTile(
                            item = item,
                            emptyIcon = Icons.Default.Bolt,
                            tag = "vant_inventory_item_${item.instanceId}",
                            onClick = { onEquipFromInventoryToVant(item.instanceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VantDroneCard(
    vant: VantSnapshot,
    indexLabel: String,
    hasCompatibleInventory: Boolean,
    onSlotTap: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isIris = vant.tier == VantTier.IRIS
    val accent = Color(vant.tier.accentColorHex)
    val primary = Color(vant.tier.primaryColorHex)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isIris) Color(0xFF131A2E) else Color(0xFF071020))
            .border(
                width = if (isIris) 1.4.dp else 1.dp,
                color = if (isIris) AmberCredits.copy(alpha = 0.75f) else accent.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(8.dp)
            .testTag("vant_card_${vant.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VantMiniPreview(tier = vant.tier)
            Column {
                Text(
                    text = "${vant.tier.shortCode} $indexLabel",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isIris) AmberCredits else primary,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "${vant.tier.slotCount} ${if (vant.tier.slotCount == 1) "SLOT" else "SLOTS"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            vant.activeSlots.forEachIndexed { slotIdx, item ->
                val itemAccent = item?.let { Color(it.type.accentHex) }
                    ?: if (hasCompatibleInventory) accent.copy(alpha = 0.6f) else SpaceHudBorder
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (item != null) itemAccent.copy(alpha = 0.16f) else Color(0xFF040914)
                        )
                        .border(
                            width = if (item != null) 1.4.dp else 1.dp,
                            color = itemAccent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSlotTap(slotIdx, item != null) }
                        .testTag("vant_${vant.id}_slot_$slotIdx")
                ) {
                    if (item == null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (hasCompatibleInventory) accent else TacticalMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "LSR/SHD",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalMuted,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon(item.type.category),
                                contentDescription = item.type.displayName,
                                tint = itemAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = item.type.shortCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = item.type.statSummary(),
                                style = MaterialTheme.typography.labelSmall,
                                color = itemAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VantMiniPreview(tier: VantTier) {
    val isIris = tier == VantTier.IRIS
    val border = if (isIris) AmberCredits else CyanLaser
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFF040914))
            .border(1.dp, border.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            if (!isIris) {
                val p = Path().apply {
                    moveTo(cx + 7f, cy)
                    lineTo(cx - 3f, cy - 5f)
                    lineTo(cx - 6f, cy)
                    lineTo(cx - 3f, cy + 5f)
                    close()
                }
                drawPath(path = p, color = Color(0xFFCBD5E1))
                drawPath(path = p, color = CyanLaser, style = Stroke(width = 1.4f))
            } else {
                val p = Path().apply {
                    moveTo(cx + 8f, cy)
                    lineTo(cx + 1f, cy - 3.5f)
                    lineTo(cx - 4f, cy - 8f)
                    lineTo(cx - 6.5f, cy - 4f)
                    lineTo(cx - 4.5f, cy)
                    lineTo(cx - 6.5f, cy + 4f)
                    lineTo(cx - 4f, cy + 8f)
                    lineTo(cx + 1f, cy + 3.5f)
                    close()
                }
                drawPath(path = p, color = Color(0xFF1D4ED8))
                drawPath(path = p, color = AmberCredits, style = Stroke(width = 1.6f))
            }
        }
    }
}

// =============================================================================
// TAB 2: MINIMALIST SHOP (3 Ship Classes + Level 1 Base Equipment + Flax VANT)
// =============================================================================
@Composable
private fun ShopTabContent(
    credits: Int,
    equipment: EquipmentSnapshot,
    onBuyOrSelectShipClass: (ShipClass) -> Unit,
    onBuyOrSelectFormation: (DroneFormation) -> Unit,
    onBuyEquipment: (EquipmentItemId) -> Unit,
    onBuyFlaxVant: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Ship Classes Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.hangar_shop_ships),
                style = MaterialTheme.typography.labelLarge,
                color = CyanLaser,
                maxLines = 1,
                softWrap = false
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ShipClass.entries.forEach { ship ->
                    val isOwned = ship in equipment.ownedShipClasses
                    val isActive = ship == equipment.activeShipClass
                    val canAct = isOwned && !isActive || (!isOwned && credits >= ship.cost)
                    val accent = Color(ship.accentColorHex)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isActive) accent.copy(alpha = 0.12f) else Color(0xFF071020))
                            .border(
                                width = if (isActive) 1.6.dp else 1.dp,
                                color = if (isActive) accent else accent.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShipClassMiniPreview(shipClass = ship)
                            Column {
                                Text(
                                    text = ship.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TacticalWhite,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = ship.roleLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = "SPD ${ship.speed.toInt()} • HULL ${ship.baseHull.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalWhite,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "${ship.laserSlotCount} Laser Slots • ${ship.shieldSlotCount} Shield Slots",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            maxLines = 1,
                            softWrap = false
                        )

                        Button(
                            onClick = { onBuyOrSelectShipClass(ship) },
                            enabled = canAct,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOwned) CyanLaser else accent,
                                contentColor = SpaceVoid,
                                disabledContainerColor = if (isActive) EmeraldHull.copy(alpha = 0.25f) else Color(0xFF1A2438),
                                disabledContentColor = if (isActive) EmeraldHull else TacticalMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("shop_ship_${ship.name.lowercase()}")
                        ) {
                            val label = when {
                                isActive -> stringResource(R.string.hangar_active)
                                isOwned -> stringResource(R.string.hangar_equip)
                                else -> "${stringResource(R.string.hangar_buy)} ${"%,d".format(ship.cost)} CR"
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. Minimalist Base Level 1 Equipment ONLY (LF-1 Laser, SG-1 Shield, and Flax VANT; no Sector Chips)
        val baseItems = listOf(
            EquipmentItemId.LF_1,
            EquipmentItemId.SG_1
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.hangar_shop_base_gear),
                style = MaterialTheme.typography.labelLarge,
                color = AmberCredits,
                maxLines = 1,
                softWrap = false
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                baseItems.forEach { item ->
                    val cost = item.shopCost ?: return@forEach
                    val accent = Color(item.accentHex)
                    val ownedCount = equipment.ownedItemCounts[item] ?: 0
                    val canAfford = credits >= cost

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF071020))
                            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(accent.copy(alpha = 0.16f))
                                    .border(1.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(7.dp))
                            ) {
                                Icon(
                                    imageVector = categoryIcon(item.category),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = item.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TacticalWhite,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "${item.statSummary()} • Owned: $ownedCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Button(
                            onClick = { onBuyEquipment(item) },
                            enabled = canAfford,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = SpaceVoid,
                                disabledContainerColor = Color(0xFF1A2438),
                                disabledContentColor = TacticalMuted
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .testTag("shop_buy_${item.name.lowercase()}")
                        ) {
                            Text(
                                text = "${stringResource(R.string.hangar_buy)} $cost CR",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 4th Base Card: Flax VANT (600 CR; allowed while irisCount < 8 && total < 9 for 8th Iris fusion)
                val irisCount = equipment.irisVantCount
                val flaxCount = equipment.flaxVantCount
                val totalOwnedVants = irisCount + flaxCount
                val atMaxIris = irisCount >= GameConfig.MAX_VANTS
                val atFusionCap = !atMaxIris && totalOwnedVants >= GameConfig.MAX_OWNED_VANTS_FOR_FUSION
                val canBuyVant = !atMaxIris && !atFusionCap && credits >= GameConfig.FLAX_VANT_COST
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF071020))
                        .border(1.dp, CyanLaser.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        VantMiniPreview(tier = VantTier.FLAX)
                        Column {
                            Text(
                                text = VantTier.FLAX.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = TacticalWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (totalOwnedVants > GameConfig.MAX_VANTS) {
                                    "1 Slot • ${equipment.vants.size}/${GameConfig.MAX_VANTS} (+1 RES)"
                                } else {
                                    "1 Slot • $totalOwnedVants/${GameConfig.MAX_VANTS}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanLaser,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Button(
                        onClick = onBuyFlaxVant,
                        enabled = canBuyVant,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanLaser,
                            contentColor = SpaceVoid,
                            disabledContainerColor = Color(0xFF1A2438),
                            disabledContentColor = TacticalMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("shop_buy_flax_vant")
                    ) {
                        Text(
                            text = when {
                                atMaxIris -> stringResource(R.string.hangar_vant_max)
                                atFusionCap -> stringResource(R.string.hangar_vant_fuse_required)
                                else -> "${stringResource(R.string.hangar_buy)} ${GameConfig.FLAX_VANT_COST} CR"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 3. Drone Formations Section (2 rows x 3 cards)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.hangar_shop_formations),
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF38BDF8),
                maxLines = 1,
                softWrap = false
            )

            val allFormations = DroneFormation.entries
            val formationRows = allFormations.chunked(3)
            formationRows.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { formation ->
                        val isOwned = formation in equipment.ownedFormations
                        val isActive = formation == equipment.activeFormation
                        val canAct = (isOwned && !isActive) || (!isOwned && credits >= formation.cost)
                        val accent = Color(formation.accentHex)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isActive) accent.copy(alpha = 0.14f) else Color(0xFF071020))
                                .border(
                                    width = if (isActive) 1.6.dp else 1.dp,
                                    color = if (isActive) accent else accent.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(9.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                FormationMiniPreview(formation = formation)
                                Column {
                                    Text(
                                        text = formation.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TacticalWhite,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (formation.cost == 0) "DEFAULT" else "${"%,d".format(formation.cost)} CR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            val bonusText = formation.bonuses.joinToString(", ")
                            val penaltyText = formation.penalties.takeIf { it.isNotEmpty() }?.joinToString(", ")

                            Text(
                                text = bonusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (formation == DroneFormation.NORMAL) TacticalMuted else EmeraldHull,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (penaltyText != null) {
                                Text(
                                    text = penaltyText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonDanger,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = "No penalties",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalMuted,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = { onBuyOrSelectFormation(formation) },
                                enabled = canAct,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOwned) CyanLaser else accent,
                                    contentColor = SpaceVoid,
                                    disabledContainerColor = if (isActive) EmeraldHull.copy(alpha = 0.25f) else Color(0xFF1A2438),
                                    disabledContentColor = if (isActive) EmeraldHull else TacticalMuted
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("shop_formation_${formation.name.lowercase()}")
                            ) {
                                val btnLabel = when {
                                    isActive -> stringResource(R.string.hangar_active)
                                    isOwned -> stringResource(R.string.hangar_equip)
                                    else -> "${stringResource(R.string.hangar_buy)} ${"%,d".format(formation.cost)} CR"
                                }
                                Text(
                                    text = btnLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false
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
private fun FormationMiniPreview(formation: DroneFormation) {
    val accent = Color(formation.accentHex)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040914))
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            // Center ship marker
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = 2.4f,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
            val previewScale = if (formation == DroneFormation.WHEEL) 0.105f else 0.145f
            for (i in 0 until 8) {
                val (localX, localY, _) = formation.computeLocalSlot(
                    index = i,
                    total = 8,
                    hullRadius = 26f,
                    elapsedTime = 0.3f
                )
                // Rotate -90 deg so ship nose points UP in the mini icon
                val px = cx + localY * previewScale
                val py = cy - localX * previewScale
                drawCircle(
                    color = accent,
                    radius = 1.9f,
                    center = androidx.compose.ui.geometry.Offset(px, py)
                )
            }
        }
    }
}

@Composable
private fun ShipClassMiniPreview(shipClass: ShipClass) {
    val fill = Color(shipClass.primaryColorHex)
    val accent = Color(shipClass.accentColorHex)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF040914))
            .border(1.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.5f
            val path = when (shipClass) {
                ShipClass.INTERCEPTOR -> Path().apply {
                    moveTo(cx + 11f, cy)
                    lineTo(cx - 8f, cy - 9f)
                    lineTo(cx - 5f, cy)
                    lineTo(cx - 8f, cy + 9f)
                    close()
                }
                ShipClass.BATTLECRUISER -> Path().apply {
                    moveTo(cx + 11f, cy - 3f)
                    lineTo(cx + 2f, cy - 10f)
                    lineTo(cx - 9f, cy - 8f)
                    lineTo(cx - 9f, cy + 8f)
                    lineTo(cx + 2f, cy + 10f)
                    lineTo(cx + 11f, cy + 3f)
                    close()
                }
                ShipClass.DREADNOUGHT -> Path().apply {
                    moveTo(cx + 12f, cy - 4f)
                    lineTo(cx + 5f, cy - 11f)
                    lineTo(cx - 9f, cy - 11f)
                    lineTo(cx - 11f, cy)
                    lineTo(cx - 9f, cy + 11f)
                    lineTo(cx + 5f, cy + 11f)
                    lineTo(cx + 12f, cy + 4f)
                    close()
                }
            }
            drawPath(path = path, color = fill)
            drawPath(path = path, color = accent, style = Stroke(width = 1.8f))
        }
    }
}

// =============================================================================
// TAB 3: 2-FOR-1 FUSION WORKSHOP ("FUSE" TAB)
// =============================================================================
@Composable
private fun FusionWorkshopTabContent(
    equipment: EquipmentSnapshot,
    onFuseItems: (EquipmentItemId) -> Unit,
    onFuseFlaxVants: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.hangar_fuse_title),
                style = MaterialTheme.typography.labelLarge,
                color = AmberCredits,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = stringResource(R.string.hangar_fuse_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = TacticalMuted
            )
        }

        // VANT DRONE FORGE: 2x Flax VANT -> 1x Iris VANT
        val flaxCount = equipment.flaxVantCount
        val vantFuseReady = flaxCount >= 2
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "VANT DRONE FORGE",
                style = MaterialTheme.typography.labelSmall,
                color = AmberCredits,
                maxLines = 1,
                softWrap = false
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (vantFuseReady) AmberCredits.copy(alpha = 0.14f) else Color(0xFF071020))
                    .border(
                        width = if (vantFuseReady) 1.6.dp else 1.dp,
                        color = if (vantFuseReady) AmberCredits else SpaceHudBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    VantMiniPreview(tier = VantTier.FLAX)
                    Text(
                        text = "2x FLAX",
                        style = MaterialTheme.typography.labelLarge,
                        color = TacticalWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AmberCredits,
                        modifier = Modifier.size(15.dp)
                    )
                    VantMiniPreview(tier = VantTier.IRIS)
                    Column {
                        Text(
                            text = "1x IRIS VANT (2 UNIVERSAL SLOTS)",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberCredits,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Safely returns equipped Flax gear to Inventory before fusing.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onFuseFlaxVants,
                    enabled = vantFuseReady,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberCredits,
                        contentColor = SpaceVoid,
                        disabledContainerColor = Color(0xFF1A2438),
                        disabledContentColor = TacticalMuted
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("fuse_flax_vant")
                ) {
                    if (vantFuseReady) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (vantFuseReady) {
                            "${stringResource(R.string.hangar_fuse_action)} ($flaxCount/2)"
                        } else {
                            "NEED 2 ($flaxCount/2)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        FusionCategoryGroup(
            title = "LASER FORGE",
            accent = AlienCoreOrange,
            recipes = FusionCatalog.recipes.filter { it.source.category == EquipmentCategory.LASER },
            ownedCounts = equipment.ownedItemCounts,
            onFuseItems = onFuseItems
        )

        FusionCategoryGroup(
            title = "SHIELD FORGE",
            accent = CyanThruster,
            recipes = FusionCatalog.recipes.filter { it.source.category == EquipmentCategory.SHIELD_GENERATOR },
            ownedCounts = equipment.ownedItemCounts,
            onFuseItems = onFuseItems
        )

        FusionCategoryGroup(
            title = "SECTOR CHIP FORGE",
            accent = Color(0xFFC084FC),
            recipes = FusionCatalog.recipes.filter { it.source.category == EquipmentCategory.SECTOR_CHIP },
            ownedCounts = equipment.ownedItemCounts,
            onFuseItems = onFuseItems
        )
    }
}

@Composable
private fun FusionCategoryGroup(
    title: String,
    accent: Color,
    recipes: List<FusionRecipe>,
    ownedCounts: Map<EquipmentItemId, Int>,
    onFuseItems: (EquipmentItemId) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            softWrap = false
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            recipes.forEach { recipe ->
                val count = ownedCounts[recipe.source] ?: 0
                val ready = count >= 2
                val resultAccent = Color(recipe.result.accentHex)

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (ready) resultAccent.copy(alpha = 0.14f) else Color(0xFF071020))
                        .border(
                            width = if (ready) 1.6.dp else 1.dp,
                            color = if (ready) resultAccent else SpaceHudBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "2x ${recipe.source.shortCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalWhite,
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = resultAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "1x ${recipe.result.shortCode}",
                            style = MaterialTheme.typography.labelLarge,
                            color = resultAccent,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Text(
                        text = "${recipe.result.displayName} (${recipe.result.statSummary()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ready) TacticalWhite else TacticalMuted,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = { onFuseItems(recipe.source) },
                        enabled = ready,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = resultAccent,
                            contentColor = SpaceVoid,
                            disabledContainerColor = Color(0xFF1A2438),
                            disabledContentColor = TacticalMuted
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("fuse_${recipe.source.name.lowercase()}")
                    ) {
                        if (ready) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (ready) {
                                "${stringResource(R.string.hangar_fuse_action)} ($count/2)"
                            } else {
                                "NEED 2 ($count/2)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// TAB 3: SEQUENTIAL MISSION CHAINS ("MISSIONS" TAB)
// =============================================================================
@Composable
private fun MissionsTabContent(
    playerLevel: Int,
    mission: MissionSnapshot,
    onClaimMissionReward: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.hangar_missions_title),
                style = MaterialTheme.typography.labelLarge,
                color = CyanLaser,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = stringResource(R.string.hangar_missions_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = TacticalMuted
            )
        }

        MissionCatalog.all.forEach { spec ->
            val isCompleted = spec.index < mission.currentMissionIndex
            val isCurrent = spec.index == mission.currentMissionIndex
            val isLevelLocked = isCurrent && playerLevel < spec.requiredLevel
            val isReadyToClaim = isCurrent && !isLevelLocked && mission.currentMissionProgress >= spec.targetCount
            val isUpcoming = spec.index > mission.currentMissionIndex

            val accent = when {
                isCompleted -> EmeraldHull
                isReadyToClaim -> EmeraldHull
                isLevelLocked -> Color(0xFFC084FC)
                isCurrent && spec.isSectorFinal -> AmberCredits
                isCurrent -> CyanLaser
                else -> SpaceHudBorder
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isReadyToClaim -> Color(0xFF062C1B)
                            isCurrent -> Color(0xFF0B182E)
                            isCompleted -> Color(0xFF061614)
                            else -> Color(0xFF060C18)
                        }
                    )
                    .border(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = accent.copy(alpha = if (isUpcoming) 0.45f else 0.85f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("mission_row_${spec.code.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when {
                            isCompleted || isReadyToClaim -> Icons.Default.CheckCircle
                            isLevelLocked || (isUpcoming && spec.isSectorFinal) -> Icons.Default.Lock
                            else -> Icons.Default.Flag
                        },
                        contentDescription = null,
                        tint = if (isUpcoming) TacticalMuted else accent,
                        modifier = Modifier.size(18.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${spec.code} • ${spec.sectorChainLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUpcoming) TacticalMuted else accent,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (spec.isSectorFinal) {
                                Text(
                                    text = "REQ LV ${spec.requiredLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE9D5FF),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = spec.description,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isUpcoming) TacticalMuted else TacticalWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Reward: ${spec.rewardSummary()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (spec.rewardChip != null) AmberCredits else TacticalMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    isCompleted -> {
                        Text(
                            text = "DONE",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmeraldHull
                        )
                    }
                    isReadyToClaim -> {
                        Button(
                            onClick = onClaimMissionReward,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldHull,
                                contentColor = SpaceVoid
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("mission_claim_button")
                        ) {
                            Text(
                                text = stringResource(R.string.hud_mission_claim),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    isLevelLocked -> {
                        Text(
                            text = "LOCKED — REACH LEVEL ${spec.requiredLevel} TO UNLOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE9D5FF),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    isCurrent -> {
                        Text(
                            text = "${mission.currentMissionProgress} / ${spec.targetCount}",
                            style = MaterialTheme.typography.titleMedium,
                            color = accent
                        )
                    }
                    else -> {
                        Text(
                            text = "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted
                        )
                    }
                }
            }
        }
    }
}

private fun categoryIcon(category: EquipmentCategory): ImageVector = when (category) {
    EquipmentCategory.LASER -> Icons.Default.Bolt
    EquipmentCategory.SHIELD_GENERATOR -> Icons.Default.Shield
    EquipmentCategory.SECTOR_CHIP -> Icons.Default.Memory
}
