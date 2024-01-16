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
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences.Component;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class OffLocationPreference extends Component {

	private final Location location;

	private final List<Tuple<Double, Double>> targetIntervals;

	private final MathHelpers math = new MathHelpers();

	public OffLocationPreference(Location location, double duration_h, double end_h) {
		this.location = location;
		this.targetIntervals = Episode.effectiveIntervals(duration_h, end_h);
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {

		double realizedDuration_h = 0.0;

		for (Episode e : roundTrip.getEpisodes()) {
			if (e instanceof ParkingEpisode) {
				ParkingEpisode p = (ParkingEpisode) e;
				if (this.location.equals(p.getLocation())) {
					final List<Tuple<Double, Double>> realizedIntervals = p.effectiveIntervals();
					for (Tuple<Double, Double> target : this.targetIntervals) {
						for (Tuple<Double, Double> realized : realizedIntervals) {
							realizedDuration_h += this.math.overlap(target.getA(), target.getB(), realized.getA(),
									realized.getB());
						}
					}
				}
			}
		}

		return -realizedDuration_h;
	}

}
