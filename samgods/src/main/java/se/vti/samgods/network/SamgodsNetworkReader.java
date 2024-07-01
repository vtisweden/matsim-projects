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
package se.vti.samgods.network;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import de.vandermeer.asciitable.AsciiTable;
import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.utils.ParseNumberUtils;

/**
 *
 * @author GunnarF
 *
 */
public class SamgodsNetworkReader {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(SamgodsNetworkReader.class);

	private static final String NODE_COUNTER = "N";
	private static final String NODE_X = "X";
	private static final String NODE_Y = "Y";
	private static final String NODE_ID = "NORIG";

	private static final String LINK_ID = "OBJECTID";
	private static final String LINK_FROM_NODE_COUNTER = "A";
	private static final String LINK_TO_NODE_COUNTER = "B";
	private static final String LINK_LENGTH_M = "SHAPE_Length";
	private static final String LINK_SPEED_1 = "SPEED_1";
	private static final String LINK_SPEED_2 = "SPEED_2";
	private static final String LINK_LANES = "NLANES";
	private static final String LINK_MODE = "GENERAL_MO";
	private static final String LINK_CAPACITY_TRAINS_DAY = "ORIGCAP";

	// -------------------- CONSTRUCTION --------------------

	private SamgodsNetworkReader() {
		// Do not instantiate.
	}

	// -------------------- IMPLEMENTATION --------------------

