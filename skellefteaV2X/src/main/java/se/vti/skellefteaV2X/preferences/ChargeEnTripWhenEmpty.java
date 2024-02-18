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
package se.vti.skellefteaV2X.preferences;

import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class ChargeEnTripWhenEmpty extends Preferences.Component {

	@Override
	public double logWeight(SimulatedRoundTrip simulatedRoundTrip) {
		double remainingWhenCharging_kWh = 0.0;
		for (Episode e : simulatedRoundTrip.getEpisodes()) {
			if (e != simulatedRoundTrip.getEpisodes().get(0) && e instanceof ParkingEpisode) {
				ParkingEpisode p = (ParkingEpisode) e;
				if (p.getChargeAtEnd_kWh() > p.getChargeAtStart_kWh()) {
					remainingWhenCharging_kWh += Math.max(0.0, p.getChargeAtStart_kWh());
				}
			}
		}
		return -remainingWhenCharging_kWh;
	}

}