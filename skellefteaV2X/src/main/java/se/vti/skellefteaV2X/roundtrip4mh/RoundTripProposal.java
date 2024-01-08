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
package se.vti.skellefteaV2X.roundtrip4mh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * TODO Deal with infeasible transitions (empty chains, max length, ...)
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripProposal<L> implements MHProposal<RoundTrip<L>> {

	private final Random rnd = new Random();

	private final double insertProba = 1.0 / 3.0;
	private final double removeProba = 1.0 / 3.0;
	private final double flipProba = 1.0 / 3.0;

	private final double retimeProba = 1.0 - this.insertProba - this.removeProba; // TODO test first without times
	private final int maxLength = 4;

	private final long maxRejects = 1000l * 1000l;

	private final RoundTripScenario<L> scenario;

	private final List<L> allLocations;

	public RoundTripProposal(RoundTripScenario<L> scenario) {
		this.scenario = scenario;
		this.allLocations = Collections.unmodifiableList(new ArrayList<>(scenario.getAllLocations()));
	}

	// INTERNALS

	private L randomLocation() {
		return this.allLocations.get(this.rnd.nextInt(this.allLocations.size()));
	}

	private int predecessorIndex(int index, int size) {
		if (index > 0) {
			return index - 1;
		} else {
			return size - 1;
		}
	}

	private int successorIndex(int index, int size) {
		if (index < size - 1) {
			return index + 1;
		} else {
			return 0;
		}
	}

	private List<Integer> feasibleRemovalIndices(RoundTrip<L> state) {
		final ArrayList<Integer> result = new ArrayList<>(state.size());
		if (state.size() == 1) {
			return result;
		}
		for (int i = 0; i < state.size(); i++) {
			if ((state.size() <= 3) || !state.getLocation(predecessorIndex(i, state.size()))
					.equals(state.getLocation(successorIndex(i, state.size())))) {
				result.add(i);
			}
		}
		return result;
	}

	private long numberOfPossibleInsertions(RoundTrip<L> state) {
		long result = 0;
		for (int whereToInsert = 0; whereToInsert < state.size(); whereToInsert++) {
			final L newPred = state.getLocation(predecessorIndex(whereToInsert, state.size()));
			final L newSucc = state.getLocation(whereToInsert);
			result += this.allLocations.size() - (newPred.equals(newSucc) ? 1 : 2);
		}
		return result;
	}

	@Override
	public RoundTrip<L> newInitialState() {
		return new RoundTrip<L>(Arrays.asList(this.randomLocation()),
				Arrays.asList(this.scenario.getAnalysisPeriod_s()));
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		long rejects = 0;

		while (rejects < this.maxRejects) {

			final double randomNumber = this.rnd.nextDouble();
			if (randomNumber < this.insertProba) {

				// INSERT

				if (state.size() < this.maxLength) {
					final RoundTrip<L> newState = state.deepCopy();

					final int whereToInsert = this.rnd.nextInt(state.size());
					final L newPred = newState.getLocation(predecessorIndex(whereToInsert, state.size()));
					final L newSucc = newState.getLocation(whereToInsert);
					L whatToInsert = null;
					while (whatToInsert == null) {
						L cand = this.randomLocation();
						if (!cand.equals(newPred) && !cand.equals(newSucc)) {
							whatToInsert = cand;
						}
					}
					newState.add(whereToInsert, whatToInsert, 0.0);
					final double fwdLogProba = Math.log(this.insertProba)
							+ Math.log(1.0 / this.numberOfPossibleInsertions(state));

					final int feasibleRemovalLocations = this.feasibleRemovalIndices(newState).size();
					final double bwdLogProba = Math.log(this.removeProba) + Math.log(1.0 / feasibleRemovalLocations);

					return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);
				}

			} else if (randomNumber < this.insertProba + this.removeProba) {

				// REMOVE

				final List<Integer> feasibleRemovalLocations = this.feasibleRemovalIndices(state);
				if (feasibleRemovalLocations.size() > 0) {

					final RoundTrip<L> newState = state.deepCopy();

					final int removalLocation = feasibleRemovalLocations
							.get(this.rnd.nextInt(feasibleRemovalLocations.size()));

					newState.remove(removalLocation);
					final double fwdLogProba = Math.log(this.removeProba)
							+ Math.log(1.0 / feasibleRemovalLocations.size());

					final double bwdLogProba = Math.log(this.insertProba)
							+ Math.log(1.0 / numberOfPossibleInsertions(state));

					return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);
				}

			} else if (randomNumber < this.insertProba + this.removeProba + this.flipProba) {

				// FLIP

				final int flipIndex = this.rnd.nextInt(state.size());
				final L current = state.getLocation(flipIndex);
				final L pred = state.getLocation(predecessorIndex(flipIndex, state.size()));
				final L succ = state.getLocation(successorIndex(flipIndex, state.size()));

				L whereToFlip = null;
				while (whereToFlip == null) {
					L cand = this.randomLocation();
					if (!cand.equals(pred) && !cand.equals(succ)) {
						whereToFlip = cand;
					}
				}

				final RoundTrip<L> newState = state.deepCopy();
				if (!whereToFlip.equals(current)) {
					newState.remove(flipIndex);
					newState.add(flipIndex, whereToFlip, 0.0);
				}

				// uniform selection from previous set (different from neighbors); this is symmetric
				return new MHTransition<RoundTrip<L>>(state, newState, 0.0, 0.0);

			} else {

				// RETIME

				// TODO ignore for the time being
			}

			rejects++;
		}

		throw new RuntimeException("Could not find a feasible transition within " + this.maxRejects + " trials.");
	}
}
