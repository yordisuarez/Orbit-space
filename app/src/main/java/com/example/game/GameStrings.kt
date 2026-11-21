package com.example.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class GameLanguage(val code: String, val displayLabel: String) {
    EN("en", "English (EN)"),
    ES("es", "Español (ES)");

    companion object {
        fun fromCode(raw: String?): GameLanguage =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: EN
    }
}

val LocalGameLanguage = staticCompositionLocalOf { GameLanguage.EN }

data class GameText(
    // Top HUD
    val hudCredits: String,
    val hudKills: String,
    val hudLevelPrefix: String,
    val hudShield: String,
    val hudHp: String,
    val hudConfig1: String,
    val hudConfig2: String,
    val hudHangar: String,
    val hudGalaxy: String,
    val hudSkills: String,
    val hudSettings: String,
    val hudMenu: String,
    val hudPause: String,
    val hudSafeZone: String,
    val hudSafeZoneBlackMarket: String,
    val hudRadiationWarning: String,
    val hudRadiationNeedPrefix: String,
    val hudRoutePrefix: String,
    val hudMiningPrefix: String,
    val hudAutopilotBadge: String,
    val hudOverchargePrefix: String,
    val hudMissionPrefix: String,
    val hudMissionComplete: String,
    val hudMissionClaim: String,
    val hudAllMissionsComplete: String,
    val hudNeedLevelPrefix: String,

    // Bottom combat & navigation controls
    val ctrlMove: String,
    val ctrlTarget: String,
    val ctrlRocket: String,
    val ctrlReady: String,
    val ctrlReload: String,
    val ctrlFire: String,
    val ctrlLaserOn: String,
    val ctrlNoTarget: String,
    val ctrlInRange: String,
    val ctrlOutOfRange: String,
    val ctrlJump: String,
    val ctrlDock: String,
    val ctrlRefineryShort: String,
    val ctrlMarketShort: String,
    val ctrlBlackMarketShort: String,
    val ctrlCraftingShort: String,
    val canvasTapDockHint: String,

    // Pause / Ship Destroyed / Settings Modals
    val pausedTitle: String,
    val actionResume: String,
    val actionResetSave: String,
    val shipDestroyedTitle: String,
    val shipDestroyedSubtitle: String,
    val actionRelaunch: String,
    val settingsTitle: String,
    val settingsLanguageLabel: String,
    val settingsLanguageHint: String,
    val actionClose: String,
    val actionBack: String,

    // Unified HUD Menu Overlay
    val menuTitle: String,
    val menuSubtitle: String,
    val menuSkills: String,
    val menuSkillsHint: String,
    val menuHangar: String,
    val menuHangarDockedHint: String,
    val menuHangarSpaceHint: String,
    val menuHangarReadOnlyTag: String,
    val menuInventory: String,
    val menuInventoryHint: String,
    val menuInfo: String,
    val menuInfoHint: String,
    val menuSettings: String,
    val menuSettingsHint: String,

    // Hangar Read-Only Notice
    val hangarReadOnlyNotice: String,

    // Cargo & Inventory Overlay
    val inventoryTitle: String,
    val inventorySubtitle: String,
    val inventoryTabCargo: String,
    val inventoryTabModules: String,
    val inventoryCapacityPrefix: String,
    val inventoryRawSection: String,
    val inventoryRefinedSection: String,
    val inventoryConsumablesSection: String,
    val inventoryEmptyCargo: String,
    val inventoryEmptyModules: String,

    // Modular Info / Game Guide
    val guideTitle: String,
    val guideSubtitle: String,
    val guideTapToExpand: String,
    val guideTopicsSuffix: String,

    // Local Tactical Map & Autopilot
    val minimapTapToOpen: String,
    val localMapSectorPrefix: String,
    val sectorMapTitle: String,
    val sectorMapTapHint: String,
    val sectorMapEtaPrefix: String,
    val sectorMapDangerZone: String,
    val sectorMapSafeZone: String,
    val sectorMapCoordinates: String,
    val actionSetAutopilot: String,
    val actionCancel: String,
    val hudAutopilotActivePrefix: String,
    val autopilotDisengagedManual: String,
    val autopilotDisengagedArrivedPrefix: String,
    val autopilotDisengagedAttacked: String,
    val autopilotDisengagedShield: String,

    // Galactic Map
    val galaxyMapTitle: String,
    val galaxyCurrentPrefix: String,
    val galaxyLocalMap: String,
    val galaxyResetZoom: String,
    val galaxyLegendCities: String,
    val galaxyLegendOuter: String,
    val galaxyLegendInner: String,
    val galaxyLegendNucleus: String,
    val galaxyLegendShortcuts: String,
    val galaxyRouteWord: String,
    val galaxyJumpsWord: String,
    val galaxyNextPrefix: String,
    val galaxyFlyToGate: String,
    val galaxyYouBadge: String,
    val galaxyResourcesPrefix: String,
    val galaxyGatesPrefix: String,
    val galaxyPathWord: String,
    val galaxyPlotRoute: String,
    val galaxyPlotted: String,

    // Station Overlay (Refinery, Local Market, Crafting)
    val stationUndock: String,
    val refineryRatePrefix: String,
    val refineryRawOwned: String,
    val refineryRefinedOwned: String,
    val refineryNativePrefix: String,
    val refineryRefineOne: String,
    val refineryAll: String,
    val refineryMax: String,
    val refineryFeePrefix: String,
    val refineryFeeDiscountPrefix: String,
    val refineryBonusChancePrefix: String,
    val refineryEmptyCargo: String,
    val refineryDirectUseOnly: String,
    val refineryOtherCargoHeader: String,
    val refineryCannotRefineIn: String,
    val refineryHaulTo: String,
    val refineryRefineInBadge: String,
    val marketTabSell: String,
    val marketTabBuy: String,
    val marketTabLocalGear: String,
    val marketTabBlackGear: String,
    val marketEmptyCargo: String,
    val marketRawTag: String,
    val marketRefinedTag: String,
    val marketRefinesInPrefix: String,
    val marketCargoPrefix: String,
    val marketBidPrefix: String,
    val marketLocalStockPrefix: String,
    val marketSellOne: String,
    val marketBuyPrefix: String,
    val marketOut: String,
    val marketSoldOut: String,
    val marketExclusiveStockSuffix: String,
    val craftingAvailableRefined: String,
    val craftingAmmoStock: String,
    val craftingCostPrefix: String,
    val craftingAnyRefinedTierPrefix: String,
    val craftingButton: String,
    val craftingNeedRefinedHint: String,
    val stationTabRefinery: String,
    val stationTabCrafting: String,
    val stationTabMarket: String,
    val craftSubTabNaval: String,
    val craftSubTabGear: String,
    val craftSubTabSynthesis: String,
    val craftMaterialReturnPrefix: String,
    val craftDoubleChancePrefix: String,
    val craftQualityOddsPrefix: String,
    val craftBannerCraftedPrefix: String,
    val craftBannerDoubleBonus: String,
    val craftBannerRefundedSuffix: String,
    val craftBannerAddedHangar: String,
    val craftBannerAddedInventory: String,
    val craftHangarShipsOwnedPrefix: String,
    val craftInventoryItemsOwnedPrefix: String,

    // Pilot Skills Panel (9 Skills, Lv 1-99)
    val skillsTitle: String,
    val skillsSubtitle: String,
    val skillsTotalLevelPrefix: String,
    val skillsMaxLabel: String,

    // Step 6: City Basic Vendor, Permanent Capsule, Hangar Fleet & Death (60/40)
    val marketTabBasicVendor: String,
    val basicVendorBanner: String,
    val basicVendorOwnedPrefix: String,
    val basicVendorUnlimitedTag: String,
    val capsuleName: String,
    val capsulePermanentBadge: String,
    val capsuleCannotSellHint: String,
    val capsuleSlotsTitle: String,
    val capsuleSlotLaser: String,
    val capsuleSlotTool: String,
    val capsuleSlotToolDetail: String,
    val capsuleSlotShield: String,
    val hangarMyShipsTitle: String,
    val hangarPilotingBadge: String,
    val hangarSwitchShip: String,
    val hangarCraftedShipsEmpty: String,
    val vantDurabilityPrefix: String,
    val vantRepairAction: String,
    val deathCapsuleRespawnBtn: String,
    val deathShipLostPrefix: String,
    val deathCapsuleSafeNotice: String,
    val deathWreckSpawnedPrefix: String,
    val deathDestroyedRatioSuffix: String,
    val deathVantsDamagedPrefix: String,
    val deathRespawnCityPrefix: String,
    val wreckLootRecoveredPopup: String
)

