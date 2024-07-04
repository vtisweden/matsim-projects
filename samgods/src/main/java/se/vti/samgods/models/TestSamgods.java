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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.DetailedTransportCost;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.logistics.TransportDemand.AnnualShipment;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.choicemodel.Alternative;
import se.vti.samgods.logistics.choicemodel.AnnualShipmentUtilityFunction;
import se.vti.samgods.logistics.choicemodel.ChoiceModelUtils;
import se.vti.samgods.logistics.choicemodel.ChoiceSetGenerator;
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

	static Logger log = Logger.getLogger(TestSamgods.class);

	public static void main(String[] args) throws IOException {

//		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.TIMBER);
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

		TransportDemand transportDemand = new TransportDemand();

		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			new ChainChoiReader(commodity, transportDemand)
					.setSamplingRate(samplingRate, new Random(4711)).checkAgainstNetwork(network)
					.parse("./input_2024/ChainChoi" + commodity.twoDigitCode() + "XTD.out");
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
			Map<OD, Set<TransportChain>> od2chains = transportDemand.commodity2od2transportChains.get(commodity);
			NetworkRouter router = new NetworkRouter(routingData).setLogProgress(true).setMaxThreads(Integer.MAX_VALUE);
			router.route(commodity, od2chains);
		}
		
		// REMOVE UNROUTED CHAINS
		
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			long removedCnt = 0;
			long totalCnt = 0;
			for (Map.Entry<OD, Set<TransportChain>> entry : transportDemand.commodity2od2transportChains.get(commodity).entrySet()) {
				final int chainCnt = entry.getValue().size();
				totalCnt += chainCnt;
				entry.setValue(entry.getValue().stream().filter(c -> c.isRouted()).collect(Collectors.toSet()));
				removedCnt += chainCnt - entry.getValue().size();
			}
			log.warn(
					commodity + ": Removed " + removedCnt + " out of " + totalCnt + " chains with incomplete routes. ");
		}

		// RUN LOGISTICS MODEL

		AnnualShipmentUtilityFunction utilityFunction = new AnnualShipmentUtilityFunction() {
			@Override
			public double computeUtility(Commodity commodity, AnnualShipment annualShipment,
					DetailedTransportCost transportCost_1_ton) {
				return -transportCost_1_ton.getMonetaryCost() * annualShipment.getTotalAmount_ton();
			}
		};

		double scale = 0.0;

		ChoiceSetGenerator choiceSetGenerator = new ChoiceSetGenerator(empiricalEpisodeCostModel,
				fallbackEpisodeCostModel, utilityFunction);
		ChoiceModelUtils choiceModel = new ChoiceModelUtils();

		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			long cnt = 0;

			for (Map.Entry<OD, List<TransportDemand.AnnualShipment>> e : transportDemand.commodity2od2annualShipments
					.get(commodity).entrySet()) {
				final OD od = e.getKey();

				final Set<TransportChain> transportChains = transportDemand.commodity2od2transportChains.get(commodity)
						.get(od);
				if (transportChains.size() > 0) {

					// encapsulate >>>

					Map<TransportChain, DetailedTransportCost> transportChain2transportUnitCost_1_ton = new LinkedHashMap<>(
							transportChains.size());
					for (TransportChain transportChain : transportChains) {
						DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(1.0);
						for (TransportEpisode episode : transportChain.getEpisodes()) {
							DetailedTransportCost episodeCost = null;
							if (empiricalEpisodeCostModel != null) {
								episodeCost = empiricalEpisodeCostModel.computeCost_1_ton(episode);
							}
							if (episodeCost == null) {
								episodeCost = fallbackEpisodeCostModel.computeCost_1_ton(episode);
							}
							builder.addLoadingCost(episodeCost.loadingCost)
									.addLoadingDuration_h(episodeCost.loadingDuration_h)
									.addMoveCost(episodeCost.moveCost).addMoveDuration_h(episodeCost.moveDuration_h)
									.addTransferCost(episodeCost.transferCost)
									.addTransferDuration_h(episodeCost.transferDuration_h)
									.addUnloadingCost(episodeCost.unloadingCost)
									.addUnloadingDuration_h(episodeCost.unloadingDuration_h);
						}
						transportChain2transportUnitCost_1_ton.put(transportChain, builder.build());
					}

					// encapsuate <<<

					for (TransportDemand.AnnualShipment shipment : e.getValue()) {
						List<Alternative> alternatives = choiceSetGenerator.createChoiceSet(commodity, shipment,
								transportChain2transportUnitCost_1_ton);
						for (int instance = 0; instance < shipment.getNumberOfInstances(); instance++) {
							Alternative choice = choiceModel.choose(alternatives, scale);
							assert (choice != null);
							cnt++;
						}
					}
				}
			}
			log.info(commodity + ": Created " + cnt + " shipments.");
		}

		log.info("DONE");
	}
}
