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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

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

	public static Set<TransportMode> extractUsedModes(final Collection<List<TransportChain>> collectionOfListOfChains) {
		return collectionOfListOfChains.stream().flatMap(l -> l.stream()).flatMap(c -> c.getLegs().stream())
				.map(l -> l.getMode()).collect(Collectors.toSet());
	}

	public static void reduceToMainModeLegs(final Collection<List<TransportChain>> collectionOfListOfChains) {
		collectionOfListOfChains.stream().flatMap(l -> l.stream()).forEach(c -> reduceToMainModeLegs(c));
	}

	public static void reduceToMainModeLegs(final TransportChain chain) {
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

	public static void reduceToMainModeLegsVerbose(final Collection<List<TransportChain>> collectionOfListOfChains) {
		collectionOfListOfChains.stream().flatMap(l -> l.stream()).forEach(c -> reduceToMainModeLegsVerbose(c));
	}

	public static void reduceToMainModeLegsVerbose(final TransportChain chain) {
		for (TransportLeg leg : chain.getLegs()) {
			System.out.print(leg.getMode() + " ");
		}
		reduceToMainModeLegs(chain);
		System.out.print(" -->  ");
		for (TransportLeg leg : chain.getLegs()) {
			System.out.print(leg.getMode() + " ");
		}
		System.out.println();
	}

	public static void removeChainsByLegCondition(final Collection<List<TransportChain>> collectionOfListOfChains,
			final Function<TransportLeg, Boolean> removeCondition) {
		for (List<TransportChain> testedListOfChains : collectionOfListOfChains) {
			List<TransportChain> chainsToRemove = new ArrayList<>();
			for (TransportChain chain : testedListOfChains) {
				boolean remove = false;
				for (Iterator<TransportLeg> it = chain.getLegs().iterator(); it.hasNext() && !remove;) {
					TransportLeg leg = it.next();
					if (removeCondition.apply(leg)) {
						chainsToRemove.add(chain);
						remove = true;
					}
				}
			}
			testedListOfChains.removeAll(chainsToRemove);
		}
	}

	// TODO CHECKED UNTIL HERE

	public static double computeLength_m(TransportLeg leg) {
		if (leg.getRouteView() == null) {
			return 0.0;
		} else {
			return leg.getRouteView().stream().mapToDouble(l -> l.getLength()).sum();
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
