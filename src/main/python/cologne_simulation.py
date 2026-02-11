#!/usr/bin/env python3
"""
Cologne (Köln) Agent-Based Traffic Microsimulation
====================================================

A self-contained, MATSim-equivalent traffic simulation for Cologne, Germany.

Implements the core MATSim simulation loop:
1. Network loading (synthetic Cologne network)
2. Population generation (synthetic commuters)
3. Mobility simulation (mesoscopic queue-based traffic flow with time-dependent BPR)
4. Scoring (utility-based plan evaluation)
5. Replanning (route reassignment via shortest path on experienced travel times)
6. Iteration until convergence

Coordination logic:
- Baseline: no intervention
- Coordinated: X% of agents shift departure times away from peak (7-9 AM)
  - 50% shift earlier by 45-90 min, 50% shift later by 45-90 min
  - Represents a voluntary "staggered work hours" policy

KPIs extracted:
- Average travel time, P95 travel time, Peak-hour average travel time
- Vehicle Kilometers Traveled (VKT), Vehicle Hours Traveled (VHT)
- Number of trips, Number of agents

Author: Claude (Anthropic) — MATSim Cologne Scenario
Coordinate system: EPSG:25832 (UTM zone 32N)
"""

import argparse
import heapq
import sys
import math
import random
import time as time_mod
import os
import csv
from collections import defaultdict
from concurrent.futures import ProcessPoolExecutor
from dataclasses import dataclass, field
from multiprocessing import cpu_count
from typing import Dict, List, Optional, Tuple

import numpy as np

# Default path for real OSM data (written by download_cologne_data.py)
DATA_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "..", "..", "scenarios", "cologne", "data"
)

# ============================================================================
# CONFIGURATION
# ============================================================================

NUM_AGENTS = 20000
NUM_ITERATIONS = 25
RANDOM_SEED = 42

# Cologne center in UTM 32N
CENTER_X = 356000.0
CENTER_Y = 5645000.0
RHINE_X = CENTER_X + 500.0

# BPR (Bureau of Public Roads) function parameters:
# t = t_free * (1 + ALPHA * (flow/capacity)^BETA)
# Using higher alpha (0.5) for urban network with signalized intersections
BPR_ALPHA = 0.50
BPR_BETA = 4.0

# Time bins for flow tracking (15-minute bins)
TIME_BIN_SIZE = 900  # 15 minutes in seconds
NUM_TIME_BINS = 96   # 24 hours / 15 min = 96 bins

# Scoring parameters (MATSim-compatible)
PERFORMING_UTILS_HR = 6.0
TRAVELING_UTILS_HR = -6.0
MONETARY_DISTANCE_RATE = -0.0002

# Activity parameters
HOME_TYPICAL_DURATION = 12 * 3600
WORK_TYPICAL_DURATION = 8 * 3600

# Replanning
FRACTION_REROUTE = 0.15
FRACTION_TIME_MUTATE = 0.05
TIME_MUTATION_RANGE = 1800
PLAN_MEMORY_SIZE = 5
INNOVATION_DISABLE_FRACTION = 0.8

# Departure time distribution
MEAN_DEPARTURE = 7.5 * 3600
STD_DEPARTURE = 45 * 60

# Coordination parameters
PEAK_START = 7 * 3600
PEAK_END = 9 * 3600
MIN_SHIFT = 45 * 60
MAX_SHIFT = 90 * 60


# ============================================================================
# DATA STRUCTURES
# ============================================================================

@dataclass
class Node:
    id: str
    x: float
    y: float


@dataclass
class Link:
    id: str
    from_node: str
    to_node: str
    length: float
    freespeed: float
    capacity: float     # vehicles per hour (per direction)
    lanes: int
    # Runtime: time-binned flows and travel times
    bin_flows: np.ndarray = field(default_factory=lambda: np.zeros(NUM_TIME_BINS))
    bin_travel_times: np.ndarray = field(default_factory=lambda: np.zeros(NUM_TIME_BINS))
    smoothed_flows: np.ndarray = field(default_factory=lambda: np.zeros(NUM_TIME_BINS))

    @property
    def free_flow_time(self):
        return self.length / self.freespeed if self.freespeed > 0 else float('inf')

    def get_travel_time(self, time_sec: float) -> float:
        """Get travel time for a specific departure time."""
        b = int(time_sec / TIME_BIN_SIZE) % NUM_TIME_BINS
        return self.bin_travel_times[b]


@dataclass
class Activity:
    type: str
    link_id: str
    end_time: Optional[float] = None


@dataclass
class Leg:
    mode: str = "car"
    route: List[str] = field(default_factory=list)
    departure_time: float = 0.0
    arrival_time: float = 0.0
    distance: float = 0.0


@dataclass
class Plan:
    activities: List[Activity] = field(default_factory=list)
    legs: List[Leg] = field(default_factory=list)
    score: float = float('-inf')


@dataclass
class Person:
    id: str
    plans: List[Plan] = field(default_factory=list)
    selected_plan_idx: int = 0

    @property
    def selected_plan(self):
        return self.plans[self.selected_plan_idx]


@dataclass
class TripRecord:
    person_id: str
    departure_time: float
    arrival_time: float
    distance: float
    origin_link: str
    dest_link: str


# ============================================================================
# NETWORK GENERATION
# ============================================================================

