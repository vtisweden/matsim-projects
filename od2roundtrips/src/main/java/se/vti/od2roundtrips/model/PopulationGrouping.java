/**
 * od2roundtrips.model
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
package se.vti.od2roundtrips.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class PopulationGrouping {

	private final int populationSize;

	private final Map<String, Double> group2weights = new LinkedHashMap<>();

	private Map<String, int[]> group2indices = null;

	public PopulationGrouping(int popuationSize) {
		this.populationSize = popuationSize;
	}

	public void addGroup(String name, double weight) {
		this.group2weights.put(name, weight);
	}

	private void ensureIndexing() {
		if (this.group2indices != null) {
			return;
		}
		final double weightSum = this.group2weights.values().stream().mapToDouble(w -> w).sum();
		Map<String, List<Integer>> group2indexList = this.group2weights.keySet().stream()
				.collect(Collectors.toMap(g -> g, g -> new ArrayList<>()));
		Map<String, Double> group2slack = this.group2weights.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue() / weightSum * this.populationSize));
		for (int i = 0; i < this.populationSize; i++) {
			String worstGroup = group2slack.entrySet().stream()
					.max((e1, e2) -> Double.compare(e1.getValue(), e2.getValue())).get().getKey();
			group2indexList.get(worstGroup).add(i);
			group2slack.compute(worstGroup, (g, s) -> s - 1.0);
		}
		this.group2indices = group2indexList.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().stream().mapToInt(i -> i).toArray()));
	}

	public Function<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>, MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> createFilter(
			String group) {
		this.ensureIndexing();
		return new Function<>() {
			@Override
			public MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> apply(
					MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> multiRoundTrip) {
				final int[] indices = group2indices.get(group);
				MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> result = new MultiRoundTripWithOD<>(indices.length);
				for (int i = 0; i < indices.length; i++) {
					result.setRoundTrip(i, multiRoundTrip.getRoundTrip(indices[i]));
				}
				return result;
			}
		};
	}

	public static void main(String[] args) {
		for (int size = 10; size <= 10; size++) {

			PopulationGrouping g = new PopulationGrouping(size);
			g.addGroup("a", 1.0);
			g.addGroup("b", 2.0);
			g.addGroup("c", 4.0);

			g.ensureIndexing();

			System.out.println("population size");
			for (Map.Entry<String, int[]> e : g.group2indices.entrySet()) {
				System.out.println(e.getKey() + "\t" + Arrays.toString(e.getValue()));
			}
			System.out.println();

		}
	}

}
