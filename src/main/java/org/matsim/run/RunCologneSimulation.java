package org.matsim.run;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;

/**
 * Main entry point for the Cologne traffic simulation scenario with SimWrapper visualization.
 *
 * <p>Usage:</p>
 * <pre>
 *   java -cp matsim-example-project.jar org.matsim.run.RunCologneSimulation
 *   java -cp matsim-example-project.jar org.matsim.run.RunCologneSimulation --config:controller.lastIteration=50
 * </pre>
 *
 * <p>After the simulation completes, open the output directory with
 * <a href="https://simwrapper.app">SimWrapper</a> to view interactive dashboards.</p>
 */
@CommandLine.Command(
	header = ":: Cologne Traffic Simulation ::",
	version = "1.0"
)
public class RunCologneSimulation extends MATSimApplication {

	public static final String CRS = "EPSG:25832";
	public static final String MAP_CENTER = "6.9578,50.9375";
	public static final int MAP_ZOOM = 11;

	public RunCologneSimulation() {
		super("scenarios/cologne/config.xml");
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunCologneSimulation.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		if ("Atlantis".equals(config.global().getCoordinateSystem())) {
			config.global().setCoordinateSystem(CRS);
		}

		SimWrapperConfigGroup sw = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		sw.setDefaultDashboards(SimWrapperConfigGroup.Mode.enabled);
		sw.setSampleSize(config.qsim().getFlowCapFactor());

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		// Cologne-specific scenario modifications can go here
	}

	@Override
	protected void prepareControler(Controler controler) {
		controler.addOverridingModule(new SimWrapperModule());
	}
}
