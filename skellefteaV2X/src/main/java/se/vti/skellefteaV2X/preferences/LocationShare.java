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
package se.vti.skellefteaV2X.preferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.model.Episode;
import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.roundtrips.single.Location;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class LocationShare extends PreferenceComponent<ElectrifiedRoundTrip> {

	protected final MathHelpers math = new MathHelpers();

	private final double targetDuration_h;
	private final double intervalDuration_h;
	private final double endTime_h;
	private final double compliance = 0.5;
	private final double overlapStrictness;
	private final List<Tuple<Double, Double>> targetIntervals;

	private final Map<Location, Double> location2Share = new LinkedHashMap<>();
	private double shareSum = 0.0;

	public LocationShare(double targetDuration_h, double intervalDuration_h, double endTime_h,
			double overlapStrictness) {
		this.targetDuration_h = targetDuration_h;
		this.intervalDuration_h = intervalDuration_h;
		this.endTime_h = endTime_h;
		this.overlapStrictness = overlapStrictness;
		this.targetIntervals = Collections.unmodifiableList(Episode.effectiveIntervals(intervalDuration_h, endTime_h));
	}

	public void setShare(Location location, double share) {
		assert (!this.location2Share.containsKey(location));
		this.shareSum += share;
		this.location2Share.put(location, share);
	}

	double emphasize(double ratio) {
		return Math.pow(ratio, this.overlapStrictness);
//		double xThresh = 0.9;
//		double yThreshVal = 0.1;
//		if (ratio < xThresh) {
//			return (yThreshVal / xThresh) * ratio;
//		} else {
//			double rel = (ratio - xThresh) / (1.0 - xThresh);
//			return yThreshVal * (1.0 - rel) + 1.0 * rel;
//		}
	}

	protected double logWeight(List<ParkingEpisode<?,?>> parkingEpisodes) {

		double maxOverlap_h = 0;
		ParkingEpisode<?,?> relevantEpisode = null;
		for (ParkingEpisode<?,?> p : parkingEpisodes) {
			double overlap_h = p.overlap_h(this.targetIntervals);
			if (relevantEpisode == null || overlap_h > maxOverlap_h) {
				relevantEpisode = p;
				maxOverlap_h = overlap_h;
			}
		}
		maxOverlap_h = Math.min(maxOverlap_h, this.targetDuration_h);

		if (relevantEpisode == null) {
			return Math.log(1e-8);
		} else {
			double weight = Math.pow(maxOverlap_h / this.targetDuration_h, this.overlapStrictness)
					* (this.location2Share.get(relevantEpisode.getLocation()) / this.shareSum);
			return Math.log(Math.max(1e-8, weight));
		}

	}

}