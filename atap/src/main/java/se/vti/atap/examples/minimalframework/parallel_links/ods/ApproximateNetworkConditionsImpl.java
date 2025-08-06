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

import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.minimalframework.planselection.proposed.AbstractApproximateNetworkConditions;

/**
 * 
 * @author GunnarF
 *
 */
public class ApproximateNetworkConditionsImpl
		extends AbstractApproximateNetworkConditions<Paths, ODPair, ApproximateNetworkConditionsImpl> {

	private double[] linkFlows_veh;

//	private double[] memorizedLinkFlows_veh;

//	private Function<double[], double[]> transformation = new Function<>() {
//		@Override
//		public double[] apply(double[] data) {
//			return data;
//		}
//	};

	public ApproximateNetworkConditionsImpl(Set<ODPair> agentsUsingCurrentPlan, Set<ODPair> agentsUsingCandidatePlan,
			Network network) {
		super(agentsUsingCurrentPlan, agentsUsingCandidatePlan, network);
	}

//	public ApproximateNetworkConditionsImpl(Set<ODPair> agentsUsingCurrentPlan, Set<ODPair> agentsUsingCandidatePlan,
//			Network network, ApproximateNetworkConditionsImpl parent) {
//		super(agentsUsingCurrentPlan, agentsUsingCandidatePlan, network, parent);
//	}
//
//	public void setFlowTransformation(Function<double[], double[]> transformation) {
//		this.transformation = transformation;
//	}

	private Paths lastSwitchedPaths = null;
	private ODPair lastSwitchedODPair = null;
	private double[] lastSwitchedLinkFlows_veh = null;

	@Override
	public void switchToPlan(Paths paths, ODPair odPair) {
		this.lastSwitchedPaths = this.agent2plan.get(odPair);
		this.lastSwitchedODPair = odPair;
		this.lastSwitchedLinkFlows_veh = new double[odPair.getNumberOfPaths()];
		for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
			this.lastSwitchedLinkFlows_veh[path] = this.linkFlows_veh[odPair.availableLinks[path]];
		}
		if (paths != null) {
			this.agent2plan.put(odPair, paths);
			for (int path = 0; path < paths.getNumberOfPaths(); path++) {
				this.linkFlows_veh[odPair.availableLinks[path]] += paths.pathFlows_veh[path];
			}
		} else {
			this.agent2plan.remove(odPair);			
		}
		if (this.lastSwitchedPaths != null) {
			for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
				this.linkFlows_veh[odPair.availableLinks[path]] -= this.lastSwitchedPaths.pathFlows_veh[path];
			}
		}
	}

	@Override
	public void undoLastSwitch() {
		for (int path = 0; path < this.lastSwitchedODPair.getNumberOfPaths(); path++) {
			this.linkFlows_veh[this.lastSwitchedODPair.availableLinks[path]] = this.lastSwitchedLinkFlows_veh[path];
		}
		this.agent2plan.put(this.lastSwitchedODPair, this.lastSwitchedPaths);
		this.lastSwitchedPaths = null;
		this.lastSwitchedODPair = null;
		this.lastSwitchedLinkFlows_veh = null;
	}

	@Override
	public double computeDistance(ApproximateNetworkConditionsImpl other) {
//		double[] thisTransformed = this.transformation.apply(this.linkFlows_veh);
//		double[] otherTransformed = other.transformation.apply(other.linkFlows_veh);
		double sumOfSquares = 0.0;
		for (int link = 0; link < this.linkFlows_veh.length; link++) {
			double diff = this.linkFlows_veh[link] - other.linkFlows_veh[link];
//			double diff = (this.linkFlows_veh[link] - other.linkFlows_veh[link]) / this.network.cap_veh[link];
			sumOfSquares += diff * diff;
		}
		return Math.sqrt(sumOfSquares);
	}

	@Override
	protected void initializeInternalState(Network network) {
		this.linkFlows_veh = new double[network.getNumberOfLinks()];
	}

//	@Override
//	protected void copyInternalState(ApproximateNetworkConditionsImpl other) {
//		this.linkFlows_veh = Arrays.copyOf(other.linkFlows_veh, other.linkFlows_veh.length);
//	}

//	@Override
//	protected void addToInternalState(Paths paths, ODPair odPair) {
//		for (int path = 0; path < paths.getNumberOfPaths(); path++) {
//			this.linkFlows_veh[odPair.availableLinks[path]] += paths.pathFlows_veh[path];
//		}
//	}

//	@Override
//	protected void removeFromInternalState(Paths paths, ODPair odPair) {
//		for (int path = 0; path < paths.getNumberOfPaths(); path++) {
//			this.linkFlows_veh[odPair.availableLinks[path]] -= paths.pathFlows_veh[path];
//		}
//	}

//	@Override
//	protected void memorizeInternalState() {
//		this.memorizedLinkFlows_veh = Arrays.copyOf(this.linkFlows_veh, this.linkFlows_veh.length);
//	}

//	@Override
//	protected void restoreInternalState() {
//		this.linkFlows_veh = Arrays.copyOf(this.memorizedLinkFlows_veh, this.memorizedLinkFlows_veh.length);
//	}
}
