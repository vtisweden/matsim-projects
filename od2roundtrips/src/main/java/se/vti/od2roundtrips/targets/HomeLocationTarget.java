/**
 * se.vti.od2roundtrips.targets
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
import java.util.stream.Collectors;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class HomeLocationTarget<L extends Location> extends Target<L> {

	private final Map<L, Double> home2target = new LinkedHashMap<>();

	public HomeLocationTarget() {
	}

	public void setTarget(L home, double target) {
		this.home2target.put(home, target);
	}

	@Override
	public double[] computeTarget() {
		return this.home2target.values().stream().mapToDouble(t -> t).toArray();
	}

	@Override
	public double[] computeSample(MultiRoundTrip<L> multiRoundTrip) {
		Map<L, Integer> home2sample = this.home2target.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> 0)); // ensure ordering
		for (RoundTrip<L> roundTrip : multiRoundTrip) {
			home2sample.compute(roundTrip.getLocation(0), (l, c) -> c == null ? 1 : c + 1);
		}
		return home2sample.values().stream().mapToDouble(c -> c).toArray();
	}

	@Override
	public String[] createLabels() {
		String[] result = new String[this.home2target.size()];
		int i = 0;
		for (L taz : this.home2target.keySet()) {
			result[i++] = taz.toString();
		}
		return result;
	}
	

}
