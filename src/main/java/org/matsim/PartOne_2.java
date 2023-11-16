package org.matsim;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.ScenarioUtils;

class PartOne_2 {
	private static final Logger log = LogManager.getLogger( PartOne_2.class ) ;

	private static class AnaEventHandler implements EventHandler {
	}

	private static class AnaControlerListener implements StartupListener {
		@Override
		public void notifyStartup(StartupEvent startupEvent) {
			log.warn("I am here");
		}
	}

	private static class AnaControlerListener2 implements IterationStartsListener {

		@Inject Helper helper;
		@Override public void notifyIterationStarts(IterationStartsEvent event){
			log.warn("Iteration start");
			helper.getAccessToSomething();
		}
	}
	public static void main(String[] args){

		//create config
		Config config = ConfigUtils.createConfig();

		//modify config
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(10);

		//create scenario
		Scenario scenario = ScenarioUtils.createScenario(config);

		//create controller
		Controler controler = new Controler(scenario);

		//modify controller
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(Helper.class).to(MyHelper2.class);
				//this.bindScoringFunctionFactory().toInstance(null);
				this.addEventHandlerBinding().to(AnaEventHandler.class);
				this.addControlerListenerBinding().to(AnaControlerListener.class);
				this.addControlerListenerBinding().to(AnaControlerListener2.class);
			}
		});

		controler.run();
	}

	interface Simulation {
		void run() ;
	}

	interface Helper {
		Object getAccessToSomething() ;

	}

	static class MySimulation1 implements Simulation {
		// arguments that would normally be in the constructor can now be obtained via @Inject !
		@Inject private Helper helper ;
		@Inject MySimulation1(Helper helper){
			log.info("calling for simulation 1");
			this.helper = helper;
		};
		@Override public void run() {
			log.info( "called MySimulation1 run method") ;
			helper.getAccessToSomething() ;
		}
	}
	static class MySimulation2 implements Simulation {
		@Inject Helper helper ;

		@Inject MySimulation2(){
			log.info("calling for simulation 2");
		};
		@Override public void run() {
			log.info( "called MySimulation2 run method") ;
			helper.getAccessToSomething() ;
		}
	}

	static class MyHelper1 implements Helper {
		@Override public Object getAccessToSomething(){
			log.info( "called MyHelper1 getAccess... method") ;
			return null ;
		}
	}
	static class MyHelper2 implements Helper {
		@Override public Object getAccessToSomething(){
			log.info( "called MyHelper2 getAccess... method") ;
			return null ;
		}
	}


}
