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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import se.vti.atap.examples.minimalframework.parallel_links.agents.AgentImpl;
import se.vti.atap.examples.minimalframework.parallel_links.ods.ODPair;

/**
 * 
 * @author GunnarF
 *
 */
public class RandomScenarioGenerator {

	private final Random rnd;

	public RandomScenarioGenerator(long seed) {
		this.rnd = new Random(seed);
	}

	public RandomScenarioGenerator() {
		this.rnd = new Random();
	}

	public Network createRandomNetwork(int minNumberOfLinks, int maxNumberOfLinks, double minT0_s, double maxT0_s,
			double minCap_veh, double maxCap_veh) {
		Network network = new Network(this.rnd.nextInt(minNumberOfLinks, maxNumberOfLinks + 1));
		for (int link = 0; link < network.getNumberOfLinks(); link++) {
			network.setBPRParameters(link, this.rnd.nextDouble(minT0_s, maxT0_s),
					this.rnd.nextDouble(minCap_veh, maxCap_veh));
		}
		return network;
	}

	private List<int[]> createRandomPathChoiceSets(int minNumberOfChoiceSets, int maxNumberOfChoiceSets,
			int minNumberOfAlternatives, int maxNumberOfAlternatives, Network network) {
		int numberOfChoiceSets = this.rnd.nextInt(minNumberOfChoiceSets, maxNumberOfChoiceSets + 1);
		List<int[]> result = new ArrayList<>(numberOfChoiceSets);
		List<Integer> links = new ArrayList<>(IntStream.range(0, network.getNumberOfLinks()).boxed().toList());
		for (int i = 0; i < numberOfChoiceSets; i++) {
			Collections.shuffle(links, this.rnd);
			int choiceSetSize = this.rnd.nextInt(minNumberOfAlternatives, maxNumberOfAlternatives + 1);
			result.add(links.subList(0, choiceSetSize).stream().mapToInt(l -> l).toArray());
		}
		return result;
	}

	public Set<AgentImpl> createRandomAgentDemand(int minNumberOfAgents, int maxNumberOfAgents,
			int minNumberOfAlternatives, int maxNumberOfAlternatives, Network network) {
		List<int[]> choiceSets = this.createRandomPathChoiceSets(minNumberOfAgents, maxNumberOfAgents,
				minNumberOfAlternatives, maxNumberOfAlternatives, network);
		Set<AgentImpl> result = new LinkedHashSet<>(choiceSets.size());
		int n = 0;
		for (int[] choiceSet : choiceSets) {
			result.add(new AgentImpl("agent " + (n++), choiceSet));
		}
		return result;
	}

	public Set<ODPair> createRandomOdDemand(int minNumberOfODPairs, int maxNumberOfODPairs, double minDemand_veh,
			double maxDemand_veh, int minNumberOfPaths, int maxNumberOfPaths, Network network) {
		List<int[]> choiceSets = this.createRandomPathChoiceSets(minNumberOfODPairs, maxNumberOfODPairs,
				minNumberOfPaths, maxNumberOfPaths, network);
		Set<ODPair> result = new LinkedHashSet<>(choiceSets.size());
		int n = 0;
		for (int[] choiceSet : choiceSets) {
			result.add(new ODPair("od pair " + (n++), this.rnd.nextDouble(minDemand_veh, maxDemand_veh), choiceSet));
		}
		return result;
	}
}
