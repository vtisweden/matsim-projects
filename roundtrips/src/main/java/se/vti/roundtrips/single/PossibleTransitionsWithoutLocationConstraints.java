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
package se.vti.roundtrips.single;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.vti.roundtrips.model.Scenario;

/**
 * 
 * @author GunnarF
 *
 */
public class PossibleTransitionsWithoutLocationConstraints<L extends Location> implements PossibleTransitions<L> {

	private final Scenario<L> scenario;

	final List<Integer> possibleInsertIndices;
	final List<Integer> possibleRemoveIndices;
	final List<Integer> possibleFlipIndices;

	final List<List<L>> possibleInserts;
	final List<List<L>> possibleFlips;

	final double insertProba;
	final double removeProba;
	final double flipProba;

	public PossibleTransitionsWithoutLocationConstraints(RoundTrip<L> state, Scenario<L> scenario) {
		
		this.scenario = scenario;

		this.possibleInsertIndices = new ArrayList<>(state.locationCnt() + 1);
		this.possibleRemoveIndices = new ArrayList<>(state.locationCnt());
		this.possibleFlipIndices = new ArrayList<>(state.locationCnt());

		this.possibleInserts = new ArrayList<>(state.locationCnt() + 1);
		this.possibleFlips = new ArrayList<>(state.locationCnt());

		double insertIndicator = 0.0;
		double removeIndicator = 0.0;
		double flipIndicator = 0.0;

		for (int i = 0; i < state.locationCnt(); i++) {

			final L curr = state.getLocation(i);

			// analyze inserts

			final List<L> localInserts;
			if (state.locationCnt() == scenario.getMaxParkingEpisodes()) {
				localInserts = Collections.emptyList();
			} else {
				localInserts = new ArrayList<>(scenario.getLocationsView());
			}
			this.possibleInserts.add(localInserts);
			if (localInserts.size() > 0) {
				this.possibleInsertIndices.add(i);
				insertIndicator = 1.0;
			}

			// analyze removes

			if ((state.locationCnt() > 1)) { // && (state.locationCnt() <= 3 || !pred.equals(succ))) {
				this.possibleRemoveIndices.add(i);
				removeIndicator = 1.0;
			}

			// analyze flips

			final List<L> localFlips = new ArrayList<>(scenario.getLocationsView());
			localFlips.remove(curr); // must change!
			this.possibleFlips.add(localFlips);
			if (localFlips.size() > 0) {
				this.possibleFlipIndices.add(i);
				flipIndicator = 1.0;
			}
		}

		// analyze appends-to end of list

		final List<L> lastInserts;
		if (state.locationCnt() == scenario.getMaxParkingEpisodes()) {
			lastInserts = Collections.emptyList();
		} else {
			lastInserts = new ArrayList<>(scenario.getLocationsView());
		}
		this.possibleInserts.add(lastInserts);
		if (lastInserts.size() > 0) {
			this.possibleInsertIndices.add(state.locationCnt());
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
		return list.get(this.scenario.getRandom().nextInt(list.size()));
	}

	public int drawInsertIndex() {
		return this.draw(this.possibleInsertIndices);
	}

	public L drawInsertValue(int index) {
		return this.draw(this.possibleInserts.get(index));
	}

	public int drawRemoveIndex() {
		return this.draw(this.possibleRemoveIndices);
	}

	public int drawFlipIndex() {
		return this.draw(this.possibleFlipIndices);
	}

	public L drawFlipValue(int index) {
		return this.draw(this.possibleFlips.get(index));
	}

	public double concreteInsertProba(int index) {
		return this.insertProba // insert at all
				* (1.0 / this.possibleInsertIndices.size() / this.possibleInserts.get(index).size()) // location
				* (1.0 / (this.scenario.getBinCnt() - this.fromStateLength())); // new depature in unused time slot
	}

	public double concreteRemoveProba() {
		return this.removeProba // remove at all
				* (1.0 / this.possibleRemoveIndices.size()) // location
				* (1.0 / this.fromStateLength()); // remove randomly selected departure
	}

	public double concreteFlipProba(int index) {
		return this.flipProba // flip at all
				* (1.0 / this.possibleFlipIndices.size()) // location
				* (1.0 / this.possibleFlips.get(index).size()); // new value
	}

	@Override
	public double getInsertProba() {
		return this.insertProba;
	}

	@Override
	public double getRemoveProba() {
		return this.removeProba;
	}

	@Override
	public double getFlipProba() {
		return this.flipProba;
	}
}
