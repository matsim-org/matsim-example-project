package org.matsim.analysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import scala.Int;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class OriginDestinationAnalysis {

    private static final String fromBezirk = "001";
    private static final String toBezirk = "002";
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:3857");

    public static void main(String[] args) {

        var features = ShapeFileReader.getAllFeatures("C:\\Users\\Janekdererste\\Downloads\\Bezirke_-_Berlin-shp\\Berlin_Bezirke.shp");

        var fromGeometry = getGeometry(fromBezirk, features);
        var toGeometry = getGeometry(toBezirk, features);

        var population = PopulationUtils.readPopulation("C:\\Users\\Janekdererste\\Downloads\\matsim class\\matsim class\\5.5.x-1pct\\berlin-v5.5-1pct.output_plans.xml.gz");
        var network = NetworkUtils.readNetwork("C:\\Users\\Janekdererste\\Downloads\\matsim class\\matsim class\\5.5.x-1pct\\berlin-v5.5-1pct.output_network.xml.gz");

        int counter = 0;

        for (var person : population.getPersons().values()) {

            var plan = person.getSelectedPlan();
            var activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

            for (var i = 0 ; i < activities.size() - 1; i++) {

                var fromCoord = getCoord(activities.get(i), network);
                var toCoord = getCoord(activities.get(i + 1), network);

                if (isInGeometry(fromCoord, fromGeometry) && isInGeometry(toCoord, toGeometry)) {
                    counter++;
                }
            }
        }

        System.out.println(counter + " trips from " + fromBezirk + " to " + toBezirk);
    }

    private static Coord getCoord(Activity activity, Network network) {

        if (activity.getCoord() != null) {
            return activity.getCoord();
        }

        return network.getLinks().get(activity.getLinkId()).getCoord();
    }

    private static boolean isInGeometry(Coord coord, Geometry geometry) {

        var transformed = transformation.transform(coord);
        return geometry.covers(MGC.coord2Point(transformed));
    }

    private static Geometry getGeometry(String identifier, Collection<SimpleFeature> features) {
        return features.stream()
                .filter(feature -> feature.getAttribute("Gemeinde_s").equals("001"))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .findAny()
                .orElseThrow();
    }
}
