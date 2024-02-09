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
import java.util.Map;

/**
 * 
 * @author GunnarF
 *
 */
public class Vehicle {

	private final Object id;

	private final VehicleType type;

	private final Map<IndividualShipment, Double> assignedShipment2tons = new LinkedHashMap<>();

	public Vehicle(final Object id, final VehicleType vehicleType) {
		this.id = id;
		this.type = vehicleType;
	}

	public Object getId() {
		return this.id;
	}

	public VehicleType getType() {
		return this.type;
	}

	public Map<IndividualShipment, Double> getAssignedShipments2tons() {
		return this.assignedShipment2tons;
	}
	
	public void assignShipment(final IndividualShipment shipment, final double tons) {
		this.assignedShipment2tons.put(shipment, tons);
	}

	public void unassignShipment(final IndividualShipment shipment) {
		this.assignedShipment2tons.remove(shipment);
	}

	public void unassignAllShipments() {
		this.assignedShipment2tons.clear();
	}

	public double computePayload_ton() {
		return this.assignedShipment2tons.values().stream().mapToDouble(t -> t).sum();
	}

	public double computeRemainingCapacity_ton() {
		return this.type.getCapacity_ton() - this.computePayload_ton();
	}

//	public double computePayload_m3() {
//		return this.assignedShipment2tons.entrySet().stream()
//				.mapToDouble(e -> e.getKey().getType().computeVolume_m3(e.getValue())).sum();
//	}

//	public double computeRemainingCapacity_m3() {
//		return this.type.getCapacity_m3() - this.computePayload_m3();
//	}

	@Override
	public String toString() {
		return this.id + " of type " + this.type;
	}
}
