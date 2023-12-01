/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation;

import java.util.List;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.OD;

/**
 * 
 * @author GunnarF
 *
 */
public class UnimodalNetworkRouter {

	private final Network network;
	private final LeastCostPathCalculator router;

	public UnimodalNetworkRouter(final Network network, final TravelDisutility disutility) {
		this.network = network;
		AStarLandmarksFactory factory = new AStarLandmarksFactory(1);
		this.router = factory.createPathCalculator(network, disutility, new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return 0;
			}
		});
	}

	public List<Link> route(final OD od) {
		return this.route(od.origin, od.destination);
	}

	public List<Link> route(final Id<Node> fromNodeId, final Id<Node> toNodeId) {
		final Node fromNode = this.network.getNodes().get(fromNodeId);
		final Node toNode = this.network.getNodes().get(toNodeId);
		if (fromNode == null || toNode == null) {
			Log.warn("Nonexisting od-pair from " + fromNodeId + " to " + toNodeId);
			return null;
		} else {
			return this.router.calcLeastCostPath(fromNode, toNode, 0, null, null).links;
		}
	}
}
