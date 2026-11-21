package com.example.game

import kotlin.random.Random

/**
 * The 3 Station Crafting disciplines matching the 3 Pilot crafting skills.
 */
enum class CraftingDiscipline(
    val skill: SkillType,
    val accentHex: Long
) {
    NAVAL_ENGINEERING(
        skill = SkillType.NAVAL_ENGINEERING,
        accentHex = 0xFF34D399
    ),
    GEAR_MANUFACTURING(
        skill = SkillType.GEAR_MANUFACTURING,
        accentHex = 0xFF38BDF8
    ),
    SYNTHESIS(
        skill = SkillType.SYNTHESIS,
        accentHex = 0xFFE879F9
    )
}

/**
 * Single ingredient cost for a station crafting recipe.
 * Refined materials consume from refinedCargo; direct-use items (e.g., XENO_CORE) consume from rawCargo.
 */
data class CraftMaterialCost(
    val resource: ResourceType,
    val isRefined: Boolean = !resource.isDirectUse,
    val amount: Int
)

enum class CraftOutputKind {
    INDUSTRIAL_SHIP,
    EQUIPMENT_GEAR,
    EXTRACTION_TOOL,
    CONSUMABLE_REPAIR,
    CONSUMABLE_BOOSTER
}

enum class StationCraftRecipeId(
    val discipline: CraftingDiscipline,
    val outputKind: CraftOutputKind,
    val accentHex: Long,
    val xpReward: Long,
    val ingredients: List<CraftMaterialCost>,
    val shipHullId: String? = null,
    val industrialType: IndustrialType? = null,
    val resultEquipmentId: EquipmentItemId? = null,
    val basePrimaryStat: Float = 0f,
    val baseDurationMinutes: Int = 0
) {
    // -------------------------------------------------------------------------
    // 1. NAVAL ENGINEERING (4 Industrial Ships -> added directly to Hangar)
    // -------------------------------------------------------------------------
    SHIP_MINER(
        discipline = CraftingDiscipline.NAVAL_ENGINEERING,
        outputKind = CraftOutputKind.INDUSTRIAL_SHIP,
        accentHex = 0xFF38BDF8,
        xpReward = 180L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.STAR_COBALT, isRefined = true, amount = 4),
            CraftMaterialCost(ResourceType.VERIDIAN_ORE, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.AETHER_QUARTZ, isRefined = true, amount = 2)
        ),
        shipHullId = "MINER_T1",
        industrialType = IndustrialType.MINER
    ),
    SHIP_PLASMA_COLLECTOR(
        discipline = CraftingDiscipline.NAVAL_ENGINEERING,
        outputKind = CraftOutputKind.INDUSTRIAL_SHIP,
        accentHex = 0xFF2DD4BF,
        xpReward = 195L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.STAR_COBALT, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.BIO_RESIN, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.MAGMA_STONE, isRefined = true, amount = 3)
        ),
        shipHullId = "PLASMA_COLLECTOR_T1",
        industrialType = IndustrialType.PLASMA_COLLECTOR
    ),
    SHIP_MEDIUM_HAULER(
        discipline = CraftingDiscipline.NAVAL_ENGINEERING,
        outputKind = CraftOutputKind.INDUSTRIAL_SHIP,
        accentHex = 0xFF4ADE80,
        xpReward = 260L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.VERIDIAN_ORE, isRefined = true, amount = 5),
            CraftMaterialCost(ResourceType.CRIMSON_IRON, isRefined = true, amount = 5),
            CraftMaterialCost(ResourceType.BIO_RESIN, isRefined = true, amount = 3)
        ),
        shipHullId = "MEDIUM_HAULER_T1",
        industrialType = IndustrialType.MEDIUM_HAULER
    ),
    SHIP_HEAVY_HAULER(
        discipline = CraftingDiscipline.NAVAL_ENGINEERING,
        outputKind = CraftOutputKind.INDUSTRIAL_SHIP,
        accentHex = 0xFFFACC15,
        xpReward = 380L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.CRIMSON_IRON, isRefined = true, amount = 8),
            CraftMaterialCost(ResourceType.STAR_COBALT, isRefined = true, amount = 6),
            CraftMaterialCost(ResourceType.MAGMA_STONE, isRefined = true, amount = 4),
            CraftMaterialCost(ResourceType.AETHER_QUARTZ, isRefined = true, amount = 4)
        ),
        shipHullId = "HEAVY_HAULER_T1",
        industrialType = IndustrialType.HEAVY_HAULER
    ),

    // -------------------------------------------------------------------------
    // 2. GEAR MANUFACTURING (Tier 2+ Lasers, Shields & Mining/Salvage Tools)
    // -------------------------------------------------------------------------
    GEAR_LASER_T2(
        discipline = CraftingDiscipline.GEAR_MANUFACTURING,
        outputKind = CraftOutputKind.EQUIPMENT_GEAR,
        accentHex = 0xFFFF5252,
        xpReward = 140L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.STAR_COBALT, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.MAGMA_STONE, isRefined = true, amount = 2),
            CraftMaterialCost(ResourceType.XENO_SALVAGE, isRefined = true, amount = 2)
        ),
        resultEquipmentId = EquipmentItemId.LF_2,
        basePrimaryStat = EquipmentItemId.LF_2.damageBonus
    ),
    GEAR_LASER_T3(
        discipline = CraftingDiscipline.GEAR_MANUFACTURING,
        outputKind = CraftOutputKind.EQUIPMENT_GEAR,
        accentHex = 0xFFF43F5E,
        xpReward = 240L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.CRIMSON_IRON, isRefined = true, amount = 4),
            CraftMaterialCost(ResourceType.AETHER_QUARTZ, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.XENO_SALVAGE, isRefined = true, amount = 4)
        ),
        resultEquipmentId = EquipmentItemId.LF_3,
        basePrimaryStat = EquipmentItemId.LF_3.damageBonus
    ),
    GEAR_SHIELD_T2(
        discipline = CraftingDiscipline.GEAR_MANUFACTURING,
        outputKind = CraftOutputKind.EQUIPMENT_GEAR,
        accentHex = 0xFF0EA5E9,
        xpReward = 140L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.VERIDIAN_ORE, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.AETHER_QUARTZ, isRefined = true, amount = 2),
            CraftMaterialCost(ResourceType.XENO_SALVAGE, isRefined = true, amount = 2)
        ),
        resultEquipmentId = EquipmentItemId.SG_2,
        basePrimaryStat = EquipmentItemId.SG_2.shieldBonus
    ),
    GEAR_SHIELD_T3(
        discipline = CraftingDiscipline.GEAR_MANUFACTURING,
        outputKind = CraftOutputKind.EQUIPMENT_GEAR,
        accentHex = 0xFF818CF8,
        xpReward = 240L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.VERIDIAN_ORE, isRefined = true, amount = 4),
            CraftMaterialCost(ResourceType.BIO_RESIN, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.XENO_SALVAGE, isRefined = true, amount = 4)
        ),
        resultEquipmentId = EquipmentItemId.SG_3,
        basePrimaryStat = EquipmentItemId.SG_3.shieldBonus
    ),
    GEAR_MINING_TOOL_T2(
        discipline = CraftingDiscipline.GEAR_MANUFACTURING,
        outputKind = CraftOutputKind.EXTRACTION_TOOL,
        accentHex = 0xFF38BDF8,
        xpReward = 125L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.STAR_COBALT, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.BIO_RESIN, isRefined = true, amount = 2),
            CraftMaterialCost(ResourceType.XENO_SALVAGE, isRefined = true, amount = 1)
        ),
        basePrimaryStat = 15f // +15% base mining speed (scaled by quality)
    ),
    GEAR_SALVAGE_TOOL_T2(
        discipline = CraftingDiscipline.GEAR_MANUFACTURING,
        outputKind = CraftOutputKind.EXTRACTION_TOOL,
        accentHex = 0xFFC084FC,
        xpReward = 135L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.CRIMSON_IRON, isRefined = true, amount = 3),
            CraftMaterialCost(ResourceType.MAGMA_STONE, isRefined = true, amount = 2),
            CraftMaterialCost(ResourceType.XENO_SALVAGE, isRefined = true, amount = 2)
        ),
        basePrimaryStat = 15f // +15% base salvage speed (scaled by quality)
    ),

    // -------------------------------------------------------------------------
    // 3. SYNTHESIS (Repair Kits & Boosters -> added to Inventory, up to 20% 2x)
    // -------------------------------------------------------------------------
    SYNTH_REPAIR_KIT(
        discipline = CraftingDiscipline.SYNTHESIS,
        outputKind = CraftOutputKind.CONSUMABLE_REPAIR,
        accentHex = 0xFF4ADE80,
        xpReward = 90L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.BIO_RESIN, isRefined = true, amount = 2),
            CraftMaterialCost(ResourceType.MAGMA_STONE, isRefined = true, amount = 1)
        ),
        basePrimaryStat = 250f // Restores 250 HP/SHD (scaled by quality)
    ),
    SYNTH_DAMAGE_BOOSTER(
        discipline = CraftingDiscipline.SYNTHESIS,
        outputKind = CraftOutputKind.CONSUMABLE_BOOSTER,
        accentHex = 0xFFFF6B35,
        xpReward = 190L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.MAGMA_STONE, isRefined = true, amount = 2),
            CraftMaterialCost(ResourceType.YELLOW_CRYSTAL, isRefined = true, amount = 1),
            CraftMaterialCost(ResourceType.XENO_CORE, isRefined = false, amount = 1)
        ),
        basePrimaryStat = 12f, // +12% Damage (scaled by quality)
        baseDurationMinutes = 10
    ),
    SYNTH_SHIELD_BOOSTER(
        discipline = CraftingDiscipline.SYNTHESIS,
        outputKind = CraftOutputKind.CONSUMABLE_BOOSTER,
        accentHex = 0xFF38BDF8,
        xpReward = 190L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.BIO_RESIN, isRefined = true, amount = 2),
            CraftMaterialCost(ResourceType.YELLOW_CRYSTAL, isRefined = true, amount = 1),
            CraftMaterialCost(ResourceType.XENO_CORE, isRefined = false, amount = 1)
        ),
        basePrimaryStat = 15f, // +15% Shield (scaled by quality)
        baseDurationMinutes = 10
    ),
    SYNTH_SPEED_BOOSTER(
        discipline = CraftingDiscipline.SYNTHESIS,
        outputKind = CraftOutputKind.CONSUMABLE_BOOSTER,
        accentHex = 0xFFFACC15,
        xpReward = 165L,
        ingredients = listOf(
            CraftMaterialCost(ResourceType.BIO_RESIN, isRefined = true, amount = 1),
            CraftMaterialCost(ResourceType.MAGMA_STONE, isRefined = true, amount = 1),
            CraftMaterialCost(ResourceType.YELLOW_CRYSTAL, isRefined = true, amount = 1)
        ),
        basePrimaryStat = 10f, // +10% Speed (scaled by quality)
        baseDurationMinutes = 10
    );

    fun shipHullDefinition(): ShipHullDefinition? =
        shipHullId?.let { id -> ShipHullCatalog.all.firstOrNull { it.id == id } }

    companion object {
        fun forDiscipline(discipline: CraftingDiscipline): List<StationCraftRecipeId> =
            entries.filter { it.discipline == discipline }
    }
}

