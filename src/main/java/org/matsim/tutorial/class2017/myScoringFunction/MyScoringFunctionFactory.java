/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.tutorial.class2017.myScoringFunction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;

import javax.inject.Inject;

/**
 * A scoring function Factory that includes an event scoring and punishes people that start to work before 10 am.
 *  
 */
public final class MyScoringFunctionFactory implements ScoringFunctionFactory {

	protected Network network;

	private final CharyparNagelScoringParametersForPerson params;

	public MyScoringFunctionFactory( final Scenario sc ) {
		this( new SubpopulationCharyparNagelScoringParameters( sc ) , sc.getNetwork() );
	}

	@Inject
	MyScoringFunctionFactory(final CharyparNagelScoringParametersForPerson params, Network network) {
		this.params = params;
		this.network = network;
	}

	/**
     *
     * In every iteration, the framework creates a new ScoringFunction for each Person.
     * A ScoringFunction is much like an EventHandler: It reacts to scoring-relevant events
     * by accumulating them. After the iteration, it is asked for a score value.
     *
     * Since the factory method gets the Person, it can create a ScoringFunction
     * which depends on Person attributes. This implementation does not.
     *
	 * <li>The fact that you have a person-specific scoring function does not mean that the "creative" modules
	 * (such as route choice) are person-specific.  This is not a bug but a deliberate design concept in order 
	 * to reduce the consistency burden.  Instead, the creative modules should generate a diversity of possible
	 * solutions.  In order to do a better job, they may (or may not) use person-specific info.  kai, apr'11
	 * </ul>
	 * 
	 * @param person
	 * @return new ScoringFunction
	 */
	@Override
	public ScoringFunction createNewScoringFunction(Person person) {

		final CharyparNagelScoringParameters parameters = params.getScoringParameters( person );

		SumScoringFunction sumScoringFunction = new SumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring( parameters ));
		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring( parameters , this.network));
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring( parameters ));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring( parameters ));
		sumScoringFunction.addScoringFunction(new SumScoringFunction.ArbitraryEventScoring() {
			
			double score = 0;
			
			@Override
			public double getScore() {
				// TODO Auto-generated method stub
				return score;
			}
			
			@Override
			public void finish() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void handleEvent(Event event) {
				if (event instanceof ActivityStartEvent){
					ActivityStartEvent start = (ActivityStartEvent) event;
					if (event.getTime()<10*3600){
						score = score - 100;
					}
				}
			}
		});
		return sumScoringFunction;
	}
}
