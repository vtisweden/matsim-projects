/**
 * se.vti.atap.framework
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

import java.util.Set;

/**
 * 
 * @author GunnarF
 *
 */
public class Runner<T extends NetworkConditions, A extends Agent<P>, P extends Plan> {

	private Set<A> agents = null;

	private NetworkLoading<T, A> networkLoading = null;

	private UtilityFunction<T, A, P> utilityFunction = null;

	private PlanInnovation<T, A> planInnovation = null;

	private PlanSelection<T, A> planSelection = null;

	private Integer maxIterations = null;

	private Logger<T, A> logger = null;

	public Runner() {
	}

	public Runner<T, A, P> setAgents(Set<A> agents) {
		this.agents = agents;
		return this;
	}

	public Runner<T, A, P> setNetworkLoading(NetworkLoading<T, A> networkLoading) {
		this.networkLoading = networkLoading;
		return this;
	}

	public Runner<T, A, P> setUtilityFunction(UtilityFunction<T, A, P> utilityFunction) {
		this.utilityFunction = utilityFunction;
		return this;
	}

	public Runner<T, A, P> setPlanInnovation(PlanInnovation<T, A> planInnovation) {
		this.planInnovation = planInnovation;
		return this;
	}

	public Runner<T, A, P> setPlanSelection(PlanSelection<T, A> planSelection) {
		this.planSelection = planSelection;
		return this;
	}

	public Runner<T, A, P> setIterations(int maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	public Runner<T, A, P> setLogger(Logger<T, A> logger) {
		this.logger = logger;
		return this;
	}

	public void run() {

		this.agents.stream().forEach(a -> this.planInnovation.assignInitialPlan(a));

		for (int iteration = 0; iteration < this.maxIterations; iteration++) {

			System.out.println(iteration);

			T networkConditions = this.networkLoading.compute(this.agents);

			for (A agent : this.agents) {

				P currentPlan = agent.getCurrentPlan();
				currentPlan.setUtility(this.utilityFunction.computeUtility(agent, currentPlan, networkConditions));

				this.planInnovation.assignCandidatePlan(agent, networkConditions);
				P candidatePlan = agent.getCandidatePlan();
				if (candidatePlan.getUtility() == null) {
					candidatePlan
							.setUtility(this.utilityFunction.computeUtility(agent, candidatePlan, networkConditions));
				}

				if (candidatePlan.getUtility() < currentPlan.getUtility()) {
					agent.setCandidatePlan(currentPlan);
				}
			}

			this.logger.log(networkConditions, this.agents);

			if (iteration < this.maxIterations - 1) {
				this.planSelection.assignSelectedPlans(this.agents, networkConditions, iteration);
			}
		}
	}

	public Logger<T, A> getLogger() {
		return this.logger;
	}
}
