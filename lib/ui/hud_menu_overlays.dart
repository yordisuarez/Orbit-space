import 'package:flutter/material.dart';

import '../game/dark_orbit_game.dart';

// =============================================================================
// 1. UNIFIED COMMAND MENU OVERLAY (5 BUTTONS: SKILLS, HANGAR, INVENTORY, INFO, SETTINGS)
// =============================================================================
class UnifiedMenuOverlay extends StatelessWidget {
  const UnifiedMenuOverlay({super.key, required this.game});

  final DarkOrbitGame game;

  @override
  Widget build(BuildContext context) {
    final GameLanguage lang = game.language;
    final bool inSafe = game.canSwapShipsHere;

    return Material(
      color: const Color(0xDD03060E),
      child: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440.0, maxHeight: 580.0),
            child: Container(
              margin: const EdgeInsets.all(14.0),
              padding: const EdgeInsets.all(16.0),
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
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      const Icon(
                        Icons.grid_view_rounded,
                        color: Color(0xFF00E5FF),
                        size: 22.0,
                      ),
                      const SizedBox(width: 10.0),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              GameStrings.t(lang, 'COMMAND MENU', 'MENÚ DE MANDO'),
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 16.0,
                                fontWeight: FontWeight.w900,
                                letterSpacing: 1.0,
                              ),
                            ),
                            Text(
                              GameStrings.t(
                                lang,
                                'Pilot Systems & Tactical Guide',
                                'Sistemas de Piloto y Guía Táctica',
                              ),
                              style: const TextStyle(
                                color: Color(0xFF94A3B8),
                                fontSize: 11.5,
                              ),
                            ),
                          ],
                        ),
                      ),
                      _CloseChipButton(
                        label: GameStrings.actionClose(lang),
                        onTap: game.closeMenu,
                      ),
                    ],
                  ),
                  const SizedBox(height: 14.0),
                  Flexible(
                    child: SingleChildScrollView(
                      child: Column(
                        children: <Widget>[
                          _MenuRowButton(
                            icon: Icons.auto_graph_rounded,
                            accent: const Color(0xFFC084FC),
                            title: GameStrings.t(lang, 'Skills', 'Habilidades'),
                            subtitle: GameStrings.t(
                              lang,
                              '9 pilot professions (Lv 1–99) • Total ${game.totalSkillLevels}',
                              '9 profesiones de piloto (Nv 1–99) • Total ${game.totalSkillLevels}',
                            ),
                            onTap: game.openSkills,
                          ),
                          const SizedBox(height: 8.0),
                          _MenuRowButton(
                            icon: Icons.rocket_launch_rounded,
                            accent: const Color(0xFF00E5FF),
                            title: 'Hangar',
                            subtitle: inSafe
                                ? GameStrings.t(
                                    lang,
                                    'Capsule, Industrial ships, loadouts & VANTs',
                                    'Cápsula, naves Industriales, equipo y VANTs',
                                  )
                                : GameStrings.t(
                                    lang,
                                    'Read-only in space • Dock to swap ships',
                                    'Solo lectura en espacio • Atraca para cambiar nave',
                                  ),
                            trailingBadge: inSafe
                                ? null
                                : GameStrings.t(lang, 'READ-ONLY', 'SOLO LECTURA'),
                            onTap: game.openHangar,
                          ),
                          const SizedBox(height: 8.0),
                          _MenuRowButton(
                            icon: Icons.inventory_2_rounded,
                            accent: const Color(0xFFFACC15),
                            title: GameStrings.t(lang, 'Inventory', 'Inventario'),
                            subtitle: GameStrings.t(
                              lang,
                              'Cargo ${game.totalCargoUsed}/${game.currentCargoCapacity} • Resources & items',
                              'Bodega ${game.totalCargoUsed}/${game.currentCargoCapacity} • Recursos y objetos',
                            ),
                            onTap: game.openInventory,
                          ),
                          const SizedBox(height: 8.0),
                          _MenuRowButton(
                            icon: Icons.menu_book_rounded,
                            accent: const Color(0xFF4ADE80),
                            title: GameStrings.t(lang, 'Info', 'Información'),
                            subtitle: GameStrings.t(
                              lang,
                              'Mechanics, clockwise refining & 60/40 guide',
                              'Guía de mecánicas, refinado horario y 60/40',
                            ),
                            onTap: game.openGuide,
                          ),
                          const SizedBox(height: 8.0),
                          _MenuRowButton(
                            icon: Icons.settings_rounded,
                            accent: const Color(0xFF94A3B8),
                            title: GameStrings.t(lang, 'Settings', 'Configuración'),
                            subtitle: GameStrings.t(
                              lang,
                              'Language (${lang.displayLabel}) & save options',
                              'Idioma (${lang.displayLabel}) y opciones',
                            ),
                            onTap: game.openSettings,
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _MenuRowButton extends StatelessWidget {
  const _MenuRowButton({
    required this.icon,
    required this.accent,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.trailingBadge,
  });

  final IconData icon;
  final Color accent;
  final String title;
  final String subtitle;
  final String? trailingBadge;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFF0E1A2F),
      borderRadius: BorderRadius.circular(12.0),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12.0),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 11.0),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12.0),
            border: Border.all(color: accent.withOpacity(0.38)),
          ),
          child: Row(
            children: <Widget>[
              Container(
                width: 38.0,
                height: 38.0,
                decoration: BoxDecoration(
                  color: accent.withOpacity(0.16),
                  borderRadius: BorderRadius.circular(10.0),
                  border: Border.all(color: accent.withOpacity(0.5)),
                ),
                child: Icon(icon, color: accent, size: 20.0),
              ),
              const SizedBox(width: 12.0),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        Text(
                          title,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 14.0,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        if (trailingBadge != null) ...<Widget>[
                          const SizedBox(width: 8.0),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 6.0,
                              vertical: 2.0,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0x33FBBF24),
                              borderRadius: BorderRadius.circular(5.0),
                              border: Border.all(
                                color: const Color(0xFFFBBF24),
                                width: 0.8,
                              ),
                            ),
                            child: Text(
                              trailingBadge!,
                              style: const TextStyle(
                                color: Color(0xFFFBBF24),
                                fontSize: 9.0,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                    const SizedBox(height: 2.0),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 11.2,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(Icons.chevron_right_rounded, color: accent.withOpacity(0.8)),
            ],
          ),
        ),
      ),
    );
  }
}

