/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.samplingweights.priors;

import org.apache.commons.math3.util.CombinatoricsUtils;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.samplingweights.SamplingWeight;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class UniformPrior<N extends Node> implements SamplingWeight<RoundTrip<N>> {

	private final int locationCnt;
	private final int timeBinCnt;

	public UniformPrior(int locationCnt, int timeBinCnt) {
		this.locationCnt = locationCnt;
		this.timeBinCnt = timeBinCnt;
	}

	@Override
	public double logWeight(RoundTrip<N> roundTrip) {
		return -(roundTrip.size() * Math.log(this.locationCnt))
				- CombinatoricsUtils.binomialCoefficientLog(this.timeBinCnt, roundTrip.size());
	}

}
