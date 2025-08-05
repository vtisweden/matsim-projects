/**
 * se.vti.atap.minimalframework.common
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

import se.vti.atap.examples.minimalframework.parallel_links.DoubleArrayNetworkConditions;
import se.vti.atap.examples.minimalframework.parallel_links.Network;
import se.vti.atap.minimalframework.planselection.proposed.ApproximateNetworkLoading;

/**
 * 
 * @author GunnarF
 *
 */
public class ApproximateNetworkLoadingImpl
		implements ApproximateNetworkLoading<ODPair, DoubleArrayNetworkConditions, ApproximateNetworkConditionsImpl> {

	private final Network network;

	private final boolean unfair;

	public ApproximateNetworkLoadingImpl(Network network, boolean unfair) {
		this.network = network;
		this.unfair = unfair;
	}

	@Override
	public ApproximateNetworkConditionsImpl compute(Set<ODPair> agentsUsingCurrentPlans,
			Set<ODPair> agentsUsingCandidatePlans, DoubleArrayNetworkConditions networkConditions) {
		var result = new ApproximateNetworkConditionsImpl(agentsUsingCurrentPlans, agentsUsingCandidatePlans,
				this.network);
		if (this.unfair) {
			result.setFlowTransformation(f -> new ExactNetworkLoadingImpl(this.network).computeNetworkLoading(f).data);
		}
		return result;
	}

}
