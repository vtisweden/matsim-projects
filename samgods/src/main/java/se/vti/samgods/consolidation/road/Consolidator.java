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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author GunnarF
 *
 */
public class Consolidator {

	// -------------------- CONSTANTS --------------------

	// -------------------- VARIABLES --------------------

	public final Set<IndividualShipment> shipments = new LinkedHashSet<>();

	private final Set<Vehicle> vehicles = new LinkedHashSet<>();

	private VehicleAllocationModel vehicleSampler = null;

	// -------------------- CONSTRUCTION --------------------

	public Consolidator() {
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void addVehicle(Vehicle vehicle) {
		this.vehicles.add(vehicle);
	}

	public void addVehicles(Collection<Vehicle> vehicles) {
		this.vehicles.addAll(vehicles);
	}
	
	public void addShipment(IndividualShipment shipment) {
		this.shipments.add(shipment);
	}
	
	public void addShipments(Collection<IndividualShipment> shipments) {
		this.shipments.addAll(shipments);
	}

	public void setVehicleSampler(VehicleAllocationModel allocationModel) {
		this.vehicleSampler = allocationModel;
	}

	// -------------------- INTERNALS --------------------

	private void step() {

		final List<IndividualShipment> shipmentsToReplan = new ArrayList<>(this.shipments);
		Collections.shuffle(shipmentsToReplan);

		for (IndividualShipment shipment : shipmentsToReplan) {

			for (Vehicle vehicle : shipment.getAssignedVehicle2tons().keySet()) {
				vehicle.unassignShipment(shipment);
			}
			shipment.clearAssignedVehicle2tons();

			final Map<Vehicle, Double> vehicle2tons = this.vehicleSampler.allocate(shipment, this.vehicles);

			for (Map.Entry<Vehicle, Double> entry : vehicle2tons.entrySet()) {
				entry.getKey().assignShipment(shipment, entry.getValue());
			}
			shipment.setAssignedVehicles(vehicle2tons);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

//	public double computeUtility() {
//		double result = 0.0;
//		for (Shipment shipment : this.shipments) {
//			if (shipment.getAssignedVehicle() != null) {
//				result += this.vehicleSampler.getUtilityFunction().computeUtility(shipment,
//						shipment.getAssignedVehicle());
//			}
//		}
//		return result;
//	}
//
//	public String assignmentToString() {
//		StringBuffer result = new StringBuffer();
//		for (Shipment shipment : this.shipments) {
//			result.append(shipment.getId() + " -> ");
//			if (shipment.getAssignedVehicle() == null) {
//				result.append("NULL\n");
//			} else {
//				result.append(shipment.getAssignedVehicle().getId() + "\n");
//			}
//		}
//		return result.toString();
//
//	}
//
//	public long countUnassigned() {
//		return this.shipments.stream().filter(s -> (s.getAssignedVehicle() == null)).count();
//	}
//
//	public Map<VehicleType, Integer> countVehicleTypes() {
//		Map<VehicleType, Integer> type2count = new LinkedHashMap<>();
//		for (Shipment shipment : this.shipments) {
//			type2count.compute(shipment.getAssignedVehicleType(), (t, c) -> (c == null) ? 1 : (c + 1));
//		}
//		return type2count;
//	}
//
//	public String vehicleUsagesToString() {
//		StringBuffer result = new StringBuffer();
//		for (Vehicle vehicle : this.vehicles) {
//			if (vehicle.computeCurrentVolume_m3() > 0) {
//				result.append(vehicle.getType() + " with ID " + vehicle.getId() + " loaded with "
//						+ vehicle.computeCurrentVolume_m3() + " out of " + vehicle.getType().getCapacity_m3() + "\n");
//			}
//		}
//		return result.toString();
//	}

	public ConsolidationReport createReport() {		
		ConsolidationReport report = new ConsolidationReport();
		this.vehicles.forEach(v -> report.addVehicleType(v.getType()));

		throw new UnsupportedOperationException("TODO");
//		this.shipments.forEach(s -> report.addShipmentType(s.getType()));

		//		this.vehicles.forEach(v -> report.add(v));
//		return report;
	}
	
	public void startRun(int iterations) {
		this.vehicles.stream().forEach(v -> v.unassignAllShipments());
		this.shipments.stream().forEach(s -> s.clearAssignedVehicle2tons());
		this.step();
		this.continueRun(iterations);
	}

	public void continueRun(int iterations) {
		for (int k = 0; k < iterations; k++) {
			this.step();
		}
	}

}
