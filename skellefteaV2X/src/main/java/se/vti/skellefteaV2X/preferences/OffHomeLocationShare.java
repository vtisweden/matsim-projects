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

import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class OffHomeLocationShare extends LocationShare {

	public OffHomeLocationShare() {
		super();
	}

	@Override
	public double logWeight(SimulatedRoundTrip simulatedRoundTrip) {
		double result = 0.0;
		for (int i = 2; i < simulatedRoundTrip.episodeCnt(); i+= 2) {
			ParkingEpisode p = (ParkingEpisode) simulatedRoundTrip.getEpisodes().get(i);
			result += this.location2logShare.get(p.getLocation()) - this.maxLogShare;
			
		}
		return result;
	}

	
}
