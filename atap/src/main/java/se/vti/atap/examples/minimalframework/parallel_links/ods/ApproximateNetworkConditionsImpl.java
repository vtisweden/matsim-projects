/**
 * se.vti.atap.examples.minimalframework.parallel_links.ods
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
package se.vti.atap.examples.minimalframework.parallel_links.ods;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.minimalframework.planselection.proposed.AbstractApproximateNetworkConditions;

/**
 * 
 * @author GunnarF
 *
 */
public class ApproximateNetworkConditionsImpl extends AbstractApproximateNetworkConditions<ApproximateNetworkConditionsImpl, ODPair, Paths> {

	private double[] linkFlows_veh;

	private double[] memorizedLinkFlows_veh;

	private Function<double[], double[]> transformation = new Function<>() {
		@Override
		public double[] apply(double[] data) {
			return data;
		}

	};

	public ApproximateNetworkConditionsImpl(Set<ODPair> agentsUsingCurrentPlan, Set<ODPair> agentsUsingCandidatePlan, Network network) {
		super(agentsUsingCurrentPlan, agentsUsingCandidatePlan);
		this.linkFlows_veh = new double[network.getNumberOfLinks()];
		for (Map.Entry<ODPair, Paths> entry : super.agent2plan.entrySet()) {
			this.addToInternalFlows(entry.getKey(), entry.getValue(), 1.0);
		}
	}

	public ApproximateNetworkConditionsImpl setFlowTransformation(Function<double[], double[]> transformation) {
		this.transformation = transformation;
		return this;
	}

	@Override
	protected void addToInternalFlows(ODPair odPair, Paths paths, double weight) {
		for (int pathIndex = 0; pathIndex < paths.getNumberOfPaths(); pathIndex++) {
			this.linkFlows_veh[odPair.availableLinks[pathIndex]] += weight * paths.flows_veh[pathIndex];
		}
	}

	@Override
	public double computeDistance(ApproximateNetworkConditionsImpl other) {
		double[] thisTransformed = this.transformation.apply(this.linkFlows_veh);
		double[] otherTransformed = other.transformation.apply(other.linkFlows_veh);
		double sumOfSquares = 0.0;
		for (int linkIndex = 0; linkIndex < thisTransformed.length; linkIndex++) {
			double diff = thisTransformed[linkIndex] - otherTransformed[linkIndex];
			sumOfSquares += diff * diff;
		}
		return Math.sqrt(sumOfSquares);
	}

	@Override
	protected void memorizeInternalFlows() {
		this.memorizedLinkFlows_veh = Arrays.copyOf(this.linkFlows_veh, this.linkFlows_veh.length);
	}

	@Override
	protected void restoreInternalFlows() {
		this.linkFlows_veh = Arrays.copyOf(this.memorizedLinkFlows_veh, this.memorizedLinkFlows_veh.length);
	}

}
