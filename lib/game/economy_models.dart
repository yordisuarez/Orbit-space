import 'dart:math';
import 'package:flutter/material.dart';

import 'game_models.dart';

// =============================================================================
// PILOT SKILLS (9 PROFESSIONS, LV 1-99), SHIP ROLES, RESOURCES & CRAFTING
// =============================================================================

const int kPilotSkillMinLevel = 1;
const int kPilotSkillMaxLevel = 99;
const double kCameraZoomMin = 0.6;
const double kCameraZoomMax = 2.5;
const double kCameraZoomDefault = 1.0;
const double kShipDetailLodThreshold = 1.35;

enum ShipRole {
  capsule,
  industrial,
  combat,
}

enum IndustrialType {
  miner,
  plasmaCollector,
  mediumHauler,
  heavyHauler,
}

extension IndustrialTypeExt on IndustrialType {
  double get solidTimeMult {
    switch (this) {
      case IndustrialType.miner:
        return 1.0;
      case IndustrialType.plasmaCollector:
        return 2.0;
      case IndustrialType.mediumHauler:
        return 3.0;
      case IndustrialType.heavyHauler:
        return 5.0;
    }
  }

  double get plasmaTimeMult {
    switch (this) {
      case IndustrialType.miner:
        return 2.0;
      case IndustrialType.plasmaCollector:
        return 1.0;
      case IndustrialType.mediumHauler:
        return 3.0;
      case IndustrialType.heavyHauler:
        return 5.0;
    }
  }

  double extractionTimeMultiplier(ResourceCategory category) {
    switch (category) {
      case ResourceCategory.solid:
        return solidTimeMult;
      case ResourceCategory.plasma:
        return plasmaTimeMult;
      case ResourceCategory.salvage:
        return 1.0;
    }
  }

  Color get accentColor {
    switch (this) {
      case IndustrialType.miner:
        return const Color(0xFF38BDF8);
      case IndustrialType.plasmaCollector:
        return const Color(0xFF2DD4BF);
      case IndustrialType.mediumHauler:
        return const Color(0xFF4ADE80);
      case IndustrialType.heavyHauler:
        return const Color(0xFFFACC15);
    }
  }

  String get defaultHullId {
    switch (this) {
      case IndustrialType.miner:
        return 'MINER_T1';
      case IndustrialType.plasmaCollector:
        return 'PLASMA_COLLECTOR_T1';
      case IndustrialType.mediumHauler:
        return 'MEDIUM_HAULER_T1';
      case IndustrialType.heavyHauler:
        return 'HEAVY_HAULER_T1';
    }
  }
}

enum CraftQuality {
  standard,
  reinforced,
  excellent,
  masterwork,
}

extension CraftQualityExt on CraftQuality {
  double get bonusPct {
    switch (this) {
      case CraftQuality.standard:
        return 0.0;
      case CraftQuality.reinforced:
        return 0.05;
      case CraftQuality.excellent:
        return 0.10;
      case CraftQuality.masterwork:
        return 0.15;
    }
  }

  double get statMultiplier => 1.0 + bonusPct;

  double applyBonus(double baseValue) => baseValue * statMultiplier;

  int applyBonusInt(int baseValue) => (baseValue * statMultiplier).round();

  String get bonusLabel => '+${(bonusPct * 100).round()}%';

  Color get accentColor {
    switch (this) {
      case CraftQuality.standard:
        return const Color(0xFFE2E8F0);
      case CraftQuality.reinforced:
        return const Color(0xFF4ADE80);
      case CraftQuality.excellent:
        return const Color(0xFFA855F7);
      case CraftQuality.masterwork:
        return const Color(0xFFFACC15);
    }
  }
}

enum SkillType {
  mining,
  salvaging,
  refining,
  navalEngineering,
  gearManufacturing,
  synthesis,
  weaponry,
  shields,
  piloting,
}

extension SkillTypeExt on SkillType {
  Color get accentColor {
    switch (this) {
      case SkillType.mining:
        return const Color(0xFF38BDF8);
      case SkillType.salvaging:
        return const Color(0xFFC084FC);
      case SkillType.refining:
        return const Color(0xFFFB923C);
      case SkillType.navalEngineering:
        return const Color(0xFF34D399);
      case SkillType.gearManufacturing:
        return const Color(0xFF0EA5E9);
      case SkillType.synthesis:
        return const Color(0xFFE879F9);
      case SkillType.weaponry:
        return const Color(0xFFFF5252);
      case SkillType.shields:
        return const Color(0xFF60A5FA);
      case SkillType.piloting:
        return const Color(0xFFFACC15);
    }
  }
}

/// 4-tier exponential XP curve for Pilot Skills (Lv 1-99).
int xpRequiredForNextSkillLevel(int level) {
  if (level >= kPilotSkillMaxLevel) return 0;
  final int lv = max(kPilotSkillMinLevel, level);
  final double t1End = 100.0 * pow(1.06, 32.0);
  final double t2End = t1End * pow(1.085, 33.0);
  final double t3End = t2End * pow(1.12, 23.0);

  final double raw;
  if (lv <= 33) {
    raw = 100.0 * pow(1.06, (lv - 1).toDouble());
  } else if (lv <= 66) {
    raw = t1End * pow(1.085, (lv - 33).toDouble());
  } else if (lv <= 89) {
    raw = t2End * pow(1.12, (lv - 66).toDouble());
  } else {
    raw = t3End * pow(1.22, (lv - 89).toDouble());
  }
  return max(100, raw.round());
}

