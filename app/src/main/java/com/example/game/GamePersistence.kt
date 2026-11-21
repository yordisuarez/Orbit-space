package com.example.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class GamePersistence(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(state: SaveState) {
        val root = JSONObject().apply {
            put("credits", state.credits)
            put("kills", state.kills)
            put("level", state.level)
            put("xp", state.xp)
            put("activeConfig", state.activeConfig)
            put("activeShipClass", state.activeShipClass.name)
            put("nextItemInstanceId", state.nextItemInstanceId)
            put("nextVantId", state.nextVantId)
            put("equippedSectorChipId", state.equippedSectorChipId ?: JSONObject.NULL)
            put("currentMissionIndex", state.currentMissionIndex)
            put("currentMissionProgress", state.currentMissionProgress)

            val shipsArray = JSONArray()
            for (ship in state.ownedShipClasses) {
                shipsArray.put(ship.name)
            }
            put("ownedShipClasses", shipsArray)

            val itemsArray = JSONArray()
            for (item in state.ownedItems) {
                itemsArray.put(
                    JSONObject().apply {
                        put("id", item.instanceId)
                        put("type", item.type.name)
                        put("quality", item.quality.name)
                    }
                )
            }
            put("ownedItems", itemsArray)
            put("config1Lasers", encodeSlotIds(state.config1LaserIds))
            put("config1Shields", encodeSlotIds(state.config1ShieldIds))
            put("config2Lasers", encodeSlotIds(state.config2LaserIds))
            put("config2Shields", encodeSlotIds(state.config2ShieldIds))

            val vantsArray = JSONArray()
            for (vant in state.ownedVants) {
                vantsArray.put(
                    JSONObject().apply {
                        put("id", vant.id)
                        put("tier", vant.tier.name)
                        put("c1Slots", encodeSlotIds(vant.config1SlotIds))
                        put("c2Slots", encodeSlotIds(vant.config2SlotIds))
                        put("durabilityPct", vant.durabilityPct)
                    }
                )
            }
            put("ownedVants", vantsArray)

            val formationsArray = JSONArray()
            for (formation in state.ownedFormations) {
                formationsArray.put(formation.name)
            }
            put("ownedFormations", formationsArray)
            put("activeFormation", state.activeFormation.name)
            put("pilotName", state.pilotName ?: JSONObject.NULL)
            put("companyId", state.companyId ?: JSONObject.NULL)
            put("currentSectorId", state.currentSectorId)
            put("rawCargo", encodeCargoMap(state.rawCargo))
            put("refinedCargo", encodeCargoMap(state.refinedCargo))
            put("overchargedAmmo", state.overchargedAmmo)
            put("activeShipQuality", state.activeShipQuality.name)
            put("pilotSkills", encodePilotSkills(state.pilotSkills))
            put("craftedHangarShips", encodeCraftedShips(state.craftedHangarShips))
            put("craftedInventoryItems", encodeCraftedItems(state.craftedInventoryItems))
            put("isFlyingCapsule", state.isFlyingCapsule)
            put("activeCraftedShipId", state.activeCraftedShipId ?: JSONObject.NULL)
            put("basicVendorItems", encodeBasicVendorItems(state.basicVendorItems))
        }
        prefs.edit()
            .putString(KEY_SAVE_JSON, root.toString())
            .apply {
                if (state.pilotName != null && state.companyId != null) {
                    putString(KEY_PILOT_NAME, state.pilotName)
                    putString(KEY_COMPANY_ID, state.companyId)
                } else {
                    remove(KEY_PILOT_NAME)
                    remove(KEY_COMPANY_ID)
                }
            }
            .apply()
    }

    fun load(): SaveState? {
        val raw = prefs.getString(KEY_SAVE_JSON, null) ?: return null
        return try {
            val root = JSONObject(raw)
            val ownedItems = mutableListOf<EquipmentItem>()
            val itemsArray = root.optJSONArray("ownedItems") ?: JSONArray()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(i) ?: continue
                val id = obj.optInt("id", -1)
                val typeName = obj.optString("type", "")
                val type = EquipmentItemId.entries.firstOrNull { it.name == typeName } ?: continue
                val quality = CraftQuality.entries.firstOrNull {
                    it.name == obj.optString("quality", CraftQuality.STANDARD.name)
                } ?: CraftQuality.STANDARD
                if (id > 0) {
                    ownedItems.add(EquipmentItem(instanceId = id, type = type, quality = quality))
                }
            }
            val ownedShips = mutableSetOf<ShipClass>()
            val shipsArray = root.optJSONArray("ownedShipClasses")
            if (shipsArray != null) {
                for (i in 0 until shipsArray.length()) {
                    val shipName = shipsArray.optString(i, "")
                    ShipClass.entries.firstOrNull { it.name == shipName }?.let { ownedShips.add(it) }
                }
            } else {
                // Legacy save fallback only when field did not exist
                ownedShips.add(ShipClass.INTERCEPTOR)
            }

            val parsedActiveShip = ShipClass.entries.firstOrNull {
                it.name == root.optString("activeShipClass", ShipClass.INTERCEPTOR.name)
            }?.takeIf { it in ownedShips } ?: ownedShips.firstOrNull() ?: ShipClass.INTERCEPTOR

            val chipId = if (root.isNull("equippedSectorChipId")) {
                null
            } else {
                root.optInt("equippedSectorChipId", -1).takeIf { it > 0 }
            }

            val parsedVants = mutableListOf<SavedVant>()
            val vantsArray = root.optJSONArray("ownedVants")
            if (vantsArray != null) {
                for (i in 0 until vantsArray.length()) {
                    if (parsedVants.size >= GameConfig.MAX_OWNED_VANTS_FOR_FUSION) break
                    val vObj = vantsArray.optJSONObject(i) ?: continue
                    val vId = vObj.optInt("id", i + 1).coerceAtLeast(1)
                    val tierName = vObj.optString("tier", "")
                    val tier = VantTier.entries.firstOrNull { it.name == tierName } ?: continue
                    val dur = vObj.optInt("durabilityPct", 100).coerceIn(0, 100)
                    parsedVants.add(
                        SavedVant(
                            id = vId,
                            tier = tier,
                            config1SlotIds = decodeSlotIds(vObj.optJSONArray("c1Slots"), tier.slotCount),
                            config2SlotIds = decodeSlotIds(vObj.optJSONArray("c2Slots"), tier.slotCount),
                            durabilityPct = dur
                        )
                    )
                }
            }

            val parsedFormations = mutableSetOf(DroneFormation.NORMAL)
            val formationsArray = root.optJSONArray("ownedFormations")
            if (formationsArray != null) {
                for (i in 0 until formationsArray.length()) {
                    val fName = formationsArray.optString(i, "")
                    DroneFormation.entries.firstOrNull { it.name == fName }?.let { parsedFormations.add(it) }
                }
            }
            val parsedActiveFormation = DroneFormation.entries.firstOrNull {
                it.name == root.optString("activeFormation", DroneFormation.NORMAL.name)
            }?.takeIf { it in parsedFormations } ?: DroneFormation.NORMAL

            val laserCount = parsedActiveShip.laserSlotCount
            val shieldCount = parsedActiveShip.shieldSlotCount

            val savedPilotName = if (root.isNull("pilotName")) {
                prefs.getString(KEY_PILOT_NAME, null)
            } else {
                root.optString("pilotName", "").ifBlank { prefs.getString(KEY_PILOT_NAME, null) }
            }
            val savedCompanyId = if (root.isNull("companyId")) {
                prefs.getString(KEY_COMPANY_ID, null)
            } else {
                root.optString("companyId", "").ifBlank { prefs.getString(KEY_COMPANY_ID, null) }
            }

            SaveState(
                credits = root.optInt("credits", 0).coerceAtLeast(0),
                kills = root.optInt("kills", 0).coerceAtLeast(0),
                level = root.optInt("level", 1).coerceAtLeast(1),
                xp = root.optInt("xp", 0).coerceAtLeast(0),
                activeConfig = if (root.optInt("activeConfig", 1) == 2) 2 else 1,
                activeShipClass = parsedActiveShip,
                ownedShipClasses = ownedShips,
                nextItemInstanceId = root.optInt("nextItemInstanceId", 10).coerceAtLeast(
                    (ownedItems.maxOfOrNull { it.instanceId } ?: 0) + 1
                ),
                nextVantId = root.optInt("nextVantId", 1).coerceAtLeast(
                    (parsedVants.maxOfOrNull { it.id } ?: 0) + 1
                ),
                ownedItems = ownedItems,
                config1LaserIds = decodeSlotIds(root.optJSONArray("config1Lasers"), laserCount),
                config1ShieldIds = decodeSlotIds(root.optJSONArray("config1Shields"), shieldCount),
                config2LaserIds = decodeSlotIds(root.optJSONArray("config2Lasers"), laserCount),
                config2ShieldIds = decodeSlotIds(root.optJSONArray("config2Shields"), shieldCount),
                ownedVants = parsedVants,
                ownedFormations = parsedFormations,
                activeFormation = parsedActiveFormation,
                equippedSectorChipId = chipId,
                currentMissionIndex = root.optInt("currentMissionIndex", 0).coerceIn(0, MissionCatalog.all.size),
                currentMissionProgress = root.optInt("currentMissionProgress", 0).coerceAtLeast(0),
                pilotName = savedPilotName?.trim()?.takeIf { it.length in 3..14 },
                companyId = PilotCompany.fromId(savedCompanyId)?.id,
                currentSectorId = root.optString("currentSectorId", "").takeIf {
                    it in GalaxyMapGraph.sectors
                } ?: GalaxyMapGraph.homeCityForCompany(PilotCompany.fromId(savedCompanyId)),
                rawCargo = decodeCargoMap(root.optJSONObject("rawCargo")),
                refinedCargo = decodeCargoMap(root.optJSONObject("refinedCargo")),
                overchargedAmmo = root.optInt("overchargedAmmo", 0).coerceAtLeast(0),
                activeShipQuality = CraftQuality.entries.firstOrNull {
                    it.name == root.optString("activeShipQuality", CraftQuality.STANDARD.name)
                } ?: CraftQuality.STANDARD,
                pilotSkills = decodePilotSkills(root.optJSONObject("pilotSkills")),
                craftedHangarShips = decodeCraftedShips(root.optJSONArray("craftedHangarShips")),
                craftedInventoryItems = decodeCraftedItems(root.optJSONArray("craftedInventoryItems")),
                isFlyingCapsule = if (root.has("isFlyingCapsule")) {
                    root.optBoolean("isFlyingCapsule", true)
                } else {
                    ownedShips.isEmpty()
                },
                activeCraftedShipId = if (!root.has("activeCraftedShipId") || root.isNull("activeCraftedShipId")) {
                    null
                } else {
                    root.optInt("activeCraftedShipId", -1).takeIf { it > 0 }
                },
                basicVendorItems = decodeBasicVendorItems(root.optJSONObject("basicVendorItems"))
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_SAVE_JSON)
            .remove(KEY_PILOT_NAME)
            .remove(KEY_COMPANY_ID)
            .apply()
    }

    private fun encodeSlotIds(ids: List<Int?>): JSONArray {
        val arr = JSONArray()
        for (id in ids) {
            arr.put(id ?: JSONObject.NULL)
        }
        return arr
    }

    private fun decodeSlotIds(arr: JSONArray?, size: Int): List<Int?> {
        return List(size) { idx ->
            if (arr == null || idx >= arr.length() || arr.isNull(idx)) {
                null
            } else {
                arr.optInt(idx, -1).takeIf { it > 0 }
            }
        }
    }

    private fun encodeCargoMap(cargo: Map<ResourceType, Int>): JSONObject {
        val obj = JSONObject()
        for ((res, qty) in cargo) {
            if (qty > 0) obj.put(res.name, qty)
        }
        return obj
    }

    private fun decodeCargoMap(obj: JSONObject?): Map<ResourceType, Int> {
        if (obj == null) return emptyMap()
        val result = linkedMapOf<ResourceType, Int>()
        for (res in ResourceType.entries) {
            val qty = obj.optInt(res.name, 0)
            if (qty > 0) result[res] = qty
        }
        return result
    }

    private fun encodePilotSkills(skills: Map<SkillType, SkillProgress>): JSONObject {
        val obj = JSONObject()
        for (skill in SkillType.entries) {
            val progress = skills[skill] ?: SkillProgress()
            obj.put(
                skill.name,
                JSONObject().apply {
                    put("level", progress.level)
                    put("xp", progress.xp)
                }
            )
        }
        return obj
    }

    private fun decodePilotSkills(obj: JSONObject?): Map<SkillType, SkillProgress> {
        val defaults = defaultPilotSkills().toMutableMap()
        if (obj == null) return defaults
        for (skill in SkillType.entries) {
            val sObj = obj.optJSONObject(skill.name) ?: continue
            val level = sObj.optInt("level", PILOT_SKILL_MIN_LEVEL)
                .coerceIn(PILOT_SKILL_MIN_LEVEL, PILOT_SKILL_MAX_LEVEL)
            val xp = sObj.optLong("xp", 0L).coerceAtLeast(0L)
            defaults[skill] = SkillProgress(level = level, xp = xp)
        }
        return defaults
    }

    private fun encodeCraftedShips(ships: List<CraftedShipRecord>): JSONArray {
        val arr = JSONArray()
        for (ship in ships) {
            arr.put(
                JSONObject().apply {
                    put("id", ship.instanceId)
                    put("recipeId", ship.recipeId.name)
                    put("hullId", ship.hullId)
                    put("industrialType", ship.industrialType.name)
                    put("quality", ship.quality.name)
                    put("hp", ship.effectiveHull)
                    put("shd", ship.effectiveShield)
                    put("cargo", ship.effectiveCargo)
                }
            )
        }
        return arr
    }

    private fun decodeCraftedShips(arr: JSONArray?): List<CraftedShipRecord> {
        if (arr == null) return emptyList()
        val out = mutableListOf<CraftedShipRecord>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val recipe = StationCraftRecipeId.entries.firstOrNull {
                it.name == obj.optString("recipeId", "")
            } ?: continue
            val indType = IndustrialType.entries.firstOrNull {
                it.name == obj.optString("industrialType", "")
            } ?: recipe.industrialType ?: continue
            val quality = CraftQuality.entries.firstOrNull {
                it.name == obj.optString("quality", CraftQuality.STANDARD.name)
            } ?: CraftQuality.STANDARD
            out.add(
                CraftedShipRecord(
                    instanceId = obj.optInt("id", i + 1).coerceAtLeast(1),
                    recipeId = recipe,
                    hullId = obj.optString("hullId", recipe.shipHullId ?: recipe.name),
                    industrialType = indType,
                    quality = quality,
                    effectiveHull = obj.optInt("hp", 220).coerceAtLeast(1),
                    effectiveShield = obj.optInt("shd", 140).coerceAtLeast(1),
                    effectiveCargo = obj.optInt("cargo", 240).coerceAtLeast(1)
                )
            )
        }
        return out
    }

    private fun encodeCraftedItems(items: List<CraftedItemRecord>): JSONArray {
        val arr = JSONArray()
        for (item in items) {
            arr.put(
                JSONObject().apply {
                    put("id", item.instanceId)
                    put("recipeId", item.recipeId.name)
                    put("quality", item.quality.name)
                    put("stat", item.effectivePrimaryStat.toDouble())
                    put("dur", item.effectiveDurationMinutes)
                    put("qty", item.quantity)
                }
            )
        }
        return arr
    }

    private fun decodeCraftedItems(arr: JSONArray?): List<CraftedItemRecord> {
        if (arr == null) return emptyList()
        val out = mutableListOf<CraftedItemRecord>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val recipe = StationCraftRecipeId.entries.firstOrNull {
                it.name == obj.optString("recipeId", "")
            } ?: continue
            val quality = CraftQuality.entries.firstOrNull {
                it.name == obj.optString("quality", CraftQuality.STANDARD.name)
            } ?: CraftQuality.STANDARD
            val qty = obj.optInt("qty", 1).coerceAtLeast(1)
            out.add(
                CraftedItemRecord(
                    instanceId = obj.optInt("id", i + 1).coerceAtLeast(1),
                    recipeId = recipe,
                    quality = quality,
                    effectivePrimaryStat = obj.optDouble("stat", recipe.basePrimaryStat.toDouble()).toFloat(),
                    effectiveDurationMinutes = obj.optInt("dur", recipe.baseDurationMinutes).coerceAtLeast(0),
                    quantity = qty
                )
            )
        }
        return out
    }

    private fun encodeBasicVendorItems(items: Map<BasicVendorItemId, Int>): JSONObject {
        val obj = JSONObject()
        for ((item, qty) in items) {
            if (qty > 0) obj.put(item.name, qty)
        }
        return obj
    }

    private fun decodeBasicVendorItems(obj: JSONObject?): Map<BasicVendorItemId, Int> {
        if (obj == null) return emptyMap()
        val result = linkedMapOf<BasicVendorItemId, Int>()
        for (item in BasicVendorItemId.entries) {
            val qty = obj.optInt(item.name, 0)
            if (qty > 0) result[item] = qty
        }
        return result
    }

    fun saveLanguage(language: GameLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun loadLanguage(): GameLanguage =
        GameLanguage.fromCode(prefs.getString(KEY_LANGUAGE, GameLanguage.EN.code))

    companion object {
        private const val PREFS_NAME = "dark_orbit_mobile_save"
        private const val KEY_SAVE_JSON = "save_state_v2"
        private const val KEY_PILOT_NAME = "pilotName"
        private const val KEY_COMPANY_ID = "companyId"
        private const val KEY_LANGUAGE = "app_language"
    }
}
