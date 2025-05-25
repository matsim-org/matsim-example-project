package org.matsim.project;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import static org.matsim.core.config.groups.RoutingConfigGroup.*;
import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

class RunMultimodalMatsim{

	public static void main( String[] args ){

		Config config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );

		// avoid errors and warnings:
		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.routing().setAccessEgressType( AccessEgressType.accessEgressModeToLink );
		config.routing().setRoutingRandomness( 0. );
		config.qsim().setUsePersonIdForMissingVehicleId( false );

		config.controller().setLastIteration( 0 );

//		config.replanning().clearStrategySettings();
//		config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultStrategy.SubtourModeChoice ).setWeight( 0.1 ) );
//		config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 0.9 ) );
//
		String[] modes = new String[]{ TransportMode.car, "pedelec" };
//		config.subtourModeChoice().setModes( modes );

//		config.routing().clearTeleportedModeParams();
//		config.routing().addTeleportedModeParams( new TeleportedModeParams( TransportMode.walk ).setTeleportedModeSpeed( 4*3.6 ) );
//		config.routing().addTeleportedModeParams( new TeleportedModeParams( "pedelec" ).setTeleportedModeSpeed( 15.*3.6 ) );

		config.routing().setNetworkModes( CollectionUtils.stringArrayToSet( modes ) );

		config.scoring().addModeParams( new ScoringConfigGroup.ModeParams( "pedelec" ) );

		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		VehiclesFactory vf = scenario.getVehicles().getFactory();
		scenario.getVehicles().addVehicleType( vf.createVehicleType( Id.create("pedelec", VehicleType.class ) ) );
		scenario.getVehicles().addVehicleType( vf.createVehicleType( Id.create(TransportMode.car, VehicleType.class ) ) );

		for( Link link : scenario.getNetwork().getLinks().values() ){
			link.setAllowedModes( CollectionUtils.stringArrayToSet( modes ) );
		}

		for( Person person : scenario.getPopulation().getPersons().values() ){
			for( PlanElement planElement : person.getSelectedPlan().getPlanElements() ){
				if ( planElement instanceof Leg ) {
					((Leg) planElement).setMode( "pedelec" );
				}
			}
		}
		PopulationUtils.checkRouteModeAndReset( scenario.getPopulation(), scenario.getNetwork() );

		Controler controler = new Controler( scenario );

		controler.run();

	}
}
