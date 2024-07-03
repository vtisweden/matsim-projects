/**
 * se.vti.samgods.models.saana
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
package se.vti.samgods.models;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.DetailedTransportCost;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.ShipmentSizeClass;
import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.PWCMatrix;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.choicemodel.Alternative;
import se.vti.samgods.logistics.choicemodel.AnnualShipmentUtilityFunction;
import se.vti.samgods.logistics.choicemodel.ChoiceModelUtils;
import se.vti.samgods.logistics.choicemodel.ChoiceSetGenerator;
import se.vti.samgods.models.saana.SaanaModelRunner;
import se.vti.samgods.network.NetworkRouter;
import se.vti.samgods.network.NetworkRoutingData;
import se.vti.samgods.network.SamgodsNetworkReader;
import se.vti.samgods.transportation.consolidation.EpisodeCostModel;
import se.vti.samgods.transportation.consolidation.FallbackEpisodeCostModel;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.consolidation.road.PerformanceMeasures;
import se.vti.samgods.transportation.fleet.SamgodsFleetReader;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class TestSamgods {

	static Logger log = Logger.getLogger(SaanaModelRunner.class);

	public static void main(String[] args) throws IOException {

//		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.METAL,
//				SamgodsConstants.Commodity.OTHERMINERAL);
		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.values());
		double samplingRate = 1.0;

		log.info("STARTED ...");

		VehicleFleet fleet = new VehicleFleet();
		SamgodsFleetReader fleetReader = new SamgodsFleetReader(fleet);
		fleetReader.load_v12("./input_2024/vehicleparameters_air.csv", "./input_2024/transferparameters_air.csv",
				SamgodsConstants.TransportMode.Air);
		fleetReader.load_v12("./input_2024/vehicleparameters_rail.csv", "./input_2024/transferparameters_rail.csv",
				SamgodsConstants.TransportMode.Rail);
		fleetReader.load_v12("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
				SamgodsConstants.TransportMode.Road);
		fleetReader.load_v12("./input_2024/vehicleparameters_sea.csv", "./input_2024/transferparameters_sea.csv",
				SamgodsConstants.TransportMode.Sea);

		Network network = SamgodsNetworkReader.load("./input_2024/node_parameters.csv",
				"./input_2024/link_parameters.csv");

		// LOAD DEMAND

		Map<SamgodsConstants.Commodity, PWCMatrix> commodity2pwc = new LinkedHashMap<>();
		Map<SamgodsConstants.Commodity, Map<OD, List<TransportChain>>> commodity2OD2chains = new LinkedHashMap<>();

		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			ChainChoiReader commodityReader = new ChainChoiReader(commodity).setStoreSamgodsShipments(false)
					.setSamplingRate(samplingRate, new Random(4711))
					.parse("./input_2024/ChainChoi" + commodity.twoDigitCode() + "XTD.out");
			commodity2pwc.put(commodity, commodityReader.getPWCMatrix());
			commodity2OD2chains.put(commodity, commodityReader.getOD2transportChains());
		}

		// CREATE COST CONTAINERS

		PerformanceMeasures performanceMeasures = new PerformanceMeasures() {
			@Override
			public double getTotalArrivalDelay_h(Id<Node> nodeId) {
				return 0;
			}

			@Override
			public double getTotalDepartureDelay_h(Id<Node> nodeId) {
				return 0;
			}
		};

		ConsolidationCostModel consolidationCostModel = new ConsolidationCostModel(performanceMeasures, network);
		EpisodeCostModel fallbackEpisodeCostModel = new FallbackEpisodeCostModel(fleet, consolidationCostModel);
		EpisodeCostModel empiricalEpisodeCostModel = null;

		// ROUTE CHAINS

		NetworkRoutingData routingData = new NetworkRoutingData(network, empiricalEpisodeCostModel,
				fallbackEpisodeCostModel, fleet);
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			Map<OD, List<TransportChain>> od2chains = commodity2OD2chains.get(commodity);
			NetworkRouter router = new NetworkRouter(routingData).setLogProgress(false);
			router.route(commodity, od2chains);

			for (Map.Entry<OD, List<TransportChain>> od2failed : router.getOD2failedChains().entrySet()) {
				log.warn("Removing " + od2failed.getValue().size() + " chains with incomplete routes from OD "
						+ od2failed.getKey());
				od2chains.get(od2failed.getKey()).removeAll(od2failed.getValue());
			}
		}

		// RUN LOGISTICS MODEL

		AnnualShipmentUtilityFunction utilityFunction = new AnnualShipmentUtilityFunction() {
			@Override
			public double computeUtility(ShipmentSizeClass realizedShipmentSizeClass, AnnualShipment annualShipment,
					DetailedTransportCost transportCost_1_ton) {
				// TODO include storage cost
				return -transportCost_1_ton.getMonetaryCost() * annualShipment.getTotalAmount_ton();
			}
		};

		double scale = 0.0;

		ChoiceSetGenerator choiceSetGenerator = new ChoiceSetGenerator(empiricalEpisodeCostModel,
				fallbackEpisodeCostModel, utilityFunction);
		ChoiceModelUtils choiceModel = new ChoiceModelUtils();

		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			PWCMatrix pwc = commodity2pwc.get(commodity);
			Map<OD, List<TransportChain>> od2chains = commodity2OD2chains.get(commodity);
			for (OD od : pwc.getOd2Amount_ton_yr().keySet()) {
				double amount_ton_yr = pwc.getOd2Amount_ton_yr().get(od);
				List<TransportChain> chains = od2chains.get(od);
				if (chains.size() == 0) {
					log.warn("No feasible transport chains in OD pair " + od + ", loosing + " + amount_ton_yr + " of " + commodity + ".");
				} else {
					List<Alternative> alternatives = choiceSetGenerator.createChoiceSet(chains, amount_ton_yr,
							commodity);
					Alternative choice = choiceModel.choose(alternatives, scale);
					System.out.println(choice);
				}
			}
		}

		System.out.println("... DONE");
	}
}
