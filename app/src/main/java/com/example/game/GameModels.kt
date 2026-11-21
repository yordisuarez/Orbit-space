package com.example.game

object GameConfig {
    const val MAP_RADIUS = 20000f
    const val SAFE_ZONE_RADIUS = 600f
    const val ZONE_1_MAX_RADIUS = 5000f
    const val ZONE_2_MAX_RADIUS = 10000f
    const val ZONE_3_MAX_RADIUS = 15000f
    const val ZONE_4_MAX_RADIUS = 20000f

    const val BORDER_PADDING = 30f
    const val RADIATION_WARNING_DISTANCE = 260f
    const val RADIATION_TICK_INTERVAL = 0.65f

    // Base bonuses independent of ship class
    const val PLAYER_HP_PER_LEVEL = 15f
    const val PLAYER_BASE_MAX_SHIELD = 40f
    const val PLAYER_BASE_LASER_DAMAGE = 6f

    // VANT (Drone) limits, pricing & formations
    const val MAX_VANTS = 8
    const val MAX_OWNED_VANTS_FOR_FUSION = 9
    const val FLAX_VANT_COST = 600
    const val FORMATION_SWITCH_COOLDOWN_SEC = 3.0f

    // Boss alien variant scaling (20% spawn chance per sector, +65% boosted BOSS HP)
    const val BOSS_SPAWN_CHANCE = 0.20f
    const val BOSS_HP_MULTIPLIER = 6.6f
    const val BOSS_DAMAGE_MULTIPLIER = 2f
    const val BOSS_REWARD_MULTIPLIER = 4
    const val BOSS_SIZE_MULTIPLIER = 1.5f

    // Tactical Map Autopilot & Local Radar Minimap
    const val AUTOPILOT_STOP_RADIUS = 60f
    const val AUTOPILOT_MAX_TARGET_RADIUS = 19800f
    const val HUD_MINIMAP_VIEW_RADIUS = 4500f
    const val ALIEN_RADAR_DETECTION_RANGE = 2800f

    // Jump Gate Portal & Classic Square Map (MAP X-1)
    const val MAIN_JUMP_GATE_X = 220f
    const val MAIN_JUMP_GATE_Y = 0f
    const val MAP_X1_SIZE = 14000f
    const val MAP_X1_SAFE_ZONE_X = 1200f
    const val MAP_X1_SAFE_ZONE_Y = 1200f
    const val MAP_X1_SAFE_ZONE_RADIUS = 500f
    const val MAP_X1_SPAWN_X = 1200f
    const val MAP_X1_SPAWN_Y = 1350f
    const val MAP_X1_AUTOPILOT_MIN = 200f
    const val MAP_X1_AUTOPILOT_MAX = 13800f
    const val MAP_X1_RADIATION_OUTER_BUFFER = 360f
    const val MAP_X1_BORDER_RADIATION_DPS = 58f
    const val MAP_X1_STAR_COUNT = 1500
    const val MAP_X1_NEBULA_COUNT = 28
    const val MAP_X1_BONUS_BOX_COUNT = 50
    const val JUMP_GATE_INTERACT_RADIUS = 120f
    const val WARP_FLASH_DURATION_SEC = 0.4f

    // Sector 1 Alien Base Stats & 2.5x Exponential Scaling
    const val SECTOR_BASE_ALIEN_HP = 120.0
    const val SECTOR_BASE_ALIEN_DAMAGE = 10.0
    const val SECTOR_BASE_ALIEN_CREDITS = 100.0
    const val SECTOR_BASE_ALIEN_XP = 25.0
    const val SECTOR_SCALING_BASE = 2.5

    const val PLAYER_TURN_SPEED = 11f
    const val PLAYER_RADIUS = 22f
    const val PLAYER_ATTACK_RANGE = 520f
    const val PLAYER_FIRE_INTERVAL = 0.24f
    const val PLAYER_LASER_SPEED = 960f

    // Homing Rockets
    const val ROCKET_COOLDOWN_SEC = 3.2f
    const val ROCKET_SPEED = 560f
    const val ROCKET_TURN_RATE = 7.5f
    const val ROCKET_LIFETIME_SEC = 2.6f

    // Shield & Safe Zone Repair rates
    const val SHIELD_REGEN_DELAY = 4.0f
    const val SHIELD_REGEN_RATE = 26f
    const val SAFE_ZONE_HP_REGEN_RATE = 64f
    const val SAFE_ZONE_SHIELD_REGEN_RATE = 130f

    // Right joystick radar cone (56 degrees wide = +/- 28 degrees)
    const val RADAR_CONE_RANGE = 520f
    const val RADAR_CONE_SWEEP_DEG = 56f
    const val RADAR_CONE_HALF_ANGLE_RAD = (28.0 * Math.PI / 180.0).toFloat()

    // Bonus Boxes & Cargo Loot
    const val BONUS_BOX_COUNT = 120
    const val LOOT_COLLECT_RADIUS = 42f
    const val CARGO_LIFETIME_SEC = 50f

    const val STAR_COUNT = 3000
    const val NEBULA_COUNT = 64

    const val TAP_LOCK_RADIUS = 74f
    const val CAMERA_SMOOTHNESS = 8.5f
    const val JOYSTICK_DEADZONE = 0.12f

    fun xpRequiredForLevel(level: Int): Int {
        val lv = level.coerceAtLeast(1)
        return 180 * lv + 85 * (lv - 1) * (lv - 1)
    }
}

enum class ShipRole {
    CAPSULE,
    INDUSTRIAL,
    COMBAT
}

enum class IndustrialType(
    val solidTimeMult: Float,
    val plasmaTimeMult: Float
) {
    MINER(solidTimeMult = 1.0f, plasmaTimeMult = 2.0f),
    PLASMA_COLLECTOR(solidTimeMult = 2.0f, plasmaTimeMult = 1.0f),
    MEDIUM_HAULER(solidTimeMult = 3.0f, plasmaTimeMult = 3.0f),
    HEAVY_HAULER(solidTimeMult = 5.0f, plasmaTimeMult = 5.0f);

    fun extractionTimeMultiplier(category: ResourceCategory): Float = when (category) {
        ResourceCategory.SOLID -> solidTimeMult
        ResourceCategory.PLASMA -> plasmaTimeMult
        ResourceCategory.SALVAGE -> 1.0f
    }
}

enum class CraftQuality(
    val bonusPct: Float,
    val accentHex: Long
) {
    STANDARD(bonusPct = 0.0f, accentHex = 0xFFE2E8F0),
    REINFORCED(bonusPct = 0.05f, accentHex = 0xFF4ADE80),
    EXCELLENT(bonusPct = 0.10f, accentHex = 0xFFA855F7),
    MASTERWORK(bonusPct = 0.15f, accentHex = 0xFFFACC15);

    val statMultiplier: Float get() = 1.0f + bonusPct

    fun applyBonus(baseValue: Float): Float = baseValue * statMultiplier

    fun applyBonusInt(baseValue: Int): Int = Math.round(baseValue * statMultiplier)
}

const val PILOT_SKILL_MIN_LEVEL = 1
const val PILOT_SKILL_MAX_LEVEL = 99

enum class SkillType {
    MINING,
    SALVAGING,
    REFINING,
    NAVAL_ENGINEERING,
    GEAR_MANUFACTURING,
    SYNTHESIS,
    WEAPONRY,
    SHIELDS,
    PILOTING
}

/**
 * 4-tier exponential XP curve for Pilot Skills (Lv 1–99):
 * - Lv 1–33: Easy
 * - Lv 34–66: Medium
 * - Lv 67–89: Hard
 * - Lv 90–99: Very Hard (returns 0L at max Lv 99)
 */
fun xpRequiredForNextLevel(level: Int): Long {
    if (level >= PILOT_SKILL_MAX_LEVEL) return 0L
    val lv = level.coerceAtLeast(PILOT_SKILL_MIN_LEVEL)
    val t1End = 100.0 * Math.pow(1.06, 32.0)       // Lv 33 anchor (~645)
    val t2End = t1End * Math.pow(1.085, 33.0)      // Lv 66 anchor (~9,528)
    val t3End = t2End * Math.pow(1.12, 23.0)       // Lv 89 anchor (~129,144)

    val raw = when {
        lv <= 33 -> 100.0 * Math.pow(1.06, (lv - 1).toDouble())
        lv <= 66 -> t1End * Math.pow(1.085, (lv - 33).toDouble())
        lv <= 89 -> t2End * Math.pow(1.12, (lv - 66).toDouble())
        else -> t3End * Math.pow(1.22, (lv - 89).toDouble())
    }
    return Math.round(raw).coerceAtLeast(100L)
}

/**
 * Piecewise linear extraction time per charge based on pilot skill level:
 * Lv 1 -> 3.0s, Lv 33 -> 2.0s, Lv 66 -> 1.0s, Lv 99 -> 0.1s (clamped at 0.1s).
 */
fun baseExtractionSeconds(skillLevel: Int): Float {
    val lv = skillLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
    val seconds = when {
        lv <= 33 -> 3.0f - ((lv - 1) / 32.0f) * 1.0f
        lv <= 66 -> 2.0f - ((lv - 33) / 33.0f) * 1.0f
        else -> 1.0f - ((lv - 66) / 33.0f) * 0.9f
    }
    return seconds.coerceAtLeast(0.1f)
}

fun effectiveExtractionSeconds(
    skillLevel: Int,
    category: ResourceCategory,
    industrialType: IndustrialType?
): Float {
    val base = baseExtractionSeconds(skillLevel)
    val mult = industrialType?.extractionTimeMultiplier(category) ?: 1.0f
    return (base * mult).coerceAtLeast(0.1f)
}

/**
 * Smoothly scales Credit refining fee discount from 0% at Lv 1 up to 35% at Lv 99.
 */
fun refiningFeeDiscountPct(refiningLevel: Int): Float {
    val lv = refiningLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
    return ((lv - 1) / 98.0f) * 0.35f
}

