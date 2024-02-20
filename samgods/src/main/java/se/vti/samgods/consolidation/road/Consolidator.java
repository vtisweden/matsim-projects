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
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class Consolidator {

	// -------------------- CONSTANTS --------------------

	private final VehicleFleet fleet;

	private final int shipmentPeriod_day;

	private final ConsolidationSlotChoiceModel slotChoiceModel;

	// -------------------- VARIABLES --------------------

	// exogeneously set

	private final List<Shipment> shipments = new ArrayList<>();

	// endogeneous

	private Map<Shipment, Integer> shipment2day = null;

	private List<ShipmentVehicleAssignment> assignmentsOverDays = null;

	private List<Map<Id<Vehicle>, Vehicle>> consideredVehiclesOverDays = null;

	// -------------------- CONSTRUCTION --------------------

	public Consolidator(VehicleFleet fleet, int shipmentPeriod_day, ConsolidationSlotChoiceModel vehicleSampler) {
		this.fleet = fleet;
		this.shipmentPeriod_day = shipmentPeriod_day;
		this.slotChoiceModel = vehicleSampler;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void addShipment(Shipment shipment) {
		this.shipments.add(shipment);
	}

	public void addShipments(Collection<Shipment> shipments) {
		this.shipments.addAll(shipments);
	}

	// -------------------- INTERNALS --------------------

	private void drawAssignment(Shipment shipment) {

		final List<Map<Vehicle, Double>> vehicle2utilityOverDays = new ArrayList<>(this.shipmentPeriod_day);
		for (int day = 0; day < this.shipmentPeriod_day; day++) {
			final Map<Vehicle, Double> veh2utl = new LinkedHashMap<>(this.consideredVehiclesOverDays.get(day).size());
			for (Vehicle veh : this.consideredVehiclesOverDays.get(day).values()) {
				veh2utl.put(veh, this.slotChoiceModel.getUtility(shipment.getWeight_ton(), veh, day,
						this.assignmentsOverDays.get(day)));
			}
			vehicle2utilityOverDays.add(veh2utl);
		}

		double remaining_ton = shipment.getWeight_ton();
		while (remaining_ton > 1e-8) {

			// TODO DrawSlot (re)computes all choice probabilities each time when called.
			final ConsolidationSlotChoiceModel.Slot slot = this.slotChoiceModel.drawSlot(shipment,
					vehicle2utilityOverDays);

			final Vehicle newVehicle;
			if (slot.vehicle instanceof PrototypeVehicle) {
				newVehicle = this.fleet.createVehicle(slot.vehicle.getType());
				this.consideredVehiclesOverDays.get(slot.day).put(newVehicle.getId(), newVehicle);
			} else {
				newVehicle = slot.vehicle;
			}

			final ShipmentVehicleAssignment assignment = this.assignmentsOverDays.get(slot.day);
			final double assigned_ton = Math.min(remaining_ton, assignment.getRemainingCapacity_ton(newVehicle));
			assignment.assign(shipment, newVehicle, assigned_ton);
			remaining_ton -= assigned_ton;

			vehicle2utilityOverDays.get(slot.day).put(newVehicle,
					this.slotChoiceModel.getUtility(remaining_ton, newVehicle, slot.day, assignment));
		}
	}

	// -------------------- NEW SIMULATION LOGIC --------------------

	public void init() {

		this.shipment2day = new LinkedHashMap<>();
		this.assignmentsOverDays = new ArrayList<>(this.shipmentPeriod_day);
		this.consideredVehiclesOverDays = new ArrayList<>(this.shipmentPeriod_day);
		for (int day = 0; day < this.shipmentPeriod_day; day++) {
			this.assignmentsOverDays.add(new ShipmentVehicleAssignment());
			this.consideredVehiclesOverDays.add(new LinkedHashMap<>(this.fleet.createPrototypeVehicles()));
		}

		Collections.shuffle(this.shipments);
		for (Shipment shipment : this.shipments) {
			this.drawAssignment(shipment);
		}
	}

	public void step() {
		Collections.shuffle(this.shipments);
		for (Shipment shipment : this.shipments) {

			final int day = this.shipment2day.get(shipment);
			final ShipmentVehicleAssignment assignment = this.assignmentsOverDays.get(day);
			final Map<Id<Vehicle>, Vehicle> consideredVehicles = this.consideredVehiclesOverDays.get(day);
			assignment.unassign(shipment);
			for (Vehicle vehicle : consideredVehicles.values()) {
				if (!assignment.usesVehicle(vehicle) /* assignment uses no prototype vehicles */) {
					consideredVehicles.remove(vehicle.getId());
				}
			}
			this.shipment2day.remove(shipment);

			this.drawAssignment(shipment);
		}
	}
}
