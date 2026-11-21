import 'dart:convert';
import 'dart:math';
import 'dart:ui' as ui;

import 'package:flame/components.dart';
import 'package:flame/events.dart';
import 'package:flame/game.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'economy_models.dart';
import 'galaxy_map_data.dart';
import 'game_models.dart';
import 'game_strings.dart';
import 'ship_renderer.dart';

export 'economy_models.dart';
export 'galaxy_map_data.dart';
export 'game_models.dart';
export 'game_strings.dart';
export 'ship_renderer.dart';

class DarkOrbitGame extends FlameGame with TapCallbacks {
  DarkOrbitGame({
    this.pilotName,
    this.pilotCompany,
    this.onResetToIntro,
  });

  static const String hangarOverlayId = 'hangar';
  static const String tacticalMapOverlayId = 'tactical_map';
  static const String galaxyMapOverlayId = 'galaxy_map';
  static const String menuOverlayId = 'menu';
  static const String skillsOverlayId = 'skills';
  static const String inventoryOverlayId = 'inventory';
  static const String guideOverlayId = 'guide';
  static const String settingsOverlayId = 'settings';
  static const String stationOverlayId = 'station';
  static const String deathOverlayId = 'death';

  static const String savePrefsKey = 'dark_orbit_mobile_save_v2';
  static const String pilotNamePrefsKey = 'pilotName';
  static const String companyIdPrefsKey = 'companyId';
  static const String languagePrefsKey = 'gameLanguage';

  String? pilotName;
  PilotCompany? pilotCompany;
  final VoidCallback? onResetToIntro;

  final ValueNotifier<int> uiVersion = ValueNotifier<int>(0);
  double _uiHudTickAccum = 0.0;

  void notifyUi() {
    uiVersion.value = uiVersion.value + 1;
  }

  GameLanguage language = GameLanguage.en;

  // Toggleable Admin / Debug Mode (free testing across Market, Refinery, Crafting, Skills)
  bool isAdminMode = false;
  String? adminBannerText;
  double adminBannerTimer = 0.0;

  // Pinch-to-Zoom Camera State (0.6x - 2.5x) + Dynamic Ship Detail LOD (>= 1.35x)
  double cameraZoom = kCameraZoomDefault;
  bool isPinching = false;

  PilotCompanySpec? get pilotCompanySpec =>
      pilotCompany != null ? PilotCompanyCatalog.of(pilotCompany!) : null;

  String? get formattedPilotCallsign {
    final String? name = pilotName?.trim();
    if (name == null || name.isEmpty) return null;
    final PilotCompanySpec? spec = pilotCompanySpec;
    return spec != null ? '[${spec.tag}] $name' : name;
  }

  final Random rng = Random();

  late PlayerShipComponent player;
  late VantFleetComponent vantFleet;
  late JoystickComponent moveJoystick;
  late JoystickComponent aimJoystick;
  late AttackButtonComponent attackButton;
  late RocketButtonComponent rocketButton;
  late JumpButtonComponent jumpButton;
  late ConfigButtonComponent configButton;
  late FormationButtonComponent formationButton;
  late FormationSubmenuComponent formationSubmenu;
  late GalaxyMapButtonComponent galaxyMapButton;
  late MissionHudComponent missionHud;
  late MinimapComponent minimap;
  late TacticalHudComponent tacticalHud;
  late WarpFlashComponent warpFlashOverlay;

  final List<AlienShipComponent> aliens = <AlienShipComponent>[];
  final List<LootBoxComponent> lootBoxes = <LootBoxComponent>[];

  // Permanent Capsule, Crafted Industrial Ships & Combat Ship Classes
  ShipRole activeShipRole = ShipRole.capsule;
  int? activeCraftedShipId;
  final Set<ShipClass> ownedShipClasses = <ShipClass>{ShipClass.interceptor};
  ShipClass activeShipClass = ShipClass.interceptor;
  final List<CraftedShipRecord> ownedCraftedShips = <CraftedShipRecord>[];
  final List<CraftedItemRecord> ownedCraftedItems = <CraftedItemRecord>[];
  int _nextCraftedId = 1;
  int _nextCraftOutcomeId = 1;
  CraftOutcome? lastCraftOutcome;
  int basicToolsOwned = 0;

  // 9 Pilot Skills (Lv 1-99) & Cargo Hold
  final Map<SkillType, SkillProgress> pilotSkills = createDefaultPilotSkills();
  final Map<ResourceType, int> rawCargo = <ResourceType, int>{};
  final Map<ResourceType, int> refinedCargo = <ResourceType, int>{};

  // World Resource Nodes, Alien Wrecks, Player Ship Wrecks & Stations
  final List<ResourceNodeEntity> resourceNodes = <ResourceNodeEntity>[];
  final List<AlienWreckEntity> alienWrecks = <AlienWreckEntity>[];
  final List<PlayerShipWreck> playerWrecks = <PlayerShipWreck>[];
  List<SectorStation> activeStations = <SectorStation>[];
  StationKind openStationTab = StationKind.refinery;
  final Map<String, LocalMarketState> _localMarkets =
      <String, LocalMarketState>{};
  ResourceNodeEntity? activeMiningNode;
  AlienWreckEntity? activeSalvageWreck;
  double _combatMiningWarnCooldown = 0.0;
  int _nextWorldEntityId = 1;
  DeathSummary? lastDeathSummary;

  // Classic DarkOrbit VANT Drone Formations
  final Set<DroneFormation> ownedFormations = <DroneFormation>{
    DroneFormation.normal,
  };
  DroneFormation activeFormation = DroneFormation.normal;
  double formationSwitchCooldown = 0.0;
  bool isFormationMenuOpen = false;

  // Direct Tap-to-Move Destination Target
  Vector2? autopilotTarget;

  // Owned equipment pool + Config 1 & Config 2 Laser/Shield slot grids + Dedicated Sector Chip slot
  final List<EquipmentItem> ownedItems = <EquipmentItem>[];
  final List<EquipmentItem?> config1LaserSlots =
      List<EquipmentItem?>.filled(4, null, growable: true);
  final List<EquipmentItem?> config1ShieldSlots =
      List<EquipmentItem?>.filled(3, null, growable: true);
  final List<EquipmentItem?> config2LaserSlots =
      List<EquipmentItem?>.filled(4, null, growable: true);
  final List<EquipmentItem?> config2ShieldSlots =
      List<EquipmentItem?>.filled(3, null, growable: true);

  int activeConfig = 1;
  EquipmentItem? equippedSectorChip;
  final List<VantDrone> ownedVants = <VantDrone>[];
  int _nextItemInstanceId = 1;
  int _nextVantId = 1;

  // Sequential Mission Chain state
  int currentMissionIndex = 0;
  int currentMissionProgress = 0;

  AlienShipComponent? lockedTarget;
  bool autoFireEnabled = false;
  int credits = 0;
  int kills = 0;
  double elapsedTime = 0.0;

  // 25-Sector Galactic Map & Multi-Portal Navigation State
  String currentSectorId = GalaxyMapGraph.defaultSectorId;
  List<SectorPortal> activePortals = GalaxyMapGraph.portalsForSector(
    GalaxyMapGraph.defaultSectorId,
  );
  List<String> plottedRoute = <String>[];
  double warpFlashTimer = 0.0;

  SectorNode get currentSector => GalaxyMapGraph.sectorOf(currentSectorId);
  double get activeMapSize => currentSector.mapSize;
  String? get nextRouteHopSectorId =>
      (plottedRoute.length >= 2 && plottedRoute.first == currentSectorId)
          ? plottedRoute[1]
          : null;

  bool get isFlyingCapsule => activeShipRole == ShipRole.capsule;

  CraftedShipRecord? get activeCraftedShip {
    if (activeShipRole != ShipRole.industrial || activeCraftedShipId == null) {
      return null;
    }
    for (final CraftedShipRecord s in ownedCraftedShips) {
      if (s.instanceId == activeCraftedShipId) return s;
    }
    return null;
  }

  IndustrialType? get activeIndustrialType => activeCraftedShip?.industrialType;

  ShipClassSpec get activeShipSpec => ShipClassCatalog.of(activeShipClass);

  double get activeHullRadius {
    switch (activeShipRole) {
      case ShipRole.capsule:
        return CapsulePermanentConfig.hullRadius;
      case ShipRole.industrial:
        final IndustrialType? ind = activeIndustrialType;
        if (ind != null) {
          return ShipHullCatalog.industrialT1(ind).hullRadius;
        }
        return 22.0;
      case ShipRole.combat:
        return activeShipSpec.hullRadius;
    }
  }

  Color get activeShipAccentColor {
    switch (activeShipRole) {
      case ShipRole.capsule:
        return const Color(0xFF00E5FF);
      case ShipRole.industrial:
        return activeIndustrialType?.accentColor ?? const Color(0xFF38BDF8);
      case ShipRole.combat:
        return activeShipSpec.accentColor;
    }
  }

  String get activeShipDisplayName {
    switch (activeShipRole) {
      case ShipRole.capsule:
        return GameStrings.t(language, 'Pilot Capsule', 'Cápsula');
      case ShipRole.industrial:
        final IndustrialType? ind = activeIndustrialType;
        if (ind != null) {
          return GameStrings.industrialTypeName(ind, language);
        }
        return 'Industrial';
      case ShipRole.combat:
        return activeShipSpec.displayName;
    }
  }

  int get currentCargoCapacity {
    switch (activeShipRole) {
      case ShipRole.capsule:
        return CapsulePermanentConfig.cargoCapacity;
      case ShipRole.industrial:
        return activeCraftedShip?.effectiveCargo ?? 240;
      case ShipRole.combat:
        return ShipHullCatalog.cargoForCombatClass(activeShipClass);
    }
  }

  int get totalCargoUsed {
    int total = 0;
    for (final int v in rawCargo.values) {
      total += v;
    }
    for (final int v in refinedCargo.values) {
      total += v;
    }
    return total;
  }

  int get remainingCargoCapacity => max(0, currentCargoCapacity - totalCargoUsed);

  int skillLevelOf(SkillType skill) =>
      (pilotSkills[skill] ?? const SkillProgress()).level;

  SkillProgress skillProgressOf(SkillType skill) =>
      pilotSkills[skill] ?? const SkillProgress();

  int get totalSkillLevels {
    int sum = 0;
    for (final SkillType s in SkillType.values) {
      sum += skillLevelOf(s);
    }
    return sum;
  }

  DroneFormationSpec get activeFormationSpec =>
      DroneFormationCatalog.of(activeFormation);

  List<EquipmentItem?> get laserSlots =>
      activeConfig == 2 ? config2LaserSlots : config1LaserSlots;

  List<EquipmentItem?> get shieldSlots =>
      activeConfig == 2 ? config2ShieldSlots : config1ShieldSlots;

  int get equippedChipTier => equippedSectorChip?.spec.sectorTier ?? 0;

  int get flaxVantCount =>
      ownedVants.where((VantDrone v) => v.tier == VantTier.flax).length;

  int get irisVantCount =>
      ownedVants.where((VantDrone v) => v.tier == VantTier.iris).length;

  /// Returns up to 8 active VANTs for flight, rendering, stats, and slot equipping,
  /// strictly prioritizing Iris drones first and then Flax drones (stable by ID).
  List<VantDrone> get activeVants {
    if (ownedVants.isEmpty) return const <VantDrone>[];
    final List<VantDrone> sorted = List<VantDrone>.of(ownedVants);
    sorted.sort((VantDrone a, VantDrone b) {
      final int tierA = a.tier == VantTier.iris ? 0 : 1;
      final int tierB = b.tier == VantTier.iris ? 0 : 1;
      final int cmp = tierA.compareTo(tierB);
      return cmp != 0 ? cmp : a.id.compareTo(b.id);
    });
    if (sorted.length <= GameConstants.maxVants) return sorted;
    return sorted.sublist(0, GameConstants.maxVants);
  }

  bool get canBuyFlaxVant =>
      irisVantCount < GameConstants.maxVants &&
      ownedVants.length < GameConstants.maxOwnedVantsForFusion;

  void _clearReserveVantSlots() {
    if (ownedVants.length <= GameConstants.maxVants) return;
    final Set<int> activeIds =
        activeVants.map((VantDrone v) => v.id).toSet();
    for (final VantDrone vant in ownedVants) {
      if (!activeIds.contains(vant.id)) {
        for (int i = 0; i < vant.config1Slots.length; i++) {
          vant.config1Slots[i] = null;
        }
        for (int i = 0; i < vant.config2Slots.length; i++) {
          vant.config2Slots[i] = null;
        }
      }
    }
  }

  List<EquipmentItem> get inventory {
    final Set<int> equippedIds = <int>{};
    for (final EquipmentItem? item in laserSlots) {
      if (item != null) equippedIds.add(item.instanceId);
    }
    for (final EquipmentItem? item in shieldSlots) {
      if (item != null) equippedIds.add(item.instanceId);
    }
    for (final VantDrone vant in activeVants) {
      for (final EquipmentItem? item in vant.slotsForConfig(activeConfig)) {
        if (item != null) equippedIds.add(item.instanceId);
      }
    }
    if (equippedSectorChip != null) {
      equippedIds.add(equippedSectorChip!.instanceId);
    }
    return ownedItems
        .where((EquipmentItem item) => !equippedIds.contains(item.instanceId))
        .toList();
  }

  Map<EquipmentItemId, int> get ownedCountsByType {
    final Map<EquipmentItemId, int> counts = <EquipmentItemId, int>{};
    for (final EquipmentItem item in ownedItems) {
      counts[item.id] = (counts[item.id] ?? 0) + 1;
    }
    return counts;
  }

  bool get hasAnyReadyFusion {
    if (flaxVantCount >= 2) return true;
    final Map<EquipmentItemId, int> counts = ownedCountsByType;
    for (final FusionRecipe recipe in FusionCatalog.recipes) {
      if ((counts[recipe.source] ?? 0) >= 2) {
        return true;
      }
    }
    return false;
  }

  MissionSpec? get activeMission =>
      currentMissionIndex >= 0 && currentMissionIndex < MissionCatalog.all.length
          ? MissionCatalog.all[currentMissionIndex]
          : null;

  bool get isMissionLevelLocked {
    final MissionSpec? m = activeMission;
    return m != null && player.level < m.requiredLevel;
  }

  bool get isMissionReadyToClaim {
    final MissionSpec? m = activeMission;
    return m != null &&
        !isMissionLevelLocked &&
        currentMissionProgress >= m.targetCount;
  }

  Vector2 get activeSafeZoneCenter =>
      Vector2(activeMapSize * 0.5, activeMapSize * 0.5);

  double get activeSafeZoneRadius =>
      currentSector.hasCentralSafeZone ? currentSector.safeZoneRadius : 0.0;

  bool isPointInActiveSafeZone(Vector2 pt) {
    if (currentSector.isEntireSectorSafe) return true;
    if (!currentSector.hasCentralSafeZone) return false;
    return pt.distanceTo(activeSafeZoneCenter) <= activeSafeZoneRadius;
  }

  SectorPortal? get nearestJumpPortal {
    SectorPortal? best;
    double bestDist = GameConstants.jumpGateInteractRadius;
    for (final SectorPortal portal in activePortals) {
      final double dx = player.position.x - portal.worldX;
      final double dy = player.position.y - portal.worldY;
      final double d = sqrt(dx * dx + dy * dy);
      if (d <= bestDist) {
        bestDist = d;
        best = portal;
      }
    }
    return best;
  }

  bool get isPlayerNearJumpGate => nearestJumpPortal != null;

  bool isOutOfActiveMapBounds(Vector2 pt) {
    const double pad = GameConstants.mapX1RadiationOuterBuffer + 180.0;
    return pt.x < -pad ||
        pt.x > activeMapSize + pad ||
        pt.y < -pad ||
        pt.y > activeMapSize + pad;
  }

  SpaceZone get currentZone {
    if (player.inSafeZone) return SpaceZone.safeZone;
    switch (currentSector.zoneType) {
      case ZoneType.capitalCity:
      case ZoneType.safeZone:
      case ZoneType.intermediateZone:
        return SpaceZone.zone1Alpha;
      case ZoneType.moderateZone:
        return SpaceZone.zone2Beta;
      case ZoneType.highRiskYellowRing:
        return SpaceZone.zone3Gamma;
      case ZoneType.nucleus:
        return SpaceZone.zone4Omega;
    }
  }

  static SpaceZone zoneForDistance(double dist) {
    if (dist <= GameConstants.safeZoneRadius) {
      return SpaceZone.safeZone;
    } else if (dist <= GameConstants.zone1MaxRadius) {
      return SpaceZone.zone1Alpha;
    } else if (dist <= GameConstants.zone2MaxRadius) {
      return SpaceZone.zone2Beta;
    } else if (dist <= GameConstants.zone3MaxRadius) {
      return SpaceZone.zone3Gamma;
    } else {
      return SpaceZone.zone4Omega;
    }
  }

  bool isInViewport(Vector2 worldPos, {double margin = 120.0}) {
    final Vector2 cam = camera.viewfinder.position;
    final double invZoom = 1.0 / max(0.25, cameraZoom);
    final double halfW = size.x * 0.5 * invZoom + margin;
    final double halfH = size.y * 0.5 * invZoom + margin;
    return (worldPos.x - cam.x).abs() <= halfW &&
        (worldPos.y - cam.y).abs() <= halfH;
  }

  // Dynamic Ship Stats (Capsule / Industrial / Combat + Quality Gear + VANTs + Pilot Skills + Formation)
  double get currentLaserDamage {
    final double skillMult =
        1.0 + ((skillLevelOf(SkillType.weaponry) - 1) / 98.0) * 0.20;
    if (activeShipRole == ShipRole.capsule) {
      return max(
        1.0,
        (GameConstants.playerBaseLaserDamage +
                CapsulePermanentConfig.permanentLaserDamage) *
            skillMult,
      );
    }
    double sum = GameConstants.playerBaseLaserDamage;
    for (final EquipmentItem? item in laserSlots) {
      if (item != null) sum += item.effectiveDamageBonus;
    }
    for (final VantDrone vant in activeVants) {
      for (final EquipmentItem? item in vant.slotsForConfig(activeConfig)) {
        if (item != null) sum += item.effectiveDamageBonus;
      }
    }
    return max(1.0, sum * activeFormationSpec.damageMult * skillMult);
  }

  double get currentMaxShield {
    final double skillMult =
        1.0 + ((skillLevelOf(SkillType.shields) - 1) / 98.0) * 0.20;
    if (activeShipRole == ShipRole.capsule) {
      return max(
        10.0,
        (CapsulePermanentConfig.baseShield +
                CapsulePermanentConfig.permanentShieldBonus) *
            skillMult,
      );
    }
    double sum = activeShipRole == ShipRole.industrial
        ? (activeCraftedShip?.effectiveShield.toDouble() ?? 140.0)
        : GameConstants.playerBaseMaxShield;
    for (final EquipmentItem? item in shieldSlots) {
      if (item != null) sum += item.effectiveShieldBonus;
    }
    for (final VantDrone vant in activeVants) {
      for (final EquipmentItem? item in vant.slotsForConfig(activeConfig)) {
        if (item != null) sum += item.effectiveShieldBonus;
      }
    }
    return max(10.0, sum * activeFormationSpec.shieldMult * skillMult);
  }

  double get currentSpeed {
    final double skillMult =
        1.0 + ((skillLevelOf(SkillType.piloting) - 1) / 98.0) * 0.15;
    final double baseSpeed;
    switch (activeShipRole) {
      case ShipRole.capsule:
        baseSpeed = CapsulePermanentConfig.speed;
        break;
      case ShipRole.industrial:
        final IndustrialType? ind = activeIndustrialType;
        baseSpeed = ind != null
            ? ShipHullCatalog.industrialT1(ind).speed
            : 220.0;
        break;
      case ShipRole.combat:
        baseSpeed = activeShipSpec.speed;
        break;
    }
    return baseSpeed * activeFormationSpec.speedMult * skillMult;
  }

  double get currentMaxHull {
    final double base;
    switch (activeShipRole) {
      case ShipRole.capsule:
        base = CapsulePermanentConfig.baseHull +
            (player.level - 1) * (GameConstants.playerHpPerLevel * 0.5);
        break;
      case ShipRole.industrial:
        base = (activeCraftedShip?.effectiveHull.toDouble() ?? 220.0) +
            (player.level - 1) * GameConstants.playerHpPerLevel;
        break;
      case ShipRole.combat:
        base = activeShipSpec.baseHull +
            (player.level - 1) * GameConstants.playerHpPerLevel;
        break;
    }
    return max(40.0, base * activeFormationSpec.hullMult);
  }

  @override
  Color backgroundColor() => currentSector.zoneType.spaceBackgroundColor;

  @override
  Future<void> onLoad() async {
    await super.onLoad();

    await world.add(SpaceMapComponent());

    currentSectorId = GalaxyMapGraph.homeCityForCompany(pilotCompany);
    activePortals = GalaxyMapGraph.portalsForSector(currentSectorId);
    player = PlayerShipComponent(position: activeSafeZoneCenter.clone());
    vantFleet = VantFleetComponent();
    _initStarterState();
    await _loadSavedGame();
    activePortals = GalaxyMapGraph.portalsForSector(currentSectorId);
    player
      ..position.setFrom(activeSafeZoneCenter)
      ..hp = currentMaxHull
      ..shield = currentMaxShield;
    _snapVantsToFormation();
    await world.add(player);
    await world.add(vantFleet);

    _populateActiveMapEntities();

    camera.viewfinder.anchor = Anchor.center;
    camera.viewfinder.zoom = cameraZoom;
    camera.follow(player, snap: true);

    moveJoystick = JoystickComponent(
      knob: CircleComponent(
        radius: 22.0,
        paint: Paint()..color = const Color(0xDD00E5FF),
      ),
      background: CircleComponent(
        radius: 56.0,
        paint: Paint()
          ..color = const Color(0x440B1324)
          ..style = PaintingStyle.fill,
      )..add(
          CircleComponent(
            radius: 56.0,
            paint: Paint()
              ..color = const Color(0x8800E5FF)
              ..style = PaintingStyle.stroke
              ..strokeWidth = 2.0,
          ),
        ),
      margin: const EdgeInsets.only(left: 32.0, bottom: 28.0),
    );

    aimJoystick = JoystickComponent(
      knob: CircleComponent(
        radius: 22.0,
        paint: Paint()..color = const Color(0xDDFF2E4C),
      ),
      background: CircleComponent(
        radius: 56.0,
        paint: Paint()
          ..color = const Color(0x440B1324)
          ..style = PaintingStyle.fill,
      )..add(
          CircleComponent(
            radius: 56.0,
            paint: Paint()
              ..color = const Color(0x88FF2E4C)
              ..style = PaintingStyle.stroke
              ..strokeWidth = 2.0,
          ),
        ),
      margin: const EdgeInsets.only(right: 32.0, bottom: 28.0),
    );

    attackButton = AttackButtonComponent();
    rocketButton = RocketButtonComponent();
    jumpButton = JumpButtonComponent();
    configButton = ConfigButtonComponent();
    formationButton = FormationButtonComponent();
    formationSubmenu = FormationSubmenuComponent();
    galaxyMapButton = GalaxyMapButtonComponent();
    missionHud = MissionHudComponent();
    minimap = MinimapComponent();
    tacticalHud = TacticalHudComponent();
    warpFlashOverlay = WarpFlashComponent();

    await camera.viewport.addAll(<Component>[
      moveJoystick,
      aimJoystick,
      attackButton,
      rocketButton,
      jumpButton,
      configButton,
      formationButton,
      galaxyMapButton,
      missionHud,
      minimap,
      tacticalHud,
      formationSubmenu,
      warpFlashOverlay,
    ]);
    notifyUi();
  }

  void _initStarterState() {
    credits = 250;
    kills = 0;
    player.level = 1;
    player.xp = 0;
    activeConfig = 1;
    activeShipRole = ShipRole.capsule;
    activeCraftedShipId = null;
    ownedShipClasses
      ..clear()
      ..add(ShipClass.interceptor);
    activeShipClass = ShipClass.interceptor;
    ownedCraftedShips.clear();
    ownedCraftedItems.clear();
    _nextCraftedId = 1;
    lastCraftOutcome = null;
    basicToolsOwned = 0;
    pilotSkills
      ..clear()
      ..addAll(createDefaultPilotSkills());
    rawCargo.clear();
    refinedCargo.clear();
    playerWrecks.clear();
    lastDeathSummary = null;
    ownedFormations
      ..clear()
      ..add(DroneFormation.normal);
    activeFormation = DroneFormation.normal;
    formationSwitchCooldown = 0.0;
    autopilotTarget = null;
    equippedSectorChip = null;
    ownedVants.clear();
    _nextVantId = 1;
    currentMissionIndex = 0;
    currentMissionProgress = 0;
    ownedItems.clear();
    _resizeSlotsForShip(activeShipClass);
    for (int i = 0; i < config1LaserSlots.length; i++) {
      config1LaserSlots[i] = null;
      config2LaserSlots[i] = null;
    }
    for (int i = 0; i < config1ShieldSlots.length; i++) {
      config1ShieldSlots[i] = null;
      config2ShieldSlots[i] = null;
    }
    _nextItemInstanceId = 1;
    final EquipmentItem starterLaser = EquipmentItem(
      instanceId: _nextItemInstanceId++,
      id: EquipmentItemId.lf1,
    );
    final EquipmentItem starterShield = EquipmentItem(
      instanceId: _nextItemInstanceId++,
      id: EquipmentItemId.sg1,
    );
    final EquipmentItem starterChip = EquipmentItem(
      instanceId: _nextItemInstanceId++,
      id: EquipmentItemId.chip1,
    );
    ownedItems.addAll(<EquipmentItem>[starterLaser, starterShield, starterChip]);
    config1LaserSlots[0] = starterLaser;
    config1ShieldSlots[0] = starterShield;
    config2LaserSlots[0] = starterLaser;
    config2ShieldSlots[0] = starterShield;
    equippedSectorChip = starterChip;
  }

  Future<void> saveGame() async {
    try {
      final SharedPreferences prefs = await SharedPreferences.getInstance();
      final Map<String, dynamic> payload = <String, dynamic>{
        'credits': credits,
        'kills': kills,
        'level': player.level,
        'xp': player.xp,
        'activeConfig': activeConfig,
        'activeShipClass': activeShipClass.name,
        'ownedShipClasses':
            ownedShipClasses.map((ShipClass s) => s.name).toList(),
        'activeFormation': activeFormation.name,
        'ownedFormations':
            ownedFormations.map((DroneFormation f) => f.name).toList(),
        'nextId': _nextItemInstanceId,
        'nextVantId': _nextVantId,
        'equippedChipId': equippedSectorChip?.instanceId,
        'currentMissionIndex': currentMissionIndex,
        'currentMissionProgress': currentMissionProgress,
        'ownedItems': ownedItems
            .map((EquipmentItem e) => <String, dynamic>{
                  'id': e.instanceId,
                  'type': e.id.name,
                  'quality': e.quality.name,
                })
            .toList(),
        'c1Lasers':
            config1LaserSlots.map((EquipmentItem? e) => e?.instanceId).toList(),
        'c1Shields':
            config1ShieldSlots.map((EquipmentItem? e) => e?.instanceId).toList(),
        'c2Lasers':
            config2LaserSlots.map((EquipmentItem? e) => e?.instanceId).toList(),
        'c2Shields':
            config2ShieldSlots.map((EquipmentItem? e) => e?.instanceId).toList(),
        'ownedVants': ownedVants
            .map((VantDrone v) => <String, dynamic>{
                  'id': v.id,
                  'tier': v.tier.name,
                  'c1Slots': v.config1Slots
                      .map((EquipmentItem? e) => e?.instanceId)
                      .toList(),
                  'c2Slots': v.config2Slots
                      .map((EquipmentItem? e) => e?.instanceId)
                      .toList(),
                })
            .toList(),
        'pilotName': pilotName,
        'companyId': pilotCompanySpec?.id,
        'currentSectorId': currentSectorId,
        'language': language.code,
        'activeShipRole': activeShipRole.name,
        'activeCraftedShipId': activeCraftedShipId,
        'nextCraftedId': _nextCraftedId,
        'basicToolsOwned': basicToolsOwned,
        'ownedCraftedShips': ownedCraftedShips
            .map((CraftedShipRecord s) => s.toJson())
            .toList(),
        'ownedCraftedItems': ownedCraftedItems
            .map((CraftedItemRecord i) => i.toJson())
            .toList(),
        'pilotSkills': <String, dynamic>{
          for (final MapEntry<SkillType, SkillProgress> e
              in pilotSkills.entries)
            e.key.name: <String, int>{'lv': e.value.level, 'xp': e.value.xp},
        },
        'rawCargo': <String, int>{
          for (final MapEntry<ResourceType, int> e in rawCargo.entries)
            if (e.value > 0) e.key.name: e.value,
        },
        'refinedCargo': <String, int>{
          for (final MapEntry<ResourceType, int> e in refinedCargo.entries)
            if (e.value > 0) e.key.name: e.value,
        },
      };
      await prefs.setString(savePrefsKey, jsonEncode(payload));
      await prefs.setString(languagePrefsKey, language.code);
      if (pilotName != null &&
          pilotName!.trim().length >= 3 &&
          pilotCompanySpec != null) {
        await prefs.setString(pilotNamePrefsKey, pilotName!.trim());
        await prefs.setString(companyIdPrefsKey, pilotCompanySpec!.id);
      } else {
        await prefs.remove(pilotNamePrefsKey);
        await prefs.remove(companyIdPrefsKey);
      }
    } catch (_) {
      // Safe fallback if SharedPreferences is unavailable in test host
    }
  }

