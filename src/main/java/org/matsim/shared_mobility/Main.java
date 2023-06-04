package org.matsim.shared_mobility;

import org.apache.commons.configuration.ConfigurationUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
public class Main {

	public static void main(String[] args) {
		
		// shared autonomous mobilty main class 
		
		String test = "scenarios/siouxfalls-2014/test/config.xml";
		String filename = "scenarios/siouxfalls-2014/config.xml";
		String vehicles = "scenarios/siouxfalls-2014/vehicles_100.xml";
		String drt = "scenarios/drt/config.xml";
		
		Config config = ConfigUtils.loadConfig(drt,  new DvrpConfigGroup(), new MultiModeDrtConfigGroup(), new OTFVisConfigGroup());
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		
		
		  Scenario scenario = ScenarioUtils.loadScenario(config);
		  
			/*
			 * var drtConfig = MultiModeDrtConfigGroup.get(config).getModalElements();
			 * for(DrtConfigGroup drtConfig1 : drtConfig) {
			 * 
			 * var veh = drtConfig1.createParameterSet(vehicles);
			 * 
			 * }
			 */
		  
		  
			/*
			 * VehicleType avType =
			 * VehicleUtils.createVehicleType(Id.create("autonomousVehicleType",VehicleType.
			 * class ) ); avType.setFlowEfficiencyFactor(2.0);
			 * scenario.getVehicles().addVehicleType(avType);
			 * 
			 * for (int i = 0; i < scenario.getPopulation().getPersons().size(); i++) {
			 * //agents on lower route get AVs as vehicles, agents on upper route keep a
			 * standard vehicle (= default, if nothing is set) Id<Vehicle> vid =
			 * Id.createVehicleId("AV_" + i); Vehicle v =
			 * scenario.getVehicles().getFactory().createVehicle(vid, avType);
			 * scenario.getVehicles().addVehicle(v); }
			 */
		  boolean otfvis = false;
		  Controler controler = DrtControlerCreator.createControler(config, otfvis);
			/*
			 * Controler controler = new Controler(scenario);
			 * controler.addOverridingModule(new DvrpModule());
			 * controler.addOverridingModule(new MultiModeTaxiModule());
			 * controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(
			 * MultiModeTaxiConfigGroup.get(config)));
			 */
		  controler.run();
		 

	}

}
