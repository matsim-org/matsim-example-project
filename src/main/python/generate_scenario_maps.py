#!/usr/bin/env python3
"""
Cologne Traffic Simulation — Interactive Map Generator
=======================================================

Generates one interactive Folium HTML map per scenario with togglable layers:
  1. Traffic Volume   — link width/color by daily vehicle count
  2. Congestion (V/C) — volume-to-capacity ratio (green → red)
  3. Speed Ratio      — actual speed vs free-flow (green → red)
  4. Peak Hour V/C    — congestion during AM peak (07:00–09:00) only

Each map includes a floating KPI summary panel and a color legend.

Usage:
    python generate_scenario_maps.py
"""

import math
import os
import sys

import folium
import numpy as np
from branca.element import MacroElement, Template
from pyproj import Transformer

# ---------------------------------------------------------------------------
# Import the simulation module from the same directory
# ---------------------------------------------------------------------------
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import cologne_simulation as sim

# UTM 32N (EPSG:25832) → WGS84
_transformer = Transformer.from_crs("EPSG:25832", "EPSG:4326", always_xy=True)

COLOGNE_CENTER_LAT = 50.9375
COLOGNE_CENTER_LON = 6.9603

# Peak-hour time bins (07:00–09:00 in 15-min bins)
PEAK_BIN_START = int(7 * 3600 / sim.TIME_BIN_SIZE)   # bin 28
PEAK_BIN_END   = int(9 * 3600 / sim.TIME_BIN_SIZE)   # bin 36


def utm_to_latlon(x: float, y: float):
    """Convert UTM 32N to (lat, lon)."""
    lon, lat = _transformer.transform(x, y)
    return lat, lon


# ---------------------------------------------------------------------------
# Color scales
# ---------------------------------------------------------------------------

def volume_color(vol: float, max_vol: float) -> str:
    """Blue (low) → Yellow (mid) → Red (high)."""
    if max_vol <= 0:
        return "#3388ff"
    t = min(vol / max_vol, 1.0)
    if t < 0.5:
        s = t / 0.5
        r = int(30 + 225 * s)
        g = int(100 + 155 * s)
        b = int(255 - 200 * s)
    else:
        s = (t - 0.5) / 0.5
        r = int(255)
        g = int(255 - 255 * s)
        b = int(55 - 55 * s)
    return f"#{r:02x}{g:02x}{b:02x}"


def vc_color(vc: float) -> str:
    """Green (free-flow) → Yellow → Orange → Red (congested)."""
    if vc < 0.4:
        return "#2ecc71"   # green
    elif vc < 0.6:
        return "#82e0aa"   # light green
    elif vc < 0.75:
        return "#f4d03f"   # yellow
    elif vc < 0.9:
        return "#f39c12"   # orange
    elif vc < 1.0:
        return "#e74c3c"   # red
    else:
        return "#8b0000"   # dark red (over capacity)


def speed_ratio_color(ratio: float) -> str:
    """Green (near free-flow) → Red (heavily delayed)."""
    # ratio = actual_speed / free_flow_speed (1.0 = free-flow, 0 = stopped)
    if ratio > 0.9:
        return "#2ecc71"
    elif ratio > 0.75:
        return "#82e0aa"
    elif ratio > 0.6:
        return "#f4d03f"
    elif ratio > 0.4:
        return "#f39c12"
    elif ratio > 0.2:
        return "#e74c3c"
    else:
        return "#8b0000"


# ---------------------------------------------------------------------------
# Per-link metric computation
# ---------------------------------------------------------------------------