object GameStrings {
    private val EN_TEXT = GameText(
        hudCredits = "CREDITS",
        hudKills = "KILLS",
        hudLevelPrefix = "LV",
        hudShield = "SHD",
        hudHp = "HP",
        hudConfig1 = "CFG 1",
        hudConfig2 = "CFG 2",
        hudHangar = "HANGAR",
        hudGalaxy = "GALAXY",
        hudSkills = "SKILLS",
        hudSettings = "Settings",
        hudMenu = "MENU",
        hudPause = "Pause",
        hudSafeZone = "SAFE ZONE",
        hudSafeZoneBlackMarket = "SAFE ZONE • BLACK MKT",
        hudRadiationWarning = "RADIATION ZONE",
        hudRadiationNeedPrefix = "RADIATION • NEED",
        hudRoutePrefix = "ROUTE →",
        hudMiningPrefix = "MINING",
        hudAutopilotBadge = "AUTOPILOT • TAP CANCEL",
        hudOverchargePrefix = "PLASMA",
        hudMissionPrefix = "MISSION:",
        hudMissionComplete = "COMPLETED",
        hudMissionClaim = "CLAIM",
        hudAllMissionsComplete = "ALL SECTOR MISSIONS COMPLETED",
        hudNeedLevelPrefix = "LVL",

        ctrlMove = "MOVE",
        ctrlTarget = "TARGET",
        ctrlRocket = "ROCKET",
        ctrlReady = "READY",
        ctrlReload = "RELOAD",
        ctrlFire = "FIRE",
        ctrlLaserOn = "LASER ON",
        ctrlNoTarget = "NO TARGET",
        ctrlInRange = "IN RANGE",
        ctrlOutOfRange = "OUT OF RANGE",
        ctrlJump = "JUMP",
        ctrlDock = "DOCK",
        ctrlRefineryShort = "REFINERY",
        ctrlMarketShort = "MARKET",
        ctrlBlackMarketShort = "BLK MKT",
        ctrlCraftingShort = "CRAFTING",
        canvasTapDockHint = "TAP DOCK TO INTERACT",

        pausedTitle = "GAME PAUSED",
        actionResume = "RESUME",
        actionResetSave = "RESET SAVE",
        shipDestroyedTitle = "SHIP DESTROYED",
        shipDestroyedSubtitle = "Hull Integrity Critical",
        actionRelaunch = "RELAUNCH",
        settingsTitle = "SETTINGS",
        settingsLanguageLabel = "LANGUAGE",
        settingsLanguageHint = "Updates HUD, menus, Galactic Map, and station interfaces immediately.",
        actionClose = "CLOSE",
        actionBack = "BACK",

        menuTitle = "COMMAND MENU",
        menuSubtitle = "Pilot Systems & Tactical Guide",
        menuSkills = "Skills",
        menuSkillsHint = "9 pilot professions (Lv 1–99)",
        menuHangar = "Hangar",
        menuHangarDockedHint = "Ships, loadouts, VANTs & missions",
        menuHangarSpaceHint = "Read-only in space • Dock to swap ships",
        menuHangarReadOnlyTag = "READ-ONLY",
        menuInventory = "Inventory",
        menuInventoryHint = "Cargo hold, resources & stored gear",
        menuInfo = "Info",
        menuInfoHint = "Mechanics, refining & economy guide",
        menuSettings = "Settings",
        menuSettingsHint = "Language (EN / ES) & preferences",

        hangarReadOnlyNotice = "IN SPACE (READ-ONLY) • Dock in a Safe Zone to swap or purchase ships",

        inventoryTitle = "INVENTORY",
        inventorySubtitle = "Ship Cargo Hold & Stored Modules",
        inventoryTabCargo = "CARGO HOLD",
        inventoryTabModules = "MODULES",
        inventoryCapacityPrefix = "CARGO",
        inventoryRawSection = "RAW RESOURCES",
        inventoryRefinedSection = "REFINED MATERIALS",
        inventoryConsumablesSection = "CONSUMABLES",
        inventoryEmptyCargo = "Your cargo hold is empty. Mine resource nodes or salvage alien wrecks in outer sectors.",
        inventoryEmptyModules = "No unequipped modules in storage. Equipped gear can be managed in the Hangar.",

        guideTitle = "INFO & GAME GUIDE",
        guideSubtitle = "Orbi Space Core Mechanics Reference",
        guideTapToExpand = "Tap any topic to expand details",
        guideTopicsSuffix = "topics",

        minimapTapToOpen = "TAP TO OPEN",
        localMapSectorPrefix = "SECTOR",
        sectorMapTitle = "SECTOR MAP",
        sectorMapTapHint = "Tap any coordinates, gate, station, or zone to set course",
        sectorMapEtaPrefix = "ETA",
        sectorMapDangerZone = "Danger Zone",
        sectorMapSafeZone = "Safe Zone",
        sectorMapCoordinates = "Coordinates",
        actionSetAutopilot = "SET AUTOPILOT",
        actionCancel = "CANCEL",
        hudAutopilotActivePrefix = "Autopilot Active → Going to",
        autopilotDisengagedManual = "Autopilot off: Manual control",
        autopilotDisengagedArrivedPrefix = "Arrived at",
        autopilotDisengagedAttacked = "Autopilot off: Under attack!",
        autopilotDisengagedShield = "Autopilot off: Shield compromised!",

        galaxyMapTitle = "ORBI SPACE • GALACTIC MAP",
        galaxyCurrentPrefix = "CURRENT:",
        galaxyLocalMap = "LOCAL MAP",
        galaxyResetZoom = "RESET",
        galaxyLegendCities = "C1 Blue • C2 Green • C3 Red (Safe Cities)",
        galaxyLegendOuter = "Outer Ring R1–R3 (Tier 1–2)",
        galaxyLegendInner = "Inner Ring 1-2/2-2/3-2 (Tier 3–4)",
        galaxyLegendNucleus = "Yellow Ring & 0-0 Nucleus (Tier 4–5)",
        galaxyLegendShortcuts = "Dashed Links = Border Shortcuts",
        galaxyRouteWord = "ROUTE",
        galaxyJumpsWord = "JUMPS",
        galaxyNextPrefix = "NEXT:",
        galaxyFlyToGate = "FLY TO GATE",
        galaxyYouBadge = "YOU",
        galaxyResourcesPrefix = "Resources:",
        galaxyGatesPrefix = "Gates:",
        galaxyPathWord = "Path",
        galaxyPlotRoute = "PLOT ROUTE",
        galaxyPlotted = "PLOTTED",

        stationUndock = "UNDOCK",
        refineryRatePrefix = "RATE",
        refineryRawOwned = "Raw",
        refineryRefinedOwned = "Refined",
        refineryNativePrefix = "Native",
        refineryRefineOne = "REFINE 1",
        refineryAll = "ALL",
        refineryMax = "MAX",
        refineryFeePrefix = "Fee",
        refineryFeeDiscountPrefix = "Fee",
        refineryBonusChancePrefix = "Bonus",
        refineryEmptyCargo = "No raw resources in cargo. Mine nodes or salvage alien wrecks to refine them here.",
        refineryDirectUseOnly = "Direct use (Nucleus Boosters)",
        refineryOtherCargoHeader = "CARGO REQUIRING ANOTHER REFINERY",
        refineryCannotRefineIn = "Cannot refine in",
        refineryHaulTo = "haul to",
        refineryRefineInBadge = "REFINE IN",
        marketTabSell = "SELL CARGO",
        marketTabBuy = "BUY RESOURCES",
        marketTabLocalGear = "LOCAL GEAR",
        marketTabBlackGear = "BLACK MARKET GEAR",
        marketEmptyCargo = "Your cargo hold is empty. Mine resource nodes outside Capital Cities, refine them along the clockwise city loop, and sell here at local prices.",
        marketRawTag = "Raw",
        marketRefinedTag = "Refined",
        marketRefinesInPrefix = "Refines in",
        marketCargoPrefix = "Cargo",
        marketBidPrefix = "Bid",
        marketLocalStockPrefix = "Stock",
        marketSellOne = "SELL 1",
        marketBuyPrefix = "BUY",
        marketOut = "OUT",
        marketSoldOut = "SOLD OUT",
        marketExclusiveStockSuffix = "remaining",
        craftingAvailableRefined = "Refined Available",
        craftingAmmoStock = "Plasma Ammo",
        craftingCostPrefix = "Cost",
        craftingAnyRefinedTierPrefix = "Refined T",
        craftingButton = "CRAFT",
        craftingNeedRefinedHint = "Refine raw ores at the city Refinery first to craft gear & consumables.",
        stationTabRefinery = "REFINERY",
        stationTabCrafting = "CRAFTING",
        stationTabMarket = "MARKET",
        craftSubTabNaval = "Naval Eng.",
        craftSubTabGear = "Gear Mfg.",
        craftSubTabSynthesis = "Synthesis",
        craftMaterialReturnPrefix = "Refund",
        craftDoubleChancePrefix = "2× Chance",
        craftQualityOddsPrefix = "Quality",
        craftBannerCraftedPrefix = "CRAFTED",
        craftBannerDoubleBonus = "2× BONUS!",
        craftBannerRefundedSuffix = "mat. refunded",
        craftBannerAddedHangar = "Added to Hangar",
        craftBannerAddedInventory = "Added to Inventory",
        craftHangarShipsOwnedPrefix = "Hangar Ships",
        craftInventoryItemsOwnedPrefix = "Crafted Gear/Items",

        skillsTitle = "PILOT SKILLS",
        skillsSubtitle = "Capsule Profile • 9 Professions (Lv 1–99)",
        skillsTotalLevelPrefix = "TOTAL LV",
        skillsMaxLabel = "MAX",

        marketTabBasicVendor = "BASIC VENDOR",
        basicVendorBanner = "Tier 1 starter essentials only • Ships, T2+ gear, VANTs & boosters are player-crafted",
        basicVendorOwnedPrefix = "Owned",
        basicVendorUnlimitedTag = "NPC • T1",
        capsuleName = "Pilot Capsule",
        capsulePermanentBadge = "PERMANENT",
        capsuleCannotSellHint = "Always available • Cannot be sold or destroyed",
        capsuleSlotsTitle = "PERMANENT CAPSULE SLOTS (NEVER LOST)",
        capsuleSlotLaser = "Basic Laser",
        capsuleSlotTool = "Basic Tool",
        capsuleSlotToolDetail = "Mining / Salvaging",
        capsuleSlotShield = "Basic Extra / Shield",
        hangarMyShipsTitle = "MY SHIPS (DOCKED SWITCH)",
        hangarPilotingBadge = "PILOTING",
        hangarSwitchShip = "BOARD",
        hangarCraftedShipsEmpty = "Craft Industrial ships at a city Crafting Station.",
        vantDurabilityPrefix = "Durability",
        vantRepairAction = "REPAIR",
        deathCapsuleRespawnBtn = "RESPAWN IN HOME CITY (CAPSULE)",
        deathShipLostPrefix = "Ship permanently destroyed:",
        deathCapsuleSafeNotice = "Capsule & 3 permanent slots intact — never lost.",
        deathWreckSpawnedPrefix = "60% loot wreck at",
        deathDestroyedRatioSuffix = "40% destroyed",
        deathVantsDamagedPrefix = "VANTs returned to Home City (-25% durability):",
        deathRespawnCityPrefix = "Home City:",
        wreckLootRecoveredPopup = "WRECK LOOT RECOVERED"
    )

