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

    val stars: List<Star> = generateCircularStars()
    val nebulae: List<NebulaCloud> = generateCircularNebulae()

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
        for (savedVant in state.ownedVants.take(GameConfig.MAX_VANTS)) {
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
        nextVantId = max(
            state.nextVantId,
            (ownedVants.maxOfOrNull { it.id } ?: 0) + 1
        )

        equippedSectorChip = state.equippedSectorChipId?.let { byId[it] }
            ?.takeIf { it.type.category == EquipmentCategory.SECTOR_CHIP }

        currentMissionIndex = state.currentMissionIndex.coerceIn(0, MissionCatalog.all.size)
        val activeMission = MissionCatalog.all.getOrNull(currentMissionIndex)
        currentMissionProgress = if (activeMission != null) {
            state.currentMissionProgress.coerceIn(0, activeMission.targetCount)
        } else {
            0
        }

        recalculateShipStats(refillVitals = true)
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
            // Starter loadout: 1x LF-1 Laser and 1x SG-1 Shield on Interceptor
            val starterLaser = EquipmentItem(nextItemInstanceId++, EquipmentItemId.LF_1)
            val starterShield = EquipmentItem(nextItemInstanceId++, EquipmentItemId.SG_1)
            ownedItems.add(starterLaser)
            ownedItems.add(starterShield)
            config1LaserSlots[0] = starterLaser
            config1ShieldSlots[0] = starterShield
            config2LaserSlots[0] = starterLaser
            config2ShieldSlots[0] = starterShield
        }
        recalculateShipStats(refillVitals = true)

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

        lasers.clear()
        rockets.clear()
        particles.clear()
        shockwaves.clear()
        popups.clear()
        aliens.clear()
        lootBoxes.clear()

        for (tier in AlienTier.entries) {
            repeat(tier.activeCount) {
                spawnAlienForTier(tier, minDistanceFromPlayer = 360f)
            }
        }
        repeat(GameConfig.BONUS_BOX_COUNT) {
            spawnBonusBox()
        }

        if (triggerSave) {
            requestSave()
        }
    }

    fun relaunchShip() {
        recalculateShipStats(refillVitals = true)
        player.x = 0f
        player.y = 0f
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
        isHangarOpen = true
        isPaused = true
    }

    fun closeHangar() {
        isHangarOpen = false
        isPaused = false
        recalculateShipStats(refillVitals = false)
        requestSave()
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

    fun buyFlaxVant(): Boolean {
        if (ownedVants.size >= GameConfig.MAX_VANTS) return false
        if (credits < GameConfig.FLAX_VANT_COST) return false
        credits -= GameConfig.FLAX_VANT_COST
        ownedVants.add(VantDrone(id = nextVantId++, tier = VantTier.FLAX))
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
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun equipItemToVantSlot(vantId: Int, slotIndex: Int, instanceId: Int? = null): Boolean {
        val vant = ownedVants.firstOrNull { it.id == vantId } ?: return false
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

        for (vant in ownedVants) {
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
        val vant = ownedVants.firstOrNull { it.id == vantId } ?: return false
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
        for (vant in ownedVants) {
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
        val activeEquippedIds = HashSet<Int>()
        for (item in laserSlots) if (item != null) activeEquippedIds.add(item.instanceId)
        for (item in shieldSlots) if (item != null) activeEquippedIds.add(item.instanceId)
        for (vant in ownedVants) {
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
        for (vant in ownedVants) {
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
        for (vant in ownedVants) {
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

        // Include all Lasers and Shields equipped inside owned VANT drones for the active Config
        for (vant in ownedVants) {
            for (slot in vant.slotsForConfig(activeConfig)) {
                if (slot != null) {
                    bonusDamage += slot.type.damageBonus
                    bonusShield += slot.type.shieldBonus
                }
            }
        }

        player.shipClass = activeShipClass
        player.laserDamage = GameConfig.PLAYER_BASE_LASER_DAMAGE + bonusDamage
        player.maxShield = GameConfig.PLAYER_BASE_MAX_SHIELD + bonusShield
        // Ship speed is strictly determined by the equipped Ship Class (no Speed Engines)
        player.speed = activeShipClass.speed
        player.maxHp = activeShipClass.baseHull + (player.level - 1) * GameConfig.PLAYER_HP_PER_LEVEL

        if (refillVitals) {
            player.hp = player.maxHp
            player.shield = player.maxShield
        } else {
            if (shieldDeltaAdded > 0f && player.inSafeZone) {
                player.shield = min(player.maxShield, player.shield + shieldDeltaAdded)
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
            playerHp = player.hp.toInt().coerceAtLeast(0),
            playerMaxHp = player.maxHp.toInt(),
            playerShield = player.shield.toInt().coerceAtLeast(0),
            playerMaxShield = player.maxShield.toInt(),
            coordX = player.x.toInt(),
            coordY = player.y.toInt(),
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
                vants = ownedVants.map { vant ->
                    VantSnapshot(
                        id = vant.id,
                        tier = vant.tier,
                        activeSlots = vant.slotsForConfig(activeConfig).toList()
                    )
                },
                flaxVantCount = flaxCount,
                irisVantCount = irisCount,
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

        val mag = hypot(moveInputX, moveInputY)
        if (mag > GameConfig.JOYSTICK_DEADZONE) {
            val targetAngle = atan2(moveInputY, moveInputX)
            player.angleRad = lerpAngle(player.angleRad, targetAngle, 1f - exp(-GameConfig.PLAYER_TURN_SPEED * dt))
            val dirX = moveInputX / mag
            val dirY = moveInputY / mag
            player.x += dirX * player.speed * dt
            player.y += dirY * player.speed * dt
            player.isMoving = true
            player.thrusterPulse += dt * 18f
        } else {
            player.isMoving = false
            val locked = getLockedAlien()
            if (player.isAutoFireEnabled && !player.inSafeZone && locked != null && isLockedTargetInRange()) {
                val aimAngle = atan2(locked.y - player.y, locked.x - player.x)
                player.angleRad = lerpAngle(player.angleRad, aimAngle, 1f - exp(-8f * dt))
            }
        }

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

        if (player.inSafeZone && player.isAutoFireEnabled) {
            player.isAutoFireEnabled = false
        }

        if (player.hitFlash > 0f) {
            player.hitFlash = max(0f, player.hitFlash - dt * 4f)
        }
        if (player.shieldHitFlash > 0f) {
            player.shieldHitFlash = max(0f, player.shieldHitFlash - dt * 4.5f)
        }

        player.timeSinceDamaged += dt
        if (player.inSafeZone) {
            if (player.hp < player.maxHp) {
                player.hp = min(player.maxHp, player.hp + GameConfig.SAFE_ZONE_HP_REGEN_RATE * dt)
            }
            if (player.shield < player.maxShield) {
                player.shield = min(player.maxShield, player.shield + GameConfig.SAFE_ZONE_SHIELD_REGEN_RATE * dt)
            }
        } else if (player.timeSinceDamaged >= GameConfig.SHIELD_REGEN_DELAY) {
            if (player.shield < player.maxShield) {
                player.shield = min(player.maxShield, player.shield + GameConfig.SHIELD_REGEN_RATE * dt)
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

            val playerDistFromOrigin = hypot(player.x, player.y)
            val playerInAlienZone = playerDistFromOrigin >= tier.minRadius - 120f &&
                playerDistFromOrigin <= tier.maxRadius + 120f

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
                            damage = tier.laserDamage,
                            colorHex = tier.primaryColorHex,
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

            if (bolt.lifeRemaining <= 0f || hypot(bolt.x, bolt.y) > GameConfig.MAP_RADIUS + 100f) {
                iterator.remove()
                continue
            }

            if (!bolt.fromPlayer && hypot(bolt.x, bolt.y) <= GameConfig.SAFE_ZONE_RADIUS) {
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
            if (rocket.lifeRemaining <= 0f || hypot(rocket.x, rocket.y) > GameConfig.MAP_RADIUS + 100f) {
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
            if (!isRadiation) {
                spawnHitSparks(hitX, hitY, colorHex = 0xFFFF2E4C, count = 5)
            }
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
            spawnExplosion(player.x, player.y, primaryHex = activeShipClass.accentColorHex, accentHex = 0xFF38BDF8, isBoss = false)
            screenShake = 14f
        }
    }

    private fun onAlienDestroyed(alien: AlienEntity) {
        val tier = alien.tier
        aliens.remove(alien)
        credits += tier.rewardCredits
        kills += 1
        addXp(tier.rewardXp)

        when (tier) {
            AlienTier.SCOUT -> recordMissionProgress(MissionGoalType.DESTROY_SCOUTS)
            AlienTier.HUNTER -> recordMissionProgress(MissionGoalType.DESTROY_HUNTERS)
            AlienTier.DESTROYER -> recordMissionProgress(MissionGoalType.DESTROY_DESTROYERS)
            AlienTier.DREADNOUGHT_BOSS -> Unit
        }

        val isBoss = tier == AlienTier.DREADNOUGHT_BOSS
        screenShake = max(screenShake, if (isBoss) 14f else 7.5f)

        spawnExplosion(
            x = alien.x,
            y = alien.y,
            primaryHex = tier.primaryColorHex,
            accentHex = tier.accentColorHex,
            isBoss = isBoss
        )

        popups.add(
            FloatingPopup(
                x = alien.x,
                y = alien.y - alien.radius - 14f,
                text = "+${tier.rewardCredits} CR  +${tier.rewardXp} XP",
                life = 1.25f,
                maxLife = 1.25f,
                colorRgb = 0xFBBF24,
                textSize = if (isBoss) 17f else 14f
            )
        )

        val cargoBonus = ((tier.rewardCredits * (0.5f + random.nextFloat() * 0.35f)) / 25f).roundToInt() * 25
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

        spawnAlienForTier(tier, minDistanceFromPlayer = 480f)
        requestSave()
    }

    private fun spawnBonusBox() {
        val (bx, by) = randomPointInAnnulus(
            minRadius = GameConfig.SAFE_ZONE_RADIUS + 90f,
            maxRadius = GameConfig.MAP_RADIUS - 160f,
            rng = random
        )
        val dist = hypot(bx, by)
        val zone = resolveZone(dist)
        val creditAmount = when (zone) {
            SpaceZone.ZONE_1_ALPHA -> (4 + random.nextInt(5)) * 25
            SpaceZone.ZONE_2_BETA -> (8 + random.nextInt(8)) * 25
            SpaceZone.ZONE_3_GAMMA -> (16 + random.nextInt(11)) * 25
            SpaceZone.ZONE_4_OMEGA -> (28 + random.nextInt(17)) * 25
            SpaceZone.SAFE_ZONE -> 100
        }
        val xpAmount = when (zone) {
            SpaceZone.ZONE_1_ALPHA -> 15
            SpaceZone.ZONE_2_BETA -> 35
            SpaceZone.ZONE_3_GAMMA -> 70
            SpaceZone.ZONE_4_OMEGA -> 140
            SpaceZone.SAFE_ZONE -> 10
        }
        val isShieldBox = random.nextFloat() < 0.24f
        val shieldAmount = when (zone) {
            SpaceZone.ZONE_1_ALPHA -> 40f
            SpaceZone.ZONE_2_BETA -> 70f
            SpaceZone.ZONE_3_GAMMA -> 110f
            else -> 160f
        }
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
        aliens.add(
            AlienEntity(
                id = nextAlienId++,
                tier = tier,
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
        val inner = tier.minRadius + tier.radius + 50f
        val outer = tier.maxRadius - tier.radius - 50f
        return randomPointInAnnulus(inner, outer, random)
    }

    private fun clampAlienToZoneRing(alien: AlienEntity) {
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
