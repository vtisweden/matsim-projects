/**
 * se.vti.skellefteaV2X.instances.v0
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
package se.vti.skellefteaV2X.instances.v0;

import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;

public class ChargeAtHomePreference extends PreferenceComponent<ElectrifiedRoundTrip> {

	public final double shareOfHomeChargers;
	
	public ChargeAtHomePreference(double shareOfHomeChargers) {
		this.shareOfHomeChargers = shareOfHomeChargers;
	}
	
	@Override
	public double logWeight(ElectrifiedRoundTrip roundTrip) {
		
		boolean chargeAtHomeIsPossible = roundTrip.getLocation(0).getAllowsCharging();
		boolean wantsToChargeAtHome = roundTrip.getCharging(0);
		
		boolean chargesAtHome = chargeAtHomeIsPossible && wantsToChargeAtHome;
		double weight = (chargesAtHome ? this.shareOfHomeChargers : (1.0 - this.shareOfHomeChargers));
		
		return Math.log(weight);
	}

}
