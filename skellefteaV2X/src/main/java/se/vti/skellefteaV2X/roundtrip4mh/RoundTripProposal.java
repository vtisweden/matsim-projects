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

	private final int maxLength = 4;

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

	private class PossibleTransitions {

		private List<List<L>> possibleInserts;
		private List<List<L>> possibleFlips;
		private List<Integer> possibleInsertIndices;
		private List<Integer> possibleRemoveIndices;
		private List<Integer> possibleFlipIndices;

		PossibleTransitions(RoundTrip<L> state) {

			this.possibleInserts = new ArrayList<>(state.size() + 1);
			this.possibleFlips = new ArrayList<>(state.size());
			this.possibleInsertIndices = new ArrayList<>(state.size() + 1);
			this.possibleRemoveIndices = new ArrayList<>(state.size());
			this.possibleFlipIndices = new ArrayList<>(state.size());

			for (int i = 0; i < state.size(); i++) {

				final L pred = state.getPredecessorLocation(i);
				final L curr = state.getLocation(i);
				final L succ = state.getSuccessorLocation(i);

				// analyze inserts

				final List<L> localInserts;
				if (state.size() == maxLength) {
					localInserts = Collections.emptyList();
				} else {
					localInserts = new ArrayList<>(allLocations);
					localInserts.remove(pred);
					localInserts.remove(curr);
				}
				this.possibleInserts.add(localInserts);
				if (localInserts.size() > 0) {
					this.possibleInsertIndices.add(i);
				}

				// analyze removes

				if ((state.size() > 1) && (state.size() <= 3 || !pred.equals(succ))) {
					this.possibleRemoveIndices.add(i);
				}

				// analyze flips

				final List<L> localFlips = new ArrayList<>(allLocations);
				if (state.size() > 1) {
					localFlips.remove(pred);
					localFlips.remove(curr); // must change!
					localFlips.remove(succ);
				}
				this.possibleFlips.add(localFlips);
				if (localFlips.size() > 0) {
					this.possibleFlipIndices.add(i);
				}
			}

			// analyze append-inserts at end of list

			final List<L> lastInserts;
			if (state.size() == maxLength) {
				lastInserts = Collections.emptyList();
			} else {
				lastInserts = new ArrayList<>(allLocations);
				lastInserts.remove(state.getLocation(state.size() - 1));
				lastInserts.remove(state.getLocation(0));
			}
			this.possibleInserts.add(lastInserts);
			if (lastInserts.size() > 0) {
				this.possibleInsertIndices.add(state.size());
			}
		}

		boolean canInsert() {
			return this.possibleInsertIndices.size() > 0;
		}

		boolean canRemove() {
			return this.possibleRemoveIndices.size() > 0;
		}

		boolean canFlip() {
			return this.possibleFlipIndices.size() > 0;
		}

		double insertProba() {
			return (canInsert() ? 1. : 0.)
					/ ((canInsert() ? 1. : 0.) + (canRemove() ? 1. : 0.) + (canFlip() ? 1. : 0.));
		}

		double removeProba() {
			return (canRemove() ? 1. : 0.)
					/ ((canInsert() ? 1. : 0.) + (canRemove() ? 1. : 0.) + (canFlip() ? 1. : 0.));
		}

		double flipProba() {
			return (canFlip() ? 1. : 0.) / ((canInsert() ? 1. : 0.) + (canRemove() ? 1. : 0.) + (canFlip() ? 1. : 0.));
		}

		private <X> X draw(List<X> list) {
			return list.get(rnd.nextInt(list.size()));
		}

		int drawInsertIndex() {
			return this.draw(this.possibleInsertIndices);
		}

		L drawInsertValue(int index) {
			return this.draw(this.possibleInserts.get(index));
		}

		int drawRemoveIndex() {
			return this.draw(this.possibleRemoveIndices);
		}

		int drawFlipIndex() {
			return this.draw(this.possibleFlipIndices);
		}

		L drawFlipValue(int index) {
			return this.draw(this.possibleFlips.get(index));
		}

		double concreteInsertProba(int index) {
			return this.insertProba() * 1.0 / this.possibleInsertIndices.size()
					/ this.possibleInserts.get(index).size();
		}

		double concreteRemoveProba() {
			return this.removeProba() * 1.0 / this.possibleRemoveIndices.size();
		}

		double concreteFlipProba(int index) {
			return this.flipProba() * 1.0 / this.possibleFlipIndices.size() / this.possibleFlips.get(index).size();
		}

	}

	@Override
	public RoundTrip<L> newInitialState() {
		return new RoundTrip<L>(Arrays.asList(this.randomLocation()),
				Arrays.asList(this.scenario.getAnalysisPeriod_s()));
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		final double randomNumber = this.rnd.nextDouble();
		PossibleTransitions fwdActions = new PossibleTransitions(state);

		if (randomNumber < fwdActions.insertProba()) {

			// INSERT

			final int whereToInsert = fwdActions.drawInsertIndex();
			final L whatToInsert = fwdActions.drawInsertValue(whereToInsert);
			final RoundTrip<L> newState = state.deepCopy();
			newState.add(whereToInsert, whatToInsert, 0.0);
			final double fwdLogProba = Math.log(fwdActions.concreteInsertProba(whereToInsert));

			final PossibleTransitions bwdActions = new PossibleTransitions(newState);
			final double bwdLogProba = Math.log(bwdActions.concreteRemoveProba());

			return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.insertProba() + fwdActions.removeProba()) {

			// REMOVE

			final int whereToRemove = fwdActions.drawRemoveIndex();
			final RoundTrip<L> newState = state.deepCopy();
			newState.remove(whereToRemove);
			final double fwdLogProba = Math.log(fwdActions.concreteRemoveProba());

			final PossibleTransitions bwdActions = new PossibleTransitions(newState);
			final double bwdLogProba = Math.log(bwdActions.concreteInsertProba(whereToRemove));

			return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.insertProba() + fwdActions.removeProba() + fwdActions.flipProba()) {

			// FLIP

			final int whereToFlip = fwdActions.drawFlipIndex();
			final L whatToFlip = fwdActions.drawFlipValue(whereToFlip);

			final RoundTrip<L> newState = state.deepCopy();
			newState.remove(whereToFlip);
			newState.add(whereToFlip, whatToFlip, 0.0);
			final double fwdLogProba = Math.log(fwdActions.concreteFlipProba(whereToFlip));

			final PossibleTransitions bwdActions = new PossibleTransitions(newState);
			final double bwdLogProba = Math.log(bwdActions.concreteFlipProba(whereToFlip));

			return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);

		} else {

			// RETIME

			throw new RuntimeException();
			// TODO ignore for the time being
		}

	}
}
