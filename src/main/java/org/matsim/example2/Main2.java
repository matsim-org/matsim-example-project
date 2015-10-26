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
package org.matsim.example2;

import javax.inject.Provider;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

/**
 * @author nagel
 *
 */
public class Main2 {

	public static void main(String[] args) {

		final Config config = ConfigUtils.createConfig() ;
		config.controler().setLastIteration(2);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		final Scenario scenario = ScenarioUtils.createScenario(config) ;

		// ---

		Network network = scenario.getNetwork() ;
		NetworkFactory nf = network.getFactory() ;
		
		final int LL=10 , MM=10 ;
		final double offset = 100. ;
		
		for ( int ii=0 ; ii<LL; ii++ ) {
			for ( int jj=0 ; jj<MM; jj++ ) {
				Coord coord = new Coord(offset*ii,offset*jj) ;
				Node node = nf.createNode(Id.createNodeId(ii+"x"+jj), coord) ;
				network.addNode( node );
			}
		}
		for ( int ii=1; ii<LL; ii++ ) {
			for ( int jj=1 ; jj<MM; jj++ ) {
				Node node0 = network.getNodes().get( Id.createNodeId((ii-1)+"x"+(jj-1)) ) ;
				{
					Node node1 = network.getNodes().get( Id.createNodeId((ii)+"x"+(jj-1)) ) ;
					Link link = nf.createLink(Id.createLinkId(node0.getId().toString() + "--" + node1.getId().toString() ), node0, node1 ) ;
					link.setCapacity(25. );
					link.setFreespeed(10.);
					network.addLink( link );
				}
			}
		}

		// ---

		Population pops = scenario.getPopulation() ;

		Vehicles vehs = scenario.getVehicles() ;

		final Controler controler = new Controler(scenario) ;
		
		controler.setDirtyShutdown(true);

		controler.addOverridingModule(new AbstractModule(){
			@Override public void install() {
				this.bindMobsim().toProvider( new Provider<Mobsim>(){
					@Override public Mobsim get() {
						QSim qSim = QSimUtils.createDefaultQSim(scenario, controler.getEvents() ) ;
						OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config, scenario, controler.getEvents(), qSim);
						OTFClientLive.run(config, server);
						return qSim ;
					}
				} ) ; 
			}
		});

		controler.run();
	}

}
