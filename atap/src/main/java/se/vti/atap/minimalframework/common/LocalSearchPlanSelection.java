/**
 * se.vti.atap.minimalframework
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
package se.vti.atap.minimalframework.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.ApproximateNetworkLoading;
import se.vti.atap.minimalframework.NetworkConditions;
import se.vti.atap.minimalframework.NetworkFlowDistance;
import se.vti.atap.minimalframework.NetworkFlows;
import se.vti.atap.minimalframework.PlanSelection;

/**
 * 
 * @author GunnarF
 *
 */
public class LocalSearchPlanSelection<T extends NetworkConditions, Q extends NetworkFlows, A extends Agent<?>>
		implements PlanSelection<T, A> {

	private final Random rnd = new Random(4711);
	
	private final ApproximateNetworkLoading<T, Q, A> approximateNetworkLoading;

	private final NetworkFlowDistance<Q> networkFlowDistance;

	private final double stepSizeIterationExponent;
	
	public LocalSearchPlanSelection(ApproximateNetworkLoading<T, Q, A> approximateNetworkLoading,
			NetworkFlowDistance<Q> networkConditionDistance, double stepSizeIterationExponent) {
		this.approximateNetworkLoading = approximateNetworkLoading;
		this.networkFlowDistance = networkConditionDistance;
		this.stepSizeIterationExponent = stepSizeIterationExponent;
	}

	private double computeObjectiveFunction(Set<A> agentsUsingCandidatePlan, Q candidateFlows, Q originalFlows,
			double absoluteAmbitionLevel) {
		double expectedImprovement = agentsUsingCandidatePlan.stream()
				.mapToDouble(a -> a.getCandidatePlan().getUtility() - a.getCurrentPlan().getUtility()).sum();
		double distance = this.networkFlowDistance.computeDistance(candidateFlows, originalFlows);
		return (expectedImprovement - absoluteAmbitionLevel) / (distance + distance * distance + 1e-8);
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {

		double relativeAmbitionLevel = Math.pow(iteration + 1, this.stepSizeIterationExponent);
		double absoluteAmbitionLevel = relativeAmbitionLevel *
				agents.stream().mapToDouble(a -> a.getCandidatePlan().getUtility() - a.getCurrentPlan().getUtility()).sum();
		
		Q originalFlows = this.approximateNetworkLoading.computeFlows(agents, Collections.emptySet(),
				networkConditions);

		Set<A> agentsUsingCurrentPlans = new LinkedHashSet<>();
		Set<A> agentsUsingCandidatePlans = new LinkedHashSet<>(agents);
		Q candidateFlows = this.approximateNetworkLoading.computeFlows(agentsUsingCurrentPlans,
				agentsUsingCandidatePlans, networkConditions);

		double objectiveFunctionValue = this.computeObjectiveFunction(agentsUsingCandidatePlans, candidateFlows,
				originalFlows, absoluteAmbitionLevel);

		List<A> allAgents = new ArrayList<>(agents);
		boolean switched;
		do {
//			System.out.println("\t\tQ = " + objectiveFunctionValue);
			
			switched = false;
			Collections.shuffle(allAgents, this.rnd);
			for (A agent : allAgents) {
				boolean agentWasUsingCandidatePlan = agentsUsingCandidatePlans.contains(agent);
				if (agentWasUsingCandidatePlan) {
					agentsUsingCandidatePlans.remove(agent);
					agentsUsingCurrentPlans.add(agent);
				} else {
					agentsUsingCurrentPlans.remove(agent);
					agentsUsingCandidatePlans.add(agent);
				}
				candidateFlows = this.approximateNetworkLoading.computeFlows(agentsUsingCurrentPlans,
						agentsUsingCandidatePlans, networkConditions);
				double candidateObjectiveFunctionValue = this.computeObjectiveFunction(agentsUsingCandidatePlans,
						candidateFlows, originalFlows, absoluteAmbitionLevel);
				if (candidateObjectiveFunctionValue > objectiveFunctionValue) {
					objectiveFunctionValue = candidateObjectiveFunctionValue;
					switched = true;
				} else {
					if (agentWasUsingCandidatePlan) {
						agentsUsingCurrentPlans.remove(agent);
						agentsUsingCandidatePlans.add(agent);
					} else {
						agentsUsingCandidatePlans.remove(agent);
						agentsUsingCurrentPlans.add(agent);
					}
				}
			}
		} while (switched);

		for (A a : agents) {
			if (agentsUsingCandidatePlans.contains(a)) {
				a.setCurrentPlanToCandidatePlan();
			}
			a.setCandidatePlan(null);
		}
	}
}
