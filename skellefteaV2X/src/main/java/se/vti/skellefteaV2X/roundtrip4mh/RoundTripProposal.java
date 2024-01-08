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

	private final double insertProba = 0.5;
	private final double removeProba = 0.5;
	private final double retimeProba = 1.0 - this.insertProba - this.removeProba; // TODO test first without times

	private final int maxLength = 5;

	private final long maxRejects = 1000l * 1000l;

	private final RoundTripScenario<L> scenario;

	private final List<L> allLocations;

	public RoundTripProposal(RoundTripScenario<L> scenario) {
		this.scenario = scenario;
		this.allLocations = Collections.unmodifiableList(new ArrayList<>(scenario.getAllLocations()));
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

	private L randomLocation() {
		return this.allLocations.get(this.rnd.nextInt(this.allLocations.size()));
	}

	private List<Integer> feasibleRemovalLocations(RoundTrip<L> state) {
		final ArrayList<Integer> result = new ArrayList<>(state.size());
		for (int i = 0; i < state.size(); i++) {
			if ((state.size() <= 2) || !state.getLocation(predecessorIndex(i, state.size()))
					.equals(state.getLocation(successorIndex(i, state.size())))) {
				result.add(i);
			}
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
					final RoundTrip<L> newState = state.deepCopy(); // to be modified

					final int whereToInsert = this.rnd.nextInt(state.size());
					L whatToInsert = null;
					final L pred = newState.getLocation(predecessorIndex(whereToInsert, state.size()));
					final L succ = newState.getLocation(whereToInsert);
					while (whatToInsert == null) {
						L cand = this.randomLocation();
						if (!cand.equals(pred) && !cand.equals(succ)) {
							whatToInsert = cand;
						}
					}
					newState.add(whereToInsert, whatToInsert, 0.0);
					final double fwdLogProba = Math.log(this.insertProba) + Math.log(1.0 / state.size())
							+ Math.log(1.0 / (this.allLocations.size() - 2.0));

					int feasibleRemovalLocations = 0;
					for (int i = 0; i < newState.size(); i++) {
						if (newState.getLocation(predecessorIndex(i, newState.size()))
								.equals(newState.getLocation(successorIndex(i, newState.size())))) {
							feasibleRemovalLocations++;
						}
					}
					final double bwdLogProba = Math.log(this.removeProba) + Math.log(1.0 / feasibleRemovalLocations);

					return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);
				}

			} else if (randomNumber < this.insertProba + this.removeProba) {

				// REMOVE

				if (state.size() > 1) {

					final List<Integer> feasibleRemovalLocations = this.feasibleRemovalLocations(state);
					if (feasibleRemovalLocations.size() > 0) {

						final RoundTrip<L> newState = state.deepCopy(); // to be modified

						final int removalLocation = feasibleRemovalLocations
								.get(this.rnd.nextInt(feasibleRemovalLocations.size()));
						newState.remove(removalLocation);
						final double fwdLogProba = Math.log(this.removeProba)
								+ Math.log(1.0 / feasibleRemovalLocations.size());

						final double bwdLogProba = Math.log(this.insertProba) + Math.log(1.0 / newState.size())
								+ Math.log(1.0 / (this.allLocations.size() - 2.0));

						return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);
					}
				}

			} else {

				// RETIME

				// TODO ignore for the time being
			}

			rejects++;
		}

		throw new RuntimeException("Could not find a feasible transition within " + this.maxRejects + " trials.");
	}
}
