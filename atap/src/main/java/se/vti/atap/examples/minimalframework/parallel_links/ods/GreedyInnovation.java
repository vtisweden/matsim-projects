/**
 * se.vti.atap.examples.minimalframework.parallel_links_ods
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

import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.examples.minimalframework.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.PlanInnovation;

/**
 * 
 * @author GunnarF
 *
 */
public class GreedyInnovation implements PlanInnovation<ODPair, NetworkConditionsImpl> {

	private final NetworkConditionsImpl initialNetworkConditions;

	public GreedyInnovation(Network network) {
		this.initialNetworkConditions = NetworkConditionsImpl.createEmptyNetworkConditions(network);
	}

	@Override
	public void assignInitialPlan(ODPair odPair) {
		double[] pathFlows_veh = new double[odPair.getNumberOfPaths()];
		pathFlows_veh[odPair.computeBestPath(this.initialNetworkConditions)] = odPair.demand_veh;
		odPair.setCurrentPlan(new Paths(pathFlows_veh));
	}

	@Override
	public void assignCandidatePlan(ODPair odPair, NetworkConditionsImpl networkConditions) {
		odPair.setCandidatePlan(new Paths(odPair.computeApproximatelyEquilibratedPathFlows_veh(networkConditions)));
	}
}
