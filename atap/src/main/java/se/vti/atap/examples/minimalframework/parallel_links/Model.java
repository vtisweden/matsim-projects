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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import se.vti.atap.minimalframework.ApproximateNetworkLoading;
import se.vti.atap.minimalframework.ExactNetworkLoading;
import se.vti.atap.minimalframework.PlanInnovation;

/**
 * 
 * @author GunnarF
 *
 */
public class Model {

	private final double[] t0_s;
	private final double[] cap_veh;
	private final double[] exponent;

	private final Map<String, AgentImpl> id2agent;;

	public Model(int numberOfLinks) {
		this.t0_s = new double[numberOfLinks];
		this.cap_veh = new double[numberOfLinks];
		this.exponent = new double[numberOfLinks];
		this.id2agent = new LinkedHashMap<>();
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
		for (int i = 0; i < this.t0_s.length; i++) {
			this.setBPRParameters(i, t0_s, cap_veh, exponent);
		}
	}

	public void createAgent(String id, int... availableLinks) {
		this.id2agent.put(id, new AgentImpl(id, availableLinks));
	}

	public ApproximateNetworkLoading<DoubleArrayWrapper, DoubleArrayWrapper, AgentImpl, PlanImpl> createApproximateNetworkLoading() {
		return new ApproximateNetworkLoading<>() {
			@Override
			public DoubleArrayWrapper computeFlows(Set<AgentImpl> agentsUsingCurrentPlans,
					Set<AgentImpl> agentsUsingCandidatePlans, DoubleArrayWrapper networkConditions) {
				DoubleArrayWrapper result = new DoubleArrayWrapper(getNumberOfLinks());
				for (var agent : agentsUsingCurrentPlans) {
					result.data[agent.getCurrentPlan().getLinkIndex()]++;
				}
				for (var agent : agentsUsingCandidatePlans) {
					result.data[agent.getCandidatePlan().getLinkIndex()]++;
				}
				return result;
			}
		};
	}

	public ExactNetworkLoading<DoubleArrayWrapper, AgentImpl> createExactNetworkLoading() {
		return new ExactNetworkLoading<>() {
			@Override
			public DoubleArrayWrapper computeNetworkLoading(Set<AgentImpl> agents) {
				DoubleArrayWrapper flows = createApproximateNetworkLoading().computeFlows(agents,
						Collections.emptySet(), null);
				DoubleArrayWrapper travelTimes = new DoubleArrayWrapper(getNumberOfLinks());
				for (int i = 0; i < getNumberOfLinks(); i++) {
					travelTimes.data[i] = t0_s[i] * Math.pow(1.0 + flows.data[i] / cap_veh[i], exponent[i]);
				}
				return travelTimes;
			}
		};
	}

	public PlanInnovation<DoubleArrayWrapper, AgentImpl, PlanImpl> createBestResponsePlanInnovation() {
		return new PlanInnovation<>() {
			@Override
			public PlanImpl computeInitialPlan(AgentImpl agent) {
				return new PlanImpl(agent.getAvailableLinks()[0]);
			}

			@Override
			public PlanImpl computeCandidatePlan(AgentImpl agent, DoubleArrayWrapper networkConditions) {
				PlanImpl bestPlan = null;
				for (int i : agent.getAvailableLinks()) {
					if ((bestPlan == null)
							|| (networkConditions.data[i] < networkConditions.data[bestPlan.getLinkIndex()])) {
						bestPlan = new PlanImpl(i);
					}
				}
				return bestPlan;
			}
		};
	}
}
