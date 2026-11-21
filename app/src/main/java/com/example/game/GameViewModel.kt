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

    private val _screenStage = MutableStateFlow(AppScreenStage.INTRO)
    val screenStage: StateFlow<AppScreenStage> = _screenStage.asStateFlow()

    private val _selectedOnboardingCompany = MutableStateFlow<PilotCompany?>(PilotCompany.AETHER)
    val selectedOnboardingCompany: StateFlow<PilotCompany?> = _selectedOnboardingCompany.asStateFlow()

    private val _pilotNameDraft = MutableStateFlow("")
    val pilotNameDraft: StateFlow<String> = _pilotNameDraft.asStateFlow()

    private val _language = MutableStateFlow(persistence.loadLanguage()).also {
        engine.language = it.value
    }
    val language: StateFlow<GameLanguage> = _language.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private var hudAccumulator = 0f

    fun setLanguage(lang: GameLanguage) {
        if (_language.value == lang) return
        _language.value = lang
        engine.language = lang
        persistence.saveLanguage(lang)
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun continueAsSavedPilot() {
        if (engine.hasRegisteredPilot) {
            _screenStage.value = AppScreenStage.GAMEPLAY
            _hudState.value = engine.buildHudSnapshot()
        } else {
            beginOnboarding()
        }
    }

    fun beginOnboarding() {
        _selectedOnboardingCompany.value = PilotCompany.AETHER
        _pilotNameDraft.value = ""
        _screenStage.value = AppScreenStage.ONBOARDING_COMPANY
    }

    fun goBackToIntro() {
        _screenStage.value = AppScreenStage.INTRO
    }

    fun selectOnboardingCompany(company: PilotCompany) {
        _selectedOnboardingCompany.value = company
    }

    fun proceedToPilotNameStep() {
        if (_selectedOnboardingCompany.value != null) {
            _screenStage.value = AppScreenStage.ONBOARDING_NAME
        }
    }

    fun goBackToCompanySelection() {
        _screenStage.value = AppScreenStage.ONBOARDING_COMPANY
    }

    fun updatePilotNameDraft(name: String) {
        _pilotNameDraft.value = name.take(14)
    }

    fun completePilotRegistration() {
        val company = _selectedOnboardingCompany.value ?: return
        val trimmed = _pilotNameDraft.value.trim()
        if (trimmed.length !in 3..14) return
        engine.setPilotProfile(trimmed, company)
        _hudState.value = engine.buildHudSnapshot()
        _screenStage.value = AppScreenStage.GAMEPLAY
    }

    fun tick(dt: Float) {
        if (_screenStage.value != AppScreenStage.GAMEPLAY) return
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

    fun openNearbyStation() {
        if (engine.openNearbyStation()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun closeStation() {
        engine.closeStation()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun refineResource(type: ResourceType, refineAll: Boolean) {
        if (engine.refineResource(type, refineAll)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun buyMarketResource(type: ResourceType, isRefined: Boolean) {
        if (engine.buyMarketResource(type, isRefined)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun sellMarketResource(type: ResourceType, isRefined: Boolean, sellAll: Boolean) {
        if (engine.sellMarketResource(type, isRefined, sellAll)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun buyMarketEquipment(itemId: EquipmentItemId) {
        if (engine.buyMarketEquipment(itemId)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun craftRecipe(recipe: CraftingRecipeId) {
        if (engine.craftRecipe(recipe)) {
            _hudState.value = engine.buildHudSnapshot()
        }
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

    fun selectOwnedFormationFromHud(formation: DroneFormation) {
        if (engine.selectOwnedFormationFromHud(formation)) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun cycleOwnedFormation() {
        if (engine.cycleOwnedFormation()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun openTacticalMap() {
        engine.openTacticalMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun toggleTacticalMap() {
        engine.toggleTacticalMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun closeTacticalMap() {
        engine.closeTacticalMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun openGalaxyMap() {
        engine.openGalaxyMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun closeGalaxyMap() {
        engine.closeGalaxyMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun toggleGalaxyMap() {
        engine.toggleGalaxyMap()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun setPlottedRoute(route: List<String>) {
        engine.setPlottedRoute(route)
        _hudState.value = engine.buildHudSnapshot()
    }

    fun clearPlottedRoute() {
        engine.clearPlottedRoute()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun engageAutopilotToNextRoutePortal() {
        if (engine.engageAutopilotToNextRoutePortal()) {
            _hudState.value = engine.buildHudSnapshot()
        }
    }

    fun setAutopilotTarget(worldX: Float, worldY: Float) {
        engine.setAutopilotTarget(worldX, worldY)
        _hudState.value = engine.buildHudSnapshot()
    }

    fun cancelAutopilot() {
        engine.cancelAutopilot()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun triggerJumpGate() {
        if (engine.triggerJumpGate()) {
            _hudState.value = engine.buildHudSnapshot()
        }
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
        engine.resetSector(keepProgress = false, triggerSave = false)
        _selectedOnboardingCompany.value = PilotCompany.AETHER
        _pilotNameDraft.value = ""
        _hudState.value = engine.buildHudSnapshot()
        _screenStage.value = AppScreenStage.INTRO
    }

    fun debugAddOneMillionCredits() {
        engine.debugAddOneMillionCredits()
        _hudState.value = engine.buildHudSnapshot()
    }

    fun debugAddOneLevel() {
        engine.debugAddOneLevel()
        _hudState.value = engine.buildHudSnapshot()
    }
}
