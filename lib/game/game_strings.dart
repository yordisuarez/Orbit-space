import 'economy_models.dart';
import 'galaxy_map_data.dart';
import 'game_models.dart';

enum GameLanguage {
  en,
  es,
}

extension GameLanguageExt on GameLanguage {
  String get code => this == GameLanguage.es ? 'es' : 'en';

  String get displayLabel =>
      this == GameLanguage.es ? 'Español (ES)' : 'English (EN)';

  static GameLanguage fromCode(String? raw) {
    if (raw != null && raw.toLowerCase() == 'es') {
      return GameLanguage.es;
    }
    return GameLanguage.en;
  }
}

class GuideEntry {
  const GuideEntry({
    required this.id,
    required this.titleEn,
    required this.bodyEn,
    required this.titleEs,
    required this.bodyEs,
  });

  final String id;
  final String titleEn;
  final String bodyEn;
  final String titleEs;
  final String bodyEs;

  String title(GameLanguage lang) =>
      lang == GameLanguage.es ? titleEs : titleEn;

  String body(GameLanguage lang) => lang == GameLanguage.es ? bodyEs : bodyEn;
}

class GameStrings {
    const GameStrings._(this.lang);
  final GameLanguage lang;

  static GameStrings of(GameLanguage lang) => GameStrings._(lang);

