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
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.net.URL;
import java.util.*;

import static org.matsim.core.config.groups.PlanCalcScoreConfigGroup.*;
import static org.matsim.core.config.groups.PlansCalcRouteConfigGroup.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.*;

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public static void main(String[] args) {

		Config config= ConfigUtils.loadConfig( "scenarios/equil/config.xml" );

//		URL url = ExamplesUtils.getTestScenarioURL( "chessboard" );
//		URL configUrl = IOUtils.extendUrl( url, "config.xml" );
//		Config config = ConfigUtils.loadConfig( configUrl );

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration( 1 );

		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );

		config.planCalcScore().addActivityParams( new ActivityParams( "w" ).setTypicalDuration( 8 * 3600. ).setClosingTime( 20*3600. ).setOpeningTime( 7*3600. ).setLatestStartTime( 10*3600. ) );
		config.planCalcScore().addActivityParams( new ActivityParams( "h" ).setTypicalDuration( 8 * 3600. ) );

		config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 0.7 ) );

		// multi-modal starts here

		final String newMode = "eScooter";
		String[] modes = { TransportMode.car, newMode};

		// mode innovation:
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName( DefaultStrategy.ChangeSingleTripMode ).setWeight( 0.3 ) );

		config.changeMode().setModes( modes );

		// routing:
//		config.plansCalcRoute().setClearingDefaultModeRoutingParams( true );
//		config.plansCalcRoute().addTeleportedModeParams( new TeleportedModeParams( TransportMode.walk ).setTeleportedModeSpeed( 4.5/3.6 ) );
//
		config.plansCalcRoute().setNetworkModes( List.of( modes ) );

		// scoring:
//		config.planCalcScore().addModeParams( new ModeParams( newMode ) );

//		config.planCalcScore().setFractionOfIterationsToStartScoreMSA( 0.8 );
//		config.strategy().setFractionOfIterationsToDisableInnovation( 0.8 );

		// qsim
//		config.qsim().setMainModes( List.of( modes ) );

		// ======================

		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// possibly modify scenario here

//		for( Link link : scenario.getNetwork().getLinks().values() ){
//			link.setAllowedModes( Set.of( modes ) );
//		}

		// ======================

		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;
//		controler.addOverridingModule( new SimWrapperModule() );
		
		controler.run();
	}
	
}
