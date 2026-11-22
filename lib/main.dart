import 'dart:convert';

import 'package:flame/game.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'game/dark_orbit_game.dart';
import 'ui/galaxy_map_screen.dart';
import 'ui/hangar_overlay.dart';
import 'ui/hud_menu_overlays.dart';
import 'ui/intro_onboarding_screens.dart';
import 'ui/pilot_skills_overlay.dart';
import 'ui/station_overlay.dart';
import 'ui/tactical_map_overlay.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Lock screen orientation to landscape on startup
  await SystemChrome.setPreferredOrientations(<DeviceOrientation>[
    DeviceOrientation.landscapeLeft,
    DeviceOrientation.landscapeRight,
  ]);
  await SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);

  runApp(const DarkOrbitApp());
}

enum _AppStage {
  loading,
  intro,
  onboarding,
  gameplay,
}

class DarkOrbitApp extends StatefulWidget {
  const DarkOrbitApp({super.key});

  @override
  State<DarkOrbitApp> createState() => _DarkOrbitAppState();
}

class _DarkOrbitAppState extends State<DarkOrbitApp> {
  _AppStage _stage = _AppStage.loading;
  String? _savedPilotName;
  PilotCompany? _savedCompany;
  DarkOrbitGame? _activeGame;
  int _gameSessionId = 0;

  @override
  void initState() {
    super.initState();
    _loadSavedPilotProfile();
  }

  Future<void> _loadSavedPilotProfile() async {
    String? pilotName;
    PilotCompany? company;
    try {
      final SharedPreferences prefs = await SharedPreferences.getInstance();
      final String? prefName =
          prefs.getString(DarkOrbitGame.pilotNamePrefsKey)?.trim();
      final PilotCompany? prefCompany = PilotCompanyCatalog.fromId(
        prefs.getString(DarkOrbitGame.companyIdPrefsKey),
      );
      if (prefName != null && prefName.length >= 3 && prefName.length <= 14) {
        pilotName = prefName;
      }
      company = prefCompany;

      // Fallback to JSON save payload if standalone keys weren't written yet
      if (pilotName == null || company == null) {
        final String? raw = prefs.getString(DarkOrbitGame.savePrefsKey);
        if (raw != null && raw.isNotEmpty) {
          final Map<String, dynamic> data =
              jsonDecode(raw) as Map<String, dynamic>;
          final String? jsonName = (data['pilotName'] as String?)?.trim();
          final PilotCompany? jsonCompany =
              PilotCompanyCatalog.fromId(data['companyId'] as String?);
          if (pilotName == null &&
              jsonName != null &&
              jsonName.length >= 3 &&
              jsonName.length <= 14) {
            pilotName = jsonName;
          }
          company ??= jsonCompany;
        }
      }
    } catch (_) {
      // Safe fallback if SharedPreferences is unavailable
    }

    if (!mounted) return;
    setState(() {
      _savedPilotName = (pilotName != null && company != null) ? pilotName : null;
      _savedCompany = (pilotName != null && company != null) ? company : null;
      _stage = _AppStage.intro;
    });
  }

  void _launchGameplay({
    required String pilotName,
    required PilotCompany company,
  }) {
    _gameSessionId++;
    final DarkOrbitGame game = DarkOrbitGame(
      pilotName: pilotName,
      pilotCompany: company,
      onResetToIntro: _handleResetToIntro,
    );
    setState(() {
      _savedPilotName = pilotName;
      _savedCompany = company;
      _activeGame = game;
      _stage = _AppStage.gameplay;
    });
  }

  Future<void> _completeOnboarding(
    PilotCompany company,
    String pilotName,
  ) async {
    final String cleaned = pilotName.trim();
    if (cleaned.length < 3 || cleaned.length > 14) return;
    try {
      final SharedPreferences prefs = await SharedPreferences.getInstance();
      await prefs.setString(DarkOrbitGame.pilotNamePrefsKey, cleaned);
      await prefs.setString(
        DarkOrbitGame.companyIdPrefsKey,
        PilotCompanyCatalog.of(company).id,
      );
    } catch (_) {
      // Continue into gameplay even if prefs write fails
    }
    if (!mounted) return;
    _launchGameplay(pilotName: cleaned, company: company);
  }

  void _handleResetToIntro() {
    if (!mounted) return;
    setState(() {
      _savedPilotName = null;
      _savedCompany = null;
      _activeGame = null;
      _stage = _AppStage.intro;
    });
  }

