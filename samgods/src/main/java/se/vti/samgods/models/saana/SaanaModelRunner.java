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
package se.vti.samgods.models.saana;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportPrices;
import se.vti.samgods.TransportPrices.LinkPrices;
import se.vti.samgods.TransportPrices.NodePrices;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.readers.ChainChoiReader;
import se.vti.samgods.readers.SamgodsNetworkReader;
import se.vti.samgods.transportation.TransportSupply;

/**
 * 
 * @author GunnarF
 *
 */
public class SaanaModelRunner {

	static Logger log = Logger.getLogger(SaanaModelRunner.class);

	public static void main(String[] args) {

		final double odSamplingRate = 0.01; // TODO below one only for testing

		log.info("STARTED ...");

		final List<SamgodsConstants.Commodity> consideredCommodities = Arrays
				.asList(SamgodsConstants.Commodity.values()).subList(0, 2);

		/*
		 * PREPARE DEMAND
		 */
		final TransportDemand demand = new TransportDemand();
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			log.info("Loading " + commodity.description);
			final ChainChoiReader commodityReader = new ChainChoiReader(
					"./2023-06-01_basecase/ChainChoi" + commodity.twoDigitCode() + "STD.out", commodity);
			demand.setPWCMatrix(commodity, commodityReader.getPWCMatrix());
			demand.setTransportChains(commodity, commodityReader.getOd2transportChains());
		}

		/*
		 * PREPARE SUPPLY
		 */

		final LinkPrices roadPrices = new LinkPrices() {
			@Override
			public double getPrice_1_ton(Link link) {
				return 2000.0 * getDuration_h(link);
			}
		};

		final LinkPrices railPrices = new LinkPrices() {
			@Override
			public double getPrice_1_ton(Link link) {
				return 1000.0 * getDuration_h(link);
			}
		};

		final LinkPrices seaPrices = new LinkPrices() {
			@Override
			public double getPrice_1_ton(Link link) {
				return 500.0 * getDuration_h(link);
			}
		};

		final LinkPrices airPrices = new LinkPrices() {
			@Override
			public double getPrice_1_ton(Link link) {
				return 10000.0 * getDuration_h(link);
			}
		};

		final NodePrices nodePrices = new NodePrices() {
			@Override
			public double getPrice_1_ton(Node nodeId, TransportMode fromNode, TransportMode toMode) {
				return 0.01;
			}
		};

		final TransportPrices transportPrices = new TransportPrices();
		for (Commodity commodity : consideredCommodities) {
			transportPrices.setLinkPrices(commodity, TransportMode.Road, roadPrices);
			transportPrices.setLinkPrices(commodity, TransportMode.Rail, railPrices);
			transportPrices.setLinkPrices(commodity, TransportMode.Sea, seaPrices);
			transportPrices.setLinkPrices(commodity, TransportMode.Air, airPrices);
		}

		final SamgodsNetworkReader networkReader = new SamgodsNetworkReader("./2023-06-01_basecase/node_table.csv",
				"./2023-06-01_basecase/link_table.csv");

		final TransportSupply supply = new TransportSupply(networkReader.getNetwork(), null, transportPrices);

		/*
		 * PREPARE DEMAND/SUPPLY INTERACTIONS
		 */
		Random rnd = new Random();
		for (Commodity commodity : consideredCommodities) {

			// Only for testing: reduce the number of OD pairs to be processed.
			final Map<OD, List<TransportChain>> od2chains = demand.getTransportChains(commodity).entrySet().stream()
					.filter(e -> rnd.nextDouble() < odSamplingRate)
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

			log.info("Routing " + commodity.description + "...");
			supply.route(commodity, od2chains);
			log.info("Simplifying " + commodity.description + "...");
			log.info("  number of chains before: " + od2chains.values().stream().flatMap(l -> l.stream()).count());
			(new SaanaTransportChainReducer()).reduce(od2chains);
			log.info("  number of chains after: " + od2chains.values().stream().flatMap(l -> l.stream()).count());
			demand.setTransportChains(commodity, od2chains);
		}

		log.info("... DONE");
	}
}
