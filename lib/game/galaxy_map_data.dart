import 'dart:collection';
import 'dart:math';
import 'dart:ui';

import 'game_models.dart';

/// Risk & gameplay classification for every sector in the 25-sector Orbi Space galaxy.
enum ZoneType {
  /// Capital Cities (C1, C2, C3): Safe Zone with Base, Market, and Vault.
  capitalCity,

  /// Inner Spoke Level 1 (1-1, 2-1, 3-1): Safe Zone, Tier 1–2 resources.
  safeZone,

  /// Outer Ring (R1-1..R3-4): Intermediate Zone, Tier 1–2, no full ship loss.
  intermediateZone,

  /// Inner Spoke Level 2 & Inner Ring (1-2, 2-2, 3-2): Moderate Zone, Tier 3–4.
  moderateZone,

  /// Inner Spoke Level 3 (1-3, 2-3, 3-3): High-Risk / Yellow Ring, Tier 4–5, Full-Loot, Yellow Crystal.
  highRiskYellowRing,

  /// The Nucleus (0-0): Large High-Risk Full-Loot Sector with a Central Safe Zone (Black Market).
  nucleus,
}

extension ZoneTypeMeta on ZoneType {
  String get shortLabel {
    switch (this) {
      case ZoneType.capitalCity:
        return 'CAPITAL SAFE ZONE';
      case ZoneType.safeZone:
        return 'SAFE ZONE';
      case ZoneType.intermediateZone:
        return 'INTERMEDIATE';
      case ZoneType.moderateZone:
        return 'MODERATE RISK';
      case ZoneType.highRiskYellowRing:
        return 'HIGH-RISK • YELLOW RING';
      case ZoneType.nucleus:
        return 'NUCLEUS • FULL-LOOT';
    }
  }

  String get riskDescription {
    switch (this) {
      case ZoneType.capitalCity:
        return 'Safe Zone — Base, Market & Vault (No Combat)';
      case ZoneType.safeZone:
        return 'Safe Zone — Protected Patrol Space (No Full Loss)';
      case ZoneType.intermediateZone:
        return 'Intermediate Zone — No Full Ship Loss';
      case ZoneType.moderateZone:
        return 'Moderate Zone — Contested Inner Ring';
      case ZoneType.highRiskYellowRing:
        return 'High-Risk (Full-Loot • Yellow Crystal)';
      case ZoneType.nucleus:
        return 'High-Risk (Full-Loot • Central Black Market Safe Zone)';
    }
  }

  Color get badgeColor {
    switch (this) {
      case ZoneType.capitalCity:
        return const Color(0xFF00E5FF);
      case ZoneType.safeZone:
        return const Color(0xFF38BDF8);
      case ZoneType.intermediateZone:
        return const Color(0xFF4ADE80);
      case ZoneType.moderateZone:
        return const Color(0xFFFB923C);
      case ZoneType.highRiskYellowRing:
        return const Color(0xFFFACC15);
      case ZoneType.nucleus:
        return const Color(0xFFFBBF24);
    }
  }

  Color get badgeBgColor {
    switch (this) {
      case ZoneType.capitalCity:
        return const Color(0xCC062838);
      case ZoneType.safeZone:
        return const Color(0xCC0A2239);
      case ZoneType.intermediateZone:
        return const Color(0xCC072415);
      case ZoneType.moderateZone:
        return const Color(0xCC2C1506);
      case ZoneType.highRiskYellowRing:
        return const Color(0xCC2E2305);
      case ZoneType.nucleus:
        return const Color(0xCC301D04);
    }
  }

  Color get spaceBackgroundColor {
    switch (this) {
      case ZoneType.capitalCity:
        return const Color(0xFF050A18);
      case ZoneType.safeZone:
        return const Color(0xFF060B1A);
      case ZoneType.intermediateZone:
        return const Color(0xFF07071C);
      case ZoneType.moderateZone:
        return const Color(0xFF0D081C);
      case ZoneType.highRiskYellowRing:
        return const Color(0xFF130E05);
      case ZoneType.nucleus:
        return const Color(0xFF140D04);
    }
  }
}

