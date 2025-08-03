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
import java.util.LinkedHashSet;
import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.examples.minimalframework.parallel_links.RandomScenarioGenerator;
import se.vti.atap.minimalframework.ApproximateNetworkLoading;
import se.vti.atap.minimalframework.ExactNetworkLoading;
import se.vti.atap.minimalframework.PlanInnovation;
import se.vti.atap.minimalframework.UtilityFunction;
import se.vti.atap.minimalframework.common.BasicLoggerImpl;
import se.vti.atap.minimalframework.common.DoubleArrayDistance;
import se.vti.atap.minimalframework.common.DoubleArrayWrapper;
import se.vti.atap.minimalframework.common.LocalSearchPlanSelection;
import se.vti.atap.minimalframework.common.Runner;
import se.vti.atap.minimalframework.common.SortingPlanSelection;
import se.vti.atap.minimalframework.common.UniformPlanSelection;

/**
 * 
 * @author GunnarF
 *
 */
public class ExampleRunner {

	private Set<ODPair> agents = new LinkedHashSet<>();

	private Network network = null;

	public ExampleRunner() {
	}

	public void createODPair(String id, Double demand_veh, int... availableLinks) {
		this.agents.add(new ODPair(id, demand_veh, availableLinks));
	}

	public Set<ODPair> getAgents() {
		return this.agents;
	}
	
	public void setODPairs(Set<ODPair> agents) {
		this.agents = agents;
	}

	public void setNetwork(Network network) {
		this.network = network;
	}

	public ApproximateNetworkLoading<DoubleArrayWrapper, DoubleArrayWrapper, ODPair> createApproximateNetworkLoading() {
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
				return linkFlows_veh;
			}
		};
	}

	public UtilityFunction<DoubleArrayWrapper, ODPair, Paths> createUtilityFunction() {
		return new UtilityFunction<>() {
			@Override
			public double computeUtility(ODPair odPair, Paths paths, DoubleArrayWrapper travelTimes) {
				return (-1.0) * odPair.computeCurrentTotalTravelTime_s(paths, travelTimes);
			}
		};
	}

	public ExactNetworkLoading<DoubleArrayWrapper, ODPair> createExactNetworkLoading() {
		return new ExactNetworkLoading<>() {
			@Override
			public DoubleArrayWrapper computeNetworkLoading(Set<ODPair> agents) {
				double[] flows = createApproximateNetworkLoading().computeFlows(agents, Collections.emptySet(),
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

	public static ExampleRunner createSmallExampleModel() {
		ExampleRunner model = new ExampleRunner();
		model.setNetwork(new Network(2).setAllBPRParameters(60.0, 50, 4));
		model.createODPair("single od", 100.0, 0, 1);
		return model;
	}

	public static ExampleRunner createLargeRandomExample() {
		ExampleRunner exampleRunner = new ExampleRunner();
		RandomScenarioGenerator scenarioGenerator = new RandomScenarioGenerator(4711);
		// 1000 links with avg capacity 1000.0
		Network network = scenarioGenerator.createRandomNetwork(1000, 1000, 
				60.0, 900.0, 100.0, 1900.0, 1.0, 2.0);
		// 100 ods with avg demand 2000.0 und avg 100 route alternatives
		Set<ODPair> odPairs = scenarioGenerator.createRandomOdDemand(100, 100, 1000.0, 3000.0, 2, 198, network);
		exampleRunner.setNetwork(network);
		exampleRunner.setODPairs(odPairs);
		return exampleRunner;
	}

	public static Runner<DoubleArrayWrapper, ODPair, Paths> createBasicRunner(ExampleRunner model) {
		var runner = new Runner<DoubleArrayWrapper, ODPair, Paths>();
		runner.setAgents(model.getAgents()).setIterations(100).setNetworkLoading(model.createExactNetworkLoading())
				.setPlanInnovation(model.createBestResponsePlanInnovation())
				.setUtilityFunction(model.createUtilityFunction()).setLogger(new BasicLoggerImpl<>());
		return runner;
	}

	public static String runSmallExampleWithUniformMethod() {
		var model = createSmallExampleModel();
		var runner = createBasicRunner(model);
		runner.setPlanSelection(new UniformPlanSelection<>(-1.0));
		runner.run();
		return runner.getLogString();
	}

	public static String runSmallExampleWithSortingMethod() {
		var model = createSmallExampleModel();
		var runner = createBasicRunner(model);
		runner.setPlanSelection(new SortingPlanSelection<>(-1.0));
		runner.run();
		return runner.getLogString();
	}

	public static String runSmallExampleWithProposedMethod() {
		var model = createSmallExampleModel();
		var runner = createBasicRunner(model);
		runner.setPlanSelection(new LocalSearchPlanSelection<>(model.createApproximateNetworkLoading(),
				new DoubleArrayDistance(), -1.0));
		runner.run();
		return runner.getLogString();
	}

	public static String runLargeRandomExampleWithUniformMethod() {
		var model = createLargeRandomExample();
		var runner = createBasicRunner(model);
		runner.setPlanSelection(new UniformPlanSelection<>(-1.0));
		runner.run();
		return runner.getLogString();
	}

	public static String runLargeRandomExampleWithSortingMethod() {
		var model = createLargeRandomExample();
		var runner = createBasicRunner(model);
		runner.setPlanSelection(new SortingPlanSelection<>(-1.0));
		runner.run();
		return runner.getLogString();
	}

	public static String runLargeRandomExampleWithProposedMethod() {
		var model = createLargeRandomExample();
		var runner = createBasicRunner(model);
		runner.setPlanSelection(new LocalSearchPlanSelection<>(model.createApproximateNetworkLoading(),
				new DoubleArrayDistance(), -1.0));
		runner.run();
		return runner.getLogString();
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
//		runSmallExampleWithUniformMethod();
//		runSmallExampleWithSortingMethod();
//		runSmallExampleWithProposedMethod();

//		runLargeRandomExampleWithUniformMethod();
//		runLargeRandomExampleWithSortingMethod();
		runLargeRandomExampleWithProposedMethod();

		System.out.println("... DONE");
	}

}
