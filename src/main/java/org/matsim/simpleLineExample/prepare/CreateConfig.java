package org.matsim.simpleLineExample.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CreateConfig {
    private static final Logger log = Logger.getLogger(CreateConfig.class);
    private static final String CONFIG_FILE_NAME = "config.xml";

    public static void main(String[] args) {
        // this method creates the respective config
        // take input argument

        CreateConfig.Input input = new CreateConfig.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Output directory: " + input.outputDir);

        CreateConfig.writeConfig(CreateConfig.createConfig(), Paths.get(input.outputDir));

    }

    public static Config createConfig(){
        Config config = ConfigUtils.createConfig();
        return config;
    }


    public static void writeConfig(Config config, Path path) {
        log.info("Writing config to " + path.resolve(CONFIG_FILE_NAME));
        new ConfigWriter(config).write(path.resolve(CONFIG_FILE_NAME).toString());

        log.info("");
        log.info("Finished \uD83C\uDF89");
    }


    private static class Input {

        @Parameter(names = "-outputDir")
        private String outputDir;

    }
}
