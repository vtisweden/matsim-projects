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

	private Double computeNormalized(Commodity commodity, TransportMode mode,
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final double den = this.commodity2mode2weightSum.get(commodity).getOrDefault(mode, 0.0);
		if (den < this.weightThreshold) {
			return null;
		} else {
			return commodity2mode2weightedSum.get(commodity).getOrDefault(mode, 0.0) / den;
		}
	}

	private Double computeNormalized(Commodity commodity,
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final double den = this.commodity2mode2weightSum.get(commodity).values().stream().mapToDouble(s -> s).sum();
		if (den < this.weightThreshold) {
			return null;
		} else {
			return commodity2mode2weightedSum.get(commodity).values().stream().mapToDouble(s -> s).sum() / den;
		}
	}

	private Double computeNormalized(TransportMode mode,
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2weightedSum) {
		final double den = this.commodity2mode2weightSum.values().stream()
				.mapToDouble(m2s -> m2s.getOrDefault(mode, 0.0)).sum();
		if (den < this.weightThreshold) {
			return null;
		} else {
			return commodity2mode2weightedSum.values().stream().mapToDouble(m2e -> m2e.getOrDefault(mode, 0.0)).sum()
					/ den;
		}
	}

	// -------------------- CONTENT ACCESS --------------------

	public Double computeEfficiency(Commodity commodity, TransportMode mode) {
		return this.computeNormalized(commodity, mode, this.commodity2mode2weightedEfficiencySum);
	}

	public Double computeUnitCost_1_tonKm(Commodity commodity, TransportMode mode) {
		return this.computeNormalized(commodity, mode, this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Double computeEfficiency(Commodity commodity) {
		return this.computeNormalized(commodity, this.commodity2mode2weightedEfficiencySum);
	}

	public Double computeUnitCost_1_tonKm(Commodity commodity) {
		return this.computeNormalized(commodity, this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

	public Double computeEfficiency(TransportMode mode) {
		return this.computeNormalized(mode, this.commodity2mode2weightedEfficiencySum);
	}

	public Double computeUnitCost_1_tonKm(TransportMode mode) {
		return this.computeNormalized(mode, this.commodity2mode2weightedUnitCostSum_1_tonKm);
	}

}
