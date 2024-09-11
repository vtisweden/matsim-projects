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
package se.vti.samgods.deprecated.logitprocessconsolidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.deprecated.Shipment;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;
import se.vti.samgods.transportation.fleet.SamgodsVehicles;

/**
 * 
 * @author GunnarF
 *
 */
public class Consolidator {

	// -------------------- CONSTANTS --------------------

	private final Random rnd;

	private final TransportEpisode transportEpisode;

	private final SamgodsVehicles fleet;

	private final int shipmentPeriod_day;

	private final ConsolidationCostModel costModel;

	private final ConsolidationChoiceModel choiceModel;

	private final Map<VehicleType, Vehicle> prototypeVehicles;

	// -------------------- VARIABLES --------------------

	// exogeneously set

	private final List<Shipment> shipments = new ArrayList<>();

	// endogeneous

	private List<ShipmentVehicleAssignment> assignmentsOverDays = null;

//	private List<Map<Id<Vehicle>, Vehicle>> consideredVehiclesOverDays = null;

	// -------------------- CONSTRUCTION --------------------

	public Consolidator(Random rnd, TransportEpisode transportEpisode, SamgodsVehicles fleet, int shipmentPeriod_day,
			ConsolidationCostModel costModel, ConsolidationChoiceModel choiceModel) {
		this.rnd = rnd;
		this.transportEpisode = transportEpisode;
		this.fleet = fleet;
		this.shipmentPeriod_day = shipmentPeriod_day;
		this.costModel = costModel;
		this.choiceModel = choiceModel;
		this.prototypeVehicles = Collections
				.unmodifiableMap(PrototypeVehicle.createPrototypeVehicles(fleet.getVehicles()));
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void addShipment(Shipment shipment) {
		this.shipments.add(shipment);
	}

	public void addShipments(Collection<Shipment> shipments) {
		this.shipments.addAll(shipments);
	}

	public List<ShipmentVehicleAssignment> getAssignmentsOverDays() {
		return this.assignmentsOverDays;
	}

	// -------------------- INTERNALS --------------------

	private void drawAssignment(Shipment shipment) {

		if (this.rnd.nextDouble() >= shipment.getProbability()) {
			return;
		}

		/*
		 * (1) Identify which vehicles are available and what they cost.
		 */

		final List<Map<Vehicle, BasicTransportCost>> vehicle2costOverDays = new ArrayList<>(this.shipmentPeriod_day);
		for (int day = 0; day < this.shipmentPeriod_day; day++) {
			final Set<Vehicle> alreadyUsedVehicles = this.assignmentsOverDays.get(day).getVehicle2shipments().keySet();

			final Map<Vehicle, BasicTransportCost> veh2cost = new LinkedHashMap<>(
					this.prototypeVehicles.size() + alreadyUsedVehicles.size());
			for (Vehicle vehicle : this.prototypeVehicles.values()) {
				try {
					BasicTransportCost vehCost = this.costModel.computeInVehicleShipmentCost(vehicle,
							shipment.getWeight_ton(), this.assignmentsOverDays.get(day));
					if (vehCost != null) {
						veh2cost.put(vehicle, vehCost);
					}
				} catch (InsufficientDataException e) {
					e.log(this.getClass(), "TODO");
				}
			}
			for (Vehicle vehicle : alreadyUsedVehicles) {
				try {
					BasicTransportCost vehCost = this.costModel.computeInVehicleShipmentCost(vehicle,
							shipment.getWeight_ton(), this.assignmentsOverDays.get(day));
					if (vehCost != null) {
						veh2cost.put(vehicle, vehCost);
					}
				} catch (InsufficientDataException e) {
					e.log(this.getClass(), "TODO");
				}
			}

			vehicle2costOverDays.add(veh2cost);
		}

		/*
		 * (2) Repeatedly choose a vehicle and put as much load as possible into that
		 * vehicle.
		 * 
		 * TODO: Attention. If we want sequence independence then cost must not depend
		 * on earlier choices.
		 */

		double remaining_ton = shipment.getWeight_ton();
		while (remaining_ton > 1e-8) {

			// TODO DrawSlot (re)computes all choice probabilities each time when called.
			final ConsolidationChoiceModel.Slot slot = this.choiceModel.drawSlot(shipment, vehicle2costOverDays);

			final Vehicle assignedVehicle;
			if (slot.vehicle instanceof PrototypeVehicle) {
				assignedVehicle = this.fleet.createAndAddVehicle(slot.vehicle.getType());
			} else {
				assignedVehicle = slot.vehicle;
			}

//			final ShipmentVehicleAssignment assignment = this.assignmentsOverDays.get(slot.day);
//			final double assigned_ton = Math.min(remaining_ton,
//					SamgodsVehicleAttributes.getFreightAttributesSynchronized(assignedVehicle.getType()).capacity_ton - assignment.getPayload_ton(assignedVehicle));
//			assignment.assign(shipment, assignedVehicle, assigned_ton);
//			remaining_ton -= assigned_ton;
//
//			/*
//			 * Either the vehicle is full and and more useful for the considered shipment,
//			 * or it is not not full because the shipment is completely assigned. In either
//			 * case, it will no longer be considered.
//			 */
//			assert (Math.abs(SamgodsVehicleAttributes.getFreightAttributesSynchronized(assignedVehicle.getType()).capacity_ton
//					- assignment.getPayload_ton(assignedVehicle)) <= 1e-8 || remaining_ton <= 1e-8);
//			vehicle2costOverDays.get(slot.day).remove(assignedVehicle);

			throw new UnsupportedOperationException();
		}
	}

	// -------------------- SIMULATION LOGIC --------------------

	public void init() {

		this.assignmentsOverDays = new ArrayList<>(this.shipmentPeriod_day);
		for (int day = 0; day < this.shipmentPeriod_day; day++) {
			this.assignmentsOverDays.add(new ShipmentVehicleAssignment(this.transportEpisode));
		}

		Collections.shuffle(this.shipments);
		for (Shipment shipment : this.shipments) {
			this.drawAssignment(shipment);
		}
	}

	public void step() {
		Collections.shuffle(this.shipments);
		for (Shipment shipment : this.shipments) {
			for (ShipmentVehicleAssignment assignment : this.assignmentsOverDays) {
				assignment.unassign(shipment);
			}
			this.drawAssignment(shipment);
		}
	}
}
