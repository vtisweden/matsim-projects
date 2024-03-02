/**
 * se.vti.skellefteaV2X.model
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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

import se.vti.roundtrips.model.DefaultParkingSimulator;
import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class ElectrifiedParkingSimulator
		extends DefaultParkingSimulator<ElectrifiedLocation, ElectrifiedVehicleState, ElectrifiedRoundTrip> {

	private final double chargingRate_kW;
	private final double batteryCapacity_kWh;

	public ElectrifiedParkingSimulator(ElectrifiedScenario scenario) {
		super(scenario, new ElectrifiedVehicleStateFactory());
		this.chargingRate_kW = scenario.getChargingRate_kW();
		this.batteryCapacity_kWh = scenario.getMaxCharge_kWh();
	}

	@Override
	public ElectrifiedVehicleState computeFinalState(ElectrifiedRoundTrip roundTrip, int roundTripIndex,
			ParkingEpisode<ElectrifiedLocation, ElectrifiedVehicleState> parking) {
		ElectrifiedVehicleState finalState = super.computeFinalState(roundTrip, roundTripIndex, parking);
		if (roundTrip.getCharging(roundTripIndex) && roundTrip.getLocation(roundTripIndex).getAllowsCharging()) {
			finalState.setBatteryCharge_kWh(Math.min(this.batteryCapacity_kWh,
					parking.getInitialState().getBatteryCharge_kWh() + parking.getDuration_h() * this.chargingRate_kW));
		} else {
			finalState.setBatteryCharge_kWh(parking.getInitialState().getBatteryCharge_kWh());
		}
		return finalState;
	}

}
