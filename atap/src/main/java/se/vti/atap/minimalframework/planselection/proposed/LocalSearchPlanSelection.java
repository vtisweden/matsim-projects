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
package se.vti.atap.minimalframework.planselection.proposed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.NetworkConditions;
import se.vti.atap.minimalframework.PlanSelection;
import se.vti.atap.minimalframework.planselection.MSAStepSize;

/**
 * 
 * @author GunnarF
 *
 */
public class LocalSearchPlanSelection<T extends NetworkConditions, Q extends ApproximateNetworkConditions<Q>, A extends Agent<?>>
		implements PlanSelection<A, T> {

	private final ApproximateNetworkLoading<T, Q, A> approximateNetworkLoading;

	private final MSAStepSize stepSize;

	private final Random rnd;

	public LocalSearchPlanSelection(ApproximateNetworkLoading<T, Q, A> approximateNetworkLoading,
			double stepSizeIterationExponent, Random rnd) {
		this.stepSize = new MSAStepSize(stepSizeIterationExponent);
		this.rnd = rnd;
		this.approximateNetworkLoading = approximateNetworkLoading;
	}

	private double computeObjectiveFunction(Set<A> agentsUsingCandidatePlan, Q candidateFlows, Q originalFlows,
			double absoluteAmbitionLevel) {
		double expectedImprovement = agentsUsingCandidatePlan.stream()
				.mapToDouble(a -> a.getCandidatePlan().getUtility() - a.getCurrentPlan().getUtility()).sum();
//		double distance = this.networkFlowDistance.computeDistance(candidateFlows, originalFlows);
		double distance = candidateFlows.computeLeaveOneOutDistance(originalFlows);
		return (expectedImprovement - absoluteAmbitionLevel) / (distance + distance * distance + 1e-8);
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {

		double relativeAmbitionLevel = this.stepSize.compute(iteration);
		double absoluteAmbitionLevel = relativeAmbitionLevel * agents.stream()
				.mapToDouble(a -> a.getCandidatePlan().getUtility() - a.getCurrentPlan().getUtility()).sum();

		Q originalFlows = this.approximateNetworkLoading.computeApproximateNetworkConditions(agents,
				Collections.emptySet(), networkConditions);

		Set<A> agentsUsingCurrentPlans = new LinkedHashSet<>();
		Set<A> agentsUsingCandidatePlans = new LinkedHashSet<>(agents);
		Q candidateFlows = this.approximateNetworkLoading.computeApproximateNetworkConditions(agentsUsingCurrentPlans,
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
				candidateFlows = this.approximateNetworkLoading.computeApproximateNetworkConditions(
						agentsUsingCurrentPlans, agentsUsingCandidatePlans, networkConditions);
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
