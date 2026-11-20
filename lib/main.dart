import 'package:flame/game.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'game/dark_orbit_game.dart';
import 'ui/hangar_overlay.dart';
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

class DarkOrbitApp extends StatelessWidget {
  const DarkOrbitApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'DarkOrbit Mobile',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark(useMaterial3: true),
      home: Scaffold(
        backgroundColor: const Color(0xFF040711),
        body: GameWidget<DarkOrbitGame>(
          game: DarkOrbitGame(),
          overlayBuilderMap: <String, Widget Function(BuildContext, DarkOrbitGame)>{
            DarkOrbitGame.hangarOverlayId:
                (BuildContext context, DarkOrbitGame game) {
              return HangarOverlay(game: game);
            },
            DarkOrbitGame.tacticalMapOverlayId:
                (BuildContext context, DarkOrbitGame game) {
              return TacticalMapOverlay(game: game);
            },
          },
        ),
      ),
    );
  }
}
