package org.matsim.simpleLineExample.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class CreateConfig {
    private static final Logger log = Logger.getLogger(CreateConfig.class);
    private static final String CONFIG_FILE_NAME = "config.xml";

    public static void main(String[] args) {
        // this method creates the respective config
        // take input argument

        CreateConfig.Input input = new CreateConfig.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Config template: " + input.template);
        log.info("Output directory: " + input.outputDir);

        Config config = ConfigUtils.loadConfig(input.template);
        CreateConfig.writeConfig(CreateConfig.modifyConfig(config), Paths.get(input.outputDir));

    }

    public static Config modifyConfig(Config config){

        // global
        config.global().setCoordinateSystem("Atlantis");
        config.global().setRandomSeed(4711);
        config.global().setNumberOfThreads(8);

        // controler
        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setRunId("walkOnly_test_1");

        // plans
        config.plans().setInputFile("population.xml");

        // network
        config.network().setInputFile("network.xml");

        // qsim
        config.qsim().setMainModes(Collections.singletonList(TransportMode.walk));


        return config;
    }


    public static void writeConfig(Config config, Path path) {
        log.info("Writing config to " + path.resolve(CONFIG_FILE_NAME));
        new ConfigWriter(config).write(path.resolve(CONFIG_FILE_NAME).toString());

        log.info("");
        log.info("Finished \uD83C\uDF89");
    }


    private static class Input {

        @Parameter(names = "-template")
        private String template;

        @Parameter(names = "-outputDir")
        private String outputDir;

    }
}