  Future<void> _loadSavedGame() async {
    try {
      final SharedPreferences prefs = await SharedPreferences.getInstance();
      language = GameLanguageExt.fromCode(prefs.getString(languagePrefsKey));
      final String? savedNamePref = prefs.getString(pilotNamePrefsKey)?.trim();
      final PilotCompany? savedCompanyPref =
          PilotCompanyCatalog.fromId(prefs.getString(companyIdPrefsKey));
      if (pilotName == null &&
          savedNamePref != null &&
          savedNamePref.length >= 3 &&
          savedNamePref.length <= 14) {
        pilotName = savedNamePref;
      }
      pilotCompany ??= savedCompanyPref;

      final String? raw = prefs.getString(savePrefsKey);
      if (raw == null || raw.isEmpty) {
        if (pilotName != null && pilotCompany != null) {
          await saveGame();
        }
        return;
      }

      final Map<String, dynamic> data = jsonDecode(raw) as Map<String, dynamic>;
      if (data['language'] is String) {
        language = GameLanguageExt.fromCode(data['language'] as String);
      }
      final String? jsonName = (data['pilotName'] as String?)?.trim();
      final PilotCompany? jsonCompany =
          PilotCompanyCatalog.fromId(data['companyId'] as String?);
      if (pilotName == null &&
          jsonName != null &&
          jsonName.length >= 3 &&
          jsonName.length <= 14) {
        pilotName = jsonName;
      }
      pilotCompany ??= jsonCompany;
      final List<dynamic> rawItems =
          (data['ownedItems'] as List<dynamic>?) ?? <dynamic>[];
      final List<EquipmentItem> parsedItems = <EquipmentItem>[];
      for (final dynamic item in rawItems) {
        final Map<String, dynamic> m = item as Map<String, dynamic>;
        final int? id = m['id'] as int?;
        final String? typeName = m['type'] as String?;
        final String? qualityName = m['quality'] as String?;
        if (id == null || typeName == null) continue;
        EquipmentItemId? matched;
        for (final EquipmentItemId e in EquipmentItemId.values) {
          if (e.name == typeName) {
            matched = e;
            break;
          }
        }
        final CraftQuality matchedQuality = CraftQuality.values.firstWhere(
          (CraftQuality q) => q.name == qualityName,
          orElse: () => CraftQuality.standard,
        );
        if (matched != null) {
          parsedItems.add(
            EquipmentItem(instanceId: id, id: matched, quality: matchedQuality),
          );
        }
      }
      if (parsedItems.isEmpty) return;

      ownedShipClasses
        ..clear()
        ..add(ShipClass.interceptor);
      final List<dynamic>? rawShips = data['ownedShipClasses'] as List<dynamic>?;
      if (rawShips != null) {
        for (final dynamic s in rawShips) {
          if (s is String) {
            for (final ShipClass sc in ShipClass.values) {
              if (sc.name == s) {
                ownedShipClasses.add(sc);
                break;
              }
            }
          }
        }
      }

      ShipClass restoredShip = ShipClass.interceptor;
      final String? activeShipName = data['activeShipClass'] as String?;
      if (activeShipName != null) {
        for (final ShipClass sc in ownedShipClasses) {
          if (sc.name == activeShipName) {
            restoredShip = sc;
            break;
          }
        }
      }
      activeShipClass = restoredShip;
      _resizeSlotsForShip(activeShipClass);

      ownedFormations
        ..clear()
        ..add(DroneFormation.normal);
      final List<dynamic>? rawFormations =
          data['ownedFormations'] as List<dynamic>?;
      if (rawFormations != null) {
        for (final dynamic f in rawFormations) {
          if (f is String) {
            for (final DroneFormation df in DroneFormation.values) {
              if (df.name == f) {
                ownedFormations.add(df);
                break;
              }
            }
          }
        }
      }
      DroneFormation restoredFormation = DroneFormation.normal;
      final String? activeFormationName = data['activeFormation'] as String?;
      if (activeFormationName != null) {
        for (final DroneFormation df in ownedFormations) {
          if (df.name == activeFormationName) {
            restoredFormation = df;
            break;
          }
        }
      }
      activeFormation = restoredFormation;

      ownedItems
        ..clear()
        ..addAll(parsedItems);
      final Map<int, EquipmentItem> byId = <int, EquipmentItem>{
        for (final EquipmentItem e in ownedItems) e.instanceId: e,
      };

      credits = max(0, (data['credits'] as int?) ?? 0);
      kills = max(0, (data['kills'] as int?) ?? 0);
      player.level = max(1, (data['level'] as int?) ?? 1);
      player.xp = max(0, (data['xp'] as int?) ?? 0);
      activeConfig = (data['activeConfig'] as int?) == 2 ? 2 : 1;
      _nextItemInstanceId = max(
        (data['nextId'] as int?) ?? 10,
        ownedItems.fold<int>(
              0,
              (int m, EquipmentItem e) => max(m, e.instanceId),
            ) +
            1,
      );

      final Set<int> c1Used = <int>{};
      final Set<int> c2Used = <int>{};

      void restoreSlots(
        List<EquipmentItem?> target,
        List<dynamic>? rawIds,
        EquipmentCategory allowed,
        Set<int> usedIds,
      ) {
        for (int i = 0; i < target.length; i++) {
          final dynamic id =
              (rawIds != null && i < rawIds.length) ? rawIds[i] : null;
          final EquipmentItem? found = id is int ? byId[id] : null;
          if (found != null &&
              found.spec.category == allowed &&
              usedIds.add(found.instanceId)) {
            target[i] = found;
          } else {
            target[i] = null;
          }
        }
      }

      restoreSlots(
        config1LaserSlots,
        data['c1Lasers'] as List<dynamic>?,
        EquipmentCategory.laser,
        c1Used,
      );
      restoreSlots(
        config1ShieldSlots,
        data['c1Shields'] as List<dynamic>?,
        EquipmentCategory.shieldGenerator,
        c1Used,
      );
      restoreSlots(
        config2LaserSlots,
        data['c2Lasers'] as List<dynamic>?,
        EquipmentCategory.laser,
        c2Used,
      );
      restoreSlots(
        config2ShieldSlots,
        data['c2Shields'] as List<dynamic>?,
        EquipmentCategory.shieldGenerator,
        c2Used,
      );

      ownedVants.clear();
      final List<dynamic>? rawVants = data['ownedVants'] as List<dynamic>?;
      if (rawVants != null) {
        for (final dynamic rv in rawVants) {
          if (ownedVants.length >= GameConstants.maxOwnedVantsForFusion) break;
          if (rv is! Map<String, dynamic>) continue;
          final String? tierName = rv['tier'] as String?;
          VantTier? matchedTier;
          for (final VantTier t in VantTier.values) {
            if (t.name == tierName) {
              matchedTier = t;
              break;
            }
          }
          if (matchedTier == null) continue;
          final int vId = max(1, (rv['id'] as int?) ?? (ownedVants.length + 1));
          final VantDrone drone = VantDrone(id: vId, tier: matchedTier);

          final List<dynamic>? rawC1 = rv['c1Slots'] as List<dynamic>?;
          for (int i = 0; i < drone.config1Slots.length; i++) {
            final dynamic itemId =
                (rawC1 != null && i < rawC1.length) ? rawC1[i] : null;
            final EquipmentItem? found = itemId is int ? byId[itemId] : null;
            if (found != null &&
                _isVantCompatibleItem(found.id) &&
                c1Used.add(found.instanceId)) {
              drone.config1Slots[i] = found;
            }
          }
          final List<dynamic>? rawC2 = rv['c2Slots'] as List<dynamic>?;
          for (int i = 0; i < drone.config2Slots.length; i++) {
            final dynamic itemId =
                (rawC2 != null && i < rawC2.length) ? rawC2[i] : null;
            final EquipmentItem? found = itemId is int ? byId[itemId] : null;
            if (found != null &&
                _isVantCompatibleItem(found.id) &&
                c2Used.add(found.instanceId)) {
              drone.config2Slots[i] = found;
            }
          }
          ownedVants.add(drone);
        }
      }
      _clearReserveVantSlots();
      _nextVantId = max(
        (data['nextVantId'] as int?) ?? 1,
        ownedVants.fold<int>(0, (int m, VantDrone v) => max(m, v.id)) + 1,
      );

      final dynamic chipId = data['equippedChipId'];
      final EquipmentItem? chip = chipId is int ? byId[chipId] : null;
      equippedSectorChip =
          (chip != null && chip.spec.category == EquipmentCategory.sectorChip)
              ? chip
              : null;

      // Failsafe: if loaded save has no Chip equipped AND none in Inventory, grant & equip 1x Chip Lv 1
      final bool hasAnyChip = equippedSectorChip != null ||
          ownedItems.any(
            (EquipmentItem e) => e.spec.category == EquipmentCategory.sectorChip,
          );
      if (!hasAnyChip) {
        final EquipmentItem fallbackChip = EquipmentItem(
          instanceId: _nextItemInstanceId++,
          id: EquipmentItemId.chip1,
        );
        ownedItems.add(fallbackChip);
        equippedSectorChip = fallbackChip;
      }

      final int rawMissionIdx = (data['currentMissionIndex'] as int?) ?? 0;
      currentMissionIndex = rawMissionIdx.clamp(0, MissionCatalog.all.length);
      final MissionSpec? m = activeMission;
      final int rawProg = (data['currentMissionProgress'] as int?) ?? 0;
      currentMissionProgress = m != null ? rawProg.clamp(0, m.targetCount) : 0;

      final String? savedSector = data['currentSectorId'] as String?;
      if (savedSector != null && galaxyMapData.containsKey(savedSector)) {
        currentSectorId = savedSector;
      } else {
        currentSectorId = GalaxyMapGraph.homeCityForCompany(pilotCompany);
      }
      activePortals = GalaxyMapGraph.portalsForSector(currentSectorId);
      plottedRoute = <String>[];

      // Restore v1.10+ Crafted Ships, Items, Pilot Skills, Cargo & Active Ship Role
      ownedCraftedShips.clear();
      final List<dynamic>? rawCraftedShips =
          data['ownedCraftedShips'] as List<dynamic>?;
      if (rawCraftedShips != null) {
        for (final dynamic item in rawCraftedShips) {
          if (item is Map<String, dynamic>) {
            final CraftedShipRecord? rec = CraftedShipRecord.fromJson(item);
            if (rec != null) ownedCraftedShips.add(rec);
          }
        }
      }
      ownedCraftedItems.clear();
      final List<dynamic>? rawCraftedItems =
          data['ownedCraftedItems'] as List<dynamic>?;
      if (rawCraftedItems != null) {
        for (final dynamic item in rawCraftedItems) {
          if (item is Map<String, dynamic>) {
            final CraftedItemRecord? rec = CraftedItemRecord.fromJson(item);
            if (rec != null) ownedCraftedItems.add(rec);
          }
        }
      }
      _nextCraftedId = max(
        (data['nextCraftedId'] as int?) ?? 1,
        max(
              ownedCraftedShips.fold<int>(
                0,
                (int m, CraftedShipRecord s) => max(m, s.instanceId),
              ),
              ownedCraftedItems.fold<int>(
                0,
                (int m, CraftedItemRecord i) => max(m, i.instanceId),
              ),
            ) +
            1,
      );
      basicToolsOwned = max(0, (data['basicToolsOwned'] as int?) ?? 0);

      final Map<String, dynamic>? rawSkills =
          data['pilotSkills'] as Map<String, dynamic>?;
      if (rawSkills != null) {
        for (final SkillType s in SkillType.values) {
          final dynamic entry = rawSkills[s.name];
          if (entry is Map<String, dynamic>) {
            final int lv = ((entry['lv'] as num?)?.toInt() ?? 1)
                .clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
            final int sxp = max(0, (entry['xp'] as num?)?.toInt() ?? 0);
            pilotSkills[s] = SkillProgress(level: lv, xp: sxp);
          }
        }
      }

      rawCargo.clear();
      final Map<String, dynamic>? savedRaw =
          data['rawCargo'] as Map<String, dynamic>?;
      if (savedRaw != null) {
        for (final ResourceType r in ResourceType.values) {
          final int qty = (savedRaw[r.name] as num?)?.toInt() ?? 0;
          if (qty > 0) rawCargo[r] = qty;
        }
      }
      refinedCargo.clear();
      final Map<String, dynamic>? savedRef =
          data['refinedCargo'] as Map<String, dynamic>?;
      if (savedRef != null) {
        for (final ResourceType r in ResourceType.values) {
          final int qty = (savedRef[r.name] as num?)?.toInt() ?? 0;
          if (qty > 0) refinedCargo[r] = qty;
        }
      }

      final String? savedRole = data['activeShipRole'] as String?;
      activeShipRole = ShipRole.values.firstWhere(
        (ShipRole r) => r.name == savedRole,
        orElse: () => ShipRole.capsule,
      );
      activeCraftedShipId = data['activeCraftedShipId'] as int?;
      if (activeShipRole == ShipRole.industrial && activeCraftedShip == null) {
        activeShipRole = ShipRole.capsule;
        activeCraftedShipId = null;
      }
    } catch (_) {
      // Keep starter state on corrupt save
    }
  }

  Future<void> resetSave() async {
    try {
      final SharedPreferences prefs = await SharedPreferences.getInstance();
      await prefs.remove(savePrefsKey);
      await prefs.remove(pilotNamePrefsKey);
      await prefs.remove(companyIdPrefsKey);
    } catch (_) {
      // Ignore if SharedPreferences unavailable
    }
    pilotName = null;
    pilotCompany = null;
    isAdminMode = false;
    adminBannerText = null;
    adminBannerTimer = 0.0;
    cameraZoom = kCameraZoomDefault;
    camera.viewfinder.zoom = cameraZoom;
    _initStarterState();
    currentSectorId = GalaxyMapGraph.defaultSectorId;
    activePortals = GalaxyMapGraph.portalsForSector(currentSectorId);
    plottedRoute = <String>[];
    warpFlashTimer = 0.0;
    player
      ..position.setFrom(activeSafeZoneCenter)
      ..hp = currentMaxHull
      ..shield = currentMaxShield
      ..rocketCooldown = 0.0;
    autoFireEnabled = false;
    lockedTarget = null;
    autopilotTarget = null;
    formationSwitchCooldown = 0.0;
    isFormationMenuOpen = false;
    _clearActiveMapEntities();
    _populateActiveMapEntities();
    _snapVantsToFormation();
    camera.follow(player, snap: true);
    closeAllModalOverlays();
    resumeEngine();
    notifyUi();
    onResetToIntro?.call();
  }

  void _clearActiveMapEntities() {
    for (final AlienShipComponent a in List<AlienShipComponent>.of(aliens)) {
      a.removeFromParent();
    }
    aliens.clear();
    for (final LootBoxComponent b in List<LootBoxComponent>.of(lootBoxes)) {
      b.removeFromParent();
    }
    lootBoxes.clear();
    resourceNodes.clear();
    alienWrecks.clear();
    activeMiningNode = null;
    activeSalvageWreck = null;
    for (final Component child in List<Component>.of(world.children)) {
      if (child is LaserBoltComponent ||
          child is HomingRocketComponent ||
          child is ExplosionParticleComponent ||
          child is ShockwaveRingComponent ||
          child is FloatingTextComponent) {
        child.removeFromParent();
      }
    }
  }

  void _buildActiveSectorStations() {
    final SectorNode sector = currentSector;
    final Vector2 c = activeSafeZoneCenter;
    if (sector.isCapitalCity) {
      activeStations = <SectorStation>[
        SectorStation(
          kind: StationKind.refinery,
          sectorId: sector.id,
          name: '${sector.id} REFINERY',
          worldX: c.x - 210.0,
          worldY: c.y - 110.0,
          accentColor: const Color(0xFFFB923C),
        ),
        SectorStation(
          kind: StationKind.crafting,
          sectorId: sector.id,
          name: '${sector.id} CRAFTING',
          worldX: c.x + 210.0,
          worldY: c.y - 110.0,
          accentColor: const Color(0xFF34D399),
        ),
        SectorStation(
          kind: StationKind.localMarket,
          sectorId: sector.id,
          name: '${sector.id} MARKET',
          worldX: c.x,
          worldY: c.y + 185.0,
          accentColor: const Color(0xFFFACC15),
        ),
      ];
    } else if (sector.isNucleus) {
      activeStations = <SectorStation>[
        SectorStation(
          kind: StationKind.localMarket,
          sectorId: sector.id,
          name: '0-0 BLACK MARKET',
          worldX: c.x,
          worldY: c.y + 140.0,
          accentColor: const Color(0xFFFACC15),
        ),
      ];
    } else {
      activeStations = <SectorStation>[];
    }
  }

  String _regionCityForSector(SectorNode sector) {
    final String id = sector.id;
    if (id == '0-0' || id.startsWith('Y')) return '0-0';
    if (id.startsWith('C1') || id.startsWith('1-') || id.startsWith('R1')) {
      return 'C1';
    }
    if (id.startsWith('C2') || id.startsWith('2-') || id.startsWith('R2')) {
      return 'C2';
    }
    if (id.startsWith('C3') || id.startsWith('3-') || id.startsWith('R3')) {
      return 'C3';
    }
    return 'C1';
  }

  void _spawnResourceNodesForSector(SectorNode sector) {
    if (sector.isEntireSectorSafe) return;
    final String region = _regionCityForSector(sector);
    final int count = sector.isNucleus ? 26 : 18;
    for (int i = 0; i < count; i++) {
      final int nodeTier = (i % 2 == 0) ? 1 : 2;
      final ResourceType resType =
          ResourceCatalog.forRegionAndTier(region, nodeTier);
      final Vector2 pos = randomPointInActiveSector(
        margin: 340.0,
        safeZoneBuffer: 260.0,
      );
      final int maxCh = 4 + rng.nextInt(2); // 4-5 charges
      resourceNodes.add(
        ResourceNodeEntity(
          id: _nextWorldEntityId++,
          resourceType: resType,
          x: pos.x,
          y: pos.y,
          radius: resType == ResourceType.yellowCrystal ? 28.0 : 22.0,
          rotationRad: rng.nextDouble() * 2.0 * pi,
          pulsePhase: rng.nextDouble() * 2.0 * pi,
          maxCharges: maxCh,
        ),
      );
    }
  }

  void _populateActiveMapEntities() {
    _buildActiveSectorStations();
    final SectorNode sector = currentSector;
    // Capital Cities (C1, C2, C3) are full Safe Zones (Base, Market, Refinery, Crafting) with zero hostile aliens
    if (sector.isEntireSectorSafe) {
      for (int i = 0; i < 16; i++) {
        spawnBonusBox();
      }
      return;
    }

    _spawnResourceNodesForSector(sector);

    switch (sector.zoneType) {
      case ZoneType.capitalCity:
        break;
      case ZoneType.safeZone:
        for (int i = 0; i < 20; i++) {
          spawnAlien(AlienTier.scout, minDistFromPlayer: 640.0);
        }
        for (int i = 0; i < 8; i++) {
          spawnAlien(AlienTier.hunter, minDistFromPlayer: 760.0);
        }
        for (int i = 0; i < 42; i++) {
          spawnBonusBox();
        }
        break;
      case ZoneType.intermediateZone:
        for (int i = 0; i < 18; i++) {
          spawnAlien(AlienTier.scout, minDistFromPlayer: 640.0);
        }
        for (int i = 0; i < 16; i++) {
          spawnAlien(AlienTier.hunter, minDistFromPlayer: 700.0);
        }
        for (int i = 0; i < 46; i++) {
          spawnBonusBox();
        }
        break;
      case ZoneType.moderateZone:
        for (int i = 0; i < 16; i++) {
          spawnAlien(AlienTier.hunter, minDistFromPlayer: 700.0);
        }
        for (int i = 0; i < 16; i++) {
          spawnAlien(AlienTier.destroyer, minDistFromPlayer: 760.0);
        }
        for (int i = 0; i < 50; i++) {
          spawnBonusBox();
        }
        break;
      case ZoneType.highRiskYellowRing:
        for (int i = 0; i < 16; i++) {
          spawnAlien(AlienTier.destroyer, minDistFromPlayer: 760.0);
        }
        for (int i = 0; i < 12; i++) {
          spawnAlien(AlienTier.dreadnoughtBoss, minDistFromPlayer: 840.0);
        }
        for (int i = 0; i < 58; i++) {
          spawnBonusBox();
        }
        break;
      case ZoneType.nucleus:
        // Large 22,000 x 22,000 High-Risk sector with Central Black Market Safe Zone
        for (int i = 0; i < 22; i++) {
          spawnAlien(AlienTier.destroyer, minDistFromPlayer: 820.0);
        }
        for (int i = 0; i < 18; i++) {
          spawnAlien(AlienTier.dreadnoughtBoss, minDistFromPlayer: 900.0);
        }
        for (int i = 0; i < 80; i++) {
          spawnBonusBox();
        }
        break;
    }
  }

  void triggerJumpGate() {
    final SectorPortal? portal = nearestJumpPortal;
    if (portal == null) return;
    jumpToSector(portal.targetSectorId, arrivedFromSectorId: currentSectorId);
  }

  void jumpToSector(String targetSectorId, {String? arrivedFromSectorId}) {
    if (!galaxyMapData.containsKey(targetSectorId)) return;

    // Cancel Autopilot, combat lock, auto-fire, and close map overlays
    autopilotTarget = null;
    lockedTarget = null;
    autoFireEnabled = false;
    overlays.remove(tacticalMapOverlayId);
    overlays.remove(galaxyMapOverlayId);

    final String prevId = arrivedFromSectorId ?? currentSectorId;
    currentSectorId = targetSectorId;
    activePortals = GalaxyMapGraph.portalsForSector(currentSectorId);
    warpFlashTimer = GameConstants.warpFlashDurationSec;

    // Advance or recalculate active plotted BFS route
    if (plottedRoute.length >= 2) {
      if (plottedRoute.last == currentSectorId) {
        plottedRoute = <String>[];
      } else if (plottedRoute[1] == currentSectorId) {
        plottedRoute = plottedRoute.sublist(1);
      } else {
        plottedRoute = GalaxyMapGraph.findShortestRoute(
          currentSectorId,
          plottedRoute.last,
        );
      }
    }

    // Arrive beside the matching return portal (offset inward slightly) or at city base center
    final SectorNode dest = currentSector;
    if (dest.isCapitalCity) {
      player.position.setFrom(activeSafeZoneCenter);
      player.angle = -pi * 0.5;
    } else {
      SectorPortal? arrivalPortal;
      for (final SectorPortal p in activePortals) {
        if (p.targetSectorId == prevId) {
          arrivalPortal = p;
          break;
        }
      }
      if (arrivalPortal != null) {
        final double cx = dest.mapSize * 0.5;
        final double cy = dest.mapSize * 0.5;
        final double dx = cx - arrivalPortal.worldX;
        final double dy = cy - arrivalPortal.worldY;
        final double dist = max(1.0, sqrt(dx * dx + dy * dy));
        // Offset 150 units inward so the ship arrives clear of immediate bounce-back
        player.position.setValues(
          arrivalPortal.worldX + (dx / dist) * 150.0,
          arrivalPortal.worldY + (dy / dist) * 150.0,
        );
        player.angle = atan2(dy, dx);
      } else {
        player.position.setFrom(activeSafeZoneCenter);
        player.angle = -pi * 0.5;
      }
    }

    player.thrustFactor = 0.0;
    player.isTakingZoneRadiation = false;
    player.requiredChipWarning = null;
    player.radiationTickTimer = 0.0;

    _clearActiveMapEntities();
    _populateActiveMapEntities();
    _snapVantsToFormation();
    camera.follow(player, snap: true);
    saveGame();

    world.add(
      ShockwaveRingComponent(
        position: player.position.clone(),
        maxRadius: 155.0,
        duration: 0.45,
        color: dest.nodeColor,
      ),
    );
    spawnFloatingText(
      position: player.position + Vector2(0.0, -activeHullRadius - 32.0),
      text: 'SECTOR ${dest.id} • ${dest.zoneType.shortLabel}',
      color: dest.nodeColor,
      fontSize: 14.5,
    );
    notifyUi();
  }

  SectorStation? get nearestStation {
    if (activeStations.isEmpty) return null;
    SectorStation? closest;
    double bestDist = double.infinity;
    for (final SectorStation st in activeStations) {
      final double d =
          hypot(player.position.x - st.worldX, player.position.y - st.worldY);
      if (d < bestDist) {
        bestDist = d;
        closest = st;
      }
    }
    if (closest == null) return null;
    if (bestDist <= closest.interactRadius ||
        currentSector.isCapitalCity ||
        (currentSector.isNucleus && player.inSafeZone)) {
      return closest;
    }
    return null;
  }

  double hypot(double dx, double dy) => sqrt(dx * dx + dy * dy);

  @override
  void update(double dt) {
    super.update(dt);
    elapsedTime += dt;
    if (formationSwitchCooldown > 0.0) {
      formationSwitchCooldown = max(0.0, formationSwitchCooldown - dt);
    }
    if (warpFlashTimer > 0.0) {
      warpFlashTimer = max(0.0, warpFlashTimer - dt);
    }
    if (_combatMiningWarnCooldown > 0.0) {
      _combatMiningWarnCooldown = max(0.0, _combatMiningWarnCooldown - dt);
    }
    if (adminBannerTimer > 0.0) {
      adminBannerTimer = max(0.0, adminBannerTimer - dt);
      if (adminBannerTimer <= 0.0) {
        adminBannerText = null;
        notifyUi();
      }
    }

    if (aimJoystick.relativeDelta.length > 0.12) {
      final double aimAngle = atan2(
        aimJoystick.relativeDelta.y,
        aimJoystick.relativeDelta.x,
      );
      _lockClosestAlienInRadarCone(aimAngle);
    }

    _updateExtractionAndWrecks(dt);
    _updateVantFormationPositions(dt);

    _uiHudTickAccum += dt;
    if (_uiHudTickAccum >= 0.2) {
      _uiHudTickAccum = 0.0;
      if (activeStations.isNotEmpty ||
          activeMiningNode != null ||
          activeSalvageWreck != null ||
          adminBannerTimer > 0.0) {
        notifyUi();
      }
    }
  }

  void _updateExtractionAndWrecks(double dt) {
    // 1. Update & expire alien wrecks
    for (final AlienWreckEntity wreck in List<AlienWreckEntity>.of(alienWrecks)) {
      wreck.expirationTimerSec -= dt;
      wreck.pulsePhase += dt * 3.0;
      if (wreck.isDepleted) {
        if (activeSalvageWreck == wreck) activeSalvageWreck = null;
        alienWrecks.remove(wreck);
      }
    }

    // 2. Update & auto-recover player ship wrecks in current sector
    for (final PlayerShipWreck wreck
        in List<PlayerShipWreck>.of(playerWrecks)) {
      wreck.expirationTimerSec -= dt;
      if (wreck.expirationTimerSec <= 0.0 || wreck.isEmpty) {
        playerWrecks.remove(wreck);
        continue;
      }
      if (wreck.sectorId != currentSectorId) continue;
      final double dist =
          hypot(player.position.x - wreck.x, player.position.y - wreck.y);
      if (dist <= 85.0) {
        int recoveredUnits = 0;
        for (final ResourceType res in List<ResourceType>.of(wreck.rawCargo.keys)) {
          final int avail = wreck.rawCargo[res] ?? 0;
          if (avail <= 0) continue;
          final int take = min(avail, remainingCargoCapacity);
          if (take > 0) {
            rawCargo[res] = (rawCargo[res] ?? 0) + take;
            wreck.rawCargo[res] = avail - take;
            recoveredUnits += take;
          }
        }
        for (final ResourceType res
            in List<ResourceType>.of(wreck.refinedCargo.keys)) {
          final int avail = wreck.refinedCargo[res] ?? 0;
          if (avail <= 0) continue;
          final int take = min(avail, remainingCargoCapacity);
          if (take > 0) {
            refinedCargo[res] = (refinedCargo[res] ?? 0) + take;
            wreck.refinedCargo[res] = avail - take;
            recoveredUnits += take;
          }
        }
        int recoveredGear = 0;
        if (wreck.droppedEquipment.isNotEmpty) {
          ownedItems.addAll(wreck.droppedEquipment);
          recoveredGear = wreck.droppedEquipment.length;
          wreck.droppedEquipment.clear();
        }
        if (recoveredUnits > 0 || recoveredGear > 0) {
          spawnFloatingText(
            position: player.position + Vector2(0.0, -activeHullRadius - 26.0),
            text:
                '${GameStrings.t(language, 'WRECK RECOVERED', 'PECIO RECUPERADO')} +$recoveredUnits${recoveredGear > 0 ? ' • +$recoveredGear GEAR' : ''}',
            color: const Color(0xFF4ADE80),
            fontSize: 13.5,
          );
          if (wreck.isEmpty) {
            playerWrecks.remove(wreck);
          }
          saveGame();
          notifyUi();
        }
      }
    }

    // 3. Auto-detect nearby Alien Wreck or Resource Node for extraction when within range
    const double extractRange = 155.0;
    if (autoFireEnabled || player.inSafeZone) {
      activeMiningNode = null;
      activeSalvageWreck = null;
      return;
    }

    // Salvaging Alien Wrecks (all ship roles can salvage)
    if (activeSalvageWreck != null) {
      final AlienWreckEntity w = activeSalvageWreck!;
      if (w.isDepleted ||
          hypot(player.position.x - w.x, player.position.y - w.y) >
              extractRange + 25.0) {
        w.salvageProgress = 0.0;
        activeSalvageWreck = null;
      }
    }
    if (activeSalvageWreck == null) {
      for (final AlienWreckEntity w in alienWrecks) {
        if (hypot(player.position.x - w.x, player.position.y - w.y) <=
            extractRange) {
          activeSalvageWreck = w;
          break;
        }
      }
    }
    if (activeSalvageWreck != null) {
      final AlienWreckEntity w = activeSalvageWreck!;
      final double secPerCharge =
          baseExtractionSeconds(skillLevelOf(SkillType.salvaging));
      w.salvageProgress += dt / max(0.1, secPerCharge);
      if (w.salvageProgress >= 1.0) {
        w.salvageProgress = 0.0;
        if (remainingCargoCapacity > 0) {
          rawCargo[ResourceType.xenoSalvage] =
              (rawCargo[ResourceType.xenoSalvage] ?? 0) + 1;
          final bool coreDrop =
              rng.nextDouble() < (w.fromBoss ? 0.35 : 0.15) &&
                  remainingCargoCapacity > 0;
          if (coreDrop) {
            rawCargo[ResourceType.xenoCore] =
                (rawCargo[ResourceType.xenoCore] ?? 0) + 1;
          }
          w.charges -= 1;
          addSkillXp(SkillType.salvaging, w.fromBoss ? 65 : 35);
          spawnFloatingText(
            position: Vector2(w.x, w.y - w.radius - 14.0),
            text: coreDrop
                ? '+1 ${GameStrings.resourceName(ResourceType.xenoSalvage, language)} +1 CORE!'
                : '+1 ${GameStrings.resourceName(ResourceType.xenoSalvage, language)}',
            color: const Color(0xFFC084FC),
            fontSize: 12.5,
          );
          if (w.isDepleted) {
            alienWrecks.remove(w);
            activeSalvageWreck = null;
          }
          saveGame();
          notifyUi();
        } else {
          spawnFloatingText(
            position: player.position + Vector2(0.0, -activeHullRadius - 24.0),
            text: GameStrings.t(language, 'CARGO FULL', 'BODEGA LLENA'),
            color: const Color(0xFFFBBF24),
            fontSize: 12.5,
          );
          activeSalvageWreck = null;
        }
      }
      return;
    }

    // Mining Resource Nodes by Charges (Capsule & Industrial ships only)
    if (activeMiningNode != null) {
      final ResourceNodeEntity n = activeMiningNode!;
      if (n.isDepleted ||
          hypot(player.position.x - n.x, player.position.y - n.y) >
              extractRange + 25.0) {
        n.harvestProgress = 0.0;
        activeMiningNode = null;
      }
    }
    if (activeMiningNode == null) {
      for (final ResourceNodeEntity n in resourceNodes) {
        if (hypot(player.position.x - n.x, player.position.y - n.y) <=
            extractRange) {
          if (activeShipRole == ShipRole.combat) {
            if (_combatMiningWarnCooldown <= 0.0) {
              _combatMiningWarnCooldown = 3.5;
              spawnFloatingText(
                position:
                    player.position + Vector2(0.0, -activeHullRadius - 26.0),
                text: GameStrings.t(
                  language,
                  'Combat ships cannot mine (Use Capsule/Industrial)',
                  'Naves de combate no minan (Usa Cápsula/Industrial)',
                ),
                color: const Color(0xFFFBBF24),
                fontSize: 12.0,
              );
            }
            break;
          }
          activeMiningNode = n;
          break;
        }
      }
    }
    if (activeMiningNode != null) {
      if (activeShipRole == ShipRole.combat) {
        activeMiningNode = null;
        return;
      }
      final ResourceNodeEntity n = activeMiningNode!;
      final ResourceSpec spec = ResourceCatalog.of(n.resourceType);
      final double secPerCharge = effectiveExtractionSeconds(
        skillLevel: skillLevelOf(SkillType.mining),
        category: spec.category,
        shipRole: activeShipRole,
        industrialType: activeIndustrialType,
      );
      n.harvestProgress += dt / max(0.1, secPerCharge);
      if (n.harvestProgress >= 1.0) {
        n.harvestProgress = 0.0;
        if (remainingCargoCapacity > 0) {
          rawCargo[n.resourceType] = (rawCargo[n.resourceType] ?? 0) + 1;
          n.charges -= 1;
          addSkillXp(
            SkillType.mining,
            spec.isYellowCrystal ? 85 : (spec.tier >= 2 ? 42 : 28),
          );
          spawnFloatingText(
            position: Vector2(n.x, n.y - n.radius - 14.0),
            text: '+1 ${GameStrings.resourceName(n.resourceType, language)}',
            color: spec.color,
            fontSize: 12.5,
          );
          if (n.isDepleted) {
            resourceNodes.remove(n);
            activeMiningNode = null;
          }
          saveGame();
          notifyUi();
        } else {
          spawnFloatingText(
            position: player.position + Vector2(0.0, -activeHullRadius - 24.0),
            text: GameStrings.t(language, 'CARGO FULL', 'BODEGA LLENA'),
            color: const Color(0xFFFBBF24),
            fontSize: 12.5,
          );
          activeMiningNode = null;
        }
      }
    }
  }

  void addSkillXp(SkillType skill, int amount) {
    if (amount <= 0) return;
    final SkillProgress before = skillProgressOf(skill);
    final SkillProgress after = before.addXp(amount);
    pilotSkills[skill] = after;
    if (after.level > before.level) {
      spawnFloatingText(
        position: player.position + Vector2(0.0, -activeHullRadius - 42.0),
        text:
            '${GameStrings.skillName(skill, language).toUpperCase()} LV ${after.level}!',
        color: skill.accentColor,
        fontSize: 14.5,
      );
      _syncPlayerVitalsToStats();
    }
  }

  void setLanguage(GameLanguage lang) {
    if (language == lang) return;
    language = lang;
    saveGame();
    notifyUi();
  }

  // ---------------------------------------------------------------------------
  // Admin / Debug Mode (Free Testing) & Pinch-to-Zoom Camera
  // ---------------------------------------------------------------------------
  void toggleAdminMode() {
    isAdminMode = !isAdminMode;
    if (isAdminMode) {
      adminBannerText = GameStrings.adminActiveBanner(language);
      adminBannerTimer = 3.2;
    } else {
      adminBannerText = null;
      adminBannerTimer = 0.0;
    }
    notifyUi();
  }

  void adminSetSkillLevel(SkillType skill, int targetLevel) {
    if (!isAdminMode) return;
    final int clamped =
        targetLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
    pilotSkills[skill] = SkillProgress(level: clamped, xp: 0);
    _syncPlayerVitalsToStats();
    saveGame();
    notifyUi();
  }

  void adminCycleSkillLevel(SkillType skill) {
    if (!isAdminMode) return;
    final int cur = skillLevelOf(skill);
    final int next = cur < 33
        ? 33
        : (cur < 66 ? 66 : (cur < 99 ? 99 : 1));
    adminSetSkillLevel(skill, next);
  }

  void adminSetAllSkillsLevel(int targetLevel) {
    if (!isAdminMode) return;
    final int clamped =
        targetLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
    for (final SkillType s in SkillType.values) {
      pilotSkills[s] = SkillProgress(level: clamped, xp: 0);
    }
    _syncPlayerVitalsToStats();
    saveGame();
    notifyUi();
  }

  void setCameraZoom(double zoom) {
    final double clamped = zoom.clamp(kCameraZoomMin, kCameraZoomMax);
    if ((clamped - cameraZoom).abs() < 0.002) return;
    cameraZoom = clamped;
    camera.viewfinder.zoom = cameraZoom;
    notifyUi();
  }

  void resetCameraZoom() {
    setCameraZoom(kCameraZoomDefault);
  }

  // ---------------------------------------------------------------------------
  // Overlay Management (Unified Menu, Skills, Hangar, Inventory, Guide, Settings, Station, Death)
  // ---------------------------------------------------------------------------
  void closeAllModalOverlays() {
    overlays.remove(hangarOverlayId);
    overlays.remove(tacticalMapOverlayId);
    overlays.remove(galaxyMapOverlayId);
    overlays.remove(menuOverlayId);
    overlays.remove(skillsOverlayId);
    overlays.remove(inventoryOverlayId);
    overlays.remove(guideOverlayId);
    overlays.remove(settingsOverlayId);
    overlays.remove(stationOverlayId);
    overlays.remove(deathOverlayId);
    notifyUi();
  }

  void openMenu() {
    isFormationMenuOpen = false;
    closeAllModalOverlays();
    pauseEngine();
    overlays.add(menuOverlayId);
    notifyUi();
  }

  void closeMenu() {
    overlays.remove(menuOverlayId);
    resumeEngine();
    notifyUi();
  }

  void openSkills() {
    isFormationMenuOpen = false;
    closeAllModalOverlays();
    pauseEngine();
    overlays.add(skillsOverlayId);
    notifyUi();
  }

  void closeSkills() {
    overlays.remove(skillsOverlayId);
    resumeEngine();
    notifyUi();
  }

  void openInventory() {
    isFormationMenuOpen = false;
    closeAllModalOverlays();
    pauseEngine();
    overlays.add(inventoryOverlayId);
    notifyUi();
  }

  void closeInventory() {
    overlays.remove(inventoryOverlayId);
    resumeEngine();
    notifyUi();
  }

  void openGuide() {
    isFormationMenuOpen = false;
    closeAllModalOverlays();
    pauseEngine();
    overlays.add(guideOverlayId);
    notifyUi();
  }

