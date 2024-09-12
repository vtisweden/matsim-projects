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
package se.vti.samgods.transportation.costs;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class PredictedEpisodeUnitCostModel {

	// -------------------- MEMBERS --------------------

	private final RealizedConsolidationCostModel consolidationCostModel;

	private final Map<TransportMode, Double> mode2efficiency;
	private final Map<ConsolidationUnit, Double> consolidationUnit2efficiency;

	private final NetworkData networkData;
	private final FleetData fleetData;

	// -------------------- CONSTRUCTION --------------------

	public PredictedEpisodeUnitCostModel(RealizedConsolidationCostModel consolidationCostModel,
			Map<TransportMode, Double> mode2efficiency, Map<ConsolidationUnit, Double> consolidationUnit2efficiency,
			NetworkData networkData, FleetData fleetData) {
		this.consolidationCostModel = consolidationCostModel;
		this.mode2efficiency = new LinkedHashMap<>(mode2efficiency);

		double fallbackCapacityUsage = mode2efficiency.values().stream().mapToDouble(e -> e).average().getAsDouble();
		for (TransportMode mode : TransportMode.values()) {
			if (!this.mode2efficiency.containsKey(mode)) {
				this.mode2efficiency.put(mode, fallbackCapacityUsage);
			}
		}
		this.consolidationUnit2efficiency = new LinkedHashMap<>(consolidationUnit2efficiency);

		this.networkData = networkData;
		this.fleetData = fleetData;
	}

	public PredictedEpisodeUnitCostModel(RealizedConsolidationCostModel consolidationCostModel, double meanEfficiency,
			NetworkData networkData, FleetData fleetData) {
		this(consolidationCostModel,
				Arrays.stream(TransportMode.values()).collect(Collectors.toMap(m -> m, m -> meanEfficiency)),
				new LinkedHashMap<>(), networkData, fleetData);
	}

	// -------------------- IMPLEMENTATION --------------------

	private double efficiency(ConsolidationUnit signature) {
		return this.consolidationUnit2efficiency.getOrDefault(signature, this.mode2efficiency.get(signature.mode));
	}

	public DetailedTransportCost computeUnitCost_1_ton(TransportEpisode episode) throws InsufficientDataException {

		final VehicleType vehicleType = this.fleetData.getRepresentativeVehicleType(episode.getCommodity(),
				episode.getMode(), episode.isContainer(),
				episode.getConsolidationUnits().stream().anyMatch(cu -> cu.containsFerry));
		final SamgodsVehicleAttributes vehicleAttributes = (SamgodsVehicleAttributes) vehicleType.getAttributes()
				.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(1.0)
				.addLoadingDuration_h(0.0).addTransferDuration_h(0.0).addUnloadingDuration_h(0.0).addMoveDuration_h(0.0)
				.addLoadingCost(0.0).addTransferCost(0.0).addUnloadingCost(0.0).addMoveCost(0.0).addDistance_km(0.0);
		final List<ConsolidationUnit> signatures = episode.getConsolidationUnits();
		for (ConsolidationUnit signature : signatures) {
			final DetailedTransportCost signatureUnitCost = this.consolidationCostModel
					.computeSignatureCost(vehicleAttributes,
							this.efficiency(signature) * vehicleAttributes.capacity_ton, signature,
							signatures.get(0) == signature, signatures.get(signatures.size() - 1) == signature,
							this.networkData.getLinkId2unitCost(vehicleType), this.networkData.getFerryLinkIds())
					.computeUnitCost_1_ton();
			builder.addLoadingDuration_h(signatureUnitCost.loadingDuration_h)
					.addTransferDuration_h(signatureUnitCost.transferDuration_h)
					.addUnloadingDuration_h(signatureUnitCost.unloadingDuration_h)
					.addMoveDuration_h(signatureUnitCost.moveDuration_h).addLoadingCost(signatureUnitCost.loadingCost)
					.addTransferCost(signatureUnitCost.transferCost).addUnloadingCost(signatureUnitCost.unloadingCost)
					.addMoveCost(signatureUnitCost.moveCost).addDistance_km(signatureUnitCost.length_km);
		}
		return builder.build();
	}

}
