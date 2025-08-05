/**
 * se.vti.atap.examples.minimalframework.parallel_links
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.examples.minimalframework.parallel_links;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * 
 * @author GunnarF
 *
 */
public class RandomChoiceSetGenerator {

	private RandomChoiceSetGenerator() {
	}

	public static List<int[]> createRandomChoiceSets(int numberOfChoiceSets, int numberOfAlternatives, Network network,
			Random rnd) {
		List<int[]> result = new ArrayList<>(numberOfChoiceSets);
		List<Integer> links = new ArrayList<>(IntStream.range(0, network.getNumberOfLinks()).boxed().toList());
		for (int i = 0; i < numberOfChoiceSets; i++) {
			Collections.shuffle(links, rnd);
			result.add(links.subList(0, numberOfAlternatives).stream().mapToInt(l -> l).toArray());
		}
		return result;
	}
}