/**
 * Quality roll odds snapshot for a given crafting skill level (Lv 1–99).
 */
data class CraftQualityOdds(
    val standardPct: Int,
    val reinforcedPct: Int,
    val excellentPct: Int,
    val masterworkPct: Int
)

/**
 * Computes quality probability brackets based on pilot crafting skill level (Lv 1–99):
 * - Lv 1–33: Mostly STANDARD (+0%), small chance of REINFORCED (+5%).
 * - Lv 34–66: Increased REINFORCED (+5%) and EXCELLENT (+10%), very small chance of MASTERWORK (+15%).
 * - Lv 67–99: Strong scaling for EXCELLENT (+10%) and MASTERWORK (+15%).
 */
fun craftQualityOdds(skillLevel: Int): CraftQualityOdds {
    val lv = skillLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
    val masterwork: Float
    val excellent: Float
    val reinforced: Float
    when {
        lv <= 33 -> {
            val t = (lv - 1) / 32.0f
            masterwork = 0f
            excellent = 0f
            reinforced = 0.06f + t * 0.16f // 6% -> 22%
        }
        lv <= 66 -> {
            val t = (lv - 34) / 32.0f
            masterwork = 0.01f + t * 0.04f // 1% -> 5%
            excellent = 0.08f + t * 0.18f  // 8% -> 26%
            reinforced = 0.24f + t * 0.14f // 24% -> 38%
        }
        else -> {
            val t = (lv - 67) / 32.0f
            masterwork = 0.07f + t * 0.21f // 7% -> 28%
            excellent = 0.28f + t * 0.14f  // 28% -> 42%
            reinforced = 0.36f - t * 0.14f // 36% -> 22%
        }
    }
    val mwInt = Math.round(masterwork * 100f)
    val exInt = Math.round(excellent * 100f)
    val rfInt = Math.round(reinforced * 100f)
    val stInt = (100 - mwInt - exInt - rfInt).coerceAtLeast(0)
    return CraftQualityOdds(
        standardPct = stInt,
        reinforcedPct = rfInt,
        excellentPct = exInt,
        masterworkPct = mwInt
    )
}

