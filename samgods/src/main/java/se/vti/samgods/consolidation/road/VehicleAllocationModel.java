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
import java.util.Set;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleAllocationModel {

	private final VehicleUtilityFunction utilityFunction;

	private final SingleVehicleSampler vehicleSampler;

	public VehicleAllocationModel(final VehicleUtilityFunction utilityFunction,
			final SingleVehicleSampler vehicleSampler) {
		this.utilityFunction = utilityFunction;
		this.vehicleSampler = vehicleSampler;
	}

	public Map<Vehicle, Double> allocate(final Shipment shipment, final Set<Vehicle> vehicles) {

		final Map<Vehicle, Double> vehicle2ton = new LinkedHashMap<>();
		double weightAllocatedSoFar_ton = 0.0;
		double utilityReceivedSoFar = 0.0;

		while (weightAllocatedSoFar_ton < shipment.getWeight_ton() - 1e-8) {

			final Map<Vehicle, Double> vehicle2extrapolatedUtility = new LinkedHashMap<>(vehicles.size());
			for (Vehicle vehicle : vehicles) {
				if (!vehicle2ton.containsKey(vehicle)) {
					if (this.utilityFunction.computeCompatibility(shipment, vehicle)) {
						vehicle2extrapolatedUtility.put(vehicle, this.utilityFunction.computeExtrapolatedUtility(
								shipment, vehicle, weightAllocatedSoFar_ton, utilityReceivedSoFar));
					}
				}
			}

			final Vehicle vehicle = this.vehicleSampler.drawVehicle(shipment, vehicle2extrapolatedUtility);
			if (vehicle == null) {
				return vehicle2ton;
			}
			final double newlyAllocatedWeight_ton = Math.min(shipment.getWeight_ton() - weightAllocatedSoFar_ton,
					vehicle.computeRemainingCapacity_ton());
			vehicle2ton.put(vehicle, newlyAllocatedWeight_ton);
			weightAllocatedSoFar_ton += newlyAllocatedWeight_ton;
			utilityReceivedSoFar += this.utilityFunction.computeUtility(shipment, vehicle);
		}

		return vehicle2ton;
	}
}
