package io.github.chiragjjjks.metronudge.core

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A WGS84 coordinate. */
data class LatLng(val lat: Double, val lng: Double) {
    val isValid: Boolean
        get() = lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0
}

data class BoundingBox(val south: Double, val west: Double, val north: Double, val east: Double) {
    operator fun contains(p: LatLng): Boolean =
        p.lat in south..north && p.lng in west..east
}

private const val EARTH_RADIUS_M = 6_371_000.0

/** Great-circle distance between [a] and [b] in metres. */
fun haversineMeters(a: LatLng, b: LatLng): Double {
    val p1 = Math.toRadians(a.lat)
    val p2 = Math.toRadians(b.lat)
    val dp = p2 - p1
    val dl = Math.toRadians(b.lng - a.lng)
    val h = sin(dp / 2).let { it * it } + cos(p1) * cos(p2) * sin(dl / 2).let { it * it }
    return 2 * EARTH_RADIUS_M * asin(sqrt(h))
}