/// Immutable data definition for a single sector in the 25-sector Galactic Map.
class SectorNode {
  const SectorNode({
    required this.id,
    required this.name,
    required this.position,
    required this.zoneType,
    required this.resourceTiers,
    required this.minTier,
    required this.maxTier,
    required this.connectedSectorIds,
    this.shortcutSectorIds = const <String>{},
    this.hasCentralSafeZone = false,
    this.isEntireSectorSafe = false,
    this.isLargeSector = false,
    this.mapSize = 14000.0,
    this.safeZoneRadius = 0.0,
    required this.nodeColor,
    this.factionTag,
  });

  /// Unique sector code (e.g. 'C1', 'R1-3', '1-2', '0-0').
  final String id;

  /// Human-readable sector name.
  final String name;

  /// Normalized coordinates in [0.0, 1.0] matching the Galactic Flight Map layout.
  final Offset position;

  /// Zone risk category.
  final ZoneType zoneType;

  /// Display text for available resource/enemy tiers (e.g. 'Tier 4–5 • Yellow Crystal').
  final String resourceTiers;

  /// Minimum and maximum gameplay scaling tiers (1..5).
  final int minTier;
  final int maxTier;

  /// Bidirectional portal connections to adjacent sectors.
  final List<String> connectedSectorIds;

  /// Subset of [connectedSectorIds] that are Outer-to-Inner dashed border shortcuts.
  final Set<String> shortcutSectorIds;

  /// True for sectors with a Central Safe Zone where combat is disabled (Cities, Level 1, and Nucleus 0-0 Black Market).
  final bool hasCentralSafeZone;

  /// True for Capital Cities (C1, C2, C3) where the entire station perimeter is non-combat.
  final bool isEntireSectorSafe;

  /// True for The Nucleus (0-0), which has a physically larger playable map area.
  final bool isLargeSector;

  /// Square world width & height for this sector (standard 14,000; Nucleus 22,000; Capital 10,000).
  final double mapSize;

  /// Radius of the central safe zone at `(mapSize / 2, mapSize / 2)` when [hasCentralSafeZone] is true.
  final double safeZoneRadius;

  /// Glowing primary node color on the Galactic Map.
  final Color nodeColor;

  /// Optional faction tag for Capital Cities ('AET', 'VER', 'SOL').
  final String? factionTag;

  bool isShortcutTo(String neighborId) => shortcutSectorIds.contains(neighborId);

  bool get isCapitalCity => zoneType == ZoneType.capitalCity;
  bool get isNucleus => zoneType == ZoneType.nucleus;
  bool get isShortcutHub => shortcutSectorIds.isNotEmpty && id.startsWith('R');

  String get safeZoneStatusLabel {
    if (isCapitalCity) return 'Full Safe Zone (Base • Market • Vault)';
    if (isNucleus && hasCentralSafeZone) {
      return 'Central Safe Zone (Black Market — No Combat)';
    }
    if (hasCentralSafeZone) return 'Central Safe Zone Outpost';
    return 'No Safe Zone (Active Combat Space)';
  }
}

/// Represents a unique undirected portal link between two sectors on the Galactic Map.
class GalaxyPortalLink {
  const GalaxyPortalLink({
    required this.fromId,
    required this.toId,
    required this.isShortcut,
    required this.isInnerRing,
    required this.isOuterRing,
  });

  final String fromId;
  final String toId;
  final bool isShortcut;
  final bool isInnerRing;
  final bool isOuterRing;
}

/// World-space portal placed inside the currently active 2D sector.
class SectorPortal {
  const SectorPortal({
    required this.targetSectorId,
    required this.worldX,
    required this.worldY,
    required this.isShortcut,
    required this.accentColor,
  });

  final String targetSectorId;
  final double worldX;
  final double worldY;
  final bool isShortcut;
  final Color accentColor;

  String get gateLabel => isShortcut
      ? 'SHORTCUT: $targetSectorId'
      : 'GATE: $targetSectorId';
}

/// Helper to compute normalized polar coordinates around `(0.50, 0.51)`.
Offset _polar(double angleDeg, double radius) {
  final double rad = angleDeg * pi / 180.0;
  return Offset(0.50 + radius * cos(rad), 0.51 + radius * sin(rad));
}

