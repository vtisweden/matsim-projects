/**
 * se.vti.roundtrips.single
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
class PossibleTransitions_DEPRECATED<L extends Location> {

	private final RoundTrip<L> fromState;

	private final Scenario<L> scenario;

	final List<Integer> possibleInserts;
	final List<Integer> possibleRemoves;
	final List<Integer> possibleFlips;

	final double insertProba;
	final double removeProba;
	final double flipProba;

	PossibleTransitions_DEPRECATED(RoundTrip<L> state, Scenario<L> scenario) {

		this.fromState = state;
		this.scenario = scenario;

		if (state.locationCnt() < Math.min(scenario.getMaxStayEpisodes(), scenario.getBinCnt() - 1)) {
			this.possibleInserts = IntStream.rangeClosed(0, state.locationCnt()).boxed().toList();
		} else {
			this.possibleInserts = Collections.emptyList();
		}

		if (state.locationCnt() > 1) {
			this.possibleRemoves = IntStream.range(0, state.locationCnt()).boxed().toList();
		} else {
			this.possibleRemoves = Collections.emptyList();
		}

		this.possibleFlips = IntStream.range(0, state.locationCnt()).boxed().toList();

		final double insertIndicator = (this.possibleInserts.size() > 0 ? 1.0 : 0.0);
		final double removeIndicator = (this.possibleRemoves.size() > 0 ? 1.0 : 0.0);
		final double flipIndicator = (this.possibleFlips.size() > 0 ? 1.0 : 0.0);

		// derived quantities

		assert (insertIndicator + removeIndicator + flipIndicator > 0);
		this.insertProba = insertIndicator / (insertIndicator + removeIndicator + flipIndicator);
		this.removeProba = removeIndicator / (insertIndicator + removeIndicator + flipIndicator);
		this.flipProba = flipIndicator / (insertIndicator + removeIndicator + flipIndicator);
	}

	private <X> X draw(List<X> list) {
		return list.get(this.scenario.getRandom().nextInt(list.size()));
	}

	double getInsertProba() {
		return this.insertProba;
	}

	double getRemoveProba() {
		return this.removeProba;
	}

	double getFlipProba() {
		return this.flipProba;
	}

	int drawInsertIndex() {
		return this.draw(this.possibleInserts);
	}

	int drawRemoveIndex() {
		return this.draw(this.possibleRemoves);
	}

	int drawFlipIndex() {
		return this.draw(this.possibleFlips);
	}

	L drawInsertValue(int index) {
		return this.draw(this.scenario.getLocationsView());
	}

	L drawFlipValue(int index) {
		while (true) {
			L newValue = this.draw(this.scenario.getLocationsView());
			if (!newValue.equals(this.fromState.getLocation(index))) {
				return newValue;
			}
		}
	}

	double concreteInsertProba(int index) {
		return this.insertProba // insert at all
				* (1.0 / (this.fromState.locationCnt() + 1)) // where in the plan to insert
				* (1.0 / (this.scenario.getBinCnt() - this.fromState.locationCnt())) // which timeslot
				* (1.0 / this.scenario.getLocationCnt()); // which location
	}

	double concreteRemoveProba() {
		return this.removeProba // remove at all
				* (1.0 / this.fromState.locationCnt()) // which location to remove
				* (1.0 / this.fromState.locationCnt()); // which timeslot to remove
	}

	double concreteFlipProba(int index) {
		return this.flipProba // flip at all
				* (1.0 / this.fromState.locationCnt()) // where in the plan to flip
				* (1.0 / this.scenario.getLocationCnt() - 1); // to what new (and different) location
	}
}
