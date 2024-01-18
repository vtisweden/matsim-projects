/**
 * se.vti.skellefteaV2X
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

import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class RoundTripIgnoreLocationCombinations<L> implements MHWeight<RoundTrip<L>> {

	private final int locationCnt;

	public RoundTripIgnoreLocationCombinations(int locationCnt) {
		this.locationCnt = locationCnt;
	}

	@Override
	public double logWeight(RoundTrip<L> state) {
		if (state.locationCnt() == 1) {
			return -Math.log(this.locationCnt);
		} else if (state.locationCnt() == 2) {
			return -Math.log(this.locationCnt) - Math.log(this.locationCnt - 1);
		} else if (state.locationCnt() == 3) {
			return -Math.log(this.locationCnt) - Math.log(this.locationCnt - 1) - Math.log(this.locationCnt - 2);
		} else {

			// first one is anything
			double proba = 1.0 / this.locationCnt;

			// second one is anything different from first one
			double probaEqualsFirst = 0.0;
			proba *= 1.0 / (this.locationCnt - 1);

			for (int i = 2; i < state.locationCnt() - 1; i++) {
				// anything different from previous one
				proba *= 1.0 / (this.locationCnt - 1);
				// only if previous not equals first one, we may uniformly select first one
				// again
				probaEqualsFirst = (1.0 - probaEqualsFirst) * 1.0 / (this.locationCnt - 1);
			}

			// number of options for last one depends on proba of previous one being equal
			// to first one
			proba *= probaEqualsFirst * 1.0 / (this.locationCnt - 1)
					+ (1.0 - probaEqualsFirst) * 1.0 / (this.locationCnt - 2);

			double size = 1.0 / proba; // since uniform

			return -Math.log(size);
		}

	}

}
