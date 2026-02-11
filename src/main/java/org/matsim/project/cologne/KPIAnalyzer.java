package org.matsim.project.cologne;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts Key Performance Indicators (KPIs) from MATSim simulation output.
 *
 * KPIs computed:
 * - Average travel time (seconds and minutes)
 * - P95 travel time (95th percentile)
 * - Peak-hour average travel time (7:00-9:00 AM)
 * - Vehicle Kilometers Traveled (VKT)
 * - Vehicle Hours Traveled (VHT)
 * - Number of completed trips
 * - Number of agents
 */
public class KPIAnalyzer {

    public static class KPIResult {
        public String scenarioName;
        public double avgTravelTimeSec;
        public double p95TravelTimeSec;
        public double peakHourAvgTravelTimeSec;
        public double vkt; // vehicle-km traveled
        public double vht; // vehicle-hours traveled
        public int numTrips;
        public int numAgents;

        public String toTableRow() {
            return String.format("| %-25s | %8.1f | %8.1f | %8.1f | %12.0f | %10.1f | %8d | %8d |",
                    scenarioName,
                    avgTravelTimeSec / 60.0,
                    p95TravelTimeSec / 60.0,
                    peakHourAvgTravelTimeSec / 60.0,
                    vkt,
                    vht,
                    numTrips,
                    numAgents);
        }

        @Override
        public String toString() {
            return String.format("Scenario: %s%n  Avg Travel Time: %.1f min%n  P95 Travel Time: %.1f min%n  Peak-Hour Avg: %.1f min%n  VKT: %.0f km%n  VHT: %.1f h%n  Trips: %d%n  Agents: %d",
                    scenarioName, avgTravelTimeSec / 60.0, p95TravelTimeSec / 60.0,
                    peakHourAvgTravelTimeSec / 60.0, vkt, vht, numTrips, numAgents);
        }
    }

    private static class TripData {
        double departureTime;
        double arrivalTime;
        double distance; // meters
    }

    public KPIResult analyzeScenario(String scenarioName, String eventsFile, Network network) {
        EventsManager eventsManager = EventsUtils.createEventsManager();
        TripHandler handler = new TripHandler(network);
        eventsManager.addHandler(handler);

        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        return computeKPIs(scenarioName, handler);
    }

    private KPIResult computeKPIs(String scenarioName, TripHandler handler) {
        KPIResult result = new KPIResult();
        result.scenarioName = scenarioName;

        List<TripData> trips = handler.getCompletedTrips();
        result.numTrips = trips.size();
        result.numAgents = handler.getUniqueAgents().size();

        if (trips.isEmpty()) {
            return result;
        }

        // Travel times
        List<Double> travelTimes = trips.stream()
                .map(t -> t.arrivalTime - t.departureTime)
                .sorted()
                .collect(Collectors.toList());

        result.avgTravelTimeSec = travelTimes.stream().mapToDouble(d -> d).average().orElse(0);

        // P95
        int p95Index = (int) Math.ceil(travelTimes.size() * 0.95) - 1;
        p95Index = Math.max(0, Math.min(p95Index, travelTimes.size() - 1));
        result.p95TravelTimeSec = travelTimes.get(p95Index);

        // Peak hour (7:00-9:00 AM = 25200-32400 seconds)
        List<Double> peakTravelTimes = trips.stream()
                .filter(t -> t.departureTime >= 25200 && t.departureTime <= 32400)
                .map(t -> t.arrivalTime - t.departureTime)
                .collect(Collectors.toList());

        result.peakHourAvgTravelTimeSec = peakTravelTimes.isEmpty() ? 0 :
                peakTravelTimes.stream().mapToDouble(d -> d).average().orElse(0);

        // VKT (vehicle kilometers traveled)
        result.vkt = trips.stream().mapToDouble(t -> t.distance / 1000.0).sum();

        // VHT (vehicle hours traveled)
        result.vht = trips.stream().mapToDouble(t -> (t.arrivalTime - t.departureTime) / 3600.0).sum();

        return result;
    }

    /**
     * Event handler that tracks person departures, arrivals, and link traversals.
     */
    private static class TripHandler implements PersonDepartureEventHandler,
            PersonArrivalEventHandler, LinkLeaveEventHandler {

        private final Network network;
        private final Map<Id<?>, TripData> activeTrips = new HashMap<>();
        private final List<TripData> completedTrips = new ArrayList<>();
        private final Set<Id<?>> uniqueAgents = new HashSet<>();

        public TripHandler(Network network) {
            this.network = network;
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getPersonId().toString().startsWith("pt_")) return; // skip pt vehicles

            TripData trip = new TripData();
            trip.departureTime = event.getTime();
            trip.distance = 0;
            activeTrips.put(event.getPersonId(), trip);
            uniqueAgents.add(event.getPersonId());
        }