def compute_link_metrics(network: sim.CologneNetwork):
    """Compute per-link daily and peak-hour metrics from smoothed flows."""
    metrics = {}
    for lid, link in network.links.items():
        cap_per_bin = link.capacity * link.lanes * (sim.TIME_BIN_SIZE / 3600.0)
        cap_per_bin = max(cap_per_bin, 1)

        daily_vol = float(np.sum(link.smoothed_flows))
        daily_vc = float(np.max(link.smoothed_flows / cap_per_bin))

        # Average actual travel time vs free-flow
        nonzero_bins = link.smoothed_flows > 0
        if np.any(nonzero_bins):
            avg_tt = float(np.mean(link.bin_travel_times[nonzero_bins]))
        else:
            avg_tt = link.free_flow_time
        speed_ratio = link.free_flow_time / avg_tt if avg_tt > 0 else 1.0

        # Peak hour (07:00–09:00)
        peak_flows = link.smoothed_flows[PEAK_BIN_START:PEAK_BIN_END]
        peak_vc = float(np.max(peak_flows / cap_per_bin)) if len(peak_flows) > 0 else 0.0

        from_node = network.nodes[link.from_node]
        to_node = network.nodes[link.to_node]
        start_ll = utm_to_latlon(from_node.x, from_node.y)
        end_ll = utm_to_latlon(to_node.x, to_node.y)

        metrics[lid] = {
            "start": start_ll,
            "end": end_ll,
            "daily_vol": daily_vol,
            "daily_vc": daily_vc,
            "speed_ratio": speed_ratio,
            "peak_vc": peak_vc,
            "length_m": link.length,
            "capacity": link.capacity * link.lanes,
            "freespeed_kmh": link.freespeed * 3.6,
            "lanes": link.lanes,
        }
    return metrics


# ---------------------------------------------------------------------------
# Legend HTML
# ---------------------------------------------------------------------------

def make_legend_html(layer_name: str, items: list) -> str:
    """Build HTML for a small floating legend box."""
    rows = ""
    for color, label in items:
        rows += (
            f'<div style="display:flex;align-items:center;margin:2px 0;">'
            f'<span style="background:{color};width:28px;height:6px;'
            f'display:inline-block;margin-right:6px;border-radius:2px;"></span>'
            f'<span style="font-size:11px;">{label}</span></div>'
        )
    return (
        f'<div style="position:fixed;bottom:30px;left:10px;z-index:1000;'
        f'background:rgba(255,255,255,0.92);padding:10px 14px;border-radius:6px;'
        f'box-shadow:0 1px 6px rgba(0,0,0,0.3);font-family:Arial,sans-serif;'
        f'max-width:180px;">'
        f'<div style="font-weight:bold;font-size:12px;margin-bottom:4px;">{layer_name}</div>'
        f'{rows}</div>'
    )


# ---------------------------------------------------------------------------
# KPI panel
# ---------------------------------------------------------------------------

def make_kpi_panel(kpi: sim.ScenarioKPI, scenario_name: str) -> str:
    return (
        f'<div style="position:fixed;top:10px;right:10px;z-index:1000;'
        f'background:rgba(255,255,255,0.94);padding:14px 18px;border-radius:8px;'
        f'box-shadow:0 2px 8px rgba(0,0,0,0.25);font-family:Arial,sans-serif;'
        f'max-width:260px;">'
        f'<div style="font-weight:bold;font-size:14px;margin-bottom:8px;'
        f'border-bottom:2px solid #2c3e50;padding-bottom:4px;">{scenario_name}</div>'
        f'<table style="font-size:12px;border-collapse:collapse;width:100%;">'
        f'<tr><td style="padding:2px 8px 2px 0;color:#555;">Avg Travel Time</td>'
        f'<td style="text-align:right;font-weight:bold;">{kpi.avg_travel_time_min:.2f} min</td></tr>'
        f'<tr><td style="padding:2px 8px 2px 0;color:#555;">P95 Travel Time</td>'
        f'<td style="text-align:right;font-weight:bold;">{kpi.p95_travel_time_min:.2f} min</td></tr>'
        f'<tr><td style="padding:2px 8px 2px 0;color:#555;">Peak Hour Avg TT</td>'
        f'<td style="text-align:right;font-weight:bold;">{kpi.peak_hour_avg_travel_time_min:.2f} min</td></tr>'
        f'<tr><td style="padding:2px 8px 2px 0;color:#555;">VKT</td>'
        f'<td style="text-align:right;font-weight:bold;">{kpi.vkt_km:,.0f} km</td></tr>'
        f'<tr><td style="padding:2px 8px 2px 0;color:#555;">VHT</td>'
        f'<td style="text-align:right;font-weight:bold;">{kpi.vht_hours:,.1f} hrs</td></tr>'
        f'<tr><td style="padding:2px 8px 2px 0;color:#555;">Trips</td>'
        f'<td style="text-align:right;font-weight:bold;">{kpi.num_trips:,}</td></tr>'
        f'<tr><td style="padding:2px 8px 2px 0;color:#555;">Agents</td>'
        f'<td style="text-align:right;font-weight:bold;">{kpi.num_agents:,}</td></tr>'
        f'</table></div>'
    )