/// Piecewise linear extraction time per charge:
/// Lv 1 -> 3.0s, Lv 33 -> 2.0s, Lv 66 -> 1.0s, Lv 99 -> 0.1s.
double baseExtractionSeconds(int skillLevel) {
  final int lv = skillLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
  final double seconds;
  if (lv <= 33) {
    seconds = 3.0 - ((lv - 1) / 32.0) * 1.0;
  } else if (lv <= 66) {
    seconds = 2.0 - ((lv - 33) / 33.0) * 1.0;
  } else {
    seconds = 1.0 - ((lv - 66) / 33.0) * 0.9;
  }
  return max(0.1, seconds);
}

double effectiveExtractionSeconds({
  required int skillLevel,
  required ResourceCategory category,
  required ShipRole shipRole,
  IndustrialType? industrialType,
}) {
  final double base = baseExtractionSeconds(skillLevel);
  final double mult;
  if (shipRole == ShipRole.capsule) {
    mult = CapsulePermanentConfig.miningTimeMultiplier;
  } else if (industrialType != null) {
    mult = industrialType.extractionTimeMultiplier(category);
  } else {
    mult = 1.0;
  }
  return max(0.1, base * mult);
}

double refiningFeeDiscountPct(int refiningLevel) {
  final int lv = refiningLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
  return ((lv - 1) / 98.0) * 0.35;
}

double refiningBonusOutputChance(int refiningLevel) {
  final int lv = refiningLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
  return ((lv - 1) / 98.0) * 0.15;
}

class SkillProgress {
  const SkillProgress({
    this.level = kPilotSkillMinLevel,
    this.xp = 0,
  });

  final int level;
  final int xp;

  int get xpToNextLevel => xpRequiredForNextSkillLevel(level);
  bool get isMaxLevel => level >= kPilotSkillMaxLevel;

  double get progressFraction {
    final int req = xpToNextLevel;
    if (req <= 0) return 1.0;
    return (xp / req).clamp(0.0, 1.0);
  }

  SkillProgress addXp(int gainedXp) {
    if (gainedXp <= 0 || isMaxLevel) return this;
    int currentLevel = level.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
    int currentXp = max(0, xp + gainedXp);
    while (currentLevel < kPilotSkillMaxLevel) {
      final int needed = xpRequiredForNextSkillLevel(currentLevel);
      if (needed <= 0 || currentXp < needed) break;
      currentXp -= needed;
      currentLevel++;
    }
    if (currentLevel >= kPilotSkillMaxLevel) {
      return const SkillProgress(level: kPilotSkillMaxLevel, xp: 0);
    }
    return SkillProgress(level: currentLevel, xp: currentXp);
  }
}

Map<SkillType, SkillProgress> createDefaultPilotSkills() {
  return <SkillType, SkillProgress>{
    for (final SkillType s in SkillType.values) s: const SkillProgress(),
  };
}

// =============================================================================
// PERMANENT STARTING CAPSULE & INDUSTRIAL HULL CATALOG
// =============================================================================

class CapsulePermanentConfig {
  const CapsulePermanentConfig._();

  static const double baseHull = 90.0;
  static const double baseShield = 40.0;
  static const double speed = 310.0;
  static const int cargoCapacity = 25;
  static const double hullRadius = 14.0;
  static const double permanentLaserDamage = 12.0;
  static const double permanentShieldBonus = 80.0;
  static const double miningTimeMultiplier = 1.5;
}

class ShipHullDefinition {
  const ShipHullDefinition({
    required this.id,
    required this.displayName,
    required this.role,
    required this.tier,
    required this.speed,
    required this.baseHull,
    required this.baseShield,
    required this.baseCargo,
    required this.weaponSlotCount,
    required this.shieldSlotCount,
    required this.hullRadius,
    this.industrialType,
  });

  final String id;
  final String displayName;
  final ShipRole role;
  final IndustrialType? industrialType;
  final int tier;
  final double speed;
  final double baseHull;
  final double baseShield;
  final int baseCargo;
  final int weaponSlotCount;
  final int shieldSlotCount;
  final double hullRadius;

  double effectiveHull([CraftQuality quality = CraftQuality.standard]) =>
      quality.applyBonus(baseHull);

  double effectiveShield([CraftQuality quality = CraftQuality.standard]) =>
      quality.applyBonus(baseShield);

  int effectiveCargo([CraftQuality quality = CraftQuality.standard]) =>
      quality.applyBonusInt(baseCargo);
}

class ShipHullCatalog {
  const ShipHullCatalog._();

  static const ShipHullDefinition capsule = ShipHullDefinition(
    id: 'CAPSULE_T1',
    displayName: 'Pilot Capsule',
    role: ShipRole.capsule,
    tier: 1,
    speed: CapsulePermanentConfig.speed,
    baseHull: CapsulePermanentConfig.baseHull,
    baseShield: CapsulePermanentConfig.baseShield,
    baseCargo: CapsulePermanentConfig.cargoCapacity,
    weaponSlotCount: 1,
    shieldSlotCount: 1,
    hullRadius: CapsulePermanentConfig.hullRadius,
  );

  static const ShipHullDefinition minerT1 = ShipHullDefinition(
    id: 'MINER_T1',
    displayName: 'Prospector Mk I',
    role: ShipRole.industrial,
    industrialType: IndustrialType.miner,
    tier: 1,
    speed: 235.0,
    baseHull: 220.0,
    baseShield: 140.0,
    baseCargo: 240,
    weaponSlotCount: 2,
    shieldSlotCount: 4,
    hullRadius: 22.0,
  );

  static const ShipHullDefinition plasmaCollectorT1 = ShipHullDefinition(
    id: 'PLASMA_COLLECTOR_T1',
    displayName: 'Siphon Mk I',
    role: ShipRole.industrial,
    industrialType: IndustrialType.plasmaCollector,
    tier: 1,
    speed: 245.0,
    baseHull: 200.0,
    baseShield: 180.0,
    baseCargo: 220,
    weaponSlotCount: 2,
    shieldSlotCount: 5,
    hullRadius: 22.0,
  );

