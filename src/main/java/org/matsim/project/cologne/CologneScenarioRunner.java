package org.matsim.project.cologne;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Main runner for the Cologne traffic simulation scenarios.
 *
 * Executes the following scenarios:
 * 1. Baseline (no coordination)
 * 2. 1% coordinated departure times
 * 3. 2% coordinated departure times
 * 4. 5% coordinated departure times
 * 5. 10% coordinated departure times
 *
 * Each scenario uses identical network and base population.
 * Coordination modifies departure times for X% of agents.
 *
 * Usage: java CologneScenarioRunner [numIterations] [numAgents]
 */
public class CologneScenarioRunner {

    private static final String BASE_DIR = "scenarios/cologne";
    private static final String INPUT_DIR = BASE_DIR + "/input";
    private static final String OUTPUT_BASE = BASE_DIR + "/output";
    private static final String NETWORK_FILE = INPUT_DIR + "/network.xml";
    private static final String PLANS_FILE = INPUT_DIR + "/plans.xml";

    private static final int DEFAULT_ITERATIONS = 30;
    private static final int DEFAULT_NUM_AGENTS = 5000;

    private static final double[] COORDINATION_FRACTIONS = {0.0, 0.01, 0.02, 0.05, 0.10};
    private static final String[] SCENARIO_NAMES = {"Baseline", "Coord 1%", "Coord 2%", "Coord 5%", "Coord 10%"};

    private final int numIterations;
    private final int numAgents;

    public CologneScenarioRunner(int numIterations, int numAgents) {
        this.numIterations = numIterations;
        this.numAgents = numAgents;
    }

    public void generateInputData() {
        System.out.println("========================================");
        System.out.println("  GENERATING COLOGNE SCENARIO INPUT DATA");
        System.out.println("========================================");

        // Generate network
        System.out.println("\n--- Generating network ---");
        CologneNetworkGenerator networkGen = new CologneNetworkGenerator();
        Network network = networkGen.generateNetwork();
        System.out.println("Network: " + network.getNodes().size() + " nodes, " + network.getLinks().size() + " links");
        networkGen.writeNetwork(network, NETWORK_FILE);

        // Generate population
        System.out.println("\n--- Generating population ---");
        ColognePopulationGenerator popGen = new ColognePopulationGenerator(numAgents);
        Population population = popGen.generatePopulation(network);
        System.out.println("Population: " + population.getPersons().size() + " agents");
        popGen.writePopulation(population, PLANS_FILE);

        System.out.println("\nInput data written to: " + INPUT_DIR);
    }

