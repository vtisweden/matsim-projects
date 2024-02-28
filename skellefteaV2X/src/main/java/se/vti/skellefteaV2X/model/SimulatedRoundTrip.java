/**
 * se.vti.skelleftea
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
package se.vti.skellefteaV2X.model;

import java.util.List;

import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class SimulatedRoundTrip extends RoundTrip<Location> {

	private List<Episode> episodes = null;

	public SimulatedRoundTrip(List<Location> locations, List<Integer> departures, List<Boolean> charging) {
		super(locations, departures, charging);
	}

	@Override
	public SimulatedRoundTrip clone() {
		return new SimulatedRoundTrip(cloneLocations(), cloneDepartures(), cloneChargings());
	}

	public int episodeCnt() {
		return this.episodes.size();
	}

	public Location getHomeLocation() {
		return ((ParkingEpisode) this.episodes.get(0)).getLocation();
	}

	public List<Episode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(List<Episode> episodes) {
		this.episodes = episodes;
	}

}
