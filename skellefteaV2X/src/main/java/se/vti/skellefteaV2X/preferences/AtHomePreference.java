/**
 * se.vti.skellefeaV2X
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

import java.util.List;

import floetteroed.utilities.Tuple;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences.Component;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class AtHomePreference implements Component {

	private final List<Tuple<Double, Double>> targetIntervals;
	private final double targetDuration_h;
	private final MathHelpers math = new MathHelpers();
	
	public AtHomePreference(double duration_h, double homeEnd_h) {
		
		this.targetIntervals = Episode.effectiveIntervals(duration_h, homeEnd_h);
		
		
		this.targetDuration_h = this.targetIntervals.stream().mapToDouble(t -> t.getB() - t.getA()).sum();
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {

		final ParkingEpisode home = (ParkingEpisode) roundTrip.getEpisodes().get(0);
		final List<Tuple<Double, Double>> realizedIntervals = home.effectiveIntervals();

		double realizedDuration_h = 0.0;
		for (Tuple<Double, Double> target : this.targetIntervals) {
			for (Tuple<Double, Double> realized : realizedIntervals) {
				realizedDuration_h += this.math.overlap(target.getA(), target.getB(), realized.getA(),
						realized.getB());
			}
		}
		assert (this.targetDuration_h >= realizedDuration_h);
		return realizedDuration_h - this.targetDuration_h;

//		if (roundTrip.size() == 1) {
//			return 0.0; // FIXME may be on campus
//		}
//		List<Episode> episodes = roundTrip.getEpisodes();
//		ParkingEpisode home = (ParkingEpisode) episodes.get(0);
//		double discrepancy_h = 0.0;
//		if (this.campus.equals(home.getLocation())) {
//			discrepancy_h = this.homeEnd_h - this.homeStart_h;
//		} else {
//			discrepancy_h += Math.max(0, home.getStartTime_h() - this.homeStart_h);
//			discrepancy_h += Math.max(0, this.homeEnd_h - home.getEndTime_h());
//
//		}
//		return -discrepancy_h;
	}

}
