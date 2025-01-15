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

import java.util.Random;

import org.checkerframework.common.returnsreceiver.qual.This;

import se.vti.roundtrips.model.Scenario;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripDepartureProposal<L extends Location> implements MHProposal<RoundTrip<L>> {

	// -------------------- CONSTANTS --------------------

	private final Scenario<L> scenario;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripDepartureProposal(Scenario<L> scenario) {
		this.scenario = scenario;
	}

	// -------------------- HELPERS --------------------

	public synchronized static Integer drawUnusedDeparture(RoundTrip<?> state, Scenario<?> scenario) {
		// TODO Not ideal, handle this upstream:
		if (state.locationCnt() == scenario.getBinCnt()) {
			throw new RuntimeException("No space for inserting a departure time bin.");
		}		
		while (true) {
			final Integer dpt = scenario.getRandom().nextInt(scenario.getBinCnt());
			if (!state.containsDeparture(dpt)) {
				return dpt;
			}
		}
	}

	// -------------------- IMPLEMENTATION OF MHProposal --------------------

	@Override
	public RoundTrip<L> newInitialState() {
		// not to be used standalone
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		final Integer newDeparture = drawUnusedDeparture(state, this.scenario);
		final RoundTrip<L> newState = state.clone(); 
		newState.setDepartureAndEnsureOrdering(this.scenario.getRandom().nextInt(state.locationCnt()), newDeparture);

		final double fwdLogProba = -Math.log(state.locationCnt())
				- Math.log(this.scenario.getBinCnt() - state.locationCnt());
		final double bwdLogProba = fwdLogProba;

		return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);
	}
}
