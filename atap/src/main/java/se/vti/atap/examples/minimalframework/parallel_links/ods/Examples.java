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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.examples.minimalframework.parallel_links.NetworkConditionsImpl;
import se.vti.atap.examples.minimalframework.parallel_links.RandomChoiceSetGenerator;
import se.vti.atap.examples.minimalframework.parallel_links.RandomNetworkGenerator;
import se.vti.atap.minimalframework.PlanSelection;
import se.vti.atap.minimalframework.Runner;
import se.vti.atap.minimalframework.defaults.BasicLoggerImpl;
import se.vti.atap.minimalframework.planselection.OneAtATimePlanSelection;
import se.vti.atap.minimalframework.planselection.OnlyBestPlanSelection;
import se.vti.atap.minimalframework.planselection.SortingPlanSelection;
import se.vti.atap.minimalframework.planselection.UniformPlanSelection;
import se.vti.atap.minimalframework.planselection.proposed.LocalSearchPlanSelection;

/**
 * 
 * @author GunnarF
 *
 */
public class Examples {

	static Set<ODPair> createRandomODPairs(Network network, int numberOfODPairs, int numberOfPaths, double demandScale,
			Random rnd) {
		double baselineDemand_veh = (2000.0 * network.getNumberOfLinks()) / numberOfODPairs;
		List<int[]> choiceSets = RandomChoiceSetGenerator.createRandomChoiceSets(numberOfODPairs, numberOfPaths,
				network, rnd);
		Set<ODPair> odPairs = new LinkedHashSet<>(choiceSets.size());
		int n = 0;
		for (int[] choiceSet : choiceSets) {
			odPairs.add(new ODPair("od pair " + (n++), demandScale * baselineDemand_veh, choiceSet));
		}
		return odPairs;
	}

	public static Runner<Paths, ODPair, NetworkConditionsImpl> createModelRunner(Network network, Set<ODPair> odPairs,
			Random rnd) {
		return new Runner<Paths, ODPair, NetworkConditionsImpl>().setAgents(odPairs)
				.setNetworkLoading(new NetworkLoadingImpl(network)).setPlanInnovation(new GreedyInnovation(network))
				.setUtilityFunction(new UtilityFunctionImpl()).setVerbose(false);
	}

	public static LoggerImpl runWithPlanSelection(Runner<Paths, ODPair, NetworkConditionsImpl> runner,
			PlanSelection<ODPair, NetworkConditionsImpl> planSelection, int iterations) {
		runner.setPlanSelection(planSelection);
		runner.setIterations(iterations);
		var logger = new LoggerImpl();
		runner.setLogger(logger);
		runner.run();
		return logger;
	}

	//////////
	//////////
	//////////

	// ==========

	static List<DescriptiveStatistics> computeUpdated(List<DescriptiveStatistics> stats, List<Double> values) {
		List<DescriptiveStatistics> updatedStats = stats;
		if (updatedStats == null) {
			updatedStats = new ArrayList<>(values.size());
			for (int i = 0; i < values.size(); i++) {
				updatedStats.add(new DescriptiveStatistics());
			}
		}
		for (int i = 0; i < values.size(); i++) {
			updatedStats.get(i).addValue(values.get(i));
		}
		return updatedStats;
	}

	public static void runAllMethodsOnRandomNetwork(long seed, int numberOfLinks, int numberOfODPairs,
			int numberOfPaths, double demandScale, int iterations, int replications, String fileName) {

		Random rnd = new Random(seed);

		List<DescriptiveStatistics> oneAtATimeStats = null;
		List<DescriptiveStatistics> onlyBestStats = null;
		List<DescriptiveStatistics> uniformStats = null;
		List<DescriptiveStatistics> sortingStats = null;
		List<DescriptiveStatistics> proposedStats = null;

		for (int replication = 0; replication < replications; replication++) {

			System.out.println((replication + 1) + "/" + replications);

			Network network = RandomNetworkGenerator.createRandomNetwork(numberOfLinks, 60.0, 600.0, 1000.0, 3000.0,
					rnd);
			Set<ODPair> odPairs = createRandomODPairs(network, numberOfODPairs, numberOfPaths, demandScale, rnd);

			Random rndForRunner = null;

			oneAtATimeStats = computeUpdated(oneAtATimeStats,
					runWithPlanSelection(createModelRunner(network, odPairs, rndForRunner),
							new OneAtATimePlanSelection<>(null), iterations).getAverageGaps());
			onlyBestStats = computeUpdated(onlyBestStats,
					runWithPlanSelection(createModelRunner(network, odPairs, rndForRunner),
							new OnlyBestPlanSelection<>(), iterations).getAverageGaps());
			uniformStats = computeUpdated(uniformStats,
					runWithPlanSelection(createModelRunner(network, odPairs, rndForRunner),
							new UniformPlanSelection<>(-1.0, rnd), iterations).getAverageGaps());
			sortingStats = computeUpdated(sortingStats,
					runWithPlanSelection(createModelRunner(network, odPairs, rndForRunner),
							new SortingPlanSelection<>(-1.0), iterations).getAverageGaps());			
			proposedStats = computeUpdated(proposedStats,
					runWithPlanSelection(createModelRunner(network, odPairs, rndForRunner),
							new LocalSearchPlanSelection<>(-1.0, rnd, new ApproximateNetworkLoadingImpl(network))
									.setApproximateDistance(true).setMinimalRelativeImprovement(1e-8),
							iterations).getAverageGaps());

			try {
				PrintWriter writer = new PrintWriter(fileName);
				writer.println("Iteration\tOneAtATimeGap\tOnlyLargestGap\tUniform\tSorting\tProposed");
				for (int i = 0; i < oneAtATimeStats.size(); i++) {
					writer.print(i);
					writer.print("\t");
					writer.print(oneAtATimeStats.get(i).getMean());
					writer.print("\t");
					writer.print(onlyBestStats.get(i).getMean());
					writer.print("\t");
					writer.print(uniformStats.get(i).getMean());
					writer.print("\t");
					writer.print(sortingStats.get(i).getMean());
					writer.print("\t");
					writer.print(proposedStats.get(i).getMean());
					writer.println();
				}
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException();
			}
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
