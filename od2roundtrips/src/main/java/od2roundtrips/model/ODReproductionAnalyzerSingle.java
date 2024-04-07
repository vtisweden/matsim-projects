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

import floetteroed.utilities.SetUtils;
import floetteroed.utilities.Tuple;
import se.vti.roundtrips.model.RoundTripAnalyzer;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class ODReproductionAnalyzerSingle extends RoundTripAnalyzer<RoundTrip<TAZ>, TAZ> {

	private final Map<Tuple<TAZ, TAZ>, Double> targetOdMatrix;

	private final Map<Tuple<TAZ, TAZ>, Double> sampleOdMatrix;

	public ODReproductionAnalyzerSingle(long burnInIterations, long samplingInterval,
			Map<Tuple<TAZ, TAZ>, Double> targetOdMatrix) {
		super(burnInIterations, samplingInterval, new Preferences<>());
		this.targetOdMatrix = targetOdMatrix;
		this.sampleOdMatrix = new LinkedHashMap<>(targetOdMatrix.size());
	}

	@Override
	public void processRelevantRoundTrip(RoundTrip<TAZ> roundTrip, double sampleWeight) {
		for (int i = 0; i < roundTrip.locationCnt(); i++) {
			Tuple<TAZ, TAZ> od = new Tuple<>(roundTrip.getLocation(i), roundTrip.getSuccessorLocation(i));
			if (!od.getA().equals(od.getB())) {
				this.sampleOdMatrix.compute(od, (od2, c) -> c == null ? sampleWeight : c + sampleWeight);
			}
		}
	}

	@Override
	public String toString() {
		final double fact = this.targetOdMatrix.values().stream().mapToDouble(v -> v).sum()
				/ this.sampleOdMatrix.values().stream().mapToDouble(v -> v).sum();
		StringBuffer result = new StringBuffer("from\tto\ttarget\trealized\n");
		for (Tuple<TAZ, TAZ> od : SetUtils.union(this.targetOdMatrix.keySet(), this.sampleOdMatrix.keySet())) {
			result.append(od.getA());
			result.append("\t");
			result.append(od.getB());
			result.append("\t");
			result.append(this.targetOdMatrix.getOrDefault(od, 0.0));
			result.append("\t");
			result.append(fact * this.sampleOdMatrix.getOrDefault(od, 0.0));
			result.append("\n");
		}
		return result.toString();
	}

}
