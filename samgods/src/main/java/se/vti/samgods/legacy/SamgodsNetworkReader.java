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
package se.vti.samgods.legacy;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

import floetteroed.utilities.Units;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 *
 * @author GunnarF
 *
 */
public class SamgodsNetworkReader {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(SamgodsNetworkReader.class);

	private static final double bwdWaveSpeed_m_s = Units.M_S_PER_KM_H * 15.0;
	private static final double rhoMax_veh_m = Units.VEH_M_PER_VEH_KM * 140.0;

	private static final String NODE_ID = "Node";
	private static final String NODE_X = "X";
	private static final String NODE_Y = "Y";
	private static final String NODE_MODE = "Mode";

	static final String NODE_IS_TERMINAL = "Transfer_point";

	private static final String LINK_FROM_NODE = "From";
	private static final String LINK_TO_NODE = "To";
	private static final String LINK_MODE = "Mode";
	private static final String LINK_LENGTH_KM = "Length_km";
	private static final String LINK_MAXSPEED_KM_H = "Max_speed_km_per_hour";
	private static final String LINK_CAPACITY_TRAINS_DAY = "Capacity_trains_per_day";

	// -------------------- MEMBERS --------------------

	private final Network network;

	// -------------------- CONSTRUCTION AND IMPLEMENTATION --------------------

	public SamgodsNetworkReader(final String nodesFile, final String linksFile) {

		this.network = NetworkUtils.createNetwork();
		this.network.setCapacityPeriod(3600.0);

		{
			log.info("Loading samgods nodes file: " + nodesFile);
			final TabularFileHandler nodesHandler = new AbstractTabularFileHandlerWithHeaderLine() {
				@Override
				public void startCurrentDataRow() {
					final Id<Node> id = Id.createNodeId(this.getStringValue(NODE_ID));
					final double x = this.getDoubleValue(NODE_X);
					final double y = this.getDoubleValue(NODE_Y);
					final String mode = this.getStringValue(NODE_MODE);
					final boolean isTerminal = "1".equals(this.getStringValue(NODE_IS_TERMINAL));
					final Node node = NetworkUtils.createAndAddNode(network, id, new Coord(x, y));
					node.getAttributes().putAttribute(NODE_MODE, mode);
					node.getAttributes().putAttribute(NODE_IS_TERMINAL, isTerminal);
				}
			};
			final TabularFileParser nodesParser = new TabularFileParser();
			nodesParser.setDelimiterTags(new String[] { "," });
			nodesParser.setOmitEmptyColumns(false);
			try {
				nodesParser.parse(nodesFile, nodesHandler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		{
			log.info("Loading samgods links file: " + linksFile);
			final TabularFileHandler linksHandler = new AbstractTabularFileHandlerWithHeaderLine() {

				@Override
				public void startCurrentDataRow() {

					final Node fromNode = network.getNodes().get(Id.createNodeId(this.getStringValue(LINK_FROM_NODE)));
					final Node toNode = network.getNodes().get(Id.createNodeId(this.getStringValue(LINK_TO_NODE)));
					final Id<Link> id = Id.createLinkId(fromNode.getId() + "_" + toNode.getId());
					final String mode = this.getStringValue(LINK_MODE);
					final Double length_m = Units.M_PER_KM * this.getDoubleValue(LINK_LENGTH_KM);
					final Double maxSpeed_km_h = this.getDoubleValue(LINK_MAXSPEED_KM_H); // may be null

					final String matsimMode;
					final Double maxSpeed_m_s;
					final Double capacity_veh_h;
					final Integer lanes;
					if (Samgods.TransportMode.Road.toString().equals(mode)) {
						matsimMode = TransportMode.car;
						maxSpeed_m_s = Units.M_S_PER_KM_H * maxSpeed_km_h;
						capacity_veh_h = Units.VEH_H_PER_VEH_S * rhoMax_veh_m * maxSpeed_m_s * bwdWaveSpeed_m_s
								/ (maxSpeed_m_s + bwdWaveSpeed_m_s);
						lanes = 2;
					} else if (Samgods.TransportMode.Rail.toString().equals(mode)) {
						matsimMode = TransportMode.train;
						maxSpeed_m_s = Units.M_S_PER_KM_H * maxSpeed_km_h;
						final Double capFromFile = this.getDoubleValue(LINK_CAPACITY_TRAINS_DAY);
						if (capFromFile != null) {
							capacity_veh_h = this.getDoubleValue(LINK_CAPACITY_TRAINS_DAY) / 24.0;
						} else {
							log.warn("Rail link without capacity value, setting to 1 train/minute.");
							capacity_veh_h = 60.0;
						}
						lanes = 1;
					} else if (Samgods.TransportMode.Sea.toString().equals(mode)) {
						matsimMode = TransportMode.ship;
						if (maxSpeed_km_h != null) {
							maxSpeed_m_s = Units.M_S_PER_KM_H * maxSpeed_km_h;
						} else {
							log.warn("Sea link without max speed value, setting to 30 km/h.");
							maxSpeed_m_s = Units.M_S_PER_KM_H * 30.0;
						}
						capacity_veh_h = 60.0;
						lanes = 1;
					} else if (Samgods.TransportMode.Air.toString().equals(mode)) {
						matsimMode = TransportMode.airplane;
						if (maxSpeed_km_h != null) {
							maxSpeed_m_s = Units.M_S_PER_KM_H * maxSpeed_km_h;
						} else {
							log.warn("Air link without capacity value, setting to 800 km/h.");
							maxSpeed_m_s = Units.M_S_PER_KM_H * 800.0;
						}
						capacity_veh_h = 60.0;
						lanes = 1;
					} else {
						matsimMode = null;
						maxSpeed_m_s = null;
						capacity_veh_h = null;
						lanes = null;
					}

					if (matsimMode != null) {
						final Link link = NetworkUtils.createAndAddLink(network, id, fromNode, toNode, length_m,
								maxSpeed_m_s, capacity_veh_h, lanes, null, null);
						link.setAllowedModes(Collections.singleton(matsimMode));
					}
				}
			};
			final TabularFileParser linksParser = new TabularFileParser();
			linksParser.setDelimiterTags(new String[] { "," });
			linksParser.setOmitEmptyColumns(false);
			try {
				linksParser.parse(linksFile, linksHandler);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		System.out.println("nodes: " + this.network.getNodes().size());
		System.out.println("links: " + this.network.getLinks().size());
	}

	public Network getNetwork() {
		return this.network;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		SamgodsNetworkReader loader = new SamgodsNetworkReader("./2023-06-01_basecase/node_table.csv",
				"./2023-06-01_basecase/link_table.csv");
		// NetworkUtils.writeNetwork(loader.getNetwork(), "network.xml");

	}
}
