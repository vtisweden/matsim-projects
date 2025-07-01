/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.single;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.simulator.Episode;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 * 
 */
public class RoundTrip<L extends Node> {

	// -------------------- CONSTANTS --------------------

	private final Object attributes;

	// -------------------- MEMBERS --------------------

	private List<L> locations;

	private List<Integer> departures;

	private List<Episode> episodes = null;

	// -------------------- CONSTRUCTION --------------------

	public RoundTrip(List<L> locations, List<Integer> departures, Object attributes) {
		this.locations = locations;
		this.departures = departures;
		this.attributes = attributes;
	}

	public RoundTrip(List<L> locations, List<Integer> departures) {
		this(locations, departures, null);
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

	public Object getAttributes() {
		return this.attributes;
	}

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

	public List<L> getLocationsView() {
		return Collections.unmodifiableList(this.locations);
	}

	public void setLocation(int i, L location) {
		this.locations.set(i, location);
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

	public List<Integer> getDeparturesView() {
		return Collections.unmodifiableList(this.departures);
	}

	public void addAndEnsureSortedDepartures(int i, L location, Integer departureBin) {
		this.locations.add(i, location);
		this.departures.add(i, departureBin);
		Collections.sort(this.departures);
	}

	public void remove(int locationIndex, int departureIndex) {
		this.locations.remove(locationIndex);
		this.departures.remove(departureIndex);
	}

	public void remove(int i) {
		this.remove(i, i);
	}

	public ArrayList<L> cloneLocations() {
		return new ArrayList<>(this.locations);
	}

	public ArrayList<Integer> cloneDepartures() {
		return new ArrayList<>(this.departures);
	}

	public List<Episode> getEpisodes() {
		return episodes;
	}

	public void setEpisodes(List<Episode> episodes) {
		this.episodes = episodes;
	}
	
	// TODO NEW
	public void cloneEpisodes(RoundTrip<L> other) {
		// Deliberately not checking for this.episodes==null, should fail clearly.
		this.episodes = new ArrayList<>(other.episodes.size());
		for (Episode episode : other.episodes) {
			this.episodes.add(episode.clone());
		}
	}
	
	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public RoundTrip<L> clone() {
		// TODO for this to work, attributes have to be immutable, as they are shared by
		// round trips
		final RoundTrip<L> result = new RoundTrip<>(this.cloneLocations(), this.cloneDepartures(), this.attributes);
		if (this.episodes != null) {
			result.cloneEpisodes(this);
		}
		return result;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof RoundTrip) {
			RoundTrip<?> otherRoundTrip = (RoundTrip<?>) other;
			return this.locations.equals(otherRoundTrip.locations) && this.departures.equals(otherRoundTrip.departures);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.locations.hashCode() + 31 * this.departures.hashCode();
	}

	@Override
	public String toString() {
		return "locs[" + this.locations.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "],bins["
				+ this.departures.stream().map(l -> l.toString()).collect(Collectors.joining(",")) + "]";
	}
}
