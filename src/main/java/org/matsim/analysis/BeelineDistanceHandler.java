package org.matsim.analysis;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handler collects the beeline distances of each trip a person has made.
 * It listenes to activity-end and activity-start events for this. To keep things simple
 * the activity location is assumed to be at the center of the link an activity is associated with
 */
public class BeelineDistanceHandler implements ActivityEndEventHandler, ActivityStartEventHandler {

    // container to store the departure coordinate of the current trip for each person
    private final Map<Id<Person>, Coord> personToDepartureCoord = new HashMap<>();

    // the actual data container which stores a list of distances for each person
    // each trip has its own value in the list of trips which is associated with the id of a person
    private final Map<Id<Person>, List<Double>> tripDistances = new HashMap<>();

    // we need the network to know the activity locations
    private final Network network;

    // the network is passed in from outside
    public BeelineDistanceHandler(Network network) {
        this.network = network;
    }

    public Map<Id<Person>, List<Double>> getTripDistances() {
        return tripDistances;
    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {

        // filter out interaction activities
        if (isInteraction(activityEndEvent.getActType())) return;

        // if we reach here we have a real activity such as 'home' or 'work' for example
        // get the coordinate of that activity by finding the link this activity belongs to
        var coord = network.getLinks().get(activityEndEvent.getLinkId()).getCoord();

        // store the person's id and the coordinate of the ended activity. This works like this
        // because every person will only conduct one trip at a time.
        personToDepartureCoord.put(activityEndEvent.getPersonId(), coord);
    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {

        // filter out interaction activities
        if (isInteraction(activityStartEvent.getActType())) return;

        // get the coordinate of the last activity
        // also remove the entry because our person is not conducting a trips anymore
        var startCoord = personToDepartureCoord.remove(activityStartEvent.getPersonId());

        // get the coordinate of the starting activity by fetching the corresponding link and using its center coordinate
        var endCoord = network.getLinks().get(activityStartEvent.getLinkId()).getCoord();

        // calculate the eucledian distance between start and end-coordinate
        var distance = CoordUtils.calcEuclideanDistance(startCoord, endCoord);
        var personId = activityStartEvent.getPersonId();

        // get the list of trips for the person's id and add the distance to that list
        // the compute if absent will put a new list into our map in case none is present yet
        tripDistances.computeIfAbsent(personId, id -> new ArrayList<>()).add(distance);
    }

    private boolean isInteraction(String type) {
        return type.endsWith("interaction");
    }
}
