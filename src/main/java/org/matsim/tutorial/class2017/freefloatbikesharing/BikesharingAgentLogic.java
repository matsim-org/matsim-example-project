/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.tutorial.class2017.freefloatbikesharing;

import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.contrib.dynagent.StaticDynActivity;
import org.matsim.contrib.dynagent.StaticPassengerDynLeg;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.DynAgent.ParkingDynLeg;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.tutorial.class2017.freefloatbikesharing.setup.BikeSharingManager;
import org.matsim.vehicles.Vehicle;


/**
 * @author jbischoff
 *
 */
public class BikesharingAgentLogic implements DynAgentLogic {

	public enum LastAgentActionState  {
			
			NONBIKETRIP,	// non-car trip arrival: start Activity
			BIKETRIP, // car-trip arrival: add park-car activity 
			ACTIVITY, // ordinary activity: get next Leg, if car: go to car, otherwise add ordinary leg by other mode
			WALKTOBIKE, // walk-leg to car: add unpark activity
			UNPARKACTIVITY // unpark activity: find the way to the next route & start leg
	}
	protected LastAgentActionState lastActionState;
	protected DynAgent agent;
	protected Iterator<PlanElement> planElemIter;
	protected PlanElement currentPlanElement;
	protected WalkLegFactory walkLegFactory;
	protected MobsimTimer timer;
	protected EventsManager events;
	protected Id<Vehicle> currentlyAssignedBikeVehicleId = null;
	private BikeSharingManager manager;
	private Network network;
	private LeastCostPathCalculator lcp;

	/**
	 * @param plan
	 *            (always starts with Activity)
	 * 
	 */
	public BikesharingAgentLogic(Plan plan, WalkLegFactory walkLegFactory, EventsManager events,  MobsimTimer timer, BikeSharingManager manager, Network network, LeastCostPathCalculator lcp) {
		planElemIter = plan.getPlanElements().iterator();
		this.walkLegFactory = walkLegFactory;
		this.timer = timer;
		this.events = events;
		this.manager = manager;
		this.network = network;
		lcp = lcp;
		

		
		
	}

	@Override
	public DynActivity computeInitialActivity(DynAgent adapterAgent) {
		this.agent = adapterAgent;
		this.lastActionState = LastAgentActionState.ACTIVITY;
		this.currentPlanElement = planElemIter.next();
		Activity act = (Activity) currentPlanElement;
		//TODO: assume something different regarding initial parking location
		
		return new StaticDynActivity(act.getType(), act.getEndTime());
	}

	@Override
	public DynAgent getDynAgent() {
		return agent;
	}

	@Override
	public DynAction computeNextAction(DynAction oldAction, double now) {
		
		switch (lastActionState){
		
		
		}
		throw new RuntimeException("unreachable code");

	}

	

}
