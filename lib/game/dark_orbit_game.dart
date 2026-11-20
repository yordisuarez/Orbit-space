import 'dart:convert';
import 'dart:math';

import 'package:flame/components.dart';
import 'package:flame/events.dart';
import 'package:flame/game.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'game_models.dart';

export 'game_models.dart';

class DarkOrbitGame extends FlameGame with TapCallbacks {
  static const String hangarOverlayId = 'hangar';
  static const String tacticalMapOverlayId = 'tactical_map';
  static const String _savePrefsKey = 'dark_orbit_mobile_save_v2';

  final Random rng = Random();

  late PlayerShipComponent player;
  late VantFleetComponent vantFleet;
  late JoystickComponent moveJoystick;
  late JoystickComponent aimJoystick;
  late AttackButtonComponent attackButton;
  late RocketButtonComponent rocketButton;
  late ConfigButtonComponent configButton;
  late FormationButtonComponent formationButton;
  late HangarButtonComponent hangarButton;
  late AutopilotBadgeComponent autopilotBadge;
  late MissionHudComponent missionHud;
  late MinimapComponent minimap;
  late TacticalHudComponent tacticalHud;

  final List<AlienShipComponent> aliens = <AlienShipComponent>[];
  final List<LootBoxComponent> lootBoxes = <LootBoxComponent>[];

  // Owned Ship Classes & Active Ship Class
  final Set<ShipClass> ownedShipClasses = <ShipClass>{ShipClass.interceptor};
  ShipClass activeShipClass = ShipClass.interceptor;

  // Classic DarkOrbit VANT Drone Formations
  final Set<DroneFormation> ownedFormations = <DroneFormation>{
    DroneFormation.normal,
  };
  DroneFormation activeFormation = DroneFormation.normal;
  double formationSwitchCooldown = 0.0;

  // Tap-to-Fly Autopilot Waypoint
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

  ShipClassSpec get activeShipSpec => ShipClassCatalog.of(activeShipClass);
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

