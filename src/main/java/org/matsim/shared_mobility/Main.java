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
		
		Config config = ConfigUtils.loadConfig(filename,  new DvrpConfigGroup(), new MultiModeDrtConfigGroup(), new OTFVisConfigGroup());
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Randomly decrease populations from to check waiting time, keeping drt vehciles constant (10 min avg waiting time).
		int popSize = scenario.getPopulation().getPersons().size();
		
		
		
	    Controler controler = DrtControlerCreator.createControler(config, false);
	
		controler.run();
		 

	}

}
