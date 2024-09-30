/**
 * se.vti.samgods.network
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import floetteroed.utilities.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class LinkRegionsReader {

	private static final Logger log = Logger.getLogger(LinkRegionsReader.class);

	private static final String From = "From";
	private static final String To = "To";
	private static final String REGION = "REGION";
	private static final String WEIGHT = "WEIGHT";

	private final Network network;


	public LinkRegionsReader(Network network) {
		this.network = network;
	}

	public Map<Id<Link>, Double> read(String fileName) throws IOException {

		final Map<Id<Link>, Double> linkId2domesticWeightSum = new LinkedHashMap<>();

		List<Tuple<Id<Node>, Id<Node>>> missingLinkNodeIds = new ArrayList<>();

		for (CSVRecord record : CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(fileName))) {

			final String regionStr = record.get(REGION);
			if (!"NA".equals(regionStr)) { // TODO replace by NULL value!!!

				final Id<Node> fromNodeId = Id.createNodeId(record.get(From));
				final Id<Node> toNodeId = Id.createNodeId(record.get(To));

				final Node fromNode = network.getNodes().get(fromNodeId);
				final Node toNode = network.getNodes().get(toNodeId);
				if (fromNode != null && toNode != null) {

					final Link link = NetworkUtils.getConnectingLink(fromNode, toNode);
					if (link != null) {
						// final Long regionId = Long.parseLong(record.get(REGION));
						final Double weight = Double.parseDouble(record.get(WEIGHT)) / 100.0;
						linkId2domesticWeightSum.compute(link.getId(), (l, s) -> s == null ? weight : s + weight);

					} else {
						missingLinkNodeIds.add(new Tuple<>(fromNodeId, toNodeId));
					}
				}
			}
		}

		log.info(linkId2domesticWeightSum.values().stream().filter(s -> s > 0.0).count()
				+ " links with positive weights.");
		log.warn(linkId2domesticWeightSum.values().stream().filter(s -> s > 1.001).count()
				+ " links with weights larger than one.");
		log.warn(missingLinkNodeIds.size() + " node pairs without links: " + missingLinkNodeIds);

		int i = 0;
		StringBuffer txt = new StringBuffer("Links with weights > 1:\n");
		for (Map.Entry<Id<Link>, Double> e : linkId2domesticWeightSum.entrySet()) {
			if (e.getValue() > 1.0) {
				txt.append(e + "\t");
				if (++i % 10 == 0) {
					txt.append("\n");
				}
			}
		}
		log.warn(txt);
		
		return linkId2domesticWeightSum;
	}

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED");

		Network network = new NetworkReader().load("./input_2024/node_parameters.csv",
				"./input_2024/link_parameters.csv");

		new LinkRegionsReader(network).read("./input_2024/link_regions.csv");

		System.out.println("DONE");

	}

}
