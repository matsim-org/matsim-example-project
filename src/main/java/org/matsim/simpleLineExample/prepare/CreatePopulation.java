package org.matsim.simpleLineExample.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CreatePopulation {
    private static final Logger log = Logger.getLogger(CreatePopulation.class);
    private static final String POP_FILE_NAME = "population.xml";


    public static void main(String[] args) {
        // this method creates a simple population
        // take input argument

        CreatePopulation.Input input = new CreatePopulation.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Output directory: " + input.outputDir);

        CreatePopulation.writePopulation(CreatePopulation.createPopulation(), Paths.get(input.outputDir));

    }


    public static Population createPopulation(){
        Population pop = PopulationUtils.createPopulation(ConfigUtils.createConfig());;
        pop.addPerson(createPerson(
                pop.getFactory(),
                new Coord(0,0),
                new Coord(280, 0),
                TransportMode.walk,
                0.,
                1000.,
                "p_1"
                ));

        return pop;
    }


    private static Plan createPlan(PopulationFactory fac, Coord origin, Coord destination, String mode, double originEndTime, double destinationStartTime) {

        // create a plan with chain activity -> leg -> activity
        Plan plan = fac.createPlan();

        Activity originActivity = fac.createActivityFromCoord("origin", origin);
        originActivity.setEndTime(originEndTime);
        plan.addActivity(originActivity);

        Leg toDestination = fac.createLeg(mode);
        plan.addLeg(toDestination);

        Activity destinationActivity = fac.createActivityFromCoord("destination", destination);
        destinationActivity.setStartTime(destinationStartTime);
        plan.addActivity(destinationActivity);

        return plan;
    }


    private static Person createPerson(PopulationFactory fac, Coord origin, Coord destination, String mode, double originEndTime, double destinationStartTime, String id){
        Person person = fac.createPerson(Id.createPersonId(id));
        Plan plan = createPlan(fac, origin, destination, mode, originEndTime, destinationStartTime);
        person.addPlan(plan);
        return person;

    }


    public static void writePopulation(Population pop, Path path) {
        log.info("Writing population to " + path.resolve(POP_FILE_NAME));
        new PopulationWriter(pop).write(path.resolve(POP_FILE_NAME).toString());

        log.info("");
        log.info("Finished \uD83C\uDF89");
    }


    private static class Input {

        @Parameter(names = "-outputDir")
        private String outputDir;

    }
}
