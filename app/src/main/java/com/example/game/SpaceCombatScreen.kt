package com.example.game

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.EmeraldHull
import com.example.ui.theme.SpaceDeepSurface
import com.example.ui.theme.SpaceHudBorder
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun SpaceCombatScreen(
    viewModel: GameViewModel = viewModel()
) {
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val screenStage by viewModel.screenStage.collectAsStateWithLifecycle()
    val selectedCompany by viewModel.selectedOnboardingCompany.collectAsStateWithLifecycle()
    val pilotNameDraft by viewModel.pilotNameDraft.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isMenuOpen by viewModel.isMenuOpen.collectAsStateWithLifecycle()
    val isInventoryOpen by viewModel.isInventoryOpen.collectAsStateWithLifecycle()
    val isGuideOpen by viewModel.isGuideOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val isSkillsOpen by viewModel.isSkillsOpen.collectAsStateWithLifecycle()
    val lastCraftOutcome by viewModel.lastCraftOutcome.collectAsStateWithLifecycle()
    val engine = viewModel.engine

    CompositionLocalProvider(LocalGameLanguage provides language) {
        val strings = GameStrings.of(language)

    when (screenStage) {
        AppScreenStage.INTRO -> {
            IntroTitleScreen(
                savedPilotName = hud.pilotName,
                savedCompany = hud.pilotCompany,
                onContinueSaved = { viewModel.continueAsSavedPilot() },
                onStartNew = { viewModel.beginOnboarding() }
            )
            return@CompositionLocalProvider
        }

        AppScreenStage.ONBOARDING_COMPANY -> {
            CompanySelectionScreen(
                selectedCompany = selectedCompany,
                onSelectCompany = { viewModel.selectOnboardingCompany(it) },
                onNext = { viewModel.proceedToPilotNameStep() },
                onBack = { viewModel.goBackToIntro() }
            )
            return@CompositionLocalProvider
        }

        AppScreenStage.ONBOARDING_NAME -> {
            PilotNameRegistrationScreen(
                company = selectedCompany ?: PilotCompany.AETHER,
                pilotNameDraft = pilotNameDraft,
                onPilotNameChange = { viewModel.updatePilotNameDraft(it) },
                onLaunchShip = { viewModel.completePilotRegistration() },
                onBack = { viewModel.goBackToCompanySelection() }
            )
            return@CompositionLocalProvider
        }

        AppScreenStage.GAMEPLAY -> Unit
    }

    var frameTick by remember { mutableLongStateOf(0L) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var cameraZoom by remember { mutableFloatStateOf(1.0f) }
    var isPinchZooming by remember { mutableStateOf(false) }
    var showAdminActiveToast by remember { mutableStateOf(false) }
    var lastAdminModeState by remember { mutableStateOf(hud.isAdminMode) }
    val popupPaint = remember { createPopupTextPaint() }

    LaunchedEffect(hud.isAdminMode) {
        if (hud.isAdminMode && !lastAdminModeState) {
            lastAdminModeState = true
            showAdminActiveToast = true
            delay(2600L)
            showAdminActiveToast = false
        } else {
            lastAdminModeState = hud.isAdminMode
            if (!hud.isAdminMode) {
                showAdminActiveToast = false
            }
        }
    }
    val context = LocalContext.current
    val versionLabel = remember(context) {
        val rawVersion = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        }.getOrNull()?.trim().orEmpty()

        when {
            rawVersion.isEmpty() -> "v0.1.0"
            rawVersion.startsWith("v", ignoreCase = true) -> rawVersion
            else -> "v$rawVersion"
        }
    }

    LaunchedEffect(screenStage) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    viewModel.tick(dt)
                }
                lastNanos = frameNanos
                frameTick++
            }
        }
    }

    BackHandler(enabled = hud.isAlive) {
        when {
            isMenuOpen -> viewModel.closeMenu()
            isSettingsOpen -> viewModel.closeSettings()
            isGuideOpen -> viewModel.closeGuide()
            isInventoryOpen -> viewModel.closeInventory()
            isSkillsOpen -> viewModel.closeSkills()
            hud.activeStationOverlay != null -> viewModel.closeStation()
            hud.isGalaxyMapOpen -> viewModel.closeGalaxyMap()
            hud.isTacticalMapOpen -> viewModel.closeTacticalMap()
            hud.isHangarOpen -> viewModel.closeHangar()
            else -> viewModel.togglePause()
        }
    }

    val isAnyOverlayOpen = hud.isTacticalMapOpen ||
        hud.isGalaxyMapOpen ||
        hud.activeStationOverlay != null ||
        hud.isHangarOpen ||
        hud.isPaused ||
        isMenuOpen ||
        isInventoryOpen ||
        isGuideOpen ||
        isSettingsOpen ||
        isSkillsOpen ||
        !hud.isAlive

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceVoid)
            .onSizeChanged { viewportSize = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("space_combat_canvas")
                .pointerInput(isAnyOverlayOpen) {
                    if (!isAnyOverlayOpen) {
                        awaitEachGesture {
                            try {
                                val down = awaitFirstDown(requireUnconsumed = true)
                                var totalDrag = 0f
                                var lastPos = down.position
                                var dragged = false
                                var hadMultiTouch = false
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }

                                    if (pressedCount >= 2) {
                                        hadMultiTouch = true
                                        if (!isPinchZooming) {
                                            isPinchZooming = true
                                        }
                                        val zoomDelta = event.calculateZoom()
                                        if (zoomDelta.isFinite() && zoomDelta != 1f) {
                                            cameraZoom = (cameraZoom * zoomDelta).coerceIn(0.6f, 2.5f)
                                        }
                                        event.changes.forEach { change ->
                                            if (change.pressed) change.consume()
                                        }
                                        continue
                                    }

                                    if (pressedCount == 0) {
                                        if (!hadMultiTouch && !dragged) {
                                            val w = viewportSize.width.toFloat()
                                            val h = viewportSize.height.toFloat()
                                            if (w > 0f && h > 0f) {
                                                val scale = computeWorldScale(h) * cameraZoom
                                                val worldX = engine.cameraX + (lastPos.x - w * 0.5f) / scale
                                                val worldY = engine.cameraY + (lastPos.y - h * 0.5f) / scale
                                                viewModel.onTapWorld(worldX, worldY)
                                            }
                                        }
                                        break
                                    }

                                    if (hadMultiTouch) {
                                        event.changes.forEach { it.consume() }
                                        continue
                                    }

                                    val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                                    val delta = change.position - lastPos
                                    lastPos = change.position
                                    totalDrag += hypot(delta.x, delta.y)
                                    if (totalDrag > viewConfiguration.touchSlop) {
                                        dragged = true
                                        viewModel.clearAutoMoveTarget()
                                    }
                                }
                            } finally {
                                isPinchZooming = false
                            }
                        }
                    }
                }
        ) {
            @Suppress("UNUSED_EXPRESSION")
            frameTick
            renderSpaceWorld(
                engine = engine,
                popupPaint = popupPaint,
                cameraZoom = cameraZoom
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TopTacticalHud(
                hud = hud,
                onOpenMenu = { viewModel.openMenu() },
                onOpenGalaxyMap = { viewModel.toggleGalaxyMap() },
                onToggleConfig = { viewModel.toggleConfig() },
                onClaimMission = { viewModel.claimMissionReward() },
                onTogglePause = { viewModel.togglePause() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(2f)
            )

            TacticalMinimap(
                engine = engine,
                frameTick = frameTick,
                onExpandMap = { viewModel.toggleTacticalMap() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(5f)
            )

            VirtualJoystick(
                label = strings.ctrlMove,
                accentColor = CyanLaser,
                tag = "left_movement_joystick",
                onTouchStart = { if (!isPinchZooming) viewModel.clearAutoMoveTarget() },
                onMove = { x, y -> if (!isPinchZooming) viewModel.setMoveJoystick(x, y) },
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                CircularFormationButton(
                    activeFormation = hud.activeFormation,
                    ownedFormationsSet = hud.equipment.ownedFormations,
                    cooldownRemaining = hud.formationCooldownRemaining,
                    onSelectFormation = { viewModel.selectOwnedFormationFromHud(it) },
                    modifier = Modifier.padding(end = 26.dp)
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val nearStationKind = hud.nearestStationKind
                    if (nearStationKind != null) {
                        StationInteractButton(
                            stationKind = nearStationKind,
                            stationName = hud.nearestStationName
                                ?: when (nearStationKind) {
                                    StationKind.REFINERY -> strings.ctrlRefineryShort
                                    StationKind.CRAFTING -> strings.ctrlCraftingShort
                                    StationKind.LOCAL_MARKET -> strings.ctrlMarketShort
                                },
                            isBlackMarket = hud.currentSectorId == "0-0",
                            onInteract = { viewModel.openNearbyStation() },
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    if (hud.isNearJumpGate) {
                        JumpGateButton(
                            gateLabel = hud.jumpGateLabel,
                            targetSectorId = hud.nearestJumpTargetId,
                            onJump = { viewModel.triggerJumpGate() },
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    RocketFireButton(
                        cooldownRemaining = hud.rocketCooldownRemaining,
                        inSafeZone = hud.inSafeZone,
                        onFireRocket = { if (!isPinchZooming) viewModel.fireHomingRocket() },
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    AttackLaserButton(
                        isAutoFireEnabled = hud.isAutoFireEnabled,
                        inSafeZone = hud.inSafeZone,
                        hasTarget = hud.lockedTargetCallsign != null,
                        targetInRange = hud.lockedTargetInRange,
                        onToggle = { if (!isPinchZooming) viewModel.toggleAutoFire() },
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    VirtualJoystick(
                        label = strings.ctrlTarget,
                        accentColor = CrimsonDanger,
                        tag = "right_targeting_joystick",
                        onTouchStart = { if (!isPinchZooming) viewModel.clearAutoMoveTarget() },
                        onMove = { x, y -> if (!isPinchZooming) viewModel.setAimJoystick(x, y) }
                    )
                }
            }

            // Compact Camera Zoom 1x Reset Pill (only visible when zoomed in or out)
            if (abs(cameraZoom - 1.0f) > 0.03f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xCC060D1C))
                        .border(1.dp, CyanLaser.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
                        .clickable { cameraZoom = 1.0f }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .zIndex(4f)
                        .testTag("camera_zoom_reset_badge")
                ) {
                    Text(
                        text = "${"%.1f".format(cameraZoom)}x",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.5.sp,
                        color = TacticalMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "1x",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = CyanLaser,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Text(
                text = versionLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
                color = TacticalWhite.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .testTag("hud_version_label")
            )
        }

        if (hud.isTacticalMapOpen && hud.isAlive && !hud.isHangarOpen && !hud.isPaused) {
            ExpandableTacticalMapOverlay(
                engine = engine,
                frameTick = frameTick,
                onTapMapToMove = { wx, wy -> viewModel.setAutoMoveTarget(wx, wy) },
                onOpenGalaxyMap = { viewModel.openGalaxyMap() },
                onClose = { viewModel.closeTacticalMap() },
                modifier = Modifier.zIndex(20f)
            )
        }

        if (hud.isGalaxyMapOpen && hud.isAlive && !hud.isHangarOpen && !hud.isPaused) {
            GalaxyMapOverlay(
                currentSectorId = hud.currentSectorId,
                plottedRoute = hud.plottedRoute,
                onPlotRoute = { viewModel.setPlottedRoute(it) },
                onPlotAndEngageAutopilot = { viewModel.plotRouteAndEngageAutopilot(it) },
                onClearRoute = { viewModel.clearPlottedRoute() },
                onEngageAutopilotToNextGate = { viewModel.engageAutopilotToNextRoutePortal() },
                onSwitchToLocalMap = { viewModel.openTacticalMap() },
                onClose = { viewModel.closeGalaxyMap() },
                modifier = Modifier.zIndex(20f)
            )
        }

        if (hud.activeStationOverlay != null && hud.isAlive && !hud.isHangarOpen && !hud.isPaused) {
            StationOverlay(
                hud = hud,
                lastCraftOutcome = lastCraftOutcome,
                onSwitchStationTab = { viewModel.switchStationTab(it) },
                onRefineResource = { type, units -> viewModel.refineResource(type, units) },
                onBuyMarketResource = { type, isRefined -> viewModel.buyMarketResource(type, isRefined) },
                onSellMarketResource = { type, isRefined, sellAll -> viewModel.sellMarketResource(type, isRefined, sellAll) },
                onBuyMarketEquipment = { itemId -> viewModel.buyMarketEquipment(itemId) },
                onBuyBasicVendorItem = { itemId -> viewModel.buyBasicVendorItem(itemId) },
                onCraftStationRecipe = { recipeId -> viewModel.craftStationRecipe(recipeId) },
                onDismissCraftOutcome = { viewModel.clearCraftOutcome() },
                onOpenSkills = { viewModel.openSkills() },
                onClose = { viewModel.closeStation() }
            )
        }

        if (hud.isHangarOpen && hud.isAlive) {
            HangarShopOverlay(
                hud = hud,
                onSwitchConfig = { viewModel.switchConfig(it) },
                onSelectCapsule = { viewModel.selectCapsule() },
                onSelectCraftedShip = { viewModel.selectCraftedShip(it) },
                onBuyOrSelectShipClass = { viewModel.buyOrSelectShipClass(it) },
                onBuyOrSelectFormation = { viewModel.buyOrSelectFormation(it) },
                onBuyEquipment = { viewModel.buyEquipment(it) },
                onBuyFlaxVant = { viewModel.buyFlaxVant() },
                onRepairVant = { viewModel.repairVant(it) },
                onFuseItems = { viewModel.fuseItems(it) },
                onFuseFlaxVants = { viewModel.fuseFlaxVants() },
                onEquipFromInventory = { viewModel.equipFromInventory(it) },
                onEquipItemToVantSlot = { vantId, slotIdx ->
                    viewModel.equipItemToVantSlot(vantId, slotIdx)
                },
                onEquipFromInventoryToVant = { viewModel.equipFromInventoryToVant(it) },
                onUnequipVantSlot = { vantId, slotIdx ->
                    viewModel.unequipVantSlot(vantId, slotIdx)
                },
                onUnequipLaserSlot = { viewModel.unequipLaserSlot(it) },
                onUnequipShieldSlot = { viewModel.unequipShieldSlot(it) },
                onUnequipSectorChip = { viewModel.unequipSectorChip() },
                onClaimMissionReward = { viewModel.claimMissionReward() },
                onResetSave = { viewModel.resetSave() },
                onDebugAddCredits = { viewModel.debugAddOneMillionCredits() },
                onToggleAdminMode = { viewModel.toggleAdminMode() },
                onClose = { viewModel.closeHangar() }
            )
        }

        if (hud.isPaused && !hud.isHangarOpen && hud.isAlive && !isSettingsOpen) {
            TacticalModalOverlay(
                title = strings.pausedTitle,
                subtitle = "${hud.activeShipDisplayName} • ${strings.hudCredits}: ${"%,d".format(hud.credits)} • ${strings.hudKills}: ${hud.kills}",
                accentColor = CyanLaser,
                primaryLabel = strings.actionResume,
                primaryTag = "resume_button",
                onPrimaryClick = { viewModel.togglePause() },
                secondaryLabel = strings.actionResetSave,
                secondaryTag = "reset_save_button",
                onSecondaryClick = { viewModel.resetSave() }
            )
        }

        if (isSkillsOpen && hud.isAlive) {
            PilotSkillsOverlay(
                pilotName = hud.pilotName,
                pilotCompany = hud.pilotCompany,
                skills = hud.pilotSkills,
                isAdminMode = hud.isAdminMode,
                onAdminCycleSkill = { viewModel.adminCycleSkillLevel(it) },
                onAdminSetSkill = { skill, level -> viewModel.adminSetSkillLevel(skill, level) },
                onAdminSetAllSkills = { level -> viewModel.adminSetAllSkillsLevel(level) },
                onClose = { viewModel.closeSkills() }
            )
        }

        if (isInventoryOpen && hud.isAlive) {
            InventoryOverlay(
                hud = hud,
                onClose = { viewModel.closeInventory() }
            )
        }

        if (isGuideOpen && hud.isAlive) {
            GameGuideOverlay(
                onClose = { viewModel.closeGuide() }
            )
        }

        if (isSettingsOpen) {
            SettingsModalOverlay(
                currentLanguage = language,
                onSelectLanguage = { viewModel.setLanguage(it) },
                onClose = { viewModel.closeSettings() }
            )
        }

        if (isMenuOpen && hud.isAlive) {
            UnifiedMenuOverlay(
                hud = hud,
                currentLanguage = language,
                onOpenSkills = { viewModel.openSkillsFromMenu() },
                onOpenHangar = { viewModel.openHangarFromMenu() },
                onOpenInventory = { viewModel.openInventoryFromMenu() },
                onOpenGuide = { viewModel.openGuideFromMenu() },
                onOpenSettings = { viewModel.openSettingsFromMenu() },
                onClose = { viewModel.closeMenu() }
            )
        }

        if (!hud.isAlive) {
            DeathRespawnModalOverlay(
                hud = hud,
                strings = strings,
                onRespawn = { viewModel.relaunchShip() }
            )
        }

        if (showAdminActiveToast) {
            val toastText = if (language == GameLanguage.ES) {
                "Modo Admin Activo (Pruebas Gratis)"
            } else {
                "Admin Mode Active (Free Testing)"
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 12.dp)
                    .zIndex(40f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEB071B18))
                    .border(1.3.dp, AmberCredits, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("admin_mode_active_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldHull,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = toastText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFEF08A),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun SettingsModalOverlay(
    currentLanguage: GameLanguage,
    onSelectLanguage: (GameLanguage) -> Unit,
    onClose: () -> Unit
) {
    val strings = GameStrings.of(currentLanguage)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xC8040711))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .testTag("settings_modal")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(min = 300.dp, max = 440.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceDeepSurface)
                .border(1.5.dp, CyanLaser.copy(alpha = 0.78f), RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = CyanLaser,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = CyanLaser,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                OutlinedButton(
                    onClick = onClose,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalWhite),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.actionClose, style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = AmberCredits,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = strings.settingsLanguageLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TacticalWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (lang in GameLanguage.entries) {
                    val selected = lang == currentLanguage
                    Button(
                        onClick = { onSelectLanguage(lang) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) CyanLaser else Color(0xFF132238),
                            contentColor = if (selected) SpaceVoid else TacticalWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("language_option_${lang.code}")
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = lang.displayLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }

            Text(
                text = strings.settingsLanguageHint,
                style = MaterialTheme.typography.bodySmall,
                color = TacticalMuted
            )
        }
    }
}

@Composable
private fun TacticalModalOverlay(
    title: String,
    subtitle: String,
    accentColor: Color,
    primaryLabel: String,
    primaryTag: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String? = null,
    secondaryTag: String? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3040711))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceDeepSurface)
                .border(1.5.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TacticalMuted
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPrimaryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = SpaceVoid
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag(primaryTag)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (secondaryLabel != null && secondaryTag != null && onSecondaryClick != null) {
                    OutlinedButton(
                        onClick = onSecondaryClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalWhite),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag(secondaryTag)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AmberCredits
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = secondaryLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeathRespawnModalOverlay(
    hud: HudSnapshot,
    strings: GameText,
    onRespawn: () -> Unit
) {
    val summary = hud.lastDeathSummary
    val homeCityId = summary?.homeCityId ?: GalaxyMapGraph.homeCityForCompany(hud.pilotCompany)
    val droppedTotal = (summary?.cargoDroppedToWreck ?: 0) + (summary?.gearDroppedToWreck ?: 0)
    val destroyedTotal = (summary?.cargoDestroyed ?: 0) + (summary?.gearDestroyed ?: 0)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC040711))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
            .testTag("death_respawn_modal")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .widthIn(min = 300.dp, max = 460.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceDeepSurface)
                .border(1.5.dp, CrimsonDanger.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_capsule_ship),
                contentDescription = strings.capsuleName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, CyanLaser.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .testTag("death_modal_capsule_art")
            )

            Text(
                text = strings.shipDestroyedTitle,
                style = MaterialTheme.typography.titleLarge,
                color = CrimsonDanger,
                fontWeight = FontWeight.ExtraBold
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC071020))
                    .border(1.dp, SpaceHudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (summary != null && !summary.wasInCapsule && summary.destroyedShipName != null) {
                    Text(
                        text = "• ${strings.deathShipLostPrefix} ${summary.destroyedShipName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrimsonDanger,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "• ${strings.deathCapsuleSafeNotice}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanLaser,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (summary != null && (droppedTotal > 0 || destroyedTotal > 0)) {
                    Text(
                        text = "• ${strings.deathWreckSpawnedPrefix} ${summary.deathSectorId} (${summary.deathX}, ${summary.deathY}) • $droppedTotal / $destroyedTotal ${strings.deathDestroyedRatioSuffix}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberCredits
                    )
                }

                if (summary != null && summary.vantsDamagedCount > 0) {
                    Text(
                        text = "• ${strings.deathVantsDamagedPrefix} ${summary.vantsDamagedCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDBA74)
                    )
                }

                Text(
                    text = "• ${strings.deathRespawnCityPrefix} $homeCityId (${strings.capsuleName})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TacticalWhite
                )
            }

            Button(
                onClick = onRespawn,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanLaser,
                    contentColor = SpaceVoid
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("relaunch_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.deathCapsuleRespawnBtn,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
    }
}
