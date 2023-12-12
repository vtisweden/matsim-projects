/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation.pricing;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportPrices.TransshipmentPrices;

/**
 * 
 * @author GunnarF
 *
 */
public class ProportionalTransshipmentPrices implements TransshipmentPrices {

	// -------------------- CONSTANTS --------------------

	private final Commodity commodity;

	// -------------------- MEMBERS --------------------

	private final Map<TransportMode, Map<TransportMode, Double>> mode2mode2price_1_ton = new LinkedHashMap<>();
	private final Map<TransportMode, Map<TransportMode, Double>> mode2mode2duration_min = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ProportionalTransshipmentPrices(final Commodity commodity) {
		this.commodity = commodity;
	}

	// -------------------- SETTERS --------------------

	public void setPrice_1_ton(TransportMode fromMode, TransportMode toMode, double price_1_ton) {
		this.mode2mode2price_1_ton.computeIfAbsent(fromMode, m -> new LinkedHashMap<>()).put(toMode, price_1_ton);
	}

	public void setDuration_min(TransportMode fromMode, TransportMode toMode, double duration_min) {
		this.mode2mode2duration_min.computeIfAbsent(fromMode, m -> new LinkedHashMap<>()).put(toMode, duration_min);
	}

	// --------------- IMPLEMENTATION OF TransshipmentPrices ---------------

	@Override
	public Commodity getCommodity() {
		return this.commodity;
	}

	@Override
	public double getTransshipmentPrice_1_ton(Node node, TransportMode fromMode, TransportMode toMode) {
		return this.mode2mode2price_1_ton.computeIfAbsent(fromMode, m -> new LinkedHashMap<>()).get(toMode);
	}

	@Override
	public double getTransshipmentDuration_min(Node node, TransportMode fromMode, TransportMode toMode) {
		return this.mode2mode2duration_min.computeIfAbsent(fromMode, m -> new LinkedHashMap<>()).get(toMode);
	}

	@Override
	public ProportionalTransshipmentPrices deepCopy() {
		ProportionalTransshipmentPrices result = new ProportionalTransshipmentPrices(this.commodity);
		for (Map.Entry<TransportMode, Map<TransportMode, Double>> entry : this.mode2mode2price_1_ton.entrySet()) {
			result.mode2mode2price_1_ton.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
		}
		for (Map.Entry<TransportMode, Map<TransportMode, Double>> entry : this.mode2mode2duration_min.entrySet()) {
			result.mode2mode2duration_min.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
		}
		return result;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer(
				"Transshipment prices/durations for commodity " + this.commodity + "\n");
		for (Map.Entry<TransportMode, Map<TransportMode, Double>> e : this.mode2mode2price_1_ton.entrySet()) {
			final TransportMode fromMode = e.getKey();
			for (Map.Entry<TransportMode, Double> e2 : e.getValue().entrySet()) {
				final TransportMode toMode = e2.getKey();
				final double price_1_ton = e2.getValue();
				result.append("  price [per ton] " + fromMode + "->" + toMode + ": " + price_1_ton + "\n");
			}
		}
		for (Map.Entry<TransportMode, Map<TransportMode, Double>> e : this.mode2mode2duration_min.entrySet()) {
			final TransportMode fromMode = e.getKey();
			for (Map.Entry<TransportMode, Double> e2 : e.getValue().entrySet()) {
				final TransportMode toMode = e2.getKey();
				final double duration_min = e2.getValue();
				result.append("  duration [min] " + fromMode + "->" + toMode + ": " + duration_min + "\n");
			}
		}
		return result.toString();
	}
}
