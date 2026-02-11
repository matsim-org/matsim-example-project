package org.matsim.project.cologne;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;

/**
 * Generates a synthetic population for Cologne (Köln) traffic simulation.
 *
 * Population characteristics:
 * - 5000 agents (scalable)
 * - Home locations: distributed across residential areas (left bank dominant)
 * - Work locations: concentrated in city center, Deutz business district, and distributed
 * - Activity pattern: home -> work -> home
 * - Departure times: normally distributed around 7:30 AM (morning peak)
 * - Mode: car only (simplification for this scenario)
 *
 * This is a synthetic population — not based on census microdata.
 */
public class ColognePopulationGenerator {

    private static final double CENTER_X = 356000.0;
    private static final double CENTER_Y = 5645000.0;
    private static final double RHINE_X = CENTER_X + 500.0;

    // Default number of agents
    private static final int DEFAULT_NUM_AGENTS = 5000;

    // Departure time distribution parameters (seconds from midnight)
    private static final double MEAN_DEPARTURE_TIME = 7.5 * 3600;  // 7:30 AM
    private static final double STD_DEPARTURE_TIME = 45 * 60;       // 45 min std dev
    private static final double WORK_DURATION = 8.0 * 3600;         // 8 hours

    private final Random random;
    private final int numAgents;

    public ColognePopulationGenerator(int numAgents) {
        this.numAgents = numAgents;
        this.random = new Random(12345);
    }

    public ColognePopulationGenerator() {
        this(DEFAULT_NUM_AGENTS);
    }

    public Population generatePopulation(Network network) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        PopulationFactory factory = population.getFactory();

        // Collect all links for location assignment
        List<Link> allLinks = new ArrayList<>(network.getLinks().values());
        List<Link> residentialLinks = new ArrayList<>();
        List<Link> workLinks = new ArrayList<>();

        // Classify links by location
        for (Link link : allLinks) {
            Coord midpoint = getMidpoint(link);
            double distFromCenter = getDistance(midpoint, new Coord(CENTER_X, CENTER_Y));

            // Residential: mostly in middle/outer ring areas
            if (distFromCenter > 800 && distFromCenter < 8000) {
                residentialLinks.add(link);
            }
            // Work: city center + business districts
            if (distFromCenter < 4000) {
                workLinks.add(link);
            }
        }

        // If classification yields too few links, use all
        if (residentialLinks.size() < 20) residentialLinks = allLinks;
        if (workLinks.size() < 20) workLinks = allLinks;

        for (int i = 0; i < numAgents; i++) {
            Person person = factory.createPerson(Id.createPersonId("person_" + i));

            // Assign home and work locations
            Link homeLink = residentialLinks.get(random.nextInt(residentialLinks.size()));
            Link workLink = workLinks.get(random.nextInt(workLinks.size()));

            // Ensure home != work
            while (workLink.getId().equals(homeLink.getId())) {
                workLink = workLinks.get(random.nextInt(workLinks.size()));
            }

            // Generate departure time (normal distribution around morning peak)
            double departureTime = MEAN_DEPARTURE_TIME + random.nextGaussian() * STD_DEPARTURE_TIME;
            departureTime = Math.max(5.0 * 3600, Math.min(10.0 * 3600, departureTime)); // clamp 5AM-10AM

            double returnTime = departureTime + WORK_DURATION + random.nextGaussian() * 30 * 60;
            returnTime = Math.max(departureTime + 6 * 3600, Math.min(22 * 3600, returnTime)); // at least 6h work

            // Create plan
            Plan plan = factory.createPlan();

            // Home activity (morning)
            Activity homeAct1 = factory.createActivityFromLinkId("home", homeLink.getId());
            homeAct1.setEndTime(departureTime);
            plan.addActivity(homeAct1);

            // Leg to work
            Leg legToWork = factory.createLeg("car");
            plan.addLeg(legToWork);

            // Work activity
            Activity workAct = factory.createActivityFromLinkId("work", workLink.getId());
            workAct.setEndTime(returnTime);
            plan.addActivity(workAct);

            // Leg home
            Leg legToHome = factory.createLeg("car");
            plan.addLeg(legToHome);

            // Home activity (evening)
            Activity homeAct2 = factory.createActivityFromLinkId("home", homeLink.getId());
            plan.addActivity(homeAct2);

            person.addPlan(plan);
            population.addPerson(person);
        }

        return population;
    }

    private Coord getMidpoint(Link link) {
        Coord from = link.getFromNode().getCoord();
        Coord to = link.getToNode().getCoord();
        return new Coord((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2);
    }

    private double getDistance(Coord a, Coord b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));
    }

    public void writePopulation(Population population, String outputPath) {
        new PopulationWriter(population).write(outputPath);
    }

    public static void main(String[] args) {
        // Load previously generated network
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile("scenarios/cologne/input/network.xml");

        ColognePopulationGenerator generator = new ColognePopulationGenerator();
        Population population = generator.generatePopulation(scenario.getNetwork());
        System.out.println("Generated population: " + population.getPersons().size() + " agents");
        generator.writePopulation(population, "scenarios/cologne/input/plans.xml");
    }
}