    private val ES_TEXT = GameText(
        hudCredits = "CRÉDITOS",
        hudKills = "BAJAS",
        hudLevelPrefix = "NV",
        hudShield = "ESC",
        hudHp = "CASCO",
        hudConfig1 = "CFG 1",
        hudConfig2 = "CFG 2",
        hudHangar = "HANGAR",
        hudGalaxy = "GALAXIA",
        hudSkills = "HAB.",
        hudSettings = "Ajustes",
        hudMenu = "MENÚ",
        hudPause = "Pausa",
        hudSafeZone = "ZONA SEGURA",
        hudSafeZoneBlackMarket = "ZONA SEGURA • M. NEGRO",
        hudRadiationWarning = "ZONA DE RADIACIÓN",
        hudRadiationNeedPrefix = "RADIACIÓN • REQ.",
        hudRoutePrefix = "RUTA →",
        hudMiningPrefix = "MINANDO",
        hudAutopilotBadge = "PILOTO AUTO • CANCELAR",
        hudOverchargePrefix = "PLASMA",
        hudMissionPrefix = "MISIÓN:",
        hudMissionComplete = "COMPLETADA",
        hudMissionClaim = "COBRAR",
        hudAllMissionsComplete = "TODAS LAS MISIONES COMPLETADAS",
        hudNeedLevelPrefix = "NV",

        ctrlMove = "MOVER",
        ctrlTarget = "OBJETIVO",
        ctrlRocket = "MISIL",
        ctrlReady = "LISTO",
        ctrlReload = "RECARGA",
        ctrlFire = "FUEGO",
        ctrlLaserOn = "LÁSER ACT.",
        ctrlNoTarget = "SIN OBJETIVO",
        ctrlInRange = "EN RANGO",
        ctrlOutOfRange = "FUERA DE RANGO",
        ctrlJump = "SALTO",
        ctrlDock = "ATRACAR",
        ctrlRefineryShort = "REFINERÍA",
        ctrlMarketShort = "MERCADO",
        ctrlBlackMarketShort = "M. NEGRO",
        ctrlCraftingShort = "TALLER",
        canvasTapDockHint = "TOCA ATRACAR PARA ENTRAR",

        pausedTitle = "JUEGO EN PAUSA",
        actionResume = "CONTINUAR",
        actionResetSave = "REINICIAR PARTIDA",
        shipDestroyedTitle = "NAVE DESTRUIDA",
        shipDestroyedSubtitle = "Integridad del casco crítica",
        actionRelaunch = "RELANZAR",
        settingsTitle = "AJUSTES",
        settingsLanguageLabel = "IDIOMA",
        settingsLanguageHint = "Actualiza el HUD, los menús, el Mapa Galáctico y las estaciones al instante.",
        actionClose = "CERRAR",
        actionBack = "ATRÁS",

        menuTitle = "MENÚ DE MANDO",
        menuSubtitle = "Sistemas de Piloto y Guía Táctica",
        menuSkills = "Habilidades",
        menuSkillsHint = "9 profesiones de piloto (Nv 1–99)",
        menuHangar = "Hangar",
        menuHangarDockedHint = "Naves, equipo, VANTs y misiones",
        menuHangarSpaceHint = "Solo lectura en espacio • Atraca para cambiar nave",
        menuHangarReadOnlyTag = "SOLO LECTURA",
        menuInventory = "Inventario",
        menuInventoryHint = "Bodega de carga, recursos y módulos",
        menuInfo = "Información",
        menuInfoHint = "Guía de mecánicas, refinado y economía",
        menuSettings = "Configuración",
        menuSettingsHint = "Idioma (EN / ES) y preferencias",

        hangarReadOnlyNotice = "EN EL ESPACIO (SOLO LECTURA) • Atraca en Zona Segura para cambiar o comprar naves",

        inventoryTitle = "INVENTARIO",
        inventorySubtitle = "Bodega de la Nave y Módulos Guardados",
        inventoryTabCargo = "BODEGA",
        inventoryTabModules = "MÓDULOS",
        inventoryCapacityPrefix = "CARGA",
        inventoryRawSection = "RECURSOS BRUTOS",
        inventoryRefinedSection = "MATERIALES REFINADOS",
        inventoryConsumablesSection = "CONSUMIBLES",
        inventoryEmptyCargo = "Tu bodega está vacía. Mina nodos de recursos o chatarrea pecios alienígenas en los sectores exteriores.",
        inventoryEmptyModules = "Sin módulos sueltos en almacén. El equipo instalado se gestiona desde el Hangar.",

        guideTitle = "INFORMACIÓN Y GUÍA",
        guideSubtitle = "Referencia de Mecánicas de Orbi Space",
        guideTapToExpand = "Toca cualquier tema para expandir detalles",
        guideTopicsSuffix = "temas",

        minimapTapToOpen = "TOCAR PARA ABRIR",
        localMapSectorPrefix = "SECTOR",
        sectorMapTitle = "MAPA DEL SECTOR",
        sectorMapTapHint = "Toca coordenadas, portal, estación o zona para fijar rumbo",
        sectorMapEtaPrefix = "ETA",
        sectorMapDangerZone = "Zona Peligrosa",
        sectorMapSafeZone = "Zona Segura",
        sectorMapCoordinates = "Coordenadas",
        actionSetAutopilot = "PILOTO AUTOMÁTICO",
        actionCancel = "CANCELAR",
        hudAutopilotActivePrefix = "Piloto Automático Activo → Rumbo a",
        autopilotDisengagedManual = "Piloto automático desactivado: Control manual",
        autopilotDisengagedArrivedPrefix = "Llegaste a",
        autopilotDisengagedAttacked = "Piloto automático desactivado: ¡Bajo ataque!",
        autopilotDisengagedShield = "Piloto automático desactivado: ¡Escudo comprometido!",

        galaxyMapTitle = "ORBI SPACE • MAPA GALÁCTICO",
        galaxyCurrentPrefix = "ACTUAL:",
        galaxyLocalMap = "MAPA LOCAL",
        galaxyResetZoom = "CENTRAR",
        galaxyLegendCities = "C1 Azul • C2 Verde • C3 Roja (Ciudades Seguras)",
        galaxyLegendOuter = "Anillo Exterior R1–R3 (Nivel 1–2)",
        galaxyLegendInner = "Anillo Interior 1-2/2-2/3-2 (Nivel 3–4)",
        galaxyLegendNucleus = "Anillo Amarillo y Núcleo 0-0 (Nivel 4–5)",
        galaxyLegendShortcuts = "Líneas Punteadas = Atajos Fronterizos",
        galaxyRouteWord = "RUTA",
        galaxyJumpsWord = "SALTOS",
        galaxyNextPrefix = "SIG.:",
        galaxyFlyToGate = "VOLAR AL PORTAL",
        galaxyYouBadge = "TÚ",
        galaxyResourcesPrefix = "Recursos:",
        galaxyGatesPrefix = "Portales:",
        galaxyPathWord = "Ruta",
        galaxyPlotRoute = "TRAZAR RUTA",
        galaxyPlotted = "TRAZADA",

        stationUndock = "DESATRACAR",
        refineryRatePrefix = "TASA",
        refineryRawOwned = "Bruto",
        refineryRefinedOwned = "Refinado",
        refineryNativePrefix = "Origen",
        refineryRefineOne = "REFINAR 1",
        refineryAll = "TODO",
        refineryMax = "MÁX",
        refineryFeePrefix = "Tarifa",
        refineryFeeDiscountPrefix = "Tarifa",
        refineryBonusChancePrefix = "Extra",
        refineryEmptyCargo = "Sin recursos brutos en bodega. Mina nodos o chatarrea pecios alienígenas para refinarlos aquí.",
        refineryDirectUseOnly = "Uso directo (Potenciadores Núcleo)",
        refineryOtherCargoHeader = "CARGA QUE REQUIERE OTRA REFINERÍA",
        refineryCannotRefineIn = "No se refina en",
        refineryHaulTo = "llévalo a",
        refineryRefineInBadge = "REFINAR EN",
        marketTabSell = "VENDER CARGA",
        marketTabBuy = "COMPRAR RECURSOS",
        marketTabLocalGear = "EQUIPO LOCAL",
        marketTabBlackGear = "EQUIPO MERC. NEGRO",
        marketEmptyCargo = "Tu bodega está vacía. Mina nodos de recursos fuera de las Ciudades Capitales, refínalos en el ciclo horario y véndelos aquí a precios locales.",
        marketRawTag = "Bruto",
        marketRefinedTag = "Refinado",
        marketRefinesInPrefix = "Se refina en",
        marketCargoPrefix = "Bodega",
        marketBidPrefix = "Compra",
        marketLocalStockPrefix = "Stock",
        marketSellOne = "VENDER 1",
        marketBuyPrefix = "COMPRAR",
        marketOut = "AGOT.",
        marketSoldOut = "AGOTADO",
        marketExclusiveStockSuffix = "disponibles",
        craftingAvailableRefined = "Refinados disponibles",
        craftingAmmoStock = "Munición Plasma",
        craftingCostPrefix = "Coste",
        craftingAnyRefinedTierPrefix = "Refinado Nv.",
        craftingButton = "FABRICAR",
        craftingNeedRefinedHint = "Refina minerales en la Refinería de la ciudad para fabricar equipo y consumibles.",
        stationTabRefinery = "REFINERÍA",
        stationTabCrafting = "FABRICACIÓN",
        stationTabMarket = "MERCADO",
        craftSubTabNaval = "Ing. Naval",
        craftSubTabGear = "Fab. Equipo",
        craftSubTabSynthesis = "Síntesis",
        craftMaterialReturnPrefix = "Reembolso",
        craftDoubleChancePrefix = "Prob. 2×",
        craftQualityOddsPrefix = "Calidad",
        craftBannerCraftedPrefix = "FABRICADO",
        craftBannerDoubleBonus = "¡BONO 2×!",
        craftBannerRefundedSuffix = "mat. devueltos",
        craftBannerAddedHangar = "Enviada al Hangar",
        craftBannerAddedInventory = "Enviado al Inventario",
        craftHangarShipsOwnedPrefix = "Naves en Hangar",
        craftInventoryItemsOwnedPrefix = "Equipo/Objetos",

        skillsTitle = "HABILIDADES",
        skillsSubtitle = "Perfil de Cápsula • 9 Profesiones (Nv 1–99)",
        skillsTotalLevelPrefix = "NV TOTAL",
        skillsMaxLabel = "MÁX",

        marketTabBasicVendor = "VENDEDOR BÁSICO",
        basicVendorBanner = "Solo equipo inicial Nivel 1 • Naves, equipo N2+, VANTs y potenciadores son fabricados",
        basicVendorOwnedPrefix = "Tienes",
        basicVendorUnlimitedTag = "PNJ • N1",
        capsuleName = "Cápsula de Piloto",
        capsulePermanentBadge = "PERMANENTE",
        capsuleCannotSellHint = "Siempre disponible • No se vende ni se destruye",
        capsuleSlotsTitle = "RANURAS PERMANENTES DE CÁPSULA (NUNCA SE PIERDEN)",
        capsuleSlotLaser = "Láser Básico",
        capsuleSlotTool = "Herram. Básica",
        capsuleSlotToolDetail = "Minería / Chatarreo",
        capsuleSlotShield = "Extra / Escudo Básico",
        hangarMyShipsTitle = "MIS NAVES (CAMBIO EN CIUDAD)",
        hangarPilotingBadge = "PILOTANDO",
        hangarSwitchShip = "ABORDAR",
        hangarCraftedShipsEmpty = "Fabrica naves Industriales en la Estación de Fabricación.",
        vantDurabilityPrefix = "Durabilidad",
        vantRepairAction = "REPARAR",
        deathCapsuleRespawnBtn = "REAPARECER EN CIUDAD NATAL (CÁPSULA)",
        deathShipLostPrefix = "Nave destruida permanentemente:",
        deathCapsuleSafeNotice = "Cápsula y sus 3 ranuras permanentes intactas — nunca se pierden.",
        deathWreckSpawnedPrefix = "Pecio con 60% botín en",
        deathDestroyedRatioSuffix = "40% destruido",
        deathVantsDamagedPrefix = "VANTs devueltos a Ciudad Natal (-25% durabilidad):",
        deathRespawnCityPrefix = "Ciudad Natal:",
        wreckLootRecoveredPopup = "BOTÍN DEL PECIO RECUPERADO"
    )

