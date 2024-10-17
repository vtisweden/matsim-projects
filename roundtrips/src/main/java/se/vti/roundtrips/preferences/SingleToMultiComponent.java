/**
 * instances.testing
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.preferences;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class SingleToMultiComponent<L extends Location> extends PreferenceComponent<MultiRoundTrip<L>> {

	private final PreferenceComponent<RoundTrip<L>> singleComponent;

	public SingleToMultiComponent(PreferenceComponent<RoundTrip<L>> singleComponent) {
		this.singleComponent = singleComponent;
	}

	@Override
	public double logWeight(MultiRoundTrip<L> multiRoundTrip) {
		double logWeight = 0.0;
		for (RoundTrip<L> roundTrip : multiRoundTrip) {
			logWeight += this.singleComponent.logWeight(roundTrip);
		}
		return logWeight;
	}

}
