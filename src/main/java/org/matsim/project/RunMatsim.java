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
package org.matsim.project;

import com.google.inject.internal.asm.$Type;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public static void main(String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}
		config.global().setInsistingOnDeprecatedConfigVersion( false );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 2 );

		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
		config.qsim().setSnapshotStyle( SnapshotStyle.kinematicWaves );

		final String mode2 = "abc";
		Set<String> networkModesAsSet = CollectionUtils.stringArrayToSet( new String [] {TransportMode.car, mode2} );

		// possibly modify config here
		config.plansCalcRoute().clearTeleportedModeParams();
		{
			PlansCalcRouteConfigGroup.TeleportedModeParams abc = new PlansCalcRouteConfigGroup.TeleportedModeParams( TransportMode.walk );
			abc.setTeleportedModeSpeed( 5./3.6 );
			config.plansCalcRoute().addTeleportedModeParams( abc );
		}
//		{
//			PlansCalcRouteConfigGroup.TeleportedModeParams abc = new PlansCalcRouteConfigGroup.TeleportedModeParams( "abc" );
//			abc.setTeleportedModeSpeed( 10. );
//			config.plansCalcRoute().addTeleportedModeParams( abc );
//		}
		{
			config.plansCalcRoute().setNetworkModes( networkModesAsSet );
		}
		{
			StrategyConfigGroup.StrategySettings abc = new StrategyConfigGroup.StrategySettings();
			abc.setStrategyName( DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode );
			abc.setWeight( 0.1 );
			config.strategy().addStrategySettings( abc );
		}
		{
			config.changeMode().setModes( networkModesAsSet.toArray(new String[0] ) );
		}
		{
			PlanCalcScoreConfigGroup.ModeParams abc = new PlanCalcScoreConfigGroup.ModeParams( mode2 );
			config.planCalcScore().addModeParams( abc );
		}

//		config.qsim().setVehiclesSource( VehiclesSource.modeVehicleTypesFromVehiclesData );

		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		{
			VehicleType vehicleType = VehicleUtils.createVehicleType( Id.create( mode2, VehicleType.class ) );
			scenario.getVehicles().addVehicleType( vehicleType );
		}
		{
			VehicleType vehicleType = VehicleUtils.createVehicleType( Id.create(TransportMode.car, VehicleType.class) );
			scenario.getVehicles().addVehicleType( vehicleType );
		}
		for( Link link : scenario.getNetwork().getLinks().values() ){
			link.setAllowedModes( networkModesAsSet );
		}
		
		// possibly modify scenario here
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;

		
		// ---
		
		controler.run();
	}
	
}
