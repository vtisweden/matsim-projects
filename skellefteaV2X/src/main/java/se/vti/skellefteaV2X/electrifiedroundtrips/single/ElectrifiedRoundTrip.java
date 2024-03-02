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
package se.vti.skellefteaV2X.electrifiedroundtrips.single;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import se.vti.roundtrips.single.RoundTrip;
import se.vti.skellefteaV2X.model.ElectrifiedLocation;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class ElectrifiedRoundTrip extends RoundTrip<ElectrifiedLocation> {

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private List<Boolean> chargings;

	// -------------------- CONSTRUCTION --------------------

	public ElectrifiedRoundTrip(List<ElectrifiedLocation> locations, List<Integer> departures, List<Boolean> charging, Random rnd) {
		super(locations, departures);
		this.chargings = charging;
		this.rnd = rnd;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setCharging(int i, Boolean doCharge) {
		this.chargings.set(i, doCharge);
	}

	public Boolean getCharging(int i) {
		return this.chargings.get(i);
	}

	public Boolean getSuccessorCharging(int i) {
		return this.chargings.get(this.successorIndex(i));
	}

	@Override
	public void addAndEnsureSortedDepartures(int i, ElectrifiedLocation location, Integer departureBin) {
		super.addAndEnsureSortedDepartures(i, location, departureBin);
		this.chargings.add(i, this.rnd.nextBoolean());
	}

	@Override
	public void remove(int locationChargingIndex, int departureIndex) {
		super.remove(locationChargingIndex, departureIndex);
		this.chargings.remove(locationChargingIndex);
	}

	public ArrayList<Boolean> cloneChargings() {
		return new ArrayList<>(this.chargings);
	}

	@Override
	public ElectrifiedRoundTrip clone() {
		return new ElectrifiedRoundTrip(this.cloneLocations(), this.cloneDepartures(), this.cloneChargings(), this.rnd);
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public boolean equals(Object other) {
		if (other instanceof ElectrifiedRoundTrip && super.equals(other)) {
			return this.chargings.equals(((ElectrifiedRoundTrip) other).chargings);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.chargings.hashCode() + 31 * super.hashCode();
	}

	@Override
	public String toString() {
		return super.toString() + ",charge["
				+ this.chargings.stream().map(c -> c ? "1" : "0").collect(Collectors.joining(",")) + "]";
	}
}
