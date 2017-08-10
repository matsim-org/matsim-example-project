package org.matsim.tutorial.class2017.routingModule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TeleportationRoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;

public class MyCarRoutingModule implements RoutingModule {

	
	
	final Network filteredNetwork;
	final PopulationFactory populationFactory;
	
	RoutingModule walkRouter;
	
	LeastCostPathCalculator lcp;
	
	Random r = new Random(42);
	
	final int maxparkdistance = 600;
	final NetworkRoutingModule nrm;
	
	@Inject
	public MyCarRoutingModule(Map<String, TravelTime> travelTimes, Map<String, TravelDisutilityFactory> travelDisutilityFactories, PopulationFactory populationFactory,  Network network, LeastCostPathCalculatorFactory df)
	{
		
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		Set<String> modes = new HashSet<>();
		modes.add("car");
		filteredNetwork = NetworkUtils.createNetwork();
		filter.filter(filteredNetwork, modes);
		this.populationFactory = populationFactory;
		TravelTime carTravelTime = travelTimes.get("car");
		TravelDisutility carDis = travelDisutilityFactories.get("car").createTravelDisutility(carTravelTime);
		lcp = df.createPathCalculator(filteredNetwork, carDis, carTravelTime);
		walkRouter = new TeleportationRoutingModule("car", populationFactory, 1.25 , 1.3);
		nrm = new NetworkRoutingModule("car", populationFactory, filteredNetwork, lcp);
	}
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime,
			Person person) {
		
		List <PlanElement> routeList = new ArrayList<>();
		Id<Link> fromId = fromFacility.getLinkId();
		Coord fromCoord = fromFacility.getCoord();
		Coord fromParkCoord = new Coord(fromCoord.getX()+r.nextInt(maxparkdistance)-maxparkdistance/2,fromCoord.getY()+r.nextInt(maxparkdistance)-maxparkdistance/2);
		Activity parkInteraction1 = populationFactory.createActivityFromCoord("car interaction", fromParkCoord);
		parkInteraction1.setLinkId(NetworkUtils.getNearestLinkExactly(filteredNetwork, fromParkCoord).getId());
		
		Id<Link> toId = toFacility.getLinkId();
		Coord toCoord = toFacility.getCoord();
		Coord toParkCoord = new Coord(toCoord.getX()+r.nextInt(maxparkdistance)-maxparkdistance/2,toCoord.getY()+r.nextInt(maxparkdistance)-maxparkdistance/2);
		Activity parkInteraction2 = populationFactory.createActivityFromCoord("car interaction", toParkCoord);
		parkInteraction2.setLinkId(NetworkUtils.getNearestLinkExactly(filteredNetwork, toParkCoord).getId());
		
		Leg walkLeg = (Leg) walkRouter.calcRoute(fromFacility,new ActivityWrapperFacility(parkInteraction1) , departureTime, person).get(0);
		routeList.add(walkLeg);
		parkInteraction1.setEndTime(walkLeg.getDepartureTime()+walkLeg.getTravelTime()+60);
		routeList.add(parkInteraction1);
		System.out.println(parkInteraction1.getLinkId()+"  "+parkInteraction2.getLinkId());
		Leg carLeg = (Leg) nrm.calcRoute(new ActivityWrapperFacility(parkInteraction1), new ActivityWrapperFacility(parkInteraction2), parkInteraction1.getEndTime(), person).get(0);
		
		routeList.add(carLeg);
		parkInteraction2.setEndTime(carLeg.getDepartureTime()+carLeg.getTravelTime()+60);
		routeList.add(parkInteraction2);
		Leg walkLeg2 = (Leg) walkRouter.calcRoute(new ActivityWrapperFacility(parkInteraction2),toFacility , parkInteraction2.getEndTime(), person).get(0);
		routeList.add(walkLeg2);
		return routeList;
	}

	
	
	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl("car interaction");
	}

}
