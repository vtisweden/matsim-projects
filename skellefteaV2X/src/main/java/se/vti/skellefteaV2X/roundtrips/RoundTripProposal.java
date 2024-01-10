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
 * TODO Deal with infeasible transitions (empty chains, max length, ...)
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripProposal<L> implements MHProposal<RoundTrip<L>> {

	private final Random rnd = new Random();

	private final RoundTripConfiguration<L> scenario;

	private final RoundTripLocationProposal<L> locationProposal;
	private final RoundTripDepartureProposal<L> timeBinProposal;
	private final RoundTripChargingProposal<L> chargingProposal;

	public RoundTripProposal(RoundTripConfiguration<L> scenario) {
		this.scenario = scenario;
		this.locationProposal = new RoundTripLocationProposal<>(scenario);
		this.timeBinProposal = new RoundTripDepartureProposal<>(scenario);
		this.chargingProposal = new RoundTripChargingProposal<>(scenario);
	}

	// IMPLEMENTATION OF INTERFACE

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		final double randomNumber = this.rnd.nextDouble();

		if (randomNumber < this.scenario.getLocationProposalProbability()) {

			MHTransition<RoundTrip<L>> transition = this.locationProposal.newTransition(state);
			transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
					Math.log(this.scenario.getLocationProposalProbability()) + transition.getFwdLogProb(),
					Math.log(this.scenario.getLocationProposalProbability()) + transition.getBwdLogProb());
			return transition;

		} else if (randomNumber < this.scenario.getLocationProposalProbability()
				+ this.scenario.getDepartureProposalProbability()) {

			MHTransition<RoundTrip<L>> transition = this.timeBinProposal.newTransition(state);
			transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
					Math.log(this.scenario.getDepartureProposalProbability()) + transition.getFwdLogProb(),
					Math.log(this.scenario.getDepartureProposalProbability()) + transition.getBwdLogProb());
			return transition;

		} else if (randomNumber < this.scenario.getLocationProposalProbability()
				+ this.scenario.getDepartureProposalProbability() + this.scenario.getChargingProposalProbability()) {

			MHTransition<RoundTrip<L>> transition = this.chargingProposal.newTransition(state);
			transition = new MHTransition<>(transition.getOldState(), transition.getNewState(),
					Math.log(this.scenario.getChargingProposalProbability()) + transition.getFwdLogProb(),
					Math.log(this.scenario.getChargingProposalProbability()) + transition.getBwdLogProb());
			return transition;

		} else {

			return null; // should not happen

		}
	}
}
