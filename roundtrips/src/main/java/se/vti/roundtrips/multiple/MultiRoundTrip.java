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

import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class MultiRoundTrip<L, R extends RoundTrip<L>> {

	private final List<R> roundTrips;

	public MultiRoundTrip(int size) {
		this.roundTrips = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			this.roundTrips.add(null);
		}
	}

	public R getRoundTrip(int i) {
		return this.roundTrips.get(i);
	}

	public void setRoundTrip(int i, R roundTrip) {
		this.roundTrips.set(i, roundTrip);
	}

	public int size() {
		return this.roundTrips.size();
	}

	@Override
	public MultiRoundTrip<L, R> clone() {
		MultiRoundTrip<L, R> result = new MultiRoundTrip<L, R>(this.size());
		for (int i = 0; i < this.size(); i++) {
			result.setRoundTrip(i, (R) this.getRoundTrip(i).clone());
		}
		return result;
	}

}
