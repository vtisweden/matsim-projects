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

import java.util.List;
import java.util.Random;

import se.vti.roundtrips.model.Scenario;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class SimplifiedRoundTripProposal<L extends Location> implements MHProposal<RoundTrip<L>> {

	// -------------------- MEMBERS --------------------

	private final SimplifiedRoundTripProposalParameters proposalParams;

	private final Scenario<L> scenario;

	private final Simulator<L> simulator;
	
	private final List<L> allLocations;

	private final Random rnd;
	
	// -------------------- CONSTRUCTION --------------------

	public SimplifiedRoundTripProposal(SimplifiedRoundTripProposalParameters proposalParams, Scenario<L> scenario, Simulator<L> simulator) {
		this.proposalParams = proposalParams;
		this.scenario = scenario;
		this.simulator = simulator;
		this.allLocations = scenario.getLocationsView();
		this.rnd = scenario.getRandom();
	}

	public SimplifiedRoundTripProposal(Scenario<L> scenario, Simulator<L> simulator) {
		this(new SimplifiedRoundTripProposalParameters(), scenario, simulator);
	}

	// -------------------- INTERNALS --------------------

	private L drawLocationDifferentFrom(L notThisOne) {
		if (this.allLocations.size() <= 1) {
			throw new RuntimeException("Not more than one location available.");
		}
		while (true) {
			final L location = this.allLocations.get(this.rnd.nextInt(this.allLocations.size()));
			if (!location.equals(notThisOne)) {
				return location;
			}
		}
	}

	private Integer drawUnusedDepartureTime(RoundTrip<?> state) {
		if (state.locationCnt() == this.scenario.getBinCnt()) {
			throw new RuntimeException("No space for inserting a departure time bin.");
		}
		while (true) {
			final Integer depTime = this.rnd.nextInt(this.scenario.getBinCnt());
			if (!state.containsDeparture(depTime)) {
				return depTime;
			}
		}
	}

	// -------------------- IMPLEMENTATION OF INTERFACE --------------------

	@Override
	public RoundTrip<L> newInitialState() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> from) {

		final RoundTripTransitionKernel fwdTransitionKernel = new RoundTripTransitionKernel(from,
				this.scenario, this.proposalParams);

		final RoundTrip<L> to = from.clone();

		final double _U = this.rnd.nextDouble();
		if (_U < fwdTransitionKernel.insertProba) { // INSERT

			final int whereToInsert = this.rnd.nextInt(from.locationCnt() + 1);
			final L whatLocationToInsert = this.allLocations.get(this.rnd.nextInt(this.allLocations.size()));
			final Integer whatDptTimeToInsert = this.drawUnusedDepartureTime(from);
			to.addAndEnsureSortedDepartures(whereToInsert, whatLocationToInsert, whatDptTimeToInsert);

		} else if (_U < fwdTransitionKernel.insertProba + fwdTransitionKernel.removeProba) { // REMOVE

			final int whereToRemove = this.rnd.nextInt(from.locationCnt());
			to.remove(whereToRemove, whereToRemove);

		} else if (_U < fwdTransitionKernel.insertProba + fwdTransitionKernel.removeProba
				+ fwdTransitionKernel.flipLocationProba) { // FLIP LOCATION

			final int whereToFlip = this.rnd.nextInt(from.locationCnt());
			final L newLocation = this.drawLocationDifferentFrom(from.getLocation(whereToFlip));
			to.setLocation(whereToFlip, newLocation);

		} else { // FLIP DEPARTURE TIME

			final int whereToFlip = this.rnd.nextInt(from.locationCnt());
			final Integer newDptTime = this.drawUnusedDepartureTime(from);
			to.setDepartureAndEnsureOrdering(whereToFlip, newDptTime);
		}
		to.setEpisodes(this.simulator.simulate(to));

		final RoundTripTransitionKernel bwdTransitionKernel = new RoundTripTransitionKernel(to,
				this.scenario, this.proposalParams);
		return new MHTransition<>(from, to, Math.log(fwdTransitionKernel.transitionProba(to)),
				Math.log(bwdTransitionKernel.transitionProba(from)));
	}
}
