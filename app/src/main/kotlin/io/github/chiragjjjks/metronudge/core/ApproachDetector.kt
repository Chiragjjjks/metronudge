package io.github.chiragjjjks.metronudge.core

/** One location sample from the phone. */
data class Fix(val location: LatLng, val timeMillis: Long, val accuracyMeters: Float = 0f)

data class DetectorConfig(
    /** Nudge when you are this close to a station and walking towards it. */
    val triggerRadiusMeters: Double = 150.0,
    /** Within this distance we check more often so we don't walk past the zone. */
    val nearRadiusMeters: Double = 1_200.0,
    /** How often to check when far from every station (owner's choice: 5 min). */
    val farIntervalMillis: Long = 5 * 60_000L,
    /** How often to check when near a station. */
    val nearIntervalMillis: Long = 60_000L,
    /** You must have got at least this much closer to count as "approaching". */
    val minApproachMeters: Double = 15.0,
    /** Don't nudge again for the same station within this window. */
    val cooldownMillis: Long = 30 * 60_000L,
    /** Ignore fixes worse than this (e.g. coarse cell-tower locations). */
    val maxAccuracyMeters: Float = 120f,
)

data class Decision(
    val station: Station,
    val distanceMeters: Double,
    /** Metres closer than the previous check, negative if moving away; null on the first fix. */
    val closedInMeters: Double?,
    val shouldNudge: Boolean,
    val nextCheckMillis: Long,
) {
    val approaching: Boolean get() = closedInMeters != null && closedInMeters > 0
}

/**
 * Decides, for each location check, whether you're walking towards a metro
 * station and should be nudged to buy a ticket. Pure logic: no Android APIs.
 */
class ApproachDetector(
    private val resolver: StationResolver,
    private val config: DetectorConfig = DetectorConfig(),
) {
    /**
     * @param previous the last accepted fix, or null if this is the first one.
     * @param lastNudgedAt when each station id was last nudged (epoch millis).
     */
    fun evaluate(previous: Fix?, current: Fix, lastNudgedAt: Map<String, Long>): Decision? {
        if (current.accuracyMeters > config.maxAccuracyMeters) return null
        val match = resolver.nearest(current.location) ?: return null
        val station = match.station
        val distance = match.distanceMeters

        val closedIn = previous?.let { resolver.distanceTo(station, it.location) - distance }
        val approaching = closedIn != null && closedIn >= config.minApproachMeters
        val coolingDown = lastNudgedAt[station.id]?.let { current.timeMillis - it < config.cooldownMillis } == true

        val shouldNudge = distance <= config.triggerRadiusMeters && approaching && !coolingDown
        val next = if (distance <= config.nearRadiusMeters) config.nearIntervalMillis else config.farIntervalMillis

        return Decision(station, distance, closedIn, shouldNudge, next)
    }
}
