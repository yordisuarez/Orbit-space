import 'dart:math';

import 'package:flutter/material.dart';

import '../game/game_models.dart';

class CompanyEmblemWidget extends StatelessWidget {
  const CompanyEmblemWidget({
    super.key,
    required this.company,
    this.size = 56.0,
  });

  final PilotCompany company;
  final double size;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(
        painter: _CompanyEmblemPainter(company: company),
      ),
    );
  }
}

class _CompanyEmblemPainter extends CustomPainter {
  const _CompanyEmblemPainter({required this.company});

  final PilotCompany company;

  @override
  void paint(Canvas canvas, Size size) {
    final PilotCompanySpec spec = PilotCompanyCatalog.of(company);
    final Color primary = spec.primaryColor;
    final Color darkBg = spec.darkBgColor;
    final Offset center = Offset(size.width * 0.5, size.height * 0.5);
    final double cx = center.dx;
    final double cy = center.dy;
    final double r = min(size.width, size.height) * 0.46;

    // Ambient radial glow behind emblem
    canvas.drawCircle(
      center,
      r * 1.12,
      Paint()
        ..shader = RadialGradient(
          colors: <Color>[
            primary.withOpacity(0.34),
            Colors.transparent,
          ],
        ).createShader(Rect.fromCircle(center: center, radius: r * 1.12)),
    );

    switch (company) {
      case PilotCompany.solaris:
        final Path diamond = Path()
          ..moveTo(cx, cy - r)
          ..lineTo(cx + r, cy)
          ..lineTo(cx, cy + r)
          ..lineTo(cx - r, cy)
          ..close();
        canvas.drawPath(diamond, Paint()..color = darkBg);
        canvas.drawPath(
          diamond,
          Paint()
            ..color = primary
            ..style = PaintingStyle.stroke
            ..strokeWidth = r * 0.11
            ..strokeJoin = StrokeJoin.miter,
        );

        final Path star = Path();
        for (int i = 0; i < 16; i++) {
          final double rad = i.isEven ? r * 0.66 : r * 0.28;
          final double a = (i * 22.5 - 90.0) * pi / 180.0;
          final double px = cx + cos(a) * rad;
          final double py = cy + sin(a) * rad;
          if (i == 0) {
            star.moveTo(px, py);
          } else {
            star.lineTo(px, py);
          }
        }
        star.close();
        canvas.drawPath(
          star,
          Paint()
            ..shader = RadialGradient(
              colors: <Color>[const Color(0xFFFFD166), primary],
            ).createShader(Rect.fromCircle(center: center, radius: r * 0.7)),
        );
        canvas.drawCircle(center, r * 0.14, Paint()..color = Colors.white);
        break;

      case PilotCompany.aether:
        final Path hex = Path();
        for (int i = 0; i < 6; i++) {
          final double a = (i * 60.0 - 90.0) * pi / 180.0;
          final double px = cx + cos(a) * r;
          final double py = cy + sin(a) * r;
          if (i == 0) {
            hex.moveTo(px, py);
          } else {
            hex.lineTo(px, py);
          }
        }
        hex.close();
        canvas.drawPath(hex, Paint()..color = darkBg);
        canvas.drawPath(
          hex,
          Paint()
            ..color = primary
            ..style = PaintingStyle.stroke
            ..strokeWidth = r * 0.11
            ..strokeJoin = StrokeJoin.miter,
        );
        canvas.drawCircle(
          center,
          r * 0.58,
          Paint()
            ..color = primary.withOpacity(0.72)
            ..style = PaintingStyle.stroke
            ..strokeWidth = r * 0.08,
        );
        final Paint tickPaint = Paint()
          ..color = primary
          ..strokeWidth = r * 0.08
          ..strokeCap = StrokeCap.round;
        for (int i = 0; i < 4; i++) {
          final double a = i * pi * 0.5;
          canvas.drawLine(
            Offset(cx + cos(a) * r * 0.34, cy + sin(a) * r * 0.34),
            Offset(cx + cos(a) * r * 0.76, cy + sin(a) * r * 0.76),
            tickPaint,
          );
        }
        final Path core = Path()
          ..moveTo(cx, cy - r * 0.28)
          ..lineTo(cx + r * 0.28, cy)
          ..lineTo(cx, cy + r * 0.28)
          ..lineTo(cx - r * 0.28, cy)
          ..close();
        canvas.drawPath(core, Paint()..color = Colors.white);
        break;

      case PilotCompany.verdant:
        final Path crest = Path()
          ..moveTo(cx, cy - r)
          ..lineTo(cx + r * 0.88, cy - r * 0.42)
          ..lineTo(cx + r * 0.68, cy + r * 0.56)
          ..lineTo(cx, cy + r)
          ..lineTo(cx - r * 0.68, cy + r * 0.56)
          ..lineTo(cx - r * 0.88, cy - r * 0.42)
          ..close();
        canvas.drawPath(crest, Paint()..color = darkBg);
        canvas.drawPath(
          crest,
          Paint()
            ..color = primary
            ..style = PaintingStyle.stroke
            ..strokeWidth = r * 0.11
            ..strokeJoin = StrokeJoin.miter,
        );
        final Path wings = Path()
          ..moveTo(cx - r * 0.58, cy + r * 0.06)
          ..lineTo(cx, cy - r * 0.54)
          ..lineTo(cx + r * 0.58, cy + r * 0.06)
          ..lineTo(cx + r * 0.34, cy + r * 0.22)
          ..lineTo(cx, cy - r * 0.16)
          ..lineTo(cx - r * 0.34, cy + r * 0.22)
          ..close();
        canvas.drawPath(
          wings,
          Paint()
            ..shader = LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: <Color>[const Color(0xFF86EFAC), primary],
            ).createShader(
              Rect.fromLTRB(cx - r, cy - r * 0.6, cx + r, cy + r * 0.3),
            ),
        );
        final Path crystal = Path()
          ..moveTo(cx, cy + r * 0.06)
          ..lineTo(cx + r * 0.24, cy + r * 0.36)
          ..lineTo(cx, cy + r * 0.66)
          ..lineTo(cx - r * 0.24, cy + r * 0.36)
          ..close();
        canvas.drawPath(crystal, Paint()..color = const Color(0xFFFDE047));
        break;
    }
  }

  @override
  bool shouldRepaint(covariant _CompanyEmblemPainter oldDelegate) =>
      oldDelegate.company != company;
}

