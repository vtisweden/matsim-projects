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

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.Node;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripProposal;
import se.vti.roundtrips.single.Simulator;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class MultiRoundTripProposal<L extends Node> implements MHProposal<MultiRoundTrip<L>> {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final MHProposal<RoundTrip<L>> singleProposal;

	private Double flipProba = null;

	// -------------------- CONSTRUCTION --------------------

	public MultiRoundTripProposal(Random rnd, MHProposal<RoundTrip<L>> singleProposal) {
		this.rnd = rnd;
		this.singleProposal = singleProposal;
	}

	public MultiRoundTripProposal(Scenario<L> scenario, Simulator<L> simulator) {
		this(scenario.getRandom(), new RoundTripProposal<>(scenario, simulator));
	}

	public MultiRoundTripProposal<L> setFlipProbability(double flipProbability) {
		this.flipProba = flipProbability;
		return this;
	}

	// --------------------IMPLEMENTATION OF MHProposal --------------------

	@Override
	public MultiRoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<MultiRoundTrip<L>> newTransition(MultiRoundTrip<L> from) {

		final double minFlipProba = 1.0 / Math.max(1.0, from.size());
		final double flipProba = (this.flipProba != null ? Math.max(this.flipProba, minFlipProba) : minFlipProba);
		final double atLeastOneFlipProba = 1.0 - Math.pow(1.0 - flipProba, from.size());

		final MultiRoundTrip<L> to = from.clone();

		boolean flipped = false;
		double fwdLogProba;
		double bwdLogProba;
		do {
			fwdLogProba = 0.0;
			bwdLogProba = 0.0;
			for (int i = 0; i < from.size(); i++) {
				if (this.rnd.nextDouble() < flipProba) {
					MHTransition<RoundTrip<L>> transition = this.singleProposal.newTransition(from.getRoundTrip(i));
					to.setRoundTripAndUpdateSummaries(i, transition.getNewState());
					fwdLogProba += Math.log(flipProba) + transition.getFwdLogProb();
					bwdLogProba += Math.log(flipProba) + transition.getBwdLogProb();
					flipped = true;
				} else {
					fwdLogProba += Math.log(1.0 - flipProba);
					bwdLogProba += Math.log(1.0 - flipProba);
				}
			}
		} while (!flipped);
		fwdLogProba -= Math.log(atLeastOneFlipProba);
		bwdLogProba -= Math.log(atLeastOneFlipProba);

		return new MHTransition<>(from, to, fwdLogProba, bwdLogProba);
	}

}
