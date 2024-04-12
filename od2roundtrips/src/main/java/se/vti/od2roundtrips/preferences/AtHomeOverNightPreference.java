/**
 * org.matsim.contrib.emulation
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
package se.vti.od2roundtrips.preferences;

import java.util.Collections;
import java.util.List;

import floetteroed.utilities.Tuple;
import se.vti.od2roundtrips.model.TAZ;
import se.vti.roundtrips.model.Episode;
import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class AtHomeOverNightPreference extends PreferenceComponent<RoundTrip<TAZ>> {

	private final MathHelpers math = new MathHelpers();

	private final double targetDuration_h;
	private final double overlapStrictness;
	private final List<Tuple<Double, Double>> targetIntervals;

	public AtHomeOverNightPreference(double targetDuration_h, double intervalDuration_h, double endTime_h,
			double overlapStrictness) {
		this.targetDuration_h = targetDuration_h;
		this.overlapStrictness = overlapStrictness;
		this.targetIntervals = Collections.unmodifiableList(Episode.effectiveIntervals(intervalDuration_h, endTime_h));
	}

	@Override
	public double logWeight(RoundTrip<TAZ> roundTrip) {
		final ParkingEpisode<?, ?> home = (ParkingEpisode<?, ?>) roundTrip.getEpisodes().get(0);
		final double overlap_h = Math.max(Math.min(home.overlap_h(this.targetIntervals), this.targetDuration_h),
				0.001 * this.targetDuration_h);
		return this.overlapStrictness * (Math.log(overlap_h) - Math.log(this.targetDuration_h));

	}

}