import 'dart:math';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import 'economy_models.dart';
import 'game_models.dart';

// =============================================================================
// PROCEDURAL 2D SHIP RENDERER (CAPSULE, 4 INDUSTRIAL SHIPS + DYNAMIC LOD)
// =============================================================================
// Coordinates assume forward = +X, right = +Y, origin = (center.dx, center.dy).
// When cameraZoom >= kShipDetailLodThreshold (1.35x), close-up vector details
// (rivets, canopy reflections, armor seams, energy coils, radiator fins, valves,
// container latches, docking hooks) render automatically.

class ShipVectorRenderer {
  const ShipVectorRenderer._();

  static void drawShip({
    required Canvas canvas,
    required Offset center,
    required ShipRole role,
    required ShipClass combatClass,
    IndustrialType? industrialType,
    double thrustFactor = 0.0,
    double thrusterPhase = 0.0,
    double cameraZoom = 1.0,
    double scale = 1.0,
  }) {
    final bool isHighDetail = cameraZoom >= kShipDetailLodThreshold;
    canvas.save();
    canvas.translate(center.dx, center.dy);
    if (scale != 1.0) {
      canvas.scale(scale, scale);
    }

    switch (role) {
      case ShipRole.capsule:
        _drawCapsuleShip(
          canvas: canvas,
          thrustFactor: thrustFactor,
          phase: thrusterPhase,
          isHighDetail: isHighDetail,
        );
        break;
      case ShipRole.industrial:
        switch (industrialType ?? IndustrialType.miner) {
          case IndustrialType.miner:
            _drawMinerShip(
              canvas: canvas,
              thrustFactor: thrustFactor,
              phase: thrusterPhase,
              isHighDetail: isHighDetail,
            );
            break;
          case IndustrialType.plasmaCollector:
            _drawPlasmaCollectorShip(
              canvas: canvas,
              thrustFactor: thrustFactor,
              phase: thrusterPhase,
              isHighDetail: isHighDetail,
            );
            break;
          case IndustrialType.mediumHauler:
            _drawMediumHaulerShip(
              canvas: canvas,
              thrustFactor: thrustFactor,
              phase: thrusterPhase,
              isHighDetail: isHighDetail,
            );
            break;
          case IndustrialType.heavyHauler:
            _drawHeavyHaulerShip(
              canvas: canvas,
              thrustFactor: thrustFactor,
              phase: thrusterPhase,
              isHighDetail: isHighDetail,
            );
            break;
        }
        break;
      case ShipRole.combat:
        _drawCombatShip(
          canvas: canvas,
          shipClass: combatClass,
          thrustFactor: thrustFactor,
          phase: thrusterPhase,
          isHighDetail: isHighDetail,
        );
        break;
    }

    canvas.restore();
  }

  static void _drawThrusterFlame(
    Canvas canvas, {
    required double nozzleX,
    required double nozzleY,
    required double length,
    required double halfWidth,
    required double thrustFactor,
    required double phase,
    Color outerColor = const Color(0xFF00E5FF),
    Color midColor = const Color(0xFF38BDF8),
  }) {
    final double active = 0.22 + 0.78 * thrustFactor.clamp(0.0, 1.0);
    final double flicker = 0.88 + 0.14 * sin(phase);
    final double len = length * active * flicker;
    final double hw = halfWidth * (0.7 + 0.35 * active);

    final Path outer = Path()
      ..moveTo(nozzleX, nozzleY - hw)
      ..lineTo(nozzleX - len, nozzleY)
      ..lineTo(nozzleX, nozzleY + hw)
      ..close();
    canvas.drawPath(
      outer,
      Paint()..color = outerColor.withOpacity(0.55 + 0.35 * thrustFactor),
    );

    final Path inner = Path()
      ..moveTo(nozzleX, nozzleY - hw * 0.52)
      ..lineTo(nozzleX - len * 0.62, nozzleY)
      ..lineTo(nozzleX, nozzleY + hw * 0.52)
      ..close();
    canvas.drawPath(inner, Paint()..color = midColor.withOpacity(0.92));

    canvas.drawCircle(
      Offset(nozzleX - len * 0.16, nozzleY),
      max(1.2, hw * 0.38),
      Paint()..color = Colors.white,
    );
  }

