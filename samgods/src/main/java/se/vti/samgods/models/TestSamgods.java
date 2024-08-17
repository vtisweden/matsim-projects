/**
 * se.vti.samgods.models
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import de.vandermeer.asciitable.AsciiTable;
import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.math.MathHelpers;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.NonTransportCost;
import se.vti.samgods.logistics.NonTransportCostModel;
import se.vti.samgods.logistics.NonTransportCostModel_v1_22;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.logistics.TransportDemand.AnnualShipment;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentChoiceStats;
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentSize;
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentSizeChoiceModel;
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentSizeUtilityFunction;
import se.vti.samgods.network.NetworkReader;
import se.vti.samgods.network.Router;
import se.vti.samgods.network.RoutingData;
import se.vti.samgods.transportation.DetailedTransportCost;
import se.vti.samgods.transportation.EpisodeCostModel;
import se.vti.samgods.transportation.EpisodeCostModels;
import se.vti.samgods.transportation.FallbackEpisodeCostModel;
import se.vti.samgods.transportation.consolidation.halfloop.HalfLoopConsolidator;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.consolidation.road.PerformanceMeasures;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
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

		InsufficientDataException.setLogDuringRuntime(false);

		boolean enforceMaxShipmentSize = false;
		boolean flexiblePeriod = true;

//		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.AGRICULTURE);
//		double samplingRate = 0.1;
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

		Network network = NetworkReader.load("./input_2024/node_parameters.csv", "./input_2024/link_parameters.csv");

		// LOAD DEMAND

		TransportDemand transportDemand = new TransportDemand();

		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			new ChainChoiReader(commodity, transportDemand).setSamplingRate(samplingRate, new Random(4711))
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
		EpisodeCostModels episodeCostModels = new EpisodeCostModels(fallbackEpisodeCostModel);

		NonTransportCostModel nonTransportCostModel = new NonTransportCostModel_v1_22();

		// ROUTE CHAINS

		RoutingData routingData = new RoutingData(network, episodeCostModels);
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			Map<OD, List<TransportChain>> od2chains = transportDemand.commodity2od2transportChains.get(commodity);
			Router router = new Router(routingData).setLogProgress(true).setMaxThreads(Integer.MAX_VALUE);
			router.route(commodity, od2chains);
		}

		// REMOVE UNROUTED CHAINS

		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			long removedCnt = 0;
			long totalCnt = 0;
			for (Map.Entry<OD, List<TransportChain>> entry : transportDemand.commodity2od2transportChains.get(commodity)
					.entrySet()) {
				final int chainCnt = entry.getValue().size();
				totalCnt += chainCnt;
				entry.setValue(entry.getValue().stream().filter(c -> c.isRouted()).collect(Collectors.toList()));
				removedCnt += chainCnt - entry.getValue().size();
			}
			log.warn(
					commodity + ": Removed " + removedCnt + " out of " + totalCnt + " chains with incomplete routes. ");
		}

		// RUN LOGISTICS MODEL

//		Map<TransportChain, List<Shipment>> chain2shipments = new LinkedHashMap<>();
		List<ChainAndShipmentSize> allChoices = new ArrayList<>();

		for (double capacityUsageFactor : new double[] { 0.7 }) {

			fallbackEpisodeCostModel = new FallbackEpisodeCostModel(fleet, consolidationCostModel)
					.setCapacityUsageFactor(capacityUsageFactor);
			episodeCostModels = new EpisodeCostModels(fallbackEpisodeCostModel);

			ChainAndShipmentSizeUtilityFunction utilityFunction = new ChainAndShipmentSizeUtilityFunction() {
				@Override
				public double computeUtility(Commodity commodity, double amount_ton,
						DetailedTransportCost transportUnitCost, NonTransportCost totalNonTransportCost) {
					return -transportUnitCost.monetaryCost * amount_ton - totalNonTransportCost.totalOrderCost
							- totalNonTransportCost.totalEnRouteLoss - totalNonTransportCost.totalInventoryCost;
				}
			};

			for (double scale : Arrays.asList(1.0)) {

				log.info("capacity usage factor = " + capacityUsageFactor + ", scale = " + scale);
				ChainAndShipmentChoiceStats stats = new ChainAndShipmentChoiceStats();

				ChainAndShipmentSizeChoiceModel choiceModel = new ChainAndShipmentSizeChoiceModel(scale,
						episodeCostModels, nonTransportCostModel, utilityFunction).setEnforceMaxShipmentSize(enforceMaxShipmentSize);
				for (SamgodsConstants.Commodity commodity : consideredCommodities) {

					Map<SamgodsConstants.ShipmentSize, Long> size2cnt = Arrays
							.stream(SamgodsConstants.ShipmentSize.values()).collect(Collectors.toMap(s -> s, s -> 0l));

					for (Map.Entry<OD, List<TransportDemand.AnnualShipment>> e : transportDemand.commodity2od2annualShipments
							.get(commodity).entrySet()) {
						final OD od = e.getKey();
						final List<AnnualShipment> annualShipments = e.getValue();
						final List<TransportChain> transportChains = transportDemand.commodity2od2transportChains
								.get(commodity).get(od);
						if (transportChains.size() == 0) {
							new InsufficientDataException(TestSamgods.class, "No transport chains available.",
									commodity, od, null, null, null).log();
						} else {
							List<ChainAndShipmentSize> choices = choiceModel.choose(commodity, od, transportChains,
									annualShipments);
							stats.add(commodity, choices);
							allChoices.addAll(choices);
//							for (ChainAndShipmentSize choice : choices) {
//								size2cnt.compute(choice.sizeClass, (s, c) -> c + 1);
//								List<Shipment> shipments = UniformConsolidator.createSimulatedShipments(
//										ConsolidationUtils.disaggregateIntoAnalysisPeriod(choice.annualShipment, 7,
//												choice.sizeClass),
//										rnd);
//								if (shipments.size() > 0) {
//									chain2shipments.computeIfAbsent(choice.transportChain, c -> new ArrayList<>())
//											.addAll(shipments);
//								}
//							}
						}
					}
				}

				log.info("\n" + stats.createChoiceStatsTable());
			}
		}
//		log.info(chain2shipments.size() + " chains");
//		log.info(chain2shipments.values().stream().mapToLong(s -> s.size()).sum() + " shipments");

		// PREPARE CONSOLIDATION

//		Map<Signature.Episode, List<Shipment>> episodeSignature2shipments = new LinkedHashMap<>();
//		for (Map.Entry<SamgodsConstants.Commodity, Map<OD, List<TransportChain>>> commodityAndMap : transportDemand.commodity2od2transportChains
//				.entrySet()) {
//			for (Map.Entry<OD, List<TransportChain>> odAndChains : commodityAndMap.getValue().entrySet()) {
//				for (TransportChain chain : odAndChains.getValue()) {
//					for (TransportEpisode episode : chain.getEpisodes()) {
//						Signature.Episode signature = new Signature.Episode(episode);
//					}
//				}
//			}
//		}

		Map<Signature.Episode, List<ChainAndShipmentSize>> episodeSignature2choices = new LinkedHashMap<>();
		Map<Signature.Episode, List<TransportEpisode>> episodeSignature2episodes = new LinkedHashMap<>();
		for (ChainAndShipmentSize choice : allChoices) {
			for (TransportEpisode episode : choice.transportChain.getEpisodes()) {
				Signature.Episode signature = new Signature.Episode(episode);
				episodeSignature2choices.computeIfAbsent(signature, s -> new LinkedList<>()).add(choice);
				episodeSignature2episodes.computeIfAbsent(signature, s -> new LinkedList<>()).add(episode);
			}

		}
		log.info(episodeSignature2choices.size() + " episode signatures.");
		log.info(episodeSignature2choices.values().stream().flatMap(l -> l.stream())
				.mapToDouble(c -> c.annualShipment.getTotalAmount_ton() / c.sizeClass.getRepresentativeValue_ton())
				.sum() + " shipments.");

		for (int serviceWindow_days : new int[] { 1, 7, 30, 365 }) {

			log.info("SERVICE WINDOW = " + serviceWindow_days + " DAYS");

			Map<TransportMode, BasicStatistics> mode2efficiencyStats = new LinkedHashMap<>();
			Map<Commodity, BasicStatistics> commodity2efficiencyStats = new LinkedHashMap<>();

			Map<TransportMode, Double> mode2payloadSum = new LinkedHashMap<>();
			Map<Commodity, Double> commodity2payloadSum = new LinkedHashMap<>();

			Map<TransportMode, Double> mode2capacitySum = new LinkedHashMap<>();
			Map<Commodity, Double> commodity2capacitySum = new LinkedHashMap<>();

			HalfLoopConsolidator consolidator = new HalfLoopConsolidator(fleet, consolidationCostModel,
					serviceWindow_days, flexiblePeriod);
			for (Map.Entry<Signature.Episode, List<ChainAndShipmentSize>> e : episodeSignature2choices.entrySet()) {
				try {
					TransportEpisode episode = episodeSignature2episodes.get(e.getKey()).get(0);
					List<ChainAndShipmentSize> shipments = e.getValue();
					if (!episode.getLoadingNode().equals(episode.getUnloadingNode()) && shipments.stream()
							.mapToDouble(s -> s.annualShipment.getTotalAmount_ton()).sum() > 1e-3) {
						HalfLoopConsolidator.FleetAssignment assignment = consolidator
								.computeOptimalFleetAssignment(episode, e.getValue());
						double capacity_ton = FreightVehicleAttributes.getCapacity_ton(assignment.vehicleType);
						double efficiency = assignment.payload_ton / capacity_ton;
						mode2efficiencyStats.computeIfAbsent(episode.getMode(), m -> new BasicStatistics())
								.add(efficiency);
						commodity2efficiencyStats.computeIfAbsent(episode.getCommodity(), c -> new BasicStatistics())
								.add(efficiency);
						mode2payloadSum.compute(episode.getMode(),
								(m, s) -> s == null ? assignment.payload_ton : s + assignment.payload_ton);
						commodity2payloadSum.compute(episode.getCommodity(),
								(c, s) -> s == null ? assignment.payload_ton : s + assignment.payload_ton);
						mode2capacitySum.compute(episode.getMode(),
								(m, s) -> s == null ? capacity_ton : s + capacity_ton);
						commodity2capacitySum.compute(episode.getCommodity(),
								(c, s) -> s == null ? capacity_ton : s + capacity_ton);
//						if (assignment.vehicleCnt > 1) {
//							System.out.println(episode.getCommodity() + ", " + episode.getMode() + ": " + assignment);
//						}
					}
				} catch (InsufficientDataException e1) {
					e1.log(TestSamgods.class, "during consolidation");
				}
			}

			AsciiTable table = new AsciiTable();
			table.addRule();
			table.addRow("Mode", "E{payload/capacity}[%]", "E{payload}/E{capacity}[%]");
			table.addRule();
			for (Map.Entry<TransportMode, BasicStatistics> e : mode2efficiencyStats.entrySet()) {
				table.addRow(e.getKey(), MathHelpers.round(e.getValue().getAvg() * 100.0, 2),
						MathHelpers.round(100.0 * mode2payloadSum.getOrDefault(e.getKey(), 0.0)
								/ mode2capacitySum.getOrDefault(e.getKey(), 0.0), 2));
				table.addRule();
			}
			log.info("Transport efficiency per mode");
			log.info("\n" + table.render());

			table = new AsciiTable();
			table.addRule();
			table.addRow("Commodity", "Efficiency[%]", "E{payload}/E{capacity}[%]");
			table.addRule();
			for (Map.Entry<Commodity, BasicStatistics> e : commodity2efficiencyStats.entrySet()) {
				table.addRow(e.getKey(), MathHelpers.round(e.getValue().getAvg() * 100.0, 2),
						MathHelpers.round(100.0 * commodity2payloadSum.getOrDefault(e.getKey(), 0.0)
								/ commodity2capacitySum.getOrDefault(e.getKey(), 0.0), 2));
				table.addRule();
			}
			log.info("Transport efficiency per commodity");
			log.info("\n" + table.render());

		}

//		Map<Id<VehicleType>, Long> type2cnt = new LinkedHashMap<>();
//
//		System.out.println();
//		System.out.println("totalDemand[ton]\tmeanVehicleCapacity[ton]");
//		for (Map.Entry<Signature.Episode, List<Shipment>> e : episodeSignature2shipments.entrySet()) {
//			UniformConsolidator consolidator = new UniformConsolidator(fleet, 7, 0.7);
//			List<List<Shipment>> shipmentsOverDays = consolidator.distributeShipmentsOverDays(e.getValue());
//			double totalDemand_ton = shipmentsOverDays.stream().flatMap(l -> l.stream())
//					.mapToDouble(s -> s.getWeight_ton()).sum();
//			if (totalDemand_ton > 0) {
//				List<List<Vehicle>> vehiclesOverDays = consolidator.createVehiclesOverDays(shipmentsOverDays);
//
//				for (List<Vehicle> vehicles : vehiclesOverDays) {
//					for (Vehicle vehicle : vehicles) {
//						type2cnt.compute(vehicle.getType().getId(), (id, cnt) -> cnt == null ? 1 : cnt + 1);
//					}
//				}
//
//				if (Math.random() < 1e-1) {
//					OptionalDouble meanCapacity_ton = vehiclesOverDays.stream().flatMap(l -> l.stream())
//							.mapToDouble(v -> FreightVehicleAttributes.getCapacity_ton(v)).average();
//					if (meanCapacity_ton.isPresent()) {
//						System.out.println(totalDemand_ton + "\t" + meanCapacity_ton.getAsDouble());
//					}
//				}
//			}
//		}
//
//		System.out.println();
//
//		double totalTons = type2cnt.entrySet().stream().mapToDouble(
//				e -> FreightVehicleAttributes.getCapacity_ton(fleet.getVehicles().getVehicleTypes().get(e.getKey()))
//						* e.getValue())
//				.sum();
//		System.out.println("vehType\tshare");
//		for (VehicleType type : fleet.getVehicles().getVehicleTypes().values()) {
//			System.out.println(type.getId() + "\t" + type2cnt.getOrDefault(type.getId(), 0l)
//					* FreightVehicleAttributes.getCapacity_ton(type) / totalTons);
//		}
//		System.out.println();

		log.info("DONE");
	}
}
