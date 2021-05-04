package org.matsim.analysis;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

public class ReducePopulation {

    public static void main(String[] args) {

        var population = PopulationUtils.readPopulation("C:\\Users\\Janekdererste\\repos\\public-svn\\matsim\\scenarios\\countries\\de\\berlin\\berlin-v5.5-1pct\\input\\berlin-v5.5-1pct.plans.xml.gz");
        var person = findFirstPersonWithCarMode(population);

        var newPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());
        newPopulation.addPerson(person);

        PopulationUtils.writePopulation(newPopulation, "C:\\Users\\Janekdererste\\Desktop\\single-plan.xml.gz");
    }

    private static Person findFirstPersonWithCarMode(Population population) {
        for (var person : population.getPersons().values()) {

            for (var element : person.getSelectedPlan().getPlanElements()) {
                if (element instanceof Leg) {
                    var leg = (Leg) element;
                    if (leg.getMode().equals(TransportMode.car)) {
                        return person;
                    }
                }
            }
        }
        throw new RuntimeException("Didn't find anybody");
    }
}
