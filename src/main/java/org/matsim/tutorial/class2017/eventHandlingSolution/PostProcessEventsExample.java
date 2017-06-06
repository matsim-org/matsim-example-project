/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.tutorial.class2017.eventHandlingSolution;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;

public class PostProcessEventsExample {

	public static void main(String[] args) {

		// create and parse a MATSim network from file
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile("C:/Users/Joschka/class2017/sampledata/output/run01/output_network.xml.gz");
		
		// Creating an Events Manager
		EventsManager events = EventsUtils.createEventsManager();
		
		// creates a new instance of our event handler
		MyCarDistanceEvaluator myCarDistanceEvaluator = new MyCarDistanceEvaluator(network);
		
		ActivityChainEventHandler activityChainEventHandler = new ActivityChainEventHandler();
		events.addHandler(activityChainEventHandler);
		
		events.addHandler(myCarDistanceEvaluator);
		
		//starts to stream through the events file, please set the path accordingly
		new MatsimEventsReader(events).readFile("C:/Users/Joschka/class2017/sampledata/output/run01/output_events.xml.gz");
		
		activityChainEventHandler.writeActivitestoFile("C:/Users/Joschka/class2017/sampledata/output/run01/activitychains.txt");
		
		int[] result = myCarDistanceEvaluator.getDistanceBins();
		writeArrayToFile(result, "C:/Users/Joschka/class2017/sampledata/output/run01/travelbins.txt");
	}
	
	static void writeArrayToFile(int[] data, String filename){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			bw.write("bin\tdata");
			for (int i = 0; i<data.length;i++){
				bw.newLine();
				bw.write(i+"\t"+data[i]);
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
					e.printStackTrace();
		}
	}
	
	

}