class CologneNetwork:
    """
    Generates a synthetic road network for Cologne.

    Key design: reduced link capacities in the core to create realistic
    bottleneck congestion that responds to departure time coordination.
    """

    def __init__(self):
        self.nodes: Dict[str, Node] = {}
        self.links: Dict[str, Link] = {}
        self.adjacency: Dict[str, List[str]] = defaultdict(list)
        self.link_counter = 0

    def build(self):
        INNER_R = 1200
        MIDDLE_R = 3000
        OUTER_R = 5500
        AUTOBAHN_R = 9000

        left_angles = [70, 110, 150, 180, 210, 250, 290, 330]

        self._add_node("center", CENTER_X, CENTER_Y)

        # Left bank rings
        inner_left = self._create_ring("il", INNER_R, left_angles, CENTER_X, CENTER_Y)
        middle_left = self._create_ring("ml", MIDDLE_R, left_angles, CENTER_X, CENTER_Y)
        outer_left = self._create_ring("ol", OUTER_R, left_angles, CENTER_X, CENTER_Y)

        # Right bank
        right_cx, right_cy = RHINE_X + 1000, CENTER_Y
        right_angles = [330, 0, 30, 60]
        self._add_node("deutz", right_cx, right_cy)
        inner_right = self._create_ring("ir", 1500, right_angles, right_cx, right_cy)
        middle_right = self._create_ring("mr", 3500, right_angles, right_cx, right_cy)
        outer_right = self._create_ring("or_r", 6000, right_angles, right_cx, right_cy)

        ab_angles = list(range(0, 360, 30))
        autobahn = self._create_ring("ab", AUTOBAHN_R, ab_angles, CENTER_X, CENTER_Y)

        # --- LINKS ---
        # Inner city: realistic bottleneck capacities for a congested urban core
        # With 20k agents, ~12k heading to center during peak → must funnel through
        # 8 radial directions with ~1500 veh/direction/peak-hour
        self._radial_links("center", inner_left, 8.33, 150, 1)          # 30 km/h, ~150 veh/h (narrow downtown)
        self._ring_to_ring_links(inner_left, middle_left, 11.11, 250, 1) # 40 km/h
        self._ring_to_ring_links(middle_left, outer_left, 13.89, 500, 1) # 50 km/h

        self._ring_links(inner_left, 8.33, 200, 1)                       # Inner ring
        self._ring_links(middle_left, 11.11, 400, 1)
        self._ring_links(outer_left, 13.89, 800, 2)

        # Right bank — similar bottleneck structure
        self._radial_links("deutz", inner_right, 8.33, 150, 1)
        self._ring_to_ring_links(inner_right, middle_right, 11.11, 250, 1)
        self._ring_to_ring_links(middle_right, outer_right, 13.89, 500, 1)

        self._ring_links(inner_right, 8.33, 200, 1)
        self._ring_links(middle_right, 11.11, 400, 1)
        self._ring_links(outer_right, 13.89, 800, 2)

        # Autobahn: high capacity but still has limits
        self._ring_links(autobahn, 33.33, 2000, 3)

        self._connect_nearest(outer_left, autobahn, 16.67, 600, 2, max_dist=8000)
        self._connect_nearest(outer_right, autobahn, 16.67, 600, 2, max_dist=8000)

        # Bridges: MAJOR BOTTLENECK — key capacity constraint
        # Cologne's Rhine bridges are notorious bottlenecks
        bridges = [
            ("bw1", RHINE_X - 100, CENTER_Y + 1500, "be1", RHINE_X + 100, CENTER_Y + 1500),
            ("bw2", RHINE_X - 100, CENTER_Y + 300, "be2", RHINE_X + 100, CENTER_Y + 300),
            ("bw3", RHINE_X - 100, CENTER_Y - 500, "be3", RHINE_X + 100, CENTER_Y - 500),
            ("bw4", RHINE_X - 100, CENTER_Y - 2000, "be4", RHINE_X + 100, CENTER_Y - 2000),
        ]
        for bw_id, bw_x, bw_y, be_id, be_x, be_y in bridges:
            self._add_node(bw_id, bw_x, bw_y)
            self._add_node(be_id, be_x, be_y)
            self._add_bidir_link(bw_id, be_id, 11.11, 400, 2)  # Bridge: 400 veh/h

        bridge_west = ["bw1", "bw2", "bw3", "bw4"]
        bridge_east = ["be1", "be2", "be3", "be4"]
        all_left = inner_left + middle_left + outer_left + ["center"]
        all_right = inner_right + middle_right + outer_right + ["deutz"]

        for bw in bridge_west:
            self._connect_nearest_n([bw], all_left, 11.11, 300, 1, n=2)
        for be in bridge_east:
            self._connect_nearest_n([be], all_right, 11.11, 300, 1, n=2)

        # Local street grid — low capacity, creates realistic congestion
        rng = random.Random(42)
        grid_nodes = []
        spacing = 800
        for gx in np.arange(CENTER_X - 4000, CENTER_X - 200, spacing):
            for gy in np.arange(CENTER_Y - 4000, CENTER_Y + 4000, spacing):
                dist = math.sqrt((gx - CENTER_X)**2 + (gy - CENTER_Y)**2)
                if dist < OUTER_R and gx < RHINE_X - 200:
                    nid = f"g{len(grid_nodes)}"
                    self._add_node(nid, gx + rng.uniform(-50, 50), gy + rng.uniform(-50, 50))
                    grid_nodes.append(nid)

        for i in range(len(grid_nodes)):
            for j in range(i+1, len(grid_nodes)):
                d = self._dist(grid_nodes[i], grid_nodes[j])
                if d < spacing * 1.5:
                    self._add_bidir_link(grid_nodes[i], grid_nodes[j], 8.33, 150, 1)

        for gn in grid_nodes:
            self._connect_nearest_n([gn], all_left, 8.33, 200, 1, n=1)

        # Right bank grid
        grid_right = []
        for gx in np.arange(RHINE_X + 500, RHINE_X + 5000, spacing * 1.5):
            for gy in np.arange(CENTER_Y - 3000, CENTER_Y + 3000, spacing * 1.5):
                dist = math.sqrt((gx - right_cx)**2 + (gy - right_cy)**2)
                if dist < 5500:
                    nid = f"gr{len(grid_right)}"
                    self._add_node(nid, gx + rng.uniform(-50, 50), gy + rng.uniform(-50, 50))
                    grid_right.append(nid)

        for i in range(len(grid_right)):
            for j in range(i+1, len(grid_right)):
                d = self._dist(grid_right[i], grid_right[j])
                if d < spacing * 2.0:
                    self._add_bidir_link(grid_right[i], grid_right[j], 8.33, 150, 1)

        for gn in grid_right:
            self._connect_nearest_n([gn], all_right, 8.33, 200, 1, n=1)

        # Initialize travel times to free-flow
        for link in self.links.values():
            link.bin_travel_times[:] = link.free_flow_time

        print(f"  Network: {len(self.nodes)} nodes, {len(self.links)} links")
        return self

    def load_from_csv(self, nodes_path: str, links_path: str):
        """
        Load a real road network from CSV files produced by download_cologne_data.py.

        Files expected:
          nodes.csv — id, x_utm, y_utm, lat, lon
          links.csv — id, from_node, to_node, length_m, freespeed_ms,
                       capacity_veh_hr, lanes, highway, name, osm_way_id

        After loading, removes disconnected components to keep only the
        largest strongly-connected component (ensures all OD pairs are routable).
        """
        print(f"  Loading nodes from {nodes_path} ...")
        with open(nodes_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                nid = str(row["id"])
                self.nodes[nid] = Node(nid, float(row["x_utm"]), float(row["y_utm"]))

        print(f"  Loading links from {links_path} ...")
        with open(links_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                lid = str(row["id"])
                from_node = str(row["from_node"])
                to_node = str(row["to_node"])

                # Skip links referencing missing nodes
                if from_node not in self.nodes or to_node not in self.nodes:
                    continue

                link = Link(
                    id=lid,
                    from_node=from_node,
                    to_node=to_node,
                    length=float(row["length_m"]),
                    freespeed=float(row["freespeed_ms"]),
                    capacity=float(row["capacity_veh_hr"]),
                    lanes=int(row["lanes"]),
                )
                self.links[lid] = link
                self.adjacency[from_node].append(lid)

        print(f"  Raw network: {len(self.nodes):,} nodes, {len(self.links):,} links")

        # Extract the largest weakly-connected component so all OD pairs are
        # routable.  A full SCC is expensive on large graphs; weakly-connected
        # (treating edges as undirected) is a good practical approximation.
        self._keep_largest_component()

        # Initialize travel times to free-flow
        for link in self.links.values():
            link.bin_travel_times[:] = link.free_flow_time

        print(f"  Network: {len(self.nodes):,} nodes, {len(self.links):,} links")
        return self

    def _keep_largest_component(self):
        """Keep only the largest weakly-connected component."""
        # Build undirected adjacency
        adj = defaultdict(set)
        for link in self.links.values():
            adj[link.from_node].add(link.to_node)
            adj[link.to_node].add(link.from_node)

        # BFS to find components
        visited = set()
        components = []
        for nid in self.nodes:
            if nid in visited:
                continue
            component = set()
            queue = [nid]
            while queue:
                n = queue.pop()
                if n in visited:
                    continue
                visited.add(n)
                component.add(n)
                for neighbor in adj.get(n, []):
                    if neighbor not in visited:
                        queue.append(neighbor)
            components.append(component)

        # Keep the largest
        largest = max(components, key=len)
        removed_nodes = set(self.nodes.keys()) - largest

        if removed_nodes:
            pct = len(removed_nodes) / (len(removed_nodes) + len(largest)) * 100
            print(f"  Removing {len(removed_nodes):,} disconnected nodes ({pct:.1f}%)")
            for nid in removed_nodes:
                del self.nodes[nid]
            # Remove links that reference removed nodes
            to_remove = [lid for lid, link in self.links.items()
                         if link.from_node not in largest or link.to_node not in largest]
            for lid in to_remove:
                del self.links[lid]
            # Rebuild adjacency
            self.adjacency = defaultdict(list)
            for lid, link in self.links.items():
                self.adjacency[link.from_node].append(lid)

    def _add_node(self, nid, x, y):
        self.nodes[nid] = Node(nid, x, y)

    def _create_ring(self, prefix, radius, angles, cx, cy):
        node_ids = []
        for i, angle in enumerate(angles):
            rad = math.radians(angle)
            x = cx + radius * math.cos(rad)
            y = cy + radius * math.sin(rad)
            nid = f"{prefix}{i}"
            self._add_node(nid, x, y)
            node_ids.append(nid)
        return node_ids

    def _add_bidir_link(self, from_id, to_id, freespeed, capacity, lanes):
        length = self._dist(from_id, to_id)
        if length < 10:
            length = 100

        self.link_counter += 1
        lid_ab = f"L{self.link_counter}"
        self.links[lid_ab] = Link(lid_ab, from_id, to_id, length, freespeed, capacity, lanes)
        self.adjacency[from_id].append(lid_ab)

        self.link_counter += 1
        lid_ba = f"L{self.link_counter}"
        self.links[lid_ba] = Link(lid_ba, to_id, from_id, length, freespeed, capacity, lanes)
        self.adjacency[to_id].append(lid_ba)

    def _dist(self, n1, n2):
        a, b = self.nodes[n1], self.nodes[n2]
        return math.sqrt((a.x - b.x)**2 + (a.y - b.y)**2)

    def _radial_links(self, center_id, ring_ids, freespeed, capacity, lanes):
        for rid in ring_ids:
            self._add_bidir_link(center_id, rid, freespeed, capacity, lanes)

    def _ring_to_ring_links(self, inner, outer, freespeed, capacity, lanes):
        for i in range(min(len(inner), len(outer))):
            self._add_bidir_link(inner[i], outer[i], freespeed, capacity, lanes)

    def _ring_links(self, ring_ids, freespeed, capacity, lanes):
        for i in range(len(ring_ids)):
            j = (i + 1) % len(ring_ids)
            self._add_bidir_link(ring_ids[i], ring_ids[j], freespeed, capacity, lanes)

    def _connect_nearest(self, sources, targets, freespeed, capacity, lanes, max_dist=float('inf')):
        for src in sources:
            best, best_d = None, float('inf')
            for tgt in targets:
                d = self._dist(src, tgt)
                if 10 < d < best_d and d < max_dist:
                    best, best_d = tgt, d
            if best:
                self._add_bidir_link(src, best, freespeed, capacity, lanes)

    def _connect_nearest_n(self, sources, targets, freespeed, capacity, lanes, n=2):
        for src in sources:
            dists = [(tgt, self._dist(src, tgt)) for tgt in targets if self._dist(src, tgt) > 10]
            dists.sort(key=lambda x: x[1])
            for tgt, _ in dists[:n]:
                self._add_bidir_link(src, tgt, freespeed, capacity, lanes)

    def reset_flows(self):
        """Reset current iteration flows (not the smoothed averages)."""
        for link in self.links.values():
            link.bin_flows[:] = 0.0

    def add_flow(self, link_id: str, departure_time: float):
        """Add a vehicle to the time-binned flow count."""
        b = int(departure_time / TIME_BIN_SIZE) % NUM_TIME_BINS
        self.links[link_id].bin_flows[b] += 1

    def update_travel_times_msa(self, iteration: int):
        """
        Update link travel times using BPR function with MSA flow smoothing.

        MSA (Method of Successive Averages):
          smoothed_flow(n) = (1/n) * current_flow + (1 - 1/n) * smoothed_flow(n-1)
        This dampens oscillation and ensures convergence to equilibrium.
        """
        weight = 1.0 / (iteration + 1)  # MSA weight

        for link in self.links.values():
            cap_per_bin = link.capacity * link.lanes * (TIME_BIN_SIZE / 3600.0)
            cap_per_bin = max(cap_per_bin, 1)

            # MSA smoothing: vectorized over all bins
            link.smoothed_flows = weight * link.bin_flows + (1 - weight) * link.smoothed_flows
            vol_cap = link.smoothed_flows / cap_per_bin
            link.bin_travel_times = link.free_flow_time * (1 + BPR_ALPHA * np.power(vol_cap, BPR_BETA))


# ============================================================================
# SHORTEST PATH (Time-dependent A*)
# ============================================================================

# Maximum free-speed in the network (autobahn ~120 km/h = 33.33 m/s).
# Used as the heuristic divisor so A* remains admissible.
_MAX_SPEED = 33.33


def astar_td(network: CologneNetwork, origin_node: str, dest_node: str,
             departure_time: float) -> Tuple[List[str], float, float]:
    """
    Time-dependent A* using Euclidean distance heuristic.
    Much faster than plain Dijkstra for point-to-point queries because the
    heuristic prunes large portions of the search space.
    Returns (route_link_ids, total_travel_time, total_distance).
    """
    dest = network.nodes[dest_node]
    dest_x, dest_y = dest.x, dest.y
    nodes = network.nodes

    g = {origin_node: 0.0}
    prev_link = {}
    visited = set()

    # Heuristic: Euclidean distance / max network speed (admissible lower bound)
    ox, oy = nodes[origin_node].x, nodes[origin_node].y
    h0 = math.sqrt((ox - dest_x) ** 2 + (oy - dest_y) ** 2) / _MAX_SPEED
    heap = [(h0, 0.0, origin_node)]  # (f=g+h, g, node)

    while heap:
        _f, d, u = heapq.heappop(heap)
        if u in visited:
            continue
        visited.add(u)

        if u == dest_node:
            path = []
            node = dest_node
            while node in prev_link:
                lid = prev_link[node]
                path.append(lid)
                node = network.links[lid].from_node
            path.reverse()
            total_dist = sum(network.links[lid].length for lid in path)
            return path, d, total_dist

        current_time = departure_time + d
        for lid in network.adjacency.get(u, []):
            link = network.links[lid]
            v = link.to_node
            if v in visited:
                continue
            tt = link.get_travel_time(current_time)
            new_g = d + tt
            if new_g < g.get(v, float('inf')):
                g[v] = new_g
                prev_link[v] = lid
                vn = nodes[v]
                h = math.sqrt((vn.x - dest_x) ** 2 + (vn.y - dest_y) ** 2) / _MAX_SPEED
                heapq.heappush(heap, (new_g + h, new_g, v))

    return [], float('inf'), 0.0


# ============================================================================
# POPULATION GENERATION
# ============================================================================

def generate_population(network: CologneNetwork, num_agents: int, seed: int = RANDOM_SEED) -> List[Person]:
    """Generate synthetic commuter population for Cologne."""
    rng = random.Random(seed)
    np_rng = np.random.RandomState(seed)

    all_links = list(network.links.keys())

    residential_links = []
    work_links = []
    center_links = []  # high-demand work area

    for lid, link in network.links.items():
        mid_x = (network.nodes[link.from_node].x + network.nodes[link.to_node].x) / 2
        mid_y = (network.nodes[link.from_node].y + network.nodes[link.to_node].y) / 2
        dist_center = math.sqrt((mid_x - CENTER_X)**2 + (mid_y - CENTER_Y)**2)

        if 800 < dist_center < 8000:
            residential_links.append(lid)
        if dist_center < 4000:
            work_links.append(lid)
        if dist_center < 2000:
            center_links.append(lid)

    if len(residential_links) < 20:
        residential_links = all_links
    if len(work_links) < 20:
        work_links = all_links
    if len(center_links) < 10:
        center_links = work_links

    persons = []
    for i in range(num_agents):
        # 60% of workers go to city center (creates congestion)
        if rng.random() < 0.6:
            work_link = rng.choice(center_links)
        else:
            work_link = rng.choice(work_links)

        # Home locations: mostly outer areas
        home_link = rng.choice(residential_links)
        while work_link == home_link:
            home_link = rng.choice(residential_links)

        dep_time = float(np_rng.normal(MEAN_DEPARTURE, STD_DEPARTURE))
        dep_time = max(5 * 3600, min(10 * 3600, dep_time))

        ret_time = dep_time + WORK_TYPICAL_DURATION + float(np_rng.normal(0, 30 * 60))
        ret_time = max(dep_time + 6 * 3600, min(22 * 3600, ret_time))

        plan = Plan(
            activities=[
                Activity("home", home_link, end_time=dep_time),
                Activity("work", work_link, end_time=ret_time),
                Activity("home", home_link, end_time=None),
            ],
            legs=[Leg(), Leg()]
        )

        person = Person(id=f"p{i}", plans=[plan], selected_plan_idx=0)
        persons.append(person)

    print(f"  Population: {len(persons)} agents")
    print(f"  Residential links: {len(residential_links)}, Work links: {len(work_links)}, Center links: {len(center_links)}")
    return persons


# ============================================================================
# DEPARTURE TIME COORDINATION
# ============================================================================

def apply_coordination(persons: List[Person], fraction: float, seed: int = 98765) -> int:
    """
    Apply departure time coordination to a fraction of agents.

    Logic (simple, transparent, rule-based):
    - Select fraction% of agents (deterministic, reproducible)
    - For agents departing during peak (7-9 AM):
      - 50% shift EARLIER by 45-90 minutes
      - 50% shift LATER by 45-90 minutes
    - Work end time adjusted to maintain same work duration

    Returns number of agents actually modified.
    """
    if fraction <= 0:
        return 0

    rng = random.Random(seed)
    shuffled = list(persons)
    rng.shuffle(shuffled)

    num_to_coord = int(round(len(shuffled) * fraction))
    modified = 0

    for person in shuffled[:num_to_coord]:
        plan = person.selected_plan
        home_act = plan.activities[0]

        if home_act.end_time is None:
            continue

        dep = home_act.end_time
        if PEAK_START <= dep <= PEAK_END:
            shift = rng.uniform(MIN_SHIFT, MAX_SHIFT)
            if rng.random() < 0.5:
                shift = -shift

            new_dep = max(5 * 3600, min(11 * 3600, dep + shift))
            home_act.end_time = new_dep

            work_act = plan.activities[1]
            if work_act.end_time is not None:
                work_act.end_time += (new_dep - dep)

            modified += 1

    return modified


# ============================================================================
# MESOSCOPIC TRAFFIC SIMULATION (Time-Dependent)
# ============================================================================

def simulate_traffic(network: CologneNetwork, persons: List[Person], quiet: bool = False) -> List[TripRecord]:
    """
    Mesoscopic traffic simulation with time-dependent routing.

    Each agent routes using current time-bin travel times.
    Flows are accumulated per time bin for BPR travel time update.
    """
    network.reset_flows()
    trip_records = []
    total = len(persons)
    t_start = time_mod.time()

    for idx, person in enumerate(persons):
        if not quiet and (idx + 1) % 100 == 0:
            elapsed = time_mod.time() - t_start
            rate = (idx + 1) / elapsed if elapsed > 0 else 0
            eta = (total - idx - 1) / rate if rate > 0 else 0
            print(f"\r    Routing agent {idx+1}/{total} "
                  f"({rate:.1f} agents/s, ETA {eta:.0f}s)   ", end="", flush=True)

        plan = person.selected_plan
        if len(plan.activities) < 3 or len(plan.legs) < 2:
            continue

        home_act = plan.activities[0]
        work_act = plan.activities[1]
        home_act2 = plan.activities[2]

        # Trip 1: Home -> Work
        origin_node = network.links[home_act.link_id].from_node
        dest_node = network.links[work_act.link_id].from_node
        dep_time = home_act.end_time if home_act.end_time else MEAN_DEPARTURE

        route, route_tt, route_dist = astar_td(network, origin_node, dest_node, dep_time)

        if route:
            plan.legs[0].route = route
            plan.legs[0].departure_time = dep_time
            plan.legs[0].arrival_time = dep_time + route_tt
            plan.legs[0].distance = route_dist

            # Accumulate time-binned flows (assign flow to departure time bin)
            t = dep_time
            for lid in route:
                network.add_flow(lid, t)
                t += network.links[lid].get_travel_time(t)

            trip_records.append(TripRecord(
                person_id=person.id,
                departure_time=dep_time,
                arrival_time=dep_time + route_tt,
                distance=route_dist,
                origin_link=home_act.link_id,
                dest_link=work_act.link_id,
            ))
        else:
            plan.legs[0].route = []
            plan.legs[0].departure_time = dep_time
            plan.legs[0].arrival_time = dep_time + 7200
            plan.legs[0].distance = 0

        # Trip 2: Work -> Home
        origin_node2 = network.links[work_act.link_id].from_node
        dest_node2 = network.links[home_act2.link_id].from_node
        ret_time = work_act.end_time if work_act.end_time else dep_time + WORK_TYPICAL_DURATION

        route2, route_tt2, route_dist2 = astar_td(network, origin_node2, dest_node2, ret_time)

        if route2:
            plan.legs[1].route = route2
            plan.legs[1].departure_time = ret_time
            plan.legs[1].arrival_time = ret_time + route_tt2
            plan.legs[1].distance = route_dist2

            t = ret_time
            for lid in route2:
                network.add_flow(lid, t)
                t += network.links[lid].get_travel_time(t)

            trip_records.append(TripRecord(
                person_id=person.id,
                departure_time=ret_time,
                arrival_time=ret_time + route_tt2,
                distance=route_dist2,
                origin_link=work_act.link_id,
                dest_link=home_act2.link_id,
            ))
        else:
            plan.legs[1].route = []
            plan.legs[1].departure_time = ret_time
            plan.legs[1].arrival_time = ret_time + 7200
            plan.legs[1].distance = 0

    return trip_records


# ============================================================================
# SCORING
# ============================================================================

def score_plan(plan: Plan) -> float:
    score = 0.0
    for act in plan.activities:
        typ_dur = HOME_TYPICAL_DURATION if act.type == "home" else WORK_TYPICAL_DURATION
        score += PERFORMING_UTILS_HR * (typ_dur / 3600.0) * 0.5

    for leg in plan.legs:
        tt = leg.arrival_time - leg.departure_time
        if tt > 0:
            score += TRAVELING_UTILS_HR * (tt / 3600.0)
            score += MONETARY_DISTANCE_RATE * leg.distance

    plan.score = score
    return score


# ============================================================================
# REPLANNING
# ============================================================================

def replan(network: CologneNetwork, persons: List[Person], iteration: int,
           max_iterations: int, rng: random.Random):
    disable_after = int(max_iterations * INNOVATION_DISABLE_FRACTION)
    innovation_allowed = iteration < disable_after

    for person in persons:
        r = rng.random()

        if innovation_allowed and r < FRACTION_REROUTE:
            old_plan = person.selected_plan
            new_plan = Plan(
                activities=[Activity(a.type, a.link_id, a.end_time) for a in old_plan.activities],
                legs=[Leg() for _ in old_plan.legs]
            )
            person.plans.append(new_plan)
            person.selected_plan_idx = len(person.plans) - 1

        elif innovation_allowed and r < FRACTION_REROUTE + FRACTION_TIME_MUTATE:
            old_plan = person.selected_plan
            new_plan = Plan(
                activities=[Activity(a.type, a.link_id, a.end_time) for a in old_plan.activities],
                legs=[Leg() for _ in old_plan.legs]
            )
            if new_plan.activities[0].end_time is not None:
                shift = rng.uniform(-TIME_MUTATION_RANGE, TIME_MUTATION_RANGE)
                new_dep = new_plan.activities[0].end_time + shift
                new_dep = max(5 * 3600, min(11 * 3600, new_dep))
                old_dep = new_plan.activities[0].end_time
                new_plan.activities[0].end_time = new_dep
                if new_plan.activities[1].end_time is not None:
                    new_plan.activities[1].end_time += (new_dep - old_dep)

            person.plans.append(new_plan)
            person.selected_plan_idx = len(person.plans) - 1

        else:
            if person.plans:
                best_idx = max(range(len(person.plans)), key=lambda i: person.plans[i].score)
                person.selected_plan_idx = best_idx

        while len(person.plans) > PLAN_MEMORY_SIZE:
            worst_idx = min(
                (i for i in range(len(person.plans)) if i != person.selected_plan_idx),
                key=lambda i: person.plans[i].score
            )
            person.plans.pop(worst_idx)
            if person.selected_plan_idx > worst_idx:
                person.selected_plan_idx -= 1


# ============================================================================
# KPI COMPUTATION
# ============================================================================

@dataclass
class ScenarioKPI:
    name: str
    avg_travel_time_min: float = 0.0
    p95_travel_time_min: float = 0.0
    peak_hour_avg_travel_time_min: float = 0.0
    vkt_km: float = 0.0
    vht_hours: float = 0.0
    num_trips: int = 0
    num_agents: int = 0


def compute_kpis(name: str, trips: List[TripRecord], num_agents: int) -> ScenarioKPI:
    kpi = ScenarioKPI(name=name, num_agents=num_agents, num_trips=len(trips))
    if not trips:
        return kpi

    travel_times = sorted([t.arrival_time - t.departure_time for t in trips])

    kpi.avg_travel_time_min = np.mean(travel_times) / 60.0

    p95_idx = min(int(math.ceil(len(travel_times) * 0.95)) - 1, len(travel_times) - 1)
    kpi.p95_travel_time_min = travel_times[max(0, p95_idx)] / 60.0

    peak_tts = [t.arrival_time - t.departure_time for t in trips
                if PEAK_START <= t.departure_time <= PEAK_END]
    if peak_tts:
        kpi.peak_hour_avg_travel_time_min = np.mean(peak_tts) / 60.0

    kpi.vkt_km = sum(t.distance for t in trips) / 1000.0
    kpi.vht_hours = sum(t.arrival_time - t.departure_time for t in trips) / 3600.0

    return kpi


# ============================================================================
# MAIN SCENARIO RUNNER
# ============================================================================

def run_scenario(scenario_name: str, coordination_fraction: float,
                 num_agents: int = NUM_AGENTS, num_iterations: int = NUM_ITERATIONS,
                 network_mode: str = "synthetic", quiet: bool = False) -> ScenarioKPI:
    _print = (lambda *a, **k: None) if quiet else print
    _print(f"\n{'='*70}")
    _print(f"  SCENARIO: {scenario_name}")
    _print(f"  Network: {network_mode}")
    _print(f"  Coordination: {coordination_fraction*100:.0f}% of agents")
    _print(f"  Agents: {num_agents}, Iterations: {num_iterations}")
    _print(f"{'='*70}")

    network = CologneNetwork()
    if network_mode == "real":
        nodes_path = os.path.join(DATA_DIR, "nodes.csv")
        links_path = os.path.join(DATA_DIR, "links.csv")
        if not os.path.exists(nodes_path) or not os.path.exists(links_path):
            _print(f"  ERROR: Real network data not found at {DATA_DIR}")
            _print(f"  Run download_cologne_data.py first.")
            sys.exit(1)
        _print("\n  Loading real Cologne network...")
        network.load_from_csv(nodes_path, links_path)
    else:
        _print("\n  Building synthetic Cologne network...")
        network.build()

    _print("  Generating population...")
    persons = generate_population(network, num_agents)

    if coordination_fraction > 0:
        modified = apply_coordination(persons, coordination_fraction)
        _print(f"  Coordination applied: {modified} agents modified "
               f"({modified/num_agents*100:.1f}% actual)")

    rng = random.Random(RANDOM_SEED + 1000)
    all_trip_records = []  # collect last N iterations for averaging
    prev_avg_tt = None
    AVG_LAST_N = 5  # average KPIs over last 5 iterations

    for iteration in range(num_iterations):
        t0 = time_mod.time()

        trips = simulate_traffic(network, persons, quiet=quiet)
        if not quiet:
            print()  # newline after progress bar
        network.update_travel_times_msa(iteration)

        for person in persons:
            score_plan(person.selected_plan)

        avg_score = np.mean([p.selected_plan.score for p in persons])
        avg_tt = np.mean([t.arrival_time - t.departure_time for t in trips]) / 60 if trips else 0

        elapsed = time_mod.time() - t0

        if iteration % 5 == 0 or iteration == num_iterations - 1:
            convergence = ""
            if prev_avg_tt is not None and prev_avg_tt > 0:
                change = abs(avg_tt - prev_avg_tt) / prev_avg_tt * 100
                convergence = f"  delta={change:.2f}%"
            _print(f"  Iter {iteration:3d}/{num_iterations}: "
                   f"avg_score={avg_score:8.1f}  avg_tt={avg_tt:6.2f}min  "
                   f"trips={len(trips):6d}  [{elapsed:.1f}s]{convergence}")

        prev_avg_tt = avg_tt

        # Collect trip records from last N iterations for averaging
        if iteration >= num_iterations - AVG_LAST_N:
            all_trip_records.extend(trips)

        if iteration < num_iterations - 1:
            replan(network, persons, iteration, num_iterations, rng)

    # Compute KPIs averaged over last N iterations for stability
    kpis = compute_kpis(scenario_name, all_trip_records, num_agents)
    # Scale to per-iteration averages
    kpis.num_trips = len(all_trip_records) // AVG_LAST_N
    kpis.vkt_km /= AVG_LAST_N
    kpis.vht_hours /= AVG_LAST_N
    _print(f"\n  Final KPIs for {scenario_name}:")
    _print(f"    Avg Travel Time:      {kpis.avg_travel_time_min:8.2f} min")
    _print(f"    P95 Travel Time:      {kpis.p95_travel_time_min:8.2f} min")
    _print(f"    Peak-Hour Avg TT:     {kpis.peak_hour_avg_travel_time_min:8.2f} min")
    _print(f"    VKT:                  {kpis.vkt_km:12.0f} km")
    _print(f"    VHT:                  {kpis.vht_hours:10.1f} hours")
    _print(f"    Trips:                {kpis.num_trips:8d}")
    _print(f"    Agents:               {kpis.num_agents:8d}")

    return kpis


def format_comparison_table(results: List[ScenarioKPI]) -> str:
    lines = []
    lines.append("")
    lines.append("=" * 132)
    lines.append("  COLOGNE (KÖLN) TRAFFIC SIMULATION — KPI COMPARISON TABLE")
    lines.append("  Agent-Based Microsimulation | Departure Time Coordination Scenarios")
    lines.append("=" * 132)
    lines.append(f"| {'Scenario':<25} | {'Avg TT':>10} | {'P95 TT':>10} | {'Peak TT':>10} | {'VKT':>14} | {'VHT':>12} | {'Trips':>8} | {'Agents':>8} |")
    lines.append(f"| {'':<25} | {'(min)':>10} | {'(min)':>10} | {'(min)':>10} | {'(km)':>14} | {'(hours)':>12} | {'':>8} | {'':>8} |")
    lines.append("|" + "-"*27 + "|" + "-"*12 + "|" + "-"*12 + "|" + "-"*12 + "|" + "-"*16 + "|" + "-"*14 + "|" + "-"*10 + "|" + "-"*10 + "|")

    for r in results:
        lines.append(f"| {r.name:<25} | {r.avg_travel_time_min:10.2f} | {r.p95_travel_time_min:10.2f} | "
                     f"{r.peak_hour_avg_travel_time_min:10.2f} | {r.vkt_km:14.0f} | {r.vht_hours:12.1f} | "
                     f"{r.num_trips:8d} | {r.num_agents:8d} |")

    lines.append("=" * 132)

    if len(results) > 1:
        bl = results[0]
        lines.append("")
        lines.append("  Percentage Change from Baseline:")
        lines.append(f"| {'Scenario':<25} | {'Avg TT':>10} | {'P95 TT':>10} | {'Peak TT':>10} | {'VKT':>14} | {'VHT':>12} |")
        lines.append("|" + "-"*27 + "|" + "-"*12 + "|" + "-"*12 + "|" + "-"*12 + "|" + "-"*16 + "|" + "-"*14 + "|")

        for r in results[1:]:
            def pct(base, val):
                return ((val - base) / base * 100) if base != 0 else 0

            lines.append(f"| {r.name:<25} | {pct(bl.avg_travel_time_min, r.avg_travel_time_min):+9.2f}% | "
                         f"{pct(bl.p95_travel_time_min, r.p95_travel_time_min):+9.2f}% | "
                         f"{pct(bl.peak_hour_avg_travel_time_min, r.peak_hour_avg_travel_time_min):+9.2f}% | "
                         f"{pct(bl.vkt_km, r.vkt_km):+13.2f}% | "
                         f"{pct(bl.vht_hours, r.vht_hours):+11.2f}% |")
        lines.append("")

    return "\n".join(lines)


def write_csv(results: List[ScenarioKPI], path: str):
    with open(path, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(["scenario", "avg_travel_time_min", "p95_travel_time_min",
                          "peak_hour_avg_travel_time_min", "vkt_km", "vht_hours",
                          "num_trips", "num_agents"])
        for r in results:
            writer.writerow([r.name, f"{r.avg_travel_time_min:.2f}",
                             f"{r.p95_travel_time_min:.2f}",
                             f"{r.peak_hour_avg_travel_time_min:.2f}",
                             f"{r.vkt_km:.0f}", f"{r.vht_hours:.1f}",
                             r.num_trips, r.num_agents])


def _run_scenario_worker(args_tuple):
    """Wrapper for multiprocessing — unpacks arguments for run_scenario."""
    name, fraction, num_agents, num_iterations, network_mode = args_tuple
    return run_scenario(name, fraction, num_agents=num_agents,
                        num_iterations=num_iterations, network_mode=network_mode,
                        quiet=True)


def main():
    parser = argparse.ArgumentParser(description="Cologne Traffic Microsimulation")
    parser.add_argument("--network", choices=["synthetic", "real"], default="synthetic",
                        help="Network source: 'synthetic' (built-in) or 'real' (OSM data from download_cologne_data.py)")
    parser.add_argument("--agents", type=int, default=NUM_AGENTS,
                        help=f"Number of agents (default: {NUM_AGENTS})")
    parser.add_argument("--iterations", type=int, default=NUM_ITERATIONS,
                        help=f"Iterations per scenario (default: {NUM_ITERATIONS})")
    parser.add_argument("--workers", type=int, default=0,
                        help="Number of parallel workers (0=sequential, default). "
                             "Set to number of CPU cores (e.g. 5) to run scenarios in parallel.")
    args = parser.parse_args()

    overall_start = time_mod.time()

    print("=" * 70)
    print("  COLOGNE AGENT-BASED TRAFFIC MICROSIMULATION")
    print("  MATSim-Equivalent Implementation")
    print("=" * 70)
    print(f"  Network: {args.network}")
    print(f"  Agents: {args.agents}")
    print(f"  Iterations per scenario: {args.iterations}")
    print(f"  Workers: {args.workers if args.workers > 0 else 'sequential'}")
    print(f"  Scenarios: Baseline + 4 coordination levels (1%, 2%, 5%, 10%)")
    print()

    output_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                              "..", "..", "..", "scenarios", "cologne", "output")
    os.makedirs(output_dir, exist_ok=True)

    scenarios = [
        ("Baseline (0%)", 0.00),
        ("Coordinated 1%", 0.01),
        ("Coordinated 2%", 0.02),
        ("Coordinated 5%", 0.05),
        ("Coordinated 10%", 0.10),
    ]

    if args.workers > 0:
        print(f"  Running {len(scenarios)} scenarios in parallel ({args.workers} workers)...")
        work_items = [(name, fraction, args.agents, args.iterations, args.network)
                      for name, fraction in scenarios]
        with ProcessPoolExecutor(max_workers=args.workers) as executor:
            results = list(executor.map(_run_scenario_worker, work_items))
    else:
        results = []
        for name, fraction in scenarios:
            kpi = run_scenario(name, fraction, num_agents=args.agents,
                               num_iterations=args.iterations, network_mode=args.network)
            results.append(kpi)

    table = format_comparison_table(results)
    print(table)

    csv_path = os.path.join(output_dir, "kpi_comparison.csv")
    write_csv(results, csv_path)
    print(f"  CSV written to: {csv_path}")

    txt_path = os.path.join(output_dir, "kpi_comparison.txt")
    with open(txt_path, 'w') as f:
        f.write(table)
    print(f"  Table written to: {txt_path}")

    elapsed = time_mod.time() - overall_start
    print(f"\n  Total elapsed time: {elapsed:.0f}s ({elapsed/60:.1f}min)")
    print("  Done.")


if __name__ == "__main__":
    main()
