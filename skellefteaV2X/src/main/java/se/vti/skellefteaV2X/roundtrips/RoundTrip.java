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
package se.vti.skellefteaV2X.roundtrips;

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

	// -------------------- MEMBERS --------------------

	private List<L> locations;

	private List<Integer> departures;

	private List<Boolean> chargings;

	// -------------------- CONSTRUCTION --------------------

	public RoundTrip(List<L> locations, List<Integer> departures, List<Boolean> charging) {
		this.locations = locations;
		this.departures = departures;
		this.chargings = charging;
	}

	// -------------------- INTERNALS --------------------

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

	// -------------------- IMPLEMENTATION --------------------

	public int size() {
		return this.locations.size();
	}

	public L getPredecessorLocation(int i) {
		return this.locations.get(this.predecessorIndex(i));
	}

	public L getLocation(int i) {
		return this.locations.get(i);
	}

	public L getSuccessorLocation(int i) {
		return this.locations.get(this.successorIndex(i));
	}

	public void setLocation(int i, L location) {
		this.locations.set(i, location);
	}

	public void setCharging(int i, Boolean doCharge) {
		this.chargings.set(i, doCharge);
	}

	public Boolean getCharging(int i) {
		return this.chargings.get(i);
	}

	public Boolean getSuccessorCharging(int i) {
		return this.chargings.get(this.successorIndex(i));
	}

	public void setDepartureAndEnsureOrdering(int i, Integer departureBin) {
		this.departures.set(i, departureBin);
		Collections.sort(this.departures);
	}

	public Integer getDeparture(int i) {
		return this.departures.get(i);
	}

	public Integer getNextDeparture(int i) {
		return this.departures.get(this.successorIndex(i));
	}

	public boolean containsDeparture(int bin) {
		return this.departures.contains(bin);
	}

	public void addAndEnsureSortedDepartures(int i, L location, Integer departureBin, Boolean charging) {
		this.locations.add(i, location);
		this.departures.add(i, departureBin);
		this.chargings.add(i, charging);
		Collections.sort(this.departures);
	}

	public void remove(int locationChargingIndex, int departureIndex) {
		this.locations.remove(locationChargingIndex);
		this.chargings.remove(locationChargingIndex);
		this.departures.remove(departureIndex);
	}

	public void remove(int i) {
		this.remove(i, i);
	}

	public RoundTrip<L> deepCopy() {
		return new RoundTrip<L>(new ArrayList<>(this.locations), new ArrayList<>(this.departures),
				new ArrayList<>(this.chargings));
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public boolean equals(Object other) {
		if (other instanceof RoundTrip) {
			RoundTrip<?> otherRoundTrip = (RoundTrip<?>) other;
			return this.locations.equals(otherRoundTrip.locations) && this.departures.equals(otherRoundTrip.departures)
					&& this.chargings.equals(otherRoundTrip.chargings);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.locations.hashCode() + 31 * (this.departures.hashCode() + 31 * this.chargings.hashCode());
	}

	@Override
	public String toString() {
		return "locs[" + this.locations.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "],bins["
				+ this.departures.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "],charge["
				+ this.chargings.stream().map(c -> c ? "1" : "0").collect(Collectors.joining(",")) + "]";
	}
}
