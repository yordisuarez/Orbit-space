import 'dart:math';
import 'dart:ui';

class GameConstants {
  static const double mapRadius = 20000.0;
  static const double safeZoneRadius = 600.0;
  static const double zone1MaxRadius = 5000.0;
  static const double zone2MaxRadius = 10000.0;
  static const double zone3MaxRadius = 15000.0;
  static const double zone4MaxRadius = 20000.0;

  static const double borderPadding = 30.0;
  static const double radiationTickInterval = 0.65;

  static const double playerHpPerLevel = 15.0;
  static const double playerBaseMaxShield = 40.0;
  static const double playerBaseLaserDamage = 6.0;

  static const int maxVants = 8;
  static const int maxOwnedVantsForFusion = 9;
  static const int flaxVantCost = 600;
  static const double formationSwitchCooldownSec = 3.0;
  static const double diamondShieldRegenPercentPerSec = 0.015;

  static const double sectorBaseAlienHp = 120.0;
  static const double sectorBaseAlienDamage = 10.0;
  static const double sectorBaseAlienCredits = 100.0;
  static const double sectorBaseAlienXp = 25.0;
  static const double sectorScalingBase = 2.5;

  static const double bossSpawnChance = 0.20;
  static const double bossHpMultiplier = 4.0;
  static const double bossDamageMultiplier = 2.0;
  static const int bossRewardMultiplier = 4;
  static const double bossSizeMultiplier = 1.5;

  static const double autopilotStopRadius = 60.0;
  static const double autopilotMaxTargetRadius = 19800.0;
  static const double hudMinimapViewRadius = 4500.0;

  // Jump Gate Portal & Classic Square Map (MAP X-1)
  static const double mainJumpGateX = 220.0;
  static const double mainJumpGateY = 0.0;
  static const double mapX1Size = 14000.0;
  static const double mapX1SafeZoneX = 1200.0;
  static const double mapX1SafeZoneY = 1200.0;
  static const double mapX1SafeZoneRadius = 500.0;
  static const double mapX1SpawnX = 1200.0;
  static const double mapX1SpawnY = 1350.0;
  static const double mapX1AutopilotMin = 200.0;
  static const double mapX1AutopilotMax = 13800.0;
  static const double mapX1RadiationOuterBuffer = 360.0;
  static const double mapX1BorderRadiationDps = 58.0;
  static const int mapX1StarCount = 1500;
  static const int mapX1BonusBoxCount = 50;
  static const double jumpGateInteractRadius = 120.0;
  static const double warpFlashDurationSec = 0.4;

  static const double playerAttackRange = 520.0;
  static const double playerFireInterval = 0.24;
  static const double playerLaserSpeed = 960.0;

  static const double rocketCooldownSec = 3.2;
  static const double rocketSpeed = 560.0;
  static const double rocketTurnRate = 7.5;
  static const double rocketLifetimeSec = 2.6;

  static const double radarConeRange = 520.0;
  static const double radarConeHalfAngleRad = 28.0 * pi / 180.0;

  static const double shieldRegenDelay = 4.0;
  static const double shieldRegenRate = 26.0;
  static const double safeZoneHpRegenRate = 64.0;
  static const double safeZoneShieldRegenRate = 130.0;

  static const int bonusBoxCount = 120;
  static const double lootCollectRadius = 42.0;
  static const double cargoLifetimeSec = 50.0;

  static const int starCount = 3000;

  static int xpRequiredForLevel(int level) {
    final int lv = level < 1 ? 1 : level;
    return 180 * lv + 85 * (lv - 1) * (lv - 1);
  }
}

enum ShipClass {
  interceptor,
  battlecruiser,
  dreadnought,
}

class ShipClassSpec {
  const ShipClassSpec({
    required this.shipClass,
    required this.displayName,
    required this.roleLabel,
    required this.cost,
    required this.speed,
    required this.baseHull,
    required this.laserSlotCount,
    required this.shieldSlotCount,
    required this.hullRadius,
    required this.primaryColor,
    required this.accentColor,
  });

  final ShipClass shipClass;
  final String displayName;
  final String roleLabel;
  final int cost;
  final double speed;
  final double baseHull;
  final int laserSlotCount;
  final int shieldSlotCount;
  final double hullRadius;
  final Color primaryColor;
  final Color accentColor;
}

