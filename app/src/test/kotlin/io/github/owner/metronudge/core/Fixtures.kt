package io.github.owner.metronudge.core

import org.json.JSONArray
import org.json.JSONObject

fun samplePackJson(): JSONObject = JSONObject(
    """
    {
      "city": "testville",
      "system": "Test Metro",
      "version": "2026-10-21",
      "source": "test",
      "bbox": {"south": 12.7, "west": 77.3, "north": 13.3, "east": 77.9},
      "lines": [
        {"id": "green", "name": "Green Line", "color": "#2E8B3E"},
        {"id": "purple", "name": "Purple Line", "color": "#8E3A96"}
      ],
      "stations": [
        {"id": "rajajinagar", "name": "Rajajinagar", "name_kn": "ರಾಜಾಜಿನಗರ",
         "lines": ["green"], "lat": 12.9975, "lng": 77.5497, "entrances": []},
        {"id": "majestic", "name": "Majestic", "lines": ["green", "purple"],
         "lat": 12.9757, "lng": 77.5728,
         "entrances": [
           {"lat": 12.9760, "lng": 77.5720, "label": "A"},
           {"lat": 12.9750, "lng": 77.5735, "label": "B"}
         ]},
        {"id": "mg-road", "name": "MG Road", "lines": ["purple"],
         "lat": 12.9755, "lng": 77.6068}
      ]
    }
    """,
)

fun samplePack(): CityPack = CityPack.parse(samplePackJson().toString())

fun JSONObject.station(i: Int): JSONObject = getJSONArray("stations").getJSONObject(i)

fun JSONObject.lineAt(i: Int): JSONObject = getJSONArray("lines").getJSONObject(i)

fun emptyArray() = JSONArray()
