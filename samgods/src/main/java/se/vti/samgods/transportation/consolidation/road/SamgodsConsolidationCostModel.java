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
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.fleet.FreightVehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsConsolidationCostModel implements ConsolidationCostModel {

	private static final double minTransferredAmount_ton = 1.0;

	private final PerformanceMeasures performanceMeasures;

	public SamgodsConsolidationCostModel(PerformanceMeasures performanceMeasures) {
		this.performanceMeasures = performanceMeasures;
	}

	// TODO include ferry in network loading, routing, ... !!!

	// TODO Check: If all goes as it should, there should be alternative
	// containerized / non-containerized vehicles available.

	@Override
	public Cost getCost(Vehicle vehicle, Commodity addedCommodity, double maxAddedAmount_ton,
			ShipmentVehicleAssignment assignment) {

		final double vehicleCapacity_ton = ConsolidationUtils.getCapacity_ton(vehicle);
		final double availableCapacity_ton = vehicleCapacity_ton - assignment.getPayload_ton(vehicle);
		final double assignedWeight_ton = Math.min(maxAddedAmount_ton, availableCapacity_ton);
		final boolean feasible = assignedWeight_ton >= 0.01 * Math.max(maxAddedAmount_ton, vehicleCapacity_ton); // TODO

		if (feasible) {

			final FreightVehicleFleet.TypeAttributes vehicleAttributess = ConsolidationUtils
					.getFreightAttributes(vehicle);

			// Total transport duration.
			double duration_h = 0.0;

			// Vehicle usage cost.
			double vehicleCost = 0.0;

			/*
			 * Loading/unloading at origin/destination. Specific to this shipment, not
			 * shared.
			 */

			duration_h += this.performanceMeasures
					.getTotalDepartureDelay_h(assignment.getTransportEpisode().getLoadingNode())
					+ this.performanceMeasures
							.getTotalArrivalDelay_h(assignment.getTransportEpisode().getUnloadingNode());
			vehicleCost += 2.0 * vehicleAttributess.loadCost_1_ton.get(addedCommodity)
					* Math.max(minTransferredAmount_ton, assignedWeight_ton);

			/*
			 * Transfer at intermediate nodes. Specific to this shipment, not shared. (The
			 * considered transport chain is constructed such that transfer (not
			 * loading/unloading) costs apply to all intermediate nodes.
			 */

			// The transfer cost arises once at each transfer point.
			vehicleCost += assignment.getTransportEpisode().getTransferNodeCnt()
					* vehicleAttributess.transferCost_1_ton.get(addedCommodity)
					* Math.max(minTransferredAmount_ton, assignedWeight_ton);

			for (Id<Node> internalNodeId : assignment.getTransportEpisode().createTransferNodesList()) {
				duration_h += this.performanceMeasures.getTotalArrivalDelay_h(internalNodeId)
						+ this.performanceMeasures.getTotalDepartureDelay_h(internalNodeId);
			}

			/*
			 * Movement along network links.
			 */

			double sharedCost = 0.0;
			for (TransportLeg leg : assignment.getTransportEpisode().getLegs()) {
				final double legDur_h = Units.H_PER_S * leg.getDuration_s();
				final double legLen_km = Units.KM_PER_M * leg.getLength_m();
				duration_h += legDur_h;
				if (SamgodsConstants.TransportMode.Ferry.equals(leg.getMode())) {
					sharedCost += legDur_h * vehicleAttributess.onFerryCost_1_h;
					sharedCost += legLen_km * vehicleAttributess.onFerryCost_1_km;
				} else {
					sharedCost += legDur_h * vehicleAttributess.cost_1_h;
					sharedCost += legLen_km * vehicleAttributess.cost_1_km;
				}
			}
			vehicleCost += sharedCost * assignedWeight_ton / (assignedWeight_ton + assignment.getPayload_ton(vehicle));

			return new Cost(true, assignedWeight_ton, vehicleCost, duration_h);

			/*
			 * Vehicle/vessel type specific costs: Cost for loading at the sender and
			 * unloading at the receiver.
			 * 
			 * Vehicle/vessel pair specific costs: Transfer costs at lorry terminals, ports,
			 * railway terminals and airports; the transfer costs are given per tonne per
			 * vehicle/vessel type. The minimum transfer cost in the Swedish model are the
			 * costs of transferring one tonne.
			 * 
			 * Except for the following cases: Transfers involving ferries, Transfers
			 * involving RoRo-vessels, Rail-Rail transfers [[DOES NOT APPLY, THIS IS FOR
			 * LORRY]].
			 */

			/*
			 * we assume that if unitised transport is chosen, this will refer to all OD
			 * legs of the PWC relation: there is no stuffing and stripping of containers at
			 * consolidation and distribution centres, but only transfer of entire
			 * containers between sub-modes.
			 */

			/*
			 * In the cost functions, the time-based cost only apply to the time on the link
			 * (including loading and unloading time), not to the wait time in the nodes.
			 * The wait time in the nodes is only used for the capital cost on the inventory
			 * in transit.
			 */

			// Total transport cost, to be distributed across all shipments.

			/*
			 * Link-based cost: Distance-based costs (given in the cost functions as cost
			 * per kilometre per vehicle/vessel, for each of the vehicle/vessel types.
			 * 
			 * Time-based costs: These are given in the cost functions as cost per hour per
			 * vehicle/vessel for all the vehicle/vessel alternatives). These are only the
			 * time costs of the vehicle.
			 */

		} else {
			return new Cost(false, 0.0, 0.0, 0.0);
		}
	}

}
