import 'package:flutter/material.dart';

import '../game/dark_orbit_game.dart';

// =============================================================================
// DOCKED STATION OVERLAY (REFINERY • CRAFTING • MARKET / BASIC VENDOR)
// =============================================================================
class StationOverlay extends StatefulWidget {
  const StationOverlay({super.key, required this.game});

  final DarkOrbitGame game;

  @override
  State<StationOverlay> createState() => _StationOverlayState();
}

class _StationOverlayState extends State<StationOverlay> {
  CraftingDiscipline _craftTab = CraftingDiscipline.navalEngineering;
  int _marketSubTab = 0; // 0 = Sell, 1 = Buy, 2 = Basic Vendor / Black Gear

  @override
  Widget build(BuildContext context) {
    final DarkOrbitGame game = widget.game;
    final GameLanguage lang = game.language;
    final SectorNode sector = game.currentSector;
    final StationKind activeTab = game.openStationTab;
    final bool isNucleus = sector.isNucleus;

    return Material(
      color: const Color(0xDE03060E),
      child: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 780.0, maxHeight: 580.0),
            child: Container(
              margin: const EdgeInsets.all(10.0),
              padding: const EdgeInsets.all(14.0),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: <Color>[Color(0xFF0A1424), Color(0xFF060B16)],
                ),
                borderRadius: BorderRadius.circular(16.0),
                border: Border.all(
                  color: const Color(0xFF00E5FF).withOpacity(0.55),
                  width: 1.3,
                ),
              ),
              child: Column(
                children: <Widget>[
                  // Header
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              GameStrings.stationHeaderTitle(
                                activeTab,
                                sector,
                                lang,
                              ),
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 15.5,
                                fontWeight: FontWeight.w900,
                                letterSpacing: 0.7,
                              ),
                            ),
                            Text(
                              '${game.credits} CR  •  ${GameStrings.t(lang, 'CARGO', 'CARGA')} ${game.totalCargoUsed}/${game.currentCargoCapacity}${game.isAdminMode ? '  •  ADMIN FREE' : ''}',
                              style: TextStyle(
                                color: game.isAdminMode
                                    ? const Color(0xFF4ADE80)
                                    : const Color(0xFFFBBF24),
                                fontSize: 11.5,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                      ElevatedButton.icon(
                        onPressed: game.closeStation,
                        icon: const Icon(Icons.logout_rounded, size: 16.0),
                        label: Text(
                          GameStrings.stationUndock(lang),
                          style: const TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF1A253B),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12.0,
                            vertical: 8.0,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10.0),

                  // 3 Main Station Tabs (Capital Cities only; Nucleus = Market only)
                  if (!isNucleus) ...<Widget>[
                    Row(
                      children: <Widget>[
                        Expanded(
                          child: _StationTabBtn(
                            label: GameStrings.t(lang, 'REFINERY', 'REFINERÍA'),
                            accent: const Color(0xFFFB923C),
                            selected: activeTab == StationKind.refinery,
                            onTap: () {
                              game.switchStationTab(StationKind.refinery);
                              setState(() {});
                            },
                          ),
                        ),
                        const SizedBox(width: 8.0),
                        Expanded(
                          child: _StationTabBtn(
                            label:
                                GameStrings.t(lang, 'CRAFTING', 'FABRICACIÓN'),
                            accent: const Color(0xFF34D399),
                            selected: activeTab == StationKind.crafting,
                            onTap: () {
                              game.switchStationTab(StationKind.crafting);
                              setState(() {});
                            },
                          ),
                        ),
                        const SizedBox(width: 8.0),
                        Expanded(
                          child: _StationTabBtn(
                            label: GameStrings.t(lang, 'MARKET', 'MERCADO'),
                            accent: const Color(0xFFFACC15),
                            selected: activeTab == StationKind.localMarket,
                            onTap: () {
                              game.switchStationTab(StationKind.localMarket);
                              setState(() {});
                            },
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10.0),
                  ],

                  // Tab Body
                  Expanded(
                    child: activeTab == StationKind.refinery && !isNucleus
                        ? _buildRefineryTab(game, lang)
                        : activeTab == StationKind.crafting && !isNucleus
                            ? _buildCraftingTab(game, lang)
                            : _buildMarketTab(game, lang, isNucleus),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // 1. REFINERY TAB (CLOCKWISE LOOP C1->C2->C3->C1 + ADMIN FREE BYPASS)
  // ---------------------------------------------------------------------------
  Widget _buildRefineryTab(DarkOrbitGame game, GameLanguage lang) {
    final int refLv = game.skillLevelOf(SkillType.refining);
    final int discountPct = (refiningFeeDiscountPct(refLv) * 100.0).round();
    final int bonusPct = (refiningBonusOutputChance(refLv) * 100.0).round();
    final bool admin = game.isAdminMode;

    final List<ResourceType> list = ResourceType.values
        .where((ResourceType r) =>
            ResourceCatalog.of(r).isRefinable &&
            (admin || (game.rawCargo[r] ?? 0) > 0 || (game.refinedCargo[r] ?? 0) > 0))
        .toList();
    final List<ResourceType> displayList = list.isEmpty
        ? ResourceType.values
            .where((ResourceType r) => ResourceCatalog.of(r).isRefinable)
            .toList()
        : list;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 7.0),
          decoration: BoxDecoration(
            color: const Color(0xFF131D31),
            borderRadius: BorderRadius.circular(9.0),
            border: Border.all(color: const Color(0x55FB923C)),
          ),
          child: Text(
            '${GameStrings.t(lang, 'Clockwise Loop: C1→C2 • C2→C3 • C3→C1 • Crystal/Salvage→Any', 'Ciclo Horario: C1→C2 • C2→C3 • C3→C1 • Cristal/Chatarra→Cualquier ciudad')}\n'
            '${GameStrings.skillName(SkillType.refining, lang)} LV $refLv: -$discountPct% ${GameStrings.t(lang, 'fee', 'tarifa')} • +$bonusPct% ${GameStrings.t(lang, 'bonus output', 'producción extra')}',
            style: const TextStyle(color: Color(0xFFFED7AA), fontSize: 11.2),
          ),
        ),
        const SizedBox(height: 8.0),
        Expanded(
          child: ListView.separated(
            itemCount: displayList.length,
            separatorBuilder: (_, __) => const SizedBox(height: 7.0),
            itemBuilder: (_, int i) {
              final ResourceType res = displayList[i];
              final ResourceSpec spec = ResourceCatalog.of(res);
              final int rawOwned = game.rawCargo[res] ?? 0;
              final int refOwned = game.refinedCargo[res] ?? 0;
              final bool rightCity = spec.canRefineIn(game.currentSectorId);
              final bool canRefineHere = admin || rightCity;
              final int fee =
                  admin ? 0 : spec.effectiveRefiningFeePerUnit(refLv);
              final int maxByRaw =
                  rawOwned ~/ ResourceCatalog.rawRequiredPerRefine;
              final int maxByCreds = fee > 0 ? game.credits ~/ fee : maxByRaw;
              final int maxCraftable =
                  admin ? 10 : (maxByRaw < maxByCreds ? maxByRaw : maxByCreds);

              return Container(
                padding: const EdgeInsets.all(10.0),
                decoration: BoxDecoration(
                  color: const Color(0xFF0E1A2F),
                  borderRadius: BorderRadius.circular(11.0),
                  border: Border.all(
                    color: canRefineHere
                        ? spec.color.withOpacity(0.45)
                        : const Color(0xFF334155),
                  ),
                ),
                child: Row(
                  children: <Widget>[
                    Container(
                      width: 10.0,
                      height: 36.0,
                      decoration: BoxDecoration(
                        color: spec.color,
                        borderRadius: BorderRadius.circular(4.0),
                      ),
                    ),
                    const SizedBox(width: 10.0),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            '${GameStrings.resourceName(res, lang)} → ${GameStrings.refinedName(res, lang)}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12.8,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 2.0),
                          Text(
                            '${GameStrings.t(lang, 'Raw', 'Bruto')}: $rawOwned  •  ${GameStrings.t(lang, 'Refined', 'Refinado')}: $refOwned  •  '
                            '${admin ? GameStrings.adminFreeLabel(lang) : '2 Raw + $fee CR'}',
                            style: TextStyle(
                              color: admin
                                  ? const Color(0xFF4ADE80)
                                  : const Color(0xFF94A3B8),
                              fontSize: 11.0,
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (!canRefineHere)
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8.0,
                          vertical: 5.0,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0x33FF4D6D),
                          borderRadius: BorderRadius.circular(7.0),
                          border: Border.all(color: const Color(0x88FF4D6D)),
                        ),
                        child: Text(
                          GameStrings.refinesInLabel(res, lang),
                          style: const TextStyle(
                            color: Color(0xFFFF8A80),
                            fontSize: 10.5,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      )
                    else
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          _ActionPillBtn(
                            label: 'x1',
                            enabled: admin || maxCraftable >= 1,
                            color: const Color(0xFFFB923C),
                            onTap: () {
                              game.refineResource(res, 1);
                              setState(() {});
                            },
                          ),
                          const SizedBox(width: 6.0),
                          _ActionPillBtn(
                            label: admin ? '+10' : GameStrings.t(lang, 'MAX', 'MÁX'),
                            enabled: admin || maxCraftable >= 1,
                            color: const Color(0xFF4ADE80),
                            onTap: () {
                              game.refineResource(
                                res,
                                admin ? 10 : maxCraftable,
                              );
                              setState(() {});
                            },
                          ),
                        ],
                      ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  // ---------------------------------------------------------------------------
  // 2. CRAFTING TAB (3 SUB-TABS + QUALITY ODDS + OUTCOME BANNER)
  // ---------------------------------------------------------------------------
  Widget _buildCraftingTab(DarkOrbitGame game, GameLanguage lang) {
    final bool admin = game.isAdminMode;
    final SkillType skill = _craftTab.skill;
    final int lv = game.skillLevelOf(skill);
    final CraftQualityOdds odds = computeCraftQualityOdds(lv);
    final List<StationCraftRecipeSpec> recipes =
        StationCraftCatalog.forDiscipline(_craftTab);
    final CraftOutcome? outcome = game.lastCraftOutcome;

    return Column(
      children: <Widget>[
        Row(
          children: <Widget>[
            Expanded(
              child: _StationTabBtn(
                label: GameStrings.t(lang, 'Naval Eng.', 'Ing. Naval'),
                accent: CraftingDiscipline.navalEngineering.accentColor,
                selected: _craftTab == CraftingDiscipline.navalEngineering,
                onTap: () => setState(
                  () => _craftTab = CraftingDiscipline.navalEngineering,
                ),
              ),
            ),
            const SizedBox(width: 6.0),
            Expanded(
              child: _StationTabBtn(
                label: GameStrings.t(lang, 'Gear Mfg.', 'Fab. Equipo'),
                accent: CraftingDiscipline.gearManufacturing.accentColor,
                selected: _craftTab == CraftingDiscipline.gearManufacturing,
                onTap: () => setState(
                  () => _craftTab = CraftingDiscipline.gearManufacturing,
                ),
              ),
            ),
            const SizedBox(width: 6.0),
            Expanded(
              child: _StationTabBtn(
                label: GameStrings.t(lang, 'Synthesis', 'Síntesis'),
                accent: CraftingDiscipline.synthesis.accentColor,
                selected: _craftTab == CraftingDiscipline.synthesis,
                onTap: () =>
                    setState(() => _craftTab = CraftingDiscipline.synthesis),
              ),
            ),
          ],
        ),
        const SizedBox(height: 7.0),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 6.0),
          decoration: BoxDecoration(
            color: const Color(0xFF101C30),
            borderRadius: BorderRadius.circular(8.0),
            border: Border.all(color: _craftTab.accentColor.withOpacity(0.4)),
          ),
          child: Text(
            '${GameStrings.skillName(skill, lang)} LV $lv  •  '
            'Std ${odds.standardPct}% / +5% ${odds.reinforcedPct}% / +10% ${odds.excellentPct}% / +15% ${odds.masterworkPct}%',
            style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 11.0),
          ),
        ),
        if (outcome != null) ...<Widget>[
          const SizedBox(height: 6.0),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 7.0),
            decoration: BoxDecoration(
              color: outcome.quality.accentColor.withOpacity(0.16),
              borderRadius: BorderRadius.circular(8.0),
              border: Border.all(color: outcome.quality.accentColor),
            ),
            child: Text(
              '${GameStrings.t(lang, 'CRAFTED', 'FABRICADO')}: ${GameStrings.recipeName(outcome.recipeId, lang)} '
              '[${GameStrings.craftQualityName(outcome.quality, lang)} ${outcome.quality.bonusLabel}] • ${outcome.statSummary}'
              '${outcome.doubledOutput ? ' • 2× BONUS!' : ''}'
              '${outcome.refundedUnits > 0 ? ' • +${outcome.refundedUnits} mat.' : ''}',
              style: TextStyle(
                color: outcome.quality.accentColor,
                fontSize: 11.5,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
        ],
        const SizedBox(height: 8.0),
        Expanded(
          child: ListView.separated(
            itemCount: recipes.length,
            separatorBuilder: (_, __) => const SizedBox(height: 7.0),
            itemBuilder: (_, int i) {
              final StationCraftRecipeSpec spec = recipes[i];
              final bool canCraft = game.canCraftRecipe(spec.id);
              return Container(
                padding: const EdgeInsets.all(10.0),
                decoration: BoxDecoration(
                  color: const Color(0xFF0E1A2F),
                  borderRadius: BorderRadius.circular(11.0),
                  border: Border.all(color: spec.accentColor.withOpacity(0.4)),
                ),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            GameStrings.recipeName(spec.id, lang),
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 13.0,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 2.0),
                          Text(
                            GameStrings.recipeSummary(spec.id, lang),
                            style: const TextStyle(
                              color: Color(0xFF94A3B8),
                              fontSize: 11.0,
                            ),
                          ),
                          const SizedBox(height: 4.0),
                          Wrap(
                            spacing: 6.0,
                            runSpacing: 4.0,
                            children: spec.ingredients.map((CraftMaterialCost c) {
                              final int have = c.isRefined
                                  ? (game.refinedCargo[c.resource] ?? 0)
                                  : (game.rawCargo[c.resource] ?? 0);
                              final bool ok = admin || have >= c.amount;
                              final String matName = c.isRefined
                                  ? GameStrings.refinedName(c.resource, lang)
                                  : GameStrings.resourceName(c.resource, lang);
                              return Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 6.0,
                                  vertical: 2.0,
                                ),
                                decoration: BoxDecoration(
                                  color: ok
                                      ? const Color(0x224ADE80)
                                      : const Color(0x22FF4D6D),
                                  borderRadius: BorderRadius.circular(5.0),
                                ),
                                child: Text(
                                  admin
                                      ? '$matName: 0 (ADMIN)'
                                      : '$matName: $have/${c.amount}',
                                  style: TextStyle(
                                    color: ok
                                        ? const Color(0xFF86EFAC)
                                        : const Color(0xFFFF8A80),
                                    fontSize: 10.0,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              );
                            }).toList(),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8.0),
                    _ActionPillBtn(
                      label: admin
                          ? GameStrings.t(lang, 'FREE', 'GRATIS')
                          : GameStrings.t(lang, 'CRAFT', 'FABRICAR'),
                      enabled: canCraft,
                      color: spec.accentColor,
                      onTap: () {
                        game.craftStationRecipe(spec.id);
                        setState(() {});
                      },
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  // ---------------------------------------------------------------------------
  // 3. MARKET TAB (SELL CARGO • BUY RESOURCES • BASIC VENDOR / BLACK MARKET)
  // ---------------------------------------------------------------------------
  Widget _buildMarketTab(
    DarkOrbitGame game,
    GameLanguage lang,
    bool isNucleus,
  ) {
    final LocalMarketState market =
        game.getOrCreateLocalMarket(game.currentSectorId);
    final bool admin = game.isAdminMode;

    return Column(
      children: <Widget>[
        Row(
          children: <Widget>[
            Expanded(
              child: _StationTabBtn(
                label: GameStrings.t(lang, 'SELL CARGO', 'VENDER'),
                accent: const Color(0xFF4ADE80),
                selected: _marketSubTab == 0,
                onTap: () => setState(() => _marketSubTab = 0),
              ),
            ),
            const SizedBox(width: 6.0),
            Expanded(
              child: _StationTabBtn(
                label: GameStrings.t(lang, 'BUY RESOURCES', 'COMPRAR'),
                accent: const Color(0xFF38BDF8),
                selected: _marketSubTab == 1,
                onTap: () => setState(() => _marketSubTab = 1),
              ),
            ),
            const SizedBox(width: 6.0),
            Expanded(
              child: _StationTabBtn(
                label: isNucleus
                    ? GameStrings.t(lang, 'BLACK GEAR', 'MERC. NEGRO')
                    : GameStrings.t(lang, 'BASIC VENDOR', 'VENDEDOR BÁSICO'),
                accent: const Color(0xFFFACC15),
                selected: _marketSubTab == 2,
                onTap: () => setState(() => _marketSubTab = 2),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8.0),
        Expanded(
          child: _marketSubTab == 0
              ? _buildSellCargoList(game, market, lang)
              : _marketSubTab == 1
                  ? _buildBuyResourceList(game, market, lang, admin)
                  : isNucleus
                      ? _buildBlackMarketGearList(game, market, lang, admin)
                      : _buildBasicVendorList(game, lang, admin),
        ),
      ],
    );
  }

  Widget _buildSellCargoList(
    DarkOrbitGame game,
    LocalMarketState market,
    GameLanguage lang,
  ) {
    final List<MarketResourceListing> sellable = market.resourceListings
        .where((MarketResourceListing l) {
          final int owned = l.isRefined
              ? (game.refinedCargo[l.resourceType] ?? 0)
              : (game.rawCargo[l.resourceType] ?? 0);
          return owned > 0;
        })
        .toList();

    if (sellable.isEmpty) {
      return Center(
        child: Text(
          GameStrings.t(
            lang,
            'No cargo to sell. Mine resources or refine them first.',
            'Sin carga para vender. Mina recursos o refínalos primero.',
          ),
          style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 13.0),
        ),
      );
    }

    return ListView.separated(
      itemCount: sellable.length,
      separatorBuilder: (_, __) => const SizedBox(height: 7.0),
      itemBuilder: (_, int i) {
        final MarketResourceListing l = sellable[i];
        final int owned = l.isRefined
            ? (game.refinedCargo[l.resourceType] ?? 0)
            : (game.rawCargo[l.resourceType] ?? 0);
        final String name = l.isRefined
            ? GameStrings.refinedName(l.resourceType, lang)
            : GameStrings.resourceName(l.resourceType, lang);
        return Container(
          padding: const EdgeInsets.all(10.0),
          decoration: BoxDecoration(
            color: const Color(0xFF0E1A2F),
            borderRadius: BorderRadius.circular(10.0),
            border: Border.all(
              color: ResourceCatalog.of(l.resourceType).color.withOpacity(0.4),
            ),
          ),
          child: Row(
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      '$name (${l.isRefined ? GameStrings.t(lang, 'Refined', 'Refinado') : GameStrings.t(lang, 'Raw', 'Bruto')})',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 12.8,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      '${GameStrings.t(lang, 'Cargo', 'Bodega')}: $owned  •  ${l.sellPrice} CR/u',
                      style: const TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 11.0,
                      ),
                    ),
                  ],
                ),
              ),
              _ActionPillBtn(
                label: 'x1',
                enabled: owned >= 1,
                color: const Color(0xFF4ADE80),
                onTap: () {
                  game.sellMarketResource(l.resourceType, l.isRefined, 1);
                  setState(() {});
                },
              ),
              const SizedBox(width: 6.0),
              _ActionPillBtn(
                label: GameStrings.t(lang, 'ALL', 'TODO'),
                enabled: owned >= 1,
                color: const Color(0xFFFACC15),
                onTap: () {
                  game.sellMarketResource(l.resourceType, l.isRefined, owned);
                  setState(() {});
                },
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildBuyResourceList(
    DarkOrbitGame game,
    LocalMarketState market,
    GameLanguage lang,
    bool admin,
  ) {
    return ListView.separated(
      itemCount: market.resourceListings.length,
      separatorBuilder: (_, __) => const SizedBox(height: 7.0),
      itemBuilder: (_, int i) {
        final MarketResourceListing l = market.resourceListings[i];
        final String name = l.isRefined
            ? GameStrings.refinedName(l.resourceType, lang)
            : GameStrings.resourceName(l.resourceType, lang);
        final bool canBuy1 = admin ||
            (l.localStock >= 1 &&
                game.credits >= l.buyPrice &&
                game.remainingCargoCapacity >= 1);
        return Container(
          padding: const EdgeInsets.all(10.0),
          decoration: BoxDecoration(
            color: const Color(0xFF0E1A2F),
            borderRadius: BorderRadius.circular(10.0),
            border: Border.all(
              color: ResourceCatalog.of(l.resourceType).color.withOpacity(0.35),
            ),
          ),
          child: Row(
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      '$name (${l.isRefined ? GameStrings.t(lang, 'Refined', 'Refinado') : GameStrings.t(lang, 'Raw', 'Bruto')})',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 12.8,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      'Stock: ${l.localStock}  •  ${admin ? '0 CR (ADMIN)' : '${l.buyPrice} CR'}',
                      style: TextStyle(
                        color: admin
                            ? const Color(0xFF4ADE80)
                            : const Color(0xFF94A3B8),
                        fontSize: 11.0,
                      ),
                    ),
                  ],
                ),
              ),
              _ActionPillBtn(
                label: '+1',
                enabled: canBuy1,
                color: const Color(0xFF38BDF8),
                onTap: () {
                  game.buyMarketResource(l.resourceType, l.isRefined, 1);
                  setState(() {});
                },
              ),
              if (admin) ...<Widget>[
                const SizedBox(width: 6.0),
                _ActionPillBtn(
                  label: '+10',
                  enabled: true,
                  color: const Color(0xFF4ADE80),
                  onTap: () {
                    game.buyMarketResource(l.resourceType, l.isRefined, 10);
                    setState(() {});
                  },
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _buildBasicVendorList(
    DarkOrbitGame game,
    GameLanguage lang,
    bool admin,
  ) {
    return Column(
      children: <Widget>[
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 6.0),
          decoration: BoxDecoration(
            color: const Color(0xFF131D31),
            borderRadius: BorderRadius.circular(8.0),
            border: Border.all(color: const Color(0x55FACC15)),
          ),
          child: Text(
            GameStrings.t(
              lang,
              'Tier 1 starter essentials only • Ships, T2+ gear & boosters are crafted',
              'Solo equipo inicial Nivel 1 • Naves, equipo N2+ y potenciadores son fabricados',
            ),
            style: const TextStyle(color: Color(0xFFFDE68A), fontSize: 11.0),
          ),
        ),
        const SizedBox(height: 8.0),
        Expanded(
          child: ListView.separated(
            itemCount: BasicVendorItemId.values.length,
            separatorBuilder: (_, __) => const SizedBox(height: 7.0),
            itemBuilder: (_, int i) {
              final BasicVendorItemId item = BasicVendorItemId.values[i];
              final int price = admin ? 0 : item.priceCredits;
              final bool canBuy = admin || game.credits >= price;
              return Container(
                padding: const EdgeInsets.all(10.0),
                decoration: BoxDecoration(
                  color: const Color(0xFF0E1A2F),
                  borderRadius: BorderRadius.circular(10.0),
                  border: Border.all(color: item.accentColor.withOpacity(0.45)),
                ),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            GameStrings.basicVendorItemName(item, lang),
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12.8,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          Text(
                            GameStrings.basicVendorItemDesc(item, lang),
                            style: const TextStyle(
                              color: Color(0xFF94A3B8),
                              fontSize: 11.0,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8.0),
                    _ActionPillBtn(
                      label: admin ? '0 CR' : '$price CR',
                      enabled: canBuy,
                      color: item.accentColor,
                      onTap: () {
                        game.buyBasicVendorItem(item);
                        setState(() {});
                      },
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildBlackMarketGearList(
    DarkOrbitGame game,
    LocalMarketState market,
    GameLanguage lang,
    bool admin,
  ) {
    return ListView.separated(
      itemCount: market.equipmentListings.length,
      separatorBuilder: (_, __) => const SizedBox(height: 7.0),
      itemBuilder: (_, int i) {
        final MarketEquipmentListing l = market.equipmentListings[i];
        final EquipmentSpec spec = EquipmentCatalog.of(l.itemId);
        final bool canBuy =
            admin || (l.localStock > 0 && game.credits >= l.buyPrice);
        return Container(
          padding: const EdgeInsets.all(10.0),
          decoration: BoxDecoration(
            color: const Color(0xFF0E1A2F),
            borderRadius: BorderRadius.circular(10.0),
            border: Border.all(color: spec.accentColor.withOpacity(0.5)),
          ),
          child: Row(
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      spec.displayName,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 13.0,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      '${spec.statLabel} • Stock: ${l.localStock}',
                      style: const TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 11.0,
                      ),
                    ),
                  ],
                ),
              ),
              _ActionPillBtn(
                label: admin ? '0 CR' : '${l.buyPrice} CR',
                enabled: canBuy,
                color: const Color(0xFFFACC15),
                onTap: () {
                  game.buyMarketEquipment(l.itemId);
                  setState(() {});
                },
              ),
            ],
          ),
        );
      },
    );
  }
}

class _StationTabBtn extends StatelessWidget {
  const _StationTabBtn({
    required this.label,
    required this.accent,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final Color accent;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(9.0),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8.5, horizontal: 6.0),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? accent.withOpacity(0.22) : const Color(0xFF0E1A2F),
          borderRadius: BorderRadius.circular(9.0),
          border: Border.all(
            color: selected ? accent : const Color(0xFF26354D),
          ),
        ),
        child: Text(
          label,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            color: selected ? accent : const Color(0xFF94A3B8),
            fontSize: 11.2,
            fontWeight: FontWeight.w900,
          ),
        ),
      ),
    );
  }
}

class _ActionPillBtn extends StatelessWidget {
  const _ActionPillBtn({
    required this.label,
    required this.enabled,
    required this.color,
    required this.onTap,
  });

  final String label;
  final bool enabled;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: enabled ? onTap : null,
      style: ElevatedButton.styleFrom(
        backgroundColor: enabled ? color : const Color(0xFF1E293B),
        foregroundColor: enabled ? Colors.black : const Color(0xFF64748B),
        padding: const EdgeInsets.symmetric(horizontal: 11.0, vertical: 7.0),
        minimumSize: const Size(52.0, 34.0),
      ),
      child: Text(
        label,
        style: const TextStyle(fontSize: 11.0, fontWeight: FontWeight.w900),
      ),
    );
  }
}