// =============================================================================
// 2. INVENTORY OVERLAY (CARGO HOLD, CRAFTED TOOLS/CONSUMABLES & MODULES)
// =============================================================================
class InventoryOverlay extends StatefulWidget {
  const InventoryOverlay({super.key, required this.game});

  final DarkOrbitGame game;

  @override
  State<InventoryOverlay> createState() => _InventoryOverlayState();
}

class _InventoryOverlayState extends State<InventoryOverlay> {
  int _tab = 0; // 0 = Cargo Hold, 1 = Stored Modules

  @override
  Widget build(BuildContext context) {
    final DarkOrbitGame game = widget.game;
    final GameLanguage lang = game.language;

    return Material(
      color: const Color(0xDD03060E),
      child: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 680.0, maxHeight: 560.0),
            child: Container(
              margin: const EdgeInsets.all(12.0),
              padding: const EdgeInsets.all(14.0),
              decoration: BoxDecoration(
                color: const Color(0xFF081120),
                borderRadius: BorderRadius.circular(16.0),
                border: Border.all(
                  color: const Color(0xFFFACC15).withOpacity(0.55),
                  width: 1.2,
                ),
              ),
              child: Column(
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      const Icon(
                        Icons.inventory_2_rounded,
                        color: Color(0xFFFACC15),
                        size: 22.0,
                      ),
                      const SizedBox(width: 10.0),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              GameStrings.t(lang, 'INVENTORY', 'INVENTARIO'),
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 15.5,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            Text(
                              '${GameStrings.t(lang, 'CARGO', 'CARGA')}: ${game.totalCargoUsed}/${game.currentCargoCapacity}  •  ${game.activeShipDisplayName}',
                              style: const TextStyle(
                                color: Color(0xFF94A3B8),
                                fontSize: 11.5,
                              ),
                            ),
                          ],
                        ),
                      ),
                      _CloseChipButton(
                        label: GameStrings.actionClose(lang),
                        onTap: game.closeInventory,
                      ),
                    ],
                  ),
                  const SizedBox(height: 10.0),
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: _OverlaySubTab(
                          label: GameStrings.t(lang, 'CARGO HOLD', 'BODEGA'),
                          selected: _tab == 0,
                          accent: const Color(0xFFFACC15),
                          onTap: () => setState(() => _tab = 0),
                        ),
                      ),
                      const SizedBox(width: 8.0),
                      Expanded(
                        child: _OverlaySubTab(
                          label: GameStrings.t(lang, 'MODULES & GEAR', 'MÓDULOS Y EQUIPO'),
                          selected: _tab == 1,
                          accent: const Color(0xFF38BDF8),
                          onTap: () => setState(() => _tab = 1),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10.0),
                  Expanded(
                    child: _tab == 0
                        ? _buildCargoList(game, lang)
                        : _buildModulesList(game, lang),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildCargoList(DarkOrbitGame game, GameLanguage lang) {
    final List<MapEntry<ResourceType, int>> rawEntries = game.rawCargo.entries
        .where((MapEntry<ResourceType, int> e) => e.value > 0)
        .toList();
    final List<MapEntry<ResourceType, int>> refEntries = game.refinedCargo.entries
        .where((MapEntry<ResourceType, int> e) => e.value > 0)
        .toList();
    final List<CraftedItemRecord> crafted = game.ownedCraftedItems;

    if (rawEntries.isEmpty && refEntries.isEmpty && crafted.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Text(
            GameStrings.t(
              lang,
              'Your cargo hold is empty. Mine resource nodes or salvage alien wrecks in outer sectors.',
              'Tu bodega está vacía. Mina nodos de recursos o chatarrea pecios alienígenas en los sectores exteriores.',
            ),
            textAlign: TextAlign.center,
            style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 13.0),
          ),
        ),
      );
    }

    return ListView(
      children: <Widget>[
        if (rawEntries.isNotEmpty) ...<Widget>[
          _SectionLabel(
            GameStrings.t(lang, 'RAW RESOURCES', 'RECURSOS BRUTOS'),
          ),
          for (final MapEntry<ResourceType, int> e in rawEntries)
            _CargoItemRow(
              color: ResourceCatalog.of(e.key).color,
              title: GameStrings.resourceName(e.key, lang),
              subtitle: GameStrings.refinesInLabel(e.key, lang),
              badge: 'x${e.value}',
            ),
        ],
        if (refEntries.isNotEmpty) ...<Widget>[
          const SizedBox(height: 8.0),
          _SectionLabel(
            GameStrings.t(lang, 'REFINED MATERIALS', 'MATERIALES REFINADOS'),
          ),
          for (final MapEntry<ResourceType, int> e in refEntries)
            _CargoItemRow(
              color: ResourceCatalog.of(e.key).color,
              title: GameStrings.refinedName(e.key, lang),
              subtitle: GameStrings.t(lang, 'Refined Material', 'Material Refinado'),
              badge: 'x${e.value}',
            ),
        ],
        if (crafted.isNotEmpty) ...<Widget>[
          const SizedBox(height: 8.0),
          _SectionLabel(
            GameStrings.t(lang, 'CRAFTED TOOLS & CONSUMABLES', 'HERRAMIENTAS Y CONSUMIBLES'),
          ),
          for (final CraftedItemRecord item in crafted)
            _CraftedItemRow(
              item: item,
              lang: lang,
              onUse: () {
                if (game.useCraftedConsumable(item.instanceId)) {
                  setState(() {});
                }
              },
            ),
        ],
      ],
    );
  }

  Widget _buildModulesList(DarkOrbitGame game, GameLanguage lang) {
    final List<EquipmentItem> stored = game.inventory;
    if (stored.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Text(
            GameStrings.t(
              lang,
              'No unequipped modules in storage. Equipped gear can be managed in the Hangar.',
              'Sin módulos sueltos en almacén. El equipo instalado se gestiona desde el Hangar.',
            ),
            textAlign: TextAlign.center,
            style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 13.0),
          ),
        ),
      );
    }

    return ListView.separated(
      itemCount: stored.length,
      separatorBuilder: (_, __) => const SizedBox(height: 6.0),
      itemBuilder: (_, int i) {
        final EquipmentItem item = stored[i];
        final EquipmentSpec spec = item.spec;
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 9.0),
          decoration: BoxDecoration(
            color: const Color(0xFF0E1A2F),
            borderRadius: BorderRadius.circular(10.0),
            border: Border.all(color: item.quality.accentColor.withOpacity(0.45)),
          ),
          child: Row(
            children: <Widget>[
              Container(
                width: 8.0,
                height: 30.0,
                decoration: BoxDecoration(
                  color: spec.accentColor,
                  borderRadius: BorderRadius.circular(4.0),
                ),
              ),
              const SizedBox(width: 10.0),
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
                      spec.statLabel,
                      style: const TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 11.0,
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 7.0, vertical: 3.0),
                decoration: BoxDecoration(
                  color: item.quality.accentColor.withOpacity(0.16),
                  borderRadius: BorderRadius.circular(6.0),
                  border: Border.all(
                    color: item.quality.accentColor.withOpacity(0.6),
                  ),
                ),
                child: Text(
                  '${GameStrings.craftQualityName(item.quality, lang)} ${item.quality.bonusLabel}',
                  style: TextStyle(
                    color: item.quality.accentColor,
                    fontSize: 10.0,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6.0),
      child: Text(
        text,
        style: const TextStyle(
          color: Color(0xFF94A3B8),
          fontSize: 10.5,
          fontWeight: FontWeight.w800,
          letterSpacing: 1.0,
        ),
      ),
    );
  }
}