class ShipClassCatalog {
  static const Map<ShipClass, ShipClassSpec> specs = <ShipClass, ShipClassSpec>{
    ShipClass.interceptor: ShipClassSpec(
      shipClass: ShipClass.interceptor,
      displayName: 'Interceptor',
      roleLabel: 'STARTER • FAST',
      cost: 0,
      speed: 340.0,
      baseHull: 140.0,
      laserSlotCount: 4,
      shieldSlotCount: 3,
      hullRadius: 20.0,
      primaryColor: Color(0xFF1E293B),
      accentColor: Color(0xFF00E5FF),
    ),
    ShipClass.battlecruiser: ShipClassSpec(
      shipClass: ShipClass.battlecruiser,
      displayName: 'Battlecruiser',
      roleLabel: 'BALANCED',
      cost: 1500,
      speed: 265.0,
      baseHull: 260.0,
      laserSlotCount: 6,
      shieldSlotCount: 6,
      hullRadius: 24.0,
      primaryColor: Color(0xFF172554),
      accentColor: Color(0xFFFBBF24),
    ),
    ShipClass.dreadnought: ShipClassSpec(
      shipClass: ShipClass.dreadnought,
      displayName: 'Dreadnought',
      roleLabel: 'HEAVY TANK',
      cost: 4000,
      speed: 205.0,
      baseHull: 480.0,
      laserSlotCount: 8,
      shieldSlotCount: 10,
      hullRadius: 29.0,
      primaryColor: Color(0xFF064E3B),
      accentColor: Color(0xFF34D399),
    ),
  };

  static ShipClassSpec of(ShipClass shipClass) => specs[shipClass]!;
}

enum GameMap {
  mainMap,
  mapX1,
}

extension GameMapMeta on GameMap {
  String get displayName => this == GameMap.mapX1 ? 'MAP X-1' : 'MAIN MAP';
  String get gateLabel => this == GameMap.mapX1 ? 'GATE: BASE' : 'GATE: X-1';
}

enum SpaceZone {
  safeZone,
  zone1Alpha,
  zone2Beta,
  zone3Gamma,
  zone4Omega,
}

class SpaceZoneInfo {
  const SpaceZoneInfo({
    required this.label,
    required this.color,
    required this.bgColor,
    required this.requiredChipTier,
    required this.radiationDps,
    this.requiredChipName,
  });

  final String label;
  final Color color;
  final Color bgColor;
  final int requiredChipTier;
  final double radiationDps;
  final String? requiredChipName;

  static SpaceZoneInfo of(SpaceZone zone) {
    switch (zone) {
      case SpaceZone.safeZone:
        return const SpaceZoneInfo(
          label: 'SAFE ZONE',
          color: Color(0xFF00E5FF),
          bgColor: Color(0xCC062838),
          requiredChipTier: 0,
          radiationDps: 0.0,
        );
      case SpaceZone.zone1Alpha:
        return const SpaceZoneInfo(
          label: 'ZONE 1: ALPHA',
          color: Color(0xFF4ADE80),
          bgColor: Color(0xCC072415),
          requiredChipTier: 0,
          radiationDps: 0.0,
        );
      case SpaceZone.zone2Beta:
        return const SpaceZoneInfo(
          label: 'ZONE 2: BETA',
          color: Color(0xFFFACC15),
          bgColor: Color(0xCC2A2006),
          requiredChipTier: 1,
          radiationDps: 14.0,
          requiredChipName: 'CHIP LV 2',
        );
      case SpaceZone.zone3Gamma:
        return const SpaceZoneInfo(
          label: 'ZONE 3: GAMMA',
          color: Color(0xFFFB923C),
          bgColor: Color(0xCC2C1306),
          requiredChipTier: 2,
          radiationDps: 28.0,
          requiredChipName: 'CHIP LV 3',
        );
      case SpaceZone.zone4Omega:
        return const SpaceZoneInfo(
          label: 'ZONE 4: OMEGA',
          color: Color(0xFFC084FC),
          bgColor: Color(0xCC280A2E),
          requiredChipTier: 3,
          radiationDps: 52.0,
          requiredChipName: 'CHIP LV 4',
        );
    }
  }
}

class SectorAlienScaling {
  static double multiplierForSector(int sectorIndex) {
    final int exp = sectorIndex > 1 ? sectorIndex - 1 : 0;
    return pow(GameConstants.sectorScalingBase, exp).toDouble();
  }

  static double hpForSector(int sectorIndex) =>
      (GameConstants.sectorBaseAlienHp * multiplierForSector(sectorIndex))
          .roundToDouble();

  static double damageForSector(int sectorIndex) =>
      (GameConstants.sectorBaseAlienDamage * multiplierForSector(sectorIndex))
          .roundToDouble();

  static int creditsForSector(int sectorIndex) =>
      (GameConstants.sectorBaseAlienCredits * multiplierForSector(sectorIndex))
          .round();

  static int xpForSector(int sectorIndex) =>
      (GameConstants.sectorBaseAlienXp * multiplierForSector(sectorIndex))
          .round();
}

