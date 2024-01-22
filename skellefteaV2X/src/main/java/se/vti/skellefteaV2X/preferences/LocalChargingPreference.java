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
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences.Component;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class LocalChargingPreference extends Component {
	
	private final Scenario scenario;
	
	private final Location location;

	private final double minCharging_kWh;
	
	public LocalChargingPreference(Scenario scenario, Location location, double minCharging_kWh) {
		this.scenario = scenario;
		this.location = location;
		this.minCharging_kWh = minCharging_kWh;
		this.setLogWeightThreshold(-1e-8);
	}

	@Override
	public double logWeight(SimulatedRoundTrip roundTrip) {
		double amount_kWh = 0.0;
		for (Episode e : roundTrip.getEpisodes()) {
			if (e instanceof ParkingEpisode) {
				ParkingEpisode p = (ParkingEpisode) e;
				if (this.location.equals(p.getLocation())) {
					amount_kWh += p.getChargeAtEnd_kWh() - p.getChargeAtStart_kWh();
				}
			}
		}
		return Math.min(0.0, amount_kWh - this.minCharging_kWh) / this.scenario.getMaxCharge_kWh();
	}

}