        @Override
        public void handleEvent(PersonArrivalEvent event) {
            if (event.getPersonId().toString().startsWith("pt_")) return;

            TripData trip = activeTrips.remove(event.getPersonId());
            if (trip != null) {
                trip.arrivalTime = event.getTime();
                completedTrips.add(trip);
            }
        }

        @Override
        public void handleEvent(LinkLeaveEvent event) {
            TripData trip = activeTrips.get(event.getVehicleId());
            if (trip != null) {
                Link link = network.getLinks().get(event.getLinkId());
                if (link != null) {
                    trip.distance += link.getLength();
                }
            }
        }

        @Override
        public void reset(int iteration) {
            activeTrips.clear();
            completedTrips.clear();
            uniqueAgents.clear();
        }

        public List<TripData> getCompletedTrips() {
            return completedTrips;
        }

        public Set<Id<?>> getUniqueAgents() {
            return uniqueAgents;
        }
    }

    /**
     * Print a formatted comparison table of multiple scenario results.
     */
    public static String formatComparisonTable(List<KPIResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("=".repeat(120)).append("\n");
        sb.append("  COLOGNE TRAFFIC SIMULATION — KPI COMPARISON TABLE\n");
        sb.append("=".repeat(120)).append("\n");
        sb.append(String.format("| %-25s | %8s | %8s | %8s | %12s | %10s | %8s | %8s |%n",
                "Scenario", "Avg TT", "P95 TT", "Peak TT", "VKT", "VHT", "Trips", "Agents"));
        sb.append(String.format("| %-25s | %8s | %8s | %8s | %12s | %10s | %8s | %8s |%n",
                "", "(min)", "(min)", "(min)", "(km)", "(hours)", "", ""));
        sb.append("|" + "-".repeat(27) + "|" + "-".repeat(10) + "|" + "-".repeat(10) + "|" +
                "-".repeat(10) + "|" + "-".repeat(14) + "|" + "-".repeat(12) + "|" + "-".repeat(10) + "|" + "-".repeat(10) + "|\n");

        for (KPIResult r : results) {
            sb.append(r.toTableRow()).append("\n");
        }
        sb.append("=".repeat(120)).append("\n");

        // Add percentage change from baseline
        if (results.size() > 1) {
            KPIResult baseline = results.get(0);
            sb.append("\nPercentage change from baseline:\n");
            sb.append(String.format("| %-25s | %8s | %8s | %8s | %12s | %10s |%n",
                    "Scenario", "Avg TT", "P95 TT", "Peak TT", "VKT", "VHT"));
            sb.append("|" + "-".repeat(27) + "|" + "-".repeat(10) + "|" + "-".repeat(10) + "|" +
                    "-".repeat(10) + "|" + "-".repeat(14) + "|" + "-".repeat(12) + "|\n");
            for (int i = 1; i < results.size(); i++) {
                KPIResult r = results.get(i);
                sb.append(String.format("| %-25s | %+7.2f%% | %+7.2f%% | %+7.2f%% | %+11.2f%% | %+9.2f%% |%n",
                        r.scenarioName,
                        pctChange(baseline.avgTravelTimeSec, r.avgTravelTimeSec),
                        pctChange(baseline.p95TravelTimeSec, r.p95TravelTimeSec),
                        pctChange(baseline.peakHourAvgTravelTimeSec, r.peakHourAvgTravelTimeSec),
                        pctChange(baseline.vkt, r.vkt),
                        pctChange(baseline.vht, r.vht)));
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private static double pctChange(double baseline, double scenario) {
        if (baseline == 0) return 0;
        return ((scenario - baseline) / baseline) * 100.0;
    }

    /**
     * Write KPI results to a CSV file.
     */
    public static void writeCSV(List<KPIResult> results, String outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("scenario,avg_travel_time_min,p95_travel_time_min,peak_hour_avg_travel_time_min,vkt_km,vht_hours,num_trips,num_agents");
            for (KPIResult r : results) {
                writer.printf("%s,%.2f,%.2f,%.2f,%.0f,%.1f,%d,%d%n",
                        r.scenarioName,
                        r.avgTravelTimeSec / 60.0,
                        r.p95TravelTimeSec / 60.0,
                        r.peakHourAvgTravelTimeSec / 60.0,
                        r.vkt,
                        r.vht,
                        r.numTrips,
                        r.numAgents);
            }
        }
    }
}
