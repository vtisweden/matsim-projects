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
package se.vti.atap.examples.minimalframework.parallel_links;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import se.vti.atap.examples.minimalframework.parallel_links.ods.ODPair;
import se.vti.atap.minimalframework.NetworkConditions;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkConditionsImpl implements NetworkConditions {

	public final double[] linkFlows_veh;

	public final double[] linkTravelTimes_s;

	public final double[] dLinkTravelTimes_dLinkFlows_s_veh;

	public final Map<ODPair, SingleODBeckmanApproximation> od2beckmanApproximations;

	public NetworkConditionsImpl(Set<ODPair> odPairs, double[] linkFlows_veh, double[] linkTravelTimes_s,
			double[] dLinkTravelTimes_dLinkFlows_s_veh) {
		this.linkFlows_veh = linkFlows_veh;
		this.linkTravelTimes_s = linkTravelTimes_s;
		this.dLinkTravelTimes_dLinkFlows_s_veh = dLinkTravelTimes_dLinkFlows_s_veh;
		this.od2beckmanApproximations = odPairs.stream()
				.collect(Collectors.toMap(od -> od, od -> new SingleODBeckmanApproximation(od, this)));
	}

}
