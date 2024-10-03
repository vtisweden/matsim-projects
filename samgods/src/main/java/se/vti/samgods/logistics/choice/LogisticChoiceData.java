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

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
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
		try {
			final VehicleType vehicleType = this.fleetData.getRepresentativeVehicleType(episode.getCommodity(),
					episode.getMode(), episode.isContainer(),
					episode.getConsolidationUnits().stream().anyMatch(cu -> cu.containsFerry));
			final SamgodsVehicleAttributes vehicleAttributes = this.fleetData.getVehicleType2attributes()
					.get(vehicleType);
			final double realizedPayload_ton = episode.getConsolidationUnits().stream()
					.mapToDouble(cu -> this.logisticChoiceDataProvider.getRealizedCost(cu).amount_ton).average()
					.getAsDouble();
//			final double inflatedPayload_ton = Math.max(this.minPayload_ton, realizedPayload_ton * this
//					.computeFreightInflationFactor(episode.getCommodity(), episode.getMode(), realizedPayload_ton));
			final double inflatedPayload_ton = realizedPayload_ton;
//					+ this.computeFreightOffset_ton(episode.getCommodity(), episode.getMode(), realizedPayload_ton);

			final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().setToAllZeros()
					.addAmount_ton(inflatedPayload_ton);
			builder.addLoadingDuration_h(vehicleAttributes.loadTime_h.get(episode.getCommodity()));
			builder.addLoadingCost(vehicleAttributes.loadCost_1_ton.get(episode.getCommodity()) * inflatedPayload_ton);

			final int transferCnt = episode.getConsolidationUnits().size() - 1;
			if (transferCnt > 0) {
				builder.addTransferDuration_h(
						transferCnt * vehicleAttributes.transferTime_h.get(episode.getCommodity()));
				builder.addTransferCost(transferCnt * vehicleAttributes.transferCost_1_ton.get(episode.getCommodity())
						* inflatedPayload_ton);
			}

			builder.addUnloadingDuration_h(vehicleAttributes.loadTime_h.get(episode.getCommodity()));
			builder.addUnloadingCost(
					vehicleAttributes.loadCost_1_ton.get(episode.getCommodity()) * inflatedPayload_ton);

			final List<ConsolidationUnit> consolidationUnits = episode.getConsolidationUnits();
			for (ConsolidationUnit consolidationUnit : consolidationUnits) {
				builder.add(this.logisticChoiceDataProvider.getRealizedCost(consolidationUnit), true);
			}
			return builder.build().createUnitCost_1_ton();
//					.createWithScaledMonetaryCost(this.fleetData.getVehicleType2costFactor().get(vehicleType));

		} catch (InsufficientDataException e) {
			InsufficientDataException.log(e,
					new InsufficientDataException(this.getClass(), "cannot create episode unit cost"));
			return null;
		}
	}

	public DetailedTransportCost getEpisodeUnitCost_1_ton(TransportEpisode episode) {
		return this.logisticChoiceDataProvider.getEpisode2unitCost_1_ton().computeIfAbsent(episode,
				e -> this.createEpisodeUnitCost_1_ton(e));
	}

//	public DetailedTransportCost getRealizedCost(ConsolidationUnit consolidationUnit) {
//		return this.logisticChoiceDataProvider.getRealizedCost(consolidationUnit);
//	}
//
//	public DetailedTransportCost getRealizedDomesticCost(ConsolidationUnit consolidationUnit) {
//		return this.logisticChoiceDataProvider.getRealizedDomesticCost(consolidationUnit);
//	}
	
	// -------------------- TRANSPORT CHAIN UNIT COSTS --------------------

	public DetailedTransportCost computeChain2transportUnitCost_1_ton(TransportChain transportChain) {
		try {
			final DetailedTransportCost.Builder chainCostBuilder = new DetailedTransportCost.Builder()
					.addAmount_ton(1.0);
			for (TransportEpisode episode : transportChain.getEpisodes()) {
				chainCostBuilder.add(this.getEpisodeUnitCost_1_ton(episode), false);
			}
			return chainCostBuilder.build();
		} catch (InsufficientDataException e) {
			InsufficientDataException.log(e, new InsufficientDataException(this.getClass(),
					"No transport cost data for at least one episode in this transport chain."));
			return null;
		}
	}

	// --------------- FREIGHT INFLATION FACTORS, FOR CALIBRATION ---------------

//	private double minFreightFactor = 1e-2;
//	private double maxFreightFactor = 1e+2;
//
//	public double computeFreightInflationFactor(Commodity commodity, TransportMode mode, double demand_ton) {
//		if (!this.logisticChoiceDataProvider.getCommodity2mode2avgTotalDemand_ton().containsKey(commodity)
//				|| !this.logisticChoiceDataProvider.getCommodity2mode2freightFactor().containsKey(commodity)) {
//			return 1.0;
//		}
//		final Double avgDemand_ton = this.logisticChoiceDataProvider.getCommodity2mode2avgTotalDemand_ton()
//				.get(commodity).get(mode);
//		final Double freightFactor = this.logisticChoiceDataProvider.getCommodity2mode2freightFactor().get(commodity)
//				.get(mode);
//		if (avgDemand_ton == null || freightFactor == null) {
//			return 1.0;
//		}
//		return Math.max(this.minFreightFactor,
//				Math.min(this.maxFreightFactor, freightFactor * Math.sqrt(avgDemand_ton / demand_ton)));
//	}

//	public double computeFreightOffset_ton(Commodity commodity, TransportMode mode, double demand_ton) {
//		if (!this.logisticChoiceDataProvider.getCommodity2mode2freightFactor().containsKey(commodity)) {
//			return 1.0;
//		}
//		final Double freightFactor = this.logisticChoiceDataProvider.getCommodity2mode2freightFactor().get(commodity)
//				.get(mode);
//		if (freightFactor == null) {
//			return 1.0;
//		}
//		final double a_ton = Math.max(freightFactor, 1e-8); // TODO Not really a factor
//		return a_ton * Math.exp(-demand_ton / a_ton);
//	}

}
