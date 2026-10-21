package io.github.chiragjjjks.metronudge.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripPlannerTest {
    private val pack = samplePack()
    private val home = pack.stationById("rajajinagar")!!
    private val mgRoad = pack.stationById("mg-road")!!

    @Test
    fun `defaults the destination to home`() {
        assertEquals(home, TripPlanner.plan(mgRoad, home).destination)
    }

    @Test
    fun `asks where to when leaving home`() {
        assertNull(TripPlanner.plan(home, home).destination)
    }

    @Test
    fun `no destination when no home is set`() {
        assertNull(TripPlanner.plan(mgRoad, null).destination)
    }
}
