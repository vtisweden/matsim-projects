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
import se.vti.skellefteaV2X.model.Preferences.Component;

/**
 * 
 * @author GunnarF
 *
 */
public class OnCampusPreference implements Component {

	private final Location campus;
	private final double targetTime_h;

	public OnCampusPreference(Location campus, double targetTime_h) {
		this.campus = campus;
		this.targetTime_h = targetTime_h;
	}

	@Override
	public double logWeight(List<Episode> episodes) {
		double minDist_h = 24.0;
		if (episodes.size() == 1) {
			if (this.campus.equals(((ParkingEpisode) episodes.get(0)).getLocation())) {
				minDist_h = 0.0;
			}
		} else {
			for (Episode e : episodes) {
				if (e instanceof ParkingEpisode) {
					ParkingEpisode p = (ParkingEpisode) e;
					if (this.campus.equals(p.getLocation())) {
						final double candDist_h;
						if (this.targetTime_h < p.getStartTime_h()) {
							candDist_h = p.getStartTime_h() - this.targetTime_h;
						} else if (this.targetTime_h < p.getEndTime_h()) {
							candDist_h = 0.0;
						} else {
							candDist_h = this.targetTime_h - p.getEndTime_h();
						}
						minDist_h = Math.min(minDist_h, candDist_h);
					}
				}
			}
		}
		return -minDist_h;
	}

}
