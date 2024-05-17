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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.RecurrentShipment;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.logistics.choicemodel.Alternative;
import se.vti.samgods.logistics.choicemodel.ChoiceModelUtils;
import se.vti.samgods.logistics.choicemodel.ChoiceSetGenerator;
import se.vti.samgods.logistics.choicemodel.ShipmentUtilityFunction;
import se.vti.samgods.network.SamgodsNetworkReader;
import se.vti.samgods.readers.SamgodsPriceReader;
import se.vti.samgods.transportation.NetworkRouter;
import se.vti.samgods.transportation.TransportSupply;
import se.vti.samgods.transportation.pricing.BasicShipmentCost;
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

	public static void main(String[] args) throws IOException {

		log.info("STARTED ...");

		/*
		 * For testing: Consider only subsets of all OD relations and commodity types.
		 */
		double odSamplingRate = 0.01; // one percent of all shipment relations
		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.values())
				.subList(0, 15); // omit AIR

		/*
		 * Load transport demand from "ChainChoi" files into a demand container object
		 * that encapsulates PWC matrices and transport chains.
		 */
		TransportDemand demand = new TransportDemand();
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			log.info("Loading " + commodity.description);
			ChainChoiReader commodityReader = new ChainChoiReader(
					"./2023-06-01_basecase/ChainChoi" + commodity.twoDigitCode() + "STD.out", commodity);
			demand.setPWCMatrix(commodity, commodityReader.getPWCMatrix());
			demand.setTransportChains(commodity, commodityReader.getOd2transportChains());
		}

		/*
		 * Processing of logistic transport chains. Here: Take out all air
		 * transportation and merge detailed (sub-mode) based transport legs into
		 * coarser, main-mode based episodes.
		 */
		for (Commodity commodity : consideredCommodities) {
			if (!Commodity.AIR.equals(commodity)) {
//				TransportChainUtils.removeChainsByLegCondition(demand.getTransportChains(commodity).values(),
//						l -> TransportMode.Air.equals(l.getMode()));
			}
//			TransportChainUtils.reduceToMainModeLegs(demand.getTransportChains(commodity).values());
		}

		/*
		 * Load transport supply: network and (yet to be specified, not needed for
		 * logistic choices) vehicle fleet. Put everything into a supply container.
		 */
		SamgodsNetworkReader networkReader = new SamgodsNetworkReader("./2023-06-01_basecase/node_table.csv",
				"./2023-06-01_basecase/link_table.csv");
		TransportSupply supply = new TransportSupply(networkReader.getNetwork(), null);

		/*
		 * Load transport prices. These prices will later come out of downstream
		 * consolidation and vehicle flow models. For standalone testing, we read fixed
		 * prices from a file.
		 */
		SamgodsPriceReader priceReader = new SamgodsPriceReader(networkReader.getNetwork(),
				"./2023-06-01_basecase/LinkPrices.csv", "./2023-06-01_basecase/NodePrices.csv",
				"./2023-06-01_basecase/NodeTimes.csv");
		TransportPrices<ProportionalShipmentPrices, ProportionalTransshipmentPrices> transportPrices = priceReader
				.getTransportPrices();

		/*
		 * Compute routes for all network transport episodes and attach them to the
		 * corresponding transport chains.
		 */
		for (Commodity commodity : consideredCommodities) {
			demand.downsample(commodity, odSamplingRate); // for testing only
			NetworkRouter router = new NetworkRouter(supply.getNetwork(), transportPrices);
			router.route(commodity, demand.getTransportChains(commodity));
		}

		/*
		 * Compose a logistics choice model (of transport chain and shipment size).
		 * Modular and decoupled from downstream consolidation and transport models. The
		 * resulting choice model depends on what modules are composed, below is an
		 * example.
		 */
		// (Monetary) shipment cost, given its characteristics and transport prices.

		// SamgodsShipmentCostFunction costFunction = new
		// SamgodsShipmentCostFunction(transportPrices);

		// (Choice model) shipment utility, given its properties and (monetary) cost.
		ShipmentUtilityFunction<BasicShipmentCost> utilityFunction = new ShipmentUtilityFunction<>() {
			public double computeUtility(RecurrentShipment shipment, BasicShipmentCost shipmentCost) {
				return -shipmentCost.getMonetaryCost() * shipment.getFrequency_1_yr(); // for testing
			}
		};
		// Create choice sets by combining transport chains and shipment sizes.
		ChoiceSetGenerator<BasicShipmentCost> choiceSetGenerator = new ChoiceSetGenerator<>(null, utilityFunction,
				SaanaShipmentSizeClass.values());
		// Basic choice model functionality
		ChoiceModelUtils choiceModel = new ChoiceModelUtils();

		/*
		 * Using the functionality specified above, simulate concrete logistic choices.
		 */
		for (Commodity commodity : consideredCommodities) {
			for (OD od : demand.getTransportChains(commodity).keySet()) {
				double amount_ton_yr = demand.getPWCMatrix(commodity).getOd2Amount_ton_yr().get(od);
				List<TransportChain> chains = demand.getTransportChains(commodity, od);
				List<Alternative<BasicShipmentCost>> alternatives = choiceSetGenerator.createChoiceSet(chains,
						amount_ton_yr, commodity);
				Alternative<BasicShipmentCost> choice = choiceModel.choose(alternatives, utilityFunction);
				System.out.println(choice);
			}
		}

		log.info("... DONE");
	}
}
