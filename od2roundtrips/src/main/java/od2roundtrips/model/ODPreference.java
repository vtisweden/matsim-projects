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

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class ODPreference extends PreferenceComponent<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> {

	private final Map<Tuple<TAZ, TAZ>, Double> targetODMatrix = new LinkedHashMap<>();
	private Double targetOdSum = null;

	public ODPreference() {
	}

	public void setODEntry(TAZ origin, TAZ destination, double value) {
		if (origin.equals(destination)) {
			return; // Attention, ignoring main diagonals!
		}
		this.targetODMatrix.put(new Tuple<>(origin, destination), value);
	}

	public Map<Tuple<TAZ, TAZ>, Double> getTargetOdMatrix() {
		return this.targetODMatrix;
	}

	public double getNonNullTargetOdSum() {
		if (this.targetOdSum == null) {
			this.targetOdSum = this.targetODMatrix.values().stream().mapToDouble(v -> v).sum();
		}
		return this.targetOdSum;
	}

	@Override
	public double logWeight(MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> multiRoundTrip) {

		final Map<Tuple<TAZ, TAZ>, Integer> realizedOdMatrix = multiRoundTrip.getODView();
		final double realizedTripCnt = multiRoundTrip.getSingleTripCnt();
		final double targetTripCnt = this.getNonNullTargetOdSum();

		double slack = 0.5 / targetTripCnt;
		double err = 0.0;
		for (Map.Entry<Tuple<TAZ, TAZ>, Double> target : this.targetODMatrix.entrySet()) {
			err += Math.max(0.0, Math.abs(realizedOdMatrix.getOrDefault(target.getKey(), 0) / realizedTripCnt
					- target.getValue() / targetTripCnt) - slack);
		}
		multiRoundTrip.setODReproductionError(err);

		return (-1.0) * multiRoundTrip.size() * (err + slack * this.targetODMatrix.size());
	}
}
