/**
 * se.vti.samgods
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
package se.vti.samgods.logistics;

import java.util.LinkedHashMap;
import java.util.Map;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChainUtils {

	private TransportChainUtils() {
	}

	public static double computeLength_m(TransportLeg leg) {
		if (leg.getRoute() == null) {
			return 0.0;
		} else {
			return leg.getRoute().stream().mapToDouble(l -> l.getLength()).sum();
		}
	}

	public static Map<TransportMode, Double> computeLengthPerMainMode_m(TransportChain chain) {
		final Map<TransportMode, Double> result = new LinkedHashMap<>(SamgodsConstants.TransportMode.values().length);
		for (TransportLeg leg : chain.getLegs()) {
			final double length_m = TransportChainUtils.computeLength_m(leg);
			result.compute(leg.getMode(), (m, l) -> l == null ? length_m : l + length_m);
		}
		return result;
	}

}
