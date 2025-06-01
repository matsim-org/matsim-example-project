package org.matsim.project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import static org.matsim.core.config.groups.RoutingConfigGroup.*;
import static org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.*;
import static org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;

class RunMultimodalMatsim{
	private static final Logger log = LogManager.getLogger( RunMultimodalMatsim.class );

	public static void main( String[] args ){

		Config config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );

		// avoid errors and warnings:
		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.routing().setAccessEgressType( AccessEgressType.accessEgressModeToLink );
		config.routing().setRoutingRandomness( 0. );
		config.qsim().setUsePersonIdForMissingVehicleId( false );

		config.controller().setLastIteration( 1 );
//		config.global().setRandomSeed( 4712 );

		config.replanning().clearStrategySettings();
		config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultStrategy.SubtourModeChoice ).setWeight( 0.1 ) );
		config.replanning().addStrategySettings( new StrategySettings().setStrategyName( DefaultSelector.ChangeExpBeta ).setWeight( 0.9 ) );

		String[] modes = new String[]{ TransportMode.car, "pedelec" };
		config.subtourModeChoice().setModes( modes );

		config.scoring().addModeParams( new ScoringConfigGroup.ModeParams( "pedelec" ) );

		// everything above is already debugged with teleportation

		// make the router generate network routes for all modes specified in the argument:
		config.routing().setNetworkModes( CollectionUtils.stringArrayToSet( modes ) );

		// make the qsim simulate all modes specified in the argument on the network:
		config.qsim().setMainModes( CollectionUtils.stringArrayToSet( modes ) );

		// use the mode vehicles from below:
		config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );

		// allow faster vehicles to pass slower vehicles:
//		config.qsim().setLinkDynamics( QSimConfigGroup.LinkDynamics.PassingQ );


		//--
		Scenario scenario = ScenarioUtils.loadScenario( config );

		// create one "mode vehicle" per mode, where we can set the speed:
		final Vehicles vehicles = scenario.getVehicles();
		VehiclesFactory vf = vehicles.getFactory();
		vehicles.addVehicleType( vf.createVehicleType( Id.create( "pedelec", VehicleType.class ) ).setNetworkMode( "pedelec" ).setMaximumVelocity( 15./3.6 ) );
		vehicles.addVehicleType( vf.createVehicleType( Id.create( "car", VehicleType.class ) ).setNetworkMode( "car" ).setMaximumVelocity( 200/3.6 ) );

		// annotate the network links with the modes we need
		for( Link link : scenario.getNetwork().getLinks().values() ){
			link.setAllowedModes( CollectionUtils.stringArrayToSet( modes ) );
		}

		// remove all routes so that a re-route is forced:
		for( Person person : scenario.getPopulation().getPersons().values() ){
			for( Plan plan : person.getPlans() ){
				for( PlanElement planElement : plan.getPlanElements() ){
					if ( planElement instanceof Leg) {
						((Leg) planElement).setRoute( null );
					}
				}
			}
		}


		Controler controler = new Controler( scenario );

		controler.addOverridingModule( new OTFVisLiveModule() );
		OTFVisConfigGroup otfConfig = ConfigUtils.addOrGetModule( config, OTFVisConfigGroup.class );
		otfConfig.setDrawNonMovingItems( true );
		otfConfig.setShowTeleportedAgents( true );
		otfConfig.setAgentSize( 200 );

		controler.run();

	}
}
