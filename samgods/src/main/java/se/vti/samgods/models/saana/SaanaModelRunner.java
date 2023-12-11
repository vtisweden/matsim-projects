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
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportPrices;
import se.vti.samgods.TransportPrices.ShipmentPrices;
import se.vti.samgods.TransportPrices.TransshipmentPrices;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportChainUtils;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.readers.ChainChoiReader;
import se.vti.samgods.readers.SamgodsNetworkReader;
import se.vti.samgods.transportation.NetworkRouter;
import se.vti.samgods.transportation.TransportSupply;
import se.vti.samgods.transportation.pricing.NoTransshipmentPrices;
import se.vti.samgods.transportation.pricing.ProportionalShipmentPrices;
import se.vti.samgods.transportation.pricing.ProportionalTransshipmentPrices;

/**
 * 
 * @author GunnarF
 *
 */
public class SaanaModelRunner {

	static Logger log = Logger.getLogger(SaanaModelRunner.class);

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

		for (Commodity commodity : consideredCommodities) {
			TransportChainUtils.reduceToMainModeLegs(demand.getTransportChains(commodity).values());
		}

		/*
		 * PREPARE SUPPLY
		 */

		final TransportPrices transportPrices = new TransportPrices();

		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.AGRICULTURE, TransportMode.Rail, 0.103043995));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.AGRICULTURE, TransportMode.Road, 0.451447135));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.AGRICULTURE, TransportMode.Sea, 0.101688729));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.COAL, TransportMode.Rail, 0.066692183));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.COAL, TransportMode.Road, 0.342202995));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.COAL, TransportMode.Sea, 0.033313577));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.METAL, TransportMode.Rail, 0.105756004));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.METAL, TransportMode.Road, 0.365603092));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.METAL, TransportMode.Sea, 0.037603618));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.FOOD, TransportMode.Rail, 0.123022168));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.FOOD, TransportMode.Road, 0.441055184));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.FOOD, TransportMode.Sea, 0.243970312));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TEXTILES, TransportMode.Rail, 0.13886835));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TEXTILES, TransportMode.Road, 0.489421154));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TEXTILES, TransportMode.Sea, 1.629053206));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.WOOD, TransportMode.Rail, 0.094168683));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.WOOD, TransportMode.Road, 0.361584081));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.WOOD, TransportMode.Sea, 0.14409457));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.COKE, TransportMode.Rail, 0.084174732));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.COKE, TransportMode.Road, 0.376315634));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.COKE, TransportMode.Sea, 0.030321232));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.CHEMICALS, TransportMode.Rail, 0.102427456));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.CHEMICALS, TransportMode.Road, 0.423346666));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.CHEMICALS, TransportMode.Sea, 0.108915908));

		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.OTHERMINERAL, TransportMode.Rail, 0.0847216));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.OTHERMINERAL, TransportMode.Road, 0.398305644));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.OTHERMINERAL, TransportMode.Sea, 0.060082237));

		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.BASICMETALS, TransportMode.Rail, 0.092282118));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.BASICMETALS, TransportMode.Road, 0.417577884));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.BASICMETALS, TransportMode.Sea, 0.125784546));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.MACHINERY, TransportMode.Rail, 0.119529765));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.MACHINERY, TransportMode.Road, 0.460460801));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.MACHINERY, TransportMode.Sea, 0.207422961));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TRANSPORT, TransportMode.Rail, 0.100139089));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TRANSPORT, TransportMode.Road, 0.423233368));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TRANSPORT, TransportMode.Sea, 0.101153576));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.FURNITURE, TransportMode.Rail, 0.096255791));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.FURNITURE, TransportMode.Road, 0.434177873));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.FURNITURE, TransportMode.Sea, 0.14054423));

		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.SECONDARYRAW, TransportMode.Rail, 0.080184686));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.SECONDARYRAW, TransportMode.Road, 0.387911162));
		transportPrices
				.addShipmentPrices(new ProportionalShipmentPrices(Commodity.SECONDARYRAW, TransportMode.Sea, 0.05684892));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TIMBER, TransportMode.Rail, 0.073707607));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TIMBER, TransportMode.Road, 0.307112697));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.TIMBER, TransportMode.Sea, 0.059324767));

		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.AIR, TransportMode.Air, 6.652620813));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.AIR, TransportMode.Road, 0.898005173));
		transportPrices.addShipmentPrices(new ProportionalShipmentPrices(Commodity.AIR, TransportMode.Sea, 993.5655383));

		// FIXME TIMES IN MINUTES !!!!
		ProportionalTransshipmentPrices nodePrices = new ProportionalTransshipmentPrices(Commodity.COAL);
