package io.github.owner.metronudge.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CityPackTest {

    @Test
    fun `parses a valid pack`() {
        val pack = samplePack()
        assertEquals("testville", pack.city)
        assertEquals(3, pack.stations.size)
        assertEquals(listOf("green", "purple"), pack.lines.map { it.id })
        assertEquals(0xFF2E8B3E.toInt(), pack.lineById("green")!!.color)

        val raja = pack.stationById("rajajinagar")!!
        assertEquals("ರಾಜಾಜಿನಗರ", raja.nameKn)
        assertTrue(raja.entrances.isEmpty())

        val majestic = pack.stationById("majestic")!!
        assertEquals(listOf("green", "purple"), majestic.lines)
        assertEquals(listOf("A", "B"), majestic.entrances.map { it.label })

        // "entrances" and "name_kn" are optional.
        assertTrue(pack.stationById("mg-road")!!.entrances.isEmpty())
        assertNull(pack.stationById("mg-road")!!.nameKn)
    }

    private fun errorsFor(mutate: JSONObject.() -> Unit): List<String> {
        val json = samplePackJson().apply(mutate)
        try {
            CityPack.parse(json.toString())
        } catch (e: CityPackException) {
            return e.errors
        }
        fail("expected CityPackException")
        error("unreachable")
    }

    private fun assertSingleError(errors: List<String>, fragment: String) {
        assertEquals(errors.toString(), 1, errors.size)
        assertTrue(errors.single(), errors.single().contains(fragment))
    }

    @Test
    fun `rejects a station outside the bbox`() =
        assertSingleError(errorsFor { station(0).put("lat", 28.6) }, "outside the city bbox")

    @Test
    fun `rejects a missing name`() =
        assertSingleError(errorsFor { station(0).remove("name") }, "\"name\"")

    @Test
    fun `rejects a station with no lines`() =
        assertSingleError(errorsFor { station(0).put("lines", emptyArray()) }, "\"lines\"")

    @Test
    fun `rejects an unknown line`() =
        assertSingleError(
            errorsFor { station(0).put("lines", org.json.JSONArray(listOf("blue"))) },
            "unknown line \"blue\"",
        )

    @Test
    fun `rejects duplicate station ids`() =
        assertSingleError(errorsFor { station(1).put("id", "rajajinagar") }, "duplicate id")

    @Test
    fun `rejects a bad line colour`() =
        assertSingleError(errorsFor { lineAt(0).put("color", "green") }, "#RRGGBB")

    @Test
    fun `reports every error at once`() {
        val errors = errorsFor {
            station(0).put("lat", "x")
            station(1).remove("id")
        }
        assertTrue(errors.toString(), errors.size >= 2)
    }

    @Test
    fun `rejects non-JSON input`() {
        try {
            CityPack.parse("not json")
            fail("expected CityPackException")
        } catch (e: CityPackException) {
            assertTrue(e.errors.single().startsWith("not a JSON object"))
        }
    }
}
