package org.matsim.simpleLineExample.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.NetworkUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class CreateNetwork {
    private static final Logger log = Logger.getLogger(CreateNetwork.class);

    private static final double WALK_SPEED = 1.5; // walk speed in m/s
    private static final Set<String> ALLOWED_MODES = Set.of("walk_reg");
    private static final String NETWORK_FILE_NAME = "network.xml";



    public static void main(String[] args) {
        // this method creates a simple triangular network
        // take input argument

        CreateNetwork.Input input = new CreateNetwork.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Output directory: " + input.outputDir);

        CreateNetwork.writeNetwork(CreateNetwork.createNetwork(), Paths.get(input.outputDir));

    }

    public static Network createNetwork(){

        // create an empty network
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        // create nodes
        Node n0 = fac.createNode(Id.createNodeId("n0"), new Coord(0, 200));
        net.addNode(n0);
        Node n1 = fac.createNode(Id.createNodeId("n1"), new Coord(200, 150));
        net.addNode(n1);
        Node n2 = fac.createNode(Id.createNodeId("n2"), new Coord(280, 0));
        net.addNode(n2);

        // create links
        Link l01 = fac.createLink(Id.createLinkId("l_01"), n0, n1);
        l01.setLength(250.); // length in m
        l01.setCapacity(6000.); // capacity in veh/h
        l01.setFreespeed(WALK_SPEED);
        l01.setAllowedModes(ALLOWED_MODES);
        net.addLink(l01);

        Link l12 = fac.createLink(Id.createLinkId("l_12"), n0, n1);
        l12.setLength(170.); // length in m
        l12.setCapacity(6000.); // capacity in veh/h
        l12.setFreespeed(WALK_SPEED);
        l12.setAllowedModes(ALLOWED_MODES);
        net.addLink(l12);

        Link l02 = fac.createLink(Id.createLinkId("l_02"), n0, n1);
        l02.setLength(280.); // length in m
        l02.setCapacity(60.); // capacity in veh/h
        l02.setFreespeed(WALK_SPEED);
        l02.setAllowedModes(ALLOWED_MODES);
        net.addLink(l02);

        return net;

    }

    public static void writeNetwork(Network net, Path outputPath) {
        log.info("Writing network to " + outputPath.resolve(NETWORK_FILE_NAME));
        new NetworkWriter(net).write(outputPath.resolve(NETWORK_FILE_NAME).toString());

        log.info("");
        log.info("Finished \uD83C\uDF89");
    }


    private static class Input {

        @Parameter(names = "-outputDir")
        private String outputDir;

    }
}
