import 'dart:math';

import 'package:flame/components.dart';
import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';

import '../game/dark_orbit_game.dart';

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
    if (widget.game.activeMap == GameMap.mapX1) {
      const double pad = 14.0;
      final double usable = max(1.0, diameter - pad * 2.0);
      final double normX = ((details.localPosition.dx - pad) / usable).clamp(0.0, 1.0);
      final double normY = ((details.localPosition.dy - pad) / usable).clamp(0.0, 1.0);
      final double wx = (normX * GameConstants.mapX1Size)
          .clamp(GameConstants.mapX1AutopilotMin, GameConstants.mapX1AutopilotMax);
      final double wy = (normY * GameConstants.mapX1Size)
          .clamp(GameConstants.mapX1AutopilotMin, GameConstants.mapX1AutopilotMax);
      widget.game.setAutopilotTarget(wx, wy);
      setState(() {});
      return;
    }

    final double center = diameter * 0.5;
    final double usableRadius = center - 10.0;
    final double dx = details.localPosition.dx - center;
    final double dy = details.localPosition.dy - center;
    final double dist = sqrt(dx * dx + dy * dy);
    if (dist <= usableRadius + 8.0 && usableRadius > 1.0) {
      final double worldScale = GameConstants.mapRadius / usableRadius;
      widget.game.setAutopilotTarget(dx * worldScale, dy * worldScale);
      setState(() {});
    } else {
      widget.game.closeTacticalMap();
    }
  }

  @override
  Widget build(BuildContext context) {
    final bool isMapX1 = widget.game.activeMap == GameMap.mapX1;
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
                      color: isMapX1
                          ? const Color(0xCC818CF8)
                          : const Color(0xAA00E5FF),
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      const Icon(
                        Icons.navigation,
                        color: Color(0xFF00E5FF),
                        size: 16.0,
                      ),
                      const SizedBox(width: 6.0),
                      Text(
                        isMapX1 ? 'TACTICAL MAP • X-1' : 'TACTICAL MAP',
                        maxLines: 1,
                        softWrap: false,
                        style: const TextStyle(
                          color: Color(0xFF00E5FF),
                          fontSize: 12.0,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(width: 8.0),
                      const Text(
                        '• Tap map to engage Autopilot',
                        maxLines: 1,
                        softWrap: false,
                        style: TextStyle(
                          color: Color(0xFF94A3B8),
                          fontSize: 10.5,
                        ),
                      ),
                      const SizedBox(width: 10.0),
                      ElevatedButton.icon(
                        onPressed: widget.game.closeTacticalMap,
                        icon: const Icon(Icons.close, size: 14.0),
                        label: const Text(
                          'CLOSE',
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
                child: isMapX1
                    ? ClipRRect(
                        borderRadius: BorderRadius.circular(14.0),
                        child: Container(
                          width: diameter,
                          height: diameter,
                          decoration: BoxDecoration(
                            color: const Color(0xD407081F),
                            borderRadius: BorderRadius.circular(14.0),
                            border: Border.all(
                              color: const Color(0xCC6366F1),
                              width: 2.2,
                            ),
                          ),
                          child: mapCanvas,
                        ),
                      )
                    : ClipOval(
                        child: Container(
                          width: diameter,
                          height: diameter,
                          decoration: BoxDecoration(
                            color: const Color(0xC4050C1B),
                            shape: BoxShape.circle,
                            border: Border.all(
                              color: const Color(0xCC00E5FF),
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
    final bool isMapX1 = game.activeMap == GameMap.mapX1;
    final Offset center = Offset(size.width * 0.5, size.height * 0.5);
    const double pad = 14.0;
    final double mapSpan = max(1.0, size.shortestSide - pad * 2.0);
    final double mapR = size.shortestSide * 0.5 - 10.0;
    final double scale = isMapX1
        ? mapSpan / GameConstants.mapX1Size
        : mapR / GameConstants.mapRadius;
    final double pulse = 0.55 + 0.45 * sin(game.elapsedTime * 6.5);

    Offset worldToCanvas(double wx, double wy) {
      if (isMapX1) {
        return Offset(pad + wx * scale, pad + wy * scale);
      }
      return Offset(center.dx + wx * scale, center.dy + wy * scale);
    }

    if (isMapX1) {
      // Classic square 14000 x 14000 grid (every 2000 units)
      final Paint gridPaint = Paint()
        ..color = const Color(0x28818CF8)
        ..strokeWidth = 1.1;
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

      // Corner Safe Zone & Return Portal (1200, 1200)
      final Offset safeCenter = worldToCanvas(
        GameConstants.mapX1SafeZoneX,
        GameConstants.mapX1SafeZoneY,
      );
      final double safeR = max(10.0, GameConstants.mapX1SafeZoneRadius * scale);
      canvas.drawCircle(safeCenter, safeR, Paint()..color = const Color(0x3800E5FF));
      canvas.drawCircle(
        safeCenter,
        safeR,
        Paint()
          ..color = const Color(0xFF00E5FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
      canvas.drawCircle(
        safeCenter,
        6.0 + 1.4 * pulse,
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.55 + 0.40 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
      canvas.drawCircle(safeCenter, 2.8, Paint()..color = Colors.white);

      _drawCenteredLabel(
        canvas,
        Offset(safeCenter.dx + safeR + 30.0, safeCenter.dy),
        'GATE: BASE',
        const Color(0xFF38BDF8),
        fontSize: 10.0,
      );
      _drawCenteredLabel(
        canvas,
        center,
        'X-1',
        const Color(0x55818CF8),
        fontSize: 16.0,
      );
    } else {
      // Subtle crosshairs
      final Paint gridPaint = Paint()
        ..color = const Color(0x2838BDF8)
        ..strokeWidth = 1.2;
      canvas.drawLine(
        Offset(center.dx - mapR, center.dy),
        Offset(center.dx + mapR, center.dy),
        gridPaint,
      );
      canvas.drawLine(
        Offset(center.dx, center.dy - mapR),
        Offset(center.dx, center.dy + mapR),
        gridPaint,
      );

      // 4 Concentric Sector Rings
      _drawSectorRing(
        canvas,
        center,
        GameConstants.zone1MaxRadius * scale,
        const Color(0x994ADE80),
      );
      _drawSectorRing(
        canvas,
        center,
        GameConstants.zone2MaxRadius * scale,
        const Color(0x99FACC15),
      );
      _drawSectorRing(
        canvas,
        center,
        GameConstants.zone3MaxRadius * scale,
        const Color(0x99FB923C),
      );
      canvas.drawCircle(
        center,
        mapR,
        Paint()
          ..color = const Color(0xDDFF2E4C)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 2.4,
      );

      // Sector Labels S1..S4
      _drawSectorLabel(canvas, center, 'S1', 2800.0 * scale, const Color(0xFF4ADE80));
      _drawSectorLabel(canvas, center, 'S2', 7500.0 * scale, const Color(0xFFFACC15));
      _drawSectorLabel(canvas, center, 'S3', 12500.0 * scale, const Color(0xFFFB923C));
      _drawSectorLabel(canvas, center, 'S4', 17500.0 * scale, const Color(0xFFC084FC));

      // Central Safe Zone Base (Cyan) & Jump Gate indicator
      final double safeR = max(8.0, GameConstants.safeZoneRadius * scale);
      canvas.drawCircle(center, safeR, Paint()..color = const Color(0x3D00E5FF));
      canvas.drawCircle(
        center,
        safeR,
        Paint()
          ..color = const Color(0xFF00E5FF)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.8,
      );
      canvas.drawCircle(center, 3.0, Paint()..color = Colors.white);
      final Offset gatePt = worldToCanvas(
        GameConstants.mainJumpGateX,
        GameConstants.mainJumpGateY,
      );
      canvas.drawCircle(
        gatePt,
        3.4,
        Paint()
          ..color = const Color(0xFF00E5FF).withOpacity(0.65 + 0.35 * pulse)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.4,
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

    // Aliens (normal & pulsing BOSS)
    for (final AlienShipComponent alien in game.aliens) {
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

    final double clampedPx = isMapX1
        ? game.player.position.x.clamp(0.0, GameConstants.mapX1Size)
        : game.player.position.x;
    final double clampedPy = isMapX1
        ? game.player.position.y.clamp(0.0, GameConstants.mapX1Size)
        : game.player.position.y;
    final Offset playerPos = worldToCanvas(clampedPx, clampedPy);

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

    // High-visibility glowing Player directional chevron on full Tactical Map
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

  void _drawSectorRing(Canvas canvas, Offset center, double radius, Color color) {
    final Paint paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.5;
    const int segs = 48;
    const double step = (2.0 * pi) / segs;
    final Rect rect = Rect.fromCircle(center: center, radius: radius);
    for (int i = 0; i < segs; i++) {
      canvas.drawArc(rect, i * step, step * 0.55, false, paint);
    }
  }

  void _drawSectorLabel(
    Canvas canvas,
    Offset center,
    String text,
    double offsetUp,
    Color color,
  ) {
    _drawCenteredLabel(
      canvas,
      Offset(center.dx, center.dy - offsetUp),
      text,
      color,
      fontSize: 11.5,
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
