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

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class BackgroundTransportWork {

	// -------------------- CONSTANTS --------------------

	private final double defaultStepSize = 0.2;

	// -------------------- MEMBERS --------------------

	private double stepSize;

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2targetUnitCost_1_tonKm = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2freightFactor = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2errorSum_1_tonKm = new LinkedHashMap<>();

	private Map<Commodity, Map<TransportMode, Double>> commodity2mode2avgTotalDemand_ton = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public BackgroundTransportWork() {
		this.setStepSize(this.defaultStepSize);
	}

	public BackgroundTransportWork setStepSize(double stepSize) {
		this.stepSize = stepSize;
		return this;
	}

	public BackgroundTransportWork setTargetUnitCost_1_tonKm(Commodity commodity, TransportMode mode,
			double targetUnitCost_1_tonKm) {
		this.commodity2mode2targetUnitCost_1_tonKm.computeIfAbsent(commodity, c -> new LinkedHashMap<>()).put(mode,
				targetUnitCost_1_tonKm);
		return this;
	}

	public BackgroundTransportWork setTargetUnitCost_1_tonKm(TransportMode mode, double targetUnitCost_1_tonKm) {
		for (Commodity commodity : SamgodsConstants.Commodity.values()) {
			this.setTargetUnitCost_1_tonKm(commodity, mode, targetUnitCost_1_tonKm);
		}
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateInternally(TransportationStatistics transportationStatistics) {
		this.commodity2mode2avgTotalDemand_ton = transportationStatistics.getCommodity2mode2avgTotalDemand_ton();
		final Map<Commodity, Map<TransportMode, Double>> commodity2mode2realizedUnitCost_1_tonKm = transportationStatistics
				.computeCommodity2mode2unitCost_1_tonKm();
		for (Commodity commodity : commodity2mode2realizedUnitCost_1_tonKm.keySet()) {
			Map<TransportMode, Double> mode2realizedUnitCost_1_tonKm = commodity2mode2realizedUnitCost_1_tonKm
					.get(commodity);
			for (TransportMode mode : mode2realizedUnitCost_1_tonKm.keySet()) {
				final double realizedUnitCost_1_tonKm = mode2realizedUnitCost_1_tonKm.get(mode);
				final Double targetUnitCost_1_tonKm = this.commodity2mode2targetUnitCost_1_tonKm.get(commodity)
						.get(mode);
				if (targetUnitCost_1_tonKm != null) {
					final double error_1_tonKm = realizedUnitCost_1_tonKm - targetUnitCost_1_tonKm;
					this.commodity2mode2errorSum_1_tonKm.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
							.compute(mode, (m, s) -> s == null ? error_1_tonKm : s + error_1_tonKm);
					this.commodity2mode2freightFactor.computeIfAbsent(commodity, c -> new LinkedHashMap<>()).put(mode,
							this.stepSize * this.commodity2mode2errorSum_1_tonKm.get(commodity).get(mode));
				}
			}
		}
	}

	public Map<Commodity, Map<TransportMode, Double>> getCommodity2mode2freightFactor() {
		return this.commodity2mode2freightFactor;
	}

	public Map<Commodity, Map<TransportMode, Double>> getCommodity2mode2avgTotalDemand_ton() {
		return this.commodity2mode2avgTotalDemand_ton;
	}

}
