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
package se.vti.atap.minimalframework.planselection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.NetworkConditions;
import se.vti.atap.minimalframework.planselection.proposed.ApproximateNetworkConditions;

/**
 * 
 * @author GunnarF
 *
 */
public class UniformPlanSelection<T extends NetworkConditions, Q extends ApproximateNetworkConditions, A extends Agent<?>>
		extends AbstractPlanSelection<T, A> {

	private final Random rnd = new Random(4711);

	public UniformPlanSelection(double stepSizeIterationExponent) {
		super(stepSizeIterationExponent);
	}

	@Override
	public void assignSelectedPlans(Set<A> agents, T networkConditions, int iteration) {
		List<A> allAgents = new ArrayList<>(agents);
		Collections.shuffle(allAgents, this.rnd);
		double numberOfReplanners = this.computeStepSize(iteration) * allAgents.size();
		for (int n = 0; n < numberOfReplanners; n++) {
			allAgents.get(n).setCurrentPlanToCandidatePlan();
		}
	}
}
