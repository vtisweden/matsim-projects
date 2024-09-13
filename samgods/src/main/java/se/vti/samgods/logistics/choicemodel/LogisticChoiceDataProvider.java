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
package se.vti.samgods.logistics.choicemodel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.RealizedConsolidationCostModel;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class LogisticChoiceDataProvider {

	// -------------------- CONSTANTS --------------------

	private final RealizedConsolidationCostModel consolidationCostModel;
	private final ConcurrentMap<TransportMode, Double> mode2efficiency;
	private final ConcurrentMap<ConsolidationUnit, Double> consolidationUnit2efficiency;
	private final NetworkData networkData;
	private final FleetData fleetData;

	// -------------------- CONSTRUCTION --------------------

	public LogisticChoiceDataProvider(Map<TransportMode, Double> mode2efficiency,
			Map<ConsolidationUnit, Double> consolidationUnit2efficiency, NetworkData networkData, FleetData fleetData) {
		this.consolidationCostModel = new RealizedConsolidationCostModel();
		this.mode2efficiency = new ConcurrentHashMap<>(mode2efficiency);
		this.consolidationUnit2efficiency = new ConcurrentHashMap<>(consolidationUnit2efficiency);
		this.networkData = networkData;
		this.fleetData = fleetData;
	}
	
	public LogisticChoiceData createLogisticChoiceData() {
		return new LogisticChoiceData(this);
	}

	// ---------- THREAD SAFE, INTERNALLY CHACHED EPISODE UNIT COSTS ----------

	private final ConcurrentMap<TransportEpisode, DetailedTransportCost> episodeToUnitCost_1_ton = new ConcurrentHashMap<>();

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
			final List<ConsolidationUnit> signatures = episode.getConsolidationUnits();
			for (ConsolidationUnit signature : signatures) {
				final double efficiency = this.consolidationUnit2efficiency.getOrDefault(signature,
						this.mode2efficiency.get(signature.mode));
				final DetailedTransportCost signatureUnitCost_1_ton = this.consolidationCostModel
						.computeRealizedSignatureCost(vehicleAttributes, efficiency * vehicleAttributes.capacity_ton,
								signature, signatures.get(0) == signature,
								signatures.get(signatures.size() - 1) == signature,
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
		return this.episodeToUnitCost_1_ton.computeIfAbsent(episode, e -> this.createEpisodeUnitCost_1_ton(episode));
	}
}
