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
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class BatteryRangePreference implements Preferences.Component {

	private Scenario scenario;
	
	public BatteryRangePreference(Scenario scenario) {
		this.scenario = scenario;
	}
	
	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {
		double range_kWh = 0;
		if (roundTrip.locationCnt() > 1) {
			double minCharge_kWh = Double.POSITIVE_INFINITY;
			double maxCharge_kWh = Double.NEGATIVE_INFINITY;
			for (Episode e : roundTrip.getEpisodes()) {
				if (e instanceof ParkingEpisode) {
					ParkingEpisode p = (ParkingEpisode) e;
					minCharge_kWh = Math.min(minCharge_kWh, p.getChargeAtStart_kWh());
					maxCharge_kWh = Math.max(maxCharge_kWh, p.getChargeAtEnd_kWh());
				}
			}
			range_kWh = maxCharge_kWh - minCharge_kWh;
		}			
		return range_kWh - this.scenario.getMaxCharge_kWh();
	}

}