enum AlienTier {
  scout,
  hunter,
  destroyer,
  dreadnoughtBoss,
}

class AlienTierSpec {
  const AlienTierSpec({
    required this.sectorIndex,
    required this.tier,
    required this.callsign,
    required this.minRadius,
    required this.maxRadius,
    required this.activeCount,
    required this.radius,
    required this.patrolSpeed,
    required this.chaseSpeed,
    required this.attackRange,
    required this.fireInterval,
    required this.laserSpeed,
    required this.primaryColor,
    required this.accentColor,
    required this.minimapDotRadius,
  });

  final int sectorIndex;
  final AlienTier tier;
  final String callsign;
  final double minRadius;
  final double maxRadius;
  final int activeCount;
  final double radius;
  final double patrolSpeed;
  final double chaseSpeed;
  final double attackRange;
  final double fireInterval;
  final double laserSpeed;
  final Color primaryColor;
  final Color accentColor;
  final double minimapDotRadius;

  double get maxHp => SectorAlienScaling.hpForSector(sectorIndex);
  double get laserDamage => SectorAlienScaling.damageForSector(sectorIndex);
  int get rewardCredits => SectorAlienScaling.creditsForSector(sectorIndex);
  int get rewardXp => SectorAlienScaling.xpForSector(sectorIndex);
}

class AlienTierCatalog {
  static const Map<AlienTier, AlienTierSpec> specs = <AlienTier, AlienTierSpec>{
    AlienTier.scout: AlienTierSpec(
      sectorIndex: 1,
      tier: AlienTier.scout,
      callsign: 'SCOUT',
      minRadius: GameConstants.safeZoneRadius,
      maxRadius: GameConstants.zone1MaxRadius,
      activeCount: 22,
      radius: 17.0,
      patrolSpeed: 118.0,
      chaseSpeed: 190.0,
      attackRange: 360.0,
      fireInterval: 1.25,
      laserSpeed: 520.0,
      primaryColor: Color(0xFF4ADE80),
      accentColor: Color(0xFF84CC16),
      minimapDotRadius: 2.5,
    ),
    AlienTier.hunter: AlienTierSpec(
      sectorIndex: 2,
      tier: AlienTier.hunter,
      callsign: 'HUNTER',
      minRadius: GameConstants.zone1MaxRadius,
      maxRadius: GameConstants.zone2MaxRadius,
      activeCount: 24,
      radius: 22.0,
      patrolSpeed: 96.0,
      chaseSpeed: 160.0,
      attackRange: 400.0,
      fireInterval: 1.08,
      laserSpeed: 550.0,
      primaryColor: Color(0xFFFACC15),
      accentColor: Color(0xFFF59E0B),
      minimapDotRadius: 2.8,
    ),
    AlienTier.destroyer: AlienTierSpec(
      sectorIndex: 3,
      tier: AlienTier.destroyer,
      callsign: 'DESTROYER',
      minRadius: GameConstants.zone2MaxRadius,
      maxRadius: GameConstants.zone3MaxRadius,
      activeCount: 20,
      radius: 28.0,
      patrolSpeed: 80.0,
      chaseSpeed: 135.0,
      attackRange: 440.0,
      fireInterval: 0.96,
      laserSpeed: 580.0,
      primaryColor: Color(0xFFFB923C),
      accentColor: Color(0xFFEF4444),
      minimapDotRadius: 3.2,
    ),
    AlienTier.dreadnoughtBoss: AlienTierSpec(
      sectorIndex: 4,
      tier: AlienTier.dreadnoughtBoss,
      callsign: 'DREADNOUGHT',
      minRadius: GameConstants.zone3MaxRadius,
      maxRadius: GameConstants.zone4MaxRadius,
      activeCount: 14,
      radius: 40.0,
      patrolSpeed: 62.0,
      chaseSpeed: 110.0,
      attackRange: 500.0,
      fireInterval: 0.85,
      laserSpeed: 620.0,
      primaryColor: Color(0xFFC084FC),
      accentColor: Color(0xFFE11D48),
      minimapDotRadius: 4.1,
    ),
  };

  static AlienTierSpec of(AlienTier tier) => specs[tier]!;
}

enum VantTier {
  flax,
  iris,
}

class VantTierSpec {
  const VantTierSpec({
    required this.tier,
    required this.displayName,
    required this.shortCode,
    required this.slotCount,
    this.shopCost,
    required this.primaryColor,
    required this.accentColor,
  });

  final VantTier tier;
  final String displayName;
  final String shortCode;
  final int slotCount;
  final int? shopCost;
  final Color primaryColor;
  final Color accentColor;
}

