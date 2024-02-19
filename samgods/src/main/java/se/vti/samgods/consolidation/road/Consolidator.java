/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package se.vti.samgods.consolidation.road;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class Consolidator {

	// -------------------- CONSTANTS --------------------

	private final VehicleFleet fleet;

	private final VehicleUtilityFunction vehicleUtilityFunction;

	private final VehicleSampler vehicleSampler;

	// -------------------- VARIABLES --------------------

	// exogeneously set

	private final List<Shipment> shipments = new ArrayList<>();

	// endogeneous

	private ShipmentVehicleAssignment assignment = null;

	private Map<Id<Vehicle>, Vehicle> assignedVehicles = null;

	// -------------------- CONSTRUCTION --------------------

	public Consolidator(VehicleFleet fleet, VehicleUtilityFunction vehicleUtilityFunction, VehicleSampler vehicleSampler) {
		this.fleet = fleet;
		this.vehicleUtilityFunction = vehicleUtilityFunction;
		this.vehicleSampler = vehicleSampler;
	}


	// -------------------- SETTERS AND GETTERS --------------------

	public void addShipment(Shipment shipment) {
		this.shipments.add(shipment);
	}

	public void addShipments(Collection<Shipment> shipments) {
		this.shipments.addAll(shipments);
	}

	// -------------------- INTERNALS --------------------

	private void removeUnusedNonPrototypeVehicles() {
		Set<Vehicle> remove = new LinkedHashSet<>();
		for (Vehicle vehicle : this.assignedVehicles.values()) {
			if (!this.assignment.isUsed(vehicle)) {
				remove.add(vehicle);
			}
		}
		for (Vehicle vehicle : remove) {
			this.assignedVehicles.remove(vehicle.getId());
		}
	}

	private void drawAssignment(Shipment shipment) {

		final Map<Vehicle, Double> vehicle2utility = this.assignedVehicles.values().stream().collect(Collectors.toMap(
				v -> v, v -> this.vehicleUtilityFunction.getUtility(shipment.getWeight_ton(), v, this.assignment)));

		double remaining_ton = shipment.getWeight_ton();
		while (remaining_ton > 1e-8) {

			final Vehicle drawnVehicle = this.vehicleSampler.drawVehicle(shipment, vehicle2utility);
			final Vehicle newVehicle;
			if (drawnVehicle instanceof PrototypeVehicle) {
				newVehicle = this.fleet.createVehicle(drawnVehicle.getType());
				this.assignedVehicles.put(newVehicle.getId(), newVehicle);
			} else {
				newVehicle = drawnVehicle;
			}

			final double assigned_ton = Math.min(remaining_ton, this.assignment.getRemainingCapacity_ton(newVehicle));
			this.assignment.assign(shipment, newVehicle, assigned_ton);
			remaining_ton -= assigned_ton;

			vehicle2utility.put(newVehicle,
					this.vehicleUtilityFunction.getUtility(remaining_ton, newVehicle, this.assignment));
		}
	}

	// -------------------- NEW SIMULATION LOGIC --------------------

	public void init() {
		this.assignment = new ShipmentVehicleAssignment();
		this.assignedVehicles = new LinkedHashMap<>(this.fleet.createPrototypeVehicles());

		Collections.shuffle(this.shipments);
		for (Shipment shipment : this.shipments) {
			this.drawAssignment(shipment);
		}
	}

	public void step() {
		Collections.shuffle(this.shipments);
		for (Shipment shipment : this.shipments) {
			this.assignment.unassign(shipment);
			this.removeUnusedNonPrototypeVehicles();
			this.drawAssignment(shipment);
		}
	}
}
