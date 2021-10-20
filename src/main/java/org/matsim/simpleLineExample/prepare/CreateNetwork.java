package org.matsim.simpleLineExample.prepare;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.network.NetworkUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class CreateNetwork {
    private static final Logger log = Logger.getLogger(CreateNetwork.class);

    private static final double WALK_SPEED = 1.5; // walk speed in m/s
    private static final Set<String> ALLOWED_MODES = Set.of(TransportMode.car);
    private static final String NETWORK_FILE_NAME = "network.xml";


    public static void main(String[] args) {
        // this method creates a simple triangular network
        // take input argument

        CreateNetwork.Input input = new CreateNetwork.Input();
        JCommander.newBuilder().addObject(input).build().parse(args);
        log.info("Output directory: " + input.outputDir);

        CreateNetwork.writeNetwork(CreateNetwork.createSimpleStationNetwork(), Paths.get(input.outputDir));

    }


    public static Network createSimpleStationNetwork(){

        // create an empty network
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        // create nodes
        createNodes(net, fac);

        // create links
        createLinks(net, fac);

        return net;

    }

    public static void writeNetwork(Network net, Path outputPath) {
        log.info("Writing network to " + outputPath.resolve(NETWORK_FILE_NAME));
        new NetworkWriter(net).write(outputPath.resolve(NETWORK_FILE_NAME).toString());

        log.info("");
        log.info("Finished \uD83C\uDF89");
    }

    private static void createNode(Network net, NetworkFactory fac, String nodeId, int x, int y){
        Node nd = fac.createNode(Id.createNodeId(nodeId), new Coord(x, y));
        net.addNode(nd);
    }

    private static void createLink(Network net, NetworkFactory fac, String linkId, String n1, String n2, double length, double cap) {
        Node node1 = net.getNodes().get(Id.createNodeId(n1));
        Node node2 = net.getNodes().get(Id.createNodeId(n2));

        Link lnk = fac.createLink(Id.createLinkId(linkId), node1, node2);
        lnk.setLength(length); // length in m
        lnk.setCapacity(cap); // capacity in veh/h
        lnk.setFreespeed(WALK_SPEED);
        lnk.setAllowedModes(ALLOWED_MODES);
        net.addLink(lnk);
    }

    private static void createLink(Network net, NetworkFactory fac, String n1, String n2, double length, double cap) {
        String linkId = "l_" + n1 + "_" + n2;
        createLink(net, fac, linkId, n1, n2, length, cap);
    }

    public static void createNodes(Network net, NetworkFactory fac) {
        // platform middle
        createNode(net, fac, "pf_1", 100,210);
        createNode(net, fac, "pf_2", 100,160);
        createNode(net, fac, "pf_3", 100,140);
        createNode(net, fac, "pf_4", 100,120);
        createNode(net, fac, "pf_5", 100,100);
        createNode(net, fac, "pf_6", 100,50);

        // track 1
        createNode(net, fac, "tr_1_1", 110,160);
        createNode(net, fac, "tr_1_2", 110,140);
        createNode(net, fac, "tr_1_3", 110,120);
        createNode(net, fac, "tr_1_4", 110,100);

        // track 2
        createNode(net, fac, "tr_2_1", 90,100);
        createNode(net, fac, "tr_2_2", 90,120);
        createNode(net, fac, "tr_2_3", 90,140);
        createNode(net, fac, "tr_2_4", 90,160);

        // exit 1
        createNode(net, fac, "ex_1_1", 100,260);
        createNode(net, fac, "ex_1_2", 150,260);
        createNode(net, fac, "ex_1_3", 200,260);
        createNode(net, fac, "ex_1_4", 50,260);
        createNode(net, fac, "ex_1_5", 0,260);

        // exit 2
        createNode(net, fac, "ex_2_1", 100,0);
        createNode(net, fac, "ex_2_2", 150,0);
        createNode(net, fac, "ex_2_3", 200,0);
        createNode(net, fac, "ex_2_4", 50,0);
        createNode(net, fac, "ex_2_5", 0,0);

    }

    private static void createLinks(Network net, NetworkFactory fac) {
        // platform links
        createLink(net, fac, "pf_1", "pf_2", 50., 100.);
        createLink(net, fac, "pf_2", "pf_1", 50., 100.);
        createLink(net, fac, "pf_2", "pf_3", 20., 100.);
        createLink(net, fac, "pf_3", "pf_2", 20., 100.);
        createLink(net, fac, "pf_3", "pf_4", 20., 100.);
        createLink(net, fac, "pf_4", "pf_3", 20., 100.);
        createLink(net, fac, "pf_4", "pf_5", 20., 100.);
        createLink(net, fac, "pf_5", "pf_4", 20., 100.);
        createLink(net, fac, "pf_5", "pf_6", 50., 100.);
        createLink(net, fac, "pf_6", "pf_5", 50., 100.);

        createLink(net, fac, "pf_2", "tr_1_1", 50., 100.);
        createLink(net, fac, "pf_2", "tr_2_4", 50., 100.);
        createLink(net, fac, "tr_1_1", "pf_2", 50., 100.);
        createLink(net, fac, "tr_2_4", "pf_2", 50., 100.);

        createLink(net, fac, "pf_3", "tr_1_2", 50., 100.);
        createLink(net, fac, "pf_3", "tr_2_3", 50., 100.);
        createLink(net, fac, "tr_1_2", "pf_3", 50., 100.);
        createLink(net, fac, "tr_2_3", "pf_3", 50., 100.);

        createLink(net, fac, "pf_4", "tr_1_3", 50., 100.);
        createLink(net, fac, "pf_4", "tr_2_2", 50., 100.);
        createLink(net, fac, "tr_1_3", "pf_4", 50., 100.);
        createLink(net, fac, "tr_2_2", "pf_4", 50., 100.);

        createLink(net, fac, "pf_5", "tr_1_4", 50., 100.);
        createLink(net, fac, "pf_5", "tr_2_1", 50., 100.);
        createLink(net, fac, "tr_1_4", "pf_5", 50., 100.);
        createLink(net, fac, "tr_2_1", "pf_5", 50., 100.);

        // exit 1 links
        createLink(net, fac, "pf_1", "ex_1_1", 50., 100.);
        createLink(net, fac, "ex_1_1", "pf_1", 50., 100.);
        createLink(net, fac, "ex_1_1", "ex_1_2", 50., 100.);
        createLink(net, fac, "ex_1_2", "ex_1_1", 50., 100.);
        createLink(net, fac, "ex_1_2", "ex_1_3", 50., 100.);
        createLink(net, fac, "ex_1_3", "ex_1_2", 50., 100.);
        createLink(net, fac, "ex_1_1", "ex_1_4", 50., 100.);
        createLink(net, fac, "ex_1_4", "ex_1_1", 50., 100.);
        createLink(net, fac, "ex_1_4", "ex_1_5", 50., 100.);
        createLink(net, fac, "ex_1_5", "ex_1_4", 50., 100.);

        // exit 2 links
        createLink(net, fac, "pf_6", "ex_2_1", 50., 100.);
        createLink(net, fac, "ex_2_1", "pf_6", 50., 100.);
        createLink(net, fac, "ex_2_1", "ex_2_2", 50., 100.);
        createLink(net, fac, "ex_2_2", "ex_2_1", 50., 100.);
        createLink(net, fac, "ex_2_2", "ex_2_3", 50., 100.);
        createLink(net, fac, "ex_2_3", "ex_2_2", 50., 100.);
        createLink(net, fac, "ex_2_1", "ex_2_4", 50., 100.);
        createLink(net, fac, "ex_2_4", "ex_2_1", 50., 100.);
        createLink(net, fac, "ex_2_4", "ex_2_5", 50., 100.);
        createLink(net, fac, "ex_2_5", "ex_2_4", 50., 100.);
    }

    private static class Input {

        @Parameter(names = "-outputDir")
        private String outputDir;

    }
}