# ---------------------------------------------------------------------------
# Build a single scenario map
# ---------------------------------------------------------------------------

def build_scenario_map(
    scenario_name: str,
    network: sim.CologneNetwork,
    kpi: sim.ScenarioKPI,
    link_metrics: dict,
) -> folium.Map:
    """Build an interactive Folium map with four togglable layers."""

    m = folium.Map(
        location=[COLOGNE_CENTER_LAT, COLOGNE_CENTER_LON],
        zoom_start=12,
        tiles="CartoDB positron",
    )

    max_vol = max((v["daily_vol"] for v in link_metrics.values()), default=1)

    # --- Layer 1: Traffic Volume ---
    fg_vol = folium.FeatureGroup(name="Traffic Volume (Daily)", show=True)
    for lid, met in link_metrics.items():
        if met["daily_vol"] < 1:
            continue
        weight = max(1.5, min(8, 1.5 + 6.5 * (met["daily_vol"] / max_vol)))
        color = volume_color(met["daily_vol"], max_vol)
        tooltip = (f"<b>Link {lid}</b><br>"
                   f"Volume: {met['daily_vol']:.0f} veh/day<br>"
                   f"Capacity: {met['capacity']:.0f} veh/hr<br>"
                   f"Free speed: {met['freespeed_kmh']:.0f} km/h<br>"
                   f"Lanes: {met['lanes']}")
        folium.PolyLine(
            [met["start"], met["end"]],
            weight=weight, color=color, opacity=0.8,
            tooltip=tooltip,
        ).add_to(fg_vol)
    fg_vol.add_to(m)

    # --- Layer 2: Congestion (V/C Ratio) ---
    fg_vc = folium.FeatureGroup(name="Congestion (V/C Ratio)", show=False)
    for lid, met in link_metrics.items():
        if met["daily_vol"] < 1:
            continue
        color = vc_color(met["daily_vc"])
        weight = max(2, min(7, 2 + 5 * met["daily_vc"]))
        tooltip = (f"<b>Link {lid}</b><br>"
                   f"V/C Ratio: {met['daily_vc']:.2f}<br>"
                   f"Volume: {met['daily_vol']:.0f} veh/day<br>"
                   f"Capacity: {met['capacity']:.0f} veh/hr")
        folium.PolyLine(
            [met["start"], met["end"]],
            weight=weight, color=color, opacity=0.8,
            tooltip=tooltip,
        ).add_to(fg_vc)
    fg_vc.add_to(m)

    # --- Layer 3: Speed Performance ---
    fg_speed = folium.FeatureGroup(name="Speed vs Free-Flow", show=False)
    for lid, met in link_metrics.items():
        if met["daily_vol"] < 1:
            continue
        color = speed_ratio_color(met["speed_ratio"])
        weight = max(2, min(7, 2 + 5 * (1 - met["speed_ratio"])))
        actual_speed = met["freespeed_kmh"] * met["speed_ratio"]
        tooltip = (f"<b>Link {lid}</b><br>"
                   f"Speed: {actual_speed:.1f} / {met['freespeed_kmh']:.0f} km/h "
                   f"({met['speed_ratio']*100:.0f}%)<br>"
                   f"Volume: {met['daily_vol']:.0f} veh/day")
        folium.PolyLine(
            [met["start"], met["end"]],
            weight=weight, color=color, opacity=0.8,
            tooltip=tooltip,
        ).add_to(fg_speed)
    fg_speed.add_to(m)

    # --- Layer 4: Peak Hour V/C ---
    fg_peak = folium.FeatureGroup(name="AM Peak Congestion (07-09)", show=False)
    for lid, met in link_metrics.items():
        if met["daily_vol"] < 1:
            continue
        color = vc_color(met["peak_vc"])
        weight = max(2, min(7, 2 + 5 * met["peak_vc"]))
        tooltip = (f"<b>Link {lid}</b><br>"
                   f"Peak V/C: {met['peak_vc']:.2f}<br>"
                   f"Capacity: {met['capacity']:.0f} veh/hr")
        folium.PolyLine(
            [met["start"], met["end"]],
            weight=weight, color=color, opacity=0.8,
            tooltip=tooltip,
        ).add_to(fg_peak)
    fg_peak.add_to(m)

    # Layer control toggle
    folium.LayerControl(collapsed=False).add_to(m)

    # KPI panel
    kpi_html = make_kpi_panel(kpi, scenario_name)
    m.get_root().html.add_child(folium.Element(kpi_html))

    # Legend (for default volume layer)
    legend_items = [
        ("#1e64ff", "Low"),
        ("#ffff37", "Medium"),
        ("#ff6400", "High"),
        ("#ff0000", "Very High"),
    ]
    legend_html = make_legend_html("Traffic Volume", legend_items)
    m.get_root().html.add_child(folium.Element(legend_html))

    return m


