/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.tutorial.class2017.eventHandlingSolution;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

public class MyCarDistanceEvaluator implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler {

	// the network contains all links, we require their length
	final private Network network;
	private int[] distanceBins = new int[31];
	final private Map<Id<Person>,Double> currentTraveledDistance = new HashMap<>();
	
	public MyCarDistanceEvaluator(Network network) {

		this.network = network;
		
	}
	
	public int[] getDistanceBins() {
		return distanceBins;
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getPersonId().toString().startsWith("pt")){
			return;
		}
		
		if (event.getLegMode().equals("car")){
			currentTraveledDistance.put(event.getPersonId(), 0.0);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		//This will work as long as people are driving in cars named exactly like them.
		if (currentTraveledDistance.containsKey(personId)){
			double distanceTraveledSoFar  = currentTraveledDistance.get(personId);
			double linkLength = network.getLinks().get(event.getLinkId()).getLength();
			currentTraveledDistance.put(personId, linkLength+distanceTraveledSoFar);
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (currentTraveledDistance.containsKey(event.getPersonId())){
			double traveledDistance = currentTraveledDistance.remove(event.getPersonId()) / 1000.0;
			int bin = (int) traveledDistance;
			if (bin>30) {
				bin = 30;
			}
			distanceBins[bin]++;
		}
	}

	

	@Override
	public void reset(int iteration) {
		// this method is called before each iteration starts. No need to fill anything if you use your EventHandler only in Postprocessing
	}
	
}
