/**
 * se.vti.roundtrips.multiple
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
package se.vti.roundtrips.multiple;

import java.util.Random;

import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class MultiRoundTripProposal<R extends RoundTrip<?>, M extends MultiRoundTrip<R>> implements MHProposal<M> {

	private final Random rnd;

	private final RoundTripProposal<R> singleProposal;

	private Double flipProba = null;

	public MultiRoundTripProposal(Random rnd, RoundTripProposal<R> singleProposal) {
		this.rnd = rnd;
		this.singleProposal = singleProposal;
	}

	public MultiRoundTripProposal<R, M> setFlipProbability(double flipProbability) {
		this.flipProba = flipProbability;
		return this;
	}

	// IMPLEMENTATION OF INTERFACE

	@Override
	public M newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<M> newTransition(M state) {
//		final double minFlipProba = 1.0 / (state.size() + 1);
//		final double flipProba = 1.0 / (state.size() + 1);
		final double minFlipProba = 1.0 / Math.max(1.0, state.size());
		final double flipProba = (this.flipProba != null ? Math.max(this.flipProba, minFlipProba) : minFlipProba);

		final double atLeastOneFlipProba = 1.0 - Math.pow(1.0 - flipProba, state.size());

		M newState = (M) state.clone();
		boolean flipped = false;
		double fwdLogProba;
		double bwdLogProba;
		do {
			fwdLogProba = 0.0;
			bwdLogProba = 0.0;
			for (int i = 0; i < state.size(); i++) {
				if (this.rnd.nextDouble() < flipProba) {
					MHTransition<R> transition = this.singleProposal.newTransition(state.getRoundTrip(i));
					newState.setRoundTrip(i, transition.getNewState());
					fwdLogProba += Math.log(flipProba) + transition.getFwdLogProb();
					bwdLogProba += Math.log(flipProba) + transition.getBwdLogProb();
					flipped = true;
				} else {
					newState.getRoundTrip(i)
							.setEpisodes(this.singleProposal.getSimulator().simulate(newState.getRoundTrip(i)));
					fwdLogProba += Math.log(1.0 - flipProba);
					bwdLogProba += Math.log(1.0 - flipProba);
				}
			}
		} while (!flipped);

		fwdLogProba -= Math.log(atLeastOneFlipProba);
		bwdLogProba -= Math.log(atLeastOneFlipProba);
//		assert (fwdLogProba == bwdLogProba);

		return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);
	}

}