class _DriftingStar {
  const _DriftingStar({
    required this.x,
    required this.y,
    required this.speed,
    required this.radius,
    required this.alpha,
    required this.color,
  });

  final double x;
  final double y;
  final double speed;
  final double radius;
  final double alpha;
  final Color color;
}

class AnimatedSpaceBackdrop extends StatefulWidget {
  const AnimatedSpaceBackdrop({
    super.key,
    this.accentColor = const Color(0xFF00E5FF),
  });

  final Color accentColor;

  @override
  State<AnimatedSpaceBackdrop> createState() => _AnimatedSpaceBackdropState();
}

class _AnimatedSpaceBackdropState extends State<AnimatedSpaceBackdrop>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  late final List<_DriftingStar> _stars;
  final Stopwatch _stopwatch = Stopwatch()..start();

  @override
  void initState() {
    super.initState();
    final Random rng = Random(4207);
    const List<Color> palette = <Color>[
      Color(0xFFFFFFFF),
      Color(0xFFBAE6FD),
      Color(0xFFC7D2FE),
      Color(0xFFFDE68A),
      Color(0xFF7DD3FC),
    ];
    _stars = List<_DriftingStar>.generate(140, (int idx) {
      final int layer = idx % 3;
      return _DriftingStar(
        x: rng.nextDouble(),
        y: rng.nextDouble(),
        speed: layer == 0
            ? 0.012
            : layer == 1
                ? 0.028
                : 0.056,
        radius: layer == 0
            ? 1.1
            : layer == 1
                ? 1.8
                : 2.7,
        alpha: layer == 0
            ? 0.35 + rng.nextDouble() * 0.25
            : layer == 1
                ? 0.55 + rng.nextDouble() * 0.3
                : 0.78 + rng.nextDouble() * 0.22,
        color: palette[rng.nextInt(palette.length)],
      );
    });
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 60),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (BuildContext context, Widget? child) {
        return CustomPaint(
          painter: _SpaceBackdropPainter(
            stars: _stars,
            elapsedSeconds: _stopwatch.elapsedMilliseconds / 1000.0,
            accentColor: widget.accentColor,
          ),
          size: Size.infinite,
        );
      },
    );
  }
}

class _SpaceBackdropPainter extends CustomPainter {
  const _SpaceBackdropPainter({
    required this.stars,
    required this.elapsedSeconds,
    required this.accentColor,
  });

  final List<_DriftingStar> stars;
  final double elapsedSeconds;
  final Color accentColor;

