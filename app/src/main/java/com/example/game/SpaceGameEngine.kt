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

data class AutoMoveTarget(
    val x: Float,
    val y: Float
)

class SpaceGameEngine(
    private val random: Random = Random.Default,
    private val onSaveRequested: ((SaveState) -> Unit)? = null
) {
    val player = PlayerState()
    val aliens = mutableListOf<AlienEntity>()
    val lootBoxes = mutableListOf<LootBoxEntity>()
    val resourceNodes = mutableListOf<ResourceNodeEntity>()
    val alienWrecks = mutableListOf<AlienWreck>()
    val lasers = mutableListOf<LaserBolt>()
    val rockets = mutableListOf<HomingRocket>()
    val particles = mutableListOf<ExplosionParticle>()
    val shockwaves = mutableListOf<ShockwaveRing>()
    val popups = mutableListOf<FloatingPopup>()

    private var sectorStars: List<Star> = generateSectorStars(GalaxyMapGraph.DEFAULT_SECTOR_ID)
    private var sectorNebulae: List<NebulaCloud> = generateSectorNebulae(GalaxyMapGraph.DEFAULT_SECTOR_ID)

    val stars: List<Star> get() = sectorStars
    val nebulae: List<NebulaCloud> get() = sectorNebulae

    var currentSectorId: String = GalaxyMapGraph.DEFAULT_SECTOR_ID
        private set
    val currentSector: SectorNode
        get() = GalaxyMapGraph.sectorOf(currentSectorId)

    var activePortals: List<SectorPortal> = buildBorderPortalsForSector(GalaxyMapGraph.DEFAULT_SECTOR_ID)
        private set

    var activeStations: List<SectorStation> = buildStationsForSector(GalaxyMapGraph.DEFAULT_SECTOR_ID)
        private set

    // Player resource cargo (raw ores & refined goods)
    val rawCargo: MutableMap<ResourceType, Int> = linkedMapOf()
    val refinedCargo: MutableMap<ResourceType, Int> = linkedMapOf()

    // Crafted Industrial Ships (Hangar) & Crafted Tools/Consumables (Inventory)
    val craftedHangarShips: MutableList<CraftedShipRecord> = mutableListOf()
    val craftedInventoryItems: MutableList<CraftedItemRecord> = mutableListOf()
    private var nextCraftedRecordId: Int = 1
    private var nextCraftOutcomeId: Long = 1L

    // Strictly isolated local markets keyed independently by sectorId ("C1", "C2", "C3", "0-0")
    private class MutableMarketResourceEntry(
        val resourceType: ResourceType,
        val isRefined: Boolean,
        var buyPrice: Int,
        var sellPrice: Int,
        var localStock: Int
    )

    private class MutableMarketEquipmentEntry(
        val itemId: EquipmentItemId,
        var buyPrice: Int,
        var localStock: Int
    )

    private class IsolatedLocalMarket(
        val sectorId: String,
        val marketName: String,
        val isBlackMarket: Boolean,
        val resourceEntries: MutableMap<Pair<ResourceType, Boolean>, MutableMarketResourceEntry>,
        val equipmentEntries: MutableList<MutableMarketEquipmentEntry>
    ) {
        fun toSnapshot(): LocalMarketSnapshot = LocalMarketSnapshot(
            sectorId = sectorId,
            marketName = marketName,
            isBlackMarket = isBlackMarket,
            resourceListings = resourceEntries.values.map {
                MarketResourceListing(
                    resourceType = it.resourceType,
                    isRefined = it.isRefined,
                    buyPrice = it.buyPrice,
                    sellPrice = it.sellPrice,
                    localStock = it.localStock
                )
            },
            equipmentListings = equipmentEntries.map {
                MarketEquipmentListing(
                    itemId = it.itemId,
                    buyPrice = it.buyPrice,
                    localStock = it.localStock
                )
            }
        )
    }

    private val isolatedLocalMarkets: MutableMap<String, IsolatedLocalMarket> = createIsolatedLocalMarkets()

    var activeStationOverlay: StationKind? = null
        private set

    var activeMiningNodeId: Int? = null
        private set
    private var nextResourceNodeId: Int = 1

    var activeSalvageWreckId: Int? = null
        private set
    private var nextAlienWreckId: Int = 1
    private var extractionFeedbackCooldown: Float = 0f

    var plottedRoute: List<String> = emptyList()
        private set

    val nextRouteHopSectorId: String?
        get() = if (plottedRoute.size >= 2 && plottedRoute[0] == currentSectorId) plottedRoute[1] else null

    val activeMap: GameMap
        get() = GameMap.MAP_X1

    var warpFlashTimer: Float = 0f
        private set

    // Owned Ship Classes, Permanent Pilot Capsule & Active Vessel State
    val ownedShipClasses: MutableSet<ShipClass> = mutableSetOf()
    var activeShipClass: ShipClass = ShipClass.INTERCEPTOR
        private set
    var isFlyingCapsule: Boolean = true
        private set
    var activeCraftedShipId: Int? = null
        private set
    val basicVendorItems: MutableMap<BasicVendorItemId, Int> = linkedMapOf()
    val playerShipWrecks: MutableList<PlayerShipWreck> = mutableListOf()
    private var nextPlayerWreckId: Int = 1
    var lastDeathSummary: DeathSummary? = null
        private set

    // Toggleable Admin / Debug Testing Mode (cleanly isolated for easy removal)
    var isAdminMode: Boolean = false
        private set

    fun activeCraftedShip(): CraftedShipRecord? =
        activeCraftedShipId?.let { id -> craftedHangarShips.firstOrNull { it.instanceId == id } }

    fun activeShipRole(): ShipRole = when {
        isFlyingCapsule -> ShipRole.CAPSULE
        activeCraftedShip() != null -> ShipRole.INDUSTRIAL
        else -> activeShipClass.role
    }

    fun activeIndustrialType(): IndustrialType? = when {
        isFlyingCapsule -> null
        activeCraftedShip() != null -> activeCraftedShip()?.industrialType
        else -> activeShipClass.industrialType
    }

    fun activeHullRadius(): Float = player.hullRadius

    fun activeShipDisplayName(): String = when {
        isFlyingCapsule -> GameStrings.of(language).capsuleName
        activeCraftedShip() != null -> GameStrings.stationCraftRecipeName(activeCraftedShip()!!.recipeId, language)
        else -> activeShipClass.displayName
    }

    private fun visualShipClassForIndustrial(type: IndustrialType): ShipClass = when (type) {
        IndustrialType.MINER, IndustrialType.PLASMA_COLLECTOR -> ShipClass.INTERCEPTOR
        IndustrialType.MEDIUM_HAULER -> ShipClass.BATTLECRUISER
        IndustrialType.HEAVY_HAULER -> ShipClass.DREADNOUGHT
    }

    /**
     * Authoritative sync of the player's active ship visual & structural identity.
     * Guarantees the renderer and physics always reflect the exact piloted vessel
     * (Permanent Capsule, any of the 4 Industrial Ships, or a Combat Ship) with no stale visual state.
     */
    private fun syncPlayerActiveShipVisualState() {
        val crafted = activeCraftedShip()
        when {
            isFlyingCapsule -> {
                activeCraftedShipId = null
                activeShipClass = ShipClass.INTERCEPTOR
                player.shipRole = ShipRole.CAPSULE
                player.industrialType = null
                player.activeHullId = ShipHullCatalog.capsule.id
                player.hullRadius = CapsulePermanentConfig.HULL_RADIUS
                player.shipClass = ShipClass.INTERCEPTOR
                player.shipQuality = CraftQuality.STANDARD
            }
            crafted != null -> {
                val hullDef = crafted.recipeId.shipHullDefinition()
                    ?: ShipHullCatalog.industrialT1(crafted.industrialType)
                activeShipClass = visualShipClassForIndustrial(crafted.industrialType)
                player.shipRole = ShipRole.INDUSTRIAL
                player.industrialType = crafted.industrialType
                player.activeHullId = hullDef.id
                player.hullRadius = hullDef.hullRadius
                player.shipClass = activeShipClass
                player.shipQuality = crafted.quality
            }
            else -> {
                player.shipRole = activeShipClass.role
                player.industrialType = activeShipClass.industrialType
                player.activeHullId = ShipHullCatalog.forCombatClass(activeShipClass).id
                player.hullRadius = activeShipClass.hullRadius
                player.shipClass = activeShipClass
            }
        }
    }

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

    // Interactive Tactical Map, Galactic Map & Direct Tap-to-Move Navigation
    var isTacticalMapOpen: Boolean = false
        private set
    var isGalaxyMapOpen: Boolean = false
        private set
    var autoMoveTarget: AutoMoveTarget? = null
        private set
    val autopilotTargetX: Float?
        get() = autoMoveTarget?.x
    val autopilotTargetY: Float?
        get() = autoMoveTarget?.y

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
    var overchargedAmmo: Int = 0
        private set
    var language: GameLanguage = GameLanguage.EN
    var isPaused: Boolean = false
        private set
    var isHangarOpen: Boolean = false
        private set
    var elapsedTime: Float = 0f
        private set

    // Pilot Profile (Company + Callsign) persisted via SharedPreferences
    var pilotName: String? = null
        private set
    var pilotCompany: PilotCompany? = null
        private set

    val hasRegisteredPilot: Boolean
        get() = !pilotName.isNullOrBlank() && pilotCompany != null

    fun setPilotProfile(name: String, company: PilotCompany) {
        val cleaned = name.trim().take(14)
        if (cleaned.length < 3) return
        pilotName = cleaned
        pilotCompany = company
        val homeCity = GalaxyMapGraph.homeCityForCompany(company)
        enterSector(homeCity, fromSectorId = null, triggerWarpFlash = false, showPopup = false)
        requestSave()
    }

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
                    config2SlotIds = vant.config2Slots.map { it?.instanceId },
                    durabilityPct = vant.durabilityPct
                )
            },
            ownedFormations = ownedFormations.toSet(),
            activeFormation = activeFormation,
            equippedSectorChipId = equippedSectorChip?.instanceId,
            currentMissionIndex = currentMissionIndex,
            currentMissionProgress = currentMissionProgress,
            pilotName = pilotName,
            companyId = pilotCompany?.id,
            currentSectorId = currentSectorId,
            rawCargo = rawCargo.filterValues { it > 0 }.toMap(),
            refinedCargo = refinedCargo.filterValues { it > 0 }.toMap(),
            overchargedAmmo = overchargedAmmo,
            activeShipQuality = player.shipQuality,
            pilotSkills = player.pilotSkills.toMap(),
            craftedHangarShips = craftedHangarShips.toList(),
            craftedInventoryItems = craftedInventoryItems.toList(),
            isFlyingCapsule = isFlyingCapsule,
            activeCraftedShipId = activeCraftedShipId,
            basicVendorItems = basicVendorItems.filterValues { it > 0 }.toMap()
        )
    }

    fun restoreSaveState(state: SaveState) {
        pilotName = state.pilotName?.trim()?.takeIf { it.length in 3..14 }
        pilotCompany = PilotCompany.fromId(state.companyId)
        credits = state.credits.coerceAtLeast(0)
        kills = state.kills.coerceAtLeast(0)
        player.level = state.level.coerceAtLeast(1)
        player.xp = state.xp.coerceAtLeast(0)
        activeConfig = if (state.activeConfig == 2) 2 else 1

        craftedHangarShips.clear()
        craftedHangarShips.addAll(state.craftedHangarShips)
        craftedInventoryItems.clear()
        craftedInventoryItems.addAll(state.craftedInventoryItems)
        val maxShipId = craftedHangarShips.maxOfOrNull { it.instanceId } ?: 0
        val maxItemId = craftedInventoryItems.maxOfOrNull { it.instanceId } ?: 0
        nextCraftedRecordId = maxOf(maxShipId, maxItemId) + 1

        ownedShipClasses.clear()
        ownedShipClasses.addAll(state.ownedShipClasses)

        val matchedCraftedShip = state.activeCraftedShipId?.let { id ->
            craftedHangarShips.firstOrNull { it.instanceId == id }
        }
        when {
            state.isFlyingCapsule -> {
                isFlyingCapsule = true
                activeCraftedShipId = null
                activeShipClass = ShipClass.INTERCEPTOR
                player.shipQuality = CraftQuality.STANDARD
            }
            matchedCraftedShip != null -> {
                isFlyingCapsule = false
                activeCraftedShipId = matchedCraftedShip.instanceId
                activeShipClass = visualShipClassForIndustrial(matchedCraftedShip.industrialType)
                player.shipQuality = matchedCraftedShip.quality
            }
            state.activeShipClass in ownedShipClasses -> {
                isFlyingCapsule = false
                activeCraftedShipId = null
                activeShipClass = state.activeShipClass
                player.shipQuality = state.activeShipQuality
            }
            else -> {
                // Safe fallback to Permanent Capsule if previously active vessel no longer exists
                isFlyingCapsule = true
                activeCraftedShipId = null
                activeShipClass = ShipClass.INTERCEPTOR
                player.shipQuality = CraftQuality.STANDARD
            }
        }
        syncPlayerActiveShipVisualState()

        ownedItems.clear()
        ownedItems.addAll(state.ownedItems)
        val byId = ownedItems.associateBy { it.instanceId }
        nextItemInstanceId = max(
            state.nextItemInstanceId,
            (ownedItems.maxOfOrNull { it.instanceId } ?: 0) + 1
        )

        val restoredCrafted = activeCraftedShip()
        if (restoredCrafted != null) {
            val hullDef = restoredCrafted.recipeId.shipHullDefinition()
            resizeSlotListsToCounts(
                laserCount = hullDef?.weaponSlotCount ?: 2,
                shieldCount = hullDef?.shieldSlotCount ?: 3
            )
        } else {
            resizeSlotListsForShip(activeShipClass)
        }
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
            val drone = VantDrone(
                id = savedVant.id,
                tier = savedVant.tier,
                durabilityPct = savedVant.durabilityPct.coerceIn(0, 100)
            )
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

        currentMissionIndex = state.currentMissionIndex.coerceIn(0, MissionCatalog.all.size)
        val activeMission = MissionCatalog.all.getOrNull(currentMissionIndex)
        currentMissionProgress = if (activeMission != null) {
            state.currentMissionProgress.coerceIn(0, activeMission.targetCount)
        } else {
            0
        }

        rawCargo.clear()
        for ((res, qty) in state.rawCargo) {
            if (qty > 0) rawCargo[res] = qty
        }
        refinedCargo.clear()
        for ((res, qty) in state.refinedCargo) {
            if (qty > 0) refinedCargo[res] = qty
        }
        basicVendorItems.clear()
        for ((item, qty) in state.basicVendorItems) {
            if (qty > 0) basicVendorItems[item] = qty
        }
        overchargedAmmo = state.overchargedAmmo.coerceAtLeast(0)
        player.pilotSkills = defaultPilotSkills().toMutableMap().apply {
            putAll(state.pilotSkills)
        }
        lastDeathSummary = null

        recalculateShipStats(refillVitals = true)
        val restoredSector = if (state.currentSectorId in GalaxyMapGraph.sectors) {
            state.currentSectorId
        } else {
            GalaxyMapGraph.homeCityForCompany(pilotCompany)
        }
        enterSector(restoredSector, fromSectorId = null, triggerWarpFlash = false, showPopup = false)
    }

    fun resetSector(keepProgress: Boolean, triggerSave: Boolean = true) {
        if (!keepProgress) {
            pilotName = null
            pilotCompany = null
            credits = 0
            kills = 0
            player.level = 1
            player.xp = 0
            activeConfig = 1
            ownedShipClasses.clear()
            isFlyingCapsule = true
            activeCraftedShipId = null
            activeShipClass = ShipClass.INTERCEPTOR
            syncPlayerActiveShipVisualState()
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
            rawCargo.clear()
            refinedCargo.clear()
            basicVendorItems.clear()
            playerShipWrecks.clear()
            lastDeathSummary = null
            overchargedAmmo = 0
            player.shipQuality = CraftQuality.STANDARD
            player.pilotSkills = defaultPilotSkills()
            craftedHangarShips.clear()
            craftedInventoryItems.clear()
            nextCraftedRecordId = 1
            isolatedLocalMarkets.clear()
            isolatedLocalMarkets.putAll(createIsolatedLocalMarkets())
        }
        recalculateShipStats(refillVitals = true)

        val spawnSector = GalaxyMapGraph.homeCityForCompany(pilotCompany)
        plottedRoute = emptyList()
        warpFlashTimer = 0f
        player.isMoving = false
        player.isAutoFireEnabled = false
        player.hitFlash = 0f
        player.shieldHitFlash = 0f
        player.fireCooldown = 0f
        player.rocketCooldown = 0f
        player.radiationTickTimer = 0f
        player.timeSinceDamaged = GameConfig.SHIELD_REGEN_DELAY
        player.isAlive = true

        screenShake = 0f
        lockedAlienId = null
        isPaused = false
        isHangarOpen = false
        isTacticalMapOpen = false
        isGalaxyMapOpen = false
        activeStationOverlay = null
        autoMoveTarget = null

        enterSector(spawnSector, fromSectorId = null, triggerWarpFlash = false, showPopup = false)

        if (triggerSave) {
            requestSave()
        }
    }

    fun activeMapSize(): Float = currentSector.mapSize

    fun activeSafeZoneCenter(): Pair<Float, Float> {
        val half = currentSector.mapSize * 0.5f
        return Pair(half, half)
    }

    fun activeSafeZoneRadius(): Float =
        if (currentSector.hasCentralSafeZone) currentSector.safeZoneRadius else 0f

    fun isPointInActiveSafeZone(x: Float, y: Float): Boolean {
        val sector = currentSector
        if (sector.isEntireSectorSafe) return true
        if (!sector.hasCentralSafeZone || sector.safeZoneRadius <= 0f) return false
        val (sx, sy) = activeSafeZoneCenter()
        return hypot(x - sx, y - sy) <= sector.safeZoneRadius
    }

    fun nearestJumpPortal(): SectorPortal? {
        if (!player.isAlive) return null
        var best: SectorPortal? = null
        var bestDist = GameConfig.JUMP_GATE_INTERACT_RADIUS
        for (portal in activePortals) {
            val d = hypot(player.x - portal.worldX, player.y - portal.worldY)
            if (d <= bestDist) {
                bestDist = d
                best = portal
            }
        }
        return best
    }

    fun isPlayerNearJumpGate(): Boolean = nearestJumpPortal() != null

    fun nearestStation(): SectorStation? {
        if (!player.isAlive) return null
        var best: SectorStation? = null
        var bestDist = Float.MAX_VALUE
        for (station in activeStations) {
            val d = hypot(player.x - station.worldX, player.y - station.worldY)
            if (d <= station.interactRadius && d < bestDist) {
                bestDist = d
                best = station
            }
        }
        return best
    }

    private fun enterSector(
        targetSectorId: String,
        fromSectorId: String?,
        triggerWarpFlash: Boolean,
        showPopup: Boolean
    ) {
        val sector = GalaxyMapGraph.sectorOf(targetSectorId)
        currentSectorId = sector.id
        activePortals = buildBorderPortalsForSector(sector.id)
        activeStations = buildStationsForSector(sector.id)
        activeStationOverlay = null
        activeMiningNodeId = null
        activeSalvageWreckId = null
        sectorStars = generateSectorStars(sector.id)
        sectorNebulae = generateSectorNebulae(sector.id)

        // Update or advance plotted route
        if (plottedRoute.isNotEmpty()) {
            val idx = plottedRoute.indexOf(sector.id)
            plottedRoute = if (idx >= 0 && idx < plottedRoute.lastIndex) {
                plottedRoute.subList(idx, plottedRoute.size)
            } else {
                emptyList()
            }
        }

        val half = sector.mapSize * 0.5f
        val returnPortal = fromSectorId?.let { src ->
            activePortals.firstOrNull { it.targetSectorId == src }
        }
        if (returnPortal != null && !sector.isCapitalCity) {
            val dx = half - returnPortal.worldX
            val dy = half - returnPortal.worldY
            val d = hypot(dx, dy).coerceAtLeast(1f)
            player.x = (returnPortal.worldX + (dx / d) * 150f).coerceIn(300f, sector.mapSize - 300f)
            player.y = (returnPortal.worldY + (dy / d) * 150f).coerceIn(300f, sector.mapSize - 300f)
            player.angleRad = atan2(dy, dx)
        } else {
            player.x = half
            player.y = half
            player.angleRad = -PI.toFloat() * 0.5f
        }

        player.isMoving = false
        player.thrustFactor = 0f
        player.inSafeZone = isPointInActiveSafeZone(player.x, player.y)
        player.currentZone = resolveSpaceZoneForSector(sector, player.inSafeZone)
        player.inRadiationWarning = false
        player.isTakingZoneRadiation = false
        player.requiredChipWarning = null
        player.radiationTickTimer = 0f

        cameraX = player.x
        cameraY = player.y
        snapVantsToFormation()
        populateActiveMapEntities()

        if (triggerWarpFlash) {
            warpFlashTimer = GameConfig.WARP_FLASH_DURATION_SEC
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
        }
        if (showPopup) {
            popups.add(
                FloatingPopup(
                    x = player.x,
                    y = player.y - activeShipClass.hullRadius - 32f,
                    text = "SECTOR ${sector.id} • ${sector.zoneType.shortLabel}",
                    life = 1.85f,
                    maxLife = 1.85f,
                    colorRgb = (sector.nodeColorHex and 0xFFFFFF).toInt(),
                    textSize = 15f
                )
            )
        }
    }

    /**
     * Positions every jump portal along the square map border (10% inward margin from all edges),
     * aligned to the direction angle from the current sector to each connected destination sector,
     * keeping the central play area and Nucleus Black Market completely clear.
     */
    private fun buildBorderPortalsForSector(sectorId: String): List<SectorPortal> {
        val current = GalaxyMapGraph.sectorOf(sectorId)
        val mapSize = current.mapSize
        val half = mapSize * 0.5f
        val margin = mapSize * 0.10f
        val borderHalf = half - margin

        val placedAngles = mutableListOf<Double>()
        val portals = mutableListOf<SectorPortal>()

        for ((index, neighborId) in current.connectedSectorIds.withIndex()) {
            val neighbor = GalaxyMapGraph.sectorOf(neighborId)
            val dx = (neighbor.position.x - current.position.x).toDouble()
            val dy = (neighbor.position.y - current.position.y).toDouble()
            var angle = if (dx == 0.0 && dy == 0.0) {
                index * (2.0 * PI / max(1, current.connectedSectorIds.size))
            } else {
                atan2(dy, dx)
            }

            // Prevent overlapping portals if two neighbors share a near-identical bearing
            for (existingAngle in placedAngles) {
                var diff = angle - existingAngle
                while (diff > PI) diff -= 2.0 * PI
                while (diff < -PI) diff += 2.0 * PI
                if (abs(diff) < 0.22) {
                    angle += 0.26
                }
            }
            placedAngles.add(angle)

            val ux = cos(angle)
            val uy = sin(angle)
            val scale = borderHalf / max(abs(ux), abs(uy)).coerceAtLeast(1e-6)
            val px = (half + ux * scale).toFloat().coerceIn(margin, mapSize - margin)
            val py = (half + uy * scale).toFloat().coerceIn(margin, mapSize - margin)
            val shortcut = current.isShortcutTo(neighborId)

            portals.add(
                SectorPortal(
                    targetSectorId = neighborId,
                    worldX = px,
                    worldY = py,
                    isShortcut = shortcut,
                    accentColorHex = if (shortcut) 0xFF34D399 else neighbor.nodeColorHex
                )
            )
        }
        return portals
    }

    /**
     * Places central stations inside the Safe Zone of each Capital City (C1, C2, C3:
     * Refinery, Local Market, and Crafting Station) and Nucleus (0-0: Refinery & Black Market).
     */
    private fun buildStationsForSector(sectorId: String): List<SectorStation> {
        val sector = GalaxyMapGraph.sectorOf(sectorId)
        if (!sector.hasCentralStations) return emptyList()
        val half = sector.mapSize * 0.5f
        val offset = if (sector.isNucleus) 285f else 245f
        val topY = if (sector.isCapitalCity) half - 115f else half - 30f
        val marketLabel = if (sector.isNucleus) "BLACK MARKET" else "${sector.id} LOCAL MARKET"
        val marketAccent = if (sector.isNucleus) 0xFFFACC15 else sector.nodeColorHex
        val stations = mutableListOf(
            SectorStation(
                kind = StationKind.REFINERY,
                sectorId = sector.id,
                name = "${sector.id} REFINERY",
                worldX = half - offset,
                worldY = topY,
                interactRadius = 175f,
                accentColorHex = 0xFFFB923C
            ),
            SectorStation(
                kind = StationKind.LOCAL_MARKET,
                sectorId = sector.id,
                name = marketLabel,
                worldX = half + offset,
                worldY = topY,
                interactRadius = 175f,
                accentColorHex = marketAccent
            )
        )
        if (sector.isCapitalCity) {
            stations.add(
                SectorStation(
                    kind = StationKind.CRAFTING,
                    sectorId = sector.id,
                    name = "${sector.id} CRAFTING",
                    worldX = half,
                    worldY = half + 215f,
                    interactRadius = 175f,
                    accentColorHex = 0xFF34D399
                )
            )
        }
        return stations
    }

    private fun resolveSpaceZoneForSector(sector: SectorNode, inSafe: Boolean): SpaceZone {
        if (inSafe) return SpaceZone.SAFE_ZONE
        return when (sector.zoneType) {
            GalaxyZoneType.CAPITAL_CITY, GalaxyZoneType.SAFE_ZONE -> SpaceZone.ZONE_1_ALPHA
            GalaxyZoneType.INTERMEDIATE_ZONE -> SpaceZone.ZONE_1_ALPHA
            GalaxyZoneType.MODERATE_ZONE -> SpaceZone.ZONE_2_BETA
            GalaxyZoneType.HIGH_RISK_YELLOW_RING -> SpaceZone.ZONE_3_GAMMA
            GalaxyZoneType.NUCLEUS -> SpaceZone.ZONE_4_OMEGA
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
        resourceNodes.clear()
        alienWrecks.clear()
        activeMiningNodeId = null
        activeSalvageWreckId = null

        val sector = currentSector
        if (sector.isEntireSectorSafe) {
            // Capital Cities have no hostile NPCs and no raw resource nodes
            repeat(10) { spawnBonusBox() }
            return
        }

        when (sector.zoneType) {
            GalaxyZoneType.SAFE_ZONE -> {
                repeat(16) { spawnAlienForTier(AlienTier.SCOUT, minDistanceFromPlayer = 700f) }
                repeat(6) { spawnAlienForTier(AlienTier.HUNTER, minDistanceFromPlayer = 800f) }
                repeat(36) { spawnBonusBox() }
                repeat(28) { spawnResourceNodeForActiveSector() }
            }
            GalaxyZoneType.INTERMEDIATE_ZONE -> {
                repeat(16) { spawnAlienForTier(AlienTier.SCOUT, minDistanceFromPlayer = 680f) }
                repeat(12) { spawnAlienForTier(AlienTier.HUNTER, minDistanceFromPlayer = 720f) }
                repeat(42) { spawnBonusBox() }
                repeat(32) { spawnResourceNodeForActiveSector() }
            }
            GalaxyZoneType.MODERATE_ZONE -> {
                repeat(14) { spawnAlienForTier(AlienTier.HUNTER, minDistanceFromPlayer = 720f) }
                repeat(14) { spawnAlienForTier(AlienTier.DESTROYER, minDistanceFromPlayer = 780f) }
                repeat(44) { spawnBonusBox() }
                repeat(34) { spawnResourceNodeForActiveSector() }
            }
            GalaxyZoneType.HIGH_RISK_YELLOW_RING -> {
                repeat(14) { spawnAlienForTier(AlienTier.DESTROYER, minDistanceFromPlayer = 760f) }
                repeat(10) { spawnAlienForTier(AlienTier.DREADNOUGHT_BOSS, minDistanceFromPlayer = 860f) }
                repeat(50) { spawnBonusBox() }
                repeat(38) { spawnResourceNodeForActiveSector() }
            }
            GalaxyZoneType.NUCLEUS -> {
                repeat(18) { spawnAlienForTier(AlienTier.DESTROYER, minDistanceFromPlayer = 900f) }
                repeat(20) { spawnAlienForTier(AlienTier.DREADNOUGHT_BOSS, minDistanceFromPlayer = 960f) }
                repeat(80) { spawnBonusBox() }
                repeat(68) { spawnResourceNodeForActiveSector() }
            }
            GalaxyZoneType.CAPITAL_CITY -> Unit
        }
    }

    private fun spawnResourceNodeForActiveSector() {
        val sector = currentSector
        if (sector.isCapitalCity) return

        val resourceType: ResourceType = when (sector.zoneType) {
            GalaxyZoneType.SAFE_ZONE, GalaxyZoneType.INTERMEDIATE_ZONE -> {
                val tier = if (random.nextFloat() < 0.56f) 1 else 2
                ResourceType.forRegionAndTier(sector.nativeCityId, tier)
            }
            GalaxyZoneType.MODERATE_ZONE -> {
                val tier = if (random.nextFloat() < 0.54f) 3 else 4
                ResourceType.forRegionAndTier(sector.nativeCityId, tier)
            }
            GalaxyZoneType.HIGH_RISK_YELLOW_RING -> {
                if (random.nextFloat() < 0.36f) {
                    ResourceType.YELLOW_CRYSTAL
                } else {
                    val tier = if (random.nextFloat() < 0.50f) 4 else 5
                    ResourceType.forRegionAndTier(sector.nativeCityId, tier)
                }
            }
            GalaxyZoneType.NUCLEUS -> {
                if (random.nextFloat() < 0.50f) {
                    ResourceType.YELLOW_CRYSTAL
                } else {
                    val regionCities = arrayOf("C1", "C2", "C3")
                    val region = regionCities[random.nextInt(regionCities.size)]
                    val tier = if (random.nextFloat() < 0.42f) 4 else 5
                    ResourceType.forRegionAndTier(region, tier)
                }
            }
            GalaxyZoneType.CAPITAL_CITY -> return
        }

        // Always spawn outside the central safe zone (crucial for Nucleus 0-0)
        val (nx, ny) = randomPointInActiveSector(margin = 420f, safeZoneBuffer = 260f)
        val nodeCharges = ResourceNodeEntity.MIN_CHARGES +
            random.nextInt(ResourceNodeEntity.MAX_CHARGES - ResourceNodeEntity.MIN_CHARGES + 1)
        val radius = when {
            resourceType.isYellowCrystal -> 26f
            resourceType.tier >= 4 -> 23f
            else -> 20f
        }
        resourceNodes.add(
            ResourceNodeEntity(
                id = nextResourceNodeId++,
                resourceType = resourceType,
                x = nx,
                y = ny,
                yieldAmount = 1,
                radius = radius,
                rotationRad = random.nextFloat() * 2f * PI.toFloat(),
                pulsePhase = random.nextFloat() * 6.28f,
                maxCharges = nodeCharges,
                charges = nodeCharges
            )
        )
    }

    fun triggerJumpGate(): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen) return false
        val portal = nearestJumpPortal() ?: return false

        autoMoveTarget = null
        lockedAlienId = null
        player.isAutoFireEnabled = false
        isTacticalMapOpen = false
        isGalaxyMapOpen = false

        val originSector = currentSectorId
        enterSector(
            targetSectorId = portal.targetSectorId,
            fromSectorId = originSector,
            triggerWarpFlash = true,
            showPopup = true
        )
        requestSave()
        return true
    }

    fun relaunchShip() {
        // Pilot always respawns docked in their Home City inside their Permanent Capsule
        isFlyingCapsule = true
        activeCraftedShipId = null
        activeShipClass = ShipClass.INTERCEPTOR
        lastDeathSummary = null
        syncPlayerActiveShipVisualState()

        recalculateShipStats(refillVitals = true)
        val homeCity = GalaxyMapGraph.homeCityForCompany(pilotCompany)
        player.rocketCooldown = 0f
        player.isAlive = true
        player.isAutoFireEnabled = false
        player.hitFlash = 0f
        player.shieldHitFlash = 0f
        player.timeSinceDamaged = GameConfig.SHIELD_REGEN_DELAY
        autoMoveTarget = null
        isTacticalMapOpen = false
        isGalaxyMapOpen = false
        enterSector(homeCity, fromSectorId = null, triggerWarpFlash = false, showPopup = true)
        requestSave()
    }

    fun setMoveJoystick(x: Float, y: Float) {
        if (isTacticalMapOpen || isGalaxyMapOpen || activeStationOverlay != null || isHangarOpen || isPaused) {
            moveInputX = 0f
            moveInputY = 0f
            return
        }
        moveInputX = x
        moveInputY = y
        if (hypot(x, y) > GameConfig.JOYSTICK_DEADZONE) {
            clearAutoMoveTarget()
        }
    }

    fun setAimJoystick(x: Float, y: Float) {
        if (isTacticalMapOpen || isGalaxyMapOpen || activeStationOverlay != null || isHangarOpen || isPaused) {
            aimInputX = 0f
            aimInputY = 0f
            isAiming = false
            return
        }
        aimInputX = x
        aimInputY = y
        val mag = hypot(x, y)
        isAiming = mag > GameConfig.JOYSTICK_DEADZONE
        if (isAiming) {
            clearAutoMoveTarget()
            if (player.isAlive && !isPaused && !isHangarOpen) {
                acquireClosestAlienInRadarCone(atan2(y, x))
            }
        }
    }

    fun toggleAutoFire() {
        if (!player.isAlive || isPaused || isHangarOpen || isTacticalMapOpen || isGalaxyMapOpen || activeStationOverlay != null) return
        clearAutoMoveTarget()
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
        if (!player.isAlive || isPaused || isHangarOpen || isTacticalMapOpen || isGalaxyMapOpen || activeStationOverlay != null || player.inSafeZone) return false
        clearAutoMoveTarget()
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
                x = player.x + cos(launchAngle) * (activeHullRadius() + 4f),
                y = player.y + sin(launchAngle) * (activeHullRadius() + 4f),
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
        isGalaxyMapOpen = false
        activeStationOverlay = null
        isHangarOpen = true
        isPaused = true
    }

    fun closeHangar() {
        isHangarOpen = false
        isPaused = false
        recalculateShipStats(refillVitals = false)
        requestSave()
    }

    fun openNearbyStation(): Boolean {
        if (!player.isAlive || isHangarOpen || isPaused) return false
        val station = nearestStation() ?: return false
        isTacticalMapOpen = false
        isGalaxyMapOpen = false
        autoMoveTarget = null
        activeStationOverlay = station.kind
        return true
    }

    fun closeStation() {
        activeStationOverlay = null
        requestSave()
    }

    fun switchStationTab(kind: StationKind): Boolean {
        if (activeStationOverlay == null) return false
        if (currentSector.zoneType != GalaxyZoneType.CAPITAL_CITY && kind != StationKind.LOCAL_MARKET) {
            return false
        }
        activeStationOverlay = kind
        return true
    }

    /**
     * Enforces the v1.10 Clockwise City Refining Loop & multi-city refining:
     * - C1 raw -> refines only in C2
     * - C2 raw -> refines only in C3
     * - C3 raw -> refines only in C1
     * - YELLOW_CRYSTAL & XENO_SALVAGE -> refine in C1, C2, or C3
     * Consumes raw units + Credit fee (discounted by REFINING skill up to 35% at Lv 99),
     * rolls bonus output chance (up to 15% at Lv 99), and awards REFINING XP.
     */
    fun refineResource(type: ResourceType, requestedUnits: Int): Boolean {
        if (activeStationOverlay != StationKind.REFINERY) return false
        if (!isAdminMode && !type.canRefineIn(currentSectorId)) return false
        if (requestedUnits <= 0) return false

        val ownedRaw = rawCargo[type] ?: 0
        val refiningLv = skillLevel(SkillType.REFINING)
        val unitsToRefine: Int
        if (isAdminMode) {
            // Admin Mode: free refining in any city, 0 raw & 0 credits consumed
            unitsToRefine = requestedUnits.coerceAtLeast(1)
        } else {
            val maxByRaw = ownedRaw / ResourceType.RAW_REQUIRED_PER_REFINE
            if (maxByRaw <= 0) return false

            val feePerUnit = effectiveRefiningFeePerUnit(type, refiningLv)
            val maxByCredits = if (feePerUnit > 0) credits / feePerUnit else maxByRaw
            unitsToRefine = min(requestedUnits, min(maxByRaw, maxByCredits))
            if (unitsToRefine <= 0) return false

            val rawConsumed = unitsToRefine * ResourceType.RAW_REQUIRED_PER_REFINE
            val totalFee = unitsToRefine * feePerUnit
            credits = (credits - totalFee).coerceAtLeast(0)

            val remainingRaw = ownedRaw - rawConsumed
            if (remainingRaw > 0) {
                rawCargo[type] = remainingRaw
            } else {
                rawCargo.remove(type)
            }
        }

        val bonusChance = refiningBonusOutputChance(refiningLv)
        var bonusUnits = 0
        if (bonusChance > 0f) {
            repeat(unitsToRefine) {
                if (random.nextFloat() < bonusChance) {
                    bonusUnits++
                }
            }
        }
        val totalOutput = unitsToRefine + bonusUnits
        refinedCargo[type] = (refinedCargo[type] ?: 0) + totalOutput

        val xpGained = refiningXpPerUnit(type) * unitsToRefine.toLong()
        awardSkillXp(
            skill = SkillType.REFINING,
            amount = xpGained,
            popupX = player.x,
            popupY = player.y - activeShipClass.hullRadius - 24f
        )

        val refinedLabel = GameStrings.refinedNameFor(type, language)
        val bonusText = if (bonusUnits > 0) " (+$bonusUnits BONUS)" else ""
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 26f,
                text = "+$totalOutput $refinedLabel$bonusText • +$xpGained XP",
                life = 1.65f,
                maxLife = 1.65f,
                colorRgb = (type.colorHex and 0xFFFFFF).toInt()
            )
        )
        requestSave()
        return true
    }

    /**
     * Buys 1 unit of a resource from the CURRENT sector's strictly isolated Local Market.
     * Mutates ONLY [isolatedLocalMarkets][currentSectorId].
     */
    fun buyMarketResource(type: ResourceType, isRefined: Boolean): Boolean {
        if (activeStationOverlay != StationKind.LOCAL_MARKET) return false
        val market = isolatedLocalMarkets[currentSectorId] ?: return false
        val entry = market.resourceEntries[type to isRefined] ?: return false
        if (!isAdminMode) {
            if (entry.localStock <= 0 || credits < entry.buyPrice) return false
            credits -= entry.buyPrice
            entry.localStock -= 1
            // Local scarcity gently nudges ONLY this city's buy price upward
            entry.buyPrice = (entry.buyPrice * 1.02f).roundToInt().coerceAtLeast(entry.buyPrice + 1)
        }

        if (isRefined) {
            refinedCargo[type] = (refinedCargo[type] ?: 0) + 1
        } else {
            rawCargo[type] = (rawCargo[type] ?: 0) + 1
        }
        requestSave()
        return true
    }

    /**
     * Sells resource cargo to the CURRENT sector's strictly isolated Local Market.
     * Increases ONLY this city's local stock — never shared with any other city.
     */
    fun sellMarketResource(type: ResourceType, isRefined: Boolean, sellAll: Boolean): Boolean {
        if (activeStationOverlay != StationKind.LOCAL_MARKET) return false
        val market = isolatedLocalMarkets[currentSectorId] ?: return false
        val entry = market.resourceEntries[type to isRefined] ?: return false
        val cargoMap = if (isRefined) refinedCargo else rawCargo
        val owned = cargoMap[type] ?: 0
        if (owned <= 0) return false

        val qty = if (sellAll) owned else 1
        val payout = entry.sellPrice * qty
        credits += payout
        val remaining = owned - qty
        if (remaining > 0) {
            cargoMap[type] = remaining
        } else {
            cargoMap.remove(type)
        }
        entry.localStock += qty
        requestSave()
        return true
    }

    /**
     * Buys a locally stocked equipment module from THIS sector's isolated Local Market.
     */
    fun buyMarketEquipment(itemId: EquipmentItemId): Boolean {
        if (activeStationOverlay != StationKind.LOCAL_MARKET) return false
        val market = isolatedLocalMarkets[currentSectorId] ?: return false
        val entry = market.equipmentEntries.firstOrNull { it.itemId == itemId } ?: return false
        if (!isAdminMode) {
            if (entry.localStock <= 0 || credits < entry.buyPrice) return false
            credits -= entry.buyPrice
            entry.localStock -= 1
        }
        ownedItems.add(EquipmentItem(instanceId = nextItemInstanceId++, type = itemId))
        requestSave()
        return true
    }

    /**
     * Capital City NPC Basic Vendor (C1, C2, C3 only).
     * Strictly limited to Tier 1 starter gear & basic repair kits so players can never get soft-locked.
     */
    fun buyBasicVendorItem(itemId: BasicVendorItemId): Boolean {
        if (activeStationOverlay != StationKind.LOCAL_MARKET) return false
        if (currentSector.zoneType != GalaxyZoneType.CAPITAL_CITY) return false
        if (!isAdminMode) {
            if (credits < itemId.priceCredits) return false
            credits -= itemId.priceCredits
        }
        val eqId = itemId.equipmentId
        if (eqId != null) {
            ownedItems.add(EquipmentItem(instanceId = nextItemInstanceId++, type = eqId, quality = CraftQuality.STANDARD))
        } else {
            basicVendorItems[itemId] = (basicVendorItems[itemId] ?: 0) + 1
            if (itemId == BasicVendorItemId.BASIC_REPAIR_KIT) {
                player.hp = player.maxHp
                player.shield = player.maxShield
            }
        }
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeHullRadius() - 32f,
                text = "+1 ${GameStrings.basicVendorItemName(itemId, language)}",
                life = 1.6f,
                maxLife = 1.6f,
                colorRgb = (itemId.accentHex and 0xFFFFFF).toInt(),
                textSize = 13.5f
            )
        )
        requestSave()
        return true
    }

    /**
     * Crafts consumables, sector chips, or basic ship gear at a Capital City Crafting Station
     * using refined resources (consumes lowest qualifying refined tier first).
     */
    fun craftRecipe(recipe: CraftingRecipeId): Boolean {
        if (activeStationOverlay != StationKind.CRAFTING) return false
        if (!isAdminMode) {
            val qualifying = refinedCargo.entries
                .filter { (res, qty) -> qty > 0 && res.tier >= recipe.minRefinedTier }
                .sortedWith(compareBy<Map.Entry<ResourceType, Int>> { it.key.isYellowCrystal }.thenBy { it.key.tier })
            val totalAvailable = qualifying.sumOf { it.value }
            if (totalAvailable < recipe.refinedCost) return false

            var remainingCost = recipe.refinedCost
            for ((res, qty) in qualifying) {
                if (remainingCost <= 0) break
                val take = min(qty, remainingCost)
                val left = qty - take
                if (left > 0) refinedCargo[res] = left else refinedCargo.remove(res)
                remainingCost -= take
            }
        }

        val popupText = when (recipe) {
            CraftingRecipeId.HULL_REPAIR_KIT -> {
                player.hp = player.maxHp
                player.shield = player.maxShield
                "HULL & SHIELD 100%"
            }
            CraftingRecipeId.PLASMA_AMMO_PACK -> {
                overchargedAmmo += 25
                "+25 PLASMA AMMO"
            }
            else -> {
                val eq = recipe.resultEquipment
                if (eq != null) {
                    ownedItems.add(EquipmentItem(instanceId = nextItemInstanceId++, type = eq))
                    "+1 ${eq.displayName}"
                } else {
                    recipe.displayName
                }
            }
        }
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 36f,
                text = popupText,
                life = 1.6f,
                maxLife = 1.6f,
                colorRgb = (recipe.accentHex and 0xFFFFFF).toInt(),
                textSize = 14f
            )
        )
        requestSave()
        return true
    }

    /**
     * Step 5 Station Crafting System:
     * - Validates required refined/direct materials across Naval Engineering, Gear Manufacturing, and Synthesis.
     * - Rolls CraftQuality (STANDARD +0%, REINFORCED +5%, EXCELLENT +10%, MASTERWORK +15%) from skill Lv 1–99.
     * - Applies skill perks: material return up to 15% (Naval/Gear) or double output up to 20% (Synthesis).
     * - Adds crafted Industrial Ships to Hangar, Gear/Tools/Consumables to Inventory, and awards skill XP.
     */
    fun craftStationRecipe(recipeId: StationCraftRecipeId): CraftOutcome? {
        if (activeStationOverlay == null) return null
        if (currentSector.zoneType != GalaxyZoneType.CAPITAL_CITY) return null

        val skill = recipeId.discipline.skill
        val skillLevel = skillLevel(skill)

        var totalRefunded = 0
        if (!isAdminMode) {
            // Verify all required materials are owned
            for (cost in recipeId.ingredients) {
                val map = if (cost.isRefined) refinedCargo else rawCargo
                val owned = map[cost.resource] ?: 0
                if (owned < cost.amount) return null
            }

            // Deduct ingredients and apply material return perk for Naval Engineering & Gear Manufacturing
            val refundRate = when (recipeId.discipline) {
                CraftingDiscipline.NAVAL_ENGINEERING,
                CraftingDiscipline.GEAR_MANUFACTURING -> craftingMaterialReturnRate(skillLevel)
                CraftingDiscipline.SYNTHESIS -> 0f
            }

            for (cost in recipeId.ingredients) {
                val map = if (cost.isRefined) refinedCargo else rawCargo
                val owned = map[cost.resource] ?: 0
                var refundedForCost = 0
                if (refundRate > 0f) {
                    repeat(cost.amount) {
                        if (random.nextFloat() < refundRate) {
                            refundedForCost++
                        }
                    }
                }
                val netConsumed = (cost.amount - refundedForCost).coerceAtLeast(0)
                val remaining = (owned - netConsumed).coerceAtLeast(0)
                if (remaining > 0) map[cost.resource] = remaining else map.remove(cost.resource)
                totalRefunded += refundedForCost
            }
        }

        val quality = rollCraftQuality(skillLevel, random)
        val doubled = recipeId.discipline == CraftingDiscipline.SYNTHESIS &&
            random.nextFloat() < synthesisDoubleOutputChance(skillLevel)
        val outputQuantity = if (doubled) 2 else 1

        when (recipeId.outputKind) {
            CraftOutputKind.INDUSTRIAL_SHIP -> {
                val hull = recipeId.shipHullDefinition()
                val indType = recipeId.industrialType ?: IndustrialType.MINER
                val effHp = quality.applyBonusInt(hull?.baseHull?.toInt() ?: 220)
                val effShd = quality.applyBonusInt(hull?.baseShield?.toInt() ?: 140)
                val effCargo = quality.applyBonusInt(hull?.baseCargo ?: 240)
                craftedHangarShips.add(
                    CraftedShipRecord(
                        instanceId = nextCraftedRecordId++,
                        recipeId = recipeId,
                        hullId = hull?.id ?: recipeId.name,
                        industrialType = indType,
                        quality = quality,
                        effectiveHull = effHp,
                        effectiveShield = effShd,
                        effectiveCargo = effCargo
                    )
                )
            }

            CraftOutputKind.EQUIPMENT_GEAR -> {
                val eqId = recipeId.resultEquipmentId
                if (eqId != null) {
                    repeat(outputQuantity) {
                        ownedItems.add(
                            EquipmentItem(
                                instanceId = nextItemInstanceId++,
                                type = eqId,
                                quality = quality
                            )
                        )
                    }
                }
            }

            CraftOutputKind.EXTRACTION_TOOL,
            CraftOutputKind.CONSUMABLE_REPAIR,
            CraftOutputKind.CONSUMABLE_BOOSTER -> {
                val effPrimary = quality.applyBonus(recipeId.basePrimaryStat)
                val effDur = if (recipeId.baseDurationMinutes > 0) {
                    quality.applyBonusInt(recipeId.baseDurationMinutes)
                } else 0
                val existingIdx = craftedInventoryItems.indexOfFirst {
                    it.recipeId == recipeId && it.quality == quality
                }
                if (existingIdx >= 0) {
                    val existing = craftedInventoryItems[existingIdx]
                    craftedInventoryItems[existingIdx] = existing.copy(
                        quantity = existing.quantity + outputQuantity
                    )
                } else {
                    craftedInventoryItems.add(
                        CraftedItemRecord(
                            instanceId = nextCraftedRecordId++,
                            recipeId = recipeId,
                            quality = quality,
                            effectivePrimaryStat = effPrimary,
                            effectiveDurationMinutes = effDur,
                            quantity = outputQuantity
                        )
                    )
                }
            }
        }

        awardSkillXp(
            skill = skill,
            amount = recipeId.xpReward,
            popupX = player.x,
            popupY = player.y - activeShipClass.hullRadius - 32f
        )

        val statSummary = GameStrings.craftOutcomeStatSummary(recipeId, quality, language)
        requestSave()
        return CraftOutcome(
            id = nextCraftOutcomeId++,
            recipeId = recipeId,
            quality = quality,
            quantityCrafted = outputQuantity,
            refundedUnits = totalRefunded,
            xpAwarded = recipeId.xpReward,
            doubledOutput = doubled,
            statSummary = statSummary
        )
    }

    private fun createIsolatedLocalMarkets(): MutableMap<String, IsolatedLocalMarket> {
        fun buildMarket(
            sectorId: String,
            marketName: String,
            isBlackMarket: Boolean,
            preferredRefinedNativeCity: String,
            priceBias: Float,
            stockSeed: Int,
            gearStock: List<Triple<EquipmentItemId, Int, Int>>
        ): IsolatedLocalMarket {
            val marketRng = Random(stockSeed)
            val entries = linkedMapOf<Pair<ResourceType, Boolean>, MutableMarketResourceEntry>()
            for (res in ResourceType.entries) {
                val isForeignRefinedBonus = !isBlackMarket && res.nativeSectorId == preferredRefinedNativeCity
                val isNucleusCrystalBonus = isBlackMarket && res.isYellowCrystal
                val rawSellMult = when {
                    isNucleusCrystalBonus -> 1.15f
                    res.refinesInSectorId == sectorId -> 1.05f
                    else -> 0.88f
                } * priceBias
                val refinedSellMult = when {
                    isNucleusCrystalBonus -> 1.45f
                    isForeignRefinedBonus -> 1.28f
                    res.refinesInSectorId == sectorId -> 1.12f
                    else -> 0.96f
                } * priceBias

                val rawSell = (res.rawBasePrice * rawSellMult).roundToInt().coerceAtLeast(15)
                val rawBuy = (rawSell * 1.32f).roundToInt().coerceAtLeast(rawSell + 10)
                val rawStock = when {
                    isBlackMarket && res.isYellowCrystal -> 4 + marketRng.nextInt(4)
                    res.nativeSectorId == sectorId -> 12 + marketRng.nextInt(10)
                    else -> 3 + marketRng.nextInt(6)
                }
                entries[res to false] = MutableMarketResourceEntry(
                    resourceType = res,
                    isRefined = false,
                    buyPrice = rawBuy,
                    sellPrice = rawSell,
                    localStock = rawStock
                )

                val refSell = (res.refinedBasePrice * refinedSellMult).roundToInt().coerceAtLeast(45)
                val refBuy = (refSell * 1.35f).roundToInt().coerceAtLeast(refSell + 25)
                val refStock = when {
                    isBlackMarket && res.isYellowCrystal -> 2 + marketRng.nextInt(3)
                    res.refinesInSectorId == sectorId -> 5 + marketRng.nextInt(5)
                    else -> 1 + marketRng.nextInt(3)
                }
                entries[res to true] = MutableMarketResourceEntry(
                    resourceType = res,
                    isRefined = true,
                    buyPrice = refBuy,
                    sellPrice = refSell,
                    localStock = refStock
                )
            }
            val gear = gearStock.map { (id, price, stock) ->
                MutableMarketEquipmentEntry(itemId = id, buyPrice = price, localStock = stock)
            }.toMutableList()
            return IsolatedLocalMarket(
                sectorId = sectorId,
                marketName = marketName,
                isBlackMarket = isBlackMarket,
                resourceEntries = entries,
                equipmentEntries = gear
            )
        }

        return linkedMapOf(
            "C1" to buildMarket(
                sectorId = "C1",
                marketName = "C1 AETHER LOCAL MARKET",
                isBlackMarket = false,
                preferredRefinedNativeCity = "C3",
                priceBias = 1.00f,
                stockSeed = 1011,
                gearStock = listOf(
                    Triple(EquipmentItemId.SG_1, 225, 6),
                    Triple(EquipmentItemId.LF_1, 210, 4)
                )
            ),
            "C2" to buildMarket(
                sectorId = "C2",
                marketName = "C2 VERDANT LOCAL MARKET",
                isBlackMarket = false,
                preferredRefinedNativeCity = "C1",
                priceBias = 1.04f,
                stockSeed = 2022,
                gearStock = listOf(
                    Triple(EquipmentItemId.LF_1, 195, 5),
                    Triple(EquipmentItemId.SG_1, 240, 5)
                )
            ),
            "C3" to buildMarket(
                sectorId = "C3",
                marketName = "C3 SOLARIS LOCAL MARKET",
                isBlackMarket = false,
                preferredRefinedNativeCity = "C2",
                priceBias = 1.02f,
                stockSeed = 3033,
                gearStock = listOf(
                    Triple(EquipmentItemId.LF_1, 185, 7),
                    Triple(EquipmentItemId.SG_1, 260, 4)
                )
            ),
            "0-0" to buildMarket(
                sectorId = "0-0",
                marketName = "0-0 NUCLEUS BLACK MARKET",
                isBlackMarket = true,
                preferredRefinedNativeCity = "0-0",
                priceBias = 1.18f,
                stockSeed = 9000,
                gearStock = listOf(
                    Triple(EquipmentItemId.LF_2, 1150, 3),
                    Triple(EquipmentItemId.SG_2, 1300, 3),
                    Triple(EquipmentItemId.LF_1, 240, 4)
                )
            )
        )
    }

    fun openTacticalMap() {
        if (!player.isAlive || isHangarOpen || isPaused || activeStationOverlay != null) return
        moveInputX = 0f
        moveInputY = 0f
        aimInputX = 0f
        aimInputY = 0f
        isAiming = false
        isGalaxyMapOpen = false
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

    fun openGalaxyMap() {
        if (!player.isAlive || isHangarOpen || isPaused || activeStationOverlay != null) return
        moveInputX = 0f
        moveInputY = 0f
        aimInputX = 0f
        aimInputY = 0f
        isAiming = false
        isTacticalMapOpen = false
        isGalaxyMapOpen = true
    }

    fun closeGalaxyMap() {
        isGalaxyMapOpen = false
    }

    fun toggleGalaxyMap() {
        if (isGalaxyMapOpen) {
            closeGalaxyMap()
        } else {
            openGalaxyMap()
        }
    }

    fun setPlottedRoute(route: List<String>) {
        plottedRoute = if (route.size >= 2 && route.first() == currentSectorId) {
            route.toList()
        } else {
            emptyList()
        }
    }

    fun clearPlottedRoute() {
        plottedRoute = emptyList()
    }

    fun plotRouteAndEngageAutopilot(route: List<String>): Boolean {
        if (route.size < 2 || route.first() != currentSectorId) return false
        setPlottedRoute(route)
        return engageAutopilotToNextRoutePortal()
    }

    fun engageAutopilotToNextRoutePortal(): Boolean {
        val nextId = nextRouteHopSectorId ?: return false
        val portal = activePortals.firstOrNull { it.targetSectorId == nextId } ?: return false
        val finalSectorId = plottedRoute.lastOrNull() ?: nextId
        val finalSector = GalaxyMapGraph.sectorOf(finalSectorId)
        val label = if (finalSectorId == nextId) {
            "${finalSector.id} (${finalSector.name})"
        } else {
            "${finalSector.id} → Gate $nextId"
        }
        setAutoMoveTarget(portal.worldX, portal.worldY)
        isGalaxyMapOpen = false
        isTacticalMapOpen = false
        return true
    }

    fun setAutoMoveTarget(worldX: Float, worldY: Float) {
        if (!player.isAlive) return
        val maxBound = (activeMapSize() - 120f).coerceAtLeast(120f)
        val clampedX = worldX.coerceIn(120f, maxBound)
        val clampedY = worldY.coerceIn(120f, maxBound)
        // Clear any residual manual joystick state so direct map movement engages immediately
        moveInputX = 0f
        moveInputY = 0f
        autoMoveTarget = AutoMoveTarget(clampedX, clampedY)
    }

    fun clearAutoMoveTarget() {
        autoMoveTarget = null
    }

    fun setAutopilotTarget(worldX: Float, worldY: Float, label: String? = null) {
        setAutoMoveTarget(worldX, worldY)
    }

    fun cancelAutopilot() {
        clearAutoMoveTarget()
    }

    fun switchConfig(configNumber: Int): Boolean {
        clearAutoMoveTarget()
        val target = if (configNumber == 2) 2 else 1
        if (activeConfig == target) return false
        activeConfig = target
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun toggleConfig(): Boolean {
        clearAutoMoveTarget()
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

    fun selectOwnedFormationFromHud(formation: DroneFormation): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen) return false
        clearAutoMoveTarget()
        if (formation !in ownedFormations) return false
        if (activeFormation == formation) return false
        if (formationSwitchCooldown > 0.05f) return false
        activeFormation = formation
        formationSwitchCooldown = GameConfig.FORMATION_SWITCH_COOLDOWN_SEC
        recalculateShipStats(refillVitals = false)
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 36f,
                text = "${formation.displayName.uppercase()} FORMATION",
                life = 1.25f,
                maxLife = 1.25f,
                colorRgb = (formation.accentHex and 0xFFFFFF).toInt(),
                textSize = 14.5f
            )
        )
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
        return selectOwnedFormationFromHud(next)
    }

    // -------------------------------------------------------------------------
    // Ship Classes, Permanent Pilot Capsule & Crafted Fleet Boarding
    // -------------------------------------------------------------------------
    fun selectCapsule(force: Boolean = false): Boolean {
        if (!force && !player.inSafeZone) return false
        if (!force && isFlyingCapsule) return false
        isFlyingCapsule = true
        activeCraftedShipId = null
        activeShipClass = ShipClass.INTERCEPTOR
        syncPlayerActiveShipVisualState()
        recalculateShipStats(refillVitals = player.inSafeZone || force)
        snapVantsToFormation()
        requestSave()
        return true
    }

    fun selectCraftedShip(instanceId: Int): Boolean {
        if (!player.inSafeZone) return false
        val ship = craftedHangarShips.firstOrNull { it.instanceId == instanceId } ?: return false
        if (!isFlyingCapsule && activeCraftedShipId == instanceId) return false
        isFlyingCapsule = false
        activeCraftedShipId = ship.instanceId
        activeShipClass = visualShipClassForIndustrial(ship.industrialType)
        syncPlayerActiveShipVisualState()
        val hullDef = ship.recipeId.shipHullDefinition()
        resizeSlotListsToCounts(
            laserCount = hullDef?.weaponSlotCount ?: 2,
            shieldCount = hullDef?.shieldSlotCount ?: 3
        )
        recalculateShipStats(refillVitals = true)
        snapVantsToFormation()
        requestSave()
        return true
    }

    fun buyOrSelectShipClass(shipClass: ShipClass): Boolean {
        if (!player.inSafeZone) return false
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
        if (!player.inSafeZone) return false
        if (shipClass !in ownedShipClasses) return false
        if (!isFlyingCapsule && activeCraftedShipId == null && activeShipClass == shipClass) return false
        applyShipClassChange(shipClass, refillHpOnUpgrade = false)
        requestSave()
        return true
    }

    private fun applyShipClassChange(newShip: ShipClass, refillHpOnUpgrade: Boolean) {
        val oldMaxHp = player.maxHp
        isFlyingCapsule = false
        activeCraftedShipId = null
        activeShipClass = newShip
        player.shipQuality = CraftQuality.STANDARD
        syncPlayerActiveShipVisualState()
        // Resize Config 1 & 2 slot lists: excess items automatically return to Inventory
        resizeSlotListsForShip(newShip)
        recalculateShipStats(refillVitals = false)
        snapVantsToFormation()
        if (refillHpOnUpgrade || player.inSafeZone) {
            val hpGain = (player.maxHp - oldMaxHp).coerceAtLeast(0f)
            player.hp = min(player.maxHp, player.hp + hpGain)
        }
    }

    private fun resizeSlotListsForShip(shipClass: ShipClass) {
        resizeSlotListsToCounts(shipClass.laserSlotCount, shipClass.shieldSlotCount)
    }

    private fun resizeSlotListsToCounts(laserCount: Int, shieldCount: Int) {
        resizeSlotList(config1LaserSlots, laserCount)
        resizeSlotList(config2LaserSlots, laserCount)
        resizeSlotList(config1ShieldSlots, shieldCount)
        resizeSlotList(config2ShieldSlots, shieldCount)
    }

    private fun resizeSlotList(list: MutableList<EquipmentItem?>, targetSize: Int) {
        while (list.size > targetSize) {
            list.removeAt(list.lastIndex)
        }
        while (list.size < targetSize) {
            list.add(null)
        }
    }

    fun repairVant(vantId: Int): Boolean {
        if (!player.inSafeZone) return false
        val vant = ownedVants.firstOrNull { it.id == vantId } ?: return false
        if (vant.durabilityPct >= 100) return false
        val repairCost = ((100 - vant.durabilityPct) * 2).coerceAtLeast(10)
        if (credits < repairCost) return false
        credits -= repairCost
        vant.durabilityPct = 100
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
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
        // Capsule uses its 3 fixed permanent slots; ship laser/shield grids apply to boarded ships
        if (isFlyingCapsule) return false
        if (!tryAutoEquipInActiveConfig(item)) return false
        recalculateShipStats(refillVitals = false, shieldDeltaAdded = item.quality.applyBonus(item.type.shieldBonus))
        requestSave()
        return true
    }

    fun unequipLaserSlot(slotIndex: Int): Boolean {
        if (isFlyingCapsule) return false
        if (slotIndex !in laserSlots.indices) return false
        if (laserSlots[slotIndex] == null) return false
        laserSlots[slotIndex] = null
        recalculateShipStats(refillVitals = false)
        requestSave()
        return true
    }

    fun unequipShieldSlot(slotIndex: Int): Boolean {
        if (isFlyingCapsule) return false
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
        var bonusShield = 0f

        if (isFlyingCapsule) {
            // Fixed permanent Capsule slots: 1 Basic Laser + 1 Basic Extra/Shield (never lost)
            bonusDamage += CapsulePermanentConfig.PERMANENT_LASER_DAMAGE
            bonusShield += CapsulePermanentConfig.PERMANENT_SHIELD_BONUS
        } else {
            for (slot in laserSlots) {
                if (slot != null) bonusDamage += slot.quality.applyBonus(slot.type.damageBonus)
            }
            for (slot in shieldSlots) {
                if (slot != null) bonusShield += slot.quality.applyBonus(slot.type.shieldBonus)
            }
        }

        // Include Lasers and Shields equipped inside up to 8 active operational VANT drones
        for (vant in getActiveVants()) {
            if (vant.durabilityPct <= 0) continue
            for (slot in vant.slotsForConfig(activeConfig)) {
                if (slot != null) {
                    bonusDamage += slot.quality.applyBonus(slot.type.damageBonus)
                    bonusShield += slot.quality.applyBonus(slot.type.shieldBonus)
                }
            }
        }

        syncPlayerActiveShipVisualState()

        val craftedShip = activeCraftedShip()
        val rawLaserDamage = GameConfig.PLAYER_BASE_LASER_DAMAGE + bonusDamage
        val rawMaxShield: Float
        val rawSpeed: Float
        val rawMaxHull: Float

        when {
            isFlyingCapsule -> {
                rawMaxShield = CapsulePermanentConfig.BASE_SHIELD + bonusShield
                rawSpeed = CapsulePermanentConfig.SPEED
                rawMaxHull = CapsulePermanentConfig.BASE_HULL + (player.level - 1) * GameConfig.PLAYER_HP_PER_LEVEL
            }
            craftedShip != null -> {
                val hullDef = craftedShip.recipeId.shipHullDefinition()
                rawMaxShield = craftedShip.effectiveShield.toFloat() + bonusShield
                rawSpeed = hullDef?.speed ?: activeShipClass.speed
                rawMaxHull = craftedShip.effectiveHull.toFloat() + (player.level - 1) * GameConfig.PLAYER_HP_PER_LEVEL
            }
            else -> {
                rawMaxShield = GameConfig.PLAYER_BASE_MAX_SHIELD + bonusShield
                rawSpeed = activeShipClass.speed
                rawMaxHull = activeShipClass.baseHull + (player.level - 1) * GameConfig.PLAYER_HP_PER_LEVEL
            }
        }

        val f = activeFormation
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

    fun toggleAdminMode(): Boolean {
        isAdminMode = !isAdminMode
        return isAdminMode
    }

    fun adminSetSkillLevel(skill: SkillType, targetLevel: Int) {
        if (!isAdminMode) return
        val clamped = targetLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
        val updated = player.pilotSkills.toMutableMap()
        updated[skill] = SkillProgress(level = clamped, xp = 0L)
        player.pilotSkills = updated
        recalculateShipStats(refillVitals = false)
        requestSave()
    }

    fun adminCycleSkillLevel(skill: SkillType) {
        if (!isAdminMode) return
        val current = skillLevel(skill)
        val next = when {
            current < 33 -> 33
            current < 66 -> 66
            current < PILOT_SKILL_MAX_LEVEL -> PILOT_SKILL_MAX_LEVEL
            else -> PILOT_SKILL_MIN_LEVEL
        }
        adminSetSkillLevel(skill, next)
    }

    fun adminSetAllSkillsLevel(targetLevel: Int) {
        if (!isAdminMode) return
        val clamped = targetLevel.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
        val updated = player.pilotSkills.toMutableMap()
        for (skill in SkillType.entries) {
            updated[skill] = SkillProgress(level = clamped, xp = 0L)
        }
        player.pilotSkills = updated
        recalculateShipStats(refillVitals = false)
        requestSave()
    }

    fun handleWorldTap(worldX: Float, worldY: Float): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen || isTacticalMapOpen || isGalaxyMapOpen || activeStationOverlay != null) {
            return false
        }
        val clearedAutoMove = autoMoveTarget != null
        clearAutoMoveTarget()
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
        return clearedAutoMove
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
        if (isPaused || isHangarOpen || activeStationOverlay != null) return
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        elapsedTime += dt

        if (screenShake > 0f) {
            screenShake = max(0f, screenShake - dt * 28f)
        }
        if (warpFlashTimer > 0f) {
            warpFlashTimer = max(0f, warpFlashTimer - dt)
        }
        if (extractionFeedbackCooldown > 0f) {
            extractionFeedbackCooldown = max(0f, extractionFeedbackCooldown - dt)
        }

        if (player.isAlive) {
            updatePlayer(dt)
            if (isAiming) {
                acquireClosestAlienInRadarCone(atan2(aimInputY, aimInputX))
            }
            handlePlayerAutoFire(dt)
            updateLootBoxes(dt)
            updateResourceNodes(dt)
        } else {
            activeMiningNodeId = null
            activeSalvageWreckId = null
        }

        updateAlienWrecks(dt)
        updatePlayerShipWrecks(dt)
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
        val shipDisplayName = activeShipDisplayName()

        return HudSnapshot(
            credits = credits,
            kills = kills,
            playerLevel = player.level,
            playerXp = player.xp,
            playerXpToNext = GameConfig.xpRequiredForLevel(player.level),
            activeConfig = activeConfig,
            activeShipClass = activeShipClass,
            isFlyingCapsule = isFlyingCapsule,
            activeCraftedShipId = activeCraftedShipId,
            activeShipDisplayName = shipDisplayName,
            activeFormation = activeFormation,
            formationCooldownRemaining = formationSwitchCooldown,
            ownedFormationCount = ownedFormations.size,
            isAutopilotActive = autoMoveTarget != null,
            autopilotTargetX = autoMoveTarget?.x?.toInt() ?: 0,
            autopilotTargetY = autoMoveTarget?.y?.toInt() ?: 0,
            autopilotTargetLabel = "",
            autopilotEtaSeconds = 0,
            autopilotNoticeText = null,
            isTacticalMapOpen = isTacticalMapOpen,
            isGalaxyMapOpen = isGalaxyMapOpen,
            currentSectorId = currentSectorId,
            plottedRoute = plottedRoute,
            nextRouteHopSectorId = nextRouteHopSectorId,
            playerHp = player.hp.roundToInt().coerceAtLeast(0),
            playerMaxHp = player.maxHp.roundToInt().coerceAtLeast(1),
            playerShield = player.shield.roundToInt().coerceAtLeast(0),
            playerMaxShield = player.maxShield.roundToInt().coerceAtLeast(1),
            coordX = player.x.toInt(),
            coordY = player.y.toInt(),
            activeMap = activeMap,
            isNearJumpGate = nearestJumpPortal() != null,
            jumpGateLabel = nearestJumpPortal()?.gateLabel ?: "",
            nearestJumpTargetId = nearestJumpPortal()?.targetSectorId,
            pilotName = pilotName,
            pilotCompany = pilotCompany,
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
            nearestStationKind = nearestStation()?.kind,
            nearestStationName = nearestStation()?.name,
            activeStationOverlay = activeStationOverlay,
            activeLocalMarket = if (activeStationOverlay != null) {
                isolatedLocalMarkets[currentSectorId]?.toSnapshot()
            } else null,
            rawCargo = rawCargo.filterValues { it > 0 }.toMap(),
            refinedCargo = refinedCargo.filterValues { it > 0 }.toMap(),
            overchargedAmmo = overchargedAmmo,
            miningNodeName = resourceNodes.firstOrNull { it.id == activeMiningNodeId }
                ?.let { GameStrings.resourceName(it.resourceType, language) }
                ?: alienWrecks.firstOrNull { it.id == activeSalvageWreckId }
                    ?.let { GameStrings.alienWreckLabel(language) },
            miningProgress = resourceNodes.firstOrNull { it.id == activeMiningNodeId }?.harvestProgress
                ?: alienWrecks.firstOrNull { it.id == activeSalvageWreckId }?.salvageProgress
                ?: 0f,
            equipment = EquipmentSnapshot(
                activeConfig = activeConfig,
                activeShipClass = activeShipClass,
                ownedShipClasses = ownedShipClasses.toSet(),
                isFlyingCapsule = isFlyingCapsule,
                activeCraftedShipId = activeCraftedShipId,
                activeShipRole = activeShipRole(),
                activeIndustrialType = activeIndustrialType(),
                activeShipDisplayName = shipDisplayName,
                equippedSectorChip = equippedSectorChip,
                laserSlots = laserSlots.toList(),
                shieldSlots = shieldSlots.toList(),
                vants = getActiveVants().map { vant ->
                    VantSnapshot(
                        id = vant.id,
                        tier = vant.tier,
                        activeSlots = vant.slotsForConfig(activeConfig).toList(),
                        durabilityPct = vant.durabilityPct
                    )
                },
                flaxVantCount = flaxCount,
                irisVantCount = irisCount,
                ownedFormations = ownedFormations.toSet(),
                activeFormation = activeFormation,
                inventory = getAvailableInventory(),
                ownedItemCounts = counts,
                basicVendorItemCounts = basicVendorItems.toMap(),
                hasAnyReadyFusion = hasFusion,
                totalDamage = player.laserDamage.toInt(),
                totalMaxShield = player.maxShield.toInt(),
                totalSpeed = player.speed.toInt(),
                totalMaxHull = player.maxHp.toInt(),
                activeShipQuality = player.shipQuality,
                totalCargoCapacity = currentCargoCapacity()
            ),
            mission = MissionSnapshot(
                currentMissionIndex = currentMissionIndex,
                currentMissionProgress = currentMissionProgress,
                activeMission = activeMission,
                isLevelLocked = isMissionLevelLocked(),
                isReadyToClaim = isMissionReadyToClaim(),
                allCompleted = activeMission == null
            ),
            pilotSkills = player.pilotSkills.toMap(),
            craftedHangarShips = craftedHangarShips.toList(),
            craftedInventoryItems = craftedInventoryItems.toList(),
            lastDeathSummary = lastDeathSummary,
            activeSectorWreckCount = playerShipWrecks.count { it.sectorId == currentSectorId && !it.isEmpty },
            isAdminMode = isAdminMode
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
        val isAutoMoving: Boolean
        val moveTarget = autoMoveTarget
        if (mag > GameConfig.JOYSTICK_DEADZONE) {
            isAutoMoving = false
            // Instant manual override cancels tap-to-move seamlessly
            clearAutoMoveTarget()
            movementAngle = atan2(moveInputY, moveInputX)
            val dirX = moveInputX / mag
            val dirY = moveInputY / mag
            player.x += dirX * player.speed * dt
            player.y += dirY * player.speed * dt
            player.isMoving = true
            targetThrust = mag.coerceIn(0.45f, 1f)
        } else if (moveTarget != null) {
            val dx = moveTarget.x - player.x
            val dy = moveTarget.y - player.y
            val dist = hypot(dx, dy)
            if (dist <= GameConfig.AUTOPILOT_STOP_RADIUS) {
                isAutoMoving = false
                clearAutoMoveTarget()
                player.isMoving = false
                targetThrust = 0f
            } else {
                isAutoMoving = true
                movementAngle = atan2(dy, dx)
                val stepDist = min(dist, player.speed * dt)
                player.x += (dx / dist) * stepDist
                player.y += (dy / dist) * stepDist
                player.isMoving = true
                targetThrust = 1f
            }
        } else {
            isAutoMoving = false
            player.isMoving = false
            targetThrust = 0f
        }
        val thrustLerp = 1f - exp(-10f * dt)
        player.thrustFactor += (targetThrust - player.thrustFactor) * thrustLerp
        if (player.thrustFactor < 0.005f) player.thrustFactor = 0f
        player.thrusterPulse += dt * (8f + 18f * player.thrustFactor)

        // Trajectory alignment: prioritize continuous heading alignment while auto-moving
        val locked = getLockedAlien()
        val isActivelyAttacking = player.isAutoFireEnabled && !player.inSafeZone && locked != null
        if (isAutoMoving && movementAngle != null) {
            player.angleRad = lerpAngle(
                player.angleRad,
                movementAngle,
                1f - exp(-GameConfig.PLAYER_TURN_SPEED * 1.4f * dt)
            )
        } else if (isActivelyAttacking && locked != null) {
            val aimAngle = atan2(locked.y - player.y, locked.x - player.x)
            player.angleRad = lerpAngle(player.angleRad, aimAngle, 1f - exp(-14f * dt))
        } else if (movementAngle != null) {
            player.angleRad = lerpAngle(
                player.angleRad,
                movementAngle,
                1f - exp(-GameConfig.PLAYER_TURN_SPEED * dt)
            )
        }

        val sector = currentSector
        val mapSize = sector.mapSize
        val minOuter = -GameConfig.MAP_X1_RADIATION_OUTER_BUFFER
        val maxOuter = mapSize + GameConfig.MAP_X1_RADIATION_OUTER_BUFFER
        player.x = player.x.coerceIn(minOuter, maxOuter)
        player.y = player.y.coerceIn(minOuter, maxOuter)

        val inSafe = isPointInActiveSafeZone(player.x, player.y)
        player.inSafeZone = inSafe
        player.currentZone = resolveSpaceZoneForSector(sector, inSafe)

        val outsideSquare = player.x < 0f || player.x > mapSize || player.y < 0f || player.y > mapSize
        val distToBorder = min(
            min(player.x, mapSize - player.x),
            min(player.y, mapSize - player.y)
        )
        player.isTakingZoneRadiation = !sector.isEntireSectorSafe && outsideSquare
        player.requiredChipWarning = null
        player.inRadiationWarning = !sector.isEntireSectorSafe &&
            (outsideSquare || distToBorder < GameConfig.RADIATION_WARNING_DISTANCE)

        if (player.isTakingZoneRadiation && player.isAlive) {
            player.radiationTickTimer += dt
            if (player.radiationTickTimer >= GameConfig.RADIATION_TICK_INTERVAL) {
                player.radiationTickTimer -= GameConfig.RADIATION_TICK_INTERVAL
                val radDmg = GameConfig.MAP_X1_BORDER_RADIATION_DPS * GameConfig.RADIATION_TICK_INTERVAL
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
        val hullR = activeHullRadius()
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
        val hullR = activeHullRadius()
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
        val useOvercharge = overchargedAmmo > 0
        if (useOvercharge) {
            overchargedAmmo = (overchargedAmmo - 1).coerceAtLeast(0)
        }
        lasers.add(
            LaserBolt(
                x = player.x + cos(fireAngle) * 16f,
                y = player.y + sin(fireAngle) * 16f,
                vx = cos(fireAngle) * GameConfig.PLAYER_LASER_SPEED,
                vy = sin(fireAngle) * GameConfig.PLAYER_LASER_SPEED,
                angleRad = fireAngle,
                fromPlayer = true,
                targetAlienId = target.id,
                damage = if (useOvercharge) player.laserDamage * 1.35f else player.laserDamage,
                colorHex = if (useOvercharge) 0xFFFACC15 else activeShipClass.accentColorHex,
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
            if (dist <= GameConfig.LOOT_COLLECT_RADIUS + (activeHullRadius() - 20f).coerceAtLeast(0f)) {
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

    fun currentCargoUsed(): Int = rawCargo.values.sum() + refinedCargo.values.sum()

    fun currentCargoCapacity(): Int = when {
        isFlyingCapsule -> CapsulePermanentConfig.CARGO_CAPACITY
        activeCraftedShip() != null -> activeCraftedShip()!!.effectiveCargo
        else -> activeShipClass.effectiveCargo(player.shipQuality)
    }

    private fun hasCargoSpace(units: Int = 1): Boolean =
        currentCargoUsed() + units <= currentCargoCapacity()

    private fun skillLevel(skill: SkillType): Int =
        player.pilotSkills[skill]?.level ?: PILOT_SKILL_MIN_LEVEL

    private fun awardSkillXp(skill: SkillType, amount: Long, popupX: Float, popupY: Float) {
        if (amount <= 0L) return
        val before = player.pilotSkills[skill] ?: SkillProgress()
        val after = before.addXp(amount)
        val updated = player.pilotSkills.toMutableMap()
        updated[skill] = after
        player.pilotSkills = updated

        if (after.level > before.level) {
            val skillLabel = GameStrings.skillName(skill, language).uppercase()
            popups.add(
                FloatingPopup(
                    x = popupX,
                    y = popupY - 26f,
                    text = "$skillLabel LV ${after.level}!",
                    life = 2.0f,
                    maxLife = 2.0f,
                    colorRgb = 0x38BDF8,
                    textSize = 15.5f
                )
            )
            shockwaves.add(
                ShockwaveRing(
                    x = popupX,
                    y = popupY,
                    maxRadius = 72f,
                    life = 0.55f,
                    maxLife = 0.55f,
                    colorHex = 0xFF38BDF8
                )
            )
        }
    }

    private fun emitExtractionFeedback(text: String, x: Float, y: Float, colorRgb: Int) {
        if (extractionFeedbackCooldown > 0f) return
        extractionFeedbackCooldown = 2.4f
        popups.add(
            FloatingPopup(
                x = x,
                y = y,
                text = text,
                life = 1.65f,
                maxLife = 1.65f,
                colorRgb = colorRgb,
                textSize = 12.5f
            )
        )
    }

    private fun miningXpForResource(res: ResourceType, sector: SectorNode): Long {
        if (res.isYellowCrystal) {
            return if (sector.isNucleus) 120L else 95L
        }
        val zoneBonus = when (sector.zoneType) {
            GalaxyZoneType.NUCLEUS -> 45L
            GalaxyZoneType.HIGH_RISK_YELLOW_RING -> 30L
            GalaxyZoneType.MODERATE_ZONE -> 18L
            GalaxyZoneType.INTERMEDIATE_ZONE -> 8L
            else -> 0L
        }
        return (18L + (res.tier - 1).coerceAtLeast(0) * 12L + zoneBonus)
    }

    private fun salvageXpForWreck(wreck: AlienWreck, sector: SectorNode): Long {
        val base = when (wreck.sourceTier) {
            AlienTier.SCOUT -> 22L
            AlienTier.HUNTER -> 36L
            AlienTier.DESTROYER -> 58L
            AlienTier.DREADNOUGHT_BOSS -> 90L
        }
        val bossMult = if (wreck.fromBoss) 1.4 else 1.0
        val nucleusBonus = if (sector.isNucleus) 30L else 0L
        return Math.round(base * bossMult) + nucleusBonus
    }

    private fun xenoCoreDropChance(wreck: AlienWreck, sector: SectorNode): Float {
        val isStrong = wreck.fromBoss ||
            wreck.sourceTier == AlienTier.DESTROYER ||
            wreck.sourceTier == AlienTier.DREADNOUGHT_BOSS
        return when {
            sector.isNucleus && isStrong -> 0.16f
            sector.isNucleus -> 0.10f
            wreck.fromBoss -> 0.10f
            isStrong -> 0.06f
            else -> 0f
        }
    }

    private fun updateResourceNodes(dt: Float) {
        if (resourceNodes.isEmpty()) {
            activeMiningNodeId = null
            return
        }
        val mineRange = 122f + (activeHullRadius() - 20f).coerceAtLeast(0f)
        var closest: ResourceNodeEntity? = null
        var closestDist = mineRange
        for (node in resourceNodes) {
            node.pulsePhase += dt * 3.1f
            node.rotationRad += dt * (if (node.resourceType.isYellowCrystal) 0.42f else 0.22f)
            val d = hypot(player.x - node.x, player.y - node.y)
            if (d <= closestDist) {
                closestDist = d
                closest = node
            }
        }

        val shipRole = activeShipRole()
        val cargoFull = !hasCargoSpace(1)
        var stateChanged = false
        val depletedNodes = mutableListOf<ResourceNodeEntity>()

        for (node in resourceNodes) {
            if (node != closest) {
                if (node.harvestProgress > 0f) {
                    node.harvestProgress = 0f
                }
                continue
            }

            // Role restriction: COMBAT ships cannot mine resource nodes
            if (shipRole == ShipRole.COMBAT) {
                node.harvestProgress = 0f
                activeMiningNodeId = null
                val msg = if (language == GameLanguage.ES) {
                    "NAVES DE COMBATE NO PUEDEN MINAR"
                } else {
                    "COMBAT SHIPS CANNOT MINE"
                }
                emitExtractionFeedback(msg, node.x, node.y - node.radius - 18f, 0xFF5252)
                continue
            }

            if (cargoFull) {
                node.harvestProgress = 0f
                activeMiningNodeId = null
                val msg = if (language == GameLanguage.ES) "BODEGA LLENA" else "CARGO FULL"
                emitExtractionFeedback(msg, player.x, player.y - activeHullRadius() - 24f, 0xFBBF24)
                continue
            }

            val shipMultiplier = when (shipRole) {
                ShipRole.CAPSULE -> CapsulePermanentConfig.MINING_TIME_MULTIPLIER
                ShipRole.INDUSTRIAL -> activeIndustrialType()
                    ?.extractionTimeMultiplier(node.resourceType.category) ?: 1.0f
                ShipRole.COMBAT -> 1.0f
            }
            val extractionSeconds = (baseExtractionSeconds(skillLevel(SkillType.MINING)) * shipMultiplier)
                .coerceAtLeast(0.1f)

            activeMiningNodeId = node.id
            node.harvestProgress = (node.harvestProgress + dt / extractionSeconds).coerceAtMost(1f)

            if (node.harvestProgress >= 1f) {
                node.harvestProgress = 0f
                node.charges = (node.charges - 1).coerceAtLeast(0)

                val res = node.resourceType
                rawCargo[res] = (rawCargo[res] ?: 0) + 1

                val xpGained = miningXpForResource(res, currentSector)
                awardSkillXp(SkillType.MINING, xpGained, node.x, node.y - node.radius - 10f)

                val resName = GameStrings.resourceName(res, language)
                popups.add(
                    FloatingPopup(
                        x = node.x,
                        y = node.y - node.radius - 14f,
                        text = "+1 $resName (${node.charges}/${node.maxCharges}) +$xpGained XP",
                        life = 1.5f,
                        maxLife = 1.5f,
                        colorRgb = (res.colorHex and 0xFFFFFF).toInt(),
                        textSize = 13f
                    )
                )
                spawnHitSparks(node.x, node.y, colorHex = res.colorHex, count = 8)
                stateChanged = true

                if (node.isDepleted) {
                    depletedNodes.add(node)
                    activeMiningNodeId = null
                }
            }
        }

        if (depletedNodes.isNotEmpty()) {
            for (deadNode in depletedNodes) {
                resourceNodes.remove(deadNode)
                spawnResourceNodeForActiveSector()
            }
        }
        if (closest == null) {
            activeMiningNodeId = null
        }
        if (stateChanged) {
            requestSave()
        }
    }

    private fun spawnAlienWreck(alien: AlienEntity) {
        val wreckCharges = AlienWreck.MIN_CHARGES +
            random.nextInt(AlienWreck.MAX_CHARGES - AlienWreck.MIN_CHARGES + 1)
        alienWrecks.add(
            AlienWreck(
                id = nextAlienWreckId++,
                sourceTier = alien.tier,
                fromBoss = alien.isBoss,
                x = alien.x,
                y = alien.y,
                rotationRad = alien.angleRad,
                radius = if (alien.isBoss) 26f else 20f,
                maxCharges = wreckCharges,
                charges = wreckCharges,
                expirationTimerSec = 60f,
                maxExpirationSec = 60f,
                salvageProgress = 0f,
                pulsePhase = random.nextFloat() * 6.28f
            )
        )
    }

    private fun updateAlienWrecks(dt: Float) {
        if (alienWrecks.isEmpty()) {
            activeSalvageWreckId = null
            return
        }

        val salvageRange = 122f + (activeHullRadius() - 20f).coerceAtLeast(0f)
        var closest: AlienWreck? = null
        var closestDist = salvageRange
        val expiredWrecks = mutableListOf<AlienWreck>()

        for (wreck in alienWrecks) {
            wreck.expirationTimerSec -= dt
            wreck.pulsePhase += dt * 3.2f
            if (wreck.isExpired || wreck.charges <= 0) {
                expiredWrecks.add(wreck)
                continue
            }
            if (player.isAlive) {
                val d = hypot(player.x - wreck.x, player.y - wreck.y)
                if (d <= closestDist) {
                    closestDist = d
                    closest = wreck
                }
            }
        }

        if (expiredWrecks.isNotEmpty()) {
            for (expired in expiredWrecks) {
                if (activeSalvageWreckId == expired.id) {
                    activeSalvageWreckId = null
                }
                alienWrecks.remove(expired)
            }
        }

        if (!player.isAlive || alienWrecks.isEmpty()) {
            activeSalvageWreckId = null
            return
        }

        val shipRole = activeShipRole()
        val cargoFull = !hasCargoSpace(1)
        var stateChanged = false
        val depletedWrecks = mutableListOf<AlienWreck>()

        for (wreck in alienWrecks) {
            if (wreck != closest) {
                if (wreck.salvageProgress > 0f) {
                    wreck.salvageProgress = 0f
                }
                continue
            }

            // Role restriction: INDUSTRIAL ships cannot salvage wrecks
            if (shipRole == ShipRole.INDUSTRIAL) {
                wreck.salvageProgress = 0f
                activeSalvageWreckId = null
                val msg = if (language == GameLanguage.ES) {
                    "NAVES INDUSTRIALES NO PUEDEN CHATARREAR"
                } else {
                    "INDUSTRIAL SHIPS CANNOT SALVAGE WRECKS"
                }
                emitExtractionFeedback(msg, wreck.x, wreck.y - wreck.radius - 18f, 0xFF5252)
                continue
            }

            if (cargoFull) {
                wreck.salvageProgress = 0f
                activeSalvageWreckId = null
                val msg = if (language == GameLanguage.ES) "BODEGA LLENA" else "CARGO FULL"
                emitExtractionFeedback(msg, player.x, player.y - activeHullRadius() - 24f, 0xFBBF24)
                continue
            }

            val salvageSeconds = baseExtractionSeconds(skillLevel(SkillType.SALVAGING))
                .coerceAtLeast(0.1f)
            activeSalvageWreckId = wreck.id
            wreck.salvageProgress = (wreck.salvageProgress + dt / salvageSeconds).coerceAtMost(1f)

            if (wreck.salvageProgress >= 1f) {
                wreck.salvageProgress = 0f
                wreck.charges = (wreck.charges - 1).coerceAtLeast(0)

                val salvageType = ResourceType.XENO_SALVAGE
                rawCargo[salvageType] = (rawCargo[salvageType] ?: 0) + 1

                val coreChance = xenoCoreDropChance(wreck, currentSector)
                val droppedCore = coreChance > 0f &&
                    random.nextFloat() < coreChance &&
                    hasCargoSpace(1)
                if (droppedCore) {
                    rawCargo[ResourceType.XENO_CORE] = (rawCargo[ResourceType.XENO_CORE] ?: 0) + 1
                }

                val xpGained = salvageXpForWreck(wreck, currentSector)
                awardSkillXp(SkillType.SALVAGING, xpGained, wreck.x, wreck.y - wreck.radius - 10f)

                val salvageName = GameStrings.resourceName(salvageType, language)
                val coreSuffix = if (droppedCore) {
                    " • ★ +1 ${GameStrings.resourceName(ResourceType.XENO_CORE, language)}"
                } else {
                    ""
                }
                popups.add(
                    FloatingPopup(
                        x = wreck.x,
                        y = wreck.y - wreck.radius - 14f,
                        text = "+1 $salvageName$coreSuffix (${wreck.charges}/${wreck.maxCharges}) +$xpGained XP",
                        life = 1.6f,
                        maxLife = 1.6f,
                        colorRgb = if (droppedCore) 0xE879F9 else (salvageType.colorHex and 0xFFFFFF).toInt(),
                        textSize = 13f
                    )
                )
                spawnHitSparks(
                    wreck.x,
                    wreck.y,
                    colorHex = if (droppedCore) ResourceType.XENO_CORE.colorHex else salvageType.colorHex,
                    count = if (droppedCore) 14 else 9
                )
                stateChanged = true

                if (wreck.isDepleted) {
                    depletedWrecks.add(wreck)
                    activeSalvageWreckId = null
                }
            }
        }

        if (depletedWrecks.isNotEmpty()) {
            for (done in depletedWrecks) {
                alienWrecks.remove(done)
            }
        }
        if (closest == null) {
            activeSalvageWreckId = null
        }
        if (stateChanged) {
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

            val mapSize = activeMapSize()
            val playerInAlienZone = !player.inSafeZone &&
                player.x >= 0f && player.x <= mapSize &&
                player.y >= 0f && player.y <= mapSize

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
                if (hypot(bolt.x - player.x, bolt.y - player.y) <= activeHullRadius() + 6f) {
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
        // Strict Safe Zone protection: zero damage in Capital Cities and inside Safe Zones
        if (currentSector.isEntireSectorSafe) return
        if (player.inSafeZone && !isRadiation) return
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
                y = player.y - activeHullRadius() - 28f,
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
            autoMoveTarget = null
            isTacticalMapOpen = false
            isGalaxyMapOpen = false
            spawnExplosion(player.x, player.y, primaryHex = activeShipClass.accentColorHex, accentHex = 0xFF38BDF8, isBoss = false)
            screenShake = 14f
            handlePlayerDeath()
        }
    }

    /**
     * Step 6 Death & 60/40 Loot Rules:
     * - Crafted/non-capsule ship is permanently destroyed and removed from Hangar.
     * - 60% of cargo and equipped non-capsule ship gear spawns in a lootable PlayerShipWreck at death coords; 40% destroyed.
     * - Any owned VANTs return with the pilot to Home City taking 25% durability damage.
     * - Capsule & its 3 permanent items are NEVER lost (only loose cargo drops at 60/40).
     */
    private fun handlePlayerDeath() {
        val wasInCapsule = isFlyingCapsule
        val destroyedName = if (wasInCapsule) null else activeShipDisplayName()
        val deathSector = currentSectorId
        val deathX = player.x
        val deathY = player.y
        val homeCity = GalaxyMapGraph.homeCityForCompany(pilotCompany)

        val wreckRaw = linkedMapOf<ResourceType, Int>()
        val wreckRefined = linkedMapOf<ResourceType, Int>()
        var cargoDropped = 0
        var cargoDestroyed = 0

        for ((res, qty) in rawCargo) {
            if (qty <= 0) continue
            val dropQty = splitSixtyForty(qty)
            if (dropQty > 0) wreckRaw[res] = dropQty
            cargoDropped += dropQty
            cargoDestroyed += (qty - dropQty)
        }
        rawCargo.clear()

        for ((res, qty) in refinedCargo) {
            if (qty <= 0) continue
            val dropQty = splitSixtyForty(qty)
            if (dropQty > 0) wreckRefined[res] = dropQty
            cargoDropped += dropQty
            cargoDestroyed += (qty - dropQty)
        }
        refinedCargo.clear()

        val wreckGear = mutableListOf<EquipmentItem>()
        var gearDropped = 0
        var gearDestroyed = 0
        var vantsDamagedCount = 0

        if (!wasInCapsule) {
            // Gather all equipped non-capsule ship gear (dedup by instanceId across Config 1 & 2)
            val equippedById = linkedMapOf<Int, EquipmentItem>()
            for (slot in config1LaserSlots) if (slot != null) equippedById[slot.instanceId] = slot
            for (slot in config2LaserSlots) if (slot != null) equippedById[slot.instanceId] = slot
            for (slot in config1ShieldSlots) if (slot != null) equippedById[slot.instanceId] = slot
            for (slot in config2ShieldSlots) if (slot != null) equippedById[slot.instanceId] = slot
            equippedSectorChip?.let { equippedById[it.instanceId] = it }

            val equippedIds = equippedById.keys
            if (equippedIds.isNotEmpty()) {
                ownedItems.removeAll { it.instanceId in equippedIds }
                for (i in config1LaserSlots.indices) config1LaserSlots[i] = null
                for (i in config2LaserSlots.indices) config2LaserSlots[i] = null
                for (i in config1ShieldSlots.indices) config1ShieldSlots[i] = null
                for (i in config2ShieldSlots.indices) config2ShieldSlots[i] = null
                equippedSectorChip = null

                val shuffled = equippedById.values.toMutableList()
                shuffled.shuffle(random)
                val dropCount = splitSixtyForty(shuffled.size)
                for (i in shuffled.indices) {
                    if (i < dropCount) {
                        wreckGear.add(shuffled[i])
                    }
                }
                gearDropped = dropCount
                gearDestroyed = shuffled.size - dropCount
            }

            // Permanently destroy the active non-capsule ship
            val lostCraftedId = activeCraftedShipId
            if (lostCraftedId != null) {
                craftedHangarShips.removeAll { it.instanceId == lostCraftedId }
            } else {
                ownedShipClasses.remove(activeShipClass)
            }

            // Equipped/owned VANTs return with pilot to Home City taking 25% durability damage
            for (vant in ownedVants) {
                vant.durabilityPct = (vant.durabilityPct - 25).coerceAtLeast(0)
                vantsDamagedCount++
            }

            // Return pilot to their Permanent Capsule immediately in persisted state
            isFlyingCapsule = true
            activeCraftedShipId = null
            activeShipClass = ShipClass.INTERCEPTOR
            syncPlayerActiveShipVisualState()
        }

        val wreck = PlayerShipWreck(
            id = nextPlayerWreckId++,
            sectorId = deathSector,
            x = deathX,
            y = deathY,
            shipName = destroyedName ?: GameStrings.of(language).capsuleName,
            isCapsuleWreck = wasInCapsule,
            rawCargo = wreckRaw,
            refinedCargo = wreckRefined,
            droppedEquipment = wreckGear
        )
        if (!wreck.isEmpty) {
            playerShipWrecks.add(wreck)
        }

        lastDeathSummary = DeathSummary(
            wasInCapsule = wasInCapsule,
            destroyedShipName = destroyedName,
            deathSectorId = deathSector,
            deathX = deathX.roundToInt(),
            deathY = deathY.roundToInt(),
            cargoDroppedToWreck = cargoDropped,
            cargoDestroyed = cargoDestroyed,
            gearDroppedToWreck = gearDropped,
            gearDestroyed = gearDestroyed,
            vantsDamagedCount = vantsDamagedCount,
            homeCityId = homeCity
        )
        requestSave()
    }

    private fun splitSixtyForty(quantity: Int): Int {
        if (quantity <= 0) return 0
        if (quantity == 1) return if (random.nextFloat() < 0.60f) 1 else 0
        return (quantity * 0.60f).roundToInt().coerceIn(0, quantity)
    }

    private fun updatePlayerShipWrecks(dt: Float) {
        if (playerShipWrecks.isEmpty()) return
        val toRemove = mutableListOf<PlayerShipWreck>()
        var stateChanged = false

        for (wreck in playerShipWrecks) {
            wreck.expirationTimerSec -= dt
            if (wreck.expirationTimerSec <= 0f || wreck.isEmpty) {
                toRemove.add(wreck)
                continue
            }
            if (wreck.sectorId != currentSectorId) continue

            wreck.beaconTimerSec -= dt
            if (wreck.beaconTimerSec <= 0f) {
                wreck.beaconTimerSec = 1.4f
                shockwaves.add(
                    ShockwaveRing(
                        x = wreck.x,
                        y = wreck.y,
                        maxRadius = 64f,
                        life = 0.85f,
                        maxLife = 0.85f,
                        colorHex = 0xFFF59E0B
                    )
                )
            }

            if (player.isAlive && hypot(player.x - wreck.x, player.y - wreck.y) <= 84f) {
                var recoveredCount = 0
                if (wreck.droppedEquipment.isNotEmpty()) {
                    ownedItems.addAll(wreck.droppedEquipment)
                    recoveredCount += wreck.droppedEquipment.size
                    wreck.droppedEquipment.clear()
                }
                val rawKeys = wreck.rawCargo.keys.toList()
                for (res in rawKeys) {
                    val qty = wreck.rawCargo[res] ?: 0
                    val free = (currentCargoCapacity() - currentCargoUsed()).coerceAtLeast(0)
                    if (free <= 0) break
                    val take = min(qty, free)
                    if (take > 0) {
                        rawCargo[res] = (rawCargo[res] ?: 0) + take
                        recoveredCount += take
                        val left = qty - take
                        if (left > 0) wreck.rawCargo[res] = left else wreck.rawCargo.remove(res)
                    }
                }
                val refKeys = wreck.refinedCargo.keys.toList()
                for (res in refKeys) {
                    val qty = wreck.refinedCargo[res] ?: 0
                    val free = (currentCargoCapacity() - currentCargoUsed()).coerceAtLeast(0)
                    if (free <= 0) break
                    val take = min(qty, free)
                    if (take > 0) {
                        refinedCargo[res] = (refinedCargo[res] ?: 0) + take
                        recoveredCount += take
                        val left = qty - take
                        if (left > 0) wreck.refinedCargo[res] = left else wreck.refinedCargo.remove(res)
                    }
                }
                if (recoveredCount > 0) {
                    popups.add(
                        FloatingPopup(
                            x = wreck.x,
                            y = wreck.y - 24f,
                            text = "${GameStrings.of(language).wreckLootRecoveredPopup} (+$recoveredCount)",
                            life = 2.0f,
                            maxLife = 2.0f,
                            colorRgb = 0xFBBF24,
                            textSize = 14f
                        )
                    )
                    stateChanged = true
                }
                if (wreck.isEmpty) {
                    toRemove.add(wreck)
                }
            }
        }
        if (toRemove.isNotEmpty()) {
            playerShipWrecks.removeAll(toRemove)
        }
        if (stateChanged) {
            requestSave()
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

        // Spawn salvageable Alien Wreck (2–4 charges, 60s expiration timer)
        spawnAlienWreck(alien)

        if (lockedAlienId == alien.id) {
            lockedAlienId = null
        }

        spawnAlienForTier(tier, minDistanceFromPlayer = 600f)
        requestSave()
    }

    private fun isOutOfActiveMapBounds(x: Float, y: Float): Boolean {
        val mapSize = activeMapSize()
        val pad = GameConfig.MAP_X1_RADIATION_OUTER_BUFFER + 180f
        return x < -pad || x > mapSize + pad || y < -pad || y > mapSize + pad
    }

    private fun randomPointInActiveSector(margin: Float, safeZoneBuffer: Float): Pair<Float, Float> {
        val sector = currentSector
        val mapSize = sector.mapSize
        val minCoord = margin.coerceAtMost(mapSize * 0.4f)
        val maxCoord = (mapSize - margin).coerceAtLeast(mapSize * 0.6f)
        val (sx, sy) = activeSafeZoneCenter()
        val safeR = if (sector.hasCentralSafeZone) sector.safeZoneRadius + safeZoneBuffer else 0f
        repeat(28) {
            val rx = minCoord + random.nextFloat() * (maxCoord - minCoord)
            val ry = minCoord + random.nextFloat() * (maxCoord - minCoord)
            if (safeR <= 0f || hypot(rx - sx, ry - sy) >= safeR) {
                return Pair(rx, ry)
            }
        }
        return Pair(minCoord + 200f, minCoord + 200f)
    }

    private fun spawnBonusBox() {
        val sector = currentSector
        val (bx, by) = randomPointInActiveSector(margin = 260f, safeZoneBuffer = 140f)
        val tierScale = sector.maxTier.coerceIn(1, 5)
        val creditAmount = ((4 + tierScale * 3) + random.nextInt(5 + tierScale * 2)) * 25
        val xpAmount = 15 + tierScale * 18
        val shieldAmount = 40f + tierScale * 25f
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
        val margin = 280f + tier.radius
        return randomPointInActiveSector(margin = margin, safeZoneBuffer = 200f + tier.radius)
    }

    private fun clampAlienToZoneRing(alien: AlienEntity) {
        val sector = currentSector
        val mapSize = sector.mapSize
        val pad = alien.radius + 90f
        alien.x = alien.x.coerceIn(pad, mapSize - pad)
        alien.y = alien.y.coerceIn(pad, mapSize - pad)
        if (sector.hasCentralSafeZone && sector.safeZoneRadius > 0f) {
            val (sx, sy) = activeSafeZoneCenter()
            val dx = alien.x - sx
            val dy = alien.y - sy
            val minDist = sector.safeZoneRadius + alien.radius + 40f
            val dist = hypot(dx, dy)
            if (dist < minDist) {
                if (dist < 0.001f) {
                    alien.x = sx + minDist
                    alien.y = sy
                } else {
                    val scale = minDist / dist
                    alien.x = sx + dx * scale
                    alien.y = sy + dy * scale
                }
            }
        }
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

    private fun generateSectorStars(sectorId: String): List<Star> {
        val sector = GalaxyMapGraph.sectorOf(sectorId)
        val mapSize = sector.mapSize
        val count = if (sector.isLargeSector) 2600 else GameConfig.MAP_X1_STAR_COUNT
        val starRng = Random(77411L + sectorId.hashCode().toLong() * 31L)
        return List(count) {
            Star(
                x = starRng.nextFloat() * mapSize,
                y = starRng.nextFloat() * mapSize,
                radius = 0.85f + starRng.nextFloat() * 2.15f,
                alpha = 0.28f + starRng.nextFloat() * 0.70f,
                tintType = starRng.nextInt(3)
            )
        }
    }

    private fun generateSectorNebulae(sectorId: String): List<NebulaCloud> {
        val sector = GalaxyMapGraph.sectorOf(sectorId)
        val mapSize = sector.mapSize
        val count = if (sector.isLargeSector) 44 else GameConfig.MAP_X1_NEBULA_COUNT
        val nebRng = Random(51827L + sectorId.hashCode().toLong() * 17L)
        val palette = when (sector.zoneType) {
            GalaxyZoneType.NUCLEUS, GalaxyZoneType.HIGH_RISK_YELLOW_RING ->
                longArrayOf(0x1C92400E, 0x18B45309, 0x1678350F, 0x164C1D95)
            GalaxyZoneType.CAPITAL_CITY, GalaxyZoneType.SAFE_ZONE ->
                longArrayOf(0x1C0E7490, 0x181D4ED8, 0x1610B981, 0x16312E81)
            else ->
                longArrayOf(0x1C312E81, 0x1A4C1D95, 0x181E3A8A, 0x166D28D9, 0x140E7490)
        }
        return List(count) {
            NebulaCloud(
                x = 600f + nebRng.nextFloat() * (mapSize - 1200f),
                y = 600f + nebRng.nextFloat() * (mapSize - 1200f),
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
