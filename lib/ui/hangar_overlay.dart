import 'package:flutter/material.dart';

import '../game/dark_orbit_game.dart';

class HangarOverlay extends StatefulWidget {
  const HangarOverlay({
    super.key,
    required this.game,
  });

  final DarkOrbitGame game;

  @override
  State<HangarOverlay> createState() => _HangarOverlayState();
}

class _HangarOverlayState extends State<HangarOverlay> {
  int _selectedTab = 0; // 0 = EQUIPMENT, 1 = VANTS, 2 = SHOP, 3 = FUSE, 4 = MISSIONS

  void _switchConfig(int configNumber) {
    widget.game.switchConfig(configNumber);
    setState(() {});
  }

  void _buyOrSelectShip(ShipClass shipClass) {
    if (widget.game.buyOrSelectShipClass(shipClass)) {
      setState(() {});
    }
  }

  void _buyOrSelectFormation(DroneFormation formation) {
    if (widget.game.buyOrSelectFormation(formation)) {
      setState(() {});
    }
  }

  void _buyItem(EquipmentItemId itemId) {
    if (widget.game.buyEquipment(itemId)) {
      setState(() {});
    }
  }

  void _buyFlaxVant() {
    if (widget.game.buyFlaxVant()) {
      setState(() {});
    }
  }

  void _fuse(EquipmentItemId sourceType) {
    if (widget.game.fuseItems(sourceType)) {
      setState(() {});
    }
  }

  void _fuseFlaxVants() {
    if (widget.game.fuseFlaxVants()) {
      setState(() {});
    }
  }

  void _equipFromInventory(int instanceId) {
    if (widget.game.equipFromInventory(instanceId)) {
      setState(() {});
    }
  }

  void _equipToVantSlot(int vantId, int slotIdx) {
    if (widget.game.equipItemToVantSlot(vantId, slotIdx)) {
      setState(() {});
    }
  }

  void _equipFromInventoryToVant(int instanceId) {
    if (widget.game.equipFromInventoryToVant(instanceId)) {
      setState(() {});
    }
  }

  void _unequipVantSlot(int vantId, int slotIdx) {
    if (widget.game.unequipVantSlot(vantId, slotIdx)) {
      setState(() {});
    }
  }

  void _unequipLaser(int index) {
    if (widget.game.unequipLaserSlot(index)) {
      setState(() {});
    }
  }

  void _unequipShield(int index) {
    if (widget.game.unequipShieldSlot(index)) {
      setState(() {});
    }
  }

  void _unequipSectorChip() {
    if (widget.game.unequipSectorChip()) {
      setState(() {});
    }
  }

  void _claimMission() {
    if (widget.game.claimMissionReward()) {
      setState(() {});
    }
  }

  Future<void> _resetSave() async {
    await widget.game.resetSave();
    if (mounted) {
      setState(() {});
    }
  }

  void _debugAddCredits() {
    widget.game.debugAddOneMillionCredits();
    setState(() {});
  }

