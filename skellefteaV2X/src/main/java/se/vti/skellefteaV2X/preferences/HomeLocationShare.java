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

import java.util.LinkedHashMap;
import java.util.Map;

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class HomeLocationShare implements Preferences.Component {

	private final Map<Location, Double> location2logShare = new LinkedHashMap<>();

	private Double maxLogShare = null;

	public HomeLocationShare() {
	}

	public void setShare(Location location, double share) {
		final double logShare = Math.log(share);
		this.location2logShare.put(location, logShare);
		this.maxLogShare = (this.maxLogShare == null ? logShare : Math.max(this.maxLogShare, logShare));
	}

	@Override
	public double logWeight(SimulatedRoundTrip simulatedRoundTrip) {
		return this.location2logShare.get(simulatedRoundTrip.getHomeLocation()) - this.maxLogShare;
	}

	
}
