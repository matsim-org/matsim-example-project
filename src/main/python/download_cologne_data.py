#!/usr/bin/env python3
"""
Download Real Cologne Road Network & Traffic Data
===================================================

Downloads and processes the following data for Cologne (Köln):

1. **Road Network** — Full drivable road network from OpenStreetMap via Overpass API
   - Motorway through residential streets
   - Attributes: speed limits, lanes, one-way, surface type
   - Converted to MATSim-compatible nodes/links CSVs (EPSG:25832)

2. **Traffic Count Stations** — Real traffic sensor data from Offene Daten Köln
   - Loop detector locations and current readings
   - For model validation against real-world volumes

3. **City Boundary** — Cologne administrative boundary polygon
   - Used for spatial clipping and visualization

Output (scenarios/cologne/data/):
    nodes.csv           — Network nodes (id, x_utm, y_utm, lat, lon)
    links.csv           — Network links (id, from, to, length, freespeed, capacity, lanes, highway, name)
    boundary.geojson    — Cologne city boundary polygon
    count_stations.csv  — Traffic count station locations (if available)
    network_summary.txt — Summary statistics

Usage:
    python download_cologne_data.py

Requirements:
    pip install requests pyproj
"""

import csv
import json
import math
import os
import sys
import time

import requests
from pyproj import Transformer

# ============================================================================
# CONFIGURATION
# ============================================================================

# Cologne OSM relation ID
COLOGNE_RELATION_ID = 62578

# Output directory
OUTPUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "..", "scenarios", "cologne", "data"
)

# Overpass API endpoint
OVERPASS_URL = "https://overpass-api.de/api/interpreter"

# Coordinate transformer: WGS84 → UTM 32N
_to_utm = Transformer.from_crs("EPSG:4326", "EPSG:25832", always_xy=True)

# MATSim highway defaults for German roads
# Source: MATSim OsmNetworkReader + German HBS 2015 manual
# Format: (freespeed_m_s, capacity_veh_hr_per_lane, default_lanes)
HIGHWAY_DEFAULTS = {
    "motorway":       (27.78, 2000, 2),   # 100 km/h
    "motorway_link":  (22.22, 1500, 1),   # 80 km/h
    "trunk":          (22.22, 2000, 2),   # 80 km/h
    "trunk_link":     (13.89, 1500, 1),   # 50 km/h
    "primary":        (13.89, 1500, 1),   # 50 km/h
    "primary_link":   (13.89, 1500, 1),   # 50 km/h
    "secondary":      (11.11, 1000, 1),   # 40 km/h
    "secondary_link": (11.11, 1000, 1),   # 40 km/h
    "tertiary":       (8.33,  600,  1),   # 30 km/h
    "tertiary_link":  (8.33,  600,  1),   # 30 km/h
    "unclassified":   (8.33,  600,  1),   # 30 km/h
    "residential":    (8.33,  600,  1),   # 30 km/h
}

# Maximum retries for API requests
MAX_RETRIES = 4
RETRY_BACKOFF = [2, 4, 8, 16]


# ============================================================================
# HTTP HELPERS
# ============================================================================

def fetch_with_retry(url, params=None, data=None, timeout=300):
    """Fetch a URL with exponential backoff retry."""
    for attempt in range(MAX_RETRIES):
        try:
            if data:
                resp = requests.post(url, data=data, timeout=timeout)
            else:
                resp = requests.get(url, params=params, timeout=timeout)
            resp.raise_for_status()
            return resp
        except (requests.RequestException, requests.Timeout) as e:
            if attempt < MAX_RETRIES - 1:
                wait = RETRY_BACKOFF[attempt]
                print(f"    Retry {attempt+1}/{MAX_RETRIES} after {wait}s — {e}")
                time.sleep(wait)
            else:
                raise


# ============================================================================
# 1. DOWNLOAD ROAD NETWORK
# ============================================================================