/**
 * Smoothly scales bonus refined output chance from 0% at Lv 1 up to 15% at Lv 99.
 */
fun refiningBonusOutputChance(refiningLevel: Int): Float {
    val lv = refiningLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
    return ((lv - 1) / 98.0f) * 0.15f
}

fun baseRefiningFeePerUnit(resource: ResourceType): Int = when {
    resource.isYellowCrystal -> 240
    resource == ResourceType.XENO_SALVAGE -> 80
    resource.category == ResourceCategory.PLASMA -> 40
    else -> 20
}

fun effectiveRefiningFeePerUnit(resource: ResourceType, refiningLevel: Int): Int {
    val base = baseRefiningFeePerUnit(resource)
    val discount = refiningFeeDiscountPct(refiningLevel)
    return Math.round(base * (1.0f - discount)).coerceAtLeast(1)
}

fun refiningXpPerUnit(resource: ResourceType): Long = when {
    resource.isYellowCrystal -> 140L
    resource == ResourceType.XENO_SALVAGE -> 68L
    resource.category == ResourceCategory.PLASMA -> 42L
    else -> 25L
}

data class SkillProgress(
    val level: Int = PILOT_SKILL_MIN_LEVEL,
    val xp: Long = 0L
) {
    val xpToNextLevel: Long get() = xpRequiredForNextLevel(level)

    val isMaxLevel: Boolean get() = level >= PILOT_SKILL_MAX_LEVEL

    val progressFraction: Float
        get() {
            val req = xpToNextLevel
            if (req <= 0L) return 1f
            return (xp.toFloat() / req.toFloat()).coerceIn(0f, 1f)
        }

    fun addXp(gainedXp: Long): SkillProgress {
        if (gainedXp <= 0L || isMaxLevel) return this
        var currentLevel = level.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
        var currentXp = (xp + gainedXp).coerceAtLeast(0L)
        while (currentLevel < PILOT_SKILL_MAX_LEVEL) {
            val needed = xpRequiredForNextLevel(currentLevel)
            if (needed <= 0L || currentXp < needed) break
            currentXp -= needed
            currentLevel++
        }
        if (currentLevel >= PILOT_SKILL_MAX_LEVEL) {
            currentLevel = PILOT_SKILL_MAX_LEVEL
            currentXp = 0L
        }
        return SkillProgress(level = currentLevel, xp = currentXp)
    }
}

fun defaultPilotSkills(): Map<SkillType, SkillProgress> =
    SkillType.entries.associateWith { SkillProgress() }

/**
 * Persistent Pilot / Capsule profile storing all 9 Pilot Skills (Lv 1–99).
 */
data class PilotProfile(
    val pilotName: String? = null,
    val company: PilotCompany? = null,
    val skills: Map<SkillType, SkillProgress> = defaultPilotSkills()
) {
    fun progressOf(skill: SkillType): SkillProgress = skills[skill] ?: SkillProgress()

    fun levelOf(skill: SkillType): Int = progressOf(skill).level

    fun withSkillXp(skill: SkillType, gainedXp: Long): PilotProfile {
        val updated = skills.toMutableMap()
        updated[skill] = progressOf(skill).addXp(gainedXp)
        return copy(skills = updated)
    }
}

enum class ShipClass(
    val displayName: String,
    val roleLabel: String,
    val role: ShipRole,
    val industrialType: IndustrialType? = null,
    val tier: Int,
    val cost: Int,
    val speed: Float,
    val baseHull: Float,
    val baseShield: Float,
    val baseCargo: Int,
    val laserSlotCount: Int,
    val shieldSlotCount: Int,
    val hullRadius: Float,
    val primaryColorHex: Long,
    val accentColorHex: Long
) {
    INTERCEPTOR(
        displayName = "Interceptor",
        roleLabel = "COMBAT T1 • FAST",
        role = ShipRole.COMBAT,
        industrialType = null,
        tier = 1,
        cost = 0,
        speed = 340f,
        baseHull = 140f,
        baseShield = 100f,
        baseCargo = 80,
        laserSlotCount = 6,
        shieldSlotCount = 4,
        hullRadius = 20f,
        primaryColorHex = 0xFF1E293B,
        accentColorHex = 0xFF00E5FF
    ),
    BATTLECRUISER(
        displayName = "Battlecruiser",
        roleLabel = "COMBAT T2 • BALANCED",
        role = ShipRole.COMBAT,
        industrialType = null,
        tier = 2,
        cost = 1500,
        speed = 265f,
        baseHull = 260f,
        baseShield = 220f,
        baseCargo = 160,
        laserSlotCount = 6,
        shieldSlotCount = 6,
        hullRadius = 24f,
        primaryColorHex = 0xFF172554,
        accentColorHex = 0xFFFBBF24
    ),
    DREADNOUGHT(
        displayName = "Dreadnought",
        roleLabel = "COMBAT T3 • HEAVY TANK",
        role = ShipRole.COMBAT,
        industrialType = null,
        tier = 3,
        cost = 4000,
        speed = 205f,
        baseHull = 480f,
        baseShield = 420f,
        baseCargo = 280,
        laserSlotCount = 6,
        shieldSlotCount = 10,
        hullRadius = 29f,
        primaryColorHex = 0xFF064E3B,
        accentColorHex = 0xFF34D399
    );

    fun effectiveHull(quality: CraftQuality = CraftQuality.STANDARD): Float =
        quality.applyBonus(baseHull)

    fun effectiveShield(quality: CraftQuality = CraftQuality.STANDARD): Float =
        quality.applyBonus(baseShield)

    fun effectiveCargo(quality: CraftQuality = CraftQuality.STANDARD): Int =
        quality.applyBonusInt(baseCargo)
}

/**
 * Full v1.10 ship hull blueprint definition supporting Capsule, Industrial archetypes,
 * and tiered Combat ships. Weapon slot counts stay constant across tiers within a role,
 * while higher tiers scale Hull HP, Shield capacity, and Cargo hold.
 */
data class ShipHullDefinition(
    val id: String,
    val displayName: String,
    val role: ShipRole,
    val industrialType: IndustrialType? = null,
    val tier: Int,
    val speed: Float,
    val baseHull: Float,
    val baseShield: Float,
    val baseCargo: Int,
    val weaponSlotCount: Int,
    val shieldSlotCount: Int,
    val hullRadius: Float
) {
    fun effectiveHull(quality: CraftQuality = CraftQuality.STANDARD): Float =
        quality.applyBonus(baseHull)

    fun effectiveShield(quality: CraftQuality = CraftQuality.STANDARD): Float =
        quality.applyBonus(baseShield)

    fun effectiveCargo(quality: CraftQuality = CraftQuality.STANDARD): Int =
        quality.applyBonusInt(baseCargo)
}

object ShipHullCatalog {
    val all: List<ShipHullDefinition> = listOf(
        // Escape / Pilot Capsule (Permanent starter vessel: 25 cargo, 3 fixed permanent slots)
        ShipHullDefinition(
            id = "CAPSULE_T1",
            displayName = "Pilot Capsule",
            role = ShipRole.CAPSULE,
            industrialType = null,
            tier = 1,
            speed = 310f,
            baseHull = 90f,
            baseShield = 40f,
            baseCargo = 25,
            weaponSlotCount = 1,
            shieldSlotCount = 1,
            hullRadius = 14f
        ),
        // Industrial Miners (Solid 1.0x, Plasma 2.0x; constant weapon slots across tiers)
        ShipHullDefinition(
            id = "MINER_T1",
            displayName = "Prospector Mk I",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.MINER,
            tier = 1,
            speed = 235f,
            baseHull = 220f,
            baseShield = 140f,
            baseCargo = 240,
            weaponSlotCount = 2,
            shieldSlotCount = 4,
            hullRadius = 22f
        ),
        ShipHullDefinition(
            id = "MINER_T2",
            displayName = "Prospector Mk II",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.MINER,
            tier = 2,
            speed = 225f,
            baseHull = 380f,
            baseShield = 260f,
            baseCargo = 440,
            weaponSlotCount = 2,
            shieldSlotCount = 6,
            hullRadius = 24f
        ),
        // Industrial Plasma Collectors (Solid 2.0x, Plasma 1.0x)
        ShipHullDefinition(
            id = "PLASMA_COLLECTOR_T1",
            displayName = "Siphon Mk I",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.PLASMA_COLLECTOR,
            tier = 1,
            speed = 245f,
            baseHull = 200f,
            baseShield = 180f,
            baseCargo = 220,
            weaponSlotCount = 2,
            shieldSlotCount = 5,
            hullRadius = 22f
        ),
        ShipHullDefinition(
            id = "PLASMA_COLLECTOR_T2",
            displayName = "Siphon Mk II",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.PLASMA_COLLECTOR,
            tier = 2,
            speed = 235f,
            baseHull = 340f,
            baseShield = 320f,
            baseCargo = 420,
            weaponSlotCount = 2,
            shieldSlotCount = 7,
            hullRadius = 24f
        ),
        // Medium Hauler (Solid 3.0x, Plasma 3.0x)
        ShipHullDefinition(
            id = "MEDIUM_HAULER_T1",
            displayName = "Freighter M-1",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.MEDIUM_HAULER,
            tier = 1,
            speed = 200f,
            baseHull = 360f,
            baseShield = 240f,
            baseCargo = 650,
            weaponSlotCount = 2,
            shieldSlotCount = 6,
            hullRadius = 26f
        ),
        ShipHullDefinition(
            id = "MEDIUM_HAULER_T2",
            displayName = "Freighter M-2",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.MEDIUM_HAULER,
            tier = 2,
            speed = 192f,
            baseHull = 580f,
            baseShield = 400f,
            baseCargo = 1100,
            weaponSlotCount = 2,
            shieldSlotCount = 8,
            hullRadius = 27f
        ),
        // Heavy Hauler (Solid 5.0x, Plasma 5.0x)
        ShipHullDefinition(
            id = "HEAVY_HAULER_T1",
            displayName = "Leviathan H-1",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.HEAVY_HAULER,
            tier = 1,
            speed = 165f,
            baseHull = 620f,
            baseShield = 440f,
            baseCargo = 1400,
            weaponSlotCount = 2,
            shieldSlotCount = 8,
            hullRadius = 31f
        ),
        ShipHullDefinition(
            id = "HEAVY_HAULER_T2",
            displayName = "Leviathan H-2",
            role = ShipRole.INDUSTRIAL,
            industrialType = IndustrialType.HEAVY_HAULER,
            tier = 2,
            speed = 158f,
            baseHull = 980f,
            baseShield = 720f,
            baseCargo = 2400,
            weaponSlotCount = 2,
            shieldSlotCount = 10,
            hullRadius = 33f
        ),
        // Combat Ships (constant 6 weapon slots across tiers; scaling HP, Shield, Cargo)
        ShipHullDefinition(
            id = "COMBAT_T1",
            displayName = ShipClass.INTERCEPTOR.displayName,
            role = ShipRole.COMBAT,
            industrialType = null,
            tier = 1,
            speed = ShipClass.INTERCEPTOR.speed,
            baseHull = ShipClass.INTERCEPTOR.baseHull,
            baseShield = ShipClass.INTERCEPTOR.baseShield,
            baseCargo = ShipClass.INTERCEPTOR.baseCargo,
            weaponSlotCount = ShipClass.INTERCEPTOR.laserSlotCount,
            shieldSlotCount = ShipClass.INTERCEPTOR.shieldSlotCount,
            hullRadius = ShipClass.INTERCEPTOR.hullRadius
        ),
        ShipHullDefinition(
            id = "COMBAT_T2",
            displayName = ShipClass.BATTLECRUISER.displayName,
            role = ShipRole.COMBAT,
            industrialType = null,
            tier = 2,
            speed = ShipClass.BATTLECRUISER.speed,
            baseHull = ShipClass.BATTLECRUISER.baseHull,
            baseShield = ShipClass.BATTLECRUISER.baseShield,
            baseCargo = ShipClass.BATTLECRUISER.baseCargo,
            weaponSlotCount = ShipClass.BATTLECRUISER.laserSlotCount,
            shieldSlotCount = ShipClass.BATTLECRUISER.shieldSlotCount,
            hullRadius = ShipClass.BATTLECRUISER.hullRadius
        ),
        ShipHullDefinition(
            id = "COMBAT_T3",
            displayName = ShipClass.DREADNOUGHT.displayName,
            role = ShipRole.COMBAT,
            industrialType = null,
            tier = 3,
            speed = ShipClass.DREADNOUGHT.speed,
            baseHull = ShipClass.DREADNOUGHT.baseHull,
            baseShield = ShipClass.DREADNOUGHT.baseShield,
            baseCargo = ShipClass.DREADNOUGHT.baseCargo,
            weaponSlotCount = ShipClass.DREADNOUGHT.laserSlotCount,
            shieldSlotCount = ShipClass.DREADNOUGHT.shieldSlotCount,
            hullRadius = ShipClass.DREADNOUGHT.hullRadius
        )
    )

