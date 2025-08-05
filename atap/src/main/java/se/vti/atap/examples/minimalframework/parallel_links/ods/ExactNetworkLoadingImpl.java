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

import java.util.Collections;
import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.DoubleArrayNetworkConditions;
import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.minimalframework.NetworkLoading;

/**
 * 
 * @author GunnarF
 *
 */
public class ExactNetworkLoadingImpl implements NetworkLoading<ODPair, DoubleArrayNetworkConditions> {

	private final Network network;

	public ExactNetworkLoadingImpl(Network network) {
		this.network = network;
	}

	public double[] computeLinkFlows_veh(Set<ODPair> agentsUsingCurrentPlan, Set<ODPair> agentsUsingCandidatePlan) {
		double[] linkFlows_veh = new double[this.network.getNumberOfLinks()];
		for (ODPair odPair : agentsUsingCurrentPlan) {
			for (int pathIndex = 0; pathIndex < odPair.getNumberOfPaths(); pathIndex++) {
				int linkIndex = odPair.availableLinks[pathIndex];
				linkFlows_veh[linkIndex] += odPair.getCurrentPlan().flows_veh[pathIndex];
			}
		}
		for (ODPair odPair : agentsUsingCandidatePlan) {
			for (int pathIndex = 0; pathIndex < odPair.getNumberOfPaths(); pathIndex++) {
				int linkIndex = odPair.availableLinks[pathIndex];
				linkFlows_veh[linkIndex] += odPair.getCandidatePlan().flows_veh[pathIndex];
			}
		}
		return linkFlows_veh;
	}


	public DoubleArrayNetworkConditions computeNetworkLoading(double[] linkFlows_veh) {
		DoubleArrayNetworkConditions travelTimes = new DoubleArrayNetworkConditions(this.network.getNumberOfLinks());
		for (int i = 0; i < this.network.getNumberOfLinks(); i++) {
			travelTimes.data[i] = this.network.computeTravelTime_s(i, linkFlows_veh[i]);
		}
		return travelTimes;
	}

	
//	public DoubleArrayWrapper computeNetworkLoading(Set<ODPair> agentsUsingCurrentPlan,
//			Set<ODPair> agentsUsingCandidatePlan) {
//		double[] linkFlows_veh = this.computeLinkFlows_veh(agentsUsingCurrentPlan, agentsUsingCandidatePlan);
//		DoubleArrayWrapper travelTimes = new DoubleArrayWrapper(this.network.getNumberOfLinks());
//		for (int i = 0; i < this.network.getNumberOfLinks(); i++) {
//			travelTimes.data[i] = this.network.computeTravelTime_s(i, linkFlows_veh[i]);
//		}
//		return travelTimes;
//	}

	@Override
	public DoubleArrayNetworkConditions compute(Set<ODPair> agents) {
//		return this.computeNetworkLoading(agents, Collections.emptySet());
		double[] linkFlows_veh = this.computeLinkFlows_veh(agents, Collections.emptySet());
		DoubleArrayNetworkConditions travelTimes = new DoubleArrayNetworkConditions(this.network.getNumberOfLinks());
		for (int i = 0; i < this.network.getNumberOfLinks(); i++) {
			travelTimes.data[i] = this.network.computeTravelTime_s(i, linkFlows_veh[i]);
		}
		return travelTimes;
	}
}