    public List<KPIAnalyzer.KPIResult> runAllScenarios() {
        List<KPIAnalyzer.KPIResult> results = new ArrayList<>();

        for (int i = 0; i < COORDINATION_FRACTIONS.length; i++) {
            double fraction = COORDINATION_FRACTIONS[i];
            String name = SCENARIO_NAMES[i];
            String outputDir = OUTPUT_BASE + "/" + name.toLowerCase().replace(" ", "_").replace("%", "pct");

            System.out.println("\n========================================");
            System.out.println("  RUNNING SCENARIO: " + name);
            System.out.println("  Coordination fraction: " + (fraction * 100) + "%");
            System.out.println("  Output: " + outputDir);
            System.out.println("========================================\n");

            try {
                runSingleScenario(name, fraction, outputDir);

                // Analyze results
                String eventsFile = outputDir + "/output_events.xml.gz";
                if (Files.exists(Paths.get(eventsFile))) {
                    // Load network for analysis
                    Scenario analysisScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
                    new MatsimNetworkReader(analysisScenario.getNetwork()).readFile(NETWORK_FILE);

                    KPIAnalyzer analyzer = new KPIAnalyzer();
                    KPIAnalyzer.KPIResult result = analyzer.analyzeScenario(name, eventsFile, analysisScenario.getNetwork());
                    results.add(result);

                    System.out.println("\n" + result);
                } else {
                    System.err.println("WARNING: Events file not found: " + eventsFile);
                }
            } catch (Exception e) {
                System.err.println("ERROR running scenario " + name + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return results;
    }

    private void runSingleScenario(String scenarioName, double coordinationFraction, String outputDir) {
        Config config = createConfig(outputDir);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Apply coordination if needed
        if (coordinationFraction > 0) {
            DepartureTimeCoordinator coordinator = new DepartureTimeCoordinator(coordinationFraction, 98765L);
            int modified = coordinator.applyCoordination(scenario.getPopulation());
            System.out.println("Coordinated " + modified + " agents (" + coordinator.getLabel() + " target)");
        }

        Controler controler = new Controler(scenario);
        controler.run();
    }

    private Config createConfig(String outputDir) {
        Config config = ConfigUtils.createConfig();

        // Network
        config.network().setInputFile("../../" + NETWORK_FILE);

        // Plans
        config.plans().setInputPlansFile("../../" + PLANS_FILE);

        // Global
        config.global().setCoordinateSystem("EPSG:25832");
        config.global().setRandomSeed(4711);

        // Controller
        config.controller().setOutputDirectory(outputDir);
        config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setLastIteration(numIterations);
        config.controller().setWriteEventsInterval(numIterations); // only write final iteration events
        config.controller().setWritePlansInterval(numIterations);

        // QSim
        config.qsim().setStartTime(4.0 * 3600); // 4 AM
        config.qsim().setEndTime(24.0 * 3600);   // midnight
        config.qsim().setFlowCapFactor(1.0);
        config.qsim().setStorageCapFactor(1.0);
        config.qsim().setSnapshotPeriod(0); // no snapshots
        config.qsim().setMainModes(List.of("car"));

        // Scoring
        ScoringConfigGroup scoring = config.scoring();
        scoring.setPerforming_utils_hr(6.0);
        scoring.setLateArrival_utils_hr(-18.0);
        scoring.setMarginalUtlOfWaiting_utils_hr(-1.0);

        // Activity parameters - home
        ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("home");
        homeParams.setTypicalDuration(12.0 * 3600);
        scoring.addActivityParams(homeParams);

        // Activity parameters - work
        ScoringConfigGroup.ActivityParams workParams = new ScoringConfigGroup.ActivityParams("work");
        workParams.setTypicalDuration(8.0 * 3600);
        workParams.setOpeningTime(6.0 * 3600);
        workParams.setClosingTime(20.0 * 3600);
        scoring.addActivityParams(workParams);

        // Mode parameters for car
        ScoringConfigGroup.ModeParams carParams = new ScoringConfigGroup.ModeParams("car");
        carParams.setMarginalUtilityOfTraveling(-6.0);
        carParams.setConstant(0.0);
        carParams.setMonetaryDistanceRate(-0.0002);
        scoring.addModeParams(carParams);

        // Replanning strategies
        ReplanningConfigGroup replanning = config.replanning();
        replanning.setMaxAgentPlanMemorySize(5);

        ReplanningConfigGroup.StrategySettings bestScore = new ReplanningConfigGroup.StrategySettings();
        bestScore.setStrategyName("BestScore");
        bestScore.setWeight(0.8);
        replanning.addStrategySettings(bestScore);

        ReplanningConfigGroup.StrategySettings reRoute = new ReplanningConfigGroup.StrategySettings();
        reRoute.setStrategyName("ReRoute");
        reRoute.setWeight(0.15);
        reRoute.setDisableAfterIteration((int) (numIterations * 0.8));
        replanning.addStrategySettings(reRoute);

        ReplanningConfigGroup.StrategySettings timeAlloc = new ReplanningConfigGroup.StrategySettings();
        timeAlloc.setStrategyName("TimeAllocationMutator");
        timeAlloc.setWeight(0.05);
        timeAlloc.setDisableAfterIteration((int) (numIterations * 0.8));
        replanning.addStrategySettings(timeAlloc);

        // Time allocation mutator
        config.timeAllocationMutator().setMutationRange(1800); // 30 min range

        // Routing
        config.routing().setRoutingRandomness(3.0);

        return config;
    }

    public static void main(String[] args) {
        int numIterations = DEFAULT_ITERATIONS;
        int numAgents = DEFAULT_NUM_AGENTS;

        if (args.length >= 1) {
            numIterations = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            numAgents = Integer.parseInt(args[1]);
        }

        System.out.println("Cologne Traffic Simulation");
        System.out.println("Iterations: " + numIterations);
        System.out.println("Agents: " + numAgents);

        CologneScenarioRunner runner = new CologneScenarioRunner(numIterations, numAgents);

        // Step 1: Generate input data
        runner.generateInputData();

        // Step 2: Run all scenarios
        List<KPIAnalyzer.KPIResult> results = runner.runAllScenarios();

        // Step 3: Output KPI comparison
        if (!results.isEmpty()) {
            String table = KPIAnalyzer.formatComparisonTable(results);
            System.out.println(table);

            // Write CSV
            try {
                KPIAnalyzer.writeCSV(results, OUTPUT_BASE + "/kpi_comparison.csv");
                System.out.println("KPI CSV written to: " + OUTPUT_BASE + "/kpi_comparison.csv");

                // Write table to file
                Files.writeString(Paths.get(OUTPUT_BASE + "/kpi_comparison.txt"), table);
                System.out.println("KPI table written to: " + OUTPUT_BASE + "/kpi_comparison.txt");
            } catch (IOException e) {
                System.err.println("Error writing output files: " + e.getMessage());
            }
        }

        System.out.println("\nAll scenarios completed.");
    }
}