  String get sectorMapTitle => t(lang, 'SECTOR MAP', 'MAPA DE SECTOR');
  String get tapToSteerHint =>
      t(lang, 'Tap map to autopilot', 'Toca el mapa para piloto automático');
  String get closeLabel => actionClose(lang);
  String get dockedSwapReady => t(lang, 'DOCKED • READY', 'ATRACADO • LISTO');
  String get dockRequiredShort => t(lang, 'DOCK REQUIRED', 'REQUIERE ATRAQUE');
  String get capsuleShipTitle => t(lang, 'PILOT CAPSULE', 'CÁPSULA PILOTO');
  String get permanentBadge => t(lang, 'PERMANENT', 'PERMANENTE');
  String get pilotingLabel => t(lang, 'PILOTING', 'PILOTANDO');
  String get boardShipLabel => t(lang, 'BOARD SHIP', 'ABORDAR NAVE');
  String get adminModeOnLabel => 'ADMIN: ON';
  String get adminModeOffLabel => 'ADMIN: OFF';
  String get hangarReadOnlyNotice => t(
        lang,
        'Dock at a station to swap ships',
        'Atraca en una estación para cambiar de nave',
      );
  String get fleetBayTitle => t(lang, 'FLEET BAY', 'BAHÍA DE FLOTA');
    String get hudMenuButton => t(lang, 'MENU', 'MENÚ');
  String get dockButtonLabel => t(lang, 'DOCK', 'ATRACAR');
  
  
  static const List<GuideEntry> guideEntries = <GuideEntry>[
    GuideEntry(
      id: 'capsule_death_loot',
      titleEn: 'Capsule, Death & Loot (60/40)',
      bodyEn:
          '• Your Pilot never leaves the Capsule. Its 1 laser, 1 tool, and 1 extra slot are permanent and never lost.\n'
          '• Losing a ship in danger zones destroys that ship permanently.\n'
          '• 60% of carried cargo and ship gear drops in a wreck; the remaining 40% is destroyed.\n'
          '• Your Capsule and VANTs automatically return to your home city.',
      titleEs: 'Cápsula, Muerte y Botín (60/40)',
      bodyEs:
          '• El Piloto nunca abandona la Cápsula: su 1 láser, 1 herramienta y 1 ranura extra son permanentes y nunca se pierden.\n'
          '• Perder una nave en zonas de peligro la destruye permanentemente.\n'
          '• El 60% de la carga y el equipo cae en un pecio; el 40% restante se destruye.\n'
          '• Tu Cápsula y tus VANTs regresan automáticamente a tu ciudad natal.',
    ),
    GuideEntry(
      id: 'professions_industrial',
      titleEn: 'Professions & Industrial Ships',
      bodyEn:
          '• Industrial ships mine resource nodes by charges; Combat ships salvage Alien Wrecks for Xeno Salvage and Xeno Cores.\n'
          '• Industrial extraction time multipliers:\n'
          '  – Miner: 1x Solid / 2x Plasma\n'
          '  – Plasma Collector: 1x Plasma / 2x Solid\n'
          '  – Medium Hauler: 3x Any resource\n'
          '  – Heavy Hauler: 5x Any resource',
      titleEs: 'Profesiones y Naves Industriales',
      bodyEs:
          '• Las naves Industriales minan nodos de recursos por cargas; las naves de Combate chatarrean Pecios Alienígenas para obtener Chatarra Xeno y Núcleos Xeno.\n'
          '• Multiplicadores de tiempo de extracción industrial:\n'
          '  – Minero: 1x Sólido / 2x Plasma\n'
          '  – Recolector de Plasma: 1x Plasma / 2x Sólido\n'
          '  – Carguero Mediano: 3x Cualquier recurso\n'
          '  – Carguero Pesado: 5x Cualquier recurso',
    ),
    GuideEntry(
      id: 'resources_refining',
      titleEn: 'Resources & Clockwise Refining',
      bodyEn:
          '• Raw regional resources refine in the next Capital City clockwise:\n'
          '  – C1 raw → refines in C2\n'
          '  – C2 raw → refines in C3\n'
          '  – C3 raw → refines in C1\n'
          '• Nucleus Yellow Crystal and Alien Xeno Salvage can be refined in any Capital City (C1, C2, or C3).',
      titleEs: 'Recursos y Refinado Horario',
      bodyEs:
          '• Los recursos brutos regionales se refinan en la siguiente Ciudad Capital en sentido horario:\n'
          '  – Bruto C1 → se refina en C2\n'
          '  – Bruto C2 → se refina en C3\n'
          '  – Bruto C3 → se refina en C1\n'
          '• El Cristal Amarillo del Núcleo y la Chatarra Xeno se pueden refinar en cualquier ciudad (C1, C2 o C3).',
    ),
    GuideEntry(
      id: 'pilot_skills',
      titleEn: 'Pilot Skills (Lv 1–99)',
      bodyEn:
          '• All 9 skills belong to your Pilot profile (not your ship) and persist across ships.\n'
          '• Shared 4-tier XP curve: Lv 1–33 Easy • Lv 34–66 Medium • Lv 67–89 Hard • Lv 90–99 Very Hard.\n'
          '• Mining & Salvaging base time per charge scales smoothly:\n'
          '  3.0s (Lv 1) → 2.0s (Lv 33) → 1.0s (Lv 66) → 0.1s (Lv 99).',
      titleEs: 'Habilidades de Piloto (Nv 1–99)',
      bodyEs:
          '• Las 9 habilidades pertenecen al Piloto (no a la nave) y se conservan al cambiar de casco.\n'
          '• Curva compartida en 4 tramos: Nv 1–33 Fácil • Nv 34–66 Media • Nv 67–89 Difícil • Nv 90–99 Muy Difícil.\n'
          '• El tiempo base de Minería y Chatarreo por carga escala de forma continua:\n'
          '  3.0s (Nv 1) → 2.0s (Nv 33) → 1.0s (Nv 66) → 0.1s (Nv 99).',
    ),
    GuideEntry(
      id: 'crafting_quality_balance',
      titleEn: 'Crafting Quality & Tier Balance',
      bodyEn:
          '• Crafted items roll one of 4 qualities: Standard (+0%), Reinforced (+5%), Excellent (+10%), and Masterwork (+15%).\n'
          '• Quality bonuses apply to HP, Shield, and Cargo on ships, or the primary stat on gear and boosters.\n'
          '• Higher-tier ships keep balanced weapon slots while scaling hull, shield, and cargo.',
      titleEs: 'Calidad de Fabricación y Balance',
      bodyEs:
          '• Los objetos fabricados obtienen 4 calidades: Estándar (+0%), Reforzado (+5%), Excelente (+10%) y Obra Maestra (+15%).\n'
          '• El bono se aplica a Casco, Escudo y Carga en naves, o al atributo principal en equipo y potenciadores.\n'
          '• Las naves de mayor nivel mantienen ranuras de armas equilibradas mientras escalan resistencia y carga.',
    ),
    GuideEntry(
      id: 'player_economy',
      titleEn: 'Player-Driven Economy & Camera',
      bodyEn:
          '• Capital Cities only sell basic Tier 1 starter gear (laser, tool, shield, repair kit).\n'
          '• Industrial ships, Tier 2+ equipment, and boosters are crafted at Stations and traded in isolated city markets.\n'
          '• Pinch with two fingers in space to zoom (0.6×–2.5×) and inspect close-up hull details.',
      titleEs: 'Economía de Jugadores y Cámara',
      bodyEs:
          '• Las Ciudades Capitales solo venden equipo básico Nivel 1 (láser, herramienta, escudo y kit de reparación).\n'
          '• Las naves industriales, equipo N2+ y potenciadores se fabrican en Estaciones y se comercian en mercados locales aislados.\n'
          '• Pellizca con dos dedos en el espacio para hacer zoom (0.6×–2.5×) y ver detalles cercanos del casco.',
    ),
  ];

