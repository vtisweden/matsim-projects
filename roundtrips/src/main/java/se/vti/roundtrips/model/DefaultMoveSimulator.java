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

import se.vti.roundtrips.model.DefaultSimulator.MoveSimulator;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.SimulatorState;

/**
 * 
 * @author GunnarF
 *
 */
public class DefaultMoveSimulator<L extends Location> implements MoveSimulator<L> {

	protected final Scenario<L> scenario;

	public DefaultMoveSimulator(Scenario<L> scenario) {
		this.scenario = scenario;
	}

	public SimulatorState computeFinalState(RoundTrip<L> roundTrip, int roundTripIndex, MoveEpisode<L> driving) {
		return null;
	}

	@Override
	public MoveEpisode<L> newMoveEpisode(RoundTrip<L> roundTrip, int roundTripIndex, double time_h,
			SimulatorState initialState) {
		L origin = roundTrip.getLocation(roundTripIndex);
		L destination = roundTrip.getSuccessorLocation(roundTripIndex);

		final MoveEpisode<L> move = new MoveEpisode<>(origin, destination);
		move.setInitialState(initialState);

		move.setDuration_h(this.scenario.getTime_h(origin, destination));
		move.setEndTime_h(time_h + move.getDuration_h());

		move.setFinalState(this.computeFinalState(roundTrip, roundTripIndex, move));

		return move;
	}

}
