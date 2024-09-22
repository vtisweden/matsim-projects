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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
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

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightSum = new LinkedHashMap<>();
	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedEfficiencySum = new LinkedHashMap<>();
	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedUnitCostSum_1_tonKm = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2avgTotalDemand_ton = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TransportationStatistics(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			FleetData fleetData, double payloadThreshold_ton, double weightThreshold) {

//		final Map<TransportMode, Regression> mode2regr = new LinkedHashMap<>();
		final Map<Commodity, Map<TransportMode, List<Double>>> commodity2mode2totalDemandList = new LinkedHashMap<>();

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

//					mode2regr.computeIfAbsent(consolidationUnit.samgodsMode, m -> new Regression(1.0, 1)).update(
//							new Vector(1.0 / fleetAssignment.totalDemand_ton), fleetAssignment.unitCost_1_tonKm);
					commodity2mode2totalDemandList
							.computeIfAbsent(consolidationUnit.commodity, c -> new LinkedHashMap<>())
							.computeIfAbsent(consolidationUnit.samgodsMode, m -> new ArrayList<>())
							.add(fleetAssignment.totalDemand_ton);
				}
			}
		}

		for (Map.Entry<Commodity, Map<TransportMode, List<Double>>> c2m2lEntry : commodity2mode2totalDemandList
				.entrySet()) {
			final Commodity commodity = c2m2lEntry.getKey();
			for (Map.Entry<TransportMode, List<Double>> m2lEntry : c2m2lEntry.getValue().entrySet()) {
				if (m2lEntry.getValue().size() > 0) {
					this.commodity2mode2avgTotalDemand_ton.computeIfAbsent(commodity, c -> new LinkedHashMap<>()).put(
							m2lEntry.getKey(),
							m2lEntry.getValue().stream().mapToDouble(v -> v.doubleValue()).average().getAsDouble());
				}
			}
		}

//		this.mode2oneOverTotalDemandSlope.clear();
//		for (Map.Entry<TransportMode, Regression> mode2regrEntry : mode2regr.entrySet()) {
//			final TransportMode mode = mode2regrEntry.getKey();
//			final Regression regr = mode2regrEntry.getValue();
//			this.mode2oneOverTotalDemandSlope.put(mode, regr.getCoefficients().get(0));
//		}

//		System.out.println("PRED\tREAL");
//		for (Map.Entry<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> entry : consolidationUnit2fleetAssignment
//				.entrySet()) {
//			final FleetAssignment fleetAssignment = entry.getValue();
//			if (fleetAssignment.payload_ton >= payloadThreshold_ton) {
//				final double weight = fleetAssignment.expectedSnapshotVehicleCnt;
//				if (weight >= weightThreshold) {
//					final ConsolidationUnit consolidationUnit = entry.getKey();
//					final double predUnitCost_1_ton = this.mode2oneOverTotalDemandSlope
//							.get(consolidationUnit.samgodsMode) / fleetAssignment.totalDemand_ton;
//					System.out.println(predUnitCost_1_ton + "\t" + fleetAssignment.unitCost_1_tonKm);
//				}
//			}
//		}
//		System.out.println("---");
	}

	public TransportationStatistics(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			FleetData fleetData) {
		this(consolidationUnit2fleetAssignment, fleetData, 1e-3, 1e-6);
	}

	// -------------------- INTERNALS --------------------

	private Map<TransportMode, Double> computeMode2weightedMean(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final Map<TransportMode, Double> mode2weightedSum = new LinkedHashMap<>();
		commodity2mode2weightedSum.values().stream().flatMap(m2s -> m2s.entrySet().stream()).forEach(
				e -> mode2weightedSum.compute(e.getKey(), (m, s) -> s == null ? e.getValue() : s + e.getValue()));
		final Map<TransportMode, Double> mode2weightSum = new LinkedHashMap<>();
		this.commodity2mode2weightSum.values().stream().flatMap(m2s -> m2s.entrySet().stream()).forEach(
				e -> mode2weightSum.compute(e.getKey(), (m, s) -> s == null ? e.getValue() : s + e.getValue()));
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

//	public Map<TransportMode, Double> getMode2oneOverTotalDemandSlope() {
//		return mode2oneOverTotalDemandSlope;
//	}

	public Map<Commodity, Map<TransportMode, Double>> getCommodity2mode2avgTotalDemand_ton() {
		return commodity2mode2avgTotalDemand_ton;
	}
}
