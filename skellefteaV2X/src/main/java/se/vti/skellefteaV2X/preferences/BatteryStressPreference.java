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
package se.vti.skellefteaV2X.preferences;

import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class BatteryStressPreference extends Preferences.Component {

	private final Scenario scenario;

	public BatteryStressPreference(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {
		double stress_kWh = 0;
		if (roundTrip.locationCnt() > 1) {
			for (Episode e : roundTrip.getEpisodes()) {
				stress_kWh += Math.abs(e.getChargeAtStart_kWh() - e.getChargeAtEnd_kWh());
			}
		}
		return Math.min(0.0, stress_kWh - 2.0 * this.scenario.getMaxCharge_kWh());
	}
	
	public void setBatteryChangeThreshold_kWh(double threshold_kWh) {
		this.setLogWeightThreshold(threshold_kWh - 2.0 * this.scenario.getMaxCharge_kWh());
	}

}
