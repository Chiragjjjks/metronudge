#!/usr/bin/env python3
"""Regenerate data/cities/bengaluru.json from OpenStreetMap via Overpass.

Data © OpenStreetMap contributors, licensed under the ODbL.

This is a developer tool. It is never run by the app at runtime.

Usage:
    python3 tools/fetch_stations.py                 # writes data/cities/bengaluru.json
    python3 tools/fetch_stations.py --raw raw.json  # reuse a saved Overpass response
    python3 tools/fetch_stations.py --save-raw raw.json

Standard library only, so it runs anywhere Python 3.9+ is available.
"""

import argparse
import datetime
import json
import math
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_PATH = REPO_ROOT / "data" / "cities" / "bengaluru.json"

# south, west, north, east
BBOX = (12.70, 77.30, 13.30, 77.90)
NETWORK = "Namma Metro"

ENDPOINTS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass.private.coffee/api/interpreter",
]
USER_AGENT = "MetroNudge-station-fetch/0.1 (open-source; tools/fetch_stations.py)"

# A station node/way belongs to a line if one of the line's stop/platform
# members is within this distance.
LINE_MATCH_M = 300
# Entrances further than this from every station are ignored.
ENTRANCE_MATCH_M = 300
# Two station objects with the same name closer than this are one station.
MERGE_M = 400

# Stable ids and display names for lines, keyed by the route's `ref` tag.
# Colours fall back to these when OSM has none or uses a named colour.
LINE_META = {
    "purple": {"name": "Purple Line", "color": "#8E3A96"},
    "green": {"name": "Green Line", "color": "#2E8B3E"},
    "yellow": {"name": "Yellow Line", "color": "#F2C200"},
    "pink": {"name": "Pink Line", "color": "#E7609D"},
    "blue": {"name": "Blue Line", "color": "#1E6FD9"},
}

HEX_RE = re.compile(r"^#[0-9a-fA-F]{6}$")


def build_query():
    s, w, n, e = BBOX
    bbox = f"{s},{w},{n},{e}"
    return f"""
[out:json][timeout:180];
rel["route"="subway"]["network"="{NETWORK}"]({bbox})->.routes;
.routes out body;
node(r.routes)->.members;
.members out;
nwr["railway"="station"]({bbox})->.stations;
.stations out center tags;
node["railway"="subway_entrance"]({bbox});
out;
"""


def fetch(query):
    data = urllib.parse.urlencode({"data": query}).encode()
    last_err = None
    for endpoint in ENDPOINTS:
        for attempt in range(2):
            try:
                req = urllib.request.Request(
                    endpoint, data=data, headers={"User-Agent": USER_AGENT}
                )
                with urllib.request.urlopen(req, timeout=240) as resp:
                    return json.load(resp)
            except Exception as err:  # noqa: BLE001 - try the next mirror
                last_err = err
                print(f"  {endpoint} failed ({err}); retrying", file=sys.stderr)
                time.sleep(3 * (attempt + 1))
    raise SystemExit(f"All Overpass endpoints failed: {last_err}")