def download_road_network():
    """Download Cologne road network from Overpass API."""
    print("\n  [1/3] Downloading road network from OpenStreetMap...")
    print(f"        Area: Cologne (relation/{COLOGNE_RELATION_ID})")

    query = f"""
    [out:json][timeout:300];
    area(3600{COLOGNE_RELATION_ID:06d})->.searchArea;
    (
      way(area.searchArea)[highway~"^(motorway|motorway_link|trunk|trunk_link|primary|primary_link|secondary|secondary_link|tertiary|tertiary_link|unclassified|residential)$"];
    );
    (._;>;);
    out body;
    """

    resp = fetch_with_retry(OVERPASS_URL, data={"data": query}, timeout=300)
    data = resp.json()

    elements = data.get("elements", [])
    print(f"        Raw elements: {len(elements)}")

    # Separate nodes and ways
    osm_nodes = {}
    osm_ways = []

    for el in elements:
        if el["type"] == "node":
            osm_nodes[el["id"]] = {
                "lat": el["lat"],
                "lon": el["lon"],
            }
        elif el["type"] == "way":
            osm_ways.append(el)

    print(f"        OSM nodes: {len(osm_nodes)}")
    print(f"        OSM ways:  {len(osm_ways)}")

    return osm_nodes, osm_ways


def parse_speed(tags):
    """Parse maxspeed tag to m/s. Returns None if missing/unparseable."""
    raw = tags.get("maxspeed", "")
    if not raw:
        return None
    # Handle special German values
    if raw in ("signals", "variable", "none", "walk"):
        return None
    if raw == "DE:motorway":
        return 33.33  # 120 km/h effective
    if raw == "DE:urban":
        return 13.89  # 50 km/h
    if raw == "DE:rural":
        return 27.78  # 100 km/h
    if raw.startswith("DE:zone"):
        try:
            return float(raw.split(":")[-1]) / 3.6
        except ValueError:
            return None
    # Try numeric parsing
    try:
        # Handle "50 mph" or just "50"
        parts = raw.split()
        speed_val = float(parts[0])
        if len(parts) > 1 and parts[1] == "mph":
            speed_val *= 1.60934
        return speed_val / 3.6  # km/h → m/s
    except (ValueError, IndexError):
        return None


