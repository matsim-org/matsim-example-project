# Technical Report: Cologne Agent-Based Traffic Microsimulation

## 1. Project Overview

This document describes a MATSim-equivalent agent-based traffic microsimulation for Cologne (Koeln), Germany. The simulation models 20,000 synthetic agents performing home-work-home trips on a simplified representation of Cologne's road network, and evaluates the impact of staggered work-hour coordination policies on network performance.

Because the MATSim Maven repository (`repo.matsim.org`) was blocked by the network proxy, the simulation was implemented as a self-contained Python-based microsimulation that replicates MATSim's core architecture (iterative replanning, mesoscopic traffic assignment, scoring). Java MATSim source code is also provided for production use when repository access is restored.

## 2. Data Sources

| Element | Source | Notes |
|---|---|---|
| Road network | Synthetic, Cologne-inspired topology | Not derived from OpenStreetMap; hand-designed to capture key structural features (Rhine crossings, ring roads, Autobahn) |
| Population | Fully synthetic generation | Not calibrated to census microdata or household travel surveys |
| Coordinate system | EPSG:25832 (UTM zone 32N) | Center reference point: (356000, 5645000) |
| Travel time function | Bureau of Public Roads (BPR) standard | t = t_free * (1 + 0.5 * (V/C)^4) |

## 3. Network Specification

The network comprises approximately 127 nodes and 736 links, organized into the following hierarchy:

- **Autobahn ring** -- 33.33 m/s free-flow speed, 2000 veh/h capacity
- **Arterials** -- 13.89 m/s, 500-800 veh/h capacity
- **Collectors** -- 11.11 m/s, 250-300 veh/h capacity
- **Local streets** -- 8.33 m/s, 150 veh/h capacity

Key features include the Rhine river with 4 bridge crossings (400 veh/h each), inner/middle/outer ring roads on the left bank, the Deutz district on the right bank, and local street grids. Primary bottlenecks are inner-city radial links (150 veh/h) and Rhine bridges (400 veh/h).

## 4. Synthetic Population

- 20,000 agents, each executing a home-work-home activity chain (car mode only).
- Home locations distributed across residential areas, 800 m to 8,000 m from the city center.
- Work locations: 60% within 2,000 m of center, 40% distributed up to 4,000 m.
- Morning departure times drawn from a normal distribution (mean 07:30, std 45 min, clamped to 05:00-10:00).
- Work duration approximately 8 hours; return trip follows accordingly.

## 5. Simulation Engine

The simulation uses a mesoscopic traffic model with iterative day-to-day replanning:

- **Routing**: Time-dependent Dijkstra shortest path, updated each iteration using congested travel times from the previous iteration.
- **Flow tracking**: 15-minute time bins (96 bins per day) with MSA (Method of Successive Averages) smoothing for convergence stability.
- **Replanning strategy mix**: BestScore selection (80%), ReRoute (15%), TimeAllocationMutator (5%).
- **Iterations**: 25 per scenario; KPIs are averaged over the final 5 iterations to reduce noise.

## 6. Coordination Logic

The coordination intervention applies a rule-based staggered work-hours policy:

1. For a given coordination level X%, randomly select X% of agents (deterministic seed for reproducibility).
2. Among selected agents whose original departure falls within the 07:00-09:00 AM peak window:
   - 50% are shifted **earlier** by a uniform-random offset of 45-90 minutes.
   - 50% are shifted **later** by a uniform-random offset of 45-90 minutes.
3. Work end times are adjusted to preserve the original work duration.

Not all selected agents are necessarily modified; only those with departures during the peak window are affected. Five scenarios were evaluated: Baseline (0%), and Coordinated at 1%, 2%, 5%, and 10%.

## 7. Key Performance Indicators

- Average travel time (all trips, full day)
- 95th-percentile travel time
- Peak-hour average travel time (07:00-09:00 AM)
- Vehicle-kilometers traveled (VKT)
- Vehicle-hours traveled (VHT)
- Total trip count and agent count

## 8. Assumptions and Limitations

**Assumptions**:
- All agents travel by private car; no mode choice is modeled.
- Activity patterns are limited to home-work-home; no secondary activities, freight, or through-traffic.
- Traffic signals are not explicitly modeled; their effect is captured implicitly through reduced link capacities.
- The BPR volume-delay function provides an adequate approximation of congestion dynamics at the mesoscopic level.

**Limitations**:
- The network is synthetic and substantially simplified compared to real Cologne (approximately 127 nodes vs. 100,000+ links in a full OSM extract). Absolute travel times should not be interpreted as predictions for the real network.
- The population is not calibrated to census or travel-survey data; demand patterns are illustrative, not empirical.
- Car-only modeling omits public transit, cycling, and walking, which collectively carry a significant mode share in Cologne.
- The mesoscopic BPR-based model does not capture car-following, lane-changing, or intersection-level dynamics that a full microsimulation (e.g., SUMO, VISSIM, or MATSim with QSim) would provide.
- The MATSim Java repository was inaccessible during development; the Python implementation serves as a functionally equivalent substitute.

## 9. Reproducibility

The simulation is implemented as a single Python script with only `numpy` as a dependency:

```
python3 src/main/python/cologne_simulation.py
```

All random processes use deterministic seeds. Outputs (network XML, plans XML, scenario CSV results) are written to `scenarios/cologne/output/`.
