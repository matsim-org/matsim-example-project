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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.ScoringParameters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author nagel
 *
 */
public class RunMatsim{

	public static void main(String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		config.controler().setLastIteration(0);

		// possibly modify config here

		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// possibly modify scenario here
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;

		
		// ---

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PriceBroker.class).in(Singleton.class);
				bind(PriceListener.class).in(Singleton.class);
				this.addEventHandlerBinding().to(PriceBroker.class);
				this.addEventHandlerBinding().to(PriceListener.class);
			}
		});

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {

			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				SumScoringFunction sumScoringFunction = new SumScoringFunction();

				// Score activities, legs, payments and being stuck
				// with the default MATSim scoring based on utility parameters in the config file.
				final ScoringParameters params =
						new ScoringParameters.Builder(scenario, person).build();
				sumScoringFunction.addScoringFunction(new SumScoringFunction.ActivityScoring() {

					@Inject
					private PriceListener priceListener;

					@Override
					public void handleFirstActivity(Activity act) {

					}

					@Override
					public void handleActivity(Activity act) {

					}

					@Override
					public void handleLastActivity(Activity act) {

					}

					@Override
					public void finish() {

					}

					@Override
					public double getScore() {
						if (priceListener.personRecords.containsKey(person.getId())) {
							var record = priceListener.personRecords.get(person.getId());
							var score = record.consumptionStatus.equals("combustion") ? -10 : 10;
							System.out.println("--------------------------------------------------------- Scoring of: " + score);
							return score;
						}
						return 0;
					}
				});
				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(params, scenario.getNetwork()));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(params));
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				return sumScoringFunction;

			}

		});

		
		controler.run();
	}

	public static class PriceBroker implements ActivityStartEventHandler, ActivityEndEventHandler {

		private int electricityPrice = 0;

		@Inject
		private EventsManager eventsManager;

		@Override
		public void handleEvent(ActivityEndEvent event) {
			var newPrice = electricityPrice - 1;
			var nextPriceAtLeastZero = Math.max(0, newPrice);

			if (newPrice != nextPriceAtLeastZero) {

				electricityPrice = nextPriceAtLeastZero;
				eventsManager.processEvent(new ElectricityPriceEvent(event.getTime(), electricityPrice));
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			electricityPrice++;
			eventsManager.processEvent(new ElectricityPriceEvent(event.getTime(), electricityPrice));
		}

		@Override
		public void reset(int iteration) {
			electricityPrice = 0;
		}
	}

	public static class PriceListener implements BasicEventHandler {

		private final Set<Id<Person>> personsAtActivity = new HashSet<>();
		private final Map<Id<Person>, PersonRecord> personRecords = new HashMap<>();

		@Override
		public void handleEvent(Event event) {
			if (ElectricityPriceEvent.TYPE.equals(event.getEventType())) {

				var epe = (ElectricityPriceEvent) event;
				var price = epe.getPrice();

				if (price % 10 == 0) {
					for (var id : personsAtActivity) {
						if (personRecords.containsKey(id)) {
							var record = personRecords.get(id);
							var prevStatus = record.consumptionStatus;
							record.notifyAboutElectricityPrice(price);
							//System.out.println("after notify, price:" + price + ", threshold: " + record.threshold);
							if (!prevStatus.equals(record.consumptionStatus)) {
								System.out.println("Agent " + id + " has changed status to: " + record.consumptionStatus + " current price is: " + price);
							}
						}
					}
				}
			} else if (ActivityEndEvent.EVENT_TYPE.equals(event.getEventType())) {
				var actEndEvent = (ActivityEndEvent)event;
				personsAtActivity.remove(actEndEvent.getPersonId());
			} else if (ActivityStartEvent.EVENT_TYPE.equals(event.getEventType())) {
				var actStartEvent = (ActivityStartEvent)event;
				personsAtActivity.add(actStartEvent.getPersonId());
				//var individualThreshold = personRecords.size();
				personRecords.computeIfAbsent(actStartEvent.getPersonId(), id -> new PersonRecord(30));
			}
		}

		@Override
		public void reset(int iteration) {
			BasicEventHandler.super.reset(iteration);
		}
	}

	public static class PersonRecord {

		private final double threshold;
		private String consumptionStatus = "combustion";

		public PersonRecord(double threshold) {
			this.threshold = threshold;
		}

		public void notifyAboutElectricityPrice(double price) {
			this.consumptionStatus = price > threshold ? "combustion" : "landline";
		}
	}

	public static class ElectricityPriceEvent extends Event {

		public static final String TYPE = "electricity_price";

		private final double price;

		public double getPrice() {
			return price;
		}

		public ElectricityPriceEvent(double time, double price) {
			super(time);
			this.price = price;
		}

		@Override
		public String getEventType() {
			return TYPE;
		}
	}
	
}