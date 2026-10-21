package io.github.chiragjjjks.metronudge.core

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A metro line, e.g. the Purple Line. [color] is ARGB. */
data class MetroLine(val id: String, val name: String, val color: Int)

/** A station entrance, used as a geofence centre. */
data class Entrance(val location: LatLng, val label: String? = null)

data class Station(
    val id: String,
    val name: String,
    val nameKn: String?,
    val lines: List<String>,
    val location: LatLng,
    val entrances: List<Entrance> = emptyList(),
)

class CityPackException(val errors: List<String>) :
    Exception("Invalid city pack:\n" + errors.joinToString("\n"))

/** Bundled station data for one city (`data/cities/<city>.json`). */
class CityPack private constructor(
    val city: String,
    val system: String,
    val version: String,
    val source: String,
    val bounds: BoundingBox,
    val lines: List<MetroLine>,
    val stations: List<Station>,
) {
    private val stationsById = stations.associateBy { it.id }
    private val linesById = lines.associateBy { it.id }

    fun stationById(id: String): Station? = stationsById[id]
    fun lineById(id: String): MetroLine? = linesById[id]

    companion object {
        private val HEX = Regex("^#([0-9a-fA-F]{6})$")

        /**
         * Parses and validates a city pack. Throws [CityPackException] listing
         * every problem found.
         */
        fun parse(source: String): CityPack {
            val root = try {
                JSONObject(source)
            } catch (e: JSONException) {
                throw CityPackException(listOf("not a JSON object: ${e.message}"))
            }
            return Parser(root).parse()
        }
    }

    private class Parser(private val root: JSONObject) {
        val errors = mutableListOf<String>()

        fun str(o: JSONObject, key: String, where: String): String {
            val v = o.opt(key)
            if (v is String && v.isNotBlank()) return v
            errors += "$where: \"$key\" must be a non-empty string"
            return ""
        }

        fun num(o: JSONObject, key: String, where: String): Double {
            val v = o.opt(key)
            if (v is Number) return v.toDouble()
            errors += "$where: \"$key\" must be a number"
            return Double.NaN
        }

        fun objects(arr: JSONArray, where: String): List<JSONObject?> =
            (0 until arr.length()).map { i ->
                arr.opt(i) as? JSONObject ?: null.also { errors += "$where[$i]: must be an object" }
            }

        fun parse(): CityPack {
            val city = str(root, "city", "pack")
            val system = str(root, "system", "pack")
            val version = str(root, "version", "pack")
            val source = str(root, "source", "pack")

            val bbox = root.optJSONObject("bbox")
            val bounds = if (bbox == null) {
                errors += "pack: \"bbox\" must be an object"
                BoundingBox(-90.0, -180.0, 90.0, 180.0)
            } else {
                BoundingBox(
                    south = num(bbox, "south", "bbox"),
                    west = num(bbox, "west", "bbox"),
                    north = num(bbox, "north", "bbox"),
                    east = num(bbox, "east", "bbox"),
                )
            }

            val lines = mutableListOf<MetroLine>()
            val lineIds = mutableSetOf<String>()
            val linesJson = root.optJSONArray("lines")
            if (linesJson == null || linesJson.length() == 0) {
                errors += "pack: \"lines\" must be a non-empty list"
            } else {
                objects(linesJson, "lines").forEachIndexed { i, l ->
                    if (l == null) return@forEachIndexed
                    val id = str(l, "id", "lines[$i]")
                    val name = str(l, "name", "lines[$i]")
                    val colorStr = str(l, "color", "lines[$i]")
                    val color = HEX.matchEntire(colorStr)?.let {
                        (0xFF000000 or it.groupValues[1].toLong(16)).toInt()
                    }
                    if (colorStr.isNotEmpty() && color == null) {
                        errors += "lines[$i]: \"color\" must be #RRGGBB, got \"$colorStr\""
                    }
                    if (!lineIds.add(id)) errors += "lines[$i]: duplicate id \"$id\""
                    lines += MetroLine(id, name, color ?: 0xFF777777.toInt())
                }
            }

            val stations = mutableListOf<Station>()
            val stationIds = mutableSetOf<String>()
            val stationsJson = root.optJSONArray("stations")
            if (stationsJson == null || stationsJson.length() == 0) {
                errors += "pack: \"stations\" must be a non-empty list"
            } else {
                objects(stationsJson, "stations").forEachIndexed { i, s ->
                    if (s == null) return@forEachIndexed
                    stations += station(s, i, bounds, lineIds, stationIds)
                }
            }

            if (errors.isNotEmpty()) throw CityPackException(errors)
            return CityPack(city, system, version, source, bounds, lines, stations)
        }

        fun station(
            s: JSONObject,
            i: Int,
            bounds: BoundingBox,
            lineIds: Set<String>,
            stationIds: MutableSet<String>,
        ): Station {
            val id = str(s, "id", "stations[$i]")
            val where = "stations[$i] ($id)"
            val name = str(s, "name", where)
            if (id.isNotEmpty() && !stationIds.add(id)) errors += "$where: duplicate id"

            val nameKn = s.opt("name_kn")
            if (nameKn != null && nameKn !is String) errors += "$where: \"name_kn\" must be a string"

            val loc = LatLng(num(s, "lat", where), num(s, "lng", where))
            if (!loc.isValid || loc !in bounds) errors += "$where: location $loc is outside the city bbox"

            val stationLines = mutableListOf<String>()
            val sl = s.optJSONArray("lines")
            if (sl == null || sl.length() == 0) {
                errors += "$where: \"lines\" must be a non-empty list"
            } else {
                for (j in 0 until sl.length()) {
                    val l = sl.opt(j)
                    if (l is String && l in lineIds) stationLines += l
                    else errors += "$where: unknown line \"$l\""
                }
            }

            val entrances = mutableListOf<Entrance>()
            when (val ej = s.opt("entrances")) {
                null -> Unit
                is JSONArray -> objects(ej, "$where entrances").forEachIndexed { j, e ->
                    if (e == null) return@forEachIndexed
                    val eLoc = LatLng(
                        num(e, "lat", "$where entrances[$j]"),
                        num(e, "lng", "$where entrances[$j]"),
                    )
                    if (!eLoc.isValid || eLoc !in bounds) {
                        errors += "$where: entrances[$j] is outside the city bbox"
                    }
                    entrances += Entrance(eLoc, e.opt("label") as? String)
                }
                else -> errors += "$where: \"entrances\" must be a list"
            }

            return Station(
                id = id,
                name = name,
                nameKn = (nameKn as? String)?.takeIf { it.isNotEmpty() },
                lines = stationLines,
                location = loc,
                entrances = entrances,
            )
        }
    }
}
