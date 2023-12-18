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

import java.util.ArrayList;
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
import org.matsim.core.gbl.MatsimRandom;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportChainUtils;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.logistics.choicemodel.Alternative;
import se.vti.samgods.logistics.choicemodel.ChoiceModelUtils;
import se.vti.samgods.logistics.choicemodel.ChoiceSetGenerator;
import se.vti.samgods.readers.ChainChoiReader;
import se.vti.samgods.readers.SamgodsNetworkReader;
import se.vti.samgods.readers.SamgodsPriceReader;
import se.vti.samgods.transportation.NetworkRouter;
import se.vti.samgods.transportation.TransportSupply;
import se.vti.samgods.transportation.pricing.BasicShipmentCost;
import se.vti.samgods.transportation.pricing.BasicShipmentCostFunction;
import se.vti.samgods.transportation.pricing.ProportionalShipmentPrices;
import se.vti.samgods.transportation.pricing.ProportionalTransshipmentPrices;
import se.vti.samgods.transportation.pricing.TransportPrices;

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
				.asList(SamgodsConstants.Commodity.values()).subList(0, 1);

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

		for (Commodity commodity : consideredCommodities) {
			if (!Commodity.AIR.equals(commodity)) {
				TransportChainUtils.removeChainsWithMode(demand.getTransportChains(commodity).values(),
						TransportMode.Air);
			}
//			TransportChainUtils.reduceToMainModeLegs(demand.getTransportChains(commodity).values());
		}

		/*
		 * PREPARE SUPPLY
		 */

		final SamgodsNetworkReader networkReader = new SamgodsNetworkReader("./2023-06-01_basecase/node_table.csv",
				"./2023-06-01_basecase/link_table.csv");

		final SamgodsPriceReader priceReader = new SamgodsPriceReader(networkReader.getNetwork(),
				"./2023-06-01_basecase/LinkPrices.csv", "./2023-06-01_basecase/NodePrices.csv",
				"./2023-06-01_basecase/NodeTimes.csv");
		final TransportPrices<ProportionalShipmentPrices, ProportionalTransshipmentPrices> transportPrices = priceReader
				.getTransportPrices();

		final TransportSupply supply = new TransportSupply(networkReader.getNetwork(), null, transportPrices);

		/*
		 * PREPARE DEMAND/SUPPLY INTERACTIONS
		 */

//		List<Commodity> commodities = new LinkedList<>();
//		List<Long> chainsBefore = new LinkedList<>();
//		List<Long> chainsAfter = new LinkedList<>();
		List<AtomicLong> legCounts = new LinkedList<>();
		List<AtomicLong> linkCounts = new LinkedList<>();
		List<Map<TransportMode, Set<Id<Node>>>> mode2routingErrors = new LinkedList<>();

		Random rnd = new Random();

		for (Commodity commodity : consideredCommodities) {

			// >>>>> Only for testing: reduce the number of OD pairs to be processed.
			final Map<OD, List<TransportChain>> downsampledOd2chains = demand.getTransportChains(commodity).entrySet().stream()
					.filter(e -> rnd.nextDouble() < odSamplingRate)
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
			demand.getTransportChains(commodity).clear();
			demand.getTransportChains(commodity).putAll(downsampledOd2chains);
			// <<<<< Only for testing: reduce the number of OD pairs to be processed.

			log.info("Routing " + commodity.description + ", which uses the following modes: "
					+ TransportChainUtils.extractUsedModes(demand.getTransportChains(commodity).values()));

			NetworkRouter router = new NetworkRouter(supply.getNetwork(), supply.getTransportPrices());
			router.route(commodity, demand.getTransportChains(commodity));

//			commodities.add(commodity);
			linkCounts.add(router.routedLinkCnt);
			legCounts.add(router.routedLegCnt);
			mode2routingErrors.add(router.mode2LegRoutingFailures);

//			chainsBefore.add(od2chains.values().stream().flatMap(l -> l.stream()).count());
//			(new SaanaTransportChainReducer()).reduce(od2chains);
//			chainsAfter.add(od2chains.values().stream().flatMap(l -> l.stream()).count());
//			demand.setTransportChains(commodity, od2chains);
		}

		System.out.println();
		for (int i = 0; i < consideredCommodities.size(); i++) {
			System.out.println(consideredCommodities.get(i));
//			System.out.println("  chains before: " + chainsBefore.get(i));
//			System.out.println("  chains after: " + chainsAfter.get(i));
			System.out.println("  found legs: " + legCounts.get(i));
			System.out
					.println("  found links per leg: " + linkCounts.get(i).longValue() / legCounts.get(i).longValue());
			for (Map.Entry<TransportMode, Set<Id<Node>>> entry : mode2routingErrors.get(i).entrySet()) {
				System.out.println("  failures with mode " + entry.getKey() + " at nodes: " + entry.getValue());
			}
		}

		// >>> REMOVE TransportChains without routing information

		ArrayList<TransportChain> removedChains = new ArrayList<>();
		for (Commodity commodity : consideredCommodities) {
			System.out.println(commodity);
			removedChains.addAll(TransportChainUtils.removeChainsByLegCondition(
					demand.getTransportChains(commodity).values(), l -> !l.getOrigin().equals(l.getDestination())
							&& (l.getRouteView() == null || l.getRouteView().size() == 0)));
		}

		// >>> CHOICE

		BasicShipmentCostFunction costFunction = new BasicShipmentCostFunction(supply.getTransportPrices());
		SaanaUtilityFunction utilityFunction = new SaanaUtilityFunction();
		ChoiceSetGenerator<BasicShipmentCost> choiceSetGenerator = new ChoiceSetGenerator<>(costFunction,
				utilityFunction, SaanaShipmentSizeClass.values());
		ChoiceModelUtils choiceModel = new ChoiceModelUtils(MatsimRandom.getRandom());
		for (Commodity commodity : consideredCommodities) {
			double lostDemand_ton_yr = 0;
//			for (Map.Entry<OD, Double> e : demand.getPWCMatrix(commodity).getOd2AmountView().entrySet()) {
			for (Map.Entry<OD, List<TransportChain>> e : demand.getTransportChains(commodity).entrySet()) {
				OD od = e.getKey();
				double amount_ton_yr = demand.getPWCMatrix(commodity).getOd2AmountView().get(od);
				List<TransportChain> chains = demand.getTransportChains(commodity, od);
				if (chains != null && chains.size() > 0) {
					List<Alternative<BasicShipmentCost>> alternatives = choiceSetGenerator.createChoiceSet(chains,
							amount_ton_yr, commodity);
					Alternative<BasicShipmentCost> choice = choiceModel.choose(alternatives, utilityFunction);
					System.out.println(choice);
				} else {
					System.out.println("No transport chains for commodity " + commodity + " in OD pair " + od + ", "
							+ amount_ton_yr + " not assigned.");
					lostDemand_ton_yr += amount_ton_yr;
				}

			}
			System.out.println("Total demand = " + demand.getPWCMatrix(commodity).computeTotal_ton_yr());
			System.out.println("Lost demand = " + lostDemand_ton_yr);
		}

		// <<< CHOICE

		log.info("... DONE");
	}
}