    val capsule: ShipHullDefinition
        get() = all.first { it.id == "CAPSULE_T1" }

    fun byId(id: String): ShipHullDefinition? = all.firstOrNull { it.id == id }

    fun industrialT1(type: IndustrialType): ShipHullDefinition =
        all.first { it.role == ShipRole.INDUSTRIAL && it.industrialType == type && it.tier == 1 }

    fun forCombatClass(shipClass: ShipClass): ShipHullDefinition = when (shipClass) {
        ShipClass.INTERCEPTOR -> all.first { it.id == "COMBAT_T1" }
        ShipClass.BATTLECRUISER -> all.first { it.id == "COMBAT_T2" }
        ShipClass.DREADNOUGHT -> all.first { it.id == "COMBAT_T3" }
    }
}

/**
 * Fixed permanent loadout for the Pilot Capsule (never lost, never sold, never destroyed):
 * 1 Basic Laser, 1 Basic Tool (Mining/Salvaging), and 1 Basic Extra/Shield.
 */
object CapsulePermanentConfig {
    const val BASE_HULL = 90f
    const val BASE_SHIELD = GameConfig.PLAYER_BASE_MAX_SHIELD
    const val SPEED = 310f
    const val CARGO_CAPACITY = 25
    const val HULL_RADIUS = 14f
    const val PERMANENT_LASER_DAMAGE = 12f // Basic Tier 1 Laser
    const val PERMANENT_SHIELD_BONUS = 80f // Basic Tier 1 Extra/Shield
    const val MINING_TIME_MULTIPLIER = 1.5f
}

/**
 * NPC Basic Vendor catalog in Capital Cities (C1, C2, C3).
 * Strictly limited to Tier 1 starter essentials so players can never get soft-locked.
 * Ships, Tier 2+ gear, VANTs, and Nucleus boosters remain 100% player-crafted.
 */
enum class BasicVendorItemId(
    val priceCredits: Int,
    val accentHex: Long,
    val equipmentId: EquipmentItemId? = null
) {
    BASIC_LASER_T1(
        priceCredits = 120,
        accentHex = 0xFFFF6B35,
        equipmentId = EquipmentItemId.LF_1
    ),
    BASIC_TOOL_T1(
        priceCredits = 100,
        accentHex = 0xFF38BDF8
    ),
    BASIC_SHIELD_T1(
        priceCredits = 140,
        accentHex = 0xFF0EA5E9,
        equipmentId = EquipmentItemId.SG_1
    ),
    BASIC_REPAIR_KIT(
        priceCredits = 80,
        accentHex = 0xFF4ADE80
    )
}

/**
 * Lootable player Ship Wreck spawned at death coordinates containing 60% of cargo
 * and equipped non-capsule gear (the remaining 40% is permanently destroyed).
 */
class PlayerShipWreck(
    val id: Int,
    val sectorId: String,
    val x: Float,
    val y: Float,
    val shipName: String,
    val isCapsuleWreck: Boolean,
    val rawCargo: MutableMap<ResourceType, Int> = linkedMapOf(),
    val refinedCargo: MutableMap<ResourceType, Int> = linkedMapOf(),
    val droppedEquipment: MutableList<EquipmentItem> = mutableListOf(),
    var expirationTimerSec: Float = DEFAULT_LIFETIME_SEC,
    var beaconTimerSec: Float = 0f
) {
    val totalCargoUnits: Int
        get() = rawCargo.values.sum() + refinedCargo.values.sum()
    val totalGearItems: Int
        get() = droppedEquipment.size
    val isEmpty: Boolean
        get() = totalCargoUnits <= 0 && totalGearItems <= 0

    companion object {
        const val DEFAULT_LIFETIME_SEC = 300f
    }
}

/**
 * Summary of the player's most recent destruction event for localized death/respawn feedback.
 */
data class DeathSummary(
    val wasInCapsule: Boolean,
    val destroyedShipName: String?,
    val deathSectorId: String,
    val deathX: Int,
    val deathY: Int,
    val cargoDroppedToWreck: Int,
    val cargoDestroyed: Int,
    val gearDroppedToWreck: Int,
    val gearDestroyed: Int,
    val vantsDamagedCount: Int,
    val homeCityId: String
)

enum class GameMap(
    val displayName: String,
    val gateLabel: String
) {
    MAIN_MAP(displayName = "MAIN MAP", gateLabel = "GATE: X-1"),
    MAP_X1(displayName = "MAP X-1", gateLabel = "GATE: BASE")
}

enum class SpaceZone(
    val requiredChipTier: Int,
    val radiationDps: Float,
    val requiredChipName: String?
) {
    SAFE_ZONE(requiredChipTier = 0, radiationDps = 0f, requiredChipName = null),
    ZONE_1_ALPHA(requiredChipTier = 0, radiationDps = 0f, requiredChipName = null),
    ZONE_2_BETA(requiredChipTier = 1, radiationDps = 14f, requiredChipName = "CHIP LV 2"),
    ZONE_3_GAMMA(requiredChipTier = 2, radiationDps = 28f, requiredChipName = "CHIP LV 3"),
    ZONE_4_OMEGA(requiredChipTier = 3, radiationDps = 52f, requiredChipName = "CHIP LV 4")
}

object SectorAlienScaling {
    fun multiplierForSector(sectorIndex: Int): Double {
        val exp = (sectorIndex - 1).coerceAtLeast(0)
        return Math.pow(GameConfig.SECTOR_SCALING_BASE, exp.toDouble())
    }

    fun hpForSector(sectorIndex: Int): Float =
        Math.round(GameConfig.SECTOR_BASE_ALIEN_HP * multiplierForSector(sectorIndex)).toFloat()

    fun damageForSector(sectorIndex: Int): Float =
        Math.round(GameConfig.SECTOR_BASE_ALIEN_DAMAGE * multiplierForSector(sectorIndex)).toFloat()

    fun creditsForSector(sectorIndex: Int): Int =
        Math.round(GameConfig.SECTOR_BASE_ALIEN_CREDITS * multiplierForSector(sectorIndex)).toInt()

    fun xpForSector(sectorIndex: Int): Int =
        Math.round(GameConfig.SECTOR_BASE_ALIEN_XP * multiplierForSector(sectorIndex)).toInt()
}