  static const ShipHullDefinition mediumHaulerT1 = ShipHullDefinition(
    id: 'MEDIUM_HAULER_T1',
    displayName: 'Freighter M-1',
    role: ShipRole.industrial,
    industrialType: IndustrialType.mediumHauler,
    tier: 1,
    speed: 200.0,
    baseHull: 360.0,
    baseShield: 240.0,
    baseCargo: 650,
    weaponSlotCount: 2,
    shieldSlotCount: 6,
    hullRadius: 26.0,
  );

  static const ShipHullDefinition heavyHaulerT1 = ShipHullDefinition(
    id: 'HEAVY_HAULER_T1',
    displayName: 'Leviathan H-1',
    role: ShipRole.industrial,
    industrialType: IndustrialType.heavyHauler,
    tier: 1,
    speed: 165.0,
    baseHull: 620.0,
    baseShield: 440.0,
    baseCargo: 1400,
    weaponSlotCount: 2,
    shieldSlotCount: 8,
    hullRadius: 31.0,
  );

  static const List<ShipHullDefinition> all = <ShipHullDefinition>[
    capsule,
    minerT1,
    plasmaCollectorT1,
    mediumHaulerT1,
    heavyHaulerT1,
  ];

  static ShipHullDefinition? byId(String id) {
    for (final ShipHullDefinition def in all) {
      if (def.id == id) return def;
    }
    return null;
  }

  static ShipHullDefinition industrialT1(IndustrialType type) {
    switch (type) {
      case IndustrialType.miner:
        return minerT1;
      case IndustrialType.plasmaCollector:
        return plasmaCollectorT1;
      case IndustrialType.mediumHauler:
        return mediumHaulerT1;
      case IndustrialType.heavyHauler:
        return heavyHaulerT1;
    }
  }

  static int cargoForCombatClass(ShipClass shipClass) {
    switch (shipClass) {
      case ShipClass.interceptor:
        return 80;
      case ShipClass.battlecruiser:
        return 160;
      case ShipClass.dreadnought:
        return 280;
    }
  }
}

// =============================================================================
// RESOURCES & CLOCKWISE REFINING LOOP (C1 -> C2 -> C3 -> C1)
// =============================================================================

enum ResourceCategory {
  solid,
  plasma,
  salvage,
}

enum ResourceType {
  starCobalt,
  aetherQuartz,
  veridianOre,
  bioResin,
  crimsonIron,
  magmaStone,
  yellowCrystal,
  xenoSalvage,
  xenoCore,
}

class ResourceSpec {
  const ResourceSpec({
    required this.type,
    required this.displayName,
    required this.refinedName,
    required this.category,
    required this.tier,
    required this.nativeSectorId,
    required this.refinesInSectorIds,
    required this.rawBasePrice,
    required this.refinedBasePrice,
    required this.color,
    this.isYellowCrystal = false,
    this.isDirectUse = false,
    this.isRare = false,
  });

  final ResourceType type;
  final String displayName;
  final String refinedName;
  final ResourceCategory category;
  final int tier;
  final String nativeSectorId;
  final Set<String> refinesInSectorIds;
  final int rawBasePrice;
  final int refinedBasePrice;
  final Color color;
  final bool isYellowCrystal;
  final bool isDirectUse;
  final bool isRare;
    Color get accentColor => color;
  

  bool get isRefinable => !isDirectUse && refinesInSectorIds.isNotEmpty;

  bool canRefineIn(String sectorId) =>
      isRefinable && refinesInSectorIds.contains(sectorId);

  int get baseRefiningFeePerUnit {
    if (isYellowCrystal) return 240;
    if (type == ResourceType.xenoSalvage) return 80;
    if (category == ResourceCategory.plasma) return 40;
    return 20;
  }

  int effectiveRefiningFeePerUnit(int refiningLevel) {
    final double discount = refiningFeeDiscountPct(refiningLevel);
    return max(1, (baseRefiningFeePerUnit * (1.0 - discount)).round());
  }

  int get refiningXpPerUnit {
    if (isYellowCrystal) return 140;
    if (type == ResourceType.xenoSalvage) return 68;
    if (category == ResourceCategory.plasma) return 42;
    return 25;
  }
}

class ResourceCatalog {
  const ResourceCatalog._();

  static const int rawRequiredPerRefine = 2;

