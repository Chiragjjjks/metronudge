package io.github.chiragjjjks.metronudge.core

/**
 * A point the OS should geofence: a station entrance, or the station centroid
 * when no entrances are mapped. [fenceId] is `<stationId>` for a centroid and
 * `<stationId>#<entranceIndex>` for an entrance.
 */
data class FencePoint(val fenceId: String, val station: Station, val location: LatLng)

data class StationMatch(val station: Station, val distanceMeters: Double)

/** Maps geofence ids and coordinates to stations. */
class StationResolver(val pack: CityPack) {

    /** Every geofence candidate in the city. */
    val fencePoints: List<FencePoint> = pack.stations.flatMap { s ->
        if (s.entrances.isEmpty()) {
            listOf(FencePoint(s.id, s, s.location))
        } else {
            s.entrances.mapIndexed { i, e -> FencePoint("${s.id}$FENCE_SEPARATOR$i", s, e.location) }
        }
    }

    private val fencesById = fencePoints.associateBy { it.fenceId }

    /**
     * The station a geofence id belongs to, or null if unknown. Falls back to
     * the station id so fences registered from an older pack still resolve.
     */
    fun stationForFence(fenceId: String): Station? =
        fencesById[fenceId]?.station
            ?: pack.stationById(fenceId.substringBefore(FENCE_SEPARATOR))

    /** Distance from [point] to the closest fence point of [station]. */
    fun distanceTo(station: Station, point: LatLng): Double =
        (station.entrances.map { it.location } + station.location)
            .minOf { haversineMeters(point, it) }

    /** The nearest station, or null if none is within [maxDistanceMeters]. */
    fun nearest(point: LatLng, maxDistanceMeters: Double? = null): StationMatch? {
        val best = pack.stations
            .map { StationMatch(it, distanceTo(it, point)) }
            .minByOrNull { it.distanceMeters }
            ?: return null
        return if (maxDistanceMeters != null && best.distanceMeters > maxDistanceMeters) null else best
    }

    /** The [n] stations nearest to [point], closest first. */
    fun nearestStations(point: LatLng, n: Int): List<StationMatch> =
        pack.stations
            .map { StationMatch(it, distanceTo(it, point)) }
            .sortedBy { it.distanceMeters }
            .take(n)

    /**
     * The [n] fence points nearest to [point], closest first. The region
     * manager uses this to stay within OS limits (100 geofences on Android).
     */
    fun nearestFences(point: LatLng, n: Int): List<FencePoint> =
        fencePoints.sortedBy { haversineMeters(point, it.location) }.take(n)

    companion object {
        const val FENCE_SEPARATOR = '#'
    }
}