enum class AlienTier(
    val sectorIndex: Int,
    val callsign: String,
    val minRadius: Float,
    val maxRadius: Float,
    val activeCount: Int,
    val radius: Float,
    val patrolSpeed: Float,
    val chaseSpeed: Float,
    val attackRange: Float,
    val fireInterval: Float,
    val laserSpeed: Float,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val minimapDotRadius: Float
) {
    SCOUT(
        sectorIndex = 1,
        callsign = "SCOUT",
        minRadius = GameConfig.SAFE_ZONE_RADIUS,
        maxRadius = GameConfig.ZONE_1_MAX_RADIUS,
        activeCount = 22,
        radius = 17f,
        patrolSpeed = 118f,
        chaseSpeed = 190f,
        attackRange = 360f,
        fireInterval = 1.25f,
        laserSpeed = 520f,
        primaryColorHex = 0xFF4ADE80,
        accentColorHex = 0xFF84CC16,
        minimapDotRadius = 2.4f
    ),
    HUNTER(
        sectorIndex = 2,
        callsign = "HUNTER",
        minRadius = GameConfig.ZONE_1_MAX_RADIUS,
        maxRadius = GameConfig.ZONE_2_MAX_RADIUS,
        activeCount = 24,
        radius = 22f,
        patrolSpeed = 96f,
        chaseSpeed = 160f,
        attackRange = 400f,
        fireInterval = 1.08f,
        laserSpeed = 550f,
        primaryColorHex = 0xFFFACC15,
        accentColorHex = 0xFFF59E0B,
        minimapDotRadius = 2.7f
    ),
    DESTROYER(
        sectorIndex = 3,
        callsign = "DESTROYER",
        minRadius = GameConfig.ZONE_2_MAX_RADIUS,
        maxRadius = GameConfig.ZONE_3_MAX_RADIUS,
        activeCount = 20,
        radius = 28f,
        patrolSpeed = 80f,
        chaseSpeed = 135f,
        attackRange = 440f,
        fireInterval = 0.96f,
        laserSpeed = 580f,
        primaryColorHex = 0xFFFB923C,
        accentColorHex = 0xFFEF4444,
        minimapDotRadius = 3.0f
    ),
    DREADNOUGHT_BOSS(
        sectorIndex = 4,
        callsign = "DREADNOUGHT",
        minRadius = GameConfig.ZONE_3_MAX_RADIUS,
        maxRadius = GameConfig.ZONE_4_MAX_RADIUS,
        activeCount = 14,
        radius = 40f,
        patrolSpeed = 62f,
        chaseSpeed = 110f,
        attackRange = 500f,
        fireInterval = 0.85f,
        laserSpeed = 620f,
        primaryColorHex = 0xFFC084FC,
        accentColorHex = 0xFFE11D48,
        minimapDotRadius = 3.6f
    );

    val maxHp: Float get() = SectorAlienScaling.hpForSector(sectorIndex)
    val laserDamage: Float get() = SectorAlienScaling.damageForSector(sectorIndex)
    val rewardCredits: Int get() = SectorAlienScaling.creditsForSector(sectorIndex)
    val rewardXp: Int get() = SectorAlienScaling.xpForSector(sectorIndex)
}

enum class PilotCompany(
    val id: String,
    val displayName: String,
    val tag: String,
    val lore: String,
    val primaryColorHex: Long,
    val darkBgColorHex: Long,
    val rgbInt: Int
) {
    SOLARIS(
        id = "solaris",
        displayName = "SOLARIS",
        tag = "SOL",
        lore = "Aggressive mining & combat syndicate.",
        primaryColorHex = 0xFFFF3B5C,
        darkBgColorHex = 0xFF240912,
        rgbInt = 0xFF3B5C
    ),
    AETHER(
        id = "aether",
        displayName = "AETHER",
        tag = "AET",
        lore = "High-tech tactical defense corporation.",
        primaryColorHex = 0xFF00E5FF,
        darkBgColorHex = 0xFF061E2B,
        rgbInt = 0x00E5FF
    ),
    VERDANT(
        id = "verdant",
        displayName = "VERDANT",
        tag = "VER",
        lore = "Wealthy trade & exploration empire.",
        primaryColorHex = 0xFF22C55E,
        darkBgColorHex = 0xFF072416,
        rgbInt = 0x22C55E
    );

    companion object {
        fun fromId(raw: String?): PilotCompany? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull {
                it.id.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true)
            }
        }
    }
}

enum class AppScreenStage {
    INTRO,
    ONBOARDING_COMPANY,
    ONBOARDING_NAME,
    GAMEPLAY
}

enum class VantTier(
    val displayName: String,
    val shortCode: String,
    val slotCount: Int,
    val shopCost: Int?,
    val primaryColorHex: Long,
    val accentColorHex: Long
) {
    FLAX(
        displayName = "Flax VANT",
        shortCode = "FLAX",
        slotCount = 1,
        shopCost = GameConfig.FLAX_VANT_COST,
        primaryColorHex = 0xFFCBD5E1,
        accentColorHex = 0xFF00E5FF
    ),
    IRIS(
        displayName = "Iris VANT",
        shortCode = "IRIS",
        slotCount = 2,
        shopCost = null,
        primaryColorHex = 0xFFFBBF24,
        accentColorHex = 0xFF38BDF8
    )
}

enum class DroneFormation(
    val displayName: String,
    val shortCode: String,
    val cost: Int,
    val laserMultiplier: Float = 1.0f,
    val shieldMultiplier: Float = 1.0f,
    val hullMultiplier: Float = 1.0f,
    val speedMultiplier: Float = 1.0f,
    val passiveShieldRegenPctPerSec: Float = 0.0f,
    val bonuses: List<String> = emptyList(),
    val penalties: List<String> = emptyList(),
    val accentHex: Long = 0xFF00E5FF
) {
    NORMAL(
        displayName = "Normal",
        shortCode = "NORM",
        cost = 0,
        bonuses = listOf("Balanced Escort"),
        penalties = emptyList(),
        accentHex = 0xFF94A3B8
    ),
    DRILL(
        displayName = "Drill",
        shortCode = "DRIL",
        cost = 1000,
        laserMultiplier = 1.20f,
        shieldMultiplier = 0.75f,
        speedMultiplier = 0.95f,
        bonuses = listOf("+20% Laser DMG"),
        penalties = listOf("-25% Max SHD", "-5% Speed"),
        accentHex = 0xFFFF5252
    ),
    HEART(
        displayName = "Heart",
        shortCode = "HART",
        cost = 1200,
        laserMultiplier = 0.95f,
        shieldMultiplier = 1.20f,
        hullMultiplier = 1.20f,
        bonuses = listOf("+20% Max Hull", "+20% Max SHD"),
        penalties = listOf("-5% Laser DMG"),
        accentHex = 0xFFF472B6
    ),
    DIAMOND(
        displayName = "Diamond",
        shortCode = "DIAM",
        cost = 1500,
        hullMultiplier = 0.70f,
        passiveShieldRegenPctPerSec = 0.015f,
        bonuses = listOf("+1.5% SHD/s Combat Regen"),
        penalties = listOf("-30% Max Hull"),
        accentHex = 0xFF38BDF8
    ),
    RING(
        displayName = "Ring",
        shortCode = "RING",
        cost = 1800,
        laserMultiplier = 0.75f,
        shieldMultiplier = 1.85f,
        speedMultiplier = 0.95f,
        bonuses = listOf("+85% Max SHD"),
        penalties = listOf("-25% Laser DMG", "-5% Speed"),
        accentHex = 0xFFA855F7
    ),
    WHEEL(
        displayName = "Wheel",
        shortCode = "WHEL",
        cost = 1200,
        laserMultiplier = 0.80f,
        speedMultiplier = 1.15f,
        bonuses = listOf("+15% Speed"),
        penalties = listOf("-20% Laser DMG"),
        accentHex = 0xFFFACC15
    );

    /**
     * Computes target local offset (forward = +X, right = +Y) and extra facing rotation (rad)
     * for drone [index] out of [total] around a ship of radius [hullRadius].
     */
    fun computeLocalSlot(
        index: Int,
        total: Int,
        hullRadius: Float,
        elapsedTime: Float
    ): Triple<Float, Float, Float> {
        val count = total.coerceAtLeast(1)
        val base = hullRadius + 22f
        return when (this) {
            NORMAL -> {
                // Classic flanking formation on left/right wings and behind the ship
                val slots = arrayOf(
                    Pair(6f, -base),
                    Pair(6f, base),
                    Pair(-14f, -(base + 14f)),
                    Pair(-14f, base + 14f),
                    Pair(-32f, -(base + 6f)),
                    Pair(-32f, base + 6f),
                    Pair(-42f, -15f),
                    Pair(-42f, 15f)
                )
                val (lx, ly) = slots[index % slots.size]
                Triple(lx, ly, 0f)
            }

            DRILL -> {
                // Forward-pointing wedge / arrowhead triangle
                val slots = arrayOf(
                    Pair(base + 24f, 0f),
                    Pair(base + 8f, -18f),
                    Pair(base + 8f, 18f),
                    Pair(base - 10f, -34f),
                    Pair(base - 10f, 34f),
                    Pair(-14f, -46f),
                    Pair(-14f, 46f),
                    Pair(-34f, 0f)
                )
                val (lx, ly) = slots[index % slots.size]
                Triple(lx, ly, 0f)
            }

            HEART -> {
                // Parametric heart curve around ship (+X forward point, lobes behind)
                val t = (index.toFloat() / count) * (2.0 * Math.PI)
                val sinT = kotlin.math.sin(t)
                val cosT = kotlin.math.cos(t)
                // Classic heart: xH = 16 sin^3(t), yH = 13 cos(t) - 5 cos(2t) - 2 cos(3t) - cos(4t)
                val xH = 16.0 * sinT * sinT * sinT
                val yH = 13.0 * cosT - 5.0 * kotlin.math.cos(2.0 * t) -
                    2.0 * kotlin.math.cos(3.0 * t) - kotlin.math.cos(4.0 * t)
                val scale = (base + 14f) / 16f
                // Point the heart tip forward (+X), lobes to left/right/rear
                val lx = (-yH * scale).toFloat() + 6f
                val ly = (xH * scale).toFloat()
                Triple(lx, ly, 0f)
            }

            DIAMOND -> {
                // Sharp diamond / rhombus perimeter (4 vertices first, then 4 mid-edges)
                val rF = base + 20f
                val rS = base + 18f
                val slots = arrayOf(
                    Pair(rF, 0f),
                    Pair(0f, rS),
                    Pair(-rF, 0f),
                    Pair(0f, -rS),
                    Pair(rF * 0.5f, rS * 0.5f),
                    Pair(-rF * 0.5f, rS * 0.5f),
                    Pair(-rF * 0.5f, -rS * 0.5f),
                    Pair(rF * 0.5f, -rS * 0.5f)
                )
                val (lx, ly) = slots[index % slots.size]
                Triple(lx, ly, 0f)
            }

            RING -> {
                // Tight protective rotating ring
                val r = hullRadius + 20f
                val theta = elapsedTime * 1.5f + index * (2f * Math.PI.toFloat() / count)
                val lx = kotlin.math.cos(theta) * r
                val ly = kotlin.math.sin(theta) * r
                Triple(lx, ly, theta + Math.PI.toFloat() * 0.5f)
            }

            WHEEL -> {
                // Two side-flank spinning circles: left flank (-Y) and right flank (+Y), opposite spin
                val leftCount = (count + 1) / 2
                val rightCount = count - leftCount
                val flankOffset = 70f
                val wheelRadius = 35f
                val spinSpeed = 3.2f
                if (index < leftCount) {
                    val theta = elapsedTime * spinSpeed + index * (2f * Math.PI.toFloat() / leftCount)
                    val lx = kotlin.math.cos(theta) * wheelRadius
                    val ly = -flankOffset + kotlin.math.sin(theta) * wheelRadius
                    Triple(lx, ly, theta + Math.PI.toFloat() * 0.5f)
                } else {
                    val rightIdx = index - leftCount
                    val safeRight = rightCount.coerceAtLeast(1)
                    val theta = -elapsedTime * spinSpeed + rightIdx * (2f * Math.PI.toFloat() / safeRight)
                    val lx = kotlin.math.cos(theta) * wheelRadius
                    val ly = flankOffset + kotlin.math.sin(theta) * wheelRadius
                    Triple(lx, ly, theta - Math.PI.toFloat() * 0.5f)
                }
            }
        }
    }
}

