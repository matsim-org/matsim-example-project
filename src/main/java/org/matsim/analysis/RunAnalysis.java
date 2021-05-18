package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RunAnalysis {

    public static void main(String[] args) {

        // load the network
        var network = NetworkUtils.readNetwork("C:\\Users\\Janekdererste\\Downloads\\berlin-v5.4-1pct.output_network.xml.gz");

        // create event handlers which you need for your analysis
        var beelineHandler = new BeelineDistanceHandler(network);
        var travelledDistanceHandler = new TravelledDistanceHandler(network);

        // create an events manager which will parse the events file
        var manager = EventsUtils.createEventsManager();

        // add your event handlers to the manager, so that the manager can call the handlers
        manager.addHandler(beelineHandler);
        manager.addHandler(travelledDistanceHandler);

        // actually read the events file. This step will take some time.
        EventsUtils.readEvents(manager, "C:\\Users\\Janekdererste\\Downloads\\berlin-v5.4-1pct.output_events.xml.gz");

        //Now, we have parsed all events and our event handlers have collected data. We can run different analysis now
        // Let's start with the beeline handler. Get the collected data first
        var tripsByPerson = beelineHandler.getTripDistances();

        // sort travelled distances into bins the native java way
        printDistanceBins(tripsByPerson);

        // sort travelled distnaces into bins using streams
        printDistanceBinsWithStreams(tripsByPerson);

        // print the number of trips each person has conducted
        printNumberOfTrips(tripsByPerson);

        // print the average travelled distance of each trip
        printAverageDistance(travelledDistanceHandler.getPersonToTrips());

        // one could come up with many more thing of what we could analyze only using the data of the two event handlers
        // we have here

    }

    /**
     * This method counts how many trips fall into a certain distance class. It uses an int array to keep track of
     * the counts and iterates over all person entries and all trip-distances of each person. This only uses Java
     * constructs which one should know after taking our oop class
     *
     * @param tripsByPerson
     */
    private static void printDistanceBins(Map<Id<Person>, List<Double>> tripsByPerson) {

        // we want 5 distance classes
        var distances = new int[5];

        // iterate over evey person id
        for (var entry : tripsByPerson.entrySet()) {
            var tripsList = entry.getValue();

            // iterate over each beeline distance that was collected for each trip of a person
            for (var distance : tripsList) {
                // convert the distance into the index to which the distance belongs. I.e. into which bucket does this trip fall
                var index = distanceToIndex(distance);
                // increment the count for the corresponding bucket because we have one more trips that falls into that bucket
                distances[index]++;
            }
        }

        System.out.println("################ Distances ##################");
        System.out.println("< 1000m: " + distances[0]);
        System.out.println("< 5000m: " + distances[1]);
        System.out.println("< 10000m: " + distances[2]);
        System.out.println("< 20000m: " + distances[3]);
        System.out.println("> 20000m: " + distances[4]);
    }


    /**
     * Does the same as the method above but uses streams to re-arrange the data from our handler
     * Streams were not part of the oop class but are very handy when it comes to wrangling data in Java
     *
     * @param tripsByPerson
     */
    private static void printDistanceBinsWithStreams(Map<Id<Person>, List<Double>> tripsByPerson) {
        // initialize the stream which will iterate over the List<Double> beeline distances of each person
        var distancesMap = tripsByPerson.values().stream()
                // this maps each list of beeline distances to a stream of doubles contained in that lists
                // This means in the next step we'll be iterating over all the beeline distances collected by the handler
                .flatMap(Collection::stream)
                // collect all distances in a map which has the distance bucket as key and the sum of trips for each bucket as value
                .collect(Collectors.toMap(RunAnalysis::distanceToKey, distance -> 1, Integer::sum));

        System.out.println("################ Distances streamed ##################");
        for (var entry : distancesMap.entrySet()) {
            System.out.println(entry.getKey() + entry.getValue());
        }
    }

    /**
     * This method counts the average number of trips each person conducted during a day
     *
     * @param tripsByPerson
     */
    private static void printNumberOfTrips(Map<Id<Person>, List<Double>> tripsByPerson) {
        //iterate over each List<Double> tripsDistances of each person
        var averageNumberOfTrips = tripsByPerson.values().stream()
                // map the tripList to the number of trips in that list
                .mapToInt(List::size)
                // compute the average of all list-sizes (which is the number of trips for each person)
                .average();

        System.out.println("Average number of trips: " + averageNumberOfTrips);
    }

    private static void printAverageDistance(Map<Id<Person>, List<Double>> tripsByPerson) {
        //iterate over each List<Double> tripsDistances of each person
        var averageDistanceTravelled = tripsByPerson.values().stream()
                // map each list of distances to their values
                // in the next step we'll be iterating doubles and not lists anymore
                .flatMap(Collection::stream)
                // map the Boxed Double value to a primitive double value -> this is very java specific you can google java boxed primitives if you want to know whats going on
                .mapToDouble(value -> value)
                // compute the average distance
                .average()
                // if anything goes wrong throw an error
                .orElseThrow();

        System.out.println("Average distance of trips was: " + averageDistanceTravelled);
    }

    /**
     * Converts distances into index. Using a fixed range of distances
     */
    private static int distanceToIndex(double distance) {
        if (distance < 1000) return 0;
        if (distance < 5000) return 1;
        if (distance < 10000) return 2;
        if (distance < 20000) return 3;
        return 4;
    }

    /**
     * converts distances into keys. Using a fixed range of distances
     *
     * @param distance
     * @return
     */
    private static String distanceToKey(double distance) {
        if (distance < 1000) return "< 1000: ";
        if (distance < 5000) return "< 5000: ";
        if (distance < 10000) return "< 10000: ";
        if (distance < 20000) return "< 20000: ";
        return "> 20000: ";
    }
}