  @override
  Widget build(BuildContext context) {
    Widget home;
    switch (_stage) {
      case _AppStage.loading:
        home = const Scaffold(
          backgroundColor: Color(0xFF030612),
          body: Stack(
            fit: StackFit.expand,
            children: <Widget>[
              AnimatedSpaceBackdrop(),
              Center(
                child: CircularProgressIndicator(
                  color: Color(0xFF00E5FF),
                ),
              ),
            ],
          ),
        );
        break;

      case _AppStage.intro:
        home = IntroTitleScreen(
          savedPilotName: _savedPilotName,
          savedCompany: _savedCompany,
          onContinueSaved: () {
            if (_savedPilotName != null && _savedCompany != null) {
              _launchGameplay(
                pilotName: _savedPilotName!,
                company: _savedCompany!,
              );
            } else {
              setState(() => _stage = _AppStage.onboarding);
            }
          },
          onStartNew: () => setState(() => _stage = _AppStage.onboarding),
        );
        break;

      case _AppStage.onboarding:
        home = PilotOnboardingScreen(
          onBackToIntro: () => setState(() => _stage = _AppStage.intro),
          onComplete: _completeOnboarding,
        );
        break;

      case _AppStage.gameplay:
        final DarkOrbitGame game = _activeGame!;
        home = _GameplayShell(
          key: ValueKey<int>(_gameSessionId),
          game: game,
        );
        break;
    }

    return MaterialApp(
      title: 'Orbi Space',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(useMaterial3: true),
      home: home,
    );
  }
}

/// Hosts the Flame [GameWidget] together with raw multi-touch Pinch-to-Zoom
/// (0.6x..2.5x) and the compact Flutter HUD controls (Unified MENU button,
/// 1x Zoom Reset pill, Station DOCK button, Mining/Salvage progress, and
/// Admin Mode active toast banner).
class _GameplayShell extends StatefulWidget {
  const _GameplayShell({
    super.key,
    required this.game,
  });

  final DarkOrbitGame game;

  @override
  State<_GameplayShell> createState() => _GameplayShellState();
}

class _GameplayShellState extends State<_GameplayShell> {
  final Map<int, Offset> _activePointers = <int, Offset>{};
  double? _lastPinchSpan;

  double? _computeTwoFingerSpan() {
    if (_activePointers.length != 2) return null;
    final Iterator<Offset> it = _activePointers.values.iterator;
    it.moveNext();
    final Offset p1 = it.current;
    it.moveNext();
    final Offset p2 = it.current;
    return (p1 - p2).distance;
  }

  bool _hasBlockingModalOpen(DarkOrbitGame g) {
    return g.overlays.isActive(DarkOrbitGame.hangarOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.galaxyMapOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.menuOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.skillsOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.inventoryOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.guideOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.settingsOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.stationOverlayId) ||
        g.overlays.isActive(DarkOrbitGame.deathOverlayId);
  }

  void _onPointerDown(PointerDownEvent e) {
    _activePointers[e.pointer] = e.position;
    if (_activePointers.length == 2 && !_hasBlockingModalOpen(widget.game)) {
      _lastPinchSpan = _computeTwoFingerSpan();
      widget.game.isPinching = true;
    } else if (_activePointers.length > 2) {
      _lastPinchSpan = null;
    }
  }

  void _onPointerMove(PointerMoveEvent e) {
    if (!_activePointers.containsKey(e.pointer)) return;
    _activePointers[e.pointer] = e.position;
    if (_activePointers.length == 2 && !_hasBlockingModalOpen(widget.game)) {
      final double? span = _computeTwoFingerSpan();
      if (span != null && _lastPinchSpan != null && _lastPinchSpan! > 16.0) {
        final double ratio = span / _lastPinchSpan!;
        if (ratio > 0.5 && ratio < 2.0 && (ratio - 1.0).abs() > 0.002) {
          widget.game.setCameraZoom(widget.game.cameraZoom * ratio);
        }
      }
      _lastPinchSpan = span;
      widget.game.isPinching = true;
    }
  }

