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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.TransportEpisode;
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

	// -------------------- CONSTRUCTION --------------------

	public BasicEpisodeCostModel(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			Map<TransportMode, Double> mode2efficiency,
			Map<Signature.ConsolidationUnit, Double> consolidationUnit2efficiency) {
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
	}

	public BasicEpisodeCostModel(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			double meanEfficiency) {
		this(fleet, consolidationCostModel,
				Arrays.stream(TransportMode.values()).collect(Collectors.toMap(m -> m, m -> meanEfficiency)),
				new LinkedHashMap<>());
	}

	// -------------------- IMPLEMENTATION OF EpisodeCostModel --------------------

	private double efficiency(Signature.ConsolidationUnit signature) {
		return this.consolidationUnit2efficiency.getOrDefault(signature, this.mode2efficiency.get(signature.mode));
	}

//	@Override
//	public void populateLink2transportCost(Map<Link, BasicTransportCost> link2cost,
//			SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, Boolean isContainer,
//			Network network, VehicleType vehicleType) throws InsufficientDataException {
//
//		throw new RuntimeException("TODO");
//		
//		final FreightVehicleAttributes vehicleAttributes = this.fleet.getRepresentativeVehicleAttributes(commodity,
//				mode, isContainer, null);
//
//		FreightVehicleAttributes ferryCompatibleVehicleAttributes;
//		try {
//			ferryCompatibleVehicleAttributes = this.fleet.getRepresentativeVehicleAttributes(commodity, mode,
//					isContainer, true);
//		} catch (InsufficientDataException e) {
//			ferryCompatibleVehicleAttributes = vehicleAttributes;
//		}
//
//		for (Link link : network.getLinks().values()) {
//			if (!link2cost.containsKey(link)) {
//				final double length_km = Units.KM_PER_M * link.getLength();
//				final double duration_h = Units.H_PER_S * vehicleAttributes.travelTimeOnLink_s(link);
//				assert (Double.isFinite(length_km));
//				assert (Double.isFinite(duration_h));
//				if (LinkAttributes.isFerry(link)) {
//					link2cost.put(link,
//							new BasicTransportCost(1.0,
//									duration_h * ferryCompatibleVehicleAttributes.onFerryCost_1_h
//											+ length_km * ferryCompatibleVehicleAttributes.onFerryCost_1_km,
//									duration_h));
//				} else {
//					link2cost.put(link,
//							new BasicTransportCost(1.0,
//									duration_h * vehicleAttributes.cost_1_h + length_km * vehicleAttributes.cost_1_km,
//									duration_h));
//				}
//			}
//		}
//	}

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
			final DetailedTransportCost signatureCost = this.consolidationCostModel
					.computeSignatureCost(vehicleAttributes,
							this.efficiency(signature) * vehicleAttributes.capacity_ton, signature,
							signatures.get(0) == signature, signatures.get(signatures.size() - 1) == signature)
					.computeUnitCost();
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
