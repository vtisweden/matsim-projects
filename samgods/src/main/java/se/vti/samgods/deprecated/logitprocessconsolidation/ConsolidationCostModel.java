/**
 * se.vti.samgods.consolidation.road
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.transportation.consolidation.PerformanceMeasures;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationCostModel extends se.vti.samgods.transportation.consolidation.ConsolidationCostModel {

	// -------------------- CONSTANTS --------------------

	private static final double minTransferredAmount_ton = 1.0;

	// -------------------- CONSTRUCTION --------------------

	public ConsolidationCostModel(PerformanceMeasures performanceMeasures, Network network) {
		super();
	}

	public BasicTransportCost computeInVehicleShipmentCost(Vehicle vehicle, double maxAddedAmount_ton,
			ShipmentVehicleAssignment assignment) throws InsufficientDataException {

//		final double vehicleCapacity_ton = FreightVehicleAttributes.getCapacity_ton(vehicle);
//		final double availableCapacity_ton = vehicleCapacity_ton - assignment.getPayload_ton(vehicle);
//		assert (availableCapacity_ton >= 0);
//		final double assignedWeight_ton = Math.min(maxAddedAmount_ton, availableCapacity_ton);
//		final boolean feasible = assignedWeight_ton >= 0.01 * Math.max(maxAddedAmount_ton, vehicleCapacity_ton); // TODO

//		if (feasible) {
			throw new UnsupportedOperationException("TODO");
//			final DetailedTransportCost vehicleCost = computeEpisodeCost(
//					FreightVehicleAttributes.getFreightAttributes(vehicle),
//					assignment.getPayload_ton(vehicle) + assignedWeight_ton, assignment.getTransportEpisode());
//			final double share = assignedWeight_ton / (assignedWeight_ton + assignment.getPayload_ton(vehicle));
//			return new BasicTransportCost(assignedWeight_ton, share * vehicleCost.monetaryCost, vehicleCost.duration_h);
//		} else {
//			return null;
//		}
	}

}