    fun of(lang: GameLanguage): GameText = when (lang) {
        GameLanguage.EN -> EN_TEXT
        GameLanguage.ES -> ES_TEXT
    }

    @Composable
    @ReadOnlyComposable
    fun current(): GameText = of(LocalGameLanguage.current)

    fun zoneShortLabel(zone: GalaxyZoneType, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> zone.shortLabel
        GameLanguage.ES -> when (zone) {
            GalaxyZoneType.CAPITAL_CITY -> "CIUDAD SEGURA"
            GalaxyZoneType.SAFE_ZONE -> "ZONA SEGURA"
            GalaxyZoneType.INTERMEDIATE_ZONE -> "ANILLO EXT."
            GalaxyZoneType.MODERATE_ZONE -> "MODERADA"
            GalaxyZoneType.HIGH_RISK_YELLOW_RING -> "ALTO RIESGO"
            GalaxyZoneType.NUCLEUS -> "NÚCLEO 0-0"
        }
    }

    fun safeZoneStatus(sector: SectorNode, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> sector.safeZoneStatusLabel
        GameLanguage.ES -> when (sector.zoneType) {
            GalaxyZoneType.CAPITAL_CITY -> "Ciudad Segura • Base, Mercado, Taller y Refinería"
            GalaxyZoneType.SAFE_ZONE -> "Puesto de Zona Segura • Sin pérdida total"
            GalaxyZoneType.INTERMEDIATE_ZONE -> "Zona Intermedia • Sin pérdida total"
            GalaxyZoneType.MODERATE_ZONE -> "Zona Moderada • Riesgo de combate PvP"
            GalaxyZoneType.HIGH_RISK_YELLOW_RING -> "Anillo Amarillo • Botín Total • Cristal Amarillo"
            GalaxyZoneType.NUCLEUS -> "Núcleo Central • Zona Segura del Mercado Negro"
        }
    }

    fun stationCanvasName(station: SectorStation, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> station.name
        GameLanguage.ES -> when (station.kind) {
            StationKind.REFINERY -> "REFINERÍA ${station.sectorId}"
            StationKind.LOCAL_MARKET -> if (station.sectorId == "0-0") "MERCADO NEGRO" else "MERCADO ${station.sectorId}"
            StationKind.CRAFTING -> "TALLER ${station.sectorId}"
        }
    }

