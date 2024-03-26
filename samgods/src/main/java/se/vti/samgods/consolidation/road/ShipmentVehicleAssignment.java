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
import se.vti.samgods.logistics.TransportChain;

/**
 * 
 * @author GunnarF
 *
 */
public class ShipmentVehicleAssignment {

	// -------------------- MEMBERS --------------------

	private final TransportChain transportChain;

	private final Map<Shipment, LinkedList<Vehicle>> shipment2vehicles = new LinkedHashMap<>();
	private final Map<Vehicle, LinkedList<Shipment>> vehicle2shipments = new LinkedHashMap<>();
	private final Map<Tuple<Shipment, Vehicle>, Double> shipmentAndVehicle2tons = new LinkedHashMap<>();
	private final Map<Vehicle, Double> vehicle2payload_ton = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ShipmentVehicleAssignment(TransportChain transportChain) {
		this.transportChain = transportChain;
	}

	// -------------------- GETTERS --------------------

	public TransportChain getTransportChain() {
		return this.transportChain;
	}

	public Map<Shipment, LinkedList<Vehicle>> getShipment2vehicles() {
		return shipment2vehicles;
	}

	public Map<Vehicle, LinkedList<Shipment>> getVehicle2shipments() {
		return vehicle2shipments;
	}

	public Map<Tuple<Shipment, Vehicle>, Double> getShipmentAndVehicle2tons() {
		return shipmentAndVehicle2tons;
	}

	public Map<Vehicle, Double> getVehicle2payload_ton() {
		return vehicle2payload_ton;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void assign(final Shipment shipment, final Vehicle vehicle, final double tons) {
		assert (!this.shipmentAndVehicle2tons.containsKey(new Tuple<>(shipment, vehicle)));

		this.shipment2vehicles.computeIfAbsent(shipment, s -> new LinkedList<>()).add(vehicle);
		this.vehicle2shipments.computeIfAbsent(vehicle, v -> new LinkedList<>()).add(shipment);
		this.shipmentAndVehicle2tons.put(new Tuple<>(shipment, vehicle), tons);
		this.vehicle2payload_ton.compute(vehicle, (v, pl) -> pl == null ? tons : pl + tons);
	}

	public void unassign(final Shipment shipment, final Vehicle vehicle) {
		final Tuple<Shipment, Vehicle> shipmentAndVehicle = new Tuple<>(shipment, vehicle);

		final List<Vehicle> vehiclesUsedForShipment = this.shipment2vehicles.get(shipment);
		if (vehiclesUsedForShipment.size() == 1) {
			this.shipment2vehicles.remove(shipment);
		} else {
			vehiclesUsedForShipment.remove(vehicle);
		}

		final List<Shipment> shipmentsInVehicle = this.vehicle2shipments.get(vehicle);
		if (shipmentsInVehicle.size() == 1) {
			this.vehicle2shipments.remove(vehicle);
			this.vehicle2payload_ton.remove(vehicle);
		} else {
			shipmentsInVehicle.remove(shipment);
			this.vehicle2payload_ton.compute(vehicle,
					(v, pl) -> pl - this.shipmentAndVehicle2tons.get(shipmentAndVehicle));
		}
		this.shipmentAndVehicle2tons.remove(shipmentAndVehicle);
	}

	public void unassign(final Shipment shipment) {
		if (this.shipment2vehicles.containsKey(shipment)) {
			for (Vehicle vehicle : this.shipment2vehicles.get(shipment)) {
				this.unassign(shipment, vehicle);
			}
			assert (!this.shipment2vehicles.containsKey(shipment));
		}
	}

	public void clear() {
		this.shipment2vehicles.clear();
		this.vehicle2shipments.clear();
		this.shipmentAndVehicle2tons.clear();
		this.vehicle2payload_ton.clear();
	}

	public double getPayload_ton(Vehicle vehicle) {
		return this.vehicle2payload_ton.getOrDefault(vehicle, 0.0);
	}	
}
