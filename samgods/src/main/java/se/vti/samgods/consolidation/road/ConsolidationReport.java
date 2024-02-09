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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationReport {

	private Set<ShipmentType> shipmentTypes = new LinkedHashSet<>();
	private Set<VehicleType> vehicleTypes = new LinkedHashSet<>();

	private Map<Tuple<ShipmentType, VehicleType>, Double> commodityAndVehicle2Ton = new LinkedHashMap<>();

	void addShipmentType(ShipmentType shipmentType) {
		this.shipmentTypes.add(shipmentType);
	}
	
	void addVehicleType(VehicleType vehicleType) {
		this.vehicleTypes.add(vehicleType);
	}
	
	void add(Vehicle vehicle) {
		for (Map.Entry<Shipment, Double> entry : vehicle.getAssignedShipments2tons().entrySet()) {
			Tuple<ShipmentType, VehicleType> key = new Tuple<>(entry.getKey().getType(), vehicle.getType());
			if (this.shipmentTypes.contains(key.getA()) && this.vehicleTypes.contains(key.getB())) {
				this.commodityAndVehicle2Ton.compute(key,
						(k, t) -> t == null ? entry.getValue() : t + entry.getValue());
			}
		}
	}

	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append("\t");
		for (VehicleType vehicleType : this.vehicleTypes) {
			result.append(vehicleType.getName());
			result.append("\t");
		}
		result.append("\n");

		for (ShipmentType shipmentType : this.shipmentTypes) {
			result.append(shipmentType.getName());
			result.append("\t");
			for (VehicleType vehicleType : this.vehicleTypes) {
				result.append(this.commodityAndVehicle2Ton.getOrDefault(new Tuple<>(shipmentType, vehicleType), 0.0));
				result.append("\t");
			}
			result.append("\n");
		}

		return result.toString();
	}

}