/// Complete 25-sector Galactic Map dataset (3 Capital Cities + 12 Outer Ring + 9 Inner Spokes + 1 Nucleus).
final Map<String, SectorNode> galaxyMapData = <String, SectorNode>{
  // =========================================================================
  // 3 CAPITAL CITIES (Safe Zone — Base, Market, Vault)
  // =========================================================================
  'C1': SectorNode(
    id: 'C1',
    name: 'City 1 — Aether Prime',
    position: _polar(-90.0, 0.385),
    zoneType: ZoneType.capitalCity,
    resourceTiers: 'Base • Market • Vault',
    minTier: 1,
    maxTier: 1,
    connectedSectorIds: const <String>['R1-1', 'R3-4', '1-1'],
    hasCentralSafeZone: true,
    isEntireSectorSafe: true,
    mapSize: 10000.0,
    safeZoneRadius: 1100.0,
    nodeColor: const Color(0xFF38BDF8),
    factionTag: 'AET',
  ),
  'C2': SectorNode(
    id: 'C2',
    name: 'City 2 — Verdant Haven',
    position: _polar(30.0, 0.385),
    zoneType: ZoneType.capitalCity,
    resourceTiers: 'Base • Market • Vault',
    minTier: 1,
    maxTier: 1,
    connectedSectorIds: const <String>['R2-1', 'R1-4', '2-1'],
    hasCentralSafeZone: true,
    isEntireSectorSafe: true,
    mapSize: 10000.0,
    safeZoneRadius: 1100.0,
    nodeColor: const Color(0xFF22C55E),
    factionTag: 'VER',
  ),
  'C3': SectorNode(
    id: 'C3',
    name: 'City 3 — Solaris Forge',
    position: _polar(150.0, 0.385),
    zoneType: ZoneType.capitalCity,
    resourceTiers: 'Base • Market • Vault',
    minTier: 1,
    maxTier: 1,
    connectedSectorIds: const <String>['R3-1', 'R2-4', '3-1'],
    hasCentralSafeZone: true,
    isEntireSectorSafe: true,
    mapSize: 10000.0,
    safeZoneRadius: 1100.0,
    nodeColor: const Color(0xFFFF3B5C),
    factionTag: 'SOL',
  ),

  // =========================================================================
  // OUTER RING (12 Sectors — Intermediate Zone, Tier 1–2, No Full Ship Loss)
  // =========================================================================
  // Arc 1: C1 (-90°) -> C2 (+30°)
  'R1-1': SectorNode(
    id: 'R1-1',
    name: 'Outer Rim 1-1',
    position: _polar(-66.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['C1', 'R1-2'],
    nodeColor: const Color(0xFF38BDF8),
  ),
  'R1-2': SectorNode(
    id: 'R1-2',
    name: 'Outer Rim 1-2',
    position: _polar(-42.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R1-1', 'R1-3'],
    nodeColor: const Color(0xFF38BDF8),
  ),
  'R1-3': SectorNode(
    id: 'R1-3',
    name: 'Outer Relay 1-3',
    position: _polar(-18.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2 • Border Shortcut',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R1-2', 'R1-4', '2-2'],
    shortcutSectorIds: const <String>{'2-2'},
    nodeColor: const Color(0xFF34D399),
  ),
  'R1-4': SectorNode(
    id: 'R1-4',
    name: 'Outer Rim 1-4',
    position: _polar(6.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R1-3', 'C2'],
    nodeColor: const Color(0xFF38BDF8),
  ),

  // Arc 2: C2 (+30°) -> C3 (+150°)
  'R2-1': SectorNode(
    id: 'R2-1',
    name: 'Outer Rim 2-1',
    position: _polar(54.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['C2', 'R2-2'],
    nodeColor: const Color(0xFF38BDF8),
  ),
  'R2-2': SectorNode(
    id: 'R2-2',
    name: 'Outer Rim 2-2',
    position: _polar(78.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R2-1', 'R2-3'],
    nodeColor: const Color(0xFF38BDF8),
  ),
  'R2-3': SectorNode(
    id: 'R2-3',
    name: 'Outer Relay 2-3',
    position: _polar(102.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2 • Border Shortcut',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R2-2', 'R2-4', '3-2'],
    shortcutSectorIds: const <String>{'3-2'},
    nodeColor: const Color(0xFF34D399),
  ),
  'R2-4': SectorNode(
    id: 'R2-4',
    name: 'Outer Rim 2-4',
    position: _polar(126.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R2-3', 'C3'],
    nodeColor: const Color(0xFF38BDF8),
  ),

  // Arc 3: C3 (+150°) -> C1 (+270° / -90°)
  'R3-1': SectorNode(
    id: 'R3-1',
    name: 'Outer Rim 3-1',
    position: _polar(174.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['C3', 'R3-2'],
    nodeColor: const Color(0xFF38BDF8),
  ),
  'R3-2': SectorNode(
    id: 'R3-2',
    name: 'Outer Rim 3-2',
    position: _polar(198.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R3-1', 'R3-3'],
    nodeColor: const Color(0xFF38BDF8),
  ),
  'R3-3': SectorNode(
    id: 'R3-3',
    name: 'Outer Relay 3-3',
    position: _polar(222.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2 • Border Shortcut',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R3-2', 'R3-4', '1-2'],
    shortcutSectorIds: const <String>{'1-2'},
    nodeColor: const Color(0xFF34D399),
  ),
  'R3-4': SectorNode(
    id: 'R3-4',
    name: 'Outer Rim 3-4',
    position: _polar(246.0, 0.385),
    zoneType: ZoneType.intermediateZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['R3-3', 'C1'],
    nodeColor: const Color(0xFF38BDF8),
  ),

  // =========================================================================
  // INNER SPOKES (9 Sectors — 3 per Capital City)
  // =========================================================================
  // Level 1 (Safe Zone, Tier 1–2)
  '1-1': SectorNode(
    id: '1-1',
    name: 'Sector 1-1 — Aether Approach',
    position: _polar(-90.0, 0.305),
    zoneType: ZoneType.safeZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['C1', '1-2'],
    hasCentralSafeZone: true,
    safeZoneRadius: 540.0,
    nodeColor: const Color(0xFF38BDF8),
  ),
  '2-1': SectorNode(
    id: '2-1',
    name: 'Sector 2-1 — Verdant Approach',
    position: _polar(30.0, 0.305),
    zoneType: ZoneType.safeZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['C2', '2-2'],
    hasCentralSafeZone: true,
    safeZoneRadius: 540.0,
    nodeColor: const Color(0xFF22C55E),
  ),
  '3-1': SectorNode(
    id: '3-1',
    name: 'Sector 3-1 — Solaris Approach',
    position: _polar(150.0, 0.305),
    zoneType: ZoneType.safeZone,
    resourceTiers: 'Tier 1–2',
    minTier: 1,
    maxTier: 2,
    connectedSectorIds: const <String>['C3', '3-2'],
    hasCentralSafeZone: true,
    safeZoneRadius: 540.0,
    nodeColor: const Color(0xFFFF3B5C),
  ),

  // Level 2 (Moderate Zone, Tier 3–4 — Connected in the Inner Ring)
  '1-2': SectorNode(
    id: '1-2',
    name: 'Sector 1-2 — North Ring Hub',
    position: _polar(-90.0, 0.228),
    zoneType: ZoneType.moderateZone,
    resourceTiers: 'Tier 3–4',
    minTier: 3,
    maxTier: 4,
    connectedSectorIds: const <String>['1-1', '1-3', '2-2', '3-2', 'R3-3'],
    shortcutSectorIds: const <String>{'R3-3'},
    nodeColor: const Color(0xFF00E5FF),
  ),
  '2-2': SectorNode(
    id: '2-2',
    name: 'Sector 2-2 — East Ring Hub',
    position: _polar(30.0, 0.228),
    zoneType: ZoneType.moderateZone,
    resourceTiers: 'Tier 3–4',
    minTier: 3,
    maxTier: 4,
    connectedSectorIds: const <String>['2-1', '2-3', '3-2', '1-2', 'R1-3'],
    shortcutSectorIds: const <String>{'R1-3'},
    nodeColor: const Color(0xFF2DD4BF),
  ),
  '3-2': SectorNode(
    id: '3-2',
    name: 'Sector 3-2 — West Ring Hub',
    position: _polar(150.0, 0.228),
    zoneType: ZoneType.moderateZone,
    resourceTiers: 'Tier 3–4',
    minTier: 3,
    maxTier: 4,
    connectedSectorIds: const <String>['3-1', '3-3', '1-2', '2-2', 'R2-3'],
    shortcutSectorIds: const <String>{'R2-3'},
    nodeColor: const Color(0xFFFB7185),
  ),

  // Level 3 (High-Risk / Yellow Ring, Tier 4–5, Full-Loot, Yellow Crystal)
  '1-3': SectorNode(
    id: '1-3',
    name: 'Sector 1-3 — North Yellow Ring',
    position: _polar(-90.0, 0.148),
    zoneType: ZoneType.highRiskYellowRing,
    resourceTiers: 'Tier 4–5 • Yellow Crystal',
    minTier: 4,
    maxTier: 5,
    connectedSectorIds: const <String>['1-2', '0-0'],
    mapSize: 16000.0,
    nodeColor: const Color(0xFFFACC15),
  ),
  '2-3': SectorNode(
    id: '2-3',
    name: 'Sector 2-3 — East Yellow Ring',
    position: _polar(30.0, 0.148),
    zoneType: ZoneType.highRiskYellowRing,
    resourceTiers: 'Tier 4–5 • Yellow Crystal',
    minTier: 4,
    maxTier: 5,
    connectedSectorIds: const <String>['2-2', '0-0'],
    mapSize: 16000.0,
    nodeColor: const Color(0xFFFACC15),
  ),
  '3-3': SectorNode(
    id: '3-3',
    name: 'Sector 3-3 — West Yellow Ring',
    position: _polar(150.0, 0.148),
    zoneType: ZoneType.highRiskYellowRing,
    resourceTiers: 'Tier 4–5 • Yellow Crystal',
    minTier: 4,
    maxTier: 5,
    connectedSectorIds: const <String>['3-2', '0-0'],
    mapSize: 16000.0,
    nodeColor: const Color(0xFFFACC15),
  ),

  // =========================================================================
  // THE NUCLEUS (0-0 — Large High-Risk Sector with Central Black Market Safe Zone)
  // =========================================================================
  '0-0': const SectorNode(
    id: '0-0',
    name: 'Nucleus (Black Market)',
    position: Offset(0.50, 0.51),
    zoneType: ZoneType.nucleus,
    resourceTiers: 'Tier 5 • Yellow Crystal • Black Market',
    minTier: 5,
    maxTier: 5,
    connectedSectorIds: <String>['1-3', '2-3', '3-3'],
    hasCentralSafeZone: true,
    isLargeSector: true,
    mapSize: 22000.0,
    safeZoneRadius: 950.0,
    nodeColor: Color(0xFFFBBF24),
  ),
};

/// Utility methods for querying the 25-sector galaxy topology, BFS pathfinding, and in-sector portals.
class GalaxyMapGraph {
  static const String defaultSectorId = 'C1';

  static SectorNode sectorOf(String sectorId) =>
      galaxyMapData[sectorId] ?? galaxyMapData[defaultSectorId]!;

  /// Maps a pilot's faction company to their home Capital City.
  static String homeCityForCompany(PilotCompany? company) {
    switch (company) {
      case PilotCompany.aether:
        return 'C1';
      case PilotCompany.verdant:
        return 'C2';
      case PilotCompany.solaris:
        return 'C3';
      case null:
        return defaultSectorId;
    }
  }

  /// Returns all unique undirected portal links across the 25-sector galaxy.
  static final List<GalaxyPortalLink> allLinks = _buildUniqueLinks();

  static List<GalaxyPortalLink> _buildUniqueLinks() {
    const Set<String> innerRingIds = <String>{'1-2', '2-2', '3-2'};
    const Set<String> outerRingIds = <String>{
      'C1', 'C2', 'C3',
      'R1-1', 'R1-2', 'R1-3', 'R1-4',
      'R2-1', 'R2-2', 'R2-3', 'R2-4',
      'R3-1', 'R3-2', 'R3-3', 'R3-4',
    };
    final Set<String> seen = <String>{};
    final List<GalaxyPortalLink> result = <GalaxyPortalLink>[];
    for (final SectorNode node in galaxyMapData.values) {
      for (final String neighborId in node.connectedSectorIds) {
        if (!galaxyMapData.containsKey(neighborId)) continue;
        final String key = node.id.compareTo(neighborId) < 0
            ? '${node.id}|$neighborId'
            : '$neighborId|${node.id}';
        if (!seen.add(key)) continue;
        final bool isShortcut = node.isShortcutTo(neighborId) ||
            (galaxyMapData[neighborId]?.isShortcutTo(node.id) ?? false);
        final bool isInnerRing = !isShortcut &&
            innerRingIds.contains(node.id) &&
            innerRingIds.contains(neighborId);
        final bool isOuterRing = !isShortcut &&
            outerRingIds.contains(node.id) &&
            outerRingIds.contains(neighborId);
        result.add(
          GalaxyPortalLink(
            fromId: node.id,
            toId: neighborId,
            isShortcut: isShortcut,
            isInnerRing: isInnerRing,
            isOuterRing: isOuterRing,
          ),
        );
      }
    }
    return List<GalaxyPortalLink>.unmodifiable(result);
  }

  /// Computes the shortest path (inclusive of [startId] and [targetId]) via Breadth-First Search (BFS).
  static List<String> findShortestRoute(String startId, String targetId) {
    if (!galaxyMapData.containsKey(startId) ||
        !galaxyMapData.containsKey(targetId)) {
      return const <String>[];
    }
    if (startId == targetId) return <String>[startId];

    final Queue<String> queue = Queue<String>()..add(startId);
    final Map<String, String?> parent = <String, String?>{startId: null};

    while (queue.isNotEmpty) {
      final String current = queue.removeFirst();
      if (current == targetId) break;
      final SectorNode? node = galaxyMapData[current];
      if (node == null) continue;
      for (final String next in node.connectedSectorIds) {
        if (!parent.containsKey(next) && galaxyMapData.containsKey(next)) {
          parent[next] = current;
          queue.add(next);
        }
      }
    }

    if (!parent.containsKey(targetId)) return const <String>[];
    final List<String> path = <String>[];
    String? cur = targetId;
    while (cur != null) {
      path.add(cur);
      cur = parent[cur];
    }
    return path.reversed.toList(growable: false);
  }

  /// Deterministically generates world-space jump gate portals inside [sectorId],
  /// oriented toward each neighbor's true compass bearing on the Galactic Map.
  static List<SectorPortal> portalsForSector(String sectorId) {
    final SectorNode current = sectorOf(sectorId);
    final double half = current.mapSize * 0.5;
    final double ringRadius = current.isCapitalCity
        ? 1350.0
        : (current.isLargeSector ? current.mapSize * 0.37 : current.mapSize * 0.36);

    final List<SectorPortal> portals = <SectorPortal>[];
    for (int i = 0; i < current.connectedSectorIds.length; i++) {
      final String neighborId = current.connectedSectorIds[i];
      final SectorNode neighbor = sectorOf(neighborId);
      final double dx = neighbor.position.dx - current.position.dx;
      final double dy = neighbor.position.dy - current.position.dy;
      double angle = (dx == 0.0 && dy == 0.0)
          ? (i * (2.0 * pi / max(1, current.connectedSectorIds.length)))
          : atan2(dy, dx);

      // Slight deterministic nudge if two neighbors share nearly identical bearing
      for (final SectorPortal existing in portals) {
        final double ex = existing.worldX - half;
        final double ey = existing.worldY - half;
        final double existingAngle = atan2(ey, ex);
        double diff = angle - existingAngle;
        while (diff > pi) {
          diff -= 2.0 * pi;
        }
        while (diff < -pi) {
          diff += 2.0 * pi;
        }
        if (diff.abs() < 0.34) {
          angle += 0.42;
        }
      }

      final double px =
          (half + cos(angle) * ringRadius).clamp(900.0, current.mapSize - 900.0);
      final double py =
          (half + sin(angle) * ringRadius).clamp(900.0, current.mapSize - 900.0);
      final bool shortcut = current.isShortcutTo(neighborId);
      portals.add(
        SectorPortal(
          targetSectorId: neighborId,
          worldX: px,
          worldY: py,
          isShortcut: shortcut,
          accentColor: shortcut
              ? const Color(0xFF34D399)
              : neighbor.nodeColor,
        ),
      );
    }
    return portals;
  }
}
