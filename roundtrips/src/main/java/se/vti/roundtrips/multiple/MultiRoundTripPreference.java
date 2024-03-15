/**
 * se.vti.roundtrips.multiple
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
package se.vti.roundtrips.multiple;

import se.vti.roundtrips.model.Location;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 * @param <R>
 * @param <L>
 */
public class MultiRoundTripPreference<M extends MultiRoundTrip<L, R>, R extends RoundTrip<L>, L extends Location> {

	final Preferences.Component<R, L> preference;

	public MultiRoundTripPreference(Preferences.Component<R, L> preference) {
		this.preference = preference;
	}

	public double logWeight(M multiRoundTrip) {
		double result = 0.0;
		for (int i = 0; i < multiRoundTrip.size(); i++) {
			result += this.preference.logWeight(multiRoundTrip.getRoundTrip(i));
		}
		return result;
	}

}