  List<EquipmentItem> get inventory {
    final Set<int> equippedIds = <int>{};
    for (final EquipmentItem? item in laserSlots) {
      if (item != null) equippedIds.add(item.instanceId);
    }
    for (final EquipmentItem? item in shieldSlots) {
      if (item != null) equippedIds.add(item.instanceId);
    }
    for (final VantDrone vant in ownedVants) {
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

  SpaceZone get currentZone => zoneForDistance(player.position.length);

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
    final double halfW = size.x * 0.5 + margin;
    final double halfH = size.y * 0.5 + margin;
    return (worldPos.x - cam.x).abs() <= halfW &&
        (worldPos.y - cam.y).abs() <= halfH;
  }

  // Dynamic Ship Stats (Ship + VANT active config gear + Active Drone Formation modifiers)
  double get currentLaserDamage {
    double sum = GameConstants.playerBaseLaserDamage;
    for (final EquipmentItem? item in laserSlots) {
      if (item != null) sum += item.spec.damageBonus;
    }
    for (final VantDrone vant in ownedVants) {
      for (final EquipmentItem? item in vant.slotsForConfig(activeConfig)) {
        if (item != null) sum += item.spec.damageBonus;
      }
    }
    return max(1.0, sum * activeFormationSpec.damageMult);
  }

  double get currentMaxShield {
    double sum = GameConstants.playerBaseMaxShield;
    for (final EquipmentItem? item in shieldSlots) {
      if (item != null) sum += item.spec.shieldBonus;
    }
    for (final VantDrone vant in ownedVants) {
      for (final EquipmentItem? item in vant.slotsForConfig(activeConfig)) {
        if (item != null) sum += item.spec.shieldBonus;
      }
    }
    return max(10.0, sum * activeFormationSpec.shieldMult);
  }

  double get currentSpeed => activeShipSpec.speed * activeFormationSpec.speedMult;

  double get currentMaxHull {
    final double base = activeShipSpec.baseHull +
        (player.level - 1) * GameConstants.playerHpPerLevel;
    return max(40.0, base * activeFormationSpec.hullMult);
  }

  @override
  Color backgroundColor() => const Color(0xFF040711);

  @override
  Future<void> onLoad() async {
    await super.onLoad();

    await world.add(SpaceMapComponent());

    player = PlayerShipComponent(position: Vector2.zero());
    vantFleet = VantFleetComponent();
    _initStarterState();
    await _loadSavedGame();
    player
      ..hp = currentMaxHull
      ..shield = currentMaxShield;
    _snapVantsToFormation();
    await world.add(player);
    await world.add(vantFleet);

    for (final AlienTier tier in AlienTier.values) {
      final AlienTierSpec spec = AlienTierCatalog.of(tier);
      for (int i = 0; i < spec.activeCount; i++) {
        spawnAlien(tier, minDistFromPlayer: 520.0);
      }
    }

    for (int i = 0; i < GameConstants.bonusBoxCount; i++) {
      spawnBonusBox();
    }

    camera.viewfinder.anchor = Anchor.center;
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
    configButton = ConfigButtonComponent();
    formationButton = FormationButtonComponent();
    hangarButton = HangarButtonComponent();
    autopilotBadge = AutopilotBadgeComponent();
    missionHud = MissionHudComponent();
    minimap = MinimapComponent();
    tacticalHud = TacticalHudComponent();

    await camera.viewport.addAll(<Component>[
      moveJoystick,
      aimJoystick,
      attackButton,
      rocketButton,
      configButton,
      formationButton,
      hangarButton,
      autopilotBadge,
      missionHud,
      minimap,
      tacticalHud,
    ]);
  }

  void _initStarterState() {
    credits = 0;
    kills = 0;
    player.level = 1;
    player.xp = 0;
    activeConfig = 1;
    ownedShipClasses
      ..clear()
      ..add(ShipClass.interceptor);
    activeShipClass = ShipClass.interceptor;
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
    ownedItems.addAll(<EquipmentItem>[starterLaser, starterShield]);
    config1LaserSlots[0] = starterLaser;
    config1ShieldSlots[0] = starterShield;
    config2LaserSlots[0] = starterLaser;
    config2ShieldSlots[0] = starterShield;
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
      };
      await prefs.setString(_savePrefsKey, jsonEncode(payload));
    } catch (_) {
      // Safe fallback if SharedPreferences is unavailable in test host
    }
  }

  Future<void> _loadSavedGame() async {
    try {
      final SharedPreferences prefs = await SharedPreferences.getInstance();
      final String? raw = prefs.getString(_savePrefsKey);
      if (raw == null || raw.isEmpty) return;

      final Map<String, dynamic> data = jsonDecode(raw) as Map<String, dynamic>;
      final List<dynamic> rawItems =
          (data['ownedItems'] as List<dynamic>?) ?? <dynamic>[];
      final List<EquipmentItem> parsedItems = <EquipmentItem>[];
      for (final dynamic item in rawItems) {
        final Map<String, dynamic> m = item as Map<String, dynamic>;
        final int? id = m['id'] as int?;
        final String? typeName = m['type'] as String?;
        if (id == null || typeName == null) continue;
        EquipmentItemId? matched;
        for (final EquipmentItemId e in EquipmentItemId.values) {
          if (e.name == typeName) {
            matched = e;
            break;
          }
        }
        if (matched != null) {
          parsedItems.add(EquipmentItem(instanceId: id, id: matched));
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
          if (ownedVants.length >= GameConstants.maxVants) break;
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

      final int rawMissionIdx = (data['currentMissionIndex'] as int?) ?? 0;
      currentMissionIndex = rawMissionIdx.clamp(0, MissionCatalog.all.length);
      final MissionSpec? m = activeMission;
      final int rawProg = (data['currentMissionProgress'] as int?) ?? 0;
      currentMissionProgress = m != null ? rawProg.clamp(0, m.targetCount) : 0;
    } catch (_) {
      // Keep starter state on corrupt save
    }
  }

  Future<void> resetSave() async {
    try {
      final SharedPreferences prefs = await SharedPreferences.getInstance();
      await prefs.remove(_savePrefsKey);
    } catch (_) {
      // Ignore if SharedPreferences unavailable
    }
    _initStarterState();
    player
      ..position.setZero()
      ..hp = currentMaxHull
      ..shield = currentMaxShield
      ..rocketCooldown = 0.0;
    autoFireEnabled = false;
    lockedTarget = null;
    autopilotTarget = null;
    formationSwitchCooldown = 0.0;
    _snapVantsToFormation();
    await saveGame();
  }

  @override
  void update(double dt) {
    super.update(dt);
    elapsedTime += dt;
    if (formationSwitchCooldown > 0.0) {
      formationSwitchCooldown = max(0.0, formationSwitchCooldown - dt);
    }

    if (aimJoystick.relativeDelta.length > 0.12) {
      final double aimAngle = atan2(
        aimJoystick.relativeDelta.y,
        aimJoystick.relativeDelta.x,
      );
      _lockClosestAlienInRadarCone(aimAngle);
    }

    _updateVantFormationPositions(dt);
  }

  void openHangar() {
    overlays.remove(tacticalMapOverlayId);
    pauseEngine();
    overlays.add(hangarOverlayId);
  }

  void closeHangar() {
    _syncPlayerVitalsToStats();
    saveGame();
    overlays.remove(hangarOverlayId);
    resumeEngine();
  }

  void openTacticalMap() {
    if (overlays.isActive(hangarOverlayId)) return;
    overlays.add(tacticalMapOverlayId);
  }

  void closeTacticalMap() {
    overlays.remove(tacticalMapOverlayId);
  }

  void toggleTacticalMap() {
    if (overlays.isActive(tacticalMapOverlayId)) {
      closeTacticalMap();
    } else {
      openTacticalMap();
    }
  }

  void setAutopilotTarget(double worldX, double worldY) {
    double tx = worldX;
    double ty = worldY;
    final double dist = sqrt(tx * tx + ty * ty);
    if (dist > GameConstants.autopilotMaxTargetRadius && dist > 0.001) {
      final double s = GameConstants.autopilotMaxTargetRadius / dist;
      tx *= s;
      ty *= s;
    }
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
    if (credits < spec.cost) return false;
    credits -= spec.cost;
    ownedFormations.add(formation);
    activeFormation = formation;
    _syncPlayerVitalsToStats();
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
    activeFormation = next;
    formationSwitchCooldown = GameConstants.formationSwitchCooldownSec;
    _syncPlayerVitalsToStats();
    spawnFloatingText(
      position: player.position + Vector2(0.0, -46.0),
      text: '${DroneFormationCatalog.of(next).displayName} FORMATION',
      color: DroneFormationCatalog.of(next).accentColor,
      fontSize: 13.5,
    );
    saveGame();
    return true;
  }

  void _snapVantsToFormation() {
    final int total = ownedVants.length;
    if (total == 0) return;
    final double hullR = activeShipSpec.hullRadius;
    final double cosA = cos(player.angle);
    final double sinA = sin(player.angle);
    for (int i = 0; i < total; i++) {
      final VantDrone vant = ownedVants[i];
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
    final int total = ownedVants.length;
    if (total == 0) return;
    final double hullR = activeShipSpec.hullRadius;
    final double cosA = cos(player.angle);
    final double sinA = sin(player.angle);
    final double posAlpha = 1.0 - exp(-9.5 * dt);
    final double rotAlpha = 1.0 - exp(-11.0 * dt);

    for (int i = 0; i < total; i++) {
      final VantDrone vant = ownedVants[i];
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
    if (ownedShipClasses.contains(shipClass)) {
      if (activeShipClass == shipClass) return false;
      activeShipClass = shipClass;
      _resizeSlotsForShip(shipClass);
      _syncPlayerVitalsToStats();
      saveGame();
      return true;
    }
    final ShipClassSpec spec = ShipClassCatalog.of(shipClass);
    if (credits < spec.cost) return false;
    final double oldMaxHull = currentMaxHull;
    credits -= spec.cost;
    ownedShipClasses.add(shipClass);
    activeShipClass = shipClass;
    _resizeSlotsForShip(shipClass);
    final double hullDelta = max(0.0, currentMaxHull - oldMaxHull);
    player.hp = min(currentMaxHull, player.hp + hullDelta);
    _syncPlayerVitalsToStats();
    saveGame();
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
    if (ownedVants.length >= GameConstants.maxVants) return false;
    if (credits < GameConstants.flaxVantCost) return false;
    credits -= GameConstants.flaxVantCost;
    ownedVants.add(VantDrone(id: _nextVantId++, tier: VantTier.flax));
    saveGame();
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
    _syncPlayerVitalsToStats();
    saveGame();
    return true;
  }

  bool equipItemToVantSlot(int vantId, int slotIndex, {int? instanceId}) {
    VantDrone? vant;
    for (final VantDrone v in ownedVants) {
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

    for (final VantDrone vant in ownedVants) {
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
    for (final VantDrone v in ownedVants) {
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
    if (cost == null || credits < cost) {
      return false;
    }
    credits -= cost;
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

    final Set<int> activeIds = <int>{};
    for (final EquipmentItem? item in laserSlots) {
      if (item != null) activeIds.add(item.instanceId);
    }
    for (final EquipmentItem? item in shieldSlots) {
      if (item != null) activeIds.add(item.instanceId);
    }
    for (final VantDrone vant in ownedVants) {
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
    for (final VantDrone vant in ownedVants) {
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
    for (final VantDrone vant in ownedVants) {
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
                (activeShipSpec.hullRadius + 4.0),
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

  void spawnAlien(AlienTier tier, {double minDistFromPlayer = 480.0}) {
    final AlienTierSpec spec = AlienTierCatalog.of(tier);
    final bool isBoss = rng.nextDouble() < GameConstants.bossSpawnChance;
    final double effRadius =
        isBoss ? spec.radius * GameConstants.bossSizeMultiplier : spec.radius;
    final double inner = spec.minRadius + effRadius + 50.0;
    final double outer = spec.maxRadius - effRadius - 50.0;
    Vector2 pos = randomPointInAnnulus(inner, outer);
    for (int i = 0; i < 16; i++) {
      final Vector2 candidate = randomPointInAnnulus(inner, outer);
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
    final Vector2 pos = randomPointInAnnulus(
      GameConstants.safeZoneRadius + 90.0,
      GameConstants.mapRadius - 160.0,
    );
    final SpaceZone zone = zoneForDistance(pos.length);
    final int reward;
    final int xpReward;
    final double shieldAmount;
    switch (zone) {
      case SpaceZone.zone1Alpha:
      case SpaceZone.safeZone:
        reward = (4 + rng.nextInt(5)) * 25;
        xpReward = 15;
        shieldAmount = 40.0;
      case SpaceZone.zone2Beta:
        reward = (8 + rng.nextInt(8)) * 25;
        xpReward = 35;
        shieldAmount = 70.0;
      case SpaceZone.zone3Gamma:
        reward = (16 + rng.nextInt(11)) * 25;
        xpReward = 70;
        shieldAmount = 110.0;
      case SpaceZone.zone4Omega:
        reward = (28 + rng.nextInt(17)) * 25;
        xpReward = 140;
        shieldAmount = 160.0;
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
    // Guaranteed Cargo Box drop
    spawnCargoBox(alien.position, spec, isBoss: alien.isBoss);
    if (lockedTarget == alien) {
      lockedTarget = null;
    }
    spawnAlien(alien.tier);
    saveGame();
  }

  @override
  void onTapDown(TapDownEvent event) {
    super.onTapDown(event);
    final Vector2 worldPos = camera.globalToLocal(event.canvasPosition);
    AlienShipComponent? tapped;
    double bestDist = 74.0;
    for (final AlienShipComponent alien in aliens) {
      final double d = alien.position.distanceTo(worldPos);
      if (d <= max(bestDist, alien.effectiveRadius + 32.0)) {
        bestDist = d;
        tapped = alien;
      }
    }
    if (tapped != null) {
      lockedTarget = tapped;
    }
  }
}

// =============================================================================
// SPACE MAP (8000-Radius Circular Map, 4 Concentric Zone Rings & Central Base)
// =============================================================================
class SpaceMapComponent extends Component with HasGameRef<DarkOrbitGame> {
  final List<_StarData> _stars = <_StarData>[];

  @override
  Future<void> onLoad() async {
    final Random starRng = Random(42019);
    for (int i = 0; i < GameConstants.starCount; i++) {
      final double angle = starRng.nextDouble() * 2.0 * pi;
      final double radius =
          sqrt(starRng.nextDouble()) * (GameConstants.mapRadius - 30.0);
      _stars.add(
        _StarData(
          position: Offset(cos(angle) * radius, sin(angle) * radius),
          radius: 0.9 + starRng.nextDouble() * 2.0,
          alpha: 0.28 + starRng.nextDouble() * 0.7,
        ),
      );
    }
  }

  @override
  void render(Canvas canvas) {
    const double maxR = GameConstants.mapRadius;
    final Vector2 cam = gameRef.camera.viewfinder.position;
    final double halfW = gameRef.size.x * 0.5 + 80.0;
    final double halfH = gameRef.size.y * 0.5 + 80.0;
    final double halfDiag = sqrt(halfW * halfW + halfH * halfH);
    final double camDist = cam.length;
    final double minVisR = max(0.0, camDist - halfDiag - 60.0);
    final double maxVisR = camDist + halfDiag + 60.0;

    final Paint starPaint = Paint();
    for (final _StarData star in _stars) {
      if ((star.position.dx - cam.x).abs() > halfW ||
          (star.position.dy - cam.y).abs() > halfH) {
        continue;
      }
      starPaint.color = const Color(0xFFE2E8F0).withOpacity(star.alpha);
      canvas.drawCircle(star.position, star.radius, starPaint);
    }

    final Paint gridPaint = Paint()
      ..color = const Color(0x1438BDF8)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.0;
    for (double r = 2500.0; r < maxR; r += 2500.0) {
      if (r == GameConstants.zone1MaxRadius ||
          r == GameConstants.zone2MaxRadius ||
          r == GameConstants.zone3MaxRadius) {
        continue;
      }
      if (r >= minVisR && r <= maxVisR) {
        canvas.drawCircle(Offset.zero, r, gridPaint);
      }
    }
    for (int i = 0; i < 8; i++) {
      final double a = i * pi * 0.25;
      canvas.drawLine(
        Offset.zero,
        Offset(cos(a) * maxR, sin(a) * maxR),
        gridPaint,
      );
    }

    if (GameConstants.zone1MaxRadius >= minVisR &&
        GameConstants.zone1MaxRadius <= maxVisR) {
      _drawDashedZoneRing(
        canvas,
        GameConstants.zone1MaxRadius,
        const Color(0xFF4ADE80),
      );
    }
    if (GameConstants.zone2MaxRadius >= minVisR &&
        GameConstants.zone2MaxRadius <= maxVisR) {
      _drawDashedZoneRing(
        canvas,
        GameConstants.zone2MaxRadius,
        const Color(0xFFFACC15),
      );
    }
    if (GameConstants.zone3MaxRadius >= minVisR &&
        GameConstants.zone3MaxRadius <= maxVisR) {
      _drawDashedZoneRing(
        canvas,
        GameConstants.zone3MaxRadius,
        const Color(0xFFFB923C),
      );
    }

    if (camDist <= GameConstants.safeZoneRadius + halfDiag + 140.0) {
      _drawSafeZoneStation(canvas);
    }

    if (camDist + halfDiag >= maxR - 160.0) {
      final double pulse = 0.65 + 0.35 * sin(gameRef.elapsedTime * 4.5);
      canvas.drawCircle(
        Offset.zero,
        maxR + 240.0,
        Paint()
          ..color = const Color(0x2AE11D48)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 480.0,
      );
      canvas.drawCircle(
        Offset.zero,
        maxR,
        Paint()
          ..color = const Color(0xFFFF2E4C).withOpacity(0.28 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 22.0,
      );
      canvas.drawCircle(
        Offset.zero,
        maxR,
        Paint()
          ..color = const Color(0xFFFF2E4C).withOpacity(0.86 + 0.14 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 4.5,
      );
    }

    // World-space Autopilot faint dashed trajectory & pulsing waypoint crosshair
    final Vector2? ap = gameRef.autopilotTarget;
    if (ap != null) {
      final Offset start = Offset(gameRef.player.position.x, gameRef.player.position.y);
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
        // Draw dashed segments within visible range around player/camera
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

  void _drawDashedZoneRing(Canvas canvas, double radius, Color color) {
    canvas.drawCircle(
      Offset.zero,
      radius,
      Paint()
        ..color = color.withOpacity(0.11)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 14.0,
    );
    final Paint dashPaint = Paint()
      ..color = color.withOpacity(0.58)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.6;
    const int segments = 120;
    const double step = (2.0 * pi) / segments;
    final Rect rect = Rect.fromCircle(center: Offset.zero, radius: radius);
    for (int i = 0; i < segments; i++) {
      canvas.drawArc(rect, i * step, step * 0.58, false, dashPaint);
    }
  }

  void _drawSafeZoneStation(Canvas canvas) {
    final double pulse = 0.7 + 0.3 * sin(gameRef.elapsedTime * 3.0);
    const double safeR = GameConstants.safeZoneRadius;

    canvas.drawCircle(
      Offset.zero,
      safeR,
      Paint()..color = const Color(0x1400E5FF),
    );
    canvas.drawCircle(
      Offset.zero,
      safeR,
      Paint()
        ..color = const Color(0xFF00E5FF).withOpacity(0.24 * pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 14.0,
    );
    canvas.drawCircle(
      Offset.zero,
      safeR,
      Paint()
        ..color = const Color(0xDD00E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3.0,
    );

    canvas.save();
    canvas.rotate(gameRef.elapsedTime * 0.16);
    canvas.drawCircle(
      Offset.zero,
      78.0,
      Paint()
        ..color = const Color(0xFF16243D)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 12.0,
    );
    canvas.drawCircle(
      Offset.zero,
      84.0,
      Paint()
        ..color = const Color(0xBB00E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );
    final Paint armPaint = Paint()
      ..color = const Color(0xFF334155)
      ..strokeWidth = 10.0
      ..strokeCap = StrokeCap.round;
    final Paint armAccent = Paint()
      ..color = const Color(0xCC00E5FF)
      ..strokeWidth = 2.0
      ..strokeCap = StrokeCap.round;
    for (int i = 0; i < 4; i++) {
      final double a = i * pi * 0.5;
      final Offset p1 = Offset(cos(a) * 34.0, sin(a) * 34.0);
      final Offset p2 = Offset(cos(a) * 106.0, sin(a) * 106.0);
      canvas.drawLine(p1, p2, armPaint);
      canvas.drawLine(p1, p2, armAccent);
      canvas.drawCircle(p2, 4.0, Paint()..color = const Color(0xFF00E5FF));
    }
    canvas.restore();

    final Path hub = Path();
    for (int i = 0; i < 8; i++) {
      final double a = i * (2.0 * pi / 8.0) + pi / 8.0;
      final double x = cos(a) * 42.0;
      final double y = sin(a) * 42.0;
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
        ..color = const Color(0xFF00E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2,
    );
    canvas.drawCircle(
      Offset.zero,
      12.0 * (0.9 + 0.1 * pulse),
      Paint()..color = const Color(0xFF00E5FF),
    );
    canvas.drawCircle(Offset.zero, 5.0, Paint()..color = Colors.white);
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
  bool isTakingZoneRadiation = false;
  String? requiredChipWarning;

  bool get inSafeZone => position.length <= GameConstants.safeZoneRadius;

  @override
  void update(double dt) {
    super.update(dt);

    if (rocketCooldown > 0.0) {
      rocketCooldown = max(0.0, rocketCooldown - dt);
    }

    final Vector2 move = gameRef.moveJoystick.relativeDelta;
    if (move.length > 0.12) {
      // Instant manual override cancels Autopilot
      if (gameRef.autopilotTarget != null) {
        gameRef.cancelAutopilot();
      }
      position.add(move.normalized() * (gameRef.currentSpeed * dt));
      angle = atan2(move.y, move.x);
    } else if (gameRef.autopilotTarget != null) {
      final Vector2 toAp = gameRef.autopilotTarget! - position;
      final double apDist = toAp.length;
      if (apDist <= GameConstants.autopilotStopRadius) {
        gameRef.cancelAutopilot();
      } else {
        final double desiredAngle = atan2(toAp.y, toAp.x);
        final double angleDiff = gameRef._normalizeAngle(desiredAngle - angle);
        angle = gameRef._normalizeAngle(
          angle + angleDiff * (1.0 - exp(-9.0 * dt)),
        );
        final double step = min(apDist, gameRef.currentSpeed * dt);
        position.add(toAp.normalized() * step);
      }
    }

    const double maxPlayerRadius =
        GameConstants.mapRadius - GameConstants.borderPadding;
    final double dist = position.length;
    if (dist > maxPlayerRadius && dist > 0.001) {
      position.scale(maxPlayerRadius / dist);
    }

    final SpaceZoneInfo zoneInfo = SpaceZoneInfo.of(gameRef.currentZone);
    final bool exposedToRad =
        !inSafeZone && zoneInfo.requiredChipTier > gameRef.equippedChipTier;
    isTakingZoneRadiation = exposedToRad;
    requiredChipWarning = exposedToRad ? zoneInfo.requiredChipName : null;

    if (exposedToRad) {
      radiationTickTimer += dt;
      if (radiationTickTimer >= GameConstants.radiationTickInterval) {
        radiationTickTimer -= GameConstants.radiationTickInterval;
        takeDamage(
          zoneInfo.radiationDps * GameConstants.radiationTickInterval,
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
      shieldHitFlash = max(0.0, shieldHitFlash - dt * 4.5);
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
          color: gameRef.activeShipSpec.accentColor,
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
    }
    gameRef.spawnFloatingText(
      position: position +
          Vector2(
            (gameRef.rng.nextDouble() - 0.5) * 16.0,
            -gameRef.activeShipSpec.hullRadius - 28.0,
          ),
      text: isRadiation ? '-${amount.round()} RAD' : '-${amount.round()}',
      color: isRadiation
          ? const Color(0xFFFF2E4C)
          : (hitShieldOnly ? const Color(0xFF38BDF8) : const Color(0xFFFF4D6D)),
      fontSize: 13.5,
    );
    if (hp <= 0.0) {
      hp = gameRef.currentMaxHull;
      shield = gameRef.currentMaxShield;
      position.setZero();
      gameRef.autoFireEnabled = false;
      gameRef.cancelAutopilot();
      gameRef._snapVantsToFormation();
      gameRef.saveGame();
    }
  }

  @override
  void render(Canvas canvas) {
    final Offset center = Offset(size.x * 0.5, size.y * 0.5);
    final ShipClassSpec spec = gameRef.activeShipSpec;
    final double hullR = spec.hullRadius;

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

    if (shieldHitFlash > 0.05) {
      canvas.drawCircle(
        center,
        hullR + 10.0,
        Paint()
          ..color = const Color(0xFF38BDF8).withOpacity(0.75 * shieldHitFlash)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.2,
      );
    }

    // Draw distinct silhouette for the active Ship Class
    _drawShipClassSilhouette(canvas, center, spec);

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
    canvas.restore();
  }

  void _drawShipClassSilhouette(
    Canvas canvas,
    Offset c,
    ShipClassSpec spec,
  ) {
    final bool moving = gameRef.moveJoystick.relativeDelta.length > 0.12 ||
        gameRef.autopilotTarget != null;
    final Paint fillPaint = Paint()..color = spec.primaryColor;
    final Paint trimPaint = Paint()
      ..color = spec.accentColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0;

    switch (spec.shipClass) {
      case ShipClass.interceptor:
        if (moving) {
          _drawFlame(canvas, Offset(c.dx - 13.0, c.dy), 17.0, 5.0,
              const Color(0xFF38BDF8));
        }
        final Path hull = Path()
          ..moveTo(c.dx + 23.0, c.dy)
          ..lineTo(c.dx + 4.0, c.dy - 8.0)
          ..lineTo(c.dx - 11.0, c.dy - 19.0)
          ..lineTo(c.dx - 16.0, c.dy - 11.0)
          ..lineTo(c.dx - 11.0, c.dy)
          ..lineTo(c.dx - 16.0, c.dy + 11.0)
          ..lineTo(c.dx - 11.0, c.dy + 19.0)
          ..lineTo(c.dx + 4.0, c.dy + 8.0)
          ..close();
        canvas.drawPath(hull, fillPaint);
        canvas.drawPath(hull, trimPaint);
        canvas.drawCircle(
          Offset(c.dx + 5.0, c.dy),
          3.2,
          Paint()..color = spec.accentColor,
        );
      case ShipClass.battlecruiser:
        if (moving) {
          _drawFlame(canvas, Offset(c.dx - 17.0, c.dy - 8.0), 15.0, 4.4,
              const Color(0xFFFBBF24));
          _drawFlame(canvas, Offset(c.dx - 17.0, c.dy + 8.0), 15.0, 4.4,
              const Color(0xFFFBBF24));
        }
        final Path hull = Path()
          ..moveTo(c.dx + 26.0, c.dy - 5.0)
          ..lineTo(c.dx + 14.0, c.dy - 11.0)
          ..lineTo(c.dx + 4.0, c.dy - 22.0)
          ..lineTo(c.dx - 14.0, c.dy - 20.0)
          ..lineTo(c.dx - 19.0, c.dy - 10.0)
          ..lineTo(c.dx - 19.0, c.dy + 10.0)
          ..lineTo(c.dx - 14.0, c.dy + 20.0)
          ..lineTo(c.dx + 4.0, c.dy + 22.0)
          ..lineTo(c.dx + 14.0, c.dy + 11.0)
          ..lineTo(c.dx + 26.0, c.dy + 5.0)
          ..lineTo(c.dx + 18.0, c.dy)
          ..close();
        canvas.drawPath(hull, fillPaint);
        canvas.drawPath(hull, trimPaint);
        canvas.drawCircle(
          Offset(c.dx + 4.0, c.dy),
          4.5,
          Paint()..color = spec.accentColor,
        );
      case ShipClass.dreadnought:
        if (moving) {
          _drawFlame(canvas, Offset(c.dx - 23.0, c.dy - 11.0), 13.0, 4.0,
              const Color(0xFF34D399));
          _drawFlame(canvas, Offset(c.dx - 25.0, c.dy), 16.0, 5.5,
              const Color(0xFF38BDF8));
          _drawFlame(canvas, Offset(c.dx - 23.0, c.dy + 11.0), 13.0, 4.0,
              const Color(0xFF34D399));
        }
        final Path hull = Path()
          ..moveTo(c.dx + 29.0, c.dy - 7.0)
          ..lineTo(c.dx + 16.0, c.dy - 14.0)
          ..lineTo(c.dx + 10.0, c.dy - 26.0)
          ..lineTo(c.dx - 14.0, c.dy - 27.0)
          ..lineTo(c.dx - 24.0, c.dy - 16.0)
          ..lineTo(c.dx - 24.0, c.dy + 16.0)
          ..lineTo(c.dx - 14.0, c.dy + 27.0)
          ..lineTo(c.dx + 10.0, c.dy + 26.0)
          ..lineTo(c.dx + 16.0, c.dy + 14.0)
          ..lineTo(c.dx + 29.0, c.dy + 7.0)
          ..lineTo(c.dx + 21.0, c.dy)
          ..close();
        canvas.drawPath(hull, fillPaint);
        canvas.drawPath(
          hull,
          Paint()
            ..color = spec.accentColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = 2.5,
        );
        canvas.drawCircle(
          Offset(c.dx + 2.0, c.dy),
          5.5,
          Paint()..color = spec.accentColor,
        );
        canvas.drawCircle(
          Offset(c.dx + 2.0, c.dy),
          2.2,
          Paint()..color = Colors.white,
        );
    }
  }

  void _drawFlame(
    Canvas canvas,
    Offset base,
    double len,
    double halfW,
    Color color,
  ) {
    final Path p = Path()
      ..moveTo(base.dx, base.dy - halfW)
      ..lineTo(base.dx - len, base.dy)
      ..lineTo(base.dx, base.dy + halfW)
      ..close();
    canvas.drawPath(p, Paint()..color = color.withOpacity(0.85));
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
    final List<VantDrone> vants = gameRef.ownedVants;
    if (vants.isEmpty) return;

    final double pulse = 0.75 + 0.25 * sin(gameRef.elapsedTime * 6.5);
    final Color formAccent = gameRef.activeFormationSpec.accentColor;

    // Subtle formation ring guide for RING and WHEEL formations
    if (gameRef.activeFormation == DroneFormation.ring ||
        gameRef.activeFormation == DroneFormation.wheel) {
      final double baseR = gameRef.activeShipSpec.hullRadius + 28.0;
      final double r = gameRef.activeFormation == DroneFormation.ring
          ? baseR * 1.08
          : baseR * 1.32;
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

    final double playerDistFromOrigin = gameRef.player.position.length;
    final bool playerInRing = playerDistFromOrigin >= spec.minRadius - 120.0 &&
        playerDistFromOrigin <= spec.maxRadius + 120.0;

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

    final double innerR = spec.minRadius + effectiveRadius + 26.0;
    final double outerR = spec.maxRadius - effectiveRadius - 26.0;
    final double d = position.length;
    if (d < innerR && d > 0.001) {
      position.scale(innerR / d);
    } else if (d > outerR && d > 0.001) {
      position.scale(outerR / d);
    }
  }

  Vector2 _pickZoneWaypoint() {
    return gameRef.randomPointInAnnulus(
      spec.minRadius + effectiveRadius + 50.0,
      spec.maxRadius - effectiveRadius - 50.0,
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
    if (life <= 0.0 || position.length > GameConstants.mapRadius + 100.0) {
      removeFromParent();
      return;
    }

    if (fromPlayer && homingTarget != null && !homingTarget!.isRemoved) {
      final Vector2 dir = (homingTarget!.position - position).normalized();
      velocity.setFrom(dir * GameConstants.playerLaserSpeed);
      angle = atan2(dir.y, dir.x);
    }

    position.add(velocity * dt);

    if (!fromPlayer && position.length <= GameConstants.safeZoneRadius) {
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
        gameRef.activeShipSpec.hullRadius + 6.0) {
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
    if (life <= 0.0 || position.length > GameConstants.mapRadius + 100.0) {
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
            (gameRef.activeShipSpec.hullRadius - 20.0)) {
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
  FormationButtonComponent() : super(size: Vector2(68.0, 38.0));

  @override
  void onGameResize(Vector2 size) {
    super.onGameResize(size);
    position = Vector2(436.0, 14.0);
  }

  @override
  void onTapDown(TapDownEvent event) {
    gameRef.cycleOwnedFormation();
  }

  @override
  void render(Canvas canvas) {
    final DroneFormationSpec spec = gameRef.activeFormationSpec;
    final double cd = gameRef.formationSwitchCooldown;
    final bool onCooldown = cd > 0.05;
    final Color accent =
        onCooldown ? const Color(0xFF64748B) : spec.accentColor;
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
        fontSize: 10.8,
        fontWeight: FontWeight.bold,
      ),
    ).render(
      canvas,
      spec.shortCode,
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
          : (gameRef.ownedFormations.length > 1 ? 'FMN' : '1/6'),
      Vector2(size.x * 0.5, 27.0),
      anchor: Anchor.center,
    );
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
    crPaint.render(
      canvas,
      '${gameRef.credits} CR  •  KILLS ${gameRef.kills}',
      Vector2(10.0, 6.0),
    );
    lvPaint.render(
      canvas,
      'LV ${gameRef.player.level}  •  ${gameRef.activeShipSpec.displayName.toUpperCase()}',
      Vector2(10.0, 22.0),
    );

    // Mini Shield & Hull bars on right half of panel
    final double shdRatio =
        (gameRef.player.shield / max(1.0, gameRef.currentMaxShield))
            .clamp(0.0, 1.0);
    final double hpRatio =
        (gameRef.player.hp / max(1.0, gameRef.currentMaxHull)).clamp(0.0, 1.0);
    canvas.drawRect(
      const Rect.fromLTWH(218.0, 10.0, 110.0, 6.0),
      Paint()..color = const Color(0xFF060C18),
    );
    canvas.drawRect(
      Rect.fromLTWH(218.0, 10.0, 110.0 * shdRatio, 6.0),
      Paint()..color = const Color(0xFF38BDF8),
    );
    canvas.drawRect(
      const Rect.fromLTWH(218.0, 22.0, 110.0, 6.0),
      Paint()..color = const Color(0xFF060C18),
    );
    canvas.drawRect(
      Rect.fromLTWH(218.0, 22.0, 110.0 * hpRatio, 6.0),
      Paint()..color = const Color(0xFF22C55E),
    );

    // Zone Indicator + Radiation Warning row
    final SpaceZoneInfo zoneInfo = SpaceZoneInfo.of(gameRef.currentZone);
    final RRect zoneChip = RRect.fromRectAndRadius(
      const Rect.fromLTWH(0.0, 45.0, 132.0, 22.0),
      const Radius.circular(6.0),
    );
    canvas.drawRRect(zoneChip, Paint()..color = zoneInfo.bgColor);
    canvas.drawRRect(
      zoneChip,
      Paint()
        ..color = zoneInfo.color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.2,
    );
    TextPaint(
      style: TextStyle(
        color: zoneInfo.color,
        fontSize: 10.0,
        fontWeight: FontWeight.bold,
      ),
    ).render(canvas, zoneInfo.label, Vector2(8.0, 50.0));

    if (gameRef.player.isTakingZoneRadiation &&
        gameRef.player.requiredChipWarning != null) {
      final RRect radChip = RRect.fromRectAndRadius(
        const Rect.fromLTWH(138.0, 45.0, 202.0, 22.0),
        const Radius.circular(6.0),
      );
      canvas.drawRRect(radChip, Paint()..color = const Color(0xDD7F1D1D));
      canvas.drawRRect(
        radChip,
        Paint()
          ..color = const Color(0xFFFF2E4C)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.2,
      );
      TextPaint(
        style: const TextStyle(
          color: Colors.white,
          fontSize: 9.8,
          fontWeight: FontWeight.bold,
        ),
      ).render(
        canvas,
        'RADIATION • NEED ${gameRef.player.requiredChipWarning}',
        Vector2(146.0, 50.0),
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
    final double scale = (radarR - 4.0) / GameConstants.mapRadius;
    final double pulse = 0.55 + 0.45 * sin(gameRef.elapsedTime * 6.5);

    canvas.drawCircle(center, radarR, Paint()..color = const Color(0xCC0B1324));
    canvas.drawCircle(
      center,
      GameConstants.zone1MaxRadius * scale,
      Paint()
        ..color = const Color(0x664ADE80)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.0,
    );
    canvas.drawCircle(
      center,
      GameConstants.zone2MaxRadius * scale,
      Paint()
        ..color = const Color(0x66FACC15)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.0,
    );
    canvas.drawCircle(
      center,
      GameConstants.zone3MaxRadius * scale,
      Paint()
        ..color = const Color(0x66FB923C)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.0,
    );
    canvas.drawCircle(
      center,
      radarR - 1.0,
      Paint()
        ..color = const Color(0xBBFF2E4C)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );

    canvas.drawCircle(
      center,
      max(4.5, GameConstants.safeZoneRadius * scale),
      Paint()..color = const Color(0x5500E5FF),
    );
    canvas.drawCircle(center, 2.3, Paint()..color = const Color(0xFF00E5FF));

    for (final LootBoxComponent box in gameRef.lootBoxes) {
      canvas.drawCircle(
        Offset(
          center.dx + box.position.x * scale,
          center.dy + box.position.y * scale,
        ),
        box.kind == LootBoxKind.cargoBox ? 1.8 : 1.2,
        Paint()
          ..color = box.kind == LootBoxKind.cargoBox
              ? const Color(0xFFFBBF24)
              : const Color(0x99FACC15),
      );
    }

    for (final AlienShipComponent alien in gameRef.aliens) {
      final Offset aPos = Offset(
        center.dx + alien.position.x * scale,
        center.dy + alien.position.y * scale,
      );
      if (alien.isBoss) {
        canvas.drawCircle(
          aPos,
          alien.spec.minimapDotRadius + 2.5 + 1.3 * pulse,
          Paint()
            ..color = const Color(0xFFFBBF24).withOpacity(0.28 + 0.35 * pulse),
        );
        canvas.drawCircle(
          aPos,
          alien.spec.minimapDotRadius + 1.2,
          Paint()..color = const Color(0xFFFF2E4C),
        );
        canvas.drawCircle(
          aPos,
          alien.spec.minimapDotRadius * 0.65,
          Paint()..color = const Color(0xFFFBBF24),
        );
      } else {
        canvas.drawCircle(
          aPos,
          alien.spec.minimapDotRadius,
          Paint()..color = alien.spec.primaryColor,
        );
      }
    }

    final Offset p = Offset(
      center.dx + gameRef.player.position.x * scale,
      center.dy + gameRef.player.position.y * scale,
    );

    // Active Autopilot dashed trajectory & crosshair on small minimap
    final Vector2? ap = gameRef.autopilotTarget;
    if (ap != null) {
      final Offset t = Offset(
        center.dx + ap.x * scale,
        center.dy + ap.y * scale,
      );
      final double dx = t.dx - p.dx;
      final double dy = t.dy - p.dy;
      final double dist = sqrt(dx * dx + dy * dy);
      if (dist > 2.0) {
        final double ux = dx / dist;
        final double uy = dy / dist;
        final Paint dash = Paint()
          ..color = const Color(0xAA00E5FF)
          ..strokeWidth = 1.2;
        double d = 0.0;
        while (d < dist) {
          final double dEnd = min(dist, d + 4.0);
          canvas.drawLine(
            Offset(p.dx + ux * d, p.dy + uy * d),
            Offset(p.dx + ux * dEnd, p.dy + uy * dEnd),
            dash,
          );
          d += 8.0;
        }
      }
      canvas.drawCircle(
        t,
        4.0,
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.5 + 0.45 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.2,
      );
      canvas.drawCircle(t, 1.7, Paint()..color = const Color(0xFF00E5FF));
    }

    canvas.drawCircle(
      p,
      3.8,
      Paint()..color = gameRef.activeShipSpec.accentColor,
    );
  }
}