    fun stationHeaderTitle(kind: StationKind, sector: SectorNode, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when {
            kind == StationKind.REFINERY -> "${sector.id} REFINING STATION"
            kind == StationKind.CRAFTING -> "${sector.id} CRAFTING STATION"
            sector.isNucleus -> "0-0 NUCLEUS BLACK MARKET"
            else -> "${sector.id} ISOLATED LOCAL MARKET"
        }
        GameLanguage.ES -> when {
            kind == StationKind.REFINERY -> "ESTACIÓN DE REFINADO ${sector.id}"
            kind == StationKind.CRAFTING -> "ESTACIÓN DE FABRICACIÓN ${sector.id}"
            sector.isNucleus -> "MERCADO NEGRO DEL NÚCLEO 0-0"
            else -> "MERCADO LOCAL AISLADO ${sector.id}"
        }
    }

    fun stationHeaderSubtitle(kind: StationKind, sector: SectorNode, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (kind) {
            StationKind.REFINERY -> "Clockwise Loop: C1→C2 • C2→C3 • C3→C1 • Crystal & Salvage→C1/C2/C3"
            StationKind.CRAFTING -> "Assemble Consumables, Chips & Ship Modules Using Refined Resources"
            StationKind.LOCAL_MARKET -> "Independent Local Economy (${sector.id} Only — Not Linked to Other Cities)"
        }
        GameLanguage.ES -> when (kind) {
            StationKind.REFINERY -> "Ciclo Horario: C1→C2 • C2→C3 • C3→C1 • Cristal y Chatarra→C1/C2/C3"
            StationKind.CRAFTING -> "Fabrica Consumibles, Chips y Módulos con Recursos Refinados"
            StationKind.LOCAL_MARKET -> "Economía Local Independiente (Solo ${sector.id} — No vinculada a otras ciudades)"
        }
    }

    fun refineryAcceptedSummary(sectorId: String, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (sectorId) {
            "C1" -> "Refines C3 Raw + Yellow Crystal & Xeno Salvage"
            "C2" -> "Refines C1 Raw + Yellow Crystal & Xeno Salvage"
            "C3" -> "Refines C2 Raw + Yellow Crystal & Xeno Salvage"
            "0-0" -> "Nucleus Black Market Hub (Refine in C1, C2, or C3)"
            else -> "Local Refining Bay"
        }
        GameLanguage.ES -> when (sectorId) {
            "C1" -> "Refina Bruto C3 + Cristal Amarillo y Chatarra Xeno"
            "C2" -> "Refina Bruto C1 + Cristal Amarillo y Chatarra Xeno"
            "C3" -> "Refina Bruto C2 + Cristal Amarillo y Chatarra Xeno"
            "0-0" -> "Núcleo Mercado Negro (Refinar en C1, C2 o C3)"
            else -> "Bahía de Refinado Local"
        }
    }

    fun craftingRecipeTitle(recipe: CraftingRecipeId, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> recipe.displayName
        GameLanguage.ES -> when (recipe) {
            CraftingRecipeId.HULL_REPAIR_KIT -> "Kit de Reparación Nanite"
            CraftingRecipeId.PLASMA_AMMO_PACK -> "Munición de Plasma (+25)"
            CraftingRecipeId.SHIELD_CHIP_LV1 -> "Chip de Sector Nv. 1"
            CraftingRecipeId.SHIELD_CHIP_LV2 -> "Chip de Sector Nv. 2"
            CraftingRecipeId.LASER_LF1 -> "Cañón Láser LF-1"
            CraftingRecipeId.SHIELD_SG1 -> "Generador de Escudo SG-1"
        }
    }

    fun craftingRecipeDesc(recipe: CraftingRecipeId, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> recipe.effectSummary
        GameLanguage.ES -> when (recipe) {
            CraftingRecipeId.HULL_REPAIR_KIT -> "Restaura 100% Casco y Escudo al instante"
            CraftingRecipeId.PLASMA_AMMO_PACK -> "+25 disparos sobrecargados (+35% daño láser)"
            CraftingRecipeId.SHIELD_CHIP_LV1 -> "Añade 1× Chip Nv. 1 al inventario del Hangar"
            CraftingRecipeId.SHIELD_CHIP_LV2 -> "Añade 1× Chip Nv. 2 (protección Zona Moderada)"
            CraftingRecipeId.LASER_LF1 -> "Añade 1× Láser LF-1 (+12 DAÑO) al Hangar"
            CraftingRecipeId.SHIELD_SG1 -> "Añade 1× Escudo SG-1 (+80 ESC) al Hangar"
        }
    }

    fun missionDescription(spec: MissionSpec, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> spec.description
        GameLanguage.ES -> when (spec.goalType) {
            MissionGoalType.COLLECT_BONUS_BOXES -> "Recoge ${spec.targetCount} Cajas Bonus"
            MissionGoalType.DESTROY_SCOUTS -> "Destruye ${spec.targetCount} Exploradores"
            MissionGoalType.DESTROY_HUNTERS -> "Destruye ${spec.targetCount} Cazadores"
            MissionGoalType.DESTROY_DESTROYERS -> "Destruye ${spec.targetCount} Destructores"
        }
    }

    // -------------------------------------------------------------------------
    // v1.10 Ship Roles, Industrial Types, Quality, Pilot Skills & Resources
    // -------------------------------------------------------------------------

    fun shipRoleName(role: ShipRole, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (role) {
            ShipRole.CAPSULE -> "Capsule"
            ShipRole.INDUSTRIAL -> "Industrial"
            ShipRole.COMBAT -> "Combat"
        }
        GameLanguage.ES -> when (role) {
            ShipRole.CAPSULE -> "Cápsula"
            ShipRole.INDUSTRIAL -> "Industrial"
            ShipRole.COMBAT -> "Combate"
        }
    }

    fun industrialTypeName(type: IndustrialType, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (type) {
            IndustrialType.MINER -> "Miner"
            IndustrialType.PLASMA_COLLECTOR -> "Plasma Collector"
            IndustrialType.MEDIUM_HAULER -> "Medium Hauler"
            IndustrialType.HEAVY_HAULER -> "Heavy Hauler"
        }
        GameLanguage.ES -> when (type) {
            IndustrialType.MINER -> "Minero"
            IndustrialType.PLASMA_COLLECTOR -> "Recolector de Plasma"
            IndustrialType.MEDIUM_HAULER -> "Carguero Mediano"
            IndustrialType.HEAVY_HAULER -> "Carguero Pesado"
        }
    }

    fun craftQualityName(quality: CraftQuality, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (quality) {
            CraftQuality.STANDARD -> "Standard"
            CraftQuality.REINFORCED -> "Reinforced"
            CraftQuality.EXCELLENT -> "Excellent"
            CraftQuality.MASTERWORK -> "Masterwork"
        }
        GameLanguage.ES -> when (quality) {
            CraftQuality.STANDARD -> "Estándar"
            CraftQuality.REINFORCED -> "Reforzado"
            CraftQuality.EXCELLENT -> "Excelente"
            CraftQuality.MASTERWORK -> "Obra Maestra"
        }
    }

    fun skillName(skill: SkillType, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (skill) {
            SkillType.MINING -> "Mining"
            SkillType.SALVAGING -> "Salvaging"
            SkillType.REFINING -> "Refining"
            SkillType.NAVAL_ENGINEERING -> "Naval Engineering"
            SkillType.GEAR_MANUFACTURING -> "Gear Manufacturing"
            SkillType.SYNTHESIS -> "Synthesis"
            SkillType.WEAPONRY -> "Weaponry"
            SkillType.SHIELDS -> "Shields"
            SkillType.PILOTING -> "Piloting"
        }
        GameLanguage.ES -> when (skill) {
            SkillType.MINING -> "Minería"
            SkillType.SALVAGING -> "Chatarreo"
            SkillType.REFINING -> "Refinado"
            SkillType.NAVAL_ENGINEERING -> "Ingeniería Naval"
            SkillType.GEAR_MANUFACTURING -> "Fabricación de Equipo"
            SkillType.SYNTHESIS -> "Síntesis"
            SkillType.WEAPONRY -> "Armamento"
            SkillType.SHIELDS -> "Escudos"
            SkillType.PILOTING -> "Pilotaje"
        }
    }

    fun resourceCategoryName(category: ResourceCategory, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (category) {
            ResourceCategory.SOLID -> "Solid"
            ResourceCategory.PLASMA -> "Plasma"
            ResourceCategory.SALVAGE -> "Salvage"
        }
        GameLanguage.ES -> when (category) {
            ResourceCategory.SOLID -> "Sólido"
            ResourceCategory.PLASMA -> "Plasma"
            ResourceCategory.SALVAGE -> "Chatarra"
        }
    }

