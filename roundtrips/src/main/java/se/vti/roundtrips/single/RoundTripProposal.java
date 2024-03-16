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

import java.util.LinkedHashMap;
import java.util.Map;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * TODO Deal with infeasible transitions (empty chains, max length, ...)
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripProposal<L, R extends RoundTrip<L>> implements MHProposal<R> {

	private final RoundTripConfiguration<L> config;

	private final Simulator<L, R> simulator;

	private final Map<MHProposal<R>, Double> proposal2weight = new LinkedHashMap<>();

//	private final RoundTripLocationProposal<L> locationProposal;
//	private final RoundTripDepartureProposal<L> timeBinProposal;
//	private final RoundTripChargingProposal<L> chargingProposal;

	public RoundTripProposal(RoundTripConfiguration<L> config, Simulator<L, R> simulator) {
		this.config = config;
		this.simulator = simulator;
//		this.addProposal(new RoundTripLocationProposal<>(config), config.getLocationProposalProbability());
//		this.addProposal(new RoundTripDepartureProposal<>(config), config.getDepartureProposalProbability());
//		this.locationProposal = new RoundTripLocationProposal<>(config);
//		this.timeBinProposal = new RoundTripDepartureProposal<>(config);
//		this.chargingProposal = new RoundTripChargingProposal<>(config);
	}

	public void addProposal(MHProposal<R> proposal, double weight) {
		this.proposal2weight.put(proposal, weight);
	}

	public Simulator<L,R> getSimulator() {
		return this.simulator;
	}
	
	// IMPLEMENTATION OF INTERFACE

	@Override
	public R newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<R> newTransition(R state) {

		final double randomNumber = this.config.getRandom().nextDouble();
		final double allWeightSum = this.proposal2weight.values().stream().mapToDouble(w -> w).sum();
		double weightSum = 0.0;

		for (Map.Entry<MHProposal<R>, Double> entry : this.proposal2weight.entrySet()) {
			weightSum += entry.getValue();
			if (randomNumber < weightSum / allWeightSum) {
				MHTransition<R> transition = entry.getKey().newTransition(state);

				R newRoundTrip = transition.getNewState();
				newRoundTrip.setEpisodes(this.simulator.simulate(newRoundTrip));

				transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
						Math.log(entry.getValue() / allWeightSum) + transition.getFwdLogProb(),
						Math.log(entry.getValue() / allWeightSum) + transition.getBwdLogProb());
				return transition;
			}
		}

		// should not happen
		return null;

//		if (randomNumber < this.config.getLocationProposalProbability()) {
//
//			MHTransition<RoundTrip<L>> transition = this.locationProposal.newTransition(state);
//			transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
//					Math.log(this.config.getLocationProposalProbability()) + transition.getFwdLogProb(),
//					Math.log(this.config.getLocationProposalProbability()) + transition.getBwdLogProb());
//			return transition;
//
//		} else if (randomNumber < this.config.getLocationProposalProbability()
//				+ this.config.getDepartureProposalProbability()) {
//
//			MHTransition<RoundTrip<L>> transition = this.timeBinProposal.newTransition(state);
//			transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
//					Math.log(this.config.getDepartureProposalProbability()) + transition.getFwdLogProb(),
//					Math.log(this.config.getDepartureProposalProbability()) + transition.getBwdLogProb());
//			return transition;
//
//		} else if (randomNumber < this.config.getLocationProposalProbability()
//				+ this.config.getDepartureProposalProbability() + this.config.getChargingProposalProbability()) {
//
//			MHTransition<RoundTrip<L>> transition = this.chargingProposal.newTransition(state);
//			transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
//					Math.log(this.config.getChargingProposalProbability()) + transition.getFwdLogProb(),
//					Math.log(this.config.getChargingProposalProbability()) + transition.getBwdLogProb());
//			return transition;
//
//		} else { // do nothing
//
//			return new MHTransition<>(state, state.clone(), Math.log(this.config.getDoNothingProbability()),
//					Math.log(this.config.getDoNothingProbability()));
//		}
	}
}
