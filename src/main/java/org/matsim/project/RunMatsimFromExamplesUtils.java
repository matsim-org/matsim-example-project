package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.net.URL;

class RunMatsimFromExamplesUtils {

	private static final String outputPath = "C:\\Users\\Janekdererste\\Desktop\\equil\\output";

	public static void main(String[] args) {

		URL context = org.matsim.examples.ExamplesUtils.getTestScenarioURL("equil");
		URL url = IOUtils.extendUrl(context, "config.xml");

		Config config = ConfigUtils.loadConfig(url);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(outputPath);

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// remove length from links because they are not correct in the equil scenario

		for (var link : scenario.getNetwork().getLinks().values()) {

			var euclideanLength = CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
			link.setLength(euclideanLength);
		}

		// ---

		Controler controler = new Controler(scenario);

		// ---

		controler.run();

	}

}
