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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.Simulator;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * TODO Deal with infeasible transitions (empty chains, max length, ...)
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripProposal<L extends Location> implements MHProposal<RoundTrip<L>> {

	private final Simulator<L> simulator;

	private final Random rnd;

	private final Map<MHProposal<RoundTrip<L>>, Double> proposal2weight = new LinkedHashMap<>();

	public RoundTripProposal(Simulator<L> simulator, Random rnd) {
		this.simulator = simulator;
		this.rnd = rnd;
	}

	public void addProposal(MHProposal<RoundTrip<L>> proposal, double weight) {
		this.proposal2weight.put(proposal, weight);
	}

	public Simulator<L> getSimulator() {
		return this.simulator;
	}

	// IMPLEMENTATION OF INTERFACE

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		final double randomNumber = this.rnd.nextDouble();
		final double allWeightSum = this.proposal2weight.values().stream().mapToDouble(w -> w).sum();
		double weightSum = 0.0;

		for (Map.Entry<MHProposal<RoundTrip<L>>, Double> entry : this.proposal2weight.entrySet()) {
			weightSum += entry.getValue();
			if (randomNumber < weightSum / allWeightSum) {
				MHTransition<RoundTrip<L>> transition = entry.getKey().newTransition(state);

				RoundTrip<L> newRoundTrip = transition.getNewState();
				newRoundTrip.setEpisodes(this.simulator.simulate(newRoundTrip));

				transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
						Math.log(entry.getValue() / allWeightSum) + transition.getFwdLogProb(),
						Math.log(entry.getValue() / allWeightSum) + transition.getBwdLogProb());
				return transition;
			}
		}

		// should not happen
		return null;
	}
}