# ---------------------------------------------------------------------------
# Modified run_scenario that returns network + kpi + trips
# ---------------------------------------------------------------------------

def run_scenario_with_network(
    scenario_name: str,
    coordination_fraction: float,
    num_agents: int = sim.NUM_AGENTS,
    num_iterations: int = sim.NUM_ITERATIONS,
    network_mode: str = "synthetic",
):
    """Run a scenario and return (network, kpi, trips) for visualization."""
    print(f"\n{'='*70}")
    print(f"  SCENARIO: {scenario_name}")
    print(f"  Network: {network_mode}")
    print(f"  Coordination: {coordination_fraction*100:.0f}% of agents")
    print(f"{'='*70}")

    network = sim.CologneNetwork()
    if network_mode == "real":
        nodes_path = os.path.join(sim.DATA_DIR, "nodes.csv")
        links_path = os.path.join(sim.DATA_DIR, "links.csv")
        if not os.path.exists(nodes_path) or not os.path.exists(links_path):
            print(f"  ERROR: Real network data not found at {sim.DATA_DIR}")
            print(f"  Run download_cologne_data.py first.")
            sys.exit(1)
        print("\n  Loading real Cologne network...")
        network.load_from_csv(nodes_path, links_path)
    else:
        print("\n  Building synthetic Cologne network...")
        network.build()

    print("  Generating population...")
    persons = sim.generate_population(network, num_agents)

    if coordination_fraction > 0:
        modified = sim.apply_coordination(persons, coordination_fraction)
        print(f"  Coordination applied: {modified} agents modified")

    import random
    rng = random.Random(sim.RANDOM_SEED + 1000)
    all_trip_records = []
    AVG_LAST_N = 5

    for iteration in range(num_iterations):
        trips = sim.simulate_traffic_parallel(network, persons)
        network.update_travel_times_msa(iteration)

        for person in persons:
            sim.score_plan(person.selected_plan)

        if iteration >= num_iterations - AVG_LAST_N:
            all_trip_records.extend(trips)

        avg_tt = np.mean([t.arrival_time - t.departure_time for t in trips]) / 60 if trips else 0
        if iteration % 5 == 0 or iteration == num_iterations - 1:
            print(f"  Iter {iteration:3d}/{num_iterations}: avg_tt={avg_tt:6.2f}min  trips={len(trips)}")

        if iteration < num_iterations - 1:
            sim.replan(network, persons, iteration, num_iterations, rng)

    kpi = sim.compute_kpis(scenario_name, all_trip_records, num_agents)
    kpi.num_trips = len(all_trip_records) // AVG_LAST_N
    kpi.vkt_km /= AVG_LAST_N
    kpi.vht_hours /= AVG_LAST_N

    return network, kpi, all_trip_records


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    import argparse
    import time as time_mod

    parser = argparse.ArgumentParser(description="Cologne Traffic Simulation — Map Generator")
    parser.add_argument("--network", choices=["synthetic", "real"], default="synthetic",
                        help="Network source: 'synthetic' (built-in) or 'real' (OSM data)")
    parser.add_argument("--agents", type=int, default=sim.NUM_AGENTS,
                        help=f"Number of agents (default: {sim.NUM_AGENTS})")
    parser.add_argument("--iterations", type=int, default=sim.NUM_ITERATIONS,
                        help=f"Iterations per scenario (default: {sim.NUM_ITERATIONS})")
    args = parser.parse_args()

    overall_start = time_mod.time()

    print("=" * 70)
    print("  COLOGNE TRAFFIC SIMULATION — MAP GENERATOR")
    print("=" * 70)
    print(f"  Network: {args.network}")
    print(f"  Agents: {args.agents}")
    print(f"  Iterations: {args.iterations}")

    output_dir = os.path.join(
        os.path.dirname(os.path.abspath(__file__)),
        "..", "..", "..", "scenarios", "cologne", "output", "maps"
    )
    os.makedirs(output_dir, exist_ok=True)

    scenarios = [
        ("Baseline (0%)", 0.00),
        ("Coordinated 1%", 0.01),
        ("Coordinated 2%", 0.02),
        ("Coordinated 5%", 0.05),
        ("Coordinated 10%", 0.10),
    ]

    all_kpis = []

    for scenario_name, fraction in scenarios:
        network, kpi, trips = run_scenario_with_network(
            scenario_name, fraction,
            num_agents=args.agents, num_iterations=args.iterations,
            network_mode=args.network,
        )
        all_kpis.append(kpi)

        print(f"\n  Generating map for {scenario_name}...")
        link_metrics = compute_link_metrics(network)

        m = build_scenario_map(scenario_name, network, kpi, link_metrics)

        safe_name = scenario_name.lower().replace(" ", "_").replace("(", "").replace(")", "").replace("%", "pct")
        filepath = os.path.join(output_dir, f"{safe_name}.html")
        m.save(filepath)
        print(f"  Saved: {filepath}")

    # Also regenerate the KPI comparison files
    table = sim.format_comparison_table(all_kpis)
    print(table)

    parent_dir = os.path.dirname(output_dir)
    sim.write_csv(all_kpis, os.path.join(parent_dir, "kpi_comparison.csv"))
    with open(os.path.join(parent_dir, "kpi_comparison.txt"), 'w') as f:
        f.write(table)

    # Generate an index page linking to all scenario maps
    index_html = build_index_page(scenarios, all_kpis)
    index_path = os.path.join(output_dir, "index.html")
    with open(index_path, 'w') as f:
        f.write(index_html)
    print(f"\n  Index page: {index_path}")

    elapsed = time_mod.time() - overall_start
    print(f"\n  Total time: {elapsed:.0f}s ({elapsed/60:.1f}min)")
    print("  Done. Open the index.html in a browser to view all maps.")


