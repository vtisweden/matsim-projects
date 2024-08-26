/**
 * se.vti.samgods.transportation.consolidation
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
package se.vti.samgods.deprecated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.Signature;
import se.vti.samgods.deprecated.logitprocessconsolidation.ShipmentVehicleAssignment;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.consolidation.ConsolidationCostModel;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.EpisodeCostModel;
import se.vti.samgods.transportation.costs.DetailedTransportCost.Builder;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

/**
 * TODO This is now about all commodities, which are defining members of
 * transport episodes. Consider splitting this up per commodity.
 * 
 * TODO This is for now specified in ignorance of the rail consolidation
 * formulation.
 * 
 * TODO This needs refined TransportCosts, with separate loading unloading etc
 * and separate time and distance costs and even ferry costs.
 * 
 * @author GunnarF
 *
 */
public class EmpiricalEpisodeCostModel implements EpisodeCostModel {

	// -------------------- INNER CLASS: CUMULATIVE BASIC DATA --------------------

	private class CumulativeBasicData {

		private double monetaryCostTimesTons_ton;
		private double durationTimesTons_hTon;
		private double tons;

		private CumulativeBasicData() {
			this(0, 0, 0);
		}

		private CumulativeBasicData(double monetaryCost, double durationTimesTons_hTon, double tons) {
			this.monetaryCostTimesTons_ton = monetaryCost;
			this.durationTimesTons_hTon = durationTimesTons_hTon;
			this.tons = tons;
		}

		void add(double monetaryCost, double duration_h, double tons) {
			this.monetaryCostTimesTons_ton += monetaryCost * tons;
			this.durationTimesTons_hTon += duration_h * tons;
			this.tons += tons;
		}

		BasicTransportCost createUnitData() {
			return new BasicTransportCost(1.0, this.monetaryCostTimesTons_ton / this.tons,
					this.durationTimesTons_hTon / this.tons);
		}
	}

	// --------------- INNER CLASS: CUMULATIVE DETAILED DATA ---------------

	private class CumulativeDetailedData {

		private double tons;

		private double loadingCostTimesTons_ton;
		private double unloadingCostTimesTons_ton;
		private double transferCostTimesTons_ton;
		private double moveCostTimesTons_ton;

		private double loadingDurationTimesTons_hTon;
		private double unloadingDurationTimesTons_hTon;
		private double transferDurationTimesTons_hTon;
		private double moveDurationTimesTons_hTon;

		void add(DetailedTransportCost cost) {
			this.tons += cost.amount_ton;
			this.loadingCostTimesTons_ton += cost.loadingCost * cost.amount_ton;
			this.unloadingCostTimesTons_ton += cost.unloadingCost * cost.amount_ton;
			this.transferCostTimesTons_ton += cost.transferCost * cost.amount_ton;
			this.moveCostTimesTons_ton += cost.moveCost * cost.amount_ton;
			this.loadingDurationTimesTons_hTon += cost.loadingDuration_h * cost.amount_ton;
			this.unloadingDurationTimesTons_hTon += cost.unloadingDuration_h * cost.amount_ton;
			this.transferDurationTimesTons_hTon += cost.transferDuration_h * cost.amount_ton;
			this.moveDurationTimesTons_hTon += cost.moveDuration_h * cost.amount_ton;
		}

		double getMonetaryCostTimesTons_ton() {
			return this.loadingCostTimesTons_ton + this.unloadingCostTimesTons_ton + this.transferCostTimesTons_ton
					+ this.moveCostTimesTons_ton;
		}

		double getDurationTimesTons_hTon() {
			return this.loadingDurationTimesTons_hTon + this.unloadingDurationTimesTons_hTon
					+ this.transferDurationTimesTons_hTon + this.moveDurationTimesTons_hTon;
		}

