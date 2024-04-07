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
package od2roundtrips.model;

import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class SingleToMultiComponent extends PreferenceComponent<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> {

	private final PreferenceComponent<RoundTrip<TAZ>> singleComponent;

	public SingleToMultiComponent(PreferenceComponent<RoundTrip<TAZ>> singleComponent) {
		this.singleComponent = singleComponent;
	}

	@Override
	public double logWeight(MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> multiRoundTrip) {
		double logWeight = 0.0;
		for (RoundTrip<TAZ> roundTrip : multiRoundTrip) {
			logWeight += this.singleComponent.logWeight(roundTrip);
		}
		return logWeight;
	}

}
