/**
 * se.vti.samgods.calibration
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
package se.vti.samgods.calibration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.choice.ChainAndShipmentSize;
import se.vti.samgods.logistics.choice.LogisticChoiceData;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;
import se.vti.samgods.transportation.fleet.FleetData;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportationStatistics {

	// -------------------- CONSTANTS --------------------

	private final double weightThreshold;

	// -------------------- MEMBERS --------------------

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2domesticTransportWork_tonKm = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightSum = new LinkedHashMap<>();
	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedEfficiencySum = new LinkedHashMap<>();
	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedUnitCostSum_1_tonKm = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TransportationStatistics(BlockingQueue<ChainAndShipmentSize> allChoices,
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			NetworkData networkData, FleetData fleetData, LogisticChoiceData logisticChoiceData,
			double payloadThreshold_ton, double weightThreshold) {

		for (ChainAndShipmentSize choice : allChoices) {
//			if (networkData.getDomesticNodeIds()
//					.contains(choice.transportChain.getEpisodes().getFirst().getLoadingNodeId())
//					&& networkData.getDomesticNodeIds()
//							.contains(choice.transportChain.getEpisodes().getLast().getUnloadingNodeId())) {
//				final Map<TransportMode, Double> mode2transportWork_tonKm = this.commodity2mode2domesticTransportWork_tonKm
//						.computeIfAbsent(choice.transportChain.getCommodity(), c -> new LinkedHashMap<>());
//				final double amount_ton = choice.annualShipment.getTotalAmount_ton();
//				for (TransportEpisode episode : choice.transportChain.getEpisodes()) {
//					final double work_tonKm = amount_ton * episode.getConsolidationUnits().stream()
//							.mapToDouble(cu -> logisticChoiceData.getRealizedCost(cu).length_km).sum();
//					mode2transportWork_tonKm.compute(episode.getMode(),
//							(m, w) -> w == null ? work_tonKm : w + work_tonKm);
//				}
//			}
		}

		this.weightThreshold = weightThreshold;
		for (Map.Entry<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> entry : consolidationUnit2fleetAssignment
				.entrySet()) {
			final FleetAssignment fleetAssignment = entry.getValue();
			if (fleetAssignment.payload_ton >= payloadThreshold_ton) {
				final double weight = fleetAssignment.expectedSnapshotVehicleCnt;
				if (weight >= weightThreshold) {
					final ConsolidationUnit consolidationUnit = entry.getKey();
					final double efficiency = fleetAssignment.payload_ton
							/ fleetData.getVehicleType2attributes().get(fleetAssignment.vehicleType).capacity_ton;
					this.commodity2mode2weightSum
							.computeIfAbsent(consolidationUnit.commodity, c -> new LinkedHashMap<>())
							.compute(consolidationUnit.samgodsMode, (m, s) -> s == null ? weight : s + weight);
					this.commodity2mode2weightedEfficiencySum
							.computeIfAbsent(consolidationUnit.commodity, c -> new LinkedHashMap<>())
							.compute(consolidationUnit.samgodsMode,
									(m, s) -> s == null ? efficiency * weight : s + efficiency * weight);
					this.commodity2mode2weightedUnitCostSum_1_tonKm
							.computeIfAbsent(consolidationUnit.commodity, c -> new LinkedHashMap<>())
							.compute(consolidationUnit.samgodsMode,
									(m, s) -> s == null ? weight * fleetAssignment.unitCost_1_tonKm
											: s + weight * fleetAssignment.unitCost_1_tonKm);
//					final double transportWork_tonKm = fleetAssignment.realDemand_ton * 0.5
//							* fleetAssignment.loopLength_km;
//					this.commodity2mode2transportWork_tonKm
//							.computeIfAbsent(consolidationUnit.commodity, c -> new LinkedHashMap<>())
//							.compute(consolidationUnit.samgodsMode, (m, s) -> s == null ? weight * transportWork_tonKm
//									: s + weight * transportWork_tonKm);
				}
			}
		}
	}

	public TransportationStatistics(BlockingQueue<ChainAndShipmentSize> allChoices,
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			NetworkData networkData, FleetData fleetData, LogisticChoiceData logisticChoiceData) {
		this(allChoices, consolidationUnit2fleetAssignment, networkData, fleetData, logisticChoiceData, 1e-3, 1e-6);
	}

	// -------------------- INTERNALS --------------------

	private Map<Commodity, Double> computeCommodity2sum(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2value) {
		return commodity2mode2value.entrySet().stream().collect(
				Collectors.toMap(e -> e.getKey(), e -> e.getValue().values().stream().mapToDouble(s -> s).sum()));
	}

	private Map<TransportMode, Double> computeMode2sum(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2value) {
		final Map<TransportMode, Double> mode2sum = new LinkedHashMap<>();
		commodity2mode2value.values().stream().flatMap(m2s -> m2s.entrySet().stream())
				.forEach(e -> mode2sum.compute(e.getKey(), (m, s) -> s == null ? e.getValue() : s + e.getValue()));
		return mode2sum;
	}

	private Map<Commodity, Double> computeCommodity2weightedMean(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final Map<Commodity, Double> commodity2weightedSum = this.computeCommodity2sum(commodity2mode2weightedSum);
		final Map<Commodity, Double> commodity2weightSum = this.computeCommodity2sum(this.commodity2mode2weightSum);
		return commodity2weightSum.entrySet().stream().filter(e -> e.getValue() >= this.weightThreshold)
				.collect(Collectors.toMap(e -> e.getKey(),
						e -> commodity2weightedSum.getOrDefault(e.getKey(), 0.0) / e.getValue()));
	}

	private Map<TransportMode, Double> computeMode2weightedMean(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final Map<TransportMode, Double> mode2weightedSum = this.computeMode2sum(commodity2mode2weightedSum);
		final Map<TransportMode, Double> mode2weightSum = this.computeMode2sum(this.commodity2mode2weightSum);
		return mode2weightSum.entrySet().stream().filter(e -> e.getValue() >= this.weightThreshold).collect(
				Collectors.toMap(e -> e.getKey(), e -> mode2weightedSum.getOrDefault(e.getKey(), 0.0) / e.getValue()));
	}

	private Map<TransportMode, Double> computeMode2weightedMean(Commodity commodity,
			Map<TransportMode, Double> mode2weightedSum) {
		final Map<TransportMode, Double> mode2weightedMean = new LinkedHashMap<>();
		for (Map.Entry<TransportMode, Double> mode2weightSumEntry : this.commodity2mode2weightSum
				.getOrDefault(commodity, Collections.emptyMap()).entrySet()) {
			final double weightSum = mode2weightSumEntry.getValue();
			if (weightSum >= this.weightThreshold) {
				final TransportMode mode = mode2weightSumEntry.getKey();
				mode2weightedMean.put(mode, mode2weightedSum.getOrDefault(mode, 0.0) / weightSum);
			}
		}
		return mode2weightedMean;
	}

	private Map<Commodity, Map<TransportMode, Double>> computeCommodity2mode2weightedMean(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedMean = new LinkedHashMap<>();
		for (Commodity commodity : commodity2mode2weightedSum.keySet()) {
			final Map<TransportMode, Double> mode2weightedMean = this.computeMode2weightedMean(commodity,
					commodity2mode2weightedSum.getOrDefault(commodity, Collections.emptyMap()));
			if (mode2weightedMean != null) {
				commodity2mode2weightedMean.put(commodity, mode2weightedMean);
			}
		}
		return commodity2mode2weightedMean;
	}

	private Map<Commodity, Double> computeCommodity2weightedMean(TransportMode mode,
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final Map<Commodity, Double> commodity2weightedMean = new LinkedHashMap<>();
		for (Map.Entry<Commodity, Map<TransportMode, Double>> entry : this.commodity2mode2weightSum.entrySet()) {
			final double weightSum = entry.getValue().values().stream().mapToDouble(w -> w).sum();
			if (weightSum >= this.weightThreshold) {
				final Commodity commodity = entry.getKey();
				final double weightedSum = commodity2mode2weightedSum.getOrDefault(commodity, Collections.emptyMap())
						.values().stream().mapToDouble(w -> w).sum();
				commodity2weightedMean.put(commodity, weightedSum / weightSum);
			}
		}
		return commodity2weightedMean;
	}

	private <T> Map<T, Double> normalized(Map<T, Double> t2val) {
		final double sum = t2val.values().stream().mapToDouble(v -> v).sum();
		if (sum < this.weightThreshold) {
			return Collections.emptyMap();
		} else {
			t2val.entrySet().stream().forEach(e -> e.setValue(e.getValue() / sum));
			return t2val;
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	public Map<Commodity, Map<TransportMode, Double>> computeCommodity2mode2efficiency() {
		return this.computeCommodity2mode2weightedMean(this.commodity2mode2weightedEfficiencySum);
	}

	public Map<Commodity, Map<TransportMode, Double>> computeCommodity2mode2unitCost_1_tonKm() {
		return this.computeCommodity2mode2weightedMean(this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Map<Commodity, Double> computeCommodity2efficiency(TransportMode mode) {
		return this.computeCommodity2weightedMean(mode, this.commodity2mode2weightedEfficiencySum);
	}

	public Map<Commodity, Double> computeCommodity2unitCost_1_tonKm(TransportMode mode) {
		return this.computeCommodity2weightedMean(mode, this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Map<TransportMode, Double> computeMode2efficiency(Commodity commodity) {
		return this.computeMode2weightedMean(commodity,
				this.commodity2mode2weightedEfficiencySum.getOrDefault(commodity, Collections.emptyMap()));
	}

	public Map<TransportMode, Double> computeMode2unitCost_1_tonKm(Commodity commodity) {
		return this.computeMode2weightedMean(commodity,
				this.commodity2mode2weightedUnitCostSum_1_tonKm.getOrDefault(commodity, Collections.emptyMap()));
	}

	public Map<Commodity, Double> computeCommodity2efficiency() {
		return this.computeCommodity2weightedMean(this.commodity2mode2weightedEfficiencySum);
	}

	public Map<Commodity, Double> computeCommodity2unitCost_1_tonKm() {
		return this.computeCommodity2weightedMean(this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Map<TransportMode, Double> computeMode2efficiency() {
		return this.computeMode2weightedMean(this.commodity2mode2weightedEfficiencySum);
	}

	public Map<TransportMode, Double> computeMode2unitCost_1_tonKm() {
		return this.computeMode2weightedMean(this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Map<TransportMode, Double> computeMode2share(Commodity commodity) {
		final Map<TransportMode, Double> mode2weightSum = new LinkedHashMap<>(
				this.commodity2mode2weightSum.getOrDefault(commodity, Collections.emptyMap()));
		return this.normalized(mode2weightSum);
	}

	public Map<TransportMode, Double> computeMode2share() {
		final Map<TransportMode, Double> mode2weightSum = new LinkedHashMap<>();
		this.commodity2mode2weightSum.values().stream().flatMap(m2w -> m2w.entrySet().stream()).forEach(
				e -> mode2weightSum.compute(e.getKey(), (m, s) -> s == null ? e.getValue() : s + e.getValue()));
		return this.normalized(mode2weightSum);
	}

	public Map<Commodity, Map<TransportMode, Double>> getCommodity2mode2domesticTransportWork_tonKm() {
		return commodity2mode2domesticTransportWork_tonKm;
	}

	public Map<Commodity, Double> computeCommodity2domesticTransportWork_tonKm() {
		return this.computeCommodity2sum(this.commodity2mode2domesticTransportWork_tonKm);
	}

	public Map<TransportMode, Double> computeMode2domesticTransportWork_tonKm() {
		return this.computeMode2sum(this.commodity2mode2domesticTransportWork_tonKm);
	}
}