  static const Map<ResourceType, ResourceSpec> specs = <ResourceType, ResourceSpec>{
    ResourceType.starCobalt: ResourceSpec(
      type: ResourceType.starCobalt,
      displayName: 'Star Cobalt',
      refinedName: 'Cobalt Alloy',
      category: ResourceCategory.solid,
      tier: 1,
      nativeSectorId: 'C1',
      refinesInSectorIds: <String>{'C2'},
      rawBasePrice: 60,
      refinedBasePrice: 185,
      color: Color(0xFF38BDF8),
    ),
    ResourceType.aetherQuartz: ResourceSpec(
      type: ResourceType.aetherQuartz,
      displayName: 'Aether Quartz',
      refinedName: 'Aether Crystal',
      category: ResourceCategory.plasma,
      tier: 2,
      nativeSectorId: 'C1',
      refinesInSectorIds: <String>{'C2'},
      rawBasePrice: 115,
      refinedBasePrice: 340,
      color: Color(0xFF0EA5E9),
    ),
    ResourceType.veridianOre: ResourceSpec(
      type: ResourceType.veridianOre,
      displayName: 'Veridian Ore',
      refinedName: 'Veridian Ingot',
      category: ResourceCategory.solid,
      tier: 1,
      nativeSectorId: 'C2',
      refinesInSectorIds: <String>{'C3'},
      rawBasePrice: 60,
      refinedBasePrice: 185,
      color: Color(0xFF4ADE80),
    ),
    ResourceType.bioResin: ResourceSpec(
      type: ResourceType.bioResin,
      displayName: 'Bio Resin',
      refinedName: 'Bio Polymer',
      category: ResourceCategory.plasma,
      tier: 2,
      nativeSectorId: 'C2',
      refinesInSectorIds: <String>{'C3'},
      rawBasePrice: 115,
      refinedBasePrice: 340,
      color: Color(0xFF34D399),
    ),
    ResourceType.crimsonIron: ResourceSpec(
      type: ResourceType.crimsonIron,
      displayName: 'Crimson Iron',
      refinedName: 'Crimson Steel',
      category: ResourceCategory.solid,
      tier: 1,
      nativeSectorId: 'C3',
      refinesInSectorIds: <String>{'C1'},
      rawBasePrice: 60,
      refinedBasePrice: 185,
      color: Color(0xFFFB7185),
    ),
    ResourceType.magmaStone: ResourceSpec(
      type: ResourceType.magmaStone,
      displayName: 'Magma Stone',
      refinedName: 'Thermal Core',
      category: ResourceCategory.plasma,
      tier: 2,
      nativeSectorId: 'C3',
      refinesInSectorIds: <String>{'C1'},
      rawBasePrice: 115,
      refinedBasePrice: 340,
      color: Color(0xFFF97316),
    ),
    ResourceType.yellowCrystal: ResourceSpec(
      type: ResourceType.yellowCrystal,
      displayName: 'Yellow Crystal',
      refinedName: 'Refined Yellow Crystal',
      category: ResourceCategory.plasma,
      tier: 5,
      nativeSectorId: '0-0',
      refinesInSectorIds: <String>{'C1', 'C2', 'C3'},
      rawBasePrice: 760,
      refinedBasePrice: 2450,
      color: Color(0xFFFACC15),
      isYellowCrystal: true,
    ),
    ResourceType.xenoSalvage: ResourceSpec(
      type: ResourceType.xenoSalvage,
      displayName: 'Xeno Salvage',
      refinedName: 'Xeno Alloy',
      category: ResourceCategory.salvage,
      tier: 3,
      nativeSectorId: 'ALIEN',
      refinesInSectorIds: <String>{'C1', 'C2', 'C3'},
      rawBasePrice: 240,
      refinedBasePrice: 720,
      color: Color(0xFFC084FC),
    ),
    ResourceType.xenoCore: ResourceSpec(
      type: ResourceType.xenoCore,
      displayName: 'Xeno Core',
      refinedName: 'Xeno Core',
      category: ResourceCategory.salvage,
      tier: 5,
      nativeSectorId: 'ALIEN',
      refinesInSectorIds: <String>{},
      rawBasePrice: 1200,
      refinedBasePrice: 1200,
      color: Color(0xFFE879F9),
      isDirectUse: true,
      isRare: true,
    ),
  };

  static ResourceSpec of(ResourceType type) => specs[type]!;

  static ResourceType forRegionAndTier(String nativeCityId, int tier) {
    final bool usePlasma = tier % 2 == 0;
    switch (nativeCityId) {
      case 'C1':
        return usePlasma ? ResourceType.aetherQuartz : ResourceType.starCobalt;
      case 'C2':
        return usePlasma ? ResourceType.bioResin : ResourceType.veridianOre;
      case 'C3':
        return usePlasma ? ResourceType.magmaStone : ResourceType.crimsonIron;
      case '0-0':
        return ResourceType.yellowCrystal;
      default:
        return ResourceType.starCobalt;
    }
  }
}

// =============================================================================
// STATION CRAFTING (3 DISCIPLINES + QUALITY ROLLS) & BASIC VENDOR
// =============================================================================

enum StationKind {
  refinery,
  localMarket,
  crafting,
}

enum CraftingDiscipline {
  navalEngineering,
  gearManufacturing,
  synthesis,
}

extension CraftingDisciplineExt on CraftingDiscipline {
  SkillType get skill {
    switch (this) {
      case CraftingDiscipline.navalEngineering:
        return SkillType.navalEngineering;
      case CraftingDiscipline.gearManufacturing:
        return SkillType.gearManufacturing;
      case CraftingDiscipline.synthesis:
        return SkillType.synthesis;
    }
  }

  Color get accentColor {
    switch (this) {
      case CraftingDiscipline.navalEngineering:
        return const Color(0xFF34D399);
      case CraftingDiscipline.gearManufacturing:
        return const Color(0xFF38BDF8);
      case CraftingDiscipline.synthesis:
        return const Color(0xFFE879F9);
    }
  }
}

class CraftMaterialCost {
  const CraftMaterialCost({
    required this.resource,
    required this.amount,
    this.isRefined = true,
  });

  final ResourceType resource;
  final bool isRefined;
  final int amount;
}

enum CraftOutputKind {
  industrialShip,
  equipmentGear,
  extractionTool,
  consumableRepair,
  consumableBooster,
}

enum StationCraftRecipeId {
  shipMiner,
  shipPlasmaCollector,
  shipMediumHauler,
  shipHeavyHauler,
  gearLaserT2,
  gearLaserT3,
  gearShieldT2,
  gearShieldT3,
  gearMiningToolT2,
  gearSalvageToolT2,
  synthRepairKit,
  synthDamageBooster,
  synthShieldBooster,
  synthSpeedBooster,
}

class StationCraftRecipeSpec {
  const StationCraftRecipeSpec({
    required this.id,
    required this.discipline,
    required this.outputKind,
    required this.accentColor,
    required this.xpReward,
    required this.ingredients,
    this.shipHullId,
    this.industrialType,
    this.resultEquipmentId,
    this.basePrimaryStat = 0.0,
    this.baseDurationMinutes = 0,
  });

  final StationCraftRecipeId id;
  final CraftingDiscipline discipline;
  final CraftOutputKind outputKind;
  final Color accentColor;
  final int xpReward;
  final List<CraftMaterialCost> ingredients;
  final String? shipHullId;
  final IndustrialType? industrialType;
  final EquipmentItemId? resultEquipmentId;
  final double basePrimaryStat;
  final int baseDurationMinutes;

