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

	// -------------------- CONSTRUCTION --------------------

	public TransportationStatistics(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			FleetData fleetData, double payloadThreshold_ton, double weightThreshold) {
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
				}
			}
		}
	}

	public TransportationStatistics(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			FleetData fleetData) {
		this(consolidationUnit2fleetAssignment, fleetData, 1e-3, 1e-6);
	}

	// -------------------- INTERNALS --------------------

	private Map<TransportMode, Double> computeMode2value(Commodity commodity,
			Map<TransportMode, Double> mode2weightedSum) {
		final Map<TransportMode, Double> mode2value = new LinkedHashMap<>();
		for (Map.Entry<TransportMode, Double> mode2weightSumEntry : this.commodity2mode2weightSum.get(commodity)
				.entrySet()) {
			final double weightSum = mode2weightSumEntry.getValue();
			if (weightSum >= this.weightThreshold) {
				final TransportMode mode = mode2weightSumEntry.getKey();
				mode2value.put(mode, mode2weightedSum.getOrDefault(mode, 0.0) / weightSum);
			}
		}
		return mode2value;
	}

	private Map<Commodity, Map<TransportMode, Double>> computeCommodity2mode2value(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final Map<Commodity, Map<TransportMode, Double>> commodity2mode2value = new LinkedHashMap<>();
		for (Commodity commodity : commodity2mode2weightedSum.keySet()) {
			commodity2mode2value.put(commodity, this.computeMode2value(commodity,
					commodity2mode2weightedSum.getOrDefault(commodity, Collections.emptyMap())));
		}
		return commodity2mode2value;
	}

	private Map<Commodity, Double> computeCommodity2value(TransportMode mode,
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final Map<Commodity, Double> commodity2value = new LinkedHashMap<>();
		for (Commodity commodity : commodity2mode2weightedSum.keySet()) {
			final double weightSum = this.commodity2mode2weightSum.get(commodity).values().stream().mapToDouble(w -> w)
					.sum();
			if (weightSum >= this.weightThreshold) {
				commodity2value.put(commodity,
						commodity2mode2weightedSum.get(commodity).values().stream().mapToDouble(w -> w).sum()
								/ weightSum);
			}
		}
		return commodity2value;
	}

	private <T> Map<T, Double> normalizedOrNull(Map<T, Double> t2share) {
		final double sum = t2share.values().stream().mapToDouble(w -> w).sum();
		if (sum < this.weightThreshold) {
			return null;
		} else {
			t2share.entrySet().stream().forEach(e -> e.setValue(e.getValue() / sum));
			return t2share;
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	public Map<Commodity, Map<TransportMode, Double>> computeCommodity2mode2efficiency() {
		return this.computeCommodity2mode2value(this.commodity2mode2weightedEfficiencySum);
	}

	public Map<Commodity, Map<TransportMode, Double>> computeCommodity2mode2unitCost_1_tonKm() {
		return this.computeCommodity2mode2value(this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Map<Commodity, Double> computeCommodity2efficiency(TransportMode mode) {
		return this.computeCommodity2value(mode, this.commodity2mode2weightedEfficiencySum);
	}

	public Map<Commodity, Double> computeCommodity2unitCost_1_tonKm(TransportMode mode) {
		return this.computeCommodity2value(mode, this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Map<TransportMode, Double> computeMode2efficiency(Commodity commodity) {
		return this.computeMode2value(commodity,
				this.commodity2mode2weightedEfficiencySum.getOrDefault(commodity, Collections.emptyMap()));
	}

	public Map<TransportMode, Double> computeMode2unitCost_1_tonKm(Commodity commodity) {
		return this.computeMode2value(commodity,
				this.commodity2mode2weightedUnitCostSum_1_tonKm.getOrDefault(commodity, Collections.emptyMap()));
	}

	public Map<TransportMode, Double> computeMode2share(Commodity commodity) {
		final Map<TransportMode, Double> mode2share = new LinkedHashMap<>();
		this.commodity2mode2weightSum.getOrDefault(commodity, Collections.emptyMap()).entrySet().stream()
				.forEach(e -> mode2share.compute(e.getKey(), (m, s) -> s == null ? e.getValue() : s + e.getValue()));
		return this.normalizedOrNull(mode2share);
	}

	public Map<TransportMode, Double> computeMode2share() {
		final Map<TransportMode, Double> mode2share = new LinkedHashMap<>();
		this.commodity2mode2weightSum.values().stream().flatMap(m2w -> m2w.entrySet().stream())
				.forEach(e -> mode2share.compute(e.getKey(), (m, s) -> s == null ? e.getValue() : s + e.getValue()));
		return this.normalizedOrNull(mode2share);
	}
}