    fun resourceName(resource: ResourceType, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (resource) {
            ResourceType.STAR_COBALT -> "Star Cobalt"
            ResourceType.AETHER_QUARTZ -> "Aether Quartz"
            ResourceType.VERIDIAN_ORE -> "Veridian Ore"
            ResourceType.BIO_RESIN -> "Bio Resin"
            ResourceType.CRIMSON_IRON -> "Crimson Iron"
            ResourceType.MAGMA_STONE -> "Magma Stone"
            ResourceType.YELLOW_CRYSTAL -> "Yellow Crystal"
            ResourceType.XENO_SALVAGE -> "Xeno Salvage"
            ResourceType.XENO_CORE -> "Xeno Core"
        }
        GameLanguage.ES -> when (resource) {
            ResourceType.STAR_COBALT -> "Cobalto Estelar"
            ResourceType.AETHER_QUARTZ -> "Cuarzo Éter"
            ResourceType.VERIDIAN_ORE -> "Mena Veridiana"
            ResourceType.BIO_RESIN -> "Biorresina"
            ResourceType.CRIMSON_IRON -> "Hierro Carmesí"
            ResourceType.MAGMA_STONE -> "Piedra de Magma"
            ResourceType.YELLOW_CRYSTAL -> "Cristal Amarillo"
            ResourceType.XENO_SALVAGE -> "Chatarra Xeno"
            ResourceType.XENO_CORE -> "Núcleo Xeno"
        }
    }

    fun refinedResourceName(refined: RefinedResourceType, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (refined) {
            RefinedResourceType.COBALT_ALLOY -> "Cobalt Alloy"
            RefinedResourceType.AETHER_CRYSTAL -> "Aether Crystal"
            RefinedResourceType.VERIDIAN_INGOT -> "Veridian Ingot"
            RefinedResourceType.BIO_POLYMER -> "Bio Polymer"
            RefinedResourceType.CRIMSON_STEEL -> "Crimson Steel"
            RefinedResourceType.THERMAL_CORE -> "Thermal Core"
            RefinedResourceType.REFINED_YELLOW_CRYSTAL -> "Refined Yellow Crystal"
            RefinedResourceType.XENO_ALLOY -> "Xeno Alloy"
        }
        GameLanguage.ES -> when (refined) {
            RefinedResourceType.COBALT_ALLOY -> "Aleación de Cobalto"
            RefinedResourceType.AETHER_CRYSTAL -> "Cristal de Éter"
            RefinedResourceType.VERIDIAN_INGOT -> "Lingote Veridiano"
            RefinedResourceType.BIO_POLYMER -> "Biopolímero"
            RefinedResourceType.CRIMSON_STEEL -> "Acero Carmesí"
            RefinedResourceType.THERMAL_CORE -> "Núcleo Térmico"
            RefinedResourceType.REFINED_YELLOW_CRYSTAL -> "Cristal Amarillo Refinado"
            RefinedResourceType.XENO_ALLOY -> "Aleación Xeno"
        }
    }

    fun refinedNameFor(resource: ResourceType, lang: GameLanguage): String =
        resource.refinedResource?.let { refinedResourceName(it, lang) }
            ?: resourceName(resource, lang)

    fun alienWreckLabel(lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> "Alien Wreck"
        GameLanguage.ES -> "Pecio Alienígena"
    }

    fun chargesLabel(charges: Int, maxCharges: Int, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> "Charges: $charges/$maxCharges"
        GameLanguage.ES -> "Cargas: $charges/$maxCharges"
    }

    fun refinesInLabel(res: ResourceType, lang: GameLanguage): String {
        val strings = of(lang)
        if (res.isDirectUse || res.refinesInSectorIds.isEmpty()) {
            return strings.refineryDirectUseOnly
        }
        val cities = res.refinesInSectorIds.joinToString(", ")
        return "${strings.marketRefinesInPrefix} $cities"
    }

    fun skillActiveBonus(skill: SkillType, level: Int, lang: GameLanguage): String {
        val lv = level.coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
        val frac = (lv - 1) / 98.0f
        return when (skill) {
            SkillType.MINING, SkillType.SALVAGING -> {
                val sec = baseExtractionSeconds(lv)
                val formatted = String.format(java.util.Locale.US, "%.1f", sec)
                if (lang == GameLanguage.ES) "${formatted}s/carga" else "${formatted}s/charge"
            }
            SkillType.REFINING -> {
                val feePct = Math.round(refiningFeeDiscountPct(lv) * 100f)
                val bonusPct = Math.round(refiningBonusOutputChance(lv) * 100f)
                if (lang == GameLanguage.ES) {
                    "-${feePct}% tarifa • +${bonusPct}% extra"
                } else {
                    "-${feePct}% fee • +${bonusPct}% bonus"
                }
            }
            SkillType.NAVAL_ENGINEERING -> {
                val pct = Math.round(frac * 25f)
                if (lang == GameLanguage.ES) "+${pct}% calidad nave" else "+${pct}% hull quality"
            }
            SkillType.GEAR_MANUFACTURING -> {
                val pct = Math.round(frac * 25f)
                if (lang == GameLanguage.ES) "+${pct}% calidad equipo" else "+${pct}% gear quality"
            }
            SkillType.SYNTHESIS -> {
                val pct = Math.round(frac * 25f)
                if (lang == GameLanguage.ES) "+${pct}% potencia boost" else "+${pct}% booster power"
            }
            SkillType.WEAPONRY -> {
                val pct = Math.round(frac * 20f)
                if (lang == GameLanguage.ES) "+${pct}% daño armas" else "+${pct}% weapon DMG"
            }
            SkillType.SHIELDS -> {
                val pct = Math.round(frac * 20f)
                if (lang == GameLanguage.ES) "+${pct}% cap. escudo" else "+${pct}% shield cap"
            }
            SkillType.PILOTING -> {
                val pct = Math.round(frac * 15f)
                if (lang == GameLanguage.ES) "+${pct}% velocidad" else "+${pct}% speed"
            }
        }
    }

    // -------------------------------------------------------------------------
    // Modular Info / Game Guide EN & ES entries (keyed by GuideEntry keys)
    // -------------------------------------------------------------------------

    private val EN_GUIDE_STRINGS: Map<String, String> = mapOf(
        "guide_capsule_title" to "Capsule, Death & Loot (60/40)",
        "guide_capsule_body" to
            "• Your Pilot never leaves the Capsule. Its 1 laser, 1 tool, and 1 extra slot are permanent and never lost.\n" +
            "• Losing a ship in danger zones destroys that ship permanently.\n" +
            "• 60% of carried cargo and ship gear drops in a wreck with priority for the killer; the remaining 40% is destroyed.\n" +
            "• Your Capsule and VANTs automatically return to your home city (VANTs take 25% durability damage).",

        "guide_professions_title" to "Professions & Industrial Ships",
        "guide_professions_body" to
            "• Industrial ships mine resource nodes by charges; Combat ships salvage Alien Wrecks for Xeno Salvage and Xeno Cores.\n" +
            "• Industrial extraction time multipliers:\n" +
            "  – Miner: 1x Solid / 2x Plasma\n" +
            "  – Plasma Collector: 1x Plasma / 2x Solid\n" +
            "  – Medium Hauler: 3x Any resource\n" +
            "  – Heavy Hauler: 5x Any resource",

        "guide_refining_title" to "Resources & Clockwise Refining",
        "guide_refining_body" to
            "• Raw regional resources refine in the next Capital City clockwise:\n" +
            "  – C1 raw → refines in C2\n" +
            "  – C2 raw → refines in C3\n" +
            "  – C3 raw → refines in C1\n" +
            "• Nucleus Yellow Crystal and Alien Xeno Salvage can be refined in any Capital City (C1, C2, or C3).",

        "guide_skills_title" to "Pilot Skills (Lv 1–99)",
        "guide_skills_body" to
            "• All 9 skills belong to your Pilot profile (not your ship) and persist across ships.\n" +
            "• Shared 4-tier XP curve: Lv 1–33 Easy • Lv 34–66 Medium • Lv 67–89 Hard • Lv 90–99 Very Hard.\n" +
            "• Mining & Salvaging base time per charge scales smoothly:\n" +
            "  3.0s (Lv 1) → 2.0s (Lv 33) → 1.0s (Lv 66) → 0.1s (Lv 99).",

        "guide_quality_title" to "Crafting Quality & Tier Balance",
        "guide_quality_body" to
            "• Crafted items roll one of 4 qualities: Standard (+0%), Reinforced (+5%), Excellent (+10%), and Masterwork (+15%).\n" +
            "• Quality bonuses apply to HP, Shield, and Cargo on ships, or the primary stat on gear and boosters.\n" +
            "• Higher-tier ships keep the same weapon slot count (scaling survivability and cargo), while higher-tier lasers scale damage moderately to prevent one-shots.",

        "guide_economy_title" to "Player-Driven Economy",
        "guide_economy_body" to
            "• Capital Cities only sell basic Tier 1 starter gear (laser, tool, and shield).\n" +
            "• All ships, higher-tier equipment, VANTs, and boosters are crafted and traded exclusively by players across isolated local markets."
    )

