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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripScenario<L> {

	// -------------------- CONSTANTS --------------------

	private final int maxLocations;
	private final int timeBinCnt;
	private final double locationProposalProba;
	private final double departureProposalProba;
	private final double chargingProposalProba;

	private final Random rnd;

	// -------------------- MEMBERS --------------------

	private final Set<L> allLocations = new LinkedHashSet<>();

	// -------------------- CONSTRUCTION --------------------

	public RoundTripScenario(int maxLocations, int timeBinCnt, double locationProposalWeight,
			double timeBinProposalWeight, double chargingProposalWeight, Random rnd) {

		assert (maxLocations <= timeBinCnt);
		this.maxLocations = maxLocations;
		this.timeBinCnt = timeBinCnt;

		final double weightSum = locationProposalWeight + timeBinProposalWeight + chargingProposalWeight;
		assert (weightSum > 1e-8);
		this.locationProposalProba = locationProposalWeight / weightSum;
		this.departureProposalProba = timeBinProposalWeight / weightSum;
		this.chargingProposalProba = chargingProposalWeight / weightSum;

		this.rnd = rnd;
	}

	public void addLocation(L location) {
		this.allLocations.add(location);
	}

	// -------------------- GETTERS --------------------

	public Random getRandom() {
		return this.rnd;
	}

	public int getMaxLocations() {
		return this.maxLocations;
	}

	public int getTimeBinCnt() {
		return this.timeBinCnt;
	}

	public double getLocationProposalProbability() {
		return this.locationProposalProba;
	}

	public double getDepartureProposalProbability() {
		return this.departureProposalProba;
	}

	public double getChargingProposalProbability() {
		return this.chargingProposalProba;
	}

	public Set<L> getAllLocationsView() {
		return Collections.unmodifiableSet(this.allLocations);
	}
}
