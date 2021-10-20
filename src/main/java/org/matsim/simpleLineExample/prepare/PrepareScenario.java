package org.matsim.simpleLineExample.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PrepareScenario {
    private static final Logger log = Logger.getLogger(PrepareScenario.class);


    public static void main(String[] args) {
        PrepareScenario.Input input = new PrepareScenario.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Output directory: " + input.outputDir);

        Path outputPath = Paths.get(input.outputDir);

        // create network
        CreateNetwork.writeNetwork(CreateNetwork.createSimpleStationNetwork(), outputPath);

        // create population
        CreatePopulation.writePopulation(CreatePopulation.createSimplePopulation(), outputPath);

        // create config
        // CreateConfig.writeConfig(CreateConfig.modifyConfig(), outputPath);

        // create vehicle types
        CreateVehicleTypes.writeVehiclesFile(CreateVehicleTypes.create(), outputPath);

    }

    private static class Input {

        @Parameter(names = "-outputDir")
        private String outputDir;

    }
}
