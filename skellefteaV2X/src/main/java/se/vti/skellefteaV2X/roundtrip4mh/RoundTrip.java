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
import java.util.Collections;
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

	private List<Integer> departureBins;

	private List<Boolean> charging;
	
	public RoundTrip(List<L> locations, List<Integer> departureBins, List<Boolean> charging) {
		this.locations = locations;
		this.departureBins = departureBins;
		this.charging = charging;
	}

	// INTERNALS

	// IMPLEMENTATION

	public int size() {
		return this.locations.size();
	}

	public List<L> locationsCopy() {
		return new ArrayList<>(this.locations);
	}

	public int predecessorIndex(int i) {
		if (i > 0) {
			return i - 1;
		} else {
			return this.size() - 1;
		}
	}

	public int successorIndex(int i) {
		if (i < this.size() - 1) {
			return i + 1;
		} else {
			return 0;
		}
	}

	public L getPredecessorLocation(int i) {
		return this.locations.get(this.predecessorIndex(i));
	}

	public L getSuccessorLocation(int i) {
		return this.locations.get(this.successorIndex(i));
	}

	public L getLocation(int i) {
		return this.locations.get(i);
	}

	public void setLocation(int i, L location) {
		this.locations.set(i, location);
	}

	public Boolean getCharging(int i) {
		return this.charging.get(i);
	}

	public void setCharging(int i, Boolean doCharge) {
		this.charging.set(i, doCharge);
	}

	public Integer getDepartureBin(int i) {
		return this.departureBins.get(i);
	}

	public void setDepartureBinAndEnsureSortedDepartures(int i, Integer departureBin) {
		this.departureBins.set(i, departureBin);
		Collections.sort(this.departureBins);
	}

	public void addAndEnsureSortedDepartures(int i, L location, Integer departureBin, Boolean charging) {
		this.locations.add(i, location);
		this.departureBins.add(i, departureBin);
		this.charging.add(i, charging);
		Collections.sort(this.departureBins);
	}

	public void removeLocationAndChargingAndOtherDeparture(int locationChargingIndex, int departureIndex) {
		this.locations.remove(locationChargingIndex);
		this.charging.remove(locationChargingIndex);
		this.departureBins.remove(departureIndex);
	}

	public void remove(int i) {
		this.locations.remove(i);
		this.departureBins.remove(i);
	}

	public boolean containsDepartureBin(int bin) {
		return this.departureBins.contains(bin);
	}

	public RoundTrip<L> deepCopy() {
		return new RoundTrip<L>(new ArrayList<>(this.locations), new ArrayList<>(this.departureBins), new ArrayList<>(this.charging));
	}

	@Override
	public String toString() {
		return "locs[" + this.locations.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "],bins["
				+ this.departureBins.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "],charging["
						+ this.charging.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "]";				
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RoundTrip) {
			RoundTrip<?> otherRoundTrip = (RoundTrip<?>) other;
			return this.locations.equals(otherRoundTrip.locations)
					&& this.departureBins.equals(otherRoundTrip.departureBins)
					&& this.charging.equals(otherRoundTrip.charging);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.locations.hashCode() + 31 * (this.departureBins.hashCode() + 31 * this.charging.hashCode());
	}

}