  void closeGuide() {
    overlays.remove(guideOverlayId);
    resumeEngine();
    notifyUi();
  }

  void openSettings() {
    isFormationMenuOpen = false;
    closeAllModalOverlays();
    pauseEngine();
    overlays.add(settingsOverlayId);
    notifyUi();
  }

  void closeSettings() {
    overlays.remove(settingsOverlayId);
    resumeEngine();
    notifyUi();
  }

  void openStation({StationKind? initialTab}) {
    final SectorStation? st = nearestStation;
    if (st == null && !currentSector.isCapitalCity && !currentSector.isNucleus) {
      return;
    }
    isFormationMenuOpen = false;
    closeAllModalOverlays();
    if (currentSector.isNucleus) {
      openStationTab = StationKind.localMarket;
    } else {
      openStationTab = initialTab ?? st?.kind ?? StationKind.refinery;
    }
    lastCraftOutcome = null;
    pauseEngine();
    overlays.add(stationOverlayId);
    notifyUi();
  }

  void closeStation() {
    overlays.remove(stationOverlayId);
    lastCraftOutcome = null;
    _syncPlayerVitalsToStats();
    saveGame();
    resumeEngine();
    notifyUi();
  }

  void switchStationTab(StationKind tab) {
    if (currentSector.isNucleus && tab != StationKind.localMarket) return;
    openStationTab = tab;
    notifyUi();
  }

  void dismissDeathOverlay() {
    overlays.remove(deathOverlayId);
    lastDeathSummary = null;
    resumeEngine();
    notifyUi();
  }

  void openHangar() {
    isFormationMenuOpen = false;
    closeAllModalOverlays();
    pauseEngine();
    overlays.add(hangarOverlayId);
    notifyUi();
  }

  void closeHangar() {
    _syncPlayerVitalsToStats();
    saveGame();
    overlays.remove(hangarOverlayId);
    resumeEngine();
    notifyUi();
  }

  void openTacticalMap() {
    if (overlays.isActive(hangarOverlayId) ||
        overlays.isActive(stationOverlayId) ||
        overlays.isActive(menuOverlayId)) {
      return;
    }
    isFormationMenuOpen = false;
    overlays.remove(galaxyMapOverlayId);
    overlays.add(tacticalMapOverlayId);
    notifyUi();
  }

  void closeTacticalMap() {
    overlays.remove(tacticalMapOverlayId);
    notifyUi();
  }

  void toggleTacticalMap() {
    if (overlays.isActive(tacticalMapOverlayId)) {
      closeTacticalMap();
    } else {
      openTacticalMap();
    }
  }

  void openGalaxyMap() {
    if (overlays.isActive(hangarOverlayId) ||
        overlays.isActive(stationOverlayId) ||
        overlays.isActive(menuOverlayId)) {
      return;
    }
    isFormationMenuOpen = false;
    overlays.remove(tacticalMapOverlayId);
    overlays.add(galaxyMapOverlayId);
    notifyUi();
  }

  void closeGalaxyMap() {
    overlays.remove(galaxyMapOverlayId);
    notifyUi();
  }

  void toggleGalaxyMap() {
    if (overlays.isActive(galaxyMapOverlayId)) {
      closeGalaxyMap();
    } else {
      openGalaxyMap();
    }
  }

  // ---------------------------------------------------------------------------
  // Permanent Capsule, Industrial Fleet & Combat Ship Switching
  // ---------------------------------------------------------------------------
  bool get canSwapShipsHere => player.inSafeZone || isAdminMode;

  bool selectCapsule() {
    if (!canSwapShipsHere) return false;
    if (activeShipRole == ShipRole.capsule) return false;
    activeShipRole = ShipRole.capsule;
    activeCraftedShipId = null;
    activeMiningNode = null;
    _syncPlayerVitalsToStats();
    player.hp = currentMaxHull;
    player.shield = currentMaxShield;
    saveGame();
    notifyUi();
    return true;
  }

  bool selectCraftedShip(int instanceId) {
    if (!canSwapShipsHere) return false;
    CraftedShipRecord? target;
    for (final CraftedShipRecord s in ownedCraftedShips) {
      if (s.instanceId == instanceId) {
        target = s;
        break;
      }
    }
    if (target == null) return false;
    activeShipRole = ShipRole.industrial;
    activeCraftedShipId = target.instanceId;
    activeMiningNode = null;
    _syncPlayerVitalsToStats();
    player.hp = currentMaxHull;
    player.shield = currentMaxShield;
    saveGame();
    notifyUi();
    return true;
  }

  // ---------------------------------------------------------------------------
  // Clockwise Refinery, Station Crafting (Qualities), Local Market & Basic Vendor
  // ---------------------------------------------------------------------------
  int refineResource(ResourceType res, int requestedUnits) {
    if (requestedUnits <= 0) return 0;
    final ResourceSpec spec = ResourceCatalog.of(res);
    if (!spec.isRefinable) return 0;
    if (!isAdminMode && !spec.canRefineIn(currentSectorId)) return 0;

    final int refiningLv = skillLevelOf(SkillType.refining);
    final int feePerUnit =
        isAdminMode ? 0 : spec.effectiveRefiningFeePerUnit(refiningLv);
    final int maxUnits;
    if (isAdminMode) {
      maxUnits = requestedUnits;
    } else {
      final int ownedRaw = rawCargo[res] ?? 0;
      final int byRaw = ownedRaw ~/ ResourceCatalog.rawRequiredPerRefine;
      final int byCredits = feePerUnit > 0 ? (credits ~/ feePerUnit) : byRaw;
      maxUnits = min(requestedUnits, min(byRaw, byCredits));
    }
    if (maxUnits <= 0) return 0;

    if (!isAdminMode) {
      final int consumedRaw = maxUnits * ResourceCatalog.rawRequiredPerRefine;
      final int remainingRaw = max(0, (rawCargo[res] ?? 0) - consumedRaw);
      if (remainingRaw == 0) {
        rawCargo.remove(res);
      } else {
        rawCargo[res] = remainingRaw;
      }
      credits = max(0, credits - maxUnits * feePerUnit);
    }

    int bonusUnits = 0;
    final double bonusChance = refiningBonusOutputChance(refiningLv);
    for (int i = 0; i < maxUnits; i++) {
      if (rng.nextDouble() < bonusChance) bonusUnits++;
    }
    final int produced = maxUnits + bonusUnits;
    refinedCargo[res] = (refinedCargo[res] ?? 0) + produced;
    addSkillXp(SkillType.refining, maxUnits * spec.refiningXpPerUnit);
    saveGame();
    notifyUi();
    return produced;
  }

  bool canCraftRecipe(StationCraftRecipeId recipeId) {
    if (isAdminMode) return true;
    final StationCraftRecipeSpec spec = StationCraftCatalog.of(recipeId);
    for (final CraftMaterialCost cost in spec.ingredients) {
      final int have = cost.isRefined
          ? (refinedCargo[cost.resource] ?? 0)
          : (rawCargo[cost.resource] ?? 0);
      if (have < cost.amount) return false;
    }
    return true;
  }

  CraftOutcome? craftStationRecipe(StationCraftRecipeId recipeId) {
    if (!canCraftRecipe(recipeId)) return null;
    final StationCraftRecipeSpec spec = StationCraftCatalog.of(recipeId);
    final SkillType skill = spec.discipline.skill;
    final int skillLv = skillLevelOf(skill);

    int refundedUnits = 0;
    if (!isAdminMode) {
      final double refundRate =
          spec.discipline == CraftingDiscipline.synthesis
              ? 0.0
              : craftingMaterialReturnRate(skillLv);
      for (final CraftMaterialCost cost in spec.ingredients) {
        final Map<ResourceType, int> pool =
            cost.isRefined ? refinedCargo : rawCargo;
        int refund = 0;
        if (refundRate > 0.0) {
          for (int i = 0; i < cost.amount; i++) {
            if (rng.nextDouble() < refundRate) refund++;
          }
        }
        refundedUnits += refund;
        final int netConsume = max(0, cost.amount - refund);
        final int nextVal = max(0, (pool[cost.resource] ?? 0) - netConsume);
        if (nextVal == 0) {
          pool.remove(cost.resource);
        } else {
          pool[cost.resource] = nextVal;
        }
      }
    }

    final CraftQuality rolledQuality = rollCraftQuality(skillLv, rng);
    bool doubled = false;
    int qtyCrafted = 1;
    if (spec.discipline == CraftingDiscipline.synthesis &&
        rng.nextDouble() < synthesisDoubleOutputChance(skillLv)) {
      doubled = true;
      qtyCrafted = 2;
    }

    switch (spec.outputKind) {
      case CraftOutputKind.industrialShip:
        final ShipHullDefinition hull = spec.shipHullDefinition ??
            ShipHullCatalog.industrialT1(
              spec.industrialType ?? IndustrialType.miner,
            );
        ownedCraftedShips.add(
          CraftedShipRecord(
            instanceId: _nextCraftedId++,
            recipeId: recipeId,
            hullId: hull.id,
            industrialType: spec.industrialType ?? IndustrialType.miner,
            quality: rolledQuality,
            effectiveHull: rolledQuality.applyBonusInt(hull.baseHull.round()),
            effectiveShield:
                rolledQuality.applyBonusInt(hull.baseShield.round()),
            effectiveCargo: rolledQuality.applyBonusInt(hull.baseCargo),
          ),
        );
        break;
      case CraftOutputKind.equipmentGear:
        final EquipmentItemId eqId =
            spec.resultEquipmentId ?? EquipmentItemId.lf2;
        ownedItems.add(
          EquipmentItem(
            instanceId: _nextItemInstanceId++,
            id: eqId,
            quality: rolledQuality,
          ),
        );
        break;
      case CraftOutputKind.extractionTool:
      case CraftOutputKind.consumableRepair:
      case CraftOutputKind.consumableBooster:
        ownedCraftedItems.add(
          CraftedItemRecord(
            instanceId: _nextCraftedId++,
            recipeId: recipeId,
            quality: rolledQuality,
            effectivePrimaryStat:
                rolledQuality.applyBonus(spec.basePrimaryStat),
            effectiveDurationMinutes:
                rolledQuality.applyBonusInt(spec.baseDurationMinutes),
            quantity: qtyCrafted,
          ),
        );
        break;
    }

    addSkillXp(skill, spec.xpReward);
    final CraftOutcome outcome = CraftOutcome(
      id: _nextCraftOutcomeId++,
      recipeId: recipeId,
      quality: rolledQuality,
      quantityCrafted: qtyCrafted,
      refundedUnits: refundedUnits,
      xpAwarded: spec.xpReward,
      doubledOutput: doubled,
      statSummary: GameStrings.craftOutcomeStatSummary(
        spec,
        rolledQuality,
        language,
      ),
    );
    lastCraftOutcome = outcome;
    saveGame();
    notifyUi();
    return outcome;
  }

  bool useCraftedConsumable(int instanceId) {
    for (int i = 0; i < ownedCraftedItems.length; i++) {
      final CraftedItemRecord rec = ownedCraftedItems[i];
      if (rec.instanceId != instanceId) continue;
      final StationCraftRecipeSpec spec = StationCraftCatalog.of(rec.recipeId);
      if (spec.outputKind != CraftOutputKind.consumableRepair &&
          spec.outputKind != CraftOutputKind.consumableBooster) {
        return false;
      }
      if (spec.outputKind == CraftOutputKind.consumableRepair) {
        player.hp = min(currentMaxHull, player.hp + rec.effectivePrimaryStat);
        player.shield =
            min(currentMaxShield, player.shield + rec.effectivePrimaryStat);
      }
      rec.quantity -= 1;
      if (rec.quantity <= 0) {
        ownedCraftedItems.removeAt(i);
      }
      saveGame();
      notifyUi();
      return true;
    }
    return false;
  }

  LocalMarketState getOrCreateLocalMarket(String sectorId) {
    return _localMarkets.putIfAbsent(sectorId, () {
      final bool isBlack = sectorId == '0-0';
      final List<MarketResourceListing> resources = <MarketResourceListing>[];
      for (final ResourceType res in ResourceType.values) {
        final ResourceSpec spec = ResourceCatalog.of(res);
        final bool isNative = spec.nativeSectorId == sectorId;
        final bool canRefineHere = spec.canRefineIn(sectorId);
        final double mult = isBlack
            ? 1.35
            : (canRefineHere ? 1.18 : (isNative ? 0.90 : 1.05));
        final int rawSell = max(10, (spec.rawBasePrice * mult).round());
        final int rawBuy = max(rawSell + 8, (rawSell * 1.25).round());
        resources.add(
          MarketResourceListing(
            resourceType: res,
            isRefined: false,
            buyPrice: rawBuy,
            sellPrice: rawSell,
            localStock: isNative ? 45 : (isBlack ? 18 : 16),
          ),
        );
        if (spec.isRefinable) {
          final int refSell = max(25, (spec.refinedBasePrice * mult).round());
          final int refBuy = max(refSell + 15, (refSell * 1.25).round());
          resources.add(
            MarketResourceListing(
              resourceType: res,
              isRefined: true,
              buyPrice: refBuy,
              sellPrice: refSell,
              localStock: canRefineHere ? 28 : 10,
            ),
          );
        }
      }
      final List<MarketEquipmentListing> eq = isBlack
          ? <MarketEquipmentListing>[
              MarketEquipmentListing(
                itemId: EquipmentItemId.lf3,
                buyPrice: 2200,
                localStock: 4,
              ),
              MarketEquipmentListing(
                itemId: EquipmentItemId.sg3,
                buyPrice: 2400,
                localStock: 4,
              ),
              MarketEquipmentListing(
                itemId: EquipmentItemId.chip3,
                buyPrice: 1800,
                localStock: 3,
              ),
            ]
          : <MarketEquipmentListing>[];
      return LocalMarketState(
        sectorId: sectorId,
        marketName: isBlack ? '0-0 BLACK MARKET' : '$sectorId LOCAL MARKET',
        isBlackMarket: isBlack,
        resourceListings: resources,
        equipmentListings: eq,
      );
    });
  }

  bool buyBasicVendorItem(BasicVendorItemId item) {
    final int price = isAdminMode ? 0 : item.priceCredits;
    if (!isAdminMode && credits < price) return false;
    if (!isAdminMode) credits -= price;
    switch (item) {
      case BasicVendorItemId.basicLaserT1:
        ownedItems.add(
          EquipmentItem(
            instanceId: _nextItemInstanceId++,
            id: EquipmentItemId.lf1,
          ),
        );
        break;
      case BasicVendorItemId.basicShieldT1:
        ownedItems.add(
          EquipmentItem(
            instanceId: _nextItemInstanceId++,
            id: EquipmentItemId.sg1,
          ),
        );
        break;
      case BasicVendorItemId.basicToolT1:
        basicToolsOwned += 1;
        break;
      case BasicVendorItemId.basicRepairKit:
        player.hp = currentMaxHull;
        player.shield = currentMaxShield;
        break;
    }
    saveGame();
    notifyUi();
    return true;
  }

  bool buyMarketResource(ResourceType res, bool isRefined, int qty) {
    if (qty <= 0) return false;
    final LocalMarketState market = getOrCreateLocalMarket(currentSectorId);
    MarketResourceListing? listing;
    for (final MarketResourceListing l in market.resourceListings) {
      if (l.resourceType == res && l.isRefined == isRefined) {
        listing = l;
        break;
      }
    }
    if (listing == null) return false;
    if (!isAdminMode) {
      if (listing.localStock < qty) return false;
      if (remainingCargoCapacity < qty) return false;
      final int cost = listing.buyPrice * qty;
      if (credits < cost) return false;
      credits -= cost;
      listing.localStock -= qty;
    }
    final Map<ResourceType, int> pool = isRefined ? refinedCargo : rawCargo;
    pool[res] = (pool[res] ?? 0) + qty;
    saveGame();
    notifyUi();
    return true;
  }

  int sellMarketResource(ResourceType res, bool isRefined, int qty) {
    if (qty <= 0) return 0;
    final Map<ResourceType, int> pool = isRefined ? refinedCargo : rawCargo;
    final int owned = pool[res] ?? 0;
    final int sellQty = min(owned, qty);
    if (sellQty <= 0) return 0;
    final LocalMarketState market = getOrCreateLocalMarket(currentSectorId);
    MarketResourceListing? listing;
    for (final MarketResourceListing l in market.resourceListings) {
      if (l.resourceType == res && l.isRefined == isRefined) {
        listing = l;
        break;
      }
    }
    final int unitPrice = listing?.sellPrice ??
        (isRefined
            ? ResourceCatalog.of(res).refinedBasePrice
            : ResourceCatalog.of(res).rawBasePrice);
    final int nextQty = owned - sellQty;
    if (nextQty <= 0) {
      pool.remove(res);
    } else {
      pool[res] = nextQty;
    }
    credits += sellQty * unitPrice;
    if (listing != null) {
      listing.localStock += sellQty;
    }
    saveGame();
    notifyUi();
    return sellQty;
  }

  bool buyMarketEquipment(EquipmentItemId itemId) {
    final LocalMarketState market = getOrCreateLocalMarket(currentSectorId);
    MarketEquipmentListing? listing;
    for (final MarketEquipmentListing l in market.equipmentListings) {
      if (l.itemId == itemId) {
        listing = l;
        break;
      }
    }
    if (listing == null) return false;
    if (!isAdminMode) {
      if (listing.localStock <= 0 || credits < listing.buyPrice) return false;
      credits -= listing.buyPrice;
      listing.localStock -= 1;
    }
    ownedItems.add(
      EquipmentItem(instanceId: _nextItemInstanceId++, id: itemId),
    );
    saveGame();
    notifyUi();
    return true;
  }

  // ---------------------------------------------------------------------------
  // Death / Loot (60/40) Rules & Permanent Capsule Respawn
  // ---------------------------------------------------------------------------
  void handlePlayerDeath() {
    final bool wasInCapsule = activeShipRole == ShipRole.capsule;
    final String? destroyedShipName =
        wasInCapsule ? null : activeShipDisplayName;
    final String deathSectorId = currentSectorId;
    final double deathX = player.position.x;
    final double deathY = player.position.y;

    // 60% of cargo drops to PlayerShipWreck, 40% permanently destroyed
    final Map<ResourceType, int> droppedRaw = <ResourceType, int>{};
    final Map<ResourceType, int> droppedRef = <ResourceType, int>{};
    int cargoDropCount = 0;
    int cargoDestroyCount = 0;

    for (final MapEntry<ResourceType, int> e in rawCargo.entries) {
      final int qty = e.value;
      if (qty <= 0) continue;
      final int drop = (qty * 0.6).round();
      final int lost = qty - drop;
      if (drop > 0) droppedRaw[e.key] = drop;
      cargoDropCount += drop;
      cargoDestroyCount += lost;
    }
    for (final MapEntry<ResourceType, int> e in refinedCargo.entries) {
      final int qty = e.value;
      if (qty <= 0) continue;
      final int drop = (qty * 0.6).round();
      final int lost = qty - drop;
      if (drop > 0) droppedRef[e.key] = drop;
      cargoDropCount += drop;
      cargoDestroyCount += lost;
    }
    rawCargo.clear();
    refinedCargo.clear();

    // Non-Capsule equipped ship gear splits 60% drop / 40% destroyed
    final List<EquipmentItem> droppedGear = <EquipmentItem>[];
    int gearDropCount = 0;
    int gearDestroyCount = 0;
    if (!wasInCapsule) {
      final Set<int> equippedIds = <int>{};
      for (final EquipmentItem? item in laserSlots) {
        if (item != null) equippedIds.add(item.instanceId);
      }
      for (final EquipmentItem? item in shieldSlots) {
        if (item != null) equippedIds.add(item.instanceId);
      }
      for (int i = 0; i < config1LaserSlots.length; i++) {
        if (config1LaserSlots[i] != null &&
            equippedIds.contains(config1LaserSlots[i]!.instanceId)) {
          config1LaserSlots[i] = null;
        }
      }
      for (int i = 0; i < config2LaserSlots.length; i++) {
        if (config2LaserSlots[i] != null &&
            equippedIds.contains(config2LaserSlots[i]!.instanceId)) {
          config2LaserSlots[i] = null;
        }
      }
      for (int i = 0; i < config1ShieldSlots.length; i++) {
        if (config1ShieldSlots[i] != null &&
            equippedIds.contains(config1ShieldSlots[i]!.instanceId)) {
          config1ShieldSlots[i] = null;
        }
      }
      for (int i = 0; i < config2ShieldSlots.length; i++) {
        if (config2ShieldSlots[i] != null &&
            equippedIds.contains(config2ShieldSlots[i]!.instanceId)) {
          config2ShieldSlots[i] = null;
        }
      }
      final List<EquipmentItem> lostEquipped = ownedItems
          .where((EquipmentItem e) => equippedIds.contains(e.instanceId))
          .toList();
      ownedItems
          .removeWhere((EquipmentItem e) => equippedIds.contains(e.instanceId));
      for (final EquipmentItem item in lostEquipped) {
        if (rng.nextDouble() < 0.6) {
          droppedGear.add(item);
          gearDropCount++;
        } else {
          gearDestroyCount++;
        }
      }

      // Destroy active non-capsule ship
      if (activeShipRole == ShipRole.industrial && activeCraftedShipId != null) {
        ownedCraftedShips.removeWhere(
          (CraftedShipRecord s) => s.instanceId == activeCraftedShipId,
        );
      } else if (activeShipRole == ShipRole.combat &&
          activeShipClass != ShipClass.interceptor) {
        ownedShipClasses.remove(activeShipClass);
      }
    }

    if (cargoDropCount > 0 || gearDropCount > 0) {
      playerWrecks.add(
        PlayerShipWreck(
          id: _nextWorldEntityId++,
          sectorId: deathSectorId,
          x: deathX,
          y: deathY,
          shipName: destroyedShipName ?? 'Capsule',
          isCapsuleWreck: wasInCapsule,
          rawCargo: droppedRaw,
          refinedCargo: droppedRef,
          droppedEquipment: droppedGear,
        ),
      );
    }

    // Respawn in Home Capital City inside the permanent Pilot Capsule
    final String homeCity = GalaxyMapGraph.homeCityForCompany(pilotCompany);
    activeShipRole = ShipRole.capsule;
    activeCraftedShipId = null;
    autoFireEnabled = false;
    lockedTarget = null;
    cancelAutopilot();

    if (currentSectorId != homeCity) {
      jumpToSector(homeCity);
    } else {
      player.position.setFrom(activeSafeZoneCenter);
    }
    player.hp = currentMaxHull;
    player.shield = currentMaxShield;
    _snapVantsToFormation();

    lastDeathSummary = DeathSummary(
      wasInCapsule: wasInCapsule,
      destroyedShipName: destroyedShipName,
      deathSectorId: deathSectorId,
      deathX: deathX.round(),
      deathY: deathY.round(),
      cargoDroppedToWreck: cargoDropCount,
      cargoDestroyed: cargoDestroyCount,
      gearDroppedToWreck: gearDropCount,
      gearDestroyed: gearDestroyCount,
      vantsDamagedCount: ownedVants.length,
      homeCityId: homeCity,
    );

    saveGame();
    closeAllModalOverlays();
    pauseEngine();
    overlays.add(deathOverlayId);
    notifyUi();
  }

  void setPlottedRoute(List<String> route) {
    plottedRoute = List<String>.unmodifiable(route);
    notifyUi();
  }

  void clearPlottedRoute() {
    plottedRoute = <String>[];
    notifyUi();
  }

  bool engageAutopilotToNextRoutePortal() {
    final String? nextHop = nextRouteHopSectorId;
    if (nextHop == null) return false;
    for (final SectorPortal portal in activePortals) {
      if (portal.targetSectorId == nextHop) {
        setAutopilotTarget(portal.worldX, portal.worldY);
        return true;
      }
    }
    return false;
  }

  void setAutopilotTarget(double worldX, double worldY) {
    final double tx = worldX.clamp(200.0, activeMapSize - 200.0);
    final double ty = worldY.clamp(200.0, activeMapSize - 200.0);
    autopilotTarget = Vector2(tx, ty);
  }

  void cancelAutopilot() {
    autopilotTarget = null;
  }

  void switchConfig(int configNumber) {
    final int target = configNumber == 2 ? 2 : 1;
    if (activeConfig == target) return;
    activeConfig = target;
    _syncPlayerVitalsToStats();
    saveGame();
  }

  void toggleConfig() {
    switchConfig(activeConfig == 1 ? 2 : 1);
  }

