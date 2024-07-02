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
package se.vti.samgods.models.saana;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.network.NetworkRouter;
import se.vti.samgods.network.NetworkRoutingData;
import se.vti.samgods.network.SamgodsNetworkReader;
import se.vti.samgods.transportation.consolidation.EpisodeCostModel;
import se.vti.samgods.transportation.consolidation.FallbackEpisodeCostModel;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.consolidation.road.PerformanceMeasures;
import se.vti.samgods.transportation.fleet.SamgodsFleetReader;
import se.vti.samgods.transportation.fleet.VehicleFleet;
import se.vti.samgods.utils.CommodityModeGrouping;

/**
 * 
 * @author GunnarF
 *
 */
public class TestSamgods {

	static Logger log = Logger.getLogger(SaanaModelRunner.class);

	public static void main(String[] args) throws IOException {
		
		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.AGRICULTURE);
		double samplingRate = 1e-2;
		
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

		CommodityModeGrouping grouping = new CommodityModeGrouping();
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			grouping.addCartesian(null, Arrays.asList(mode));
		}

		EpisodeCostModel fallbackEpisodeCostModel = new FallbackEpisodeCostModel(fleet, consolidationCostModel,
				grouping);
		EpisodeCostModel empiricalEpisodeCostModel = null;

		// ROUTE CHAINS
		
		NetworkRoutingData routingData = new NetworkRoutingData(network, grouping, empiricalEpisodeCostModel,
				fallbackEpisodeCostModel);
		NetworkRouter router = new NetworkRouter(routingData).setLogProgress(false);
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			ChainChoiReader commodityReader = new ChainChoiReader(commodity).setStoreSamgodsShipments(false)
					.setSamplingRate(samplingRate, new Random(4711))
					.parse("./input_2024/ChainChoi" + commodity.twoDigitCode() + "XTD.out");
			for (List<TransportChain> chains : commodityReader.getOD2transportChains().values()) {
				for (TransportChain chain : chains) {
					for (TransportEpisode episode : chain.getEpisodes()) {
						for (TransportLeg leg : episode.getLegs()) {
							assert (network.getNodes().containsKey(leg.getOrigin()));
							assert (network.getNodes().containsKey(leg.getDestination()));
						}
					}
				}
			}

			router.route(commodity, commodityReader.getOD2transportChains());
		}

		// RUN LOGISTICS MODEL
		
		

		System.out.println("... DONE");
	}
}
