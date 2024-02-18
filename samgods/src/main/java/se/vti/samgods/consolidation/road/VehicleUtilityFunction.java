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

import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author GunnarF
 *
 */
public interface VehicleUtilityFunction {

	default boolean computeCompatibility(IndividualShipment shipment, Vehicle vehicle) {
//		final double remainingCap_ton = vehicle.computeRemainingCapacity_ton();
//		if (remainingCap_ton < 0.1) { // TODO magic number
//			return false;
//		}
	
		throw new UnsupportedOperationException("TODO");		
//		// shipment must be compatible with vehicle type
//		if (shipment.getType().isCompatible(vehicle.getType())) {
//			// shipment must be splittable or there must be enough space in vehicle
//			if (shipment.getType().isSplittable() 
//					|| ((remainingCap_ton >= shipment.getWeight_ton() - 1e-8)
//							&& (vehicle.computeRemainingCapacity_m3() >= shipment.getVolume_m3() - 1e-8))) {
//				// the must be no incompatible other shipments in vehicle
//				if (vehicle.getAssignedShipments2tons().keySet().stream()
//						.filter(s -> !s.getType().isCompatible(shipment.getType())).findFirst().isEmpty()) {
//					return true;
//				}
//			}
//		}
//		return false;
	}

	default double computeExtrapolatedUtility(IndividualShipment shipment, Vehicle vehicle, double weightAllocatedSoFar_ton,
			double utilityReceivedSoFar) {
//		final double remainingWeightToAllocate_ton = Math.max(0.0, shipment.getWeight_ton() - weightAllocatedSoFar_ton);
//		final double newlyReceivedUtility = this.computeUtility(shipment, vehicle);
//		final double newlyAllocatedWeight_ton = Math.min(remainingWeightToAllocate_ton,
//				vehicle.computeRemainingCapacity_ton());
//		return (utilityReceivedSoFar + newlyReceivedUtility) * shipment.getWeight_ton()
//				/ (weightAllocatedSoFar_ton + newlyAllocatedWeight_ton);
		throw new UnsupportedOperationException("Make this sequence independent");
	}

	public double computeUtility(IndividualShipment shipment, Vehicle vehicle);

}
