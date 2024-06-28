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
package se.vti.samgods.transportation.consolidation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.BasicTransportCost;
import se.vti.samgods.DetailedTransportCost;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.consolidation.road.ConsolidationUtils;
import se.vti.samgods.transportation.consolidation.road.ShipmentVehicleAssignment;
import se.vti.samgods.utils.TupleGrouping;

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

		void add(double monetaryCost, double durationTimesTons_hTon, double tons) {
			this.monetaryCostTimesTons_ton += monetaryCost;
			this.durationTimesTons_hTon += durationTimesTons_hTon;
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

		DetailedTransportCost createUnitCost() {
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

	// -------------------- --------------------

	private final ConsolidationCostModel consolidationCostModel;

	private final Map<TransportEpisode, CumulativeDetailedData> episode2data = new LinkedHashMap<>();

	// -------------------- --------------------

	public EmpiricalEpisodeCostModel(ConsolidationCostModel consolidationCostModel) {
		this.consolidationCostModel = consolidationCostModel;
	}

	// -------------------- --------------------

	public void add(ShipmentVehicleAssignment assignment) {
		final TransportEpisode episode = assignment.getTransportEpisode();

//		double monetaryCost = 0.0;
//		double durationTimesTons_hTon = 0.0;
//		double tons = 0.0;

		CumulativeDetailedData cumulativeCost = this.episode2data.computeIfAbsent(episode,
				e -> new CumulativeDetailedData());

		for (Map.Entry<Vehicle, Double> entry : assignment.getVehicle2payload_ton().entrySet()) {
			final Vehicle vehicle = entry.getKey();
			final double payload_ton = entry.getValue();
			final DetailedTransportCost vehicleCost = this.consolidationCostModel
					.getVehicleCost(ConsolidationUtils.getFreightAttributes(vehicle), payload_ton, episode);
			cumulativeCost.add(vehicleCost);
		}
	}

	@Override
	public DetailedTransportCost computeCost_1_ton(TransportEpisode episode) {
		CumulativeDetailedData data = this.episode2data.get(episode);
		if (data == null) {
			return null;
		} else {
			return data.createUnitCost();
		}
	}

	public Map<Link, BasicTransportCost> createLinkTransportCosts(
			TupleGrouping<SamgodsConstants.Commodity, SamgodsConstants.TransportMode>.Group commodityAndModeGroup) {

		final Map<Link, CumulativeBasicData> link2data = new LinkedHashMap<>();

		for (Map.Entry<TransportEpisode, CumulativeDetailedData> e2d : this.episode2data.entrySet()) {
			TransportEpisode episode = e2d.getKey();

			if (commodityAndModeGroup.contains(episode.getCommodity(), episode.getMode())) {
				for (TransportLeg leg : episode.getLegs()) {
					if (leg.getRouteView() != null) {
						CumulativeDetailedData episodeData = e2d.getValue();
						double routeLength_m = leg.getLength_m();
						for (Link link : leg.getRouteView()) {
							double weight = link.getLength() / routeLength_m;
							link2data.computeIfAbsent(link, l -> new CumulativeBasicData()).add(
									weight * episodeData.getMonetaryCostTimesTons_ton(),
									weight * episodeData.getDurationTimesTons_hTon(), episodeData.tons);
						}
					}
				}
			}
		}

		final Map<Link, BasicTransportCost> link2cost = new LinkedHashMap<>();
		for (Map.Entry<Link, CumulativeBasicData> l2d : link2data.entrySet()) {
			Link link = l2d.getKey();
			CumulativeBasicData data = l2d.getValue();
			link2cost.put(link, data.createUnitData());
		}
		return link2cost;
	}
}