class _CargoItemRow extends StatelessWidget {
  const _CargoItemRow({
    required this.color,
    required this.title,
    required this.subtitle,
    required this.badge,
  });

  final Color color;
  final String title;
  final String subtitle;
  final String badge;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 6.0),
      padding: const EdgeInsets.symmetric(horizontal: 11.0, vertical: 8.0),
      decoration: BoxDecoration(
        color: const Color(0xFF0E1A2F),
        borderRadius: BorderRadius.circular(10.0),
        border: Border.all(color: color.withOpacity(0.35)),
      ),
      child: Row(
        children: <Widget>[
          Container(
            width: 10.0,
            height: 10.0,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle),
          ),
          const SizedBox(width: 10.0),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  title,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 13.0,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  subtitle,
                  style: const TextStyle(
                    color: Color(0xFF94A3B8),
                    fontSize: 10.8,
                  ),
                ),
              ],
            ),
          ),
          Text(
            badge,
            style: TextStyle(
              color: color,
              fontSize: 13.5,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      ),
    );
  }
}

class _CraftedItemRow extends StatelessWidget {
  const _CraftedItemRow({
    required this.item,
    required this.lang,
    required this.onUse,
  });

  final CraftedItemRecord item;
  final GameLanguage lang;
  final VoidCallback onUse;

