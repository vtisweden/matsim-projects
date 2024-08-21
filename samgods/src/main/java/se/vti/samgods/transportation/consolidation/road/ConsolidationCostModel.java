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

import java.util.Collections;
import java.util.List;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Units;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.network.LinkAttributes;
import se.vti.samgods.transportation.BasicTransportCost;
import se.vti.samgods.transportation.DetailedTransportCost;
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

	// -------------------- INTERNALS --------------------

	private void addEpisodeLegCostToBuilder(FreightVehicleAttributes vehicleAttrs, double payload_ton,
			TransportEpisode episode, TransportLeg leg, DetailedTransportCost.Builder builder)
			throws InsufficientDataException {

		if (leg == episode.getLegs().getFirst()) {
			builder.addLoadingDuration_h(vehicleAttrs.loadTime_h.get(episode.getCommodity()));
			builder.addLoadingDuration_h(this.performanceMeasures.getTotalDepartureDelay_h(episode.getLoadingNode()));
			builder.addLoadingCost(vehicleAttrs.loadCost_1_ton.get(episode.getCommodity())
					* Math.max(minTransferredAmount_ton, payload_ton));
		} else {
			builder.addTransferDuration_h(0.5 * vehicleAttrs.transferTime_h.get(episode.getCommodity()));
			builder.addTransferDuration_h(this.performanceMeasures.getTotalDepartureDelay_h(leg.getOrigin()));
			builder.addTransferCost(0.5 * vehicleAttrs.transferCost_1_ton.get(episode.getCommodity())
					* Math.max(minTransferredAmount_ton, payload_ton));
		}

		for (Link link : NetworkUtils.getLinks(this.network, leg.getRouteIdsView())) {
			double length_km = Units.KM_H_PER_M_S * link.getLength();
			double tt_h = Units.H_PER_S * vehicleAttrs.travelTimeOnLink_s(link);
			builder.addMoveDuration_h(tt_h);
			if (LinkAttributes.isFerry(link)) {
				builder.addMoveCost(tt_h * vehicleAttrs.onFerryCost_1_h);
				builder.addMoveCost(length_km * vehicleAttrs.onFerryCost_1_km);
			} else {
				builder.addMoveCost(tt_h * vehicleAttrs.cost_1_h);
				builder.addMoveCost(length_km * vehicleAttrs.cost_1_km);
			}
		}

		if (leg == episode.getLegs().getLast()) {
			builder.addUnloadingDuration_h(this.performanceMeasures.getTotalArrivalDelay_h(episode.getUnloadingNode()));
			builder.addUnloadingDuration_h(vehicleAttrs.loadTime_h.get(episode.getCommodity()));
			builder.addUnloadingCost(vehicleAttrs.loadCost_1_ton.get(episode.getCommodity())
					* Math.max(minTransferredAmount_ton, payload_ton));
		} else {
			builder.addTransferDuration_h(this.performanceMeasures.getTotalArrivalDelay_h(leg.getDestination()));
			builder.addTransferDuration_h(0.5 * vehicleAttrs.transferTime_h.get(episode.getCommodity()));
			builder.addTransferCost(0.5 * vehicleAttrs.transferCost_1_ton.get(episode.getCommodity())
					* Math.max(minTransferredAmount_ton, payload_ton));
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public DetailedTransportCost computeEpisodeCost(FreightVehicleAttributes vehicleAttrs, double payload_ton,
			TransportEpisode episode, TransportLeg leg) throws InsufficientDataException {

		DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(payload_ton)
				.addLoadingDuration_h(0.0).addTransferDuration_h(0.0).addUnloadingDuration_h(0.0).addMoveDuration_h(0.0)
				.addLoadingCost(0.0).addTransferCost(0.0).addUnloadingCost(0.0).addMoveCost(0.0);

		final List<TransportLeg> evaluatedLegs;
		if (leg == null) {
			evaluatedLegs = episode.getLegs();
		} else {
			evaluatedLegs = Collections.singletonList(leg);
		}
		leg = null; // use evaluatedLegs

		for (TransportLeg evaluatedLeg : evaluatedLegs) {
			this.addEpisodeLegCostToBuilder(vehicleAttrs, payload_ton, episode, evaluatedLeg, builder);
		}

		return builder.build();
	}

	public DetailedTransportCost computeSignatureCost(FreightVehicleAttributes vehicleAttrs, double payload_ton,
			Signature.ConsolidationEpisode signature) throws InsufficientDataException {
		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(payload_ton)
				.addLoadingDuration_h(0.0).addTransferDuration_h(0.0).addUnloadingDuration_h(0.0).addMoveDuration_h(0.0)
				.addLoadingCost(0.0).addTransferCost(0.0).addUnloadingCost(0.0).addMoveCost(0.0);
		this.addSignatureCostToBuilder(vehicleAttrs, payload_ton, signature, builder);
		return builder.build();
	}

	public void addSignatureCostToBuilder(FreightVehicleAttributes vehicleAttrs, double payload_ton,
			Signature.ConsolidationEpisode signature, DetailedTransportCost.Builder builder)
			throws InsufficientDataException {

		final List<Link> firstLinks = signature.links.get(0);
		final List<Link> lastLinks = signature.links.get(signature.links.size() - 1);

		for (List<Link> links : signature.links) {

			if (links.size() > 0) {

				// final List<Link> links = NetworkUtils.getLinks(this.network, linkIds);
				final Id<Node> firstNodeId = links.get(0).getFromNode().getId();
				final Id<Node> lastNodeId = links.get(links.size() - 1).getToNode().getId();

				if (signature.loadAtStart && (links == firstLinks)) {
					builder.addLoadingDuration_h(vehicleAttrs.loadTime_h.get(signature.commodity));
					builder.addLoadingDuration_h(this.performanceMeasures.getTotalDepartureDelay_h(firstNodeId));
					builder.addLoadingCost(vehicleAttrs.loadCost_1_ton.get(signature.commodity)
							* Math.max(minTransferredAmount_ton, payload_ton));
				} else {
					builder.addTransferDuration_h(0.5 * vehicleAttrs.transferTime_h.get(signature.commodity));
					builder.addTransferDuration_h(this.performanceMeasures.getTotalDepartureDelay_h(firstNodeId));
					builder.addTransferCost(0.5 * vehicleAttrs.transferCost_1_ton.get(signature.commodity)
							* Math.max(minTransferredAmount_ton, payload_ton));
				}

				if (signature.unloadAtEnd && (links == lastLinks)) {
					builder.addUnloadingDuration_h(this.performanceMeasures.getTotalArrivalDelay_h(lastNodeId));
					builder.addUnloadingDuration_h(vehicleAttrs.loadTime_h.get(signature.commodity));
					builder.addUnloadingCost(vehicleAttrs.loadCost_1_ton.get(signature.commodity)
							* Math.max(minTransferredAmount_ton, payload_ton));
				} else {
					builder.addTransferDuration_h(this.performanceMeasures.getTotalArrivalDelay_h(lastNodeId));
					builder.addTransferDuration_h(0.5 * vehicleAttrs.transferTime_h.get(signature.commodity));
					builder.addTransferCost(0.5 * vehicleAttrs.transferCost_1_ton.get(signature.commodity)
							* Math.max(minTransferredAmount_ton, payload_ton));
				}

				for (Link link : links) {
					double length_km = Units.KM_H_PER_M_S * link.getLength();
					double tt_h = Units.H_PER_S * vehicleAttrs.travelTimeOnLink_s(link);
					builder.addMoveDuration_h(tt_h);
					if (LinkAttributes.isFerry(link)) {
						builder.addMoveCost(tt_h * vehicleAttrs.onFerryCost_1_h);
						builder.addMoveCost(length_km * vehicleAttrs.onFerryCost_1_km);
					} else {
						builder.addMoveCost(tt_h * vehicleAttrs.cost_1_h);
						builder.addMoveCost(length_km * vehicleAttrs.cost_1_km);
					}
				}
			} else {
				Log.warn("Skipping cost calculation for route with zero links. Consolidation episode signature: "
						+ signature);
			}
		}
	}

	// ---------- MICRO-FUNCTIONALITY BELOW

	public BasicTransportCost computeInVehicleShipmentCost(Vehicle vehicle, double maxAddedAmount_ton,
			ShipmentVehicleAssignment assignment) throws InsufficientDataException {

		final double vehicleCapacity_ton = FreightVehicleAttributes.getCapacity_ton(vehicle);
		final double availableCapacity_ton = vehicleCapacity_ton - assignment.getPayload_ton(vehicle);
		assert (availableCapacity_ton >= 0);
		final double assignedWeight_ton = Math.min(maxAddedAmount_ton, availableCapacity_ton);
		final boolean feasible = assignedWeight_ton >= 0.01 * Math.max(maxAddedAmount_ton, vehicleCapacity_ton); // TODO

		if (feasible) {
			throw new UnsupportedOperationException("TODO");
//			final DetailedTransportCost vehicleCost = computeEpisodeCost(
//					FreightVehicleAttributes.getFreightAttributes(vehicle),
//					assignment.getPayload_ton(vehicle) + assignedWeight_ton, assignment.getTransportEpisode());
//			final double share = assignedWeight_ton / (assignedWeight_ton + assignment.getPayload_ton(vehicle));
//			return new BasicTransportCost(assignedWeight_ton, share * vehicleCost.monetaryCost, vehicleCost.duration_h);
		} else {
			return null;
		}
	}

}
