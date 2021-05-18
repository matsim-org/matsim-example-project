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
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

public class HomeActivityAnalysis {

    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:31468", "EPSG:3857");
    private static final String bezirkShapes = "C:\\Users\\Janekdererste\\Downloads\\Bezirke_-_Berlin-shp\\Berlin_Bezirke.shp";
    private static final String populationFile = "C:\\Users\\Janekdererste\\Downloads\\matsim class\\matsim class\\5.5.x-1pct\\berlin-v5.5-1pct.output_plans.xml.gz";
    private static final String networkFile = "C:\\Users\\Janekdererste\\Downloads\\matsim class\\matsim class\\5.5.x-1pct\\berlin-v5.5-1pct.output_network.xml.gz";
    private static final String gemeinde_s = "001"; // this is berlin mitte

    public static void main(String[] args) {

        var features = ShapeFileReader.getAllFeatures(bezirkShapes);
        var population = PopulationUtils.readPopulation(populationFile);
        var network = NetworkUtils.readNetwork(networkFile);

        var geometry = features.stream()
                .filter(feature -> feature.getAttribute("Gemeinde_s").equals(gemeinde_s))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .findAny()
                .orElseThrow();

        int counter = 0;

        for (var person : population.getPersons().values()) {

            var plan = person.getSelectedPlan();
            var activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

            for (var activity : activities) {

                var coord = getCoord(activity, network);
                if (isInGeometry(coord, geometry)) {
                    counter++;
                }
            }
        }

        System.out.println(counter + " activities in Mitte");
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
