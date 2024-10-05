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

import java.util.List;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class LogisticChoiceData {

	// -------------------- CONSTANTS --------------------

	private final LogisticChoiceDataProvider logisticChoiceDataProvider;

	private final FleetData fleetData;

	// -------------------- CONSTRUCTION --------------------

	LogisticChoiceData(LogisticChoiceDataProvider dataProvider, FleetData fleetData) {
		this.logisticChoiceDataProvider = dataProvider;
		this.fleetData = fleetData;
	}

	// -------------------- TRANSPORT EPISODE UNIT COSTS --------------------

	private DetailedTransportCost createEpisodeUnitCost_1_ton(TransportEpisode episode) {
		final VehicleType vehicleType = this.fleetData.getRepresentativeVehicleType(episode.getCommodity(),
				episode.getMode(), episode.isContainer(),
				episode.getConsolidationUnits().stream().anyMatch(cu -> cu.containsFerry));
		final SamgodsVehicleAttributes vehicleAttributes = this.fleetData.getVehicleType2attributes().get(vehicleType);
		final double payload_ton = episode.getConsolidationUnits().stream()
				.mapToDouble(cu -> this.logisticChoiceDataProvider.getInVehicleTransportCost(cu).amount_ton).average()
				.getAsDouble();

		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().setToAllZeros()
				.addAmount_ton(payload_ton);
		builder.addLoadingDuration_h(vehicleAttributes.loadTime_h.get(episode.getCommodity()));
		builder.addLoadingCost(vehicleAttributes.loadCost_1_ton.get(episode.getCommodity()) * payload_ton);

		final int transferCnt = episode.getConsolidationUnits().size() - 1;
		if (transferCnt > 0) {
			builder.addTransferDuration_h(transferCnt * vehicleAttributes.transferTime_h.get(episode.getCommodity()));
			builder.addTransferCost(
					transferCnt * vehicleAttributes.transferCost_1_ton.get(episode.getCommodity()) * payload_ton);
		}

		builder.addUnloadingDuration_h(vehicleAttributes.loadTime_h.get(episode.getCommodity()));
		builder.addUnloadingCost(vehicleAttributes.loadCost_1_ton.get(episode.getCommodity()) * payload_ton);

		final List<ConsolidationUnit> consolidationUnits = episode.getConsolidationUnits();
		for (ConsolidationUnit consolidationUnit : consolidationUnits) {
			builder.add(this.logisticChoiceDataProvider.getInVehicleTransportCost(consolidationUnit), true);
		}
		return builder.build().createUnitCost_1_ton();
	}

	public DetailedTransportCost getEpisodeUnitCost_1_ton(TransportEpisode episode) {
		return this.logisticChoiceDataProvider.getEpisode2unitCost_1_ton().computeIfAbsent(episode,
				e -> this.createEpisodeUnitCost_1_ton(e));
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
