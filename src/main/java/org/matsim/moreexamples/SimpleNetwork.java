package org.matsim.moreexamples;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.time.LocalDateTime;
import java.util.Collection;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.RouteSearch;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.Connection;
import edu.kit.ifv.mobitopp.publictransport.model.Connections;
import edu.kit.ifv.mobitopp.publictransport.model.DefaultModifiableJourney;
import edu.kit.ifv.mobitopp.publictransport.model.DefaultStation;
import edu.kit.ifv.mobitopp.publictransport.model.ModifiableJourney;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.RoutePoints;
import edu.kit.ifv.mobitopp.publictransport.model.Station;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import edu.kit.ifv.mobitopp.publictransport.model.TransportSystem;

public class SimpleNetwork {

	private static final Double locationOfAmsterdam = new Point2D.Double(4.890444, 52.370197);
	private static final Double locationOfBerlin = new Point2D.Double(13.408333, 52.518611);
	private static final Double locationOfChemnitz = new Point2D.Double(12.9252977, 50.8322608);
	private static final Double locationOfDortmund = new Point2D.Double(7.4652789, 51.5142273);
	private static final RelativeTime noChangeTime = RelativeTime.ZERO;
	static final Time day = new Time(LocalDateTime.of(2017, 3, 14, 0, 0));
	static final Time noon = day.add(12, HOURS);
	static final Time oneOClock = noon.add(1, HOURS);
	static final Time twoOClock = noon.add(2, HOURS);
	static final Time threeOClock = noon.add(3, HOURS);
	static final Time fourOClock = noon.add(4, HOURS);
	static final TransportSystem ice = new TransportSystem("ICE");

	private final Station amsterdamStation;
	private final Stop amsterdam;
	private final Station berlinStation;
	private final Stop berlin;
	private final Station chemnitzStation;
	private final Stop chemnitz;
	private final Station dortmundStation;
	private final Stop dortmund;
	private final ModifiableJourney ice1;
	private final ModifiableJourney ice2;
	private final ModifiableJourney ice3;

	public SimpleNetwork() {
		super();
		amsterdamStation = new DefaultStation(0, emptyList());
		amsterdam = new Stop(0, "Amsterdam", locationOfAmsterdam, noChangeTime, amsterdamStation, 0);
		berlinStation = new DefaultStation(1, emptyList());
		berlin = new Stop(1, "Berlin", locationOfBerlin, noChangeTime, berlinStation, 1);
		chemnitzStation = new DefaultStation(2, emptyList());
		chemnitz = new Stop(2, "Chemnitz", locationOfChemnitz, noChangeTime, chemnitzStation, 2);
		dortmundStation = new DefaultStation(3, emptyList());
		dortmund = new Stop(3, "Dortmund", locationOfDortmund, noChangeTime, dortmundStation, 3);
		ice1 = new DefaultModifiableJourney(1, day, ice, 460);
		ice2 = new DefaultModifiableJourney(2, day, ice, 460);
		ice3 = new DefaultModifiableJourney(2, day, ice, 460);
	}

	public Stop amsterdam() {
		return amsterdam;
	}

	public Stop berlin() {
		return berlin;
	}

	public Stop chemnitz() {
		return chemnitz;
	}

	public Stop dortmund() {
		return dortmund;
	}

	public RouteSearch connectionScan() {
		return new ConnectionScan(timetable());
	}

	private TransitNetwork timetable() {
		return TransitNetwork.createOf(stops(), connections());
	}

	private Collection<Stop> stops() {
		return asList(amsterdam, berlin, chemnitz, dortmund);
	}

	private Connections connections() {
		Connections connections = new Connections();
		connections.add(amsterdamToChemnitz());
		connections.add(amsterdamToDortmund());
		connections.add(chemnitzToBerlin());
		connections.add(dortmundToBerlin());
		return connections;
	}

	public Connection amsterdamToChemnitz() {
		RoutePoints route = RoutePoints.from(amsterdam, chemnitz);
		return Connection.from(0, amsterdam, chemnitz, noon, twoOClock, ice1, route);
	}

	public Connection amsterdamToDortmund() {
		RoutePoints route = RoutePoints.from(amsterdam, dortmund);
		return Connection.from(1, amsterdam, dortmund, noon, oneOClock, ice2, route);
	}

	public Connection chemnitzToBerlin() {
		RoutePoints route = RoutePoints.from(amsterdam, chemnitz);
		return Connection.from(2, chemnitz, berlin, twoOClock, fourOClock, ice3, route);
	}

	public Connection dortmundToBerlin() {
		RoutePoints route = RoutePoints.from(dortmund, berlin);
		return Connection.from(3, dortmund, berlin, oneOClock, threeOClock, ice2, route);
	}

	public Time noon() {
		return noon;
	}

}
