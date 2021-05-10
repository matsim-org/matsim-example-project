package org.matsim.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.core.events.EventsUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class RunMainModeAnalysis {

    public static void main(String[] args) {

        var manager = EventsUtils.createEventsManager();
        var handler = new MainModeHandler();
        manager.addHandler(handler);
        EventsUtils.readEvents(manager, "C:\\Users\\Janekdererste\\Downloads\\berlin-v5.5-1pct.output_events.xml.gz");

        var personTrips = handler.getPersonTrips();
        var modes = personTrips.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(mode -> mode, mode -> 1, Integer::sum));

        var totalTrips = modes.values().stream()
                .mapToDouble(d -> d)
                .sum();

        try (var writer = Files.newBufferedWriter(Paths.get("C:\\Users\\Janekdererste\\Desktop\\modes.csv")); var printer = CSVFormat.DEFAULT.withDelimiter(',').withHeader("Mode", "Count", "Share").print(writer)) {

            for (var entry : modes.entrySet()) {
                printer.printRecord(entry.getKey(), entry.getValue(), entry.getValue() / totalTrips);
            }

            printer.printRecord("total", totalTrips, 1.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
