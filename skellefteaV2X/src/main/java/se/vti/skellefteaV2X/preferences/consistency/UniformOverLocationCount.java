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
package se.vti.skellefteaV2X.preferences.consistency;

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;
import se.vti.skellefteaV2X.roundtrips.RoundTripIgnoreChargingCombinations;
import se.vti.skellefteaV2X.roundtrips.RoundTripIgnoreDepartureCombinations;
import se.vti.skellefteaV2X.roundtrips.RoundTripIgnoreLocationCombinations;

/**
 * 
 * @author GunnarF
 *
 */
public class UniformOverLocationCount extends Preferences.Component {

	private final RoundTripIgnoreChargingCombinations<Location> correctChargingCombinations;
	private final RoundTripIgnoreDepartureCombinations<Location> correctDepartureCombinations;
	private final RoundTripIgnoreLocationCombinations<Location> correctLocationCombinations;

	public UniformOverLocationCount(Scenario scenario) {
		this.correctChargingCombinations = new RoundTripIgnoreChargingCombinations<>();
		this.correctDepartureCombinations = new RoundTripIgnoreDepartureCombinations<>(scenario.getBinCnt());
		this.correctLocationCombinations = new RoundTripIgnoreLocationCombinations<>(scenario.getLocationCnt());
	}

	@Override
	public double logWeight(SimulatedRoundTrip simulatedRoundTrip) {
		return this.correctChargingCombinations.logWeight(simulatedRoundTrip)
				+ this.correctDepartureCombinations.logWeight(simulatedRoundTrip)
				+ this.correctLocationCombinations.logWeight(simulatedRoundTrip);
	}
}