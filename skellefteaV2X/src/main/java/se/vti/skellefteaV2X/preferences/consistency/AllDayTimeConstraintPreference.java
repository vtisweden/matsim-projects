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
package se.vti.skellefteaV2X.preferences.consistency;

import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class AllDayTimeConstraintPreference extends Preferences.Component {

	private final double minHomeDuration_h;

	public AllDayTimeConstraintPreference(double minHomeDuration_h) {
		this.minHomeDuration_h = minHomeDuration_h;
	}

	public AllDayTimeConstraintPreference() {
		this(0.0);
	}
	
	public double discrepancy_h(SimulatedRoundTrip roundTrip) {
		if (roundTrip.locationCnt() == 1) {
			return 0.0;
		} else {
			final ParkingEpisode home = (ParkingEpisode) roundTrip.getEpisodes().get(0);
			final DrivingEpisode leaveHome = (DrivingEpisode) roundTrip.getEpisodes().get(1);
			final double earliestLeaveHome_h = home.getEndTime_h() - 24.0 + this.minHomeDuration_h;
			final double realizedLeaveHome_h = leaveHome.getEndTime_h() - leaveHome.getDuration_h();
			return Math.max(0.0, earliestLeaveHome_h - realizedLeaveHome_h);
		}
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {
		return -this.discrepancy_h(roundTrip) / 24.0;
	}
}
