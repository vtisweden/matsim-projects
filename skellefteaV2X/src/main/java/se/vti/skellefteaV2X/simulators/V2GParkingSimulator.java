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
package se.vti.skellefteaV2X.simulators;

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.Simulator.ParkingSimulator;

/**
 * 
 * @author GunnarF
 *
 */
public class V2GParkingSimulator implements ParkingSimulator {

	private final Scenario scenario;

	private final Location v2gLocation;

	private final ParkingSimulator defaultParkingSimulator;

	public V2GParkingSimulator(Scenario scenario, Location v2gLocation, ParkingSimulator defaultParkingSimulator) {
		this.scenario = scenario;
		this.v2gLocation = v2gLocation;
		this.defaultParkingSimulator = defaultParkingSimulator;
	}

	public V2GParkingSimulator(Scenario scenario, Location campus) {
		this(scenario, campus, new DefaultParkingSimulator(scenario));
	}

	@Override
	public ParkingEpisode newParkingEpisode(Location location, Integer departure, Boolean charging,
			double initialTime_h, double initialCharge_kWh) {
		if (this.v2gLocation.equals(location)) {
			// >>>>> replace this by the desired V2G logic >>>>>
			return this.defaultParkingSimulator.newParkingEpisode(location, departure, charging, initialTime_h,
					initialCharge_kWh);
			// <<<<< replace this by the desired V2G logic <<<<<
		} else {
			return this.defaultParkingSimulator.newParkingEpisode(location, departure, charging, initialTime_h,
					initialCharge_kWh);
		}
	}

}
