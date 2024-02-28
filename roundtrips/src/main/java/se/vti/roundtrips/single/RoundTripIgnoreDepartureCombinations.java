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

import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class RoundTripIgnoreDepartureCombinations<L> implements MHWeight<RoundTrip<L>> {

	private final int timeBinCnt;
	
	public RoundTripIgnoreDepartureCombinations(int timeBinCnt) {
		this.timeBinCnt = timeBinCnt;
	}
	
	@Override
	public double logWeight(RoundTrip<L> state) {
		double logSizeWithoutSorting = 0.0;
		double logPermutations = 0.0;
		for (int i = 0; i < state.locationCnt(); i++) {
			logSizeWithoutSorting += Math.log(this.timeBinCnt - i);
			logPermutations += Math.log(i + 1); // permutations
		}
		double logSizeWithSorting = logSizeWithoutSorting - logPermutations;
		return -logSizeWithSorting;
	}

}
