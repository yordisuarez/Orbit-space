package com.example.game

object GameConfig {
    const val MAP_RADIUS = 8000f
    const val SAFE_ZONE_RADIUS = 400f
    const val ZONE_1_MAX_RADIUS = 2000f
    const val ZONE_2_MAX_RADIUS = 4000f
    const val ZONE_3_MAX_RADIUS = 6000f
    const val ZONE_4_MAX_RADIUS = 8000f

    const val BORDER_PADDING = 30f
    const val RADIATION_WARNING_DISTANCE = 220f
    const val RADIATION_TICK_INTERVAL = 0.65f

    // Base bonuses independent of ship class
    const val PLAYER_HP_PER_LEVEL = 15f
    const val PLAYER_BASE_MAX_SHIELD = 40f
    const val PLAYER_BASE_LASER_DAMAGE = 6f

    // VANT (Drone) limits & pricing
    const val MAX_VANTS = 8
    const val FLAX_VANT_COST = 600

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
    const val BONUS_BOX_COUNT = 45
    const val LOOT_COLLECT_RADIUS = 42f
    const val CARGO_LIFETIME_SEC = 50f

    const val STAR_COUNT = 1200
    const val NEBULA_COUNT = 36

    const val TAP_LOCK_RADIUS = 74f
    const val CAMERA_SMOOTHNESS = 8.5f
    const val JOYSTICK_DEADZONE = 0.12f

    fun xpRequiredForLevel(level: Int): Int {
        val lv = level.coerceAtLeast(1)
        return 180 * lv + 85 * (lv - 1) * (lv - 1)
    }
}

enum class ShipClass(
    val displayName: String,
    val roleLabel: String,
    val cost: Int,
    val speed: Float,
    val baseHull: Float,
    val laserSlotCount: Int,
    val shieldSlotCount: Int,
    val hullRadius: Float,
    val primaryColorHex: Long,
    val accentColorHex: Long
) {
    INTERCEPTOR(
        displayName = "Interceptor",
        roleLabel = "STARTER • FAST",
        cost = 0,
        speed = 340f,
        baseHull = 140f,
        laserSlotCount = 4,
        shieldSlotCount = 3,
        hullRadius = 20f,
        primaryColorHex = 0xFF1E293B,
        accentColorHex = 0xFF00E5FF
    ),
    BATTLECRUISER(
        displayName = "Battlecruiser",
        roleLabel = "BALANCED",
        cost = 1500,
        speed = 265f,
        baseHull = 260f,
        laserSlotCount = 6,
        shieldSlotCount = 6,
        hullRadius = 24f,
        primaryColorHex = 0xFF172554,
        accentColorHex = 0xFFFBBF24
    ),
    DREADNOUGHT(
        displayName = "Dreadnought",
        roleLabel = "HEAVY TANK",
        cost = 4000,
        speed = 205f,
        baseHull = 480f,
        laserSlotCount = 8,
        shieldSlotCount = 10,
        hullRadius = 29f,
        primaryColorHex = 0xFF064E3B,
        accentColorHex = 0xFF34D399
    )
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
        activeCount = 10,
        radius = 17f,
        patrolSpeed = 118f,
        chaseSpeed = 190f,
        attackRange = 360f,
        fireInterval = 1.25f,
        laserSpeed = 520f,
        primaryColorHex = 0xFF4ADE80,
        accentColorHex = 0xFF84CC16,
        minimapDotRadius = 2.6f
    ),
    HUNTER(
        sectorIndex = 2,
        callsign = "HUNTER",
        minRadius = GameConfig.ZONE_1_MAX_RADIUS,
        maxRadius = GameConfig.ZONE_2_MAX_RADIUS,
        activeCount = 12,
        radius = 22f,
        patrolSpeed = 96f,
        chaseSpeed = 160f,
        attackRange = 400f,
        fireInterval = 1.08f,
        laserSpeed = 550f,
        primaryColorHex = 0xFFFACC15,
        accentColorHex = 0xFFF59E0B,
        minimapDotRadius = 2.9f
    ),
    DESTROYER(
        sectorIndex = 3,
        callsign = "DESTROYER",
        minRadius = GameConfig.ZONE_2_MAX_RADIUS,
        maxRadius = GameConfig.ZONE_3_MAX_RADIUS,
        activeCount = 10,
        radius = 28f,
        patrolSpeed = 80f,
        chaseSpeed = 135f,
        attackRange = 440f,
        fireInterval = 0.96f,
        laserSpeed = 580f,
        primaryColorHex = 0xFFFB923C,
        accentColorHex = 0xFFEF4444,
        minimapDotRadius = 3.3f
    ),
    DREADNOUGHT_BOSS(
        sectorIndex = 4,
        callsign = "DREADNOUGHT",
        minRadius = GameConfig.ZONE_3_MAX_RADIUS,
        maxRadius = GameConfig.ZONE_4_MAX_RADIUS,
        activeCount = 6,
        radius = 40f,
        patrolSpeed = 62f,
        chaseSpeed = 110f,
        attackRange = 500f,
        fireInterval = 0.85f,
        laserSpeed = 620f,
        primaryColorHex = 0xFFC084FC,
        accentColorHex = 0xFFE11D48,
        minimapDotRadius = 4.2f
    );

    val maxHp: Float get() = SectorAlienScaling.hpForSector(sectorIndex)
    val laserDamage: Float get() = SectorAlienScaling.damageForSector(sectorIndex)
    val rewardCredits: Int get() = SectorAlienScaling.creditsForSector(sectorIndex)
    val rewardXp: Int get() = SectorAlienScaling.xpForSector(sectorIndex)
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

