/**
 * se.vti.samgods.transportation.ntmcalc
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
package se.vti.samgods.external.ntmcalc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.network.SamgodsLinkAttributes;
import se.vti.samgods.transportation.consolidation.road.ConsolidationUtils;
import se.vti.samgods.transportation.fleet.VehicleFleet;
import se.vti.samgods.transportation.fleet.SamgodsFleetReader;

/**
 * 
 * @author GunnarF
 *
 */
public class NTMCalcSerializerTest {

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

		VehicleFleet fleet = new VehicleFleet();
		SamgodsFleetReader reader = new SamgodsFleetReader(fleet);
		reader.load_v12("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
				SamgodsConstants.TransportMode.Road);

		Random rnd = new Random(4711);
		List<VehicleType> vehTypeList = fleet.getVehicles().getVehicleTypes().values().stream()
				.collect(Collectors.toList());

		int vehCnt = 10000;

		int nodeCnt = 100;
		int maxEpisodeCnt = 10;
		int maxLegCnt = 10;
		int maxRouteLength = 10;

		List<VehicleEpisode> allEpisodes = new ArrayList<>();
		Network network = NetworkUtils.createNetwork();

		for (int vehNr = 0; vehNr < vehCnt; vehNr++) {

			VehicleType vehType = vehTypeList.get(rnd.nextInt(vehTypeList.size()));
			Vehicle veh = VehicleUtils.createVehicle(Id.createVehicleId("" + vehNr), vehType);

			int episodeCnt = rnd.nextInt(maxEpisodeCnt + 1);
			for (int episodeNr = 0; episodeNr < episodeCnt; episodeNr++) {

				TransportEpisode episode = new TransportEpisode(SamgodsConstants.TransportMode.Road,
						SamgodsConstants.Commodity.AGRICULTURE, false);

				int legCnt = rnd.nextInt(maxLegCnt + 1);
				for (int legNr = 0; legNr < legCnt; legNr++) {

					int nodesInRoute = 1 + rnd.nextInt(maxRouteLength);
					LinkedList<Node> routeNodes = new LinkedList<>();
					for (int nodeNr = 0; nodeNr < nodesInRoute; nodeNr++) {
						Id<Node> nodeId = Id.createNodeId(rnd.nextInt(nodeCnt));
						Node node = network.getNodes().get(nodeId);
						if (node == null) {
							node = NetworkUtils.createAndAddNode(network, nodeId, new Coord());
						}
						routeNodes.add(node);
					}

					List<Link> routeLinks = new ArrayList<>();
					for (int nodeIndex = 1; nodeIndex < routeNodes.size(); nodeIndex++) {
						Node from = routeNodes.get(nodeIndex - 1);
						Node to = routeNodes.get(nodeIndex);
						Link link = NetworkUtils.getConnectingLink(from, to);
						if (link == null) {
							link = NetworkUtils.createAndAddLink(network,
									Id.createLinkId(from.getId() + "_" + to.getId()), from, to, rnd.nextDouble() * 1000,
									Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 1 + rnd.nextInt(1), "", "");
							if (rnd.nextDouble() < 0.9) {
								double speed1_km_h = Arrays.asList(30, 50, 70, 90, 110).get(rnd.nextInt(5));
								link.getAttributes().putAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME,
										new SamgodsLinkAttributes(SamgodsConstants.TransportMode.Road, speed1_km_h,
												null));
							} else {
								double speed1_km_h = Arrays.asList(5, 10, 15, 20, 25).get(rnd.nextInt(5));
								link.getAttributes().putAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME,
										new SamgodsLinkAttributes(SamgodsConstants.TransportMode.Ferry, speed1_km_h,
												null));
							}

						}
						routeLinks.add(link);
					}

					TransportLeg leg = new TransportLeg(
							new OD(routeNodes.getFirst().getId(), routeNodes.getLast().getId()),
							SamgodsConstants.TransportMode.Road);
					leg.setRoute(routeLinks);
					episode.addLeg(leg);
				}

				allEpisodes.add(new VehicleEpisode(veh, rnd.nextDouble() * ConsolidationUtils.getCapacity_ton(vehType),
						episode));
			}
		}

		Collections.shuffle(allEpisodes);

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
		mapper.writeValue(new File(vehCnt + ".vehs_episodes.json"), allEpisodes);

		System.out.println("... DONE");

	}

}
