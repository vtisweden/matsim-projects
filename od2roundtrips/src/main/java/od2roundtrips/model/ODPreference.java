/**
 * od2roundtrips.model
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
package od2roundtrips.model;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.math3.util.CombinatoricsUtils;

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class ODPreference extends Preferences.Component<RoundTrip<TAZ>, TAZ> {

	private boolean odIsNormalized = false;
	private final Map<Tuple<TAZ, TAZ>, Double> targetOdMatrix = new LinkedHashMap<>();

	public ODPreference() {
	}

	public void setODEntry(TAZ origin, TAZ destination, double value) {
		this.odIsNormalized = false;
		this.targetOdMatrix.put(new Tuple<>(origin, destination), value);
	}

	public Map<Tuple<TAZ, TAZ>, Double> getTargetOdMatrix() {
		return this.targetOdMatrix;
	}

	@Override
	public double logWeight(RoundTrip<TAZ> roundTrip) {

		if (!this.odIsNormalized) {
			final double odSum = this.targetOdMatrix.values().stream().mapToDouble(v -> v).sum();
			for (Map.Entry<Tuple<TAZ, TAZ>, Double> entry : this.targetOdMatrix.entrySet()) {
				entry.setValue(entry.getValue() / odSum);
			}
			this.odIsNormalized = true;
		}

		final Map<Tuple<TAZ, TAZ>, Integer> realizedOdMatrix = new LinkedHashMap<>();
		for (int i = 0; i < roundTrip.locationCnt(); i++) {
			final Tuple<TAZ, TAZ> od = new Tuple<>(roundTrip.getLocation(i), roundTrip.getSuccessorLocation(i));
			realizedOdMatrix.compute(od, (k, v) -> v == null ? 1 : v + 1);
		}

		double logWeight = 0.0;
		for (Map.Entry<Tuple<TAZ, TAZ>, Integer> realizedEntry : realizedOdMatrix.entrySet()) {
			final double lambda = Math.max(1e-8,
					this.targetOdMatrix.getOrDefault(realizedEntry.getKey(), 0.0) * roundTrip.locationCnt());
			final int k = realizedEntry.getValue();
			logWeight += k * Math.log(lambda) - lambda - CombinatoricsUtils.factorialLog(k);
		}
		return logWeight;
	}

}
