package org.matsim.project.cologne;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.util.*;

/**
 * Generates a synthetic road network for Cologne (Köln), Germany.
 *
 * The network models Cologne's characteristic structure:
 * - Rhine river (north-south) dividing left bank (main city) and right bank (Deutz side)
 * - Concentric ring roads (Inner Ring, Middle Ring, Outer Ring, Autobahn ring)
 * - Radial arterials connecting rings
 * - Bridge crossings over the Rhine
 *
 * Coordinate system: EPSG:25832 (UTM zone 32N)
 * Cologne city center approximate UTM coords: (356000, 5645000)
 */
public class CologneNetworkGenerator {

    // Cologne city center in UTM 32N
    private static final double CENTER_X = 356000.0;
    private static final double CENTER_Y = 5645000.0;

    // Rhine river runs roughly at x = CENTER_X + 500 (slightly east of center)
    private static final double RHINE_X = CENTER_X + 500.0;

    // Ring radii (meters from center)
    private static final double INNER_RING_RADIUS = 1200.0;
    private static final double MIDDLE_RING_RADIUS = 3000.0;
    private static final double OUTER_RING_RADIUS = 5500.0;
    private static final double AUTOBAHN_RING_RADIUS = 9000.0;

    // Number of nodes per ring (left bank only for inner rings)
    private static final int RADIALS_LEFT = 8;   // radial directions on left bank
    private static final int RADIALS_RIGHT = 4;  // radial directions on right bank

    // Road properties
    private static final double AUTOBAHN_FREESPEED = 33.33;  // 120 km/h
    private static final double ARTERIAL_FREESPEED = 16.67;  // 60 km/h
    private static final double COLLECTOR_FREESPEED = 11.11;  // 40 km/h
    private static final double LOCAL_FREESPEED = 8.33;       // 30 km/h

    private static final double AUTOBAHN_CAPACITY = 4000.0;   // veh/h per lane
    private static final double ARTERIAL_CAPACITY = 1800.0;
    private static final double COLLECTOR_CAPACITY = 1200.0;
    private static final double LOCAL_CAPACITY = 600.0;

    private int nodeCounter = 0;
    private int linkCounter = 0;