def haversine_m(lat1, lng1, lat2, lng2):
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = p2 - p1
    dl = math.radians(lng2 - lng1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def coords(el):
    if "lat" in el:
        return el["lat"], el["lon"]
    if "center" in el:
        return el["center"]["lat"], el["center"]["lon"]
    return None


def slugify(name):
    s = name.lower()
    without_parens = re.sub(r"\(.*?\)", " ", s)
    if without_parens.strip():
        s = without_parens
    return re.sub(r"[^a-z0-9]+", "-", s).strip("-")


def clean_name(name):
    name = re.sub(r"\s+metro station$", "", name, flags=re.I)
    if not re.search(r"railway station$", name, flags=re.I):
        name = re.sub(r"\s+station$", "", name, flags=re.I)
    return name.strip()


def clean_name_kn(name):
    if not name:
        return name
    # Drop "metro" / "metro station" suffixes (both common spellings).
    return re.sub(r"\s+ಮೆಟ್ರ[ೋೊ](\s+ನಿಲ್ದಾಣ)?$", "", name).strip()


def line_id(ref):
    return ref.strip().lower().replace(" line", "")


def build(raw):
    elements = raw["elements"]
    by_id = {(e["type"], e["id"]): e for e in elements}

    # --- Lines and the coordinates of their stops ---------------------------
    lines = {}
    line_points = {}
    for rel in (e for e in elements if e["type"] == "relation"):
        tags = rel.get("tags", {})
        ref = tags.get("ref") or tags.get("colour") or ""
        lid = line_id(ref)
        if not lid:
            continue
        meta = LINE_META.get(lid, {"name": f"{ref} Line", "color": "#777777"})
        colour = tags.get("colour", "")
        lines.setdefault(
            lid,
            {
                "id": lid,
                "name": meta["name"],
                "color": colour.upper() if HEX_RE.match(colour) else meta["color"],
            },
        )
        pts = line_points.setdefault(lid, [])
        for m in rel.get("members", []):
            if m["type"] != "node":
                continue
            node = by_id.get(("node", m["ref"]))
            if node and "lat" in node:
                pts.append((node["lat"], node["lon"]))

    # --- Stations ------------------------------------------------------------
    candidates = []
    for el in elements:
        tags = el.get("tags", {})
        if tags.get("railway") != "station":
            continue
        c = coords(el)
        if not c:
            continue
        name = tags.get("name:en") or tags.get("name")
        if not name:
            continue
        station_lines = sorted(
            lid
            for lid, pts in line_points.items()
            if any(haversine_m(c[0], c[1], p[0], p[1]) <= LINE_MATCH_M for p in pts)
        )
        if not station_lines:
            continue  # not a Namma Metro station (e.g. Indian Railways)
        # Indian Railways stations sit next to several metro stops
        # (Yesvantpur Jn, KR Puram, Baiyappanahalli); keep only metro ones.
        is_metro = tags.get("network") == NETWORK or (
            tags.get("station") in ("subway", "light_rail") and tags.get("network") in (None, NETWORK)
        )
        if not is_metro:
            continue
        candidates.append(
            {
                "name": clean_name(name),
                "name_kn": clean_name_kn(tags.get("name:kn")),
                "lat": c[0],
                "lng": c[1],
                "lines": station_lines,
                "osm": f"{el['type']}/{el['id']}",
                "is_node": el["type"] == "node",
            }
        )

    # Prefer nodes over ways, then merge same-named duplicates.
    candidates.sort(key=lambda s: (not s["is_node"], s["name"]))
    stations = []
    for cand in candidates:
        dup = next(
            (
                s
                for s in stations
                if s["name"].lower() == cand["name"].lower()
                and haversine_m(s["lat"], s["lng"], cand["lat"], cand["lng"]) <= MERGE_M
            ),
            None,
        )
        if dup:
            dup["lines"] = sorted(set(dup["lines"]) | set(cand["lines"]))
            dup["name_kn"] = dup["name_kn"] or cand["name_kn"]
            continue
        stations.append(cand)

    # Unique ids.
    seen = set()
    for s in stations:
        base = slugify(s["name"]) or s["osm"].replace("/", "-")
        sid = base
        n = 2
        while sid in seen:
            sid = f"{base}-{n}"
            n += 1
        seen.add(sid)
        s["id"] = sid

    # --- Entrances -----------------------------------------------------------
    for s in stations:
        s["entrances"] = []
    for el in elements:
        tags = el.get("tags", {})
        if tags.get("railway") != "subway_entrance" or "lat" not in el:
            continue
        nearest = min(
            stations,
            key=lambda s: haversine_m(s["lat"], s["lng"], el["lat"], el["lon"]),
            default=None,
        )
        if not nearest:
            continue
        if haversine_m(nearest["lat"], nearest["lng"], el["lat"], el["lon"]) > ENTRANCE_MATCH_M:
            continue
        entrance = {"lat": round(el["lat"], 6), "lng": round(el["lon"], 6)}
        label = tags.get("ref") or tags.get("name")
        if label:
            entrance["label"] = label
        nearest["entrances"].append(entrance)

    out_stations = []
    for s in sorted(stations, key=lambda s: s["id"]):
        st = {"id": s["id"], "name": s["name"]}
        if s["name_kn"]:
            st["name_kn"] = s["name_kn"]
        st["lines"] = s["lines"]
        st["lat"] = round(s["lat"], 6)
        st["lng"] = round(s["lng"], 6)
        st["entrances"] = sorted(
            s["entrances"], key=lambda e: (e.get("label", ""), e["lat"], e["lng"])
        )
        st["osm"] = s["osm"]
        out_stations.append(st)

    return {
        "city": "bengaluru",
        "system": NETWORK,
        "version": datetime.date.today().isoformat(),
        "source": "OpenStreetMap contributors (ODbL)",
        "bbox": {"south": BBOX[0], "west": BBOX[1], "north": BBOX[2], "east": BBOX[3]},
        "lines": sorted(lines.values(), key=lambda line: line["id"]),
        "stations": out_stations,
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--raw", help="read Overpass JSON from this file instead of fetching")
    ap.add_argument("--save-raw", help="save the Overpass response to this file")
    ap.add_argument("--out", default=str(OUT_PATH))
    args = ap.parse_args()

    if args.raw:
        raw = json.loads(Path(args.raw).read_text())
    else:
        print("Querying Overpass…", file=sys.stderr, flush=True)
        raw = fetch(build_query())
        if args.save_raw:
            Path(args.save_raw).write_text(json.dumps(raw))

    pack = build(raw)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(pack, ensure_ascii=False, indent=2) + "\n")

    counts = {
        line["id"]: sum(line["id"] in s["lines"] for s in pack["stations"])
        for line in pack["lines"]
    }
    print(
        f"Wrote {len(pack['stations'])} stations, {len(pack['lines'])} lines "
        f"{counts} to {out}",
        file=sys.stderr,
    )


if __name__ == "__main__":
    main()
