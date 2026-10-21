package io.github.owner.metronudge.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationResolverTest {
    private val resolver = StationResolver(samplePack())

    @Test
    fun `haversine matches a known distance`() {
        // Majestic -> MG Road is about 3.68 km as the crow flies.
        val d = haversineMeters(LatLng(12.9757, 77.5728), LatLng(12.9755, 77.6068))
        assertEquals(3685.0, d, 15.0)
        assertEquals(0.0, haversineMeters(LatLng(1.0, 2.0), LatLng(1.0, 2.0)), 0.0)
    }

    @Test
    fun `builds one fence per entrance, or one per centroid`() {
        assertEquals(
            listOf("rajajinagar", "majestic#0", "majestic#1", "mg-road"),
            resolver.fencePoints.map { it.fenceId },
        )
    }

    @Test
    fun `resolves fence ids to stations`() {
        assertEquals("majestic", resolver.stationForFence("majestic#1")!!.id)
        assertEquals("rajajinagar", resolver.stationForFence("rajajinagar")!!.id)
        // An entrance index from an older pack still maps by station id.
        assertEquals("majestic", resolver.stationForFence("majestic#9")!!.id)
        assertNull(resolver.stationForFence("nowhere"))
    }

    @Test
    fun `finds the nearest station`() {
        val m = resolver.nearest(LatLng(12.9760, 77.6060))!!
        assertEquals("mg-road", m.station.id)
        assertTrue(m.distanceMeters < 150)
    }

    @Test
    fun `uses entrances when measuring distance`() {
        // About 10 m from entrance A, further from the centroid.
        val m = resolver.nearest(LatLng(12.97609, 77.5720))!!
        assertEquals("majestic", m.station.id)
        assertTrue(m.distanceMeters < 15)
    }

    @Test
    fun `respects maxDistanceMeters`() {
        val farAway = LatLng(13.1, 77.7)
        assertNull(resolver.nearest(farAway, maxDistanceMeters = 300.0))
        assertNotNull(resolver.nearest(farAway))
    }

    @Test
    fun `orders nearest stations and fences by distance`() {
        val nearRaja = LatLng(12.998, 77.55)
        assertEquals(
            listOf("rajajinagar", "majestic"),
            resolver.nearestStations(nearRaja, 2).map { it.station.id },
        )
        val fences = resolver.nearestFences(nearRaja, 3)
        assertEquals("rajajinagar", fences.first().fenceId)
        assertEquals(3, fences.size)
    }
}
