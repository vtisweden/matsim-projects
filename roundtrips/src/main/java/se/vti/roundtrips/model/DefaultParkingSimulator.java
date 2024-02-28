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
package se.vti.roundtrips.model;

import se.vti.roundtrips.model.Simulator.ParkingSimulator;

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

	public VehicleState computeFinalState(ParkingEpisode parking) {
		return new VehicleState();
	}

	@Override
	public ParkingEpisode newParkingEpisode(Location location, Integer departure, double time_h,
			VehicleState initialState) {
		final ParkingEpisode parking = new ParkingEpisode(location);
		parking.setInitialState(initialState);

//		parking.setChargeAtStart_kWh(charge_kWh);
		parking.setEndTime_h(Math.max(time_h, this.scenario.getBinSize_h() * departure));
		parking.setDuration_h(parking.getEndTime_h() - time_h);
//		if (location.getAllowsCharging() && charging) {
//			double chargingDuration_h = Math.min(
//					Math.max(0.0, this.scenario.getMaxCharge_kWh() - charge_kWh) / this.scenario.getChargingRate_kW(),
//					parking.getDuration_h());
//			charge_kWh = Math.min(this.scenario.getMaxCharge_kWh(),
//					charge_kWh + this.scenario.getChargingRate_kW() * chargingDuration_h);
//		}
//		parking.setChargeAtEnd_kWh(charge_kWh);

		VehicleState finalState = this.computeFinalState(parking);
		parking.setFinalState(finalState);

		return parking;
	}

}