  void _debugAddLevel() {
    widget.game.debugAddOneLevel();
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final DarkOrbitGame game = widget.game;
    final ShipClassSpec activeShip = game.activeShipSpec;

    return Container(
      color: const Color(0xD4040711),
      padding: const EdgeInsets.symmetric(horizontal: 14.0, vertical: 10.0),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 860.0),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14.0, vertical: 10.0),
            decoration: BoxDecoration(
              color: const Color(0xFF0B1324),
              borderRadius: BorderRadius.circular(16.0),
              border: Border.all(
                color: const Color(0xAA00E5FF),
                width: 1.5,
              ),
            ),
            child: Column(
              children: <Widget>[
                // 1. Header: Title, 5 Tabs, LV, Credits, RESET SAVE, single-line RESUME
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Row(
                      children: <Widget>[
                        const Text(
                          'STARBASE HANGAR',
                          maxLines: 1,
                          softWrap: false,
                          style: TextStyle(
                            color: Color(0xFF00E5FF),
                            fontSize: 13.5,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1.0,
                          ),
                        ),
                        const SizedBox(width: 8.0),
                        Container(
                          padding: const EdgeInsets.all(3.0),
                          decoration: BoxDecoration(
                            color: const Color(0xFF060C18),
                            borderRadius: BorderRadius.circular(9.0),
                            border: Border.all(color: const Color(0xFF1E3A5F)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              _TabPill(
                                label: 'EQUIPMENT',
                                selected: _selectedTab == 0,
                                onTap: () => setState(() => _selectedTab = 0),
                              ),
                              const SizedBox(width: 2.0),
                              _TabPill(
                                label: 'VANTS',
                                selected: _selectedTab == 1,
                                showDot: game.ownedVants.any(
                                      (VantDrone v) => v
                                          .slotsForConfig(game.activeConfig)
                                          .any((EquipmentItem? s) => s == null),
                                    ) &&
                                    game.inventory.any(
                                      (EquipmentItem i) =>
                                          i.spec.category ==
                                              EquipmentCategory.laser ||
                                          i.spec.category ==
                                              EquipmentCategory.shieldGenerator,
                                    ),
                                dotColor: const Color(0xFF38BDF8),
                                onTap: () => setState(() => _selectedTab = 1),
                              ),
                              const SizedBox(width: 2.0),
                              _TabPill(
                                label: 'SHOP',
                                selected: _selectedTab == 2,
                                onTap: () => setState(() => _selectedTab = 2),
                              ),
                              const SizedBox(width: 2.0),
                              _TabPill(
                                label: 'FUSE',
                                selected: _selectedTab == 3,
                                showDot: game.hasAnyReadyFusion,
                                dotColor: const Color(0xFFFBBF24),
                                onTap: () => setState(() => _selectedTab = 3),
                              ),
                              const SizedBox(width: 2.0),
                              _TabPill(
                                label: 'MISSIONS',
                                selected: _selectedTab == 4,
                                showDot: game.isMissionReadyToClaim,
                                dotColor: const Color(0xFF22C55E),
                                onTap: () => setState(() => _selectedTab = 4),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    Row(
                      children: <Widget>[
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8.0,
                            vertical: 6.0,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFF140D26),
                            borderRadius: BorderRadius.circular(8.0),
                            border: Border.all(color: const Color(0x88C084FC)),
                          ),
                          child: Text(
                            'LV ${game.player.level}',
                            maxLines: 1,
                            softWrap: false,
                            style: const TextStyle(
                              color: Color(0xFFE9D5FF),
                              fontSize: 11.5,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6.0),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 9.0,
                            vertical: 6.0,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFF071020),
                            borderRadius: BorderRadius.circular(8.0),
                            border: Border.all(color: const Color(0x88FBBF24)),
                          ),
                          child: Text(
                            '${game.credits} CR',
                            maxLines: 1,
                            softWrap: false,
                            style: const TextStyle(
                              color: Color(0xFFFBBF24),
                              fontSize: 12.0,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const SizedBox(width: 6.0),
                        ElevatedButton.icon(
                          onPressed: game.closeHangar,
                          icon: const Icon(Icons.play_arrow, size: 15.0),
                          label: const Text(
                            'RESUME',
                            maxLines: 1,
                            softWrap: false,
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFF00E5FF),
                            foregroundColor: const Color(0xFF040711),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12.0,
                              vertical: 8.0,
                            ),
                            minimumSize: const Size(0.0, 36.0),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                const SizedBox(height: 8.0),

                // 2. Compact live ship stats summary bar + Config 1 / 2 + Dev & Reset buttons
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 8.0,
                    vertical: 5.0,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFF071020),
                    borderRadius: BorderRadius.circular(10.0),
                    border: Border.all(color: const Color(0xFF1E3A5F)),
                  ),
                  child: SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: <Widget>[
                        Container(
                          padding: const EdgeInsets.all(2.0),
                          decoration: BoxDecoration(
                            color: const Color(0xFF040914),
                            borderRadius: BorderRadius.circular(7.0),
                            border: Border.all(color: const Color(0xFF1E3A5F)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              _TabPill(
                                label: 'CFG 1',
                                selected: game.activeConfig == 1,
                                onTap: () => _switchConfig(1),
                              ),
                              const SizedBox(width: 2.0),
                              _TabPill(
                                label: 'CFG 2',
                                selected: game.activeConfig == 2,
                                onTap: () => _switchConfig(2),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8.0),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 7.0,
                            vertical: 4.0,
                          ),
                          decoration: BoxDecoration(
                            color: activeShip.accentColor.withOpacity(0.14),
                            borderRadius: BorderRadius.circular(6.0),
                            border: Border.all(
                              color: activeShip.accentColor.withOpacity(0.6),
                            ),
                          ),
                          child: Text(
                            activeShip.displayName.toUpperCase(),
                            maxLines: 1,
                            softWrap: false,
                            style: TextStyle(
                              color: activeShip.accentColor,
                              fontSize: 10.5,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const SizedBox(width: 10.0),
                        _LiveStatItem(
                          icon: Icons.bolt,
                          label: 'DMG',
                          value: '${game.currentLaserDamage.toInt()}',
                          color: const Color(0xFFFF2E4C),
                        ),
                        const SizedBox(width: 10.0),
                        _LiveStatItem(
                          icon: Icons.shield,
                          label: 'SHD',
                          value: '${game.currentMaxShield.toInt()}',
                          color: const Color(0xFF38BDF8),
                        ),
                        const SizedBox(width: 10.0),
                        _LiveStatItem(
                          icon: Icons.speed,
                          label: 'SPD',
                          value: '${game.currentSpeed.toInt()}',
                          color: const Color(0xFFFBBF24),
                        ),
                        const SizedBox(width: 10.0),
                        _LiveStatItem(
                          icon: Icons.favorite,
                          label: 'HULL',
                          value: '${game.currentMaxHull.toInt()}',
                          color: const Color(0xFF22C55E),
                        ),
                        const SizedBox(width: 12.0),
                        // Temporary Developer / Testing Buttons next to RESET SAVE
                        OutlinedButton(
                          onPressed: _debugAddCredits,
                          style: OutlinedButton.styleFrom(
                            backgroundColor: const Color(0x33F59E0B),
                            foregroundColor: const Color(0xFFFBBF24),
                            side: const BorderSide(color: Color(0xCCFBBF24)),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8.0,
                              vertical: 4.0,
                            ),
                            minimumSize: const Size(0.0, 28.0),
                          ),
                          child: const Text(
                            '+1M CR',
                            maxLines: 1,
                            softWrap: false,
                            style: TextStyle(
                              fontSize: 10.0,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const SizedBox(width: 5.0),
                        OutlinedButton(
                          onPressed: _debugAddLevel,
                          style: OutlinedButton.styleFrom(
                            backgroundColor: const Color(0x33A855F7),
                            foregroundColor: const Color(0xFFE9D5FF),
                            side: const BorderSide(color: Color(0xCCC084FC)),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8.0,
                              vertical: 4.0,
                            ),
                            minimumSize: const Size(0.0, 28.0),
                          ),
                          child: const Text(
                            '+1 LVL',
                            maxLines: 1,
                            softWrap: false,
                            style: TextStyle(
                              fontSize: 10.0,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const SizedBox(width: 5.0),
                        OutlinedButton(
                          onPressed: _resetSave,
                          style: OutlinedButton.styleFrom(
                            backgroundColor: const Color(0x28FF2E4C),
                            foregroundColor: const Color(0xFFFF6B81),
                            side: const BorderSide(color: Color(0xAAFF2E4C)),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8.0,
                              vertical: 4.0,
                            ),
                            minimumSize: const Size(0.0, 28.0),
                          ),
                          child: const Text(
                            'RESET SAVE',
                            maxLines: 1,
                            softWrap: false,
                            style: TextStyle(
                              fontSize: 10.0,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 8.0),

                // 3. Active tab body
                Expanded(
                  child: _buildSelectedTab(game),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSelectedTab(DarkOrbitGame game) {
    switch (_selectedTab) {
      case 0:
        return _buildEquipmentTab(game);
      case 1:
        return _buildVantsTab(game);
      case 2:
        return _buildShopTab(game);
      case 3:
        return _buildFuseTab(game);
      default:
        return _buildMissionsTab(game);
    }
  }

  Widget _buildEquipmentTab(DarkOrbitGame game) {
    final int usedLasers =
        game.laserSlots.where((EquipmentItem? e) => e != null).length;
    final int usedShields =
        game.shieldSlots.where((EquipmentItem? e) => e != null).length;
    final List<EquipmentItem> inv = game.inventory;
    final EquipmentItem? chip = game.equippedSectorChip;
    final Color chipAccent =
        chip?.spec.accentColor ?? const Color(0xFF1E3A5F);

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Row(
            children: <Widget>[
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 7.0,
                  vertical: 5.0,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFF071020),
                  borderRadius: BorderRadius.circular(10.0),
                  border: Border.all(color: const Color(0xFF1E3A5F)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    _TabPill(
                      label: 'CONFIG 1',
                      selected: game.activeConfig == 1,
                      onTap: () => _switchConfig(1),
                    ),
                    const SizedBox(width: 4.0),
                    _TabPill(
                      label: 'CONFIG 2',
                      selected: game.activeConfig == 2,
                      onTap: () => _switchConfig(2),
                    ),
                  ],
                ),
              ),
              if (game.ownedShipClasses.length > 1) ...<Widget>[
                const SizedBox(width: 8.0),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 7.0,
                    vertical: 5.0,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFF071020),
                    borderRadius: BorderRadius.circular(10.0),
                    border: Border.all(color: const Color(0xFF1E3A5F)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: ShipClass.values
                        .where((ShipClass s) => game.ownedShipClasses.contains(s))
                        .map((ShipClass ship) {
                      final ShipClassSpec s = ShipClassCatalog.of(ship);
                      final bool active = game.activeShipClass == ship;
                      return GestureDetector(
                        onTap: () => _buyOrSelectShip(ship),
                        child: Container(
                          margin: const EdgeInsets.symmetric(horizontal: 2.0),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 7.0,
                            vertical: 5.0,
                          ),
                          decoration: BoxDecoration(
                            color: active ? s.accentColor : Colors.transparent,
                            borderRadius: BorderRadius.circular(6.0),
                          ),
                          child: Text(
                            s.displayName.substring(0, 4).toUpperCase(),
                            maxLines: 1,
                            softWrap: false,
                            style: TextStyle(
                              color: active
                                  ? const Color(0xFF040711)
                                  : const Color(0xFF94A3B8),
                              fontSize: 10.5,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ],
              const SizedBox(width: 8.0),
              Expanded(
                child: GestureDetector(
                  onTap: chip != null ? _unequipSectorChip : null,
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 10.0,
                      vertical: 6.0,
                    ),
                    decoration: BoxDecoration(
                      color: chip != null
                          ? chipAccent.withOpacity(0.14)
                          : const Color(0xFF071020),
                      borderRadius: BorderRadius.circular(10.0),
                      border: Border.all(
                        color: chip != null
                            ? chipAccent.withOpacity(0.85)
                            : const Color(0xFF1E3A5F),
                        width: chip != null ? 1.4 : 1.0,
                      ),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[
                        Row(
                          children: <Widget>[
                            Icon(
                              Icons.memory,
                              color: chip != null
                                  ? chipAccent
                                  : const Color(0xFF94A3B8),
                              size: 18.0,
                            ),
                            const SizedBox(width: 7.0),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                const Text(
                                  'SECTOR CHIP',
                                  maxLines: 1,
                                  softWrap: false,
                                  style: TextStyle(
                                    color: Color(0xFF94A3B8),
                                    fontSize: 9.5,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                Text(
                                  chip?.spec.displayName ??
                                      'NONE (SECTOR 1 ONLY)',
                                  maxLines: 1,
                                  softWrap: false,
                                  style: TextStyle(
                                    color: chip != null
                                        ? chipAccent
                                        : const Color(0xFFF1F5F9),
                                    fontSize: 11.5,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                        if (chip != null)
                          Text(
                            chip.spec.statSummary,
                            maxLines: 1,
                            softWrap: false,
                            style: TextStyle(
                              color: chipAccent,
                              fontSize: 10.0,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8.0),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Expanded(
                child: _DynamicSlotPanel(
                  title: 'LASER SLOTS ($usedLasers/${game.laserSlots.length})',
                  accent: const Color(0xFFFF6B35),
                  slots: game.laserSlots,
                  columns: game.laserSlots.length >= 8 ? 4 : 3,
                  emptyIcon: Icons.bolt,
                  onTapSlot: _unequipLaser,
                ),
              ),
              const SizedBox(width: 8.0),
              Expanded(
                child: _DynamicSlotPanel(
                  title:
                      'SHIELD SLOTS ($usedShields/${game.shieldSlots.length})',
                  accent: const Color(0xFF38BDF8),
                  slots: game.shieldSlots,
                  columns: game.shieldSlots.length >= 8 ? 5 : 3,
                  emptyIcon: Icons.shield,
                  onTapSlot: _unequipShield,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8.0),
          Container(
            padding:
                const EdgeInsets.symmetric(horizontal: 10.0, vertical: 8.0),
            decoration: BoxDecoration(
              color: const Color(0xFF071020),
              borderRadius: BorderRadius.circular(10.0),
              border: Border.all(color: const Color(0xFF1E3A5F)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Text(
                      'INVENTORY (${inv.length})',
                      maxLines: 1,
                      softWrap: false,
                      style: const TextStyle(
                        color: Color(0xFFF1F5F9),
                        fontSize: 11.5,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Text(
                      'Tap slot to unequip • Tap inventory to equip',
                      maxLines: 1,
                      softWrap: false,
                      style: TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 10.0,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 7.0),
                if (inv.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 6.0),
                    child: Text(
                      'Inventory empty — buy Level 1 gear in SHOP or fuse upgrades in FUSE.',
                      style: TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 11.5,
                      ),
                    ),
                  )
                else
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: inv.map((EquipmentItem item) {
                        return Padding(
                          padding: const EdgeInsets.only(right: 7.0),
                          child: _EquipmentTile(
                            item: item,
                            emptyIcon: Icons.bolt,
                            onTap: () => _equipFromInventory(item.instanceId),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildVantsTab(DarkOrbitGame game) {
    final List<VantDrone> vants = game.ownedVants;
    final List<EquipmentItem> compatInv = game.inventory
        .where((EquipmentItem i) =>
            i.spec.category == EquipmentCategory.laser ||
            i.spec.category == EquipmentCategory.shieldGenerator)
        .toList();

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Row(
                children: <Widget>[
                  Text(
                    'VANTS: ${vants.length} / ${GameConstants.maxVants}',
                    style: const TextStyle(
                      color: Color(0xFF00E5FF),
                      fontSize: 12.0,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(width: 8.0),
                  Text(
                    'FLAX: ${game.flaxVantCount} • IRIS: ${game.irisVantCount} • CFG ${game.activeConfig} • ${game.activeFormationSpec.displayName}',
                    style: const TextStyle(
                      color: Color(0xFFFBBF24),
                      fontSize: 10.5,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
              const Text(
                'Tap empty slot to equip • Tap filled slot to unequip',
                style: TextStyle(color: Color(0xFF94A3B8), fontSize: 10.0),
              ),
            ],
          ),
          const SizedBox(height: 8.0),
          if (vants.isEmpty)
            Container(
              padding: const EdgeInsets.all(18.0),
              decoration: BoxDecoration(
                color: const Color(0xFF071020),
                borderRadius: BorderRadius.circular(10.0),
                border: Border.all(color: const Color(0xFF1E3A5F)),
              ),
              child: const Text(
                'No VANTs owned yet — buy Flax VANT drones (600 CR) in SHOP and fuse 2x Flax into Iris in FUSE.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11.5),
              ),
            )
          else
            ...List<Widget>.generate((vants.length + 3) ~/ 4, (int r) {
              return Padding(
                padding: const EdgeInsets.only(bottom: 8.0),
                child: Row(
                  children: List<Widget>.generate(4, (int c) {
                    final int idx = r * 4 + c;
                    if (idx >= vants.length) {
                      return const Expanded(child: SizedBox());
                    }
                    final VantDrone vant = vants[idx];
                    final bool isIris = vant.tier == VantTier.iris;
                    final List<EquipmentItem?> slots =
                        vant.slotsForConfig(game.activeConfig);
                    return Expanded(
                      child: Container(
                        margin: EdgeInsets.only(right: c == 3 ? 0.0 : 8.0),
                        padding: const EdgeInsets.all(8.0),
                        decoration: BoxDecoration(
                          color: isIris
                              ? const Color(0xFF131A2E)
                              : const Color(0xFF071020),
                          borderRadius: BorderRadius.circular(10.0),
                          border: Border.all(
                            color: isIris
                                ? const Color(0xCCFBBF24)
                                : const Color(0x8800E5FF),
                            width: isIris ? 1.4 : 1.0,
                          ),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: <Widget>[
                                Text(
                                  '${vant.spec.shortCode} #${idx + 1}',
                                  style: TextStyle(
                                    color: isIris
                                        ? const Color(0xFFFBBF24)
                                        : const Color(0xFFCBD5E1),
                                    fontSize: 11.5,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                Text(
                                  '${vant.spec.slotCount} ${vant.spec.slotCount == 1 ? 'SLOT' : 'SLOTS'}',
                                  style: const TextStyle(
                                    color: Color(0xFF94A3B8),
                                    fontSize: 9.5,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6.0),
                            Row(
                              children: List<Widget>.generate(slots.length,
                                  (int sIdx) {
                                final EquipmentItem? item = slots[sIdx];
                                final Color slotAccent = item != null
                                    ? item.spec.accentColor
                                    : (compatInv.isNotEmpty
                                        ? vant.spec.accentColor.withOpacity(0.6)
                                        : const Color(0xFF1E3A5F));
                                return Expanded(
                                  child: GestureDetector(
                                    onTap: () {
                                      if (item != null) {
                                        _unequipVantSlot(vant.id, sIdx);
                                      } else {
                                        _equipToVantSlot(vant.id, sIdx);
                                      }
                                    },
                                    child: Container(
                                      height: 50.0,
                                      margin: EdgeInsets.only(
                                        right:
                                            sIdx == slots.length - 1 ? 0.0 : 6.0,
                                      ),
                                      decoration: BoxDecoration(
                                        color: item != null
                                            ? slotAccent.withOpacity(0.16)
                                            : const Color(0xFF040914),
                                        borderRadius:
                                            BorderRadius.circular(8.0),
                                        border: Border.all(
                                          color: slotAccent,
                                          width: item != null ? 1.4 : 1.0,
                                        ),
                                      ),
                                      child: item == null
                                          ? Column(
                                              mainAxisAlignment:
                                                  MainAxisAlignment.center,
                                              children: <Widget>[
                                                Text(
                                                  '+',
                                                  style: TextStyle(
                                                    color: compatInv.isNotEmpty
                                                        ? vant.spec.accentColor
                                                        : const Color(
                                                            0xFF64748B),
                                                    fontSize: 12.0,
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                ),
                                                const Text(
                                                  'LSR/SHD',
                                                  style: TextStyle(
                                                    color: Color(0xFF64748B),
                                                    fontSize: 8.5,
                                                  ),
                                                ),
                                              ],
                                            )
                                          : Column(
                                              mainAxisAlignment:
                                                  MainAxisAlignment.center,
                                              children: <Widget>[
                                                Icon(
                                                  _categoryIcon(
                                                      item.spec.category),
                                                  color: slotAccent,
                                                  size: 13.0,
                                                ),
                                                Text(
                                                  item.spec.shortCode,
                                                  style: const TextStyle(
                                                    color: Color(0xFFF1F5F9),
                                                    fontSize: 9.5,
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                ),
                                                Text(
                                                  item.spec.statSummary,
                                                  maxLines: 1,
                                                  softWrap: false,
                                                  style: TextStyle(
                                                    color: slotAccent,
                                                    fontSize: 8.5,
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                ),
                                              ],
                                            ),
                                    ),
                                  ),
                                );
                              }),
                            ),
                          ],
                        ),
                      ),
                    );
                  }),
                ),
              );
            }),
          const SizedBox(height: 6.0),
          Container(
            padding:
                const EdgeInsets.symmetric(horizontal: 10.0, vertical: 8.0),
            decoration: BoxDecoration(
              color: const Color(0xFF071020),
              borderRadius: BorderRadius.circular(10.0),
              border: Border.all(color: const Color(0xFF1E3A5F)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Text(
                      'LASER & SHIELD INVENTORY (${compatInv.length})',
                      style: const TextStyle(
                        color: Color(0xFFF1F5F9),
                        fontSize: 11.5,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const Text(
                      'Tap item to equip into first empty VANT slot',
                      style:
                          TextStyle(color: Color(0xFF94A3B8), fontSize: 10.0),
                    ),
                  ],
                ),
                const SizedBox(height: 7.0),
                if (compatInv.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 4.0),
                    child: Text(
                      'No unequipped Lasers or Shield Generators in Inventory.',
                      style:
                          TextStyle(color: Color(0xFF94A3B8), fontSize: 11.0),
                    ),
                  )
                else
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: compatInv.map((EquipmentItem item) {
                        return Padding(
                          padding: const EdgeInsets.only(right: 7.0),
                          child: _EquipmentTile(
                            item: item,
                            emptyIcon: Icons.bolt,
                            onTap: () =>
                                _equipFromInventoryToVant(item.instanceId),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildShopTab(DarkOrbitGame game) {
    final Map<EquipmentItemId, int> counts = game.ownedCountsByType;
    const List<EquipmentItemId> baseItems = <EquipmentItemId>[
      EquipmentItemId.lf1,
      EquipmentItemId.sg1,
    ];

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          const Text(
            'SHIP CLASSES',
            style: TextStyle(
              color: Color(0xFF00E5FF),
              fontSize: 11.0,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.9,
            ),
          ),
          const SizedBox(height: 6.0),
          Row(
            children: ShipClass.values.map((ShipClass ship) {
              final ShipClassSpec spec = ShipClassCatalog.of(ship);
              final bool isOwned = game.ownedShipClasses.contains(ship);
              final bool isActive = game.activeShipClass == ship;
              final bool canAct =
                  (isOwned && !isActive) || (!isOwned && game.credits >= spec.cost);
              return Expanded(
                child: Container(
                  margin: EdgeInsets.only(
                    right: ship == ShipClass.values.last ? 0.0 : 8.0,
                  ),
                  padding: const EdgeInsets.all(10.0),
                  decoration: BoxDecoration(
                    color: isActive
                        ? spec.accentColor.withOpacity(0.12)
                        : const Color(0xFF071020),
                    borderRadius: BorderRadius.circular(10.0),
                    border: Border.all(
                      color: isActive
                          ? spec.accentColor
                          : spec.accentColor.withOpacity(0.45),
                      width: isActive ? 1.6 : 1.0,
                    ),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        spec.displayName,
                        maxLines: 1,
                        softWrap: false,
                        style: const TextStyle(
                          color: Color(0xFFF1F5F9),
                          fontSize: 12.5,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        spec.roleLabel,
                        maxLines: 1,
                        softWrap: false,
                        style: TextStyle(
                          color: spec.accentColor,
                          fontSize: 9.8,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 5.0),
                      Text(
                        'SPD ${spec.speed.toInt()} • HULL ${spec.baseHull.toInt()}',
                        maxLines: 1,
                        softWrap: false,
                        style: const TextStyle(
                          color: Color(0xFFE2E8F0),
                          fontSize: 10.5,
                        ),
                      ),
                      Text(
                        '${spec.laserSlotCount} Laser • ${spec.shieldSlotCount} Shield Slots',
                        maxLines: 1,
                        softWrap: false,
                        style: const TextStyle(
                          color: Color(0xFF94A3B8),
                          fontSize: 10.0,
                        ),
                      ),
                      const SizedBox(height: 7.0),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton(
                          onPressed: canAct ? () => _buyOrSelectShip(ship) : null,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: isOwned
                                ? const Color(0xFF00E5FF)
                                : spec.accentColor,
                            foregroundColor: const Color(0xFF040711),
                            disabledBackgroundColor: isActive
                                ? const Color(0x3322C55E)
                                : const Color(0xFF1A2438),
                            disabledForegroundColor: isActive
                                ? const Color(0xFF4ADE80)
                                : const Color(0xFF64748B),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8.0,
                              vertical: 6.0,
                            ),
                            minimumSize: const Size(0.0, 34.0),
                          ),
                          child: Text(
                            isActive
                                ? 'ACTIVE'
                                : (isOwned ? 'EQUIP' : 'BUY ${spec.cost} CR'),
                            maxLines: 1,
                            softWrap: false,
                            style: const TextStyle(
                              fontSize: 10.8,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
          const SizedBox(height: 12.0),
          const Text(
            'BASE LEVEL 1 EQUIPMENT',
            style: TextStyle(
              color: Color(0xFFFBBF24),
              fontSize: 11.0,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.9,
            ),
          ),
          const SizedBox(height: 6.0),
          Row(
            children: <Widget>[
              ...baseItems.map((EquipmentItemId id) {
                final EquipmentSpec spec = EquipmentCatalog.of(id);
                final int cost = spec.shopCost ?? 0;
                final int owned = counts[id] ?? 0;
                final bool canAfford = game.credits >= cost;
                return Expanded(
                  child: Container(
                    margin: const EdgeInsets.only(right: 8.0),
                    padding: const EdgeInsets.all(10.0),
                    decoration: BoxDecoration(
                      color: const Color(0xFF071020),
                      borderRadius: BorderRadius.circular(10.0),
                      border: Border.all(
                        color: spec.accentColor.withOpacity(0.5),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Row(
                          children: <Widget>[
                            Icon(
                              _categoryIcon(spec.category),
                              color: spec.accentColor,
                              size: 17.0,
                            ),
                            const SizedBox(width: 6.0),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: <Widget>[
                                  Text(
                                    spec.displayName,
                                    maxLines: 1,
                                    softWrap: false,
                                    style: const TextStyle(
                                      color: Color(0xFFF1F5F9),
                                      fontSize: 12.0,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  Text(
                                    '${spec.statSummary} • Owned: $owned',
                                    maxLines: 1,
                                    softWrap: false,
                                    style: TextStyle(
                                      color: spec.accentColor,
                                      fontSize: 10.0,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8.0),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton(
                            onPressed: canAfford ? () => _buyItem(id) : null,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: spec.accentColor,
                              foregroundColor: const Color(0xFF040711),
                              disabledBackgroundColor: const Color(0xFF1A2438),
                              disabledForegroundColor: const Color(0xFF64748B),
                              padding: const EdgeInsets.symmetric(
                                horizontal: 8.0,
                                vertical: 6.0,
                              ),
                              minimumSize: const Size(0.0, 34.0),
                            ),
                            child: Text(
                              'BUY $cost CR',
                              maxLines: 1,
                              softWrap: false,
                              style: const TextStyle(
                                fontSize: 10.8,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }),
              // 4th Base Item: Flax VANT (600 CR, max 8 VANTs total)
              Expanded(
                child: Builder(
                  builder: (BuildContext context) {
                    final int ownedVants = game.ownedVants.length;
                    final bool atCap = ownedVants >= GameConstants.maxVants;
                    final bool canBuy = !atCap &&
                        game.credits >= GameConstants.flaxVantCost;
                    return Container(
                      padding: const EdgeInsets.all(10.0),
                      decoration: BoxDecoration(
                        color: const Color(0xFF071020),
                        borderRadius: BorderRadius.circular(10.0),
                        border: Border.all(
                          color: const Color(0x8800E5FF),
                        ),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Row(
                            children: <Widget>[
                              const Icon(
                                Icons.hexagon_outlined,
                                color: Color(0xFF00E5FF),
                                size: 17.0,
                              ),
                              const SizedBox(width: 6.0),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: <Widget>[
                                    const Text(
                                      'Flax VANT',
                                      maxLines: 1,
                                      softWrap: false,
                                      style: TextStyle(
                                        color: Color(0xFFF1F5F9),
                                        fontSize: 12.0,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    Text(
                                      '1 Slot • $ownedVants/${GameConstants.maxVants}',
                                      maxLines: 1,
                                      softWrap: false,
                                      style: const TextStyle(
                                        color: Color(0xFF00E5FF),
                                        fontSize: 10.0,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8.0),
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton(
                              onPressed: canBuy ? _buyFlaxVant : null,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: const Color(0xFF00E5FF),
                                foregroundColor: const Color(0xFF040711),
                                disabledBackgroundColor:
                                    const Color(0xFF1A2438),
                                disabledForegroundColor:
                                    const Color(0xFF64748B),
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 8.0,
                                  vertical: 6.0,
                                ),
                                minimumSize: const Size(0.0, 34.0),
                              ),
                              child: Text(
                                atCap
                                    ? 'MAX 8 / 8'
                                    : 'BUY ${GameConstants.flaxVantCost} CR',
                                maxLines: 1,
                                softWrap: false,
                                style: const TextStyle(
                                  fontSize: 10.8,
                                  fontWeight: FontWeight.bold,
                                ),
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
          const SizedBox(height: 12.0),
          const Text(
            'DRONE FORMATIONS',
            style: TextStyle(
              color: Color(0xFF38BDF8),
              fontSize: 11.0,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.9,
            ),
          ),
          const SizedBox(height: 6.0),
          ...List<Widget>.generate(2, (int rowIdx) {
            final int start = rowIdx * 3;
            final List<DroneFormation> rowFormations =
                DroneFormation.values.sublist(start, start + 3);
            return Padding(
              padding: EdgeInsets.only(bottom: rowIdx == 0 ? 8.0 : 0.0),
              child: Row(
                children: List<Widget>.generate(rowFormations.length, (int c) {
                  final DroneFormation formation = rowFormations[c];
                  final DroneFormationSpec spec =
                      DroneFormationCatalog.of(formation);
                  final bool isOwned = game.ownedFormations.contains(formation);
                  final bool isActive = game.activeFormation == formation;
                  final bool canAct = (isOwned && !isActive) ||
                      (!isOwned && game.credits >= spec.cost);
                  return Expanded(
                    child: Container(
                      margin: EdgeInsets.only(right: c == 2 ? 0.0 : 8.0),
                      padding: const EdgeInsets.all(9.0),
                      decoration: BoxDecoration(
                        color: isActive
                            ? spec.accentColor.withOpacity(0.14)
                            : const Color(0xFF071020),
                        borderRadius: BorderRadius.circular(10.0),
                        border: Border.all(
                          color: isActive
                              ? spec.accentColor
                              : spec.accentColor.withOpacity(0.45),
                          width: isActive ? 1.6 : 1.0,
                        ),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Row(
                            children: <Widget>[
                              Container(
                                width: 30.0,
                                height: 30.0,
                                decoration: BoxDecoration(
                                  color: const Color(0xFF040914),
                                  borderRadius: BorderRadius.circular(7.0),
                                  border: Border.all(
                                    color: spec.accentColor.withOpacity(0.65),
                                  ),
                                ),
                                child: CustomPaint(
                                  painter: _FormationMiniPainter(
                                    formation: formation,
                                    accent: spec.accentColor,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 7.0),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: <Widget>[
                                    Text(
                                      spec.displayName,
                                      maxLines: 1,
                                      softWrap: false,
                                      style: const TextStyle(
                                        color: Color(0xFFF1F5F9),
                                        fontSize: 11.8,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    Text(
                                      spec.cost == 0
                                          ? 'DEFAULT'
                                          : '${spec.cost} CR',
                                      maxLines: 1,
                                      softWrap: false,
                                      style: TextStyle(
                                        color: spec.accentColor,
                                        fontSize: 9.8,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 5.0),
                          Text(
                            spec.bonusLabel,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: formation == DroneFormation.normal
                                  ? const Color(0xFF94A3B8)
                                  : const Color(0xFF4ADE80),
                              fontSize: 9.8,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          Text(
                            spec.penaltyLabel ?? 'No penalties',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: spec.penaltyLabel != null
                                  ? const Color(0xFFFF4D6D)
                                  : const Color(0xFF64748B),
                              fontSize: 9.6,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 6.0),
                          SizedBox(
                            width: double.infinity,
                            child: ElevatedButton(
                              onPressed: canAct
                                  ? () => _buyOrSelectFormation(formation)
                                  : null,
                              style: ElevatedButton.styleFrom(
                                backgroundColor: isOwned
                                    ? const Color(0xFF00E5FF)
                                    : spec.accentColor,
                                foregroundColor: const Color(0xFF040711),
                                disabledBackgroundColor: isActive
                                    ? const Color(0x3322C55E)
                                    : const Color(0xFF1A2438),
                                disabledForegroundColor: isActive
                                    ? const Color(0xFF4ADE80)
                                    : const Color(0xFF64748B),
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 8.0,
                                  vertical: 5.0,
                                ),
                                minimumSize: const Size(0.0, 32.0),
                              ),
                              child: Text(
                                isActive
                                    ? 'ACTIVE'
                                    : (isOwned
                                        ? 'EQUIP'
                                        : 'BUY ${spec.cost} CR'),
                                maxLines: 1,
                                softWrap: false,
                                style: const TextStyle(
                                  fontSize: 10.4,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }),
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildFuseTab(DarkOrbitGame game) {
    final Map<EquipmentItemId, int> counts = game.ownedCountsByType;
    final int flaxCount = game.flaxVantCount;
    final bool vantFuseReady = flaxCount >= 2;
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          const Text(
            '2-FOR-1 FUSION WORKSHOP',
            style: TextStyle(
              color: Color(0xFFFBBF24),
              fontSize: 11.5,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.9,
            ),
          ),
          const Text(
            'Combine 2 identical items or 2x Flax VANTs to forge 1 upgraded tier.',
            style: TextStyle(color: Color(0xFF94A3B8), fontSize: 10.5),
          ),
          const SizedBox(height: 8.0),
          // VANT DRONE FORGE: 2x Flax VANT -> 1x Iris VANT
          const Text(
            'VANT DRONE FORGE',
            style: TextStyle(
              color: Color(0xFFFBBF24),
              fontSize: 10.5,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 5.0),
          Container(
            padding:
                const EdgeInsets.symmetric(horizontal: 10.0, vertical: 8.0),
            decoration: BoxDecoration(
              color: vantFuseReady
                  ? const Color(0x22FBBF24)
                  : const Color(0xFF071020),
              borderRadius: BorderRadius.circular(10.0),
              border: Border.all(
                color: vantFuseReady
                    ? const Color(0xFFFBBF24)
                    : const Color(0xFF1E3A5F),
                width: vantFuseReady ? 1.5 : 1.0,
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        '2x FLAX VANT  →  1x IRIS VANT (2 UNIVERSAL SLOTS)',
                        maxLines: 1,
                        softWrap: false,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: Color(0xFFFBBF24),
                          fontSize: 11.0,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        'Safely unequips Flax Lasers/Shields back into Inventory before fusing.',
                        maxLines: 1,
                        softWrap: false,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: Color(0xFF94A3B8),
                          fontSize: 10.0,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8.0),
                ElevatedButton(
                  onPressed: vantFuseReady ? _fuseFlaxVants : null,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFFBBF24),
                    foregroundColor: const Color(0xFF040711),
                    disabledBackgroundColor: const Color(0xFF1A2438),
                    disabledForegroundColor: const Color(0xFF64748B),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12.0,
                      vertical: 6.0,
                    ),
                    minimumSize: const Size(0.0, 34.0),
                  ),
                  child: Text(
                    vantFuseReady
                        ? 'FUSE ($flaxCount/2)'
                        : 'NEED 2 ($flaxCount/2)',
                    maxLines: 1,
                    softWrap: false,
                    style: const TextStyle(
                      fontSize: 10.5,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8.0),
          _buildFusionRow(
            title: 'LASER FORGE',
            accent: const Color(0xFFFF6B35),
            recipes: FusionCatalog.recipes
                .where((FusionRecipe r) =>
                    EquipmentCatalog.of(r.source).category ==
                    EquipmentCategory.laser)
                .toList(),
            counts: counts,
          ),
          const SizedBox(height: 8.0),
          _buildFusionRow(
            title: 'SHIELD FORGE',
            accent: const Color(0xFF38BDF8),
            recipes: FusionCatalog.recipes
                .where((FusionRecipe r) =>
                    EquipmentCatalog.of(r.source).category ==
                    EquipmentCategory.shieldGenerator)
                .toList(),
            counts: counts,
          ),
          const SizedBox(height: 8.0),
          _buildFusionRow(
            title: 'SECTOR CHIP FORGE',
            accent: const Color(0xFFC084FC),
            recipes: FusionCatalog.recipes
                .where((FusionRecipe r) =>
                    EquipmentCatalog.of(r.source).category ==
                    EquipmentCategory.sectorChip)
                .toList(),
            counts: counts,
          ),
        ],
      ),
    );
  }

  Widget _buildFusionRow({
    required String title,
    required Color accent,
    required List<FusionRecipe> recipes,
    required Map<EquipmentItemId, int> counts,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Text(
          title,
          style: TextStyle(
            color: accent,
            fontSize: 10.5,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 5.0),
        Row(
          children: recipes.map((FusionRecipe r) {
            final EquipmentSpec src = EquipmentCatalog.of(r.source);
            final EquipmentSpec dst = EquipmentCatalog.of(r.result);
            final int owned = counts[r.source] ?? 0;
            final bool ready = owned >= 2;
            return Expanded(
              child: Container(
                margin: EdgeInsets.only(right: r == recipes.last ? 0.0 : 8.0),
                padding: const EdgeInsets.all(9.0),
                decoration: BoxDecoration(
                  color: ready
                      ? dst.accentColor.withOpacity(0.14)
                      : const Color(0xFF071020),
                  borderRadius: BorderRadius.circular(10.0),
                  border: Border.all(
                    color: ready ? dst.accentColor : const Color(0xFF1E3A5F),
                    width: ready ? 1.5 : 1.0,
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: <Widget>[
                        Text(
                          '2x ${src.shortCode}',
                          style: const TextStyle(
                            color: Color(0xFFF1F5F9),
                            fontSize: 10.5,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Icon(
                          Icons.arrow_forward,
                          size: 13.0,
                          color: dst.accentColor,
                        ),
                        Text(
                          '1x ${dst.shortCode}',
                          style: TextStyle(
                            color: dst.accentColor,
                            fontSize: 11.0,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 3.0),
                    Text(
                      '${dst.displayName} (${dst.statSummary})',
                      maxLines: 1,
                      softWrap: false,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        color: ready
                            ? const Color(0xFFF1F5F9)
                            : const Color(0xFF94A3B8),
                        fontSize: 10.0,
                      ),
                    ),
                    const SizedBox(height: 6.0),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        onPressed: ready ? () => _fuse(r.source) : null,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: dst.accentColor,
                          foregroundColor: const Color(0xFF040711),
                          disabledBackgroundColor: const Color(0xFF1A2438),
                          disabledForegroundColor: const Color(0xFF64748B),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8.0,
                            vertical: 5.0,
                          ),
                          minimumSize: const Size(0.0, 32.0),
                        ),
                        child: Text(
                          ready ? 'FUSE ($owned/2)' : 'NEED 2 ($owned/2)',
                          maxLines: 1,
                          softWrap: false,
                          style: const TextStyle(
                            fontSize: 10.5,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildMissionsTab(DarkOrbitGame game) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          const Text(
            'SECTOR MISSION CHAINS',
            style: TextStyle(
              color: Color(0xFF00E5FF),
              fontSize: 11.5,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.9,
            ),
          ),
          const Text(
            'Complete missions in order. Final sector missions require target Level and award Sector Chips.',
            style: TextStyle(color: Color(0xFF94A3B8), fontSize: 10.5),
          ),
          const SizedBox(height: 8.0),
          ...MissionCatalog.all.map((MissionSpec m) {
            final bool isDone = m.index < game.currentMissionIndex;
            final bool isCurrent = m.index == game.currentMissionIndex;
            final bool isLevelLocked =
                isCurrent && game.player.level < m.requiredLevel;
            final bool isReady = isCurrent && game.isMissionReadyToClaim;
            final Color accent = isDone || isReady
                ? const Color(0xFF22C55E)
                : (isLevelLocked
                    ? const Color(0xFFC084FC)
                    : (isCurrent
                        ? (m.isSectorFinal
                            ? const Color(0xFFFBBF24)
                            : const Color(0xFF00E5FF))
                        : const Color(0xFF1E3A5F)));

            return Container(
              margin: const EdgeInsets.only(bottom: 6.0),
              padding:
                  const EdgeInsets.symmetric(horizontal: 10.0, vertical: 8.0),
              decoration: BoxDecoration(
                color: isReady
                    ? const Color(0xFF062C1B)
                    : (isCurrent
                        ? const Color(0xFF0B182E)
                        : const Color(0xFF060C18)),
                borderRadius: BorderRadius.circular(10.0),
                border: Border.all(
                  color: accent,
                  width: isCurrent ? 1.5 : 1.0,
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: <Widget>[
                        Text(
                          '${m.code} • ${m.sectorChainLabel}${m.isSectorFinal ? ' (REQ LV ${m.requiredLevel})' : ''}',
                          maxLines: 1,
                          softWrap: false,
                          style: TextStyle(
                            color: accent,
                            fontSize: 10.0,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          m.description,
                          maxLines: 1,
                          softWrap: false,
                          style: const TextStyle(
                            color: Color(0xFFF1F5F9),
                            fontSize: 12.0,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Reward: ${m.rewardSummary}',
                          maxLines: 1,
                          softWrap: false,
                          style: TextStyle(
                            color: m.rewardChip != null
                                ? const Color(0xFFFBBF24)
                                : const Color(0xFF94A3B8),
                            fontSize: 10.0,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8.0),
                  if (isDone)
                    const Text(
                      'DONE',
                      style: TextStyle(
                        color: Color(0xFF4ADE80),
                        fontSize: 11.5,
                        fontWeight: FontWeight.bold,
                      ),
                    )
                  else if (isReady)
                    ElevatedButton(
                      onPressed: _claimMission,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF22C55E),
                        foregroundColor: const Color(0xFF040711),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12.0,
                          vertical: 6.0,
                        ),
                        minimumSize: const Size(0.0, 34.0),
                      ),
                      child: const Text(
                        'CLAIM REWARD',
                        maxLines: 1,
                        softWrap: false,
                        style: TextStyle(
                          fontSize: 10.5,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  else if (isLevelLocked)
                    Text(
                      'LOCKED — REACH LEVEL ${m.requiredLevel} TO UNLOCK',
                      maxLines: 1,
                      softWrap: false,
                      style: const TextStyle(
                        color: Color(0xFFE9D5FF),
                        fontSize: 10.0,
                        fontWeight: FontWeight.bold,
                      ),
                    )
                  else if (isCurrent)
                    Text(
                      '${game.currentMissionProgress} / ${m.targetCount}',
                      style: TextStyle(
                        color: accent,
                        fontSize: 13.0,
                        fontWeight: FontWeight.bold,
                      ),
                    )
                  else
                    const Text(
                      'LOCKED',
                      style: TextStyle(
                        color: Color(0xFF64748B),
                        fontSize: 10.5,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }
}

class _TabPill extends StatelessWidget {
  const _TabPill({
    required this.label,
    required this.selected,
    required this.onTap,
    this.showDot = false,
    this.dotColor = const Color(0xFF22C55E),
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;
  final bool showDot;
  final Color dotColor;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 9.0, vertical: 6.0),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFF00E5FF) : Colors.transparent,
          borderRadius: BorderRadius.circular(7.0),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Text(
              label,
              maxLines: 1,
              softWrap: false,
              style: TextStyle(
                color: selected
                    ? const Color(0xFF040711)
                    : const Color(0xFF94A3B8),
                fontSize: 10.5,
                fontWeight: FontWeight.bold,
              ),
            ),
            if (showDot) ...<Widget>[
              const SizedBox(width: 4.0),
              Container(
                width: 6.0,
                height: 6.0,
                decoration: BoxDecoration(
                  color: selected ? const Color(0xFF040711) : dotColor,
                  shape: BoxShape.circle,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _LiveStatItem extends StatelessWidget {
  const _LiveStatItem({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  final IconData icon;
  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        Icon(icon, color: color, size: 14.0),
        const SizedBox(width: 4.0),
        Text(
          '$label: ',
          maxLines: 1,
          softWrap: false,
          style: const TextStyle(
            color: Color(0xFF94A3B8),
            fontSize: 10.5,
            fontWeight: FontWeight.bold,
          ),
        ),
        Text(
          value,
          maxLines: 1,
          softWrap: false,
          style: TextStyle(
            color: color,
            fontSize: 12.0,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}

class _DynamicSlotPanel extends StatelessWidget {
  const _DynamicSlotPanel({
    required this.title,
    required this.accent,
    required this.slots,
    required this.columns,
    required this.emptyIcon,
    required this.onTapSlot,
  });

  final String title;
  final Color accent;
  final List<EquipmentItem?> slots;
  final int columns;
  final IconData emptyIcon;
  final ValueChanged<int> onTapSlot;

  @override
  Widget build(BuildContext context) {
    final int rows = (slots.length + columns - 1) ~/ columns;
    return Container(
      padding: const EdgeInsets.all(9.0),
      decoration: BoxDecoration(
        color: const Color(0xFF071020),
        borderRadius: BorderRadius.circular(10.0),
        border: Border.all(color: accent.withOpacity(0.45)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Text(
            title,
            maxLines: 1,
            softWrap: false,
            style: TextStyle(
              color: accent,
              fontSize: 11.0,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 6.0),
          for (int r = 0; r < rows; r++) ...<Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                for (int c = 0; c < columns; c++)
                  if (r * columns + c < slots.length)
                    _EquipmentTile(
                      item: slots[r * columns + c],
                      emptyIcon: emptyIcon,
                      compact: columns >= 4,
                      onTap: () => onTapSlot(r * columns + c),
                    )
                  else
                    const SizedBox(width: 52.0, height: 52.0),
              ],
            ),
            if (r < rows - 1) const SizedBox(height: 6.0),
          ],
        ],
      ),
    );
  }
}

class _EquipmentTile extends StatelessWidget {
  const _EquipmentTile({
    required this.item,
    required this.emptyIcon,
    required this.onTap,
    this.compact = false,
  });

  final EquipmentItem? item;
  final IconData emptyIcon;
  final VoidCallback onTap;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final EquipmentSpec? spec = item?.spec;
    final Color accent = spec?.accentColor ?? const Color(0xFF1E3A5F);
    final double dim = compact ? 52.0 : 58.0;

    return GestureDetector(
      onTap: item != null ? onTap : null,
      child: Container(
        width: dim,
        height: dim,
        decoration: BoxDecoration(
          color: spec != null
              ? accent.withOpacity(0.14)
              : const Color(0xFF040914),
          borderRadius: BorderRadius.circular(9.0),
          border: Border.all(
            color: spec != null
                ? accent.withOpacity(0.85)
                : const Color(0x881E3A5F),
            width: spec != null ? 1.5 : 1.0,
          ),
        ),
        child: spec == null
            ? Icon(emptyIcon, color: const Color(0x661E3A5F), size: 18.0)
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Icon(
                    _categoryIcon(spec.category),
                    color: accent,
                    size: compact ? 15.0 : 17.0,
                  ),
                  Text(
                    spec.shortCode,
                    maxLines: 1,
                    softWrap: false,
                    style: const TextStyle(
                      color: Color(0xFFF1F5F9),
                      fontSize: 9.5,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Text(
                    spec.statSummary,
                    maxLines: 1,
                    softWrap: false,
                    style: TextStyle(
                      color: accent,
                      fontSize: 8.5,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}

IconData _categoryIcon(EquipmentCategory category) {
  switch (category) {
    case EquipmentCategory.laser:
      return Icons.bolt;
    case EquipmentCategory.shieldGenerator:
      return Icons.shield;
    case EquipmentCategory.sectorChip:
      return Icons.memory;
  }
}

class _FormationMiniPainter extends CustomPainter {
  const _FormationMiniPainter({
    required this.formation,
    required this.accent,
  });

  final DroneFormation formation;
  final Color accent;

  @override
  void paint(Canvas canvas, Size size) {
    final double cx = size.width * 0.5;
    final double cy = size.height * 0.5;

    final Paint shipPaint = Paint()..color = const Color(0xCCFFFFFF);
    canvas.drawCircle(Offset(cx, cy), 2.1, shipPaint);

    final Paint dronePaint = Paint()..color = accent;
    final double previewScale =
        formation == DroneFormation.wheel ? 0.095 : 0.125;

    for (int i = 0; i < 8; i++) {
      final DroneFormationSlot slot = DroneFormationCatalog.computeLocalSlot(
        formation: formation,
        index: i,
        total: 8,
        shipHullRadius: 24.0,
        elapsedTime: 0.3,
      );
      // Rotate -90 degrees so the ship's forward (+X) axis points upward.
      final double px = cx + slot.localY * previewScale;
      final double py = cy - slot.localX * previewScale;
      canvas.drawCircle(Offset(px, py), 1.75, dronePaint);
    }
  }

  @override
  bool shouldRepaint(covariant _FormationMiniPainter oldDelegate) {
    return oldDelegate.formation != formation || oldDelegate.accent != accent;
  }
}

