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
package org.matsim.project;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RunMatsimTest{
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public final void test() {
		try {
			String [] args = {"scenarios/equil/config.xml",
				  "--config:controler.outputDirectory", utils.getOutputDirectory(),
				  "--config:controler.lastIteration=1",
				  "--config:controler.writeEventsInterval=1"
			} ;
			RunMatsim.main( args ) ;
		} catch ( Exception ee ) {
			Logger.getLogger(this.getClass()).fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}


	}

}
