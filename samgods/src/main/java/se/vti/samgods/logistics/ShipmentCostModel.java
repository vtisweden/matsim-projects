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

import se.vti.samgods.TransportCost;
import se.vti.samgods.transportation.consolidation.EpisodeCostModel;

/**
 * 
 * @author GunnarF
 *
 */
public class ShipmentCostModel {

	private final EpisodeCostModel episodeCostModel;

	public ShipmentCostModel(EpisodeCostModel episodeCostModel) {
		this.episodeCostModel = episodeCostModel;
	}

	public TransportCost computeCost_1_ton(TransportChain chain) {
		double cost_1_ton = 0.0;
		double duration_h = 0.0;
		for (TransportEpisode episode : chain.getEpisodes()) {
			TransportCost cost = this.episodeCostModel.computeCost_1_ton(episode);
			cost_1_ton += cost.monetaryCost;
			duration_h += cost.duration_h;
		}
		return new TransportCost(1.0, cost_1_ton, duration_h);
	}
}
