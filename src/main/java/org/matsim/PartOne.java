package org.matsim;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.MultibinderBinding;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

class PartOne{
	private static final Logger log = LogManager.getLogger( PartOne.class ) ;

	public static void main(String[] args){

		//traditional constructor injection
		// providing the bindings, not instanciating

		//Helper helper = new MyHelper2();
		//Simulation sim = new MySimulation();
		AbstractModule module1 = new AbstractModule() { // can be in one module too
			@Override protected void configure(){
				bind(Simulation.class).to( MySimulation1.class ) ;
			}
		};

		AbstractModule module2 = new AbstractModule() {
			@Override protected void configure(){
				bind( Helper.class ).to( MyHelper1.class ) ;
			}
		};

		Injector injector = Guice.createInjector(module1,module2);

//		Injector injector = Guice.createInjector( new AbstractModule(){
//			@Override protected void configure(){
//				bind(Simulation.class).to( MySimulation1.class ) ;
//				bind( Helper.class ).to( MyHelper1.class ) ;
////				MapBinder<String,TravelTime>.newMapBinder( this, "car",   )
//			}
//		} );

		Simulation sim = injector.getInstance(Simulation.class);
		sim.run() ;
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