  @override
  void paint(Canvas canvas, Size size) {
    final double w = size.width;
    final double h = size.height;
    final double cx = w * 0.5;
    final double cy = h * 0.5;

    canvas.drawRect(
      Offset.zero & size,
      Paint()..color = const Color(0xFF030612),
    );

    final double maxDim = max(w, h);
    canvas.drawRect(
      Offset.zero & size,
      Paint()
        ..shader = RadialGradient(
          center: const Alignment(0.0, -0.15),
          radius: 0.9,
          colors: <Color>[
            accentColor.withOpacity(0.14),
            const Color(0xFF0A142D).withOpacity(0.72),
            const Color(0xFF030612),
          ],
        ).createShader(Rect.fromCircle(center: Offset(cx, cy), radius: maxDim * 0.7)),
    );

    canvas.drawCircle(
      Offset(w * 0.18, h * 0.22),
      h * 0.65,
      Paint()
        ..shader = RadialGradient(
          colors: <Color>[
            const Color(0xFF6366F1).withOpacity(0.13),
            Colors.transparent,
          ],
        ).createShader(
          Rect.fromCircle(center: Offset(w * 0.18, h * 0.22), radius: h * 0.65),
        ),
    );

    for (final _DriftingStar star in stars) {
      double nx = (star.x - elapsedSeconds * star.speed) % 1.0;
      if (nx < 0.0) nx += 1.0;
      double ny = (star.y + elapsedSeconds * star.speed * 0.24) % 1.0;
      if (ny < 0.0) ny += 1.0;
      final double sx = nx * w;
      final double sy = ny * h;
      final double twinkle =
          0.78 + 0.22 * sin(elapsedSeconds * 2.8 + star.x * 31.0);
      final double a = (star.alpha * twinkle).clamp(0.12, 1.0);
      canvas.drawCircle(
        Offset(sx, sy),
        star.radius,
        Paint()..color = star.color.withOpacity(a),
      );
      if (star.radius > 2.2) {
        canvas.drawLine(
          Offset(sx, sy),
          Offset(sx + star.radius * 5.5, sy - star.radius * 1.3),
          Paint()
            ..color = star.color.withOpacity(0.18 * twinkle)
            ..strokeWidth = 1.1,
        );
      }
    }

    final double ringR = min(w, h) * 0.42;
    canvas.drawCircle(
      Offset(cx, cy),
      ringR,
      Paint()
        ..color = accentColor.withOpacity(0.08)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4,
    );
    canvas.drawCircle(
      Offset(cx, cy),
      ringR * 1.35,
      Paint()
        ..color = accentColor.withOpacity(0.05)
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.0,
    );

    canvas.save();
    canvas.translate(cx, cy);
    canvas.rotate(elapsedSeconds * 0.14);
    final Paint tickPaint = Paint()
      ..color = accentColor.withOpacity(0.26)
      ..strokeWidth = 2.0;
    for (int i = 0; i < 4; i++) {
      final double a = i * pi * 0.5;
      canvas.drawLine(
        Offset(cos(a) * (ringR - 8.0), sin(a) * (ringR - 8.0)),
        Offset(cos(a) * (ringR + 10.0), sin(a) * (ringR + 10.0)),
        tickPaint,
      );
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _SpaceBackdropPainter oldDelegate) => true;
}

class IntroTitleScreen extends StatefulWidget {
  const IntroTitleScreen({
    super.key,
    required this.savedPilotName,
    required this.savedCompany,
    required this.onContinueSaved,
    required this.onStartNew,
  });

  final String? savedPilotName;
  final PilotCompany? savedCompany;
  final VoidCallback onContinueSaved;
  final VoidCallback onStartNew;

