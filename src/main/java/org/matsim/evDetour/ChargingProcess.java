package org.matsim.evDetour;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.vehicles.Vehicle;

public class ChargingProcess {

	private Id<Person> personId;
	private Id<Vehicle> vehicleId;
	private Id<Charger> chargerId;
	private double detour;

	public Id<Person> getPersonId() {
		return personId;
	}

	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}

	public Id<Charger> getChargerId() {
		return chargerId;
	}

	public void setChargerId(Id<Charger> chargerId) {
		this.chargerId = chargerId;
	}

	public double getDetour() {
		return detour;
	}

	public void setDetour(double detour) {
		this.detour = detour;
	}
}
