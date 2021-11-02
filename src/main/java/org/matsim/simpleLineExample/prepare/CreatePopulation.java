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
    private static final String POP_FILE_NAME = "plans.xml";


    public static void main(String[] args) {
        // this method creates a simple population
        // take input argument

        CreatePopulation.Input input = new CreatePopulation.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Output directory: " + input.outputDir);

        CreatePopulation.writePopulation(CreatePopulation.createSimplePopulation(), Paths.get(input.outputDir));

    }

    public static Population createSimplePopulation(){
        Population pop = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        PopulationFactory fac = pop.getFactory();

        // disembark 4711 tr_1_1 -> ex_1_3
        for (int i = 0; i < 20; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,160), new Coord(200, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_2 -> ex_1_3
        for (int i = 20; i < 30; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,140), new Coord(200, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_3 -> ex_1_3
        for (int i = 30; i < 35; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,120), new Coord(200, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_4 -> ex_1_3
        for (int i = 35; i < 40; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,100), new Coord(200, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_1 -> ex_1_5
        for (int i = 40; i < 60; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,160), new Coord(0, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_2 -> ex_1_5
        for (int i = 60; i < 70; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,140), new Coord(0, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_3 -> ex_1_5
        for (int i = 70; i < 75; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,120), new Coord(0, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_4 -> ex_1_5
        for (int i = 75; i < 80; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,100), new Coord(0, 260), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_1 -> ex_2_3
        for (int i = 80; i < 85; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,160), new Coord(200, 0), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_2 -> ex_2_3
        for (int i = 85; i < 90; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,140), new Coord(200, 0), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_3 -> ex_2_3
        for (int i = 90; i < 100; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,120), new Coord(200, 0), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_4 -> ex_2_3
        for (int i = 100; i < 120; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,100), new Coord(200, 0), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_1 -> ex_2_5
        for (int i = 120; i < 125; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,160), new Coord(0, 0), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_2 -> ex_2_5
        for (int i = 125; i < 130; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,140), new Coord(0, 0), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_3 -> ex_2_5
        for (int i = 130; i < 140; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,120), new Coord(0, 0), TransportMode.car, 25260., 26000.));
        }

        // disembark 4711 tr_1_4 -> ex_2_5
        for (int i = 140; i < 160; i++) {
            pop.addPerson(createPerson(fac, PersonType.ZA, "disembark_4711_" + i, new Coord(150,100), new Coord(0, 0), TransportMode.car, 25260., 26000.));
        }

        return pop;
    }




    private static Plan createPlan(PopulationFactory fac, String origin, String destination, Coord originCoord, Coord destinationCoord, String mode, double originEndTime, double destinationStartTime) {

        // create a plan with chain activity -> leg -> activity
        Plan plan = fac.createPlan();

        Activity originActivity = fac.createActivityFromCoord(origin, originCoord);
        originActivity.setEndTime(originEndTime);
        plan.addActivity(originActivity);

        Leg toDestination = fac.createLeg(mode);
        plan.addLeg(toDestination);

        Activity destinationActivity = fac.createActivityFromCoord(destination, destinationCoord);
        destinationActivity.setStartTime(destinationStartTime);
        plan.addActivity(destinationActivity);

        return plan;
    }


    private static Person createPerson(PopulationFactory fac, PersonType type, String id, Coord origin, Coord destination, String mode, double originEndTime, double destinationStartTime){
        Person person = fac.createPerson(Id.createPersonId(id));

        person.getAttributes().putAttribute("subpopulation", type.toString());

        String originActivity = null;
        String destinationActivity = null;

        switch (type){
            case QE:
                originActivity = "station_entrance";
                destinationActivity = "train_boarding";
                break;

            case ZA:
                originActivity = "train_disembarking";
                destinationActivity = "station_exit";
                break;

            case UM:
                originActivity = "train_disembarking";
                destinationActivity = "train_boarding";
                break;

        }

        Plan plan = createPlan(fac, originActivity, destinationActivity, origin, destination, mode, originEndTime, destinationStartTime);
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

    private enum PersonType{
        QE,
        ZA,
        UM
    }
}
