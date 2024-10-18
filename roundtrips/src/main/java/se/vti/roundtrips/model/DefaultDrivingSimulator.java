/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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

import se.vti.roundtrips.model.DefaultSimulator.DrivingSimulator;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class DefaultDrivingSimulator<L extends Location> implements DrivingSimulator<L> {

	protected final Scenario<L> scenario;
	protected final VehicleStateFactory stateFactory;

	public DefaultDrivingSimulator(Scenario<L> scenario, VehicleStateFactory stateFactory) {
		this.scenario = scenario;
		this.stateFactory = stateFactory;
	}

	public VehicleState computeFinalState(RoundTrip<L> roundTrip, int roundTripIndex, DrivingEpisode<L> driving) {
		return this.stateFactory.createVehicleState();
	}

	@Override
	public DrivingEpisode<L> newDrivingEpisode(RoundTrip<L> roundTrip, int roundTripIndex, double time_h,
			VehicleState initialState) {
		L origin = roundTrip.getLocation(roundTripIndex);
		L destination = roundTrip.getSuccessorLocation(roundTripIndex);

		final DrivingEpisode<L> driving = new DrivingEpisode<>(origin, destination);
		driving.setInitialState(initialState);

		driving.setDuration_h(this.scenario.getTime_h(origin, destination));
		driving.setEndTime_h(time_h + driving.getDuration_h());

		driving.setFinalState(this.computeFinalState(roundTrip, roundTripIndex, driving));

		return driving;
	}

}