  // ---------------------------------------------------------------------------
  // Classic DarkOrbit Drone Formations & Smooth World-Space Lerp
  // ---------------------------------------------------------------------------
  bool buyOrSelectFormation(DroneFormation formation) {
    if (ownedFormations.contains(formation)) {
      if (activeFormation == formation) return false;
      activeFormation = formation;
      _syncPlayerVitalsToStats();
      saveGame();
      return true;
    }
    final DroneFormationSpec spec = DroneFormationCatalog.of(formation);
    final int cost = isAdminMode ? 0 : spec.cost;
    if (!isAdminMode && credits < cost) return false;
    if (!isAdminMode) credits -= cost;
    ownedFormations.add(formation);
    activeFormation = formation;
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  bool selectOwnedFormationFromHud(DroneFormation formation) {
    if (!ownedFormations.contains(formation)) return false;
    if (activeFormation == formation) return false;
    if (formationSwitchCooldown > 0.05) return false;
    activeFormation = formation;
    formationSwitchCooldown = GameConstants.formationSwitchCooldownSec;
    _syncPlayerVitalsToStats();
    spawnFloatingText(
      position: player.position + Vector2(0.0, -46.0),
      text: '${DroneFormationCatalog.of(formation).displayName} FORMATION',
      color: DroneFormationCatalog.of(formation).accentColor,
      fontSize: 13.5,
    );
    saveGame();
    return true;
  }

  bool cycleOwnedFormation() {
    if (formationSwitchCooldown > 0.0) return false;
    final List<DroneFormation> ordered = DroneFormation.values
        .where((DroneFormation f) => ownedFormations.contains(f))
        .toList();
    if (ordered.length <= 1) return false;
    final int curIdx = ordered.indexOf(activeFormation);
    final DroneFormation next = ordered[(curIdx + 1) % ordered.length];
    return selectOwnedFormationFromHud(next);
  }

  void _snapVantsToFormation() {
    final List<VantDrone> active = activeVants;
    final int total = active.length;
    if (total == 0) return;
    final double hullR = activeHullRadius;
    final double cosA = cos(player.angle);
    final double sinA = sin(player.angle);
    for (int i = 0; i < total; i++) {
      final VantDrone vant = active[i];
      final DroneFormationSlot slot = DroneFormationCatalog.computeLocalSlot(
        formation: activeFormation,
        index: i,
        total: total,
        shipHullRadius: hullR,
        elapsedTime: elapsedTime,
      );
      vant.worldX =
          player.position.x + slot.localX * cosA - slot.localY * sinA;
      vant.worldY =
          player.position.y + slot.localX * sinA + slot.localY * cosA;
      vant.facingRad = player.angle + slot.extraRotation;
      vant.hasInitializedPosition = true;
    }
  }

  void _updateVantFormationPositions(double dt) {
    final List<VantDrone> active = activeVants;
    final int total = active.length;
    if (total == 0) return;
    final double hullR = activeHullRadius;
    final double cosA = cos(player.angle);
    final double sinA = sin(player.angle);
    final double posAlpha = 1.0 - exp(-9.5 * dt);
    final double rotAlpha = 1.0 - exp(-11.0 * dt);

    for (int i = 0; i < total; i++) {
      final VantDrone vant = active[i];
      final DroneFormationSlot slot = DroneFormationCatalog.computeLocalSlot(
        formation: activeFormation,
        index: i,
        total: total,
        shipHullRadius: hullR,
        elapsedTime: elapsedTime,
      );
      final double targetX =
          player.position.x + slot.localX * cosA - slot.localY * sinA;
      final double targetY =
          player.position.y + slot.localX * sinA + slot.localY * cosA;
      final double targetRot = player.angle + slot.extraRotation;

      if (!vant.hasInitializedPosition) {
        vant.worldX = targetX;
        vant.worldY = targetY;
        vant.facingRad = targetRot;
        vant.hasInitializedPosition = true;
      } else {
        vant.worldX += (targetX - vant.worldX) * posAlpha;
        vant.worldY += (targetY - vant.worldY) * posAlpha;
        final double diff = _normalizeAngle(targetRot - vant.facingRad);
        vant.facingRad = _normalizeAngle(vant.facingRad + diff * rotAlpha);
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Ship Classes (Interceptor, Battlecruiser, Dreadnought)
  // ---------------------------------------------------------------------------
  bool buyOrSelectShipClass(ShipClass shipClass) {
    if (!canSwapShipsHere) return false;
    if (ownedShipClasses.contains(shipClass)) {
      if (activeShipRole == ShipRole.combat && activeShipClass == shipClass) {
        return false;
      }
      activeShipRole = ShipRole.combat;
      activeCraftedShipId = null;
      activeShipClass = shipClass;
      _resizeSlotsForShip(shipClass);
      _syncPlayerVitalsToStats();
      player.hp = currentMaxHull;
      player.shield = currentMaxShield;
      saveGame();
      notifyUi();
      return true;
    }
    final ShipClassSpec spec = ShipClassCatalog.of(shipClass);
    final int cost = isAdminMode ? 0 : spec.cost;
    if (!isAdminMode && credits < cost) return false;
    final double oldMaxHull = currentMaxHull;
    if (!isAdminMode) credits -= cost;
    ownedShipClasses.add(shipClass);
    activeShipRole = ShipRole.combat;
    activeCraftedShipId = null;
    activeShipClass = shipClass;
    _resizeSlotsForShip(shipClass);
    final double hullDelta = max(0.0, currentMaxHull - oldMaxHull);
    player.hp = min(currentMaxHull, player.hp + hullDelta);
    _syncPlayerVitalsToStats();
    saveGame();
    notifyUi();
    return true;
  }

  void _resizeSlotsForShip(ShipClass shipClass) {
    final ShipClassSpec spec = ShipClassCatalog.of(shipClass);
    _resizeSlotList(config1LaserSlots, spec.laserSlotCount);
    _resizeSlotList(config2LaserSlots, spec.laserSlotCount);
    _resizeSlotList(config1ShieldSlots, spec.shieldSlotCount);
    _resizeSlotList(config2ShieldSlots, spec.shieldSlotCount);
  }

  void _resizeSlotList(List<EquipmentItem?> list, int targetSize) {
    while (list.length > targetSize) {
      list.removeLast();
    }
    while (list.length < targetSize) {
      list.add(null);
    }
  }

  // ---------------------------------------------------------------------------
  // VANT (Drone) System: Flax (1 Universal Slot) & Iris (2 Universal Slots)
  // ---------------------------------------------------------------------------
  bool _isVantCompatibleItem(EquipmentItemId id) {
    final EquipmentCategory cat = EquipmentCatalog.of(id).category;
    return cat == EquipmentCategory.laser ||
        cat == EquipmentCategory.shieldGenerator;
  }

  bool buyFlaxVant() {
    if (!canBuyFlaxVant) return false;
    if (!isAdminMode && credits < GameConstants.flaxVantCost) return false;
    if (!isAdminMode) credits -= GameConstants.flaxVantCost;
    ownedVants.add(VantDrone(id: _nextVantId++, tier: VantTier.flax));
    _clearReserveVantSlots();
    saveGame();
    notifyUi();
    return true;
  }

  bool fuseFlaxVants() {
    final List<VantDrone> flaxList =
        ownedVants.where((VantDrone v) => v.tier == VantTier.flax).toList();
    if (flaxList.length < 2) return false;

    int filledCount(VantDrone v) {
      int count = 0;
      for (final EquipmentItem? e in v.config1Slots) {
        if (e != null) count++;
      }
      for (final EquipmentItem? e in v.config2Slots) {
        if (e != null) count++;
      }
      return count;
    }

    flaxList.sort((VantDrone a, VantDrone b) =>
        filledCount(a).compareTo(filledCount(b)));
    final List<VantDrone> toConsume = <VantDrone>[flaxList[0], flaxList[1]];

    // Safely unequip all Lasers/Shields inside the 2 Flax VANTs back into Inventory
    for (final VantDrone drone in toConsume) {
      for (int i = 0; i < drone.config1Slots.length; i++) {
        drone.config1Slots[i] = null;
      }
      for (int i = 0; i < drone.config2Slots.length; i++) {
        drone.config2Slots[i] = null;
      }
    }
    final Set<int> consumeIds = <int>{toConsume[0].id, toConsume[1].id};
    ownedVants.removeWhere((VantDrone v) => consumeIds.contains(v.id));

    ownedVants.add(VantDrone(id: _nextVantId++, tier: VantTier.iris));
    _clearReserveVantSlots();
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  bool equipItemToVantSlot(int vantId, int slotIndex, {int? instanceId}) {
    VantDrone? vant;
    for (final VantDrone v in activeVants) {
      if (v.id == vantId) {
        vant = v;
        break;
      }
    }
    if (vant == null) return false;
    final List<EquipmentItem?> slots = vant.slotsForConfig(activeConfig);
    if (slotIndex < 0 || slotIndex >= slots.length || slots[slotIndex] != null) {
      return false;
    }

    final List<EquipmentItem> available = inventory
        .where((EquipmentItem e) => _isVantCompatibleItem(e.id))
        .toList();
    EquipmentItem? chosen;
    if (instanceId != null) {
      for (final EquipmentItem e in available) {
        if (e.instanceId == instanceId) {
          chosen = e;
          break;
        }
      }
    } else if (available.isNotEmpty) {
      available.sort(
        (EquipmentItem a, EquipmentItem b) =>
            b.spec.tierLevel.compareTo(a.spec.tierLevel),
      );
      chosen = available.first;
    }
    if (chosen == null) return false;

    slots[slotIndex] = chosen;
    _syncPlayerVitalsToStats(shieldDeltaAdded: chosen.spec.shieldBonus);
    saveGame();
    return true;
  }

  bool equipFromInventoryToVant(int instanceId) {
    EquipmentItem? chosen;
    for (final EquipmentItem e in inventory) {
      if (e.instanceId == instanceId && _isVantCompatibleItem(e.id)) {
        chosen = e;
        break;
      }
    }
    if (chosen == null) return false;

    for (final VantDrone vant in activeVants) {
      final List<EquipmentItem?> slots = vant.slotsForConfig(activeConfig);
      final int emptyIdx = slots.indexWhere((EquipmentItem? s) => s == null);
      if (emptyIdx != -1) {
        slots[emptyIdx] = chosen;
        _syncPlayerVitalsToStats(shieldDeltaAdded: chosen.spec.shieldBonus);
        saveGame();
        return true;
      }
    }
    return false;
  }

  bool unequipVantSlot(int vantId, int slotIndex) {
    VantDrone? vant;
    for (final VantDrone v in activeVants) {
      if (v.id == vantId) {
        vant = v;
        break;
      }
    }
    if (vant == null) return false;
    final List<EquipmentItem?> slots = vant.slotsForConfig(activeConfig);
    if (slotIndex < 0 || slotIndex >= slots.length || slots[slotIndex] == null) {
      return false;
    }
    slots[slotIndex] = null;
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  // ---------------------------------------------------------------------------
  // Minimalist Level 1 Shop & 2-for-1 Fusion Workshop
  // ---------------------------------------------------------------------------
  bool buyEquipment(EquipmentItemId itemId) {
    final EquipmentSpec spec = EquipmentCatalog.of(itemId);
    final int? cost = spec.shopCost;
    if (cost == null) return false;
    if (!isAdminMode && credits < cost) {
      return false;
    }
    if (!isAdminMode) credits -= cost;
    final EquipmentItem newItem = EquipmentItem(
      instanceId: _nextItemInstanceId++,
      id: itemId,
    );
    ownedItems.add(newItem);

    double shieldAdded = 0.0;
    if (spec.category == EquipmentCategory.sectorChip) {
      if (equippedSectorChip == null ||
          spec.sectorTier > equippedSectorChip!.spec.sectorTier) {
        equippedSectorChip = newItem;
      }
    } else if (_tryAutoEquipInActiveConfig(newItem)) {
      shieldAdded = spec.shieldBonus;
    }

    _syncPlayerVitalsToStats(shieldDeltaAdded: shieldAdded);
    saveGame();
    notifyUi();
    return true;
  }

  bool fuseItems(EquipmentItemId sourceType) {
    final EquipmentItemId? resultType =
        EquipmentCatalog.nextFusionResult(sourceType);
    if (resultType == null) return false;

    final List<EquipmentItem> matching = ownedItems
        .where((EquipmentItem e) => e.id == sourceType)
        .toList();
    if (matching.length < 2) return false;

    final List<VantDrone> activeVantList = activeVants;
    final Set<int> activeIds = <int>{};
    for (final EquipmentItem? item in laserSlots) {
      if (item != null) activeIds.add(item.instanceId);
    }
    for (final EquipmentItem? item in shieldSlots) {
      if (item != null) activeIds.add(item.instanceId);
    }
    for (final VantDrone vant in activeVantList) {
      for (final EquipmentItem? item in vant.slotsForConfig(activeConfig)) {
        if (item != null) activeIds.add(item.instanceId);
      }
    }
    if (equippedSectorChip != null) {
      activeIds.add(equippedSectorChip!.instanceId);
    }

    final int inactiveConfig = activeConfig == 2 ? 1 : 2;
    final List<EquipmentItem?> inactiveLasers =
        activeConfig == 2 ? config1LaserSlots : config2LaserSlots;
    final List<EquipmentItem?> inactiveShields =
        activeConfig == 2 ? config1ShieldSlots : config2ShieldSlots;
    final Set<int> inactiveIds = <int>{};
    for (final EquipmentItem? item in inactiveLasers) {
      if (item != null) inactiveIds.add(item.instanceId);
    }
    for (final EquipmentItem? item in inactiveShields) {
      if (item != null) inactiveIds.add(item.instanceId);
    }
    for (final VantDrone vant in activeVantList) {
      for (final EquipmentItem? item in vant.slotsForConfig(inactiveConfig)) {
        if (item != null) inactiveIds.add(item.instanceId);
      }
    }

    int priorityOf(EquipmentItem item) {
      if (!activeIds.contains(item.instanceId) &&
          !inactiveIds.contains(item.instanceId)) {
        return 0;
      }
      if (!activeIds.contains(item.instanceId)) return 1;
      return 2;
    }

    matching.sort(
      (EquipmentItem a, EquipmentItem b) =>
          priorityOf(a).compareTo(priorityOf(b)),
    );
    final Set<int> consumeIds = <int>{
      matching[0].instanceId,
      matching[1].instanceId,
    };

    final int vacatedLaserIdx = laserSlots.indexWhere(
      (EquipmentItem? item) => item != null && consumeIds.contains(item.instanceId),
    );
    final int vacatedShieldIdx = shieldSlots.indexWhere(
      (EquipmentItem? item) => item != null && consumeIds.contains(item.instanceId),
    );
    VantDrone? vacatedVant;
    int vacatedVantSlotIdx = -1;
    for (final VantDrone vant in activeVantList) {
      final int idx = vant.slotsForConfig(activeConfig).indexWhere(
            (EquipmentItem? item) =>
                item != null && consumeIds.contains(item.instanceId),
          );
      if (idx != -1) {
        vacatedVant = vant;
        vacatedVantSlotIdx = idx;
        break;
      }
    }
    final bool chipWasEquipped = equippedSectorChip != null &&
        consumeIds.contains(equippedSectorChip!.instanceId);

    ownedItems.removeWhere((EquipmentItem e) => consumeIds.contains(e.instanceId));
    _clearConsumedFromSlotList(config1LaserSlots, consumeIds);
    _clearConsumedFromSlotList(config2LaserSlots, consumeIds);
    _clearConsumedFromSlotList(config1ShieldSlots, consumeIds);
    _clearConsumedFromSlotList(config2ShieldSlots, consumeIds);
    for (final VantDrone vant in ownedVants) {
      _clearConsumedFromSlotList(vant.config1Slots, consumeIds);
      _clearConsumedFromSlotList(vant.config2Slots, consumeIds);
    }
    if (chipWasEquipped) {
      equippedSectorChip = null;
    }

    final EquipmentItem forged = EquipmentItem(
      instanceId: _nextItemInstanceId++,
      id: resultType,
    );
    ownedItems.add(forged);

    double shieldAdded = 0.0;
    final EquipmentSpec resultSpec = forged.spec;
    switch (resultSpec.category) {
      case EquipmentCategory.laser:
        if (vacatedLaserIdx != -1 && laserSlots[vacatedLaserIdx] == null) {
          laserSlots[vacatedLaserIdx] = forged;
        } else if (vacatedVant != null &&
            vacatedVantSlotIdx != -1 &&
            vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] ==
                null) {
          vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] = forged;
        } else {
          _tryAutoEquipInActiveConfig(forged);
        }
      case EquipmentCategory.shieldGenerator:
        if (vacatedShieldIdx != -1 && shieldSlots[vacatedShieldIdx] == null) {
          shieldSlots[vacatedShieldIdx] = forged;
          shieldAdded = resultSpec.shieldBonus;
        } else if (vacatedVant != null &&
            vacatedVantSlotIdx != -1 &&
            vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] ==
                null) {
          vacatedVant.slotsForConfig(activeConfig)[vacatedVantSlotIdx] = forged;
          shieldAdded = resultSpec.shieldBonus;
        } else if (_tryAutoEquipInActiveConfig(forged)) {
          shieldAdded = resultSpec.shieldBonus;
        }
      case EquipmentCategory.sectorChip:
        if (equippedSectorChip == null ||
            resultSpec.sectorTier > equippedSectorChip!.spec.sectorTier) {
          equippedSectorChip = forged;
        }
    }

    _syncPlayerVitalsToStats(shieldDeltaAdded: shieldAdded);
    saveGame();
    return true;
  }

  void _clearConsumedFromSlotList(
    List<EquipmentItem?> slots,
    Set<int> consumeIds,
  ) {
    for (int i = 0; i < slots.length; i++) {
      final EquipmentItem? item = slots[i];
      if (item != null && consumeIds.contains(item.instanceId)) {
        slots[i] = null;
      }
    }
  }

  bool equipFromInventory(int instanceId) {
    EquipmentItem? item;
    for (final EquipmentItem e in inventory) {
      if (e.instanceId == instanceId) {
        item = e;
        break;
      }
    }
    if (item == null) return false;
    if (item.spec.category == EquipmentCategory.sectorChip) {
      equippedSectorChip = item;
      _syncPlayerVitalsToStats();
      saveGame();
      return true;
    }
    if (!_tryAutoEquipInActiveConfig(item)) return false;
    _syncPlayerVitalsToStats(shieldDeltaAdded: item.spec.shieldBonus);
    saveGame();
    return true;
  }

  bool unequipLaserSlot(int slotIndex) {
    if (slotIndex < 0 || slotIndex >= laserSlots.length) return false;
    if (laserSlots[slotIndex] == null) return false;
    laserSlots[slotIndex] = null;
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  bool unequipShieldSlot(int slotIndex) {
    if (slotIndex < 0 || slotIndex >= shieldSlots.length) return false;
    if (shieldSlots[slotIndex] == null) return false;
    shieldSlots[slotIndex] = null;
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  bool unequipSectorChip() {
    if (equippedSectorChip == null) return false;
    equippedSectorChip = null;
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  bool _tryAutoEquipInActiveConfig(EquipmentItem item) {
    final List<EquipmentItem?> targetSlots;
    switch (item.spec.category) {
      case EquipmentCategory.laser:
        targetSlots = laserSlots;
      case EquipmentCategory.shieldGenerator:
        targetSlots = shieldSlots;
      case EquipmentCategory.sectorChip:
        return false;
    }
    final int emptyIndex = targetSlots.indexOf(null);
    if (emptyIndex == -1) return false;
    targetSlots[emptyIndex] = item;
    return true;
  }

  void _syncPlayerVitalsToStats({double shieldDeltaAdded = 0.0}) {
    if (shieldDeltaAdded > 0.0 && player.inSafeZone) {
      player.shield = min(currentMaxShield, player.shield + shieldDeltaAdded);
    }
    player.hp = player.hp.clamp(0.0, currentMaxHull);
    player.shield = player.shield.clamp(0.0, currentMaxShield);
  }

  // ---------------------------------------------------------------------------
  // Sequential Missions & XP Progression
  // ---------------------------------------------------------------------------
  void recordMissionProgress(MissionGoalType goalType) {
    final MissionSpec? m = activeMission;
    if (m == null || isMissionLevelLocked) return;
    if (m.goalType != goalType) return;
    if (currentMissionProgress >= m.targetCount) return;

    currentMissionProgress = min(m.targetCount, currentMissionProgress + 1);
    if (currentMissionProgress >= m.targetCount) {
      spawnFloatingText(
        position: player.position + Vector2(0.0, -48.0),
        text: '${m.code} COMPLETE! CLAIM REWARD',
        color: const Color(0xFF4ADE80),
        fontSize: 15.0,
      );
    }
  }

  bool claimMissionReward() {
    final MissionSpec? m = activeMission;
    if (m == null || !isMissionReadyToClaim) return false;

    credits += m.rewardCredits;
    if (m.rewardXp > 0) {
      addXp(m.rewardXp);
    }
    if (m.rewardChip != null) {
      final EquipmentItem chip = EquipmentItem(
        instanceId: _nextItemInstanceId++,
        id: m.rewardChip!,
      );
      ownedItems.add(chip);
      if (equippedSectorChip == null ||
          chip.spec.sectorTier > equippedSectorChip!.spec.sectorTier) {
        equippedSectorChip = chip;
      }
    }

    spawnFloatingText(
      position: player.position + Vector2(0.0, -44.0),
      text: 'CLAIMED: ${m.rewardSummary}',
      color: const Color(0xFFFBBF24),
      fontSize: 15.0,
    );
    world.add(
      ShockwaveRingComponent(
        position: player.position.clone(),
        maxRadius: 96.0,
        duration: 0.45,
        color: const Color(0xFF4ADE80),
      ),
    );

    currentMissionIndex = min(MissionCatalog.all.length, currentMissionIndex + 1);
    currentMissionProgress = 0;
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  void addXp(int amount) {
    if (amount <= 0) return;
    player.xp += amount;
    bool leveledUp = false;
    while (player.xp >= GameConstants.xpRequiredForLevel(player.level)) {
      player.xp -= GameConstants.xpRequiredForLevel(player.level);
      player.level += 1;
      leveledUp = true;
    }
    if (leveledUp) {
      player.hp = currentMaxHull;
      world.add(
        ShockwaveRingComponent(
          position: player.position.clone(),
          maxRadius: 110.0,
          duration: 0.55,
          color: const Color(0xFFFACC15),
        ),
      );
      spawnFloatingText(
        position: player.position + Vector2(0.0, -40.0),
        text: 'LEVEL UP! LV ${player.level}',
        color: const Color(0xFFFACC15),
        fontSize: 17.0,
      );
    }
  }

  // Developer / Testing Helpers
  void debugAddOneMillionCredits() {
    credits += 1000000;
    saveGame();
    notifyUi();
  }

  // ---------------------------------------------------------------------------
  // Combat: Laser Toggle & Homing Rockets
  // ---------------------------------------------------------------------------
  void toggleAutoFire() {
    if (player.inSafeZone) {
      autoFireEnabled = false;
      return;
    }
    autoFireEnabled = !autoFireEnabled;
    if (autoFireEnabled &&
        (lockedTarget == null || lockedTarget!.isRemoved)) {
      AlienShipComponent? nearest;
      double bestDist = GameConstants.playerAttackRange;
      for (final AlienShipComponent alien in aliens) {
        final double d = alien.position.distanceTo(player.position);
        if (d < bestDist) {
          bestDist = d;
          nearest = alien;
        }
      }
      lockedTarget = nearest;
    }
  }

  bool fireHomingRocket() {
    if (player.inSafeZone || player.rocketCooldown > 0.0) return false;
    AlienShipComponent? target =
        (lockedTarget != null && !lockedTarget!.isRemoved) ? lockedTarget : null;
    if (target == null ||
        target.position.distanceTo(player.position) >
            GameConstants.playerAttackRange * 1.15) {
      AlienShipComponent? nearest;
      double bestDist = GameConstants.playerAttackRange * 1.15;
      for (final AlienShipComponent alien in aliens) {
        final double d = alien.position.distanceTo(player.position);
        if (d < bestDist) {
          bestDist = d;
          nearest = alien;
        }
      }
      if (nearest == null) return false;
      target = nearest;
      lockedTarget = nearest;
    }

    final Vector2 delta = target.position - player.position;
    final double launchAngle = atan2(delta.y, delta.x);
    final double rocketDmg = max(48.0, currentLaserDamage * 2.5);
    world.add(
      HomingRocketComponent(
        position: player.position +
            Vector2(cos(launchAngle), sin(launchAngle)) *
                (activeHullRadius + 4.0),
        angle: launchAngle,
        target: target,
        damage: rocketDmg,
      ),
    );
    player.rocketCooldown = GameConstants.rocketCooldownSec;
    return true;
  }

  void _lockClosestAlienInRadarCone(double aimAngle) {
    AlienShipComponent? closest;
    double closestDist = GameConstants.radarConeRange;

    for (final AlienShipComponent alien in aliens) {
      if (alien.isRemoved) continue;
      final Vector2 delta = alien.position - player.position;
      final double dist = delta.length;
      if (dist > GameConstants.radarConeRange) continue;

      final double angleToAlien = atan2(delta.y, delta.x);
      final double diff = _normalizeAngle(angleToAlien - aimAngle).abs();
      if (diff <= GameConstants.radarConeHalfAngleRad && dist < closestDist) {
        closestDist = dist;
        closest = alien;
      }
    }

    if (closest != null) {
      lockedTarget = closest;
    }
  }

  double _normalizeAngle(double a) {
    double angle = a;
    while (angle > pi) {
      angle -= 2.0 * pi;
    }
    while (angle < -pi) {
      angle += 2.0 * pi;
    }
    return angle;
  }

  Vector2 randomPointInAnnulus(double minRadius, double maxRadius) {
    final double angle = rng.nextDouble() * 2.0 * pi;
    final double rMinSq = minRadius * minRadius;
    final double rMaxSq = maxRadius * maxRadius;
    final double r = sqrt(rMinSq + rng.nextDouble() * (rMaxSq - rMinSq));
    return Vector2(cos(angle) * r, sin(angle) * r);
  }

  Vector2 randomPointInActiveSector({
    required double margin,
    required double safeZoneBuffer,
  }) {
    final double mapSize = activeMapSize;
    final double minCoord = margin;
    final double maxCoord = max(margin + 100.0, mapSize - margin);
    final Vector2 safeCenter = activeSafeZoneCenter;
    final double minSafeDist = activeSafeZoneRadius + safeZoneBuffer;
    for (int i = 0; i < 24; i++) {
      final double rx = minCoord + rng.nextDouble() * (maxCoord - minCoord);
      final double ry = minCoord + rng.nextDouble() * (maxCoord - minCoord);
      if (!currentSector.hasCentralSafeZone || currentSector.isEntireSectorSafe) {
        return Vector2(rx, ry);
      }
      final double dx = rx - safeCenter.x;
      final double dy = ry - safeCenter.y;
      if (sqrt(dx * dx + dy * dy) >= minSafeDist) {
        return Vector2(rx, ry);
      }
    }
    return Vector2(maxCoord * 0.78, maxCoord * 0.78);
  }

  void spawnAlien(AlienTier tier, {double minDistFromPlayer = 480.0}) {
    if (currentSector.isEntireSectorSafe) return;
    final AlienTierSpec spec = AlienTierCatalog.of(tier);
    final bool isBoss = rng.nextDouble() < GameConstants.bossSpawnChance;
    final double effRadius =
        isBoss ? spec.radius * GameConstants.bossSizeMultiplier : spec.radius;
    Vector2 pickPos() => randomPointInActiveSector(
          margin: 280.0 + effRadius,
          safeZoneBuffer: 220.0 + effRadius,
        );

    Vector2 pos = pickPos();
    for (int i = 0; i < 16; i++) {
      final Vector2 candidate = pickPos();
      if (candidate.distanceTo(player.position) >= minDistFromPlayer) {
        pos = candidate;
        break;
      }
    }
    final AlienShipComponent alien = AlienShipComponent(
      tier: tier,
      position: pos,
      isBoss: isBoss,
    );
    aliens.add(alien);
    world.add(alien);
  }

  void spawnBonusBox() {
    final Vector2 pos = randomPointInActiveSector(
      margin: 260.0,
      safeZoneBuffer: 140.0,
    );
    final int tier = currentSector.maxTier;
    final int reward;
    final int xpReward;
    final double shieldAmount;
    switch (tier) {
      case 1:
        reward = (4 + rng.nextInt(5)) * 25;
        xpReward = 15;
        shieldAmount = 40.0;
        break;
      case 2:
        reward = (6 + rng.nextInt(6)) * 25;
        xpReward = 25;
        shieldAmount = 60.0;
        break;
      case 3:
      case 4:
        reward = (12 + rng.nextInt(9)) * 25;
        xpReward = 55;
        shieldAmount = 95.0;
        break;
      default:
        reward = (26 + rng.nextInt(16)) * 25;
        xpReward = 130;
        shieldAmount = 150.0;
        break;
    }
    final bool isShieldBox = rng.nextDouble() < 0.24;
    final LootBoxComponent box = LootBoxComponent(
      kind: LootBoxKind.bonusBox,
      position: pos,
      creditReward: reward,
      shieldReward: isShieldBox ? shieldAmount : 0.0,
      xpReward: xpReward,
    );
    lootBoxes.add(box);
    world.add(box);
  }

  void spawnCargoBox(
    Vector2 position,
    AlienTierSpec spec, {
    bool isBoss = false,
  }) {
    final int baseCr = isBoss
        ? spec.rewardCredits * GameConstants.bossRewardMultiplier
        : spec.rewardCredits;
    final int bonus =
        ((baseCr * (0.5 + rng.nextDouble() * 0.35)) / 25.0).round() * 25;
    final LootBoxComponent cargo = LootBoxComponent(
      kind: LootBoxKind.cargoBox,
      position: position.clone(),
      creditReward: max(50, bonus),
      shieldReward: 0.0,
      xpReward: 0,
    );
    lootBoxes.add(cargo);
    world.add(cargo);
  }

  void spawnFloatingText({
    required Vector2 position,
    required String text,
    required Color color,
    double fontSize = 13.5,
  }) {
    world.add(
      FloatingTextComponent(
        position: position,
        text: text,
        color: color,
        fontSize: fontSize,
      ),
    );
  }

  void spawnExplosion(
    Vector2 position,
    AlienTierSpec spec, {
    bool isBossVariant = false,
  }) {
    final bool bigBoom = isBossVariant || spec.tier == AlienTier.dreadnoughtBoss;
    world.add(
      ShockwaveRingComponent(
        position: position.clone(),
        maxRadius: bigBoom ? 145.0 : 76.0,
        duration: bigBoom ? 0.65 : 0.42,
        color: isBossVariant ? const Color(0xFFFBBF24) : spec.primaryColor,
      ),
    );
    final int count = bigBoom ? 32 : 16;
    final List<Color> palette = <Color>[
      spec.primaryColor,
      spec.accentColor,
      const Color(0xFFFACC15),
      Colors.white,
    ];
    for (int i = 0; i < count; i++) {
      final double angle = rng.nextDouble() * 2.0 * pi;
      final double speed =
          (bigBoom ? 90.0 : 60.0) + rng.nextDouble() * (bigBoom ? 250.0 : 190.0);
      world.add(
        ExplosionParticleComponent(
          position: position.clone(),
          velocity: Vector2(cos(angle) * speed, sin(angle) * speed),
          radius: (bigBoom ? 3.2 : 2.2) + rng.nextDouble() * 3.4,
          life: (bigBoom ? 0.55 : 0.36) + rng.nextDouble() * 0.4,
          color: palette[rng.nextInt(palette.length)],
        ),
      );
    }
  }

  void spawnHullHitSparks(Vector2 position, {int count = 13}) {
    const List<Color> sparkPalette = <Color>[
      Color(0xFFFF6D00),
      Color(0xFFFF2E4C),
      Color(0xFFFB923C),
      Color(0xFFFBBF24),
    ];
    for (int i = 0; i < count; i++) {
      final double angle = rng.nextDouble() * 2.0 * pi;
      final double speed = 85.0 + rng.nextDouble() * 175.0;
      final double jitter = 4.0 + rng.nextDouble() * 8.0;
      world.add(
        ExplosionParticleComponent(
          position: position + Vector2(cos(angle) * jitter, sin(angle) * jitter),
          velocity: Vector2(cos(angle) * speed, sin(angle) * speed),
          radius: 1.8 + rng.nextDouble() * 2.1,
          life: 0.18 + rng.nextDouble() * 0.22,
          color: sparkPalette[rng.nextInt(sparkPalette.length)],
        ),
      );
    }
  }

  void onAlienDestroyed(AlienShipComponent alien) {
    final AlienTierSpec spec = alien.spec;
    final int rewardCredits = alien.effectiveRewardCredits;
    final int rewardXp = alien.effectiveRewardXp;
    aliens.remove(alien);
    alien.removeFromParent();
    credits += rewardCredits;
    kills += 1;
    addXp(rewardXp);

    // Boss kills count toward their Sector's normal alien mission objectives
    switch (alien.tier) {
      case AlienTier.scout:
        recordMissionProgress(MissionGoalType.destroyScouts);
      case AlienTier.hunter:
        recordMissionProgress(MissionGoalType.destroyHunters);
      case AlienTier.destroyer:
        recordMissionProgress(MissionGoalType.destroyDestroyers);
      case AlienTier.dreadnoughtBoss:
        break;
    }

    spawnExplosion(alien.position, spec, isBossVariant: alien.isBoss);
    spawnFloatingText(
      position: alien.position + Vector2(0.0, -alien.effectiveRadius - 14.0),
      text: alien.isBoss
          ? 'BOSS DOWN! +$rewardCredits CR  +$rewardXp XP'
          : '+$rewardCredits CR  +$rewardXp XP',
      color: const Color(0xFFFBBF24),
      fontSize: (alien.isBoss || spec.tier == AlienTier.dreadnoughtBoss)
          ? 16.0
          : 13.5,
    );
    // Guaranteed Cargo Box + Salvageable Alien Wreck drop
    spawnCargoBox(alien.position, spec, isBoss: alien.isBoss);
    alienWrecks.add(
      AlienWreckEntity(
        id: _nextWorldEntityId++,
        sourceTier: alien.tier,
        fromBoss: alien.isBoss,
        x: alien.position.x + (rng.nextDouble() - 0.5) * 28.0,
        y: alien.position.y + (rng.nextDouble() - 0.5) * 28.0,
        rotationRad: alien.angle,
        maxCharges: 2 + rng.nextInt(3), // 2-4 salvage charges
      ),
    );
    addSkillXp(SkillType.weaponry, alien.isBoss ? 55 : 22);
    if (lockedTarget == alien) {
      lockedTarget = null;
    }
    spawnAlien(alien.tier);
    saveGame();
  }

  @override
  void onTapDown(TapDownEvent event) {
    super.onTapDown(event);
    if (isPinching) return;
    if (isFormationMenuOpen) {
      isFormationMenuOpen = false;
      return;
    }
    final Vector2 worldPos = camera.globalToLocal(event.canvasPosition);

    // 1. Check tapped Alien Ship
    AlienShipComponent? tapped;
    double bestDist = 74.0 / max(0.6, cameraZoom);
    for (final AlienShipComponent alien in aliens) {
      final double d = alien.position.distanceTo(worldPos);
      if (d <= max(bestDist, alien.effectiveRadius + 32.0)) {
        bestDist = d;
        tapped = alien;
      }
    }
    if (tapped != null) {
      lockedTarget = tapped;
      return;
    }

    // 2. Check tapped Sector Station (dock immediately if within range, or fly to it)
    for (final SectorStation st in activeStations) {
      final double d = hypot(st.worldX - worldPos.x, st.worldY - worldPos.y);
      if (d <= 85.0) {
        final double playerDist =
            hypot(st.worldX - player.position.x, st.worldY - player.position.y);
        if (playerDist <= st.interactRadius ||
            currentSector.isCapitalCity ||
            (currentSector.isNucleus && player.inSafeZone)) {
          openStation(initialTab: st.kind);
        } else {
          setAutopilotTarget(st.worldX, st.worldY);
        }
        return;
      }
    }

    // 3. Check tapped Resource Node or Alien Wreck -> steer directly to it
    for (final AlienWreckEntity w in alienWrecks) {
      if (hypot(w.x - worldPos.x, w.y - worldPos.y) <= w.radius + 36.0) {
        setAutopilotTarget(w.x, w.y);
        return;
      }
    }
    for (final ResourceNodeEntity n in resourceNodes) {
      if (hypot(n.x - worldPos.x, n.y - worldPos.y) <= n.radius + 36.0) {
        setAutopilotTarget(n.x, n.y);
        return;
      }
    }
  }
}

// =============================================================================
// SPACE MAP (8000-Radius Circular Map, 4 Concentric Zone Rings & Central Base)
// =============================================================================
class SpaceMapComponent extends Component with HasGameRef<DarkOrbitGame> {
  final List<_StarData> _sectorStars = <_StarData>[];

  @override
  Future<void> onLoad() async {
    final Random starRng = Random(42019);
    // Cover up to 22,000 x 22,000 (Nucleus large sector span)
    const double maxSpan = 22000.0;
    for (int i = 0; i < 2400; i++) {
      _sectorStars.add(
        _StarData(
          position: Offset(
            starRng.nextDouble() * maxSpan,
            starRng.nextDouble() * maxSpan,
          ),
          radius: 0.9 + starRng.nextDouble() * 2.0,
          alpha: 0.28 + starRng.nextDouble() * 0.7,
        ),
      );
    }
  }

  @override
  void render(Canvas canvas) {
    final SectorNode sector = gameRef.currentSector;
    final double mapSize = sector.mapSize;
    final Vector2 cam = gameRef.camera.viewfinder.position;
    final double invZoom = 1.0 / max(0.25, gameRef.cameraZoom);
    final double halfW = gameRef.size.x * 0.5 * invZoom + 120.0;
    final double halfH = gameRef.size.y * 0.5 * invZoom + 120.0;
    final double halfDiag = sqrt(halfW * halfW + halfH * halfH);

    final Paint starPaint = Paint();
    for (final _StarData star in _sectorStars) {
      if (star.position.dx > mapSize || star.position.dy > mapSize) continue;
      if ((star.position.dx - cam.x).abs() > halfW ||
          (star.position.dy - cam.y).abs() > halfH) {
        continue;
      }
      starPaint.color = const Color(0xFFE2E8F0).withOpacity(star.alpha);
      canvas.drawCircle(star.position, star.radius, starPaint);
    }

    _drawSquareSectorGridAndBorder(
      canvas,
      mapSize,
      sector.nodeColor,
      cam.x - halfW,
      cam.x + halfW,
      cam.y - halfH,
      cam.y + halfH,
    );

    if (sector.hasCentralSafeZone) {
      final Vector2 safeCenter = gameRef.activeSafeZoneCenter;
      final double dxSafe = cam.x - safeCenter.x;
      final double dySafe = cam.y - safeCenter.y;
      if (sqrt(dxSafe * dxSafe + dySafe * dySafe) <=
          sector.safeZoneRadius + halfDiag + 140.0) {
        _drawSectorCentralSafeZone(canvas, sector, safeCenter);
      }
    }

    _drawSectorStations(canvas);
    _drawResourceNodesAndWrecks(canvas);
    _drawAllSectorPortals(canvas, cam, halfW, halfH);

    // World-space Direct Tap-to-Move faint dashed trajectory & pulsing crosshair
    final Vector2? ap = gameRef.autopilotTarget;
    if (ap != null) {
      final Offset start =
          Offset(gameRef.player.position.x, gameRef.player.position.y);
      final Offset end = Offset(ap.x, ap.y);
      final double dx = end.dx - start.dx;
      final double dy = end.dy - start.dy;
      final double totalDist = sqrt(dx * dx + dy * dy);
      if (totalDist > 10.0) {
        final double ux = dx / totalDist;
        final double uy = dy / totalDist;
        final Paint dashPaint = Paint()
          ..color = const Color(0x5500E5FF)
          ..strokeWidth = 2.0;
        final double maxDraw = min(totalDist, halfDiag + 200.0);
        double d = 24.0;
        while (d < maxDraw) {
          final double dEnd = min(maxDraw, d + 18.0);
          canvas.drawLine(
            Offset(start.dx + ux * d, start.dy + uy * d),
            Offset(start.dx + ux * dEnd, start.dy + uy * dEnd),
            dashPaint,
          );
          d += 34.0;
        }
      }
      if (gameRef.isInViewport(ap, margin: 60.0)) {
        final double pulse = 0.55 + 0.45 * sin(gameRef.elapsedTime * 6.0);
        final double r = 18.0 + 6.0 * pulse;
        canvas.drawCircle(
          end,
          r,
          Paint()
            ..color = const Color(0xFF00E5FF).withOpacity(0.45 + 0.45 * pulse)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 2.2,
        );
        final Paint cross = Paint()
          ..color = const Color(0xCC00E5FF)
          ..strokeWidth = 1.6;
        canvas.drawLine(
          Offset(end.dx - r - 8.0, end.dy),
          Offset(end.dx + r + 8.0, end.dy),
          cross,
        );
        canvas.drawLine(
          Offset(end.dx, end.dy - r - 8.0),
          Offset(end.dx, end.dy + r + 8.0),
          cross,
        );
        canvas.drawCircle(end, 3.5, Paint()..color = Colors.white);
      }
    }
  }

  void _drawSectorStations(Canvas canvas) {
    final double pulse = 0.7 + 0.3 * sin(gameRef.elapsedTime * 4.0);
    for (final SectorStation st in gameRef.activeStations) {
      final Vector2 pos = Vector2(st.worldX, st.worldY);
      if (!gameRef.isInViewport(pos, margin: 180.0)) continue;
      final Offset c = Offset(st.worldX, st.worldY);
      canvas.drawCircle(
        c,
        st.interactRadius * 0.62,
        Paint()..color = st.accentColor.withOpacity(0.10),
      );
      canvas.drawCircle(
        c,
        st.interactRadius * 0.62,
        Paint()
          ..color = st.accentColor.withOpacity(0.55 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.0,
      );
      final Path hex = Path();
      for (int i = 0; i < 6; i++) {
        final double a = i * pi / 3.0 + gameRef.elapsedTime * 0.25;
        final double x = c.dx + cos(a) * 34.0;
        final double y = c.dy + sin(a) * 34.0;
        if (i == 0) {
          hex.moveTo(x, y);
        } else {
          hex.lineTo(x, y);
        }
      }
      hex.close();
      canvas.drawPath(hex, Paint()..color = const Color(0xFF0E1A30));
      canvas.drawPath(
        hex,
        Paint()
          ..color = st.accentColor
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.4,
      );
      canvas.drawCircle(c, 8.0, Paint()..color = st.accentColor);
      TextPaint(
        style: TextStyle(
          color: st.accentColor,
          fontSize: 11.5,
          fontWeight: FontWeight.bold,
          letterSpacing: 0.8,
        ),
      ).render(
        canvas,
        st.name,
        Vector2(c.dx, c.dy + 48.0),
        anchor: Anchor.center,
      );
    }
  }

  void _drawResourceNodesAndWrecks(Canvas canvas) {
    final double t = gameRef.elapsedTime;
    final Offset playerPos =
        Offset(gameRef.player.position.x, gameRef.player.position.y);

    // 1. Resource Nodes (with charge pips & mining beam)
    for (final ResourceNodeEntity node in gameRef.resourceNodes) {
      final Vector2 v = Vector2(node.x, node.y);
      if (!gameRef.isInViewport(v, margin: 70.0)) continue;
      final ResourceSpec spec = ResourceCatalog.of(node.resourceType);
      final Offset c = Offset(node.x, node.y);
      final double pulse = 0.72 + 0.28 * sin(t * 3.5 + node.pulsePhase);

      canvas.drawCircle(
        c,
        node.radius * 1.35,
        Paint()..color = spec.color.withOpacity(0.14 * pulse),
      );
      final Path rock = Path();
      const int sides = 6;
      for (int i = 0; i < sides; i++) {
        final double a = node.rotationRad + i * (2.0 * pi / sides);
        final double r = node.radius * (i.isEven ? 1.0 : 0.76);
        final double x = c.dx + cos(a) * r;
        final double y = c.dy + sin(a) * r;
        if (i == 0) {
          rock.moveTo(x, y);
        } else {
          rock.lineTo(x, y);
        }
      }
      rock.close();
      canvas.drawPath(rock, Paint()..color = const Color(0xFF162033));
      canvas.drawPath(
        rock,
        Paint()
          ..color = spec.color
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.0,
      );
      canvas.drawCircle(c, 4.5, Paint()..color = spec.color);

      // Charge pips below node
      final double pipSpan = (node.maxCharges - 1) * 7.5;
      for (int i = 0; i < node.maxCharges; i++) {
        canvas.drawCircle(
          Offset(c.dx - pipSpan * 0.5 + i * 7.5, c.dy + node.radius + 9.0),
          2.5,
          Paint()
            ..color = i < node.charges
                ? spec.color
                : const Color(0xFF334155),
        );
      }

      if (gameRef.activeMiningNode == node) {
        canvas.drawLine(
          playerPos,
          c,
          Paint()
            ..color = spec.color.withOpacity(0.85)
            ..strokeWidth = 3.2,
        );
        canvas.drawLine(
          playerPos,
          c,
          Paint()
            ..color = Colors.white
            ..strokeWidth = 1.2,
        );
      }
    }

    // 2. Salvageable Alien Wrecks
    for (final AlienWreckEntity w in gameRef.alienWrecks) {
      final Vector2 v = Vector2(w.x, w.y);
      if (!gameRef.isInViewport(v, margin: 70.0)) continue;
      final Offset c = Offset(w.x, w.y);
      final double pulse = 0.65 + 0.35 * sin(t * 4.5 + w.pulsePhase);
      canvas.drawCircle(
        c,
        w.radius * 1.25,
        Paint()..color = const Color(0xFFC084FC).withOpacity(0.16 * pulse),
      );
      final Path hull = Path()
        ..moveTo(c.dx + w.radius, c.dy - 4.0)
        ..lineTo(c.dx + 4.0, c.dy - w.radius * 0.85)
        ..lineTo(c.dx - w.radius * 0.8, c.dy - 6.0)
        ..lineTo(c.dx - w.radius * 0.4, c.dy + w.radius * 0.75)
        ..lineTo(c.dx + w.radius * 0.6, c.dy + 5.0)
        ..close();
      canvas.drawPath(hull, Paint()..color = const Color(0xFF2E1065));
      canvas.drawPath(
        hull,
        Paint()
          ..color = const Color(0xFFC084FC)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
      if (gameRef.activeSalvageWreck == w) {
        canvas.drawLine(
          playerPos,
          c,
          Paint()
            ..color = const Color(0xFFE879F9)
            ..strokeWidth = 3.0,
        );
      }
    }

    // 3. Player Ship Wrecks (60% recoverable loot)
    for (final PlayerShipWreck pw in gameRef.playerWrecks) {
      if (pw.sectorId != gameRef.currentSectorId) continue;
      final Vector2 v = Vector2(pw.x, pw.y);
      if (!gameRef.isInViewport(v, margin: 80.0)) continue;
      final Offset c = Offset(pw.x, pw.y);
      final double pulse = 0.55 + 0.45 * sin(t * 5.0);
      canvas.drawCircle(
        c,
        28.0 + 6.0 * pulse,
        Paint()
          ..color = const Color(0xFF4ADE80).withOpacity(0.75 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.4,
      );
      canvas.drawCircle(c, 10.0, Paint()..color = const Color(0xFF22C55E));
      TextPaint(
        style: const TextStyle(
          color: Color(0xFF86EFAC),
          fontSize: 10.5,
          fontWeight: FontWeight.bold,
        ),
      ).render(
        canvas,
        'WRECK (${pw.totalCargoUnits})',
        Vector2(c.dx, c.dy - 36.0),
        anchor: Anchor.center,
      );
    }
  }

  void _drawSquareSectorGridAndBorder(
    Canvas canvas,
    double mapSize,
    Color accentColor,
    double minX,
    double maxX,
    double minY,
    double maxY,
  ) {
    const double step = 1000.0;

    final double clipTop = max(0.0, minY);
    final double clipBottom = min(mapSize, maxY);
    final double clipLeft = max(0.0, minX);
    final double clipRight = min(mapSize, maxX);

    final Paint minorGrid = Paint()
      ..color = accentColor.withOpacity(0.08)
      ..strokeWidth = 1.0;
    final Paint majorGrid = Paint()
      ..color = accentColor.withOpacity(0.15)
      ..strokeWidth = 1.5;

    if (clipBottom > clipTop) {
      final double startGx = (clipLeft / step).floor() * step;
      for (double gx = startGx; gx <= clipRight; gx += step) {
        if (gx > 0.0 && gx < mapSize) {
          final bool isMajor = ((gx / step).round() % 2 == 0);
          canvas.drawLine(
            Offset(gx, clipTop),
            Offset(gx, clipBottom),
            isMajor ? majorGrid : minorGrid,
          );
        }
      }
    }

    if (clipRight > clipLeft) {
      final double startGy = (clipTop / step).floor() * step;
      for (double gy = startGy; gy <= clipBottom; gy += step) {
        if (gy > 0.0 && gy < mapSize) {
          final bool isMajor = ((gy / step).round() % 2 == 0);
          canvas.drawLine(
            Offset(clipLeft, gy),
            Offset(clipRight, gy),
            isMajor ? majorGrid : minorGrid,
          );
        }
      }
    }

    const double radBand = GameConstants.mapX1RadiationOuterBuffer + 640.0;
    final Paint radFill = Paint()..color = const Color(0x26FF1744);
    if (minY < 0.0) {
      canvas.drawRect(
        Rect.fromLTWH(-radBand, -radBand, mapSize + radBand * 2.0, radBand),
        radFill,
      );
    }
    if (maxY > mapSize) {
      canvas.drawRect(
        Rect.fromLTWH(-radBand, mapSize, mapSize + radBand * 2.0, radBand),
        radFill,
      );
    }
    if (minX < 0.0) {
      canvas.drawRect(
        Rect.fromLTWH(-radBand, 0.0, radBand, mapSize),
        radFill,
      );
    }
    if (maxX > mapSize) {
      canvas.drawRect(
        Rect.fromLTWH(mapSize, 0.0, radBand, mapSize),
        radFill,
      );
    }

    final double pulse = 0.65 + 0.35 * sin(gameRef.elapsedTime * 5.2);
    final Rect borderRect = Rect.fromLTWH(0.0, 0.0, mapSize, mapSize);
    canvas.drawRect(
      borderRect,
      Paint()
        ..color = const Color(0xFFFF2E4C).withOpacity(0.24 * pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 24.0,
    );
    canvas.drawRect(
      borderRect,
      Paint()
        ..color = const Color(0xEBFF2E4C)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 5.0,
    );
  }

  void _drawSectorCentralSafeZone(
    Canvas canvas,
    SectorNode sector,
    Vector2 safeWorld,
  ) {
    final Offset safeCenter = Offset(safeWorld.x, safeWorld.y);
    final double safeR = sector.safeZoneRadius;
    final double pulse = 0.72 + 0.28 * sin(gameRef.elapsedTime * 3.0);
    final Color ringColor = sector.isNucleus
        ? const Color(0xFFFACC15)
        : (sector.isCapitalCity ? sector.nodeColor : const Color(0xFF00E5FF));

    canvas.drawCircle(
      safeCenter,
      safeR,
      Paint()..color = const Color(0x1600E5FF),
    );
    canvas.drawCircle(
      safeCenter,
      safeR,
      Paint()
        ..color = ringColor.withOpacity(0.22 * pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 14.0,
    );
    canvas.drawCircle(
      safeCenter,
      safeR,
      Paint()
        ..color = ringColor.withOpacity(0.85)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3.2,
    );
    if (sector.isNucleus) {
      // Inner cyan safe-zone border inside Nucleus gold perimeter
      canvas.drawCircle(
        safeCenter,
        safeR * 0.92,
        Paint()
          ..color = const Color(0xCC00E5FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.0,
      );
    }

    // Station hub at exact center
    canvas.save();
    canvas.translate(safeCenter.dx, safeCenter.dy);
    canvas.rotate(gameRef.elapsedTime * 0.14);
    final Paint armPaint = Paint()
      ..color = const Color(0xFF1E293B)
      ..strokeWidth = 10.0
      ..strokeCap = StrokeCap.round;
    final Paint armAccent = Paint()
      ..color = ringColor.withOpacity(0.82)
      ..strokeWidth = 2.0
      ..strokeCap = StrokeCap.round;
    for (int i = 0; i < 4; i++) {
      final double a = i * pi * 0.5;
      final Offset p1 = Offset(cos(a) * 34.0, sin(a) * 34.0);
      final Offset p2 = Offset(cos(a) * 106.0, sin(a) * 106.0);
      canvas.drawLine(p1, p2, armPaint);
      canvas.drawLine(p1, p2, armAccent);
      canvas.drawCircle(p2, 4.0, Paint()..color = ringColor);
    }
    canvas.restore();

    final Path hub = Path();
    for (int i = 0; i < 8; i++) {
      final double a = i * (2.0 * pi / 8.0) + pi / 8.0;
      final double x = safeCenter.dx + cos(a) * 42.0;
      final double y = safeCenter.dy + sin(a) * 42.0;
      if (i == 0) {
        hub.moveTo(x, y);
      } else {
        hub.lineTo(x, y);
      }
    }
    hub.close();
    canvas.drawPath(hub, Paint()..color = const Color(0xFF0F1C33));
    canvas.drawPath(
      hub,
      Paint()
        ..color = ringColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2,
    );
    canvas.drawCircle(
      safeCenter,
      12.0 * (0.9 + 0.1 * pulse),
      Paint()..color = const Color(0xFF00E5FF),
    );
    canvas.drawCircle(safeCenter, 5.0, Paint()..color = Colors.white);

    final String baseTitle = sector.isNucleus
        ? 'CENTRAL BLACK MARKET • SAFE ZONE'
        : (sector.isCapitalCity
            ? '${sector.name.toUpperCase()} • SAFE ZONE'
            : 'OUTPOST SAFE ZONE');
    TextPaint(
      style: TextStyle(
        color: ringColor,
        fontSize: 12.5,
        fontWeight: FontWeight.bold,
        shadows: const <Shadow>[
          Shadow(color: Color(0xE0020612), offset: Offset(1.0, 1.0), blurRadius: 3.0),
        ],
      ),
    ).render(
      canvas,
      baseTitle,
      Vector2(safeCenter.dx, safeCenter.dy + 64.0),
      anchor: Anchor.center,
    );
  }

  void _drawAllSectorPortals(
    Canvas canvas,
    Vector2 cam,
    double halfW,
    double halfH,
  ) {
    final SectorPortal? nearest = gameRef.nearestJumpPortal;
    final String? nextHop = gameRef.nextRouteHopSectorId;
    for (final SectorPortal portal in gameRef.activePortals) {
      if ((portal.worldX - cam.x).abs() > halfW + 130.0 ||
          (portal.worldY - cam.y).abs() > halfH + 130.0) {
        continue;
      }
      _drawSinglePortal(
        canvas,
        portal,
        nearGate: nearest?.targetSectorId == portal.targetSectorId,
        isNextRouteHop: portal.targetSectorId == nextHop,
      );
    }
  }

  void _drawSinglePortal(
    Canvas canvas,
    SectorPortal portal, {
    required bool nearGate,
    required bool isNextRouteHop,
  }) {
    final Offset c = Offset(portal.worldX, portal.worldY);
    final double t = gameRef.elapsedTime;
    final double pulse = 0.72 + 0.28 * sin(t * 4.4);
    final Color primary = isNextRouteHop
        ? const Color(0xFF00E5FF)
        : portal.accentColor;
    const double outerR = 46.0;
    const double innerR = 32.0;

    // Ambient energy bloom
    canvas.drawCircle(
      c,
      nearGate ? 98.0 : (isNextRouteHop ? 92.0 : 80.0),
      Paint()
        ..shader = ui.Gradient.radial(
          c,
          nearGate ? 98.0 : (isNextRouteHop ? 92.0 : 80.0),
          <Color>[
            primary.withOpacity(nearGate ? 0.48 : 0.30),
            const Color(0xFF6366F1).withOpacity(nearGate ? 0.24 : 0.14),
            const Color(0x00000000),
          ],
          const <double>[0.0, 0.55, 1.0],
        ),
    );

    if (nearGate || isNextRouteHop) {
      canvas.drawCircle(
        c,
        GameConstants.jumpGateInteractRadius,
        Paint()
          ..color = primary.withOpacity((nearGate ? 0.42 : 0.24) * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = nearGate ? 2.0 : 1.5,
      );
    }

    // Inner swirling warp core
    canvas.drawCircle(
      c,
      innerR,
      Paint()
        ..shader = ui.Gradient.radial(
          c,
          innerR,
          <Color>[
            Colors.white.withOpacity(0.85 * pulse),
            primary.withOpacity(0.68),
            const Color(0x734F46E5),
            const Color(0xB80A1026),
          ],
          const <double>[0.0, 0.35, 0.7, 1.0],
        ),
    );

    // Rotating outer metallic stator ring + 3 glowing arcs + 6 pylons
    canvas.save();
    canvas.translate(c.dx, c.dy);
    canvas.rotate(t * 0.6);
    canvas.drawCircle(
      Offset.zero,
      outerR,
      Paint()
        ..color = const Color(0xFF1E293B)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 7.5,
    );
    canvas.drawCircle(
      Offset.zero,
      outerR + 3.2,
      Paint()
        ..color = const Color(0xC794A3B8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
    canvas.drawCircle(
      Offset.zero,
      outerR - 3.6,
      Paint()
        ..color = primary.withOpacity(0.75)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5,
    );
    final Rect arcRect = Rect.fromCircle(center: Offset.zero, radius: outerR);
    final Paint arcPaint = Paint()
      ..color = primary.withOpacity(0.90)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3.2
      ..strokeCap = StrokeCap.round;
    for (int i = 0; i < 3; i++) {
      canvas.drawArc(arcRect, i * (2.0 * pi / 3.0) + 0.2, 1.25, false, arcPaint);
    }
    for (int i = 0; i < 6; i++) {
      final double a = i * (pi / 3.0);
      final Offset node = Offset(cos(a) * outerR, sin(a) * outerR);
      canvas.drawCircle(node, 5.4, Paint()..color = const Color(0xFF0F172A));
      canvas.drawCircle(
        node,
        3.0,
        Paint()
          ..color = i.isEven ? primary : const Color(0xFFA78BFA),
      );
    }
    canvas.restore();

    // Counter-rotating inner holographic ring
    canvas.save();
    canvas.translate(c.dx, c.dy);
    canvas.rotate(-t * 1.02);
    final Rect innerRect =
        Rect.fromCircle(center: Offset.zero, radius: innerR - 5.0);
    final Paint innerArcPaint = Paint()
      ..color = Colors.white.withOpacity(0.85 * pulse)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0
      ..strokeCap = StrokeCap.round;
    for (int i = 0; i < 2; i++) {
      canvas.drawArc(innerRect, i * pi, 1.4, false, innerArcPaint);
    }
    canvas.drawCircle(
      Offset.zero,
      innerR + 3.0,
      Paint()
        ..color = primary.withOpacity(0.82)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );
    canvas.restore();

    final String label = isNextRouteHop
        ? '★ ${portal.gateLabel} (ROUTE)'
        : portal.gateLabel;
    final TextPaint labelTp = TextPaint(
      style: TextStyle(
        color: nearGate || isNextRouteHop ? Colors.white : primary,
        fontSize: 12.0,
        fontWeight: FontWeight.bold,
        shadows: const <Shadow>[
          Shadow(color: Color(0xD9040711), offset: Offset(1.0, 1.0), blurRadius: 2.0),
        ],
      ),
    );
    labelTp.render(
      canvas,
      label,
      Vector2(c.dx, c.dy + outerR + 16.0),
      anchor: Anchor.center,
    );
  }
}

class _StarData {
  const _StarData({
    required this.position,
    required this.radius,
    required this.alpha,
  });

  final Offset position;
  final double radius;
  final double alpha;
}

// =============================================================================
// PLAYER SHIP COMPONENT (3 Distinct Ship Class Silhouettes, Shield, Radiation)
// =============================================================================
class PlayerShipComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  PlayerShipComponent({required super.position})
      : super(anchor: Anchor.center, size: Vector2.all(60.0));

  int level = 1;
  int xp = 0;
  double hp = 140.0;
  double shield = 120.0;
  double fireCooldown = 0.0;
  double rocketCooldown = 0.0;
  double radiationTickTimer = 0.0;
  double timeSinceDamaged = GameConstants.shieldRegenDelay;
  double shieldHitFlash = 0.0;
  double hitFlash = 0.0;
  double thrustFactor = 0.0;
  double thrusterPhase = 0.0;
  bool isTakingZoneRadiation = false;
  String? requiredChipWarning;

  bool get inSafeZone => gameRef.isPointInActiveSafeZone(position);

  @override
  void update(double dt) {
    super.update(dt);

    if (rocketCooldown > 0.0) {
      rocketCooldown = max(0.0, rocketCooldown - dt);
    }

    double? movementAngle;
    double targetThrust = 0.0;
    final Vector2 move = gameRef.moveJoystick.relativeDelta;
    final double moveMag = move.length;
    if (moveMag > 0.12) {
      // Instant manual override cancels Autopilot
      if (gameRef.autopilotTarget != null) {
        gameRef.cancelAutopilot();
      }
      position.add(move.normalized() * (gameRef.currentSpeed * dt));
      movementAngle = atan2(move.y, move.x);
      targetThrust = moveMag.clamp(0.45, 1.0);
    } else if (gameRef.autopilotTarget != null) {
      final Vector2 toAp = gameRef.autopilotTarget! - position;
      final double apDist = toAp.length;
      if (apDist <= GameConstants.autopilotStopRadius) {
        gameRef.cancelAutopilot();
        targetThrust = 0.0;
      } else {
        movementAngle = atan2(toAp.y, toAp.x);
        final double step = min(apDist, gameRef.currentSpeed * dt);
        position.add(toAp.normalized() * step);
        targetThrust = 1.0;
      }
    }
    final double thrustLerp = 1.0 - exp(-10.0 * dt);
    thrustFactor += (targetThrust - thrustFactor) * thrustLerp;
    if (thrustFactor < 0.005) thrustFactor = 0.0;
    thrusterPhase += dt * (8.0 + 18.0 * thrustFactor);

    // Combat Facing Override: smoothly face locked alien whenever actively attacking
    final AlienShipComponent? combatTarget = gameRef.lockedTarget;
    final bool isActivelyAttacking = gameRef.autoFireEnabled &&
        !inSafeZone &&
        combatTarget != null &&
        !combatTarget.isRemoved;
    if (isActivelyAttacking) {
      final double aimAngle = atan2(
        combatTarget.position.y - position.y,
        combatTarget.position.x - position.x,
      );
      final double diff = gameRef._normalizeAngle(aimAngle - angle);
      angle = gameRef._normalizeAngle(
        angle + diff * (1.0 - exp(-14.0 * dt)),
      );
    } else if (movementAngle != null) {
      final double diff = gameRef._normalizeAngle(movementAngle - angle);
      angle = gameRef._normalizeAngle(
        angle + diff * (1.0 - exp(-11.0 * dt)),
      );
    }

    final double mapSize = gameRef.activeMapSize;
    const double minOuter = -GameConstants.mapX1RadiationOuterBuffer;
    final double maxOuter = mapSize + GameConstants.mapX1RadiationOuterBuffer;
    position.x = position.x.clamp(minOuter, maxOuter);
    position.y = position.y.clamp(minOuter, maxOuter);

    final bool outsideSquare = position.x < 0.0 ||
        position.x > mapSize ||
        position.y < 0.0 ||
        position.y > mapSize;
    isTakingZoneRadiation = !inSafeZone && outsideSquare;
    requiredChipWarning = null;

    if (isTakingZoneRadiation) {
      radiationTickTimer += dt;
      if (radiationTickTimer >= GameConstants.radiationTickInterval) {
        radiationTickTimer -= GameConstants.radiationTickInterval;
        takeDamage(
          GameConstants.mapX1BorderRadiationDps *
              GameConstants.radiationTickInterval,
          isRadiation: true,
        );
      }
    } else {
      radiationTickTimer = 0.0;
    }

    if (inSafeZone && gameRef.autoFireEnabled) {
      gameRef.autoFireEnabled = false;
    }

    if (shieldHitFlash > 0.0) {
      shieldHitFlash = max(0.0, shieldHitFlash - dt * 4.0);
    }
    if (hitFlash > 0.0) {
      hitFlash = max(0.0, hitFlash - dt * 4.0);
    }

    timeSinceDamaged += dt;
    if (inSafeZone) {
      hp = min(
        gameRef.currentMaxHull,
        hp + GameConstants.safeZoneHpRegenRate * dt,
      );
      shield = min(
        gameRef.currentMaxShield,
        shield + GameConstants.safeZoneShieldRegenRate * dt,
      );
    } else {
      if (timeSinceDamaged >= GameConstants.shieldRegenDelay) {
        shield = min(
          gameRef.currentMaxShield,
          shield + GameConstants.shieldRegenRate * dt,
        );
      }
      // DIAMOND formation passive continuous combat shield regen (+1.5% Max Shield/sec)
      final double diamondPct = gameRef.activeFormationSpec.combatShieldRegenPct;
      if (diamondPct > 0.0 && shield < gameRef.currentMaxShield) {
        shield = min(
          gameRef.currentMaxShield,
          shield + (gameRef.currentMaxShield * diamondPct) * dt,
        );
      }
    }

    if (fireCooldown > 0.0) {
      fireCooldown = max(0.0, fireCooldown - dt);
    }
    final AlienShipComponent? target = gameRef.lockedTarget;
    if (!inSafeZone &&
        gameRef.autoFireEnabled &&
        target != null &&
        !target.isRemoved &&
        fireCooldown <= 0.0 &&
        position.distanceTo(target.position) <=
            GameConstants.playerAttackRange) {
      fireCooldown = GameConstants.playerFireInterval;
      final Vector2 dir = (target.position - position).normalized();
      final double fireAngle = atan2(dir.y, dir.x);
      gameRef.world.add(
        LaserBoltComponent(
          position: position + dir * 16.0,
          velocity: dir * GameConstants.playerLaserSpeed,
          angle: fireAngle,
          fromPlayer: true,
          damage: gameRef.currentLaserDamage,
          color: gameRef.activeShipAccentColor,
          homingTarget: target,
        ),
      );
    }
  }

  void takeDamage(double amount, {bool isRadiation = false}) {
    if (inSafeZone) return;
    timeSinceDamaged = 0.0;
    final bool hitShieldOnly = shield >= amount;
    double remaining = amount;
    if (shield > 0.0) {
      final double absorbed = min(shield, remaining);
      shield -= absorbed;
      remaining -= absorbed;
      shieldHitFlash = 1.0;
    }
    if (remaining > 0.0) {
      hp -= remaining;
      hitFlash = 1.0;
      gameRef.spawnHullHitSparks(position, count: isRadiation ? 7 : 13);
    }
    gameRef.spawnFloatingText(
      position: position +
          Vector2(
            (gameRef.rng.nextDouble() - 0.5) * 16.0,
            -gameRef.activeHullRadius - 28.0,
          ),
      text: isRadiation ? '-${amount.round()} RAD' : '-${amount.round()}',
      color: isRadiation
          ? const Color(0xFFFF2E4C)
          : (hitShieldOnly ? const Color(0xFF38BDF8) : const Color(0xFFFF4D6D)),
      fontSize: 13.5,
    );
    if (hp <= 0.0) {
      gameRef.handlePlayerDeath();
    }
  }

  @override
  void render(Canvas canvas) {
    final Offset center = Offset(size.x * 0.5, size.y * 0.5);
    final double hullR = gameRef.activeHullRadius;

    // Radar Cone (only while right joystick is actively tilted)
    final Vector2 aim = gameRef.aimJoystick.relativeDelta;
    if (aim.length > 0.12) {
      final double aimAngle = atan2(aim.y, aim.x) - angle;
      const double coneR = GameConstants.radarConeRange;
      const double halfSweep = GameConstants.radarConeHalfAngleRad;
      final Rect arcRect = Rect.fromCircle(center: center, radius: coneR);

      canvas.drawArc(
        arcRect,
        aimAngle - halfSweep,
        halfSweep * 2.0,
        true,
        Paint()..color = const Color(0x28FF2E4C),
      );
      final Paint edgePaint = Paint()
        ..color = const Color(0xCCFF4D6D)
        ..strokeWidth = 2.0
        ..strokeCap = StrokeCap.round;
      canvas.drawLine(
        center,
        Offset(
          center.dx + cos(aimAngle - halfSweep) * coneR,
          center.dy + sin(aimAngle - halfSweep) * coneR,
        ),
        edgePaint,
      );
      canvas.drawLine(
        center,
        Offset(
          center.dx + cos(aimAngle + halfSweep) * coneR,
          center.dy + sin(aimAngle + halfSweep) * coneR,
        ),
        edgePaint,
      );
      canvas.drawArc(
        arcRect,
        aimAngle - halfSweep,
        halfSweep * 2.0,
        false,
        Paint()
          ..color = const Color(0xEEFF5C77)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.2,
      );
    }

    // Explicitly branch on ShipRole & IndustrialType with Dynamic LOD (cameraZoom >= 1.35x)
    ShipVectorRenderer.drawShip(
      canvas: canvas,
      center: center,
      role: gameRef.activeShipRole,
      combatClass: gameRef.activeShipClass,
      industrialType: gameRef.activeIndustrialType,
      thrustFactor: thrustFactor,
      thrusterPhase: thrusterPhase,
      cameraZoom: gameRef.cameraZoom,
    );

    // Translucent cyan/blue energy shield bubble ring on Shield Hit (~0.25s fade)
    if (shieldHitFlash > 0.01) {
      final double alpha = shieldHitFlash.clamp(0.0, 1.0);
      final double bubbleR = hullR + 11.0 + (1.0 - alpha) * 4.0;
      canvas.drawCircle(
        center,
        bubbleR,
        Paint()
          ..shader = ui.Gradient.radial(
            center,
            bubbleR,
            <Color>[
              const Color(0x0000E5FF),
              Color.fromRGBO(2, 132, 199, 0.08 * alpha),
              Color.fromRGBO(0, 229, 255, 0.32 * alpha),
              Color.fromRGBO(125, 211, 252, 0.55 * alpha),
            ],
            const <double>[0.0, 0.52, 0.82, 1.0],
          ),
      );
      canvas.drawCircle(
        center,
        bubbleR,
        Paint()
          ..color = const Color(0xFF38BDF8).withOpacity(0.90 * alpha)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.4,
      );
      canvas.drawCircle(
        center,
        bubbleR - 2.6,
        Paint()
          ..color = const Color(0xFFE0F2FE).withOpacity(0.55 * alpha)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.0,
      );
    }

    // Blue Shield bar directly above Green Hull bar (world-upright)
    canvas.save();
    canvas.translate(center.dx, center.dy);
    canvas.rotate(-angle);
    _drawBar(
      canvas,
      Offset(0.0, -hullR - 25.0),
      54.0,
      5.0,
      shield / max(1.0, gameRef.currentMaxShield),
      const Color(0xFF38BDF8),
    );
    _drawBar(
      canvas,
      Offset(0.0, -hullR - 17.0),
      54.0,
      5.5,
      hp / max(1.0, gameRef.currentMaxHull),
      const Color(0xFF22C55E),
    );
    final String? callsign = gameRef.formattedPilotCallsign;
    if (callsign != null && callsign.isNotEmpty) {
      final Color tagColor =
          gameRef.pilotCompanySpec?.primaryColor ?? const Color(0xFF00E5FF);
      final TextPaint callsignPaint = TextPaint(
        style: TextStyle(
          color: tagColor,
          fontSize: 12.0,
          fontWeight: FontWeight.w800,
          letterSpacing: 0.4,
          shadows: const <Shadow>[
            Shadow(
              color: Color(0xEE030612),
              offset: Offset(1.0, 1.0),
              blurRadius: 2.5,
            ),
            Shadow(
              color: Color(0xCC030612),
              offset: Offset(-1.0, -1.0),
              blurRadius: 2.0,
            ),
          ],
        ),
      );
      callsignPaint.render(
        canvas,
        callsign,
        Vector2(0.0, hullR + 10.0),
        anchor: Anchor.topCenter,
      );
    }
    canvas.restore();
  }

  void _drawShipClassSilhouette(
    Canvas canvas,
    Offset c,
    ShipClassSpec spec,
  ) {
    final double thrust = thrustFactor.clamp(0.0, 1.0);
    final double pulse = thrusterPhase;
    final double time = gameRef.elapsedTime;
    final double flash = hitFlash.clamp(0.0, 1.0);

    switch (spec.shipClass) {
      case ShipClass.interceptor:
        _drawInterceptorShip(canvas, c.dx, c.dy, thrust, pulse, time, flash);
      case ShipClass.battlecruiser:
        _drawBattlecruiserShip(canvas, c.dx, c.dy, thrust, pulse, time, flash);
      case ShipClass.dreadnought:
        _drawTankCitadelShip(canvas, c.dx, c.dy, thrust, pulse, time, flash);
    }
  }

  void _drawInterceptorShip(
    Canvas canvas,
    double cx,
    double cy,
    double thrust,
    double pulse,
    double time,
    double flash,
  ) {
    // 2 rear thrusters (Vengeance / Pusat twin-engine bank)
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 14.5, cy - 5.8),
      thrust: thrust,
      maxLen: 24.0,
      halfW: 3.8,
      pulse: pulse,
      phaseOffset: 0.0,
    );
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 14.5, cy + 5.8),
      thrust: thrust,
      maxLen: 24.0,
      halfW: 3.8,
      pulse: pulse,
      phaseOffset: 1.9,
    );

    // Twin armored exhaust nozzles
    for (final double sign in const <double>[-1.0, 1.0]) {
      final double ny = cy + sign * 5.8;
      final Path nozzle = Path()
        ..moveTo(cx - 10.5, ny - 2.7)
        ..lineTo(cx - 15.2, ny - 2.2)
        ..lineTo(cx - 15.2, ny + 2.2)
        ..lineTo(cx - 10.5, ny + 2.7)
        ..close();
      canvas.drawPath(nozzle, Paint()..color = const Color(0xFF1E293B));
      canvas.drawPath(
        nozzle,
        Paint()
          ..color = const Color(0xFF64748B)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.9,
      );
    }

    // Swept-back razor delta wings (dark metallic silver/slate gradient)
    final Path mainWings = Path()
      ..moveTo(cx + 26.5, cy)
      ..lineTo(cx + 11.0, cy - 5.5)
      ..lineTo(cx + 1.0, cy - 11.5)
      ..lineTo(cx - 11.5, cy - 21.5)
      ..lineTo(cx - 18.0, cy - 17.5)
      ..lineTo(cx - 12.5, cy - 10.0)
      ..lineTo(cx - 13.5, cy - 3.2)
      ..lineTo(cx - 10.5, cy)
      ..lineTo(cx - 13.5, cy + 3.2)
      ..lineTo(cx - 12.5, cy + 10.0)
      ..lineTo(cx - 18.0, cy + 17.5)
      ..lineTo(cx - 11.5, cy + 21.5)
      ..lineTo(cx + 1.0, cy + 11.5)
      ..lineTo(cx + 11.0, cy + 5.5)
      ..close();
    canvas.drawPath(
      mainWings,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx, cy - 22.0),
          Offset(cx, cy + 22.0),
          const <Color>[
            Color(0xFF334155),
            Color(0xFF94A3B8),
            Color(0xFF475569),
            Color(0xFF1E293B),
          ],
          const <double>[0.0, 0.36, 0.68, 1.0],
        ),
    );
    canvas.drawPath(
      mainWings,
      Paint()
        ..color = const Color(0xCCCBD5E1)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.25,
    );

    // Recessed dark mid-wing armor plates
    for (final double sign in const <double>[-1.0, 1.0]) {
      final Path wingPlate = Path()
        ..moveTo(cx + 4.0, cy + sign * 6.5)
        ..lineTo(cx - 9.5, cy + sign * 17.5)
        ..lineTo(cx - 13.5, cy + sign * 15.0)
        ..lineTo(cx - 7.5, cy + sign * 6.5)
        ..close();
      canvas.drawPath(
        wingPlate,
        Paint()..color = const Color(0xB80F172A),
      );
      canvas.drawPath(
        wingPlate,
        Paint()
          ..color = const Color(0xA664748B)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.85,
      );
    }

    // Raised razor central fuselage spine (bright metallic silver gradient)
    final Path fuselage = Path()
      ..moveTo(cx + 26.0, cy)
      ..lineTo(cx + 9.0, cy - 5.2)
      ..lineTo(cx - 9.5, cy - 6.6)
      ..lineTo(cx - 13.5, cy - 2.6)
      ..lineTo(cx - 11.0, cy)
      ..lineTo(cx - 13.5, cy + 2.6)
      ..lineTo(cx - 9.5, cy + 6.6)
      ..lineTo(cx + 9.0, cy + 5.2)
      ..close();
    canvas.drawPath(
      fuselage,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx + 14.0, cy - 7.0),
          Offset(cx - 10.0, cy + 7.0),
          const <Color>[
            Color(0xFFCBD5E1),
            Color(0xFFF1F5F9),
            Color(0xFF64748B),
            Color(0xFF334155),
          ],
          const <double>[0.0, 0.32, 0.68, 1.0],
        ),
    );
    canvas.drawPath(
      fuselage,
      Paint()
        ..color = const Color(0xFF0F172A)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.1,
    );

    // Crisp panel lines
    final Paint seamPaint = Paint()
      ..color = const Color(0xFF0F172A)
      ..strokeWidth = 1.0;
    canvas.drawLine(Offset(cx + 15.5, cy), Offset(cx + 24.5, cy), seamPaint);
    canvas.drawLine(Offset(cx - 10.5, cy), Offset(cx - 1.0, cy), seamPaint);
    for (final double sign in const <double>[-1.0, 1.0]) {
      canvas.drawLine(
        Offset(cx + 2.0, cy + sign * 5.2),
        Offset(cx - 10.5, cy + sign * 12.2),
        seamPaint,
      );
      canvas.drawLine(
        Offset(cx + 6.0, cy + sign * 4.0),
        Offset(cx - 8.0, cy + sign * 5.0),
        Paint()
          ..color = const Color(0x8C94A3B8)
          ..strokeWidth = 0.9,
      );
    }

    // Bright cyan neon wingtip & leading-edge strips
    final double neonGlow = 0.80 + 0.20 * sin(time * 5.5);
    for (final double sign in const <double>[-1.0, 1.0]) {
      canvas.drawLine(
        Offset(cx + 0.5, cy + sign * 11.8),
        Offset(cx - 12.5, cy + sign * 20.5),
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.38 * neonGlow)
          ..strokeWidth = 4.6
          ..strokeCap = StrokeCap.round,
      );
      canvas.drawLine(
        Offset(cx + 0.5, cy + sign * 11.8),
        Offset(cx - 12.5, cy + sign * 20.5),
        Paint()
          ..color = const Color(0xFF00F0FF)
          ..strokeWidth = 1.9
          ..strokeCap = StrokeCap.round,
      );
      canvas.drawLine(
        Offset(cx - 11.8, cy + sign * 20.8),
        Offset(cx - 17.0, cy + sign * 17.6),
        Paint()
          ..color = const Color(0xFF7DD3FC)
          ..strokeWidth = 1.8
          ..strokeCap = StrokeCap.round,
      );
    }

    // Glowing cyan faceted cockpit canopy
    final Path canopy = Path()
      ..moveTo(cx + 15.5, cy)
      ..lineTo(cx + 7.5, cy - 3.6)
      ..lineTo(cx - 1.8, cy - 2.6)
      ..lineTo(cx - 4.2, cy)
      ..lineTo(cx - 1.8, cy + 2.6)
      ..lineTo(cx + 7.5, cy + 3.6)
      ..close();
    canvas.drawCircle(
      Offset(cx + 6.5, cy),
      8.5,
      Paint()..color = const Color(0x4200E5FF),
    );
    canvas.drawPath(
      canopy,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx + 14.0, cy - 3.5),
          Offset(cx - 3.0, cy + 3.5),
          const <Color>[
            Color(0xFFE0F2FE),
            Color(0xFF00E5FF),
            Color(0xFF0284C7),
          ],
          const <double>[0.0, 0.45, 1.0],
        ),
    );
    canvas.drawPath(
      canopy,
      Paint()
        ..color = const Color(0xFF0F172A)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.1,
    );
    canvas.drawLine(
      Offset(cx + 11.5, cy - 1.1),
      Offset(cx + 4.5, cy - 1.8),
      Paint()
        ..color = const Color(0xE6FFFFFF)
        ..strokeWidth = 1.2
        ..strokeCap = StrokeCap.round,
    );

    if (flash > 0.08) {
      canvas.drawPath(
        mainWings,
        Paint()..color = const Color(0xFFFF6D00).withOpacity(0.35 * flash),
      );
      canvas.drawPath(
        mainWings,
        Paint()
          ..color = const Color(0xFFFFB74D).withOpacity(0.80 * flash)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
    }
  }

  void _drawBattlecruiserShip(
    Canvas canvas,
    double cx,
    double cy,
    double thrust,
    double pulse,
    double time,
    double flash,
  ) {
    // 1. Double / Twin Main Thrusters venting existing dynamic cyan plasma exhaust
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 20.6, cy - 4.8),
      thrust: thrust,
      maxLen: 24.0,
      halfW: 3.8,
      pulse: pulse,
      phaseOffset: 0.0,
    );
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 20.6, cy + 4.8),
      thrust: thrust,
      maxLen: 24.0,
      halfW: 3.8,
      pulse: pulse,
      phaseOffset: 1.9,
    );

    drawBattlecruiserGoliathBody(
      canvas,
      cx: cx,
      cy: cy,
      thrust: thrust,
      time: time,
      flash: flash,
    );
  }

  static void drawBattlecruiserGoliathBody(
    Canvas canvas, {
    required double cx,
    required double cy,
    double thrust = 0.35,
    double time = 0.0,
    double flash = 0.0,
  }) {
    final double corePulse = 0.78 + 0.22 * sin(time * 6.2);

    // Twin Main Rear Thruster Nozzles (layered dark-steel bells + cyan exhaust collar)
    for (final double sign in const <double>[-1.0, 1.0]) {
      final double ny = cy + sign * 4.8;
      final Path nozzle = Path()
        ..moveTo(cx - 15.2, ny - 3.3)
        ..lineTo(cx - 21.0, ny - 2.5)
        ..lineTo(cx - 21.0, ny + 2.5)
        ..lineTo(cx - 15.2, ny + 3.3)
        ..close();
      canvas.drawPath(
        nozzle,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx - 18.0, ny - 3.3),
            Offset(cx - 18.0, ny + 3.3),
            const <Color>[
              Color(0xFF64748B),
              Color(0xFF1E293B),
              Color(0xFF090D16),
            ],
            const <double>[0.0, 0.5, 1.0],
          ),
      );
      canvas.drawPath(
        nozzle,
        Paint()
          ..color = const Color(0xCCE2E8F0)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.85,
      );
      canvas.drawLine(
        Offset(cx - 20.4, ny - 1.8),
        Offset(cx - 20.4, ny + 1.8),
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.72 + 0.28 * thrust)
          ..strokeWidth = 1.4
          ..strokeCap = StrokeCap.round,
      );
    }

    // 2. Angular "Shoulder" & Winglet Armor Plates (stepped-panel layers + bright white highlights)
    final List<Path> shoulderWingPaths = <Path>[];
    for (final double sign in const <double>[-1.0, 1.0]) {
      final Path outerWinglet = Path()
        ..moveTo(cx - 1.2, cy + sign * 9.8)
        ..lineTo(cx - 3.0, cy + sign * 21.4)
        ..lineTo(cx - 8.2, cy + sign * 24.8)
        ..lineTo(cx - 12.2, cy + sign * 21.5)
        ..lineTo(cx - 10.8, cy + sign * 19.0)
        ..lineTo(cx - 15.4, cy + sign * 17.6)
        ..lineTo(cx - 13.8, cy + sign * 14.5)
        ..lineTo(cx - 16.8, cy + sign * 12.2)
        ..lineTo(cx - 13.2, cy + sign * 9.2)
        ..close();
      shoulderWingPaths.add(outerWinglet);
      canvas.drawPath(
        outerWinglet,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx - 3.0, cy + sign * 24.8),
            Offset(cx - 16.0, cy + sign * 9.2),
            const <Color>[
              Color(0xFF1E293B),
              Color(0xFF334155),
              Color(0xFF0F172A),
            ],
            const <double>[0.0, 0.5, 1.0],
          ),
      );
      canvas.drawPath(
        outerWinglet,
        Paint()
          ..color = const Color(0xE6F8FAFC)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.05
          ..strokeJoin = StrokeJoin.miter,
      );

      final Path midStepPlate = Path()
        ..moveTo(cx - 3.4, cy + sign * 11.4)
        ..lineTo(cx - 4.8, cy + sign * 20.4)
        ..lineTo(cx - 9.0, cy + sign * 21.6)
        ..lineTo(cx - 12.2, cy + sign * 16.4)
        ..lineTo(cx - 11.4, cy + sign * 11.2)
        ..close();
      canvas.drawPath(
        midStepPlate,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx - 4.0, cy + sign * 21.5),
            Offset(cx - 12.0, cy + sign * 11.2),
            const <Color>[
              Color(0xFF64748B),
              Color(0xFF334155),
              Color(0xFF111827),
            ],
            const <double>[0.0, 0.5, 1.0],
          ),
      );
      canvas.drawPath(
        midStepPlate,
        Paint()
          ..color = const Color(0xFF0F172A)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.9,
      );

      final Path upperBevelPlate = Path()
        ..moveTo(cx - 5.4, cy + sign * 13.2)
        ..lineTo(cx - 6.4, cy + sign * 18.6)
        ..lineTo(cx - 9.6, cy + sign * 18.4)
        ..lineTo(cx - 10.6, cy + sign * 13.6)
        ..close();
      canvas.drawPath(
        upperBevelPlate,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx - 5.5, cy + sign * 18.6),
            Offset(cx - 10.6, cy + sign * 13.2),
            const <Color>[
              Color(0xFFCBD5E1),
              Color(0xFF64748B),
              Color(0xFF334155),
            ],
            const <double>[0.0, 0.5, 1.0],
          ),
      );
      canvas.drawPath(
        upperBevelPlate,
        Paint()
          ..color = const Color(0xD9F8FAFC)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.8,
      );
    }

    // 3. Heavy Tapered Rear Engine Block with Pointed Stabilizers & Layered Armor Casing
    for (final double sign in const <double>[-1.0, 1.0]) {
      final Path sideFin = Path()
        ..moveTo(cx - 12.0, cy + sign * 7.6)
        ..lineTo(cx - 22.8, cy + sign * 9.8)
        ..lineTo(cx - 19.6, cy + sign * 6.0)
        ..lineTo(cx - 13.5, cy + sign * 5.4)
        ..close();
      canvas.drawPath(
        sideFin,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx - 12.0, cy + sign * 9.8),
            Offset(cx - 22.0, cy + sign * 5.4),
            const <Color>[
              Color(0xFF475569),
              Color(0xFF1E293B),
              Color(0xFF0B1120),
            ],
            const <double>[0.0, 0.5, 1.0],
          ),
      );
      canvas.drawPath(
        sideFin,
        Paint()
          ..color = const Color(0xDCE2E8F0)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.9
          ..strokeJoin = StrokeJoin.miter,
      );
    }

    final Path engineBlock = Path()
      ..moveTo(cx - 8.5, cy - 9.4)
      ..lineTo(cx - 16.4, cy - 8.0)
      ..lineTo(cx - 19.8, cy - 6.8)
      ..lineTo(cx - 19.8, cy - 2.6)
      ..lineTo(cx - 24.8, cy)
      ..lineTo(cx - 19.8, cy + 2.6)
      ..lineTo(cx - 19.8, cy + 6.8)
      ..lineTo(cx - 16.4, cy + 8.0)
      ..lineTo(cx - 8.5, cy + 9.4)
      ..close();
    canvas.drawPath(
      engineBlock,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx - 9.0, cy - 9.4),
          Offset(cx - 24.0, cy + 9.4),
          const <Color>[
            Color(0xFF1E293B),
            Color(0xFF475569),
            Color(0xFF334155),
            Color(0xFF0F172A),
          ],
          const <double>[0.0, 0.35, 0.7, 1.0],
        ),
    );
    canvas.drawPath(
      engineBlock,
      Paint()
        ..color = const Color(0xE0F1F5F9)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.0
        ..strokeJoin = StrokeJoin.round,
    );

    final Path enginePlate = Path()
      ..moveTo(cx - 10.6, cy - 6.0)
      ..lineTo(cx - 18.2, cy - 4.4)
      ..lineTo(cx - 19.4, cy)
      ..lineTo(cx - 18.2, cy + 4.4)
      ..lineTo(cx - 10.6, cy + 6.0)
      ..close();
    canvas.drawPath(
      enginePlate,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx - 11.0, cy - 6.0),
          Offset(cx - 19.0, cy + 6.0),
          const <Color>[
            Color(0xFF64748B),
            Color(0xFF334155),
            Color(0xFF1E293B),
          ],
          const <double>[0.0, 0.5, 1.0],
        ),
    );
    canvas.drawPath(
      enginePlate,
      Paint()
        ..color = const Color(0xFF0F172A)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 0.9,
    );
    canvas.drawLine(
      Offset(cx - 13.2, cy),
      Offset(cx - 24.0, cy),
      Paint()
        ..color = const Color(0xEBF8FAFC)
        ..strokeWidth = 1.15
        ..strokeCap = StrokeCap.round,
    );

    // 4. Small Forward-Facing Weapon / Laser Barrels on Outer Edges of Pincers
    for (final double sign in const <double>[-1.0, 1.0]) {
      final double byOuter = cy + sign * 13.8;
      canvas.drawLine(
        Offset(cx + 19.2, byOuter),
        Offset(cx + 27.6, byOuter),
        Paint()
          ..color = const Color(0xFF1E293B)
          ..strokeWidth = 2.3,
      );
      canvas.drawLine(
        Offset(cx + 20.0, byOuter - sign * 0.55),
        Offset(cx + 27.2, byOuter - sign * 0.55),
        Paint()
          ..color = const Color(0xEBF1F5F9)
          ..strokeWidth = 0.8
          ..strokeCap = StrokeCap.round,
      );
      canvas.drawCircle(
        Offset(cx + 27.9, byOuter),
        1.15,
        Paint()..color = const Color(0xFF7DD3FC),
      );

      final double byTip = cy + sign * 8.8;
      canvas.drawLine(
        Offset(cx + 23.6, byTip),
        Offset(cx + 29.8, byTip),
        Paint()
          ..color = const Color(0xFF334155)
          ..strokeWidth = 1.9,
      );
      canvas.drawLine(
        Offset(cx + 24.2, byTip - sign * 0.45),
        Offset(cx + 29.5, byTip - sign * 0.45),
        Paint()
          ..color = const Color(0xEBFFFFFF)
          ..strokeWidth = 0.7
          ..strokeCap = StrokeCap.round,
      );
      canvas.drawCircle(
        Offset(cx + 30.1, byTip),
        1.0,
        Paint()..color = const Color(0xFFBAE6FD),
      );
    }

    // 5. Massive Circular Curved Pincers ("O" / "U" Horseshoe Shape, ~18% larger than Interceptor)
    final List<Path> pincerPaths = <Path>[];
    for (final double sign in const <double>[-1.0, 1.0]) {
      final Path pincer = Path()
        ..moveTo(cx - 3.6, cy + sign * 17.6)
        ..cubicTo(
          cx + 10.6, cy + sign * 25.4,
          cx + 26.8, cy + sign * 19.2,
          cx + 28.4, cy + sign * 3.1,
        )
        ..lineTo(cx + 25.6, cy + sign * 4.5)
        ..cubicTo(
          cx + 22.8, cy + sign * 13.4,
          cx + 11.4, cy + sign * 15.6,
          cx - 1.0, cy + sign * 9.0,
        )
        ..close();
      pincerPaths.add(pincer);

      canvas.drawPath(
        pincer,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx + 2.0, cy + sign * 24.0),
            Offset(cx + 27.0, cy + sign * 3.5),
            const <Color>[
              Color(0xFF0B1120),
              Color(0xFF1E293B),
              Color(0xFF334155),
              Color(0xFF1E293B),
              Color(0xFF090D16),
            ],
            const <double>[0.0, 0.25, 0.5, 0.78, 1.0],
          ),
      );

      // Sub-panel 1: Sculpted Metallic Silver Outer Crescent Armor Plate
      final Path outerArmorPlate = Path()
        ..moveTo(cx + 0.6, cy + sign * 17.8)
        ..cubicTo(
          cx + 9.8, cy + sign * 22.0,
          cx + 20.6, cy + sign * 18.6,
          cx + 25.0, cy + sign * 10.6,
        )
        ..lineTo(cx + 22.8, cy + sign * 9.6)
        ..cubicTo(
          cx + 18.8, cy + sign * 16.0,
          cx + 9.8, cy + sign * 18.6,
          cx + 1.6, cy + sign * 14.8,
        )
        ..close();
      canvas.drawPath(
        outerArmorPlate,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx + 2.0, cy + sign * 20.0),
            Offset(cx + 24.0, cy + sign * 10.0),
            const <Color>[
              Color(0xFF94A3B8),
              Color(0xFFCBD5E1),
              Color(0xFF475569),
              Color(0xFF1E293B),
            ],
            const <double>[0.0, 0.35, 0.72, 1.0],
          ),
      );
      canvas.drawPath(
        outerArmorPlate,
        Paint()
          ..color = const Color(0xE60B1120)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.85,
      );

      // Sub-panel 2: Inner Dark Steel-Gray Recessed Curve Plate
      final Path innerArmorPlate = Path()
        ..moveTo(cx + 2.8, cy + sign * 13.6)
        ..cubicTo(
          cx + 10.2, cy + sign * 16.4,
          cx + 17.8, cy + sign * 14.8,
          cx + 21.8, cy + sign * 9.4,
        )
        ..lineTo(cx + 20.4, cy + sign * 8.4)
        ..cubicTo(
          cx + 16.8, cy + sign * 13.0,
          cx + 10.2, cy + sign * 14.4,
          cx + 3.4, cy + sign * 11.8,
        )
        ..close();
      canvas.drawPath(
        innerArmorPlate,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx + 3.0, cy + sign * 14.0),
            Offset(cx + 21.0, cy + sign * 8.5),
            const <Color>[
              Color(0xFF334155),
              Color(0xFF64748B),
              Color(0xFF1E293B),
            ],
            const <double>[0.0, 0.5, 1.0],
          ),
      );
      canvas.drawPath(
        innerArmorPlate,
        Paint()
          ..color = const Color(0xFF090D16)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 0.75,
      );

      // Sub-panel 3: Pointed Forward Tip Metallic Bevel Cap
      final Path tipBevel = Path()
        ..moveTo(cx + 27.5, cy + sign * 3.8)
        ..cubicTo(
          cx + 26.8, cy + sign * 7.2,
          cx + 25.4, cy + sign * 9.6,
          cx + 23.6, cy + sign * 11.2,
        )
        ..lineTo(cx + 22.4, cy + sign * 9.6)
        ..cubicTo(
          cx + 23.8, cy + sign * 8.2,
          cx + 24.8, cy + sign * 6.4,
          cx + 25.4, cy + sign * 4.8,
        )
        ..close();
      canvas.drawPath(
        tipBevel,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx + 27.5, cy + sign * 3.8),
            Offset(cx + 22.4, cy + sign * 11.0),
            const <Color>[Color(0xFFE2E8F0), Color(0xFF64748B)],
          ),
      );

      // Recessed dark panel divider seams
      final Paint seamPaint = Paint()
        ..color = const Color(0xE6090D16)
        ..strokeWidth = 0.95;
      canvas.drawLine(
        Offset(cx + 8.6, cy + sign * 21.0),
        Offset(cx + 9.0, cy + sign * 13.6),
        seamPaint,
      );
      canvas.drawLine(
        Offset(cx + 18.2, cy + sign * 18.6),
        Offset(cx + 16.4, cy + sign * 12.6),
        seamPaint,
      );

      // High-Contrast Bright White / Cyan Neon Edge Highlights
      canvas.drawPath(
        pincer,
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.34 * corePulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.9
          ..strokeJoin = StrokeJoin.round,
      );
      canvas.drawPath(
        pincer,
        Paint()
          ..color = const Color(0xF5F0F9FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.15
          ..strokeJoin = StrokeJoin.round,
      );
    }

    // 6. Central Body & Fuselage (Needle-Like Inner Nose Completely Enveloped by Curved Pincers)
    final Path centralHull = Path()
      ..moveTo(cx + 16.4, cy)
      ..lineTo(cx + 9.8, cy - 3.6)
      ..lineTo(cx + 4.0, cy - 7.0)
      ..lineTo(cx - 4.4, cy - 9.4)
      ..lineTo(cx - 11.8, cy - 7.4)
      ..lineTo(cx - 14.8, cy - 4.0)
      ..lineTo(cx - 14.8, cy + 4.0)
      ..lineTo(cx - 11.8, cy + 7.4)
      ..lineTo(cx - 4.4, cy + 9.4)
      ..lineTo(cx + 4.0, cy + 7.0)
      ..lineTo(cx + 9.8, cy + 3.6)
      ..close();
    canvas.drawPath(
      centralHull,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx + 4.0, cy - 9.4),
          Offset(cx - 10.0, cy + 9.4),
          const <Color>[
            Color(0xFF1E293B),
            Color(0xFF64748B),
            Color(0xFF94A3B8),
            Color(0xFF334155),
            Color(0xFF0F172A),
          ],
          const <double>[0.0, 0.25, 0.5, 0.78, 1.0],
        ),
    );
    canvas.drawPath(
      centralHull,
      Paint()
        ..color = const Color(0x3800E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2
        ..strokeJoin = StrokeJoin.round,
    );
    canvas.drawPath(
      centralHull,
      Paint()
        ..color = const Color(0xEBF1F5F9)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.05
        ..strokeJoin = StrokeJoin.round,
    );

    // Sleek Needle-Nose Prow & Fine Metallic Spire
    final Path needleProw = Path()
      ..moveTo(cx + 21.8, cy)
      ..lineTo(cx + 11.2, cy - 2.0)
      ..lineTo(cx + 9.6, cy)
      ..lineTo(cx + 11.2, cy + 2.0)
      ..close();
    canvas.drawPath(
      needleProw,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx + 21.0, cy - 2.0),
          Offset(cx + 10.0, cy + 2.0),
          const <Color>[
            Color(0xFFF8FAFC),
            Color(0xFF94A3B8),
            Color(0xFF334155),
          ],
          const <double>[0.0, 0.5, 1.0],
        ),
    );
    canvas.drawPath(
      needleProw,
      Paint()
        ..color = const Color(0xFF0F172A)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 0.75,
    );
    canvas.drawLine(
      Offset(cx + 19.6, cy),
      Offset(cx + 23.0, cy),
      Paint()
        ..color = const Color(0xFFE0F2FE)
        ..strokeWidth = 1.05
        ..strokeCap = StrokeCap.round,
    );

    // 7. Streamlined Dark Armored Cockpit + Multi-Layered Glowing GREEN Jewel Canopy/Core
    final Path cockpitHousing = Path()
      ..moveTo(cx + 6.8, cy)
      ..cubicTo(
        cx + 4.8, cy - 3.6,
        cx - 0.6, cy - 4.8,
        cx - 6.6, cy - 3.6,
      )
      ..lineTo(cx - 8.4, cy)
      ..lineTo(cx - 6.6, cy + 3.6)
      ..cubicTo(
        cx - 0.6, cy + 4.8,
        cx + 4.8, cy + 3.6,
        cx + 6.8, cy,
      )
      ..close();
    canvas.drawPath(
      cockpitHousing,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx + 6.0, cy - 4.5),
          Offset(cx - 8.0, cy + 4.5),
          const <Color>[
            Color(0xFF1E293B),
            Color(0xFF0B1120),
            Color(0xFF064E3B),
          ],
          const <double>[0.0, 0.5, 1.0],
        ),
    );
    canvas.drawPath(
      cockpitHousing,
      Paint()
        ..color = const Color(0xD9CBD5E1)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 0.9
        ..strokeJoin = StrokeJoin.round,
    );

    final Path greenJewel = Path()
      ..moveTo(cx + 2.8, cy)
      ..cubicTo(
        cx + 1.2, cy - 2.6,
        cx - 2.4, cy - 3.1,
        cx - 5.4, cy - 2.2,
      )
      ..lineTo(cx - 6.6, cy)
      ..lineTo(cx - 5.4, cy + 2.2)
      ..cubicTo(
        cx - 2.4, cy + 3.1,
        cx + 1.2, cy + 2.6,
        cx + 2.8, cy,
      )
      ..close();
    canvas.drawCircle(
      Offset(cx - 2.0, cy),
      6.2,
      Paint()..color = const Color(0xFF22C55E).withOpacity(0.28 * corePulse),
    );
    canvas.drawPath(
      greenJewel,
      Paint()
        ..color = const Color(0xFF4ADE80).withOpacity(0.45 * corePulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.6,
    );
    canvas.drawPath(
      greenJewel,
      Paint()
        ..shader = ui.Gradient.radial(
          Offset(cx - 1.8, cy - 0.5),
          5.0,
          const <Color>[
            Color(0xFFECFDF5),
            Color(0xFF4ADE80),
            Color(0xFF16A34A),
            Color(0xFF065F46),
          ],
          const <double>[0.0, 0.35, 0.72, 1.0],
        ),
    );
    canvas.drawPath(
      greenJewel,
      Paint()
        ..color = const Color(0xEBBBF7D0)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 0.85,
    );
    canvas.drawLine(
      Offset(cx + 0.8, cy - 0.9),
      Offset(cx - 3.4, cy - 1.4),
      Paint()
        ..color = const Color(0xF2FFFFFF)
        ..strokeWidth = 1.1
        ..strokeCap = StrokeCap.round,
    );
    canvas.drawCircle(
      Offset(cx - 1.8, cy),
      1.0,
      Paint()..color = const Color(0xFFDCFCE7),
    );

    if (flash > 0.08) {
      final Paint flashFill =
          Paint()..color = const Color(0xFFFF6D00).withOpacity(0.35 * flash);
      final Paint flashStroke = Paint()
        ..color = const Color(0xFFFFB74D).withOpacity(0.80 * flash)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8;
      for (final Path p in pincerPaths) {
        canvas.drawPath(p, flashFill);
        canvas.drawPath(p, flashStroke);
      }
      canvas.drawPath(centralHull, flashFill);
      canvas.drawPath(centralHull, flashStroke);
      for (final Path w in shoulderWingPaths) {
        canvas.drawPath(w, flashFill);
      }
    }
  }

  void _drawTankCitadelShip(
    Canvas canvas,
    double cx,
    double cy,
    double thrust,
    double pulse,
    double time,
    double flash,
  ) {
    // 4 heavy rear thrusters (Citadel quad capital-ship engine bank)
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 24.5, cy - 15.2),
      thrust: thrust,
      maxLen: 20.0,
      halfW: 3.8,
      pulse: pulse,
      phaseOffset: 0.4,
    );
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 26.5, cy - 6.0),
      thrust: thrust,
      maxLen: 24.0,
      halfW: 4.5,
      pulse: pulse,
      phaseOffset: 1.6,
    );
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 26.5, cy + 6.0),
      thrust: thrust,
      maxLen: 24.0,
      halfW: 4.5,
      pulse: pulse,
      phaseOffset: 2.9,
    );
    _drawPlasmaThrusterFlame(
      canvas,
      Offset(cx - 24.5, cy + 15.2),
      thrust: thrust,
      maxLen: 20.0,
      halfW: 3.8,
      pulse: pulse,
      phaseOffset: 4.1,
    );

    // 4 armored thruster shrouds
    final List<Offset> shroudOffsets = <Offset>[
      Offset(cx - 25.0, cy - 15.2),
      Offset(cx - 27.0, cy - 6.0),
      Offset(cx - 27.0, cy + 6.0),
      Offset(cx - 25.0, cy + 15.2),
    ];
    for (final Offset pt in shroudOffsets) {
      final Rect shroud = Rect.fromLTWH(pt.dx, pt.dy - 3.4, 6.5, 6.8);
      canvas.drawRect(shroud, Paint()..color = const Color(0xFF1E293B));
      canvas.drawRect(
        shroud,
        Paint()
          ..color = const Color(0xFF94A3B8)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.0,
      );
    }

    // Heavy wide fortified dreadnought base hull with reinforced twin front prows
    final Path citadelHull = Path()
      ..moveTo(cx + 32.0, cy - 8.5)
      ..lineTo(cx + 28.0, cy - 17.5)
      ..lineTo(cx + 16.0, cy - 23.0)
      ..lineTo(cx + 11.0, cy - 30.0)
      ..lineTo(cx - 14.0, cy - 30.5)
      ..lineTo(cx - 24.5, cy - 21.0)
      ..lineTo(cx - 24.5, cy - 10.5)
      ..lineTo(cx - 26.5, cy - 9.0)
      ..lineTo(cx - 26.5, cy + 9.0)
      ..lineTo(cx - 24.5, cy + 10.5)
      ..lineTo(cx - 24.5, cy + 21.0)
      ..lineTo(cx - 14.0, cy + 30.5)
      ..lineTo(cx + 11.0, cy + 30.0)
      ..lineTo(cx + 16.0, cy + 23.0)
      ..lineTo(cx + 28.0, cy + 17.5)
      ..lineTo(cx + 32.0, cy + 8.5)
      ..lineTo(cx + 20.5, cy + 5.5)
      ..lineTo(cx + 20.5, cy - 5.5)
      ..close();
    canvas.drawPath(
      citadelHull,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx, cy - 31.0),
          Offset(cx, cy + 31.0),
          const <Color>[
            Color(0xFF334155),
            Color(0xFF64748B),
            Color(0xFF94A3B8),
            Color(0xFF475569),
            Color(0xFF1E293B),
          ],
          const <double>[0.0, 0.25, 0.5, 0.78, 1.0],
        ),
    );
    canvas.drawPath(
      citadelHull,
      Paint()
        ..color = const Color(0xD9CBD5E1)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.7,
    );

    // Reinforced twin front prow / bulwarks + flank armor skirts
    for (final double sign in const <double>[-1.0, 1.0]) {
      final Path prowPlate = Path()
        ..moveTo(cx + 31.0, cy + sign * 8.8)
        ..lineTo(cx + 27.0, cy + sign * 16.5)
        ..lineTo(cx + 11.0, cy + sign * 20.5)
        ..lineTo(cx + 8.0, cy + sign * 11.5)
        ..lineTo(cx + 21.0, cy + sign * 7.2)
        ..close();
      canvas.drawPath(
        prowPlate,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx + 31.0, cy + sign * 8.0),
            Offset(cx + 9.0, cy + sign * 20.0),
            const <Color>[
              Color(0xFFCBD5E1),
              Color(0xFF94A3B8),
              Color(0xFF475569),
            ],
            const <double>[0.0, 0.48, 1.0],
          ),
      );
      canvas.drawPath(
        prowPlate,
        Paint()
          ..color = const Color(0xFF0F172A)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.25,
      );

      final Path flankBlock = Path()
        ..moveTo(cx + 10.0, cy + sign * 28.5)
        ..lineTo(cx - 13.0, cy + sign * 28.8)
        ..lineTo(cx - 21.5, cy + sign * 20.0)
        ..lineTo(cx - 17.0, cy + sign * 15.5)
        ..lineTo(cx + 12.0, cy + sign * 19.5)
        ..close();
      canvas.drawPath(
        flankBlock,
        Paint()
          ..shader = ui.Gradient.linear(
            Offset(cx + 10.0, cy + sign * 16.0),
            Offset(cx - 20.0, cy + sign * 29.0),
            const <Color>[
              Color(0xFF475569),
              Color(0xFF64748B),
              Color(0xFF1E293B),
            ],
            const <double>[0.0, 0.5, 1.0],
          ),
      );
      canvas.drawPath(
        flankBlock,
        Paint()
          ..color = const Color(0xFF0F172A)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.2,
      );
    }

    // Thick central armored titanium deck tier
    final Path innerDeck = Path()
      ..moveTo(cx + 19.5, cy - 6.5)
      ..lineTo(cx + 11.0, cy - 16.5)
      ..lineTo(cx - 15.0, cy - 17.5)
      ..lineTo(cx - 22.0, cy - 10.5)
      ..lineTo(cx - 22.0, cy + 10.5)
      ..lineTo(cx - 15.0, cy + 17.5)
      ..lineTo(cx + 11.0, cy + 16.5)
      ..lineTo(cx + 19.5, cy + 6.5)
      ..close();
    canvas.drawPath(
      innerDeck,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx + 10.0, cy - 18.0),
          Offset(cx - 14.0, cy + 18.0),
          const <Color>[
            Color(0xFF94A3B8),
            Color(0xFFCBD5E1),
            Color(0xFF64748B),
            Color(0xFF334155),
          ],
          const <double>[0.0, 0.34, 0.70, 1.0],
        ),
    );
    canvas.drawPath(
      innerDeck,
      Paint()
        ..color = const Color(0xFF0F172A)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );

    // Structural panel lines & armor ribs
    final Paint ribPaint = Paint()
      ..color = const Color(0xD90F172A)
      ..strokeWidth = 1.15;
    for (final double xRib in const <double>[-12.0, -2.0, 9.0]) {
      canvas.drawLine(
        Offset(cx + xRib, cy - 16.5),
        Offset(cx + xRib, cy + 16.5),
        ribPaint,
      );
    }

    // Subtle glowing blue/cyan shield-emitter strips along flanks & prow edges
    final double emitterPulse = 0.72 + 0.28 * sin(time * 5.0);
    for (final double sign in const <double>[-1.0, 1.0]) {
      canvas.drawLine(
        Offset(cx - 12.5, cy + sign * 24.2),
        Offset(cx + 9.5, cy + sign * 24.2),
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.30 * emitterPulse)
          ..strokeWidth = 4.6
          ..strokeCap = StrokeCap.round,
      );
      canvas.drawLine(
        Offset(cx - 12.5, cy + sign * 24.2),
        Offset(cx + 9.5, cy + sign * 24.2),
        Paint()
          ..color = const Color(0xEB38BDF8)
          ..strokeWidth = 1.8
          ..strokeCap = StrokeCap.round,
      );
      canvas.drawLine(
        Offset(cx + 15.5, cy + sign * 13.5),
        Offset(cx + 26.5, cy + sign * 12.2),
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.28 * emitterPulse)
          ..strokeWidth = 3.8
          ..strokeCap = StrokeCap.round,
      );
      canvas.drawLine(
        Offset(cx + 15.5, cy + sign * 13.5),
        Offset(cx + 26.5, cy + sign * 12.2),
        Paint()
          ..color = const Color(0xE67DD3FC)
          ..strokeWidth = 1.5
          ..strokeCap = StrokeCap.round,
      );
    }

    // Heavily armored elevated bridge citadel
    final Path bridge = Path()
      ..moveTo(cx + 10.5, cy - 5.2)
      ..lineTo(cx + 4.0, cy - 9.2)
      ..lineTo(cx - 10.5, cy - 9.2)
      ..lineTo(cx - 14.5, cy - 4.8)
      ..lineTo(cx - 14.5, cy + 4.8)
      ..lineTo(cx - 10.5, cy + 9.2)
      ..lineTo(cx + 4.0, cy + 9.2)
      ..lineTo(cx + 10.5, cy + 5.2)
      ..close();
    canvas.drawPath(
      bridge,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx, cy - 9.5),
          Offset(cx, cy + 9.5),
          const <Color>[
            Color(0xFF334155),
            Color(0xFF475569),
            Color(0xFF1E293B),
          ],
          const <double>[0.0, 0.45, 1.0],
        ),
    );
    canvas.drawPath(
      bridge,
      Paint()
        ..color = const Color(0xFF94A3B8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.35,
    );

    // Armored bridge recessed glowing viewport slit
    final Path bridgeVisor = Path()
      ..moveTo(cx + 8.6, cy - 4.2)
      ..lineTo(cx + 5.4, cy - 5.2)
      ..lineTo(cx + 4.4, cy)
      ..lineTo(cx + 5.4, cy + 5.2)
      ..lineTo(cx + 8.6, cy + 4.2)
      ..lineTo(cx + 7.2, cy)
      ..close();
    canvas.drawPath(
      bridgeVisor,
      Paint()
        ..shader = ui.Gradient.linear(
          Offset(cx + 8.5, cy - 5.0),
          Offset(cx + 4.5, cy + 5.0),
          const <Color>[
            Color(0xFFE0F2FE),
            Color(0xFF38BDF8),
            Color(0xFF0284C7),
          ],
          const <double>[0.0, 0.48, 1.0],
        ),
    );
    // Shield generator core node on rear bridge deck
    canvas.drawCircle(
      Offset(cx - 4.5, cy),
      6.0,
      Paint()..color = const Color(0xFF00E5FF).withOpacity(0.30 * emitterPulse),
    );
    canvas.drawCircle(
      Offset(cx - 4.5, cy),
      3.1,
      Paint()..color = const Color(0xFF38BDF8),
    );
    canvas.drawCircle(
      Offset(cx - 4.5, cy),
      1.3,
      Paint()..color = Colors.white,
    );

    if (flash > 0.08) {
      canvas.drawPath(
        citadelHull,
        Paint()..color = const Color(0xFFFF6D00).withOpacity(0.35 * flash),
      );
      canvas.drawPath(
        citadelHull,
        Paint()
          ..color = const Color(0xFFFFB74D).withOpacity(0.80 * flash)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.2,
      );
    }
  }

  void _drawPlasmaThrusterFlame(
    Canvas canvas,
    Offset base, {
    required double thrust,
    required double maxLen,
    required double halfW,
    required double pulse,
    required double phaseOffset,
  }) {
    final double flicker = 0.84 +
        0.14 * sin(pulse * 2.1 + phaseOffset) +
        0.08 * sin(pulse * 4.7 + phaseOffset * 1.7);
    final double len = (maxLen * (0.24 + 0.88 * thrust)) * flicker;
    final double w = halfW * (0.68 + 0.36 * thrust);

    // Soft outer blue plasma halo at nozzle
    canvas.drawCircle(
      Offset(base.dx - len * 0.16, base.dy),
      w * 1.85,
      Paint()
        ..color = const Color(0xFF0284C7).withOpacity(0.24 + 0.22 * thrust),
    );

    // Outer blue-cyan plasma glow plume
    final Path outer = Path()
      ..moveTo(base.dx + 0.5, base.dy - w * 1.25)
      ..lineTo(base.dx - len * 1.15, base.dy)
      ..lineTo(base.dx + 0.5, base.dy + w * 1.25)
      ..close();
    canvas.drawPath(
      outer,
      Paint()
        ..shader = ui.Gradient.linear(
          base,
          Offset(base.dx - len * 1.15, base.dy),
          <Color>[
            const Color(0xFF00E5FF).withOpacity(0.65 + 0.25 * thrust),
            const Color(0x1F0284C7),
          ],
        ),
    );

    // Mid bright cyan plasma tongue
    final Path mid = Path()
      ..moveTo(base.dx + 0.5, base.dy - w * 0.82)
      ..lineTo(base.dx - len * 0.86, base.dy)
      ..lineTo(base.dx + 0.5, base.dy + w * 0.82)
      ..close();
    canvas.drawPath(
      mid,
      Paint()
        ..shader = ui.Gradient.linear(
          base,
          Offset(base.dx - len * 0.86, base.dy),
          const <Color>[
            Color(0xF27DD3FC),
            Color(0x7300E5FF),
          ],
        ),
    );

    // Bright white-hot plasma inner core
    final Path core = Path()
      ..moveTo(base.dx + 0.8, base.dy - w * 0.44)
      ..lineTo(base.dx - len * 0.52, base.dy)
      ..lineTo(base.dx + 0.8, base.dy + w * 0.44)
      ..close();
    canvas.drawPath(
      core,
      Paint()..color = Colors.white.withOpacity(0.86 + 0.14 * thrust),
    );
  }

  void _drawBar(
    Canvas canvas,
    Offset centerTop,
    double width,
    double height,
    double ratio,
    Color fillColor,
  ) {
    final Rect bg = Rect.fromLTWH(
      centerTop.dx - width * 0.5,
      centerTop.dy,
      width,
      height,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(bg, const Radius.circular(2.5)),
      Paint()..color = const Color(0xCC060B14),
    );
    final double clamped = ratio.clamp(0.0, 1.0);
    if (clamped > 0.0) {
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(bg.left, bg.top, width * clamped, height),
          const Radius.circular(2.5),
        ),
        Paint()..color = fillColor,
      );
    }
  }
}

