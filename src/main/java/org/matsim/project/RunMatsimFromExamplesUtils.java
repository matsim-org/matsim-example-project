package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.controler.Controler;


import java.net.URL;

class RunMatsimFromExamplesUtils{

	public static void main( String[] args ){

		// need to add matsim-examples in pom.xml for this class to work

//		URL context = org.matsim.examples.ExamplesUtils.getTestScenarioURL( "equil" );
//		URL url = IOUtils.newUrl( context, "config.xml" );
//
//		Config config = ConfigUtils.loadConfig( url );
//
//		// ---
//
//		Scenario scenario = ScenarioUtils.loadScenario( config );
//
//		// ---
//
//		Controler controler = new Controler( scenario );
//
//		// ---
//
//		controler.run();

	}

}
