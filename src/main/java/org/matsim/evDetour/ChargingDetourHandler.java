package org.matsim.evDetour;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChargingDetourHandler implements ActivityStartEventHandler, ChargingStartEventHandler, PersonLeavesVehicleEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler {

	//alle Personen mit EV
	private final List <Id<Person>> personsWithEV = new ArrayList<>();
	// Liste aller Ladevorgänge
	private final List<ChargingProcess> chargingProcesses = new ArrayList<>();
	// Map der Ladevorgänge pro Person
	private final Map<Id<Person>, List<ChargingProcess>> chargingProcessesToPersons = new HashMap<>();
	private final Map<Id<Charger>, List<ChargingProcess>> chargingProcessesToChargers =new HashMap<>();
	private Network network;

	@Override
	public void handleEvent(ActivityStartEvent event){
		if (event.getActType().endsWith("car plugin interaction")){
			personsWithEV.add(event.getPersonId());
		}
	}

	public void handleEvent(ChargingStartEvent event){

	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		event.getEventType();
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		event.getLinkId();
	}
}

