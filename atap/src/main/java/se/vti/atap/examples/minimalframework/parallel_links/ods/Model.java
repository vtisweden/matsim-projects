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

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.examples.minimalframework.parallel_links.RandomScenarioGenerator;
import se.vti.atap.minimalframework.ApproximateNetworkLoading;
import se.vti.atap.minimalframework.ExactNetworkLoading;
import se.vti.atap.minimalframework.Logger;
import se.vti.atap.minimalframework.PlanInnovation;
import se.vti.atap.minimalframework.PlanSelection;
import se.vti.atap.minimalframework.UtilityFunction;
import se.vti.atap.minimalframework.common.BasicLoggerImpl;
import se.vti.atap.minimalframework.common.DoubleArrayDistance;
import se.vti.atap.minimalframework.common.DoubleArrayWrapper;
import se.vti.atap.minimalframework.common.LocalSearchPlanSelection;
import se.vti.atap.minimalframework.common.OneAtATimePlanSelection;
import se.vti.atap.minimalframework.common.OnlyBestPlanSelection;
import se.vti.atap.minimalframework.common.Runner;
import se.vti.atap.minimalframework.common.SortingPlanSelection;
import se.vti.atap.minimalframework.common.UniformPlanSelection;

/**
 * 
 * @author GunnarF
 *
 */
public class Model {

	private final Network network;

	private final Set<ODPair> agents;

	public Model(Network network, Set<ODPair> agents) {
		this.network = network;
		this.agents = agents;
	}

	public Network getNetwork() {
		return this.network;
	}

	public Set<ODPair> getAgents() {
		return this.agents;
	}

	public ApproximateNetworkLoading<DoubleArrayWrapper, DoubleArrayWrapper, ODPair> createApproximateNetworkLoading(
			boolean scaleWithCapacity) {
		return new ApproximateNetworkLoading<>() {
			@Override
			public DoubleArrayWrapper computeFlows(Set<ODPair> odPairsUsingCurrentPathFlows,
					Set<ODPair> odPairsUsingCandidatePathFlows, DoubleArrayWrapper travelTimes) {
				DoubleArrayWrapper linkFlows_veh = new DoubleArrayWrapper(network.getNumberOfLinks());
				for (var odPair : odPairsUsingCurrentPathFlows) {
					odPair.addCurrentPathFlowsToLinkFlows(linkFlows_veh);
				}
				for (var odPair : odPairsUsingCandidatePathFlows) {
					odPair.addCandidatePathFlowsToLinkFlows(linkFlows_veh);
				}
				if (scaleWithCapacity) {
					for (int link = 0; link < network.getNumberOfLinks(); link++) {
						linkFlows_veh.data[link] /= network.cap_veh[link];
					}
				}
				return linkFlows_veh;
			}
		};
	}

	public UtilityFunction<DoubleArrayWrapper, ODPair, Paths> createUtilityFunction() {
		return new UtilityFunction<>() {
			@Override
			public double computeUtility(ODPair odPair, Paths paths, DoubleArrayWrapper travelTimes_s) {
				return (-1.0) * new SingleODBeckmanApproximation(odPair, travelTimes_s, network).compute(paths);
			}
		};
	}

	public ExactNetworkLoading<DoubleArrayWrapper, ODPair> createExactNetworkLoading() {
		return new ExactNetworkLoading<>() {
			@Override
			public DoubleArrayWrapper computeNetworkLoading(Set<ODPair> agents) {
				double[] flows = createApproximateNetworkLoading(false).computeFlows(agents, Collections.emptySet(),
						null).data;
				DoubleArrayWrapper travelTimes = new DoubleArrayWrapper(network.getNumberOfLinks());
				for (int i = 0; i < network.getNumberOfLinks(); i++) {
					travelTimes.data[i] = network.computeTravelTime_s(i, flows[i]);
				}
				return travelTimes;
			}
		};
	}

	public PlanInnovation<DoubleArrayWrapper, ODPair> createBestResponsePlanInnovation() {
		return new GreedyInnovation(this.network);
	}

	// ==========

	public static Model createRandomModel(long seed, int numberOfLinks, int numberOfODPairs, int numberOfPaths,
			double demandScale) {
		RandomScenarioGenerator gen = new RandomScenarioGenerator(seed);
		Network network = gen.createRandomNetwork(numberOfLinks, numberOfLinks, 60.0 - 1e-8, 600.0 + 1e-8, 1000.0 - 1e-8,
				3000.0 + 1e-8);
		double baselineDemand_veh = (2000.0 * numberOfLinks) / numberOfODPairs;
		Set<ODPair> odPairs = gen.createRandomOdDemand(numberOfODPairs, numberOfODPairs,
				demandScale * baselineDemand_veh - 1e-8, demandScale * baselineDemand_veh + 1e-8, numberOfPaths,
				numberOfPaths, network);
		return new Model(network, odPairs);
	}

	public static Runner<DoubleArrayWrapper, ODPair, Paths> createRunner(Model model, int iterations) {
		var runner = new Runner<DoubleArrayWrapper, ODPair, Paths>();
		runner.setAgents(model.getAgents()).setIterations(iterations).setNetworkLoading(model.createExactNetworkLoading())
				.setPlanInnovation(model.createBestResponsePlanInnovation())
				.setUtilityFunction(model.createUtilityFunction()).setLogger(new BasicLoggerImpl<>());
		return runner;
	}

	public static Logger<DoubleArrayWrapper, ODPair> runWithPlanSelection(Model model,
			PlanSelection<DoubleArrayWrapper, ODPair> planSelection) {
		var runner = createRunner(model, 1000);
		runner.setPlanSelection(planSelection);
		runner.run();
		return runner.getLogger();
	}