class VantDrone(
    val id: Int,
    val tier: VantTier,
    val config1Slots: MutableList<EquipmentItem?> = MutableList(tier.slotCount) { null },
    val config2Slots: MutableList<EquipmentItem?> = MutableList(tier.slotCount) { null },
    var durabilityPct: Int = 100,
    var worldX: Float = 0f,
    var worldY: Float = 0f,
    var facingRad: Float = 0f,
    var hasInitializedPosition: Boolean = false
) {
    fun slotsForConfig(configNumber: Int): MutableList<EquipmentItem?> =
        if (configNumber == 2) config2Slots else config1Slots
}

data class SavedVant(
    val id: Int,
    val tier: VantTier,
    val config1SlotIds: List<Int?>,
    val config2SlotIds: List<Int?>,
    val durabilityPct: Int = 100
)

data class VantSnapshot(
    val id: Int,
    val tier: VantTier,
    val activeSlots: List<EquipmentItem?>,
    val durabilityPct: Int = 100
)

enum class EquipmentCategory {
    LASER,
    SHIELD_GENERATOR,
    SECTOR_CHIP
}

enum class EquipmentItemId(
    val shortCode: String,
    val displayName: String,
    val category: EquipmentCategory,
    val tierLevel: Int,
    val shopCost: Int?, // Non-null only for Level 1 base shop items
    val damageBonus: Float = 0f,
    val shieldBonus: Float = 0f,
    val sectorTier: Int = 0, // 0=Sector 1 only, 1=Sector 2, 2=Sectors 2-3, 3=Sectors 2-4
    val accentHex: Long = 0xFF38BDF8
) {
    // Lasers (LF-1 sold in Shop; LF-2..LF-4 forged via 2-for-1 Fusion)
    LF_1("LF-1", "LF-1 Laser", EquipmentCategory.LASER, 1, 200, damageBonus = 12f, accentHex = 0xFFFF6B35),
    LF_2("LF-2", "LF-2 Laser", EquipmentCategory.LASER, 2, null, damageBonus = 28f, accentHex = 0xFFFF5252),
    LF_3("LF-3", "LF-3 Laser", EquipmentCategory.LASER, 3, null, damageBonus = 60f, accentHex = 0xFFF43F5E),
    LF_4("LF-4", "LF-4 Laser", EquipmentCategory.LASER, 4, null, damageBonus = 120f, accentHex = 0xFFE879F9),

    // Shield Generators (SG-1 sold in Shop; SG-2..SG-4 forged via 2-for-1 Fusion)
    SG_1("SG-1", "SG-1 Shield", EquipmentCategory.SHIELD_GENERATOR, 1, 250, shieldBonus = 80f, accentHex = 0xFF38BDF8),
    SG_2("SG-2", "SG-2 Shield", EquipmentCategory.SHIELD_GENERATOR, 2, null, shieldBonus = 180f, accentHex = 0xFF0EA5E9),
    SG_3("SG-3", "SG-3 Shield", EquipmentCategory.SHIELD_GENERATOR, 3, null, shieldBonus = 380f, accentHex = 0xFF6366F1),
    SG_4("SG-4", "SG-4 Shield", EquipmentCategory.SHIELD_GENERATOR, 4, null, shieldBonus = 750f, accentHex = 0xFFA855F7),

    // Sector Chips (Starter Chip Lv 1 granted by default; Mission 3/6/8 rewards & 2-for-1 Fusion only)
    CHIP_1("CH-1", "Chip Lv 1", EquipmentCategory.SECTOR_CHIP, 1, null, sectorTier = 0, accentHex = 0xFF94A3B8),
    CHIP_2("CH-2", "Chip Lv 2", EquipmentCategory.SECTOR_CHIP, 2, null, sectorTier = 1, accentHex = 0xFFFACC15),
    CHIP_3("CH-3", "Chip Lv 3", EquipmentCategory.SECTOR_CHIP, 3, null, sectorTier = 2, accentHex = 0xFFFB923C),
    CHIP_4("CH-4", "Chip Lv 4", EquipmentCategory.SECTOR_CHIP, 4, null, sectorTier = 3, accentHex = 0xFFC084FC);

    fun nextFusionResult(): EquipmentItemId? = when (this) {
        LF_1 -> LF_2
        LF_2 -> LF_3
        LF_3 -> LF_4
        SG_1 -> SG_2
        SG_2 -> SG_3
        SG_3 -> SG_4
        CHIP_1 -> CHIP_2
        CHIP_2 -> CHIP_3
        CHIP_3 -> CHIP_4
        else -> null
    }

    fun statSummary(): String = when (category) {
        EquipmentCategory.LASER -> "+${damageBonus.toInt()} DMG"
        EquipmentCategory.SHIELD_GENERATOR -> "+${shieldBonus.toInt()} SHD"
        EquipmentCategory.SECTOR_CHIP -> when (sectorTier) {
            0 -> "BASE CHIP"
            1 -> "SEC 2 SAFE"
            2 -> "SEC 2-3 SAFE"
            else -> "SEC 2-4 SAFE"
        }
    }
}

data class FusionRecipe(
    val source: EquipmentItemId,
    val result: EquipmentItemId
)

object FusionCatalog {
    val recipes: List<FusionRecipe> = listOf(
        FusionRecipe(EquipmentItemId.LF_1, EquipmentItemId.LF_2),
        FusionRecipe(EquipmentItemId.LF_2, EquipmentItemId.LF_3),
        FusionRecipe(EquipmentItemId.LF_3, EquipmentItemId.LF_4),
        FusionRecipe(EquipmentItemId.SG_1, EquipmentItemId.SG_2),
        FusionRecipe(EquipmentItemId.SG_2, EquipmentItemId.SG_3),
        FusionRecipe(EquipmentItemId.SG_3, EquipmentItemId.SG_4),
        FusionRecipe(EquipmentItemId.CHIP_1, EquipmentItemId.CHIP_2),
        FusionRecipe(EquipmentItemId.CHIP_2, EquipmentItemId.CHIP_3),
        FusionRecipe(EquipmentItemId.CHIP_3, EquipmentItemId.CHIP_4)
    )
}

enum class MissionGoalType {
    COLLECT_BONUS_BOXES,
    DESTROY_SCOUTS,
    DESTROY_HUNTERS,
    DESTROY_DESTROYERS
}

data class MissionSpec(
    val index: Int,
    val code: String,
    val sectorChainLabel: String,
    val description: String,
    val goalType: MissionGoalType,
    val targetCount: Int,
    val requiredLevel: Int,
    val isSectorFinal: Boolean,
    val rewardCredits: Int,
    val rewardXp: Int,
    val rewardChip: EquipmentItemId? = null
) {
    fun rewardSummary(): String {
        val parts = mutableListOf<String>()
        if (rewardChip != null) parts.add("1x ${rewardChip.displayName}")
        if (rewardCredits > 0) parts.add("$rewardCredits CR")
        if (rewardXp > 0) parts.add("$rewardXp XP")
        return parts.joinToString(" + ")
    }
}