    public Network generateNetwork() {
        Network network = NetworkUtils.createNetwork();
        NetworkFactory factory = network.getFactory();

        // Generate nodes for left bank (west of Rhine)
        Map<String, Node> leftBankNodes = new LinkedHashMap<>();
        // Generate nodes for right bank (east of Rhine)
        Map<String, Node> rightBankNodes = new LinkedHashMap<>();

        // --- LEFT BANK NODES ---
        // Center node
        Node center = createNode(factory, "center", CENTER_X, CENTER_Y);
        network.addNode(center);
        leftBankNodes.put("center", center);

        // Angles for left bank radials (west semicircle, avoiding Rhine)
        // 8 radials from ~100° to ~260° (measured from east, counterclockwise)
        double[] leftAngles = {110, 150, 180, 210, 250, 290, 330, 70};

        // Inner ring nodes (left bank)
        List<Node> innerRingLeft = createRingNodes(network, factory, "inner", INNER_RING_RADIUS, leftAngles, leftBankNodes);
        // Middle ring nodes
        List<Node> middleRingLeft = createRingNodes(network, factory, "middle", MIDDLE_RING_RADIUS, leftAngles, leftBankNodes);
        // Outer ring nodes
        List<Node> outerRingLeft = createRingNodes(network, factory, "outer", OUTER_RING_RADIUS, leftAngles, leftBankNodes);

        // --- RIGHT BANK NODES ---
        // Right bank center (Deutz area)
        Node deutzCenter = createNode(factory, "deutz_center", RHINE_X + 1000, CENTER_Y);
        network.addNode(deutzCenter);
        rightBankNodes.put("deutz_center", deutzCenter);

        double[] rightAngles = {0, 30, 330, 60};
        List<Node> innerRingRight = createRingNodes(network, factory, "r_inner", 1500, rightAngles, rightBankNodes, RHINE_X + 1000, CENTER_Y);
        List<Node> middleRingRight = createRingNodes(network, factory, "r_middle", 3500, rightAngles, rightBankNodes, RHINE_X + 1000, CENTER_Y);
        List<Node> outerRingRight = createRingNodes(network, factory, "r_outer", 6000, rightAngles, rightBankNodes, RHINE_X + 1000, CENTER_Y);

        // --- AUTOBAHN RING (surrounds entire city) ---
        double[] autobahnAngles = {0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330};
        Map<String, Node> autobahnNodes = new LinkedHashMap<>();
        List<Node> autobahnRing = createRingNodes(network, factory, "autobahn", AUTOBAHN_RING_RADIUS, autobahnAngles, autobahnNodes);

        // --- BRIDGE NODES (Rhine crossings) ---
        // Cologne has several bridges; we model 4 key ones
        Node bridgeW1 = createNode(factory, "bridge_w1", RHINE_X - 100, CENTER_Y + 1500); // Mülheimer Brücke area
        Node bridgeE1 = createNode(factory, "bridge_e1", RHINE_X + 100, CENTER_Y + 1500);
        Node bridgeW2 = createNode(factory, "bridge_w2", RHINE_X - 100, CENTER_Y + 300);  // Hohenzollern
        Node bridgeE2 = createNode(factory, "bridge_e2", RHINE_X + 100, CENTER_Y + 300);
        Node bridgeW3 = createNode(factory, "bridge_w3", RHINE_X - 100, CENTER_Y - 500);  // Deutzer Brücke
        Node bridgeE3 = createNode(factory, "bridge_e3", RHINE_X + 100, CENTER_Y - 500);
        Node bridgeW4 = createNode(factory, "bridge_w4", RHINE_X - 100, CENTER_Y - 2000); // Rodenkirchener
        Node bridgeE4 = createNode(factory, "bridge_e4", RHINE_X + 100, CENTER_Y - 2000);

        for (Node n : List.of(bridgeW1, bridgeE1, bridgeW2, bridgeE2, bridgeW3, bridgeE3, bridgeW4, bridgeE4)) {
            network.addNode(n);
        }

        // --- CREATE LINKS ---

        // 1. Left bank radials (center -> inner -> middle -> outer)
        createRadialLinks(network, factory, center, innerRingLeft, COLLECTOR_FREESPEED, COLLECTOR_CAPACITY, 2);
        createRadialLinks(network, factory, innerRingLeft, middleRingLeft, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        createRadialLinks(network, factory, middleRingLeft, outerRingLeft, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);

        // 2. Left bank ring connections
        createRingLinks(network, factory, innerRingLeft, COLLECTOR_FREESPEED, COLLECTOR_CAPACITY, 1);
        createRingLinks(network, factory, middleRingLeft, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        createRingLinks(network, factory, outerRingLeft, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);

        // 3. Right bank radials
        createRadialLinks(network, factory, deutzCenter, innerRingRight, COLLECTOR_FREESPEED, COLLECTOR_CAPACITY, 2);
        createRadialLinks(network, factory, innerRingRight, middleRingRight, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        createRadialLinks(network, factory, middleRingRight, outerRingRight, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);

        // 4. Right bank ring connections
        createRingLinks(network, factory, innerRingRight, COLLECTOR_FREESPEED, COLLECTOR_CAPACITY, 1);
        createRingLinks(network, factory, middleRingRight, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        createRingLinks(network, factory, outerRingRight, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);

        // 5. Autobahn ring
        createRingLinks(network, factory, autobahnRing, AUTOBAHN_FREESPEED, AUTOBAHN_CAPACITY, 3);

        // 6. Connections from outer rings to autobahn
        connectToAutobahn(network, factory, outerRingLeft, autobahnRing, autobahnAngles);
        connectToAutobahn(network, factory, outerRingRight, autobahnRing, autobahnAngles);

        // 7. Bridge links (bidirectional)
        createBidirectionalLink(network, factory, bridgeW1, bridgeE1, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        createBidirectionalLink(network, factory, bridgeW2, bridgeE2, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        createBidirectionalLink(network, factory, bridgeW3, bridgeE3, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        createBidirectionalLink(network, factory, bridgeW4, bridgeE4, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);

        // 8. Connect bridges to nearest ring nodes
        connectBridgesToNetwork(network, factory, leftBankNodes, rightBankNodes,
                List.of(bridgeW1, bridgeW2, bridgeW3, bridgeW4),
                List.of(bridgeE1, bridgeE2, bridgeE3, bridgeE4));

        // 9. Add grid infill for residential/local streets on left bank
        addLocalStreetGrid(network, factory, leftBankNodes);

        return network;
    }

    private Node createNode(NetworkFactory factory, String id, double x, double y) {
        return factory.createNode(Id.createNodeId(id), new Coord(x, y));
    }

    private Node createNumberedNode(NetworkFactory factory, String prefix, double x, double y) {
        nodeCounter++;
        return factory.createNode(Id.createNodeId(prefix + "_" + nodeCounter), new Coord(x, y));
    }

    private List<Node> createRingNodes(Network network, NetworkFactory factory,
                                        String prefix, double radius, double[] angles,
                                        Map<String, Node> nodeMap) {
        return createRingNodes(network, factory, prefix, radius, angles, nodeMap, CENTER_X, CENTER_Y);
    }

    private List<Node> createRingNodes(Network network, NetworkFactory factory,
                                        String prefix, double radius, double[] angles,
                                        Map<String, Node> nodeMap,
                                        double centerX, double centerY) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < angles.length; i++) {
            double rad = Math.toRadians(angles[i]);
            double x = centerX + radius * Math.cos(rad);
            double y = centerY + radius * Math.sin(rad);
            String nodeId = prefix + "_" + i;
            Node node = factory.createNode(Id.createNodeId(nodeId), new Coord(x, y));
            network.addNode(node);
            nodeMap.put(nodeId, node);
            nodes.add(node);
        }
        return nodes;
    }

    private void createRadialLinks(Network network, NetworkFactory factory,
                                    Node center, List<Node> ringNodes,
                                    double freespeed, double capacity, int lanes) {
        for (Node ringNode : ringNodes) {
            createBidirectionalLink(network, factory, center, ringNode, freespeed, capacity, lanes);
        }
    }

    private void createRadialLinks(Network network, NetworkFactory factory,
                                    List<Node> innerRing, List<Node> outerRing,
                                    double freespeed, double capacity, int lanes) {
        int size = Math.min(innerRing.size(), outerRing.size());
        for (int i = 0; i < size; i++) {
            createBidirectionalLink(network, factory, innerRing.get(i), outerRing.get(i), freespeed, capacity, lanes);
        }
    }

    private void createRingLinks(Network network, NetworkFactory factory,
                                  List<Node> ringNodes, double freespeed, double capacity, int lanes) {
        for (int i = 0; i < ringNodes.size(); i++) {
            int next = (i + 1) % ringNodes.size();
            createBidirectionalLink(network, factory, ringNodes.get(i), ringNodes.get(next), freespeed, capacity, lanes);
        }
    }

    private void createBidirectionalLink(Network network, NetworkFactory factory,
                                          Node from, Node to, double freespeed, double capacity, int lanes) {
        double length = NetworkUtils.getEuclideanDistance(from.getCoord(), to.getCoord());
        if (length < 10) length = 100; // minimum link length

        linkCounter++;
        Link linkAB = factory.createLink(Id.createLinkId("link_" + linkCounter), from, to);
        linkAB.setLength(length);
        linkAB.setFreespeed(freespeed);
        linkAB.setCapacity(capacity);
        linkAB.setNumberOfLanes(lanes);
        network.addLink(linkAB);

        linkCounter++;
        Link linkBA = factory.createLink(Id.createLinkId("link_" + linkCounter), to, from);
        linkBA.setLength(length);
        linkBA.setFreespeed(freespeed);
        linkBA.setCapacity(capacity);
        linkBA.setNumberOfLanes(lanes);
        network.addLink(linkBA);
    }

    private void connectToAutobahn(Network network, NetworkFactory factory,
                                    List<Node> outerRing, List<Node> autobahnRing,
                                    double[] autobahnAngles) {
        // Connect each outer ring node to the nearest autobahn node
        for (Node outerNode : outerRing) {
            Node nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Node abNode : autobahnRing) {
                double dist = NetworkUtils.getEuclideanDistance(outerNode.getCoord(), abNode.getCoord());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = abNode;
                }
            }
            if (nearest != null && minDist < 8000) {
                createBidirectionalLink(network, factory, outerNode, nearest, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
            }
        }
    }

    private void connectBridgesToNetwork(Network network, NetworkFactory factory,
                                          Map<String, Node> leftNodes, Map<String, Node> rightNodes,
                                          List<Node> westBridgeEnds, List<Node> eastBridgeEnds) {
        // Connect west bridge ends to nearest left bank nodes
        for (Node bridgeNode : westBridgeEnds) {
            connectToNearestNodes(network, factory, bridgeNode, leftNodes.values(), 2, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        }
        // Connect east bridge ends to nearest right bank nodes
        for (Node bridgeNode : eastBridgeEnds) {
            connectToNearestNodes(network, factory, bridgeNode, rightNodes.values(), 2, ARTERIAL_FREESPEED, ARTERIAL_CAPACITY, 2);
        }
    }

    private void connectToNearestNodes(Network network, NetworkFactory factory,
                                        Node node, Collection<Node> candidates, int count,
                                        double freespeed, double capacity, int lanes) {
        List<Map.Entry<Node, Double>> distances = new ArrayList<>();
        for (Node candidate : candidates) {
            double dist = NetworkUtils.getEuclideanDistance(node.getCoord(), candidate.getCoord());
            if (dist > 10) { // avoid self-connections
                distances.add(Map.entry(candidate, dist));
            }
        }
        distances.sort(Comparator.comparingDouble(Map.Entry::getValue));
        for (int i = 0; i < Math.min(count, distances.size()); i++) {
            createBidirectionalLink(network, factory, node, distances.get(i).getKey(), freespeed, capacity, lanes);
        }
    }

    private void addLocalStreetGrid(Network network, NetworkFactory factory, Map<String, Node> existingNodes) {
        // Add a grid of local streets within the middle ring area on the left bank
        // This creates more realistic residential connectivity
        Random rand = new Random(42);
        List<Node> gridNodes = new ArrayList<>();

        // Generate grid nodes in the left bank area
        double gridSpacing = 800; // 800m between grid nodes
        for (double x = CENTER_X - 4000; x <= CENTER_X - 200; x += gridSpacing) {
            for (double y = CENTER_Y - 4000; y <= CENTER_Y + 4000; y += gridSpacing) {
                // Only add nodes within the outer ring and west of Rhine
                double distFromCenter = Math.sqrt(Math.pow(x - CENTER_X, 2) + Math.pow(y - CENTER_Y, 2));
                if (distFromCenter < OUTER_RING_RADIUS && x < RHINE_X - 200) {
                    Node node = createNumberedNode(factory, "grid", x + rand.nextDouble() * 100 - 50, y + rand.nextDouble() * 100 - 50);
                    network.addNode(node);
                    gridNodes.add(node);
                }
            }
        }

        // Connect grid nodes to neighbors
        for (int i = 0; i < gridNodes.size(); i++) {
            for (int j = i + 1; j < gridNodes.size(); j++) {
                double dist = NetworkUtils.getEuclideanDistance(gridNodes.get(i).getCoord(), gridNodes.get(j).getCoord());
                if (dist < gridSpacing * 1.5) {
                    createBidirectionalLink(network, factory, gridNodes.get(i), gridNodes.get(j),
                            LOCAL_FREESPEED, LOCAL_CAPACITY, 1);
                }
            }
        }

        // Connect grid to existing ring nodes
        for (Node gridNode : gridNodes) {
            connectToNearestNodes(network, factory, gridNode, existingNodes.values(), 1,
                    COLLECTOR_FREESPEED, COLLECTOR_CAPACITY, 1);
        }
    }

    public void writeNetwork(Network network, String outputPath) {
        new NetworkWriter(network).write(outputPath);
    }

    public static void main(String[] args) {
        CologneNetworkGenerator generator = new CologneNetworkGenerator();
        Network network = generator.generateNetwork();
        System.out.println("Generated Cologne network: " + network.getNodes().size() + " nodes, " + network.getLinks().size() + " links");
        generator.writeNetwork(network, "scenarios/cologne/input/network.xml");
    }
}
