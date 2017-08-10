package org.matsim.tutorial.class2017.routingModule;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


public class RunCustomCarRouting {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig("../sampledata/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		

		
		controler.addOverridingModule(new AbstractModule() {
						
			@Override
			public void install() {
				
				addRoutingModuleBinding("car").to(MyCarRoutingModule.class).asEagerSingleton();
				
			}
		});
		
		controler.run();
		
		
	}
}
