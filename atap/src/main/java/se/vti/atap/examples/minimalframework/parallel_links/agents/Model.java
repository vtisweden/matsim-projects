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
package se.vti.atap.examples.minimalframework.parallel_links.agents;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.examples.minimalframework.parallel_links.RandomScenarioGenerator;

/**
 * 
 * @author GunnarF
 *
 */
public class Model {

	public static Set<AgentImpl> createRandomAgentDemand(int numberOfAgents, int numberOfRoutes, Network network,
			Random rnd) {
		List<int[]> choiceSets = RandomScenarioGenerator.createRandomPathSets(numberOfAgents, numberOfRoutes, network, rnd);
		Set<AgentImpl> result = new LinkedHashSet<>(choiceSets.size());
		int n = 0;
		for (int[] choiceSet : choiceSets) {
			result.add(new AgentImpl("agent " + (n++), choiceSet));
		}
		return result;
	}

//	private final Set<AgentImpl> agents = new LinkedHashSet<>();
//
//	private Network network = null;
//
//	public Model() {
//	}
//
//	public void createAgent(String id, int... availableLinks) {
//		this.agents.add(new AgentImpl(id, availableLinks));
//	}
//
//	public Set<AgentImpl> getAgents() {
//		return this.agents;
//	}
//
//	public void setNetwork(Network network) {
//		this.network = network;
//	}
//
//	public ApproximateNetworkLoading<DoubleArrayWrapper, DoubleArrayWrapper, AgentImpl> createApproximateNetworkLoading() {
//		return new ApproximateNetworkLoading<>() {
//			@Override
//			public DoubleArrayWrapper computeFlows(Set<AgentImpl> agentsUsingCurrentPlans,
//					Set<AgentImpl> agentsUsingCandidatePlans, DoubleArrayWrapper networkConditions) {
//				DoubleArrayWrapper result = new DoubleArrayWrapper(network.getNumberOfLinks());
//				for (var agent : agentsUsingCurrentPlans) {
//					result.data[agent.getCurrentPlan().linkIndex]++;
//				}
//				for (var agent : agentsUsingCandidatePlans) {
//					result.data[agent.getCandidatePlan().linkIndex]++;
//				}
//				return result;
//			}
//		};
//	}
//
//	public UtilityFunction<DoubleArrayWrapper, AgentImpl, PlanImpl> createUtilityFunction() {
//		return new UtilityFunction<>() {
//			@Override
//			public double computeUtility(AgentImpl agent, PlanImpl plan, DoubleArrayWrapper travelTimes) {
//				return (-1.0) * travelTimes.data[plan.linkIndex];
//			}
//
//		};
//	}
//
//	public ExactNetworkLoading<DoubleArrayWrapper, AgentImpl> createExactNetworkLoading() {
//		return new ExactNetworkLoading<>() {
//			@Override
//			public DoubleArrayWrapper computeNetworkLoading(Set<AgentImpl> agents) {
//				double[] flows = createApproximateNetworkLoading().computeFlows(agents, Collections.emptySet(),
//						null).data;
//				DoubleArrayWrapper travelTimes = new DoubleArrayWrapper(network.getNumberOfLinks());
//				for (int i = 0; i < network.getNumberOfLinks(); i++) {
//					travelTimes.data[i] = network.computeTravelTime_s(i, flows[i]);
//				}
//				return travelTimes;
//			}
//		};
//	}
//
//	public PlanInnovation<DoubleArrayWrapper, AgentImpl> createBestResponsePlanInnovation() {
//		return new PlanInnovation<>() {
//			@Override
//			public void assignInitialPlan(AgentImpl agent) {
//				agent.setCurrentPlan(new PlanImpl(agent.availableLinks[0]));
//			}
//
//			@Override
//			public void assignCandidatePlan(AgentImpl agent, DoubleArrayWrapper travelTimes_s) {
//				PlanImpl bestPlan = null;
//				for (int i : agent.availableLinks) {
//					if ((bestPlan == null) || (travelTimes_s.data[i] < travelTimes_s.data[bestPlan.linkIndex])) {
//						bestPlan = new PlanImpl(i);
//					}
//				}
//				agent.setCandidatePlan(bestPlan);
//			}
//		};
//	}
//
//	public static Model createSmallExampleModel() {
//		Model model = new Model();
//		model.setNetwork(new Network(2).setAllBPRParameters(60.0, 50));
//		for (int n = 0; n < 100; n++) {
//			model.createAgent(Integer.toString(n), 0, 1);
//		}
//		return model;
//	}
//
//	public static Runner<DoubleArrayWrapper, AgentImpl, PlanImpl> createBasicRunner(Model model) {
//		var runner = new Runner<DoubleArrayWrapper, AgentImpl, PlanImpl>();
//		runner.setAgents(model.getAgents()).setIterations(100).setNetworkLoading(model.createExactNetworkLoading())
//				.setPlanInnovation(model.createBestResponsePlanInnovation())
//				.setUtilityFunction(model.createUtilityFunction()).setLogger(new LoggerImpl());
//		return runner;
//	}
//
//	public static String runSmallExampleWithUniformMethod() {
//		var model = createSmallExampleModel();
//		var runner = createBasicRunner(model);
//		runner.setPlanSelection(new UniformPlanSelection<>(-1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runSmallExampleWithSortingMethod() {
//		var model = createSmallExampleModel();
//		var runner = createBasicRunner(model);
//		runner.setPlanSelection(new SortingPlanSelection<>(-1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static String runSmallExampleWithProposedMethod() {
//		var model = createSmallExampleModel();
//		var runner = createBasicRunner(model);
//		runner.setPlanSelection(new LocalSearchPlanSelection<>(model.createApproximateNetworkLoading(),
//				new DoubleArrayDistance(), -1.0));
//		runner.run();
//		return runner.getLogger().toString();
//	}
//
//	public static void main(String[] args) {
//		System.out.println("STARTED ...");
//		runSmallExampleWithUniformMethod();
//		runSmallExampleWithSortingMethod();
//		runSmallExampleWithProposedMethod();
//		System.out.println("... DONE");
//	}

}
