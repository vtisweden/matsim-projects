/**
 * se.vti.atap.examples.minimalframework.parallel_links
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

import se.vti.atap.minimalframework.common.BasicAgentImpl;
import se.vti.atap.minimalframework.common.DoubleArrayWrapper;

/**
 * 
 * @author GunnarF
 *
 */
public class ODPair extends BasicAgentImpl<Paths> {

	public final double demand_veh;

	public final int[] availableLinks;
	
	public ODPair(String id, Double demand_veh, int... availableLinks) {
		super(id);
		this.demand_veh = demand_veh;
		this.availableLinks = availableLinks;
	}

	public int getNumberOfPaths() {
		return this.availableLinks.length;
	}
	
	private void addPathFlowsToLinkFlows(Paths pathFlows, DoubleArrayWrapper linkFlows_veh) {
		for (int pathIndex = 0; pathIndex < pathFlows.getNumberOfPaths(); pathIndex++) {
			linkFlows_veh.data[this.availableLinks[pathIndex]] += pathFlows.flows_veh[pathIndex];
		}
	}

	public void addCurrentPathFlowsToLinkFlows(DoubleArrayWrapper linkFlows_veh) {
		this.addPathFlowsToLinkFlows(this.getCurrentPlan(), linkFlows_veh);
	}

	public void addCandidatePathFlowsToLinkFlows(DoubleArrayWrapper linkFlows_veh) {
		this.addPathFlowsToLinkFlows(this.getCandidatePlan(), linkFlows_veh);
	}

	public double computeCurrentTotalTravelTime_s(Paths paths, DoubleArrayWrapper travelTimes_s) {
		double result_s = 0.0;
		for (int pathIndex = 0; pathIndex < paths.getNumberOfPaths(); pathIndex++) {
			result_s += paths.flows_veh[pathIndex] * travelTimes_s.data[this.availableLinks[pathIndex]];
		}
		return result_s;
	}
}
