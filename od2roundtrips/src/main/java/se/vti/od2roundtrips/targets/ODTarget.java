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
package se.vti.od2roundtrips.targets;

import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.Tuple;
import se.vti.od2roundtrips.model.MultiRoundTripWithOD;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.targets.Target;

/**
 * 
 * @author GunnarF
 *
 */
public class ODTarget<L extends Location> extends Target<L> {

	private final Map<Tuple<L, L>, Double> targetODMatrix = new LinkedHashMap<>();

	public ODTarget() {
	}

	public void setODEntry(L origin, L destination, double value) {
		this.targetODMatrix.put(new Tuple<>(origin, destination), value);
	}

	public Map<Tuple<L, L>, Double> getTargetOdMatrix() {
		return this.targetODMatrix;
	}

	private double[] od2array(Map<Tuple<L, L>, ? extends Number> od) {
		double[] result = new double[this.targetODMatrix.size()];
		int i = 0;
		for (Map.Entry<Tuple<L, L>, Double> targetEntry : this.targetODMatrix.entrySet()) {
			if (od.containsKey(targetEntry.getKey())) {
				result[i++] = od.get(targetEntry.getKey()).doubleValue();
			} else {
				result[i++] = 0.0;
			}
		}
		return result;
	}

	@Override
	public double[] computeTarget() {
		return this.od2array(this.targetODMatrix);
	}

	@Override
	public double[] computeSample(MultiRoundTrip<L> multiRoundTrip) {
		return this.od2array(((MultiRoundTripWithOD<L>) multiRoundTrip).getODView());
	}

	@Override
	public String[] createLabels() {
		String[] result = new String[this.targetODMatrix.size()];
		int i = 0;
		for (Tuple<L, L> od : this.targetODMatrix.keySet()) {
			result[i++] = od.getA() + "->" + od.getB();
		}
		return result;
	}

}
