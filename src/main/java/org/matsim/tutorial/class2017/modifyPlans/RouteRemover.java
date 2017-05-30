package org.matsim.tutorial.class2017.modifyPlans;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

public class RouteRemover {

	public static void main(String[] args) {
		Set<Id<Link>> linkIdsToBeRemoved = new HashSet<>();
		linkIdsToBeRemoved.add(Id.createLinkId("25662562_21487180"));
		
		String inputPlansFile = "C:/Users/Joschka/class2017/matsim-example-project/berlin-scenario/be_117j.output_plans.xml.gz";
		String outputPlansFile = "C:/Users/Joschka/class2017/matsim-example-project/berlin-scenario/be_117j.output_plans_removedRoutes.xml.gz";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(inputPlansFile);
		
		for (Person p: scenario.getPopulation().getPersons().values()){
			for (Plan plan : p.getPlans()){
				for (PlanElement planElement : plan.getPlanElements()){
					if (planElement instanceof Leg){
						Leg leg = (Leg) planElement;
						
						boolean removeRoute = false;
						
						if (linkIdsToBeRemoved.contains(leg.getRoute().getStartLinkId())){
							removeRoute = true;
						}
						
						if (linkIdsToBeRemoved.contains(leg.getRoute().getEndLinkId())){
							removeRoute = true;
						}
						
						
						if (leg.getMode().equals("car")){
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							for (Id<Link> linkId : route.getLinkIds()){
								
								if (linkIdsToBeRemoved.contains(linkId)){
									removeRoute = true;
									break;
								}
								
							}
						}
						
						if (removeRoute){
							leg.setRoute(null);;
						}
					}
					else if (planElement instanceof Activity){
						Activity act = (Activity) planElement;
						if (linkIdsToBeRemoved.contains(act.getLinkId())){
							act.setLinkId(null);
						}
					}
				}
				
			}
			
		}
		new PopulationWriter(scenario.getPopulation()).write(outputPlansFile);
		
		
	}

}