def parse_lanes(tags, highway_type, oneway):
    """Parse lanes tag. Returns default if missing."""
    raw = tags.get("lanes", "")
    default_lanes = HIGHWAY_DEFAULTS.get(highway_type, (8.33, 600, 1))[2]
    if not raw:
        return default_lanes
    try:
        total_lanes = int(raw)
        if oneway:
            return total_lanes
        # For two-way streets, lanes per direction = total / 2
        return max(1, total_lanes // 2)
    except ValueError:
        return default_lanes


def is_oneway(tags, highway_type):
    """Determine if a way is one-way."""
    ow = tags.get("oneway", "")
    if ow in ("yes", "true", "1"):
        return True
    if ow == "-1":
        return True  # one-way in reverse direction
    if highway_type in ("motorway", "motorway_link"):
        return True  # motorways are one-way by convention
    return False


def is_reverse_oneway(tags):
    """Check if oneway=-1 (one-way in reverse direction)."""
    return tags.get("oneway", "") == "-1"


def convert_to_matsim_network(osm_nodes, osm_ways):
    """Convert OSM data to MATSim-style nodes and links."""
    print("\n  Processing network...")

    # Convert all referenced nodes to UTM
    nodes = {}
    for way in osm_ways:
        for nd_id in way.get("nodes", []):
            if nd_id in osm_nodes and nd_id not in nodes:
                n = osm_nodes[nd_id]
                x, y = _to_utm.transform(n["lon"], n["lat"])
                nodes[nd_id] = {
                    "id": nd_id,
                    "x": x,
                    "y": y,
                    "lat": n["lat"],
                    "lon": n["lon"],
                }

    # Create links from ways
    links = []
    link_id = 0
    highway_counts = {}

    for way in osm_ways:
        tags = way.get("tags", {})
        highway_type = tags.get("highway", "residential")
        way_nodes = way.get("nodes", [])
        way_name = tags.get("name", "")
        way_ref = tags.get("ref", "")
        display_name = way_name or way_ref or highway_type

        highway_counts[highway_type] = highway_counts.get(highway_type, 0) + 1

        # Get defaults for this highway type
        defaults = HIGHWAY_DEFAULTS.get(highway_type, (8.33, 600, 1))
        default_speed, default_capacity, _ = defaults

        # Parse attributes
        speed = parse_speed(tags)
        if speed is None:
            speed = default_speed

        oneway = is_oneway(tags, highway_type)
        reverse = is_reverse_oneway(tags)
        lanes = parse_lanes(tags, highway_type, oneway)
        capacity = default_capacity  # per lane per hour

        # Create links between consecutive node pairs
        # (MATSim convention: one link per segment between nodes)
        for i in range(len(way_nodes) - 1):
            from_id = way_nodes[i]
            to_id = way_nodes[i + 1]

            if from_id not in nodes or to_id not in nodes:
                continue

            n1 = nodes[from_id]
            n2 = nodes[to_id]
            length = math.sqrt((n1["x"] - n2["x"])**2 + (n1["y"] - n2["y"])**2)

            if length < 1.0:
                continue  # skip degenerate segments

            if oneway and reverse:
                # One-way in reverse: only to→from
                link_id += 1
                links.append({
                    "id": link_id,
                    "from_node": to_id,
                    "to_node": from_id,
                    "length": round(length, 2),
                    "freespeed": round(speed, 2),
                    "capacity": capacity * lanes,
                    "lanes": lanes,
                    "highway": highway_type,
                    "name": display_name,
                    "osm_way_id": way["id"],
                })
            elif oneway:
                # One-way forward: only from→to
                link_id += 1
                links.append({
                    "id": link_id,
                    "from_node": from_id,
                    "to_node": to_id,
                    "length": round(length, 2),
                    "freespeed": round(speed, 2),
                    "capacity": capacity * lanes,
                    "lanes": lanes,
                    "highway": highway_type,
                    "name": display_name,
                    "osm_way_id": way["id"],
                })
            else:
                # Two-way: create links in both directions
                link_id += 1
                links.append({
                    "id": link_id,
                    "from_node": from_id,
                    "to_node": to_id,
                    "length": round(length, 2),
                    "freespeed": round(speed, 2),
                    "capacity": capacity * lanes,
                    "lanes": lanes,
                    "highway": highway_type,
                    "name": display_name,
                    "osm_way_id": way["id"],
                })
                link_id += 1
                links.append({
                    "id": link_id,
                    "from_node": to_id,
                    "to_node": from_id,
                    "length": round(length, 2),
                    "freespeed": round(speed, 2),
                    "capacity": capacity * lanes,
                    "lanes": lanes,
                    "highway": highway_type,
                    "name": display_name,
                    "osm_way_id": way["id"],
                })

    print(f"        Network nodes: {len(nodes):,}")
    print(f"        Network links: {len(links):,}")
    print(f"        Highway types:")
    for hw, count in sorted(highway_counts.items(), key=lambda x: -x[1]):
        print(f"          {hw:20s}: {count:,} ways")

    return nodes, links


# ============================================================================
# 2. DOWNLOAD CITY BOUNDARY
# ============================================================================

def download_boundary():
    """Download Cologne administrative boundary as GeoJSON."""
    print("\n  [2/3] Downloading Cologne city boundary...")

    query = f"""
    [out:json][timeout:60];
    relation({COLOGNE_RELATION_ID});
    out body;
    >;
    out skel qt;
    """

    resp = fetch_with_retry(OVERPASS_URL, data={"data": query}, timeout=120)
    data = resp.json()

    elements = data.get("elements", [])
    nodes_map = {}
    ways_map = {}
    relation = None

    for el in elements:
        if el["type"] == "node":
            nodes_map[el["id"]] = (el["lon"], el["lat"])
        elif el["type"] == "way":
            ways_map[el["id"]] = el.get("nodes", [])
        elif el["type"] == "relation":
            relation = el

    if not relation:
        print("        Warning: Could not download boundary relation")
        return None

    # Extract outer boundary members
    outer_ways = []
    for member in relation.get("members", []):
        if member.get("type") == "way" and member.get("role") in ("outer", ""):
            wid = member["ref"]
            if wid in ways_map:
                outer_ways.append(ways_map[wid])

    if not outer_ways:
        print("        Warning: No outer boundary ways found")
        return None

    # Chain ways into a ring
    ring = list(outer_ways[0])
    used = {0}
    for _ in range(len(outer_ways)):
        last_node = ring[-1]
        found = False
        for i, w in enumerate(outer_ways):
            if i in used:
                continue
            if w[0] == last_node:
                ring.extend(w[1:])
                used.add(i)
                found = True
                break
            elif w[-1] == last_node:
                ring.extend(reversed(w[:-1]))
                used.add(i)
                found = True
                break
        if not found:
            break

    # Convert to GeoJSON coordinates
    coords = []
    for nid in ring:
        if nid in nodes_map:
            coords.append(list(nodes_map[nid]))

    # Close the ring if needed
    if coords and coords[0] != coords[-1]:
        coords.append(coords[0])

    geojson = {
        "type": "FeatureCollection",
        "features": [{
            "type": "Feature",
            "properties": {
                "name": "Köln",
                "name_en": "Cologne",
                "osm_relation_id": COLOGNE_RELATION_ID,
            },
            "geometry": {
                "type": "Polygon",
                "coordinates": [coords],
            }
        }]
    }

    print(f"        Boundary points: {len(coords)}")
    return geojson


# ============================================================================
# 3. DOWNLOAD TRAFFIC COUNT STATIONS
# ============================================================================

def download_count_stations():
    """Download traffic count station locations from Offene Daten Köln."""
    print("\n  [3/3] Downloading traffic count stations...")

    # Cologne open data — traffic sensor locations
    # This endpoint provides traffic monitoring locations
    url = "https://geoportal.stadt-koeln.de/arcgis/rest/services/verkehr/gesamtstadt/MapServer/0/query"
    params = {
        "where": "1=1",
        "outFields": "*",
        "f": "json",
        "returnGeometry": "true",
        "outSR": "4326",
    }

    try:
        resp = fetch_with_retry(url, params=params, timeout=60)
        data = resp.json()
        features = data.get("features", [])

        if not features:
            print("        No count station data available from this endpoint")
            return None

        stations = []
        for feat in features:
            attrs = feat.get("attributes", {})
            geom = feat.get("geometry", {})
            if geom:
                stations.append({
                    "id": attrs.get("OBJECTID", ""),
                    "name": attrs.get("BEZEICHNUNG", attrs.get("NAME", "")),
                    "lat": geom.get("y", 0),
                    "lon": geom.get("x", 0),
                    "type": attrs.get("TYP", ""),
                })

        print(f"        Count stations: {len(stations)}")
        return stations

    except Exception as e:
        print(f"        Count stations unavailable: {e}")
        print("        (This is optional — network download is not affected)")
        return None


# ============================================================================
# OUTPUT WRITERS
# ============================================================================

def write_nodes_csv(nodes, filepath):
    """Write nodes to CSV."""
    with open(filepath, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(["id", "x_utm", "y_utm", "lat", "lon"])
        for nid, n in sorted(nodes.items()):
            writer.writerow([nid, f"{n['x']:.2f}", f"{n['y']:.2f}",
                             f"{n['lat']:.6f}", f"{n['lon']:.6f}"])


def write_links_csv(links, filepath):
    """Write links to CSV."""
    with open(filepath, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(["id", "from_node", "to_node", "length_m", "freespeed_ms",
                         "capacity_veh_hr", "lanes", "highway", "name", "osm_way_id"])
        for link in links:
            writer.writerow([
                link["id"], link["from_node"], link["to_node"],
                link["length"], link["freespeed"], link["capacity"],
                link["lanes"], link["highway"], link["name"],
                link["osm_way_id"],
            ])


def write_count_stations_csv(stations, filepath):
    """Write count stations to CSV."""
    with open(filepath, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(["id", "name", "lat", "lon", "type"])
        for s in stations:
            writer.writerow([s["id"], s["name"], f"{s['lat']:.6f}",
                             f"{s['lon']:.6f}", s["type"]])


def write_summary(nodes, links, boundary, stations, filepath):
    """Write a human-readable network summary."""
    # Compute statistics
    total_length_km = sum(l["length"] for l in links) / 1000.0

    hw_stats = {}
    for link in links:
        hw = link["highway"]
        hw_stats.setdefault(hw, {"count": 0, "length_km": 0})
        hw_stats[hw]["count"] += 1
        hw_stats[hw]["length_km"] += link["length"] / 1000.0

    xs = [n["x"] for n in nodes.values()]
    ys = [n["y"] for n in nodes.values()]
    bbox_width = (max(xs) - min(xs)) / 1000.0
    bbox_height = (max(ys) - min(ys)) / 1000.0

    with open(filepath, 'w') as f:
        f.write("=" * 70 + "\n")
        f.write("  COLOGNE ROAD NETWORK — DATA SUMMARY\n")
        f.write("  Source: OpenStreetMap (Overpass API)\n")
        f.write("=" * 70 + "\n\n")

        f.write(f"  Nodes:              {len(nodes):>10,}\n")
        f.write(f"  Links:              {len(links):>10,}\n")
        f.write(f"  Total length:       {total_length_km:>10,.1f} km\n")
        f.write(f"  Bounding box:       {bbox_width:.1f} x {bbox_height:.1f} km\n")
        f.write(f"  CRS:                EPSG:25832 (UTM 32N)\n\n")

        f.write("  Links by highway type:\n")
        f.write(f"  {'Type':<20s} {'Links':>8s} {'Length (km)':>12s}\n")
        f.write("  " + "-" * 42 + "\n")
        for hw in sorted(hw_stats.keys(), key=lambda h: -hw_stats[h]["count"]):
            s = hw_stats[hw]
            f.write(f"  {hw:<20s} {s['count']:>8,} {s['length_km']:>12,.1f}\n")
        f.write("  " + "-" * 42 + "\n")
        f.write(f"  {'TOTAL':<20s} {len(links):>8,} {total_length_km:>12,.1f}\n\n")

        if boundary:
            f.write("  City boundary:      Downloaded (GeoJSON)\n")
        if stations:
            f.write(f"  Count stations:     {len(stations)}\n")

        f.write("\n" + "=" * 70 + "\n")


# ============================================================================
# MAIN
# ============================================================================

def main():
    start_time = time.time()

    print("=" * 70)
    print("  COLOGNE DATA DOWNLOADER")
    print("  Downloading real-world data for traffic simulation")
    print("=" * 70)

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"\n  Output directory: {OUTPUT_DIR}")

    # 1. Road network
    osm_nodes, osm_ways = download_road_network()
    nodes, links = convert_to_matsim_network(osm_nodes, osm_ways)

    nodes_path = os.path.join(OUTPUT_DIR, "nodes.csv")
    links_path = os.path.join(OUTPUT_DIR, "links.csv")
    write_nodes_csv(nodes, nodes_path)
    write_links_csv(links, links_path)
    print(f"        Saved: {nodes_path}")
    print(f"        Saved: {links_path}")

    # 2. City boundary
    boundary = download_boundary()
    if boundary:
        boundary_path = os.path.join(OUTPUT_DIR, "boundary.geojson")
        with open(boundary_path, 'w') as f:
            json.dump(boundary, f)
        print(f"        Saved: {boundary_path}")

    # 3. Traffic count stations (optional)
    stations = download_count_stations()
    if stations:
        stations_path = os.path.join(OUTPUT_DIR, "count_stations.csv")
        write_count_stations_csv(stations, stations_path)
        print(f"        Saved: {stations_path}")

    # Summary
    summary_path = os.path.join(OUTPUT_DIR, "network_summary.txt")
    write_summary(nodes, links, boundary, stations, summary_path)
    print(f"        Saved: {summary_path}")

    elapsed = time.time() - start_time
    print(f"\n  Completed in {elapsed:.0f}s")
    print(f"\n  Total: {len(nodes):,} nodes, {len(links):,} links")
    print(f"  Files saved to: {OUTPUT_DIR}")
    print("\n  Next step: Run the simulation with the real network")
    print("    python cologne_simulation.py --network real")


if __name__ == "__main__":
    main()