  @override
  Widget build(BuildContext context) {
    final StationCraftRecipeSpec spec = StationCraftCatalog.of(item.recipeId);
    final bool isConsumable =
        spec.outputKind == CraftOutputKind.consumableRepair ||
            spec.outputKind == CraftOutputKind.consumableBooster;
    return Container(
      margin: const EdgeInsets.only(bottom: 6.0),
      padding: const EdgeInsets.symmetric(horizontal: 11.0, vertical: 8.0),
      decoration: BoxDecoration(
        color: const Color(0xFF0E1A2F),
        borderRadius: BorderRadius.circular(10.0),
        border: Border.all(color: item.quality.accentColor.withOpacity(0.45)),
      ),
      child: Row(
        children: <Widget>[
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  '${GameStrings.recipeName(item.recipeId, lang)} (x${item.quantity})',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12.8,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  '${GameStrings.craftQualityName(item.quality, lang)} ${item.quality.bonusLabel} • ${GameStrings.craftOutcomeStatSummary(spec, item.quality, lang)}',
                  style: TextStyle(
                    color: item.quality.accentColor,
                    fontSize: 10.8,
                  ),
                ),
              ],
            ),
          ),
          if (isConsumable)
            ElevatedButton(
              onPressed: onUse,
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF22C55E),
                foregroundColor: Colors.black,
                padding:
                    const EdgeInsets.symmetric(horizontal: 12.0, vertical: 6.0),
                minimumSize: const Size(56.0, 32.0),
              ),
              child: Text(
                GameStrings.t(lang, 'USE', 'USAR'),
                style: const TextStyle(
                  fontSize: 11.0,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
        ],
      ),
    );
  }
}

// =============================================================================
// 3. MODULAR INFO / GAME GUIDE OVERLAY
// =============================================================================
class GameGuideOverlay extends StatefulWidget {
  const GameGuideOverlay({super.key, required this.game});

  final DarkOrbitGame game;

  @override
  State<GameGuideOverlay> createState() => _GameGuideOverlayState();
}