  static String t(GameLanguage lang, String en, String es) =>
      lang == GameLanguage.es ? es : en;

  static String hudMenu(GameLanguage lang) => t(lang, 'MENU', 'MENÚ');
  static String actionClose(GameLanguage lang) => t(lang, 'CLOSE', 'CERRAR');
  static String actionBack(GameLanguage lang) => t(lang, 'BACK', 'ATRÁS');
  static String ctrlDock(GameLanguage lang) => t(lang, 'DOCK', 'ATRACAR');
  static String stationUndock(GameLanguage lang) =>
      t(lang, 'UNDOCK', 'DESATRACAR');

  static String adminActiveBanner(GameLanguage lang) => t(
        lang,
        'Admin Mode Active (Free Testing)',
        'Modo Admin Activo (Pruebas Gratis)',
      );

  static String adminFreeLabel(GameLanguage lang) =>
      t(lang, 'FREE (ADMIN)', 'GRATIS (ADMIN)');

  static String adminToggleLabel(bool isAdmin) =>
      isAdmin ? 'ADMIN: ON' : 'ADMIN: OFF';

  static String shipRoleName(ShipRole role, GameLanguage lang) {
    switch (role) {
      case ShipRole.capsule:
        return t(lang, 'Capsule', 'Cápsula');
      case ShipRole.industrial:
        return t(lang, 'Industrial', 'Industrial');
      case ShipRole.combat:
        return t(lang, 'Combat', 'Combate');
    }
  }

  static String industrialTypeName(IndustrialType type, GameLanguage lang) {
    switch (type) {
      case IndustrialType.miner:
        return t(lang, 'Miner', 'Minero');
      case IndustrialType.plasmaCollector:
        return t(lang, 'Plasma Collector', 'Recolector de Plasma');
      case IndustrialType.mediumHauler:
        return t(lang, 'Medium Hauler', 'Carguero Mediano');
      case IndustrialType.heavyHauler:
        return t(lang, 'Heavy Hauler', 'Carguero Pesado');
    }
  }

  static String craftQualityName(CraftQuality quality, GameLanguage lang) {
    switch (quality) {
      case CraftQuality.standard:
        return t(lang, 'Standard', 'Estándar');
      case CraftQuality.reinforced:
        return t(lang, 'Reinforced', 'Reforzado');
      case CraftQuality.excellent:
        return t(lang, 'Excellent', 'Excelente');
      case CraftQuality.masterwork:
        return t(lang, 'Masterwork', 'Obra Maestra');
    }
  }

  static String skillName(SkillType skill, GameLanguage lang) {
    switch (skill) {
      case SkillType.mining:
        return t(lang, 'Mining', 'Minería');
      case SkillType.salvaging:
        return t(lang, 'Salvaging', 'Chatarreo');
      case SkillType.refining:
        return t(lang, 'Refining', 'Refinado');
      case SkillType.navalEngineering:
        return t(lang, 'Naval Engineering', 'Ingeniería Naval');
      case SkillType.gearManufacturing:
        return t(lang, 'Gear Manufacturing', 'Fabricación de Equipo');
      case SkillType.synthesis:
        return t(lang, 'Synthesis', 'Síntesis');
      case SkillType.weaponry:
        return t(lang, 'Weaponry', 'Armamento');
      case SkillType.shields:
        return t(lang, 'Shields', 'Escudos');
      case SkillType.piloting:
        return t(lang, 'Piloting', 'Pilotaje');
    }
  }