class VantTierCatalog {
  static const Map<VantTier, VantTierSpec> specs = <VantTier, VantTierSpec>{
    VantTier.flax: VantTierSpec(
      tier: VantTier.flax,
      displayName: 'Flax VANT',
      shortCode: 'FLAX',
      slotCount: 1,
      shopCost: GameConstants.flaxVantCost,
      primaryColor: Color(0xFFCBD5E1),
      accentColor: Color(0xFF00E5FF),
    ),
    VantTier.iris: VantTierSpec(
      tier: VantTier.iris,
      displayName: 'Iris VANT',
      shortCode: 'IRIS',
      slotCount: 2,
      primaryColor: Color(0xFFFBBF24),
      accentColor: Color(0xFF38BDF8),
    ),
  };

  static VantTierSpec of(VantTier tier) => specs[tier]!;
}

class VantDrone {
  VantDrone({
    required this.id,
    required this.tier,
    List<EquipmentItem?>? config1Slots,
    List<EquipmentItem?>? config2Slots,
  })  : config1Slots = config1Slots ??
            List<EquipmentItem?>.filled(
              VantTierCatalog.of(tier).slotCount,
              null,
              growable: false,
            ),
        config2Slots = config2Slots ??
            List<EquipmentItem?>.filled(
              VantTierCatalog.of(tier).slotCount,
              null,
              growable: false,
            );

  final int id;
  final VantTier tier;
  final List<EquipmentItem?> config1Slots;
  final List<EquipmentItem?> config2Slots;

  // Smoothly interpolated world-space coordinates for Drone Formations
  double worldX = 0.0;
  double worldY = 0.0;
  double facingRad = 0.0;
  bool hasInitializedPosition = false;

  VantTierSpec get spec => VantTierCatalog.of(tier);

  List<EquipmentItem?> slotsForConfig(int configNumber) =>
      configNumber == 2 ? config2Slots : config1Slots;
}

enum DroneFormation {
  normal,
  drill,
  heart,
  diamond,
  ring,
  wheel,
}

class DroneFormationSlot {
  const DroneFormationSlot({
    required this.localX,
    required this.localY,
    required this.extraRotation,
  });

  final double localX;
  final double localY;
  final double extraRotation;
}

class DroneFormationSpec {
  const DroneFormationSpec({
    required this.formation,
    required this.displayName,
    required this.shortCode,
    required this.cost,
    required this.damageMult,
    required this.shieldMult,
    required this.hullMult,
    required this.speedMult,
    required this.combatShieldRegenPct,
    required this.accentColor,
    required this.bonusLabel,
    this.penaltyLabel,
  });

  final DroneFormation formation;
  final String displayName;
  final String shortCode;
  final int cost;
  final double damageMult;
  final double shieldMult;
  final double hullMult;
  final double speedMult;
  final double combatShieldRegenPct;
  final Color accentColor;
  final String bonusLabel;
  final String? penaltyLabel;
}

class DroneFormationCatalog {
  static const Map<DroneFormation, DroneFormationSpec> specs =
      <DroneFormation, DroneFormationSpec>{
    DroneFormation.normal: DroneFormationSpec(
      formation: DroneFormation.normal,
      displayName: 'NORMAL',
      shortCode: 'NORM',
      cost: 0,
      damageMult: 1.0,
      shieldMult: 1.0,
      hullMult: 1.0,
      speedMult: 1.0,
      combatShieldRegenPct: 0.0,
      accentColor: Color(0xFF94A3B8),
      bonusLabel: 'Balanced flanking formation',
    ),
    DroneFormation.drill: DroneFormationSpec(
      formation: DroneFormation.drill,
      displayName: 'DRILL',
      shortCode: 'DRIL',
      cost: 1000,
      damageMult: 1.20,
      shieldMult: 0.75,
      hullMult: 1.0,
      speedMult: 0.95,
      combatShieldRegenPct: 0.0,
      accentColor: Color(0xFFFF5252),
      bonusLabel: '+20% Laser DMG',
      penaltyLabel: '-25% Max SHD, -5% SPD',
    ),
    DroneFormation.heart: DroneFormationSpec(
      formation: DroneFormation.heart,
      displayName: 'HEART',
      shortCode: 'HART',
      cost: 1200,
      damageMult: 0.95,
      shieldMult: 1.20,
      hullMult: 1.20,
      speedMult: 1.0,
      combatShieldRegenPct: 0.0,
      accentColor: Color(0xFFF472B6),
      bonusLabel: '+20% Max Hull, +20% Max SHD',
      penaltyLabel: '-5% Laser DMG',
    ),
    DroneFormation.diamond: DroneFormationSpec(
      formation: DroneFormation.diamond,
      displayName: 'DIAMOND',
      shortCode: 'DIAM',
      cost: 1500,
      damageMult: 1.0,
      shieldMult: 1.0,
      hullMult: 0.70,
      speedMult: 1.0,
      combatShieldRegenPct: GameConstants.diamondShieldRegenPercentPerSec,
      accentColor: Color(0xFF38BDF8),
      bonusLabel: '+1.5% Max SHD/s Regen',
      penaltyLabel: '-30% Max Hull',
    ),
    DroneFormation.ring: DroneFormationSpec(
      formation: DroneFormation.ring,
      displayName: 'RING',
      shortCode: 'RING',
      cost: 1800,
      damageMult: 0.75,
      shieldMult: 1.85,
      hullMult: 1.0,
      speedMult: 0.95,
      combatShieldRegenPct: 0.0,
      accentColor: Color(0xFFFBBF24),
      bonusLabel: '+85% Max Shield',
      penaltyLabel: '-25% Laser DMG, -5% SPD',
    ),
    DroneFormation.wheel: DroneFormationSpec(
      formation: DroneFormation.wheel,
      displayName: 'WHEEL',
      shortCode: 'WHEL',
      cost: 1200,
      damageMult: 0.80,
      shieldMult: 1.0,
      hullMult: 1.0,
      speedMult: 1.15,
      combatShieldRegenPct: 0.0,
      accentColor: Color(0xFF34D399),
      bonusLabel: '+15% Speed',
      penaltyLabel: '-20% Laser DMG',
    ),
  };

