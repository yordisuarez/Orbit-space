import 'package:flutter/material.dart';

import '../game/dark_orbit_game.dart';

// =============================================================================
// 9 PILOT SKILLS OVERLAY (LV 1-99) WITH ADMIN QUICK-TEST CONTROLS
// =============================================================================
class PilotSkillsOverlay extends StatefulWidget {
  const PilotSkillsOverlay({super.key, required this.game});

  final DarkOrbitGame game;

  @override
  State<PilotSkillsOverlay> createState() => _PilotSkillsOverlayState();
}

class _PilotSkillsOverlayState extends State<PilotSkillsOverlay> {
  @override
  Widget build(BuildContext context) {
    final DarkOrbitGame game = widget.game;
    final GameLanguage lang = game.language;
    final bool admin = game.isAdminMode;

    return Material(
      color: const Color(0xDD03060E),
      child: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 740.0, maxHeight: 580.0),
            child: Container(
              margin: const EdgeInsets.all(12.0),
              padding: const EdgeInsets.all(14.0),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: <Color>[Color(0xFF0A1424), Color(0xFF060B16)],
                ),
                borderRadius: BorderRadius.circular(16.0),
                border: Border.all(
                  color: const Color(0xFFC084FC).withOpacity(0.6),
                  width: 1.2,
                ),
              ),
              child: Column(
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      const Icon(
                        Icons.auto_graph_rounded,
                        color: Color(0xFFC084FC),
                        size: 22.0,
                      ),
                      const SizedBox(width: 10.0),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Text(
                              GameStrings.t(lang, 'PILOT SKILLS', 'HABILIDADES'),
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 16.0,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                            Text(
                              '${GameStrings.t(lang, 'TOTAL LV', 'NV TOTAL')}: ${game.totalSkillLevels} / ${SkillType.values.length * kPilotSkillMaxLevel}',
                              style: const TextStyle(
                                color: Color(0xFF94A3B8),
                                fontSize: 11.5,
                              ),
                            ),
                          ],
                        ),
                      ),
                      InkWell(
                        onTap: game.closeSkills,
                        borderRadius: BorderRadius.circular(8.0),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10.0,
                            vertical: 7.0,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFF1A253B),
                            borderRadius: BorderRadius.circular(8.0),
                            border: Border.all(color: const Color(0xFF334155)),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              const Icon(Icons.close,
                                  color: Colors.white, size: 15.0),
                              const SizedBox(width: 4.0),
                              Text(
                                GameStrings.actionClose(lang),
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 11.0,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                  if (admin) ...<Widget>[
                    const SizedBox(height: 10.0),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10.0,
                        vertical: 7.0,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0x224ADE80),
                        borderRadius: BorderRadius.circular(10.0),
                        border: Border.all(
                          color: const Color(0xFF4ADE80).withOpacity(0.6),
                        ),
                      ),
                      child: Row(
                        children: <Widget>[
                          const Icon(
                            Icons.admin_panel_settings_rounded,
                            color: Color(0xFF4ADE80),
                            size: 16.0,
                          ),
                          const SizedBox(width: 6.0),
                          Expanded(
                            child: Text(
                              GameStrings.t(
                                lang,
                                'ADMIN ALL SKILLS:',
                                'ADMIN TODAS:',
                              ),
                              style: const TextStyle(
                                color: Color(0xFF4ADE80),
                                fontSize: 10.5,
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                          ),
                          for (final int lv in const <int>[1, 33, 66, 99])
                            Padding(
                              padding: const EdgeInsets.only(left: 5.0),
                              child: InkWell(
                                onTap: () {
                                  game.adminSetAllSkillsLevel(lv);
                                  setState(() {});
                                },
                                borderRadius: BorderRadius.circular(6.0),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8.0,
                                    vertical: 4.0,
                                  ),
                                  decoration: BoxDecoration(
                                    color: lv == 99
                                        ? const Color(0x33FACC15)
                                        : const Color(0xFF132238),
                                    borderRadius: BorderRadius.circular(6.0),
                                    border: Border.all(
                                      color: lv == 99
                                          ? const Color(0xFFFACC15)
                                          : const Color(0xFF4ADE80),
                                    ),
                                  ),
                                  child: Text(
                                    lv == 99 ? 'MAX 99' : 'LV $lv',
                                    style: TextStyle(
                                      color: lv == 99
                                          ? const Color(0xFFFACC15)
                                          : Colors.white,
                                      fontSize: 10.0,
                                      fontWeight: FontWeight.w900,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 10.0),
                  Expanded(
                    child: ListView.separated(
                      itemCount: SkillType.values.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 7.0),
                      itemBuilder: (_, int i) {
                        final SkillType skill = SkillType.values[i];
                        final SkillProgress prog = game.skillProgressOf(skill);
                        final Color accent = skill.accentColor;
                        return Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12.0,
                            vertical: 9.0,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFF0E1A2F),
                            borderRadius: BorderRadius.circular(11.0),
                            border: Border.all(color: accent.withOpacity(0.35)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Row(
                                children: <Widget>[
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 7.0,
                                      vertical: 3.0,
                                    ),
                                    decoration: BoxDecoration(
                                      color: accent.withOpacity(0.18),
                                      borderRadius: BorderRadius.circular(6.0),
                                    ),
                                    child: Text(
                                      'LV ${prog.level}',
                                      style: TextStyle(
                                        color: accent,
                                        fontSize: 11.0,
                                        fontWeight: FontWeight.w900,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 9.0),
                                  Expanded(
                                    child: Text(
                                      GameStrings.skillName(skill, lang),
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontSize: 13.5,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                  ),
                                  Text(
                                    GameStrings.skillActiveBonus(
                                      skill,
                                      prog.level,
                                      lang,
                                    ),
                                    style: TextStyle(
                                      color: accent,
                                      fontSize: 11.2,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                  if (admin) ...<Widget>[
                                    const SizedBox(width: 8.0),
                                    InkWell(
                                      onTap: () {
                                        game.adminCycleSkillLevel(skill);
                                        setState(() {});
                                      },
                                      child: Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 6.0,
                                          vertical: 3.0,
                                        ),
                                        decoration: BoxDecoration(
                                          color: const Color(0x334ADE80),
                                          borderRadius:
                                              BorderRadius.circular(5.0),
                                          border: Border.all(
                                            color: const Color(0xFF4ADE80),
                                          ),
                                        ),
                                        child: const Text(
                                          '+LV',
                                          style: TextStyle(
                                            color: Color(0xFF4ADE80),
                                            fontSize: 9.5,
                                            fontWeight: FontWeight.w900,
                                          ),
                                        ),
                                      ),
                                    ),
                                    const SizedBox(width: 4.0),
                                    InkWell(
                                      onTap: () {
                                        game.adminSetSkillLevel(skill, 99);
                                        setState(() {});
                                      },
                                      child: Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 6.0,
                                          vertical: 3.0,
                                        ),
                                        decoration: BoxDecoration(
                                          color: const Color(0x33FACC15),
                                          borderRadius:
                                              BorderRadius.circular(5.0),
                                          border: Border.all(
                                            color: const Color(0xFFFACC15),
                                          ),
                                        ),
                                        child: const Text(
                                          '99',
                                          style: TextStyle(
                                            color: Color(0xFFFACC15),
                                            fontSize: 9.5,
                                            fontWeight: FontWeight.w900,
                                          ),
                                        ),
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                              const SizedBox(height: 6.0),
                              ClipRRect(
                                borderRadius: BorderRadius.circular(4.0),
                                child: LinearProgressIndicator(
                                  value: prog.progressFraction,
                                  minHeight: 5.5,
                                  backgroundColor: const Color(0xFF060C18),
                                  valueColor:
                                      AlwaysStoppedAnimation<Color>(accent),
                                ),
                              ),
                              const SizedBox(height: 3.0),
                              Text(
                                prog.isMaxLevel
                                    ? GameStrings.t(lang, 'MAX LEVEL (99)', 'NIVEL MÁXIMO (99)')
                                    : '${prog.xp} / ${prog.xpToNextLevel} XP',
                                style: const TextStyle(
                                  color: Color(0xFF64748B),
                                  fontSize: 10.0,
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
            ),
          ),
        ),
      ),
    );
  }
}
