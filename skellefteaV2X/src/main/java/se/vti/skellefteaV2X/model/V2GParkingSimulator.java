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

import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.roundtrips.model.Simulator.ParkingSimulator;

/**
 * 
 * @author GunnarF
 *
 */
public class V2GParkingSimulator
		implements ElectrifiedSimulator.ParkingSimulator<ElectrifiedLocation, ElectrifiedVehicleState> {

	private final ElectrifiedLocation v2gLocation;

	private final ParkingSimulator<ElectrifiedLocation, ElectrifiedVehicleState> defaultParkingSimulator;

	public V2GParkingSimulator(ElectrifiedLocation v2gLocation, ElectrifiedScenario scenario) {
		this.v2gLocation = v2gLocation;
		this.defaultParkingSimulator = new ElectrifiedParkingSimulator(scenario);
	}

	@Override
	public ParkingEpisode<ElectrifiedLocation, ElectrifiedVehicleState> newParkingEpisode(ElectrifiedLocation location,
			Integer departure, double initialTime_h, ElectrifiedVehicleState initialState) {
		if (this.v2gLocation.equals(location)) {
			// >>>>> replace this by the desired V2G logic >>>>>
			return this.defaultParkingSimulator.newParkingEpisode(location, departure, initialTime_h, initialState);
			// <<<<< replace this by the desired V2G logic <<<<<
		} else {
			return this.defaultParkingSimulator.newParkingEpisode(location, departure, initialTime_h, initialState);
		}
	}

}
