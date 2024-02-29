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

import se.vti.roundtrips.model.VehicleStateFactory;

/**
 * 
 * @author GunnarF
 *
 */
public class ElectrifiedSimulator
		extends se.vti.roundtrips.model.Simulator<ElectrifiedLocation, ElectrifiedVehicleState> {

	private final double batteryCapacity_kWh;

	public ElectrifiedSimulator(ElectrifiedScenario scenario, VehicleStateFactory<ElectrifiedVehicleState> stateFactory,
			double batteryCapacity_kWh) {
		super(scenario, stateFactory);
		this.batteryCapacity_kWh = batteryCapacity_kWh;
	}

	@Override
	public void initializeState(ElectrifiedVehicleState initialState) {
		initialState.setBatteryCharge_kWh(this.batteryCapacity_kWh);
	}

	@Override
	public ElectrifiedVehicleState keepOrChangeInitialState(ElectrifiedVehicleState oldInitialState,
			ElectrifiedVehicleState newInitialState) {

		final double oldInitialCharge_kWh = oldInitialState.getBatteryCharge_kWh();
		final double newInitialCharge_kWh = newInitialState.getBatteryCharge_kWh();

		if ((newInitialCharge_kWh >= 0.0) && Math.abs(newInitialCharge_kWh - oldInitialCharge_kWh) > 1e-3) {
			return newInitialState;
		} else {
			return oldInitialState;
		}

	}

}
