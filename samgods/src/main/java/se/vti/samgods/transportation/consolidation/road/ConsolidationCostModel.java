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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Units;
import se.vti.samgods.BasicTransportCost;
import se.vti.samgods.DetailedTransportCost;
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

	// -------------------- CONSTANTS --------------------

	private static final double minTransferredAmount_ton = 1.0;

	// -------------------- MEMBERS --------------------

	private final PerformanceMeasures performanceMeasures;

	private final Network network;

	// -------------------- CONSTRUCTION --------------------

	public ConsolidationCostModel(PerformanceMeasures performanceMeasures, Network network) {
		this.performanceMeasures = performanceMeasures;
		this.network = network;
	}

	// -------------------- IMPLEMENTATION --------------------

	public DetailedTransportCost computeEpisodeCost(FreightVehicleAttributes vehicleAttrs, double payload_ton,
			TransportEpisode episode) {

		DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(payload_ton);

		/*
		 * Loading/unloading at origin/destination. Specific to this shipment, not
		 * shared.
		 */

		builder.addLoadingDuration_h(this.performanceMeasures.getTotalDepartureDelay_h(episode.getLoadingNode()));
		builder.addUnloadingDuration_h(this.performanceMeasures.getTotalArrivalDelay_h(episode.getUnloadingNode()));

		builder.addLoadingCost(vehicleAttrs.loadCost_1_ton.get(episode.getCommodity())
				* Math.max(minTransferredAmount_ton, payload_ton));
		builder.addUnloadingCost(vehicleAttrs.loadCost_1_ton.get(episode.getCommodity())
				* Math.max(minTransferredAmount_ton, payload_ton));

		/*
		 * Transfer at intermediate nodes. Specific to this shipment, not shared.
		 */

		builder.addTransferCost(
				episode.getTransferNodeCnt() * vehicleAttrs.transferCost_1_ton.get(episode.getCommodity())
						* Math.max(minTransferredAmount_ton, payload_ton));

		for (Id<Node> transferNodeId : episode.createTransferNodesList()) {
			builder.addTransferDuration_h(this.performanceMeasures.getTotalArrivalDelay_h(transferNodeId)
					+ this.performanceMeasures.getTotalDepartureDelay_h(transferNodeId));
		}

		/*
		 * Movement along network links.
		 */

		for (TransportLeg leg : episode.getLegs()) {

			double length_m = 0.0;
			double tt_s = 0.0;
			for (Link link : NetworkUtils.getLinks(this.network, leg.getRouteView())) {
				length_m += link.getLength();
				tt_s += vehicleAttrs.travelTimeOnLink_s(link);
			}

			builder.addMoveDuration_h(Units.H_PER_S * tt_s);
			if (SamgodsConstants.TransportMode.Ferry.equals(leg.getMode())) {
				builder.addMoveCost(Units.H_PER_S * tt_s * vehicleAttrs.onFerryCost_1_h);
				builder.addMoveCost(Units.KM_PER_M * length_m * vehicleAttrs.onFerryCost_1_km);
			} else {
				builder.addMoveCost(Units.H_PER_S * tt_s * vehicleAttrs.cost_1_h);
				builder.addMoveCost(Units.KM_PER_M * length_m * vehicleAttrs.cost_1_km);
			}
		}

		return builder.build();
	}

	public BasicTransportCost computeInVehicleShipmentCost(Vehicle vehicle, double maxAddedAmount_ton,
			ShipmentVehicleAssignment assignment) {

		final double vehicleCapacity_ton = ConsolidationUtils.getCapacity_ton(vehicle);
		final double availableCapacity_ton = vehicleCapacity_ton - assignment.getPayload_ton(vehicle);
		assert (availableCapacity_ton >= 0);
		final double assignedWeight_ton = Math.min(maxAddedAmount_ton, availableCapacity_ton);
		final boolean feasible = assignedWeight_ton >= 0.01 * Math.max(maxAddedAmount_ton, vehicleCapacity_ton); // TODO

		if (feasible) {
			final TransportCost vehicleCost = computeEpisodeCost(ConsolidationUtils.getFreightAttributes(vehicle),
					assignment.getPayload_ton(vehicle) + assignedWeight_ton, assignment.getTransportEpisode());
			final double share = assignedWeight_ton / (assignedWeight_ton + assignment.getPayload_ton(vehicle));
			return new BasicTransportCost(assignedWeight_ton, share * vehicleCost.getMonetaryCost(),
					vehicleCost.getDuration_h());
		} else {
			return null;
		}
	}

}