  static DroneFormationSpec of(DroneFormation formation) => specs[formation]!;

  /// Computes ship-local offset (+X forward, +Y starboard) for drone [index] of [total].
  static DroneFormationSlot computeLocalSlot({
    required DroneFormation formation,
    required int index,
    required int total,
    required double shipHullRadius,
    required double elapsedTime,
  }) {
    final int safeTotal = max(1, total);
    final double baseR = shipHullRadius + 28.0;
    final double bob = sin(elapsedTime * 3.0 + index * 0.9) * 2.2;

    switch (formation) {
      case DroneFormation.normal:
        const List<List<double>> offsets = <List<double>>[
          <double>[-0.35, -1.10],
          <double>[-0.35, 1.10],
          <double>[-0.95, -1.45],
          <double>[-0.95, 1.45],
          <double>[-1.40, -0.78],
          <double>[-1.40, 0.78],
          <double>[-1.65, -0.26],
          <double>[-1.65, 0.26],
        ];
        final List<double> slot = offsets[index % offsets.length];
        return DroneFormationSlot(
          localX: slot[0] * baseR + bob,
          localY: slot[1] * baseR,
          extraRotation: 0.0,
        );

      case DroneFormation.drill:
        const List<List<double>> offsets = <List<double>>[
          <double>[1.65, 0.0],
          <double>[1.15, -0.52],
          <double>[1.15, 0.52],
          <double>[0.58, -1.00],
          <double>[0.58, 1.00],
          <double>[0.0, -1.46],
          <double>[0.0, 1.46],
          <double>[-0.65, 0.0],
        ];
        final List<double> slot = offsets[index % offsets.length];
        return DroneFormationSlot(
          localX: slot[0] * baseR + bob * 0.6,
          localY: slot[1] * baseR,
          extraRotation: 0.0,
        );

      case DroneFormation.heart:
        final double t = (index / safeTotal) * 2.0 * pi;
        final double hx = 16.0 * pow(sin(t), 3).toDouble();
        final double hy = 13.0 * cos(t) -
            5.0 * cos(2.0 * t) -
            2.0 * cos(3.0 * t) -
            cos(4.0 * t);
        final double scale = (baseR * 1.42) / 16.0;
        return DroneFormationSlot(
          localX: hy * scale + bob * 0.5,
          localY: hx * scale,
          extraRotation: 0.0,
        );

      case DroneFormation.diamond:
        final double p = (index / safeTotal) * 4.0;
        final double rad = baseR * 1.38;
        double lx;
        double ly;
        if (p < 1.0) {
          lx = rad * (1.0 - p);
          ly = rad * p;
        } else if (p < 2.0) {
          final double f = p - 1.0;
          lx = -rad * f;
          ly = rad * (1.0 - f);
        } else if (p < 3.0) {
          final double f = p - 2.0;
          lx = -rad * (1.0 - f);
          ly = -rad * f;
        } else {
          final double f = p - 3.0;
          lx = rad * f;
          ly = -rad * (1.0 - f);
        }
        return DroneFormationSlot(
          localX: lx,
          localY: ly,
          extraRotation: 0.0,
        );

      case DroneFormation.ring:
        final double r = baseR * 1.08;
        final double theta = elapsedTime * 1.7 + (index * (2.0 * pi / safeTotal));
        return DroneFormationSlot(
          localX: cos(theta) * r,
          localY: sin(theta) * r,
          extraRotation: theta + pi * 0.5,
        );

      case DroneFormation.wheel:
        // Two side-flank spinning circles: left (-Y) & right (+Y) anchors, opposite spin
        final int leftCount = (safeTotal + 1) ~/ 2;
        final int rightCount = safeTotal - leftCount;
        const double flankOffset = 70.0;
        const double wheelRadius = 35.0;
        const double spinSpeed = 3.2;
        if (index < leftCount) {
          final double theta =
              elapsedTime * spinSpeed + (index * (2.0 * pi / leftCount));
          return DroneFormationSlot(
            localX: cos(theta) * wheelRadius,
            localY: -flankOffset + sin(theta) * wheelRadius,
            extraRotation: theta + pi * 0.5,
          );
        } else {
          final int rightIdx = index - leftCount;
          final int safeRight = max(1, rightCount);
          final double theta =
              -elapsedTime * spinSpeed + (rightIdx * (2.0 * pi / safeRight));
          return DroneFormationSlot(
            localX: cos(theta) * wheelRadius,
            localY: flankOffset + sin(theta) * wheelRadius,
            extraRotation: theta - pi * 0.5,
          );
        }
    }
  }
}

