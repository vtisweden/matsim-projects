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
package se.vti.roundtrips.single;

import java.util.ArrayList;
import java.util.List;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripLocationProposal<R extends RoundTrip<L>, L> implements MHProposal<R> {

	// -------------------- CONSTANTS --------------------

	private final RoundTripConfiguration<L> config;

	private final List<L> allLocations;
	
	private final PossibleTransitionFactory<L, R> possibleTransitionFactory;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripLocationProposal(RoundTripConfiguration<L> config, PossibleTransitionFactory<L, R> possibleTransitionFactory) {
		this.config = config;
		this.allLocations = new ArrayList<>(config.getAllLocationsView());
		this.possibleTransitionFactory = possibleTransitionFactory;
	}

	// --------------------IMPLEMENTATION OF MHProposal --------------------

	@Override
	public R newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<R> newTransition(R state) {

		final double randomNumber = this.config.getRandom().nextDouble();
		PossibleTransitions<L> fwdActions = this.possibleTransitionFactory.createPossibleTransitions(state, this.config, this.allLocations);

		if (randomNumber < fwdActions.insertProba) {

			// INSERT

			final int whereToInsert = fwdActions.drawInsertIndex();
			final L whatToInsert = fwdActions.drawInsertValue(whereToInsert);
			final Integer newDeparture = RoundTripDepartureProposal.drawUnusedDeparture(state, this.config);
//			final Boolean charging = this.config.getRandom().nextBoolean();

			final R newState = (R) state.clone(); // TODO
//			newState.addAndEnsureSortedDepartures(whereToInsert, whatToInsert, newDeparture, charging);
			newState.addAndEnsureSortedDepartures(whereToInsert, whatToInsert, newDeparture);

			final double fwdLogProba = Math.log(fwdActions.concreteInsertProba(whereToInsert));
			final PossibleTransitions<L> bwdActions = this.possibleTransitionFactory.createPossibleTransitions(newState, this.config,
					this.allLocations);
			final double bwdLogProba = Math.log(bwdActions.concreteRemoveProba());

			return new MHTransition<R>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.insertProba + fwdActions.removeProba) {

			// REMOVE

			final int whereToRemoveLocation = fwdActions.drawRemoveIndex();
			final int whereToRemoveDeparture = this.config.getRandom().nextInt(state.locationCnt());
			final R newState = (R) state.clone(); // TODO
			newState.remove(whereToRemoveLocation, whereToRemoveDeparture);

			final double fwdLogProba = Math.log(fwdActions.concreteRemoveProba());
			final PossibleTransitions<L> bwdActions = this.possibleTransitionFactory.createPossibleTransitions(newState, this.config,
					this.allLocations);
			final double bwdLogProba = Math.log(bwdActions.concreteInsertProba(whereToRemoveLocation));

			return new MHTransition<R>(state, newState, fwdLogProba, bwdLogProba);

		} else if (randomNumber < fwdActions.insertProba + fwdActions.removeProba + fwdActions.flipProba) {

			// FLIP

			final int whereToFlip = fwdActions.drawFlipIndex();
			final L whatToFlip = fwdActions.drawFlipValue(whereToFlip);
			final R newState = (R) state.clone(); // TODO
			newState.setLocation(whereToFlip, whatToFlip);

			final double fwdLogProba = Math.log(fwdActions.concreteFlipProba(whereToFlip));
			final PossibleTransitions<L> bwdActions = this.possibleTransitionFactory.createPossibleTransitions(newState, this.config,
					this.allLocations);
			final double bwdLogProba = Math.log(bwdActions.concreteFlipProba(whereToFlip));

			return new MHTransition<R>(state, newState, fwdLogProba, bwdLogProba);

		} else {

			// should be impossible
			return null;
		}
	}
}
