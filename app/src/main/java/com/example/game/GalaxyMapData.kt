package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Risk & gameplay classification for every sector in the 25-sector Orbi Space galaxy.
 */
enum class GalaxyZoneType(
    val shortLabel: String,
    val riskDescription: String,
    val badgeColorHex: Long,
    val badgeBgColorHex: Long,
    val spaceBackgroundColorHex: Long
) {
    CAPITAL_CITY(
        shortLabel = "CAPITAL SAFE ZONE",
        riskDescription = "Safe Zone — Base, Market & Vault (No Combat)",
        badgeColorHex = 0xFF00E5FF,
        badgeBgColorHex = 0xCC062838,
        spaceBackgroundColorHex = 0xFF050A18
    ),
    SAFE_ZONE(
        shortLabel = "SAFE ZONE",
        riskDescription = "Safe Zone — Protected Patrol Space (No Full Loss)",
        badgeColorHex = 0xFF38BDF8,
        badgeBgColorHex = 0xCC0A2239,
        spaceBackgroundColorHex = 0xFF060B1A
    ),
    INTERMEDIATE_ZONE(
        shortLabel = "INTERMEDIATE",
        riskDescription = "Intermediate Zone — No Full Ship Loss",
        badgeColorHex = 0xFF4ADE80,
        badgeBgColorHex = 0xCC072415,
        spaceBackgroundColorHex = 0xFF07071C
    ),
    MODERATE_ZONE(
        shortLabel = "MODERATE RISK",
        riskDescription = "Moderate Zone — Contested Inner Ring",
        badgeColorHex = 0xFFFB923C,
        badgeBgColorHex = 0xCC2C1506,
        spaceBackgroundColorHex = 0xFF0D081C
    ),
    HIGH_RISK_YELLOW_RING(
        shortLabel = "HIGH-RISK • YELLOW RING",
        riskDescription = "High-Risk (Full-Loot • Yellow Crystal)",
        badgeColorHex = 0xFFFACC15,
        badgeBgColorHex = 0xCC2E2305,
        spaceBackgroundColorHex = 0xFF130E05
    ),
    NUCLEUS(
        shortLabel = "NUCLEUS • FULL-LOOT",
        riskDescription = "High-Risk (Full-Loot • Central Black Market Safe Zone)",
        badgeColorHex = 0xFFFBBF24,
        badgeBgColorHex = 0xCC301D04,
        spaceBackgroundColorHex = 0xFF140D04
    );

    val badgeColor: Color get() = Color(badgeColorHex)
    val badgeBgColor: Color get() = Color(badgeBgColorHex)
    val spaceBackgroundColor: Color get() = Color(spaceBackgroundColorHex)
}

/**
 * Immutable data definition for a single sector in the 25-sector Galactic Map.
 */
data class SectorNode(
    val id: String,
    val name: String,
    val position: Offset,
    val zoneType: GalaxyZoneType,
    val resourceTiers: String,
    val minTier: Int,
    val maxTier: Int,
    val connectedSectorIds: List<String>,
    val shortcutSectorIds: Set<String> = emptySet(),
    val hasCentralSafeZone: Boolean = false,
    val isEntireSectorSafe: Boolean = false,
    val isLargeSector: Boolean = false,
    val mapSize: Float = 14000f,
    val safeZoneRadius: Float = 0f,
    val nodeColorHex: Long,
    val factionTag: String? = null
) {
    val nodeColor: Color get() = Color(nodeColorHex)
    val isCapitalCity: Boolean get() = zoneType == GalaxyZoneType.CAPITAL_CITY
    val isNucleus: Boolean get() = zoneType == GalaxyZoneType.NUCLEUS
    val isShortcutHub: Boolean get() = shortcutSectorIds.isNotEmpty() && id.startsWith("R")

    fun isShortcutTo(neighborId: String): Boolean = neighborId in shortcutSectorIds

    val safeZoneStatusLabel: String
        get() = when {
            isCapitalCity -> "Full Safe Zone (Base • Market • Vault)"
            isNucleus && hasCentralSafeZone -> "Central Safe Zone (Black Market — No Combat)"
            hasCentralSafeZone -> "Central Safe Zone Outpost"
            else -> "No Safe Zone (Active Combat Space)"
        }
}

