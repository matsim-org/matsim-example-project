package org.matsim.tutorial.class2017.mixedTraffic;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEquilMixedTraffic {
	
	public static void main(String[] args) {
		
//		String inputDir = "/Users/amit/Documents/workspace/matsim-example-project/input/";
		String inputDir = "input/";
		
		// config and other files are taken from "https://github.com/matsim-org/matsim/tree/master/examples/scenarios/equil-mixedTraffic"
		Config config = ConfigUtils.loadConfig(inputDir+"/config-with-mode-vehicles.xml");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
		
	}
}
