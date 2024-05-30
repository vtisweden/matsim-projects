/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

import se.vti.samgods.transportation.consolidation.EpisodeCostModel;

/**
 * 
 * @author GunnarF
 *
 */
public class EpisodeBasedShipmentCostModel implements ShipmentCostModel {

	private final EpisodeCostModel episodeCostModel;
	private final EpisodeCostModel fallbackEpisodeCostModel;

	public EpisodeBasedShipmentCostModel(EpisodeCostModel episodeCostModel, EpisodeCostModel fallbackEpisodeCostModel) {
		this.episodeCostModel = episodeCostModel;
		this.fallbackEpisodeCostModel = fallbackEpisodeCostModel;
	}

	private double computeEpisodeDuration_h(TransportEpisode episode) {
		Double duration_h = null;
		if (this.episodeCostModel != null) {
			duration_h = this.episodeCostModel.computeDuration_h(episode);
		}
		if (duration_h == null) {
			duration_h = this.fallbackEpisodeCostModel.computeDuration_h(episode);
		}
		return duration_h;
	}

	private double computeEpisodeCost_1_ton(TransportEpisode episode) {
		Double cost_1_ton = null;
		if (this.episodeCostModel != null) {
			cost_1_ton = this.episodeCostModel.computeMonetaryCost_1_ton(episode);
		}
		if (cost_1_ton == null) {
			cost_1_ton = this.fallbackEpisodeCostModel.computeMonetaryCost_1_ton(episode);
		}
		return cost_1_ton;
	}

	public double computeDuration_h(TransportChain chain) {
		double duration_h = 0.0;
		for (TransportEpisode episode : chain.getEpisodes()) {
			duration_h += this.computeEpisodeDuration_h(episode);
		}
		return duration_h;
	}

	public double computeCost_1_ton(TransportChain chain) {
		double cost_1_ton = 0.0;
		for (TransportEpisode episode : chain.getEpisodes()) {
			cost_1_ton += this.computeEpisodeCost_1_ton(episode);
		}
		return cost_1_ton;
	}
}