		DetailedTransportCost createUnitCost() throws InsufficientDataException {
			assert (this.tons > 1e-8);
			return new DetailedTransportCost.Builder().addAmount_ton(1.0)
					.addLoadingCost(this.loadingCostTimesTons_ton / this.tons)
					.addUnloadingCost(this.unloadingCostTimesTons_ton / this.tons)
					.addTransferCost(this.transferCostTimesTons_ton / this.tons)
					.addMoveCost(this.moveCostTimesTons_ton / this.tons)
					.addLoadingDuration_h(this.loadingDurationTimesTons_hTon / this.tons)
					.addUnloadingDuration_h(this.unloadingDurationTimesTons_hTon / this.tons)
					.addTransferDuration_h(this.transferDurationTimesTons_hTon / this.tons)
					.addMoveDuration_h(this.moveDurationTimesTons_hTon / this.tons).build();
		}
	}

	// -------------------- MEMBERS --------------------

	private final ConsolidationCostModel consolidationCostModel;

	private final Map<TransportEpisode, CumulativeDetailedData> episode2data = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public EmpiricalEpisodeCostModel(ConsolidationCostModel consolidationCostModel) {
		this.consolidationCostModel = consolidationCostModel;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(ShipmentVehicleAssignment assignment) throws InsufficientDataException {
		final TransportEpisode episode = assignment.getTransportEpisode();
		final CumulativeDetailedData cumulativeCost = this.episode2data.computeIfAbsent(episode,
				e -> new CumulativeDetailedData());

		for (Map.Entry<Vehicle, Double> entry : assignment.getVehicle2payload_ton().entrySet()) {
			final Vehicle vehicle = entry.getKey();
			final double payload_ton = entry.getValue();
			final DetailedTransportCost vehicleCost = this.consolidationCostModel
					.computeEpisodeCost(FreightVehicleAttributes.getFreightAttributes(vehicle), payload_ton, episode, null);
			cumulativeCost.add(vehicleCost);
		}
	}

	@Override
	public DetailedTransportCost computeUnitCost(TransportEpisode episode) throws InsufficientDataException {
		final CumulativeDetailedData data = this.episode2data.get(episode);
		if (data == null) {
			throw new InsufficientDataException(this.getClass(), "No empirical data for transport episode.", episode);
		} else {
			return data.createUnitCost();
		}
	}

	@Override
	public void populateLink2transportCost(Map<Link, BasicTransportCost> link2cost,
			SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, Boolean isContainer,
			Network network) throws InsufficientDataException {
		final Signature.Episode signature = new Signature.Episode(commodity, mode, isContainer, null, null);

		final Map<Link, CumulativeBasicData> link2data = new LinkedHashMap<>(network.getLinks().size());
		for (Map.Entry<TransportEpisode, CumulativeDetailedData> e2d : this.episode2data.entrySet()) {
			final TransportEpisode episode = e2d.getKey();
			if (signature.isCompatible(episode)) {
				final CumulativeDetailedData episodeData = e2d.getValue();
				for (TransportLeg leg : episode.getLegs()) {
					final List<Link> links = NetworkUtils.getLinks(network, leg.getRouteIdsView());
					final double routeLength_m = links.stream().mapToDouble(l -> l.getLength()).sum();
					if (routeLength_m > 1e-8) {
						for (Link link : links) {
							// Only complete cost data, do not override existing data:
							if (!link2cost.containsKey(link)) {
								// TODO Weight should rather be travel time dependent.
								final double weight = link.getLength() / Math.max(1e-8, routeLength_m);
								link2data.computeIfAbsent(link, l -> new CumulativeBasicData()).add(
										weight * episodeData.getMonetaryCostTimesTons_ton() / episodeData.tons,
										weight * episodeData.getDurationTimesTons_hTon() / episodeData.tons,
										episodeData.tons);
							}
						}
					}
				}
			}
		}

		for (Map.Entry<Link, CumulativeBasicData> l2d : link2data.entrySet()) {
			link2cost.put(l2d.getKey(), l2d.getValue().createUnitData());
		}
	}
}
