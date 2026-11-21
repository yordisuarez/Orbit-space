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
    val hudSettings: String,
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

    // Local Tactical Map
    val localMapSectorPrefix: String,

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
    val craftingNeedRefinedHint: String
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
        hudSettings = "Settings",
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

        localMapSectorPrefix = "SECTOR",

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
        craftingNeedRefinedHint = "Refine raw ores at the city Refinery first to craft gear & consumables."
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
        hudSettings = "Ajustes",
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

        localMapSectorPrefix = "SECTOR",

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
        craftingNeedRefinedHint = "Refina minerales en la Refinería de la ciudad para fabricar equipo y consumibles."
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
            StationKind.REFINERY -> "Clockwise Loop: C1→C2 • C2→C3 • C3→C1 • Yellow Crystal→0-0"
            StationKind.CRAFTING -> "Assemble Consumables, Chips & Ship Modules Using Refined Resources"
            StationKind.LOCAL_MARKET -> "Independent Local Economy (${sector.id} Only — Not Linked to Other Cities)"
        }
        GameLanguage.ES -> when (kind) {
            StationKind.REFINERY -> "Ciclo Horario: C1→C2 • C2→C3 • C3→C1 • Cristal Amarillo→0-0"
            StationKind.CRAFTING -> "Fabrica Consumibles, Chips y Módulos con Recursos Refinados"
            StationKind.LOCAL_MARKET -> "Economía Local Independiente (Solo ${sector.id} — No vinculada a otras ciudades)"
        }
    }

    fun refineryAcceptedSummary(sectorId: String, lang: GameLanguage): String = when (lang) {
        GameLanguage.EN -> when (sectorId) {
            "C1" -> "Accepts City 3 (Red / C3) Raw Ores Only"
            "C2" -> "Accepts City 1 (Blue / C1) Raw Ores Only"
            "C3" -> "Accepts City 2 (Green / C2) Raw Ores Only"
            "0-0" -> "Accepts Yellow Crystal Exclusively"
            else -> "Local Refining Bay"
        }
        GameLanguage.ES -> when (sectorId) {
            "C1" -> "Acepta solo minerales brutos de Ciudad 3 (Roja / C3)"
            "C2" -> "Acepta solo minerales brutos de Ciudad 1 (Azul / C1)"
            "C3" -> "Acepta solo minerales brutos de Ciudad 2 (Verde / C2)"
            "0-0" -> "Acepta exclusivamente Cristal Amarillo"
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
}
