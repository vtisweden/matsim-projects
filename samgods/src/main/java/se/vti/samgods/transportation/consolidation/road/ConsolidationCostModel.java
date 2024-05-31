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
package se.vti.samgods.transportation.consolidation.road;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.TransportCost;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationCostModel {

	private static final double minTransferredAmount_ton = 1.0;

	private final PerformanceMeasures performanceMeasures;

	public ConsolidationCostModel(PerformanceMeasures performanceMeasures) {
		this.performanceMeasures = performanceMeasures;
	}

	public TransportCost getVehicleCost(FreightVehicleAttributes vehicleAttributes, double payload_ton,
			TransportEpisode episode) {

		// Total transport duration.
		double duration_h = 0.0;

		// Vehicle usage cost.
		double vehicleCost = 0.0;

		/*
		 * Loading/unloading at origin/destination. Specific to this shipment, not
		 * shared.
		 */

		duration_h += this.performanceMeasures.getTotalDepartureDelay_h(episode.getLoadingNode())
				+ this.performanceMeasures.getTotalArrivalDelay_h(episode.getUnloadingNode());
		vehicleCost += 2.0 * vehicleAttributes.loadCost_1_ton.get(episode.getCommodity())
				* Math.max(minTransferredAmount_ton, payload_ton);

		/*
		 * Transfer at intermediate nodes. Specific to this shipment, not shared. (The
		 * considered transport chain is constructed such that transfer (not
		 * loading/unloading) costs apply to all intermediate nodes.
		 */

		// The transfer cost arises once at each transfer point.
		vehicleCost += episode.getTransferNodeCnt() * vehicleAttributes.transferCost_1_ton.get(episode.getCommodity())
				* Math.max(minTransferredAmount_ton, payload_ton);

		for (Id<Node> internalNodeId : episode.createTransferNodesList()) {
			duration_h += this.performanceMeasures.getTotalArrivalDelay_h(internalNodeId)
					+ this.performanceMeasures.getTotalDepartureDelay_h(internalNodeId);
		}

		/*
		 * Movement along network links.
		 */

		for (TransportLeg leg : episode.getLegs()) {
			final double legDur_h = Units.H_PER_S * leg.getDuration_s();
			final double legLen_km = Units.KM_PER_M * leg.getLength_m();
			duration_h += legDur_h;
			if (SamgodsConstants.TransportMode.Ferry.equals(leg.getMode())) {
				vehicleCost += legDur_h * vehicleAttributes.onFerryCost_1_h;
				vehicleCost += legLen_km * vehicleAttributes.onFerryCost_1_km;
			} else {
				vehicleCost += legDur_h * vehicleAttributes.cost_1_h;
				vehicleCost += legLen_km * vehicleAttributes.cost_1_km;
			}
		}

		return new TransportCost(payload_ton, vehicleCost, duration_h);
	}

	public TransportCost getShipmentCost(Vehicle vehicle, double maxAddedAmount_ton,
			ShipmentVehicleAssignment assignment) {

		final double vehicleCapacity_ton = ConsolidationUtils.getCapacity_ton(vehicle);
		final double availableCapacity_ton = vehicleCapacity_ton - assignment.getPayload_ton(vehicle);
		final double assignedWeight_ton = Math.min(maxAddedAmount_ton, availableCapacity_ton);
		final boolean feasible = assignedWeight_ton >= 0.01 * Math.max(maxAddedAmount_ton, vehicleCapacity_ton); // TODO

		if (feasible) {
			final TransportCost vehicleCost = getVehicleCost(ConsolidationUtils.getFreightAttributes(vehicle),
					assignment.getPayload_ton(vehicle) + assignedWeight_ton, assignment.getTransportEpisode());
			final double share = assignedWeight_ton / (assignedWeight_ton + assignment.getPayload_ton(vehicle));
			return new TransportCost(assignedWeight_ton, share * vehicleCost.monetaryCost, vehicleCost.duration_h);
		} else {
			return null;
		}
	}

}