class _GameGuideOverlayState extends State<GameGuideOverlay> {
  final Set<String> _expanded = <String>{
    GameStrings.guideEntries.first.id,
  };

  static const List<Color> _accents = <Color>[
    Color(0xFFFF4D6D),
    Color(0xFF38BDF8),
    Color(0xFFFB923C),
    Color(0xFFC084FC),
    Color(0xFF4ADE80),
    Color(0xFFFACC15),
  ];

  @override
  Widget build(BuildContext context) {
    final GameLanguage lang = widget.game.language;

    return Material(
      color: const Color(0xDD03060E),
      child: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 740.0, maxHeight: 580.0),
            child: Container(
              margin: const EdgeInsets.all(12.0),
              padding: const EdgeInsets.all(14.0),
              decoration: BoxDecoration(
                color: const Color(0xFF081120),
                borderRadius: BorderRadius.circular(16.0),
                border: Border.all(
                  color: const Color(0xFF4ADE80).withOpacity(0.55),
                  width: 1.2,
                ),
              ),
              child: Column(
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      const Icon(
                        Icons.menu_book_rounded,
                        color: Color(0xFF4ADE80),
                        size: 22.0,
                      ),
                      const SizedBox(width: 10.0),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              GameStrings.t(
                                lang,
                                'INFO & GAME GUIDE',
                                'INFORMACIÓN Y GUÍA',
                              ),
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 15.5,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            Text(
                              GameStrings.t(
                                lang,
                                'Tap any topic to expand details',
                                'Toca cualquier tema para expandir detalles',
                              ),
                              style: const TextStyle(
                                color: Color(0xFF94A3B8),
                                fontSize: 11.2,
                              ),
                            ),
                          ],
                        ),
                      ),
                      _CloseChipButton(
                        label: GameStrings.actionClose(lang),
                        onTap: widget.game.closeGuide,
                      ),
                    ],
                  ),
                  const SizedBox(height: 12.0),
                  Expanded(
                    child: ListView.separated(
                      itemCount: GameStrings.guideEntries.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8.0),
                      itemBuilder: (_, int idx) {
                        final GuideEntry entry = GameStrings.guideEntries[idx];
                        final bool isOpen = _expanded.contains(entry.id);
                        final Color accent = _accents[idx % _accents.length];
                        return Container(
                          decoration: BoxDecoration(
                            color: isOpen
                                ? const Color(0xFF0D1A2E)
                                : const Color(0xFF0A1221),
                            borderRadius: BorderRadius.circular(12.0),
                            border: Border.all(
                              color: accent.withOpacity(isOpen ? 0.65 : 0.28),
                            ),
                          ),
                          child: Column(
                            children: <Widget>[
                              InkWell(
                                borderRadius: BorderRadius.circular(12.0),
                                onTap: () {
                                  setState(() {
                                    if (isOpen) {
                                      _expanded.remove(entry.id);
                                    } else {
                                      _expanded.add(entry.id);
                                    }
                                  });
                                },
                                child: Padding(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 12.0,
                                    vertical: 11.0,
                                  ),
                                  child: Row(
                                    children: <Widget>[
                                      Container(
                                        width: 24.0,
                                        height: 24.0,
                                        alignment: Alignment.center,
                                        decoration: BoxDecoration(
                                          color: accent.withOpacity(0.18),
                                          borderRadius:
                                              BorderRadius.circular(6.0),
                                        ),
                                        child: Text(
                                          '${idx + 1}',
                                          style: TextStyle(
                                            color: accent,
                                            fontSize: 11.5,
                                            fontWeight: FontWeight.w900,
                                          ),
                                        ),
                                      ),
                                      const SizedBox(width: 10.0),
                                      Expanded(
                                        child: Text(
                                          entry.title(lang),
                                          style: const TextStyle(
                                            color: Colors.white,
                                            fontSize: 13.5,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ),
                                      Icon(
                                        isOpen
                                            ? Icons.expand_less_rounded
                                            : Icons.expand_more_rounded,
                                        color: isOpen
                                            ? accent
                                            : const Color(0xFF94A3B8),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                              if (isOpen)
                                Container(
                                  width: double.infinity,
                                  padding: const EdgeInsets.fromLTRB(
                                    14.0,
                                    6.0,
                                    14.0,
                                    12.0,
                                  ),
                                  decoration: const BoxDecoration(
                                    color: Color(0x66040914),
                                    borderRadius: BorderRadius.vertical(
                                      bottom: Radius.circular(11.0),
                                    ),
                                  ),
                                  child: Text(
                                    entry.body(lang),
                                    style: const TextStyle(
                                      color: Color(0xFFD6E2F0),
                                      fontSize: 12.4,
                                      height: 1.48,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// =============================================================================
// 4. SETTINGS OVERLAY (LANGUAGE EN/ES + SAVE RESET)
// =============================================================================
class SettingsOverlay extends StatefulWidget {
  const SettingsOverlay({super.key, required this.game});

  final DarkOrbitGame game;

  @override
  State<SettingsOverlay> createState() => _SettingsOverlayState();
}

class _SettingsOverlayState extends State<SettingsOverlay> {
  @override
  Widget build(BuildContext context) {
    final DarkOrbitGame game = widget.game;
    final GameLanguage lang = game.language;

    return Material(
      color: const Color(0xDD03060E),
      child: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440.0),
            child: Container(
              margin: const EdgeInsets.all(14.0),
              padding: const EdgeInsets.all(16.0),
              decoration: BoxDecoration(
                color: const Color(0xFF081120),
                borderRadius: BorderRadius.circular(16.0),
                border: Border.all(
                  color: const Color(0xFF38BDF8).withOpacity(0.55),
                  width: 1.2,
                ),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      const Icon(
                        Icons.settings_rounded,
                        color: Color(0xFF38BDF8),
                        size: 22.0,
                      ),
                      const SizedBox(width: 10.0),
                      Expanded(
                        child: Text(
                          GameStrings.t(lang, 'SETTINGS', 'AJUSTES'),
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 16.0,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                      _CloseChipButton(
                        label: GameStrings.actionClose(lang),
                        onTap: game.closeSettings,
                      ),
                    ],
                  ),
                  const SizedBox(height: 16.0),
                  Text(
                    GameStrings.t(lang, 'LANGUAGE', 'IDIOMA'),
                    style: const TextStyle(
                      color: Color(0xFF94A3B8),
                      fontSize: 11.0,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 1.0,
                    ),
                  ),
                  const SizedBox(height: 8.0),
                  Row(
                    children: <Widget>[
                      Expanded(
                        child: _OverlaySubTab(
                          label: GameLanguage.en.displayLabel,
                          selected: lang == GameLanguage.en,
                          accent: const Color(0xFF00E5FF),
                          onTap: () {
                            game.setLanguage(GameLanguage.en);
                            setState(() {});
                          },
                        ),
                      ),
                      const SizedBox(width: 10.0),
                      Expanded(
                        child: _OverlaySubTab(
                          label: GameLanguage.es.displayLabel,
                          selected: lang == GameLanguage.es,
                          accent: const Color(0xFF00E5FF),
                          onTap: () {
                            game.setLanguage(GameLanguage.es);
                            setState(() {});
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8.0),
                  Text(
                    GameStrings.t(
                      lang,
                      'Updates HUD, menus, Station, Skills, and Guide immediately.',
                      'Actualiza el HUD, menús, Estación, Habilidades y Guía al instante.',
                    ),
                    style: const TextStyle(
                      color: Color(0xFF94A3B8),
                      fontSize: 11.5,
                    ),
                  ),
                  const SizedBox(height: 18.0),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () => game.resetSave(),
                      icon: const Icon(
                        Icons.restart_alt_rounded,
                        color: Color(0xFFFF4D6D),
                        size: 18.0,
                      ),
                      label: Text(
                        GameStrings.t(lang, 'RESET SAVE', 'REINICIAR PARTIDA'),
                        style: const TextStyle(
                          color: Color(0xFFFF4D6D),
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      style: OutlinedButton.styleFrom(
                        side: const BorderSide(color: Color(0x88FF4D6D)),
                        padding: const EdgeInsets.symmetric(vertical: 12.0),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// =============================================================================
// 5. DEATH / LOOT (60/40) RESPAWN SUMMARY OVERLAY
// =============================================================================
class DeathRespawnOverlay extends StatelessWidget {
  const DeathRespawnOverlay({super.key, required this.game});

  final DarkOrbitGame game;

  @override
  Widget build(BuildContext context) {
    final GameLanguage lang = game.language;
    final DeathSummary? summary = game.lastDeathSummary;

    return Material(
      color: const Color(0xE605020A),
      child: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460.0),
            child: Container(
              margin: const EdgeInsets.all(14.0),
              padding: const EdgeInsets.all(18.0),
              decoration: BoxDecoration(
                color: const Color(0xFF140812),
                borderRadius: BorderRadius.circular(16.0),
                border: Border.all(
                  color: const Color(0xFFFF2E4C).withOpacity(0.75),
                  width: 1.5,
                ),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  const Icon(
                    Icons.warning_amber_rounded,
                    color: Color(0xFFFF2E4C),
                    size: 40.0,
                  ),
                  const SizedBox(height: 8.0),
                  Text(
                    GameStrings.t(lang, 'SHIP DESTROYED', 'NAVE DESTRUIDA'),
                    style: const TextStyle(
                      color: Color(0xFFFF4D6D),
                      fontSize: 18.0,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 1.2,
                    ),
                  ),
                  const SizedBox(height: 12.0),
                  if (summary != null) ...<Widget>[
                    Text(
                      summary.wasInCapsule
                          ? GameStrings.t(
                              lang,
                              'Capsule & 3 permanent slots intact — never lost.',
                              'Cápsula y sus 3 ranuras permanentes intactas — nunca se pierden.',
                            )
                          : '${GameStrings.t(lang, 'Ship permanently destroyed:', 'Nave destruida permanentemente:')} ${summary.destroyedShipName}',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 13.0,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 10.0),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(10.0),
                      decoration: BoxDecoration(
                        color: const Color(0xFF0A0F1D),
                        borderRadius: BorderRadius.circular(10.0),
                        border: Border.all(color: const Color(0xFF334155)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            '• ${GameStrings.t(lang, '60% Wreck Loot at', 'Botín 60% en pecio en')} ${summary.deathSectorId} (${summary.deathX}, ${summary.deathY}): '
                            '${summary.cargoDroppedToWreck} cargo, ${summary.gearDroppedToWreck} gear',
                            style: const TextStyle(
                              color: Color(0xFF4ADE80),
                              fontSize: 11.8,
                            ),
                          ),
                          const SizedBox(height: 4.0),
                          Text(
                            '• ${GameStrings.t(lang, '40% Permanently Destroyed:', '40% Destruido permanentemente:')} '
                            '${summary.cargoDestroyed} cargo, ${summary.gearDestroyed} gear',
                            style: const TextStyle(
                              color: Color(0xFFFF8A80),
                              fontSize: 11.8,
                            ),
                          ),
                          const SizedBox(height: 4.0),
                          Text(
                            '• ${GameStrings.t(lang, 'Respawn Home City:', 'Ciudad Natal:')} ${summary.homeCityId} (${GameStrings.t(lang, 'Permanent Capsule', 'Cápsula Permanente')})',
                            style: const TextStyle(
                              color: Color(0xFF38BDF8),
                              fontSize: 11.8,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 16.0),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton(
                      onPressed: game.dismissDeathOverlay,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF00E5FF),
                        foregroundColor: Colors.black,
                        padding: const EdgeInsets.symmetric(vertical: 13.0),
                      ),
                      child: Text(
                        GameStrings.t(
                          lang,
                          'RESPAWN IN HOME CITY (CAPSULE)',
                          'REAPARECER EN CIUDAD NATAL (CÁPSULA)',
                        ),
                        style: const TextStyle(
                          fontWeight: FontWeight.w900,
                          fontSize: 12.5,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _CloseChipButton extends StatelessWidget {
  const _CloseChipButton({required this.label, required this.onTap});

  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8.0),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 7.0),
        decoration: BoxDecoration(
          color: const Color(0xFF1A253B),
          borderRadius: BorderRadius.circular(8.0),
          border: Border.all(color: const Color(0xFF334155)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            const Icon(Icons.close, color: Colors.white, size: 15.0),
            const SizedBox(width: 4.0),
            Text(
              label,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 11.0,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _OverlaySubTab extends StatelessWidget {
  const _OverlaySubTab({
    required this.label,
    required this.selected,
    required this.accent,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final Color accent;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(9.0),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 9.0, horizontal: 10.0),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: selected ? accent.withOpacity(0.20) : const Color(0xFF0E1A2F),
          borderRadius: BorderRadius.circular(9.0),
          border: Border.all(
            color: selected ? accent : const Color(0xFF26354D),
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: selected ? accent : const Color(0xFF94A3B8),
            fontSize: 11.5,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
    );
  }
}