	public static Network load(final String nodesFile, final String linksFile) throws IOException {

		final Network network = NetworkUtils.createNetwork();
		network.setCapacityPeriod(3600.0);

		final Map<Long, Node> nodeCounter2node = new LinkedHashMap<>();

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(nodesFile))) {
			final Id<Node> id = Id.createNodeId(Long.parseLong(record.get(NODE_ID)));
			final double x = Double.parseDouble(record.get(NODE_X));
			final double y = Double.parseDouble(record.get(NODE_Y));
			Node node = NetworkUtils.createAndAddNode(network, id, new Coord(x, y));
			nodeCounter2node.put(Long.parseLong(record.get(NODE_COUNTER)), node);
		}

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(linksFile))) {

			final Id<Link> id = Id.createLinkId(Long.parseLong(record.get(LINK_ID)));

			final Node fromNode = nodeCounter2node.get(Long.parseLong(record.get(LINK_FROM_NODE_COUNTER)));
			final Node toNode = nodeCounter2node.get(Long.parseLong(record.get(LINK_TO_NODE_COUNTER)));
			assert(fromNode != null);
			assert(toNode != null);

			final double length_m = Double.parseDouble(record.get(LINK_LENGTH_M));
			final double lanes = Double.parseDouble(record.get(LINK_LANES));

			final Double speed1_km_h = ParseNumberUtils.parseDoubleOrNull(record.get(LINK_SPEED_1));
			final Double speed2_km_h = ParseNumberUtils.parseDoubleOrNull(record.get(LINK_SPEED_2));

			final SamgodsConstants.TransportMode samgodsMode = SamgodsConstants.TransportMode
					.valueOf(record.get(LINK_MODE));
			final Set<String> matsimModes = SamgodsConstants.samgodsMode2matsimModes.get(samgodsMode);

			final double speed_km_h;
			if (speed1_km_h != null) {
				if (speed2_km_h != null) {
					speed_km_h = Math.min(speed1_km_h, speed2_km_h);
				} else {
					speed_km_h = speed1_km_h;
				}
			} else {
				if (speed2_km_h != null) {
					speed_km_h = speed2_km_h;
				} else {
					speed_km_h = Double.POSITIVE_INFINITY;
//					log.warn("Link " + id + " has infinite speed.");
				}
			}
//			if (SamgodsConstants.TransportMode.Air.equals(samgodsMode)) {
//				log.info("speed_km_h = " + speed_km_h + "\t speed1_km_h = " + speed1_km_h + "\t speed2_km_h = "
//						+ speed2_km_h);
//			}

			final double capacity_veh_h = ParseNumberUtils.parseDoubleOrDefault(record.get(LINK_CAPACITY_TRAINS_DAY),
					Double.POSITIVE_INFINITY) / 24.0;
//			if (Double.isInfinite(capacity_veh_h)) {
//				log.warn("Link " + id + " has infinte capacity.");
//			}

			final Link link = NetworkUtils.createAndAddLink(network, id, fromNode, toNode, length_m,
					Units.M_S_PER_KM_H * speed_km_h, capacity_veh_h, lanes, null, null);
			link.setAllowedModes(matsimModes);
			link.getAttributes().putAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME,
					new SamgodsLinkAttributes(samgodsMode, speed1_km_h, speed2_km_h));
		}

		log.info("Loaded " + network.getNodes().size() + " nodes.");
		log.info("Loaded " + network.getLinks().size() + " links.");

		return network;
	}

	public static String createNetworkStatsTable(Network network) {

		StringBuffer result = new StringBuffer();

		Map<String, Integer> mode2cnt = new LinkedHashMap<>();
		Map<String, Integer> mode2speed1cnt = new LinkedHashMap<>();
		Map<String, Integer> mode2speed2cnt = new LinkedHashMap<>();

		Map<String, Double> mode2speedSum = new LinkedHashMap<>();
		Map<String, Double> mode2speed1Sum = new LinkedHashMap<>();
		Map<String, Double> mode2speed2Sum = new LinkedHashMap<>();
		Map<String, Double> mode2lengthSum = new LinkedHashMap<>();
		Map<String, Double> mode2lanesSum = new LinkedHashMap<>();

		for (Link link : network.getLinks().values()) {
			String mode = SamgodsLinkAttributes.getSamgodsMode(link).toString();
			mode2cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
			mode2lengthSum.compute(mode, (m, l) -> l == null ? link.getLength() : l + link.getLength());
			mode2lanesSum.compute(mode, (m, l) -> l == null ? link.getNumberOfLanes() : l + link.getNumberOfLanes());
			mode2speedSum.compute(mode, (m, s) -> s == null ? link.getFreespeed() : s + link.getFreespeed());

			SamgodsLinkAttributes attr = SamgodsLinkAttributes.getAttrs(link);
			if (attr.speed1_km_h != null && attr.speed1_km_h != 0) {
				mode2speed1Sum.compute(mode, (m, s) -> s == null ? attr.speed1_km_h : s + attr.speed1_km_h);
				mode2speed1cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
			}
			if (attr.speed2_km_h != null && attr.speed2_km_h != 0) {
				mode2speed2Sum.compute(mode, (m, s) -> s == null ? attr.speed2_km_h : s + attr.speed2_km_h);
				mode2speed2cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
			}
		}

		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Mode", "Links", "Avg. length [km]", "Avg. no. of lanes", "Avg. speed [km/h]",
				"Avg. speed 1 [km/h] if >0", "Avg. speed 2 [km/h] if >0");
		table.addRule();
		for (Map.Entry<String, Integer> e : mode2cnt.entrySet()) {
			final String mode = e.getKey();
			final int cnt = e.getValue();
			table.addRow(mode, cnt,
					ParseNumberUtils.divideOrNothing(Units.KM_PER_M * mode2lengthSum.get(e.getKey()), cnt),
					ParseNumberUtils.divideOrNothing(mode2lanesSum.get(mode), cnt),
					ParseNumberUtils.divideOrNothing(Units.KM_H_PER_M_S * mode2speedSum.get(mode), cnt),
					ParseNumberUtils.divideOrNothing(mode2speed1Sum.get(mode), mode2speed1cnt.get(mode)),
					ParseNumberUtils.divideOrNothing(mode2speed2Sum.get(mode), mode2speed2cnt.get(mode)));
			table.addRule();
		}
		result.append(table.render());

		return result.toString();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		Network network = SamgodsNetworkReader.load("./input_2024/node_parameters.csv",
				"./input_2024/link_parameters.csv");
		NetworkUtils.writeNetwork(network, "./input_2024/matsim-network.xml");

		System.out.println();
		System.out.println(SamgodsNetworkReader.createNetworkStatsTable(network));
	}
}
