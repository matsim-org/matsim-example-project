package org.matsim.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Table;

/**
 * Custom dashboard for Cologne traffic volume visualization.
 * Displays link-level traffic volumes on a map with speed-based coloring
 * and a detailed statistics table.
 */
public class CologneTrafficDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Cologne Traffic Volumes";
		header.description = "Link-level traffic volumes and average speeds for the Cologne road network.";

		// Row 1: Traffic volume map
		layout.row("map")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Daily Traffic Volumes - Cologne";
				viz.height = 12.0;

				viz.display.fill.dataset = data.compute(
					org.matsim.application.analysis.traffic.CreateAvroNetwork.class,
					"network.avro"
				);

				String trafficCsv = data.compute(
					org.matsim.application.analysis.traffic.TrafficAnalysis.class,
					"traffic_stats_by_link_daily.csv"
				);

				// Line width proportional to traffic volume
				viz.display.lineWidth.dataset = trafficCsv;
				viz.display.lineWidth.columnName = "simulated_traffic_volume";
				viz.display.lineWidth.scaleFactor = 20000d;
				viz.display.lineWidth.join = "link_id";

				// Color by average speed (red=slow, blue=fast)
				viz.display.color.dataset = trafficCsv;
				viz.display.color.columnName = "avg_speed";
				viz.display.color.join = "link_id";
				viz.display.color.setColorRamp(ColorScheme.RdYlBu, 5, true);
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