  ShipHullDefinition? get shipHullDefinition =>
      shipHullId == null ? null : ShipHullCatalog.byId(shipHullId!);
}

class StationCraftCatalog {
  const StationCraftCatalog._();

  static const Map<StationCraftRecipeId, StationCraftRecipeSpec> specs =
      <StationCraftRecipeId, StationCraftRecipeSpec>{
    StationCraftRecipeId.shipMiner: StationCraftRecipeSpec(
      id: StationCraftRecipeId.shipMiner,
      discipline: CraftingDiscipline.navalEngineering,
      outputKind: CraftOutputKind.industrialShip,
      accentColor: Color(0xFF38BDF8),
      xpReward: 180,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.starCobalt, amount: 4),
        CraftMaterialCost(resource: ResourceType.veridianOre, amount: 3),
        CraftMaterialCost(resource: ResourceType.aetherQuartz, amount: 2),
      ],
      shipHullId: 'MINER_T1',
      industrialType: IndustrialType.miner,
    ),
    StationCraftRecipeId.shipPlasmaCollector: StationCraftRecipeSpec(
      id: StationCraftRecipeId.shipPlasmaCollector,
      discipline: CraftingDiscipline.navalEngineering,
      outputKind: CraftOutputKind.industrialShip,
      accentColor: Color(0xFF2DD4BF),
      xpReward: 195,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.starCobalt, amount: 3),
        CraftMaterialCost(resource: ResourceType.bioResin, amount: 3),
        CraftMaterialCost(resource: ResourceType.magmaStone, amount: 3),
      ],
      shipHullId: 'PLASMA_COLLECTOR_T1',
      industrialType: IndustrialType.plasmaCollector,
    ),
    StationCraftRecipeId.shipMediumHauler: StationCraftRecipeSpec(
      id: StationCraftRecipeId.shipMediumHauler,
      discipline: CraftingDiscipline.navalEngineering,
      outputKind: CraftOutputKind.industrialShip,
      accentColor: Color(0xFF4ADE80),
      xpReward: 260,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.veridianOre, amount: 5),
        CraftMaterialCost(resource: ResourceType.crimsonIron, amount: 5),
        CraftMaterialCost(resource: ResourceType.bioResin, amount: 3),
      ],
      shipHullId: 'MEDIUM_HAULER_T1',
      industrialType: IndustrialType.mediumHauler,
    ),
    StationCraftRecipeId.shipHeavyHauler: StationCraftRecipeSpec(
      id: StationCraftRecipeId.shipHeavyHauler,
      discipline: CraftingDiscipline.navalEngineering,
      outputKind: CraftOutputKind.industrialShip,
      accentColor: Color(0xFFFACC15),
      xpReward: 380,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.crimsonIron, amount: 8),
        CraftMaterialCost(resource: ResourceType.starCobalt, amount: 6),
        CraftMaterialCost(resource: ResourceType.magmaStone, amount: 4),
        CraftMaterialCost(resource: ResourceType.aetherQuartz, amount: 4),
      ],
      shipHullId: 'HEAVY_HAULER_T1',
      industrialType: IndustrialType.heavyHauler,
    ),
    StationCraftRecipeId.gearLaserT2: StationCraftRecipeSpec(
      id: StationCraftRecipeId.gearLaserT2,
      discipline: CraftingDiscipline.gearManufacturing,
      outputKind: CraftOutputKind.equipmentGear,
      accentColor: Color(0xFFFF5252),
      xpReward: 140,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.starCobalt, amount: 3),
        CraftMaterialCost(resource: ResourceType.magmaStone, amount: 2),
        CraftMaterialCost(resource: ResourceType.xenoSalvage, amount: 2),
      ],
      resultEquipmentId: EquipmentItemId.lf2,
      basePrimaryStat: 28.0,
    ),
    StationCraftRecipeId.gearLaserT3: StationCraftRecipeSpec(
      id: StationCraftRecipeId.gearLaserT3,
      discipline: CraftingDiscipline.gearManufacturing,
      outputKind: CraftOutputKind.equipmentGear,
      accentColor: Color(0xFFF43F5E),
      xpReward: 240,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.crimsonIron, amount: 4),
        CraftMaterialCost(resource: ResourceType.aetherQuartz, amount: 3),
        CraftMaterialCost(resource: ResourceType.xenoSalvage, amount: 4),
      ],
      resultEquipmentId: EquipmentItemId.lf3,
      basePrimaryStat: 60.0,
    ),
    StationCraftRecipeId.gearShieldT2: StationCraftRecipeSpec(
      id: StationCraftRecipeId.gearShieldT2,
      discipline: CraftingDiscipline.gearManufacturing,
      outputKind: CraftOutputKind.equipmentGear,
      accentColor: Color(0xFF0EA5E9),
      xpReward: 140,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.veridianOre, amount: 3),
        CraftMaterialCost(resource: ResourceType.aetherQuartz, amount: 2),
        CraftMaterialCost(resource: ResourceType.xenoSalvage, amount: 2),
      ],
      resultEquipmentId: EquipmentItemId.sg2,
      basePrimaryStat: 180.0,
    ),
    StationCraftRecipeId.gearShieldT3: StationCraftRecipeSpec(
      id: StationCraftRecipeId.gearShieldT3,
      discipline: CraftingDiscipline.gearManufacturing,
      outputKind: CraftOutputKind.equipmentGear,
      accentColor: Color(0xFF818CF8),
      xpReward: 240,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.veridianOre, amount: 4),
        CraftMaterialCost(resource: ResourceType.bioResin, amount: 3),
        CraftMaterialCost(resource: ResourceType.xenoSalvage, amount: 4),
      ],
      resultEquipmentId: EquipmentItemId.sg3,
      basePrimaryStat: 380.0,
    ),
    StationCraftRecipeId.gearMiningToolT2: StationCraftRecipeSpec(
      id: StationCraftRecipeId.gearMiningToolT2,
      discipline: CraftingDiscipline.gearManufacturing,
      outputKind: CraftOutputKind.extractionTool,
      accentColor: Color(0xFF38BDF8),
      xpReward: 125,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.starCobalt, amount: 3),
        CraftMaterialCost(resource: ResourceType.bioResin, amount: 2),
        CraftMaterialCost(resource: ResourceType.xenoSalvage, amount: 1),
      ],
      basePrimaryStat: 15.0,
    ),
    StationCraftRecipeId.gearSalvageToolT2: StationCraftRecipeSpec(
      id: StationCraftRecipeId.gearSalvageToolT2,
      discipline: CraftingDiscipline.gearManufacturing,
      outputKind: CraftOutputKind.extractionTool,
      accentColor: Color(0xFFC084FC),
      xpReward: 135,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.crimsonIron, amount: 3),
        CraftMaterialCost(resource: ResourceType.magmaStone, amount: 2),
        CraftMaterialCost(resource: ResourceType.xenoSalvage, amount: 2),
      ],
      basePrimaryStat: 15.0,
    ),
    StationCraftRecipeId.synthRepairKit: StationCraftRecipeSpec(
      id: StationCraftRecipeId.synthRepairKit,
      discipline: CraftingDiscipline.synthesis,
      outputKind: CraftOutputKind.consumableRepair,
      accentColor: Color(0xFF4ADE80),
      xpReward: 90,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.bioResin, amount: 2),
        CraftMaterialCost(resource: ResourceType.magmaStone, amount: 1),
      ],
      basePrimaryStat: 250.0,
    ),
    StationCraftRecipeId.synthDamageBooster: StationCraftRecipeSpec(
      id: StationCraftRecipeId.synthDamageBooster,
      discipline: CraftingDiscipline.synthesis,
      outputKind: CraftOutputKind.consumableBooster,
      accentColor: Color(0xFFFF6B35),
      xpReward: 190,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.magmaStone, amount: 2),
        CraftMaterialCost(resource: ResourceType.yellowCrystal, amount: 1),
        CraftMaterialCost(
          resource: ResourceType.xenoCore,
          amount: 1,
          isRefined: false,
        ),
      ],
      basePrimaryStat: 12.0,
      baseDurationMinutes: 10,
    ),
    StationCraftRecipeId.synthShieldBooster: StationCraftRecipeSpec(
      id: StationCraftRecipeId.synthShieldBooster,
      discipline: CraftingDiscipline.synthesis,
      outputKind: CraftOutputKind.consumableBooster,
      accentColor: Color(0xFF38BDF8),
      xpReward: 190,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.bioResin, amount: 2),
        CraftMaterialCost(resource: ResourceType.yellowCrystal, amount: 1),
        CraftMaterialCost(
          resource: ResourceType.xenoCore,
          amount: 1,
          isRefined: false,
        ),
      ],
      basePrimaryStat: 15.0,
      baseDurationMinutes: 10,
    ),
    StationCraftRecipeId.synthSpeedBooster: StationCraftRecipeSpec(
      id: StationCraftRecipeId.synthSpeedBooster,
      discipline: CraftingDiscipline.synthesis,
      outputKind: CraftOutputKind.consumableBooster,
      accentColor: Color(0xFFFACC15),
      xpReward: 165,
      ingredients: <CraftMaterialCost>[
        CraftMaterialCost(resource: ResourceType.bioResin, amount: 1),
        CraftMaterialCost(resource: ResourceType.magmaStone, amount: 1),
        CraftMaterialCost(resource: ResourceType.yellowCrystal, amount: 1),
      ],
      basePrimaryStat: 10.0,
      baseDurationMinutes: 10,
    ),
  };

  static StationCraftRecipeSpec of(StationCraftRecipeId id) => specs[id]!;

  static List<StationCraftRecipeSpec> forDiscipline(
    CraftingDiscipline discipline,
  ) {
    return specs.values
        .where((StationCraftRecipeSpec s) => s.discipline == discipline)
        .toList();
  }
}

