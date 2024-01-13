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

import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;
import se.vti.skellefteaV2X.model.Preferences.Component;

/**
 * 
 * @author GunnarF
 *
 */
public class AtHomeOffCampusPreference implements Component {

	private final Location campus;
	private final double homeStart_h;
	private final double homeEnd_h;

	public AtHomeOffCampusPreference(Location campus, double homeStart_h, double homeEnd_h) {
		this.campus = campus;
		this.homeStart_h = homeStart_h;
		this.homeEnd_h = homeEnd_h;
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {
		if (roundTrip.size() == 1) {
			return 0.0; // FIXME may be on campus
		}
		List<Episode> episodes = roundTrip.getEpisodes();
		ParkingEpisode home = (ParkingEpisode) episodes.get(0);
		double discrepancy_h = 0.0;
		if (this.campus.equals(home.getLocation())) {
			discrepancy_h = this.homeEnd_h - this.homeStart_h;
		} else {
			discrepancy_h += Math.max(0, home.getStartTime_h() - this.homeStart_h);
			discrepancy_h += Math.max(0, this.homeEnd_h - home.getEndTime_h());

		}
		return -discrepancy_h;
	}

}