    private val ES_GUIDE_STRINGS: Map<String, String> = mapOf(
        "guide_capsule_title" to "Cápsula, Muerte y Botín (60/40)",
        "guide_capsule_body" to
            "• El Piloto nunca abandona la Cápsula: su 1 láser, 1 herramienta y 1 ranura extra son permanentes y nunca se pierden.\n" +
            "• Perder una nave en zonas de peligro la destruye permanentemente.\n" +
            "• El 60% de la carga y el equipo cae en un pecio con prioridad para el atacante; el 40% restante se destruye.\n" +
            "• Tu Cápsula y tus VANTs regresan a tu ciudad natal (los VANTs sufren un 25% de daño de durabilidad).",

        "guide_professions_title" to "Profesiones y Naves Industriales",
        "guide_professions_body" to
            "• Las naves Industriales minan nodos de recursos por cargas; las naves de Combate chatarrean Pecios Alienígenas para obtener Chatarra Xeno y Núcleos Xeno.\n" +
            "• Multiplicadores de tiempo de extracción industrial:\n" +
            "  – Minero: 1x Sólido / 2x Plasma\n" +
            "  – Recolector de Plasma: 1x Plasma / 2x Sólido\n" +
            "  – Carguero Mediano: 3x Cualquier recurso\n" +
            "  – Carguero Pesado: 5x Cualquier recurso",

        "guide_refining_title" to "Recursos y Refinado Horario",
        "guide_refining_body" to
            "• Los recursos brutos regionales se refinan en la siguiente Ciudad Capital en sentido horario:\n" +
            "  – Bruto C1 → se refina en C2\n" +
            "  – Bruto C2 → se refina en C3\n" +
            "  – Bruto C3 → se refina en C1\n" +
            "• El Cristal Amarillo del Núcleo y la Chatarra Xeno se pueden refinar en cualquier ciudad (C1, C2 o C3).",

        "guide_skills_title" to "Habilidades de Piloto (Nv 1–99)",
        "guide_skills_body" to
            "• Las 9 habilidades pertenecen al Piloto (no a la nave) y se conservan al cambiar de casco.\n" +
            "• Curva compartida en 4 tramos: Nv 1–33 Fácil • Nv 34–66 Media • Nv 67–89 Difícil • Nv 90–99 Muy Difícil.\n" +
            "• El tiempo base de Minería y Chatarreo por carga escala de forma continua:\n" +
            "  3.0s (Nv 1) → 2.0s (Nv 33) → 1.0s (Nv 66) → 0.1s (Nv 99).",

        "guide_quality_title" to "Calidad de Fabricación y Balance",
        "guide_quality_body" to
            "• Los objetos fabricados obtienen 4 calidades: Estándar (+0%), Reforzado (+5%), Excelente (+10%) y Obra Maestra (+15%).\n" +
            "• El bono se aplica a Casco, Escudo y Carga en naves, o al atributo principal en equipo y potenciadores.\n" +
            "• Las naves de mayor nivel mantienen el mismo número de ranuras de armas (escalando resistencia y carga), y los láseres escalan su daño de forma moderada para evitar bajas de un solo disparo.",

        "guide_economy_title" to "Economía Impulsada por Jugadores",
        "guide_economy_body" to
            "• Las Ciudades Capitales solo venden equipo básico de Nivel 1 (láser, herramienta y escudo).\n" +
            "• Todas las naves, equipo de nivel superior, VANTs y potenciadores son fabricados y comerciados exclusivamente por jugadores en mercados locales."
    )

    fun guideString(key: String, lang: GameLanguage): String {
        val map = when (lang) {
            GameLanguage.EN -> EN_GUIDE_STRINGS
            GameLanguage.ES -> ES_GUIDE_STRINGS
        }
        return map[key] ?: EN_GUIDE_STRINGS[key] ?: key
    }

    // -------------------------------------------------------------------------
    // Station Crafting System (v1.10 Step 5) EN & ES Recipe Names & Feedback
    // -------------------------------------------------------------------------

    fun craftingDisciplineName(discipline: CraftingDiscipline, lang: GameLanguage): String =
        skillName(discipline.skill, lang)

    fun stationCraftRecipeName(recipe: StationCraftRecipeId, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (recipe) {
            StationCraftRecipeId.SHIP_MINER -> "Prospector Mk I (Miner)"
            StationCraftRecipeId.SHIP_PLASMA_COLLECTOR -> "Siphon Mk I (Plasma Collector)"
            StationCraftRecipeId.SHIP_MEDIUM_HAULER -> "Freighter M-1 (Medium Hauler)"
            StationCraftRecipeId.SHIP_HEAVY_HAULER -> "Leviathan H-1 (Heavy Hauler)"
            StationCraftRecipeId.GEAR_LASER_T2 -> "LF-2 Pulse Laser (T2)"
            StationCraftRecipeId.GEAR_LASER_T3 -> "LF-3 Heavy Laser (T3)"
            StationCraftRecipeId.GEAR_SHIELD_T2 -> "SG-2 Barrier Shield (T2)"
            StationCraftRecipeId.GEAR_SHIELD_T3 -> "SG-3 Aegis Shield (T3)"
            StationCraftRecipeId.GEAR_MINING_TOOL_T2 -> "Mk II Mining Beam"
            StationCraftRecipeId.GEAR_SALVAGE_TOOL_T2 -> "Mk II Salvage Cutter"
            StationCraftRecipeId.SYNTH_REPAIR_KIT -> "Nanite Repair Kit"
            StationCraftRecipeId.SYNTH_DAMAGE_BOOSTER -> "Damage Overdrive Booster"
            StationCraftRecipeId.SYNTH_SHIELD_BOOSTER -> "Shield Capacitor Booster"
            StationCraftRecipeId.SYNTH_SPEED_BOOSTER -> "Ion Thruster Speed Booster"
        }
        GameLanguage.ES -> when (recipe) {
            StationCraftRecipeId.SHIP_MINER -> "Prospector Mk I (Minero)"
            StationCraftRecipeId.SHIP_PLASMA_COLLECTOR -> "Sifón Mk I (Recolector Plasma)"
            StationCraftRecipeId.SHIP_MEDIUM_HAULER -> "Carguero M-1 (Mediano)"
            StationCraftRecipeId.SHIP_HEAVY_HAULER -> "Leviatán H-1 (Carguero Pesado)"
            StationCraftRecipeId.GEAR_LASER_T2 -> "Láser de Pulso LF-2 (N2)"
            StationCraftRecipeId.GEAR_LASER_T3 -> "Láser Pesado LF-3 (N3)"
            StationCraftRecipeId.GEAR_SHIELD_T2 -> "Escudo Barrera SG-2 (N2)"
            StationCraftRecipeId.GEAR_SHIELD_T3 -> "Escudo Égida SG-3 (N3)"
            StationCraftRecipeId.GEAR_MINING_TOOL_T2 -> "Rayo Minero Mk II"
            StationCraftRecipeId.GEAR_SALVAGE_TOOL_T2 -> "Cortador de Chatarra Mk II"
            StationCraftRecipeId.SYNTH_REPAIR_KIT -> "Kit de Reparación Nanite"
            StationCraftRecipeId.SYNTH_DAMAGE_BOOSTER -> "Potenciador de Daño"
            StationCraftRecipeId.SYNTH_SHIELD_BOOSTER -> "Potenciador de Escudo"
            StationCraftRecipeId.SYNTH_SPEED_BOOSTER -> "Potenciador de Velocidad"
        }
    }

