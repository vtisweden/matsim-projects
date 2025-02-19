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
package se.vti.roundtrips.legacy.proposals;

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripLocationProposal<L extends Location> implements MHProposal<RoundTrip<L>> {

	// -------------------- CONSTANTS --------------------

	private final Scenario<L> scenario;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripLocationProposal(Scenario<L> scenario) {
		this.scenario = scenario;
	}

	// --------------------IMPLEMENTATION OF MHProposal --------------------

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		final double randomNumber = this.scenario.getRandom().nextDouble();
		final PossibleTransitions<L> fwdActions = new PossibleTransitions<>(
				state, this.scenario);

		if (randomNumber < fwdActions.getInsertProba()) {

			// INSERT

			final int whereToInsert = fwdActions.drawInsertIndex();
			final L whatToInsert = fwdActions.drawInsertValue(whereToInsert);
			final Integer newDeparture = RoundTripDepartureProposal.drawUnusedDeparture(state, this.scenario);

			final RoundTrip<L> newState = state.clone();
			newState.addAndEnsureSortedDepartures(whereToInsert, whatToInsert, newDeparture);

			final double fwdLogProba = Math.log(fwdActions.concreteInsertProba(whereToInsert));
			final PossibleTransitions<L> bwdActions = new PossibleTransitions<>(
					newState, this.scenario);
			final double bwdLogProba = Math.log(bwdActions.concreteRemoveProba());

			return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.getInsertProba() + fwdActions.getRemoveProba()) {

			// REMOVE

			final int whereToRemoveLocation = fwdActions.drawRemoveIndex();
			final int whereToRemoveDeparture = this.scenario.getRandom().nextInt(state.locationCnt());
			final RoundTrip<L> newState = state.clone();
			newState.remove(whereToRemoveLocation, whereToRemoveDeparture);

			final double fwdLogProba = Math.log(fwdActions.concreteRemoveProba());
			final PossibleTransitions<L> bwdActions = new PossibleTransitions<>(
					newState, this.scenario);
			final double bwdLogProba = Math.log(bwdActions.concreteInsertProba(whereToRemoveLocation));

			return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.getInsertProba() + fwdActions.getRemoveProba()
				+ fwdActions.getFlipProba()) {

			// FLIP

			final int whereToFlip = fwdActions.drawFlipIndex();
			final L whatToFlip = fwdActions.drawFlipValue(whereToFlip);
			final RoundTrip<L> newState = state.clone();
			newState.setLocation(whereToFlip, whatToFlip);

			final double fwdLogProba = Math.log(fwdActions.concreteFlipProba(whereToFlip));
//			final PossibleTransitions<L> bwdActions = this.possibleTransitionFactory.createPossibleTransitions(newState,
//					this.scenario);
			final PossibleTransitions<L> bwdActions = new PossibleTransitions<>(
					newState, this.scenario);
			final double bwdLogProba = Math.log(bwdActions.concreteFlipProba(whereToFlip));

			return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);

		} else {

			// should be impossible
			return null;
		}
	}
}
