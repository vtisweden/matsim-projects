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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChainUtils {

	private TransportChainUtils() {
	}

	public static class TransportChainStats {

		public final Commodity commodity;

		private int originCnt = 0;
		private int destinationCnt = 0;
		private int odCnt = 0;

		public TransportChainStats(Commodity commodity) {
			this.commodity = commodity;
		}

	}

	public static void computeTransportChainStats(Commodity commodity, Map<OD, List<TransportChain>> od2chains) {

		for (Map.Entry<OD, List<TransportChain>> entry : od2chains.entrySet()) {
			OD od = entry.getKey();
			for (TransportChain chain : entry.getValue()) {
				chain.getOrigin();
				chain.getDestination();
				for (TransportLeg leg : chain.getLegs()) {
					leg.getMode();
					leg.getOrigin();
					leg.getDestination();
					leg.getRoute();

				}

			}
		}

	}

	// SO FAR USED BELOW

	public static Set<TransportMode> extractUsedModes(Collection<List<TransportChain>> collectionOfChains) {
		return collectionOfChains.stream().flatMap(l -> l.stream()).flatMap(c -> c.getLegs().stream())
				.map(l -> l.getMode()).collect(Collectors.toSet());
	}

	public static void reduceToMainModeLegs(Map<Commodity, Map<OD, List<TransportChain>>> commodity2od2chains) {
		commodity2od2chains.values().stream().forEach(m -> reduceToMainModeLegs(m.values()));
	}

	public static void reduceToMainModeLegs(Collection<List<TransportChain>> collectionOfChains) {
		collectionOfChains.stream().flatMap(l -> l.stream()).forEach(c -> reduceToMainModeLegs(c));
	}

	public static void reduceToMainModeLegs(TransportChain chain) {
		if (chain.getLegs().size() < 2) {
			return;
		}
		final List<TransportLeg> newLegs = new LinkedList<TransportLeg>();
		Id<Node> currentOrigin = chain.getOrigin();
		TransportMode currentMode = chain.getLegs().get(0).getMode();
		for (TransportLeg leg : chain.getLegs()) {
			if (!leg.getMode().equals(currentMode)) {
				newLegs.add(new TransportLeg(currentOrigin, leg.getOrigin(), currentMode, '?'));
				currentOrigin = leg.getOrigin();
				currentMode = leg.getMode();
			}
		}
		newLegs.add(new TransportLeg(currentOrigin, chain.getDestination(), currentMode, '?'));

		chain.getLegs().clear();
		chain.getLegs().addAll(newLegs);
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
