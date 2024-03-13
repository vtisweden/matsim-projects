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

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;
import se.vti.skellefteaV2X.model.ElectrifiedLocation;
import se.vti.utils.misc.math.MathHelpers;

public class LongOffHomeActivityPreferencePreference extends Preferences.Component<ElectrifiedRoundTrip, ElectrifiedLocation> {

	private final MathHelpers math = new MathHelpers();

	private double earliestStart_h;
	private double latestEnd_h;
	private double minTargetDuration_h;
	private double maxTargetDuration_h;

	public LongOffHomeActivityPreferencePreference(double earliestStart_h, double latestEnd_h,
			double minTargetDuration_h, double maxTargetDuration_h) {
		this.earliestStart_h = earliestStart_h;
		this.latestEnd_h = latestEnd_h;
		this.minTargetDuration_h = minTargetDuration_h;
		this.maxTargetDuration_h = maxTargetDuration_h;
	}

	@Override
	public double logWeight(ElectrifiedRoundTrip simulatedRoundTrip) {
		double discrepancy_h = this.minTargetDuration_h;
		if (simulatedRoundTrip.locationCnt() > 1) {
			for (int i = 2; i < simulatedRoundTrip.getEpisodes().size(); i += 2) {
				ParkingEpisode<?, ?> p = (ParkingEpisode<?, ?>) simulatedRoundTrip.getEpisodes().get(i);
				for (Tuple<Double, Double> interval : p.effectiveIntervals()) {
					double insideOverlap_h = this.math.overlap(interval.getA(), interval.getB(), this.earliestStart_h,
							this.latestEnd_h);
					double outsideOverlap_h = this.math.overlap(interval.getA(), interval.getB(), 0.0,
							this.earliestStart_h)
							+ this.math.overlap(interval.getA(), interval.getB(), this.latestEnd_h, 24.0);
					discrepancy_h = Math.min(discrepancy_h,
							outsideOverlap_h + Math.max(0.0, this.minTargetDuration_h - insideOverlap_h)
									+ Math.max(0.0, insideOverlap_h - this.maxTargetDuration_h));
				}
			}
		}
		return -discrepancy_h;
	}

}
