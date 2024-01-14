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
import floetteroed.utilities.math.MathHelpers;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences.Component;
import se.vti.skellefteaV2X.model.RoundTripUtils;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class AtLocationPreference implements Component {

	private final Location location;

	private final List<Tuple<Double, Double>> targetIntervals;
	private final double targetDuration_h;

	public AtLocationPreference(Location location, double targetStart_h, double targetEnd_h) {
		this.location = location;
		this.targetIntervals = RoundTripUtils.effectiveIntervals(targetStart_h, targetEnd_h);
		this.targetDuration_h = RoundTripUtils.effectiveDuration_h(this.targetIntervals);
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {

		double realizedDuration_h = 0.0;

		for (Episode e : roundTrip.getEpisodes()) {
			if (e instanceof ParkingEpisode) {
				ParkingEpisode p = (ParkingEpisode) e;
				if (this.location.equals(p.getLocation())) {
					final List<Tuple<Double, Double>> realizedIntervals = RoundTripUtils
							.effectiveIntervals(p.getStartTime_h(), p.getEndTime_h());
					for (Tuple<Double, Double> target : this.targetIntervals) {
						for (Tuple<Double, Double> realized : realizedIntervals) {
							realizedDuration_h += MathHelpers.overlap(target.getA(), target.getB(), realized.getA(),
									realized.getB());
						}
					}
				}
			}
		}
		
		// because realizations may not satisfy all constraints
		realizedDuration_h = Math.min(realizedDuration_h, this.targetDuration_h);
		
		return  realizedDuration_h - this.targetDuration_h;
	}

}