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

import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.consolidation.RealizedConsolidationCostModel;
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

	private final RealizedConsolidationCostModel consolidationCostModel = new RealizedConsolidationCostModel();

	private final LogisticChoiceDataProvider logisticChoiceDdataProvider;

	private final NetworkData networkData;

	private final FleetData fleetData;

	// -------------------- CONSTRUCTION --------------------

	LogisticChoiceData(LogisticChoiceDataProvider dataProvider, NetworkData networkData, FleetData fleetData) {
		this.logisticChoiceDdataProvider = dataProvider;
		this.networkData = networkData;
		this.fleetData = fleetData;
	}

	// -------------------- TRANSPORT EPISODE UNIT COSTS --------------------

	/**
	 * This is currently an *approximation* of the realized unit cost of a
	 * previously used episode, in that it evaluates the unit cost of a
	 * *representative vehicle* loaded according to the *average transport
	 * efficiency* on that episode.
	 * 
	 * A natural refinement is to replace this by an average over the realized unit
	 * costs per episode.
	 */
	private DetailedTransportCost createEpisodeUnitCost_1_ton(TransportEpisode episode) {
		try {
			final VehicleType vehicleType = this.fleetData.getRepresentativeVehicleType(episode.getCommodity(),
					episode.getMode(), episode.isContainer(),
					episode.getConsolidationUnits().stream().anyMatch(cu -> cu.containsFerry));
			final SamgodsVehicleAttributes vehicleAttributes = this.fleetData.getVehicleType2attributes()
					.get(vehicleType);
			final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(1.0)
					.addLoadingDuration_h(0.0).addTransferDuration_h(0.0).addUnloadingDuration_h(0.0)
					.addMoveDuration_h(0.0).addLoadingCost(0.0).addTransferCost(0.0).addUnloadingCost(0.0)
					.addMoveCost(0.0).addDistance_km(0.0);
			final List<ConsolidationUnit> consolidationUnits = episode.getConsolidationUnits();
			for (ConsolidationUnit consolidationUnit : consolidationUnits) {
				final DetailedTransportCost signatureUnitCost_1_ton = this.consolidationCostModel
						.computeRealizedSignatureCost(vehicleAttributes,
								this.logisticChoiceDdataProvider.getEfficiency(consolidationUnit)
										* vehicleAttributes.capacity_ton,
								consolidationUnit, consolidationUnits.get(0) == consolidationUnit,
								consolidationUnits.get(consolidationUnits.size() - 1) == consolidationUnit,
								this.networkData.getLinkId2unitCost(vehicleType), this.networkData.getFerryLinkIds())
						.createUnitCost_1_ton();
				builder.addLoadingDuration_h(signatureUnitCost_1_ton.loadingDuration_h)
						.addTransferDuration_h(signatureUnitCost_1_ton.transferDuration_h)
						.addUnloadingDuration_h(signatureUnitCost_1_ton.unloadingDuration_h)
						.addMoveDuration_h(signatureUnitCost_1_ton.moveDuration_h)
						.addLoadingCost(signatureUnitCost_1_ton.loadingCost)
						.addTransferCost(signatureUnitCost_1_ton.transferCost)
						.addUnloadingCost(signatureUnitCost_1_ton.unloadingCost)
						.addMoveCost(signatureUnitCost_1_ton.moveCost)
						.addDistance_km(signatureUnitCost_1_ton.length_km);
			}
			return builder.build();
		} catch (InsufficientDataException e) {
			e.log(this.getClass(), "cannot create episode unit cost", episode);
			return null;
		}
	}

	public DetailedTransportCost getEpisodeUnitCost_1_ton(TransportEpisode episode) {
		return this.logisticChoiceDdataProvider.getEpisode2unitCost_1_ton().computeIfAbsent(episode,
				e -> this.createEpisodeUnitCost_1_ton(e));
	}

	// -------------------- TRANSPORT CHAIN UNIT COSTS --------------------

	public DetailedTransportCost computeChain2transportUnitCost_1_ton(TransportChain transportChain) {
		try {
			final DetailedTransportCost.Builder chainCostBuilder = new DetailedTransportCost.Builder()
					.addAmount_ton(1.0);
			for (TransportEpisode episode : transportChain.getEpisodes()) {
				final DetailedTransportCost episodeUnitCost_1_ton = this.getEpisodeUnitCost_1_ton(episode);
				chainCostBuilder.addLoadingCost(episodeUnitCost_1_ton.loadingCost)
						.addLoadingDuration_h(episodeUnitCost_1_ton.loadingDuration_h)
						.addMoveCost(episodeUnitCost_1_ton.moveCost)
						.addMoveDuration_h(episodeUnitCost_1_ton.moveDuration_h)
						.addTransferCost(episodeUnitCost_1_ton.transferCost)
						.addTransferDuration_h(episodeUnitCost_1_ton.transferDuration_h)
						.addUnloadingCost(episodeUnitCost_1_ton.unloadingCost)
						.addUnloadingDuration_h(episodeUnitCost_1_ton.unloadingDuration_h)
						.addDistance_km(episodeUnitCost_1_ton.length_km);
			}
			return chainCostBuilder.build();
		} catch (InsufficientDataException e) {
			e.log(this.getClass(), "No transport cost data for at least one episode in this transport chain.",
					transportChain);
			return null;
		}
	}
}
