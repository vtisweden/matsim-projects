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

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.TransportCost;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.consolidation.road.ConsolidationUtils;
import se.vti.samgods.transportation.consolidation.road.ShipmentVehicleAssignment;
import se.vti.samgods.utils.ParseNumberUtils;

/**
 * TODO This is now about all commodities, which are defining members of
 * transport episodes. Consider splitting this up per commodity.
 * 
 * TODO This is for now specified in ignorance of the rail consolidation
 * formulation.
 * 
 * @author GunnarF
 *
 */
public class EmpiricalEpisodeCostModel implements EpisodeCostModel {

	private final ConsolidationCostModel consolidationCostModel;

	private final Map<TransportEpisode, Double> episode2monetaryCost = new LinkedHashMap<>();
	private final Map<TransportEpisode, Double> episode2durationTimesTons_hTon = new LinkedHashMap<>();
	private final Map<TransportEpisode, Double> episode2tons = new LinkedHashMap<>();

	public EmpiricalEpisodeCostModel(ConsolidationCostModel consolidationCostModel) {
		this.consolidationCostModel = consolidationCostModel;
	}

	public void add(ShipmentVehicleAssignment assignment) {
		final TransportEpisode episode = assignment.getTransportEpisode();

		double monetaryCost = 0.0;
		double durationTimesTons_hTon = 0.0;
		double tons = 0.0;

		for (Map.Entry<Vehicle, Double> entry : assignment.getVehicle2payload_ton().entrySet()) {
			final Vehicle vehicle = entry.getKey();
			final double payload_ton = entry.getValue();
			final TransportCost vehicleCost = this.consolidationCostModel
					.getVehicleCost(ConsolidationUtils.getFreightAttributes(vehicle), payload_ton, episode);
			monetaryCost += vehicleCost.monetaryCost;
			durationTimesTons_hTon += vehicleCost.duration_h * vehicleCost.amount_ton;
			tons += vehicleCost.amount_ton;
		}

		this.episode2monetaryCost.put(episode, monetaryCost + this.episode2monetaryCost.getOrDefault(episode, 0.0));
		this.episode2durationTimesTons_hTon.put(episode,
				durationTimesTons_hTon + this.episode2durationTimesTons_hTon.getOrDefault(episode, 0.0));
		this.episode2tons.put(episode, tons + this.episode2tons.getOrDefault(episode, 0.0));
	}

	@Override
	public TransportCost computeCost_1_ton(TransportEpisode episode) {
		final double monetaryCost_1_ton = ParseNumberUtils.divideOrNull(this.episode2monetaryCost.get(episode),
				this.episode2tons.get(episode));
		final double duration_h = ParseNumberUtils.divideOrNull(this.episode2durationTimesTons_hTon.get(episode),
				this.episode2tons.get(episode));
		return new TransportCost(1.0, monetaryCost_1_ton, duration_h);
	}
}