object MissionCatalog {
    val all: List<MissionSpec> = listOf(
        // Sector 1 Chain (Unlock Sector 2)
        MissionSpec(
            index = 0,
            code = "M1",
            sectorChainLabel = "SECTOR 1 CHAIN",
            description = "Collect 3 Bonus Boxes",
            goalType = MissionGoalType.COLLECT_BONUS_BOXES,
            targetCount = 3,
            requiredLevel = 1,
            isSectorFinal = false,
            rewardCredits = 150,
            rewardXp = 50
        ),
        MissionSpec(
            index = 1,
            code = "M2",
            sectorChainLabel = "SECTOR 1 CHAIN",
            description = "Destroy 5 Scouts",
            goalType = MissionGoalType.DESTROY_SCOUTS,
            targetCount = 5,
            requiredLevel = 1,
            isSectorFinal = false,
            rewardCredits = 250,
            rewardXp = 100
        ),
        MissionSpec(
            index = 2,
            code = "M3",
            sectorChainLabel = "SECTOR 1 FINAL",
            description = "Destroy 8 Scouts",
            goalType = MissionGoalType.DESTROY_SCOUTS,
            targetCount = 8,
            requiredLevel = 2,
            isSectorFinal = true,
            rewardCredits = 300,
            rewardXp = 0,
            rewardChip = EquipmentItemId.CHIP_1
        ),
        // Sector 2 Chain (Unlock Sector 3)
        MissionSpec(
            index = 3,
            code = "M4",
            sectorChainLabel = "SECTOR 2 CHAIN",
            description = "Collect 5 Bonus Boxes",
            goalType = MissionGoalType.COLLECT_BONUS_BOXES,
            targetCount = 5,
            requiredLevel = 1,
            isSectorFinal = false,
            rewardCredits = 400,
            rewardXp = 150
        ),
        MissionSpec(
            index = 4,
            code = "M5",
            sectorChainLabel = "SECTOR 2 CHAIN",
            description = "Destroy 6 Hunters",
            goalType = MissionGoalType.DESTROY_HUNTERS,
            targetCount = 6,
            requiredLevel = 1,
            isSectorFinal = false,
            rewardCredits = 600,
            rewardXp = 250
        ),
        MissionSpec(
            index = 5,
            code = "M6",
            sectorChainLabel = "SECTOR 2 FINAL",
            description = "Destroy 10 Hunters",
            goalType = MissionGoalType.DESTROY_HUNTERS,
            targetCount = 10,
            requiredLevel = 5,
            isSectorFinal = true,
            rewardCredits = 800,
            rewardXp = 0,
            rewardChip = EquipmentItemId.CHIP_2
        ),
        // Sector 3 Chain (Unlock Sector 4)
        MissionSpec(
            index = 6,
            code = "M7",
            sectorChainLabel = "SECTOR 3 CHAIN",
            description = "Destroy 6 Destroyers",
            goalType = MissionGoalType.DESTROY_DESTROYERS,
            targetCount = 6,
            requiredLevel = 1,
            isSectorFinal = false,
            rewardCredits = 1200,
            rewardXp = 400
        ),
        MissionSpec(
            index = 7,
            code = "M8",
            sectorChainLabel = "SECTOR 3 FINAL",
            description = "Destroy 10 Destroyers",
            goalType = MissionGoalType.DESTROY_DESTROYERS,
            targetCount = 10,
            requiredLevel = 8,
            isSectorFinal = true,
            rewardCredits = 1500,
            rewardXp = 0,
            rewardChip = EquipmentItemId.CHIP_3
        )
    )
}

data class EquipmentItem(
    val instanceId: Int,
    val type: EquipmentItemId,
    val quality: CraftQuality = CraftQuality.STANDARD
) {
    val effectiveDamageBonus: Float
        get() = quality.applyBonus(type.damageBonus)

    val effectiveShieldBonus: Float
        get() = quality.applyBonus(type.shieldBonus)

    val effectivePrimaryStat: Float
        get() = when (type.category) {
            EquipmentCategory.LASER -> effectiveDamageBonus
            EquipmentCategory.SHIELD_GENERATOR -> effectiveShieldBonus
            EquipmentCategory.SECTOR_CHIP -> quality.applyBonus(type.sectorTier.toFloat())
        }
}

data class BoosterItem(
    val id: String,
    val displayName: String,
    val basePrimaryBonusPct: Float,
    val durationSec: Float,
    val requiresXenoCore: Boolean = false,
    val quality: CraftQuality = CraftQuality.STANDARD
) {
    val effectivePrimaryBonusPct: Float
        get() = quality.applyBonus(basePrimaryBonusPct)
}

enum class LootBoxKind {
    BONUS_BOX,
    CARGO_BOX
}

data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val tintType: Int
)

data class NebulaCloud(
    val x: Float,
    val y: Float,
    val radius: Float,
    val colorHex: Long
)

class PlayerState(
    var x: Float = 0f,
    var y: Float = 0f,
    var angleRad: Float = -Math.PI.toFloat() * 0.5f,
    var shipClass: ShipClass = ShipClass.INTERCEPTOR,
    var level: Int = 1,
    var xp: Int = 0,
    var hp: Float = CapsulePermanentConfig.BASE_HULL,
    var maxHp: Float = CapsulePermanentConfig.BASE_HULL,
    var shield: Float = CapsulePermanentConfig.BASE_SHIELD + CapsulePermanentConfig.PERMANENT_SHIELD_BONUS,
    var maxShield: Float = CapsulePermanentConfig.BASE_SHIELD + CapsulePermanentConfig.PERMANENT_SHIELD_BONUS,
    var speed: Float = CapsulePermanentConfig.SPEED,
    var laserDamage: Float = GameConfig.PLAYER_BASE_LASER_DAMAGE + CapsulePermanentConfig.PERMANENT_LASER_DAMAGE,
    var isMoving: Boolean = false,
    var isAutoFireEnabled: Boolean = false,
    var currentZone: SpaceZone = SpaceZone.SAFE_ZONE,
    var inSafeZone: Boolean = true,
    var inRadiationWarning: Boolean = false,
    var isTakingZoneRadiation: Boolean = false,
    var requiredChipWarning: String? = null,
    var hitFlash: Float = 0f,
    var shieldHitFlash: Float = 0f,
    var fireCooldown: Float = 0f,
    var rocketCooldown: Float = 0f,
    var radiationTickTimer: Float = 0f,
    var timeSinceDamaged: Float = GameConfig.SHIELD_REGEN_DELAY,
    var isAlive: Boolean = true,
    var thrusterPulse: Float = 0f,
    var thrustFactor: Float = 0f,
    var shipQuality: CraftQuality = CraftQuality.STANDARD,
    var shipRole: ShipRole = ShipRole.CAPSULE,
    var industrialType: IndustrialType? = null,
    var activeHullId: String = "CAPSULE_T1",
    var hullRadius: Float = CapsulePermanentConfig.HULL_RADIUS,
    var pilotSkills: Map<SkillType, SkillProgress> = defaultPilotSkills()
)

class AlienEntity(
    val id: Int,
    val tier: AlienTier,
    val isBoss: Boolean = false,
    var x: Float,
    var y: Float,
    var angleRad: Float,
    var hp: Float = if (isBoss) tier.maxHp * GameConfig.BOSS_HP_MULTIPLIER else tier.maxHp,
    val maxHp: Float = if (isBoss) tier.maxHp * GameConfig.BOSS_HP_MULTIPLIER else tier.maxHp,
    val radius: Float = if (isBoss) tier.radius * GameConfig.BOSS_SIZE_MULTIPLIER else tier.radius,
    var patrolTargetX: Float,
    var patrolTargetY: Float,
    var isAggro: Boolean = false,
    var fireCooldown: Float = 0.5f,
    var hitFlash: Float = 0f,
    var pulsePhase: Float = 0f
) {
    val callsign: String get() = if (isBoss) "BOSS ${tier.callsign}" else tier.callsign
    val laserDamage: Float get() = if (isBoss) tier.laserDamage * GameConfig.BOSS_DAMAGE_MULTIPLIER else tier.laserDamage
    val rewardCredits: Int get() = if (isBoss) tier.rewardCredits * GameConfig.BOSS_REWARD_MULTIPLIER else tier.rewardCredits
    val rewardXp: Int get() = if (isBoss) tier.rewardXp * GameConfig.BOSS_REWARD_MULTIPLIER else tier.rewardXp
}

class LootBoxEntity(
    val id: Int,
    val kind: LootBoxKind,
    var x: Float,
    var y: Float,
    val creditReward: Int,
    val shieldReward: Float,
    val xpReward: Int = 0,
    var pulsePhase: Float,
    var lifeRemaining: Float
)

class LaserBolt(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var angleRad: Float,
    val fromPlayer: Boolean,
    val targetAlienId: Int?,
    val damage: Float,
    val colorHex: Long,
    var lifeRemaining: Float
)

class HomingRocket(
    var x: Float,
    var y: Float,
    var angleRad: Float,
    val targetAlienId: Int,
    val damage: Float,
    var lifeRemaining: Float,
    var smokeTimer: Float = 0f
)

class ExplosionParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val radius: Float,
    var life: Float,
    val maxLife: Float,
    val colorHex: Long
)

class ShockwaveRing(
    val x: Float,
    val y: Float,
    val maxRadius: Float,
    var life: Float,
    val maxLife: Float,
    val colorHex: Long
)

class FloatingPopup(
    var x: Float,
    var y: Float,
    val text: String,
    var life: Float,
    val maxLife: Float,
    val colorRgb: Int, // 0xRRGGBB
    val textSize: Float = 14f
)

enum class ResourceCategory {
    SOLID,
    PLASMA,
    SALVAGE
}

enum class RefinedResourceType(
    val displayName: String,
    val category: ResourceCategory,
    val colorHex: Long
) {
    COBALT_ALLOY("Cobalt Alloy", ResourceCategory.SOLID, 0xFF38BDF8),
    AETHER_CRYSTAL("Aether Crystal", ResourceCategory.PLASMA, 0xFF0EA5E9),
    VERIDIAN_INGOT("Veridian Ingot", ResourceCategory.SOLID, 0xFF4ADE80),
    BIO_POLYMER("Bio Polymer", ResourceCategory.PLASMA, 0xFF34D399),
    CRIMSON_STEEL("Crimson Steel", ResourceCategory.SOLID, 0xFFFB7185),
    THERMAL_CORE("Thermal Core", ResourceCategory.PLASMA, 0xFFF97316),
    REFINED_YELLOW_CRYSTAL("Refined Yellow Crystal", ResourceCategory.PLASMA, 0xFFFACC15),
    XENO_ALLOY("Xeno Alloy", ResourceCategory.SALVAGE, 0xFFC084FC)
}

