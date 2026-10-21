package io.github.chiragjjjks.metronudge.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Validates the real bundled data in data/cities/bengaluru.json. */
class BengaluruDataTest {
    // Gradle runs unit tests with the module directory (app/) as cwd.
    private val pack = CityPack.parse(File("../data/cities/bengaluru.json").readText())

    // Generous box around greater Bengaluru, independent of the pack's own bbox.
    private val bengaluru = BoundingBox(south = 12.70, west = 77.30, north = 13.30, east = 77.90)

    @Test
    fun `every station is valid and inside Bengaluru`() {
        assertEquals("bengaluru", pack.city)
        assertTrue("expected a full network, got ${pack.stations.size}", pack.stations.size >= 50)
        for (s in pack.stations) {
            assertTrue("${s.id}: empty id", s.id.isNotBlank())
            assertTrue("${s.id}: empty name", s.name.isNotBlank())
            assertTrue("${s.id}: no lines", s.lines.isNotEmpty())
            assertTrue("${s.id}: ${s.location} outside Bengaluru", s.location in bengaluru)
            s.entrances.forEach {
                assertTrue("${s.id}: entrance ${it.location} outside Bengaluru", it.location in bengaluru)
            }
        }
    }

    @Test
    fun `owner's stations exist`() {
        val names = pack.stations.map { it.name.lowercase() }
        assertTrue(names.any { "rajajinagar" in it })
        assertTrue(names.any { "rashtreeya vidyalaya" in it || "rv road" in it })
    }
}
