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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.Simulator;

/**
 * 
 * @author GunnarF
 *
 */
public class DefaultSimulator<L extends Location> implements Simulator<L> {

	// -------------------- INTERFACES --------------------

	public interface StaySimulator<L extends Location> {

		StayEpisode<L> newStayEpisode(RoundTrip<L> roundTrip, int roundTripIndex, double initialTime_h,
				Object initialState);
	}

	public interface MoveSimulator<L extends Location> {

		MoveEpisode<L> newMoveEpisode(RoundTrip<L> roundTrip, int roundTripStartIndex, double initialTime_h,
				Object initialState);
	}

	// -------------------- MEMBERS --------------------

	protected final Scenario<L> scenario;

	private MoveSimulator<L> moveSimulator = null;
	private StaySimulator<L> staySimulator = null;

	// -------------------- CONSTRUCTION --------------------

	public DefaultSimulator(Scenario<L> scenario) {
		this.scenario = scenario;
		this.setMoveSimulator(new DefaultMoveSimulator<>(scenario));
		this.setStaySimulator(new DefaultStaySimulator<>(scenario));
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public Scenario<L> getScenario() {
		return this.scenario;
	}

	public void setMoveSimulator(MoveSimulator<L> drivingSimulator) {
		this.moveSimulator = drivingSimulator;
	}

	public void setStaySimulator(StaySimulator<L> parkingSimulator) {
		this.staySimulator = parkingSimulator;
	}

	// -------------------- HOOKS FOR SUBCLASSING --------------------

	public Object createAndInitializeState() {
		return null;
	}

	public StayEpisode<L> createHomeOnlyEpisode(RoundTrip<L> roundTrip) {
		StayEpisode<L> home = new StayEpisode<>(roundTrip.getLocation(0));
		home.setDuration_h(24.0);
		home.setEndTime_h(24.0 - 1e-8); // wraparound
		home.setInitialState(this.createAndInitializeState());
		home.setFinalState(this.createAndInitializeState());
		return home;
	}

	public Object keepOrChangeInitialState(Object oldInitialState, Object newInitialState) {
		return oldInitialState;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public List<? extends Episode> simulate(RoundTrip<L> roundTrip) {

		if (roundTrip.locationCnt() == 1) {
			return Collections.singletonList(this.createHomeOnlyEpisode(roundTrip));
		}

		final double initialTime_h = this.scenario.getBinSize_h() * roundTrip.getDeparture(0);
		Object initialState = this.createAndInitializeState();

		List<Episode> episodes = null;
		do {

			episodes = new ArrayList<>(2 * roundTrip.locationCnt() - 1);
			episodes.add(null); // placeholder for home episode

			double time_h = initialTime_h;
			Object currentState = initialState;

			for (int index = 0; index < roundTrip.locationCnt() - 1; index++) {

				final MoveEpisode<L> moving = this.moveSimulator.newMoveEpisode(roundTrip, index, time_h,
						currentState);
				episodes.add(moving);
				time_h = moving.getEndTime_h();
				currentState = moving.getFinalState();

				final StayEpisode<L> staying = this.staySimulator.newStayEpisode(roundTrip, index + 1, time_h,
						currentState);
				episodes.add(staying);
				time_h = staying.getEndTime_h();
				currentState = staying.getFinalState();
			}

			final MoveEpisode<L> moving = this.moveSimulator.newMoveEpisode(roundTrip,
					roundTrip.locationCnt() - 1, time_h, currentState);
			episodes.add(moving);
			time_h = moving.getEndTime_h();
			currentState = moving.getFinalState();

			final StayEpisode<L> home = this.staySimulator.newStayEpisode(roundTrip, 0, time_h - 24.0,
					currentState);
			episodes.set(0, home);

			final Object newInitialState = this.keepOrChangeInitialState(initialState, home.getFinalState());
			if (newInitialState == initialState) {
				// accept wrap-around
				home.setFinalState(initialState);
			} else {
				// try again
				episodes = null;
				initialState = newInitialState;
			}
		} while (episodes == null);

		return episodes;
	}
}