  static String skillActiveBonus(SkillType skill, int level, GameLanguage lang) {
    final int lv = level.clamp(kPilotSkillMinLevel, kPilotSkillMaxLevel);
    final double frac = (lv - 1) / 98.0;
    switch (skill) {
      case SkillType.mining:
      case SkillType.salvaging:
        final String sec = baseExtractionSeconds(lv).toStringAsFixed(1);
        return lang == GameLanguage.es ? '${sec}s/carga' : '${sec}s/charge';
      case SkillType.refining:
        final int feePct = (refiningFeeDiscountPct(lv) * 100.0).round();
        final int bonusPct = (refiningBonusOutputChance(lv) * 100.0).round();
        return lang == GameLanguage.es
            ? '-$feePct% tarifa • +$bonusPct% extra'
            : '-$feePct% fee • +$bonusPct% bonus';
      case SkillType.navalEngineering:
        final int pct = (frac * 25.0).round();
        return lang == GameLanguage.es
            ? '+$pct% calidad nave'
            : '+$pct% hull quality';
      case SkillType.gearManufacturing:
        final int pct = (frac * 25.0).round();
        return lang == GameLanguage.es
            ? '+$pct% calidad equipo'
            : '+$pct% gear quality';
      case SkillType.synthesis:
        final int pct = (frac * 25.0).round();
        return lang == GameLanguage.es
            ? '+$pct% potencia boost'
            : '+$pct% booster power';
      case SkillType.weaponry:
        final int pct = (frac * 20.0).round();
        return lang == GameLanguage.es
            ? '+$pct% daño armas'
            : '+$pct% weapon DMG';
      case SkillType.shields:
        final int pct = (frac * 20.0).round();
        return lang == GameLanguage.es
            ? '+$pct% cap. escudo'
            : '+$pct% shield cap';
      case SkillType.piloting:
        final int pct = (frac * 15.0).round();
        return lang == GameLanguage.es ? '+$pct% velocidad' : '+$pct% speed';
    }
  }

  static String resourceName(ResourceType res, GameLanguage lang) {
    if (lang == GameLanguage.en) return ResourceCatalog.of(res).displayName;
    switch (res) {
      case ResourceType.starCobalt:
        return 'Cobalto Estelar';
      case ResourceType.aetherQuartz:
        return 'Cuarzo Éter';
      case ResourceType.veridianOre:
        return 'Mena Veridiana';
      case ResourceType.bioResin:
        return 'Biorresina';
      case ResourceType.crimsonIron:
        return 'Hierro Carmesí';
      case ResourceType.magmaStone:
        return 'Piedra de Magma';
      case ResourceType.yellowCrystal:
        return 'Cristal Amarillo';
      case ResourceType.xenoSalvage:
        return 'Chatarra Xeno';
      case ResourceType.xenoCore:
        return 'Núcleo Xeno';
    }
  }

  static String refinedName(ResourceType res, GameLanguage lang) {
    if (lang == GameLanguage.en) return ResourceCatalog.of(res).refinedName;
    switch (res) {
      case ResourceType.starCobalt:
        return 'Aleación de Cobalto';
      case ResourceType.aetherQuartz:
        return 'Cristal de Éter';
      case ResourceType.veridianOre:
        return 'Lingote Veridiano';
      case ResourceType.bioResin:
        return 'Biopolímero';
      case ResourceType.crimsonIron:
        return 'Acero Carmesí';
      case ResourceType.magmaStone:
        return 'Núcleo Térmico';
      case ResourceType.yellowCrystal:
        return 'Cristal Amarillo Ref.';
      case ResourceType.xenoSalvage:
        return 'Aleación Xeno';
      case ResourceType.xenoCore:
        return 'Núcleo Xeno';
    }
  }

