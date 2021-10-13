package org.matsim.simpleLineExample;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunMatsim {
    private static final Logger log = Logger.getLogger(RunMatsim.class);

    public static void main(String[] args) {
        RunMatsim.Input input = new RunMatsim.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Config directory: " + input.configDir);

        Config config = ConfigUtils.loadConfig(input.configDir);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);

        controler.run();

    }

    private static class Input {

        @Parameter(names = "-config")
        private String configDir;

    }
}
