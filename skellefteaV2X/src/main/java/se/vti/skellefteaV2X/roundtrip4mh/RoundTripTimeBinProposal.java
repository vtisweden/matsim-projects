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

import java.util.Random;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripTimeBinProposal<L> implements MHProposal<RoundTrip<L>> {

	private final Random rnd = new Random();

	private final RoundTripScenario<L> scenario;

	public RoundTripTimeBinProposal(RoundTripScenario<L> scenario) {
		this.scenario = scenario;
	}

	// INTERNALS

	// IMPLEMENTATION OF INTERFACE

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		Integer newBin = null;
		do {
			final Integer bin = this.rnd.nextInt(this.scenario.getDepartureBinCnt());
			if (!state.containsDepartureBin(bin)) {
				// so we cannot stay in the old bin
				newBin = bin;
			}
		} while (newBin == null);

		final RoundTrip<L> newState = state.deepCopy();
		newState.setDepartureBinAndEnsureSortedDepartures(this.rnd.nextInt(state.size()), newBin);

		final double fwdLogProba = Math.log(1.0 / state.size())
				+ Math.log(1.0 / (this.scenario.getDepartureBinCnt() - state.size()));
		final double bwdLogProba = fwdLogProba;

		return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);
	}
}
