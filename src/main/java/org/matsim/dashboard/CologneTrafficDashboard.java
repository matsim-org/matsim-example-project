package org.matsim.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.Table;

/**
 * Custom dashboard for Cologne traffic volume visualization.
 * Displays link-level traffic volumes using the standard Links visualization
 * and a detailed statistics table.
 */
public class CologneTrafficDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Cologne Traffic Volumes";
		header.description = "Link-level traffic volumes and average speeds for the Cologne road network.";

		// Row 1: Traffic volume link plot
		layout.row("volumes")
			.el(Links.class, (viz, data) -> {
				viz.title = "Daily Traffic Volumes - Cologne";
				viz.height = 12.0;
				viz.datasets.csvFile = data.compute(
					org.matsim.application.analysis.traffic.TrafficAnalysis.class,
					"traffic_stats_by_link_daily.csv"
				);
				viz.network = data.output("output_network.xml.gz");
				viz.display.width.columnName = "simulated_traffic_volume";
				viz.display.width.scaleFactor = 20000d;
			});

		// Row 2: Link-level statistics table
		layout.row("stats")
			.el(Table.class, (viz, data) -> {
				viz.title = "Link-Level Traffic Statistics";
				viz.dataset = data.compute(
					org.matsim.application.analysis.traffic.TrafficAnalysis.class,
					"traffic_stats_by_link_daily.csv"
				);
				viz.height = 8.0;
			});
	}

	@Override
	public double priority() {
		return 0.5;
	}
}
