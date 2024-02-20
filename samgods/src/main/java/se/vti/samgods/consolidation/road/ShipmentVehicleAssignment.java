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

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Tuple;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class ShipmentVehicleAssignment {

	// -------------------- MEMBERS --------------------

	private final Map<Shipment, LinkedList<Vehicle>> shipment2vehicles = new LinkedHashMap<>();
	private final Map<Vehicle, LinkedList<Shipment>> vehicle2shipments = new LinkedHashMap<>();
	private final Map<Tuple<Shipment, Vehicle>, Double> shipmentAndVehicle2tons = new LinkedHashMap<>();
	private final Map<Vehicle, Double> vehicle2payload_ton = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ShipmentVehicleAssignment() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<Vehicle> getVehicles(Shipment shipment) {
		// TODO make unmodifiable
		return this.shipment2vehicles.get(shipment);
	}

	public List<Shipment> getShipments(Vehicle vehicle) {
		// TODO make unmodifiable
		return this.vehicle2shipments.get(vehicle);
	}

	public boolean usesVehicle(Vehicle vehicle) {
		return this.vehicle2shipments.containsKey(vehicle);
	}

	public void assign(final Shipment shipment, final Vehicle vehicle, final double tons) {
		assert (!this.shipmentAndVehicle2tons.containsKey(new Tuple<>(shipment, vehicle)));

		this.shipment2vehicles.computeIfAbsent(shipment, s -> new LinkedList<>()).add(vehicle);
		this.vehicle2shipments.computeIfAbsent(vehicle, v -> new LinkedList<>()).add(shipment);
		this.shipmentAndVehicle2tons.put(new Tuple<>(shipment, vehicle), tons);
		this.vehicle2payload_ton.compute(vehicle, (v, pl) -> pl == null ? tons : pl + tons);
	}

	private void takeShipmentOutOfVehicle(final Shipment shipment, final Vehicle vehicle) {

		final List<Vehicle> vehiclesUsedForShipment = this.shipment2vehicles.get(shipment);
		if (vehiclesUsedForShipment.size() == 1) {
			this.shipment2vehicles.remove(shipment);
		} else {
			vehiclesUsedForShipment.remove(vehicle);
		}

		final List<Shipment> shipmentsInVehicle = this.vehicle2shipments.get(vehicle);
		final Tuple<Shipment, Vehicle> shipmentAndVehicle = new Tuple<>(shipment, vehicle);
		if (shipmentsInVehicle.size() == 1) {
			this.vehicle2shipments.remove(vehicle);
			this.vehicle2payload_ton.remove(vehicle);
		} else {
			shipmentsInVehicle.remove(shipment);
			final double removedTons = this.shipmentAndVehicle2tons.get(shipmentAndVehicle);
			this.vehicle2payload_ton.compute(vehicle, (v, pl) -> pl - removedTons);
		}
		this.shipmentAndVehicle2tons.remove(shipmentAndVehicle);
	}

	public void unassign(final Shipment shipment, final Vehicle vehicle) {
		this.takeShipmentOutOfVehicle(shipment, vehicle);
	}

	public void unassign(final Shipment shipment) {
		if (this.shipment2vehicles.containsKey(shipment)) {
			for (Vehicle vehicle : this.shipment2vehicles.get(shipment)) {
				this.takeShipmentOutOfVehicle(shipment, vehicle);
			}
			this.shipment2vehicles.remove(shipment);
		}
	}

	public boolean isUsed(final Vehicle vehicle) {
		return this.vehicle2shipments.containsKey(vehicle) && (this.vehicle2shipments.get(vehicle).size() > 0);
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

	public double getRemainingCapacity_ton(Vehicle vehicle) {
		return ConsolidationUtils.getCapacity_ton(vehicle) - this.getPayload_ton(vehicle);
	}

	// -------------------- TODO --------------------

	public interface CommodityAssignmentReport {

		public SamgodsConstants.Commodity getCommodity();

		public double getTotalAssignedTons();

		public long getTotalShipmentCount();

		public Map<Id<VehicleType>, Double> getVehicleType2shippedTons();

		public Map<Id<VehicleType>, Integer> getVehicleType2shipmentCnt();

	}

	public void createReport() {

		double total_ton = 0.0;
		long total_cnt = 0l;

		final Map<Id<VehicleType>, Double> vehicleTypeId2ton = new LinkedHashMap<>();

		final Map<Tuple<SamgodsConstants.Commodity, VehicleType>, Double> commodityAndVehicleType2ton = new LinkedHashMap<>();
		final Map<Tuple<SamgodsConstants.Commodity, VehicleType>, Integer> commodityAndVehicleType2cnt = new LinkedHashMap<>();
		for (Map.Entry<Tuple<Shipment, Vehicle>, Double> entry : this.shipmentAndVehicle2tons.entrySet()) {
			final Shipment shipment = entry.getKey().getA();
			final Vehicle vehicle = entry.getKey().getB();
			vehicleTypeId2ton.compute(vehicle.getType().getId(),
					(type, ton) -> ton == null ? entry.getValue() : ton + entry.getValue());
		}

	}

}
