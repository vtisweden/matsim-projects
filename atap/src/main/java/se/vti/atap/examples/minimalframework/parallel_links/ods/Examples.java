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
package se.vti.atap.examples.minimalframework.parallel_links.ods;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.examples.minimalframework.parallel_links.NetworkConditionsImpl;
import se.vti.atap.examples.minimalframework.parallel_links.RandomScenarioGenerator;
import se.vti.atap.minimalframework.Runner;
import se.vti.atap.minimalframework.planselection.OneAtATimePlanSelection;
import se.vti.atap.minimalframework.planselection.SortingPlanSelection;
import se.vti.atap.minimalframework.planselection.UniformPlanSelection;
import se.vti.atap.minimalframework.planselection.proposed.LocalSearchPlanSelection;

/**
 * 
 * @author GunnarF
 *
 */
public class Examples {

	public static Set<ODPair> createRandomODPairs(Network network, int numberOfODPairs, int numberOfPaths,
			double volumeCapacityRatio, Random rnd) {
		double networkCapacityPerODPair_veh = Arrays.stream(network.cap_veh).sum() / numberOfODPairs;
		List<int[]> pathSets = RandomScenarioGenerator.createRandomPathSets(numberOfODPairs, numberOfPaths, network,
				rnd);
		Set<ODPair> odPairs = new LinkedHashSet<>(pathSets.size());
		int n = 0;
		for (int[] pathSet : pathSets) {
			odPairs.add(new ODPair("od pair " + (n++), volumeCapacityRatio * networkCapacityPerODPair_veh, pathSet));
		}
		return odPairs;
	}

	public static Runner<Paths, ODPair, NetworkConditionsImpl> createRunner(Network network, Set<ODPair> odPairs) {
		return new Runner<Paths, ODPair, NetworkConditionsImpl>().setAgents(odPairs)
				.setNetworkLoading(new NetworkLoadingImpl(network)).setPlanInnovation(new GreedyInnovation(network))
				.setUtilityFunction(new UtilityFunctionImpl()).setVerbose(false);
	}

	public static void runAllMethodsOnRandomNetwork(long seed, int numberOfLinks, int numberOfODPairs,
			int numberOfPaths, double demandScale, int iterations, int replications, String fileName) {
		Random rnd = new Random(seed);

		LoggerImpl oneAtATimeLogger = new LoggerImpl();
		LoggerImpl uniformLogger = new LoggerImpl();
		LoggerImpl sortingLogger = new LoggerImpl();
		LoggerImpl proposedLogger = new LoggerImpl();

		LogComparisonPrinter<LoggerImpl> comparison = new LogComparisonPrinter<>();
		comparison.addLogger("OneAtATime", oneAtATimeLogger).addLogger("Uniform", uniformLogger)
				.addLogger("Sorting", sortingLogger).addLogger("Proposed", proposedLogger);
		comparison.addStatistic("10Percentile", ds -> ds.getPercentile(10)).addStatistic("90Percentile",
				ds -> ds.getPercentile(90));

		for (int replication = 0; replication < replications; replication++) {
			System.out.println((replication + 1) + "/" + replications);

			Network network = RandomScenarioGenerator.createRandomNetwork(numberOfLinks, 60.0, 600.0, 1000.0, 3000.0,
					rnd);
			Set<ODPair> odPairs = createRandomODPairs(network, numberOfODPairs, numberOfPaths, demandScale, rnd);

			createRunner(network, odPairs).setPlanSelection(new OneAtATimePlanSelection<>(null))
					.setIterations(iterations).setLogger(oneAtATimeLogger).run();
			createRunner(network, odPairs).setPlanSelection(new UniformPlanSelection<>(-1.0, rnd))
					.setIterations(iterations).setLogger(uniformLogger).run();
			createRunner(network, odPairs).setPlanSelection(new SortingPlanSelection<>(-1.0)).setIterations(iterations)
					.setLogger(sortingLogger).run();

			var proposedPlanSelection = new LocalSearchPlanSelection<>(-1.0, rnd,
					new ApproximateNetworkLoadingImpl(network)).setApproximateDistance(true)
					.setMinimalRelativeImprovement(1e-8);
			createRunner(network, odPairs).setPlanSelection(proposedPlanSelection).setIterations(iterations)
					.setLogger(proposedLogger).run();

			// comparison.printToConsole();
			comparison.printToFile(fileName);
		}
	}

	public static void main(String[] args) {

		int numberOfLinks = Integer.parseInt(args[0]);
		int numberOfODPairs = Integer.parseInt(args[1]);
		int numberOfRoutes = Integer.parseInt(args[2]);
		double demandScale = Double.parseDouble(args[3]);
		int iterations = Integer.parseInt(args[4]);
		int replications = Integer.parseInt(args[5]);

		runAllMethodsOnRandomNetwork(4711, numberOfLinks, numberOfODPairs, numberOfRoutes, demandScale, iterations,
				replications,
				numberOfLinks + "links_" + numberOfODPairs + "odPairs_" + numberOfRoutes + "routes_" + demandScale
						+ "demandScale_" + iterations + "iterations_" + replications + "replications.tsv");
	}

}
