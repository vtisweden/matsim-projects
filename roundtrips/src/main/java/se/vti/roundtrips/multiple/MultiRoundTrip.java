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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.Simulator;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class MultiRoundTrip<L extends Location> implements Iterable<RoundTrip<L>> {

	// -------------------- MEMBERS --------------------

	private final List<RoundTrip<L>> roundTrips;

	private final Map<Class<?>, MultiRoundTripSummary<L>> class2summary = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public MultiRoundTrip(int size) {
		this.roundTrips = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			this.roundTrips.add(null);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public final void setRoundTripAndUpdateSummaries(int i, RoundTrip<L> roundTrip) {		
		if (roundTrip == this.getRoundTrip(i)) {
			return;
		}
		for (MultiRoundTripSummary<L> summaryStats : this.class2summary.values()) {
			summaryStats.update(i, this.getRoundTrip(i), roundTrip);
		}
		this.roundTrips.set(i, roundTrip);
	}

	public RoundTrip<L> getRoundTrip(int i) {
		return this.roundTrips.get(i);
	}

	public int size() {
		return this.roundTrips.size();
	}

	public void addSummary(MultiRoundTripSummary<L> summary) {
		this.class2summary.put(summary.getClass(), summary);
	}

	public <S extends MultiRoundTripSummary<L>> S getSummary(Class<S> summaryClass) {
		return (S) this.class2summary.get(summaryClass);
	}

	public void recomputeSummaries() {
		for (MultiRoundTripSummary<L> summary : this.class2summary.values()) {
			summary.clear();
		}
		for (int i = 0; i < this.roundTrips.size(); i++) {
			final RoundTrip<L> roundTrip = this.roundTrips.get(i);
			this.roundTrips.set(i, null);
			this.setRoundTripAndUpdateSummaries(i, roundTrip);
		}
	}

	public void simulateAll(Simulator<L> simulator) {
		for (RoundTrip<L> roundTrip : this.roundTrips) {
			roundTrip.setEpisodes(simulator.simulate(roundTrip));
		}
	}

	// -------------------- IMPLEMENTATION OF Iterable --------------------

	@Override
	public Iterator<RoundTrip<L>> iterator() {
		return this.roundTrips.iterator();
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public MultiRoundTrip<L> clone() {

		// Has initially no summary stats.
		final MultiRoundTrip<L> result = new MultiRoundTrip<L>(this.size());

		// Not yet any summary stats to update.
		for (int i = 0; i < this.roundTrips.size(); i++) {
			result.setRoundTripAndUpdateSummaries(i, this.getRoundTrip(i));
		}

		// Only now, clone summaries.
		for (MultiRoundTripSummary<L> summary : this.class2summary.values()) {
			result.addSummary(summary.clone());
		}

		return result;
	}

	@Override
	public String toString() {
		return "{" + this.roundTrips.stream().map(r -> "(" + r.toString() + ")").collect(Collectors.joining(",")) + "}";
	}

}