class VantDrone(
    val id: Int,
    val tier: VantTier,
    val config1Slots: MutableList<EquipmentItem?> = MutableList(tier.slotCount) { null },
    val config2Slots: MutableList<EquipmentItem?> = MutableList(tier.slotCount) { null }
) {
    fun slotsForConfig(configNumber: Int): MutableList<EquipmentItem?> =
        if (configNumber == 2) config2Slots else config1Slots
}

data class SavedVant(
    val id: Int,
    val tier: VantTier,
    val config1SlotIds: List<Int?>,
    val config2SlotIds: List<Int?>
)

data class VantSnapshot(
    val id: Int,
    val tier: VantTier,
    val activeSlots: List<EquipmentItem?>
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

    // Sector Chips (Chip Lv 1 sold in Shop & Mission 3; Lv 2..4 forged via 2-for-1 Fusion & final missions)
    CHIP_1("CH-1", "Chip Lv 1", EquipmentCategory.SECTOR_CHIP, 1, 300, sectorTier = 0, accentHex = 0xFF94A3B8),
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
    val type: EquipmentItemId
)

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
    var hp: Float = ShipClass.INTERCEPTOR.baseHull,
    var maxHp: Float = ShipClass.INTERCEPTOR.baseHull,
    var shield: Float = GameConfig.PLAYER_BASE_MAX_SHIELD + EquipmentItemId.SG_1.shieldBonus,
    var maxShield: Float = GameConfig.PLAYER_BASE_MAX_SHIELD + EquipmentItemId.SG_1.shieldBonus,
    var speed: Float = ShipClass.INTERCEPTOR.speed,
    var laserDamage: Float = GameConfig.PLAYER_BASE_LASER_DAMAGE + EquipmentItemId.LF_1.damageBonus,
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
    var thrusterPulse: Float = 0f
)

class AlienEntity(
    val id: Int,
    val tier: AlienTier,
    var x: Float,
    var y: Float,
    var angleRad: Float,
    var hp: Float = tier.maxHp,
    val maxHp: Float = tier.maxHp,
    val radius: Float = tier.radius,
    var patrolTargetX: Float,
    var patrolTargetY: Float,
    var isAggro: Boolean = false,
    var fireCooldown: Float = 0.5f,
    var hitFlash: Float = 0f,
    var pulsePhase: Float = 0f
) {
    val callsign: String get() = tier.callsign
    val rewardCredits: Int get() = tier.rewardCredits
    val rewardXp: Int get() = tier.rewardXp
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
    val equippedSectorChipId: Int?,
    val currentMissionIndex: Int,
    val currentMissionProgress: Int
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
    val ownedShipClasses: Set<ShipClass> = setOf(ShipClass.INTERCEPTOR),
    val equippedSectorChip: EquipmentItem? = null,
    val laserSlots: List<EquipmentItem?> = List(ShipClass.INTERCEPTOR.laserSlotCount) { null },
    val shieldSlots: List<EquipmentItem?> = List(ShipClass.INTERCEPTOR.shieldSlotCount) { null },
    val vants: List<VantSnapshot> = emptyList(),
    val flaxVantCount: Int = 0,
    val irisVantCount: Int = 0,
    val inventory: List<EquipmentItem> = emptyList(),
    val ownedItemCounts: Map<EquipmentItemId, Int> = emptyMap(),
    val hasAnyReadyFusion: Boolean = false,
    val totalDamage: Int = (GameConfig.PLAYER_BASE_LASER_DAMAGE + EquipmentItemId.LF_1.damageBonus).toInt(),
    val totalMaxShield: Int = (GameConfig.PLAYER_BASE_MAX_SHIELD + EquipmentItemId.SG_1.shieldBonus).toInt(),
    val totalSpeed: Int = ShipClass.INTERCEPTOR.speed.toInt(),
    val totalMaxHull: Int = ShipClass.INTERCEPTOR.baseHull.toInt()
)

data class HudSnapshot(
    val credits: Int = 0,
    val kills: Int = 0,
    val playerLevel: Int = 1,
    val playerXp: Int = 0,
    val playerXpToNext: Int = GameConfig.xpRequiredForLevel(1),
    val activeConfig: Int = 1,
    val activeShipClass: ShipClass = ShipClass.INTERCEPTOR,
    val playerHp: Int = ShipClass.INTERCEPTOR.baseHull.toInt(),
    val playerMaxHp: Int = ShipClass.INTERCEPTOR.baseHull.toInt(),
    val playerShield: Int = (GameConfig.PLAYER_BASE_MAX_SHIELD + EquipmentItemId.SG_1.shieldBonus).toInt(),
    val playerMaxShield: Int = (GameConfig.PLAYER_BASE_MAX_SHIELD + EquipmentItemId.SG_1.shieldBonus).toInt(),
    val coordX: Int = 0,
    val coordY: Int = 0,
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
    val equipment: EquipmentSnapshot = EquipmentSnapshot(),
    val mission: MissionSnapshot = MissionSnapshot()
)