class CraftQualityOdds {
  const CraftQualityOdds({
    required this.standardPct,
    required this.reinforcedPct,
    required this.excellentPct,
    required this.masterworkPct,
  });

  final int standardPct;
  final int reinforcedPct;
  final int excellentPct;
  final int masterworkPct;
}

CraftQualityOdds computeCraftQualityOdds(int skillLevel) {
  final int lv = skillLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
  final double masterwork;
  final double excellent;
  final double reinforced;
  if (lv <= 33) {
    final double t = (lv - 1) / 32.0;
    masterwork = 0.0;
    excellent = 0.0;
    reinforced = 0.06 + t * 0.16;
  } else if (lv <= 66) {
    final double t = (lv - 34) / 32.0;
    masterwork = 0.01 + t * 0.04;
    excellent = 0.08 + t * 0.18;
    reinforced = 0.24 + t * 0.14;
  } else {
    final double t = (lv - 67) / 32.0;
    masterwork = 0.07 + t * 0.21;
    excellent = 0.28 + t * 0.14;
    reinforced = 0.36 - t * 0.14;
  }
  final int mwInt = (masterwork * 100.0).round();
  final int exInt = (excellent * 100.0).round();
  final int rfInt = (reinforced * 100.0).round();
  final int stInt = max(0, 100 - mwInt - exInt - rfInt);
  return CraftQualityOdds(
    standardPct: stInt,
    reinforcedPct: rfInt,
    excellentPct: exInt,
    masterworkPct: mwInt,
  );
}

