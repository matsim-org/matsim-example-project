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

import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.net.URL;

/**
 * @author nagel
 *
 */
public class RunMatsimTest {
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	// @Ignore("OTFVis does not work on build server") PLEASE DO NOT DO THIS.  Rather comment out OTFVis line in RunMatsim#main.  kai, oct'22
	public final void test() {

		try {
			final URL baseUrl = ExamplesUtils.getTestScenarioURL( "equil" );
			final String fullUrl = IOUtils.extendUrl( baseUrl, "config.xml" ).toString();
			String [] args = {fullUrl,
				  "--config:controler.outputDirectory", utils.getOutputDirectory(),
				  "--config:controler.lastIteration", "1"
			} ;
			RunMatsim.main( args ) ;
			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

				for ( Id<Person> personId : expected.getPersons().keySet()) {
					double scoreReference = expected.getPersons().get(personId).getSelectedPlan().getScore();
					double scoreCurrent = actual.getPersons().get(personId).getSelectedPlan().getScore();
					Assert.assertEquals("Scores of person=" + personId + " are different", scoreReference, scoreCurrent, MatsimTestUtils.EPSILON);
				}


//				boolean result = PopulationUtils.comparePopulations( expected, actual );
//				Assert.assertTrue( result );
				// (There are small differences in the score.  Seems that there were some floating point changes in Java 17, and the
				// differ by JDK (e.g. oracle vs. ...).   So not testing this any more for the time being.  kai, jul'23
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
				Assert.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
			}

		} catch ( Exception ee ) {
			LogManager.getLogger(this.getClass() ).fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}


	}
}