	public static void runAllMethods(Model model) {

//		System.out.println();
//		System.out.println("one at a time");
		List<Double> selectOneAtATimeGaps = runWithPlanSelection(model, new OneAtATimePlanSelection<>())
				.getAverageGaps();
//		System.out.println();
//		System.out.println("only best");
		List<Double> selectOnlyBestGaps = runWithPlanSelection(model, new OnlyBestPlanSelection<>()).getAverageGaps();
//		System.out.println();
//		System.out.println("uniform");
		List<Double> uniformMethodGaps = runWithPlanSelection(model, new UniformPlanSelection<>(-1.0)).getAverageGaps();
//		System.out.println();
//		System.out.println("sorting");
		List<Double> sortingMethodGaps = runWithPlanSelection(model, new SortingPlanSelection<>(-1.0)).getAverageGaps();
//		System.out.println();
//		System.out.println("proposed");
//		List<Double> proposedMethodGaps = runWithPlanSelection(model, new LocalSearchPlanSelection<>(
//				model.createApproximateNetworkLoading(true), new DoubleArrayDistance(), -1.0)).getAverageGaps();
		List<Double> proposedMethodGaps = runWithPlanSelection(model, new LocalSearchPlanSelection<>(
				model.createApproximateNetworkLoading(true), new DoubleArrayDistance(), -1.0)).getAverageGaps();

		System.out.println("Iteration\tOneAtATime\tOnlyLargetGap\tUniform\tSorting\tProposed");
		for (int i = 0; i < selectOnlyBestGaps.size(); i++) {
			System.out.print(i);
			System.out.print("\t");
			System.out.print(selectOneAtATimeGaps.get(i));
			System.out.print("\t");
			System.out.print(selectOnlyBestGaps.get(i));
			System.out.print("\t");
			System.out.print(uniformMethodGaps.get(i));
			System.out.print("\t");
			System.out.print(sortingMethodGaps.get(i));
			System.out.print("\t");
			System.out.println(proposedMethodGaps.get(i));
		}

	}

	public static void main(String[] args) {

		runAllMethods(createRandomModel(new Random().nextLong(), 1000, 100, 100, 1.0));
//		runAllMethods(createRandomModel(new Random().nextLong(), 1000, 1000, 10, 4.0));
//		runAllMethods(createRandomModel(new Random().nextLong(), 1000, 10, 1000, 4.0));

	}

	// ==========

//	public static Model createCityWithDetourModel() {
//		Network network = new Network(5).setBPRParameters(0, 20.0 * 60.0, 4000.0, 4.0)
//				.setBPRParameters(1, 10.0 * 60.0, 1500.0, 4.0).setBPRParameters(2, 10.0 * 60.0, 1000.0, 4.0)
//				.setBPRParameters(3, 10.0 * 60.0, 500.0, 4.0).setBPRParameters(4, 10.0 * 60.0, 2000.0, 4.0);
//
//		Model model = new Model();
//		model.setNetwork(network);
//
//		model.createODPair("outside", 5000.0, 0, 1, 2, 3, 4);
//		model.createODPair("inside1", 2500.0, 1, 2, 3);
//		model.createODPair("inside2", 2500.0, 2, 3, 4);
//
//		return model;
//	}

	// ==========

//	public static Model createSmallExampleModel() {
//		Model model = new Model();
//		model.setNetwork(new Network(2).setAllBPRParameters(60.0, 50, 4));
//		model.createODPair("single od", 100.0, 0, 1);
//		return model;
//	}
//
//	public static Model createLargeRandomExample() {
//		Model exampleRunner = new Model();
//		RandomScenarioGenerator scenarioGenerator = new RandomScenarioGenerator(4711);
//		// 1000 links with avg capacity 1000.0
//		Network network = scenarioGenerator.createRandomNetwork(1000, 1000, 60.0, 900.0, 100.0, 1900.0, 1.0, 2.0);
//		// 100 ods with avg demand 2000.0 und avg 100 route alternatives
//		Set<ODPair> odPairs = scenarioGenerator.createRandomOdDemand(100, 100, 1000.0, 3000.0, 2, 198, network);
//		exampleRunner.setNetwork(network);
//		exampleRunner.setODPairs(odPairs);
//		return exampleRunner;
//	}
//
//
//	public static String runSmallExampleWithUniformMethod() {
//		var model = createSmallExampleModel();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new UniformPlanSelection<>(-1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runSmallExampleWithSortingMethod() {
//		var model = createSmallExampleModel();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new SortingPlanSelection<>(-1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runSmallExampleWithProposedMethod() {
//		var model = createSmallExampleModel();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new LocalSearchPlanSelection<>(model.createApproximateNetworkLoading(),
//				new DoubleArrayDistance(), -1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runLargeRandomExampleWithUniformMethod() {
//		var model = createLargeRandomExample();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new UniformPlanSelection<>(-1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runLargeRandomExampleWithSortingMethod() {
//		var model = createLargeRandomExample();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new SortingPlanSelection<>(-1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runLargeRandomExampleWithProposedMethod() {
//		var model = createLargeRandomExample();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new LocalSearchPlanSelection<>(model.createApproximateNetworkLoading(),
//				new DoubleArrayDistance(), -1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runLargeRandomExampleWithOneAtATimeMethod() {
//		var model = createLargeRandomExample();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new OneAtATimePlanSelection<>());
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runLargeRandomExampleWithOnlyBestPlanMethod() {
//		var model = createLargeRandomExample();
//		var runner = createRunner(model);
//		runner.setPlanSelection(new OnlyBestPlanSelection<>());
//		runner.run();
//		return runner.getLogger().toString();
//	}
}