// =============================================================================
// VANT FLEET COMPONENT (World-Space Lerped Drone Formations)
// =============================================================================
class VantFleetComponent extends Component with HasGameRef<DarkOrbitGame> {
  VantFleetComponent() : super(priority: 15);

  @override
  void render(Canvas canvas) {
    final List<VantDrone> vants = gameRef.activeVants;
    if (vants.isEmpty) return;

    final double pulse = 0.75 + 0.25 * sin(gameRef.elapsedTime * 6.5);
    final Color formAccent = gameRef.activeFormationSpec.accentColor;

    // Subtle formation ring guide for RING formation only (WHEEL draws drones only)
    if (gameRef.activeFormation == DroneFormation.ring) {
      final double baseR = gameRef.activeHullRadius + 28.0;
      final double r = baseR * 1.08;
      canvas.drawCircle(
        Offset(gameRef.player.position.x, gameRef.player.position.y),
        r,
        Paint()
          ..color = formAccent.withOpacity(0.16)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.2,
      );
    }

    for (final VantDrone vant in vants) {
      if (!vant.hasInitializedPosition) continue;
      canvas.save();
      canvas.translate(vant.worldX, vant.worldY);
      canvas.rotate(vant.facingRad);

      if (vant.tier == VantTier.flax) {
        canvas.drawCircle(
          Offset.zero,
          8.5,
          Paint()..color = const Color(0xFF00E5FF).withOpacity(0.22 * pulse),
        );
        final Path hull = Path()
          ..moveTo(7.5, 0.0)
          ..lineTo(-3.5, -5.2)
          ..lineTo(-6.0, 0.0)
          ..lineTo(-3.5, 5.2)
          ..close();
        canvas.drawPath(hull, Paint()..color = const Color(0xFFCBD5E1));
        canvas.drawPath(
          hull,
          Paint()
            ..color = const Color(0xFF00E5FF)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.4,
        );
        canvas.drawCircle(
          const Offset(1.0, 0.0),
          2.0,
          Paint()..color = const Color(0xFF00E5FF),
        );
        canvas.drawCircle(
          const Offset(1.0, 0.0),
          0.9,
          Paint()..color = Colors.white,
        );
      } else {
        canvas.drawCircle(
          Offset.zero,
          10.5,
          Paint()..color = const Color(0xFFFBBF24).withOpacity(0.26 * pulse),
        );
        final Path wings = Path()
          ..moveTo(9.0, 0.0)
          ..lineTo(1.5, -4.0)
          ..lineTo(-4.5, -9.0)
          ..lineTo(-7.0, -4.5)
          ..lineTo(-5.0, 0.0)
          ..lineTo(-7.0, 4.5)
          ..lineTo(-4.5, 9.0)
          ..lineTo(1.5, 4.0)
          ..close();
        canvas.drawPath(wings, Paint()..color = const Color(0xFF1D4ED8));
        canvas.drawPath(
          wings,
          Paint()
            ..color = const Color(0xFFFBBF24)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.6,
        );
        final Paint stripePaint = Paint()
          ..color = const Color(0xFFFDE68A)
          ..strokeWidth = 1.3
          ..strokeCap = StrokeCap.round;
        canvas.drawLine(
          const Offset(-3.0, -6.8),
          const Offset(3.0, -2.6),
          stripePaint,
        );
        canvas.drawLine(
          const Offset(-3.0, 6.8),
          const Offset(3.0, 2.6),
          stripePaint,
        );
        canvas.drawCircle(
          const Offset(0.5, 0.0),
          2.6,
          Paint()..color = const Color(0xFF38BDF8),
        );
        canvas.drawCircle(
          const Offset(0.5, 0.0),
          1.1,
          Paint()..color = Colors.white,
        );
      }

      canvas.restore();
    }
  }
}