  @override
  State<IntroTitleScreen> createState() => _IntroTitleScreenState();
}

class _IntroTitleScreenState extends State<IntroTitleScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulseController;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1250),
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bool hasSaved = widget.savedPilotName != null &&
        widget.savedPilotName!.trim().isNotEmpty &&
        widget.savedCompany != null;
    final PilotCompanySpec? companySpec =
        hasSaved ? PilotCompanyCatalog.of(widget.savedCompany!) : null;
    final Color accent =
        companySpec?.primaryColor ?? const Color(0xFF00E5FF);

    final String ctaText = hasSaved
        ? 'CONTINUE AS [${companySpec!.tag}] ${widget.savedPilotName!}'
        : 'START';

    return Scaffold(
      backgroundColor: const Color(0xFF030612),
      body: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          AnimatedSpaceBackdrop(accentColor: accent),
          SafeArea(
            child: Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 24.0,
                  vertical: 16.0,
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14.0,
                        vertical: 5.0,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0x990A1428),
                        borderRadius: BorderRadius.circular(99.0),
                        border: Border.all(color: accent.withOpacity(0.45)),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: <Widget>[
                          Container(
                            width: 7.0,
                            height: 7.0,
                            decoration: BoxDecoration(
                              color: accent,
                              shape: BoxShape.circle,
                            ),
                          ),
                          const SizedBox(width: 9.0),
                          const Text(
                            'ORBITAL COMBAT SECTOR • STARBASE COMMAND',
                            style: TextStyle(
                              color: Color(0xFF94A3B8),
                              fontSize: 11.0,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 1.5,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16.0),
                    ShaderMask(
                      shaderCallback: (Rect bounds) {
                        return LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: <Color>[
                            Colors.white,
                            const Color(0xFFE0F2FE),
                            accent,
                          ],
                        ).createShader(bounds);
                      },
                      child: Text(
                        'ORBI SPACE',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 40.0,
                          fontWeight: FontWeight.w900,
                          letterSpacing: 4.5,
                          color: Colors.white,
                          shadows: <Shadow>[
                            Shadow(
                              color: accent.withOpacity(0.65),
                              blurRadius: 24.0,
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 10.0),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: <Widget>[
                        Container(
                          width: 72.0,
                          height: 2.0,
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: <Color>[
                                Colors.transparent,
                                accent.withOpacity(0.8),
                              ],
                            ),
                          ),
                        ),
                        const SizedBox(width: 8.0),
                        Container(
                          width: 7.0,
                          height: 7.0,
                          decoration: BoxDecoration(
                            color: accent,
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 8.0),
                        Container(
                          width: 72.0,
                          height: 2.0,
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: <Color>[
                                accent.withOpacity(0.8),
                                Colors.transparent,
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 28.0),
                    AnimatedBuilder(
                      animation: _pulseController,
                      builder: (BuildContext context, Widget? child) {
                        final double t = Curves.easeInOut
                            .transform(_pulseController.value);
                        final double scale = 1.0 + 0.045 * t;
                        final double glow = 0.38 + 0.56 * t;
                        return Transform.scale(
                          scale: scale,
                          child: InkWell(
                            borderRadius: BorderRadius.circular(14.0),
                            onTap: hasSaved
                                ? widget.onContinueSaved
                                : widget.onStartNew,
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 28.0,
                                vertical: 14.0,
                              ),
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(14.0),
                                border: Border.all(
                                  color: accent.withOpacity(glow),
                                  width: 2.0,
                                ),
                                gradient: LinearGradient(
                                  colors: <Color>[
                                    accent.withOpacity(0.28),
                                    accent.withOpacity(0.14),
                                    accent.withOpacity(0.28),
                                  ],
                                ),
                                boxShadow: <BoxShadow>[
                                  BoxShadow(
                                    color: accent.withOpacity(0.32 * glow),
                                    blurRadius: 18.0,
                                    spreadRadius: 1.0,
                                  ),
                                ],
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: <Widget>[
                                  if (hasSaved)
                                    CompanyEmblemWidget(
                                      company: widget.savedCompany!,
                                      size: 26.0,
                                    )
                                  else
                                    Icon(
                                      Icons.rocket_launch,
                                      color: accent,
                                      size: 22.0,
                                    ),
                                  const SizedBox(width: 10.0),
                                  Flexible(
                                    child: Text(
                                      ctaText,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 16.0,
                                        fontWeight: FontWeight.w900,
                                        letterSpacing: 1.6,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 10.0),
                                  Icon(
                                    Icons.play_arrow_rounded,
                                    color: accent,
                                    size: 24.0,
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class PilotOnboardingScreen extends StatefulWidget {
  const PilotOnboardingScreen({
    super.key,
    required this.onBackToIntro,
    required this.onComplete,
  });

  final VoidCallback onBackToIntro;
  final void Function(PilotCompany company, String pilotName) onComplete;

  @override
  State<PilotOnboardingScreen> createState() => _PilotOnboardingScreenState();
}

class _PilotOnboardingScreenState extends State<PilotOnboardingScreen> {
  int _step = 0; // 0 = Company Selection, 1 = Pilot Name Input
  PilotCompany _selectedCompany = PilotCompany.aether;
  final TextEditingController _nameController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  String get _trimmedName => _nameController.text.trim();
  bool get _isNameValid => _trimmedName.length >= 3 && _trimmedName.length <= 14;

  void _launch() {
    if (!_isNameValid) return;
    widget.onComplete(_selectedCompany, _trimmedName);
  }

  @override
  Widget build(BuildContext context) {
    final PilotCompanySpec spec = PilotCompanyCatalog.of(_selectedCompany);
    final Color accent = spec.primaryColor;

    return Scaffold(
      backgroundColor: const Color(0xFF030612),
      resizeToAvoidBottomInset: true,
      body: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          AnimatedSpaceBackdrop(accentColor: accent),
          SafeArea(
            child: _step == 0
                ? _buildCompanyStep(accent)
                : _buildNameStep(spec),
          ),
        ],
      ),
    );
  }

  Widget _buildCompanyStep(Color accent) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 12.0),
      child: Column(
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              OutlinedButton.icon(
                onPressed: widget.onBackToIntro,
                icon: const Icon(Icons.arrow_back, size: 18.0),
                label: const Text('BACK'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.white,
                  side: const BorderSide(color: Color(0xFF334155)),
                ),
              ),
              Column(
                children: <Widget>[
                  Text(
                    'STEP 1 OF 2 • ALLEGIANCE',
                    style: TextStyle(
                      color: accent,
                      fontSize: 11.0,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 1.6,
                    ),
                  ),
                  const Text(
                    'CHOOSE YOUR COMPANY',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 20.0,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 1.2,
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 84.0),
            ],
          ),
          const SizedBox(height: 10.0),
          Expanded(
            child: Row(
              children: PilotCompany.values.map((PilotCompany company) {
                final PilotCompanySpec cSpec = PilotCompanyCatalog.of(company);
                final bool selected = _selectedCompany == company;
                return Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 6.0),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(14.0),
                      onTap: () => setState(() => _selectedCompany = company),
                      child: Container(
                        padding: const EdgeInsets.all(12.0),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(14.0),
                          gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: selected
                                ? <Color>[
                                    cSpec.primaryColor.withOpacity(0.24),
                                    cSpec.darkBgColor.withOpacity(0.94),
                                  ]
                                : const <Color>[
                                    Color(0xD90B1324),
                                    Color(0xE6060C18),
                                  ],
                          ),
                          border: Border.all(
                            color: selected
                                ? cSpec.primaryColor
                                : cSpec.primaryColor.withOpacity(0.35),
                            width: selected ? 2.2 : 1.2,
                          ),
                        ),
                        child: Stack(
                          children: <Widget>[
                            if (selected)
                              Align(
                                alignment: Alignment.topRight,
                                child: Icon(
                                  Icons.check_circle,
                                  color: cSpec.primaryColor,
                                  size: 20.0,
                                ),
                              ),
                            Center(
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: <Widget>[
                                  CompanyEmblemWidget(
                                    company: company,
                                    size: 56.0,
                                  ),
                                  const SizedBox(height: 8.0),
                                  Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: <Widget>[
                                      Text(
                                        cSpec.displayName,
                                        style: const TextStyle(
                                          color: Colors.white,
                                          fontSize: 15.0,
                                          fontWeight: FontWeight.w900,
                                          letterSpacing: 1.2,
                                        ),
                                      ),
                                      const SizedBox(width: 6.0),
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 6.0,
                                          vertical: 2.0,
                                        ),
                                        decoration: BoxDecoration(
                                          color: cSpec.primaryColor
                                              .withOpacity(0.2),
                                          borderRadius:
                                              BorderRadius.circular(6.0),
                                          border: Border.all(
                                            color: cSpec.primaryColor
                                                .withOpacity(0.7),
                                          ),
                                        ),
                                        child: Text(
                                          '[${cSpec.tag}]',
                                          style: TextStyle(
                                            color: cSpec.primaryColor,
                                            fontSize: 11.0,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 6.0),
                                  Text(
                                    cSpec.lore,
                                    textAlign: TextAlign.center,
                                    maxLines: 3,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      color: selected
                                          ? Colors.white.withOpacity(0.92)
                                          : const Color(0xFF94A3B8),
                                      fontSize: 12.0,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 10.0),
          Align(
            alignment: Alignment.centerRight,
            child: ElevatedButton.icon(
              onPressed: () => setState(() => _step = 1),
              icon: const Text(
                'NEXT: CALLSIGN',
                style: TextStyle(
                  fontWeight: FontWeight.w900,
                  letterSpacing: 1.1,
                ),
              ),
              label: const Icon(Icons.arrow_forward, size: 18.0),
              style: ElevatedButton.styleFrom(
                backgroundColor: accent,
                foregroundColor: const Color(0xFF040711),
                minimumSize: const Size(164.0, 46.0),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNameStep(PilotCompanySpec spec) {
    final Color primary = spec.primaryColor;
    final String previewName = _trimmedName.isEmpty ? 'PILOT' : _trimmedName;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 14.0),
      child: Column(
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              OutlinedButton.icon(
                onPressed: () => setState(() => _step = 0),
                icon: const Icon(Icons.arrow_back, size: 18.0),
                label: const Text('BACK'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.white,
                  side: const BorderSide(color: Color(0xFF334155)),
                ),
              ),
              Column(
                children: <Widget>[
                  Text(
                    'STEP 2 OF 2 • CALLSIGN',
                    style: TextStyle(
                      color: primary,
                      fontSize: 11.0,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 1.6,
                    ),
                  ),
                  const Text(
                    'PILOT REGISTRATION',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 20.0,
                      fontWeight: FontWeight.w900,
                      letterSpacing: 1.2,
                    ),
                  ),
                ],
              ),
              const SizedBox(width: 84.0),
            ],
          ),
          const SizedBox(height: 16.0),
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 680.0),
            child: Container(
              padding: const EdgeInsets.symmetric(
                horizontal: 22.0,
                vertical: 18.0,
              ),
              decoration: BoxDecoration(
                color: const Color(0xEA0B1324),
                borderRadius: BorderRadius.circular(16.0),
                border: Border.all(
                  color: primary.withOpacity(0.65),
                  width: 1.5,
                ),
              ),
              child: Row(
                children: <Widget>[
                  SizedBox(
                    width: 170.0,
                    child: Column(
                      children: <Widget>[
                        CompanyEmblemWidget(
                          company: spec.company,
                          size: 64.0,
                        ),
                        const SizedBox(height: 8.0),
                        Text(
                          spec.displayName,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 14.0,
                            fontWeight: FontWeight.w900,
                            letterSpacing: 1.2,
                          ),
                        ),
                        const SizedBox(height: 6.0),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10.0,
                            vertical: 5.0,
                          ),
                          decoration: BoxDecoration(
                            color: spec.darkBgColor,
                            borderRadius: BorderRadius.circular(8.0),
                            border: Border.all(
                              color: primary.withOpacity(0.7),
                            ),
                          ),
                          child: Text(
                            '[${spec.tag}] $previewName',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: primary,
                              fontSize: 12.5,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 20.0),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: <Widget>[
                        TextField(
                          controller: _nameController,
                          maxLength: 14,
                          textCapitalization: TextCapitalization.words,
                          textInputAction: TextInputAction.done,
                          onChanged: (_) => setState(() {}),
                          onSubmitted: (_) => _launch(),
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 16.0,
                            fontWeight: FontWeight.w700,
                          ),
                          decoration: InputDecoration(
                            labelText: 'PILOT NAME',
                            hintText: 'Enter 3–14 characters',
                            helperText:
                                '3 to 14 characters • Displayed in space & Hangar',
                            helperStyle: TextStyle(
                              color: (_trimmedName.isNotEmpty && !_isNameValid)
                                  ? const Color(0xFFFCA5A5)
                                  : const Color(0xFF94A3B8),
                              fontSize: 11.5,
                            ),
                            counterStyle: TextStyle(
                              color: _isNameValid
                                  ? primary
                                  : const Color(0xFF94A3B8),
                              fontWeight: FontWeight.bold,
                            ),
                            filled: true,
                            fillColor: const Color(0xFF060C18),
                            labelStyle: TextStyle(color: primary),
                            enabledBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(10.0),
                              borderSide:
                                  const BorderSide(color: Color(0xFF1E3A5F)),
                            ),
                            focusedBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(10.0),
                              borderSide: BorderSide(color: primary, width: 1.8),
                            ),
                          ),
                        ),
                        const SizedBox(height: 10.0),
                        ElevatedButton.icon(
                          onPressed: _isNameValid ? _launch : null,
                          icon: const Icon(Icons.rocket_launch, size: 18.0),
                          label: const Text(
                            'LAUNCH SHIP',
                            style: TextStyle(
                              fontWeight: FontWeight.w900,
                              letterSpacing: 1.4,
                            ),
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: primary,
                            foregroundColor: const Color(0xFF040711),
                            disabledBackgroundColor: const Color(0xFF1E293B),
                            disabledForegroundColor: const Color(0xFF64748B),
                            minimumSize: const Size(double.infinity, 48.0),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
