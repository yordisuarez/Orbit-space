package com.example.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.theme.AmberCredits
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyanLaser
import com.example.ui.theme.SpaceDeepSurface
import com.example.ui.theme.SpaceVoid
import com.example.ui.theme.TacticalMuted
import com.example.ui.theme.TacticalWhite

@Composable
fun SpaceCombatScreen(
    viewModel: GameViewModel = viewModel()
) {
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val engine = viewModel.engine

    var frameTick by remember { mutableLongStateOf(0L) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val popupPaint = remember { createPopupTextPaint() }

    LaunchedEffect(Unit) {
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
            hud.isTacticalMapOpen -> viewModel.closeTacticalMap()
            hud.isHangarOpen -> viewModel.closeHangar()
            else -> viewModel.togglePause()
        }
    }

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
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val w = viewportSize.width.toFloat()
                        val h = viewportSize.height.toFloat()
                        if (w > 0f && h > 0f) {
                            val scale = computeWorldScale(h)
                            val worldX = engine.cameraX + (tapOffset.x - w * 0.5f) / scale
                            val worldY = engine.cameraY + (tapOffset.y - h * 0.5f) / scale
                            viewModel.onTapWorld(worldX, worldY)
                        }
                    }
                }
        ) {
            @Suppress("UNUSED_EXPRESSION")
            frameTick
            renderSpaceWorld(engine = engine, popupPaint = popupPaint)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TopTacticalHud(
                hud = hud,
                onOpenHangar = { viewModel.openHangar() },
                onToggleConfig = { viewModel.toggleConfig() },
                onCycleFormation = { viewModel.cycleOwnedFormation() },
                onCancelAutopilot = { viewModel.cancelAutopilot() },
                onClaimMission = { viewModel.claimMissionReward() },
                onTogglePause = { viewModel.togglePause() },
                modifier = Modifier.align(Alignment.TopStart)
            )

            TacticalMinimap(
                engine = engine,
                frameTick = frameTick,
                onExpandMap = { viewModel.toggleTacticalMap() },
                modifier = Modifier.align(Alignment.TopEnd)
            )

            VirtualJoystick(
                label = stringResource(R.string.controls_hint_move),
                accentColor = CyanLaser,
                tag = "left_movement_joystick",
                onMove = { x, y -> viewModel.setMoveJoystick(x, y) },
                modifier = Modifier.align(Alignment.BottomStart)
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                RocketFireButton(
                    cooldownRemaining = hud.rocketCooldownRemaining,
                    inSafeZone = hud.inSafeZone,
                    onFireRocket = { viewModel.fireHomingRocket() },
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                AttackLaserButton(
                    isAutoFireEnabled = hud.isAutoFireEnabled,
                    inSafeZone = hud.inSafeZone,
                    hasTarget = hud.lockedTargetCallsign != null,
                    targetInRange = hud.lockedTargetInRange,
                    onToggle = { viewModel.toggleAutoFire() },
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                VirtualJoystick(
                    label = stringResource(R.string.controls_hint_aim),
                    accentColor = CrimsonDanger,
                    tag = "right_targeting_joystick",
                    onMove = { x, y -> viewModel.setAimJoystick(x, y) }
                )
            }
        }

        if (hud.isTacticalMapOpen && hud.isAlive && !hud.isHangarOpen && !hud.isPaused) {
            ExpandableTacticalMapOverlay(
                engine = engine,
                frameTick = frameTick,
                onSetAutopilot = { wx, wy -> viewModel.setAutopilotTarget(wx, wy) },
                onClose = { viewModel.closeTacticalMap() }
            )
        }

        if (hud.isHangarOpen && hud.isAlive) {
            HangarShopOverlay(
                hud = hud,
                onSwitchConfig = { viewModel.switchConfig(it) },
                onBuyOrSelectShipClass = { viewModel.buyOrSelectShipClass(it) },
                onBuyOrSelectFormation = { viewModel.buyOrSelectFormation(it) },
                onBuyEquipment = { viewModel.buyEquipment(it) },
                onBuyFlaxVant = { viewModel.buyFlaxVant() },
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
                onClose = { viewModel.closeHangar() }
            )
        }

        if (hud.isPaused && !hud.isHangarOpen && hud.isAlive) {
            TacticalModalOverlay(
                title = stringResource(R.string.paused_title),
                subtitle = "${hud.activeShipClass.displayName} • ${stringResource(R.string.hud_credits_label)}: ${"%,d".format(hud.credits)} • ${stringResource(R.string.hud_kills_label)}: ${hud.kills}",
                accentColor = CyanLaser,
                primaryLabel = stringResource(R.string.action_resume),
                primaryTag = "resume_button",
                onPrimaryClick = { viewModel.togglePause() },
                secondaryLabel = stringResource(R.string.action_reset_save),
                secondaryTag = "reset_save_button",
                onSecondaryClick = { viewModel.resetSave() }
            )
        }

        if (!hud.isAlive) {
            TacticalModalOverlay(
                title = stringResource(R.string.ship_destroyed_title),
                subtitle = "${stringResource(R.string.ship_destroyed_subtitle)}  •  ${"%,d".format(hud.credits)} CR",
                accentColor = CrimsonDanger,
                primaryLabel = stringResource(R.string.action_relaunch),
                primaryTag = "relaunch_button",
                onPrimaryClick = { viewModel.relaunchShip() }
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