  static String refinesInLabel(ResourceType res, GameLanguage lang) {
    final ResourceSpec spec = ResourceCatalog.of(res);
    if (spec.isDirectUse || spec.refinesInSectorIds.isEmpty) {
      return t(lang, 'Direct use (Boosters)', 'Uso directo (Potenciadores)');
    }
    final String cities = spec.refinesInSectorIds.join(', ');
    return lang == GameLanguage.es ? 'Se refina en $cities' : 'Refines in $cities';
  }

  static String stationHeaderTitle(
    StationKind kind,
    SectorNode sector,
    GameLanguage lang,
  ) {
    if (lang == GameLanguage.es) {
      if (kind == StationKind.refinery) return 'REFINERÍA ${sector.id}';
      if (kind == StationKind.crafting) return 'FABRICACIÓN ${sector.id}';
      return sector.isNucleus
          ? 'MERCADO NEGRO NÚCLEO 0-0'
          : 'MERCADO LOCAL ${sector.id}';
    }
    if (kind == StationKind.refinery) return '${sector.id} REFINING STATION';
    if (kind == StationKind.crafting) return '${sector.id} CRAFTING STATION';
    return sector.isNucleus
        ? '0-0 NUCLEUS BLACK MARKET'
        : '${sector.id} ISOLATED LOCAL MARKET';
  }

  static String recipeName(StationCraftRecipeId id, GameLanguage lang) {
    switch (id) {
      case StationCraftRecipeId.shipMiner:
        return t(lang, 'Prospector Mk I (Miner)', 'Prospector Mk I (Minero)');
      case StationCraftRecipeId.shipPlasmaCollector:
        return t(
          lang,
          'Siphon Mk I (Plasma Collector)',
          'Sifón Mk I (Recolector Plasma)',
        );
      case StationCraftRecipeId.shipMediumHauler:
        return t(
          lang,
          'Freighter M-1 (Medium Hauler)',
          'Carguero M-1 (Mediano)',
        );
      case StationCraftRecipeId.shipHeavyHauler:
        return t(
          lang,
          'Leviathan H-1 (Heavy Hauler)',
          'Leviatán H-1 (Carguero Pesado)',
        );
      case StationCraftRecipeId.gearLaserT2:
        return t(lang, 'LF-2 Pulse Laser (T2)', 'Láser de Pulso LF-2 (N2)');
      case StationCraftRecipeId.gearLaserT3:
        return t(lang, 'LF-3 Heavy Laser (T3)', 'Láser Pesado LF-3 (N3)');
      case StationCraftRecipeId.gearShieldT2:
        return t(lang, 'SG-2 Barrier Shield (T2)', 'Escudo Barrera SG-2 (N2)');
      case StationCraftRecipeId.gearShieldT3:
        return t(lang, 'SG-3 Aegis Shield (T3)', 'Escudo Égida SG-3 (N3)');
      case StationCraftRecipeId.gearMiningToolT2:
        return t(lang, 'Mk II Mining Beam', 'Rayo Minero Mk II');
      case StationCraftRecipeId.gearSalvageToolT2:
        return t(lang, 'Mk II Salvage Cutter', 'Cortador de Chatarra Mk II');
      case StationCraftRecipeId.synthRepairKit:
        return t(lang, 'Nanite Repair Kit', 'Kit de Reparación Nanite');
      case StationCraftRecipeId.synthDamageBooster:
        return t(lang, 'Damage Overdrive Booster', 'Potenciador de Daño');
      case StationCraftRecipeId.synthShieldBooster:
        return t(lang, 'Shield Capacitor Booster', 'Potenciador de Escudo');
      case StationCraftRecipeId.synthSpeedBooster:
        return t(
          lang,
          'Ion Thruster Speed Booster',
          'Potenciador de Velocidad',
        );
    }
  }