  void _onPointerUpOrCancel(int pointerId) {
    _activePointers.remove(pointerId);
    if (_activePointers.length < 2) {
      _lastPinchSpan = null;
      widget.game.isPinching = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final DarkOrbitGame game = widget.game;
    return Scaffold(
      backgroundColor: const Color(0xFF040711),
      body: Stack(
        fit: StackFit.expand,
        children: <Widget>[
          Listener(
            behavior: HitTestBehavior.translucent,
            onPointerDown: _onPointerDown,
            onPointerMove: _onPointerMove,
            onPointerUp: (PointerUpEvent e) => _onPointerUpOrCancel(e.pointer),
            onPointerCancel: (PointerCancelEvent e) =>
                _onPointerUpOrCancel(e.pointer),
            child: GameWidget<DarkOrbitGame>(
              game: game,
              overlayBuilderMap:
                  <String, Widget Function(BuildContext, DarkOrbitGame)>{
                DarkOrbitGame.hangarOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        HangarOverlay(game: g),
                DarkOrbitGame.tacticalMapOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        TacticalMapOverlay(game: g),
                DarkOrbitGame.galaxyMapOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        GalaxyMapScreen(game: g),
                DarkOrbitGame.menuOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        UnifiedMenuOverlay(game: g),
                DarkOrbitGame.skillsOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        PilotSkillsOverlay(game: g),
                DarkOrbitGame.inventoryOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        InventoryOverlay(game: g),
                DarkOrbitGame.guideOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        GameGuideOverlay(game: g),
                DarkOrbitGame.settingsOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        SettingsOverlay(game: g),
                DarkOrbitGame.stationOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        StationOverlay(game: g),
                DarkOrbitGame.deathOverlayId:
                    (BuildContext context, DarkOrbitGame g) =>
                        DeathRespawnOverlay(game: g),
              },
            ),
          ),

          // Live HUD controls (Unified MENU button, 1x Zoom pill, Station DOCK,
          // Mining/Salvage progress, Admin Mode toast banner)
          ValueListenableBuilder<int>(
            valueListenable: game.uiVersion,
            builder: (BuildContext context, int _, Widget? __) {
              final GameStrings s = GameStrings.of(game.language);
              final bool modalOpen = _hasBlockingModalOpen(game);
              final bool showZoomReset =
                  (game.cameraZoom - kCameraZoomDefault).abs() > 0.03;
              final SectorStation? dockStation = game.nearestStation;
              final bool showDockButton = !modalOpen &&
                  dockStation != null &&
                  !game.overlays.isActive(DarkOrbitGame.tacticalMapOverlayId);
              final bool isExtracting = !modalOpen &&
                  (game.activeMiningNode != null ||
                      game.activeSalvageWreck != null);

              return Stack(
                children: <Widget>[
                  // Top-right (beside minimap) compact Unified MENU button
                  if (!modalOpen)
                    Positioned(
                      top: 14.0,
                      right: 150.0,
                      child: Material(
                        color: Colors.transparent,
                        child: InkWell(
                          onTap: game.openMenu,
                          borderRadius: BorderRadius.circular(9.0),
                          child: Container(
                            height: 38.0,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10.0,
                            ),
                            decoration: BoxDecoration(
                              color: game.isAdminMode
                                  ? const Color(0xDD112419)
                                  : const Color(0xCC0B1324),
                              borderRadius: BorderRadius.circular(9.0),
                              border: Border.all(
                                color: game.isAdminMode
                                    ? const Color(0xFF4ADE80)
                                    : const Color(0xFF00E5FF),
                                width: 1.6,
                              ),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: <Widget>[
                                Icon(
                                  Icons.menu,
                                  size: 16.0,
                                  color: game.isAdminMode
                                      ? const Color(0xFF4ADE80)
                                      : const Color(0xFF00E5FF),
                                ),
                                const SizedBox(width: 5.0),
                                Text(
                                  s.hudMenuButton,
                                  maxLines: 1,
                                  softWrap: false,
                                  style: TextStyle(
                                    color: game.isAdminMode
                                        ? const Color(0xFF4ADE80)
                                        : const Color(0xFF00E5FF),
                                    fontSize: 11.5,
                                    fontWeight: FontWeight.w800,
                                    letterSpacing: 0.6,
                                  ),
                                ),
                                if (game.isAdminMode) ...<Widget>[
                                  const SizedBox(width: 5.0),
                                  Container(
                                    width: 7.0,
                                    height: 7.0,
                                    decoration: const BoxDecoration(
                                      color: Color(0xFFFACC15),
                                      shape: BoxShape.circle,
                                    ),
                                  ),
                                ],
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),

                  // Compact 1x Zoom Reset pill (only visible when zoomed != 1.0x)
                  if (!modalOpen && showZoomReset)
                    Positioned(
                      top: 58.0,
                      right: 150.0,
                      child: Material(
                        color: Colors.transparent,
                        child: InkWell(
                          onTap: game.resetCameraZoom,
                          borderRadius: BorderRadius.circular(14.0),
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 9.0,
                              vertical: 5.0,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xD6081426),
                              borderRadius: BorderRadius.circular(14.0),
                              border: Border.all(
                                color: const Color(0xAA38BDF8),
                                width: 1.2,
                              ),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: <Widget>[
                                const Icon(
                                  Icons.zoom_out_map,
                                  size: 13.0,
                                  color: Color(0xFF38BDF8),
                                ),
                                const SizedBox(width: 4.0),
                                Text(
                                  '${game.cameraZoom.toStringAsFixed(1)}x • 1x',
                                  style: const TextStyle(
                                    color: Color(0xFFE0F2FE),
                                    fontSize: 10.5,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),

                  // Admin Mode Active Toast Banner
                  if (game.adminBannerText != null &&
                      game.adminBannerTimer > 0.0)
                    Positioned(
                      top: 58.0,
                      left: 0.0,
                      right: 0.0,
                      child: IgnorePointer(
                        child: Center(
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 14.0,
                              vertical: 6.0,
                            ),
                            decoration: BoxDecoration(
                              color: game.isAdminMode
                                  ? const Color(0xEB0A2E1A)
                                  : const Color(0xEB1E293B),
                              borderRadius: BorderRadius.circular(20.0),
                              border: Border.all(
                                color: game.isAdminMode
                                    ? const Color(0xFF4ADE80)
                                    : const Color(0xFF94A3B8),
                                width: 1.4,
                              ),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: <Widget>[
                                Icon(
                                  game.isAdminMode
                                      ? Icons.verified_user
                                      : Icons.shield_outlined,
                                  size: 15.0,
                                  color: game.isAdminMode
                                      ? const Color(0xFFFACC15)
                                      : const Color(0xFFCBD5E1),
                                ),
                                const SizedBox(width: 7.0),
                                Text(
                                  game.adminBannerText!,
                                  style: TextStyle(
                                    color: game.isAdminMode
                                        ? const Color(0xFF4ADE80)
                                        : const Color(0xFFE2E8F0),
                                    fontSize: 11.5,
                                    fontWeight: FontWeight.w800,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ),

                  // Active Mining / Salvaging Progress Pill
                  if (isExtracting)
                    Positioned(
                      bottom: showDockButton ? 68.0 : 20.0,
                      left: 0.0,
                      right: 0.0,
                      child: IgnorePointer(
                        child: Center(
                          child: _ExtractionProgressPill(game: game),
                        ),
                      ),
                    ),

                  // Station DOCK Button when near a station or inside a Capital City / Nucleus
                  if (showDockButton)
                    Positioned(
                      bottom: 16.0,
                      left: 0.0,
                      right: 0.0,
                      child: Center(
                        child: ElevatedButton.icon(
                          onPressed: () => game.openStation(initialTab: dockStation.kind)
                          ,
                          icon: Icon(
                            dockStation.kind == StationKind.localMarket
                                ? Icons.storefront
                                : Icons.hub,
                            size: 16.0,
                          ),
                          label: Text(
                            '${s.dockButtonLabel} • ${dockStation.name.toUpperCase()}',
                            maxLines: 1,
                            softWrap: false,
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: dockStation.accentColor,
                            foregroundColor: const Color(0xFF040711),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16.0,
                              vertical: 10.0,
                            ),
                            minimumSize: const Size(0.0, 40.0),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12.0),
                            ),
                            textStyle: const TextStyle(
                              fontSize: 11.5,
                              fontWeight: FontWeight.w900,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}

class _ExtractionProgressPill extends StatelessWidget {
  const _ExtractionProgressPill({required this.game});

  final DarkOrbitGame game;

  @override
  Widget build(BuildContext context) {
    final bool isMining = game.activeMiningNode != null;
    final Color accent = isMining
        ? ResourceCatalog.of(game.activeMiningNode!.resourceType).accentColor
        : const Color(0xFFFB923C);
    final String label = isMining
        ? '${ResourceCatalog.of(game.activeMiningNode!.resourceType).displayName} (${game.activeMiningNode!.chargesRemaining}/${game.activeMiningNode!.maxCharges})'
        : 'SALVAGING WRECK (${game.activeSalvageWreck!.chargesRemaining}/${game.activeSalvageWreck!.maxCharges})';
    final double ratio = game.extractionDuration > 0.01
        ? (game.extractionElapsed / game.extractionDuration).clamp(0.0, 1.0)
        : 0.0;

    return Container(
      width: 240.0,
      padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 7.0),
      decoration: BoxDecoration(
        color: const Color(0xDD071020),
        borderRadius: BorderRadius.circular(12.0),
        border: Border.all(color: accent, width: 1.4),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: accent,
                    fontSize: 10.5,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              Text(
                '${(ratio * 100).round()}%',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 10.5,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          const SizedBox(height: 5.0),
          ClipRRect(
            borderRadius: BorderRadius.circular(4.0),
            child: LinearProgressIndicator(
              value: ratio,
              minHeight: 6.0,
              backgroundColor: const Color(0xFF0F172A),
              valueColor: AlwaysStoppedAnimation<Color>(accent),
            ),
          ),
        ],
      ),
    );
  }
}
