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
package se.vti.samgods.transportation;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import floetteroed.utilities.Units;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.LinkAttributes;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class FallbackEpisodeCostModel implements EpisodeCostModel {

	// -------------------- MEMBERS --------------------

	private final VehicleFleet fleet;
	private final ConsolidationCostModel consolidationCostModel;

//	private double capacityUsageFactor = 0.7;
	private final Map<TransportMode, Double> mode2efficiency;
	private final Map<Signature.ConsolidationEpisode, Double> signature2efficiency;

	// -------------------- CONSTRUCTION --------------------

	public FallbackEpisodeCostModel(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			Map<TransportMode, Double> mode2capacityUsage,
			Map<Signature.ConsolidationEpisode, Double> episode2efficiency) {
		this.fleet = fleet;
		this.consolidationCostModel = consolidationCostModel;
		this.mode2efficiency = new LinkedHashMap<>(mode2capacityUsage);

		double fallbackCapacityUsage = mode2capacityUsage.values().stream().mapToDouble(e -> e).average().getAsDouble();
		for (TransportMode mode : TransportMode.values()) {
			if (!this.mode2efficiency.containsKey(mode)) {
				this.mode2efficiency.put(mode, fallbackCapacityUsage);
			}
		}
		this.signature2efficiency = new LinkedHashMap<>(episode2efficiency);
	}

//	public FallbackEpisodeCostModel setCapacityUsageFactor(double factor) {
//		this.capacityUsageFactor = factor;
//		return this;
//	}

	// -------------------- IMPLEMENTATION OF EpisodeCostModel --------------------

	private double efficiency(Signature.ConsolidationEpisode signature) {
		return this.signature2efficiency.getOrDefault(signature, this.mode2efficiency.get(signature.mode));
	}

	@Override
	public DetailedTransportCost computeUnitCost(TransportEpisode episode) throws InsufficientDataException {
		final FreightVehicleAttributes vehicleAttributes = this.fleet.getRepresentativeVehicleAttributes(episode);
		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(1.0);
		for (Signature.ConsolidationEpisode signature : episode.getSignatures()) {
			final DetailedTransportCost signatureCost = this.consolidationCostModel
					.computeSignatureCost(vehicleAttributes,
							this.efficiency(signature) * vehicleAttributes.capacity_ton, signature)
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

	public static class LinkCostSignature extends Signature.ListRepresented {
		private final Id<Link> linkId;
		private final Commodity commodity;
		private final SamgodsConstants.TransportMode mode;
		private final Boolean isContainer;

		public LinkCostSignature(Id<Link> linkId, Commodity commodity, TransportMode mode, Boolean isContainer) {
			this.linkId = linkId;
			this.commodity = commodity;
			this.mode = mode;
			this.isContainer = isContainer;
		}

		protected List<Object> asList() {
			return Arrays.asList(this.linkId, this.commodity, this.mode, this.isContainer);
		}
	}

	private Map<LinkCostSignature, Double> signature2unitCost_1_ton = new LinkedHashMap<>();
//	private Map<LinkCostSignature, Integer> signatureCnt = new LinkedHashMap<>();
//
//	public void updateLinkUnitCosts_1_ton(Map<LinkCostSignature, Double> newSignature2unitCost_1_ton) {
//		for (Map.Entry<LinkCostSignature, Double> e : newSignature2unitCost_1_ton.entrySet()) {
//			final double innoWeight = 1.0 / (1.0 + this.signatureCnt.getOrDefault(e.getKey(), 0));
//			this.signature2unitCost_1_ton.compute(e.getKey(),
//					(s, c) -> c == null ? e.getValue() : innoWeight * e.getValue() + (1.0 - innoWeight) * c);
//			this.signatureCnt.compute(e.getKey(), (s,c) -> c == null ? 1 : c + 1);
//		}
//	}

	@Override
	public void populateLink2transportCost(Map<Link, BasicTransportCost> link2cost,
			SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, Boolean isContainer,
			Network network) throws InsufficientDataException {

		final FreightVehicleAttributes vehicleAttributes = this.fleet.getRepresentativeVehicleAttributes(commodity,
				mode, isContainer, null);
		final double expectedLoad_1_ton  = this.mode2efficiency.get(mode) * vehicleAttributes.capacity_ton;

		FreightVehicleAttributes ferryCompatibleVehicleAttributes;
		try {
			ferryCompatibleVehicleAttributes = this.fleet.getRepresentativeVehicleAttributes(commodity, mode,
					isContainer, true);
		} catch (InsufficientDataException e) {
			ferryCompatibleVehicleAttributes = vehicleAttributes;
		}

		for (Link link : network.getLinks().values()) {
			if (!link2cost.containsKey(link)) {

				double duration_h;
				try {
					duration_h = Units.H_PER_S * vehicleAttributes.travelTimeOnLink_s(link);
					assert (Double.isFinite(duration_h) && duration_h > 0 && !Double.isNaN(duration_h));
				} catch (InsufficientDataException e) {
					throw new RuntimeException(e);
				}

				final LinkCostSignature signature = new LinkCostSignature(link.getId(), commodity, mode, isContainer);
				if (this.signature2unitCost_1_ton.containsKey(signature)) {

					assert (false); // took this out

//					assert(this.signature2unitCost_1_ton.get(signature) != null);
//					assert(this.signature2unitCost_1_ton.get(signature) > 0);
//					assert(Double.isFinite(this.signature2unitCost_1_ton.get(signature)));
//					System.out.println(signature + " -> " + signature2unitCost_1_ton.get(signature));
					link2cost.put(link,
							new BasicTransportCost(1.0, this.signature2unitCost_1_ton.get(signature), duration_h));
				} else {
					// TODO INCLUDE TRANSPORT EFFICIENCY HERE!!!
					final double length_km = Units.KM_PER_M * link.getLength();
					if (LinkAttributes.isFerry(link)) {
						link2cost.put(link,
								new BasicTransportCost(1.0,
										(duration_h * ferryCompatibleVehicleAttributes.onFerryCost_1_h
												+ length_km * ferryCompatibleVehicleAttributes.onFerryCost_1_km) / expectedLoad_1_ton,
										duration_h));
					} else {
						link2cost.put(link, new BasicTransportCost(1.0,
								(duration_h * vehicleAttributes.cost_1_h + length_km * vehicleAttributes.cost_1_km) / expectedLoad_1_ton,
								duration_h));
					}
				}
			}
		}
	}
}
