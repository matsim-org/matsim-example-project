package org.matsim.tutorial.class2017.modifyPlans;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class GenerateAgentsWithSubpopulations {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Population population = scenario.getPopulation();
		PopulationFactory f = population.getFactory();
		Random r = new Random();
		for (int i = 0; i<1000; i++){
		
			Person p = f.createPerson(Id.createPersonId("person"+i));
			population.addPerson(p);
			Plan plan = f.createPlan();
			p.addPlan(plan);
			
			Coord startCoord = new Coord(4589684.420824193, 5821530.644786197);
			
			Activity act0 = f.createActivityFromCoord("home", startCoord);
			act0.setEndTime(8*3600 + i);
			plan.addActivity(act0);
			Leg leg = f.createLeg("car");
			plan.addLeg(leg);
			int xVariation = r.nextInt(200);
			int yVariation = r.nextInt(200);
			Coord endCoord = new Coord(4598728.085634814+xVariation, 5820898.31643878+yVariation);
			
			Activity act1 = f.createActivityFromCoord("home", endCoord);
			plan.addActivity(act1);
			
			if (i%2 == 0){
				//every second agent is in a subpopulation
				scenario.getPopulation().getPersonAttributes().putAttribute(p.getId().toString(), "subpopulation", "evenStudents");
			}
			
		}
		new PopulationWriter(population).write("mytestpopulation.xml");
		new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile("mytestpopulation_attributes.xml");
	}
	
}
