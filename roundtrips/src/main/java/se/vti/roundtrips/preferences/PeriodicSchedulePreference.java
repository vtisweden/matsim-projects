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
package se.vti.roundtrips.preferences;

import se.vti.roundtrips.model.MoveEpisode;
import se.vti.roundtrips.model.StayEpisode;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class PeriodicSchedulePreference<R extends RoundTrip<L>, L extends Location> extends PreferenceComponent<R> {

	private final double minHomeDuration_h;

	private final double periodLength_h;

	public PeriodicSchedulePreference(double minHomeDuration_h, double periodLength_h) {
		this.minHomeDuration_h = minHomeDuration_h;
		this.periodLength_h = periodLength_h;
	}

	public PeriodicSchedulePreference(double periodLength_h) {
		this(0.0, periodLength_h);
	}

	public double discrepancy_h(R roundTrip) {
		if (roundTrip.locationCnt() == 1) {
			return 0.0;
		} else {
			final StayEpisode<?> home = (StayEpisode<?>) roundTrip.getEpisodes().get(0);
			final MoveEpisode<?> leaveHome = (MoveEpisode<?>) roundTrip.getEpisodes().get(1);
			final double earliestLeaveHome_h = home.getEndTime_h() - this.periodLength_h + this.minHomeDuration_h;
			final double realizedLeaveHome_h = leaveHome.getEndTime_h() - leaveHome.getDuration_h();
			return Math.max(0.0, earliestLeaveHome_h - realizedLeaveHome_h);
		}
	}

	@Override
	public double logWeight(R roundTrip) {
		return -this.discrepancy_h(roundTrip) / this.periodLength_h;
	}
}
