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

	private final RoundTripScenario<L> scenario;

	private final List<L> allLocations;

	public RoundTripProposal(RoundTripScenario<L> scenario) {
		this.scenario = scenario;
		this.allLocations = scenario.getAllLocationsListView();
	}

	// INTERNALS

	private class PossibleTransitions {

		private List<List<L>> possibleInserts;
		private List<List<L>> possibleFlips;
		private List<Integer> possibleInsertIndices;
		private List<Integer> possibleRemoveIndices;
		private List<Integer> possibleFlipIndices;

		private final double insertProba;
		private final double removeProba;
		private final double flipProba;

		PossibleTransitions(RoundTrip<L> state) {

			this.possibleInserts = new ArrayList<>(state.size() + 1);
			this.possibleFlips = new ArrayList<>(state.size());
			this.possibleInsertIndices = new ArrayList<>(state.size() + 1);
			this.possibleRemoveIndices = new ArrayList<>(state.size());
			this.possibleFlipIndices = new ArrayList<>(state.size());

			double insertIndicator = 0.0;
			double removeIndicator = 0.0;
			double flipIndicator = 0.0;

			for (int i = 0; i < state.size(); i++) {

				final L pred = state.getPredecessorLocation(i);
				final L curr = state.getLocation(i);
				final L succ = state.getSuccessorLocation(i);

				// analyze inserts

				final List<L> localInserts;
				if (state.size() == scenario.getMaxLength()) {
					localInserts = Collections.emptyList();
				} else {
					localInserts = new ArrayList<>(allLocations);
					localInserts.remove(pred);
					localInserts.remove(curr);
				}
				this.possibleInserts.add(localInserts);
				if (localInserts.size() > 0) {
					this.possibleInsertIndices.add(i);
					insertIndicator = 1.0;
				}

				// analyze removes

				if ((state.size() > 1) && (state.size() <= 3 || !pred.equals(succ))) {
					this.possibleRemoveIndices.add(i);
					removeIndicator = 1.0;
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
					flipIndicator = 1.0;
				}
			}

			// analyze append-inserts at end of list

			final List<L> lastInserts;
			if (state.size() == scenario.getMaxLength()) {
				lastInserts = Collections.emptyList();
			} else {
				lastInserts = new ArrayList<>(allLocations);
				lastInserts.remove(state.getLocation(state.size() - 1));
				lastInserts.remove(state.getLocation(0));
			}
			this.possibleInserts.add(lastInserts);
			if (lastInserts.size() > 0) {
				this.possibleInsertIndices.add(state.size());
				insertIndicator = 1.0;
			}

			// derived quantities

			this.insertProba = insertIndicator / (insertIndicator + removeIndicator + flipIndicator);
			this.removeProba = removeIndicator / (insertIndicator + removeIndicator + flipIndicator);
			this.flipProba = flipIndicator / (insertIndicator + removeIndicator + flipIndicator);

		}

		double insertProba() {
			return this.insertProba;
		}

		double removeProba() {
			return this.removeProba;
		}

		double flipProba() {
			return this.flipProba;
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
			return this.insertProba() / this.possibleInsertIndices.size() / this.possibleInserts.get(index).size();
		}

		double concreteRemoveProba() {
			return this.removeProba() / this.possibleRemoveIndices.size();
		}

		double concreteFlipProba(int index) {
			return this.flipProba() / this.possibleFlipIndices.size() / this.possibleFlips.get(index).size();
		}
	}

	// IMPLEMENTATION OF INTERFACE

	@Override
	public RoundTrip<L> newInitialState() {
		return new RoundTrip<L>(Arrays.asList(this.allLocations.get(this.rnd.nextInt(this.allLocations.size()))),
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
			
			// TODO other than round trip operations
			return null;
		}
	}
}
