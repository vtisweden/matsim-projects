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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class ShipmentVehicleAssignment {

	// -------------------- MEMBERS --------------------

	private final Map<IndividualShipment, LinkedList<Vehicle>> shipment2vehicles = new LinkedHashMap<>();
	private final Map<Vehicle, LinkedList<IndividualShipment>> vehicle2shipments = new LinkedHashMap<>();
	private final Map<Tuple<IndividualShipment, Vehicle>, Double> shipmentAndVehicle2tons = new LinkedHashMap<>();
	private final Map<Vehicle, Double> vehicle2payload_ton = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ShipmentVehicleAssignment() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void assign(final IndividualShipment shipment, final Vehicle vehicle, final double tons) {
		this.shipment2vehicles.computeIfAbsent(shipment, s -> new LinkedList<>()).add(vehicle);
		this.vehicle2shipments.computeIfAbsent(vehicle, v -> new LinkedList<>()).add(shipment);
		this.shipmentAndVehicle2tons.put(new Tuple<>(shipment, vehicle), tons);
		this.vehicle2payload_ton.compute(vehicle, (v, pl) -> pl == null ? tons : pl + tons);
	}

	private <K, V> void reduceValueList(K key, V removeValue, Map<K, LinkedList<V>> mapToModify) {
		List<V> valueList = mapToModify.get(key);
		if (valueList.size() == 1) {
			mapToModify.remove(key);
		} else {
			valueList.remove(removeValue);
		}
	}

	public void unassign(final IndividualShipment shipment, final Vehicle vehicle) {
		this.reduceValueList(shipment, vehicle, this.shipment2vehicles);
		this.reduceValueList(vehicle, shipment, this.vehicle2shipments);

		final Tuple<IndividualShipment, Vehicle> shipmentAndVehicle = new Tuple<>(shipment, vehicle);
		final double removedTons = this.shipmentAndVehicle2tons.get(shipmentAndVehicle);
		this.shipmentAndVehicle2tons.remove(shipmentAndVehicle);
		if (this.vehicle2shipments.containsKey(vehicle)) {
			this.vehicle2payload_ton.compute(vehicle, (v, pl) -> pl - removedTons);
		} else { // We have removed the last shipment from the (now empty) vehicle.
			this.vehicle2payload_ton.remove(vehicle);
		}
	}

	public void unassignAllShipments() {
		this.shipment2vehicles.clear();
		this.vehicle2shipments.clear();
		this.shipmentAndVehicle2tons.clear();
		this.vehicle2payload_ton.clear();
	}

	public double getPayload_ton(Vehicle vehicle) {
		return this.vehicle2payload_ton.getOrDefault(vehicle, 0.0);
	}

	public double getRemainingCapacity_ton(Vehicle vehicle) {
		return ConsolidationUtils.getCapacity_ton(vehicle) - this.getPayload_ton(vehicle);
	}

}