CraftQuality rollCraftQuality(int skillLevel, Random rng) {
  final CraftQualityOdds odds = computeCraftQualityOdds(skillLevel);
  final int roll = rng.nextInt(100);
  final int mwCut = odds.masterworkPct;
  final int exCut = mwCut + odds.excellentPct;
  final int rfCut = exCut + odds.reinforcedPct;
  if (roll < mwCut) return CraftQuality.masterwork;
  if (roll < exCut) return CraftQuality.excellent;
  if (roll < rfCut) return CraftQuality.reinforced;
  return CraftQuality.standard;
}

double craftingMaterialReturnRate(int skillLevel) {
  final int lv = skillLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
  return ((lv - 1) / 98.0) * 0.15;
}

double synthesisDoubleOutputChance(int skillLevel) {
  final int lv = skillLevel.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
  return ((lv - 1) / 98.0) * 0.20;
}

class CraftedShipRecord {
  const CraftedShipRecord({
    required this.instanceId,
    required this.recipeId,
    required this.hullId,
    required this.industrialType,
    required this.quality,
    required this.effectiveHull,
    required this.effectiveShield,
    required this.effectiveCargo,
  });

  final int instanceId;
  final StationCraftRecipeId recipeId;
  final String hullId;
  final IndustrialType industrialType;
  final CraftQuality quality;
  final int effectiveHull;
  final int effectiveShield;
  final int effectiveCargo;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'instanceId': instanceId,
        'recipeId': recipeId.name,
        'hullId': hullId,
        'industrialType': industrialType.name,
        'quality': quality.name,
        'effectiveHull': effectiveHull,
        'effectiveShield': effectiveShield,
        'effectiveCargo': effectiveCargo,
      };

  static CraftedShipRecord? fromJson(Map<String, dynamic> json) {
    final String? rName = json['recipeId'] as String?;
    final String? iName = json['industrialType'] as String?;
    final String? qName = json['quality'] as String?;
    final StationCraftRecipeId? recipe = StationCraftRecipeId.values
        .where((StationCraftRecipeId e) => e.name == rName)
        .cast<StationCraftRecipeId?>()
        .firstWhere((StationCraftRecipeId? e) => e != null, orElse: () => null);
    final IndustrialType? ind = IndustrialType.values
        .where((IndustrialType e) => e.name == iName)
        .cast<IndustrialType?>()
        .firstWhere((IndustrialType? e) => e != null, orElse: () => null);
    final CraftQuality qual = CraftQuality.values.firstWhere(
      (CraftQuality q) => q.name == qName,
      orElse: () => CraftQuality.standard,
    );
    if (recipe == null || ind == null) return null;
    return CraftedShipRecord(
      instanceId: (json['instanceId'] as num?)?.toInt() ?? 1,
      recipeId: recipe,
      hullId: (json['hullId'] as String?) ?? ind.defaultHullId,
      industrialType: ind,
      quality: qual,
      effectiveHull: (json['effectiveHull'] as num?)?.toInt() ?? 220,
      effectiveShield: (json['effectiveShield'] as num?)?.toInt() ?? 140,
      effectiveCargo: (json['effectiveCargo'] as num?)?.toInt() ?? 240,
    );
  }
}

class CraftedItemRecord {
  CraftedItemRecord({
    required this.instanceId,
    required this.recipeId,
    required this.quality,
    required this.effectivePrimaryStat,
    this.effectiveDurationMinutes = 0,
    this.quantity = 1,
  });

  final int instanceId;
  final StationCraftRecipeId recipeId;
  final CraftQuality quality;
  final double effectivePrimaryStat;
  final int effectiveDurationMinutes;
  int quantity;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'instanceId': instanceId,
        'recipeId': recipeId.name,
        'quality': quality.name,
        'effectivePrimaryStat': effectivePrimaryStat,
        'effectiveDurationMinutes': effectiveDurationMinutes,
        'quantity': quantity,
      };

  static CraftedItemRecord? fromJson(Map<String, dynamic> json) {
    final String? rName = json['recipeId'] as String?;
    final String? qName = json['quality'] as String?;
    final StationCraftRecipeId? recipe = StationCraftRecipeId.values
        .where((StationCraftRecipeId e) => e.name == rName)
        .cast<StationCraftRecipeId?>()
        .firstWhere((StationCraftRecipeId? e) => e != null, orElse: () => null);
    if (recipe == null) return null;
    final CraftQuality qual = CraftQuality.values.firstWhere(
      (CraftQuality q) => q.name == qName,
      orElse: () => CraftQuality.standard,
    );
    return CraftedItemRecord(
      instanceId: (json['instanceId'] as num?)?.toInt() ?? 1,
      recipeId: recipe,
      quality: qual,
      effectivePrimaryStat:
          (json['effectivePrimaryStat'] as num?)?.toDouble() ?? 0.0,
      effectiveDurationMinutes:
          (json['effectiveDurationMinutes'] as num?)?.toInt() ?? 0,
      quantity: max(1, (json['quantity'] as num?)?.toInt() ?? 1),
    );
  }
}

class CraftOutcome {
  const CraftOutcome({
    required this.id,
    required this.recipeId,
    required this.quality,
    required this.quantityCrafted,
    required this.refundedUnits,
    required this.xpAwarded,
    required this.doubledOutput,
    required this.statSummary,
  });

  final int id;
  final StationCraftRecipeId recipeId;
  final CraftQuality quality;
  final int quantityCrafted;
  final int refundedUnits;
  final int xpAwarded;
  final bool doubledOutput;
  final String statSummary;
}

// =============================================================================
// BASIC VENDOR, STATIONS, MARKETS, RESOURCE NODES, WRECKS & DEATH SUMMARY
// =============================================================================

enum BasicVendorItemId {
  basicLaserT1,
  basicToolT1,
  basicShieldT1,
  basicRepairKit,
}

