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

import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.Simulator.DrivingSimulator;

/**
 * 
 * @author GunnarF
 *
 */
public class DefaultDrivingSimulator implements DrivingSimulator {

	private final Scenario scenario;

	public DefaultDrivingSimulator(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public DrivingEpisode newDrivingEpisode(Location origin, Location destination, double time_h, double charge_kWh) {
		final DrivingEpisode driving = new DrivingEpisode(origin, destination);
		driving.setChargeAtStart_kWh(charge_kWh);
		driving.setDuration_h(this.scenario.getTime_h(origin, destination));
		driving.setEndTime_h(time_h + driving.getDuration_h());
		driving.setChargeAtEnd_kWh(charge_kWh
				- this.scenario.getDistance_km(origin, destination) * this.scenario.getConsumptionRate_kWh_km());
		return driving;
	}

}
