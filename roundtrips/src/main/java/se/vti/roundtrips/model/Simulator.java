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

/**
 * 
 * @author GunnarF
 *
 */
public class Simulator<L extends Location, S extends VehicleState, R extends RoundTrip<L>> implements se.vti.roundtrips.single.Simulator<L, R> {

	// -------------------- INTERFACES --------------------

	public interface ParkingSimulator<L extends Location, S extends VehicleState, R extends RoundTrip<L>> {

		ParkingEpisode<L, S> newParkingEpisode(R roundTrip, int roundTripIndex, double initialTime_h,
				S initialState);
	}

	public interface DrivingSimulator<L extends Location, S extends VehicleState, R extends RoundTrip<L>> {

		DrivingEpisode<L, S> newDrivingEpisode(R roundTrip, int roundTripStartIndex, double initialTime_h,
				S initialState);
	}

	// -------------------- MEMBERS --------------------

	protected final Scenario<L> scenario;
	private final VehicleStateFactory<S> stateFactory;

	private DrivingSimulator<L, S, R> drivingSimulator = null;
	private ParkingSimulator<L, S, R> parkingSimulator = null;

	// -------------------- CONSTRUCTION --------------------

	public Simulator(Scenario<L> scenario, VehicleStateFactory<S> stateFactory) {
		this.scenario = scenario;
		this.stateFactory = stateFactory;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public Scenario<L> getScenario() {
		return this.scenario;
	}

	public void setDrivingSimulator(DrivingSimulator<L, S, R> drivingSimulator) {
		this.drivingSimulator = drivingSimulator;
	}

	public void setParkingSimulator(ParkingSimulator<L, S, R> parkingSimulator) {
		this.parkingSimulator = parkingSimulator;
	}

	// -------------------- HOOKS FOR SUBCLASSING --------------------

	public S createAndInitializeState() {
		return this.stateFactory.createVehicleState();
	}

	public ParkingEpisode<L, S> createHomeOnlyEpisode(R roundTrip) {
		ParkingEpisode<L, S> home = new ParkingEpisode<>(roundTrip.getLocation(0));
		home.setDuration_h(24.0);
		home.setEndTime_h(24.0 - 1e-8); // wraparound
		home.setInitialState(this.createAndInitializeState());
		home.setFinalState(this.createAndInitializeState());
		return home;
	}

	public S keepOrChangeInitialState(S oldInitialState, S newInitialState) {
		return oldInitialState;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public List<Episode<S>> simulate(R roundTrip) {

		if (roundTrip.locationCnt() == 1) {
			return Collections.singletonList(this.createHomeOnlyEpisode(roundTrip));
		}

		final double initialTime_h = this.scenario.getBinSize_h() * roundTrip.getDeparture(0);
		S initialState = this.createAndInitializeState();

		List<Episode<S>> episodes = null;
		do {

			episodes = new ArrayList<>(2 * roundTrip.locationCnt() - 1);
			episodes.add(null); // placeholder for home episode

			double time_h = initialTime_h;
			S currentState = initialState;

			for (int index = 0; index < roundTrip.locationCnt() - 1; index++) {

				final DrivingEpisode<L, S> driving = this.drivingSimulator.newDrivingEpisode(roundTrip, index, time_h,
						currentState);
				episodes.add(driving);
				time_h = driving.getEndTime_h();
				currentState = driving.getFinalState();

				final ParkingEpisode<L, S> parking = this.parkingSimulator.newParkingEpisode(roundTrip, index + 1,
						time_h, currentState);
				episodes.add(parking);
				time_h = parking.getEndTime_h();
				currentState = parking.getFinalState();
			}

			final DrivingEpisode<L, S> driving = this.drivingSimulator.newDrivingEpisode(roundTrip,
					roundTrip.locationCnt() - 1, time_h, currentState);
			episodes.add(driving);
			time_h = driving.getEndTime_h();
			currentState = driving.getFinalState();

			final ParkingEpisode<L, S> home = this.parkingSimulator.newParkingEpisode(roundTrip, 0, time_h - 24.0,
					currentState);
			episodes.set(0, home);

			final S newInitialState = this.keepOrChangeInitialState(initialState, home.getFinalState());
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
