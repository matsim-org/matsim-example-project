# matsim-example-project

A MATSim-based traffic microsimulation project featuring a **Cologne departure time coordination** study. The project includes both Java (MATSim library) and Python simulation components.

By default, this project uses the latest (pre-)release. In order to use a different version, edit `pom.xml`.

---

## Cologne Traffic Simulation

An agent-based traffic microsimulation for Cologne that evaluates **departure time coordination** as a congestion management policy. The simulation implements the core MATSim loop — routing, congestion, scoring, and replanning — and tests what happens when 1%, 2%, 5%, or 10% of peak-hour commuters shift their departure times off-peak.

### Features

- **20,000 agents** simulated over 25 iterations with BPR congestion + MSA averaging
- **Parallel A\* routing** using multiprocessing for fast iteration times
- **5 scenarios**: Baseline + 4 coordination levels (1%, 2%, 5%, 10% shift)
- **Interactive maps** with togglable layers (traffic volume, congestion, speed, peak-hour V/C)
- **Real OSM network** support via Overpass API download

### Python Prerequisites

```sh
pip install numpy folium pyproj requests
```

### Quick Start

```sh
# Run the simulation with defaults (synthetic network, 20k agents, 25 iterations)
python src/main/python/cologne_simulation.py

# Use fewer agents and iterations for a quick test
python src/main/python/cologne_simulation.py --agents 1000 --iterations 10

# Generate interactive HTML maps from the results
python src/main/python/generate_scenario_maps.py
```

### CLI Options

| Flag | Default | Description |
|------|---------|-------------|
| `--network` | `synthetic` | Network source: `synthetic` (built-in) or `real` (OSM data) |
| `--agents` | `20000` | Number of agents to simulate |
| `--iterations` | `25` | Iterations per scenario |
| `--workers` | `0` | Parallel workers for scenario-level execution (0 = sequential) |
| `--routing-workers` | `0` | Parallel workers for A\* routing (0 = auto-detect CPUs) |

### Using the Real Cologne Network

```sh
# 1. Download road network from OpenStreetMap
python src/main/python/download_cologne_data.py

# 2. Run simulation with real network
python src/main/python/cologne_simulation.py --network real

# 3. Generate maps
python src/main/python/generate_scenario_maps.py --network real
```

### Output

Results are saved to `scenarios/cologne/output/`:
- `kpi_comparison.csv` — KPI summary across all scenarios
- `kpi_comparison.txt` — Human-readable comparison table
- `maps/` — Interactive HTML maps per scenario + an `index.html` linking them all

---

## Project Structure

```
src/main/java/org/matsim/project/          # Java MATSim entry points
src/main/java/org/matsim/project/cologne/  # Java Cologne scenario classes
src/main/python/                           # Python simulation & visualization
scenarios/cologne/                         # Cologne scenario input/output
```

A recommended directory structure for additional scenarios:
* `src` for sources
* `original-input-data` for original input data (typically not in MATSim format)
* `scenarios` for MATSim scenarios, i.e. MATSim input and output data.  A good way is the following:
  * One subdirectory for each scenario, e.g. `scenarios/mySpecialScenario01`.
  * This minimally contains a config file, a network file, and a population file.
  * Output goes one level down, e.g. `scenarios/mySpecialScenario01/output-from-a-good-run/...`.


### Import into eclipse

1. download a modern version of eclipse. This should have maven and git included by default.
1. `file->import->git->projects from git->clone URI` and clone as specified above.  _It will go through a 
sequence of windows; it is important that you import as 'general project'._
1. `file->import->maven->existing maven projects`

Sometimes, step 3 does not work, in particular after previously failed attempts.  Sometimes, it is possible to
right-click to `configure->convert to maven project`.  If that fails, the best thing seems to remove all 
pieces of the failed attempt in the directory and start over.

### Import into IntelliJ

`File -> New -> Project from Version Control` paste the repository url and hit 'clone'. IntelliJ usually figures out
that the project is a maven project. If not: `Right click on pom.xml -> import as maven project`.

### Java Version

The project uses Java 11. Usually a suitable SDK is packaged within IntelliJ or Eclipse. Otherwise, one must install a 
suitable sdk manually, which is available [here](https://openjdk.java.net/)

### Building and Running it locally

You can build an executable jar-file by executing the following command:

```sh
./mvnw clean package
```

or on Windows:

```sh
mvnw.cmd clean package
```

This will download all necessary dependencies (it might take a while the first time it is run) and create a file `matsim-example-project-0.0.1-SNAPSHOT.jar` in the top directory. This jar-file can either be double-clicked to start the MATSim GUI, or executed with Java on the command line:

```sh
java -jar matsim-example-project-0.0.1-SNAPSHOT.jar
```



### Licenses
(The following paragraphs need to be adjusted according to the specifications of your project.)

The **MATSim program code** in this repository is distributed under the terms of the [GNU General Public License as published by the Free Software Foundation (version 2)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html). The MATSim program code are files that reside in the `src` directory hierarchy and typically end with `*.java`.

The **MATSim input files, output files, analysis data and visualizations** are licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/4.0/">Creative Commons Attribution 4.0 International License</a>.
<a rel="license" href="http://creativecommons.org/licenses/by/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by/4.0/80x15.png" /></a><br /> MATSim input files are those that are used as input to run MATSim. They often, but not always, have a header pointing to matsim.org. They typically reside in the `scenarios` directory hierarchy. MATSim output files, analysis data, and visualizations are files generated by MATSim runs, or by postprocessing.  They typically reside in a directory hierarchy starting with `output`.

**Other data files**, in particular in `original-input-data`, have their own individual licenses that need to be individually clarified with the copyright holders.


