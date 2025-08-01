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
public class Runner<T extends NetworkConditions, P extends Plan> {

	private Set<Agent<P>> agents = null;

	private ExactNetworkLoading<T, P> networkLoading = null;

	private UtilityFunction<T, P> utilityFunction = null;

	private PlanInnovation<T, P> planInnovation = null;

	private PlanSelection<T, P> planSelection = null;

	private Logger<T, P> logger = null;

	private Integer maxIterations = null;

	public Runner() {
	}

	public Runner<T, P> setAgents(Set<Agent<P>> agents) {
		this.agents = agents;
		return this;
	}

	public Runner<T, P> setNetworkLoading(ExactNetworkLoading<T, P> networkLoading) {
		this.networkLoading = networkLoading;
		return this;
	}

	public Runner<T, P> setUtilityFunction(UtilityFunction<T, P> utilityFunction) {
		this.utilityFunction = utilityFunction;
		return this;
	}

	public Runner<T, P> setPlanInnovation(PlanInnovation<T, P> planInnovation) {
		this.planInnovation = planInnovation;
		return this;
	}

	public Runner<T, P> setPlanSelection(PlanSelection<T, P> planSelection) {
		this.planSelection = planSelection;
		return this;
	}

	public Runner<T, P> setLogger(Logger<T, P> logger) {
		this.logger = logger;
		return this;
	}

	public Runner<T, P> setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	public void run() {

		this.agents.stream().forEach(a -> a.setCurrentPlan(this.planInnovation.computeInitialPlan(a)));

		for (int iteration = 0; iteration < this.maxIterations; iteration++) {

			T networkConditions = this.networkLoading.computeNetworkLoading(this.agents);

			for (Agent<P> agent : this.agents) {

				P currentPlan = agent.getCurrentPlan();
				currentPlan.setUtility(this.utilityFunction.computeUtility(agent, currentPlan, networkConditions));

				P candidatePlan = this.planInnovation.computeCandidatePlan(agent, networkConditions);
				if (candidatePlan.getUtility() == null) {
					candidatePlan
							.setUtility(this.utilityFunction.computeUtility(agent, candidatePlan, networkConditions));
				}
			}

			this.logger.log(this.agents, networkConditions);

			if (iteration < this.maxIterations - 1) {
				this.planSelection.selectPlans(this.agents, networkConditions, iteration);
			}
		}
	}
}
