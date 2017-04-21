/**
 * 
 */
package org.matsim.moreexamples;


import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.DefaultStopPaths;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.RouteSearch;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.StopPaths;
import edu.kit.ifv.mobitopp.publictransport.model.Connection;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.StopPath;
import edu.kit.ifv.mobitopp.publictransport.model.Time;

/**
 * @author nagel
 *
 */
public class KNTestCSA {

	public static void main( String[] args ) {
		SimpleNetwork network = new SimpleNetwork() ;
		
		Stop chemnitz = network.chemnitz();
		RelativeTime timeToChemnitz = RelativeTime.of(30, MINUTES);
		StopPath viaChemnitz = new StopPath(chemnitz, timeToChemnitz);
		
		Stop dortmund = network.dortmund();
		RelativeTime timeToDortmund = RelativeTime.of(1, HOURS);
		StopPath viaDortmund = new StopPath(dortmund, timeToDortmund);

		Stop berlin = network.berlin();
		RelativeTime walkTimeInBerlin = RelativeTime.of(5, MINUTES);
		StopPath toPlaceInBerlin = new StopPath(berlin, walkTimeInBerlin);

		Time atOneOClock = SimpleNetwork.oneOClock;
		Connection chemnitzToBerlin = network.chemnitzToBerlin();
		Time includingEgresspath = SimpleNetwork.fourOClock.add(walkTimeInBerlin);
		
		StopPaths starts = DefaultStopPaths.from(asList(viaChemnitz, viaDortmund));
		StopPaths ends = DefaultStopPaths.from(asList(toPlaceInBerlin));
		
		RouteSearch connectionScan = network.connectionScan();
		
		Optional<PublicTransportRoute> potentialRoute = connectionScan.findRoute(starts, ends, atOneOClock);
		
//		assertThat(potentialRoute, isPresent());
		PublicTransportRoute route = potentialRoute.get();
//		assertThat(route.arrival(), is(equalTo(includingEgresspath)));
//		assertThat(route.start(), is(equalTo(chemnitz)));
//		assertThat(route.end(), is(equalTo(berlin)));
//		assertThat(route.connections(), contains(chemnitzToBerlin));
		
		System.out.println( route.toString() ) ;
	}
	
}
