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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.SpaceDeepSurface
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite

/**
 * Interactive Station Overlay for:
 * 1. Central Refining Station (enforces Clockwise City Refining Loop: C1->C2->C3->C1 & Yellow Crystal->0-0)
 * 2. Strictly Isolated Local Market / Black Market (independent stock & prices per sectorId)
 * 3. Capital City Crafting Station (crafts consumables, chips & gear from refined resources)
 */
@Composable
fun StationOverlay(
    hud: HudSnapshot,
    onRefineResource: (ResourceType, Boolean) -> Unit,
    onBuyMarketResource: (ResourceType, Boolean) -> Unit,
    onSellMarketResource: (ResourceType, Boolean, Boolean) -> Unit,
    onBuyMarketEquipment: (EquipmentItemId) -> Unit,
    onCraftRecipe: (CraftingRecipeId) -> Unit,
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

            when (kind) {
                StationKind.REFINERY -> {
                    RefineryContent(
                        sectorId = hud.currentSectorId,
                        rawCargo = hud.rawCargo,
                        refinedCargo = hud.refinedCargo,
                        lang = lang,
                        strings = strings,
                        onRefine = onRefineResource,
                        modifier = Modifier.weight(1f)
                    )
                }

                StationKind.CRAFTING -> {
                    CraftingContent(
                        hud = hud,
                        lang = lang,
                        strings = strings,
                        onCraft = onCraftRecipe,
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
                            accent = accent,
                            strings = strings,
                            onBuyResource = onBuyMarketResource,
                            onSellResource = onSellMarketResource,
                            onBuyEquipment = onBuyMarketEquipment,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CraftingContent(
    hud: HudSnapshot,
    lang: GameLanguage,
    strings: GameText,
    onCraft: (CraftingRecipeId) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalRefinedT1Plus = hud.refinedCargo.entries.sumOf { (_, qty) -> qty }
    val totalRefinedT2Plus = hud.refinedCargo.entries
        .filter { (res, _) -> res.tier >= 2 }
        .sumOf { (_, qty) -> qty }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact Resource & Status Summary Strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xB3071D1A))
                .border(1.dp, Color(0x9934D399), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = "${strings.craftingAvailableRefined}: T1+ ×$totalRefinedT1Plus  •  T2+ ×$totalRefinedT2Plus",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF6EE7B7),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${strings.craftingAmmoStock}: ×${hud.overchargedAmmo}  •  ${strings.hudHp} ${hud.playerHp}/${hud.playerMaxHp}",
                style = MaterialTheme.typography.labelSmall,
                color = AmberCredits,
                fontWeight = FontWeight.Bold
            )
        }

        if (totalRefinedT1Plus == 0) {
            Text(
                text = strings.craftingNeedRefinedHint,
                style = MaterialTheme.typography.labelSmall,
                color = TacticalMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(CraftingRecipeId.entries, key = { it.name }) { recipe ->
                val availableForRecipe = hud.refinedCargo.entries
                    .filter { (res, qty) -> qty > 0 && res.tier >= recipe.minRefinedTier }
                    .sumOf { it.value }
                val canCraft = availableForRecipe >= recipe.refinedCost
                val itemColor = Color(recipe.accentHex)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC0A1324))
                        .border(
                            1.dp,
                            itemColor.copy(alpha = if (canCraft) 0.72f else 0.28f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(itemColor)
                        )
                        Column {
                            Text(
                                text = GameStrings.craftingRecipeTitle(recipe, lang),
                                style = MaterialTheme.typography.labelLarge,
                                color = TacticalWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = GameStrings.craftingRecipeDesc(recipe, lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${strings.craftingCostPrefix}: ${recipe.refinedCost}× ${strings.craftingAnyRefinedTierPrefix}${recipe.minRefinedTier}+ ($availableForRecipe/${recipe.refinedCost})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canCraft) Color(0xFF6EE7B7) else AmberCredits,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { onCraft(recipe) },
                        enabled = canCraft,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34D399),
                            contentColor = SpaceVoid,
                            disabledContainerColor = Color(0xFF1E293B),
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
            }
        }
    }
}

