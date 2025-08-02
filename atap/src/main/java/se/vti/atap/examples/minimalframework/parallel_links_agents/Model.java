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
package se.vti.atap.examples.minimalframework.parallel_links_agents;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import se.vti.atap.minimalframework.ApproximateNetworkLoading;
import se.vti.atap.minimalframework.ExactNetworkLoading;
import se.vti.atap.minimalframework.PlanInnovation;
import se.vti.atap.minimalframework.UtilityFunction;
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
public class Model {

	private final double[] t0_s;
	private final double[] cap_veh;
	private final double[] exponent;

	private final Set<AgentImpl> agents = new LinkedHashSet<>();

	public Model(int numberOfLinks) {
		this.t0_s = new double[numberOfLinks];
		this.cap_veh = new double[numberOfLinks];
		this.exponent = new double[numberOfLinks];
	}

	public int getNumberOfLinks() {
		return this.t0_s.length;
	}

	public void setBPRParameters(int linkIndex, double t0_s, double cap_veh, double exponent) {
		this.t0_s[linkIndex] = t0_s;
		this.cap_veh[linkIndex] = cap_veh;
		this.exponent[linkIndex] = exponent;
	}

	public void setAllBPRParameters(double t0_s, double cap_veh, double exponent) {
		for (int i = 0; i < this.getNumberOfLinks(); i++) {
			this.setBPRParameters(i, t0_s, cap_veh, exponent);
		}
	}

	public void createAgent(String id, int... availableLinks) {
		this.agents.add(new AgentImpl(id, availableLinks));
	}

	public Set<AgentImpl> getAgents() {
		return this.agents;
	}

	public ApproximateNetworkLoading<DoubleArrayWrapper, DoubleArrayWrapper, AgentImpl> createApproximateNetworkLoading() {
		return new ApproximateNetworkLoading<>() {
			@Override
			public DoubleArrayWrapper computeFlows(Set<AgentImpl> agentsUsingCurrentPlans,
					Set<AgentImpl> agentsUsingCandidatePlans, DoubleArrayWrapper networkConditions) {
				DoubleArrayWrapper result = new DoubleArrayWrapper(getNumberOfLinks());
				for (var agent : agentsUsingCurrentPlans) {
					result.data[agent.getCurrentPlan().linkIndex]++;
				}
				for (var agent : agentsUsingCandidatePlans) {
					result.data[agent.getCandidatePlan().linkIndex]++;
				}
				return result;
			}
		};
	}

	public UtilityFunction<DoubleArrayWrapper, AgentImpl, PlanImpl> createUtilityFunction() {
		return new UtilityFunction<>() {
			@Override
			public double computeUtility(AgentImpl agent, PlanImpl plan, DoubleArrayWrapper travelTimes) {
				return (-1.0) * travelTimes.data[plan.linkIndex];
			}

		};
	}

	public ExactNetworkLoading<DoubleArrayWrapper, AgentImpl> createExactNetworkLoading() {
		return new ExactNetworkLoading<>() {
			private double computeTravelTime_s(int linkIndex, double flow_veh) {
				return t0_s[linkIndex] * Math.pow(1.0 + flow_veh / cap_veh[linkIndex], exponent[linkIndex]);
			}

			@Override
			public DoubleArrayWrapper computeNetworkLoading(Set<AgentImpl> agents) {
				double[] flows = createApproximateNetworkLoading().computeFlows(agents, Collections.emptySet(),
						null).data;
				DoubleArrayWrapper travelTimes = new DoubleArrayWrapper(getNumberOfLinks());
				for (int i = 0; i < getNumberOfLinks(); i++) {
					travelTimes.data[i] = computeTravelTime_s(i, flows[i]);
				}
				return travelTimes;
			}
		};
	}

	public PlanInnovation<DoubleArrayWrapper, AgentImpl> createBestResponsePlanInnovation() {
		return new PlanInnovation<>() {
			@Override
			public void assignInitialPlan(AgentImpl agent) {
				agent.setCurrentPlan(new PlanImpl(agent.availableLinks[0]));
			}

			@Override
			public void assignCandidatePlan(AgentImpl agent, DoubleArrayWrapper travelTimes_s) {
				PlanImpl bestPlan = null;
				for (int i : agent.availableLinks) {
					if ((bestPlan == null) || (travelTimes_s.data[i] < travelTimes_s.data[bestPlan.linkIndex])) {
						bestPlan = new PlanImpl(i);
					}
				}
				agent.setCandidatePlan(bestPlan);
			}
		};
	}

	public static Model createSmallExampleModel() {
		Model model = new Model(2);
		model.setAllBPRParameters(60.0, 50, 4);
		for (int n = 0; n < 100; n++) {
			model.createAgent(Integer.toString(n), 0, 1);
		}
		return model;
	}

	public static Runner<DoubleArrayWrapper, AgentImpl, PlanImpl> createBasicRunner(Model model) {
		var runner = new Runner<DoubleArrayWrapper, AgentImpl, PlanImpl>();
		runner.setAgents(model.getAgents()).setIterations(100).setNetworkLoading(model.createExactNetworkLoading())
				.setPlanInnovation(model.createBestResponsePlanInnovation())
				.setUtilityFunction(model.createUtilityFunction()).setLogger(new LoggerImpl());
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

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		runSmallExampleWithUniformMethod();
		runSmallExampleWithSortingMethod();
		runSmallExampleWithProposedMethod();
		System.out.println("... DONE");
	}

}