// =============================================================================
// ALIEN SHIP COMPONENT (4 Distinct Zone Tiers + 20% Classic BOSS Variants)
// =============================================================================
class AlienShipComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  AlienShipComponent({
    required this.tier,
    required super.position,
    this.isBoss = false,
  })  : spec = AlienTierCatalog.of(tier),
        hp = AlienTierCatalog.of(tier).maxHp *
            (isBoss ? GameConstants.bossHpMultiplier : 1.0),
        super(
          anchor: Anchor.center,
          size: Vector2.all(
            AlienTierCatalog.of(tier).radius *
                (isBoss ? GameConstants.bossSizeMultiplier : 1.0) *
                2.5,
          ),
        );

  final AlienTier tier;
  final AlienTierSpec spec;
  final bool isBoss;
  double hp;
  bool isAggro = false;
  double fireCooldown = 0.7;
  double pulsePhase = 0.0;
  Vector2? patrolTarget;

  double get effectiveRadius =>
      isBoss ? spec.radius * GameConstants.bossSizeMultiplier : spec.radius;

  double get effectiveMaxHp =>
      isBoss ? spec.maxHp * GameConstants.bossHpMultiplier : spec.maxHp;

  double get effectiveLaserDamage => isBoss
      ? spec.laserDamage * GameConstants.bossDamageMultiplier
      : spec.laserDamage;

  int get effectiveRewardCredits => isBoss
      ? spec.rewardCredits * GameConstants.bossRewardMultiplier
      : spec.rewardCredits;

  int get effectiveRewardXp => isBoss
      ? spec.rewardXp * GameConstants.bossRewardMultiplier
      : spec.rewardXp;

  String get displayCallsign => isBoss ? 'BOSS ${spec.callsign}' : spec.callsign;

  @override
  void update(double dt) {
    super.update(dt);
    pulsePhase += dt * 3.5;
    if (fireCooldown > 0.0) {
      fireCooldown = max(0.0, fireCooldown - dt);
    }

    final double mapSize = gameRef.activeMapSize;
    final bool playerInRing = !gameRef.player.inSafeZone &&
        gameRef.player.position.x >= 0.0 &&
        gameRef.player.position.x <= mapSize &&
        gameRef.player.position.y >= 0.0 &&
        gameRef.player.position.y <= mapSize;

    if (isAggro && (gameRef.player.inSafeZone || !playerInRing)) {
      isAggro = false;
      patrolTarget = _pickZoneWaypoint();
    }

    if (isAggro) {
      final Vector2 toPlayer = gameRef.player.position - position;
      final double dist = toPlayer.length;
      if (dist > 0.001) {
        angle = atan2(toPlayer.y, toPlayer.x);
        final double orbitDist = max(175.0, effectiveRadius * 4.8);
        if (dist > orbitDist) {
          position.add(toPlayer.normalized() * (spec.chaseSpeed * dt));
        }
        if (dist <= spec.attackRange && fireCooldown <= 0.0) {
          fireCooldown = spec.fireInterval;
          final Vector2 dir = toPlayer.normalized();
          gameRef.world.add(
            LaserBoltComponent(
              position: position + dir * effectiveRadius,
              velocity: dir * spec.laserSpeed,
              angle: angle,
              fromPlayer: false,
              damage: effectiveLaserDamage,
              color: isBoss ? const Color(0xFFFF2E4C) : spec.primaryColor,
            ),
          );
        }
      }
    } else {
      patrolTarget ??= _pickZoneWaypoint();
      final Vector2 toWp = patrolTarget! - position;
      if (toWp.length < 56.0) {
        patrolTarget = _pickZoneWaypoint();
      } else {
        angle = atan2(toWp.y, toWp.x);
        position.add(toWp.normalized() * (spec.patrolSpeed * dt));
      }
    }

    final double pad = effectiveRadius + 90.0;
    position.x = position.x.clamp(pad, mapSize - pad);
    position.y = position.y.clamp(pad, mapSize - pad);
    if (gameRef.currentSector.hasCentralSafeZone) {
      final Vector2 safeCenter = gameRef.activeSafeZoneCenter;
      final double dx = position.x - safeCenter.x;
      final double dy = position.y - safeCenter.y;
      final double minDist =
          gameRef.activeSafeZoneRadius + effectiveRadius + 40.0;
      final double dist = sqrt(dx * dx + dy * dy);
      if (dist < minDist) {
        if (dist < 0.001) {
          position.setValues(safeCenter.x + minDist, safeCenter.y);
        } else {
          final double scale = minDist / dist;
          position.setValues(
            safeCenter.x + dx * scale,
            safeCenter.y + dy * scale,
          );
        }
      }
    }
  }

  Vector2 _pickZoneWaypoint() {
    return gameRef.randomPointInActiveSector(
      margin: 280.0 + effectiveRadius,
      safeZoneBuffer: 220.0 + effectiveRadius,
    );
  }

  void takeDamage(double amount, {bool isRocket = false}) {
    hp -= amount;
    isAggro = true;
    gameRef.spawnFloatingText(
      position: position +
          Vector2(
            (gameRef.rng.nextDouble() - 0.5) * 18.0,
            -effectiveRadius - 10.0,
          ),
      text: isRocket ? '-${amount.round()} RKT' : '-${amount.round()}',
      color: isRocket ? const Color(0xFFFB923C) : Colors.white,
      fontSize: isRocket ? 15.0 : 13.5,
    );
    if (hp <= 0.0) {
      gameRef.onAlienDestroyed(this);
    }
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isInViewport(position, margin: effectiveRadius + 90.0)) return;
    final Offset c = Offset(size.x * 0.5, size.y * 0.5);
    final double corePulse = 0.72 + 0.28 * sin(pulsePhase);

    // Pulsing Crimson / Gold BOSS Aura Ring
    if (isBoss) {
      final double auraPulse = 0.55 + 0.45 * sin(pulsePhase * 1.45);
      canvas.drawCircle(
        c,
        effectiveRadius + 14.0 + 5.0 * auraPulse,
        Paint()..color = const Color(0xFFFF2E4C).withOpacity(0.14 + 0.14 * auraPulse),
      );
      canvas.drawCircle(
        c,
        effectiveRadius + 10.0 + 3.0 * auraPulse,
        Paint()
          ..color = const Color(0xFFFBBF24).withOpacity(0.50 + 0.40 * auraPulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.6,
      );
      canvas.drawCircle(
        c,
        effectiveRadius + 5.0,
        Paint()
          ..color = const Color(0xFFFF2E4C).withOpacity(0.75)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.6,
      );
    }

    if (gameRef.lockedTarget == this) {
      canvas.drawCircle(
        c,
        effectiveRadius + (isBoss ? 18.0 : 11.0),
        Paint()
          ..color = const Color(0xDDFF2E4C)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.4,
      );
    }

    canvas.save();
    canvas.translate(c.dx, c.dy);
    if (isBoss) {
      canvas.scale(
        GameConstants.bossSizeMultiplier,
        GameConstants.bossSizeMultiplier,
      );
    }
    switch (tier) {
      case AlienTier.scout:
        final Path tri = Path()
          ..moveTo(18.0, 0.0)
          ..lineTo(-13.0, -13.0)
          ..lineTo(-7.0, 0.0)
          ..lineTo(-13.0, 13.0)
          ..close();
        canvas.drawPath(tri, Paint()..color = const Color(0xFF0E291A));
        canvas.drawPath(
          tri,
          Paint()
            ..color = isBoss ? const Color(0xFFFBBF24) : spec.primaryColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = isBoss ? 2.3 : 1.9,
        );
        canvas.drawCircle(
          Offset.zero,
          3.6 * corePulse,
          Paint()..color = isBoss ? const Color(0xFFFF2E4C) : spec.primaryColor,
        );
      case AlienTier.hunter:
        final Path diamond = Path()
          ..moveTo(23.0, 0.0)
          ..lineTo(2.0, -17.0)
          ..lineTo(-19.0, 0.0)
          ..lineTo(2.0, 17.0)
          ..close();
        canvas.drawPath(diamond, Paint()..color = const Color(0xFF2B2108));
        canvas.drawPath(
          diamond,
          Paint()
            ..color = isBoss ? const Color(0xFFFBBF24) : spec.primaryColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = isBoss ? 2.5 : 2.1,
        );
        canvas.drawCircle(
          Offset.zero,
          4.8 * corePulse,
          Paint()..color = isBoss ? const Color(0xFFFF2E4C) : spec.primaryColor,
        );
      case AlienTier.destroyer:
        final Path hex = Path();
        for (int i = 0; i < 6; i++) {
          final double a = i * pi / 3.0;
          final double x = cos(a) * 26.0;
          final double y = sin(a) * 26.0;
          if (i == 0) {
            hex.moveTo(x, y);
          } else {
            hex.lineTo(x, y);
          }
        }
        hex.close();
        canvas.drawPath(hex, Paint()..color = const Color(0xFF2E120B));
        canvas.drawPath(
          hex,
          Paint()
            ..color = isBoss ? const Color(0xFFFBBF24) : spec.primaryColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = isBoss ? 2.8 : 2.4,
        );
        canvas.drawCircle(
          Offset.zero,
          6.5 * corePulse,
          Paint()..color = isBoss ? const Color(0xFFFF2E4C) : spec.primaryColor,
        );
      case AlienTier.dreadnoughtBoss:
        final Path boss = Path()
          ..moveTo(42.0, -10.0)
          ..lineTo(24.0, -14.0)
          ..lineTo(12.0, -36.0)
          ..lineTo(-16.0, -38.0)
          ..lineTo(-36.0, -20.0)
          ..lineTo(-30.0, 0.0)
          ..lineTo(-36.0, 20.0)
          ..lineTo(-16.0, 38.0)
          ..lineTo(12.0, 36.0)
          ..lineTo(24.0, 14.0)
          ..lineTo(42.0, 10.0)
          ..lineTo(30.0, 0.0)
          ..close();
        canvas.drawPath(boss, Paint()..color = const Color(0xFF1F0A2B));
        canvas.drawPath(
          boss,
          Paint()
            ..color = isBoss ? const Color(0xFFFBBF24) : spec.primaryColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = isBoss ? 3.2 : 2.8,
        );
        canvas.drawCircle(
          Offset.zero,
          10.0 * corePulse,
          Paint()..color = isBoss ? const Color(0xFFFF2E4C) : spec.accentColor,
        );
        canvas.drawCircle(Offset.zero, 4.5, Paint()..color = Colors.white);
    }
    canvas.restore();

    // World-upright Health Bar + "BOSS" label
    canvas.save();
    canvas.translate(c.dx, c.dy);
    canvas.rotate(-angle);
    final double barW = isBoss
        ? max(62.0, effectiveRadius * 1.45)
        : (tier == AlienTier.dreadnoughtBoss ? 72.0 : 42.0);
    final double barTop = -effectiveRadius - (isBoss ? 18.0 : 14.0);
    final Rect bg = Rect.fromLTWH(-barW * 0.5, barTop, barW, isBoss ? 6.5 : 5.0);
    canvas.drawRect(bg, Paint()..color = const Color(0xCC060B14));
    canvas.drawRect(
      Rect.fromLTWH(
        bg.left,
        bg.top,
        barW * (hp / effectiveMaxHp).clamp(0.0, 1.0),
        isBoss ? 6.5 : 5.0,
      ),
      Paint()..color = isBoss ? const Color(0xFFFF2E4C) : spec.primaryColor,
    );
    if (isBoss) {
      TextPaint(
        style: const TextStyle(
          color: Color(0xFFFBBF24),
          fontSize: 10.5,
          fontWeight: FontWeight.bold,
          letterSpacing: 1.0,
        ),
      ).render(
        canvas,
        'BOSS',
        Vector2(0.0, barTop - 8.0),
        anchor: Anchor.center,
      );
    }
    canvas.restore();
  }
}

