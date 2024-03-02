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

import se.vti.roundtrips.model.DefaultDrivingSimulator;
import se.vti.roundtrips.model.DrivingEpisode;
import se.vti.roundtrips.model.VehicleStateFactory;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class ElectrifiedDrivingSimulator extends DefaultDrivingSimulator<ElectrifiedLocation, ElectrifiedVehicleState, ElectrifiedRoundTrip> {

	private final double speed_km_h;
	private final double consumptionRate_kWh_km;

	public ElectrifiedDrivingSimulator(ElectrifiedScenario scenario,
			VehicleStateFactory<ElectrifiedVehicleState> stateFactory) {
		super(scenario, stateFactory);
		this.speed_km_h = scenario.getDefaultSpeed_km_h();
		this.consumptionRate_kWh_km = scenario.getConsumptionRate_kWh_km();
	}

	@Override
	public ElectrifiedVehicleState computeFinalState(ElectrifiedRoundTrip roundTrip, int roundTripIndex,
			DrivingEpisode<ElectrifiedLocation, ElectrifiedVehicleState> driving) {
		ElectrifiedVehicleState finalState = super.computeFinalState(roundTrip, roundTripIndex, driving);
		finalState.setBatteryCharge_kWh(driving.getInitialState().getBatteryCharge_kWh()
				- driving.getDuration_h() * this.speed_km_h * this.consumptionRate_kWh_km);
		return finalState;
	}

}
