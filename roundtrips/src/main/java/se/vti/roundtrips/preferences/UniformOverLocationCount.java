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
package se.vti.roundtrips.preferences;

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripIgnoreDepartureCombinations;
import se.vti.roundtrips.single.RoundTripIgnoreLocationCombinations;

/**
 * 
 * @author GunnarF
 *
 */
public class UniformOverLocationCount<R extends RoundTrip<L>, L extends Location> extends Preferences.Component<R, L> {

	private final RoundTripIgnoreDepartureCombinations correctDepartureCombinations;
	private final RoundTripIgnoreLocationCombinations correctLocationCombinations;

	public UniformOverLocationCount(Scenario<?> scenario) {
		this.correctDepartureCombinations = new RoundTripIgnoreDepartureCombinations(scenario.getBinCnt());
		this.correctLocationCombinations = new RoundTripIgnoreLocationCombinations(scenario.getLocationCnt(),
				scenario.getMaxParkingEpisodes());
	}

	@Override
	public double logWeight(R simulatedRoundTrip) {
		return this.correctDepartureCombinations.logWeight(simulatedRoundTrip)
				+ this.correctLocationCombinations.logWeight(simulatedRoundTrip);
	}
}