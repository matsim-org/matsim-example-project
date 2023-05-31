package org.matsim.shared_mobility;

import org.apache.commons.configuration.ConfigurationUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
public class Main {

	public static void main(String[] args) {
		
		// shared autonomous mobilty main class 
		
		String filename = "scenarios/siouxfalls-2014/config.xml";
		
		Config config = ConfigUtils.loadConfig(filename,  new DvrpConfigGroup(), new MultiModeTaxiConfigGroup());
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.controler().setLastIteration(1);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		
		/*
		 * Scenario scenario = ScenarioUtils.loadScenario(config);
		 * 
		 * 
		 * VehicleType avType =
		 * VehicleUtils.createVehicleType(Id.create("autonomousVehicleType",
		 * VehicleType.class ) ); avType.setFlowEfficiencyFactor(2.0);
		 * scenario.getVehicles().addVehicleType(avType);
		 * 
		 * for (int i = 0; i < scenario.getPopulation().getPersons().size(); i++) {
		 * //agents with AV Id<Vehicle> vid = Id.createVehicleId("AV_" + i); Vehicle v =
		 * scenario.getVehicles().getFactory().createVehicle(vid, avType);
		 * scenario.getVehicles().addVehicle(v); }
		 * 
		 * Controler controler = new Controler(scenario);
		 */

		TaxiControlerCreator.createControler(config, false).run();

	}

}
