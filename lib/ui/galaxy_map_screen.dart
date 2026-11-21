import 'dart:math';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import '../game/dark_orbit_game.dart';
import '../game/galaxy_map_data.dart';

/// Interactive pan/zoom 25-sector Galactic Flight Map overlay for Orbi Space.
class GalaxyMapScreen extends StatefulWidget {
  const GalaxyMapScreen({
    super.key,
    required this.game,
  });

  final DarkOrbitGame game;

  @override
  State<GalaxyMapScreen> createState() => _GalaxyMapScreenState();
}

class _GalaxyMapScreenState extends State<GalaxyMapScreen>
    with SingleTickerProviderStateMixin {
  late final Ticker _ticker;
  final TransformationController _transformController =
      TransformationController();
  double _animTime = 0.0;
  String? _selectedSectorId;

  @override
  void initState() {
    super.initState();
    _selectedSectorId = widget.game.currentSectorId;
    _ticker = createTicker((Duration elapsed) {
      if (!mounted) return;
      setState(() {
        _animTime = elapsed.inMilliseconds / 1000.0;
      });
    })
      ..start();
  }

  @override
  void dispose() {
    _ticker.dispose();
    _transformController.dispose();
    super.dispose();
  }

  void _handleCanvasTap(TapUpDetails details, double canvasSize) {
    final Offset tap = details.localPosition;
    String? closestId;
    double closestDist = double.infinity;

    for (final SectorNode node in galaxyMapData.values) {
      final Offset nodeCenter = Offset(
        node.position.dx * canvasSize,
        node.position.dy * canvasSize,
      );
      final double hitRadius = node.isNucleus
          ? 46.0
          : (node.isCapitalCity ? 34.0 : 24.0);
      final double dist = (tap - nodeCenter).distance;
      if (dist <= hitRadius && dist < closestDist) {
        closestDist = dist;
        closestId = node.id;
      }
    }

    setState(() {
      _selectedSectorId = closestId;
    });
  }

  void _plotRouteTo(String targetId) {
    final List<String> path = GalaxyMapGraph.findShortestRoute(
      widget.game.currentSectorId,
      targetId,
    );
    if (path.length >= 2) {
      widget.game.setPlottedRoute(path);
    } else {
      widget.game.clearPlottedRoute();
    }
    setState(() {});
  }

  void _clearRoute() {
    widget.game.clearPlottedRoute();
    setState(() {});
  }

  void _autopilotToNextHop() {
    if (widget.game.engageAutopilotToNextRoutePortal()) {
      widget.game.closeGalaxyMap();
    }
  }

  void _resetZoom() {
    _transformController.value = Matrix4.identity();
  }

  @override
  Widget build(BuildContext context) {
    final Size screenSize = MediaQuery.of(context).size;
    final String currentSectorId = widget.game.currentSectorId;
    final SectorNode currentSector = GalaxyMapGraph.sectorOf(currentSectorId);
    final List<String> plottedRoute = widget.game.plottedRoute;
    final SectorNode? selectedNode = _selectedSectorId != null
        ? galaxyMapData[_selectedSectorId]
        : null;

    // Fit square map cleanly on landscape mobile/tablet screens with generous pan/zoom room
    final double mapCanvasSize = max(
      420.0,
      min(screenSize.width * 0.90, screenSize.height * 1.15),
    );

    return Material(
      color: const Color(0xEC030714),
      child: SafeArea(
        child: Stack(
          children: <Widget>[
            // Full-bleed subtle radial deep-space gradient + sci-fi corner radar frames
            Positioned.fill(
              child: CustomPaint(
                painter: _GalaxyBackdropPainter(animTime: _animTime),
              ),
            ),

            // Interactive Pan & Zoom Galactic Map Viewport
            Positioned.fill(
              child: InteractiveViewer(
                transformationController: _transformController,
                minScale: 0.65,
                maxScale: 2.8,
                boundaryMargin: const EdgeInsets.all(140.0),
                child: Center(
                  child: GestureDetector(
                    behavior: HitTestBehavior.opaque,
                    onTapUp: (TapUpDetails d) =>
                        _handleCanvasTap(d, mapCanvasSize),
                    child: SizedBox(
                      width: mapCanvasSize,
                      height: mapCanvasSize,
                      child: CustomPaint(
                        painter: _GalaxyMapPainter(
                          animTime: _animTime,
                          currentSectorId: currentSectorId,
                          selectedSectorId: _selectedSectorId,
                          plottedRoute: plottedRoute,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),

            // Top HUD Header Bar
            Positioned(
              left: 14.0,
              right: 14.0,
              top: 10.0,
              child: Row(
                children: <Widget>[
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12.0,
                      vertical: 7.0,
                    ),
                    decoration: BoxDecoration(
                      color: const Color(0xD9061124),
                      borderRadius: BorderRadius.circular(10.0),
                      border: Border.all(
                        color: const Color(0xAA00E5FF),
                        width: 1.2,
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        const Icon(
                          Icons.hub_outlined,
                          color: Color(0xFF00E5FF),
                          size: 16.0,
                        ),
                        const SizedBox(width: 8.0),
                        const Text(
                          'ORBI SPACE • GALACTIC FLIGHT MAP',
                          style: TextStyle(
                            color: Color(0xFF00E5FF),
                            fontSize: 11.5,
                            fontWeight: FontWeight.w800,
                            letterSpacing: 1.0,
                          ),
                        ),
                        const SizedBox(width: 10.0),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 7.0,
                            vertical: 2.0,
                          ),
                          decoration: BoxDecoration(
                            color: currentSector.zoneType.badgeBgColor,
                            borderRadius: BorderRadius.circular(6.0),
                            border: Border.all(
                              color: currentSector.nodeColor,
                              width: 1.0,
                            ),
                          ),
                          child: Text(
                            'LOC: ${currentSector.id}',
                            style: TextStyle(
                              color: currentSector.nodeColor,
                              fontSize: 10.5,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Spacer(),
                  _HudIconPill(
                    icon: Icons.center_focus_strong,
                    label: 'RESET',
                    accent: const Color(0xFF38BDF8),
                    onTap: _resetZoom,
                  ),
                  const SizedBox(width: 8.0),
                  _HudIconPill(
                    icon: Icons.radar,
                    label: 'LOCAL',
                    accent: const Color(0xFFA5B4FC),
                    onTap: () {
                      widget.game.closeGalaxyMap();
                      widget.game.openTacticalMap();
                    },
                  ),
                  const SizedBox(width: 8.0),
                  _HudIconPill(
                    icon: Icons.close,
                    label: 'CLOSE',
                    accent: const Color(0xFFFF2E4C),
                    filled: true,
                    onTap: widget.game.closeGalaxyMap,
                  ),
                ],
              ),
            ),

            // Active Plotted Route Status Banner (Top-Center / Bottom-Left)
            if (plottedRoute.length >= 2)
              Positioned(
                left: 14.0,
                top: 56.0,
                child: _ActiveRouteBanner(
                  route: plottedRoute,
                  onAutopilot: _autopilotToNextHop,
                  onClear: _clearRoute,
                ),
              ),

            // Compact Color Legend (Bottom-Left)
            Positioned(
              left: 14.0,
              bottom: 12.0,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10.0,
                  vertical: 6.0,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xCC061020),
                  borderRadius: BorderRadius.circular(9.0),
                  border: Border.all(
                    color: const Color(0x5538BDF8),
                    width: 1.0,
                  ),
                ),
                child: const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    Text(
                      'PINCH / DRAG TO EXPLORE • TAP SECTOR',
                      style: TextStyle(
                        color: Color(0xFF94A3B8),
                        fontSize: 9.0,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 0.6,
                      ),
                    ),
                    SizedBox(height: 5.0),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        _LegendChip(color: Color(0xFF38BDF8), label: 'C1/SAFE'),
                        SizedBox(width: 8.0),
                        _LegendChip(color: Color(0xFF22C55E), label: 'C2'),
                        SizedBox(width: 8.0),
                        _LegendChip(color: Color(0xFFFF3B5C), label: 'C3'),
                        SizedBox(width: 8.0),
                        _LegendChip(color: Color(0xFFFACC15), label: 'YELLOW/0-0'),
                        SizedBox(width: 8.0),
                        _LegendChip(
                          color: Color(0xFF34D399),
                          label: 'SHORTCUT',
                          dashed: true,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            // Selected Sector Compact Info Card (Bottom-Right)
            if (selectedNode != null)
              Positioned(
                right: 14.0,
                bottom: 12.0,
                child: _SectorInfoCard(
                  node: selectedNode,
                  isCurrentSector: selectedNode.id == currentSectorId,
                  shortestPathFromCurrent: GalaxyMapGraph.findShortestRoute(
                    currentSectorId,
                    selectedNode.id,
                  ),
                  isRouteActiveToThis: plottedRoute.length >= 2 &&
                      plottedRoute.last == selectedNode.id,
                  onPlotRoute: () => _plotRouteTo(selectedNode.id),
                  onClearRoute: _clearRoute,
                  onAutopilotNextHop: _autopilotToNextHop,
                  onDismiss: () => setState(() => _selectedSectorId = null),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _HudIconPill extends StatelessWidget {
  const _HudIconPill({
    required this.icon,
    required this.label,
    required this.accent,
    required this.onTap,
    this.filled = false,
  });

  final IconData icon;
  final String label;
  final Color accent;
  final VoidCallback onTap;
  final bool filled;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: filled ? accent : const Color(0xD9071326),
      borderRadius: BorderRadius.circular(9.0),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(9.0),
        child: Container(
          height: 34.0,
          padding: const EdgeInsets.symmetric(horizontal: 10.0),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(9.0),
            border: Border.all(
              color: filled ? Colors.white.withOpacity(0.45) : accent,
              width: 1.2,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Icon(
                icon,
                size: 14.0,
                color: filled ? Colors.white : accent,
              ),
              const SizedBox(width: 5.0),
              Text(
                label,
                style: TextStyle(
                  color: filled ? Colors.white : accent,
                  fontSize: 10.5,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 0.5,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _LegendChip extends StatelessWidget {
  const _LegendChip({
    required this.color,
    required this.label,
    this.dashed = false,
  });

  final Color color;
  final String label;
  final bool dashed;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        if (dashed)
          Row(
            children: <Widget>[
              Container(width: 5.0, height: 2.0, color: color),
              const SizedBox(width: 2.0),
              Container(width: 5.0, height: 2.0, color: color),
            ],
          )
        else
          Container(
            width: 8.0,
            height: 8.0,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
              boxShadow: <BoxShadow>[
                BoxShadow(
                  color: color.withOpacity(0.75),
                  blurRadius: 4.0,
                ),
              ],
            ),
          ),
        const SizedBox(width: 4.0),
        Text(
          label,
          style: const TextStyle(
            color: Color(0xFFCBD5E1),
            fontSize: 9.2,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}

class _ActiveRouteBanner extends StatelessWidget {
  const _ActiveRouteBanner({
    required this.route,
    required this.onAutopilot,
    required this.onClear,
  });

  final List<String> route;
  final VoidCallback onAutopilot;
  final VoidCallback onClear;

  @override
  Widget build(BuildContext context) {
    final int jumps = route.length - 1;
    final String nextHop = route[1];
    return Container(
      constraints: const BoxConstraints(maxWidth: 320.0),
      padding: const EdgeInsets.symmetric(horizontal: 10.0, vertical: 7.0),
      decoration: BoxDecoration(
        color: const Color(0xE007182B),
        borderRadius: BorderRadius.circular(10.0),
        border: Border.all(
          color: const Color(0xCC00E5FF),
          width: 1.3,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Row(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              const Icon(
                Icons.route,
                color: Color(0xFF00E5FF),
                size: 14.0,
              ),
              const SizedBox(width: 6.0),
              Text(
                'PLOTTED ROUTE • $jumps ${jumps == 1 ? 'JUMP' : 'JUMPS'}',
                style: const TextStyle(
                  color: Color(0xFF00E5FF),
                  fontSize: 10.5,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(width: 10.0),
              GestureDetector(
                onTap: onClear,
                child: const Icon(
                  Icons.close,
                  size: 14.0,
                  color: Color(0xFF94A3B8),
                ),
              ),
            ],
          ),
          const SizedBox(height: 3.0),
          Text(
            route.join(' → '),
            style: const TextStyle(
              color: Color(0xFFF1F5F9),
              fontSize: 10.0,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 6.0),
          SizedBox(
            height: 28.0,
            child: ElevatedButton.icon(
              onPressed: onAutopilot,
              icon: const Icon(Icons.near_me, size: 13.0),
              label: Text('FLY TO GATE $nextHop'),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF00E5FF),
                foregroundColor: const Color(0xFF040711),
                padding: const EdgeInsets.symmetric(horizontal: 10.0),
                textStyle: const TextStyle(
                  fontSize: 10.0,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SectorInfoCard extends StatelessWidget {
  const _SectorInfoCard({
    required this.node,
    required this.isCurrentSector,
    required this.shortestPathFromCurrent,
    required this.isRouteActiveToThis,
    required this.onPlotRoute,
    required this.onClearRoute,
    required this.onAutopilotNextHop,
    required this.onDismiss,
  });

  final SectorNode node;
  final bool isCurrentSector;
  final List<String> shortestPathFromCurrent;
  final bool isRouteActiveToThis;
  final VoidCallback onPlotRoute;
  final VoidCallback onClearRoute;
  final VoidCallback onAutopilotNextHop;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    final int hopCount = max(0, shortestPathFromCurrent.length - 1);
    final Color accent = node.nodeColor;

    return Container(
      width: 268.0,
      padding: const EdgeInsets.all(11.0),
      decoration: BoxDecoration(
        color: const Color(0xEB071020),
        borderRadius: BorderRadius.circular(12.0),
        border: Border.all(
          color: accent.withOpacity(0.85),
          width: 1.5,
        ),
        boxShadow: <BoxShadow>[
          BoxShadow(
            color: accent.withOpacity(0.24),
            blurRadius: 16.0,
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Row(
            children: <Widget>[
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 7.0,
                  vertical: 2.5,
                ),
                decoration: BoxDecoration(
                  color: accent.withOpacity(0.20),
                  borderRadius: BorderRadius.circular(6.0),
                  border: Border.all(color: accent, width: 1.0),
                ),
                child: Text(
                  node.id,
                  style: TextStyle(
                    color: accent,
                    fontSize: 12.0,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 0.6,
                  ),
                ),
              ),
              const SizedBox(width: 8.0),
              Expanded(
                child: Text(
                  node.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12.0,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              GestureDetector(
                onTap: onDismiss,
                child: const Icon(
                  Icons.close,
                  size: 16.0,
                  color: Color(0xFF94A3B8),
                ),
              ),
            ],
          ),
          const SizedBox(height: 7.0),
          _InfoRow(
            label: 'RISK',
            value: node.zoneType.riskDescription,
            valueColor: node.zoneType.badgeColor,
          ),
          const SizedBox(height: 3.5),
          _InfoRow(
            label: 'TIERS',
            value: node.resourceTiers,
            valueColor: const Color(0xFFF8FAFC),
          ),
          const SizedBox(height: 3.5),
          _InfoRow(
            label: 'SAFE',
            value: node.safeZoneStatusLabel,
            valueColor: node.hasCentralSafeZone
                ? const Color(0xFF00E5FF)
                : const Color(0xFF94A3B8),
          ),
          if (node.isLargeSector) ...<Widget>[
            const SizedBox(height: 3.5),
            const _InfoRow(
              label: 'SCALE',
              value: 'Large Sector (22,000 × 22,000)',
              valueColor: Color(0xFFFBBF24),
            ),
          ],
          const SizedBox(height: 3.5),
          _InfoRow(
            label: 'GATES',
            value: node.connectedSectorIds.join(', '),
            valueColor: const Color(0xFFCBD5E1),
          ),
          const SizedBox(height: 9.0),
          if (isCurrentSector)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 6.0),
              decoration: BoxDecoration(
                color: const Color(0x2E00E5FF),
                borderRadius: BorderRadius.circular(8.0),
                border: Border.all(color: const Color(0x8800E5FF)),
              ),
              alignment: Alignment.center,
              child: const Text(
                'CURRENT PILOT LOCATION',
                style: TextStyle(
                  color: Color(0xFF00E5FF),
                  fontSize: 10.5,
                  fontWeight: FontWeight.w800,
                  letterSpacing: 0.6,
                ),
              ),
            )
          else
            Row(
              children: <Widget>[
                Expanded(
                  child: SizedBox(
                    height: 34.0,
                    child: ElevatedButton.icon(
                      onPressed: isRouteActiveToThis
                          ? onAutopilotNextHop
                          : onPlotRoute,
                      icon: Icon(
                        isRouteActiveToThis ? Icons.near_me : Icons.alt_route,
                        size: 15.0,
                      ),
                      label: Text(
                        isRouteActiveToThis
                            ? 'FLY TO GATE'
                            : 'PLOT ROUTE ($hopCount ${hopCount == 1 ? 'HOP' : 'HOPS'})',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: isRouteActiveToThis
                            ? const Color(0xFF00E5FF)
                            : accent,
                        foregroundColor: const Color(0xFF040711),
                        padding: const EdgeInsets.symmetric(horizontal: 10.0),
                        textStyle: const TextStyle(
                          fontSize: 10.5,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                  ),
                ),
                if (isRouteActiveToThis) ...<Widget>[
                  const SizedBox(width: 6.0),
                  SizedBox(
                    height: 34.0,
                    child: OutlinedButton(
                      onPressed: onClearRoute,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: const Color(0xFFCBD5E1),
                        side: const BorderSide(color: Color(0x6694A3B8)),
                        padding: const EdgeInsets.symmetric(horizontal: 8.0),
                      ),
                      child: const Text(
                        'CLR',
                        style: TextStyle(
                          fontSize: 10.0,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                ],
              ],
            ),
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.label,
    required this.value,
    required this.valueColor,
  });

  final String label;
  final String value;
  final Color valueColor;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        SizedBox(
          width: 40.0,
          child: Text(
            label,
            style: const TextStyle(
              color: Color(0xFF64748B),
              fontSize: 9.5,
              fontWeight: FontWeight.w800,
              letterSpacing: 0.4,
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: TextStyle(
              color: valueColor,
              fontSize: 10.0,
              fontWeight: FontWeight.w600,
              height: 1.22,
            ),
          ),
        ),
      ],
    );
  }
}

/// Renders the dark sci-fi starfield, subtle nebula clouds, and corner HUD radar reticles.
class _GalaxyBackdropPainter extends CustomPainter {
  const _GalaxyBackdropPainter({required this.animTime});

  final double animTime;

  @override
  void paint(Canvas canvas, Size size) {
    // Deep nebula space gradient
    final Offset center = Offset(size.width * 0.5, size.height * 0.5);
    canvas.drawRect(
      Offset.zero & size,
      Paint()
        ..shader = ui.Gradient.radial(
          center,
          size.longestSide * 0.72,
          const <Color>[
            Color(0xFF0A162E),
            Color(0xFF050A18),
            Color(0xFF02050E),
          ],
          const <double>[0.0, 0.52, 1.0],
        ),
    );

    // Deterministic faint background starfield
    final Paint starPaint = Paint();
    for (int i = 0; i < 110; i++) {
      final double sx = ((i * 73.137) % 1.0) * size.width;
      final double sy = ((i * 39.719 + 0.17) % 1.0) * size.height;
      final double twinkle = 0.25 + 0.35 * sin(animTime * 1.4 + i * 0.7);
      starPaint.color = const Color(0xFFCBD5E1).withOpacity(twinkle);
      canvas.drawCircle(Offset(sx, sy), (i % 3 == 0) ? 1.4 : 0.9, starPaint);
    }

    // Subtle sci-fi outer HUD frame border
    final Rect frame = Rect.fromLTWH(6.0, 6.0, size.width - 12.0, size.height - 12.0);
    canvas.drawRect(
      frame,
      Paint()
        ..color = const Color(0x2838BDF8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.0,
    );

    // Decorative top-right sci-fi radar reticle (echoing the reference flight map)
    _drawCornerRadar(
      canvas,
      Offset(size.width - 68.0, 88.0),
      34.0,
      animTime * 0.85,
    );
  }

  void _drawCornerRadar(Canvas canvas, Offset c, double r, double sweepAngle) {
    final Paint ringPaint = Paint()
      ..color = const Color(0x2638BDF8)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.9;
    canvas.drawCircle(c, r, ringPaint);
    canvas.drawCircle(c, r * 0.65, ringPaint);
    canvas.drawCircle(c, r * 0.32, ringPaint);
    canvas.drawLine(Offset(c.dx - r, c.dy), Offset(c.dx + r, c.dy), ringPaint);
    canvas.drawLine(Offset(c.dx, c.dy - r), Offset(c.dx, c.dy + r), ringPaint);
    canvas.drawArc(
      Rect.fromCircle(center: c, radius: r),
      sweepAngle,
      0.85,
      true,
      Paint()..color = const Color(0x2200E5FF),
    );
  }

  @override
  bool shouldRepaint(covariant _GalaxyBackdropPainter oldDelegate) => true;
}

/// Renders the 25-sector Galactic Map topology: rings, solid/dashed portal links,
/// glowing nodes, BFS plotted route highlight, and animated current-sector indicator.
class _GalaxyMapPainter extends CustomPainter {
  const _GalaxyMapPainter({
    required this.animTime,
    required this.currentSectorId,
    required this.selectedSectorId,
    required this.plottedRoute,
  });

  final double animTime;
  final String currentSectorId;
  final String? selectedSectorId;
  final List<String> plottedRoute;

  @override
  void paint(Canvas canvas, Size size) {
    final double s = size.shortestSide;
    final Offset galaxyCenter = Offset(0.50 * s, 0.51 * s);
    final double pulse = 0.5 + 0.5 * sin(animTime * 4.2);

    Offset toCanvas(SectorNode n) =>
        Offset(n.position.dx * s, n.position.dy * s);

    // Build fast lookup for plotted route edges & nodes
    final Set<String> routeNodeSet = plottedRoute.toSet();
    final Set<String> routeEdgeSet = <String>{};
    for (int i = 0; i < plottedRoute.length - 1; i++) {
      final String a = plottedRoute[i];
      final String b = plottedRoute[i + 1];
      routeEdgeSet.add(a.compareTo(b) < 0 ? '$a|$b' : '$b|$a');
    }

    // 1. Ambient decorative orbital rings matching the reference sci-fi flight chart
    final double outerR = 0.385 * s;
    final double innerRingR = 0.228 * s;
    final double yellowRingR = 0.148 * s;

    // Subtle crimson/cyan outer orbital guide halo
    canvas.drawCircle(
      galaxyCenter,
      outerR,
      Paint()
        ..color = const Color(0x38EF4444)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.6,
    );

    // Inner Ring orbital circular guide arcs (between 1-2, 2-2, 3-2)
    canvas.drawCircle(
      galaxyCenter,
      innerRingR,
      Paint()
        ..color = const Color(0x4538BDF8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.6,
    );
    _drawDashedCircle(
      canvas,
      galaxyCenter,
      innerRingR + 7.5,
      Paint()
        ..color = const Color(0x3000E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.1,
      segments: 36,
    );

    // High-Risk Yellow Ring orbital aura around 1-3, 2-3, 3-3 & Nucleus 0-0
    _drawDashedCircle(
      canvas,
      galaxyCenter,
      yellowRingR,
      Paint()
        ..color = const Color(0x3DFACC15)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
      segments: 24,
    );

    // 2. Draw all Portal Links (Solid standard connections + Dashed Border Shortcuts)
    for (final GalaxyPortalLink link in GalaxyMapGraph.allLinks) {
      final SectorNode from = galaxyMapData[link.fromId]!;
      final SectorNode to = galaxyMapData[link.toId]!;
      final Offset p1 = toCanvas(from);
      final Offset p2 = toCanvas(to);
      final String edgeKey = link.fromId.compareTo(link.toId) < 0
          ? '${link.fromId}|${link.toId}'
          : '${link.toId}|${link.fromId}';
      final bool isRouteEdge = routeEdgeSet.contains(edgeKey);

      if (link.isShortcut) {
        // Dashed border shortcut (R1-3 <-> 2-2, R2-3 <-> 3-2, R3-3 <-> 1-2)
        final Paint shortcutPaint = Paint()
          ..color = isRouteEdge
              ? const Color(0xFF00E5FF)
              : const Color(0xCC34D399)
          ..strokeWidth = isRouteEdge ? 3.2 : 1.9
          ..strokeCap = StrokeCap.round;
        _drawDashedLine(
          canvas,
          p1,
          p2,
          shortcutPaint,
          dashLen: 7.0,
          gapLen: 5.5,
        );
      } else if (link.isInnerRing) {
        // Curved inner-ring arc connecting 1-2, 2-2, 3-2
        final double a1 = atan2(p1.dy - galaxyCenter.dy, p1.dx - galaxyCenter.dx);
        final double a2 = atan2(p2.dy - galaxyCenter.dy, p2.dx - galaxyCenter.dx);
        double sweep = a2 - a1;
        while (sweep <= -pi) {
          sweep += 2.0 * pi;
        }
        while (sweep > pi) {
          sweep -= 2.0 * pi;
        }
        final Rect arcRect = Rect.fromCircle(center: galaxyCenter, radius: innerRingR);
        if (isRouteEdge) {
          canvas.drawArc(
            arcRect,
            a1,
            sweep,
            false,
            Paint()
              ..color = const Color(0x5500E5FF)
              ..style = PaintingStyle.stroke
              ..strokeWidth = 7.0,
          );
        }
        canvas.drawArc(
          arcRect,
          a1,
          sweep,
          false,
          Paint()
            ..color = isRouteEdge
                ? const Color(0xFF00E5FF)
                : const Color(0xAA38BDF8)
            ..style = PaintingStyle.stroke
            ..strokeWidth = isRouteEdge ? 3.0 : 1.7,
        );
      } else {
        // Solid standard portal link (Outer Ring segments & Inner Spokes)
        final Color baseColor = link.isOuterRing
            ? const Color(0xAAEF4444)
            : ((from.isNucleus || to.isNucleus)
                ? const Color(0xCCFACC15)
                : from.nodeColor.withOpacity(0.75));
        if (isRouteEdge) {
          canvas.drawLine(
            p1,
            p2,
            Paint()
              ..color = const Color(0x5500E5FF)
              ..strokeWidth = 7.0
              ..strokeCap = StrokeCap.round,
          );
        }
        canvas.drawLine(
          p1,
          p2,
          Paint()
            ..color = isRouteEdge ? const Color(0xFF00E5FF) : baseColor
            ..strokeWidth = isRouteEdge ? 3.0 : 1.8
            ..strokeCap = StrokeCap.round,
        );
      }
    }

    // Animated flowing route pulse dots along the BFS plotted path
    if (plottedRoute.length >= 2) {
      final double flowT = (animTime * 0.75) % 1.0;
      for (int i = 0; i < plottedRoute.length - 1; i++) {
        final SectorNode a = galaxyMapData[plottedRoute[i]]!;
        final SectorNode b = galaxyMapData[plottedRoute[i + 1]]!;
        final Offset p1 = toCanvas(a);
        final Offset p2 = toCanvas(b);
        final Offset dot = Offset(
          p1.dx + (p2.dx - p1.dx) * flowT,
          p1.dy + (p2.dy - p1.dy) * flowT,
        );
        canvas.drawCircle(dot, 4.2, Paint()..color = const Color(0x8800E5FF));
        canvas.drawCircle(dot, 2.2, Paint()..color = Colors.white);
      }
    }

    // 3. Draw all 25 Sector Nodes
    for (final SectorNode node in galaxyMapData.values) {
      final Offset c = toCanvas(node);
      final bool isCurrent = node.id == currentSectorId;
      final bool isSelected = node.id == selectedSectorId;
      final bool isOnRoute = routeNodeSet.contains(node.id);

      if (node.isNucleus) {
        _drawNucleusNode(canvas, c, s, pulse, isCurrent, isSelected, isOnRoute);
      } else if (node.isCapitalCity) {
        _drawCapitalCityNode(
          canvas,
          node,
          c,
          pulse,
          isCurrent,
          isSelected,
          isOnRoute,
        );
      } else {
        _drawStandardSectorNode(
          canvas,
          node,
          c,
          pulse,
          isCurrent,
          isSelected,
          isOnRoute,
        );
      }
    }
  }

  void _drawNucleusNode(
    Canvas canvas,
    Offset c,
    double mapSpan,
    double pulse,
    bool isCurrent,
    bool isSelected,
    bool isOnRoute,
  ) {
    final double coreR = max(26.0, mapSpan * 0.068);

    // Golden ambient bloom
    canvas.drawCircle(
      c,
      coreR * 1.85,
      Paint()
        ..shader = ui.Gradient.radial(
          c,
          coreR * 1.85,
          <Color>[
            const Color(0x66FBBF24),
            const Color(0x28F59E0B),
            const Color(0x00000000),
          ],
          const <double>[0.0, 0.5, 1.0],
        ),
    );

    // Tilted planetary energy rings (mirroring the reference NUCLEO art)
    canvas.save();
    canvas.translate(c.dx, c.dy);
    canvas.rotate(-0.42 + sin(animTime * 0.5) * 0.03);
    canvas.scale(1.0, 0.48);
    canvas.drawCircle(
      Offset.zero,
      coreR * 1.62,
      Paint()
        ..color = const Color(0xCCFACC15)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.4,
    );
    _drawDashedCircle(
      canvas,
      Offset.zero,
      coreR * 1.82,
      Paint()
        ..color = const Color(0x88FDE047)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.6,
      segments: 16,
    );
    canvas.restore();

    // Dark core disc
    canvas.drawCircle(c, coreR, Paint()..color = const Color(0xF0110C04));

    // Outer Gold High-Risk Ring
    canvas.drawCircle(
      c,
      coreR,
      Paint()
        ..color = const Color(0xFFFACC15)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3.2,
    );

    // Inner Cyan Safe-Zone Ring (Central Base / Black Market where combat is disabled)
    canvas.drawCircle(
      c,
      coreR * 0.78,
      Paint()
        ..color = const Color(0xFF00E5FF).withOpacity(0.55 + 0.35 * pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );

    if (isSelected || isOnRoute) {
      canvas.drawCircle(
        c,
        coreR + 7.0,
        Paint()
          ..color = isSelected
              ? Colors.white.withOpacity(0.85)
              : const Color(0xFF00E5FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
    }

    if (isCurrent) {
      _drawCurrentPilotBeacon(canvas, c, coreR + 11.0, pulse);
    }

    _drawCenteredText(
      canvas,
      Offset(c.dx, c.dy - 5.0),
      'NUCLEUS',
      const Color(0xFFFDE68A),
      fontSize: 9.8,
      fontWeight: FontWeight.w900,
    );
    _drawCenteredText(
      canvas,
      Offset(c.dx, c.dy + 7.5),
      '0-0 • BLACK MKT',
      const Color(0xFF38BDF8),
      fontSize: 6.8,
      fontWeight: FontWeight.w800,
    );
  }

  void _drawCapitalCityNode(
    Canvas canvas,
    SectorNode node,
    Offset c,
    double pulse,
    bool isCurrent,
    bool isSelected,
    bool isOnRoute,
  ) {
    const double r = 20.0;
    final Color color = node.nodeColor;

    // Glowing halo
    canvas.drawCircle(
      c,
      r * 1.9,
      Paint()
        ..shader = ui.Gradient.radial(
          c,
          r * 1.9,
          <Color>[
            color.withOpacity(0.52),
            color.withOpacity(0.16),
            const Color(0x00000000),
          ],
          const <double>[0.0, 0.55, 1.0],
        ),
    );

    // Concentric sci-fi city emblem rings
    canvas.drawCircle(c, r, Paint()..color = const Color(0xEB071222));
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..color = color
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.6,
    );
    canvas.drawCircle(
      c,
      r * 0.68,
      Paint()
        ..color = color.withOpacity(0.75)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );
    canvas.drawCircle(
      c,
      r * 0.38,
      Paint()..color = color.withOpacity(0.85),
    );

    if (isSelected || isOnRoute) {
      canvas.drawCircle(
        c,
        r + 5.5,
        Paint()
          ..color = isSelected ? Colors.white : const Color(0xFF00E5FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
    }

    if (isCurrent) {
      _drawCurrentPilotBeacon(canvas, c, r + 9.0, pulse);
    }

    // Capital city title positioned outward cleanly like the reference image
    final Offset titlePos;
    if (node.id == 'C1') {
      titlePos = Offset(c.dx, c.dy - r - 14.0);
    } else if (node.id == 'C2') {
      titlePos = Offset(c.dx + 4.0, c.dy + r + 15.0);
    } else {
      titlePos = Offset(c.dx - 4.0, c.dy + r + 15.0);
    }
    final String cityLabel = node.id == 'C1'
        ? 'CITY 1 (C1)'
        : (node.id == 'C2' ? 'CITY 2 (C2)' : 'CITY 3 (C3)');
    _drawCenteredText(
      canvas,
      titlePos,
      cityLabel,
      color,
      fontSize: 10.5,
      fontWeight: FontWeight.w900,
    );
  }

  void _drawStandardSectorNode(
    Canvas canvas,
    SectorNode node,
    Offset c,
    double pulse,
    bool isCurrent,
    bool isSelected,
    bool isOnRoute,
  ) {
    final bool isShortcutHub = node.isShortcutHub;
    final bool isYellowRing = node.zoneType == ZoneType.highRiskYellowRing;
    final double r = isShortcutHub ? 11.0 : (isYellowRing ? 9.5 : 8.2);
    final Color color = node.nodeColor;

    // Glow bloom
    canvas.drawCircle(
      c,
      r * 2.0,
      Paint()..color = color.withOpacity(isSelected || isOnRoute ? 0.34 : 0.18),
    );

    canvas.drawCircle(c, r, Paint()..color = const Color(0xEE081224));
    canvas.drawCircle(
      c,
      r,
      Paint()
        ..color = isOnRoute ? const Color(0xFF00E5FF) : color
        ..style = PaintingStyle.stroke
        ..strokeWidth = (isSelected || isOnRoute) ? 2.4 : 1.7,
    );

    if (isShortcutHub) {
      canvas.drawCircle(
        c,
        r * 0.56,
        Paint()
          ..color = const Color(0xFF34D399)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.3,
      );
    }

    // Inner core dot (cyan inner ring for Safe Zone 1-1/2-1/3-1)
    canvas.drawCircle(
      c,
      r * 0.38,
      Paint()..color = isOnRoute ? Colors.white : color,
    );

    if (isSelected) {
      canvas.drawCircle(
        c,
        r + 4.5,
        Paint()
          ..color = Colors.white.withOpacity(0.9)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.6,
      );
    }

    if (isCurrent) {
      _drawCurrentPilotBeacon(canvas, c, r + 7.5, pulse);
    }

    // Crisp compact sector code label below node
    _drawCenteredText(
      canvas,
      Offset(c.dx, c.dy + r + 8.5),
      node.id,
      isSelected || isCurrent
          ? Colors.white
          : (isYellowRing ? const Color(0xFFFDE047) : const Color(0xFFCBD5E1)),
      fontSize: 8.2,
      fontWeight: FontWeight.w800,
    );
  }

  void _drawCurrentPilotBeacon(
    Canvas canvas,
    Offset c,
    double baseRadius,
    double pulse,
  ) {
    final double ringR = baseRadius + 4.5 * pulse;
    canvas.drawCircle(
      c,
      ringR,
      Paint()
        ..color = const Color(0xFF00E5FF).withOpacity(0.85 - 0.50 * pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2,
    );
    // Rotating tactical chevron bracket
    canvas.save();
    canvas.translate(c.dx, c.dy);
    canvas.rotate(animTime * 1.4);
    final Rect arcRect = Rect.fromCircle(center: Offset.zero, radius: baseRadius - 1.0);
    final Paint arcPaint = Paint()
      ..color = Colors.white.withOpacity(0.9)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.0
      ..strokeCap = StrokeCap.round;
    for (int i = 0; i < 4; i++) {
      canvas.drawArc(arcRect, i * (pi * 0.5) + 0.25, 0.65, false, arcPaint);
    }
    canvas.restore();
  }

  void _drawDashedLine(
    Canvas canvas,
    Offset start,
    Offset end,
    Paint paint, {
    required double dashLen,
    required double gapLen,
  }) {
    final double dx = end.dx - start.dx;
    final double dy = end.dy - start.dy;
    final double dist = sqrt(dx * dx + dy * dy);
    if (dist < 1.0) return;
    final double ux = dx / dist;
    final double uy = dy / dist;
    double d = 0.0;
    while (d < dist) {
      final double endD = min(dist, d + dashLen);
      canvas.drawLine(
        Offset(start.dx + ux * d, start.dy + uy * d),
        Offset(start.dx + ux * endD, start.dy + uy * endD),
        paint,
      );
      d += dashLen + gapLen;
    }
  }

  void _drawDashedCircle(
    Canvas canvas,
    Offset center,
    double radius,
    Paint paint, {
    required int segments,
  }) {
    final Rect rect = Rect.fromCircle(center: center, radius: radius);
    final double step = (2.0 * pi) / segments;
    for (int i = 0; i < segments; i++) {
      canvas.drawArc(rect, i * step, step * 0.52, false, paint);
    }
  }

  void _drawCenteredText(
    Canvas canvas,
    Offset pos,
    String text,
    Color color, {
    required double fontSize,
    required FontWeight fontWeight,
  }) {
    final TextPainter tp = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(
          color: color,
          fontSize: fontSize,
          fontWeight: fontWeight,
          shadows: const <Shadow>[
            Shadow(
              color: Color(0xEE020612),
              offset: Offset(0.8, 0.8),
              blurRadius: 3.0,
            ),
          ],
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(
      canvas,
      Offset(pos.dx - tp.width * 0.5, pos.dy - tp.height * 0.5),
    );
  }

  @override
  bool shouldRepaint(covariant _GalaxyMapPainter oldDelegate) => true;
}
