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

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.consolidation.ConsolidationCostModel;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class BasicEpisodeCostModel implements EpisodeCostModel {

	// -------------------- MEMBERS --------------------

	private final VehicleFleet fleet;
	private final ConsolidationCostModel consolidationCostModel;

	private final Map<TransportMode, Double> mode2efficiency;
	private final Map<Signature.ConsolidationUnit, Double> consolidationUnit2efficiency;

	private final NetworkData networkData;

	// -------------------- CONSTRUCTION --------------------

	public BasicEpisodeCostModel(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			Map<TransportMode, Double> mode2efficiency,
			Map<Signature.ConsolidationUnit, Double> consolidationUnit2efficiency, NetworkData networkData) {
		this.fleet = fleet;
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
	}

	public BasicEpisodeCostModel(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			double meanEfficiency, NetworkData networkData) {
		this(fleet, consolidationCostModel,
				Arrays.stream(TransportMode.values()).collect(Collectors.toMap(m -> m, m -> meanEfficiency)),
				new LinkedHashMap<>(), networkData);
	}

	// -------------------- IMPLEMENTATION OF EpisodeCostModel --------------------

	private double efficiency(Signature.ConsolidationUnit signature) {
		return this.consolidationUnit2efficiency.getOrDefault(signature, this.mode2efficiency.get(signature.mode));
	}

	private final Object lock = new Object();

	@Override
	public DetailedTransportCost computeUnitCost(TransportEpisode episode) throws InsufficientDataException {
		final FreightVehicleAttributes vehicleAttributes;
		synchronized (this.lock) {
			vehicleAttributes = this.fleet.getRepresentativeVehicleAttributes(episode);
		}
		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(1.0)
				.addLoadingDuration_h(0.0).addTransferDuration_h(0.0).addUnloadingDuration_h(0.0).addMoveDuration_h(0.0)
				.addLoadingCost(0.0).addTransferCost(0.0).addUnloadingCost(0.0).addMoveCost(0.0);
		final List<Signature.ConsolidationUnit> signatures = episode.getConsolidationUnits();
		for (Signature.ConsolidationUnit signature : signatures) {
			final DetailedTransportCost signatureCost = this.consolidationCostModel.computeSignatureCost(
					vehicleAttributes, this.efficiency(signature) * vehicleAttributes.capacity_ton, signature,
					signatures.get(0) == signature, signatures.get(signatures.size() - 1) == signature,
					this.networkData.getLinkId2representativeCost(episode.getCommodity(), episode.getMode(),
							episode.isContainer()),
					this.networkData.getFerryLinkIds()).computeUnitCost();
			builder.addLoadingDuration_h(signatureCost.loadingDuration_h)
					.addTransferDuration_h(signatureCost.transferDuration_h)
					.addUnloadingDuration_h(signatureCost.unloadingDuration_h)
					.addMoveDuration_h(signatureCost.moveDuration_h).addLoadingCost(signatureCost.loadingCost)
					.addTransferCost(signatureCost.transferCost).addUnloadingCost(signatureCost.unloadingCost)
					.addMoveCost(signatureCost.moveCost);
		}
		return builder.build();
	}

}
