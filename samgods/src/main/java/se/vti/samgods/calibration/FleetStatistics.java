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
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetStatistics {

	// -------------------- CONSTANTS --------------------

	private final double workThreshold_tonKm;

	// -------------------- MEMBERS --------------------

	private final Map<VehicleType, Double> vehicleType2tonKm = new LinkedHashMap<>();

	private final Map<VehicleType, Double> vehicleType2costSum = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public FleetStatistics(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			double workThreshold_tonKm) {
		this.workThreshold_tonKm = workThreshold_tonKm;
		for (Map.Entry<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> entry : consolidationUnit2fleetAssignment
				.entrySet()) {
			final FleetAssignment fleetAssignment = entry.getValue();
			final double transportWork_tonKm = fleetAssignment.realDemand_ton * 0.5 * fleetAssignment.domesticLoopLength_km;
			if (transportWork_tonKm >= this.workThreshold_tonKm) {
				this.vehicleType2tonKm.compute(fleetAssignment.vehicleType,
						(vt, tk) -> tk == null ? transportWork_tonKm : tk + transportWork_tonKm);
				final double cost = fleetAssignment.unitCost_1_tonKm * transportWork_tonKm;
				this.vehicleType2costSum.compute(fleetAssignment.vehicleType,
						(vt, cs) -> cs == null ? cost : cs + cost);
			}
		}
	}

	public FleetStatistics(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment) {
		this(consolidationUnit2fleetAssignment, 1e-6);
	}

	// -------------------- IMPLEMENTATION --------------------

	public Map<VehicleType, Double> getVehicleType2domesticTonKm() {
		return this.vehicleType2tonKm;
	}

	public Map<VehicleType, Double> getVehicleType2domesticCostSum() {
		return this.vehicleType2costSum;
	}

	public Map<VehicleType, Double> computeVehicleType2domesticUnitCost_1_tonKm() {
		return this.vehicleType2tonKm.entrySet().stream().filter(e -> e.getValue() >= this.workThreshold_tonKm).collect(
				Collectors.toMap(e -> e.getKey(), e -> this.vehicleType2costSum.get(e.getKey()) / e.getValue()));
	}

}
