package org.matsim.tutorial.class2017.freefloatbikesharing.setup;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

public class BikeSharingManager {

	Map<Id<Vehicle>,Id<Link>> bikeLocations = new HashMap<>();
	
	@Inject
	public BikeSharingManager(Network network) {
		// distribute bikes somehow in constructor
		// TODO Auto-generated constructor stub
	}
	
	public Tuple<Id<Link>,Id<Vehicle>> findAndReserveNearestBike(Id<Link> currentLocation){
		
		//
		return null;
		
	}
	
	public void returnBike(Id<Link> linkId, Id<Vehicle> bikeId){
		//
	}
	
	
}
