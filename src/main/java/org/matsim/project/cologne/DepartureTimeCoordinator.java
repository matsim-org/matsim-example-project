package org.matsim.project.cologne;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;

import java.util.*;

/**
 * Implements departure time coordination for a fraction of the agent population.
 *
 * Coordination logic (simple, transparent, rule-based):
 * -------------------------------------------------------
 * 1. Select X% of agents randomly (deterministic seed for reproducibility)
 * 2. For selected agents, shift their morning departure time AWAY from the peak:
 *    - If departure is during peak (7:00-9:00), shift to shoulder period:
 *      - 50% of coordinated agents shift EARLIER by 45-90 minutes
 *      - 50% of coordinated agents shift LATER by 45-90 minutes
 *    - Agents already outside peak window are not modified
 * 3. Work end times are adjusted correspondingly to maintain 8h work duration
 *
 * This models a simple "staggered work hours" coordination policy where
 * a central coordinator recommends departure time changes to willing participants.
 */
public class DepartureTimeCoordinator {

    private static final double PEAK_START = 7.0 * 3600;   // 7:00 AM
    private static final double PEAK_END = 9.0 * 3600;     // 9:00 AM
    private static final double MIN_SHIFT = 45 * 60;        // 45 minutes
    private static final double MAX_SHIFT = 90 * 60;        // 90 minutes
    private static final double MIN_DEPARTURE = 5.0 * 3600; // 5:00 AM earliest
    private static final double MAX_DEPARTURE = 11.0 * 3600; // 11:00 AM latest

    private final double coordinationFraction;
    private final long seed;

    /**
     * @param coordinationFraction fraction of agents to coordinate (0.0 to 1.0)
     * @param seed random seed for reproducibility
     */
    public DepartureTimeCoordinator(double coordinationFraction, long seed) {
        this.coordinationFraction = coordinationFraction;
        this.seed = seed;
    }

    /**
     * Apply coordination to the population. Modifies plans in-place.
     * Returns the number of agents whose departure times were actually modified.
     */
    public int applyCoordination(Population population) {
        Random random = new Random(seed);
        List<Person> allPersons = new ArrayList<>(population.getPersons().values());

        // Deterministic shuffle for reproducible agent selection
        Collections.shuffle(allPersons, new Random(seed));

        int numToCoordinate = (int) Math.round(allPersons.size() * coordinationFraction);
        int modified = 0;

        for (int i = 0; i < numToCoordinate && i < allPersons.size(); i++) {
            Person person = allPersons.get(i);
            Plan plan = person.getSelectedPlan();

            if (plan == null) continue;

            for (PlanElement element : plan.getPlanElements()) {
                if (element instanceof Activity act) {
                    if ("home".equals(act.getType()) && act.getEndTime().isDefined()) {
                        double currentDeparture = act.getEndTime().seconds();

                        // Only shift agents departing during peak
                        if (currentDeparture >= PEAK_START && currentDeparture <= PEAK_END) {
                            double shift = MIN_SHIFT + random.nextDouble() * (MAX_SHIFT - MIN_SHIFT);

                            // 50% shift earlier, 50% shift later
                            if (random.nextBoolean()) {
                                shift = -shift; // earlier
                            }

                            double newDeparture = currentDeparture + shift;
                            newDeparture = Math.max(MIN_DEPARTURE, Math.min(MAX_DEPARTURE, newDeparture));

                            act.setEndTime(newDeparture);

                            // Also adjust work end time to maintain same work duration
                            adjustWorkEndTime(plan, currentDeparture, newDeparture);
                            modified++;
                            break; // only modify first home activity
                        }
                    }
                }
            }
        }

        return modified;
    }

    private void adjustWorkEndTime(Plan plan, double oldDeparture, double newDeparture) {
        double shift = newDeparture - oldDeparture;
        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Activity act && "work".equals(act.getType())) {
                if (act.getEndTime().isDefined()) {
                    act.setEndTime(act.getEndTime().seconds() + shift);
                }
                break;
            }
        }
    }

    public double getCoordinationFraction() {
        return coordinationFraction;
    }

    public String getLabel() {
        return String.format("%.0f%%", coordinationFraction * 100);
    }
}