enum EquipmentCategory {
  laser,
  shieldGenerator,
  sectorChip,
}

enum EquipmentItemId {
  lf1,
  lf2,
  lf3,
  lf4,
  sg1,
  sg2,
  sg3,
  sg4,
  chip1,
  chip2,
  chip3,
  chip4,
}

class EquipmentSpec {
  const EquipmentSpec({
    required this.id,
    required this.shortCode,
    required this.displayName,
    required this.category,
    required this.tierLevel,
    this.shopCost,
    this.damageBonus = 0.0,
    this.shieldBonus = 0.0,
    this.sectorTier = 0,
    required this.accentColor,
  });

  final EquipmentItemId id;
  final String shortCode;
  final String displayName;
  final EquipmentCategory category;
  final int tierLevel;
  final int? shopCost;
  final double damageBonus;
  final double shieldBonus;
  final int sectorTier;
  final Color accentColor;

  String get statSummary {
    switch (category) {
      case EquipmentCategory.laser:
        return '+${damageBonus.toInt()} DMG';
      case EquipmentCategory.shieldGenerator:
        return '+${shieldBonus.toInt()} SHD';
      case EquipmentCategory.sectorChip:
        if (sectorTier == 0) return 'BASE CHIP';
        if (sectorTier == 1) return 'SEC 2 SAFE';
        if (sectorTier == 2) return 'SEC 2-3 SAFE';
        return 'SEC 2-4 SAFE';
    }
  }
}

