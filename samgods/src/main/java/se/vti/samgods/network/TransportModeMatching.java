/**
 * se.vti.samgods.network
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
package se.vti.samgods.network;

import java.util.LinkedHashSet;
import java.util.Set;

import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * Putting this into its own class to keep the special ferry cases in one place.
 * 
 * TODO Put even the "samgods network mode" matching here.
 *  
 * @author GunnarF
 *
 */
public class TransportModeMatching {

	private TransportModeMatching() {
	}

	// TODO no synchronization here
	public synchronized static String getMatsimModeIgnoreFerry(TransportMode samgodsMode) {
		if (TransportMode.Road.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.car;
		}
		if (TransportMode.Rail.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.train;
		}
		if (TransportMode.Sea.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.ship;
		}
		if (TransportMode.Air.equals(samgodsMode)) {
			return org.matsim.api.core.v01.TransportMode.airplane;
		}
		throw new RuntimeException("No MATSim mode available for " + samgodsMode);
	}

	// TODO no synchronization here
	public synchronized static Set<String> computeMatsimModes(SamgodsLinkAttributes linkAttributes) {
		final Set<String> result = new LinkedHashSet<>(2);
		if (linkAttributes.isFerryLink()) {
			if (linkAttributes.isRoadFerryLink()) {
				result.add(org.matsim.api.core.v01.TransportMode.car);
			}
			if (linkAttributes.isRailFerryLink()) {
				result.add(org.matsim.api.core.v01.TransportMode.train);
			}
		} else {
			result.add(getMatsimModeIgnoreFerry(linkAttributes.samgodsMode));
		}
		return result;
	}

}
