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
package se.vti.matsim.dynameq.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import se.vti.matsim.dynameq.utils.Units;

/**
 * 
 * @author FilipK
 *
 */
public class BaseNetworkFileReader {

	public static final String ID_KEY = "id";
	public static final String X_COORD_KEY = "x-coordinate";
	public static final String Y_COORD_KEY = "y-coordinate";
	public static final String CONTROL_KEY = "control";
	public static final String PRIORITY_KEY = "priority";
	public static final String TYPE_KEY = "type";
	public static final String LEVEL_KEY = "level";
	public static final String START_KEY = "start";
	public static final String END_KEY = "end";
	public static final String FSPEED_KEY = "fspeed";
	public static final String LANES_KEY = "lanes";
	public static final String DIR_KEY = "dir";
	public static final String REV_KEY = "rev";
	public static final String FACI_KEY = "faci";
	public static final String LEN_KEY = "len";
	public static final String LENFAC_KEY = "lenfac";
	public static final String RESFAC_KEY = "resfac";
	public static final String RABOUT_KEY = "rabout";
	public static final String NAME_KEY = "name";
	public static final String LINK_ID_KEY = "link_id";
	public static final String CENTROID_ID_KEY = "centroid_id";
	public static final String ORIGINAL_ID_KEY = "original_id";
	public static final String COST_KEY = "cost";
	public static final String CONTROL_VALUE_99_KEY = "99";
	
	public static final int SUPER_CENTROID_DISPLACEMENT_M = 10; // TODO: Needs to be longer than the longest vehicle?

	TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();

	public BaseNetworkFileReader(String filePath) {
		// Delimiter will split lines at a whitespace IF not located between quotes
		this.tabularFileParserConfig.setDelimiterRegex("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		this.tabularFileParserConfig.setFileName(filePath);
	}

	private List<HashMap<String, String>> readFileSection(String startLine, String endLine) {
		this.tabularFileParserConfig.setStartRegex(startLine);
		this.tabularFileParserConfig.setEndRegex(endLine);
		List<HashMap<String, String>> parsedFileSection = parseFileSection();
		return parsedFileSection;
	}

	private List<HashMap<String, String>> parseFileSection() {
		List<HashMap<String, String>> data = new ArrayList<>();
		new TabularFileParser().parse(tabularFileParserConfig, new TabularFileHandler() {

			List<String> header = new ArrayList<>();
			int line = 0;

			@Override
			public void startRow(String[] row) {
				if (line == 0) {
					for (int i = 0; i < row.length; i++) {
						header.add(row[i]);
					}
				}
				if (line > 0) {
					if (row.length != header.size()) {
						throw new IllegalArgumentException("Row: " + Arrays.toString(row)
								+ " does not have the same number of elements as the header");
					}
					HashMap<String, String> map = new HashMap<>();
					for (int i = 0; i < row.length; i++) {
						map.put(header.get(i), row[i]);
					}
					data.add(map);
				}
				line++;
			}
		});
		return data;
	}

	public void readAndAddNodes(Network network, String startLineNodes, String endLineNodes) {
		List<HashMap<String, String>> parsedNodes = readFileSection(startLineNodes, endLineNodes);
		for (HashMap<String, String> row : parsedNodes) {
			String nodeId = row.get(ID_KEY);
			double xCoord = Double.parseDouble((String) row.get(X_COORD_KEY));
			double yCoord = Double.parseDouble((String) row.get(Y_COORD_KEY));
			Node node = NetworkUtils.createAndAddNode(network, Id.createNodeId(nodeId), new Coord(xCoord, yCoord));
			node.getAttributes().putAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY, Utils.NodeTypeConstants.NODE);

			node.getAttributes().putAttribute(CONTROL_KEY, row.get(CONTROL_KEY));
			node.getAttributes().putAttribute(PRIORITY_KEY, row.get(PRIORITY_KEY));
			node.getAttributes().putAttribute(TYPE_KEY, row.get(TYPE_KEY));
			node.getAttributes().putAttribute(LEVEL_KEY, row.get(LEVEL_KEY));
		}
	}

