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
package se.vti.samgods.readers;

import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.TransportSupply;

/**
 *
 * @author GunnarF
 *
 */
public class SamgodsNetworkReader {

	// -------------------- CONSTANTS --------------------

//	private static final Logger log = Logger.getLogger(SamgodsNetworkReader.class);

	private static final String NODE_ID = "N";
	private static final String NODE_X = "X";
	private static final String NODE_Y = "Y";

	private static final String LINK_ID = "OBJECTID";
	private static final String LINK_FROM_NODE = "A";
	private static final String LINK_TO_NODE = "B";
	private static final String LINK_LENGTH_M = "SHAPE_Length";
	private static final String LINK_SPEED_1 = "SPEED_1";
	private static final String LINK_SPEED_2 = "SPEED_2";
	private static final String LINK_LANES = "NLANES";
	private static final String LINK_MODE = "GENERAL_MO";
	private static final String LINK_CAPACITY_TRAINS_DAY = "ORIGCAP";

	// -------------------- MEMBERS --------------------

	private final Network network;

	// -------------------- CONSTRUCTION AND IMPLEMENTATION --------------------

	public SamgodsNetworkReader(final String nodesFile, final String linksFile) throws IOException {

		this.network = NetworkUtils.createNetwork();
		this.network.setCapacityPeriod(3600.0);

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(nodesFile))) {
			final Id<Node> id = Id.createNodeId(record.get(NODE_ID));
			final double x = Double.parseDouble(record.get(NODE_X));
			final double y = Double.parseDouble(record.get(NODE_Y));
			NetworkUtils.createAndAddNode(this.network, id, new Coord(x, y));
		}

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(linksFile))) {

			final Node fromNode = this.network.getNodes().get(Id.createNodeId(record.get(LINK_FROM_NODE)));
			final Node toNode = this.network.getNodes().get(Id.createNodeId(record.get(LINK_TO_NODE)));

			final Id<Link> id = Id.createLinkId(record.get(LINK_ID));

			final double length_m = Double.parseDouble(record.get(LINK_LENGTH_M));
			final double lanes = Double.parseDouble(record.get(LINK_LANES));

			final Double speed1_km_h = ReaderUtils.parseDoubleOrNull(record.get(LINK_SPEED_1));
			final Double speed2_km_h = ReaderUtils.parseDoubleOrNull(record.get(LINK_SPEED_2));

			final double maxSpeed_m_s; // TODO Revisit this logic, unclear what speed an "undefined" link should have.
			if (speed1_km_h != null) {
				maxSpeed_m_s = Units.M_S_PER_KM_H * speed1_km_h;
			} else {
				maxSpeed_m_s = Double.NaN; // TODO Trying to cause a clean failure instead of inventing numbers.
			}

			final SamgodsConstants.TransportMode samgodsMode = SamgodsConstants.TransportMode
					.valueOf(record.get(LINK_MODE));
			final String matsimMode = TransportSupply.samgodsMode2matsimMode.get(samgodsMode);
			final double capacity_veh_h = ReaderUtils.parseDoubleOrDefault(record.get(LINK_CAPACITY_TRAINS_DAY),
					Double.POSITIVE_INFINITY) / 24.0;

			final Link link = NetworkUtils.createAndAddLink(this.network, id, fromNode, toNode, length_m, maxSpeed_m_s,
					capacity_veh_h, lanes, null, null);
			link.setAllowedModes(Collections.singleton(matsimMode));
			link.getAttributes().putAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME,
					new SamgodsLinkAttributes(samgodsMode, speed1_km_h, speed2_km_h));
		}

		System.out.println("nodes: " + this.network.getNodes().size());
		System.out.println("links: " + this.network.getLinks().size());
	}

	public Network getNetwork() {
		return this.network;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		SamgodsNetworkReader loader = new SamgodsNetworkReader("./input_2024/node_parameters.csv",
				"./input_2024/link_parameters.csv");
		NetworkUtils.writeNetwork(loader.getNetwork(), "./input_2024/matsim-network.xml");

		System.out.println();
		System.out.println(ReaderUtils.createNetworkStatsTable(loader.getNetwork()));

	}
}