  static String recipeSummary(StationCraftRecipeId id, GameLanguage lang) {
    switch (id) {
      case StationCraftRecipeId.shipMiner:
        return t(
          lang,
          'HP 220 • SHD 140 • Cargo 240 • 1x Solid / 2x Plasma',
          'Casco 220 • Esc 140 • Carga 240 • 1x Sólido / 2x Plasma',
        );
      case StationCraftRecipeId.shipPlasmaCollector:
        return t(
          lang,
          'HP 200 • SHD 180 • Cargo 220 • 1x Plasma / 2x Solid',
          'Casco 200 • Esc 180 • Carga 220 • 1x Plasma / 2x Sólido',
        );
      case StationCraftRecipeId.shipMediumHauler:
        return t(
          lang,
          'HP 360 • SHD 240 • Cargo 650 • 3x Extraction',
          'Casco 360 • Esc 240 • Carga 650 • 3x Extracción',
        );
      case StationCraftRecipeId.shipHeavyHauler:
        return t(
          lang,
          'HP 620 • SHD 440 • Cargo 1400 • 5x Extraction',
          'Casco 620 • Esc 440 • Carga 1400 • 5x Extracción',
        );
      case StationCraftRecipeId.gearLaserT2:
        return t(
          lang,
          '+28 DMG (+0%..+15% quality) • Inventory',
          '+28 DAÑO (+0%..+15% calidad) • Inventario',
        );
      case StationCraftRecipeId.gearLaserT3:
        return t(
          lang,
          '+60 DMG (+0%..+15% quality) • Inventory',
          '+60 DAÑO (+0%..+15% calidad) • Inventario',
        );
      case StationCraftRecipeId.gearShieldT2:
        return t(
          lang,
          '+180 SHD (+0%..+15% quality) • Inventory',
          '+180 ESC (+0%..+15% calidad) • Inventario',
        );
      case StationCraftRecipeId.gearShieldT3:
        return t(
          lang,
          '+380 SHD (+0%..+15% quality) • Inventory',
          '+380 ESC (+0%..+15% calidad) • Inventario',
        );
      case StationCraftRecipeId.gearMiningToolT2:
        return t(
          lang,
          '+15% Mining Speed (+0%..+15% quality) • Tool',
          '+15% Vel. Minería (+0%..+15% calidad) • Herram.',
        );
      case StationCraftRecipeId.gearSalvageToolT2:
        return t(
          lang,
          '+15% Salvage Speed (+0%..+15% quality) • Tool',
          '+15% Vel. Chatarreo (+0%..+15% calidad) • Herram.',
        );
      case StationCraftRecipeId.synthRepairKit:
        return t(
          lang,
          'Restores 250 Hull/Shield (+0%..+15% quality)',
          'Restaura 250 Casco/Escudo (+0%..+15% calidad)',
        );
      case StationCraftRecipeId.synthDamageBooster:
        return t(
          lang,
          '+12% Weapon DMG • 10m (+0%..+15% quality)',
          '+12% Daño Armas • 10m (+0%..+15% calidad)',
        );
      case StationCraftRecipeId.synthShieldBooster:
        return t(
          lang,
          '+15% Shield Cap • 10m (+0%..+15% quality)',
          '+15% Cap. Escudo • 10m (+0%..+15% calidad)',
        );
      case StationCraftRecipeId.synthSpeedBooster:
        return t(
          lang,
          '+10% Ship Speed • 10m (+0%..+15% quality)',
          '+10% Velocidad • 10m (+0%..+15% calidad)',
        );
    }
  }