data class GalaxyPortalLink(
    val fromId: String,
    val toId: String,
    val isShortcut: Boolean,
    val isInnerRing: Boolean,
    val isOuterRing: Boolean
)

data class SectorPortal(
    val targetSectorId: String,
    val worldX: Float,
    val worldY: Float,
    val isShortcut: Boolean,
    val accentColorHex: Long
) {
    val accentColor: Color get() = Color(accentColorHex)
    val gateLabel: String
        get() = if (isShortcut) "SHORTCUT: $targetSectorId" else "GATE: $targetSectorId"
}

private fun polarCoord(angleDeg: Float, radius: Float): Offset {
    val rad = angleDeg * (PI / 180.0)
    return Offset(
        x = (0.50 + radius * cos(rad)).toFloat(),
        y = (0.51 + radius * sin(rad)).toFloat()
    )
}

/**
 * Complete 25-sector Galactic Map dataset and graph utilities (BFS routing + compass portals).
 */
object GalaxyMapGraph {
    const val DEFAULT_SECTOR_ID = "C1"

    val sectors: Map<String, SectorNode> = linkedMapOf(
        // =========================================================================
        // 3 CAPITAL CITIES (Safe Zone — Base, Market, Vault)
        // =========================================================================
        "C1" to SectorNode(
            id = "C1",
            name = "City 1 — Aether Prime",
            position = polarCoord(-90f, 0.385f),
            zoneType = GalaxyZoneType.CAPITAL_CITY,
            resourceTiers = "Base • Market • Vault",
            minTier = 1,
            maxTier = 1,
            connectedSectorIds = listOf("R1-1", "R3-4", "1-1"),
            hasCentralSafeZone = true,
            isEntireSectorSafe = true,
            mapSize = 10000f,
            safeZoneRadius = 1100f,
            nodeColorHex = 0xFF38BDF8,
            factionTag = "AET"
        ),
        "C2" to SectorNode(
            id = "C2",
            name = "City 2 — Verdant Haven",
            position = polarCoord(30f, 0.385f),
            zoneType = GalaxyZoneType.CAPITAL_CITY,
            resourceTiers = "Base • Market • Vault",
            minTier = 1,
            maxTier = 1,
            connectedSectorIds = listOf("R2-1", "R1-4", "2-1"),
            hasCentralSafeZone = true,
            isEntireSectorSafe = true,
            mapSize = 10000f,
            safeZoneRadius = 1100f,
            nodeColorHex = 0xFF22C55E,
            factionTag = "VER"
        ),
        "C3" to SectorNode(
            id = "C3",
            name = "City 3 — Solaris Forge",
            position = polarCoord(150f, 0.385f),
            zoneType = GalaxyZoneType.CAPITAL_CITY,
            resourceTiers = "Base • Market • Vault",
            minTier = 1,
            maxTier = 1,
            connectedSectorIds = listOf("R3-1", "R2-4", "3-1"),
            hasCentralSafeZone = true,
            isEntireSectorSafe = true,
            mapSize = 10000f,
            safeZoneRadius = 1100f,
            nodeColorHex = 0xFFFF3B5C,
            factionTag = "SOL"
        ),

        // =========================================================================
        // OUTER RING (12 Sectors — Intermediate Zone, Tier 1–2, No Full Ship Loss)
        // =========================================================================
        "R1-1" to SectorNode(
            id = "R1-1",
            name = "Outer Rim 1-1",
            position = polarCoord(-66f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("C1", "R1-2"),
            nodeColorHex = 0xFF38BDF8
        ),
        "R1-2" to SectorNode(
            id = "R1-2",
            name = "Outer Rim 1-2",
            position = polarCoord(-42f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R1-1", "R1-3"),
            nodeColorHex = 0xFF38BDF8
        ),
        "R1-3" to SectorNode(
            id = "R1-3",
            name = "Outer Relay 1-3",
            position = polarCoord(-18f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2 • Border Shortcut",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R1-2", "R1-4", "2-2"),
            shortcutSectorIds = setOf("2-2"),
            nodeColorHex = 0xFF34D399
        ),
        "R1-4" to SectorNode(
            id = "R1-4",
            name = "Outer Rim 1-4",
            position = polarCoord(6f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R1-3", "C2"),
            nodeColorHex = 0xFF38BDF8
        ),

        "R2-1" to SectorNode(
            id = "R2-1",
            name = "Outer Rim 2-1",
            position = polarCoord(54f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("C2", "R2-2"),
            nodeColorHex = 0xFF38BDF8
        ),
        "R2-2" to SectorNode(
            id = "R2-2",
            name = "Outer Rim 2-2",
            position = polarCoord(78f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R2-1", "R2-3"),
            nodeColorHex = 0xFF38BDF8
        ),
        "R2-3" to SectorNode(
            id = "R2-3",
            name = "Outer Relay 2-3",
            position = polarCoord(102f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2 • Border Shortcut",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R2-2", "R2-4", "3-2"),
            shortcutSectorIds = setOf("3-2"),
            nodeColorHex = 0xFF34D399
        ),
        "R2-4" to SectorNode(
            id = "R2-4",
            name = "Outer Rim 2-4",
            position = polarCoord(126f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R2-3", "C3"),
            nodeColorHex = 0xFF38BDF8
        ),

        "R3-1" to SectorNode(
            id = "R3-1",
            name = "Outer Rim 3-1",
            position = polarCoord(174f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("C3", "R3-2"),
            nodeColorHex = 0xFF38BDF8
        ),
        "R3-2" to SectorNode(
            id = "R3-2",
            name = "Outer Rim 3-2",
            position = polarCoord(198f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R3-1", "R3-3"),
            nodeColorHex = 0xFF38BDF8
        ),
        "R3-3" to SectorNode(
            id = "R3-3",
            name = "Outer Relay 3-3",
            position = polarCoord(222f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2 • Border Shortcut",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R3-2", "R3-4", "1-2"),
            shortcutSectorIds = setOf("1-2"),
            nodeColorHex = 0xFF34D399
        ),
        "R3-4" to SectorNode(
            id = "R3-4",
            name = "Outer Rim 3-4",
            position = polarCoord(246f, 0.385f),
            zoneType = GalaxyZoneType.INTERMEDIATE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("R3-3", "C1"),
            nodeColorHex = 0xFF38BDF8
        ),

        // =========================================================================
        // INNER SPOKES (9 Sectors — 3 per Capital City)
        // =========================================================================
        "1-1" to SectorNode(
            id = "1-1",
            name = "Sector 1-1 — Aether Approach",
            position = polarCoord(-90f, 0.305f),
            zoneType = GalaxyZoneType.SAFE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("C1", "1-2"),
            hasCentralSafeZone = true,
            safeZoneRadius = 540f,
            nodeColorHex = 0xFF38BDF8
        ),
        "2-1" to SectorNode(
            id = "2-1",
            name = "Sector 2-1 — Verdant Approach",
            position = polarCoord(30f, 0.305f),
            zoneType = GalaxyZoneType.SAFE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("C2", "2-2"),
            hasCentralSafeZone = true,
            safeZoneRadius = 540f,
            nodeColorHex = 0xFF22C55E
        ),
        "3-1" to SectorNode(
            id = "3-1",
            name = "Sector 3-1 — Solaris Approach",
            position = polarCoord(150f, 0.305f),
            zoneType = GalaxyZoneType.SAFE_ZONE,
            resourceTiers = "Tier 1–2",
            minTier = 1,
            maxTier = 2,
            connectedSectorIds = listOf("C3", "3-2"),
            hasCentralSafeZone = true,
            safeZoneRadius = 540f,
            nodeColorHex = 0xFFFF3B5C
        ),

        "1-2" to SectorNode(
            id = "1-2",
            name = "Sector 1-2 — North Ring Hub",
            position = polarCoord(-90f, 0.228f),
            zoneType = GalaxyZoneType.MODERATE_ZONE,
            resourceTiers = "Tier 3–4",
            minTier = 3,
            maxTier = 4,
            connectedSectorIds = listOf("1-1", "1-3", "2-2", "3-2", "R3-3"),
            shortcutSectorIds = setOf("R3-3"),
            nodeColorHex = 0xFF00E5FF
        ),
        "2-2" to SectorNode(
            id = "2-2",
            name = "Sector 2-2 — East Ring Hub",
            position = polarCoord(30f, 0.228f),
            zoneType = GalaxyZoneType.MODERATE_ZONE,
            resourceTiers = "Tier 3–4",
            minTier = 3,
            maxTier = 4,
            connectedSectorIds = listOf("2-1", "2-3", "3-2", "1-2", "R1-3"),
            shortcutSectorIds = setOf("R1-3"),
            nodeColorHex = 0xFF2DD4BF
        ),
        "3-2" to SectorNode(
            id = "3-2",
            name = "Sector 3-2 — West Ring Hub",
            position = polarCoord(150f, 0.228f),
            zoneType = GalaxyZoneType.MODERATE_ZONE,
            resourceTiers = "Tier 3–4",
            minTier = 3,
            maxTier = 4,
            connectedSectorIds = listOf("3-1", "3-3", "1-2", "2-2", "R2-3"),
            shortcutSectorIds = setOf("R2-3"),
            nodeColorHex = 0xFFFB7185
        ),

        "1-3" to SectorNode(
            id = "1-3",
            name = "Sector 1-3 — North Yellow Ring",
            position = polarCoord(-90f, 0.148f),
            zoneType = GalaxyZoneType.HIGH_RISK_YELLOW_RING,
            resourceTiers = "Tier 4–5 • Yellow Crystal",
            minTier = 4,
            maxTier = 5,
            connectedSectorIds = listOf("1-2", "0-0"),
            mapSize = 16000f,
            nodeColorHex = 0xFFFACC15
        ),
        "2-3" to SectorNode(
            id = "2-3",
            name = "Sector 2-3 — East Yellow Ring",
            position = polarCoord(30f, 0.148f),
            zoneType = GalaxyZoneType.HIGH_RISK_YELLOW_RING,
            resourceTiers = "Tier 4–5 • Yellow Crystal",
            minTier = 4,
            maxTier = 5,
            connectedSectorIds = listOf("2-2", "0-0"),
            mapSize = 16000f,
            nodeColorHex = 0xFFFACC15
        ),
        "3-3" to SectorNode(
            id = "3-3",
            name = "Sector 3-3 — West Yellow Ring",
            position = polarCoord(150f, 0.148f),
            zoneType = GalaxyZoneType.HIGH_RISK_YELLOW_RING,
            resourceTiers = "Tier 4–5 • Yellow Crystal",
            minTier = 4,
            maxTier = 5,
            connectedSectorIds = listOf("3-2", "0-0"),
            mapSize = 16000f,
            nodeColorHex = 0xFFFACC15
        ),

        // =========================================================================
        // THE NUCLEUS (0-0 — Large High-Risk Sector with Central Black Market Safe Zone)
        // =========================================================================
        "0-0" to SectorNode(
            id = "0-0",
            name = "Nucleus (Black Market)",
            position = Offset(0.50f, 0.51f),
            zoneType = GalaxyZoneType.NUCLEUS,
            resourceTiers = "Tier 5 • Yellow Crystal • Black Market",
            minTier = 5,
            maxTier = 5,
            connectedSectorIds = listOf("1-3", "2-3", "3-3"),
            hasCentralSafeZone = true,
            isLargeSector = true,
            mapSize = 22000f,
            safeZoneRadius = 950f,
            nodeColorHex = 0xFFFBBF24
        )
    )

    val allLinks: List<GalaxyPortalLink> = buildUniqueLinks()

    fun sectorOf(sectorId: String): SectorNode =
        sectors[sectorId] ?: sectors.getValue(DEFAULT_SECTOR_ID)

    fun homeCityForCompany(company: PilotCompany?): String = when (company) {
        PilotCompany.AETHER -> "C1"
        PilotCompany.VERDANT -> "C2"
        PilotCompany.SOLARIS -> "C3"
        null -> DEFAULT_SECTOR_ID
    }

    private fun buildUniqueLinks(): List<GalaxyPortalLink> {
        val innerRingIds = setOf("1-2", "2-2", "3-2")
        val outerRingIds = setOf(
            "C1", "C2", "C3",
            "R1-1", "R1-2", "R1-3", "R1-4",
            "R2-1", "R2-2", "R2-3", "R2-4",
            "R3-1", "R3-2", "R3-3", "R3-4"
        )
        val seen = HashSet<String>()
        val result = ArrayList<GalaxyPortalLink>()
        for (node in sectors.values) {
            for (neighborId in node.connectedSectorIds) {
                if (neighborId !in sectors) continue
                val key = if (node.id < neighborId) "${node.id}|$neighborId" else "$neighborId|${node.id}"
                if (!seen.add(key)) continue
                val isShortcut = node.isShortcutTo(neighborId) ||
                    (sectors[neighborId]?.isShortcutTo(node.id) == true)
                val isInnerRing = !isShortcut && node.id in innerRingIds && neighborId in innerRingIds
                val isOuterRing = !isShortcut && node.id in outerRingIds && neighborId in outerRingIds
                result.add(
                    GalaxyPortalLink(
                        fromId = node.id,
                        toId = neighborId,
                        isShortcut = isShortcut,
                        isInnerRing = isInnerRing,
                        isOuterRing = isOuterRing
                    )
                )
            }
        }
        return result
    }

    /**
     * Breadth-First Search shortest path between [startId] and [targetId] (inclusive).
     */
    fun findShortestRoute(startId: String, targetId: String): List<String> {
        if (startId !in sectors || targetId !in sectors) return emptyList()
        if (startId == targetId) return listOf(startId)

        val queue = ArrayDeque<String>()
        queue.add(startId)
        val parent = HashMap<String, String?>()
        parent[startId] = null

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == targetId) break
            val node = sectors[current] ?: continue
            for (next in node.connectedSectorIds) {
                if (next !in parent && next in sectors) {
                    parent[next] = current
                    queue.add(next)
                }
            }
        }

        if (targetId !in parent) return emptyList()
        val path = ArrayList<String>()
        var cur: String? = targetId
        while (cur != null) {
            path.add(cur)
            cur = parent[cur]
        }
        return path.reversed()
    }

    /**
     * Generates world-space Jump Gate portals inside [sectorId], oriented along the true
     * compass bearing toward each connected neighbor on the Galactic Map.
     */
    fun portalsForSector(sectorId: String): List<SectorPortal> {
        val current = sectorOf(sectorId)
        val half = current.mapSize * 0.5f
        val ringRadius = when {
            current.isCapitalCity -> 1350f
            current.isLargeSector -> current.mapSize * 0.37f
            else -> current.mapSize * 0.36f
        }

        val portals = ArrayList<SectorPortal>()
        for (i in current.connectedSectorIds.indices) {
            val neighborId = current.connectedSectorIds[i]
            val neighbor = sectorOf(neighborId)
            val dx = neighbor.position.x - current.position.x
            val dy = neighbor.position.y - current.position.y
            var angle = if (dx == 0f && dy == 0f) {
                i * (2f * PI.toFloat() / max(1, current.connectedSectorIds.size))
            } else {
                atan2(dy, dx)
            }

            for (existing in portals) {
                val ex = existing.worldX - half
                val ey = existing.worldY - half
                val existingAngle = atan2(ey, ex)
                var diff = angle - existingAngle
                while (diff > PI) diff -= (2.0 * PI).toFloat()
                while (diff < -PI) diff += (2.0 * PI).toFloat()
                if (abs(diff) < 0.34f) {
                    angle += 0.42f
                }
            }

            val px = (half + cos(angle) * ringRadius).coerceIn(900f, current.mapSize - 900f)
            val py = (half + sin(angle) * ringRadius).coerceIn(900f, current.mapSize - 900f)
            val shortcut = current.isShortcutTo(neighborId)
            portals.add(
                SectorPortal(
                    targetSectorId = neighborId,
                    worldX = px,
                    worldY = py,
                    isShortcut = shortcut,
                    accentColorHex = if (shortcut) 0xFF34D399 else neighbor.nodeColorHex
                )
            )
        }
        return portals
    }
}
