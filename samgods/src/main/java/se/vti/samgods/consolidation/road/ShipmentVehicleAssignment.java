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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author GunnarF
 *
 */
public class ShipmentVehicleAssignment {

	class Assignment {
		final IndividualShipment shipment;
		final Vehicle vehicle;
		final double tons;

		Assignment(IndividualShipment shipment, Vehicle vehicle, double tons) {
			this.shipment = shipment;
			this.vehicle = vehicle;
			this.tons = tons;
		}
	}

	private Map<IndividualShipment, List<Assignment>> shipment2assignments = new LinkedHashMap<>();
	private Map<Vehicle, List<Assignment>> vehicle2assignments = new LinkedHashMap<>();
	private Map<Vehicle, Double> vehicle2payload_tons = new LinkedHashMap<>();

	public List<Assignment> getAssignments(IndividualShipment shipment) {
		return this.shipment2assignments.get(shipment);
	}

	public List<Assignment> getAssignments(Vehicle vehicle) {
		return this.vehicle2assignments.get(vehicle);
	}

	public void addAssignment(final IndividualShipment shipment, final Vehicle vehicle, final double tons) {
		if (shipment2assignments.containsKey(shipment)) {
			throw new RuntimeException("Shipment is already assigned, unassign first.");
		}
		Assignment assignment = new Assignment(shipment, vehicle, tons);
		this.shipment2assignments.computeIfAbsent(shipment, s -> new ArrayList<>()).add(assignment);
		this.vehicle2assignments.computeIfAbsent(vehicle, v -> new ArrayList<>()).add(assignment);
		this.vehicle2payload_tons.compute(vehicle, (v, pl) -> pl == null ? tons : pl + tons);
	}

	public void unassign(final IndividualShipment shipment, final Vehicle vehicle) {
		Assignment assignmentToRemove = this.shipment2assignments.get(shipment).stream()
				.filter(a -> a.vehicle.equals(vehicle)).findFirst().get();
		List<Assignment> vehicleAssignments = this.vehicle2assignments.get(vehicle);
		if (vehicleAssignments.size() == 1) {
			assert (vehicleAssignments.get(0) == assignmentToRemove);
			this.vehicle2assignments.remove(vehicle);
			this.vehicle2payload_tons.remove(vehicle);
		} else {
			final int removeIndex = vehicleAssignments.indexOf(assignmentToRemove);
			assert (vehicleAssignments.get(removeIndex) == assignmentToRemove);
			vehicleAssignments.remove(removeIndex);
			this.vehicle2payload_tons.compute(vehicle, (v, pl) -> pl - assignmentToRemove.tons);
		}
	}

	public void unassignAllShipments() {
		this.shipment2assignments.clear();
		this.vehicle2assignments.clear();
		this.vehicle2payload_tons.clear();
	}

	public double getPayload_ton(Vehicle vehicle) {
		return this.vehicle2payload_tons.getOrDefault(vehicle, 0.0);
	}

	public double getRemainingCapacity_ton(Vehicle vehicle) {
		return ConsolidationUtils.getCapacity_ton(vehicle) - this.vehicle2payload_tons.getOrDefault(vehicle, 0.0);
	}

}