// =============================================================================
// LASER BOLT, HOMING ROCKET, LOOT BOX & VISUAL EFFECTS
// =============================================================================
class LaserBoltComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  LaserBoltComponent({
    required super.position,
    required this.velocity,
    required super.angle,
    required this.fromPlayer,
    required this.damage,
    required this.color,
    this.homingTarget,
  }) : super(anchor: Anchor.center, size: Vector2(24.0, 14.0));

  final Vector2 velocity;
  final bool fromPlayer;
  final double damage;
  final Color color;
  final AlienShipComponent? homingTarget;
  double life = 0.9;

  @override
  void update(double dt) {
    super.update(dt);
    life -= dt;
    if (life <= 0.0 || gameRef.isOutOfActiveMapBounds(position)) {
      removeFromParent();
      return;
    }

    if (fromPlayer && homingTarget != null && !homingTarget!.isRemoved) {
      final Vector2 dir = (homingTarget!.position - position).normalized();
      velocity.setFrom(dir * GameConstants.playerLaserSpeed);
      angle = atan2(dir.y, dir.x);
    }

    position.add(velocity * dt);

    if (!fromPlayer && gameRef.isPointInActiveSafeZone(position)) {
      removeFromParent();
      return;
    }

    if (fromPlayer) {
      for (final AlienShipComponent alien in List<AlienShipComponent>.of(
        gameRef.aliens,
      )) {
        if (position.distanceTo(alien.position) <=
            alien.effectiveRadius + 9.0) {
          alien.takeDamage(damage);
          removeFromParent();
          return;
        }
      }
    } else if (position.distanceTo(gameRef.player.position) <=
        gameRef.activeHullRadius + 6.0) {
      gameRef.player.takeDamage(damage);
      removeFromParent();
    }
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isInViewport(position, margin: 40.0)) return;
    final double cy = size.y * 0.5;
    if (fromPlayer) {
      for (final double oy in <double>[-4.5, 4.5]) {
        canvas.drawLine(
          Offset(0.0, cy + oy),
          Offset(size.x, cy + oy),
          Paint()
            ..color = color
            ..strokeWidth = 2.6
            ..strokeCap = StrokeCap.round,
        );
      }
    } else {
      canvas.drawLine(
        Offset(0.0, cy),
        Offset(size.x, cy),
        Paint()
          ..color = color
          ..strokeWidth = 3.2
          ..strokeCap = StrokeCap.round,
      );
    }
  }
}

class HomingRocketComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  HomingRocketComponent({
    required super.position,
    required super.angle,
    required this.target,
    required this.damage,
  }) : super(anchor: Anchor.center, size: Vector2(24.0, 14.0));

  AlienShipComponent? target;
  final double damage;
  double life = GameConstants.rocketLifetimeSec;
  double smokeTimer = 0.0;

  @override
  void update(double dt) {
    super.update(dt);
    life -= dt;
    if (life <= 0.0 || gameRef.isOutOfActiveMapBounds(position)) {
      removeFromParent();
      return;
    }

    if (target == null || target!.isRemoved) {
      AlienShipComponent? nearest;
      double best = double.infinity;
      for (final AlienShipComponent alien in gameRef.aliens) {
        final double d = alien.position.distanceTo(position);
        if (d < best) {
          best = d;
          nearest = alien;
        }
      }
      target = nearest;
    }

    if (target != null && !target!.isRemoved) {
      final Vector2 toTarget = target!.position - position;
      final double desired = atan2(toTarget.y, toTarget.x);
      double diff = desired - angle;
      while (diff > pi) {
        diff -= 2.0 * pi;
      }
      while (diff < -pi) {
        diff += 2.0 * pi;
      }
      angle += diff * (1.0 - exp(-GameConstants.rocketTurnRate * dt));
    }

    position.add(
      Vector2(cos(angle), sin(angle)) * (GameConstants.rocketSpeed * dt),
    );

    smokeTimer += dt;
    if (smokeTimer >= 0.04) {
      smokeTimer = 0.0;
      gameRef.world.add(
        ExplosionParticleComponent(
          position: position - Vector2(cos(angle), sin(angle)) * 10.0,
          velocity: Vector2(-cos(angle) * 30.0, -sin(angle) * 30.0),
          radius: 2.8,
          life: 0.25,
          color: const Color(0xFFFB923C),
        ),
      );
    }

    for (final AlienShipComponent alien in List<AlienShipComponent>.of(
      gameRef.aliens,
    )) {
      if (position.distanceTo(alien.position) <= alien.effectiveRadius + 14.0) {
        gameRef.world.add(
          ShockwaveRingComponent(
            position: position.clone(),
            maxRadius: 48.0,
            duration: 0.28,
            color: const Color(0xFFFB923C),
          ),
        );
        alien.takeDamage(damage, isRocket: true);
        removeFromParent();
        return;
      }
    }
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isInViewport(position, margin: 40.0)) return;
    final Offset c = Offset(size.x * 0.5, size.y * 0.5);
    canvas.drawCircle(
      Offset(c.dx - 8.0, c.dy),
      6.0,
      Paint()..color = const Color(0x77FB923C),
    );
    final Path body = Path()
      ..moveTo(c.dx + 10.0, c.dy)
      ..lineTo(c.dx + 2.0, c.dy - 3.0)
      ..lineTo(c.dx - 8.0, c.dy - 3.0)
      ..lineTo(c.dx - 10.0, c.dy - 5.5)
      ..lineTo(c.dx - 8.0, c.dy)
      ..lineTo(c.dx - 10.0, c.dy + 5.5)
      ..lineTo(c.dx - 8.0, c.dy + 3.0)
      ..lineTo(c.dx + 2.0, c.dy + 3.0)
      ..close();
    canvas.drawPath(body, Paint()..color = const Color(0xFFF8FAFC));
    canvas.drawPath(
      body,
      Paint()
        ..color = const Color(0xFFFB923C)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
  }
}

class LootBoxComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  LootBoxComponent({
    required this.kind,
    required super.position,
    required this.creditReward,
    required this.shieldReward,
    required this.xpReward,
  }) : super(anchor: Anchor.center, size: Vector2.all(24.0));

  final LootBoxKind kind;
  final int creditReward;
  final double shieldReward;
  final int xpReward;
  double pulsePhase = 0.0;
  double life = GameConstants.cargoLifetimeSec;

  @override
  void update(double dt) {
    super.update(dt);
    pulsePhase += dt * 4.2;
    if (kind == LootBoxKind.cargoBox) {
      life -= dt;
      if (life <= 0.0) {
        gameRef.lootBoxes.remove(this);
        removeFromParent();
        return;
      }
    }

    if (position.distanceTo(gameRef.player.position) <=
        GameConstants.lootCollectRadius +
            (gameRef.activeHullRadius - 20.0)) {
      if (xpReward > 0) {
        gameRef.addXp(xpReward);
      }
      if (shieldReward > 0.0 &&
          gameRef.player.shield < gameRef.currentMaxShield) {
        final double gained = min(
          gameRef.currentMaxShield - gameRef.player.shield,
          shieldReward,
        );
        gameRef.player.shield += gained;
        gameRef.spawnFloatingText(
          position: position + Vector2(0.0, -14.0),
          text: '+${gained.toInt()} SHD',
          color: const Color(0xFF38BDF8),
        );
      } else {
        gameRef.credits += creditReward;
        gameRef.spawnFloatingText(
          position: position + Vector2(0.0, -14.0),
          text: kind == LootBoxKind.cargoBox
              ? '+$creditReward CARGO'
              : '+$creditReward CR',
          color: const Color(0xFFFBBF24),
        );
      }
      gameRef.lootBoxes.remove(this);
      removeFromParent();
      if (kind == LootBoxKind.bonusBox) {
        gameRef.recordMissionProgress(MissionGoalType.collectBonusBoxes);
        gameRef.spawnBonusBox();
      }
      gameRef.saveGame();
    }
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isInViewport(position, margin: 40.0)) return;
    final Offset c = Offset(size.x * 0.5, size.y * 0.5);
    final Color primary = kind == LootBoxKind.cargoBox
        ? const Color(0xFFFBBF24)
        : (shieldReward > 0.0
            ? const Color(0xFF00E5FF)
            : const Color(0xFFFACC15));
    canvas.drawCircle(
      c,
      12.0 + 3.0 * sin(pulsePhase),
      Paint()..color = primary.withOpacity(0.2),
    );
    final Rect box = Rect.fromCenter(center: c, width: 15.0, height: 15.0);
    canvas.drawRRect(
      RRect.fromRectAndRadius(box, const Radius.circular(3.0)),
      Paint()..color = const Color(0xDD0B1324),
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(box, const Radius.circular(3.0)),
      Paint()
        ..color = primary
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.0,
    );
  }
}

class ExplosionParticleComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  ExplosionParticleComponent({
    required super.position,
    required this.velocity,
    required this.radius,
    required this.life,
    required this.color,
  }) : maxLife = life;

  final Vector2 velocity;
  final double radius;
  double life;
  final double maxLife;
  final Color color;

  @override
  void update(double dt) {
    super.update(dt);
    life -= dt;
    if (life <= 0.0) {
      removeFromParent();
      return;
    }
    position.add(velocity * dt);
    velocity.scale(max(0.0, 1.0 - 1.9 * dt));
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isInViewport(position, margin: 24.0)) return;
    final double alpha = (life / maxLife).clamp(0.0, 1.0);
    canvas.drawCircle(
      Offset.zero,
      radius * alpha,
      Paint()..color = color.withOpacity(alpha),
    );
  }
}

class ShockwaveRingComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  ShockwaveRingComponent({
    required super.position,
    required this.maxRadius,
    required double duration,
    required this.color,
  })  : life = duration,
        maxLife = duration;

  final double maxRadius;
  double life;
  final double maxLife;
  final Color color;

  @override
  void update(double dt) {
    super.update(dt);
    life -= dt;
    if (life <= 0.0) {
      removeFromParent();
    }
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isInViewport(position, margin: maxRadius + 20.0)) return;
    final double progress = 1.0 - (life / maxLife).clamp(0.0, 1.0);
    canvas.drawCircle(
      Offset.zero,
      10.0 + maxRadius * progress,
      Paint()
        ..color = color.withOpacity((1.0 - progress) * 0.8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = max(1.0, 4.0 * (1.0 - progress)),
    );
  }
}

class FloatingTextComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  FloatingTextComponent({
    required super.position,
    required this.text,
    required this.color,
    this.fontSize = 13.5,
  });

  final String text;
  final Color color;
  final double fontSize;
  double life = 1.05;
  final double maxLife = 1.05;

  @override
  void update(double dt) {
    super.update(dt);
    life -= dt;
    position.y -= 34.0 * dt;
    if (life <= 0.0) {
      removeFromParent();
    }
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isInViewport(position, margin: 60.0)) return;
    final double alpha = (life / maxLife).clamp(0.0, 1.0);
    final TextPaint tp = TextPaint(
      style: TextStyle(
        color: color.withOpacity(alpha),
        fontSize: fontSize,
        fontWeight: FontWeight.bold,
      ),
    );
    tp.render(canvas, text, Vector2.zero(), anchor: Anchor.center);
  }
}

