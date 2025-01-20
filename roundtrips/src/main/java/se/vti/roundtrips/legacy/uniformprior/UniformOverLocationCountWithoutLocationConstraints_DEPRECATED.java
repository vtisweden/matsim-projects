/**
 * se.vti.skellefteaV2X
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
package se.vti.roundtrips.legacy.uniformprior;

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class UniformOverLocationCountWithoutLocationConstraints_DEPRECATED<R extends RoundTrip<L>, L extends Location>
		extends PreferenceComponent<R> {

	private final RoundTripIgnoreDepartureCombinations_DEPRECATED correctDepartureCombinations;
	private final RoundTripIgnoreLocationCombinationsWithoutLocationConstraints_DEPRECATED correctLocationCombinations;

	public UniformOverLocationCountWithoutLocationConstraints_DEPRECATED(Scenario<?> scenario) {
		this.correctDepartureCombinations = new RoundTripIgnoreDepartureCombinations_DEPRECATED(scenario.getBinCnt());
		this.correctLocationCombinations = new RoundTripIgnoreLocationCombinationsWithoutLocationConstraints_DEPRECATED(
				scenario.getLocationCnt());
	}

	@Override
	public double logWeight(R simulatedRoundTrip) {
		return this.correctDepartureCombinations.logWeight(simulatedRoundTrip)
				+ this.correctLocationCombinations.logWeight(simulatedRoundTrip);
	}
}