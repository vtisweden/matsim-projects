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
public class LocalSearchPlanSelection<A extends Agent<?>, T extends NetworkConditions, Q extends ApproximateNetworkConditions<Q>>
		implements PlanSelection<A, T> {

	private final MSAStepSize stepSize;

	private final Random rnd;

	private final ApproximateNetworkLoading<A, T, Q> approximateNetworkLoading;

	public LocalSearchPlanSelection(double stepSizeIterationExponent, Random rnd,
			ApproximateNetworkLoading<A, T, Q> approximateNetworkLoading) {
		this.stepSize = new MSAStepSize(stepSizeIterationExponent);
		this.rnd = rnd;
		this.approximateNetworkLoading = approximateNetworkLoading;
	}

	private double computeObjectiveFunctionValue(Set<A> agentsUsingCandidatePlan, Q currentApproximateNetworkConditions,
			Q candidateApproximatNetworkConditions, double absoluteAmbitionLevel) {
		double expectedImprovement = agentsUsingCandidatePlan.stream().mapToDouble(a -> a.computeGap()).sum();
		double distance = currentApproximateNetworkConditions
				.computeLeaveOneOutDistance(candidateApproximatNetworkConditions);
		return (expectedImprovement - absoluteAmbitionLevel) / (distance + distance * distance + 1e-8);
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {

		double absoluteAmbitionLevel = this.stepSize.compute(iteration)
				* agents.stream().mapToDouble(a -> a.computeGap()).sum();

		Q currentApproximateNetworkConditions = this.approximateNetworkLoading.compute(agents, Collections.emptySet(),
				networkConditions);

		Set<A> agentsUsingCurrentPlan = new LinkedHashSet<>();
		Set<A> agentsUsingCandidatePlan = new LinkedHashSet<>(agents);
		Q candidateApproximateNetworkConditions = this.approximateNetworkLoading.compute(agentsUsingCurrentPlan,
				agentsUsingCandidatePlan, networkConditions);

		double objectiveFunctionValue = this.computeObjectiveFunctionValue(agentsUsingCandidatePlan,
				currentApproximateNetworkConditions, candidateApproximateNetworkConditions, absoluteAmbitionLevel);

		List<A> allAgents = new ArrayList<>(agents);
		boolean switched;
		do {
			switched = false;
			Collections.shuffle(allAgents, this.rnd);
			for (A agent : allAgents) {
				boolean agentWasUsingCandidatePlan = agentsUsingCandidatePlan.contains(agent);
				if (agentWasUsingCandidatePlan) {
					agentsUsingCandidatePlan.remove(agent);
					agentsUsingCurrentPlan.add(agent);
				} else {
					agentsUsingCurrentPlan.remove(agent);
					agentsUsingCandidatePlan.add(agent);
				}
				candidateApproximateNetworkConditions = this.approximateNetworkLoading.compute(agentsUsingCurrentPlan,
						agentsUsingCandidatePlan, networkConditions);
				double candidateObjectiveFunctionValue = this.computeObjectiveFunctionValue(agentsUsingCandidatePlan,
						currentApproximateNetworkConditions, candidateApproximateNetworkConditions,
						absoluteAmbitionLevel);
				if (candidateObjectiveFunctionValue > objectiveFunctionValue) {
					objectiveFunctionValue = candidateObjectiveFunctionValue;
					switched = true;
				} else {
					if (agentWasUsingCandidatePlan) {
						agentsUsingCurrentPlan.remove(agent);
						agentsUsingCandidatePlan.add(agent);
					} else {
						agentsUsingCandidatePlan.remove(agent);
						agentsUsingCurrentPlan.add(agent);
					}
				}
			}
		} while (switched);

		agentsUsingCandidatePlan.stream().forEach(a -> a.setCurrentPlanToCandidatePlan());
		agents.stream().forEach(a -> a.setCandidatePlan(null));
	}
}
