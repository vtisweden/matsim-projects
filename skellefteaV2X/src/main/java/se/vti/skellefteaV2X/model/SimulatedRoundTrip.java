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
import java.util.Random;

import se.vti.roundtrips.model.Episode;
import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;

/**
 * 
 * TODO extract to roundtrips project
 * 
 * @author GunnarF
 *
 */
public class SimulatedRoundTrip extends ElectrifiedRoundTrip {

	private final Random rnd;
	
	private List<Episode<ElectrifiedVehicleState>> episodes = null;

	public SimulatedRoundTrip(List<ElectrifiedLocation> locations, List<Integer> departures, List<Boolean> charging, Random rnd) {
		super(locations, departures, charging, rnd);
		this.rnd = rnd;
	}

	@Override
	public SimulatedRoundTrip clone() {
		return new SimulatedRoundTrip(cloneLocations(), cloneDepartures(), cloneChargings(), this.rnd);
	}

	public int episodeCnt() {
		return this.episodes.size();
	}

	public ElectrifiedLocation getHomeLocation() {
		return ((ParkingEpisode<ElectrifiedLocation, ElectrifiedVehicleState>) this.episodes.get(0)).getLocation();
	}

	public List<Episode<ElectrifiedVehicleState>> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(List<Episode<ElectrifiedVehicleState>> episodes) {
		this.episodes = episodes;
	}

}
