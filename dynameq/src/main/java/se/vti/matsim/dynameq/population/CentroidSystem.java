/**
 * se.vti.matsim.dynameq
 * 
 * Copyright (C) 2023 by Filip Kristofferson (VTI) and Gunnar Flötteröd (VTI, LiU).
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
package se.vti.matsim.dynameq.population;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * 
 * This class handles the centroid system information. A Centroid must be
 * defined as a Node in the Network which is connected by exactly one in-link
 * and one out-link.
 * 
 * @author FilipK
 *
 */
public class CentroidSystem {

	public static final String ORIGINAL_ID_KEY = "original_id";

	/**
	 * This class stores data of a specific centroid
	 */
	public class Centroid {
		private final Id<Centroid> id;
		private final Id<Node> node;
		private final Id<Link> fromLink;
		private final Id<Link> toLink;

		private Centroid(final Id<Centroid> id, final Id<Node> node, final Id<Link> fromLink, final Id<Link> toLink) {
			this.id = id;
			this.node = node;
			this.fromLink = fromLink;
			this.toLink = toLink;
		}

		/**
		 * This method gets the id of the centroid
		 * 
		 * @return centroid id
		 */
		public Id<Centroid> getId() {
			return this.id;
		}

		/**
		 * This method gets the node id associated with the centroid
		 * 
		 * @return node id
		 */
		public Id<Node> getNode() {
			return this.node;
		}

		/**
		 * This method gets the link id of the link that goes from the centroid to the
		 * rest of the network
		 * 
		 * @return from link id
		 */
		public Id<Link> getFromLink() {
			return this.fromLink;
		}

		/**
		 * This method gets the link id of the link that goes to the centroid to the
		 * rest of the network.
		 * 
		 * @return to link id
		 */
		public Id<Link> getToLink() {
			return this.toLink;
		}
	}

	private final Map<Id<Centroid>, Centroid> id2centroid = Collections.synchronizedMap(new LinkedHashMap<>());

	/**
	 * Main constructor
	 * 
	 * @param centroidNodes - the node ids that centroids should be created from
	 * @param network       - the Network object from which the centroidNodes are
	 *                      fetched
	 * @throws IllegalStateException if a centroid node does not have exactly one
	 *                               in-link and one out-link
	 */
	public CentroidSystem(Set<Id<Node>> centroidNodes, Network network) {
		for (Id<Node> nodeId : centroidNodes) {

			Node node = network.getNodes().get(nodeId);

			// Important - the original id is needed since the node id (might) have been
			// changed and won't match the origin/destinations in the OD-matrices
			String originalNodeId = (String) node.getAttributes().getAttribute(ORIGINAL_ID_KEY);

			final Id<CentroidSystem.Centroid> centroidId = Id.create(originalNodeId, CentroidSystem.Centroid.class);

			Id<Link> inLink = getSingleLink(node.getInLinks(), "in", node.getId());
			Id<Link> outLink = getSingleLink(node.getOutLinks(), "out", node.getId());

			this.add(centroidId, node.getId(), outLink, inLink);
		}
	}

	private Id<Link> getSingleLink(Map<Id<Link>, ? extends Link> links, String linkType, Id<Node> nodeId) {
		if (links.size() > 1) {
			throw new IllegalStateException("Node " + nodeId + " has more than one " + linkType + " link!");
		} else if (links.isEmpty()) {
			throw new IllegalStateException("Node " + nodeId + " has no " + linkType + " link!");
		} else {
			return links.values().iterator().next().getId();
		}
	}

	/**
	 * This method adds a new centroid to the system
	 * 
	 * @param centroidId - the id of the centroid
	 * @param node       - the node id associated with the centroid
	 * @param fromLink   - the from link id
	 * @param toLink     - the to link id
	 */
	private void add(final Id<Centroid> centroidId, final Id<Node> node, final Id<Link> fromLink,
			final Id<Link> toLink) {
		if (id2centroid.containsKey(centroidId)) {
			throw new IllegalArgumentException("Centroid with ID " + centroidId + " already exists.");
		}

		id2centroid.put(centroidId, new Centroid(centroidId, node, fromLink, toLink));
	}

	/**
	 * This method gets all centroids
	 * 
	 * @return set of centroids
	 */
	public Map<Id<Centroid>, Centroid> getAllCentroids() {
		return this.id2centroid;
	}

	/**
	 * This method gets the number of centroids
	 * 
	 * @return number of centroids
	 */
	public int centoridCnt() {
		return this.id2centroid.size();
	}

}
