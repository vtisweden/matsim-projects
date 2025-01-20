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
package se.vti.roundtrips.targets;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.model.Episode;
import se.vti.roundtrips.model.StayEpisode;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class AtMainActivityTarget<L extends Location> extends Target<L> {

	private final Map<L, Double> location2target = new LinkedHashMap<>();

	private final double targetDuration_h;
	private final List<Tuple<Double, Double>> targetIntervals;

	public AtMainActivityTarget(double targetDuration_h, double intervalDuration_h, double endTime_h) {
		this.targetDuration_h = targetDuration_h;
		this.targetIntervals = Collections.unmodifiableList(Episode.effectiveIntervals(intervalDuration_h, endTime_h));
	}

	public void setTarget(L location, double target) {
		this.location2target.put(location, target);
	}

	@Override
	public String[] createLabels() {
		// solve this with a stream
		String[] result = new String[this.location2target.size()];
		int i = 0;
		for (L taz : this.location2target.keySet()) {
			result[i++] = taz.toString();
		}
		return result;
	}

	@Override
	public double[] computeTarget() {
		return this.location2target.values().stream().mapToDouble(t -> t).toArray();
	}

	@Override
	public double[] computeSample(MultiRoundTrip<L> filteredMultiRoundTrip) {
		Map<L, Integer> loc2cnt = this.location2target.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> 0)); // ensure ordering;
		for (RoundTrip<L> roundTrip : filteredMultiRoundTrip) {
			double bestOverlap_h = Double.NEGATIVE_INFINITY;
			L bestLocation = null;
			List<?> episodes = roundTrip.getEpisodes();
			for (int i = 2; i < episodes.size(); i += 2) {
				StayEpisode<L> episode = (StayEpisode<L>) episodes.get(i);
				double overlap_h = episode.overlap_h(this.targetIntervals);
				if (overlap_h > bestOverlap_h) {
					bestLocation = episode.getLocation();
					bestOverlap_h = overlap_h;
				}
			}
			if (bestOverlap_h >= this.targetDuration_h) {
				Integer oldCnt = loc2cnt.get(bestLocation);
				if (oldCnt != null /* otherwise not in target */) {
					loc2cnt.put(bestLocation, oldCnt + 1);
				}
			}
		}
		return loc2cnt.values().stream().mapToDouble(c -> c).toArray();
	}
}
