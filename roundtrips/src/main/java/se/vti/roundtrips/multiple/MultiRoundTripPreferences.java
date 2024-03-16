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

import java.util.ArrayList;
import java.util.List;

import se.vti.roundtrips.model.Location;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 * @param <R>
 * @param <L>
 */
public class MultiRoundTripPreferences<R extends RoundTrip<L>, L extends Location>
		implements MHWeight<MultiRoundTrip<L, R>> {

	private final List<Preferences<R, L>> singleRoundTripPreferences = new ArrayList<>();

	public MultiRoundTripPreferences() {
	}

	public MultiRoundTripPreferences(Preferences<R, L> singleRoundTripPreferences) {
		this.singleRoundTripPreferences.add(singleRoundTripPreferences);
	}

	public void addPreferences(Preferences<R, L> singleRoundTripPreferences) {
		this.singleRoundTripPreferences.add(singleRoundTripPreferences);
	}

	@Override
	public double logWeight(MultiRoundTrip<L, R> multiRoundTrip) {
		double result = 0.0;
		for (Preferences<R, L> preferences : singleRoundTripPreferences) {
			for (R roundTrip : multiRoundTrip) {
				result += preferences.logWeight(roundTrip);
			}
		}
		return result;
	}
}
