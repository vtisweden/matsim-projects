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

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.preferences.UniformOverLocationCount;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.RoundTripIgnoreChargingCombinations;
import se.vti.skellefteaV2X.model.ElectrifiedLocation;

/**
 * 
 * @author GunnarF
 *
 */
public class UniformOverElectrifiedLocationCount
		extends UniformOverLocationCount<ElectrifiedRoundTrip, ElectrifiedLocation> {

	private final RoundTripIgnoreChargingCombinations correctChargingCombinations;

	public UniformOverElectrifiedLocationCount(Scenario<?> scenario) {
		super(scenario);
		this.correctChargingCombinations = new RoundTripIgnoreChargingCombinations();
	}

	@Override
	public double logWeight(ElectrifiedRoundTrip roundTrip) {
		return super.logWeight(roundTrip) + this.correctChargingCombinations.logWeight(roundTrip);
	}
}