// =============================================================================
// VIEWPORT HUD & COMBAT CONTROLS
// =============================================================================
class AttackButtonComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  AttackButtonComponent() : super(size: Vector2.all(68.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(size.x - 168.0 - 68.0, size.y - 36.0 - 68.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.toggleAutoFire();
  }

  @override
  void render(Canvas canvas) {
    final bool safe = gameRef.player.inSafeZone;
    final bool on = gameRef.autoFireEnabled && !safe;
    final Offset c = Offset(size.x * 0.5, size.y * 0.5);
    canvas.drawCircle(
      c,
      32.0,
      Paint()
        ..color = safe
            ? const Color(0x99111C2D)
            : (on ? const Color(0xDDFF2E4C) : const Color(0xCC0D1B30)),
    );
    canvas.drawCircle(
      c,
      32.0,
      Paint()
        ..color = safe
            ? const Color(0xFF1E3A5F)
            : (on ? const Color(0xFFFF6B81) : const Color(0xFF00E5FF))
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.4,
    );
    final TextPaint tp = TextPaint(
      style: TextStyle(
        color: safe ? const Color(0xFF64748B) : Colors.white,
        fontSize: 11.0,
        fontWeight: FontWeight.bold,
      ),
    );
    tp.render(
      canvas,
      safe ? 'SAFE' : (on ? 'LASER ON' : 'FIRE'),
      Vector2(c.dx, c.dy),
      anchor: Anchor.center,
    );
  }
}

class RocketButtonComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  RocketButtonComponent() : super(size: Vector2.all(58.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(size.x - 168.0 - 68.0 - 68.0, size.y - 40.0 - 58.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.fireHomingRocket();
  }

  @override
  void render(Canvas canvas) {
    final bool safe = gameRef.player.inSafeZone;
    final double cd = gameRef.player.rocketCooldown;
    final bool ready = !safe && cd <= 0.05;
    final Offset c = Offset(size.x * 0.5, size.y * 0.5);
    canvas.drawCircle(
      c,
      27.0,
      Paint()
        ..color = ready ? const Color(0xDDFB923C) : const Color(0x99111C2D),
    );
    canvas.drawCircle(
      c,
      27.0,
      Paint()
        ..color = ready ? const Color(0xFFFBBF24) : const Color(0xFF334155)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.0,
    );
    final TextPaint tp = TextPaint(
      style: TextStyle(
        color: ready ? const Color(0xFF040711) : const Color(0xFF94A3B8),
        fontSize: 10.5,
        fontWeight: FontWeight.bold,
      ),
    );
    tp.render(
      canvas,
      cd > 0.05 ? '${cd.ceil()}s' : 'RKT',
      Vector2(c.dx, c.dy),
      anchor: Anchor.center,
    );
  }
}

class JumpButtonComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  JumpButtonComponent() : super(size: Vector2.all(64.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(size.x - 168.0 - 68.0 - 68.0 - 74.0, size.y - 38.0 - 64.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    if (gameRef.isPlayerNearJumpGate) {
      gameRef.triggerJumpGate();
    }
  }

  @override
  void render(Canvas canvas) {
    final SectorPortal? portal = gameRef.nearestJumpPortal;
    if (portal == null) return;
    final double pulse = 0.55 + 0.45 * sin(gameRef.elapsedTime * 7.5);
    final Offset c = Offset(size.x * 0.5, size.y * 0.5);

    canvas.drawCircle(
      c,
      30.0 + 3.0 * pulse,
      Paint()..color = portal.accentColor.withOpacity(0.30 * pulse),
    );
    canvas.drawCircle(
      c,
      29.0,
      Paint()
        ..shader = ui.Gradient.radial(
          c,
          29.0,
          <Color>[
            portal.accentColor.withOpacity(0.92),
            const Color(0xDD0284C7),
            const Color(0xCC1E1B4B),
          ],
          const <double>[0.0, 0.55, 1.0],
        ),
    );
    canvas.drawCircle(
      c,
      29.0,
      Paint()
        ..color = Colors.white.withOpacity(0.65 + 0.35 * pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.0 + 1.2 * pulse,
    );
    TextPaint(
      style: const TextStyle(
        color: Colors.white,
        fontSize: 10.5,
        fontWeight: FontWeight.w900,
      ),
    ).render(canvas, 'JUMP', Vector2(c.dx, c.dy - 6.0), anchor: Anchor.center);
    TextPaint(
      style: const TextStyle(
        color: Color(0xFFE0F7FA),
        fontSize: 9.2,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      portal.targetSectorId,
      Vector2(c.dx, c.dy + 7.5),
      anchor: Anchor.center,
    );
  }
}

class WarpFlashComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  WarpFlashComponent() : super(priority: 100);

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    this.size = size;
  }

  @override
  void render(Canvas canvas) {
    if (gameRef.warpFlashTimer <= 0.0) return;
    final double ratio =
        (gameRef.warpFlashTimer / GameConstants.warpFlashDurationSec)
            .clamp(0.0, 1.0);
    final double alpha = (ratio * ratio * 0.85).clamp(0.0, 0.88);
    final Offset center = Offset(size.x * 0.5, size.y * 0.5);
    final double radius = sqrt(size.x * size.x + size.y * size.y) * 0.65;
    canvas.drawRect(
      Offset.zero & Size(size.x, size.y),
      Paint()
        ..shader = ui.Gradient.radial(
          center,
          radius,
          <Color>[
            Colors.white.withOpacity((alpha * 0.92).clamp(0.0, 0.92)),
            const Color(0xFF00E5FF).withOpacity(alpha),
            const Color(0xFF0284C7).withOpacity(alpha * 0.72),
          ],
          const <double>[0.0, 0.55, 1.0],
        ),
    );
  }
}

class ConfigButtonComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  ConfigButtonComponent() : super(size: Vector2(68.0, 38.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(362.0, 14.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.toggleConfig();
  }

  @override
  void render(Canvas canvas) {
    final Color accent = gameRef.activeConfig == 1
        ? const Color(0xFF00E5FF)
        : const Color(0xFFFBBF24);
    final RRect rr = RRect.fromRectAndRadius(
      Offset.zero & Size(size.x, size.y),
      const Radius.circular(8.0),
    );
    canvas.drawRRect(rr, Paint()..color = const Color(0xCC0B1324));
    canvas.drawRRect(
      rr,
      Paint()
        ..color = accent
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.6,
    );
    TextPaint(
      style: TextStyle(
        color: accent,
        fontSize: 11.5,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      'CFG ${gameRef.activeConfig}',
      Vector2(size.x * 0.5, size.y * 0.5),
      anchor: Anchor.center,
    );
  }
}

class FormationButtonComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  FormationButtonComponent() : super(size: Vector2(68.0, 38.0), priority: 42);

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(436.0, 14.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.isFormationMenuOpen = !gameRef.isFormationMenuOpen;
  }

  @override
  void render(Canvas canvas) {
    final DroneFormationSpec spec = gameRef.activeFormationSpec;
    final double cd = gameRef.formationSwitchCooldown;
    final bool onCooldown = cd > 0.05;
    final bool open = gameRef.isFormationMenuOpen;
    final Color accent = open
        ? const Color(0xFF00E5FF)
        : (onCooldown ? const Color(0xFF64748B) : spec.accentColor);
    final RRect rr = RRect.fromRectAndRadius(
      Offset.zero & Size(size.x, size.y),
      const Radius.circular(8.0),
    );
    canvas.drawRRect(
      rr,
      Paint()
        ..color = open ? const Color(0xE60B2239) : const Color(0xCC0B1324),
    );
    canvas.drawRRect(
      rr,
      Paint()
        ..color = accent
        ..style = PaintingStyle.stroke
        ..strokeWidth = open ? 2.0 : 1.6,
    );
    TextPaint(
      style: TextStyle(
        color: accent,
        fontSize: 10.0,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      '${spec.shortCode} ${open ? "▲" : "▼"}',
      Vector2(size.x * 0.5, 13.0),
      anchor: Anchor.center,
    );
    TextPaint(
      style: TextStyle(
        color: onCooldown ? const Color(0xFFFBBF24) : const Color(0xFF94A3B8),
        fontSize: 9.0,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      onCooldown
          ? '${cd.ceil()}s'
          : 'FMN ${gameRef.ownedFormations.length}/${DroneFormation.values.length}',
      Vector2(size.x * 0.5, 27.0),
      anchor: Anchor.center,
    );
  }
}

class FormationSubmenuComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  FormationSubmenuComponent() : super(priority: 41);

  static const double _pillHeight = 32.0;
  static const double _pillGap = 6.0;
  static const double _padX = 8.0;
  static const double _padY = 6.0;
  static const double _defaultPillW = 78.0;

  Vector2 _viewportSize = Vector2(800.0, 400.0);
  double _pillWidth = _defaultPillW;

  List<DroneFormation> get _ownedOrdered => DroneFormation.values
      .where((DroneFormation f) => gameRef.ownedFormations.contains(f))
      .toList();

  void _syncBounds() {
    final List<DroneFormation> owned = _ownedOrdered;
    final int count = max(1, owned.length);
    // Keep horizontal bar cleanly left of the top-right minimap & well above the screen center/joysticks
    final double maxBarWidth = max(140.0, _viewportSize.x - 166.0);
    final double idealWidth =
        _padX * 2.0 + count * _defaultPillW + max(0, count - 1) * _pillGap;
    final double barW = min(idealWidth, maxBarWidth);
    _pillWidth = ((barW - _padX * 2.0 - max(0, count - 1) * _pillGap) / count)
        .clamp(48.0, _defaultPillW);
    final double barH = _padY * 2.0 + _pillHeight;
    size = Vector2(barW, barH);

    // Center neatly below FormationButtonComponent (x=436..504), clamped inside safe HUD band
    final double desiredLeft = 470.0 - barW * 0.5;
    final double maxLeft = max(12.0, _viewportSize.x - barW - 150.0);
    final double targetX = desiredLeft.clamp(12.0, maxLeft);
    position = Vector2(targetX, 56.0);
  }

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    _viewportSize = size.clone();
    _syncBounds();
  }

  @override
  void update(double dt) {
    super.update(dt);
    if (gameRef.isFormationMenuOpen) {
      _syncBounds();
    }
  }

  @override
  bool containsLocalPoint(Vector2 point) {
    if (!gameRef.isFormationMenuOpen) return false;
    return super.containsLocalPoint(point);
  }

  @override
  void onTapDown(TapDownEvent event) {
    if (!gameRef.isFormationMenuOpen) return;
    final List<DroneFormation> owned = _ownedOrdered;
    final double localX = event.localPosition.x - _padX;
    if (localX >= 0.0) {
      final double stride = _pillWidth + _pillGap;
      final int idx = (localX / stride).floor();
      if (idx >= 0 && idx < owned.length) {
        final DroneFormation chosen = owned[idx];
        if (chosen != gameRef.activeFormation &&
            gameRef.formationSwitchCooldown <= 0.05) {
          gameRef.selectOwnedFormationFromHud(chosen);
        }
        gameRef.isFormationMenuOpen = false;
        return;
      }
    }
    gameRef.isFormationMenuOpen = false;
  }

  @override
  void render(Canvas canvas) {
    if (!gameRef.isFormationMenuOpen) return;
    _syncBounds();
    final List<DroneFormation> owned = _ownedOrdered;
    final double cd = gameRef.formationSwitchCooldown;
    final bool onCooldown = cd > 0.05;

    final RRect bg = RRect.fromRectAndRadius(
      Offset.zero & Size(size.x, size.y),
      const Radius.circular(14.0),
    );
    canvas.drawRRect(bg, Paint()..color = const Color(0xF0071020));
    canvas.drawRRect(
      bg,
      Paint()
        ..color = onCooldown ? const Color(0xCCFBBF24) : const Color(0xB800E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.35,
    );

    double x = _padX;
    final double y = _padY;
    for (final DroneFormation f in owned) {
      final DroneFormationSpec spec = DroneFormationCatalog.of(f);
      final bool isActive = f == gameRef.activeFormation;
      final RRect pillRect = RRect.fromRectAndRadius(
        Rect.fromLTWH(x, y, _pillWidth, _pillHeight),
        const Radius.circular(16.0),
      );
      canvas.drawRRect(
        pillRect,
        Paint()
          ..color =
              isActive ? const Color(0x3800E5FF) : const Color(0xB30D182C),
      );
      canvas.drawRRect(
        pillRect,
        Paint()
          ..color =
              isActive ? const Color(0xFF00E5FF) : const Color(0xFF1E3A5F)
          ..style = PaintingStyle.stroke
          ..strokeWidth = isActive ? 1.8 : 1.0,
      );

      // Compact colored formation dot
      canvas.drawCircle(
        Offset(x + 11.0, y + _pillHeight * 0.5),
        3.5,
        Paint()..color = spec.accentColor,
      );

      final String label = _pillWidth >= 66.0
          ? spec.displayName.toUpperCase()
          : spec.shortCode;
      TextPaint(
        style: TextStyle(
          color: isActive
              ? Colors.white
              : (onCooldown ? const Color(0xFF64748B) : spec.accentColor),
          fontSize: _pillWidth >= 66.0 ? 10.2 : 9.8,
          fontWeight: FontWeight.w800,
          letterSpacing: 0.3,
        ),
      ).render(
        canvas,
        label,
        Vector2(x + (_pillWidth + 10.0) * 0.5, y + _pillHeight * 0.5),
        anchor: Anchor.center,
      );

      x += _pillWidth + _pillGap;
    }
  }
}

class AutopilotBadgeComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  AutopilotBadgeComponent() : super(size: Vector2(118.0, 24.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(362.0, 58.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    if (gameRef.autopilotTarget != null) {
      gameRef.cancelAutopilot();
    }
  }

  @override
  void render(Canvas canvas) {
    if (gameRef.autopilotTarget == null) return;
    final double pulse = 0.65 + 0.35 * sin(gameRef.elapsedTime * 6.0);
    final RRect rr = RRect.fromRectAndRadius(
      Offset.zero & Size(size.x, size.y),
      const Radius.circular(7.0),
    );
    canvas.drawRRect(rr, Paint()..color = const Color(0xCC062838));
    canvas.drawRRect(
      rr,
      Paint()
        ..color = const Color(0xFF00E5FF).withOpacity(pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
    TextPaint(
      style: const TextStyle(
        color: Color(0xFF00E5FF),
        fontSize: 10.0,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      'AUTOPILOT • TAP',
      Vector2(size.x * 0.5, size.y * 0.5),
      anchor: Anchor.center,
    );
  }
}

class HangarButtonComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  HangarButtonComponent() : super(size: Vector2(88.0, 38.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(510.0, 14.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.openHangar();
  }

  @override
  void render(Canvas canvas) {
    final bool inSafe = gameRef.player.inSafeZone;
    final bool claimable = gameRef.isMissionReadyToClaim;
    final Color accent = claimable
        ? const Color(0xFF22C55E)
        : (inSafe ? const Color(0xFF00E5FF) : const Color(0xFFFBBF24));
    final RRect rr = RRect.fromRectAndRadius(
      Offset.zero & Size(size.x, size.y),
      const Radius.circular(8.0),
    );
    canvas.drawRRect(
      rr,
      Paint()
        ..color = inSafe ? const Color(0xCC0A2E42) : const Color(0xCC0B1324),
    );
    canvas.drawRRect(
      rr,
      Paint()
        ..color = accent
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );
    TextPaint(
      style: TextStyle(
        color: accent,
        fontSize: 11.5,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      'HANGAR',
      Vector2(size.x * 0.5, size.y * 0.5),
      anchor: Anchor.center,
    );
  }
}

class GalaxyMapButtonComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  GalaxyMapButtonComponent() : super(size: Vector2(126.0, 28.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(size.x - 142.0, 146.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.toggleGalaxyMap();
  }

  @override
  void render(Canvas canvas) {
    final bool hasRoute = gameRef.plottedRoute.length >= 2;
    final Color accent =
        hasRoute ? const Color(0xFFFACC15) : const Color(0xFF00E5FF);
    final RRect rr = RRect.fromRectAndRadius(
      Offset.zero & Size(size.x, size.y),
      const Radius.circular(8.0),
    );
    canvas.drawRRect(rr, Paint()..color = const Color(0xCC0A182D));
    canvas.drawRRect(
      rr,
      Paint()
        ..color = accent
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5,
    );
    TextPaint(
      style: TextStyle(
        color: accent,
        fontSize: 10.2,
        fontWeight: FontWeight.w800,
      ),
    ).render(
      canvas,
      hasRoute
          ? 'GALAXY → ${gameRef.nextRouteHopSectorId ?? ''}'
          : 'GALAXY MAP',
      Vector2(size.x * 0.5, size.y * 0.5),
      anchor: Anchor.center,
    );
  }
}

class MissionHudComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  MissionHudComponent() : super(size: Vector2(340.0, 28.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(16.0, 86.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    if (gameRef.isMissionReadyToClaim) {
      gameRef.claimMissionReward();
    } else {
      gameRef.openHangar();
    }
  }

  @override
  void render(Canvas canvas) {
    final MissionSpec? m = gameRef.activeMission;
    final String label;
    final Color borderColor;
    final Color bgColor;
    final Color textColor;

    if (m == null) {
      label = 'ALL SECTOR MISSIONS COMPLETE';
      borderColor = const Color(0xFF22C55E);
      bgColor = const Color(0xCC06281A);
      textColor = const Color(0xFF4ADE80);
    } else if (gameRef.isMissionReadyToClaim) {
      label = '${m.code} COMPLETE! TAP TO CLAIM REWARD';
      borderColor = const Color(0xFF22C55E);
      bgColor = const Color(0xDD06351E);
      textColor = const Color(0xFF4ADE80);
    } else if (gameRef.isMissionLevelLocked) {
      label = '${m.code} LOCKED — REACH LEVEL ${m.requiredLevel} TO UNLOCK';
      borderColor = const Color(0xFFC084FC);
      bgColor = const Color(0xCC1B1626);
      textColor = const Color(0xFFE9D5FF);
    } else {
      label =
          'MISSION: ${m.description} (${gameRef.currentMissionProgress}/${m.targetCount})';
      borderColor =
          m.isSectorFinal ? const Color(0xFFFBBF24) : const Color(0xFF1E3A5F);
      bgColor = const Color(0xCC0B1324);
      textColor = const Color(0xFFF1F5F9);
    }

    final RRect rr = RRect.fromRectAndRadius(
      Offset.zero & Size(size.x, size.y),
      const Radius.circular(7.0),
    );
    canvas.drawRRect(rr, Paint()..color = bgColor);
    canvas.drawRRect(
      rr,
      Paint()
        ..color = borderColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
    TextPaint(
      style: TextStyle(
        color: textColor,
        fontSize: 10.5,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      label,
      Vector2(10.0, 14.0),
      anchor: Anchor.centerLeft,
    );
  }
}

class TacticalHudComponent extends PositionComponent
    with HasGameRef<DarkOrbitGame> {
  TacticalHudComponent()
      : super(position: Vector2(16.0, 14.0), size: Vector2(340.0, 68.0));

  @override
  void render(Canvas canvas) {
    final RRect topBg = RRect.fromRectAndRadius(
      const Rect.fromLTWH(0.0, 0.0, 340.0, 40.0),
      const Radius.circular(9.0),
    );
    canvas.drawRRect(topBg, Paint()..color = const Color(0xCC0B1324));
    canvas.drawRRect(
      topBg,
      Paint()
        ..color = const Color(0xFF1E3A5F)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.2,
    );

    final TextPaint crPaint = TextPaint(
      style: const TextStyle(
        color: Color(0xFFFBBF24),
        fontSize: 12.0,
        fontWeight: FontWeight.bold,
      ),
    );
    final TextPaint lvPaint = TextPaint(
      style: const TextStyle(
        color: Color(0xFFC084FC),
        fontSize: 11.5,
        fontWeight: FontWeight.bold,
      ),
    );
    canvas.save();
    canvas.clipRect(const Rect.fromLTWH(0.0, 0.0, 192.0, 40.0));
    crPaint.render(
      canvas,
      '${gameRef.credits} CR  •  KILLS ${gameRef.kills}',
      Vector2(10.0, 6.0),
    );
    lvPaint.render(
      canvas,
      'LV ${gameRef.player.level}  •  ${gameRef.activeShipDisplayName.toUpperCase()}',
      Vector2(10.0, 22.0),
    );
    canvas.restore();

    // Real-time Shield & Hull bars with crisp bold white shadowed numeric readouts
    final int curShd = max(0, gameRef.player.shield.round());
    final int maxShd = max(1, gameRef.currentMaxShield.round());
    final int curHp = max(0, gameRef.player.hp.round());
    final int maxHp = max(1, gameRef.currentMaxHull.round());
    final double shdRatio = (curShd / maxShd).clamp(0.0, 1.0);
    final double hpRatio = (curHp / maxHp).clamp(0.0, 1.0);
    final Color hpFillColor =
        hpRatio < 0.35 ? const Color(0xFFFF2E4C) : const Color(0xFF22C55E);

    const double barX = 196.0;
    const double barW = 138.0;
    const double barH = 14.5;

    final RRect shdTrack = RRect.fromRectAndRadius(
      const Rect.fromLTWH(barX, 4.0, barW, barH),
      const Radius.circular(4.0),
    );
    canvas.drawRRect(shdTrack, Paint()..color = const Color(0xFF060C18));
    if (shdRatio > 0.0) {
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(barX, 4.0, barW * shdRatio, barH),
          const Radius.circular(4.0),
        ),
        Paint()..color = const Color(0xE038BDF8),
      );
    }
    canvas.drawRRect(
      shdTrack,
      Paint()
        ..color = const Color(0x7338BDF8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 0.9,
    );

    final RRect hpTrack = RRect.fromRectAndRadius(
      const Rect.fromLTWH(barX, 21.5, barW, barH),
      const Radius.circular(4.0),
    );
    canvas.drawRRect(hpTrack, Paint()..color = const Color(0xFF060C18));
    if (hpRatio > 0.0) {
      canvas.drawRRect(
        RRect.fromRectAndRadius(
          Rect.fromLTWH(barX, 21.5, barW * hpRatio, barH),
          const Radius.circular(4.0),
        ),
        Paint()..color = hpFillColor.withOpacity(0.88),
      );
    }
    canvas.drawRRect(
      hpTrack,
      Paint()
        ..color = hpFillColor.withOpacity(0.45)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 0.9,
    );

    final TextPaint vitalTextPaint = TextPaint(
      style: const TextStyle(
        color: Colors.white,
        fontSize: 9.6,
        fontWeight: FontWeight.bold,
        shadows: <Shadow>[
          Shadow(
            color: Color(0xEB000000),
            offset: Offset(0.9, 1.1),
            blurRadius: 2.2,
          ),
        ],
      ),
    );
    vitalTextPaint.render(
      canvas,
      'SHD: $curShd / $maxShd',
      Vector2(barX + barW * 0.5, 4.0 + barH * 0.5),
      anchor: Anchor.center,
    );
    vitalTextPaint.render(
      canvas,
      'HP: $curHp / $maxHp',
      Vector2(barX + barW * 0.5, 21.5 + barH * 0.5),
      anchor: Anchor.center,
    );

    // Active Sector Badge + Zone Risk Indicator + Border Radiation Warning row
    final SectorNode sector = gameRef.currentSector;
    final Color mapColor = sector.nodeColor;
    final Color mapBg = sector.zoneType.badgeBgColor;
    final RRect mapChip = RRect.fromRectAndRadius(
      const Rect.fromLTWH(0.0, 45.0, 76.0, 22.0),
      const Radius.circular(6.0),
    );
    canvas.drawRRect(mapChip, Paint()..color = mapBg);
    canvas.drawRRect(
      mapChip,
      Paint()
        ..color = mapColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.2,
    );
    TextPaint(
      style: TextStyle(
        color: mapColor,
        fontSize: 9.8,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      'SEC ${sector.id}',
      Vector2(38.0, 56.0),
      anchor: Anchor.center,
    );

    final String zoneLabel;
    final Color zoneColor;
    final Color zoneBg;
    if (gameRef.player.inSafeZone) {
      zoneLabel = sector.isNucleus ? 'BLACK MARKET SAFE' : 'SAFE ZONE';
      zoneColor = const Color(0xFF00E5FF);
      zoneBg = const Color(0xCC062838);
    } else if (gameRef.player.isTakingZoneRadiation) {
      zoneLabel = 'BORDER RADIATION';
      zoneColor = const Color(0xFFFF2E4C);
      zoneBg = const Color(0xDD7F1D1D);
    } else {
      zoneLabel = sector.zoneType.shortLabel;
      zoneColor = sector.zoneType.badgeColor;
      zoneBg = sector.zoneType.badgeBgColor;
    }

    final RRect zoneChip = RRect.fromRectAndRadius(
      const Rect.fromLTWH(82.0, 45.0, 146.0, 22.0),
      const Radius.circular(6.0),
    );
    canvas.drawRRect(zoneChip, Paint()..color = zoneBg);
    canvas.drawRRect(
      zoneChip,
      Paint()
        ..color = zoneColor
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.2,
    );
    TextPaint(
      style: TextStyle(
        color: zoneColor,
        fontSize: 9.4,
        fontWeight: FontWeight.bold,
      ),
    ).render(canvas, zoneLabel, Vector2(90.0, 50.0));

    if (gameRef.nextRouteHopSectorId != null) {
      final int hops = gameRef.plottedRoute.length - 1;
      final RRect routeChip = RRect.fromRectAndRadius(
        const Rect.fromLTWH(234.0, 45.0, 106.0, 22.0),
        const Radius.circular(6.0),
      );
      canvas.drawRRect(routeChip, Paint()..color = const Color(0xCC06253A));
      canvas.drawRRect(
        routeChip,
        Paint()
          ..color = const Color(0xFF00E5FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.1,
      );
      TextPaint(
        style: const TextStyle(
          color: Color(0xFF00E5FF),
          fontSize: 9.0,
          fontWeight: FontWeight.bold,
        ),
      ).render(
        canvas,
        '→ ${gameRef.nextRouteHopSectorId} (${hops}J)',
        Vector2(241.0, 50.0),
      );
    }
  }
}

class MinimapComponent extends PositionComponent
    with TapCallbacks, HasGameRef<DarkOrbitGame> {
  MinimapComponent() : super(size: Vector2.all(126.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(size.x - 142.0, 14.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.toggleTacticalMap();
  }

  @override
  void render(Canvas canvas) {
    final Offset center = Offset(size.x * 0.5, size.y * 0.5);
    final double radarR = size.x * 0.5;
    const double viewRadius = GameConstants.hudMinimapViewRadius;
    final double scale = (radarR - 2.0) / viewRadius;
    final double pulse = 0.55 + 0.45 * sin(gameRef.elapsedTime * 6.5);

    final double playerX = gameRef.player.position.x;
    final double playerY = gameRef.player.position.y;

    final SectorNode sector = gameRef.currentSector;
    final double mapSize = sector.mapSize;
    canvas.drawCircle(
      center,
      radarR,
      Paint()..color = const Color(0xDC080D1E),
    );

    final Path clipCircle = Path()
      ..addOval(Rect.fromCircle(center: center, radius: radarR - 1.0));
    canvas.save();
    canvas.clipPath(clipCircle);

    // Subtle local radar range ring (2,250u) & crosshairs
    final Paint gridPaint = Paint()
      ..color = const Color(0x2638BDF8)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.9;
    canvas.drawCircle(center, radarR * 0.5, gridPaint);
    canvas.drawLine(
      Offset(center.dx - radarR, center.dy),
      Offset(center.dx + radarR, center.dy),
      gridPaint,
    );
    canvas.drawLine(
      Offset(center.dx, center.dy - radarR),
      Offset(center.dx, center.dy + radarR),
      gridPaint,
    );

    // Straight red square edge walls & outer radiation fill when approaching borders
    final double leftWallX = center.dx + (0.0 - playerX) * scale;
    final double rightWallX = center.dx + (mapSize - playerX) * scale;
    final double topWallY = center.dy + (0.0 - playerY) * scale;
    final double bottomWallY = center.dy + (mapSize - playerY) * scale;

    final Paint wallRad = Paint()..color = const Color(0x36FF1744);
    if (leftWallX > 0.0) {
      canvas.drawRect(
        Rect.fromLTWH(0.0, 0.0, min(size.x, leftWallX), size.y),
        wallRad,
      );
    }
    if (rightWallX < size.x) {
      final double rx = max(0.0, rightWallX);
      canvas.drawRect(
        Rect.fromLTWH(rx, 0.0, max(0.0, size.x - rx), size.y),
        wallRad,
      );
    }
    if (topWallY > 0.0) {
      canvas.drawRect(
        Rect.fromLTWH(0.0, 0.0, size.x, min(size.y, topWallY)),
        wallRad,
      );
    }
    if (bottomWallY < size.y) {
      final double by = max(0.0, bottomWallY);
      canvas.drawRect(
        Rect.fromLTWH(0.0, by, size.x, max(0.0, size.y - by)),
        wallRad,
      );
    }

    canvas.drawRect(
      Rect.fromLTWH(
        leftWallX,
        topWallY,
        mapSize * scale,
        mapSize * scale,
      ),
      Paint()
        ..color = const Color(0xF0FF2E4C)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.4,
    );

    // Central Safe Zone when within radar view (Cities, Level 1, Nucleus 0-0 Black Market)
    if (sector.hasCentralSafeZone) {
      final Vector2 safeWorld = gameRef.activeSafeZoneCenter;
      final double dxSafe = safeWorld.x - playerX;
      final double dySafe = safeWorld.y - playerY;
      if (sqrt(dxSafe * dxSafe + dySafe * dySafe) <=
          viewRadius + sector.safeZoneRadius + 120.0) {
        final Offset safeCenter =
            Offset(center.dx + dxSafe * scale, center.dy + dySafe * scale);
        final double safeR = sector.safeZoneRadius * scale;
        canvas.drawCircle(
          safeCenter,
          safeR,
          Paint()..color = const Color(0x3000E5FF),
        );
        canvas.drawCircle(
          safeCenter,
          safeR,
          Paint()
            ..color = sector.isNucleus
                ? const Color(0xFFFACC15)
                : const Color(0xDD00E5FF)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.5,
        );
        canvas.drawCircle(safeCenter, 2.2, Paint()..color = Colors.white);
      }
    }

    // All active sector portals visible on local radar
    final String? nextHop = gameRef.nextRouteHopSectorId;
    for (final SectorPortal portal in gameRef.activePortals) {
      final double dxP = portal.worldX - playerX;
      final double dyP = portal.worldY - playerY;
      if (sqrt(dxP * dxP + dyP * dyP) > viewRadius + 240.0) continue;
      final Offset pOff = Offset(center.dx + dxP * scale, center.dy + dyP * scale);
      final bool isNext = portal.targetSectorId == nextHop;
      canvas.drawCircle(
        pOff,
        isNext ? 5.2 + 1.5 * pulse : 4.2,
        Paint()
          ..color = (isNext ? const Color(0xFF00E5FF) : portal.accentColor)
              .withOpacity(0.55 + 0.4 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = isNext ? 1.8 : 1.3,
      );
      canvas.drawCircle(pOff, 1.9, Paint()..color = Colors.white);
    }

    // Nearby Bonus & Cargo Boxes within 4,500u radius
    const double boxCutoffSq = (viewRadius + 150.0) * (viewRadius + 150.0);
    for (final LootBoxComponent box in gameRef.lootBoxes) {
      final double dx = box.position.x - playerX;
      final double dy = box.position.y - playerY;
      if (dx * dx + dy * dy > boxCutoffSq) continue;
      final bool isCargo = box.kind == LootBoxKind.cargoBox;
      canvas.drawCircle(
        Offset(center.dx + dx * scale, center.dy + dy * scale),
        isCargo ? 2.6 : 1.9,
        Paint()
          ..color =
              isCargo ? const Color(0xFFFBBF24) : const Color(0xCCFACC15),
      );
    }

    // Local Radar Detection Range: only show Normal & BOSS Aliens within 2,800 world units
    const double alienCutoffSq =
        GameConstants.alienRadarDetectionRange *
        GameConstants.alienRadarDetectionRange;
    for (final AlienShipComponent alien in gameRef.aliens) {
      final double dx = alien.position.x - playerX;
      final double dy = alien.position.y - playerY;
      if (dx * dx + dy * dy > alienCutoffSq) continue;
      final Offset aPos = Offset(center.dx + dx * scale, center.dy + dy * scale);
      if (gameRef.lockedTarget == alien) {
        canvas.drawCircle(
          aPos,
          alien.isBoss
              ? alien.spec.minimapDotRadius + 6.0
              : alien.spec.minimapDotRadius + 4.0,
          Paint()
            ..color = const Color(0xDDFF2E4C)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.4,
        );
      }
      if (alien.isBoss) {
        canvas.drawCircle(
          aPos,
          alien.spec.minimapDotRadius + 3.8 + 1.8 * pulse,
          Paint()
            ..color = const Color(0xFFFBBF24).withOpacity(0.28 + 0.40 * pulse),
        );
        canvas.drawCircle(
          aPos,
          alien.spec.minimapDotRadius + 1.9,
          Paint()..color = const Color(0xFFFF2E4C),
        );
        canvas.drawCircle(
          aPos,
          alien.spec.minimapDotRadius * 0.78,
          Paint()..color = const Color(0xFFFBBF24),
        );
      } else {
        canvas.drawCircle(
          aPos,
          max(2.6, alien.spec.minimapDotRadius * 1.15),
          Paint()..color = alien.spec.primaryColor,
        );
      }
    }

    // Active Autopilot dashed trajectory line from center toward waypoint (or edge beacon if > 4,500u)
    final Vector2? ap = gameRef.autopilotTarget;
    if (ap != null) {
      final double dx = ap.x - playerX;
      final double dy = ap.y - playerY;
      final double worldDist = sqrt(dx * dx + dy * dy);
      if (worldDist > 1.0) {
        final double ux = dx / worldDist;
        final double uy = dy / worldDist;
        final bool withinRadar = worldDist <= viewRadius;
        final Offset endPt = withinRadar
            ? Offset(center.dx + dx * scale, center.dy + dy * scale)
            : Offset(
                center.dx + ux * (radarR - 4.0),
                center.dy + uy * (radarR - 4.0),
              );
        final double lineDist = (endPt - center).distance;
        final Paint dash = Paint()
          ..color = const Color(0xCC00E5FF)
          ..strokeWidth = 1.5;
        double d = 0.0;
        while (d < lineDist) {
          final double dEnd = min(lineDist, d + 4.5);
          canvas.drawLine(
            Offset(center.dx + ux * d, center.dy + uy * d),
            Offset(center.dx + ux * dEnd, center.dy + uy * dEnd),
            dash,
          );
          d += 8.5;
        }
        if (withinRadar) {
          canvas.drawCircle(
            endPt,
            4.8 + 1.4 * pulse,
            Paint()
              ..color = const Color(0xFF00E5FF).withOpacity(0.5 + 0.45 * pulse)
              ..style = PaintingStyle.stroke
              ..strokeWidth = 1.3,
          );
          canvas.drawCircle(endPt, 1.9, Paint()..color = Colors.white);
        } else {
          final Path beacon = Path()
            ..moveTo(
              center.dx + ux * (radarR - 1.5),
              center.dy + uy * (radarR - 1.5),
            )
            ..lineTo(
              center.dx + ux * (radarR - 8.0) - uy * 4.0,
              center.dy + uy * (radarR - 8.0) + ux * 4.0,
            )
            ..lineTo(
              center.dx + ux * (radarR - 8.0) + uy * 4.0,
              center.dy + uy * (radarR - 8.0) - ux * 4.0,
            )
            ..close();
          canvas.drawPath(
            beacon,
            Paint()
              ..color =
                  const Color(0xFF00E5FF).withOpacity(0.65 + 0.35 * pulse),
          );
        }
      }
    }

    // Crisp bright cyan/white directional arrow for player ship at exact center
    final double a = gameRef.player.angle;
    final double cA = cos(a);
    final double sA = sin(a);
    canvas.drawCircle(
      center,
      11.5,
      Paint()..color = const Color(0x4400E5FF),
    );
    final Path arrow = Path()
      ..moveTo(center.dx + cA * 10.0, center.dy + sA * 10.0)
      ..lineTo(
        center.dx + (-cA * 6.2 - sA * 6.2),
        center.dy + (-sA * 6.2 + cA * 6.2),
      )
      ..lineTo(center.dx - cA * 2.4, center.dy - sA * 2.4)
      ..lineTo(
        center.dx + (-cA * 6.2 + sA * 6.2),
        center.dy + (-sA * 6.2 - cA * 6.2),
      )
      ..close();
    canvas.drawPath(arrow, Paint()..color = const Color(0xFF00E5FF));
    canvas.drawPath(
      arrow,
      Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5,
    );

    canvas.restore();

    // Crisp outer circular border
    canvas.drawCircle(
      center,
      radarR - 1.0,
      Paint()
        ..color = const Color(0xCC00E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );
  }
}
