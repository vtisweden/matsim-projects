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
public class LocalChargingAmountPrefence implements Component {

	private final double maxAmount_kWh;
	
	private final Location location;

	public LocalChargingAmountPrefence(Scenario scenario, Location location) {
		this.maxAmount_kWh = scenario.getMaxCharge_kWh();
		this.location = location;
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
		amount_kWh = Math.min(amount_kWh, this.maxAmount_kWh);		
		return amount_kWh - this.maxAmount_kWh;
	}

}