	public void readAndAddLinks(Network network, String startLineLinks, String endLineLinks) {
		List<HashMap<String, String>> parsedLinks = readFileSection(startLineLinks, endLineLinks);
		for (HashMap<String, String> row : parsedLinks) {
			Id<Link> linkId = Id.createLinkId(row.get(ID_KEY));
			Node fromNode = network.getNodes().get(Id.createNodeId(row.get(START_KEY)));
			Node toNode = network.getNodes().get(Id.createNodeId(row.get(END_KEY)));
			double freespeed_M_S = Double.parseDouble((String) row.get(FSPEED_KEY)) * Units.M_S_PER_KM_H;
			int numLanes = Integer.parseInt((String) row.get(LANES_KEY));
			double length_M = CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
			double capacity_VEH_H = Utils.freespeedAndNumLanesToCapacity_VEH_H(freespeed_M_S, numLanes);

			Link link = NetworkUtils.createAndAddLink(network, linkId, fromNode, toNode, length_M, freespeed_M_S,
					capacity_VEH_H, numLanes);

			Set<String> matsimModes = new LinkedHashSet<>();
			matsimModes.add("car");
			link.setAllowedModes(matsimModes);

			link.getAttributes().putAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY, Utils.LinkTypeConstants.LINK);

			link.getAttributes().putAttribute(DIR_KEY, row.get(DIR_KEY));
			link.getAttributes().putAttribute(REV_KEY, row.get(REV_KEY));
			link.getAttributes().putAttribute(FACI_KEY, row.get(FACI_KEY));
			link.getAttributes().putAttribute(LEN_KEY, row.get(LEN_KEY));
			link.getAttributes().putAttribute(LENFAC_KEY, row.get(LENFAC_KEY));
			link.getAttributes().putAttribute(RESFAC_KEY, row.get(RESFAC_KEY));
			link.getAttributes().putAttribute(RABOUT_KEY, row.get(RABOUT_KEY));
			link.getAttributes().putAttribute(LEVEL_KEY, row.get(LEVEL_KEY));
			link.getAttributes().putAttribute(NAME_KEY, row.get(NAME_KEY));
		}
	}

	/**
	 * Centroids are added as nodes. Virtual links connect centroids to normal nodes
	 */
	public void readAndAddCentroids(Network network, String startLineCentroids, String endLineCentroids) {
		List<HashMap<String, String>> parsedCentroids = readFileSection(startLineCentroids, endLineCentroids);
		for (HashMap<String, String> row : parsedCentroids) {
			String nodeId = Utils.toCentroidId(row.get(ID_KEY));
			double xCoord = Double.parseDouble((String) row.get(X_COORD_KEY));
			double yCoord = Double.parseDouble((String) row.get(Y_COORD_KEY));
			Node node = NetworkUtils.createAndAddNode(network, Id.createNodeId(nodeId), new Coord(xCoord, yCoord));

			node.getAttributes().putAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY, Utils.NodeTypeConstants.CENTROID);
			node.getAttributes().putAttribute(ORIGINAL_ID_KEY, row.get(ID_KEY));

			node.getAttributes().putAttribute(LEVEL_KEY, row.get(LEVEL_KEY));
			node.getAttributes().putAttribute(NAME_KEY, row.get(NAME_KEY));
		}
	}

	/**
	 * Virtual links connect centroids to the rest of the network. Virtual links are
	 * added with a short (1 second) traversal time and high capacity
	 */
	public void readAndAddVirtualLinks(Network network, String startLineVirtualLinks, String endLineVirtualLinks) {

		List<HashMap<String, String>> parsedVirtualLinks = readFileSection(startLineVirtualLinks, endLineVirtualLinks);

		int virtualLinkCounter = 0;

		for (HashMap<String, String> row : parsedVirtualLinks) {

			Id<Link> virtualLinkId = Id.createLinkId(Utils.toVirtualLinkId(String.valueOf(virtualLinkCounter)));
			Link toLink = network.getLinks().get(Id.createLinkId(row.get(LINK_ID_KEY)));

			Node centroidNode = network.getNodes().get(Id.createNodeId(Utils.toCentroidId(row.get(CENTROID_ID_KEY))));
			Node connectingNode = getVirtualLinkConnectingNodeFromToLink(network, toLink);

			Map<String, Node> nodeMap = determineLinkDirection(connectingNode, centroidNode, toLink);
			Node fromNode = nodeMap.get("fromNode");
			Node toNode = nodeMap.get("toNode");

			// Such that traversal time is 1 second
			double freespeed_M_S = 100 * Units.M_S_PER_KM_H;
			double length_M = freespeed_M_S;

			int numLanes = 10;
			double capacity_VEH_H = Utils.freespeedAndNumLanesToCapacity_VEH_H(freespeed_M_S, numLanes);

			Link link = NetworkUtils.createAndAddLink(network, virtualLinkId, fromNode, toNode, length_M, freespeed_M_S,
					capacity_VEH_H, numLanes);

			Set<String> matsimModes = new LinkedHashSet<>();
			matsimModes.add("car");
			link.setAllowedModes(matsimModes);

			link.getAttributes().putAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY, Utils.LinkTypeConstants.VIRTUAL_LINK);

			link.getAttributes().putAttribute(COST_KEY, row.get(COST_KEY));

			virtualLinkCounter++;
		}
	}

	/**
	 * Determines whether the link that connects a centroid to a normal node should
	 * go from the centroid to the connecting node or vice versa
	 */
	private Map<String, Node> determineLinkDirection(Node connectingNode, Node centroidNode, Link toLink) {
		Node fromNode = null;
		Node toNode = null;
		Map<String, Node> nodeMap = new HashMap<>();

		if (connectingNode.getId().equals(toLink.getToNode().getId())) {
			fromNode = connectingNode;
			toNode = centroidNode;
		} else if (connectingNode.getId().equals(toLink.getFromNode().getId())) {
			fromNode = centroidNode;
			toNode = connectingNode;
		} else {
			throw new IllegalArgumentException("Invalid Node or Link provided");
		}

		nodeMap.put("fromNode", fromNode);
		nodeMap.put("toNode", toNode);

		return nodeMap;
	}

	/**
	 * Find the node that connects the link to the centroid. Nodes that have a
	 * centroid connection has a "control" value of 99
	 */
	private Node getVirtualLinkConnectingNodeFromToLink(Network network, Link toLink) {
		Node linkFromNode = toLink.getFromNode();
		Node linkToNode = toLink.getToNode();

		if (CONTROL_VALUE_99_KEY.equals(linkFromNode.getAttributes().getAttribute(CONTROL_KEY))
				&& CONTROL_VALUE_99_KEY.equals(linkToNode.getAttributes().getAttribute(CONTROL_KEY))) {
			throw new IllegalArgumentException("Both nodes seems to have a virtual link connected to it");

		} else if (!CONTROL_VALUE_99_KEY.equals(linkFromNode.getAttributes().getAttribute(CONTROL_KEY))
				&& !CONTROL_VALUE_99_KEY.equals(linkToNode.getAttributes().getAttribute(CONTROL_KEY))) {
			throw new IllegalArgumentException("None of the nodes seems to have a virtual link connected to it");

		} else if (CONTROL_VALUE_99_KEY.equals(linkFromNode.getAttributes().getAttribute(CONTROL_KEY))) {
			return linkFromNode;

		} else {
			return linkToNode;
		}
	}

	/**
	 * Adds and connects a super centroid close to every centroid. Centroids can be
	 * connected to more than one entry point (normal node) of the network and the
	 * super centroid assures that an agent can travel to all of the entry points
	 */
	public void addSuperCentroids(Network network) {
		for (Node node : network.getNodes().values()) {
			if (node.getAttributes().getAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY).equals(Utils.NodeTypeConstants.CENTROID)) {
				String superCentroidId = Utils.centroidToSuperCentroidId(node.getId().toString());
				double superCentroidXcoord = node.getCoord().getX() + SUPER_CENTROID_DISPLACEMENT_M;
				double superCentroidYcoord = node.getCoord().getY() + SUPER_CENTROID_DISPLACEMENT_M;
				Node superCentroid = NetworkUtils.createAndAddNode(network, Id.createNodeId(superCentroidId),
						new Coord(superCentroidXcoord, superCentroidYcoord));
				superCentroid.getAttributes().putAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY, Utils.NodeTypeConstants.SUPER_CENTROID);

				String originalId = (String) node.getAttributes().getAttribute(ORIGINAL_ID_KEY);
				superCentroid.getAttributes().putAttribute(ORIGINAL_ID_KEY, originalId);

				connectCentroidToSuperCentroid(network, node, superCentroid);
			}
		}
	}

	/**
	 * Super virtual links are added with a short (1 second) traversal time and high
	 * capacity
	 */
	private void connectCentroidToSuperCentroid(Network network, Node centroid, Node superCentroid) {

		// Such that traversal time is 1 second
		double freespeed_M_S = 100 * Units.M_S_PER_KM_H;
		double length_M = freespeed_M_S;

		int numLanes = 10;
		double capacity_VEH_H = Utils.freespeedAndNumLanesToCapacity_VEH_H(freespeed_M_S, numLanes);

		Id<Link> fromLinkId = Id.createLinkId("from_" + superCentroid.getId().toString());
		Id<Link> toLinkId = Id.createLinkId("to_" + superCentroid.getId().toString());

		Link fromLink = NetworkUtils.createAndAddLink(network, fromLinkId, superCentroid, centroid, length_M,
				freespeed_M_S, capacity_VEH_H, numLanes);
		fromLink.getAttributes().putAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY, Utils.LinkTypeConstants.SUPER_VIRTUAL_LINK);

		Link toLink = NetworkUtils.createAndAddLink(network, toLinkId, centroid, superCentroid, length_M, freespeed_M_S,
				capacity_VEH_H, numLanes);
		toLink.getAttributes().putAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY, Utils.LinkTypeConstants.SUPER_VIRTUAL_LINK);
	}
}
