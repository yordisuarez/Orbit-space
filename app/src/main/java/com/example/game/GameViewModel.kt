package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val persistence = GamePersistence(application.applicationContext)

    val engine = SpaceGameEngine(
        onSaveRequested = { saveState -> persistence.save(saveState) }
    ).also { gameEngine ->
        persistence.load()?.let { saved ->
            gameEngine.restoreSaveState(saved)
        }
    }

    private val _hudState = MutableStateFlow(engine.buildHudSnapshot())
    val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

    private var hudAccumulator = 0f

    fun tick(dt: Float) {
        engine.step(dt)
        hudAccumulator += dt
        if (hudAccumulator >= 0.05f) {
            hudAccumulator = 0f
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun setMoveJoystick(x: Float, y: Float) {
        engine.setMoveJoystick(x, y)
    }

    fun setAimJoystick(x: Float, y: Float) {
        engine.setAimJoystick(x, y)
        _hudState.value = engine.buildHudSnapshot()
    }

    fun toggleAutoFire() {
        engine.toggleAutoFire()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun fireHomingRocket() {
        if (engine.fireHomingRocket()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun onTapWorld(worldX: Float, worldY: Float) {
        if (engine.handleWorldTap(worldX, worldY)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun togglePause() {
        engine.togglePause()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun openHangar() {
        engine.openHangar()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun closeHangar() {
        engine.closeHangar()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun switchConfig(configNumber: Int) {
        if (engine.switchConfig(configNumber)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun toggleConfig() {
        if (engine.toggleConfig()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun buyOrSelectFormation(formation: DroneFormation) {
        if (engine.buyOrSelectFormation(formation)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun cycleOwnedFormation() {
        if (engine.cycleOwnedFormation()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun toggleTacticalMap() {
        engine.toggleTacticalMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun closeTacticalMap() {
        engine.closeTacticalMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun setAutopilotTarget(worldX: Float, worldY: Float) {
        engine.setAutopilotTarget(worldX, worldY)
        _hudState.value = engine.buildHudSnapshot()
    }

    fun cancelAutopilot() {
        engine.cancelAutopilot()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun buyOrSelectShipClass(shipClass: ShipClass) {
        if (engine.buyOrSelectShipClass(shipClass)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun buyEquipment(itemId: EquipmentItemId) {
        if (engine.buyEquipment(itemId)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun buyFlaxVant() {
        if (engine.buyFlaxVant()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun fuseItems(sourceType: EquipmentItemId) {
        if (engine.fuseItems(sourceType)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun fuseFlaxVants() {
        if (engine.fuseFlaxVants()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun equipFromInventory(instanceId: Int) {
        if (engine.equipFromInventory(instanceId)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun equipItemToVantSlot(vantId: Int, slotIndex: Int, instanceId: Int? = null) {
        if (engine.equipItemToVantSlot(vantId, slotIndex, instanceId)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun equipFromInventoryToVant(instanceId: Int) {
        if (engine.equipFromInventoryToVant(instanceId)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun unequipVantSlot(vantId: Int, slotIndex: Int) {
        if (engine.unequipVantSlot(vantId, slotIndex)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun unequipLaserSlot(slotIndex: Int) {
        if (engine.unequipLaserSlot(slotIndex)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun unequipShieldSlot(slotIndex: Int) {
        if (engine.unequipShieldSlot(slotIndex)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun unequipSectorChip() {
        if (engine.unequipSectorChip()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun claimMissionReward() {
        if (engine.claimMissionReward()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun relaunchShip() {
        engine.relaunchShip()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun resetSave() {
        persistence.clear()
        engine.resetSector(keepProgress = false)
        _hudState.value = engine.buildHudSnapshot()
    }
}
