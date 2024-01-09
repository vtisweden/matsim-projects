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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import floetteroed.utilities.Units;

/**
 * 
 * @author GunnarF
 *
 * @param <L> the location type
 */
public class RoundTripScenario<L> {

	private final int maxLength;
	private final double analysisPeriod_s;

	private final Set<L> allLocations = new LinkedHashSet<>();

	public RoundTripScenario(int maxLength, double analysisPeriod_s) {
		this.maxLength = maxLength;
		this.analysisPeriod_s = analysisPeriod_s;
	}

	public RoundTripScenario(int maxLength) {
		this(maxLength, Units.S_PER_D);
	}

	public void addLocation(L location) {
		this.allLocations.add(location);
	}

	public int getMaxLength() {
		return this.maxLength;
	}

	public double getAnalysisPeriod_s() {
		return this.analysisPeriod_s;
	}

	public List<L> getAllLocationsListView() {
		return Collections.unmodifiableList(new ArrayList<>(this.allLocations));
	}

}