  // ---------------------------------------------------------------------------
  // 1. PERMANENT STARTING CAPSULE ("HUEVO" EGG VESSEL)
  // ---------------------------------------------------------------------------
  static void _drawCapsuleShip({
    required Canvas canvas,
    required double thrustFactor,
    required double phase,
    required bool isHighDetail,
  }) {
    final double pulse = 0.72 + 0.28 * sin(phase * 0.6);

    // Rear rocket thruster flame
    _drawThrusterFlame(
      canvas,
      nozzleX: -14.5,
      nozzleY: 0.0,
      length: 19.0,
      halfWidth: 4.5,
      thrustFactor: thrustFactor,
      phase: phase,
      outerColor: const Color(0xFFFF6D00),
      midColor: const Color(0xFFFACC15),
    );

    // Rear short mechanical stabilizers / fins
    final Paint finFill = Paint()..color = const Color(0xFF334155);
    final Paint finEdge = Paint()
      ..color = const Color(0xFF94A3B8)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.0;

    final Path topFin = Path()
      ..moveTo(-5.0, -9.5)
      ..lineTo(-14.5, -14.2)
      ..lineTo(-15.5, -9.5)
      ..lineTo(-11.0, -7.2)
      ..close();
    canvas.drawPath(topFin, finFill);
    canvas.drawPath(topFin, finEdge);

    final Path botFin = Path()
      ..moveTo(-5.0, 9.5)
      ..lineTo(-14.5, 14.2)
      ..lineTo(-15.5, 9.5)
      ..lineTo(-11.0, 7.2)
      ..close();
    canvas.drawPath(botFin, finFill);
    canvas.drawPath(botFin, finEdge);

    // Rear thruster bell
    final RRect bell = RRect.fromRectAndRadius(
      const Rect.fromLTWH(-16.5, -4.5, 4.5, 9.0),
      const Radius.circular(1.6),
    );
    canvas.drawRRect(bell, Paint()..color = const Color(0xFF475569));
    canvas.drawRRect(
      bell,
      Paint()
        ..color = const Color(0xFFCBD5E1)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 0.9,
    );

    // Left-side (-Y) industrial multi-purpose extractor arm (mining/salvaging)
    canvas.drawLine(
      const Offset(0.0, -10.5),
      const Offset(7.0, -15.5),
      Paint()
        ..color = const Color(0xFF64748B)
        ..strokeWidth = 2.6
        ..strokeCap = StrokeCap.round,
    );
    canvas.drawLine(
      const Offset(7.0, -15.5),
      const Offset(13.5, -13.0),
      Paint()
        ..color = const Color(0xFF94A3B8)
        ..strokeWidth = 2.0
        ..strokeCap = StrokeCap.round,
    );
    canvas.drawCircle(
      const Offset(14.5, -12.6),
      2.6,
      Paint()..color = const Color(0xFF38BDF8).withOpacity(0.55 + 0.35 * pulse),
    );
    canvas.drawCircle(
      const Offset(14.5, -12.6),
      1.3,
      Paint()..color = Colors.white,
    );

    // Right-side (+Y) external compact laser weapon
    final RRect laserPod = RRect.fromRectAndRadius(
      const Rect.fromLTWH(-1.5, 9.8, 12.0, 4.2),
      const Radius.circular(1.5),
    );
    canvas.drawRRect(laserPod, Paint()..color = const Color(0xFF475569));
    canvas.drawLine(
      const Offset(9.5, 11.9),
      const Offset(16.0, 11.9),
      Paint()
        ..color = const Color(0xFFCBD5E1)
        ..strokeWidth = 2.0
        ..strokeCap = StrokeCap.round,
    );
    canvas.drawCircle(
      const Offset(16.5, 11.9),
      2.2,
      Paint()..color = const Color(0xFFFF3B5C).withOpacity(0.65 + 0.35 * pulse),
    );

    // Main unmistakable egg-shaped (oval) armored dark-steel hull
    final Rect hullRect = const Rect.fromLTRB(-13.5, -11.0, 14.5, 11.0);
    canvas.drawOval(
      hullRect,
      Paint()
        ..shader = ui.Gradient.radial(
          const Offset(2.0, -2.0),
          16.0,
          const <Color>[
            Color(0xFF64748B),
            Color(0xFF334155),
            Color(0xFF1E293B),
          ],
          const <double>[0.0, 0.55, 1.0],
        ),
    );

    // Crimson-red armor plating bands on top & bottom shoulders
    canvas.drawArc(
      const Rect.fromLTRB(-11.8, -9.6, 12.2, 9.6),
      -2.35,
      1.25,
      false,
      Paint()
        ..color = const Color(0xFFDC2626)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3.2
        ..strokeCap = StrokeCap.round,
    );
    canvas.drawArc(
      const Rect.fromLTRB(-11.8, -9.6, 12.2, 9.6),
      1.10,
      1.25,
      false,
      Paint()
        ..color = const Color(0xFFDC2626)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3.2
        ..strokeCap = StrokeCap.round,
    );

    // Silver-alloy outer hull rim
    canvas.drawOval(
      hullRect,
      Paint()
        ..color = const Color(0xFFCBD5E1)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5,
    );

    // Rounded glowing glass cockpit visor at front (+X)
    final Rect canopyRect = const Rect.fromLTRB(2.2, -5.8, 12.8, 5.8);
    canvas.drawOval(
      canopyRect,
      Paint()
        ..shader = ui.Gradient.linear(
          const Offset(3.0, -5.0),
          const Offset(12.0, 5.0),
          const <Color>[
            Color(0xFF7DD3FC),
            Color(0xFF00E5FF),
            Color(0xFF0369A1),
          ],
          const <double>[0.0, 0.45, 1.0],
        ),
    );
    canvas.drawOval(
      canopyRect,
      Paint()
        ..color = Colors.white.withOpacity(0.8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.0,
    );

    // High-detail LOD (cameraZoom >= 1.35x): rivets, sensor micro-grille & canopy glint
    if (isHighDetail) {
      final Paint rivetPaint = Paint()..color = const Color(0xFFE2E8F0);
      const List<Offset> rivets = <Offset>[
        Offset(-9.0, -6.5),
        Offset(-4.0, -8.2),
        Offset(1.0, -8.0),
        Offset(-9.0, 6.5),
        Offset(-4.0, 8.2),
        Offset(1.0, 8.0),
      ];
      for (final Offset r in rivets) {
        canvas.drawCircle(r, 0.85, rivetPaint);
      }

      // Canopy glass reflection crescent
      canvas.drawArc(
        const Rect.fromLTRB(4.0, -4.4, 11.2, 4.4),
        -1.35,
        1.1,
        false,
        Paint()
          ..color = Colors.white.withOpacity(0.75)
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1.1
          ..strokeCap = StrokeCap.round,
      );

      // Hull seam & dorsal cooling grille
      final Paint seamPaint = Paint()
        ..color = const Color(0xFF0F172A)
        ..strokeWidth = 0.8;
      canvas.drawLine(const Offset(-2.5, -9.8), const Offset(-2.5, 9.8), seamPaint);
      for (double gy = -3.0; gy <= 3.0; gy += 2.0) {
        canvas.drawLine(
          Offset(-9.5, gy),
          Offset(-5.0, gy),
          Paint()
            ..color = const Color(0xFF94A3B8)
            ..strokeWidth = 0.85,
        );
      }
    }
  }

  // ---------------------------------------------------------------------------
  // 2. INDUSTRIAL MINER (PROSPECTOR — TWIN MINING PRONGS & ORE BAY)
  // ---------------------------------------------------------------------------
  static void _drawMinerShip({
    required Canvas canvas,
    required double thrustFactor,
    required double phase,
    required bool isHighDetail,
  }) {
    final double pulse = 0.65 + 0.35 * sin(phase * 0.7);

    // Dual rugged rear thrusters
    _drawThrusterFlame(
      canvas,
      nozzleX: -20.0,
      nozzleY: -8.0,
      length: 18.0,
      halfWidth: 4.0,
      thrustFactor: thrustFactor,
      phase: phase,
      outerColor: const Color(0xFFF59E0B),
      midColor: const Color(0xFFFEF08A),
    );
    _drawThrusterFlame(
      canvas,
      nozzleX: -20.0,
      nozzleY: 8.0,
      length: 18.0,
      halfWidth: 4.0,
      thrustFactor: thrustFactor,
      phase: phase + 1.1,
      outerColor: const Color(0xFFF59E0B),
      midColor: const Color(0xFFFEF08A),
    );

    // Twin front industrial mining prongs / drill jaws
    final Paint prongPaint = Paint()..color = const Color(0xFF475569);
    final Paint hazardPaint = Paint()..color = const Color(0xFFFACC15);

    final Path topProng = Path()
      ..moveTo(8.0, -12.0)
      ..lineTo(25.0, -10.0)
      ..lineTo(27.0, -5.5)
      ..lineTo(14.0, -4.5)
      ..close();
    final Path botProng = Path()
      ..moveTo(8.0, 12.0)
      ..lineTo(25.0, 10.0)
      ..lineTo(27.0, 5.5)
      ..lineTo(14.0, 4.5)
      ..close();
    canvas.drawPath(topProng, prongPaint);
    canvas.drawPath(botProng, prongPaint);
    canvas.drawRect(const Rect.fromLTWH(16.0, -10.5, 4.5, 4.5), hazardPaint);
    canvas.drawRect(const Rect.fromLTWH(16.0, 6.0, 4.5, 4.5), hazardPaint);

    // Cyan mining emitter tips
    canvas.drawCircle(
      const Offset(26.5, -7.8),
      2.6,
      Paint()..color = const Color(0xFF38BDF8).withOpacity(0.75 + 0.25 * pulse),
    );
    canvas.drawCircle(
      const Offset(26.5, 7.8),
      2.6,
      Paint()..color = const Color(0xFF38BDF8).withOpacity(0.75 + 0.25 * pulse),
    );

    // Wide boxy armored chassis
    final Path hull = Path()
      ..moveTo(14.0, -11.0)
      ..lineTo(-4.0, -16.5)
      ..lineTo(-19.5, -14.0)
      ..lineTo(-19.5, 14.0)
      ..lineTo(-4.0, 16.5)
      ..lineTo(14.0, 11.0)
      ..close();
    canvas.drawPath(hull, Paint()..color = const Color(0xFF1E293B));
    canvas.drawPath(
      hull,
      Paint()
        ..color = const Color(0xFFF59E0B)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );

    // Central ore hopper bay
    final RRect hopper = RRect.fromRectAndRadius(
      const Rect.fromLTWH(-12.5, -8.0, 16.0, 16.0),
      const Radius.circular(3.0),
    );
    canvas.drawRRect(hopper, Paint()..color = const Color(0xFF0F172A));
    canvas.drawRRect(
      hopper,
      Paint()
        ..color = const Color(0xFF38BDF8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.3,
    );

    // Forward armored cockpit slit
    final RRect cockpit = RRect.fromRectAndRadius(
      const Rect.fromLTWH(6.5, -5.0, 6.0, 10.0),
      const Radius.circular(2.0),
    );
    canvas.drawRRect(cockpit, Paint()..color = const Color(0xFF38BDF8));

    if (isHighDetail) {
      // Drill jaw cutting teeth & magnetic containment bars
      final Paint seam = Paint()
        ..color = const Color(0xFF94A3B8)
        ..strokeWidth = 1.0;
      for (double ox = -9.0; ox <= 0.0; ox += 4.5) {
        canvas.drawLine(Offset(ox, -7.2), Offset(ox, 7.2), seam);
      }
      // Hazard diagonal stripes on shoulders
      final Paint blackStripe = Paint()
        ..color = const Color(0xFF0F172A)
        ..strokeWidth = 1.4;
      canvas.drawLine(const Offset(16.5, -10.2), const Offset(20.0, -6.2), blackStripe);
      canvas.drawLine(const Offset(16.5, 6.2), const Offset(20.0, 10.2), blackStripe);
      // Glowing ore core inside hopper
      canvas.drawCircle(
        const Offset(-4.5, 0.0),
        3.2,
        Paint()..color = const Color(0xFF38BDF8).withOpacity(0.45 + 0.35 * pulse),
      );
      // Corner rivets
      final Paint rivet = Paint()..color = const Color(0xFFFDE047);
      canvas.drawCircle(const Offset(-16.0, -11.0), 1.0, rivet);
      canvas.drawCircle(const Offset(-16.0, 11.0), 1.0, rivet);
      canvas.drawCircle(const Offset(10.0, -9.0), 1.0, rivet);
      canvas.drawCircle(const Offset(10.0, 9.0), 1.0, rivet);
    }
  }

  // ---------------------------------------------------------------------------
  // 3. INDUSTRIAL PLASMA COLLECTOR (SIPHON — TWIN GLASS PLASMA TANKS + INTAKE)
  // ---------------------------------------------------------------------------
  static void _drawPlasmaCollectorShip({
    required Canvas canvas,
    required double thrustFactor,
    required double phase,
    required bool isHighDetail,
  }) {
    final double pulse = 0.65 + 0.35 * sin(phase * 0.9);

    _drawThrusterFlame(
      canvas,
      nozzleX: -20.0,
      nozzleY: 0.0,
      length: 22.0,
      halfWidth: 5.5,
      thrustFactor: thrustFactor,
      phase: phase,
      outerColor: const Color(0xFF2DD4BF),
      midColor: const Color(0xFF99F6E4),
    );

    // Front circular plasma funnel / magnetic intake dish
    canvas.drawArc(
      const Rect.fromLTRB(12.0, -13.0, 26.0, 13.0),
      -1.2,
      2.4,
      false,
      Paint()
        ..color = const Color(0xFF2DD4BF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.6
        ..strokeCap = StrokeCap.round,
    );
    canvas.drawCircle(
      const Offset(20.5, 0.0),
      3.5,
      Paint()..color = const Color(0xFF2DD4BF).withOpacity(0.55 + 0.4 * pulse),
    );

    // Central sleek spine
    final Path spine = Path()
      ..moveTo(19.0, 0.0)
      ..lineTo(8.0, -7.5)
      ..lineTo(-19.5, -6.5)
      ..lineTo(-19.5, 6.5)
      ..lineTo(8.0, 7.5)
      ..close();
    canvas.drawPath(spine, Paint()..color = const Color(0xFF162738));
    canvas.drawPath(
      spine,
      Paint()
        ..color = const Color(0xFF2DD4BF)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.6,
    );

    // Twin cylindrical glowing turquoise plasma tanks mounted on port & starboard
    final RRect topTank = RRect.fromRectAndRadius(
      const Rect.fromLTWH(-13.5, -16.5, 22.0, 8.2),
      const Radius.circular(4.1),
    );
    final RRect botTank = RRect.fromRectAndRadius(
      const Rect.fromLTWH(-13.5, 8.3, 22.0, 8.2),
      const Radius.circular(4.1),
    );
    final Paint tankGlow = Paint()
      ..color = const Color(0xFF0D9488).withOpacity(0.75 + 0.25 * pulse);
    final Paint tankRim = Paint()
      ..color = const Color(0xFF5EEAD4)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.5;

    canvas.drawRRect(topTank, tankGlow);
    canvas.drawRRect(topTank, tankRim);
    canvas.drawRRect(botTank, tankGlow);
    canvas.drawRRect(botTank, tankRim);

    // Forward canopy
    canvas.drawOval(
      const Rect.fromLTRB(4.5, -4.0, 14.5, 4.0),
      Paint()..color = const Color(0xFF7DD3FC),
    );

    if (isHighDetail) {
      // Containment pressure rings & magnetic plasma coils
      final Paint ringPaint = Paint()
        ..color = const Color(0xFF0F172A)
        ..strokeWidth = 1.6;
      for (double tx = -8.0; tx <= 4.0; tx += 6.0) {
        canvas.drawLine(Offset(tx, -16.2), Offset(tx, -8.5), ringPaint);
        canvas.drawLine(Offset(tx, 8.5), Offset(tx, 16.2), ringPaint);
      }
      // Internal plasma energy filaments
      final Paint filament = Paint()
        ..color = const Color(0xFFCCFBF1).withOpacity(0.75)
        ..strokeWidth = 1.2;
      canvas.drawLine(const Offset(-10.0, -12.4), const Offset(5.0, -12.4), filament);
      canvas.drawLine(const Offset(-10.0, 12.4), const Offset(5.0, 12.4), filament);
      // Valve conduits connecting tanks to core spine
      final Paint pipe = Paint()
        ..color = const Color(0xFF94A3B8)
        ..strokeWidth = 1.4;
      canvas.drawLine(const Offset(-3.0, -8.3), const Offset(-3.0, -5.5), pipe);
      canvas.drawLine(const Offset(-3.0, 8.3), const Offset(-3.0, 5.5), pipe);
    }
  }

  // ---------------------------------------------------------------------------
  // 4. MEDIUM HAULER (FREIGHTER M-1 — MODULAR CARGO PODS & COMMAND BRIDGE)
  // ---------------------------------------------------------------------------
  static void _drawMediumHaulerShip({
    required Canvas canvas,
    required double thrustFactor,
    required double phase,
    required bool isHighDetail,
  }) {
    _drawThrusterFlame(
      canvas,
      nozzleX: -25.0,
      nozzleY: -9.5,
      length: 20.0,
      halfWidth: 4.5,
      thrustFactor: thrustFactor,
      phase: phase,
      outerColor: const Color(0xFF4ADE80),
      midColor: const Color(0xFFBBF7D0),
    );
    _drawThrusterFlame(
      canvas,
      nozzleX: -25.0,
      nozzleY: 9.5,
      length: 20.0,
      halfWidth: 4.5,
      thrustFactor: thrustFactor,
      phase: phase + 0.9,
      outerColor: const Color(0xFF4ADE80),
      midColor: const Color(0xFFBBF7D0),
    );

    // Central structural keel
    final RRect keel = RRect.fromRectAndRadius(
      const Rect.fromLTWH(-25.0, -12.5, 48.0, 25.0),
      const Radius.circular(4.0),
    );
    canvas.drawRRect(keel, Paint()..color = const Color(0xFF1E293B));
    canvas.drawRRect(
      keel,
      Paint()
        ..color = const Color(0xFF4ADE80)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.8,
    );

    // 4 Modular side cargo containers (2 port, 2 starboard)
    final Paint podFill = Paint()..color = const Color(0xFF334155);
    final Paint podStroke = Paint()
      ..color = const Color(0xFF86EFAC)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.3;
    const List<Rect> pods = <Rect>[
      Rect.fromLTWH(-18.0, -21.0, 14.0, 8.5),
      Rect.fromLTWH(-1.0, -21.0, 14.0, 8.5),
      Rect.fromLTWH(-18.0, 12.5, 14.0, 8.5),
      Rect.fromLTWH(-1.0, 12.5, 14.0, 8.5),
    ];
    for (final Rect pod in pods) {
      final RRect rr = RRect.fromRectAndRadius(pod, const Radius.circular(2.0));
      canvas.drawRRect(rr, podFill);
      canvas.drawRRect(rr, podStroke);
    }

    // Forward armored command prow
    final Path prow = Path()
      ..moveTo(27.5, 0.0)
      ..lineTo(20.0, -11.0)
      ..lineTo(14.0, -11.0)
      ..lineTo(14.0, 11.0)
      ..lineTo(20.0, 11.0)
      ..close();
    canvas.drawPath(prow, Paint()..color = const Color(0xFF0F172A));
    canvas.drawPath(
      prow,
      Paint()
        ..color = const Color(0xFF4ADE80)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.6,
    );
    canvas.drawRect(
      const Rect.fromLTWH(18.5, -5.5, 4.5, 11.0),
      Paint()..color = const Color(0xFF38BDF8),
    );

    if (isHighDetail) {
      // Container strapping & lock latches
      final Paint strap = Paint()
        ..color = const Color(0xFFFACC15)
        ..strokeWidth = 1.2;
      for (final Rect pod in pods) {
        final double cx = pod.center.dx;
        canvas.drawLine(Offset(cx - 3.5, pod.top), Offset(cx - 3.5, pod.bottom), strap);
        canvas.drawLine(Offset(cx + 3.5, pod.top), Offset(cx + 3.5, pod.bottom), strap);
      }
      // Central walkway markings & navigation lights
      canvas.drawLine(
        const Offset(-20.0, 0.0),
        const Offset(13.0, 0.0),
        Paint()
          ..color = const Color(0xFF64748B)
          ..strokeWidth = 1.2,
      );
      canvas.drawCircle(const Offset(23.0, -8.5), 1.4, Paint()..color = const Color(0xFFFF3B5C));
      canvas.drawCircle(const Offset(23.0, 8.5), 1.4, Paint()..color = const Color(0xFF4ADE80));
    }
  }

  // ---------------------------------------------------------------------------
  // 5. HEAVY HAULER (LEVIATHAN H-1 — MASSIVE TRIPLE-THRUSTER FREIGHTER)
  // ---------------------------------------------------------------------------
  static void _drawHeavyHaulerShip({
    required Canvas canvas,
    required double thrustFactor,
    required double phase,
    required bool isHighDetail,
  }) {
    // Triple heavy engines
    for (final double ny in const <double>[-13.0, 0.0, 13.0]) {
      _drawThrusterFlame(
        canvas,
        nozzleX: -31.0,
        nozzleY: ny,
        length: ny == 0.0 ? 25.0 : 19.0,
        halfWidth: ny == 0.0 ? 5.2 : 4.2,
        thrustFactor: thrustFactor,
        phase: phase + ny * 0.2,
        outerColor: const Color(0xFFFACC15),
        midColor: const Color(0xFFFEF9C3),
      );
    }

    // Main monolithic barge hull
    final Path barge = Path()
      ..moveTo(31.0, -10.0)
      ..lineTo(34.0, 0.0)
      ..lineTo(31.0, 10.0)
      ..lineTo(20.0, 22.0)
      ..lineTo(-27.0, 22.0)
      ..lineTo(-31.0, 15.0)
      ..lineTo(-31.0, -15.0)
      ..lineTo(-27.0, -22.0)
      ..lineTo(20.0, -22.0)
      ..close();
    canvas.drawPath(barge, Paint()..color = const Color(0xFF1A2234));
    canvas.drawPath(
      barge,
      Paint()
        ..color = const Color(0xFFFACC15)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2,
    );

    // 6 massive armored freight silos (3x2 grid)
    final Paint siloFill = Paint()..color = const Color(0xFF2B384F);
    final Paint siloStroke = Paint()
      ..color = const Color(0xFFFDE047)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1.3;
    for (int col = 0; col < 3; col++) {
      final double x = -22.5 + col * 14.5;
      for (final double y in const <double>[-18.0, 5.5]) {
        final RRect silo = RRect.fromRectAndRadius(
          Rect.fromLTWH(x, y, 11.8, 12.5),
          const Radius.circular(2.2),
        );
        canvas.drawRRect(silo, siloFill);
        canvas.drawRRect(silo, siloStroke);
      }
    }

    // Elevated central armored bridge tower
    final RRect bridge = RRect.fromRectAndRadius(
      const Rect.fromLTWH(16.0, -7.5, 13.0, 15.0),
      const Radius.circular(3.0),
    );
    canvas.drawRRect(bridge, Paint()..color = const Color(0xFF0F172A));
    canvas.drawRRect(
      bridge,
      Paint()
        ..color = const Color(0xFF38BDF8)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5,
    );
    canvas.drawRect(
      const Rect.fromLTWH(23.5, -5.0, 4.0, 10.0),
      Paint()..color = const Color(0xFF38BDF8),
    );

    if (isHighDetail) {
      // Radiator fins, docking clamps, heavy bolts & warning beacons
      final Paint rib = Paint()
        ..color = const Color(0xFF94A3B8)
        ..strokeWidth = 1.1;
      for (int col = 0; col < 3; col++) {
        final double cx = -16.6 + col * 14.5;
        canvas.drawLine(Offset(cx, -17.0), Offset(cx, -6.5), rib);
        canvas.drawLine(Offset(cx, 6.5), Offset(cx, 17.0), rib);
      }
      // Central heavy spine truss
      canvas.drawRect(
        const Rect.fromLTWH(-25.0, -3.2, 39.0, 6.4),
        Paint()..color = const Color(0xFF0F172A),
      );
      for (double tx = -22.0; tx <= 10.0; tx += 6.5) {
        canvas.drawLine(
          Offset(tx, -2.5),
          Offset(tx + 4.0, 2.5),
          Paint()
            ..color = const Color(0xFFFACC15).withOpacity(0.75)
            ..strokeWidth = 1.0,
        );
      }
      // Corner beacon lights
      canvas.drawCircle(const Offset(-27.0, -19.0), 1.5, Paint()..color = const Color(0xFFFF3B5C));
      canvas.drawCircle(const Offset(-27.0, 19.0), 1.5, Paint()..color = const Color(0xFF4ADE80));
      canvas.drawCircle(const Offset(29.5, -8.5), 1.5, Paint()..color = const Color(0xFF38BDF8));
      canvas.drawCircle(const Offset(29.5, 8.5), 1.5, Paint()..color = const Color(0xFF38BDF8));
    }
  }

  // ---------------------------------------------------------------------------
  // 6. COMBAT SHIPS (INTERCEPTOR, BATTLECRUISER, DREADNOUGHT + LOD)
  // ---------------------------------------------------------------------------
  static void _drawCombatShip({
    required Canvas canvas,
    required ShipClass shipClass,
    required double thrustFactor,
    required double phase,
    required bool isHighDetail,
  }) {
    final ShipClassSpec spec = ShipClassCatalog.of(shipClass);
    switch (shipClass) {
      case ShipClass.interceptor:
        _drawThrusterFlame(
          canvas,
          nozzleX: -16.0,
          nozzleY: -5.5,
          length: 20.0,
          halfWidth: 3.5,
          thrustFactor: thrustFactor,
          phase: phase,
          outerColor: spec.accentColor,
        );
        _drawThrusterFlame(
          canvas,
          nozzleX: -16.0,
          nozzleY: 5.5,
          length: 20.0,
          halfWidth: 3.5,
          thrustFactor: thrustFactor,
          phase: phase + 0.8,
          outerColor: spec.accentColor,
        );
        final Path delta = Path()
          ..moveTo(24.0, 0.0)
          ..lineTo(-4.0, -19.0)
          ..lineTo(-17.0, -13.0)
          ..lineTo(-11.0, 0.0)
          ..lineTo(-17.0, 13.0)
          ..lineTo(-4.0, 19.0)
          ..close();
        canvas.drawPath(delta, Paint()..color = spec.primaryColor);
        canvas.drawPath(
          delta,
          Paint()
            ..color = spec.accentColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = 1.8,
        );
        canvas.drawOval(
          const Rect.fromLTRB(2.0, -4.0, 14.0, 4.0),
          Paint()..color = spec.accentColor,
        );
        if (isHighDetail) {
          final Paint panel = Paint()
            ..color = const Color(0xFF94A3B8)
            ..strokeWidth = 0.9;
          canvas.drawLine(const Offset(10.0, 0.0), const Offset(-10.0, 0.0), panel);
          canvas.drawLine(const Offset(-2.0, -14.0), const Offset(8.0, -6.0), panel);
          canvas.drawLine(const Offset(-2.0, 14.0), const Offset(8.0, 6.0), panel);
        }
        break;
      case ShipClass.battlecruiser:
        _drawThrusterFlame(
          canvas,
          nozzleX: -22.0,
          nozzleY: -8.0,
          length: 24.0,
          halfWidth: 4.5,
          thrustFactor: thrustFactor,
          phase: phase,
          outerColor: spec.accentColor,
        );
        _drawThrusterFlame(
          canvas,
          nozzleX: -22.0,
          nozzleY: 8.0,
          length: 24.0,
          halfWidth: 4.5,
          thrustFactor: thrustFactor,
          phase: phase + 0.7,
          outerColor: spec.accentColor,
        );
        final Path cruiser = Path()
          ..moveTo(28.0, 0.0)
          ..lineTo(12.0, -12.0)
          ..lineTo(-6.0, -22.0)
          ..lineTo(-22.0, -16.0)
          ..lineTo(-18.0, 0.0)
          ..lineTo(-22.0, 16.0)
          ..lineTo(-6.0, 22.0)
          ..lineTo(12.0, 12.0)
          ..close();
        canvas.drawPath(cruiser, Paint()..color = spec.primaryColor);
        canvas.drawPath(
          cruiser,
          Paint()
            ..color = spec.accentColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = 2.0,
        );
        canvas.drawRRect(
          RRect.fromRectAndRadius(
            const Rect.fromLTWH(2.0, -5.0, 13.0, 10.0),
            const Radius.circular(3.0),
          ),
          Paint()..color = spec.accentColor,
        );
        if (isHighDetail) {
          final Paint turret = Paint()..color = const Color(0xFFCBD5E1);
          canvas.drawCircle(const Offset(-4.0, -12.0), 2.2, turret);
          canvas.drawCircle(const Offset(-4.0, 12.0), 2.2, turret);
          canvas.drawLine(
            const Offset(-16.0, 0.0),
            const Offset(22.0, 0.0),
            Paint()
              ..color = spec.accentColor.withOpacity(0.55)
              ..strokeWidth = 1.0,
          );
        }
        break;
      case ShipClass.dreadnought:
        for (final double ny in const <double>[-11.0, 0.0, 11.0]) {
          _drawThrusterFlame(
            canvas,
            nozzleX: -28.0,
            nozzleY: ny,
            length: 24.0,
            halfWidth: 4.8,
            thrustFactor: thrustFactor,
            phase: phase + ny * 0.15,
            outerColor: spec.accentColor,
          );
        }
        final Path dread = Path()
          ..moveTo(33.0, -7.0)
          ..lineTo(22.0, -16.0)
          ..lineTo(-4.0, -27.0)
          ..lineTo(-27.0, -21.0)
          ..lineTo(-24.0, 0.0)
          ..lineTo(-27.0, 21.0)
          ..lineTo(-4.0, 27.0)
          ..lineTo(22.0, 16.0)
          ..lineTo(33.0, 7.0)
          ..lineTo(25.0, 0.0)
          ..close();
        canvas.drawPath(dread, Paint()..color = spec.primaryColor);
        canvas.drawPath(
          dread,
          Paint()
            ..color = spec.accentColor
            ..style = PaintingStyle.stroke
            ..strokeWidth = 2.4,
        );
        canvas.drawCircle(
          const Offset(4.0, 0.0),
          7.0,
          Paint()..color = spec.accentColor.withOpacity(0.85),
        );
        if (isHighDetail) {
          final Paint plating = Paint()
            ..color = const Color(0xFF94A3B8)
            ..strokeWidth = 1.1;
          canvas.drawLine(const Offset(-16.0, -18.0), const Offset(18.0, -10.0), plating);
          canvas.drawLine(const Offset(-16.0, 18.0), const Offset(18.0, 10.0), plating);
          canvas.drawCircle(const Offset(4.0, 0.0), 3.0, Paint()..color = Colors.white);
        }
        break;
    }
  }
}
