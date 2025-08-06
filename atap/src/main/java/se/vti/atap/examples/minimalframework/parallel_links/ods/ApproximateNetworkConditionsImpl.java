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

	private Paths lastSwitchedPaths = null;
	private ODPair lastSwitchedODPair = null;
	private double[] lastLinkFlowsBeforeSwitch_veh = null;

	public ApproximateNetworkConditionsImpl(Set<ODPair> agentsUsingCurrentPlan, Set<ODPair> agentsUsingCandidatePlan,
			Network network) {
		super(agentsUsingCurrentPlan, agentsUsingCandidatePlan, network);
	}

	@Override
	protected void initializeInternalState(Network network) {
		this.linkFlows_veh = new double[network.getNumberOfLinks()];
	}
	
	@Override
	public double computeDistance(ApproximateNetworkConditionsImpl other) {
		double sumOfSquares = 0.0;
		for (int link = 0; link < this.linkFlows_veh.length; link++) {
			double diff = this.linkFlows_veh[link] - other.linkFlows_veh[link];
			sumOfSquares += diff * diff;
		}
		return Math.sqrt(sumOfSquares);
	}

	@Override
	public void switchToPlan(Paths paths, ODPair odPair) {
		
		this.lastSwitchedPaths = this.agent2plan.get(odPair);
		this.lastSwitchedODPair = odPair;
		this.lastLinkFlowsBeforeSwitch_veh = new double[odPair.getNumberOfPaths()];
		for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
			this.lastLinkFlowsBeforeSwitch_veh[path] = this.linkFlows_veh[odPair.availableLinks[path]];
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
			this.linkFlows_veh[this.lastSwitchedODPair.availableLinks[path]] = this.lastLinkFlowsBeforeSwitch_veh[path];
		}
		this.agent2plan.put(this.lastSwitchedODPair, this.lastSwitchedPaths);
		
		this.lastSwitchedPaths = null;
		this.lastSwitchedODPair = null;
		this.lastLinkFlowsBeforeSwitch_veh = null;
	}
}
