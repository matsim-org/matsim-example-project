/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.evDetour;


/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecifications;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;
import playground.vsp.ev.RunUrbanEVExample;
import playground.vsp.ev.UrbanEVConfigGroup;
import playground.vsp.ev.UrbanEVModule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * this is an example of how to run MATSim with the UrbanEV module which inserts charging activities for all legs which use a EV.
 * By default, {@link ElectricFleetUpdater} is used, which declares any vehicle as an EV
 * that has a vehicle type with HbefaTechnology set to 'electricity'.
 * At the beginning of each iteration, the consumption is estimated. Charging is planned to take place during the latest possible activity in the agent's plan
 * that fits certain criteria (ActivityType and minimum duration) and takes place before the estimated SOC drops below a defined threshold.
 */
public class RunMyUrbanEVExample{
	private static final Logger log = LogManager.getLogger(RunMyUrbanEVExample.class );
	public static void main(String[] args) {
		EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.timeProfiles = true;
		evConfigGroup.chargersFile = "chargers.xml";

		String pathToConfig = args.length > 0 ?
				args[0] :
				"test/input/org/matsim/evDetour/chessboard-config.xml";


		Config config = ConfigUtils.loadConfig(pathToConfig, evConfigGroup);
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
		RunUrbanEVExample.prepareConfig(config);
		// (if you need to change anything in the static RunUrbanEVExample methods, you need to inline them.  refactor --> inline method.)

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config);
		RunUrbanEVExample.prepareScenario( scenario );

		// ---

		Controler controler = RunUrbanEVExample.prepareControler(scenario );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.addEventHandlerBinding().toInstance( new TeleportationArrivalEventHandler(){
					@Override public void handleEvent( TeleportationArrivalEvent event ){
						log.info( event );
						// (the "travelled" event)
					}
				} );
			}
		} );

		controler.addOverridingModule( new OTFVisLiveModule() );
		// (this switches on the visualizer.  comment out if not needed)

		controler.run();
	}

}
