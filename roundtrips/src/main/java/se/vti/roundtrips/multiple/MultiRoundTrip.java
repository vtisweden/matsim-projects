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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class MultiRoundTrip<L extends Location> implements Iterable<RoundTrip<L>> {

	private final List<RoundTrip<L>> roundTrips;

	public MultiRoundTrip(int size) {
		this.roundTrips = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			this.roundTrips.add(null);
		}
	}

	public RoundTrip<L> getRoundTrip(int i) {
		return this.roundTrips.get(i);
	}

	public void setRoundTrip(int i, RoundTrip<L> roundTrip) {
		this.roundTrips.set(i, roundTrip);
	}

	public int size() {
		return this.roundTrips.size();
	}

	public int locationCnt() {
		return this.roundTrips.stream().mapToInt(r -> r.locationCnt()).sum();
	}

	@Override
	public MultiRoundTrip<L> clone() {
		MultiRoundTrip<L> result = new MultiRoundTrip<L>(this.size());
		for (int i = 0; i < this.size(); i++) {
			result.setRoundTrip(i, this.getRoundTrip(i).clone());
		}
		return result;
	}

	@Override
	public Iterator<RoundTrip<L>> iterator() {
		return this.roundTrips.iterator();
	}

	@Override
	public String toString() {
		return "{" + this.roundTrips.stream().map(r -> "(" + r.toString() + ")").collect(Collectors.joining(",")) + "}";
	}

}
