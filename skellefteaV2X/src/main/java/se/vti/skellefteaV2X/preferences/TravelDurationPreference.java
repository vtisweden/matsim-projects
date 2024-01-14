/**
 * se.vti.skellefteaV2X
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

import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.Preferences.Component;
import se.vti.skellefteaV2X.model.RoundTripUtils;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class TravelDurationPreference implements Component {

	private final double targetDuration_h;

	public TravelDurationPreference(double targetDuration_h) {
		this.targetDuration_h = targetDuration_h;
	}

	@Override
	public double logWeight(SimulatedRoundTrip simulatedRoundTrip) {
		double travelDuration_h = 0.0;
		for (int i = 1; i < simulatedRoundTrip.size(); i += 2) {
			travelDuration_h += RoundTripUtils
					.effectiveDuration_h((DrivingEpisode) simulatedRoundTrip.getEpisodes().get(i));
		}
		return -Math.abs(travelDuration_h - this.targetDuration_h);
	}

}