  static String craftOutcomeStatSummary(
    StationCraftRecipeSpec spec,
    CraftQuality quality,
    GameLanguage lang,
  ) {
    final bool es = lang == GameLanguage.es;
    switch (spec.outputKind) {
      case CraftOutputKind.industrialShip:
        final ShipHullDefinition? hull = spec.shipHullDefinition;
        final int hp = quality.applyBonusInt((hull?.baseHull ?? 220.0).round());
        final int shd =
            quality.applyBonusInt((hull?.baseShield ?? 140.0).round());
        final int cargo = quality.applyBonusInt(hull?.baseCargo ?? 240);
        return es
            ? 'Casco $hp • Esc $shd • Carga $cargo'
            : 'HP $hp • SHD $shd • Cargo $cargo';
      case CraftOutputKind.equipmentGear:
        final EquipmentItemId? eqId = spec.resultEquipmentId;
        if (eqId != null &&
            EquipmentCatalog.of(eqId).category == EquipmentCategory.laser) {
          final int dmg = quality
              .applyBonusInt(EquipmentCatalog.of(eqId).damageBonus.round());
          return es ? '+$dmg DAÑO' : '+$dmg DMG';
        }
        final int shd = quality.applyBonusInt(
          (eqId != null
                  ? EquipmentCatalog.of(eqId).shieldBonus
                  : spec.basePrimaryStat)
              .round(),
        );
        return es ? '+$shd ESC' : '+$shd SHD';
      case CraftOutputKind.extractionTool:
        final String pct =
            quality.applyBonus(spec.basePrimaryStat).toStringAsFixed(1);
        if (spec.id == StationCraftRecipeId.gearMiningToolT2) {
          return es ? '+$pct% Vel. Minería' : '+$pct% Mining Speed';
        }
        return es ? '+$pct% Vel. Chatarreo' : '+$pct% Salvage Speed';
      case CraftOutputKind.consumableRepair:
        final int heal = quality.applyBonusInt(spec.basePrimaryStat.round());
        return es ? '+$heal Casco/Escudo' : '+$heal Hull/Shield';
      case CraftOutputKind.consumableBooster:
        final String pct =
            quality.applyBonus(spec.basePrimaryStat).toStringAsFixed(1);
        final int mins = quality.applyBonusInt(spec.baseDurationMinutes);
        if (spec.id == StationCraftRecipeId.synthDamageBooster) {
          return es ? '+$pct% Daño • ${mins}m' : '+$pct% DMG • ${mins}m';
        }
        if (spec.id == StationCraftRecipeId.synthShieldBooster) {
          return es ? '+$pct% Escudo • ${mins}m' : '+$pct% Shield • ${mins}m';
        }
        return es ? '+$pct% Velocidad • ${mins}m' : '+$pct% Speed • ${mins}m';
    }
  }

  static String basicVendorItemName(
    BasicVendorItemId item,
    GameLanguage lang,
  ) {
    switch (item) {
      case BasicVendorItemId.basicLaserT1:
        return t(
          lang,
          'Basic Tier 1 Laser (LF-1)',
          'Láser Básico Nivel 1 (LF-1)',
        );
      case BasicVendorItemId.basicToolT1:
        return t(
          lang,
          'Basic Mining / Salvage Tool (T1)',
          'Herramienta Básica Minería/Chatarreo (N1)',
        );
      case BasicVendorItemId.basicShieldT1:
        return t(
          lang,
          'Basic Tier 1 Shield (SG-1)',
          'Escudo Básico Nivel 1 (SG-1)',
        );
      case BasicVendorItemId.basicRepairKit:
        return t(
          lang,
          'Basic Repair Kit (T1)',
          'Kit de Reparación Básico (N1)',
        );
    }
  }

  static String basicVendorItemDesc(
    BasicVendorItemId item,
    GameLanguage lang,
  ) {
    switch (item) {
      case BasicVendorItemId.basicLaserT1:
        return t(
          lang,
          '+12 DMG • Standard T1 ship laser added to Hangar',
          '+12 DAÑO • Láser estándar N1 añadido al Hangar',
        );
      case BasicVendorItemId.basicToolT1:
        return t(
          lang,
          'T1 extraction tool for resource nodes & alien wrecks',
          'Herramienta N1 para nodos de recursos y pecios',
        );
      case BasicVendorItemId.basicShieldT1:
        return t(
          lang,
          '+80 SHD • Standard T1 shield generator added to Hangar',
          '+80 ESC • Generador de escudo N1 añadido al Hangar',
        );
      case BasicVendorItemId.basicRepairKit:
        return t(
          lang,
          'Restores 100% Hull & Shield immediately on purchase',
          'Restaura 100% Casco y Escudo al instante al comprar',
        );
            }
  }
}

class GameStringsView {
  const GameStringsView(this.lang);
  final GameLanguage lang;

  String get sectorMapTitle =>
      GameStrings.t(lang, 'SECTOR MAP', 'MAPA DE SECTOR');

  String get tapToSteerHint => GameStrings.t(
        lang,
        'Tap map to autopilot',
        'Toca el mapa para piloto automático',
      );

  String get closeLabel => GameStrings.actionClose(lang);
}
