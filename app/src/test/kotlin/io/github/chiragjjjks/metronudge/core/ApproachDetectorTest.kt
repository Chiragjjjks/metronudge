package io.github.chiragjjjks.metronudge.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApproachDetectorTest {
    private val pack = samplePack()
    private val detector = ApproachDetector(StationResolver(pack))
    private val mgRoad = LatLng(12.9755, 77.6068)

    /** A point [metersEast] east of MG Road (1° lng ≈ 108.5 km at this latitude). */
    private fun eastOfMgRoad(metersEast: Double) = LatLng(mgRoad.lat, mgRoad.lng + metersEast / 108_500.0)

    private fun fix(metersEast: Double, minute: Int, accuracy: Float = 10f) =
        Fix(eastOfMgRoad(metersEast), minute * 60_000L, accuracy)

    @Test
    fun `nudges when walking towards a station and within 150 m`() {
        val d = detector.evaluate(fix(400.0, 0), fix(120.0, 4), emptyMap())!!
        assertEquals("mg-road", d.station.id)
        assertTrue(d.approaching)
        assertTrue(d.shouldNudge)
    }

    @Test
    fun `does not nudge on the first fix even when close`() {
        val d = detector.evaluate(null, fix(100.0, 0), emptyMap())!!
        assertNull(d.closedInMeters)
        assertFalse(d.shouldNudge)
    }

    @Test
    fun `does not nudge when walking away`() {
        val d = detector.evaluate(fix(50.0, 0), fix(140.0, 1), emptyMap())!!
        assertFalse(d.approaching)
        assertFalse(d.shouldNudge)
    }

    @Test
    fun `does not nudge when standing still near a station`() {
        val d = detector.evaluate(fix(100.0, 0), fix(95.0, 1), emptyMap())!!
        assertFalse(d.shouldNudge)
    }

    @Test
    fun `does not nudge when approaching but still far`() {
        val d = detector.evaluate(fix(900.0, 0), fix(600.0, 1), emptyMap())!!
        assertTrue(d.approaching)
        assertFalse(d.shouldNudge)
    }

    @Test
    fun `respects the cooldown per station`() {
        val recent = mapOf("mg-road" to 2 * 60_000L)
        assertFalse(detector.evaluate(fix(400.0, 0), fix(120.0, 4), recent)!!.shouldNudge)
        val old = mapOf("mg-road" to -60 * 60_000L)
        assertTrue(detector.evaluate(fix(400.0, 0), fix(120.0, 4), old)!!.shouldNudge)
    }

    @Test
    fun `checks every 5 minutes when far and every minute when near`() {
        val far = detector.evaluate(null, Fix(LatLng(13.25, 77.85), 0, 10f), emptyMap())!!
        assertEquals(5 * 60_000L, far.nextCheckMillis)
        val near = detector.evaluate(null, fix(800.0, 0), emptyMap())!!
        assertEquals(60_000L, near.nextCheckMillis)
    }

    @Test
    fun `ignores inaccurate fixes`() {
        assertNull(detector.evaluate(fix(400.0, 0), fix(120.0, 4, accuracy = 500f), emptyMap()))
    }
}
