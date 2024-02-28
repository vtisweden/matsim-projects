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

import java.util.ArrayList;
import java.util.List;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripChargingProposal<L> implements MHProposal<RoundTrip<L>> {

	// -------------------- CONSTANTS --------------------

	private final RoundTripConfiguration<L> config;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripChargingProposal(RoundTripConfiguration<L> config) {
		this.config = config;
	}

	// -------------------- IMPLEMENTATION OF MHProposal --------------------

	@Override
	public RoundTrip<L> newInitialState() {
		// not to be used standalone
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<RoundTrip<L>> newTransition(RoundTrip<L> state) {

		final double flipProba = 1.0 / (state.locationCnt() + 1.0);
		boolean flipped = false;
		List<Boolean> newChargings;
		double proba;
		do {
			newChargings = new ArrayList<>(state.locationCnt());
			proba = 1.0;
			for (int i = 0; i < state.locationCnt(); i++) {
				if (this.config.getRandom().nextDouble() < flipProba) {
					flipped = true;
					newChargings.add(!state.getCharging(i));
					proba *= flipProba;
				} else {
					newChargings.add(state.getCharging(i));
					proba *= (1.0 - flipProba);
				}
			}
		} while (!flipped);

		final double fwdLogProba = Math.log(proba) - state.locationCnt() * Math.log(1.0 - flipProba);
		final double bwdLogProba = fwdLogProba;

		final RoundTrip<L> newState = state.clone();
		for (int i = 0; i < newState.locationCnt(); i++) {
			newState.setCharging(i, newChargings.get(i));
		}

		return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);
	}
}
