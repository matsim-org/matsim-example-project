package org.matsim.tutorial.class2017.mixedTraffic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;

public class RunOpenBerlinMixedTraffic {
	
	public static void main(String[] args) {
		
		String networkFile = "/Users/amit/Documents/workspace/matsim-example-project/inputBerlin_1pct/network.xml";
		String plansFile = "/Users/amit/Documents/workspace/matsim-example-project/inputBerlin_1pct/be_117j.output_plans.xml";
		String configFile = "/Users/amit/Documents/workspace/matsim-example-project/inputBerlin_1pct/config.xml";
		
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(plansFile);
		
		config.controler().setOutputDirectory("outputBerlin/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(1);
		
		// slowPt is not in the plansCalcConfigGroup
		config.planCalcScore().getOrCreateModeParams("slowPt");
		
		config.vspExperimental().setWritingOutputEvents(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// 1. allow bikes on the links
		for (Link link : scenario.getNetwork().getLinks().values()){
			Set<String> modes = new HashSet<>();
			modes.add("bike");
			modes.addAll(link.getAllowedModes());
			link.setAllowedModes(modes );
		}
		
		// 2. create vehicles type
		VehicleType car = scenario.getVehicles().getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(80/3.6);
		car.setPcuEquivalents(1.);
		scenario.getVehicles().addVehicleType(car);
		
		VehicleType bike = scenario.getVehicles().getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(15.0/3.6);
		bike.setPcuEquivalents(0.25);
		scenario.getVehicles().addVehicleType(bike);
		
		// 3. parameters for queue simulation
		Collection<String> mainModes = new ArrayList<>();
		mainModes.add("car");
		mainModes.add("bike");
		
		config.qsim().setMainModes(mainModes );
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ);
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		
		config.planCalcScore().getOrCreateModeParams("bike");

		// 4. inform the router about the main modes
		config.plansCalcRoute().setNetworkModes(mainModes);
		
		String analyzedModes = "car,bike";
		config.travelTimeCalculator().setAnalyzedModes(analyzedModes );
		config.travelTimeCalculator().setSeparateModes(true);
		
		// 5. get some bikes into the plans (assign bike to 25% of the agents randomly)
		Random random = new Random();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (random.nextDouble() > 0.25 ) continue;
			
			Plan plan = person.getSelectedPlan();
			List<PlanElement> pes = plan.getPlanElements();

			for (PlanElement pe : pes) {
				if (pe instanceof Leg) {
					((Leg) pe).setRoute(null);
					((Leg) pe).setMode("bike");
				}
			}		
		}
		
		Controler controler = new Controler(scenario);
		controler.run();
	}
}
