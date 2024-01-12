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
public class DefaultParkingSimulator implements ParkingSimulator {

	private final Scenario scenario;
	
	public DefaultParkingSimulator(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public ParkingEpisode newParkingEpisode(Location location, Integer departure, Boolean charging,
			double time_h, double charge_kWh) {
		final ParkingEpisode parking = new ParkingEpisode(location);
		parking.setStartTime_h(time_h);
		parking.setChargeAtStart_kWh(charge_kWh);
		parking.setEndTime_h(Math.max(time_h, this.scenario.getBinSize_h() * departure));
		if (location.getAllowsCharging() && charging) {
			charge_kWh = Math.min(this.scenario.getMaxCharge_kWh(),
					this.scenario.getChargingRate_kW() * (parking.getEndTime_h() - parking.getStartTime_h()));
		}
		parking.setChargeAtEnd_kWh(charge_kWh);
		return parking;
	}

}
