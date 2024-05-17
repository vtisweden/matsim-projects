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
package se.vti.samgods.models.saana;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportChain;

/**
 * 
 * @author GunnarF
 *
 */
public class SaanaTransportChainReducer {

	public SaanaTransportChainReducer() {
	}

	public void reduce(final Map<OD, List<TransportChain>> od2chains) {
		od2chains.entrySet().stream().forEach(e -> this.reduce(e.getKey(), e.getValue()));
	}

	public void reduce(final OD od, final List<TransportChain> chains) {
//		Log.info("  chains before: " + chains.size());
		final Map<TransportMode, TransportChain> mode2representativeChain = new LinkedHashMap<>(
				SamgodsConstants.TransportMode.values().length);
		final Map<TransportMode, Double> mode2maxLength_m = new LinkedHashMap<>(
				SamgodsConstants.TransportMode.values().length);
		for (TransportChain chain : chains) {
			throw new RuntimeException("Commented out non-compiling code.");
//			for (Map.Entry<TransportMode, Double> entry : TransportChainUtils.computeLengthPerMainMode_m(chain)
//					.entrySet()) {
//				final TransportMode mode = entry.getKey();
//				final double length_m = entry.getValue();
//				if (length_m > mode2maxLength_m.getOrDefault(mode, Double.NEGATIVE_INFINITY)) {
//					mode2representativeChain.put(mode, chain);
//					mode2maxLength_m.put(mode, length_m);
//				}
//			}
		}
		chains.clear();
		chains.addAll(new LinkedHashSet<>(mode2representativeChain.values()));
//		Log.info("  chains after: " + chains.size());
	}
}
