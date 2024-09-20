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

/**
 * 
 * @author GunnarF
 *
 */
public class BackgroundTransportWork {

	// -------------------- MEMBERS --------------------

	private final Map<TransportMode, Double> mode2targetUnitCost_1_tonKm = new LinkedHashMap<>();

	private final Map<TransportMode, Double> mode2freightFactor = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Double>> commodity2mode2targetUnitCost_1_tonKm = new LinkedHashMap<>();

	private double msaExponent = 0.0;

	private int updateCounter = 0;

	// -------------------- CONSTRUCTION --------------------

	public BackgroundTransportWork() {
	}

	public BackgroundTransportWork setTargetUnitCost_1_tonKm(TransportMode mode, double targetUnitCost_1_tonKm) {
		this.mode2targetUnitCost_1_tonKm.put(mode, targetUnitCost_1_tonKm);
		return this;
	}

	public BackgroundTransportWork setTargetUnitCost_1_tonKm(Commodity commodity, TransportMode mode,
			double targetUnitCost_1_tonKm) {
		this.commodity2mode2targetUnitCost_1_tonKm.computeIfAbsent(commodity, c -> new LinkedHashMap<>()).put(mode,
				targetUnitCost_1_tonKm);
		return this;
	}

	public BackgroundTransportWork setMSAExponent(double msaExponent) {
		this.msaExponent = msaExponent;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateInternally(TransportationStatistics transpStats) {
		this.updateCounter++;
		final double innoWeight = 1.0 / Math.pow(this.updateCounter, this.msaExponent);

		/*-
		 * 			  totalCost     realizedUnitCost
		 * unitCost = ----------- = ----------------
		 *            fact * load   fact
		 *            
		 *            realizedUnitCost
		 * <=> fact = ----------------
		 *            unitCost
		 */
		final Map<TransportMode, Double> mode2empiricalUnitCost_1_tonKm = transpStats.computeMode2unitCost_1_tonKm();
		for (Map.Entry<TransportMode, Double> entry : this.mode2targetUnitCost_1_tonKm.entrySet()) {
			final TransportMode mode = entry.getKey();
			final double targetUnitCost_1_tonKm = entry.getValue();
			final Double realizedUnitCost_1_tonKm = mode2empiricalUnitCost_1_tonKm.get(mode);
			if (realizedUnitCost_1_tonKm != null) {
				final double freightFactor = realizedUnitCost_1_tonKm / targetUnitCost_1_tonKm;
				this.mode2freightFactor.compute(mode,
						(m, f) -> f == null ? freightFactor : innoWeight * freightFactor + (1.0 - innoWeight) * f);
			}
		}
	}
	
	public 	Map<TransportMode, Double> getMode2freightFactor() {
		return this.mode2freightFactor;
	}

	
}
