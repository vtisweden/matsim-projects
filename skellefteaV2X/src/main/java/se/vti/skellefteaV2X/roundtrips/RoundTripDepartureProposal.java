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

import java.util.Random;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripDepartureProposal<L> implements MHProposal<RoundTrip<L>> {

	// -------------------- CONSTANTS --------------------

	private final RoundTripConfiguration<L> config;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripDepartureProposal(RoundTripConfiguration<L> config) {
		this.config = config;
	}

	// -------------------- HELPERS --------------------

	public static Integer drawUnusedDeparture(RoundTrip<?> state, RoundTripConfiguration<?> scenario) {
		return drawUnusedDeparture(state, scenario.getRandom(), scenario.getTimeBinCnt());
	}

	public static Integer drawUnusedDeparture(RoundTrip<?> state, Random rnd, int timeBinCnt) {
		while (true) {
			final Integer dpt = rnd.nextInt(timeBinCnt);
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

		final Integer newDeparture = drawUnusedDeparture(state, this.config);
		final RoundTrip<L> newState = state.clone();
		newState.setDepartureAndEnsureOrdering(this.config.getRandom().nextInt(state.locationCnt()), newDeparture);

		final double fwdLogProba = -Math.log(state.locationCnt()) - Math.log(this.config.getTimeBinCnt() - state.locationCnt());
		final double bwdLogProba = fwdLogProba;

		return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);
	}
}
