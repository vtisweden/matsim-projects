/**
 * se.vti.skelleftea
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
package se.vti.skellefteaV2X.model;

import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripConfiguration;
import se.vti.roundtrips.single.RoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * TODO extract to roundtrips project
 * 
 * @author GunnarF
 *
 */
public class SimulatedRoundTripProposal implements MHProposal<RoundTrip<ElectrifiedLocation>> {

	private final MHProposal<RoundTrip<ElectrifiedLocation>> proposal;
	
	private final ElectrifiedSimulator simulator;

	public SimulatedRoundTripProposal(RoundTripConfiguration<ElectrifiedLocation> config, ElectrifiedSimulator simulator) {
		this.proposal = new RoundTripProposal<ElectrifiedLocation>(config);
		this.simulator = simulator;
	}

	@Override
	public RoundTrip<ElectrifiedLocation> newInitialState() {
		SimulatedRoundTrip state = (SimulatedRoundTrip) this.proposal.newInitialState();
		state.setEpisodes(this.simulator.simulate(state));
		return state;
	}

	@Override
	public MHTransition<RoundTrip<ElectrifiedLocation>> newTransition(RoundTrip<ElectrifiedLocation> state) {
		MHTransition<RoundTrip<ElectrifiedLocation>> transition = this.proposal.newTransition(state);
		SimulatedRoundTrip roundTrip = (SimulatedRoundTrip) transition.getNewState();
		roundTrip.setEpisodes(this.simulator.simulate(roundTrip));
		return transition;
	}

}
