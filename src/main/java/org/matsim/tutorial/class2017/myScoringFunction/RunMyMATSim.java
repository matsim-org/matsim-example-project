package org.matsim.tutorial.class2017.myScoringFunction;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Binder;

public class RunMyMATSim {

	public static void main(String[] args) {
		
		Config config = ConfigUtils.loadConfig("berlin-scenario/config.xml");
		config.controler().setOutputDirectory("output-test-with-scoring");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		
		Controler controler  = new Controler(scenario);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindScoringFunctionFactory().toInstance(new MyScoringFunctionFactory(scenario));
			}
		});
		
		controler.run();
		
	}

}
