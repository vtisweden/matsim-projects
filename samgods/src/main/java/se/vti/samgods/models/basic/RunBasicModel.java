/**
 * org.matsim.contrib.emulation
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
package se.vti.samgods.models.basic;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.legacy.Samgods;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.UnimodalNetworkRouter;
import se.vti.samgods.transportation.VehicleFleet;

public class RunBasicModel {

	static final Logger log = Logger.getLogger(RunBasicModel.class);

	public static void main(String[] args) {

		log.info("STARTED ...");

		final List<Samgods.Commodity> consideredCommodities = Arrays.asList(Samgods.Commodity.values()).subList(0, 2);

		/*
		 * Load a samgods scenario.
		 */
		final Samgods samgods = new Samgods(null, null);
		samgods.loadNetwork("./2023-06-01_basecase/node_table.csv", "./2023-06-01_basecase/link_table.csv");
		for (Samgods.Commodity commodity : consideredCommodities) {
			log.info(commodity.description);
			samgods.loadChainChoiceFile("./2023-06-01_basecase/ChainChoi" + commodity.twoDigitCode() + "STD.out",
					commodity);
		}

		/*
		 * Define one representative vehicle type per transport mode.
		 */
		final VehicleFleet fleet = new VehicleFleet();
		fleet.createVehicleType("truck", Samgods.TransportMode.Road, 20.0, 80.0).setFixedCost_1_h(300.0)
				.setFixedCost_1_km(50.0).setUnitCost_1_hTon(0.0).setUnitCost_1_kmTon(200.0);
		fleet.createVehicleType("train", Samgods.TransportMode.Rail, 200.0, 120.0).setFixedCost_1_h(3000.0)
				.setFixedCost_1_km(500.0).setUnitCost_1_hTon(0.0).setUnitCost_1_kmTon(2000.0);
		fleet.createVehicleType("ship", Samgods.TransportMode.Sea, 2000.0, 30.0).setFixedCost_1_h(30000.0)
				.setFixedCost_1_km(5000.0).setUnitCost_1_hTon(0.0).setUnitCost_1_kmTon(20000.0);
		fleet.createVehicleType("plane", Samgods.TransportMode.Air, 200.0, 800.0).setFixedCost_1_h(30000.0)
				.setFixedCost_1_km(5000.0).setUnitCost_1_hTon(0.0).setUnitCost_1_kmTon(20000.0);

		/*
		 * Route all unimodal chain segments using representative vehicles.
		 */
		final Map<Samgods.TransportMode, UnimodalNetworkRouter> mode2router = new LinkedHashMap<>(4);
//		mode2router.put(TransportMode.Road, new UnimodalNetworkRouter(samgods.getNetwork(TransportMode.Road),
//				fleet.createEmptyVehicleTravelDisutility("truck")));
//		mode2router.put(TransportMode.Rail, new UnimodalNetworkRouter(samgods.getNetwork(TransportMode.Rail),
//				fleet.createEmptyVehicleTravelDisutility("train")));
//		mode2router.put(TransportMode.Sea, new UnimodalNetworkRouter(samgods.getNetwork(TransportMode.Sea),
//				fleet.createEmptyVehicleTravelDisutility("ship")));
//		mode2router.put(TransportMode.Air, new UnimodalNetworkRouter(samgods.getNetwork(TransportMode.Air),
//				fleet.createEmptyVehicleTravelDisutility("plane")));

		long ok = 0;
		long fail = 0;
		for (Samgods.Commodity commodity : consideredCommodities) {
			log.info("Routing commodity " + commodity);
			for (List<TransportChain> chains : samgods.getTransportDemand().getTransportChains(commodity).values()) {
				log.info("ok=" + ok + ", fail=" + fail);
				for (TransportChain chain : chains) {
					for (TransportLeg leg : chain.getLegs()) {
						final List<Node> route = mode2router.get(leg.getMode()).route(leg.getOrigin(),
								leg.getDestination());
						if (route == null) {
							fail++;
						} else {
							ok++;
						}
						leg.setRoute(route);
					}
				}
			}
		}

		log.info("... DONE");
	}

}
