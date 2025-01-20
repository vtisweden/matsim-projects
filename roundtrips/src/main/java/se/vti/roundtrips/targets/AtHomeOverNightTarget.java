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
import java.util.List;

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
public class AtHomeOverNightTarget<L extends Location> extends Target<L> {

	private final double targetDuration_h;
	private final List<Tuple<Double, Double>> targetIntervals;
	private final double target;

	public AtHomeOverNightTarget(double targetDuration_h, double intervalDuration_h, double endTime_h, double target) {
		this.targetDuration_h = targetDuration_h;
		this.targetIntervals = Collections.unmodifiableList(Episode.effectiveIntervals(intervalDuration_h, endTime_h));
		this.target = target;
	}

	@Override
	public String[] createLabels() {
		return new String[] { "at home overnight", "NOT at home overnight" };
	}

	@Override
	public double[] computeTarget() {
		return new double[] { this.target, 0.0 }; 
	}

	@Override
	public double[] computeSample(MultiRoundTrip<L> filteredMultiRoundTrip) {
		double total = 0.0;
		double cnt = 0.0;
		for (RoundTrip<L> roundTrip : filteredMultiRoundTrip) {
			total++;
			final StayEpisode<L> home = (StayEpisode<L>) roundTrip.getEpisodes().get(0);
			if (home.overlap_h(this.targetIntervals) >= this.targetDuration_h) {
				cnt++;
			}
		}
		return new double[] { cnt, total - cnt };
	}
}
