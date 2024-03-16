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
package se.vti.skellefteaV2X.electrifiedroundtrips.single;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripChargingProposal implements MHProposal<ElectrifiedRoundTrip> {

	// -------------------- CONSTANTS --------------------

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripChargingProposal(Random rnd) {
		this.rnd = rnd;
	}

	// -------------------- IMPLEMENTATION OF MHProposal --------------------

	@Override
	public ElectrifiedRoundTrip newInitialState() {
		// not to be used standalone
		throw new UnsupportedOperationException();
	}

	@Override
	public MHTransition<ElectrifiedRoundTrip> newTransition(ElectrifiedRoundTrip state) {

		final double flipProba = 1.0 / (state.locationCnt() + 1.0);
		final double atLeastOneFlipProba = 1.0 - Math.pow(1.0 - flipProba, state.locationCnt());

		List<Boolean> newChargings;
		boolean flipped = false;
		double proba;
		do {
			newChargings = new ArrayList<>(state.locationCnt());
			proba = 1.0;
			for (int i = 0; i < state.locationCnt(); i++) {
				if (this.rnd.nextDouble() < flipProba) {
					flipped = true;
					newChargings.add(!state.getCharging(i));
					proba *= flipProba;
				} else {
					newChargings.add(state.getCharging(i));
					proba *= (1.0 - flipProba);
				}
			}
		} while (!flipped);

		// FIXME normalization over all possible flips
		final double fwdLogProba = Math.log(proba) - Math.log(atLeastOneFlipProba);
		final double bwdLogProba = fwdLogProba;

		final ElectrifiedRoundTrip newState = state.clone();
		for (int i = 0; i < newState.locationCnt(); i++) {
			newState.setCharging(i, newChargings.get(i));
		}

		return new MHTransition<>(state, newState, fwdLogProba, bwdLogProba);
	}
}
