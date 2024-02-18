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

import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * 
 * @author GunnarF
 *
 */
public class Consolidator {

	// -------------------- CONSTANTS --------------------

	private final VehicleAllocationModel vehicleAllocationModel;

	// -------------------- VARIABLES --------------------

	// exogeneously set

	private final Map<VehicleType, Integer> vehicleType2maxNumber = new LinkedHashMap<>();

	private final Set<Shipment> shipments = new LinkedHashSet<>();

	// endogeneous

	private final ShipmentVehicleAssignment assignment = new ShipmentVehicleAssignment();
	
	// TODO populate!
	private Set<Vehicle> vehicles = new LinkedHashSet<>();
	
	// -------------------- CONSTRUCTION --------------------

	public Consolidator(VehicleAllocationModel vehicleAllocationModel) {
		this.vehicleAllocationModel = vehicleAllocationModel;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void addVehicleType(VehicleType vehicleType) {
		this.vehicleType2maxNumber.put(vehicleType, Integer.MAX_VALUE);
	}

	public void addVehicleType(VehicleType vehicleType, int maxNumber) {
		this.vehicleType2maxNumber.put(vehicleType, maxNumber);
	}

	public void addShipment(Shipment shipment) {
		this.shipments.add(shipment);
	}

	public void addShipments(Collection<Shipment> shipments) {
		this.shipments.addAll(shipments);
	}

	// -------------------- INTERNALS --------------------

	private void step() {

		final List<Shipment> shipmentsToReplan = new ArrayList<>(this.shipments);
		Collections.shuffle(shipmentsToReplan);

		for (Shipment shipment : shipmentsToReplan) {
			this.assignment.unassign(shipment);			
			final Map<Vehicle, Double> vehicle2tons = this.vehicleAllocationModel.allocate(shipment, this.vehicles, this.assignment);
			for (Map.Entry<Vehicle, Double> entry : vehicle2tons.entrySet()) {
				this.assignment.assign(shipment, entry.getKey(), entry.getValue());
			}
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

//	public ConsolidationReport createReport() {
//		ConsolidationReport report = new ConsolidationReport();
//		this.vehicles.forEach(v -> report.addVehicleType(v.getType()));
//
//		throw new UnsupportedOperationException("TODO");
////		this.shipments.forEach(s -> report.addShipmentType(s.getType()));
//
//		// this.vehicles.forEach(v -> report.add(v));
////		return report;
//	}

	public void startRun(int iterations) {
		this.assignment.clear();
		this.step();
		this.continueRun(iterations);
	}

	public void continueRun(int iterations) {
		for (int k = 0; k < iterations; k++) {
			this.step();
		}
	}

}