extension BasicVendorItemIdExt on BasicVendorItemId {
  int get priceCredits {
    switch (this) {
      case BasicVendorItemId.basicLaserT1:
        return 120;
      case BasicVendorItemId.basicToolT1:
        return 100;
      case BasicVendorItemId.basicShieldT1:
        return 140;
      case BasicVendorItemId.basicRepairKit:
        return 80;
    }
  }

  Color get accentColor {
    switch (this) {
      case BasicVendorItemId.basicLaserT1:
        return const Color(0xFFFF6B35);
      case BasicVendorItemId.basicToolT1:
        return const Color(0xFF38BDF8);
      case BasicVendorItemId.basicShieldT1:
        return const Color(0xFF0EA5E9);
      case BasicVendorItemId.basicRepairKit:
        return const Color(0xFF4ADE80);
    }
  }

  EquipmentItemId? get equipmentId {
    switch (this) {
      case BasicVendorItemId.basicLaserT1:
        return EquipmentItemId.lf1;
      case BasicVendorItemId.basicShieldT1:
        return EquipmentItemId.sg1;
      case BasicVendorItemId.basicToolT1:
      case BasicVendorItemId.basicRepairKit:
        return null;
    }
  }
}

class SectorStation {
  const SectorStation({
    required this.kind,
    required this.sectorId,
    required this.name,
    required this.worldX,
    required this.worldY,
    required this.accentColor,
    this.interactRadius = 170.0,
  });

  final StationKind kind;
  final String sectorId;
  final String name;
  final double worldX;
  final double worldY;
  final double interactRadius;
  final Color accentColor;
}

class MarketResourceListing {
  MarketResourceListing({
    required this.resourceType,
    required this.isRefined,
    required this.buyPrice,
    required this.sellPrice,
    required this.localStock,
  });

  final ResourceType resourceType;
  final bool isRefined;
  final int buyPrice;
  final int sellPrice;
  int localStock;
}

class MarketEquipmentListing {
  MarketEquipmentListing({
    required this.itemId,
    required this.buyPrice,
    required this.localStock,
  });

  final EquipmentItemId itemId;
  final int buyPrice;
  int localStock;
}

class LocalMarketState {
  LocalMarketState({
    required this.sectorId,
    required this.marketName,
    required this.isBlackMarket,
    required this.resourceListings,
    required this.equipmentListings,
  });

  final String sectorId;
  final String marketName;
  final bool isBlackMarket;
  final List<MarketResourceListing> resourceListings;
  final List<MarketEquipmentListing> equipmentListings;
}

class ResourceNodeEntity {
  ResourceNodeEntity({
    required this.id,
    required this.resourceType,
    required this.x,
    required this.y,
    required this.radius,
    required this.rotationRad,
    required this.pulsePhase,
    this.maxCharges = 4,
    int? charges,
  }) : charges = charges ?? maxCharges;

  final int id;
  final ResourceType resourceType;
  double x;
  double y;
  final double radius;
  double rotationRad;
  double pulsePhase;
  final int maxCharges;
  int charges;
  double harvestProgress = 0.0;

  bool get isDepleted => charges <= 0;
  double get worldX => x;
  double get worldY => y;
  int get chargesRemaining => charges;
}


class AlienWreckEntity {
  AlienWreckEntity({
    required this.id,
    required this.sourceTier,
    required this.fromBoss,
    required this.x,
    required this.y,
    required this.rotationRad,
    this.maxCharges = 3,
    int? charges,
    this.expirationTimerSec = 90.0,
  })  : charges = charges ?? maxCharges,
        radius = fromBoss ? 28.0 : 22.0;

  final int id;
  final AlienTier sourceTier;
  final bool fromBoss;
  double x;
  double y;
  double rotationRad;
  final double radius;
  final int maxCharges;
  int charges;
  double expirationTimerSec;
  double salvageProgress = 0.0;
  double pulsePhase = 0.0;

  bool get isDepleted => charges <= 0 || expirationTimerSec <= 0.0;
    double get worldX => x;
  double get worldY => y;
  int get chargesRemaining => charges;
  
}

class PlayerShipWreck {
  PlayerShipWreck({
    required this.id,
    required this.sectorId,
    required this.x,
    required this.y,
    required this.shipName,
    required this.isCapsuleWreck,
    required this.rawCargo,
    required this.refinedCargo,
    required this.droppedEquipment,
    this.expirationTimerSec = 300.0,
  });

  final int id;
  final String sectorId;
  final double x;
  final double y;
  final String shipName;
  final bool isCapsuleWreck;
  final Map<ResourceType, int> rawCargo;
  final Map<ResourceType, int> refinedCargo;
  final List<EquipmentItem> droppedEquipment;
  double expirationTimerSec;

  int get totalCargoUnits {
    int sum = 0;
    for (final int v in rawCargo.values) {
      sum += v;
    }
    for (final int v in refinedCargo.values) {
      sum += v;
    }
    return sum;
  }

  int get totalGearItems => droppedEquipment.length;

  bool get isEmpty => totalCargoUnits <= 0 && totalGearItems <= 0;
    double get worldX => x;
  double get worldY => y;
  
}

class DeathSummary {
  const DeathSummary({
    required this.wasInCapsule,
    required this.destroyedShipName,
    required this.deathSectorId,
    required this.deathX,
    required this.deathY,
    required this.cargoDroppedToWreck,
    required this.cargoDestroyed,
    required this.gearDroppedToWreck,
    required this.gearDestroyed,
    required this.vantsDamagedCount,
    required this.homeCityId,
  });

  final bool wasInCapsule;
  final String? destroyedShipName;
  final String deathSectorId;
  final int deathX;
  final int deathY;
  final int cargoDroppedToWreck;
  final int cargoDestroyed;
  final int gearDroppedToWreck;
  final int gearDestroyed;
  final int vantsDamagedCount;
  final String homeCityId;
}
