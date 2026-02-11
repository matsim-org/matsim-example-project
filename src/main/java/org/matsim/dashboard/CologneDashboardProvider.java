package org.matsim.dashboard;

import org.matsim.core.config.Config;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.TripDashboard;
import org.matsim.simwrapper.dashboard.ODTripDashboard;
import org.matsim.simwrapper.dashboard.ActivityDashboard;
import org.matsim.simwrapper.dashboard.PopulationAttributeDashboard;
import org.matsim.run.RunCologneSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides Cologne-specific SimWrapper dashboards on top of the defaults.
 *
 * <p>Discovered via Java SPI mechanism. Registered in
 * {@code META-INF/services/org.matsim.simwrapper.DashboardProvider}.</p>
 *
 * <p>The default dashboards (Overview, Trip, Traffic, StuckAgent) are provided
 * by {@code DefaultDashboardProvider} at priority -1. This provider adds
 * Cologne-specific dashboards at priority 0.</p>
 */
public class CologneDashboardProvider implements DashboardProvider {

	@Override
	public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
		List<Dashboard> dashboards = new ArrayList<>();

		// Cologne trip analysis with mode distribution
		dashboards.add(Dashboard.customize(new TripDashboard())
			.context("cologne")
			.title("Cologne Trips")
			.description("Trip analysis for the Cologne metropolitan area")
		);

		// Origin-destination trip patterns with hex aggregation
		dashboards.add(Dashboard.customize(
			new ODTripDashboard(
				Set.of("car", "pt", "bike", "walk"),
				RunCologneSimulation.CRS
			))
			.context("cologne")
			.title("Cologne OD Patterns")
			.description("Origin-destination trip patterns aggregated into hexagonal bins")
		);

		// Custom Cologne traffic volume dashboard
		dashboards.add(Dashboard.customize(new CologneTrafficDashboard())
			.context("cologne")
			.title("Cologne Traffic Volumes")
			.description("Link-level traffic volumes, speeds, and congestion for the Cologne network")
		);

		// Spatial and temporal activity patterns
		dashboards.add(Dashboard.customize(new ActivityDashboard())
			.context("cologne")
			.title("Cologne Activities")
			.description("Spatial and temporal distribution of activities in Cologne")
		);

		// Population demographics
		dashboards.add(Dashboard.customize(new PopulationAttributeDashboard())
			.context("cologne")
			.title("Cologne Population")
			.description("Population demographics and attribute distributions")
		);

		return dashboards;
	}

	@Override
	public double priority() {
		return 0;
	}

}
