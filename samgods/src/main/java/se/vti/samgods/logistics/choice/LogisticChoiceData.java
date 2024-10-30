/**
 * se.vti.samgods.logistics.choicemodel
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
package se.vti.samgods.logistics.choice;

import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.costs.DetailedTransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public class LogisticChoiceData {

	// -------------------- CONSTANTS --------------------

	private final LogisticChoiceDataProvider logisticChoiceDataProvider;

	// -------------------- CONSTRUCTION --------------------

	LogisticChoiceData(LogisticChoiceDataProvider dataProvider) {
		this.logisticChoiceDataProvider = dataProvider;
	}

	// -------------------- TRANSPORT EPISODE UNIT COSTS --------------------

	private DetailedTransportCost createUnitCost_1_ton(TransportEpisode episode) {
		final DetailedTransportCost.Builder costBuilder = new DetailedTransportCost.Builder().setToAllZeros()
				.addAmount_ton(1.0);
		final ConsolidationUnit firstConsolidationUnit = episode.getConsolidationUnits().get(0);
		final ConsolidationUnit lastConsolidationUnit = episode.getConsolidationUnits()
				.get(episode.getConsolidationUnits().size() - 1);
		for (ConsolidationUnit consolidationUnit : episode.getConsolidationUnits()) {
			costBuilder.add(
					this.logisticChoiceDataProvider.getTransportUnitCost_1_ton(lastConsolidationUnit,
							firstConsolidationUnit == consolidationUnit, lastConsolidationUnit == consolidationUnit),
					false);
		}
		return costBuilder.build();
	}

	public DetailedTransportCost getEpisodeUnitCost_1_ton(TransportEpisode episode) {
		return this.logisticChoiceDataProvider.getEpisode2unitCost_1_ton().computeIfAbsent(episode,
				e -> this.createUnitCost_1_ton(e));
	}

	// -------------------- TRANSPORT CHAIN UNIT COSTS --------------------

	public DetailedTransportCost computeChain2transportUnitCost_1_ton(TransportChain transportChain) {
		final DetailedTransportCost.Builder chainCostBuilder = new DetailedTransportCost.Builder().addAmount_ton(1.0);
		for (TransportEpisode episode : transportChain.getEpisodes()) {
			chainCostBuilder.add(this.getEpisodeUnitCost_1_ton(episode), false);
		}
		return chainCostBuilder.build();
	}
}
