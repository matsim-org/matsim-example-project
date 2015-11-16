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
package org.matsim.example;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author nagel
 *
 */
public class HelloWorldTest {

	/**
	 * Test method for {@link org.matsim.example.HelloWorld#main(java.lang.String[])}.
	 */
	@Test
	public final void testMain() {
//		Config config = ConfigUtils.createConfig() ;
//		config.controler().setLastIteration(1);
//		
//		Scenario scenario = ScenarioUtils.loadScenario(config) ;
//		
//		Controler controler = new Controler( scenario ) ;
//		
//		controler.run();
		
		boolean condition1 = true ;
		boolean condition2 = true ;

		Assert.assertTrue(condition1);
		
		Assert.assertTrue( condition2 );
		
	}

}