class EquipmentCatalog {
  static const Map<EquipmentItemId, EquipmentSpec> specs =
      <EquipmentItemId, EquipmentSpec>{
    EquipmentItemId.lf1: EquipmentSpec(
      id: EquipmentItemId.lf1,
      shortCode: 'LF-1',
      displayName: 'LF-1 Laser',
      category: EquipmentCategory.laser,
      tierLevel: 1,
      shopCost: 200,
      damageBonus: 12.0,
      accentColor: Color(0xFFFF6B35),
    ),
    EquipmentItemId.lf2: EquipmentSpec(
      id: EquipmentItemId.lf2,
      shortCode: 'LF-2',
      displayName: 'LF-2 Laser',
      category: EquipmentCategory.laser,
      tierLevel: 2,
      damageBonus: 28.0,
      accentColor: Color(0xFFFF5252),
    ),
    EquipmentItemId.lf3: EquipmentSpec(
      id: EquipmentItemId.lf3,
      shortCode: 'LF-3',
      displayName: 'LF-3 Laser',
      category: EquipmentCategory.laser,
      tierLevel: 3,
      damageBonus: 60.0,
      accentColor: Color(0xFFF43F5E),
    ),
    EquipmentItemId.lf4: EquipmentSpec(
      id: EquipmentItemId.lf4,
      shortCode: 'LF-4',
      displayName: 'LF-4 Laser',
      category: EquipmentCategory.laser,
      tierLevel: 4,
      damageBonus: 120.0,
      accentColor: Color(0xFFE879F9),
    ),
    EquipmentItemId.sg1: EquipmentSpec(
      id: EquipmentItemId.sg1,
      shortCode: 'SG-1',
      displayName: 'SG-1 Shield',
      category: EquipmentCategory.shieldGenerator,
      tierLevel: 1,
      shopCost: 250,
      shieldBonus: 80.0,
      accentColor: Color(0xFF38BDF8),
    ),
    EquipmentItemId.sg2: EquipmentSpec(
      id: EquipmentItemId.sg2,
      shortCode: 'SG-2',
      displayName: 'SG-2 Shield',
      category: EquipmentCategory.shieldGenerator,
      tierLevel: 2,
      shieldBonus: 180.0,
      accentColor: Color(0xFF0EA5E9),
    ),
    EquipmentItemId.sg3: EquipmentSpec(
      id: EquipmentItemId.sg3,
      shortCode: 'SG-3',
      displayName: 'SG-3 Shield',
      category: EquipmentCategory.shieldGenerator,
      tierLevel: 3,
      shieldBonus: 380.0,
      accentColor: Color(0xFF6366F1),
    ),
    EquipmentItemId.sg4: EquipmentSpec(
      id: EquipmentItemId.sg4,
      shortCode: 'SG-4',
      displayName: 'SG-4 Shield',
      category: EquipmentCategory.shieldGenerator,
      tierLevel: 4,
      shieldBonus: 750.0,
      accentColor: Color(0xFFA855F7),
    ),
    EquipmentItemId.chip1: EquipmentSpec(
      id: EquipmentItemId.chip1,
      shortCode: 'CH-1',
      displayName: 'Chip Lv 1',
      category: EquipmentCategory.sectorChip,
      tierLevel: 1,
      sectorTier: 0,
      accentColor: Color(0xFF94A3B8),
    ),
    EquipmentItemId.chip2: EquipmentSpec(
      id: EquipmentItemId.chip2,
      shortCode: 'CH-2',
      displayName: 'Chip Lv 2',
      category: EquipmentCategory.sectorChip,
      tierLevel: 2,
      sectorTier: 1,
      accentColor: Color(0xFFFACC15),
    ),
    EquipmentItemId.chip3: EquipmentSpec(
      id: EquipmentItemId.chip3,
      shortCode: 'CH-3',
      displayName: 'Chip Lv 3',
      category: EquipmentCategory.sectorChip,
      tierLevel: 3,
      sectorTier: 2,
      accentColor: Color(0xFFFB923C),
    ),
    EquipmentItemId.chip4: EquipmentSpec(
      id: EquipmentItemId.chip4,
      shortCode: 'CH-4',
      displayName: 'Chip Lv 4',
      category: EquipmentCategory.sectorChip,
      tierLevel: 4,
      sectorTier: 3,
      accentColor: Color(0xFFC084FC),
    ),
  };

  static EquipmentSpec of(EquipmentItemId id) => specs[id]!;

  static EquipmentItemId? nextFusionResult(EquipmentItemId id) {
    switch (id) {
      case EquipmentItemId.lf1:
        return EquipmentItemId.lf2;
      case EquipmentItemId.lf2:
        return EquipmentItemId.lf3;
      case EquipmentItemId.lf3:
        return EquipmentItemId.lf4;
      case EquipmentItemId.sg1:
        return EquipmentItemId.sg2;
      case EquipmentItemId.sg2:
        return EquipmentItemId.sg3;
      case EquipmentItemId.sg3:
        return EquipmentItemId.sg4;
      case EquipmentItemId.chip1:
        return EquipmentItemId.chip2;
      case EquipmentItemId.chip2:
        return EquipmentItemId.chip3;
      case EquipmentItemId.chip3:
        return EquipmentItemId.chip4;
      case EquipmentItemId.lf4:
      case EquipmentItemId.sg4:
      case EquipmentItemId.chip4:
        return null;
    }
  }
}

class FusionRecipe {
  const FusionRecipe({
    required this.source,
    required this.result,
  });

  final EquipmentItemId source;
  final EquipmentItemId result;
}

class FusionCatalog {
  static const List<FusionRecipe> recipes = <FusionRecipe>[
    FusionRecipe(source: EquipmentItemId.lf1, result: EquipmentItemId.lf2),
    FusionRecipe(source: EquipmentItemId.lf2, result: EquipmentItemId.lf3),
    FusionRecipe(source: EquipmentItemId.lf3, result: EquipmentItemId.lf4),
    FusionRecipe(source: EquipmentItemId.sg1, result: EquipmentItemId.sg2),
    FusionRecipe(source: EquipmentItemId.sg2, result: EquipmentItemId.sg3),
    FusionRecipe(source: EquipmentItemId.sg3, result: EquipmentItemId.sg4),
    FusionRecipe(source: EquipmentItemId.chip1, result: EquipmentItemId.chip2),
    FusionRecipe(source: EquipmentItemId.chip2, result: EquipmentItemId.chip3),
    FusionRecipe(source: EquipmentItemId.chip3, result: EquipmentItemId.chip4),
  ];
}

enum MissionGoalType {
  collectBonusBoxes,
  destroyScouts,
  destroyHunters,
  destroyDestroyers,
}

