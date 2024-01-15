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

import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class AllDayTimeConstraintPreference implements Preferences.Component {

	private final double minHomeDuration_h;

	public AllDayTimeConstraintPreference(double minHomeDuration_h) {
		this.minHomeDuration_h = minHomeDuration_h;
	}

	public AllDayTimeConstraintPreference() {
		this(0.0);
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {

		if (roundTrip.locationCnt() == 1) {
			return 0.0;
		}

		List<Episode> episodes = roundTrip.getEpisodes();
		ParkingEpisode home = (ParkingEpisode) episodes.get(0);
		DrivingEpisode fromHome = (DrivingEpisode) episodes.get(1);

		return Math.max(0.0, (home.getEndTime_h() - 24.0 + this.minHomeDuration_h)
				- (fromHome.getEndTime_h() - fromHome.getDuration_h()));
	}

}
