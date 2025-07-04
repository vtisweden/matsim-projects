/**
 * se.vti.roundtrips.samplingweights.misc
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
package se.vti.roundtrips.samplingweights.misc.timeUse;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.SamplingWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class LogarithmicMultiDayTimeUse<N extends Node> extends LogarithmicTimeUse<N>
		implements SamplingWeight<MultiRoundTrip<N>> {

	public LogarithmicMultiDayTimeUse() {
	}

	public void assignComponent(Component component, N node, int... indices) {
		for (int index : indices) {
			super.assignComponent(component, node, index);
		}
	}

	@Override
	public double logWeight(MultiRoundTrip<N> multiRoundTrip) {
		return super.computeLogWeight(multiRoundTrip);
	}

}