/**
 * Rolls a [CraftQuality] using the pilot's relevant crafting skill level (Lv 1–99).
 */
fun rollCraftQuality(skillLevel: Int, rng: Random = Random.Default): CraftQuality {
    val odds = craftQualityOdds(skillLevel)
    val roll = rng.nextInt(100)
    val mwCut = odds.masterworkPct
    val exCut = mwCut + odds.excellentPct
    val rfCut = exCut + odds.reinforcedPct
    return when {
        roll < mwCut -> CraftQuality.MASTERWORK
        roll < exCut -> CraftQuality.EXCELLENT
        roll < rfCut -> CraftQuality.REINFORCED
        else -> CraftQuality.STANDARD
    }
}

/**
 * Material return perk for NAVAL_ENGINEERING and GEAR_MANUFACTURING:
 * scales smoothly from 0% at Lv 1 up to 15% of materials refunded at Lv 99.
 */
fun craftingMaterialReturnRate(skillLevel: Int): Float {
    val lv = skillLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
    return ((lv - 1) / 98.0f) * 0.15f
}

/**
 * Double-output perk for SYNTHESIS:
 * scales smoothly from 0% at Lv 1 up to 20% chance at Lv 99 to craft 2x instead of 1x.
 */
fun synthesisDoubleOutputChance(skillLevel: Int): Float {
    val lv = skillLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
    return ((lv - 1) / 98.0f) * 0.20f
}

/**
 * Crafted Industrial Ship stored in the player's Hangar with quality-scaled HP, Shield, and Cargo.
 */
data class CraftedShipRecord(
    val instanceId: Int,
    val recipeId: StationCraftRecipeId,
    val hullId: String,
    val industrialType: IndustrialType,
    val quality: CraftQuality,
    val effectiveHull: Int,
    val effectiveShield: Int,
    val effectiveCargo: Int
)

/**
 * Crafted tool or consumable (Repair Kit / Booster) stored in the player's Inventory.
 */
data class CraftedItemRecord(
    val instanceId: Int,
    val recipeId: StationCraftRecipeId,
    val quality: CraftQuality,
    val effectivePrimaryStat: Float,
    val effectiveDurationMinutes: Int = 0,
    val quantity: Int = 1
)

/**
 * Immediate result of a successful craft used to render the Station UI toast/banner.
 */
data class CraftOutcome(
    val id: Long,
    val recipeId: StationCraftRecipeId,
    val quality: CraftQuality,
    val quantityCrafted: Int,
    val refundedUnits: Int,
    val xpAwarded: Long,
    val doubledOutput: Boolean,
    val statSummary: String
)
