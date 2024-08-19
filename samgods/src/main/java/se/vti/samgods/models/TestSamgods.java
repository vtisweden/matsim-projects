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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.vehicles.VehicleType;

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

		Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval = new LinkedHashMap<>();
		commodity2serviceInterval.put(SamgodsConstants.Commodity.AGRICULTURE, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.AIR, 30);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.BASICMETALS, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.CHEMICALS, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.COAL, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.COKE, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.FOOD, 1);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.FURNITURE, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.MACHINERY, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.METAL, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.OTHERMINERAL, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.SECONDARYRAW, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.TEXTILES, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.TIMBER, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.TRANSPORT, 14);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.WOOD, 14);
		
		InsufficientDataException.setLogDuringRuntime(false);

//		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.AGRICULTURE);
//		double samplingRate = 0.01;
		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.values());
		double samplingRate = 1.0;

		boolean flexiblePeriod = true;
		boolean skipUnusedIntervals = true;

		double scale = 1.0;
		boolean enforceMaxShipmentSize = false;
		int maxIterations = 5;
		boolean useNonTransportCosts = true;

		boolean checkUnitCost = false;

		boolean routed = false;
		boolean reroute = false;

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

			double odCnt = transportDemand.commodity2od2transportChains.get(commodity).size();
			double chainCnt = transportDemand.commodity2od2transportChains.get(commodity).values().stream()
					.mapToDouble(l -> l.size()).sum();

			log.info(commodity + ": avg number of chains per OD = " + chainCnt / odCnt);
		}

		// ----------------------------------------------------------------------
		// ------------------------------ ITERATIONS ----------------------------
		// ----------------------------------------------------------------------

		Map<TransportMode, Double> mode2efficiency = Arrays.stream(TransportMode.values())
				.collect(Collectors.toMap(m -> m, m -> 0.7));
		Map<Signature.Episode, Double> episode2efficiency = new LinkedHashMap<>();

		for (int iteration = 0; iteration < maxIterations; iteration++) {

			double innoWeight = 1.0 / (1.0 + iteration);

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
			EpisodeCostModel fallbackEpisodeCostModel = new FallbackEpisodeCostModel(fleet, consolidationCostModel,
					mode2efficiency, episode2efficiency);
			EpisodeCostModels episodeCostModels = new EpisodeCostModels(fallbackEpisodeCostModel);

			NonTransportCostModel nonTransportCostModel = new NonTransportCostModel_v1_22();

			if (!routed || reroute) {
				// (RE) ROUTE CHAINS
				RoutingData routingData = new RoutingData(network, episodeCostModels);
				for (SamgodsConstants.Commodity commodity : consideredCommodities) {
					Map<OD, List<TransportChain>> od2chains = transportDemand.commodity2od2transportChains
							.get(commodity);
					Router router = new Router(routingData).setLogProgress(true).setMaxThreads(Integer.MAX_VALUE);
					router.route(commodity, od2chains);
				}
				// REMOVE UNROUTED CHAINS
				for (SamgodsConstants.Commodity commodity : consideredCommodities) {
					long removedCnt = 0;
					long totalCnt = 0;
					for (Map.Entry<OD, List<TransportChain>> entry : transportDemand.commodity2od2transportChains
							.get(commodity).entrySet()) {
						final int chainCnt = entry.getValue().size();
						totalCnt += chainCnt;
						entry.setValue(
								entry.getValue().stream().filter(c -> c.isRouted()).collect(Collectors.toList()));
						removedCnt += chainCnt - entry.getValue().size();
					}
					log.warn(commodity + ": Removed " + removedCnt + " out of " + totalCnt
							+ " chains with incomplete routes. ");
				}
				routed = true;
			}

			// RUN LOGISTICS MODEL

//			fallbackEpisodeCostModel = new FallbackEpisodeCostModel(fleet, consolidationCostModel, mode2capacityUsage);
//			episodeCostModels = new EpisodeCostModels(fallbackEpisodeCostModel);

			ChainAndShipmentSizeUtilityFunction utilityFunction = new ChainAndShipmentSizeUtilityFunction() {
				@Override
				public double computeUtility(Commodity commodity, double amount_ton,
						DetailedTransportCost transportUnitCost, NonTransportCost totalNonTransportCost) {
					return -transportUnitCost.monetaryCost * amount_ton + (useNonTransportCosts
							? -totalNonTransportCost.totalOrderCost - totalNonTransportCost.totalEnRouteLoss
									- totalNonTransportCost.totalInventoryCost
							: 0.0);
				}
			};

//			Map<TransportChain, List<Shipment>> chain2shipments = new LinkedHashMap<>();
			List<ChainAndShipmentSize> allChoices = new ArrayList<>();

			ChainAndShipmentChoiceStats stats = new ChainAndShipmentChoiceStats();
			ChainAndShipmentSizeChoiceModel choiceModel = new ChainAndShipmentSizeChoiceModel(scale, episodeCostModels,
					nonTransportCostModel, utilityFunction).setEnforceMaxShipmentSize(enforceMaxShipmentSize);
			for (SamgodsConstants.Commodity commodity : consideredCommodities) {
//				Map<SamgodsConstants.ShipmentSize, Long> size2cnt = Arrays
//						.stream(SamgodsConstants.ShipmentSize.values()).collect(Collectors.toMap(s -> s, s -> 0l));
				for (Map.Entry<OD, List<TransportDemand.AnnualShipment>> e : transportDemand.commodity2od2annualShipments
						.get(commodity).entrySet()) {
					final OD od = e.getKey();
					final List<AnnualShipment> annualShipments = e.getValue();
					final List<TransportChain> transportChains = transportDemand.commodity2od2transportChains
							.get(commodity).get(od);
					if (transportChains.size() == 0) {
						new InsufficientDataException(TestSamgods.class, "No transport chains available.", commodity,
								od, null, null, null).log();
					} else {
						List<ChainAndShipmentSize> choices = choiceModel.choose(commodity, od, transportChains,
								annualShipments);
						stats.add(commodity, choices);
						allChoices.addAll(choices);
//								for (ChainAndShipmentSize choice : choices) {
//									size2cnt.compute(choice.sizeClass, (s, c) -> c + 1);
//									List<Shipment> shipments = UniformConsolidator.createSimulatedShipments(
//											ConsolidationUtils.disaggregateIntoAnalysisPeriod(choice.annualShipment, 7,
//													choice.sizeClass),
//											rnd);
//									if (shipments.size() > 0) {
//										chain2shipments.computeIfAbsent(choice.transportChain, c -> new ArrayList<>())
//												.addAll(shipments);
//									}
//								}
					}
				}
			}
			log.info("\n" + stats.createChoiceStatsTable());
//			log.info(chain2shipments.size() + " chains");
//			log.info(chain2shipments.values().stream().mapToLong(s -> s.size()).sum() + " shipments");

//			Map<Signature.Episode, List<Shipment>> episodeSignature2shipments = new LinkedHashMap<>();
//			for (Map.Entry<SamgodsConstants.Commodity, Map<OD, List<TransportChain>>> commodityAndMap : transportDemand.commodity2od2transportChains
//					.entrySet()) {
//				for (Map.Entry<OD, List<TransportChain>> odAndChains : commodityAndMap.getValue().entrySet()) {
//					for (TransportChain chain : odAndChains.getValue()) {
//						for (TransportEpisode episode : chain.getEpisodes()) {
//							Signature.Episode signature = new Signature.Episode(episode);
//						}
//					}
//				}
//			}
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

			// CONSOLIDATE AND STATISTICS BOKKEEPING

			Map<TransportMode, Double> mode2efficiencyTimesVehicleCntSum = new LinkedHashMap<>();
			Map<VehicleType, Double> vehicleType2vehicleCntSum = new LinkedHashMap<>();
			Map<TransportMode, Double> mode2vehicleCntSum = new LinkedHashMap<>();

			Map<TransportMode, Double> mode2sumOfCosts = new LinkedHashMap<>();
			Map<TransportMode, Double> mode2sumOfTonKm = new LinkedHashMap<>();

			HalfLoopConsolidator consolidator = new HalfLoopConsolidator(fleet, consolidationCostModel,
					commodity2serviceInterval, flexiblePeriod, skipUnusedIntervals);
			for (Map.Entry<Signature.Episode, List<ChainAndShipmentSize>> e : episodeSignature2choices.entrySet()) {
				try {
					// TODO Below assumes that all episodes matching a signature are logically
					// identicaly.
					TransportEpisode episode = episodeSignature2episodes.get(e.getKey()).get(0);

					List<ChainAndShipmentSize> shipments = e.getValue();
					double totalDemand_ton = shipments.stream().mapToDouble(s -> s.annualShipment.getTotalAmount_ton())
							.sum();
					if (episode.getLoadingNode().equals(episode.getUnloadingNode())) {
						log.warn("Skipping episode with origin = destination");
					} else if (totalDemand_ton < 0.001) {
						log.warn("Skipping episode with too small total demand " + totalDemand_ton + ". Signature: "
								+ e.getKey());
					} else {

						HalfLoopConsolidator.FleetAssignment assignment = consolidator
								.computeOptimalFleetAssignment(episode, shipments);
						double vehicleCnt = assignment.expectedNumberOfSimultaneouslyMovingVehicles();
						double length_km = episode.getLegs().stream().mapToDouble(l -> l.getLength_km()).sum();

						mode2efficiencyTimesVehicleCntSum.compute(episode.getMode(),
								(m, s) -> s == null ? assignment.transportEfficiency() * vehicleCnt
										: s + assignment.transportEfficiency() * vehicleCnt);
						vehicleType2vehicleCntSum.compute(assignment.vehicleType,
								(t, c) -> c == null ? vehicleCnt : c + vehicleCnt);
						mode2vehicleCntSum.compute(episode.getMode(),
								(m, c) -> c == null ? vehicleCnt : c + vehicleCnt);

						mode2sumOfCosts.compute(episode.getMode(),
								(m, s) -> s == null ? assignment.unitCost_1_ton * totalDemand_ton
										: s + assignment.unitCost_1_ton * totalDemand_ton);
						mode2sumOfTonKm.compute(episode.getMode(),
								(m, s) -> s == null ? totalDemand_ton * length_km : s + totalDemand_ton * length_km);

						if (checkUnitCost) {
							assert (Math
									.abs(assignment.unitCost_1_ton
											- consolidationCostModel
													.computeEpisodeCost(
															FreightVehicleAttributes
																	.getFreightAttributes(assignment.vehicleType),
															assignment.payload_ton, episode)
													.computeUnitCost().monetaryCost) <= 1.0);
						}

						episode2efficiency.compute(e.getKey(),
								(epi, eff) -> eff == null ? assignment.transportEfficiency()
										: innoWeight * assignment.transportEfficiency() + (1.0 - innoWeight) * eff);
					}

				} catch (InsufficientDataException e1) {
					e1.log(TestSamgods.class, "during consolidation");
				}
			}

			final Map<SamgodsConstants.TransportMode, Double> mode2realizedEfficiency = new LinkedHashMap<>();
			final Map<SamgodsConstants.TransportMode, Double> mode2unitCost_1_tonKm = new LinkedHashMap<>();
			for (SamgodsConstants.TransportMode mode : mode2vehicleCntSum.keySet()) {
				mode2unitCost_1_tonKm.put(mode, mode2sumOfCosts.get(mode) / mode2sumOfTonKm.get(mode));
				mode2realizedEfficiency.put(mode,
						mode2efficiencyTimesVehicleCntSum.get(mode) / mode2vehicleCntSum.get(mode));
				mode2efficiency.compute(mode, (m, e) -> e == null ? mode2realizedEfficiency.get(mode)
						: innoWeight * mode2realizedEfficiency.get(mode) + (1.0 - innoWeight) * e);
			}

			logEfficiency(mode2realizedEfficiency, iteration);
			logFleet(vehicleType2vehicleCntSum, iteration, fleet);
			logCost(mode2unitCost_1_tonKm, iteration);

//			Map<TransportMode, BasicStatistics> mode2efficiencyStats = new LinkedHashMap<>();

//			AsciiTable table = new AsciiTable();
//			table.addRule();
//			table.addRow("Mode", "E{payload/capacity}[%]", "E{payload}/E{capacity}[%]");
//			table.addRule();
//			for (Map.Entry<TransportMode, BasicStatistics> e : mode2efficiencyStats.entrySet()) {
//				table.addRow(e.getKey(), MathHelpers.round(e.getValue().getAvg() * 100.0, 2),
//						MathHelpers.round(100.0 * mode2payloadSum.getOrDefault(e.getKey(), 0.0)
//								/ mode2capacitySum.getOrDefault(e.getKey(), 0.0), 2));
//				table.addRule();
//			}
//			log.info("Transport efficiency per mode");
//			log.info("\n" + table.render());
//
//			table = new AsciiTable();
//			table.addRule();
//			table.addRow("Commodity", "Efficiency[%]", "E{payload}/E{capacity}[%]");
//			table.addRule();
//			for (Map.Entry<Commodity, BasicStatistics> e : commodity2efficiencyStats.entrySet()) {
//				table.addRow(e.getKey(), MathHelpers.round(e.getValue().getAvg() * 100.0, 2),
//						MathHelpers.round(100.0 * commodity2payloadSum.getOrDefault(e.getKey(), 0.0)
//								/ commodity2capacitySum.getOrDefault(e.getKey(), 0.0), 2));
//				table.addRule();
//			}
//			log.info("Transport efficiency per commodity");
//			log.info("\n" + table.render());

//			Map<Id<VehicleType>, Long> type2cnt = new LinkedHashMap<>();
			//
//			System.out.println();
//			System.out.println("totalDemand[ton]\tmeanVehicleCapacity[ton]");
//			for (Map.Entry<Signature.Episode, List<Shipment>> e : episodeSignature2shipments.entrySet()) {
//				UniformConsolidator consolidator = new UniformConsolidator(fleet, 7, 0.7);
//				List<List<Shipment>> shipmentsOverDays = consolidator.distributeShipmentsOverDays(e.getValue());
//				double totalDemand_ton = shipmentsOverDays.stream().flatMap(l -> l.stream())
//						.mapToDouble(s -> s.getWeight_ton()).sum();
//				if (totalDemand_ton > 0) {
//					List<List<Vehicle>> vehiclesOverDays = consolidator.createVehiclesOverDays(shipmentsOverDays);
			//
//					for (List<Vehicle> vehicles : vehiclesOverDays) {
//						for (Vehicle vehicle : vehicles) {
//							type2cnt.compute(vehicle.getType().getId(), (id, cnt) -> cnt == null ? 1 : cnt + 1);
//						}
//					}
			//
//					if (Math.random() < 1e-1) {
//						OptionalDouble meanCapacity_ton = vehiclesOverDays.stream().flatMap(l -> l.stream())
//								.mapToDouble(v -> FreightVehicleAttributes.getCapacity_ton(v)).average();
//						if (meanCapacity_ton.isPresent()) {
//							System.out.println(totalDemand_ton + "\t" + meanCapacity_ton.getAsDouble());
//						}
//					}
//				}
//			}
			//
//			System.out.println();
			//
//			double totalTons = type2cnt.entrySet().stream().mapToDouble(
//					e -> FreightVehicleAttributes.getCapacity_ton(fleet.getVehicles().getVehicleTypes().get(e.getKey()))
//							* e.getValue())
//					.sum();
//			System.out.println("vehType\tshare");
//			for (VehicleType type : fleet.getVehicles().getVehicleTypes().values()) {
//				System.out.println(type.getId() + "\t" + type2cnt.getOrDefault(type.getId(), 0l)
//						* FreightVehicleAttributes.getCapacity_ton(type) / totalTons);
//			}
//			System.out.println();

		}

		log.info("DONE");
	}

	static void logEfficiency(Map<SamgodsConstants.TransportMode, Double> mode2realizedEfficiency, int iteration) {
		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				headerLine += "efficiency(" + mode + ")\t";
			}
			try {
				FileUtils.write(new File("efficiency.txt"), headerLine + "\n", false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		String dataLine = "";
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			if (mode2realizedEfficiency.containsKey(mode)) {
				dataLine += mode2realizedEfficiency.get(mode) + "\t";
			} else {
				dataLine += "\t";
			}
		}
		try {
			FileUtils.write(new File("efficiency.txt"), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void logFleet(Map<VehicleType, Double> vehType2cnt, int iteration, VehicleFleet fleet) {

		Map<SamgodsConstants.TransportMode, List<VehicleType>> mode2types = new LinkedHashMap<>();
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			mode2types.put(mode, fleet.getCompatibleVehicleTypes(null, mode, null, null));
			Collections.sort(mode2types.get(mode), new Comparator<VehicleType>() {
				@Override
				public int compare(VehicleType o1, VehicleType o2) {
					return Double.compare(FreightVehicleAttributes.getCapacity_ton(o1),
							FreightVehicleAttributes.getCapacity_ton(o2));
				}
			});
		}

		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				for (VehicleType type : mode2types.get(mode)) {
					headerLine += type.getId().toString() + "\t";
				}
				headerLine += "\t";
			}
			try {
				FileUtils.write(new File("fleet.txt"), headerLine + "\n", false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				for (VehicleType type : mode2types.get(mode)) {
					headerLine += FreightVehicleAttributes.getCapacity_ton(type) + "\t";
				}
				headerLine += "\t";
			}
			try {
				FileUtils.write(new File("fleet.txt"), headerLine + "\n", true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

		double totalCnt = vehType2cnt.values().stream().mapToDouble(c -> c).sum();

		String dataLine = "";
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			for (VehicleType type : mode2types.get(mode)) {
				dataLine += (vehType2cnt.getOrDefault(type, 0.0) / totalCnt) + "\t";
			}
			dataLine += "\t";
		}
		try {
			FileUtils.write(new File("fleet.txt"), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void logCost(Map<SamgodsConstants.TransportMode, Double> mode2unitCost_1_tonKm, int iteration) {
		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				headerLine += "unitCost(" + mode + ")[SEK/tonKm]\t";
			}
			try {
				FileUtils.write(new File("unitcost.txt"), headerLine + "\n", false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		String dataLine = "";
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			if (mode2unitCost_1_tonKm.containsKey(mode)) {
				dataLine += mode2unitCost_1_tonKm.get(mode) + "\t";
			} else {
				dataLine += "\t";
			}
		}
		try {
			FileUtils.write(new File("unitcost.txt"), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
