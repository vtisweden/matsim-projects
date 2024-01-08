/**
 * se.vti.skellefeaV2X
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
package se.vti.skellefteaV2X.roundtrip4mh;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTrip<L> {

	private List<L> locations;

	private List<Double> durations_s;

	public RoundTrip(List<L> locations, List<Double> durations_s) {
		this.locations = locations;
		this.durations_s = durations_s;
	}

	// INTERNALS

	// IMPLEMENTATION

	public int size() {
		return this.locations.size();
	}
	
	public List<L> locationsCopy() {
		return new ArrayList<>(this.locations);
	}

	public L getLocation(int i) {
		return this.locations.get(i);
	}

	public double getDuration_s(int i) {
		return this.durations_s.get(i);
	}

	public void add(int i, L location, double duration_s) {
		this.locations.add(i, location);
		this.durations_s.add(i, duration_s);
	}

	public void remove(int i) {
		this.locations.remove(i);
		this.durations_s.remove(i);
	}

	public RoundTrip<L> deepCopy() {
		return new RoundTrip<L>(new ArrayList<>(this.locations), new ArrayList<>(this.durations_s));
	}
	
	@Override
	public String toString() {
		return this.locations.stream().map(l -> l.toString()).collect(Collectors.joining(","));
	}

}