    fun stationCraftRecipeSummary(recipe: StationCraftRecipeId, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (recipe) {
            StationCraftRecipeId.SHIP_MINER -> "HP 220 • SHD 140 • Cargo 240 • 1x Solid / 2x Plasma"
            StationCraftRecipeId.SHIP_PLASMA_COLLECTOR -> "HP 200 • SHD 180 • Cargo 220 • 1x Plasma / 2x Solid"
            StationCraftRecipeId.SHIP_MEDIUM_HAULER -> "HP 360 • SHD 240 • Cargo 650 • 3x Extraction"
            StationCraftRecipeId.SHIP_HEAVY_HAULER -> "HP 620 • SHD 440 • Cargo 1400 • 5x Extraction"
            StationCraftRecipeId.GEAR_LASER_T2 -> "+28 DMG (+0%..+15% quality) • Inventory"
            StationCraftRecipeId.GEAR_LASER_T3 -> "+60 DMG (+0%..+15% quality) • Inventory"
            StationCraftRecipeId.GEAR_SHIELD_T2 -> "+180 SHD (+0%..+15% quality) • Inventory"
            StationCraftRecipeId.GEAR_SHIELD_T3 -> "+380 SHD (+0%..+15% quality) • Inventory"
            StationCraftRecipeId.GEAR_MINING_TOOL_T2 -> "+15% Mining Speed (+0%..+15% quality) • Tool"
            StationCraftRecipeId.GEAR_SALVAGE_TOOL_T2 -> "+15% Salvage Speed (+0%..+15% quality) • Tool"
            StationCraftRecipeId.SYNTH_REPAIR_KIT -> "Restores 250 Hull/Shield (+0%..+15% quality)"
            StationCraftRecipeId.SYNTH_DAMAGE_BOOSTER -> "+12% Weapon DMG • 10m (+0%..+15% quality)"
            StationCraftRecipeId.SYNTH_SHIELD_BOOSTER -> "+15% Shield Cap • 10m (+0%..+15% quality)"
            StationCraftRecipeId.SYNTH_SPEED_BOOSTER -> "+10% Ship Speed • 10m (+0%..+15% quality)"
        }
        GameLanguage.ES -> when (recipe) {
            StationCraftRecipeId.SHIP_MINER -> "Casco 220 • Esc 140 • Carga 240 • 1x Sólido / 2x Plasma"
            StationCraftRecipeId.SHIP_PLASMA_COLLECTOR -> "Casco 200 • Esc 180 • Carga 220 • 1x Plasma / 2x Sólido"
            StationCraftRecipeId.SHIP_MEDIUM_HAULER -> "Casco 360 • Esc 240 • Carga 650 • 3x Extracción"
            StationCraftRecipeId.SHIP_HEAVY_HAULER -> "Casco 620 • Esc 440 • Carga 1400 • 5x Extracción"
            StationCraftRecipeId.GEAR_LASER_T2 -> "+28 DAÑO (+0%..+15% calidad) • Inventario"
            StationCraftRecipeId.GEAR_LASER_T3 -> "+60 DAÑO (+0%..+15% calidad) • Inventario"
            StationCraftRecipeId.GEAR_SHIELD_T2 -> "+180 ESC (+0%..+15% calidad) • Inventario"
            StationCraftRecipeId.GEAR_SHIELD_T3 -> "+380 ESC (+0%..+15% calidad) • Inventario"
            StationCraftRecipeId.GEAR_MINING_TOOL_T2 -> "+15% Vel. Minería (+0%..+15% calidad) • Herram."
            StationCraftRecipeId.GEAR_SALVAGE_TOOL_T2 -> "+15% Vel. Chatarreo (+0%..+15% calidad) • Herram."
            StationCraftRecipeId.SYNTH_REPAIR_KIT -> "Restaura 250 Casco/Escudo (+0%..+15% calidad)"
            StationCraftRecipeId.SYNTH_DAMAGE_BOOSTER -> "+12% Daño Armas • 10m (+0%..+15% calidad)"
            StationCraftRecipeId.SYNTH_SHIELD_BOOSTER -> "+15% Cap. Escudo • 10m (+0%..+15% calidad)"
            StationCraftRecipeId.SYNTH_SPEED_BOOSTER -> "+10% Velocidad • 10m (+0%..+15% calidad)"
        }
    }

    fun craftMaterialLabel(cost: CraftMaterialCost, lang: GameLanguage): String =
        if (cost.isRefined) refinedNameFor(cost.resource, lang) else resourceName(cost.resource, lang)

    fun craftOutcomeStatSummary(
        recipe: StationCraftRecipeId,
        quality: CraftQuality,
        lang: GameLanguage
    ): String {
        val es = lang == GameLanguage.ES
        return when (recipe.outputKind) {
            CraftOutputKind.INDUSTRIAL_SHIP -> {
                val hull = recipe.shipHullDefinition()
                val hp = quality.applyBonusInt(hull?.baseHull?.toInt() ?: 220)
                val shd = quality.applyBonusInt(hull?.baseShield?.toInt() ?: 140)
                val cargo = quality.applyBonusInt(hull?.baseCargo ?: 240)
                if (es) "Casco $hp • Esc $shd • Carga $cargo" else "HP $hp • SHD $shd • Cargo $cargo"
            }
            CraftOutputKind.EQUIPMENT_GEAR -> {
                val eq = recipe.resultEquipmentId
                if (eq?.category == EquipmentCategory.LASER) {
                    val dmg = quality.applyBonusInt(eq.damageBonus.toInt())
                    if (es) "+$dmg DAÑO" else "+$dmg DMG"
                } else {
                    val shd = quality.applyBonusInt((eq?.shieldBonus ?: recipe.basePrimaryStat).toInt())
                    if (es) "+$shd ESC" else "+$shd SHD"
                }
            }
            CraftOutputKind.EXTRACTION_TOOL -> {
                val pct = String.format(java.util.Locale.US, "%.1f", quality.applyBonus(recipe.basePrimaryStat))
                if (recipe == StationCraftRecipeId.GEAR_MINING_TOOL_T2) {
                    if (es) "+$pct% Vel. Minería" else "+$pct% Mining Speed"
                } else {
                    if (es) "+$pct% Vel. Chatarreo" else "+$pct% Salvage Speed"
                }
            }
            CraftOutputKind.CONSUMABLE_REPAIR -> {
                val heal = quality.applyBonusInt(recipe.basePrimaryStat.toInt())
                if (es) "+$heal Casco/Escudo" else "+$heal Hull/Shield"
            }
            CraftOutputKind.CONSUMABLE_BOOSTER -> {
                val pct = String.format(java.util.Locale.US, "%.1f", quality.applyBonus(recipe.basePrimaryStat))
                val mins = quality.applyBonusInt(recipe.baseDurationMinutes)
                when (recipe) {
                    StationCraftRecipeId.SYNTH_DAMAGE_BOOSTER ->
                        if (es) "+$pct% Daño • ${mins}m" else "+$pct% DMG • ${mins}m"
                    StationCraftRecipeId.SYNTH_SHIELD_BOOSTER ->
                        if (es) "+$pct% Escudo • ${mins}m" else "+$pct% Shield • ${mins}m"
                    else ->
                        if (es) "+$pct% Velocidad • ${mins}m" else "+$pct% Speed • ${mins}m"
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // City Market Basic Vendor (Step 6) EN & ES Names & Descriptions
    // -------------------------------------------------------------------------

    fun basicVendorItemName(item: BasicVendorItemId, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (item) {
            BasicVendorItemId.BASIC_LASER_T1 -> "Basic Tier 1 Laser (LF-1)"
            BasicVendorItemId.BASIC_TOOL_T1 -> "Basic Mining / Salvage Tool (T1)"
            BasicVendorItemId.BASIC_SHIELD_T1 -> "Basic Tier 1 Shield (SG-1)"
            BasicVendorItemId.BASIC_REPAIR_KIT -> "Basic Repair Kit (T1)"
        }
        GameLanguage.ES -> when (item) {
            BasicVendorItemId.BASIC_LASER_T1 -> "Láser Básico Nivel 1 (LF-1)"
            BasicVendorItemId.BASIC_TOOL_T1 -> "Herramienta Básica Minería/Chatarreo (N1)"
            BasicVendorItemId.BASIC_SHIELD_T1 -> "Escudo Básico Nivel 1 (SG-1)"
            BasicVendorItemId.BASIC_REPAIR_KIT -> "Kit de Reparación Básico (N1)"
        }
    }

    fun basicVendorItemDesc(item: BasicVendorItemId, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (item) {
            BasicVendorItemId.BASIC_LASER_T1 -> "+12 DMG • Standard T1 ship laser added to Hangar"
            BasicVendorItemId.BASIC_TOOL_T1 -> "T1 extraction tool for resource nodes & alien wrecks"
            BasicVendorItemId.BASIC_SHIELD_T1 -> "+80 SHD • Standard T1 shield generator added to Hangar"
            BasicVendorItemId.BASIC_REPAIR_KIT -> "Restores +100% Hull & Shield immediately on purchase"
        }
        GameLanguage.ES -> when (item) {
            BasicVendorItemId.BASIC_LASER_T1 -> "+12 DAÑO • Láser estándar N1 añadido al Hangar"
            BasicVendorItemId.BASIC_TOOL_T1 -> "Herramienta N1 para nodos de recursos y pecios"
            BasicVendorItemId.BASIC_SHIELD_T1 -> "+80 ESC • Generador de escudo N1 añadido al Hangar"
            BasicVendorItemId.BASIC_REPAIR_KIT -> "Restaura 100% Casco y Escudo al instante al comprar"
        }
    }
}