//		nodePrices.set(TransportMode.Rail, TransportMode.Rail, 21.9720028, 4.962516704);
//		nodePrices.set(TransportMode.Road, TransportMode.Rail, 13.99198734, 4.282372283);
//		nodePrices.set(TransportMode.Sea, TransportMode.Rail, 34.35648861, 9.44283071);
//		nodePrices.set(TransportMode.Rail, TransportMode.Road, 38.26794775, 5.115913589);
//		nodePrices.set(TransportMode.Road, TransportMode.Road, 2.127610092, 0.045739536);
//		nodePrices.set(TransportMode.Sea, TransportMode.Road, 45.28332963, 24.5534098);
//		nodePrices.set(TransportMode.Rail, TransportMode.Sea, 0.939440475, 1.791551143);
//		nodePrices.set(TransportMode.Road, TransportMode.Sea, 6.920376719, 10.3263437);
//		nodePrices.set(TransportMode.Sea, TransportMode.Sea, 81.10656926, 142.0556775);
		transportPrices.addTransshipmentPrices(nodePrices);

		for (Commodity commodity : consideredCommodities) {

			final ShipmentPrices roadPrices = new ProportionalShipmentPrices(commodity, TransportMode.Road, 2000.0);
			final ShipmentPrices railPrices = new ProportionalShipmentPrices(commodity, TransportMode.Rail, 1000.0);
			final ShipmentPrices seaPrices = new ProportionalShipmentPrices(commodity, TransportMode.Sea, 500.0);
			final ShipmentPrices airPrices = new ProportionalShipmentPrices(commodity, TransportMode.Air, 10000.0);
			final TransshipmentPrices transshipmentPrices = new NoTransshipmentPrices(commodity);

			transportPrices.addShipmentPrices(roadPrices);
			transportPrices.addShipmentPrices(railPrices);
			transportPrices.addShipmentPrices(seaPrices);
			transportPrices.addShipmentPrices(airPrices);
			transportPrices.addTransshipmentPrices(transshipmentPrices);
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
		List<AtomicLong> legCounts = new LinkedList<>();
		List<AtomicLong> linkCounts = new LinkedList<>();
		List<Map<TransportMode, Set<Id<Node>>>> mode2routingErrors = new LinkedList<>();

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
			linkCounts.add(router.routedLinkCnt);
			legCounts.add(router.routedLegCnt);
			mode2routingErrors.add(router.mode2LegRoutingFailures);

			chainsBefore.add(od2chains.values().stream().flatMap(l -> l.stream()).count());
			(new SaanaTransportChainReducer()).reduce(od2chains);
			chainsAfter.add(od2chains.values().stream().flatMap(l -> l.stream()).count());
			demand.setTransportChains(commodity, od2chains);
		}

		System.out.println();
		for (int i = 0; i < commodities.size(); i++) {
			System.out.println(commodities.get(i));
			System.out.println("  chains before: " + chainsBefore.get(i));
			System.out.println("  chains after: " + chainsAfter.get(i));
			System.out.println("  found legs: " + legCounts.get(i));
			System.out
					.println("  found links per leg: " + linkCounts.get(i).longValue() / legCounts.get(i).longValue());
			for (Map.Entry<TransportMode, Set<Id<Node>>> entry : mode2routingErrors.get(i).entrySet()) {
				System.out.println("  failures with mode " + entry.getKey() + " at nodes: " + entry.getValue());
			}
		}

		log.info("... DONE");
	}
}