/**
 * v1.10 Resources & Categories enforcing the Clockwise City Refining Loop,
 * Nucleus multi-city refining, and Alien Salvage:
 * - C1 (refines in C2): STAR_COBALT (SOLID) -> COBALT_ALLOY, AETHER_QUARTZ (PLASMA) -> AETHER_CRYSTAL
 * - C2 (refines in C3): VERIDIAN_ORE (SOLID) -> VERIDIAN_INGOT, BIO_RESIN (PLASMA) -> BIO_POLYMER
 * - C3 (refines in C1): CRIMSON_IRON (SOLID) -> CRIMSON_STEEL, MAGMA_STONE (PLASMA) -> THERMAL_CORE
 * - Nucleus 0-0 (refines in C1, C2, C3): YELLOW_CRYSTAL (PLASMA) -> REFINED_YELLOW_CRYSTAL
 * - Alien Salvage (refines in C1, C2, C3): XENO_SALVAGE (SALVAGE) -> XENO_ALLOY, plus rare XENO_CORE (direct use)
 */
enum class ResourceType(
    val displayName: String,
    val refinedName: String,
    val category: ResourceCategory,
    val refinedResource: RefinedResourceType?,
    val tier: Int,
    val nativeSectorId: String,
    val refinesInSectorIds: Set<String>,
    val rawBasePrice: Int,
    val refinedBasePrice: Int,
    val colorHex: Long,
    val isYellowCrystal: Boolean = false,
    val isDirectUse: Boolean = false,
    val isRare: Boolean = false
) {
    // Native to City 1 (Blue / C1) -> Refines in City 2 (Green / C2)
    STAR_COBALT(
        displayName = "Star Cobalt",
        refinedName = "Cobalt Alloy",
        category = ResourceCategory.SOLID,
        refinedResource = RefinedResourceType.COBALT_ALLOY,
        tier = 1,
        nativeSectorId = "C1",
        refinesInSectorIds = setOf("C2"),
        rawBasePrice = 60,
        refinedBasePrice = 185,
        colorHex = 0xFF38BDF8
    ),
    AETHER_QUARTZ(
        displayName = "Aether Quartz",
        refinedName = "Aether Crystal",
        category = ResourceCategory.PLASMA,
        refinedResource = RefinedResourceType.AETHER_CRYSTAL,
        tier = 2,
        nativeSectorId = "C1",
        refinesInSectorIds = setOf("C2"),
        rawBasePrice = 115,
        refinedBasePrice = 340,
        colorHex = 0xFF0EA5E9
    ),

    // Native to City 2 (Green / C2) -> Refines in City 3 (Red / C3)
    VERIDIAN_ORE(
        displayName = "Veridian Ore",
        refinedName = "Veridian Ingot",
        category = ResourceCategory.SOLID,
        refinedResource = RefinedResourceType.VERIDIAN_INGOT,
        tier = 1,
        nativeSectorId = "C2",
        refinesInSectorIds = setOf("C3"),
        rawBasePrice = 60,
        refinedBasePrice = 185,
        colorHex = 0xFF4ADE80
    ),
    BIO_RESIN(
        displayName = "Bio Resin",
        refinedName = "Bio Polymer",
        category = ResourceCategory.PLASMA,
        refinedResource = RefinedResourceType.BIO_POLYMER,
        tier = 2,
        nativeSectorId = "C2",
        refinesInSectorIds = setOf("C3"),
        rawBasePrice = 115,
        refinedBasePrice = 340,
        colorHex = 0xFF34D399
    ),

    // Native to City 3 (Red / C3) -> Refines in City 1 (Blue / C1)
    CRIMSON_IRON(
        displayName = "Crimson Iron",
        refinedName = "Crimson Steel",
        category = ResourceCategory.SOLID,
        refinedResource = RefinedResourceType.CRIMSON_STEEL,
        tier = 1,
        nativeSectorId = "C3",
        refinesInSectorIds = setOf("C1"),
        rawBasePrice = 60,
        refinedBasePrice = 185,
        colorHex = 0xFFFB7185
    ),
    MAGMA_STONE(
        displayName = "Magma Stone",
        refinedName = "Thermal Core",
        category = ResourceCategory.PLASMA,
        refinedResource = RefinedResourceType.THERMAL_CORE,
        tier = 2,
        nativeSectorId = "C3",
        refinesInSectorIds = setOf("C1"),
        rawBasePrice = 115,
        refinedBasePrice = 340,
        colorHex = 0xFFF97316
    ),

    // Nucleus 0-0 & Yellow Ring -> Refines in C1, C2, or C3
    YELLOW_CRYSTAL(
        displayName = "Yellow Crystal",
        refinedName = "Refined Yellow Crystal",
        category = ResourceCategory.PLASMA,
        refinedResource = RefinedResourceType.REFINED_YELLOW_CRYSTAL,
        tier = 5,
        nativeSectorId = "0-0",
        refinesInSectorIds = setOf("C1", "C2", "C3"),
        rawBasePrice = 760,
        refinedBasePrice = 2450,
        colorHex = 0xFFFACC15,
        isYellowCrystal = true
    ),

    // Alien Salvage -> Refines in C1, C2, or C3
    XENO_SALVAGE(
        displayName = "Xeno Salvage",
        refinedName = "Xeno Alloy",
        category = ResourceCategory.SALVAGE,
        refinedResource = RefinedResourceType.XENO_ALLOY,
        tier = 3,
        nativeSectorId = "ALIEN",
        refinesInSectorIds = setOf("C1", "C2", "C3"),
        rawBasePrice = 240,
        refinedBasePrice = 720,
        colorHex = 0xFFC084FC
    ),

    // Rare Alien Salvage -> Direct use for Nucleus boosters (no refining)
    XENO_CORE(
        displayName = "Xeno Core",
        refinedName = "Xeno Core",
        category = ResourceCategory.SALVAGE,
        refinedResource = null,
        tier = 5,
        nativeSectorId = "ALIEN",
        refinesInSectorIds = emptySet(),
        rawBasePrice = 1200,
        refinedBasePrice = 1200,
        colorHex = 0xFFE879F9,
        isDirectUse = true,
        isRare = true
    );

    /** Primary refining sector ID for legacy single-city display/checks. */
    val refinesInSectorId: String
        get() = refinesInSectorIds.firstOrNull().orEmpty()

    val isRefinable: Boolean
        get() = !isDirectUse && refinedResource != null && refinesInSectorIds.isNotEmpty()

    fun canRefineIn(sectorId: String): Boolean =
        isRefinable && sectorId in refinesInSectorIds

    companion object {
        const val RAW_REQUIRED_PER_REFINE = 2

        fun forRegionAndTier(nativeCityId: String, tier: Int): ResourceType {
            val usePlasma = tier % 2 == 0
            return when (nativeCityId) {
                "C1" -> if (usePlasma) AETHER_QUARTZ else STAR_COBALT
                "C2" -> if (usePlasma) BIO_RESIN else VERIDIAN_ORE
                "C3" -> if (usePlasma) MAGMA_STONE else CRIMSON_IRON
                "0-0" -> YELLOW_CRYSTAL
                else -> STAR_COBALT
            }
        }
    }
}

class ResourceNodeEntity(
    val id: Int,
    val resourceType: ResourceType,
    var x: Float,
    var y: Float,
    val yieldAmount: Int,
    val radius: Float,
    var rotationRad: Float,
    var pulsePhase: Float,
    var harvestProgress: Float = 0f,
    val maxCharges: Int = DEFAULT_MAX_CHARGES,
    var charges: Int = maxCharges
) {
    val isDepleted: Boolean get() = charges <= 0

    companion object {
        const val MIN_CHARGES = 4
        const val MAX_CHARGES = 5
        const val DEFAULT_MAX_CHARGES = 4
    }
}

/**
 * Salvageable wreck dropped by destroyed alien ships (2–4 charges with expiration timer).
 */
class AlienWreck(
    val id: Int,
    val sourceTier: AlienTier,
    val fromBoss: Boolean = false,
    var x: Float,
    var y: Float,
    var rotationRad: Float = 0f,
    val radius: Float = if (fromBoss) 28f else 22f,
    val maxCharges: Int = DEFAULT_MAX_CHARGES,
    var charges: Int = maxCharges,
    var expirationTimerSec: Float = DEFAULT_LIFETIME_SEC,
    val maxExpirationSec: Float = DEFAULT_LIFETIME_SEC,
    var salvageProgress: Float = 0f,
    var pulsePhase: Float = 0f
) {
    val isExpired: Boolean get() = expirationTimerSec <= 0f
    val isDepleted: Boolean get() = charges <= 0 || isExpired
    val remainingLifetimeRatio: Float
        get() = if (maxExpirationSec > 0f) (expirationTimerSec / maxExpirationSec).coerceIn(0f, 1f) else 0f

    companion object {
        const val MIN_CHARGES = 2
        const val MAX_CHARGES = 4
        const val DEFAULT_MAX_CHARGES = 3
        const val DEFAULT_LIFETIME_SEC = 90f
    }
}

enum class StationKind {
    REFINERY,
    LOCAL_MARKET,
    CRAFTING
}

