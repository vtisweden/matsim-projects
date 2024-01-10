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
package se.vti.skellefteaV2X.roundtrips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripLocationProposal<L> implements MHProposal<RoundTrip<L>> {

	// -------------------- CONSTANTS --------------------

	private final RoundTripConfiguration<L> scenario;

	private final List<L> allLocations;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripLocationProposal(RoundTripConfiguration<L> scenario) {
		this.scenario = scenario;
		this.allLocations = new ArrayList<>(scenario.getAllLocationsView());
	}

	// -------------------- INTERNALS --------------------

	class PossibleTransitions {

		final List<Integer> possibleInsertIndices;
		final List<Integer> possibleRemoveIndices;
		final List<Integer> possibleFlipIndices;

		final List<List<L>> possibleInserts;
		final List<List<L>> possibleFlips;

		final double insertProba;
		final double removeProba;
		final double flipProba;

		PossibleTransitions(RoundTrip<L> state) {

			this.possibleInsertIndices = new ArrayList<>(state.size() + 1);
			this.possibleRemoveIndices = new ArrayList<>(state.size());
			this.possibleFlipIndices = new ArrayList<>(state.size());

			this.possibleInserts = new ArrayList<>(state.size() + 1);
			this.possibleFlips = new ArrayList<>(state.size());

			double insertIndicator = 0.0;
			double removeIndicator = 0.0;
			double flipIndicator = 0.0;

			for (int i = 0; i < state.size(); i++) {

				final L pred = state.getPredecessorLocation(i);
				final L curr = state.getLocation(i);
				final L succ = state.getSuccessorLocation(i);

				// analyze inserts

				final List<L> localInserts;
				if (state.size() == scenario.getMaxLocations()) {
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
				localFlips.remove(curr); // must change!
				if (state.size() > 1) {
					localFlips.remove(pred);
					localFlips.remove(succ);
				}
				this.possibleFlips.add(localFlips);
				if (localFlips.size() > 0) {
					this.possibleFlipIndices.add(i);
					flipIndicator = 1.0;
				}
			}

			// analyze appends-to end of list

			final List<L> lastInserts;
			if (state.size() == scenario.getMaxLocations()) {
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

			assert (insertIndicator + removeIndicator + flipIndicator > 0);
			this.insertProba = insertIndicator / (insertIndicator + removeIndicator + flipIndicator);
			this.removeProba = removeIndicator / (insertIndicator + removeIndicator + flipIndicator);
			this.flipProba = flipIndicator / (insertIndicator + removeIndicator + flipIndicator);
		}

		private int fromStateLength() {
			return this.possibleFlips.size();
		}

		private <X> X draw(List<X> list) {
			return list.get(scenario.getRandom().nextInt(list.size()));
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
			return this.insertProba // insert at all
					* (1.0 / this.possibleInsertIndices.size() / this.possibleInserts.get(index).size()) // location
					* (1.0 / (scenario.getTimeBinCnt() - this.fromStateLength())) // new depature in unused time slot
					* 0.5; // charging
		}

		double concreteRemoveProba() {
			return this.removeProba // remove at all
					* (1.0 / this.possibleRemoveIndices.size()) // location
					// removal is unconditional on charging
					* (1.0 / this.fromStateLength()); // remove randomly selected departure
		}

		double concreteFlipProba(int index) {
			return this.flipProba // flip at all
					* (1.0 / this.possibleFlipIndices.size()) // location
					* (1.0 / this.possibleFlips.get(index).size()); // new value
			// flip is unconditional on departure or charging
		}
	}

	// --------------------IMPLEMENTATION OF MHProposal --------------------

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		final double randomNumber = this.scenario.getRandom().nextDouble();
		PossibleTransitions fwdActions = new PossibleTransitions(state);

		if (randomNumber < fwdActions.insertProba) {

			// INSERT

			final int whereToInsert = fwdActions.drawInsertIndex();
			final L whatToInsert = fwdActions.drawInsertValue(whereToInsert);
			final Integer newDeparture = RoundTripDepartureProposal.drawUnusedDeparture(state, this.scenario);
			final Boolean charging = this.scenario.getRandom().nextBoolean();

			final RoundTrip<L> newState = state.deepCopy();
			newState.addAndEnsureSortedDepartures(whereToInsert, whatToInsert, newDeparture, charging);

			final double fwdLogProba = Math.log(fwdActions.concreteInsertProba(whereToInsert));
			final PossibleTransitions bwdActions = new PossibleTransitions(newState);
			final double bwdLogProba = Math.log(bwdActions.concreteRemoveProba());

			return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.insertProba + fwdActions.removeProba) {

			// REMOVE

			final int whereToRemoveLocation = fwdActions.drawRemoveIndex();
			final int whereToRemoveDeparture = this.scenario.getRandom().nextInt(state.size());
			final RoundTrip<L> newState = state.deepCopy();
			newState.remove(whereToRemoveLocation, whereToRemoveDeparture);

			final double fwdLogProba = Math.log(fwdActions.concreteRemoveProba());
			final PossibleTransitions bwdActions = new PossibleTransitions(newState);
			final double bwdLogProba = Math.log(bwdActions.concreteInsertProba(whereToRemoveLocation));

			return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.insertProba + fwdActions.removeProba + fwdActions.flipProba) {

			// FLIP

			final int whereToFlip = fwdActions.drawFlipIndex();
			final L whatToFlip = fwdActions.drawFlipValue(whereToFlip);
			final RoundTrip<L> newState = state.deepCopy();
			newState.setLocation(whereToFlip, whatToFlip);
			
			final double fwdLogProba = Math.log(fwdActions.concreteFlipProba(whereToFlip));
			final PossibleTransitions bwdActions = new PossibleTransitions(newState);
			final double bwdLogProba = Math.log(bwdActions.concreteFlipProba(whereToFlip));

			return new MHTransition<RoundTrip<L>>(state, newState, fwdLogProba, bwdLogProba);

		} else {

			// should be impossible
			return null;
		}
	}
}
