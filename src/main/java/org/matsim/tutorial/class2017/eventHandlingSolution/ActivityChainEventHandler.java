package org.matsim.tutorial.class2017.eventHandlingSolution;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

public class ActivityChainEventHandler
		implements ActivityEndEventHandler, ActivityStartEventHandler, LinkEnterEventHandler {

	
	
	Map<Id<Person>,String> lastActivity = new HashMap<>();
	Set<Id<Vehicle>> traveledOverLink = new HashSet<>();
	List<String> activityChain = new ArrayList<>();
	Id<Link> myMonitoredLinkId = Id.createLinkId("9846");
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (myMonitoredLinkId.equals(event.getLinkId())){
			traveledOverLink.add(event.getVehicleId());
		}
		
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		String last = lastActivity.get(event.getPersonId());
		Id<Vehicle> vid = Id.createVehicleId(event.getPersonId());
		if (traveledOverLink.contains(vid)){
		activityChain.add(last + "-->"+ event.getActType());
		traveledOverLink.remove(vid);
		}
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		lastActivity.put(event.getPersonId(), event.getActType());
		
	}
	
	void writeActivitestoFile(String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			for (String line : activityChain){
				bw.write(line);
				bw.newLine();
			}
			bw.flush();
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