class MissionSpec {
  const MissionSpec({
    required this.index,
    required this.code,
    required this.sectorChainLabel,
    required this.description,
    required this.goalType,
    required this.targetCount,
    required this.requiredLevel,
    required this.isSectorFinal,
    required this.rewardCredits,
    required this.rewardXp,
    this.rewardChip,
  });

  final int index;
  final String code;
  final String sectorChainLabel;
  final String description;
  final MissionGoalType goalType;
  final int targetCount;
  final int requiredLevel;
  final bool isSectorFinal;
  final int rewardCredits;
  final int rewardXp;
  final EquipmentItemId? rewardChip;

  String get rewardSummary {
    final List<String> parts = <String>[];
    if (rewardChip != null) {
      parts.add('1x ${EquipmentCatalog.of(rewardChip!).displayName}');
    }
    if (rewardCredits > 0) parts.add('$rewardCredits CR');
    if (rewardXp > 0) parts.add('$rewardXp XP');
    return parts.join(' + ');
  }
}

class MissionCatalog {
  static const List<MissionSpec> all = <MissionSpec>[
    MissionSpec(
      index: 0,
      code: 'M1',
      sectorChainLabel: 'SECTOR 1 CHAIN',
      description: 'Collect 3 Bonus Boxes',
      goalType: MissionGoalType.collectBonusBoxes,
      targetCount: 3,
      requiredLevel: 1,
      isSectorFinal: false,
      rewardCredits: 150,
      rewardXp: 50,
    ),
    MissionSpec(
      index: 1,
      code: 'M2',
      sectorChainLabel: 'SECTOR 1 CHAIN',
      description: 'Destroy 5 Scouts',
      goalType: MissionGoalType.destroyScouts,
      targetCount: 5,
      requiredLevel: 1,
      isSectorFinal: false,
      rewardCredits: 250,
      rewardXp: 100,
    ),
    MissionSpec(
      index: 2,
      code: 'M3',
      sectorChainLabel: 'SECTOR 1 FINAL',
      description: 'Destroy 8 Scouts',
      goalType: MissionGoalType.destroyScouts,
      targetCount: 8,
      requiredLevel: 2,
      isSectorFinal: true,
      rewardCredits: 300,
      rewardXp: 0,
      rewardChip: EquipmentItemId.chip1,
    ),
    MissionSpec(
      index: 3,
      code: 'M4',
      sectorChainLabel: 'SECTOR 2 CHAIN',
      description: 'Collect 5 Bonus Boxes',
      goalType: MissionGoalType.collectBonusBoxes,
      targetCount: 5,
      requiredLevel: 1,
      isSectorFinal: false,
      rewardCredits: 400,
      rewardXp: 150,
    ),
    MissionSpec(
      index: 4,
      code: 'M5',
      sectorChainLabel: 'SECTOR 2 CHAIN',
      description: 'Destroy 6 Hunters',
      goalType: MissionGoalType.destroyHunters,
      targetCount: 6,
      requiredLevel: 1,
      isSectorFinal: false,
      rewardCredits: 600,
      rewardXp: 250,
    ),
    MissionSpec(
      index: 5,
      code: 'M6',
      sectorChainLabel: 'SECTOR 2 FINAL',
      description: 'Destroy 10 Hunters',
      goalType: MissionGoalType.destroyHunters,
      targetCount: 10,
      requiredLevel: 5,
      isSectorFinal: true,
      rewardCredits: 800,
      rewardXp: 0,
      rewardChip: EquipmentItemId.chip2,
    ),
    MissionSpec(
      index: 6,
      code: 'M7',
      sectorChainLabel: 'SECTOR 3 CHAIN',
      description: 'Destroy 6 Destroyers',
      goalType: MissionGoalType.destroyDestroyers,
      targetCount: 6,
      requiredLevel: 1,
      isSectorFinal: false,
      rewardCredits: 1200,
      rewardXp: 400,
    ),
    MissionSpec(
      index: 7,
      code: 'M8',
      sectorChainLabel: 'SECTOR 3 FINAL',
      description: 'Destroy 10 Destroyers',
      goalType: MissionGoalType.destroyDestroyers,
      targetCount: 10,
      requiredLevel: 8,
      isSectorFinal: true,
      rewardCredits: 1500,
      rewardXp: 0,
      rewardChip: EquipmentItemId.chip3,
    ),
  ];
}

class EquipmentItem {
  const EquipmentItem({
    required this.instanceId,
    required this.id,
  });

  final int instanceId;
  final EquipmentItemId id;

  EquipmentSpec get spec => EquipmentCatalog.of(id);
}

enum LootBoxKind {
  bonusBox,
  cargoBox,
}
