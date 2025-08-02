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
package se.vti.atap.minimalframework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author GunnarF
 *
 */
public class LocalSearchPlanSelection<T extends NetworkConditions, Q extends NetworkFlows, A extends Agent<P>, P extends Plan>
		implements PlanSelection<T, A> {

	private final ApproximateNetworkLoading<T, Q, A, P> approximateNetworkLoading;

	private final NetworkFlowDistance<Q> networkFlowDistance;

	public LocalSearchPlanSelection(ApproximateNetworkLoading<T, Q, A, P> approximateNetworkLoading,
			NetworkFlowDistance<Q> networkConditionDistance) {
		this.approximateNetworkLoading = approximateNetworkLoading;
		this.networkFlowDistance = networkConditionDistance;
	}

	private double computeObjectiveFunction(Set<A> agentsUsingCandidatePlan, Q candidateFlows, Q originalFlows,
			double absoluteAmbitionLevel) {
		double expectedImprovement = agentsUsingCandidatePlan.stream()
				.mapToDouble(a -> a.getCandidatePlan().getUtility() - a.getCurrentPlan().getUtility()).sum();
		double distance = this.networkFlowDistance.computeDistance(candidateFlows, originalFlows);
		return (expectedImprovement - absoluteAmbitionLevel) / (distance + distance * distance + 1e-8);
	}

	@Override
	public void selectPlans(Set<A> agents, T networkConditions, double absoluteAmbitionLevel) {

		List<A> allAgents = new ArrayList<>(agents);

		Set<A> agentsUsingCurrentPlans = new LinkedHashSet<>();
		Set<A> agentsUsingCandidatePlans = new LinkedHashSet<>(agents);

		Q originalFlows = this.approximateNetworkLoading.computeFlows(agents, Collections.emptySet(),
				networkConditions);
		Q candidateFlows = this.approximateNetworkLoading.computeFlows(agentsUsingCurrentPlans, agentsUsingCandidatePlans, networkConditions);
		double oldObjectiveFunction = this.computeObjectiveFunction(agentsUsingCandidatePlans, candidateFlows, originalFlows, absoluteAmbitionLevel);
		
		boolean switched;
		do {
			switched = false;
			Collections.shuffle(allAgents);
			for (A agent : allAgents) {
				boolean agentWasUsingCandidatePlan = agentsUsingCandidatePlans.contains(agent);
				if (agentWasUsingCandidatePlan) {
					agentsUsingCandidatePlans.remove(agent);
					agentsUsingCurrentPlans.add(agent);
				} else {
					agentsUsingCandidatePlans.add(agent);
					agentsUsingCurrentPlans.remove(agent);
				}
				candidateFlows = this.approximateNetworkLoading.computeFlows(agentsUsingCurrentPlans,
						agentsUsingCandidatePlans, networkConditions);
				double newObjectiveFunction = this.computeObjectiveFunction(agentsUsingCandidatePlans, candidateFlows,
						originalFlows, absoluteAmbitionLevel);
				if (newObjectiveFunction > oldObjectiveFunction) {
					oldObjectiveFunction = newObjectiveFunction;
					switched = true;
				} else {
					if (agentWasUsingCandidatePlan) {
						agentsUsingCandidatePlans.add(agent);
						agentsUsingCurrentPlans.remove(agent);
					} else {
						agentsUsingCandidatePlans.remove(agent);
						agentsUsingCurrentPlans.add(agent);
					}
				}
			}
		} while (switched);
		
		for (A a : agents) {
			if (agentsUsingCandidatePlans.contains(a)) {
				a.setCurrentPlan(a.getCandidatePlan());				
			}
			a.setCandidatePlan(null);
		}
	}
}