@Composable
private fun RefineryContent(
    sectorId: String,
    rawCargo: Map<ResourceType, Int>,
    refinedCargo: Map<ResourceType, Int>,
    lang: GameLanguage,
    strings: GameText,
    onRefine: (ResourceType, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val acceptedTypes = remember(sectorId) {
        ResourceType.entries.filter { it.refinesInSectorId == sectorId }
    }
    val otherOwnedRaw = rawCargo.entries
        .filter { (res, qty) -> qty > 0 && res.refinesInSectorId != sectorId }

    val acceptedSummary = GameStrings.refineryAcceptedSummary(sectorId, lang)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            Text(
                text = acceptedSummary,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFDBA74),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${strings.refineryRatePrefix}: ${ResourceType.RAW_REQUIRED_PER_REFINE} ${strings.refineryRawOwned.uppercase()} → 1 ${strings.refineryRefinedOwned.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = CyanLaser,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(acceptedTypes, key = { it.name }) { res ->
                val rawQty = rawCargo[res] ?: 0
                val refQty = refinedCargo[res] ?: 0
                val canRefine = rawQty >= ResourceType.RAW_REQUIRED_PER_REFINE
                val resColor = Color(res.colorHex)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xCC0A1324))
                        .border(1.dp, resColor.copy(alpha = if (canRefine) 0.72f else 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(resColor)
                        )
                        Column {
                            Text(
                                text = "T${res.tier} ${res.displayName} → ${res.refinedName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TacticalWhite,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${strings.refineryNativePrefix}: ${res.nativeSectorId} • ${strings.refineryRawOwned}: $rawQty • ${strings.refineryRefinedOwned}: $refQty",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalMuted
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onRefine(res, false) },
                            enabled = canRefine,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFB923C),
                                contentColor = SpaceVoid,
                                disabledContainerColor = Color(0xFF1E293B),
                                disabledContentColor = TacticalMuted
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("refine_one_${res.name}")
                        ) {
                            Text(strings.refineryRefineOne, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                        if (rawQty >= ResourceType.RAW_REQUIRED_PER_REFINE * 2) {
                            OutlinedButton(
                                onClick = { onRefine(res, true) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFDBA74)),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("refine_all_${res.name}")
                            ) {
                                Text(strings.refineryAll, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (otherOwnedRaw.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberCredits,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = strings.refineryOtherCargoHeader,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberCredits,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                items(otherOwnedRaw, key = { "other_${it.key.name}" }) { (res, qty) ->
                    val destSector = GalaxyMapGraph.sectorOf(res.refinesInSectorId)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x99070D19))
                            .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "T${res.tier} ${res.displayName} (×$qty)",
                                style = MaterialTheme.typography.labelMedium,
                                color = TacticalWhite
                            )
                            Text(
                                text = "${strings.refineryCannotRefineIn} $sectorId — ${strings.refineryHaulTo} ${destSector.id} (${destSector.name})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TacticalMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(destSector.nodeColor.copy(alpha = 0.16f))
                                .border(1.dp, destSector.nodeColor.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = destSector.nodeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${strings.refineryRefineInBadge} ${res.refinesInSectorId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = destSector.nodeColor,
                                fontWeight = FontWeight.Bold
                            )
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
    accent: Color,
    strings: GameText,
    onBuyResource: (ResourceType, Boolean) -> Unit,
    onSellResource: (ResourceType, Boolean, Boolean) -> Unit,
    onBuyEquipment: (EquipmentItemId) -> Unit,
    modifier: Modifier = Modifier
) {
    // 0 = SELL CARGO, 1 = BUY RESOURCES, 2 = LOCAL GEAR
    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketTabPill(strings.marketTabSell, tab == 0, accent, Modifier.weight(1f)) { tab = 0 }
            MarketTabPill(strings.marketTabBuy, tab == 1, accent, Modifier.weight(1f)) { tab = 1 }
            MarketTabPill(
                if (market.isBlackMarket) strings.marketTabBlackGear else strings.marketTabLocalGear,
                tab == 2,
                accent,
                Modifier.weight(1f)
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
                            val itemName = if (listing.isRefined) res.refinedName else "${res.displayName} (${strings.marketRawTag})"
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
                        val itemName = if (listing.isRefined) res.refinedName else "${res.displayName} (${strings.marketRawTag})"
                        val canBuy = listing.localStock > 0 && credits >= listing.buyPrice
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
                                    " • ${strings.marketRefinesInPrefix} ${res.refinesInSectorId}"
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
                                    text = if (listing.localStock <= 0) strings.marketOut else "${strings.marketBuyPrefix} ${listing.buyPrice} CR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(market.equipmentListings, key = { "eq_${it.itemId.name}" }) { gear ->
                        val item = gear.itemId
                        val canBuy = gear.localStock > 0 && credits >= gear.buyPrice
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
                                    text = if (gear.localStock <= 0) strings.marketSoldOut else "${strings.marketBuyPrefix} ${gear.buyPrice} CR",
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

@Composable
private fun MarketTabPill(
    label: String,
    selected: Boolean,
    accent: Color,
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
