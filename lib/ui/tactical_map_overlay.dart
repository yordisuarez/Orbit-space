import 'dart:math';

import 'package:flame/components.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import '../game/dark_orbit_game.dart';
import '../game/galaxy_map_data.dart';

class TacticalMapOverlay extends StatefulWidget {
  const TacticalMapOverlay({
    super.key,
    required this.game,
  });

  final DarkOrbitGame game;

  @override
  State<TacticalMapOverlay> createState() => _TacticalMapOverlayState();
}

class _TacticalMapOverlayState extends State<TacticalMapOverlay>
    with SingleTickerProviderStateMixin {
  late final Ticker _ticker;
  int _frameTick = 0;

  @override
  void initState() {
    super.initState();
    _ticker = createTicker((Duration _) {
      if (mounted) {
        setState(() {
          _frameTick++;
        });
      }
    })
      ..start();
  }

  @override
  void dispose() {
    _ticker.dispose();
    super.dispose();
  }

  void _handleMapTap(TapDownDetails details, double diameter) {
    final double mapSize = widget.game.activeMapSize;
    const double pad = 14.0;
    final double usable = max(1.0, diameter - pad * 2.0);
    final double normX =
        ((details.localPosition.dx - pad) / usable).clamp(0.0, 1.0);
    final double normY =
        ((details.localPosition.dy - pad) / usable).clamp(0.0, 1.0);
    final double wx = (normX * mapSize).clamp(200.0, mapSize - 200.0);
    final double wy = (normY * mapSize).clamp(200.0, mapSize - 200.0);
    widget.game.setAutopilotTarget(wx, wy);
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    final SectorNode sector = widget.game.currentSector;
    final Size screenSize = MediaQuery.of(context).size;
    final double diameter = max(
      210.0,
      min(screenSize.height * 0.76, screenSize.width * 0.76),
    );

    final Widget mapCanvas = CustomPaint(
      size: Size(diameter, diameter),
      painter: _TacticalMapPainter(
        game: widget.game,
        frameTick: _frameTick,
      ),
    );

    return Stack(
      children: <Widget>[
        // Tap outside the map frame to close without pausing gameplay
        Positioned.fill(
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTap: widget.game.closeTacticalMap,
            child: Container(color: const Color(0x6E020612)),
          ),
        ),
        Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              GestureDetector(
                onTap: () {},
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12.0,
                    vertical: 5.0,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xD6071122),
                    borderRadius: BorderRadius.circular(10.0),
                    border: Border.all(
                      color: sector.nodeColor.withOpacity(0.82),
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Icon(
                        Icons.navigation,
                        color: sector.nodeColor,
                        size: 16.0,
                      ),
                      const SizedBox(width: 6.0),
                      Text(
                        '${GameStrings.of(widget.game.language).sectorMapTitle} ${sector.id} • ${sector.zoneType.shortLabel}',
                        maxLines: 1,
                        softWrap: false,
                        style: TextStyle(
                          color: sector.nodeColor,
                          fontSize: 11.5,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(width: 8.0),
                      Text(
                        '• ${GameStrings.of(widget.game.language).tapToSteerHint}',
                        maxLines: 1,
                        softWrap: false,
                        style: const TextStyle(
                          color: Color(0xFF94A3B8),
                          fontSize: 10.5,
                        ),
                      ),
                      const SizedBox(width: 10.0),
                      ElevatedButton.icon(
                        onPressed: () {
                          widget.game.closeTacticalMap();
                          widget.game.openGalaxyMap();
                        },
                        icon: const Icon(Icons.hub_outlined, size: 14.0),
                        label: const Text(
                          'GALAXY',
                          maxLines: 1,
                          softWrap: false,
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFF00E5FF),
                          foregroundColor: const Color(0xFF040711),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10.0,
                            vertical: 4.0,
                          ),
                          minimumSize: const Size(0.0, 28.0),
                          textStyle: const TextStyle(
                            fontSize: 10.5,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ),
                      const SizedBox(width: 6.0),
                      ElevatedButton.icon(
                        onPressed: widget.game.closeTacticalMap,
                        icon: const Icon(Icons.close, size: 14.0),
                        label: Text(
                          GameStrings.of(widget.game.language).closeLabel,
                          maxLines: 1,
                          softWrap: false,
                        ),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFFFF2E4C),
                          foregroundColor: Colors.white,
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10.0,
                            vertical: 4.0,
                          ),
                          minimumSize: const Size(0.0, 28.0),
                          textStyle: const TextStyle(
                            fontSize: 10.5,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 8.0),
              GestureDetector(
                onTapDown: (TapDownDetails d) => _handleMapTap(d, diameter),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(14.0),
                  child: Container(
                    width: diameter,
                    height: diameter,
                    decoration: BoxDecoration(
                      color: const Color(0xD9060B1A),
                      borderRadius: BorderRadius.circular(14.0),
                      border: Border.all(
                        color: sector.nodeColor.withOpacity(0.85),
                        width: 2.2,
                      ),
                    ),
                    child: mapCanvas,
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _TacticalMapPainter extends CustomPainter {
  const _TacticalMapPainter({
    required this.game,
    required this.frameTick,
  });

  final DarkOrbitGame game;
  final int frameTick;

  @override
  void paint(Canvas canvas, Size size) {
    final SectorNode sector = game.currentSector;
    final double mapSize = sector.mapSize;
    final Offset center = Offset(size.width * 0.5, size.height * 0.5);
    const double pad = 14.0;
    final double mapSpan = max(1.0, size.shortestSide - pad * 2.0);
    final double scale = mapSpan / mapSize;
    final double pulse = 0.55 + 0.45 * sin(game.elapsedTime * 6.5);

    Offset worldToCanvas(double wx, double wy) =>
        Offset(pad + wx * scale, pad + wy * scale);

    // Coordinate grid
    final Paint gridPaint = Paint()
      ..color = sector.nodeColor.withOpacity(0.16)
      ..strokeWidth = 1.0;
    const int steps = 7;
    for (int i = 1; i < steps; i++) {
      final double pos = pad + mapSpan * (i / steps);
      canvas.drawLine(Offset(pos, pad), Offset(pos, pad + mapSpan), gridPaint);
      canvas.drawLine(Offset(pad, pos), Offset(pad + mapSpan, pos), gridPaint);
    }

    // Glowing red square Radiation Zone perimeter
    final Rect mapRect = Rect.fromLTWH(pad, pad, mapSpan, mapSpan);
    canvas.drawRect(
      mapRect,
      Paint()
        ..color = const Color(0x40FF2E4C)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 6.0,
    );
    canvas.drawRect(
      mapRect,
      Paint()
        ..color = const Color(0xE6FF2E4C)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2,
    );

    // Central Safe Zone (Capital Cities, Level 1 Outposts, and Nucleus 0-0 Black Market)
    if (sector.hasCentralSafeZone) {
      final Vector2 safeWorld = game.activeSafeZoneCenter;
      final Offset safeCenter = worldToCanvas(safeWorld.x, safeWorld.y);
      final double safeR = max(11.0, sector.safeZoneRadius * scale);
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
              : const Color(0xFF00E5FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.0,
      );
      if (sector.isNucleus) {
        canvas.drawCircle(
          safeCenter,
          safeR * 0.78,
          Paint()
            ..color = const Color(0xFF00E5FF)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.5,
        );
      }
      canvas.drawCircle(safeCenter, 3.0, Paint()..color = Colors.white);
      _drawCenteredLabel(
        canvas,
        Offset(safeCenter.dx, safeCenter.dy - safeR - 9.0),
        sector.isNucleus
            ? 'BLACK MARKET SAFE ZONE'
            : (sector.isCapitalCity ? '${sector.id} BASE' : 'SAFE ZONE'),
        const Color(0xFF00E5FF),
        fontSize: 9.2,
      );
    }

    // Watermark sector ID
    if (!sector.hasCentralSafeZone) {
      _drawCenteredLabel(
        canvas,
        center,
        sector.id,
        sector.nodeColor.withOpacity(0.28),
        fontSize: 22.0,
      );
    }

    // All bidirectional Jump Gate Portals in this sector
    final String? nextHop = game.nextRouteHopSectorId;
    for (final SectorPortal portal in game.activePortals) {
      final Offset pt = worldToCanvas(portal.worldX, portal.worldY);
      final bool isNextHop = portal.targetSectorId == nextHop;
      final Color pColor = isNextHop
          ? const Color(0xFF00E5FF)
          : portal.accentColor;

      if (isNextHop) {
        canvas.drawCircle(
          pt,
          12.0 + 4.5 * pulse,
          Paint()
            ..color = const Color(0xFF00E5FF).withOpacity(0.35 + 0.45 * pulse)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 2.0,
        );
      }
      canvas.drawCircle(
        pt,
        6.5 + 1.4 * pulse,
        Paint()
          ..color = pColor.withOpacity(0.70 + 0.28 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.9,
      );
      canvas.drawCircle(pt, 2.8, Paint()..color = Colors.white);
      _drawCenteredLabel(
        canvas,
        Offset(pt.dx, pt.dy + 13.5),
        isNextHop ? '★ ${portal.targetSectorId}' : '→ ${portal.targetSectorId}',
        isNextHop ? const Color(0xFF00E5FF) : pColor,
        fontSize: isNextHop ? 10.2 : 9.2,
      );
    }

    // Sector Stations (Refinery, Crafting, Market, Black Market)
    for (final SectorStation st in game.activeStations) {
      final Offset stPos = worldToCanvas(st.worldX, st.worldY);
      final Path diamond = Path()
        ..moveTo(stPos.dx, stPos.dy - 6.0)
        ..lineTo(stPos.dx + 6.0, stPos.dy)
        ..lineTo(stPos.dx, stPos.dy + 6.0)
        ..lineTo(stPos.dx - 6.0, stPos.dy)
        ..close();
      canvas.drawPath(diamond, Paint()..color = st.accentColor);
      canvas.drawPath(
        diamond,
        Paint()
          ..color = Colors.white
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.2,
      );
      _drawCenteredLabel(
        canvas,
        Offset(stPos.dx, stPos.dy + 12.0),
        st.shortTag,
        st.accentColor,
        fontSize: 8.5,
      );
    }

    // Mineable Resource Nodes
    for (final ResourceNodeEntity node in game.resourceNodes) {
      if (node.chargesRemaining <= 0) continue;
      final ResourceSpec rSpec = ResourceCatalog.of(node.resourceType);
      canvas.drawCircle(
        worldToCanvas(node.worldX, node.worldY),
        3.0,
        Paint()..color = rSpec.accentColor.withOpacity(0.85),
      );
    }

    // Salvageable Alien Wrecks
    for (final AlienWreckEntity wreck in game.alienWrecks) {
      if (wreck.chargesRemaining <= 0) continue;
      final Offset wPos = worldToCanvas(wreck.worldX, wreck.worldY);
      canvas.drawRect(
        Rect.fromCenter(center: wPos, width: 5.5, height: 5.5),
        Paint()..color = const Color(0xFFFB923C),
      );
    }

    // Recoverable Player Ship Wrecks (40% salvageable cargo)
    for (final PlayerShipWreck pw in game.playerWrecks) {
      if (pw.sectorId != sector.id) continue;
      final Offset pPos = worldToCanvas(pw.worldX, pw.worldY);
      canvas.drawCircle(
        pPos,
        8.0 + 3.0 * pulse,
        Paint()
          ..color = const Color(0xFF22C55E).withOpacity(0.4 + 0.4 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
      canvas.drawCircle(pPos, 3.6, Paint()..color = const Color(0xFF22C55E));
      _drawCenteredLabel(
        canvas,
        Offset(pPos.dx, pPos.dy - 12.0),
        'WRECK',
        const Color(0xFF4ADE80),
        fontSize: 8.5,
      );
    }

    // Bonus & Cargo Boxes
    for (final LootBoxComponent box in game.lootBoxes) {
      final bool isCargo = box.kind == LootBoxKind.cargoBox;
      canvas.drawCircle(
        worldToCanvas(box.position.x, box.position.y),
        isCargo ? 3.2 : 2.1,
        Paint()
          ..color =
              isCargo ? const Color(0xFFFBBF24) : const Color(0xBBFACC15),
      );
    }

    final double clampedPx = game.player.position.x.clamp(0.0, mapSize);
    final double clampedPy = game.player.position.y.clamp(0.0, mapSize);
    final Offset playerPos = worldToCanvas(clampedPx, clampedPy);

    // Translucent cyan 2,800-unit ship sensor scanner range circle
    final double scannerRadius = GameConstants.alienRadarDetectionRange * scale;
    canvas.drawCircle(
      playerPos,
      scannerRadius,
      Paint()..color = const Color(0x1200E5FF),
    );
    canvas.drawCircle(
      playerPos,
      scannerRadius,
      Paint()
        ..color = const Color(0x5700E5FF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.35,
    );

    // Aliens within 2,800-unit sensor range
    const double alienDetectSq =
        GameConstants.alienRadarDetectionRange *
        GameConstants.alienRadarDetectionRange;
    for (final AlienShipComponent alien in game.aliens) {
      final double adx = alien.position.x - game.player.position.x;
      final double ady = alien.position.y - game.player.position.y;
      if (adx * adx + ady * ady > alienDetectSq) continue;

      final Offset pos = worldToCanvas(alien.position.x, alien.position.y);
      if (game.lockedTarget == alien) {
        canvas.drawCircle(
          pos,
          alien.isBoss ? 11.0 : 8.0,
          Paint()
            ..color = const Color(0xFFFF2E4C)
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.8,
        );
      }
      if (alien.isBoss) {
        canvas.drawCircle(
          pos,
          7.5 + 3.2 * pulse,
          Paint()
            ..color = const Color(0xFFFBBF24).withOpacity(0.25 + 0.38 * pulse),
        );
        canvas.drawCircle(pos, 5.2, Paint()..color = const Color(0xFFFF2E4C));
        canvas.drawCircle(pos, 2.5, Paint()..color = const Color(0xFFFBBF24));
      } else {
        canvas.drawCircle(
          pos,
          max(2.7, alien.spec.minimapDotRadius * 1.1),
          Paint()..color = alien.spec.primaryColor,
        );
      }
    }

    // Active Autopilot Dashed Trajectory & Crosshair
    final Vector2? ap = game.autopilotTarget;
    if (ap != null) {
      final Offset targetPos = worldToCanvas(ap.x, ap.y);
      _drawDashedLine(
        canvas,
        playerPos,
        targetPos,
        Paint()
          ..color = const Color(0xBB00E5FF)
          ..strokeWidth = 2.0,
        dashLen: 8.0,
        gapLen: 6.0,
      );
      final double wpR = 7.5 + 3.0 * pulse;
      canvas.drawCircle(
        targetPos,
        wpR,
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.45 + 0.45 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
      final Paint crossPaint = Paint()
        ..color = const Color(0xFF00E5FF)
        ..strokeWidth = 1.4;
      canvas.drawLine(
        Offset(targetPos.dx - wpR - 3.0, targetPos.dy),
        Offset(targetPos.dx + wpR + 3.0, targetPos.dy),
        crossPaint,
      );
      canvas.drawLine(
        Offset(targetPos.dx, targetPos.dy - wpR - 3.0),
        Offset(targetPos.dx, targetPos.dy + wpR + 3.0),
        crossPaint,
      );
      canvas.drawCircle(targetPos, 2.4, Paint()..color = Colors.white);
    }

    // Player directional chevron
    canvas.drawCircle(
      playerPos,
      19.0 + 4.5 * pulse,
      Paint()..color = const Color(0xFF00E5FF).withOpacity(0.16 + 0.14 * pulse),
    );
    canvas.drawCircle(
      playerPos,
      13.0,
      Paint()..color = const Color(0x6600E5FF),
    );
    canvas.drawCircle(
      playerPos,
      16.0 + 2.5 * pulse,
      Paint()
        ..color = Colors.white.withOpacity(0.55 + 0.35 * pulse)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
    final double a = game.player.angle;
    final double cA = cos(a);
    final double sA = sin(a);
    final Path chevron = Path()
      ..moveTo(playerPos.dx + cA * 14.5, playerPos.dy + sA * 14.5)
      ..lineTo(
        playerPos.dx + (-cA * 8.8 - sA * 8.8),
        playerPos.dy + (-sA * 8.8 + cA * 8.8),
      )
      ..lineTo(playerPos.dx - cA * 3.0, playerPos.dy - sA * 3.0)
      ..lineTo(
        playerPos.dx + (-cA * 8.8 + sA * 8.8),
        playerPos.dy + (-sA * 8.8 - cA * 8.8),
      )
      ..close();
    canvas.drawPath(chevron, Paint()..color = const Color(0xFF00E5FF));
    canvas.drawPath(
      chevron,
      Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.0,
    );
  }

  void _drawCenteredLabel(
    Canvas canvas,
    Offset pos,
    String text,
    Color color, {
    double fontSize = 11.5,
  }) {
    final TextPainter tp = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(
          color: color,
          fontSize: fontSize,
          fontWeight: FontWeight.bold,
          shadows: const <Shadow>[
            Shadow(
              color: Color(0xEE020612),
              offset: Offset(0.8, 0.8),
              blurRadius: 2.5,
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
    double traveled = 0.0;
    while (traveled < dist) {
      final double segEnd = min(dist, traveled + dashLen);
      canvas.drawLine(
        Offset(start.dx + ux * traveled, start.dy + uy * traveled),
        Offset(start.dx + ux * segEnd, start.dy + uy * segEnd),
        paint,
      );
      traveled += dashLen + gapLen;
    }
  }

  @override
  bool shouldRepaint(covariant _TacticalMapPainter oldDelegate) =>
      oldDelegate.frameTick != frameTick;
}