enum class CraftingRecipeId(
    val displayName: String,
    val effectSummary: String,
    val refinedCost: Int,
    val minRefinedTier: Int,
    val accentHex: Long,
    val resultEquipment: EquipmentItemId? = null
) {
    HULL_REPAIR_KIT(
        displayName = "Hull Nanite Repair Kit",
        effectSummary = "Restores 100% Hull & Shield immediately",
        refinedCost = 1,
        minRefinedTier = 1,
        accentHex = 0xFF34D399
    ),
    PLASMA_AMMO_PACK(
        displayName = "Overcharged Plasma Ammo (+25)",
        effectSummary = "+25 supercharged rounds (+35% Laser DMG)",
        refinedCost = 1,
        minRefinedTier = 1,
        accentHex = 0xFFFACC15
    ),
    SHIELD_CHIP_LV1(
        displayName = "Sector Chip Lv 1",
        effectSummary = "Crafts 1× Chip Lv 1 into Hangar inventory",
        refinedCost = 2,
        minRefinedTier = 1,
        accentHex = 0xFF94A3B8,
        resultEquipment = EquipmentItemId.CHIP_1
    ),
    SHIELD_CHIP_LV2(
        displayName = "Sector Chip Lv 2",
        effectSummary = "Crafts 1× Chip Lv 2 (Moderate Zone protection)",
        refinedCost = 2,
        minRefinedTier = 2,
        accentHex = 0xFFFACC15,
        resultEquipment = EquipmentItemId.CHIP_2
    ),
    LASER_LF1(
        displayName = "LF-1 Laser Cannon",
        effectSummary = "Crafts 1× LF-1 Laser (+12 DMG) into Hangar",
        refinedCost = 2,
        minRefinedTier = 1,
        accentHex = 0xFFFF6B35,
        resultEquipment = EquipmentItemId.LF_1
    ),
    SHIELD_SG1(
        displayName = "SG-1 Shield Generator",
        effectSummary = "Crafts 1× SG-1 Shield (+80 SHD) into Hangar",
        refinedCost = 2,
        minRefinedTier = 1,
        accentHex = 0xFF38BDF8,
        resultEquipment = EquipmentItemId.SG_1
    )
}

data class SectorStation(
    val kind: StationKind,
    val sectorId: String,
    val name: String,
    val worldX: Float,
    val worldY: Float,
    val interactRadius: Float = 165f,
    val accentColorHex: Long
)

data class MarketResourceListing(
    val resourceType: ResourceType,
    val isRefined: Boolean,
    val buyPrice: Int,
    val sellPrice: Int,
    val localStock: Int
)

data class MarketEquipmentListing(
    val itemId: EquipmentItemId,
    val buyPrice: Int,
    val localStock: Int
)

data class LocalMarketSnapshot(
    val sectorId: String,
    val marketName: String,
    val isBlackMarket: Boolean,
    val resourceListings: List<MarketResourceListing>,
    val equipmentListings: List<MarketEquipmentListing>
)

data class SaveState(
    val credits: Int,
    val kills: Int,
    val level: Int,
    val xp: Int,
    val activeConfig: Int,
    val activeShipClass: ShipClass,
    val ownedShipClasses: Set<ShipClass>,
    val nextItemInstanceId: Int,
    val nextVantId: Int = 1,
    val ownedItems: List<EquipmentItem>,
    val config1LaserIds: List<Int?>,
    val config1ShieldIds: List<Int?>,
    val config2LaserIds: List<Int?>,
    val config2ShieldIds: List<Int?>,
    val ownedVants: List<SavedVant> = emptyList(),
    val ownedFormations: Set<DroneFormation> = setOf(DroneFormation.NORMAL),
    val activeFormation: DroneFormation = DroneFormation.NORMAL,
    val equippedSectorChipId: Int?,
    val currentMissionIndex: Int,
    val currentMissionProgress: Int,
    val pilotName: String? = null,
    val companyId: String? = null,
    val currentSectorId: String = GalaxyMapGraph.DEFAULT_SECTOR_ID,
    val rawCargo: Map<ResourceType, Int> = emptyMap(),
    val refinedCargo: Map<ResourceType, Int> = emptyMap(),
    val overchargedAmmo: Int = 0,
    val activeShipQuality: CraftQuality = CraftQuality.STANDARD,
    val pilotSkills: Map<SkillType, SkillProgress> = defaultPilotSkills(),
    val craftedHangarShips: List<CraftedShipRecord> = emptyList(),
    val craftedInventoryItems: List<CraftedItemRecord> = emptyList(),
    val isFlyingCapsule: Boolean = true,
    val activeCraftedShipId: Int? = null,
    val basicVendorItems: Map<BasicVendorItemId, Int> = emptyMap()
)

data class MissionSnapshot(
    val currentMissionIndex: Int = 0,
    val currentMissionProgress: Int = 0,
    val activeMission: MissionSpec? = MissionCatalog.all.firstOrNull(),
    val isLevelLocked: Boolean = false,
    val isReadyToClaim: Boolean = false,
    val allCompleted: Boolean = false
)

data class EquipmentSnapshot(
    val activeConfig: Int = 1,
    val activeShipClass: ShipClass = ShipClass.INTERCEPTOR,
    val ownedShipClasses: Set<ShipClass> = emptySet(),
    val isFlyingCapsule: Boolean = true,
    val activeCraftedShipId: Int? = null,
    val activeShipRole: ShipRole = ShipRole.CAPSULE,
    val activeIndustrialType: IndustrialType? = null,
    val activeShipDisplayName: String = "Pilot Capsule",
    val equippedSectorChip: EquipmentItem? = null,
    val laserSlots: List<EquipmentItem?> = List(ShipClass.INTERCEPTOR.laserSlotCount) { null },
    val shieldSlots: List<EquipmentItem?> = List(ShipClass.INTERCEPTOR.shieldSlotCount) { null },
    val vants: List<VantSnapshot> = emptyList(),
    val flaxVantCount: Int = 0,
    val irisVantCount: Int = 0,
    val ownedFormations: Set<DroneFormation> = setOf(DroneFormation.NORMAL),
    val activeFormation: DroneFormation = DroneFormation.NORMAL,
    val inventory: List<EquipmentItem> = emptyList(),
    val ownedItemCounts: Map<EquipmentItemId, Int> = emptyMap(),
    val basicVendorItemCounts: Map<BasicVendorItemId, Int> = emptyMap(),
    val hasAnyReadyFusion: Boolean = false,
    val totalDamage: Int = (GameConfig.PLAYER_BASE_LASER_DAMAGE + CapsulePermanentConfig.PERMANENT_LASER_DAMAGE).toInt(),
    val totalMaxShield: Int = (CapsulePermanentConfig.BASE_SHIELD + CapsulePermanentConfig.PERMANENT_SHIELD_BONUS).toInt(),
    val totalSpeed: Int = CapsulePermanentConfig.SPEED.toInt(),
    val totalMaxHull: Int = CapsulePermanentConfig.BASE_HULL.toInt(),
    val activeShipQuality: CraftQuality = CraftQuality.STANDARD,
    val totalCargoCapacity: Int = CapsulePermanentConfig.CARGO_CAPACITY
)

data class HudSnapshot(
    val credits: Int = 0,
    val kills: Int = 0,
    val playerLevel: Int = 1,
    val playerXp: Int = 0,
    val playerXpToNext: Int = GameConfig.xpRequiredForLevel(1),
    val activeConfig: Int = 1,
    val activeShipClass: ShipClass = ShipClass.INTERCEPTOR,
    val isFlyingCapsule: Boolean = true,
    val activeCraftedShipId: Int? = null,
    val activeShipDisplayName: String = "Pilot Capsule",
    val activeFormation: DroneFormation = DroneFormation.NORMAL,
    val formationCooldownRemaining: Float = 0f,
    val ownedFormationCount: Int = 1,
    val isAutopilotActive: Boolean = false,
    val autopilotTargetX: Int = 0,
    val autopilotTargetY: Int = 0,
    val autopilotTargetLabel: String = "",
    val autopilotEtaSeconds: Int = 0,
    val autopilotNoticeText: String? = null,
    val isTacticalMapOpen: Boolean = false,
    val isGalaxyMapOpen: Boolean = false,
    val currentSectorId: String = GalaxyMapGraph.DEFAULT_SECTOR_ID,
    val plottedRoute: List<String> = emptyList(),
    val nextRouteHopSectorId: String? = null,
    val playerHp: Int = CapsulePermanentConfig.BASE_HULL.toInt(),
    val playerMaxHp: Int = CapsulePermanentConfig.BASE_HULL.toInt(),
    val playerShield: Int = (CapsulePermanentConfig.BASE_SHIELD + CapsulePermanentConfig.PERMANENT_SHIELD_BONUS).toInt(),
    val playerMaxShield: Int = (CapsulePermanentConfig.BASE_SHIELD + CapsulePermanentConfig.PERMANENT_SHIELD_BONUS).toInt(),
    val coordX: Int = 0,
    val coordY: Int = 0,
    val activeMap: GameMap = GameMap.MAIN_MAP,
    val isNearJumpGate: Boolean = false,
    val jumpGateLabel: String = "GATE: 1-1",
    val nearestJumpTargetId: String? = null,
    val pilotName: String? = null,
    val pilotCompany: PilotCompany? = null,
    val currentZone: SpaceZone = SpaceZone.SAFE_ZONE,
    val isAutoFireEnabled: Boolean = false,
    val rocketCooldownRemaining: Float = 0f,
    val inSafeZone: Boolean = true,
    val lockedTargetCallsign: String? = null,
    val lockedTargetHp: Int = 0,
    val lockedTargetMaxHp: Int = 100,
    val lockedTargetInRange: Boolean = false,
    val inRadiationWarning: Boolean = false,
    val isTakingZoneRadiation: Boolean = false,
    val requiredChipWarning: String? = null,
    val isAlive: Boolean = true,
    val isPaused: Boolean = false,
    val isHangarOpen: Boolean = false,
    val nearestStationKind: StationKind? = null,
    val nearestStationName: String? = null,
    val activeStationOverlay: StationKind? = null,
    val activeLocalMarket: LocalMarketSnapshot? = null,
    val rawCargo: Map<ResourceType, Int> = emptyMap(),
    val refinedCargo: Map<ResourceType, Int> = emptyMap(),
    val overchargedAmmo: Int = 0,
    val miningNodeName: String? = null,
    val miningProgress: Float = 0f,
    val equipment: EquipmentSnapshot = EquipmentSnapshot(),
    val mission: MissionSnapshot = MissionSnapshot(),
    val pilotSkills: Map<SkillType, SkillProgress> = defaultPilotSkills(),
    val craftedHangarShips: List<CraftedShipRecord> = emptyList(),
    val craftedInventoryItems: List<CraftedItemRecord> = emptyList(),
    val lastDeathSummary: DeathSummary? = null,
    val activeSectorWreckCount: Int = 0,
    val isAdminMode: Boolean = false
)