def build_index_page(scenarios, all_kpis):
    """Build a simple HTML index linking to all per-scenario maps."""
    rows = ""
    baseline = all_kpis[0] if all_kpis else None

    for i, ((name, frac), kpi) in enumerate(zip(scenarios, all_kpis)):
        safe_name = name.lower().replace(" ", "_").replace("(", "").replace(")", "").replace("%", "pct")

        delta_tt = ""
        delta_peak = ""
        if i > 0 and baseline:
            d_tt = (kpi.avg_travel_time_min - baseline.avg_travel_time_min) / baseline.avg_travel_time_min * 100
            d_pk = (kpi.peak_hour_avg_travel_time_min - baseline.peak_hour_avg_travel_time_min) / baseline.peak_hour_avg_travel_time_min * 100
            delta_tt = f'<span style="color:{"#27ae60" if d_tt < 0 else "#e74c3c"};font-weight:bold;">{d_tt:+.2f}%</span>'
            delta_peak = f'<span style="color:{"#27ae60" if d_pk < 0 else "#e74c3c"};font-weight:bold;">{d_pk:+.2f}%</span>'
        else:
            delta_tt = '<span style="color:#888;">—</span>'
            delta_peak = '<span style="color:#888;">—</span>'

        rows += f"""
        <tr>
            <td><a href="{safe_name}.html" style="color:#2980b9;font-weight:bold;text-decoration:none;">{name}</a></td>
            <td>{kpi.avg_travel_time_min:.2f} min</td>
            <td>{delta_tt}</td>
            <td>{kpi.peak_hour_avg_travel_time_min:.2f} min</td>
            <td>{delta_peak}</td>
            <td>{kpi.p95_travel_time_min:.2f} min</td>
            <td>{kpi.vkt_km:,.0f} km</td>
            <td>{kpi.vht_hours:,.1f} hrs</td>
            <td>{kpi.num_trips:,}</td>
        </tr>"""

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Cologne Traffic Simulation — Scenario Maps</title>
<style>
    body {{ font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px 40px; background: #f5f6fa; color: #2c3e50; }}
    h1 {{ font-size: 24px; border-bottom: 3px solid #2c3e50; padding-bottom: 10px; }}
    h2 {{ font-size: 18px; color: #34495e; margin-top: 30px; }}
    p.subtitle {{ color: #7f8c8d; margin-top: -10px; font-size: 14px; }}
    table {{ border-collapse: collapse; width: 100%; margin: 20px 0; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.1); }}
    th {{ background: #2c3e50; color: white; padding: 12px 16px; text-align: left; font-size: 13px; }}
    td {{ padding: 10px 16px; border-bottom: 1px solid #ecf0f1; font-size: 13px; }}
    tr:hover {{ background: #f0f3f7; }}
    .note {{ background: #eaf2f8; border-left: 4px solid #2980b9; padding: 12px 16px; margin: 20px 0; border-radius: 0 6px 6px 0; font-size: 13px; }}
    .layers {{ display: flex; gap: 16px; flex-wrap: wrap; margin: 16px 0; }}
    .layer-card {{ background: white; border-radius: 6px; padding: 12px 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); flex: 1; min-width: 200px; }}
    .layer-card h3 {{ margin: 0 0 4px 0; font-size: 14px; color: #2c3e50; }}
    .layer-card p {{ margin: 0; font-size: 12px; color: #7f8c8d; }}
</style>
</head>
<body>
<h1>Cologne Traffic Simulation — Scenario Comparison</h1>
<p class="subtitle">Agent-Based Microsimulation | Departure Time Coordination Scenarios | {all_kpis[0].num_agents:,} Agents</p>

<h2>Per-Scenario Interactive Maps</h2>
<p>Click a scenario name to open its interactive map. Each map has four togglable layers:</p>

<div class="layers">
    <div class="layer-card">
        <h3>Traffic Volume</h3>
        <p>Link width and color by daily vehicle count. Blue (low) → Red (high).</p>
    </div>
    <div class="layer-card">
        <h3>Congestion (V/C)</h3>
        <p>Volume-to-Capacity ratio. Green (free-flow) → Red (over capacity).</p>
    </div>
    <div class="layer-card">
        <h3>Speed vs Free-Flow</h3>
        <p>Actual speed as percentage of free-flow. Green (fast) → Red (slow).</p>
    </div>
    <div class="layer-card">
        <h3>AM Peak Congestion</h3>
        <p>V/C ratio during 07:00–09:00 AM peak only.</p>
    </div>
</div>

<table>
<thead>
<tr>
    <th>Scenario</th>
    <th>Avg TT</th>
    <th>vs Baseline</th>
    <th>Peak TT</th>
    <th>vs Baseline</th>
    <th>P95 TT</th>
    <th>VKT</th>
    <th>VHT</th>
    <th>Trips</th>
</tr>
</thead>
<tbody>
{rows}
</tbody>
</table>

<div class="note">
    <strong>How to read the maps:</strong> Use the layer control (top-right of each map) to toggle between
    Traffic Volume, Congestion, Speed, and Peak Hour views. Hover over any link to see detailed metrics.
    The KPI summary panel (top-right corner) shows aggregate scenario statistics.
</div>

</body>
</html>"""


if __name__ == "__main__":
    main()
