import 'dart:convert';

import 'package:flame/game.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'game/dark_orbit_game.dart';
import 'ui/galaxy_map_screen.dart';
import 'ui/hangar_overlay.dart';
import 'ui/intro_onboarding_screens.dart';
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
        home = Scaffold(
          backgroundColor: const Color(0xFF040711),
          body: GameWidget<DarkOrbitGame>(
            key: ValueKey<int>(_gameSessionId),
            game: game,
            overlayBuilderMap:
                <String, Widget Function(BuildContext, DarkOrbitGame)>{
              DarkOrbitGame.hangarOverlayId:
                  (BuildContext context, DarkOrbitGame g) {
                return HangarOverlay(game: g);
              },
              DarkOrbitGame.tacticalMapOverlayId:
                  (BuildContext context, DarkOrbitGame g) {
                return TacticalMapOverlay(game: g);
              },
              DarkOrbitGame.galaxyMapOverlayId:
                  (BuildContext context, DarkOrbitGame g) {
                return GalaxyMapScreen(game: g);
              },
            },
          ),
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
