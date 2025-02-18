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
package se.vti.roundtrips.parallel;

import java.util.List;
import java.util.Random;

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.Simulator;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * TODO Deal with infeasible transitions (empty chains, max length, ...)
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class SimplifiedRoundTripProposal<L extends Location> implements MHProposal<RoundTrip<L>> {

	private final Simulator<L> simulator;
	private final Scenario<L> scenario;

	private final List<L> allLocations;

	private double weight_INS = 1.0;
	private double weight_REM = 1.0;
	private double weight_FLIP_LOC = 1.0;
	private double weight_FLIP_DEP = 1.0;

	public SimplifiedRoundTripProposal(Scenario<L> scenario, Simulator<L> simulator, Random rnd) {
		this.simulator = simulator;
		this.scenario = scenario;
		this.allLocations = scenario.getLocationsView();
	}

	public Simulator<L> getSimulator() {
		return this.simulator;
	}

	// INTERNALS

	public class TransitionKernel {

		private final RoundTrip<L> from;

		private final double proba_INS;
		private final double proba_REM;
		private final double proba_FLIP_LOC;
		private final double proba_FLIP_DEP;

		public TransitionKernel(RoundTrip<L> from) {
			this.from = from;

			double effWeight_INS = (from.locationCnt() < Math.min(scenario.getMaxStayEpisodes(),
					scenario.getBinCnt() - 1) ? weight_INS : 0.0);
			double effWeight_REM = (from.locationCnt() > 1 ? weight_REM : 0.0);
			double effWeight_FLIP_LOC = weight_FLIP_LOC;
			double effWeight_FLIP_DEP = weight_FLIP_DEP;
			final double effWeightSum = effWeight_INS + effWeight_REM + effWeight_FLIP_LOC + effWeight_FLIP_DEP;

			this.proba_INS = effWeight_INS / effWeightSum;
			this.proba_REM = effWeight_REM / effWeightSum;
			this.proba_FLIP_LOC = effWeight_FLIP_LOC / effWeightSum;
			this.proba_FLIP_DEP = effWeight_FLIP_DEP / effWeightSum;
		}

		public double transitionProbaGivenInsert(RoundTrip<?> to) {
			return 1.0 / (this.from.locationCnt() + 1.0) / scenario.getLocationCnt()
					/ (scenario.getBinCnt() - from.locationCnt());
		}

		public double transitionProbaGivenRemove(RoundTrip<?> to) {
			return 1.0 / this.from.locationCnt();
		}

		public double transitionProbaGivenFlipLocation(RoundTrip<?> to) {
			return 1.0 / this.from.locationCnt() / (scenario.getLocationCnt() - 1);
		}

		public double transitionProbaGivenFlipDepartureTime(RoundTrip<?> to) {
			return 1.0 / this.from.locationCnt() / (scenario.getBinCnt() - from.locationCnt());
		}

		public double transitionProba(RoundTrip<?> to) {
			if (this.from.locationCnt() + 1 == to.locationCnt()) { // was INSERT
				return this.proba_INS * this.transitionProbaGivenInsert(to);
			} else if (from.locationCnt() == 1 + to.locationCnt()) { // was REMOVE
				return this.proba_REM * this.transitionProbaGivenRemove(to);
			} else if (!from.getLocationsView().equals(to.getLocationsView())) { // was FLIP LOCATION
				return this.proba_FLIP_LOC * this.transitionProbaGivenFlipLocation(to);
			} else if (!from.getDeparturesView().equals(to.getDeparturesView())) { // was FLIP DEPARTURE
				return this.proba_FLIP_DEP / this.transitionProbaGivenFlipDepartureTime(to);
			} else { // was IMPOSSIBLE
				return 0.0;
			}
		}

	}

	private L drawLocationDifferentFrom(L notThisOne) {
		while (true) {
			final L location = this.allLocations.get(this.scenario.getRandom().nextInt(0, this.allLocations.size()));
			if (!location.equals(notThisOne)) {
				return location;
			}
		}
	}

	private Integer drawUnusedDepartureTime(RoundTrip<?> state) {
		// TODO Not ideal, handle this upstream:
		if (state.locationCnt() == scenario.getBinCnt()) {
			throw new RuntimeException("No space for inserting a departure time bin.");
		}
		while (true) {
			final Integer dpt = this.scenario.getRandom().nextInt(this.scenario.getBinCnt());
			if (!state.containsDeparture(dpt)) {
				return dpt;
			}
		}
	}

	// IMPLEMENTATION OF INTERFACE

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> oldState) {

		final Random rnd = this.scenario.getRandom();

		double effWeight_INS = (oldState.locationCnt() < Math.min(this.scenario.getMaxStayEpisodes(),
				this.scenario.getBinCnt() - 1) ? this.weight_INS : 0.0);
		double effWeight_REM = (oldState.locationCnt() > 1 ? this.weight_REM : 0.0);
		double effWeight_FLIP_LOC = this.weight_FLIP_LOC;
		double effWeight_FLIP_DEP = this.weight_FLIP_DEP;

		final double effWeightSum = effWeight_INS + effWeight_REM + effWeight_FLIP_LOC + effWeight_FLIP_DEP;
		assert (effWeightSum > 0.0);
		final double randomWeight = effWeightSum * rnd.nextDouble();

		final RoundTrip<L> newState = oldState.clone();
		if (randomWeight < effWeight_INS) {
			// INSERT

			final int whereToInsert = rnd.nextInt(0, oldState.locationCnt() + 1);
			final L whatLocationToInsert = this.allLocations.get(rnd.nextInt(0, this.allLocations.size()));
			final Integer whatDptTimeToInsert = this.drawUnusedDepartureTime(oldState);
			newState.addAndEnsureSortedDepartures(whereToInsert, whatLocationToInsert, whatDptTimeToInsert);

		} else if (randomWeight < effWeight_INS + effWeight_REM) {
			// REMOVE

			final int whereToRemove = rnd.nextInt(0, oldState.locationCnt());
			newState.remove(whereToRemove, whereToRemove);

		} else if (randomWeight < effWeight_INS + effWeight_REM + effWeight_FLIP_LOC) {
			// FLIP LOCATION

			final int whereToFlip = rnd.nextInt(0, oldState.locationCnt());
			final L newLocation = this.drawLocationDifferentFrom(oldState.getLocation(whereToFlip));
			newState.setLocation(whereToFlip, newLocation);

		} else {
			// FLIP DEPARTURE TIME

			final int whereToFlip = rnd.nextInt(0, oldState.locationCnt());
			final Integer newDptTime = this.drawUnusedDepartureTime(oldState);
			newState.setDepartureAndEnsureOrdering(whereToFlip, newDptTime);
		}

		final TransitionKernel fwdTransitionKernel = new TransitionKernel(oldState);
		final TransitionKernel bwdTransitionKernel = new TransitionKernel(newState);

		return new MHTransition<>(oldState, newState, Math.log(fwdTransitionKernel.transitionProba(newState)),
				bwdTransitionKernel.transitionProba(oldState));
	}
}
