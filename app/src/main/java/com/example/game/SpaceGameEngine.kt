package com.example.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class SpaceGameEngine(
    private val random: Random = Random.Default,
    private val onSaveRequested: ((SaveState) -> Unit)? = null
) {
    val player = PlayerState()
    val aliens = mutableListOf<AlienEntity>()
    val lootBoxes = mutableListOf<LootBoxEntity>()
    val lasers = mutableListOf<LaserBolt>()
    val rockets = mutableListOf<HomingRocket>()
    val particles = mutableListOf<ExplosionParticle>()
    val shockwaves = mutableListOf<ShockwaveRing>()
    val popups = mutableListOf<FloatingPopup>()

    private val mainStars: List<Star> = generateCircularStars()
    private val x1Stars: List<Star> = generateSquareX1Stars()
    private val mainNebulae: List<NebulaCloud> = generateCircularNebulae()
    private val x1Nebulae: List<NebulaCloud> = generateSquareX1Nebulae()

    val stars: List<Star>
        get() = if (activeMap == GameMap.MAP_X1) x1Stars else mainStars

    val nebulae: List<NebulaCloud>
        get() = if (activeMap == GameMap.MAP_X1) x1Nebulae else mainNebulae

    var activeMap: GameMap = GameMap.MAIN_MAP
        private set
    var warpFlashTimer: Float = 0f
        private set

    // Owned Ship Classes & Active Ship Class
    val ownedShipClasses: MutableSet<ShipClass> = mutableSetOf(ShipClass.INTERCEPTOR)
    var activeShipClass: ShipClass = ShipClass.INTERCEPTOR
        private set

    // Master owned equipment list + Config 1 & Config 2 slot layouts + Dedicated Sector Chip slot
    val ownedItems: MutableList<EquipmentItem> = mutableListOf()
    val config1LaserSlots: MutableList<EquipmentItem?> =
        MutableList(ShipClass.INTERCEPTOR.laserSlotCount) { null }
    val config1ShieldSlots: MutableList<EquipmentItem?> =
        MutableList(ShipClass.INTERCEPTOR.shieldSlotCount) { null }
    val config2LaserSlots: MutableList<EquipmentItem?> =
        MutableList(ShipClass.INTERCEPTOR.laserSlotCount) { null }
    val config2ShieldSlots: MutableList<EquipmentItem?> =
        MutableList(ShipClass.INTERCEPTOR.shieldSlotCount) { null }

    var activeConfig: Int = 1
        private set
    var equippedSectorChip: EquipmentItem? = null
        private set
    val ownedVants: MutableList<VantDrone> = mutableListOf()

    // Classic DarkOrbit Drone Formations
    val ownedFormations: MutableSet<DroneFormation> = mutableSetOf(DroneFormation.NORMAL)
    var activeFormation: DroneFormation = DroneFormation.NORMAL
        private set
    var formationSwitchCooldown: Float = 0f
        private set

    // Interactive Tactical Map & Tap-to-Fly Autopilot
    var isTacticalMapOpen: Boolean = false
        private set
    var autopilotTargetX: Float? = null
        private set
    var autopilotTargetY: Float? = null
        private set

    // Sequential Mission Chain State
    var currentMissionIndex: Int = 0
        private set
    var currentMissionProgress: Int = 0
        private set

    val equippedChipTier: Int
        get() = equippedSectorChip?.type?.sectorTier ?: 0

    val laserSlots: MutableList<EquipmentItem?>
        get() = if (activeConfig == 2) config2LaserSlots else config1LaserSlots

    val shieldSlots: MutableList<EquipmentItem?>
        get() = if (activeConfig == 2) config2ShieldSlots else config1ShieldSlots

    var cameraX: Float = player.x
        private set
    var cameraY: Float = player.y
        private set
    var screenShake: Float = 0f
        private set

    var lockedAlienId: Int? = null
        private set
    var credits: Int = 0
        private set
    var kills: Int = 0
        private set
    var isPaused: Boolean = false
        private set
    var isHangarOpen: Boolean = false
        private set
    var elapsedTime: Float = 0f
        private set

    var moveInputX: Float = 0f
        private set
    var moveInputY: Float = 0f
        private set
    var aimInputX: Float = 0f
        private set
    var aimInputY: Float = 0f
        private set
    var isAiming: Boolean = false
        private set

    private var nextAlienId = 1
    private var nextLootId = 1
    private var nextItemInstanceId = 1
    private var nextVantId = 1

    init {
        resetSector(keepProgress = false, triggerSave = false)
    }

    fun requestSave() {
        onSaveRequested?.invoke(exportSaveState())
    }

    fun exportSaveState(): SaveState {
        return SaveState(
            credits = credits,
            kills = kills,
            level = player.level,
            xp = player.xp,
            activeConfig = activeConfig,
            activeShipClass = activeShipClass,
            ownedShipClasses = ownedShipClasses.toSet(),
            nextItemInstanceId = nextItemInstanceId,
            nextVantId = nextVantId,
            ownedItems = ownedItems.toList(),
            config1LaserIds = config1LaserSlots.map { it?.instanceId },
            config1ShieldIds = config1ShieldSlots.map { it?.instanceId },
            config2LaserIds = config2LaserSlots.map { it?.instanceId },
            config2ShieldIds = config2ShieldSlots.map { it?.instanceId },
            ownedVants = ownedVants.map { vant ->
                SavedVant(
                    id = vant.id,
                    tier = vant.tier,
                    config1SlotIds = vant.config1Slots.map { it?.instanceId },
                    config2SlotIds = vant.config2Slots.map { it?.instanceId }
                )
            },
            ownedFormations = ownedFormations.toSet(),
            activeFormation = activeFormation,
            equippedSectorChipId = equippedSectorChip?.instanceId,
            currentMissionIndex = currentMissionIndex,
            currentMissionProgress = currentMissionProgress
        )
    }

    fun restoreSaveState(state: SaveState) {
        credits = state.credits.coerceAtLeast(0)
        kills = state.kills.coerceAtLeast(0)
        player.level = state.level.coerceAtLeast(1)
        player.xp = state.xp.coerceAtLeast(0)
        activeConfig = if (state.activeConfig == 2) 2 else 1

        ownedShipClasses.clear()
        ownedShipClasses.add(ShipClass.INTERCEPTOR)
        ownedShipClasses.addAll(state.ownedShipClasses)
        activeShipClass = if (state.activeShipClass in ownedShipClasses) {
            state.activeShipClass
        } else {
            ShipClass.INTERCEPTOR
        }
        player.shipClass = activeShipClass

        ownedItems.clear()
        ownedItems.addAll(state.ownedItems)
        val byId = ownedItems.associateBy { it.instanceId }
        nextItemInstanceId = max(
            state.nextItemInstanceId,
            (ownedItems.maxOfOrNull { it.instanceId } ?: 0) + 1
        )

        resizeSlotListsForShip(activeShipClass)
        val c1UsedIds = HashSet<Int>()
        val c2UsedIds = HashSet<Int>()

        for (i in config1LaserSlots.indices) {
            val found = state.config1LaserIds.getOrNull(i)?.let { byId[it] }
                ?.takeIf { it.type.category == EquipmentCategory.LASER && c1UsedIds.add(it.instanceId) }
            config1LaserSlots[i] = found
        }
        for (i in config1ShieldSlots.indices) {
            val found = state.config1ShieldIds.getOrNull(i)?.let { byId[it] }
                ?.takeIf { it.type.category == EquipmentCategory.SHIELD_GENERATOR && c1UsedIds.add(it.instanceId) }
            config1ShieldSlots[i] = found
        }
        for (i in config2LaserSlots.indices) {
            val found = state.config2LaserIds.getOrNull(i)?.let { byId[it] }
                ?.takeIf { it.type.category == EquipmentCategory.LASER && c2UsedIds.add(it.instanceId) }
            config2LaserSlots[i] = found
        }
        for (i in config2ShieldSlots.indices) {
            val found = state.config2ShieldIds.getOrNull(i)?.let { byId[it] }
                ?.takeIf { it.type.category == EquipmentCategory.SHIELD_GENERATOR && c2UsedIds.add(it.instanceId) }
            config2ShieldSlots[i] = found
        }

        ownedVants.clear()
        for (savedVant in state.ownedVants.take(GameConfig.MAX_OWNED_VANTS_FOR_FUSION)) {
            val drone = VantDrone(id = savedVant.id, tier = savedVant.tier)
            for (i in drone.config1Slots.indices) {
                val found = savedVant.config1SlotIds.getOrNull(i)?.let { byId[it] }
                    ?.takeIf { isVantCompatibleItem(it.type) && c1UsedIds.add(it.instanceId) }
                drone.config1Slots[i] = found
            }
            for (i in drone.config2Slots.indices) {
                val found = savedVant.config2SlotIds.getOrNull(i)?.let { byId[it] }
                    ?.takeIf { isVantCompatibleItem(it.type) && c2UsedIds.add(it.instanceId) }
                drone.config2Slots[i] = found
            }
            ownedVants.add(drone)
        }
        clearReserveVantSlots()
        nextVantId = max(
            state.nextVantId,
            (ownedVants.maxOfOrNull { it.id } ?: 0) + 1
        )

        ownedFormations.clear()
        ownedFormations.add(DroneFormation.NORMAL)
        ownedFormations.addAll(state.ownedFormations)
        activeFormation = if (state.activeFormation in ownedFormations) {
            state.activeFormation
        } else {
            DroneFormation.NORMAL
        }
        formationSwitchCooldown = 0f

        equippedSectorChip = state.equippedSectorChipId?.let { byId[it] }
            ?.takeIf { it.type.category == EquipmentCategory.SECTOR_CHIP }

        // Failsafe: ensure player always has at least 1x Chip Lv 1 if save has no chips equipped or in inventory
        val hasAnyChip = equippedSectorChip != null ||
            ownedItems.any { it.type.category == EquipmentCategory.SECTOR_CHIP }
        if (!hasAnyChip) {
            val fallbackChip = EquipmentItem(nextItemInstanceId++, EquipmentItemId.CHIP_1)
            ownedItems.add(fallbackChip)
            equippedSectorChip = fallbackChip
        }

        currentMissionIndex = state.currentMissionIndex.coerceIn(0, MissionCatalog.all.size)
        val activeMission = MissionCatalog.all.getOrNull(currentMissionIndex)
        currentMissionProgress = if (activeMission != null) {
            state.currentMissionProgress.coerceIn(0, activeMission.targetCount)
        } else {
            0
        }

        recalculateShipStats(refillVitals = true)
        snapVantsToFormation()
    }

    fun resetSector(keepProgress: Boolean, triggerSave: Boolean = true) {
        if (!keepProgress) {
            credits = 0
            kills = 0
            player.level = 1
            player.xp = 0
            activeConfig = 1
            ownedShipClasses.clear()
            ownedShipClasses.add(ShipClass.INTERCEPTOR)
            activeShipClass = ShipClass.INTERCEPTOR
            player.shipClass = activeShipClass
            equippedSectorChip = null
            ownedVants.clear()
            nextVantId = 1
            ownedFormations.clear()
            ownedFormations.add(DroneFormation.NORMAL)
            activeFormation = DroneFormation.NORMAL
            formationSwitchCooldown = 0f
            currentMissionIndex = 0
            currentMissionProgress = 0
            ownedItems.clear()
            resizeSlotListsForShip(activeShipClass)
            for (i in config1LaserSlots.indices) {
                config1LaserSlots[i] = null
                config2LaserSlots[i] = null
            }
            for (i in config1ShieldSlots.indices) {
                config1ShieldSlots[i] = null
                config2ShieldSlots[i] = null
            }
            nextItemInstanceId = 1
            // Starter loadout: 1x LF-1 Laser, 1x SG-1 Shield, and 1x Chip Lv 1 equipped by default
            val starterLaser = EquipmentItem(nextItemInstanceId++, EquipmentItemId.LF_1)
            val starterShield = EquipmentItem(nextItemInstanceId++, EquipmentItemId.SG_1)
            val starterChip = EquipmentItem(nextItemInstanceId++, EquipmentItemId.CHIP_1)
            ownedItems.add(starterLaser)
            ownedItems.add(starterShield)
            ownedItems.add(starterChip)
            config1LaserSlots[0] = starterLaser
            config1ShieldSlots[0] = starterShield
            config2LaserSlots[0] = starterLaser
            config2ShieldSlots[0] = starterShield
            equippedSectorChip = starterChip
        }
        recalculateShipStats(refillVitals = true)

        activeMap = GameMap.MAIN_MAP
        warpFlashTimer = 0f
        player.x = 0f
        player.y = 0f
        player.angleRad = -PI.toFloat() * 0.5f
        player.isMoving = false
        player.isAutoFireEnabled = false
        player.currentZone = SpaceZone.SAFE_ZONE
        player.inSafeZone = true
        player.inRadiationWarning = false
        player.isTakingZoneRadiation = false
        player.requiredChipWarning = null
        player.hitFlash = 0f
        player.shieldHitFlash = 0f
        player.fireCooldown = 0f
        player.rocketCooldown = 0f
        player.radiationTickTimer = 0f
        player.timeSinceDamaged = GameConfig.SHIELD_REGEN_DELAY
        player.isAlive = true

        cameraX = player.x
        cameraY = player.y
        screenShake = 0f
        lockedAlienId = null
        isPaused = false
        isHangarOpen = false
        isTacticalMapOpen = false
        autopilotTargetX = null
        autopilotTargetY = null
        snapVantsToFormation()

        populateActiveMapEntities()

        if (triggerSave) {
            requestSave()
        }
    }

    private fun populateActiveMapEntities() {
        lasers.clear()
        rockets.clear()
        particles.clear()
        shockwaves.clear()
        popups.clear()
        aliens.clear()
        lootBoxes.clear()

        if (activeMap == GameMap.MAP_X1) {
            // Classic mix of Scouts, Hunters, and Destroyers with existing 20% BOSS chance
            repeat(16) { spawnAlienForTier(AlienTier.SCOUT, minDistanceFromPlayer = 680f) }
            repeat(14) { spawnAlienForTier(AlienTier.HUNTER, minDistanceFromPlayer = 720f) }
            repeat(10) { spawnAlienForTier(AlienTier.DESTROYER, minDistanceFromPlayer = 760f) }
            repeat(GameConfig.MAP_X1_BONUS_BOX_COUNT) {
                spawnBonusBox()
            }
        } else {
            for (tier in AlienTier.entries) {
                repeat(tier.activeCount) {
                    spawnAlienForTier(tier, minDistanceFromPlayer = 360f)
                }
            }
            repeat(GameConfig.BONUS_BOX_COUNT) {
                spawnBonusBox()
            }
        }
    }

    fun activeSafeZoneCenter(): Pair<Float, Float> = if (activeMap == GameMap.MAP_X1) {
        Pair(GameConfig.MAP_X1_SAFE_ZONE_X, GameConfig.MAP_X1_SAFE_ZONE_Y)
    } else {
        Pair(0f, 0f)
    }

    fun activeSafeZoneRadius(): Float = if (activeMap == GameMap.MAP_X1) {
        GameConfig.MAP_X1_SAFE_ZONE_RADIUS
    } else {
        GameConfig.SAFE_ZONE_RADIUS
    }

    fun activeJumpGatePosition(): Pair<Float, Float> = if (activeMap == GameMap.MAP_X1) {
        Pair(GameConfig.MAP_X1_SAFE_ZONE_X, GameConfig.MAP_X1_SAFE_ZONE_Y)
    } else {
        Pair(GameConfig.MAIN_JUMP_GATE_X, GameConfig.MAIN_JUMP_GATE_Y)
    }

    fun isPointInActiveSafeZone(x: Float, y: Float): Boolean {
        val (sx, sy) = activeSafeZoneCenter()
        return hypot(x - sx, y - sy) <= activeSafeZoneRadius()
    }

    fun isPlayerNearJumpGate(): Boolean {
        if (!player.isAlive) return false
        val (gx, gy) = activeJumpGatePosition()
        return hypot(player.x - gx, player.y - gy) <= GameConfig.JUMP_GATE_INTERACT_RADIUS
    }

    fun triggerJumpGate(): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen) return false
        if (!isPlayerNearJumpGate()) return false

        // Cancel Autopilot, target lock, auto-fire, and close Tactical Map
        autopilotTargetX = null
        autopilotTargetY = null
        lockedAlienId = null
        player.isAutoFireEnabled = false
        isTacticalMapOpen = false

        activeMap = if (activeMap == GameMap.MAIN_MAP) GameMap.MAP_X1 else GameMap.MAIN_MAP
        warpFlashTimer = GameConfig.WARP_FLASH_DURATION_SEC

        if (activeMap == GameMap.MAP_X1) {
            player.x = GameConfig.MAP_X1_SPAWN_X
            player.y = GameConfig.MAP_X1_SPAWN_Y
            player.angleRad = 0.785398f // Facing diagonally out into X-1
        } else {
            player.x = 0f
            player.y = 0f
            player.angleRad = -PI.toFloat() * 0.5f
        }
        player.isMoving = false
        player.thrustFactor = 0f
        player.currentZone = SpaceZone.SAFE_ZONE
        player.inSafeZone = true
        player.inRadiationWarning = false
        player.isTakingZoneRadiation = false
        player.requiredChipWarning = null
        player.radiationTickTimer = 0f

        cameraX = player.x
        cameraY = player.y
        snapVantsToFormation()
        populateActiveMapEntities()

        shockwaves.add(
            ShockwaveRing(
                x = player.x,
                y = player.y,
                maxRadius = 150f,
                life = 0.45f,
                maxLife = 0.45f,
                colorHex = 0xFF00E5FF
            )
        )
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 32f,
                text = "JUMPED TO ${activeMap.displayName}",
                life = 1.6f,
                maxLife = 1.6f,
                colorRgb = 0x00E5FF,
                textSize = 15f
            )
        )
        return true
    }

    fun relaunchShip() {
        recalculateShipStats(refillVitals = true)
        if (activeMap == GameMap.MAP_X1) {
            player.x = GameConfig.MAP_X1_SPAWN_X
            player.y = GameConfig.MAP_X1_SPAWN_Y
        } else {
            player.x = 0f
            player.y = 0f
        }
        player.currentZone = SpaceZone.SAFE_ZONE
        player.inSafeZone = true
        player.inRadiationWarning = false
        player.isTakingZoneRadiation = false
        player.requiredChipWarning = null
        player.radiationTickTimer = 0f
        player.rocketCooldown = 0f
        player.isAlive = true
        player.isAutoFireEnabled = false
        player.hitFlash = 0f
        player.shieldHitFlash = 0f
        player.timeSinceDamaged = GameConfig.SHIELD_REGEN_DELAY
        cameraX = player.x
        cameraY = player.y
        autopilotTargetX = null
        autopilotTargetY = null
        isTacticalMapOpen = false
        snapVantsToFormation()
        lasers.clear()
        rockets.clear()
        aliens.forEach { alien ->
            alien.isAggro = false
            clampAlienToZoneRing(alien)
        }
        requestSave()
    }

    fun setMoveJoystick(x: Float, y: Float) {
        moveInputX = x
        moveInputY = y
        if (hypot(x, y) > GameConfig.JOYSTICK_DEADZONE) {
            autopilotTargetX = null
            autopilotTargetY = null
        }
    }

    fun setAimJoystick(x: Float, y: Float) {
        aimInputX = x
        aimInputY = y
        val mag = hypot(x, y)
        isAiming = mag > GameConfig.JOYSTICK_DEADZONE
        if (isAiming && player.isAlive && !isPaused && !isHangarOpen) {
            acquireClosestAlienInRadarCone(atan2(y, x))
        }
    }

    fun toggleAutoFire() {
        if (!player.isAlive || isPaused || isHangarOpen) return
        if (player.inSafeZone) {
            player.isAutoFireEnabled = false
            return
        }
        player.isAutoFireEnabled = !player.isAutoFireEnabled
        if (player.isAutoFireEnabled && getLockedAlien() == null) {
            val nearest = aliens.minByOrNull { hypot(it.x - player.x, it.y - player.y) }
            if (nearest != null && hypot(nearest.x - player.x, nearest.y - player.y) <= GameConfig.PLAYER_ATTACK_RANGE) {
                lockedAlienId = nearest.id
            }
        }
    }

    fun fireHomingRocket(): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen || player.inSafeZone) return false
        if (player.rocketCooldown > 0f) return false

        var target = getLockedAlien()
        if (target == null || hypot(target.x - player.x, target.y - player.y) > GameConfig.PLAYER_ATTACK_RANGE * 1.15f) {
            val nearest = aliens.minByOrNull { hypot(it.x - player.x, it.y - player.y) }
            if (nearest != null && hypot(nearest.x - player.x, nearest.y - player.y) <= GameConfig.PLAYER_ATTACK_RANGE * 1.15f) {
                target = nearest
                lockedAlienId = nearest.id
            } else {
                return false
            }
        }

        val launchAngle = atan2(target.y - player.y, target.x - player.x)
        val rocketDmg = max(48f, player.laserDamage * 2.5f)
        rockets.add(
            HomingRocket(
                x = player.x + cos(launchAngle) * (activeShipClass.hullRadius + 4f),
                y = player.y + sin(launchAngle) * (activeShipClass.hullRadius + 4f),
                angleRad = launchAngle,
                targetAlienId = target.id,
                damage = rocketDmg,
                lifeRemaining = GameConfig.ROCKET_LIFETIME_SEC
            )
        )
        player.rocketCooldown = GameConfig.ROCKET_COOLDOWN_SEC
        spawnHitSparks(
            player.x + cos(launchAngle) * 18f,
            player.y + sin(launchAngle) * 18f,
            colorHex = 0xFFFB923C,
            count = 5
        )
        return true
    }

    fun togglePause() {
        if (!player.isAlive || isHangarOpen) return
        isPaused = !isPaused
        if (isPaused) {
            requestSave()
        }
    }

    fun openHangar() {
        if (!player.isAlive) return
        isTacticalMapOpen = false
        isHangarOpen = true
        isPaused = true
    }

    fun closeHangar() {
        isHangarOpen = false
        isPaused = false
        recalculateShipStats(refillVitals = false)
        requestSave()
    }

    fun openTacticalMap() {
        if (!player.isAlive || isHangarOpen || isPaused) return
        isTacticalMapOpen = true
    }

    fun closeTacticalMap() {
        isTacticalMapOpen = false
    }

    fun toggleTacticalMap() {
        if (isTacticalMapOpen) {
            closeTacticalMap()
        } else {
            openTacticalMap()
        }
    }

    fun setAutopilotTarget(worldX: Float, worldY: Float) {
        if (!player.isAlive) return
        val clampedX: Float
        val clampedY: Float
        if (activeMap == GameMap.MAP_X1) {
            clampedX = worldX.coerceIn(GameConfig.MAP_X1_AUTOPILOT_MIN, GameConfig.MAP_X1_AUTOPILOT_MAX)
            clampedY = worldY.coerceIn(GameConfig.MAP_X1_AUTOPILOT_MIN, GameConfig.MAP_X1_AUTOPILOT_MAX)
        } else {
            val dist = hypot(worldX, worldY)
            if (dist > GameConfig.AUTOPILOT_MAX_TARGET_RADIUS && dist > 0.001f) {
                val scale = GameConfig.AUTOPILOT_MAX_TARGET_RADIUS / dist
                clampedX = worldX * scale
                clampedY = worldY * scale
            } else {
                clampedX = worldX
                clampedY = worldY
            }
        }
        autopilotTargetX = clampedX
        autopilotTargetY = clampedY
    }

    fun cancelAutopilot() {
        autopilotTargetX = null
        autopilotTargetY = null
    }

    fun switchConfig(configNumber: Int): Boolean {
        val target = if (configNumber == 2) 2 else 1
        if (activeConfig == target) return false
        activeConfig = target
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun toggleConfig(): Boolean {
        return switchConfig(if (activeConfig == 1) 2 else 1)
    }

    // -------------------------------------------------------------------------
    // Drone Formations (Shop purchase, equip, and mid-flight 3s cooldown cycle)
    // -------------------------------------------------------------------------
    fun buyOrSelectFormation(formation: DroneFormation): Boolean {
        if (formation in ownedFormations) {
            if (activeFormation == formation) return false
            activeFormation = formation
            recalculateShipStats(refillVitals = false)
            requestSave()
            return true
        }
        if (credits < formation.cost) return false
        credits -= formation.cost
        ownedFormations.add(formation)
        activeFormation = formation
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun cycleOwnedFormation(): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen) return false
        if (formationSwitchCooldown > 0.05f) return false
        val ordered = DroneFormation.entries.filter { it in ownedFormations }
        if (ordered.size <= 1) return false
        val currentIdx = ordered.indexOf(activeFormation).coerceAtLeast(0)
        val next = ordered[(currentIdx + 1) % ordered.size]
        activeFormation = next
        formationSwitchCooldown = GameConfig.FORMATION_SWITCH_COOLDOWN_SEC
        recalculateShipStats(refillVitals = false)
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 36f,
                text = "${next.displayName.uppercase()} FORMATION",
                life = 1.25f,
                maxLife = 1.25f,
                colorRgb = (next.accentHex and 0xFFFFFF).toInt(),
                textSize = 14.5f
            )
        )
        requestSave()
        return true
    }

    // -------------------------------------------------------------------------
    // Ship Classes
    // -------------------------------------------------------------------------
    fun buyOrSelectShipClass(shipClass: ShipClass): Boolean {
        if (shipClass in ownedShipClasses) {
            return selectShipClass(shipClass)
        }
        if (credits < shipClass.cost) return false
        credits -= shipClass.cost
        ownedShipClasses.add(shipClass)
        applyShipClassChange(shipClass, refillHpOnUpgrade = true)
        requestSave()
        return true
    }

    fun selectShipClass(shipClass: ShipClass): Boolean {
        if (shipClass !in ownedShipClasses) return false
        if (activeShipClass == shipClass) return false
        applyShipClassChange(shipClass, refillHpOnUpgrade = false)
        requestSave()
        return true
    }

    private fun applyShipClassChange(newShip: ShipClass, refillHpOnUpgrade: Boolean) {
        val oldMaxHp = player.maxHp
        activeShipClass = newShip
        player.shipClass = newShip
        // Resize Config 1 & 2 slot lists: excess items automatically return to Inventory
        resizeSlotListsForShip(newShip)
        recalculateShipStats(refillVitals = false)
        if (refillHpOnUpgrade || player.inSafeZone) {
            val hpGain = (player.maxHp - oldMaxHp).coerceAtLeast(0f)
            player.hp = min(player.maxHp, player.hp + hpGain)
        }
    }

    private fun resizeSlotListsForShip(shipClass: ShipClass) {
        resizeSlotList(config1LaserSlots, shipClass.laserSlotCount)
        resizeSlotList(config2LaserSlots, shipClass.laserSlotCount)
        resizeSlotList(config1ShieldSlots, shipClass.shieldSlotCount)
        resizeSlotList(config2ShieldSlots, shipClass.shieldSlotCount)
    }

    private fun resizeSlotList(list: MutableList<EquipmentItem?>, targetSize: Int) {
        while (list.size > targetSize) {
            list.removeAt(list.lastIndex)
        }
        while (list.size < targetSize) {
            list.add(null)
        }
    }

    // -------------------------------------------------------------------------
    // VANT (Drone) System: Flax (1 Universal Slot) & Iris (2 Universal Slots)
    // -------------------------------------------------------------------------
    private fun isVantCompatibleItem(type: EquipmentItemId): Boolean =
        type.category == EquipmentCategory.LASER || type.category == EquipmentCategory.SHIELD_GENERATOR

    /**
     * Returns up to 8 active VANTs for flight, rendering, stats, and slot equipping,
     * strictly prioritizing Iris drones first and then Flax drones (stable by ID).
     */
    fun getActiveVants(): List<VantDrone> {
        if (ownedVants.isEmpty()) return emptyList()
        return ownedVants
            .sortedWith(compareBy<VantDrone>({ if (it.tier == VantTier.IRIS) 0 else 1 }, { it.id }))
            .take(GameConfig.MAX_VANTS)
    }

    fun canBuyFlaxVant(): Boolean {
        val irisCount = ownedVants.count { it.tier == VantTier.IRIS }
        val totalOwned = ownedVants.size
        return irisCount < GameConfig.MAX_VANTS && totalOwned < GameConfig.MAX_OWNED_VANTS_FOR_FUSION
    }

    private fun clearReserveVantSlots() {
        if (ownedVants.size <= GameConfig.MAX_VANTS) return
        val activeIds = getActiveVants().map { it.id }.toSet()
        for (vant in ownedVants) {
            if (vant.id !in activeIds) {
                for (i in vant.config1Slots.indices) vant.config1Slots[i] = null
                for (i in vant.config2Slots.indices) vant.config2Slots[i] = null
            }
        }
    }

    fun buyFlaxVant(): Boolean {
        if (!canBuyFlaxVant()) return false
        if (credits < GameConfig.FLAX_VANT_COST) return false
        credits -= GameConfig.FLAX_VANT_COST
        ownedVants.add(VantDrone(id = nextVantId++, tier = VantTier.FLAX))
        clearReserveVantSlots()
        requestSave()
        return true
    }

    fun fuseFlaxVants(): Boolean {
        val flaxVants = ownedVants.filter { it.tier == VantTier.FLAX }
        if (flaxVants.size < 2) return false

        // Prefer consuming empty Flax VANTs first, then safely unequip any items back to Inventory
        val prioritized = flaxVants.sortedBy { drone ->
            drone.config1Slots.count { it != null } + drone.config2Slots.count { it != null }
        }
        val toConsume = prioritized.take(2)
        for (drone in toConsume) {
            for (i in drone.config1Slots.indices) drone.config1Slots[i] = null
            for (i in drone.config2Slots.indices) drone.config2Slots[i] = null
        }
        val consumeIds = toConsume.map { it.id }.toSet()
        ownedVants.removeAll { it.id in consumeIds }

        ownedVants.add(VantDrone(id = nextVantId++, tier = VantTier.IRIS))
        clearReserveVantSlots()
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun equipItemToVantSlot(vantId: Int, slotIndex: Int, instanceId: Int? = null): Boolean {
        val vant = getActiveVants().firstOrNull { it.id == vantId } ?: return false
        val slots = vant.slotsForConfig(activeConfig)
        if (slotIndex !in slots.indices || slots[slotIndex] != null) return false

        val available = getAvailableInventory().filter { isVantCompatibleItem(it.type) }
        val item = if (instanceId != null) {
            available.firstOrNull { it.instanceId == instanceId }
        } else {
            available.maxByOrNull { it.type.tierLevel }
        } ?: return false

        slots[slotIndex] = item
        recalculateShipStats(refillVitals = false, shieldDeltaAdded = item.type.shieldBonus)
        requestSave()
        return true
    }

    fun equipFromInventoryToVant(instanceId: Int): Boolean {
        val item = getAvailableInventory().firstOrNull {
            it.instanceId == instanceId && isVantCompatibleItem(it.type)
        } ?: return false

        for (vant in getActiveVants()) {
            val slots = vant.slotsForConfig(activeConfig)
            val emptyIdx = slots.indexOfFirst { it == null }
            if (emptyIdx != -1) {
                slots[emptyIdx] = item
                recalculateShipStats(refillVitals = false, shieldDeltaAdded = item.type.shieldBonus)
                requestSave()
                return true
            }
        }
        return false
    }

    fun unequipVantSlot(vantId: Int, slotIndex: Int): Boolean {
        val vant = getActiveVants().firstOrNull { it.id == vantId } ?: return false
        val slots = vant.slotsForConfig(activeConfig)
        if (slotIndex !in slots.indices || slots[slotIndex] == null) return false
        slots[slotIndex] = null
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    // -------------------------------------------------------------------------
    // Minimalist Shop, Equipment & 2-for-1 Fusion Workshop
    // -------------------------------------------------------------------------
    fun getAvailableInventory(): List<EquipmentItem> {
        val equippedIds = HashSet<Int>()
        for (item in laserSlots) {
            if (item != null) equippedIds.add(item.instanceId)
        }
        for (item in shieldSlots) {
            if (item != null) equippedIds.add(item.instanceId)
        }
        for (vant in getActiveVants()) {
            for (item in vant.slotsForConfig(activeConfig)) {
                if (item != null) equippedIds.add(item.instanceId)
            }
        }
        equippedSectorChip?.let { equippedIds.add(it.instanceId) }
        return ownedItems.filter { it.instanceId !in equippedIds }
    }

    fun getOwnedCountsByType(): Map<EquipmentItemId, Int> {
        val counts = HashMap<EquipmentItemId, Int>()
        for (item in ownedItems) {
            counts[item.type] = (counts[item.type] ?: 0) + 1
        }
        return counts
    }

    fun buyEquipment(itemId: EquipmentItemId): Boolean {
        val cost = itemId.shopCost ?: return false
        if (credits < cost) return false

        credits -= cost
        val newItem = EquipmentItem(nextItemInstanceId++, itemId)
        ownedItems.add(newItem)

        var shieldAdded = 0f
        if (itemId.category == EquipmentCategory.SECTOR_CHIP) {
            val currentChip = equippedSectorChip
            if (currentChip == null || itemId.sectorTier > currentChip.type.sectorTier) {
                equippedSectorChip = newItem
            }
        } else {
            val equipped = tryAutoEquipInActiveConfig(newItem)
            if (equipped) {
                shieldAdded = itemId.shieldBonus
            }
        }
        recalculateShipStats(refillVitals = false, shieldDeltaAdded = shieldAdded)
        requestSave()
        return true
    }

    fun fuseItems(sourceType: EquipmentItemId): Boolean {
        val resultType = sourceType.nextFusionResult() ?: return false
        val matching = ownedItems.filter { it.type == sourceType }
        if (matching.size < 2) return false

        // Prefer consuming unequipped inventory items first so equipped ship & VANT slots stay intact
        val activeVants = getActiveVants()
        val activeEquippedIds = HashSet<Int>()
        for (item in laserSlots) if (item != null) activeEquippedIds.add(item.instanceId)
        for (item in shieldSlots) if (item != null) activeEquippedIds.add(item.instanceId)
        for (vant in activeVants) {
            for (item in vant.slotsForConfig(activeConfig)) {
                if (item != null) activeEquippedIds.add(item.instanceId)
            }
        }
        equippedSectorChip?.let { activeEquippedIds.add(it.instanceId) }

        val inactiveConfig = if (activeConfig == 2) 1 else 2
        val inactiveLasers = if (activeConfig == 2) config1LaserSlots else config2LaserSlots
        val inactiveShields = if (activeConfig == 2) config1ShieldSlots else config2ShieldSlots
        val inactiveEquippedIds = HashSet<Int>()
        for (item in inactiveLasers) if (item != null) inactiveEquippedIds.add(item.instanceId)
        for (item in inactiveShields) if (item != null) inactiveEquippedIds.add(item.instanceId)
        for (vant in activeVants) {
            for (item in vant.slotsForConfig(inactiveConfig)) {
                if (item != null) inactiveEquippedIds.add(item.instanceId)
            }
        }

        val prioritized = matching.sortedBy { item ->
            when {
                item.instanceId !in activeEquippedIds && item.instanceId !in inactiveEquippedIds -> 0
                item.instanceId !in activeEquippedIds -> 1
                else -> 2
            }
        }
        val toConsume = prioritized.take(2)
        val consumeIds = toConsume.map { it.instanceId }.toSet()

        // Remember first vacated active slot so the forged upgrade can slot straight into it
        val vacatedLaserIndex = laserSlots.indexOfFirst { it != null && it.instanceId in consumeIds }
        val vacatedShieldIndex = shieldSlots.indexOfFirst { it != null && it.instanceId in consumeIds }
        var vacatedVant: VantDrone? = null
        var vacatedVantSlotIdx = -1
        for (vant in activeVants) {
            val idx = vant.slotsForConfig(activeConfig).indexOfFirst { it != null && it.instanceId in consumeIds }
            if (idx != -1) {
                vacatedVant = vant
                vacatedVantSlotIdx = idx
                break
            }
        }
        val chipWasEquipped = equippedSectorChip?.instanceId in consumeIds

        // Remove consumed items from master owned list and all ship/VANT config & chip slots
        ownedItems.removeAll { it.instanceId in consumeIds }
        clearConsumedFromSlotList(config1LaserSlots, consumeIds)
        clearConsumedFromSlotList(config2LaserSlots, consumeIds)
        clearConsumedFromSlotList(config1ShieldSlots, consumeIds)
        clearConsumedFromSlotList(config2ShieldSlots, consumeIds)
        for (vant in ownedVants) {
            clearConsumedFromSlotList(vant.config1Slots, consumeIds)
            clearConsumedFromSlotList(vant.config2Slots, consumeIds)
        }
        if (chipWasEquipped) {
            equippedSectorChip = null
        }

        // Forge 1 upgraded item
        val forged = EquipmentItem(nextItemInstanceId++, resultType)
        ownedItems.add(forged)

        var shieldAdded = 0f
        when (resultType.category) {
            EquipmentCategory.LASER -> {
                if (vacatedLaserIndex != -1 && laserSlots[vacatedLaserIndex] == null) {
                    laserSlots[vacatedLaserIndex] = forged
                } else if (vacatedVant != null && vacatedVantSlotIdx != -1 &&
                    vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] == null
                ) {
                    vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] = forged
                } else {
                    tryAutoEquipInActiveConfig(forged)
                }
            }
            EquipmentCategory.SHIELD_GENERATOR -> {
                if (vacatedShieldIndex != -1 && shieldSlots[vacatedShieldIndex] == null) {
                    shieldSlots[vacatedShieldIndex] = forged
                    shieldAdded = resultType.shieldBonus
                } else if (vacatedVant != null && vacatedVantSlotIdx != -1 &&
                    vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] == null
                ) {
                    vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] = forged
                    shieldAdded = resultType.shieldBonus
                } else if (tryAutoEquipInActiveConfig(forged)) {
                    shieldAdded = resultType.shieldBonus
                }
            }
            EquipmentCategory.SECTOR_CHIP -> {
                val currentChip = equippedSectorChip
                if (currentChip == null || resultType.sectorTier > currentChip.type.sectorTier) {
                    equippedSectorChip = forged
                }
            }
        }

        recalculateShipStats(refillVitals = false, shieldDeltaAdded = shieldAdded)
        requestSave()
        return true
    }

    private fun clearConsumedFromSlotList(slots: MutableList<EquipmentItem?>, consumeIds: Set<Int>) {
        for (i in slots.indices) {
            val item = slots[i]
            if (item != null && item.instanceId in consumeIds) {
                slots[i] = null
            }
        }
    }

    fun equipFromInventory(instanceId: Int): Boolean {
        val item = getAvailableInventory().firstOrNull { it.instanceId == instanceId } ?: return false
        if (item.type.category == EquipmentCategory.SECTOR_CHIP) {
            equippedSectorChip = item
            recalculateShipStats(refillVitals = false)
            requestSave()
            return true
        }
        if (!tryAutoEquipInActiveConfig(item)) return false
        recalculateShipStats(refillVitals = false, shieldDeltaAdded = item.type.shieldBonus)
        requestSave()
        return true
    }

    fun unequipLaserSlot(slotIndex: Int): Boolean {
        if (slotIndex !in laserSlots.indices) return false
        if (laserSlots[slotIndex] == null) return false
        laserSlots[slotIndex] = null
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun unequipShieldSlot(slotIndex: Int): Boolean {
        if (slotIndex !in shieldSlots.indices) return false
        if (shieldSlots[slotIndex] == null) return false
        shieldSlots[slotIndex] = null
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun unequipSectorChip(): Boolean {
        if (equippedSectorChip == null) return false
        equippedSectorChip = null
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    private fun tryAutoEquipInActiveConfig(item: EquipmentItem): Boolean {
        val targetSlots = when (item.type.category) {
            EquipmentCategory.LASER -> laserSlots
            EquipmentCategory.SHIELD_GENERATOR -> shieldSlots
            EquipmentCategory.SECTOR_CHIP -> return false
        }
        val emptyIndex = targetSlots.indexOfFirst { it == null }
        if (emptyIndex == -1) return false
        targetSlots[emptyIndex] = item
        return true
    }

    fun recalculateShipStats(refillVitals: Boolean, shieldDeltaAdded: Float = 0f) {
        var bonusDamage = 0f
        for (slot in laserSlots) {
            if (slot != null) bonusDamage += slot.type.damageBonus
        }

        var bonusShield = 0f
        for (slot in shieldSlots) {
            if (slot != null) bonusShield += slot.type.shieldBonus
        }

        // Include all Lasers and Shields equipped inside up to 8 active VANT drones for the active Config
        for (vant in getActiveVants()) {
            for (slot in vant.slotsForConfig(activeConfig)) {
                if (slot != null) {
                    bonusDamage += slot.type.damageBonus
                    bonusShield += slot.type.shieldBonus
                }
            }
        }

        val rawLaserDamage = GameConfig.PLAYER_BASE_LASER_DAMAGE + bonusDamage
        val rawMaxShield = GameConfig.PLAYER_BASE_MAX_SHIELD + bonusShield
        val rawSpeed = activeShipClass.speed
        val rawMaxHull = activeShipClass.baseHull + (player.level - 1) * GameConfig.PLAYER_HP_PER_LEVEL

        val f = activeFormation
        player.shipClass = activeShipClass
        player.laserDamage = rawLaserDamage * f.laserMultiplier
        player.maxShield = rawMaxShield * f.shieldMultiplier
        player.speed = rawSpeed * f.speedMultiplier
        player.maxHp = rawMaxHull * f.hullMultiplier

        if (refillVitals) {
            player.hp = player.maxHp
            player.shield = player.maxShield
        } else {
            if (shieldDeltaAdded > 0f && player.inSafeZone) {
                player.shield = min(player.maxShield, player.shield + shieldDeltaAdded * f.shieldMultiplier)
            }
            player.hp = player.hp.coerceIn(0f, player.maxHp)
            player.shield = player.shield.coerceIn(0f, player.maxShield)
        }
    }

    // -------------------------------------------------------------------------
    // Sequential Mission Chains
    // -------------------------------------------------------------------------
    fun getActiveMission(): MissionSpec? = MissionCatalog.all.getOrNull(currentMissionIndex)

    fun isMissionLevelLocked(): Boolean {
        val active = getActiveMission() ?: return false
        return player.level < active.requiredLevel
    }

    fun isMissionReadyToClaim(): Boolean {
        val active = getActiveMission() ?: return false
        return !isMissionLevelLocked() && currentMissionProgress >= active.targetCount
    }

    private fun recordMissionProgress(goalType: MissionGoalType) {
        val active = getActiveMission() ?: return
        if (player.level < active.requiredLevel) return
        if (active.goalType != goalType) return
        if (currentMissionProgress >= active.targetCount) return

        currentMissionProgress = min(active.targetCount, currentMissionProgress + 1)
        if (currentMissionProgress >= active.targetCount) {
            popups.add(
                FloatingPopup(
                    x = player.x,
                    y = player.y - activeShipClass.hullRadius - 48f,
                    text = "${active.code} COMPLETE! CLAIM REWARD",
                    life = 1.9f,
                    maxLife = 1.9f,
                    colorRgb = 0x4ADE80,
                    textSize = 16f
                )
            )
        }
    }

    fun claimMissionReward(): Boolean {
        val active = getActiveMission() ?: return false
        if (!isMissionReadyToClaim()) return false

        credits += active.rewardCredits
        if (active.rewardXp > 0) {
            addXp(active.rewardXp)
        }
        val rewardChip = active.rewardChip
        if (rewardChip != null) {
            val newChip = EquipmentItem(nextItemInstanceId++, rewardChip)
            ownedItems.add(newChip)
            val current = equippedSectorChip
            if (current == null || rewardChip.sectorTier > current.type.sectorTier) {
                equippedSectorChip = newChip
            }
        }

        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 44f,
                text = "CLAIMED: ${active.rewardSummary()}",
                life = 1.85f,
                maxLife = 1.85f,
                colorRgb = 0xFBBF24,
                textSize = 16f
            )
        )
        shockwaves.add(
            ShockwaveRing(
                x = player.x,
                y = player.y,
                maxRadius = 100f,
                life = 0.5f,
                maxLife = 0.5f,
                colorHex = 0xFF4ADE80
            )
        )

        currentMissionIndex = min(MissionCatalog.all.size, currentMissionIndex + 1)
        currentMissionProgress = 0
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    private fun addXp(amount: Int) {
        if (amount <= 0) return
        player.xp += amount
        var leveledUp = false
        while (player.xp >= GameConfig.xpRequiredForLevel(player.level)) {
            player.xp -= GameConfig.xpRequiredForLevel(player.level)
            player.level += 1
            leveledUp = true
        }
        if (leveledUp) {
            recalculateShipStats(refillVitals = false)
            player.hp = player.maxHp
            shockwaves.add(
                ShockwaveRing(
                    x = player.x,
                    y = player.y,
                    maxRadius = 110f,
                    life = 0.55f,
                    maxLife = 0.55f,
                    colorHex = 0xFFFACC15
                )
            )
            popups.add(
                FloatingPopup(
                    x = player.x,
                    y = player.y - activeShipClass.hullRadius - 34f,
                    text = "LEVEL UP! LV ${player.level}",
                    life = 1.6f,
                    maxLife = 1.6f,
                    colorRgb = 0xFACC15,
                    textSize = 17f
                )
            )
        }
    }

    // Temporary Developer / Testing Helpers
    fun debugAddOneMillionCredits() {
        credits += 1_000_000
        requestSave()
    }

    fun debugAddOneLevel() {
        val xpNeeded = (GameConfig.xpRequiredForLevel(player.level) - player.xp).coerceAtLeast(1)
        addXp(xpNeeded)
        requestSave()
    }

    fun handleWorldTap(worldX: Float, worldY: Float): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen) return false
        var bestAlien: AlienEntity? = null
        var bestDist = GameConfig.TAP_LOCK_RADIUS
        for (alien in aliens) {
            val d = hypot(alien.x - worldX, alien.y - worldY)
            if (d <= max(bestDist, alien.radius + 32f)) {
                bestDist = d
                bestAlien = alien
            }
        }
        if (bestAlien != null) {
            lockedAlienId = bestAlien.id
            shockwaves.add(
                ShockwaveRing(
                    x = bestAlien.x,
                    y = bestAlien.y,
                    maxRadius = bestAlien.radius * 2.2f,
                    life = 0.25f,
                    maxLife = 0.25f,
                    colorHex = 0xFFFF2E4C
                )
            )
            return true
        }
        return false
    }

    fun getLockedAlien(): AlienEntity? {
        val id = lockedAlienId ?: return null
        return aliens.firstOrNull { it.id == id }
    }

    fun isLockedTargetInRange(): Boolean {
        val target = getLockedAlien() ?: return false
        return hypot(target.x - player.x, target.y - player.y) <= GameConfig.PLAYER_ATTACK_RANGE
    }

    fun step(rawDt: Float) {
        if (isPaused || isHangarOpen) return
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        elapsedTime += dt

        if (screenShake > 0f) {
            screenShake = max(0f, screenShake - dt * 28f)
        }
        if (warpFlashTimer > 0f) {
            warpFlashTimer = max(0f, warpFlashTimer - dt)
        }

        if (player.isAlive) {
            updatePlayer(dt)
            if (isAiming) {
                acquireClosestAlienInRadarCone(atan2(aimInputY, aimInputX))
            }
            handlePlayerAutoFire(dt)
            updateLootBoxes(dt)
        }

        updateAliens(dt)
        updateLasers(dt)
        updateRockets(dt)
        updateEffects(dt)

        val camFactor = 1f - exp(-GameConfig.CAMERA_SMOOTHNESS * dt)
        cameraX += (player.x - cameraX) * camFactor
        cameraY += (player.y - cameraY) * camFactor
    }

    fun buildHudSnapshot(): HudSnapshot {
        val target = getLockedAlien()
        val inRange = target != null &&
            hypot(target.x - player.x, target.y - player.y) <= GameConfig.PLAYER_ATTACK_RANGE
        val counts = getOwnedCountsByType()
        val flaxCount = ownedVants.count { it.tier == VantTier.FLAX }
        val irisCount = ownedVants.count { it.tier == VantTier.IRIS }
        val hasFusion = flaxCount >= 2 || FusionCatalog.recipes.any { (counts[it.source] ?: 0) >= 2 }
        val activeMission = getActiveMission()

        return HudSnapshot(
            credits = credits,
            kills = kills,
            playerLevel = player.level,
            playerXp = player.xp,
            playerXpToNext = GameConfig.xpRequiredForLevel(player.level),
            activeConfig = activeConfig,
            activeShipClass = activeShipClass,
            activeFormation = activeFormation,
            formationCooldownRemaining = formationSwitchCooldown,
            ownedFormationCount = ownedFormations.size,
            isAutopilotActive = autopilotTargetX != null && autopilotTargetY != null,
            autopilotTargetX = autopilotTargetX?.toInt() ?: 0,
            autopilotTargetY = autopilotTargetY?.toInt() ?: 0,
            isTacticalMapOpen = isTacticalMapOpen,
            playerHp = player.hp.roundToInt().coerceAtLeast(0),
            playerMaxHp = player.maxHp.roundToInt().coerceAtLeast(1),
            playerShield = player.shield.roundToInt().coerceAtLeast(0),
            playerMaxShield = player.maxShield.roundToInt().coerceAtLeast(1),
            coordX = player.x.toInt(),
            coordY = player.y.toInt(),
            activeMap = activeMap,
            isNearJumpGate = isPlayerNearJumpGate(),
            jumpGateLabel = activeMap.gateLabel,
            currentZone = player.currentZone,
            isAutoFireEnabled = player.isAutoFireEnabled,
            rocketCooldownRemaining = player.rocketCooldown,
            inSafeZone = player.inSafeZone,
            lockedTargetCallsign = target?.callsign,
            lockedTargetHp = target?.hp?.toInt()?.coerceAtLeast(0) ?: 0,
            lockedTargetMaxHp = target?.maxHp?.toInt() ?: 100,
            lockedTargetInRange = inRange,
            inRadiationWarning = player.inRadiationWarning,
            isTakingZoneRadiation = player.isTakingZoneRadiation,
            requiredChipWarning = player.requiredChipWarning,
            isAlive = player.isAlive,
            isPaused = isPaused,
            isHangarOpen = isHangarOpen,
            equipment = EquipmentSnapshot(
                activeConfig = activeConfig,
                activeShipClass = activeShipClass,
                ownedShipClasses = ownedShipClasses.toSet(),
                equippedSectorChip = equippedSectorChip,
                laserSlots = laserSlots.toList(),
                shieldSlots = shieldSlots.toList(),
                vants = getActiveVants().map { vant ->
                    VantSnapshot(
                        id = vant.id,
                        tier = vant.tier,
                        activeSlots = vant.slotsForConfig(activeConfig).toList()
                    )
                },
                flaxVantCount = flaxCount,
                irisVantCount = irisCount,
                ownedFormations = ownedFormations.toSet(),
                activeFormation = activeFormation,
                inventory = getAvailableInventory(),
                ownedItemCounts = counts,
                hasAnyReadyFusion = hasFusion,
                totalDamage = player.laserDamage.toInt(),
                totalMaxShield = player.maxShield.toInt(),
                totalSpeed = player.speed.toInt(),
                totalMaxHull = player.maxHp.toInt()
            ),
            mission = MissionSnapshot(
                currentMissionIndex = currentMissionIndex,
                currentMissionProgress = currentMissionProgress,
                activeMission = activeMission,
                isLevelLocked = isMissionLevelLocked(),
                isReadyToClaim = isMissionReadyToClaim(),
                allCompleted = activeMission == null
            )
        )
    }

    private fun updatePlayer(dt: Float) {
        if (player.rocketCooldown > 0f) {
            player.rocketCooldown = max(0f, player.rocketCooldown - dt)
        }
        if (formationSwitchCooldown > 0f) {
            formationSwitchCooldown = max(0f, formationSwitchCooldown - dt)
        }

        var movementAngle: Float? = null
        var targetThrust = 0f
        val mag = hypot(moveInputX, moveInputY)
        if (mag > GameConfig.JOYSTICK_DEADZONE) {
            // Instant manual override cancels autopilot
            autopilotTargetX = null
            autopilotTargetY = null
            movementAngle = atan2(moveInputY, moveInputX)
            val dirX = moveInputX / mag
            val dirY = moveInputY / mag
            player.x += dirX * player.speed * dt
            player.y += dirY * player.speed * dt
            player.isMoving = true
            targetThrust = mag.coerceIn(0.45f, 1f)
        } else if (autopilotTargetX != null && autopilotTargetY != null) {
            val tx = autopilotTargetX!!
            val ty = autopilotTargetY!!
            val dx = tx - player.x
            val dy = ty - player.y
            val dist = hypot(dx, dy)
            if (dist <= GameConfig.AUTOPILOT_STOP_RADIUS) {
                autopilotTargetX = null
                autopilotTargetY = null
                player.isMoving = false
                targetThrust = 0f
            } else {
                movementAngle = atan2(dy, dx)
                val stepDist = min(dist, player.speed * dt)
                player.x += (dx / dist) * stepDist
                player.y += (dy / dist) * stepDist
                player.isMoving = true
                targetThrust = 1f
            }
        } else {
            player.isMoving = false
            targetThrust = 0f
        }
        val thrustLerp = 1f - exp(-10f * dt)
        player.thrustFactor += (targetThrust - player.thrustFactor) * thrustLerp
        if (player.thrustFactor < 0.005f) player.thrustFactor = 0f
        player.thrusterPulse += dt * (8f + 18f * player.thrustFactor)

        // Combat Facing Override: decouple ship facing from movement velocity while attacking a locked alien
        val locked = getLockedAlien()
        val isActivelyAttacking = player.isAutoFireEnabled && !player.inSafeZone && locked != null
        if (isActivelyAttacking && locked != null) {
            val aimAngle = atan2(locked.y - player.y, locked.x - player.x)
            player.angleRad = lerpAngle(player.angleRad, aimAngle, 1f - exp(-14f * dt))
        } else if (movementAngle != null) {
            player.angleRad = lerpAngle(
                player.angleRad,
                movementAngle,
                1f - exp(-GameConfig.PLAYER_TURN_SPEED * dt)
            )
        }

        if (activeMap == GameMap.MAP_X1) {
            val minOuter = -GameConfig.MAP_X1_RADIATION_OUTER_BUFFER
            val maxOuter = GameConfig.MAP_X1_SIZE + GameConfig.MAP_X1_RADIATION_OUTER_BUFFER
            player.x = player.x.coerceIn(minOuter, maxOuter)
            player.y = player.y.coerceIn(minOuter, maxOuter)

            val inX1Safe = isPointInActiveSafeZone(player.x, player.y)
            player.inSafeZone = inX1Safe
            player.currentZone = if (inX1Safe) SpaceZone.SAFE_ZONE else SpaceZone.ZONE_1_ALPHA

            val outsideSquare = player.x < 0f ||
                player.x > GameConfig.MAP_X1_SIZE ||
                player.y < 0f ||
                player.y > GameConfig.MAP_X1_SIZE
            val distToBorder = min(
                min(player.x, GameConfig.MAP_X1_SIZE - player.x),
                min(player.y, GameConfig.MAP_X1_SIZE - player.y)
            )
            player.isTakingZoneRadiation = !inX1Safe && outsideSquare
            player.requiredChipWarning = null
            player.inRadiationWarning = outsideSquare || distToBorder < GameConfig.RADIATION_WARNING_DISTANCE

            if (outsideSquare && !inX1Safe && player.isAlive) {
                player.radiationTickTimer += dt
                if (player.radiationTickTimer >= GameConfig.RADIATION_TICK_INTERVAL) {
                    player.radiationTickTimer -= GameConfig.RADIATION_TICK_INTERVAL
                    val radDmg = GameConfig.MAP_X1_BORDER_RADIATION_DPS * GameConfig.RADIATION_TICK_INTERVAL
                    applyDamageToPlayer(radDmg, player.x, player.y, isRadiation = true)
                }
            } else {
                player.radiationTickTimer = 0f
            }
        } else {
            val maxPlayerRadius = GameConfig.MAP_RADIUS - GameConfig.BORDER_PADDING
            val distFromCenter = hypot(player.x, player.y)
            if (distFromCenter > maxPlayerRadius && distFromCenter > 0.001f) {
                val scale = maxPlayerRadius / distFromCenter
                player.x *= scale
                player.y *= scale
            }

            val clampedDist = min(distFromCenter, maxPlayerRadius)
            player.currentZone = resolveZone(clampedDist)
            player.inSafeZone = player.currentZone == SpaceZone.SAFE_ZONE

            // Sector Chip radiation check
            val zoneReqTier = player.currentZone.requiredChipTier
            val exposedToZoneRad = !player.inSafeZone && zoneReqTier > equippedChipTier
            player.isTakingZoneRadiation = exposedToZoneRad
            player.requiredChipWarning = if (exposedToZoneRad) player.currentZone.requiredChipName else null
            player.inRadiationWarning = exposedToZoneRad ||
                (GameConfig.MAP_RADIUS - clampedDist) < GameConfig.RADIATION_WARNING_DISTANCE

            if (exposedToZoneRad && player.isAlive) {
                player.radiationTickTimer += dt
                if (player.radiationTickTimer >= GameConfig.RADIATION_TICK_INTERVAL) {
                    player.radiationTickTimer -= GameConfig.RADIATION_TICK_INTERVAL
                    val radDmg = player.currentZone.radiationDps * GameConfig.RADIATION_TICK_INTERVAL
                    applyDamageToPlayer(radDmg, player.x, player.y, isRadiation = true)
                }
            } else {
                player.radiationTickTimer = 0f
            }
        }

        if (player.inSafeZone && player.isAutoFireEnabled) {
            player.isAutoFireEnabled = false
        }

        if (player.hitFlash > 0f) {
            player.hitFlash = max(0f, player.hitFlash - dt * 4f)
        }
        if (player.shieldHitFlash > 0f) {
            player.shieldHitFlash = max(0f, player.shieldHitFlash - dt * 4f)
        }

        player.timeSinceDamaged += dt
        if (player.inSafeZone) {
            if (player.hp < player.maxHp) {
                player.hp = min(player.maxHp, player.hp + GameConfig.SAFE_ZONE_HP_REGEN_RATE * dt)
            }
            if (player.shield < player.maxShield) {
                player.shield = min(player.maxShield, player.shield + GameConfig.SAFE_ZONE_SHIELD_REGEN_RATE * dt)
            }
        } else {
            // DIAMOND formation passive continuous combat shield regen (+1.5% Max Shield/sec even during combat)
            val formationRegenPct = activeFormation.passiveShieldRegenPctPerSec
            if (formationRegenPct > 0f && player.shield < player.maxShield) {
                player.shield = min(player.maxShield, player.shield + player.maxShield * formationRegenPct * dt)
            }
            if (player.timeSinceDamaged >= GameConfig.SHIELD_REGEN_DELAY && player.shield < player.maxShield) {
                player.shield = min(player.maxShield, player.shield + GameConfig.SHIELD_REGEN_RATE * dt)
            }
        }

        updateVantFormationPositions(dt)
    }

    private fun snapVantsToFormation() {
        val activeVants = getActiveVants()
        val count = activeVants.size
        if (count == 0) return
        val cosA = cos(player.angleRad)
        val sinA = sin(player.angleRad)
        val hullR = activeShipClass.hullRadius
        for (i in 0 until count) {
            val vant = activeVants[i]
            val (lx, ly, extraRot) = activeFormation.computeLocalSlot(i, count, hullR, elapsedTime)
            vant.worldX = player.x + lx * cosA - ly * sinA
            vant.worldY = player.y + lx * sinA + ly * cosA
            vant.facingRad = player.angleRad + extraRot
            vant.hasInitializedPosition = true
        }
    }

    private fun updateVantFormationPositions(dt: Float) {
        val activeVants = getActiveVants()
        val count = activeVants.size
        if (count == 0) return
        val cosA = cos(player.angleRad)
        val sinA = sin(player.angleRad)
        val hullR = activeShipClass.hullRadius
        val lerpFactor = 1f - exp(-9.5f * dt)
        for (i in 0 until count) {
            val vant = activeVants[i]
            val (lx, ly, extraRot) = activeFormation.computeLocalSlot(i, count, hullR, elapsedTime)
            val targetX = player.x + lx * cosA - ly * sinA
            val targetY = player.y + lx * sinA + ly * cosA
            val targetFacing = player.angleRad + extraRot
            if (!vant.hasInitializedPosition) {
                vant.worldX = targetX
                vant.worldY = targetY
                vant.facingRad = targetFacing
                vant.hasInitializedPosition = true
            } else {
                vant.worldX += (targetX - vant.worldX) * lerpFactor
                vant.worldY += (targetY - vant.worldY) * lerpFactor
                vant.facingRad = lerpAngle(vant.facingRad, targetFacing, lerpFactor)
            }
        }
    }

    private fun resolveZone(distFromCenter: Float): SpaceZone = when {
        distFromCenter <= GameConfig.SAFE_ZONE_RADIUS -> SpaceZone.SAFE_ZONE
        distFromCenter <= GameConfig.ZONE_1_MAX_RADIUS -> SpaceZone.ZONE_1_ALPHA
        distFromCenter <= GameConfig.ZONE_2_MAX_RADIUS -> SpaceZone.ZONE_2_BETA
        distFromCenter <= GameConfig.ZONE_3_MAX_RADIUS -> SpaceZone.ZONE_3_GAMMA
        else -> SpaceZone.ZONE_4_OMEGA
    }

    private fun acquireClosestAlienInRadarCone(aimAngle: Float) {
        var closestAlien: AlienEntity? = null
        var closestDist = GameConfig.RADAR_CONE_RANGE

        for (alien in aliens) {
            val dx = alien.x - player.x
            val dy = alien.y - player.y
            val dist = hypot(dx, dy)
            if (dist > GameConfig.RADAR_CONE_RANGE) continue

            val angleToAlien = atan2(dy, dx)
            val angleDiff = abs(normalizeAngle(angleToAlien - aimAngle))
            if (angleDiff <= GameConfig.RADAR_CONE_HALF_ANGLE_RAD && dist < closestDist) {
                closestDist = dist
                closestAlien = alien
            }
        }

        if (closestAlien != null) {
            lockedAlienId = closestAlien.id
        }
    }

    private fun handlePlayerAutoFire(dt: Float) {
        if (player.fireCooldown > 0f) {
            player.fireCooldown = max(0f, player.fireCooldown - dt)
        }
        if (player.inSafeZone || !player.isAutoFireEnabled || player.fireCooldown > 0f) return

        val target = getLockedAlien() ?: return
        val dx = target.x - player.x
        val dy = target.y - player.y
        val dist = hypot(dx, dy)
        if (dist > GameConfig.PLAYER_ATTACK_RANGE) return

        player.fireCooldown = GameConfig.PLAYER_FIRE_INTERVAL
        val fireAngle = atan2(dy, dx)
        lasers.add(
            LaserBolt(
                x = player.x + cos(fireAngle) * 16f,
                y = player.y + sin(fireAngle) * 16f,
                vx = cos(fireAngle) * GameConfig.PLAYER_LASER_SPEED,
                vy = sin(fireAngle) * GameConfig.PLAYER_LASER_SPEED,
                angleRad = fireAngle,
                fromPlayer = true,
                targetAlienId = target.id,
                damage = player.laserDamage,
                colorHex = activeShipClass.accentColorHex,
                lifeRemaining = 0.72f
            )
        )
    }

    private fun updateLootBoxes(dt: Float) {
        var collectedAny = false
        var bonusBoxesCollected = 0
        val it = lootBoxes.iterator()
        while (it.hasNext()) {
            val box = it.next()
            box.pulsePhase += dt * 4.2f
            if (box.kind == LootBoxKind.CARGO_BOX) {
                box.lifeRemaining -= dt
                if (box.lifeRemaining <= 0f) {
                    it.remove()
                    continue
                }
            }

            val dist = hypot(player.x - box.x, player.y - box.y)
            if (dist <= GameConfig.LOOT_COLLECT_RADIUS + (activeShipClass.hullRadius - 20f)) {
                collectedAny = true
                if (box.xpReward > 0) {
                    addXp(box.xpReward)
                }
                if (box.shieldReward > 0f && player.shield < player.maxShield) {
                    val gained = min(player.maxShield - player.shield, box.shieldReward)
                    player.shield += gained
                    popups.add(
                        FloatingPopup(
                            x = box.x,
                            y = box.y - 16f,
                            text = "+${gained.toInt()} SHD",
                            life = 1.15f,
                            maxLife = 1.15f,
                            colorRgb = 0x38BDF8
                        )
                    )
                } else {
                    val cr = if (box.creditReward > 0) box.creditReward else 150
                    credits += cr
                    val label = if (box.kind == LootBoxKind.CARGO_BOX) "+$cr CARGO" else "+$cr CR"
                    popups.add(
                        FloatingPopup(
                            x = box.x,
                            y = box.y - 16f,
                            text = label,
                            life = 1.15f,
                            maxLife = 1.15f,
                            colorRgb = 0xFBBF24
                        )
                    )
                }
                spawnHitSparks(
                    box.x,
                    box.y,
                    colorHex = if (box.shieldReward > 0f) 0xFF38BDF8 else 0xFFFACC15,
                    count = 7
                )
                if (box.kind == LootBoxKind.BONUS_BOX) {
                    bonusBoxesCollected++
                    recordMissionProgress(MissionGoalType.COLLECT_BONUS_BOXES)
                }
                it.remove()
            }
        }

        repeat(bonusBoxesCollected) {
            spawnBonusBox()
        }
        if (collectedAny) {
            requestSave()
        }
    }

    private fun updateAliens(dt: Float) {
        for (alien in aliens) {
            val tier = alien.tier
            alien.pulsePhase += dt * 3.5f
            if (alien.hitFlash > 0f) {
                alien.hitFlash = max(0f, alien.hitFlash - dt * 5f)
            }
            if (alien.fireCooldown > 0f) {
                alien.fireCooldown = max(0f, alien.fireCooldown - dt)
            }

            val playerInAlienZone = if (activeMap == GameMap.MAP_X1) {
                !player.inSafeZone &&
                    player.x >= 0f && player.x <= GameConfig.MAP_X1_SIZE &&
                    player.y >= 0f && player.y <= GameConfig.MAP_X1_SIZE
            } else {
                val playerDistFromOrigin = hypot(player.x, player.y)
                playerDistFromOrigin >= tier.minRadius - 120f &&
                    playerDistFromOrigin <= tier.maxRadius + 120f
            }

            if (alien.isAggro && (player.inSafeZone || !playerInAlienZone)) {
                alien.isAggro = false
                val (wx, wy) = randomPointInZone(tier)
                alien.patrolTargetX = wx
                alien.patrolTargetY = wy
            }

            if (alien.isAggro && player.isAlive) {
                val dx = player.x - alien.x
                val dy = player.y - alien.y
                val dist = hypot(dx, dy)
                val angleToPlayer = atan2(dy, dx)
                alien.angleRad = lerpAngle(alien.angleRad, angleToPlayer, 1f - exp(-6.5f * dt))

                val orbitDist = max(175f, alien.radius * 5.2f)
                if (dist > orbitDist) {
                    alien.x += cos(angleToPlayer) * tier.chaseSpeed * dt
                    alien.y += sin(angleToPlayer) * tier.chaseSpeed * dt
                } else {
                    val strafeAngle = angleToPlayer + PI.toFloat() * 0.45f
                    alien.x += cos(strafeAngle) * (tier.chaseSpeed * 0.55f) * dt
                    alien.y += sin(strafeAngle) * (tier.chaseSpeed * 0.55f) * dt
                }

                if (dist <= tier.attackRange && alien.fireCooldown <= 0f) {
                    alien.fireCooldown = tier.fireInterval + random.nextFloat() * 0.25f
                    lasers.add(
                        LaserBolt(
                            x = alien.x + cos(angleToPlayer) * alien.radius,
                            y = alien.y + sin(angleToPlayer) * alien.radius,
                            vx = cos(angleToPlayer) * tier.laserSpeed,
                            vy = sin(angleToPlayer) * tier.laserSpeed,
                            angleRad = angleToPlayer,
                            fromPlayer = false,
                            targetAlienId = null,
                            damage = alien.laserDamage,
                            colorHex = if (alien.isBoss) 0xFFFF2E4C else tier.primaryColorHex,
                            lifeRemaining = 1.15f
                        )
                    )
                }
            } else {
                val dx = alien.patrolTargetX - alien.x
                val dy = alien.patrolTargetY - alien.y
                val dist = hypot(dx, dy)
                if (dist < 56f) {
                    val (wx, wy) = randomPointInZone(tier)
                    alien.patrolTargetX = wx
                    alien.patrolTargetY = wy
                } else {
                    val patrolAngle = atan2(dy, dx)
                    alien.angleRad = lerpAngle(alien.angleRad, patrolAngle, 1f - exp(-4f * dt))
                    alien.x += cos(patrolAngle) * tier.patrolSpeed * dt
                    alien.y += sin(patrolAngle) * tier.patrolSpeed * dt
                }
            }

            clampAlienToZoneRing(alien)
        }
    }

    private fun updateLasers(dt: Float) {
        val destroyedAliens = mutableListOf<AlienEntity>()
        val iterator = lasers.iterator()

        while (iterator.hasNext()) {
            val bolt = iterator.next()
            bolt.lifeRemaining -= dt

            if (bolt.fromPlayer && bolt.targetAlienId != null) {
                val target = aliens.firstOrNull { it.id == bolt.targetAlienId }
                if (target != null) {
                    val desiredAngle = atan2(target.y - bolt.y, target.x - bolt.x)
                    bolt.angleRad = desiredAngle
                    bolt.vx = cos(desiredAngle) * GameConfig.PLAYER_LASER_SPEED
                    bolt.vy = sin(desiredAngle) * GameConfig.PLAYER_LASER_SPEED
                }
            }

            bolt.x += bolt.vx * dt
            bolt.y += bolt.vy * dt

            if (bolt.lifeRemaining <= 0f || isOutOfActiveMapBounds(bolt.x, bolt.y)) {
                iterator.remove()
                continue
            }

            if (!bolt.fromPlayer && isPointInActiveSafeZone(bolt.x, bolt.y)) {
                spawnHitSparks(bolt.x, bolt.y, colorHex = 0xFF00E5FF, count = 4)
                iterator.remove()
                continue
            }

            if (bolt.fromPlayer) {
                var hitAlien: AlienEntity? = null
                for (alien in aliens) {
                    if (hypot(bolt.x - alien.x, bolt.y - alien.y) <= alien.radius + 9f) {
                        hitAlien = alien
                        break
                    }
                }
                if (hitAlien != null) {
                    hitAlien.hp -= bolt.damage
                    hitAlien.hitFlash = 1f
                    hitAlien.isAggro = true
                    spawnHitSparks(bolt.x, bolt.y, colorHex = hitAlien.tier.primaryColorHex, count = 4)

                    val jitterX = (random.nextFloat() - 0.5f) * 20f
                    popups.add(
                        FloatingPopup(
                            x = hitAlien.x + jitterX,
                            y = hitAlien.y - hitAlien.radius - 10f,
                            text = "-${bolt.damage.roundToInt()}",
                            life = 0.78f,
                            maxLife = 0.78f,
                            colorRgb = 0xFFFFFF,
                            textSize = 14f
                        )
                    )

                    iterator.remove()
                    if (hitAlien.hp <= 0f && hitAlien !in destroyedAliens) {
                        destroyedAliens.add(hitAlien)
                    }
                }
            } else if (player.isAlive) {
                if (hypot(bolt.x - player.x, bolt.y - player.y) <= activeShipClass.hullRadius + 6f) {
                    iterator.remove()
                    if (!player.inSafeZone) {
                        applyDamageToPlayer(bolt.damage, bolt.x, bolt.y)
                    }
                }
            }
        }

        for (dead in destroyedAliens) {
            onAlienDestroyed(dead)
        }
    }

    private fun updateRockets(dt: Float) {
        val destroyedAliens = mutableListOf<AlienEntity>()
        val iterator = rockets.iterator()

        while (iterator.hasNext()) {
            val rocket = iterator.next()
            rocket.lifeRemaining -= dt
            if (rocket.lifeRemaining <= 0f || isOutOfActiveMapBounds(rocket.x, rocket.y)) {
                iterator.remove()
                continue
            }

            val target = aliens.firstOrNull { it.id == rocket.targetAlienId }
                ?: aliens.minByOrNull { hypot(it.x - rocket.x, it.y - rocket.y) }
            if (target != null) {
                val desiredAngle = atan2(target.y - rocket.y, target.x - rocket.x)
                rocket.angleRad = lerpAngle(
                    rocket.angleRad,
                    desiredAngle,
                    1f - exp(-GameConfig.ROCKET_TURN_RATE * dt)
                )
            }

            rocket.x += cos(rocket.angleRad) * GameConfig.ROCKET_SPEED * dt
            rocket.y += sin(rocket.angleRad) * GameConfig.ROCKET_SPEED * dt

            // Emit glowing rocket exhaust smoke trail
            rocket.smokeTimer += dt
            if (rocket.smokeTimer >= 0.035f) {
                rocket.smokeTimer = 0f
                particles.add(
                    ExplosionParticle(
                        x = rocket.x - cos(rocket.angleRad) * 10f,
                        y = rocket.y - sin(rocket.angleRad) * 10f,
                        vx = -cos(rocket.angleRad) * 35f + (random.nextFloat() - 0.5f) * 28f,
                        vy = -sin(rocket.angleRad) * 35f + (random.nextFloat() - 0.5f) * 28f,
                        radius = 2.6f + random.nextFloat() * 1.6f,
                        life = 0.28f,
                        maxLife = 0.28f,
                        colorHex = if (random.nextBoolean()) 0xFFFB923C else 0xFFFACC15
                    )
                )
            }

            var hitAlien: AlienEntity? = null
            for (alien in aliens) {
                if (hypot(rocket.x - alien.x, rocket.y - alien.y) <= alien.radius + 14f) {
                    hitAlien = alien
                    break
                }
            }
            if (hitAlien != null) {
                hitAlien.hp -= rocket.damage
                hitAlien.hitFlash = 1f
                hitAlien.isAggro = true
                screenShake = max(screenShake, 5.5f)
                spawnHitSparks(rocket.x, rocket.y, colorHex = 0xFFFB923C, count = 12)
                shockwaves.add(
                    ShockwaveRing(
                        x = rocket.x,
                        y = rocket.y,
                        maxRadius = 46f,
                        life = 0.28f,
                        maxLife = 0.28f,
                        colorHex = 0xFFFB923C
                    )
                )
                popups.add(
                    FloatingPopup(
                        x = hitAlien.x,
                        y = hitAlien.y - hitAlien.radius - 16f,
                        text = "-${rocket.damage.roundToInt()} RKT",
                        life = 0.95f,
                        maxLife = 0.95f,
                        colorRgb = 0xFB923C,
                        textSize = 15.5f
                    )
                )
                iterator.remove()
                if (hitAlien.hp <= 0f && hitAlien !in destroyedAliens) {
                    destroyedAliens.add(hitAlien)
                }
            }
        }

        for (dead in destroyedAliens) {
            onAlienDestroyed(dead)
        }
    }

    private fun applyDamageToPlayer(
        amount: Float,
        hitX: Float,
        hitY: Float,
        isRadiation: Boolean = false
    ) {
        player.timeSinceDamaged = 0f
        screenShake = max(screenShake, if (isRadiation) 2.5f else 4.5f)

        var remainingDamage = amount
        val hitShieldOnly = player.shield >= amount
        if (player.shield > 0f) {
            val absorbed = min(player.shield, remainingDamage)
            player.shield -= absorbed
            remainingDamage -= absorbed
            player.shieldHitFlash = 1f
            if (!isRadiation) {
                spawnHitSparks(hitX, hitY, colorHex = 0xFF38BDF8, count = 5)
            }
        }

        if (remainingDamage > 0f) {
            player.hp -= remainingDamage
            player.hitFlash = 1f
            spawnHullHitSparks(
                x = if (isRadiation) player.x else hitX,
                y = if (isRadiation) player.y else hitY,
                count = if (isRadiation) 7 else 13
            )
        }

        val jitterX = (random.nextFloat() - 0.5f) * 18f
        val popupLabel = if (isRadiation) "-${amount.roundToInt()} RAD" else "-${amount.roundToInt()}"
        val popupColor = when {
            isRadiation -> 0xFF2E4C
            hitShieldOnly -> 0x38BDF8
            else -> 0xFF4D6D
        }
        popups.add(
            FloatingPopup(
                x = player.x + jitterX,
                y = player.y - activeShipClass.hullRadius - 28f,
                text = popupLabel,
                life = 0.85f,
                maxLife = 0.85f,
                colorRgb = popupColor,
                textSize = 14f
            )
        )

        if (player.hp <= 0f) {
            player.hp = 0f
            player.shield = 0f
            player.isAlive = false
            player.isAutoFireEnabled = false
            autopilotTargetX = null
            autopilotTargetY = null
            isTacticalMapOpen = false
            spawnExplosion(player.x, player.y, primaryHex = activeShipClass.accentColorHex, accentHex = 0xFF38BDF8, isBoss = false)
            screenShake = 14f
        }
    }

    private fun onAlienDestroyed(alien: AlienEntity) {
        val tier = alien.tier
        aliens.remove(alien)
        credits += alien.rewardCredits
        kills += 1
        addXp(alien.rewardXp)

        // Boss kills also count toward their Sector's normal alien mission objectives
        when (tier) {
            AlienTier.SCOUT -> recordMissionProgress(MissionGoalType.DESTROY_SCOUTS)
            AlienTier.HUNTER -> recordMissionProgress(MissionGoalType.DESTROY_HUNTERS)
            AlienTier.DESTROYER -> recordMissionProgress(MissionGoalType.DESTROY_DESTROYERS)
            AlienTier.DREADNOUGHT_BOSS -> Unit
        }

        val isBigBoss = alien.isBoss || tier == AlienTier.DREADNOUGHT_BOSS
        screenShake = max(screenShake, if (isBigBoss) 14f else 7.5f)

        spawnExplosion(
            x = alien.x,
            y = alien.y,
            primaryHex = if (alien.isBoss) 0xFFFBBF24 else tier.primaryColorHex,
            accentHex = if (alien.isBoss) 0xFFFF2E4C else tier.accentColorHex,
            isBoss = isBigBoss
        )

        popups.add(
            FloatingPopup(
                x = alien.x,
                y = alien.y - alien.radius - 14f,
                text = if (alien.isBoss) {
                    "BOSS! +${alien.rewardCredits} CR  +${alien.rewardXp} XP"
                } else {
                    "+${alien.rewardCredits} CR  +${alien.rewardXp} XP"
                },
                life = if (alien.isBoss) 1.55f else 1.25f,
                maxLife = if (alien.isBoss) 1.55f else 1.25f,
                colorRgb = 0xFBBF24,
                textSize = if (isBigBoss) 17f else 14f
            )
        )

        // Guaranteed Cargo Box drop (Boss aliens drop scaled 4x-based cargo rewards)
        val cargoBonus = ((alien.rewardCredits * (0.5f + random.nextFloat() * 0.35f)) / 25f).roundToInt() * 25
        lootBoxes.add(
            LootBoxEntity(
                id = nextLootId++,
                kind = LootBoxKind.CARGO_BOX,
                x = alien.x,
                y = alien.y,
                creditReward = max(50, cargoBonus),
                shieldReward = 0f,
                xpReward = 0,
                pulsePhase = 0f,
                lifeRemaining = GameConfig.CARGO_LIFETIME_SEC
            )
        )

        if (lockedAlienId == alien.id) {
            lockedAlienId = null
        }

        spawnAlienForTier(tier, minDistanceFromPlayer = 600f)
        requestSave()
    }

    private fun isOutOfActiveMapBounds(x: Float, y: Float): Boolean {
        return if (activeMap == GameMap.MAP_X1) {
            val pad = GameConfig.MAP_X1_RADIATION_OUTER_BUFFER + 180f
            x < -pad || x > GameConfig.MAP_X1_SIZE + pad || y < -pad || y > GameConfig.MAP_X1_SIZE + pad
        } else {
            hypot(x, y) > GameConfig.MAP_RADIUS + 100f
        }
    }

    private fun randomPointInX1Playable(margin: Float, safeZoneBuffer: Float): Pair<Float, Float> {
        val minCoord = margin
        val maxCoord = GameConfig.MAP_X1_SIZE - margin
        val minSafeDist = GameConfig.MAP_X1_SAFE_ZONE_RADIUS + safeZoneBuffer
        repeat(24) {
            val rx = minCoord + random.nextFloat() * (maxCoord - minCoord)
            val ry = minCoord + random.nextFloat() * (maxCoord - minCoord)
            if (hypot(rx - GameConfig.MAP_X1_SAFE_ZONE_X, ry - GameConfig.MAP_X1_SAFE_ZONE_Y) >= minSafeDist) {
                return Pair(rx, ry)
            }
        }
        return Pair(GameConfig.MAP_X1_SIZE * 0.5f, GameConfig.MAP_X1_SIZE * 0.5f)
    }

    private fun spawnBonusBox() {
        val bx: Float
        val by: Float
        val creditAmount: Int
        val xpAmount: Int
        val shieldAmount: Float

        if (activeMap == GameMap.MAP_X1) {
            val (rx, ry) = randomPointInX1Playable(margin = 260f, safeZoneBuffer = 120f)
            bx = rx
            by = ry
            val distFromCorner = hypot(
                bx - GameConfig.MAP_X1_SAFE_ZONE_X,
                by - GameConfig.MAP_X1_SAFE_ZONE_Y
            )
            when {
                distFromCorner < 5000f -> {
                    creditAmount = (5 + random.nextInt(6)) * 25
                    xpAmount = 20
                    shieldAmount = 50f
                }
                distFromCorner < 10500f -> {
                    creditAmount = (9 + random.nextInt(8)) * 25
                    xpAmount = 40
                    shieldAmount = 80f
                }
                else -> {
                    creditAmount = (14 + random.nextInt(10)) * 25
                    xpAmount = 65
                    shieldAmount = 110f
                }
            }
        } else {
            val (rx, ry) = randomPointInAnnulus(
                minRadius = GameConfig.SAFE_ZONE_RADIUS + 90f,
                maxRadius = GameConfig.MAP_RADIUS - 160f,
                rng = random
            )
            bx = rx
            by = ry
            val dist = hypot(bx, by)
            val zone = resolveZone(dist)
            creditAmount = when (zone) {
                SpaceZone.ZONE_1_ALPHA -> (4 + random.nextInt(5)) * 25
                SpaceZone.ZONE_2_BETA -> (8 + random.nextInt(8)) * 25
                SpaceZone.ZONE_3_GAMMA -> (16 + random.nextInt(11)) * 25
                SpaceZone.ZONE_4_OMEGA -> (28 + random.nextInt(17)) * 25
                SpaceZone.SAFE_ZONE -> 100
            }
            xpAmount = when (zone) {
                SpaceZone.ZONE_1_ALPHA -> 15
                SpaceZone.ZONE_2_BETA -> 35
                SpaceZone.ZONE_3_GAMMA -> 70
                SpaceZone.ZONE_4_OMEGA -> 140
                SpaceZone.SAFE_ZONE -> 10
            }
            shieldAmount = when (zone) {
                SpaceZone.ZONE_1_ALPHA -> 40f
                SpaceZone.ZONE_2_BETA -> 70f
                SpaceZone.ZONE_3_GAMMA -> 110f
                else -> 160f
            }
        }
        val isShieldBox = random.nextFloat() < 0.24f
        lootBoxes.add(
            LootBoxEntity(
                id = nextLootId++,
                kind = LootBoxKind.BONUS_BOX,
                x = bx,
                y = by,
                creditReward = creditAmount,
                shieldReward = if (isShieldBox) shieldAmount else 0f,
                xpReward = xpAmount,
                pulsePhase = random.nextFloat() * 6.28f,
                lifeRemaining = Float.POSITIVE_INFINITY
            )
        )
    }

    private fun spawnAlienForTier(tier: AlienTier, minDistanceFromPlayer: Float) {
        var (x, y) = randomPointInZone(tier)
        for (attempt in 0 until 16) {
            val (cx, cy) = randomPointInZone(tier)
            if (hypot(cx - player.x, cy - player.y) >= minDistanceFromPlayer) {
                x = cx
                y = cy
                break
            }
        }
        val (wx, wy) = randomPointInZone(tier)
        val isBossVariant = random.nextFloat() < GameConfig.BOSS_SPAWN_CHANCE
        aliens.add(
            AlienEntity(
                id = nextAlienId++,
                tier = tier,
                isBoss = isBossVariant,
                x = x,
                y = y,
                angleRad = random.nextFloat() * 2f * PI.toFloat(),
                patrolTargetX = wx,
                patrolTargetY = wy,
                fireCooldown = 0.55f + random.nextFloat() * 0.6f,
                pulsePhase = random.nextFloat() * 6.28f
            )
        )
    }

    private fun randomPointInZone(tier: AlienTier): Pair<Float, Float> {
        if (activeMap == GameMap.MAP_X1) {
            val margin = 280f + tier.radius
            return randomPointInX1Playable(margin = margin, safeZoneBuffer = 200f + tier.radius)
        }
        val inner = tier.minRadius + tier.radius + 50f
        val outer = tier.maxRadius - tier.radius - 50f
        return randomPointInAnnulus(inner, outer, random)
    }

    private fun clampAlienToZoneRing(alien: AlienEntity) {
        if (activeMap == GameMap.MAP_X1) {
            val pad = alien.radius + 90f
            alien.x = alien.x.coerceIn(pad, GameConfig.MAP_X1_SIZE - pad)
            alien.y = alien.y.coerceIn(pad, GameConfig.MAP_X1_SIZE - pad)
            val dx = alien.x - GameConfig.MAP_X1_SAFE_ZONE_X
            val dy = alien.y - GameConfig.MAP_X1_SAFE_ZONE_Y
            val minDist = GameConfig.MAP_X1_SAFE_ZONE_RADIUS + alien.radius + 36f
            val dist = hypot(dx, dy)
            if (dist < minDist) {
                if (dist < 0.001f) {
                    alien.x = GameConfig.MAP_X1_SAFE_ZONE_X + minDist
                    alien.y = GameConfig.MAP_X1_SAFE_ZONE_Y
                } else {
                    val scale = minDist / dist
                    alien.x = GameConfig.MAP_X1_SAFE_ZONE_X + dx * scale
                    alien.y = GameConfig.MAP_X1_SAFE_ZONE_Y + dy * scale
                }
            }
            return
        }
        val innerR = alien.tier.minRadius + alien.radius + 26f
        val outerR = alien.tier.maxRadius - alien.radius - 26f
        val d = hypot(alien.x, alien.y)
        if (d < 0.001f) {
            alien.x = innerR
            alien.y = 0f
            return
        }
        if (d < innerR) {
            val scale = innerR / d
            alien.x *= scale
            alien.y *= scale
        } else if (d > outerR) {
            val scale = outerR / d
            alien.x *= scale
            alien.y *= scale
        }
    }

    private fun randomPointInAnnulus(
        minRadius: Float,
        maxRadius: Float,
        rng: Random
    ): Pair<Float, Float> {
        val angle = rng.nextFloat() * 2f * PI.toFloat()
        val rMinSq = minRadius * minRadius
        val rMaxSq = maxRadius * maxRadius
        val r = sqrt(rMinSq + rng.nextFloat() * (rMaxSq - rMinSq))
        return Pair(cos(angle) * r, sin(angle) * r)
    }

    private fun spawnExplosion(
        x: Float,
        y: Float,
        primaryHex: Long,
        accentHex: Long,
        isBoss: Boolean
    ) {
        shockwaves.add(
            ShockwaveRing(
                x = x,
                y = y,
                maxRadius = if (isBoss) 140f else 76f,
                life = if (isBoss) 0.65f else 0.42f,
                maxLife = if (isBoss) 0.65f else 0.42f,
                colorHex = primaryHex
            )
        )
        if (isBoss) {
            shockwaves.add(
                ShockwaveRing(
                    x = x,
                    y = y,
                    maxRadius = 92f,
                    life = 0.48f,
                    maxLife = 0.48f,
                    colorHex = 0xFFFFFFFF
                )
            )
        }

        val count = if (isBoss) 34 else 18
        val colors = longArrayOf(primaryHex, accentHex, 0xFFFACC15, 0xFFFFFFFF)
        repeat(count) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val speed = (if (isBoss) 95f else 65f) + random.nextFloat() * (if (isBoss) 280f else 210f)
            val life = (if (isBoss) 0.55f else 0.36f) + random.nextFloat() * 0.45f
            particles.add(
                ExplosionParticle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    radius = (if (isBoss) 3.4f else 2.2f) + random.nextFloat() * 3.8f,
                    life = life,
                    maxLife = life,
                    colorHex = colors[random.nextInt(colors.size)]
                )
            )
        }
    }

    private fun spawnHitSparks(x: Float, y: Float, colorHex: Long, count: Int) {
        repeat(count) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val speed = 60f + random.nextFloat() * 140f
            val life = 0.16f + random.nextFloat() * 0.18f
            particles.add(
                ExplosionParticle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    radius = 1.6f + random.nextFloat() * 1.8f,
                    life = life,
                    maxLife = life,
                    colorHex = colorHex
                )
            )
        }
    }

    private fun spawnHullHitSparks(x: Float, y: Float, count: Int) {
        val sparkPalette = longArrayOf(0xFFFF6D00, 0xFFFF2E4C, 0xFFFB923C, 0xFFFBBF24)
        repeat(count) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val speed = 85f + random.nextFloat() * 175f
            val life = 0.18f + random.nextFloat() * 0.22f
            val jitter = 4f + random.nextFloat() * 8f
            particles.add(
                ExplosionParticle(
                    x = x + cos(angle) * jitter,
                    y = y + sin(angle) * jitter,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    radius = 1.8f + random.nextFloat() * 2.1f,
                    life = life,
                    maxLife = life,
                    colorHex = sparkPalette[random.nextInt(sparkPalette.size)]
                )
            )
        }
    }

    private fun updateEffects(dt: Float) {
        val pIt = particles.iterator()
        while (pIt.hasNext()) {
            val p = pIt.next()
            p.life -= dt
            if (p.life <= 0f) {
                pIt.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vx *= (1f - 1.9f * dt).coerceAtLeast(0f)
                p.vy *= (1f - 1.9f * dt).coerceAtLeast(0f)
            }
        }

        val sIt = shockwaves.iterator()
        while (sIt.hasNext()) {
            val s = sIt.next()
            s.life -= dt
            if (s.life <= 0f) {
                sIt.remove()
            }
        }

        val popIt = popups.iterator()
        while (popIt.hasNext()) {
            val pop = popIt.next()
            pop.life -= dt
            if (pop.life <= 0f) {
                popIt.remove()
            } else {
                pop.y -= 34f * dt
            }
        }
    }

    private fun generateCircularStars(): List<Star> {
        val starRng = Random(42019L)
        return List(GameConfig.STAR_COUNT) {
            val angle = starRng.nextFloat() * 2f * PI.toFloat()
            val r = sqrt(starRng.nextFloat()) * (GameConfig.MAP_RADIUS - 30f)
            Star(
                x = cos(angle) * r,
                y = sin(angle) * r,
                radius = 0.8f + starRng.nextFloat() * 2.1f,
                alpha = 0.25f + starRng.nextFloat() * 0.72f,
                tintType = starRng.nextInt(3)
            )
        }
    }

    private fun generateSquareX1Stars(): List<Star> {
        val starRng = Random(77411L)
        return List(GameConfig.MAP_X1_STAR_COUNT) {
            Star(
                x = starRng.nextFloat() * GameConfig.MAP_X1_SIZE,
                y = starRng.nextFloat() * GameConfig.MAP_X1_SIZE,
                radius = 0.85f + starRng.nextFloat() * 2.15f,
                alpha = 0.28f + starRng.nextFloat() * 0.70f,
                tintType = starRng.nextInt(3)
            )
        }
    }

    private fun generateCircularNebulae(): List<NebulaCloud> {
        val nebRng = Random(99103L)
        val palette = longArrayOf(0x181D4ED8, 0x160E7490, 0x166D28D9, 0x1510B981, 0x149F1239)
        return List(GameConfig.NEBULA_COUNT) {
            val angle = nebRng.nextFloat() * 2f * PI.toFloat()
            val r = sqrt(nebRng.nextFloat()) * (GameConfig.MAP_RADIUS * 0.9f)
            NebulaCloud(
                x = cos(angle) * r,
                y = sin(angle) * r,
                radius = 280f + nebRng.nextFloat() * 440f,
                colorHex = palette[nebRng.nextInt(palette.size)]
            )
        }
    }

    private fun generateSquareX1Nebulae(): List<NebulaCloud> {
        val nebRng = Random(51827L)
        val palette = longArrayOf(0x1C312E81, 0x1A4C1D95, 0x181E3A8A, 0x166D28D9, 0x140E7490)
        return List(GameConfig.MAP_X1_NEBULA_COUNT) {
            NebulaCloud(
                x = 600f + nebRng.nextFloat() * (GameConfig.MAP_X1_SIZE - 1200f),
                y = 600f + nebRng.nextFloat() * (GameConfig.MAP_X1_SIZE - 1200f),
                radius = 340f + nebRng.nextFloat() * 480f,
                colorHex = palette[nebRng.nextInt(palette.size)]
            )
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a > PI) a -= (2.0 * PI).toFloat()
        while (a < -PI) a += (2.0 * PI).toFloat()
        return a
    }

    private fun lerpAngle(current: Float, target: Float, factor: Float): Float {
        val diff = normalizeAngle(target - current)
        return current + diff * factor
    }
}
