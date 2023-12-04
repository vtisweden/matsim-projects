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
import java.util.LinkedList;
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
import se.vti.samgods.transportation.NetworkRouter;
import se.vti.samgods.transportation.TransportSupply;

/**
 * 
 * @author GunnarF
 *
 */
public class SaanaModelRunner {

	static Logger log = Logger.getLogger(SaanaModelRunner.class);

	static class ProportionalLinkPrices implements LinkPrices {

		private final double price_1_tonM;

		ProportionalLinkPrices(double price_1_tonKm) {
			this.price_1_tonM = 0.001 * price_1_tonKm;
		}

		@Override
		public double getPrice_1_ton(Link link) {
			return 1e-8 + this.price_1_tonM * link.getLength();
		}

		@Override
		public LinkPrices deepCopy() {
			return new ProportionalLinkPrices(1000.0 * this.price_1_tonM);
		}

	}

	static class NoTransshipmentPrices implements NodePrices {

		@Override
		public double getPrice_1_ton(Node node, TransportMode fromMode, TransportMode toMode) {
			return 0;
		}

		@Override
		public NodePrices deepCopy() {
			return new NoTransshipmentPrices();
		}

	}

	public static void main(String[] args) {

		final double odSamplingRate = 1.0; // TODO below one only for testing

		log.info("STARTED ...");

		final List<SamgodsConstants.Commodity> consideredCommodities = Arrays
				.asList(SamgodsConstants.Commodity.values()); // .subList(0, 1);

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

//		for (Commodity commodity : consideredCommodities) {
//			PWCMatrix matrix = demand.getPWCMatrix(commodity);
//			System.out.println(commodity.description);
//			int relations = matrix.getRelationsCnt();
//			int nodes2 = matrix.getLocationsView().size() *matrix.getLocationsView().size();
//			System.out.println("  " + relations + " nonzero OD relations");
//			System.out.println("  " + nodes2 + " possible OD relations");
//			System.out.println("  density = " + ((double) relations) / nodes2);
//		}
//		System.out.println("exiting");
//		System.exit(0);

		/*
		 * PREPARE SUPPLY
		 */

		final LinkPrices roadPrices = new ProportionalLinkPrices(2000.0);
		final LinkPrices railPrices = new ProportionalLinkPrices(1000.0);
		final LinkPrices seaPrices = new ProportionalLinkPrices(500.0);
		final LinkPrices airPrices = new ProportionalLinkPrices(10000.0);
		final NodePrices transshipmentPrices = new NoTransshipmentPrices();

		final TransportPrices transportPrices = new TransportPrices();
		for (Commodity commodity : consideredCommodities) {
			transportPrices.setLinkPrices(commodity, TransportMode.Road, roadPrices);
			transportPrices.setLinkPrices(commodity, TransportMode.Rail, railPrices);
			transportPrices.setLinkPrices(commodity, TransportMode.Sea, seaPrices);
			transportPrices.setLinkPrices(commodity, TransportMode.Air, airPrices);
			transportPrices.setNodePrices(commodity, transshipmentPrices);
		}

		final SamgodsNetworkReader networkReader = new SamgodsNetworkReader("./2023-06-01_basecase/node_table.csv",
				"./2023-06-01_basecase/link_table.csv");

		final TransportSupply supply = new TransportSupply(networkReader.getNetwork(), null, transportPrices);

		/*
		 * PREPARE DEMAND/SUPPLY INTERACTIONS
		 */
		
		List<Commodity> commodities = new LinkedList<>();
		List<Long> chainsBefore = new LinkedList<>();
		List<Long> chainsAfter = new LinkedList<>();
		List<Long> failures = new LinkedList<>();
		List<Long> successes = new LinkedList<>();
		List<Long> linkCounts = new LinkedList<>();
		
		Random rnd = new Random();
		for (Commodity commodity : consideredCommodities) {

			// Only for testing: reduce the number of OD pairs to be processed.
			final Map<OD, List<TransportChain>> od2chains = demand.getTransportChains(commodity).entrySet().stream()
					.filter(e -> rnd.nextDouble() < odSamplingRate)
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

			log.info("Routing " + commodity.description + "...");
			NetworkRouter router = new NetworkRouter(supply.getNetwork(), supply.getTransportPrice());
			router.route(commodity, od2chains);

			commodities.add(commodity);
			failures.add(router.getFailures());
			successes.add(router.getSuccesses());
			linkCounts.add(router.getLinkCnt());
			

			chainsBefore.add(od2chains.values().stream().flatMap(l -> l.stream()).count());
			(new SaanaTransportChainReducer()).reduce(od2chains);
			chainsAfter.add(od2chains.values().stream().flatMap(l -> l.stream()).count());
			demand.setTransportChains(commodity, od2chains);
		}

		System.out.println();
		for (int i = 0; i < commodities.size(); i++) {
			System.out.println(commodities.get(i));
			System.out.println("  chains before: " + chainsBefore.get(i));
			System.out.println("  chains after:  " + chainsAfter.get(i));
			System.out.println("  failures:    " + failures.get(i));
			System.out.println("  successes:   " + successes.get(i));
			System.out.println("  found links: " + linkCounts.get(i));
		}
		
		log.info("... DONE");
	}
}
