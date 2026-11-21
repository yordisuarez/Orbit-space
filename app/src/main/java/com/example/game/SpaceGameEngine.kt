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
    val resourceNodes = mutableListOf<ResourceNodeEntity>()
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

    var plottedRoute: List<String> = emptyList()
        private set

    val nextRouteHopSectorId: String?
        get() = if (plottedRoute.size >= 2 && plottedRoute[0] == currentSectorId) plottedRoute[1] else null

    val activeMap: GameMap
        get() = GameMap.MAP_X1

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

    // Interactive Tactical Map, Galactic Map & Tap-to-Fly Autopilot
    var isTacticalMapOpen: Boolean = false
        private set
    var isGalaxyMapOpen: Boolean = false
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
                    config2SlotIds = vant.config2Slots.map { it?.instanceId }
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
            overchargedAmmo = overchargedAmmo
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

        rawCargo.clear()
        for ((res, qty) in state.rawCargo) {
            if (qty > 0) rawCargo[res] = qty
        }
        refinedCargo.clear()
        for ((res, qty) in state.refinedCargo) {
            if (qty > 0) refinedCargo[res] = qty
        }
        overchargedAmmo = state.overchargedAmmo.coerceAtLeast(0)

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
            rawCargo.clear()
            refinedCargo.clear()
            overchargedAmmo = 0
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
        autopilotTargetX = null
        autopilotTargetY = null

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
        activeMiningNodeId = null

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
        val yieldAmt = if (resourceType.isYellowCrystal) 2 else (2 + random.nextInt(2))
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
                yieldAmount = yieldAmt,
                radius = radius,
                rotationRad = random.nextFloat() * 2f * PI.toFloat(),
                pulsePhase = random.nextFloat() * 6.28f
            )
        )
    }

    fun triggerJumpGate(): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen) return false
        val portal = nearestJumpPortal() ?: return false

        autopilotTargetX = null
        autopilotTargetY = null
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
        recalculateShipStats(refillVitals = true)
        val homeCity = GalaxyMapGraph.homeCityForCompany(pilotCompany)
        val respawnSector = if (currentSector.hasCentralSafeZone) currentSectorId else homeCity
        player.rocketCooldown = 0f
        player.isAlive = true
        player.isAutoFireEnabled = false
        player.hitFlash = 0f
        player.shieldHitFlash = 0f
        player.timeSinceDamaged = GameConfig.SHIELD_REGEN_DELAY
        autopilotTargetX = null
        autopilotTargetY = null
        isTacticalMapOpen = false
        isGalaxyMapOpen = false
        enterSector(respawnSector, fromSectorId = null, triggerWarpFlash = false, showPopup = true)
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
        autopilotTargetX = null
        autopilotTargetY = null
        activeStationOverlay = station.kind
        return true
    }

    fun closeStation() {
        activeStationOverlay = null
        requestSave()
    }

    /**
     * Enforces the Clockwise City Refining Loop:
     * Only raw resources whose [ResourceType.refinesInSectorId] matches [currentSectorId]
     * can be refined at this station (2 raw -> 1 refined).
     */
    fun refineResource(type: ResourceType, refineAll: Boolean): Boolean {
        if (activeStationOverlay != StationKind.REFINERY) return false
        if (type.refinesInSectorId != currentSectorId) return false
        val ownedRaw = rawCargo[type] ?: 0
        val batches = if (refineAll) {
            ownedRaw / ResourceType.RAW_REQUIRED_PER_REFINE
        } else {
            if (ownedRaw >= ResourceType.RAW_REQUIRED_PER_REFINE) 1 else 0
        }
        if (batches <= 0) return false

        val remainingRaw = ownedRaw - batches * ResourceType.RAW_REQUIRED_PER_REFINE
        if (remainingRaw > 0) {
            rawCargo[type] = remainingRaw
        } else {
            rawCargo.remove(type)
        }
        refinedCargo[type] = (refinedCargo[type] ?: 0) + batches
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 26f,
                text = "REFINED +$batches ${type.refinedName}",
                life = 1.55f,
                maxLife = 1.55f,
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
        if (entry.localStock <= 0 || credits < entry.buyPrice) return false

        credits -= entry.buyPrice
        entry.localStock -= 1
        // Local scarcity gently nudges ONLY this city's buy price upward
        entry.buyPrice = (entry.buyPrice * 1.02f).roundToInt().coerceAtLeast(entry.buyPrice + 1)

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
        if (entry.localStock <= 0 || credits < entry.buyPrice) return false

        credits -= entry.buyPrice
        entry.localStock -= 1
        ownedItems.add(EquipmentItem(instanceId = nextItemInstanceId++, type = itemId))
        requestSave()
        return true
    }

    /**
     * Crafts consumables, sector chips, or basic ship gear at a Capital City Crafting Station
     * using refined resources (consumes lowest qualifying refined tier first).
     */
    fun craftRecipe(recipe: CraftingRecipeId): Boolean {
        if (activeStationOverlay != StationKind.CRAFTING) return false
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

    fun engageAutopilotToNextRoutePortal(): Boolean {
        val nextId = nextRouteHopSectorId ?: return false
        val portal = activePortals.firstOrNull { it.targetSectorId == nextId } ?: return false
        setAutopilotTarget(portal.worldX, portal.worldY)
        isGalaxyMapOpen = false
        isTacticalMapOpen = false
        popups.add(
            FloatingPopup(
                x = player.x,
                y = player.y - activeShipClass.hullRadius - 36f,
                text = "AUTOPILOT → GATE $nextId",
                life = 1.4f,
                maxLife = 1.4f,
                colorRgb = 0x00E5FF,
                textSize = 14f
            )
        )
        return true
    }

    fun setAutopilotTarget(worldX: Float, worldY: Float) {
        if (!player.isAlive) return
        val maxBound = (activeMapSize() - 200f).coerceAtLeast(200f)
        autopilotTargetX = worldX.coerceIn(200f, maxBound)
        autopilotTargetY = worldY.coerceIn(200f, maxBound)
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

    fun selectOwnedFormationFromHud(formation: DroneFormation): Boolean {
        if (!player.isAlive || isPaused || isHangarOpen) return false
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
        if (isPaused || isHangarOpen || activeStationOverlay != null) return
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
            updateResourceNodes(dt)
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
            activeLocalMarket = if (activeStationOverlay == StationKind.LOCAL_MARKET) {
                isolatedLocalMarkets[currentSectorId]?.toSnapshot()
            } else null,
            rawCargo = rawCargo.filterValues { it > 0 }.toMap(),
            refinedCargo = refinedCargo.filterValues { it > 0 }.toMap(),
            overchargedAmmo = overchargedAmmo,
            miningNodeName = resourceNodes.firstOrNull { it.id == activeMiningNodeId }?.resourceType?.displayName,
            miningProgress = resourceNodes.firstOrNull { it.id == activeMiningNodeId }?.harvestProgress ?: 0f,
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

    private fun updateResourceNodes(dt: Float) {
        if (resourceNodes.isEmpty()) {
            activeMiningNodeId = null
            return
        }
        val mineRange = 122f + (activeShipClass.hullRadius - 20f)
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

        var harvested = false
        for (node in resourceNodes) {
            if (node == closest) {
                activeMiningNodeId = node.id
                val harvestSpeed = if (node.resourceType.isYellowCrystal) 0.78f else 0.96f
                node.harvestProgress = (node.harvestProgress + dt * harvestSpeed).coerceAtMost(1f)
                if (node.harvestProgress >= 1f) {
                    val res = node.resourceType
                    rawCargo[res] = (rawCargo[res] ?: 0) + node.yieldAmount
                    popups.add(
                        FloatingPopup(
                            x = node.x,
                            y = node.y - node.radius - 12f,
                            text = "+${node.yieldAmount} ${res.displayName} (Refine: ${res.refinesInSectorId})",
                            life = 1.65f,
                            maxLife = 1.65f,
                            colorRgb = (res.colorHex and 0xFFFFFF).toInt(),
                            textSize = 13.5f
                        )
                    )
                    spawnHitSparks(node.x, node.y, colorHex = res.colorHex, count = 10)
                    resourceNodes.remove(node)
                    spawnResourceNodeForActiveSector()
                    activeMiningNodeId = null
                    harvested = true
                    break
                }
            } else if (node.harvestProgress > 0f) {
                node.harvestProgress = max(0f, node.harvestProgress - dt * 0.6f)
            }
        }
        if (closest == null) {
            activeMiningNodeId = null
        }
        if (harvested) {
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
