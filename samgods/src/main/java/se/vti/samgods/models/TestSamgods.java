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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import floetteroed.utilities.math.BasicStatistics;
import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
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
import se.vti.samgods.logistics.choicemodel.ChainAndShipmentSizeUtilityFunction;
import se.vti.samgods.logistics.choicemodel.ChoiceJob;
import se.vti.samgods.logistics.choicemodel.ChoiceSimulator;
import se.vti.samgods.network.NetworkDataProvider;
import se.vti.samgods.network.NetworkReader;
import se.vti.samgods.network.Router;
import se.vti.samgods.transportation.consolidation.ConsolidationCostModel;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidator;
import se.vti.samgods.transportation.consolidation.PerformanceMeasures;
import se.vti.samgods.transportation.costs.BasicEpisodeCostModel;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.EpisodeCostModel;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.SamgodsFleetReader;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class TestSamgods {

	static final Logger log = Logger.getLogger(TestSamgods.class);

	static final String consolidationUnitsFileName = "consolidationUnits.json";

	public static void main(String[] args) throws IOException {

		/*
		 * SET EXPERIMENTAL PARAMETERS
		 */

//		Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval = Arrays.stream(Commodity.values())
//				.collect(Collectors.toMap(c -> c, c -> 1));

		Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval = new LinkedHashMap<>();

		commodity2serviceInterval.put(SamgodsConstants.Commodity.AGRICULTURE, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.AIR, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.BASICMETALS, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.CHEMICALS, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.COAL, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.COKE, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.FOOD, 1);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.FURNITURE, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.MACHINERY, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.METAL, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.OTHERMINERAL, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.SECONDARYRAW, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.TEXTILES, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.TIMBER, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.TRANSPORT, 7);
		commodity2serviceInterval.put(SamgodsConstants.Commodity.WOOD, 7);

//		commodity2serviceInterval.put(SamgodsConstants.Commodity.AGRICULTURE, 7);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.AIR, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.BASICMETALS, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.CHEMICALS, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.COAL, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.COKE, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.FOOD, 1);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.FURNITURE, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.MACHINERY, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.METAL, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.OTHERMINERAL, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.SECONDARYRAW, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.TEXTILES, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.TIMBER, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.TRANSPORT, 14);
//		commodity2serviceInterval.put(SamgodsConstants.Commodity.WOOD, 14);

		InsufficientDataException.setLogDuringRuntime(false);

		EfficiencyLogger effLog = new EfficiencyLogger("efficiencyDetail.txt");

//		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.AGRICULTURE);
//		double samplingRate = 0.01;
//		boolean upscale = false;
		List<SamgodsConstants.Commodity> consideredCommodities = Arrays.asList(SamgodsConstants.Commodity.values());
		double samplingRate = 1.0;
		boolean upscale = false;

		double scale = 1.0;
		int maxIterations = 10;
		double nonTransportCostFactor = 1.0;
		boolean enforceReroute = false;

		log.info("STARTED ...");

		/*
		 * LOAD FLEET
		 */

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

		/*
		 * LOAD NETWORK
		 */

		Network network = NetworkReader.load("./input_2024/node_parameters.csv", "./input_2024/link_parameters.csv");

		/*
		 * LOAD DEMAND
		 */

		TransportDemand transportDemand = new TransportDemand();
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			new ChainChoiReader(commodity, transportDemand).setSamplingRate(samplingRate, new Random(4711))
					.setUpscaleAgainstSamplingRate(upscale)
					.parse("./input_2024/ChainChoi" + commodity.twoDigitCode() + "XTD.out");
			double odCnt = transportDemand.commodity2od2transportChains.get(commodity).size();
			double chainCnt = transportDemand.commodity2od2transportChains.get(commodity).values().stream()
					.mapToDouble(l -> l.size()).sum();
			log.info(commodity + ": avg number of chains per OD = " + chainCnt / odCnt);
		}

		/*
		 * CREATE / LOAD CONSOLIDATION UNITS
		 */

		if (enforceReroute || !(new File(consolidationUnitsFileName).exists())) {

			/*
			 * Several episodes may have consolidation units with the same routes. To reduce
			 * routing effort, we here collect, for each possible routing configuration, one
			 * representative consolidation unit to be routed. We store a back-links to the
			 * consolidation units attached to the episodes in order to later replace them
			 * by equivalent routed instances.
			 */
			final Map<ConsolidationUnit, ConsolidationUnit> consolidationUnitPattern2representativeUnit = new LinkedHashMap<>();
			for (SamgodsConstants.Commodity commodity : consideredCommodities) {
				for (List<TransportChain> chains : transportDemand.commodity2od2transportChains.get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							episode.setConsolidationUnits(ConsolidationUnit.createUnrouted(episode));
							for (ConsolidationUnit consolidationUnit : episode.getConsolidationUnits()) {
								consolidationUnitPattern2representativeUnit
										.put(consolidationUnit.createRoutingEquivalentCopy(), consolidationUnit);
							}
						}
					}
				}
			}

			/*
			 * Route (if possible) the representative consolidation units.
			 * 
			 * Routing changes the behavior of hashcode(..) / equals(..) in
			 * ConsolidationUnit, hence we store (to be) routed units in a List.
			 */
			for (Commodity commodity : consideredCommodities) {
				log.info(commodity + ": Routing consolidation units.");
				Router router = new Router(network, fleet).setLogProgress(true).setMaxThreads(Integer.MAX_VALUE);
				router.route(commodity, consolidationUnitPattern2representativeUnit.entrySet().stream()
						.filter(e -> commodity.equals(e.getKey().commodity)).map(e -> e.getValue()).toList());
			}

			/*
			 * Stream *routed* consolidation units to json file.
			 */
			long routedCnt = 0;
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
			FileOutputStream fos = new FileOutputStream(new File(consolidationUnitsFileName));
			JsonGenerator gen = mapper.getFactory().createGenerator(fos);
			for (ConsolidationUnit consolidationUnit : consolidationUnitPattern2representativeUnit.values()) {
				if (consolidationUnit.isRouted()) {
					mapper.writeValue(gen, consolidationUnit);
					routedCnt++;
				}
			}
			gen.flush();
			gen.close();
			fos.flush();
			fos.close();
			log.info("Wrote " + routedCnt + " (out of in total " + consolidationUnitPattern2representativeUnit.size()
					+ ") routed consolidation units to file " + consolidationUnitsFileName);

			/*
			 * Attach representative consolidation units to the episodes. This means that
			 * episodes from now on share consolidation units.
			 */
			for (SamgodsConstants.Commodity commodity : consideredCommodities) {
				for (List<TransportChain> chains : transportDemand.commodity2od2transportChains.get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							episode.setConsolidationUnits(episode.getConsolidationUnits().stream()
									.map(s -> consolidationUnitPattern2representativeUnit
											.get(s.createRoutingEquivalentCopy()))
									.toList());
						}
					}
				}
			}

		} else {
			try {

				/*
				 * Load (routed!) consolidation units.
				 */
				log.info("Loading consolidation units from file " + consolidationUnitsFileName);
				ObjectMapper mapper = new ObjectMapper();
				// TODO >>> can do without this? >>>
				SimpleModule module = (new SimpleModule()).addDeserializer(ConsolidationUnit.class,
						new ConsolidationUnit.Deserializer());
				mapper.registerModule(module);
				// TODO <<< can do without this? <<<
				ObjectReader reader = mapper.readerFor(ConsolidationUnit.class);
				JsonParser parser = mapper.getFactory().createParser(new File(consolidationUnitsFileName));
				Map<ConsolidationUnit, ConsolidationUnit> consolidationUnitPattern2representativeUnit = new LinkedHashMap<>();
				while (parser.nextToken() != null) {
					ConsolidationUnit unit = reader.readValue(parser);
					unit.addNetworkCharacteristics(network);
					consolidationUnitPattern2representativeUnit.put(unit.createRoutingEquivalentCopy(), unit);
				}
				parser.close();

				/*
				 * Attach consolidation units to episodes.
				 */
				log.info("Attaching consolidation units to episodes.");
				for (SamgodsConstants.Commodity commodity : consideredCommodities) {
					log.info("... processing commodity: " + commodity);
					for (List<TransportChain> chains : transportDemand.commodity2od2transportChains.get(commodity)
							.values()) {
						for (TransportChain chain : chains) {
							for (TransportEpisode episode : chain.getEpisodes()) {
								List<ConsolidationUnit> tmpUnits = ConsolidationUnit.createUnrouted(episode);
								List<ConsolidationUnit> recoveredUnits = new ArrayList<>(tmpUnits.size());
								for (ConsolidationUnit tmpUnit : tmpUnits) {
									ConsolidationUnit match = consolidationUnitPattern2representativeUnit
											.get(tmpUnit.createRoutingEquivalentCopy());
									if (match != null) {
										recoveredUnits.add(match);
									} else {
										recoveredUnits = null;
										break;
									}
								}
								episode.setConsolidationUnits(recoveredUnits);
							}
						}
					}
				}

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		/*
		 * REMOVE UNROUTED TRANSPORT CHAINS
		 */
		for (SamgodsConstants.Commodity commodity : consideredCommodities) {
			long removedChainCnt = 0;
			long totalChainCnt = 0;
			for (Map.Entry<OD, List<TransportChain>> entry : transportDemand.commodity2od2transportChains.get(commodity)
					.entrySet()) {
				final int chainCnt = entry.getValue().size();
				totalChainCnt += chainCnt;
				entry.setValue(entry.getValue().stream().filter(c -> c.isRouted()).toList());
				removedChainCnt += chainCnt - entry.getValue().size();
			}
			log.warn(commodity + ": Removed " + removedChainCnt + " out of " + totalChainCnt
					+ " chains with incomplete routes.");
		}

		/*
		 * ITERATIONS
		 */

		Map<ConsolidationUnit, Double> signature2efficiency = new LinkedHashMap<>();
		Map<TransportMode, Double> mode2efficiency = Arrays.stream(TransportMode.values())
				.collect(Collectors.toMap(m -> m, m -> 0.7));

		for (int iteration = 0; iteration < maxIterations; iteration++) {

			double innoWeight = 1.0 / (1.0 + iteration);

			// >>>>> ATTEMPT TO PARALLELIZE CHOICES BELOW >>>>>

			final List<ChainAndShipmentSize> allChoices = Collections.synchronizedList(new ArrayList<>());
			{
				final int threadCnt = Runtime.getRuntime().availableProcessors();
				BlockingQueue<ChoiceJob> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
				List<Thread> choiceThreads = new ArrayList<>();

				log.info("Starting " + threadCnt + " choice simulation threads.");
				try {

					NetworkDataProvider networkDataProvider = new NetworkDataProvider(network, fleet);
					for (int i = 0; i < threadCnt; i++) {
						ConsolidationCostModel consolidationCostModel = new ConsolidationCostModel(
								PerformanceMeasures.createAllZero());
						EpisodeCostModel episodeCostModel = new BasicEpisodeCostModel(fleet, consolidationCostModel,
								mode2efficiency, signature2efficiency, networkDataProvider.createNetworkData());
						NonTransportCostModel nonTransportCostModel = new NonTransportCostModel_v1_22();
						ChainAndShipmentSizeUtilityFunction utilityFunction = new ChainAndShipmentSizeUtilityFunction() {
							@Override
							public double computeUtility(Commodity commodity, double amount_ton,
									DetailedTransportCost transportUnitCost, NonTransportCost totalNonTransportCost) {
								return -transportUnitCost.monetaryCost * amount_ton
										+ nonTransportCostFactor * (-totalNonTransportCost.totalOrderCost
												- totalNonTransportCost.totalEnRouteLoss
												- totalNonTransportCost.totalInventoryCost);
							}
						};
						ChoiceSimulator choiceSimulator = new ChoiceSimulator(scale, episodeCostModel,
								nonTransportCostModel, utilityFunction, jobQueue, allChoices);
						Thread choiceThread = new Thread(choiceSimulator);
						choiceThreads.add(choiceThread);
						choiceThread.start();
					}

					log.info("Starting to populate choice job queue, continuing as threads progress.");
					for (SamgodsConstants.Commodity commodity : consideredCommodities) {
						for (Map.Entry<OD, List<TransportDemand.AnnualShipment>> e : transportDemand.commodity2od2annualShipments
								.get(commodity).entrySet()) {
							final OD od = e.getKey();
							final List<AnnualShipment> annualShipments = e.getValue();
							final List<TransportChain> transportChains = transportDemand.commodity2od2transportChains
									.get(commodity).get(od);
							if (transportChains.size() > 0) {
								jobQueue.put(new ChoiceJob(commodity, od, transportChains, annualShipments));
							} else {
								new InsufficientDataException(TestSamgods.class, "No transport chains available.",
										commodity, od, null, null, null).log();
							}
						}
					}

					log.info("Waiting for choice jobs to complete.");
					for (int i = 0; i < choiceThreads.size(); i++) {
						jobQueue.put(ChoiceJob.TERMINATE);
					}
					for (Thread choiceThread : choiceThreads) {
						choiceThread.join();
					}

				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				log.info("Calculating choice stats.");
				ChainAndShipmentChoiceStats stats = new ChainAndShipmentChoiceStats();
				for (ChainAndShipmentSize choice : allChoices) {
					stats.add(choice);
				}
				log.info("\n" + stats.createChoiceStatsTable());
			}

			/*
			 * 
			 */

			Map<ConsolidationUnit, List<ChainAndShipmentSize>> signature2choices = new LinkedHashMap<>();
			for (ChainAndShipmentSize choice : allChoices) {
				for (TransportEpisode episode : choice.transportChain.getEpisodes()) {
					List<ConsolidationUnit> signatures = episode.getConsolidationUnits();
					for (ConsolidationUnit signature : signatures) {
						signature2choices.computeIfAbsent(signature, s -> new LinkedList<>()).add(choice);
					}
				}
			}
			log.info(allChoices.stream().mapToLong(c -> c.transportChain.getEpisodes().size()).sum() + " episodes.");
			log.info(signature2choices.size() + " episode signatures.");
			log.info(signature2choices.values().stream().flatMap(l -> l.stream())
					.mapToDouble(c -> c.annualShipment.getTotalAmount_ton() / c.sizeClass.getRepresentativeValue_ton())
					.sum() + " shipments.");

			// CONSOLIDATE AND STATISTICS BOKKEEPING

			Map<TransportMode, Double> mode2WeightedEfficiencySum = new LinkedHashMap<>();
			Map<TransportMode, Double> mode2CostSum = new LinkedHashMap<>();
			Map<TransportMode, Double> mode2WeightedUnitCostSum = new LinkedHashMap<>();
			Map<TransportMode, Double> mode2TonKmSum = new LinkedHashMap<>();
			Map<TransportMode, Double> mode2WeightSum = new LinkedHashMap<>();

			Map<VehicleType, Double> vehicleType2WeightSum = new LinkedHashMap<>();

			final Map<Commodity, Map<SamgodsConstants.TransportMode, Double>> commodity2mode2weightedRealizedEfficiencySum = new LinkedHashMap<>();
			final Map<Commodity, Map<SamgodsConstants.TransportMode, Double>> commodity2mode2weightedCostSum = new LinkedHashMap<>();
			final Map<Commodity, Map<SamgodsConstants.TransportMode, Double>> commodity2mode2weightedRealizedUnitCostSum_avgOfRatio = new LinkedHashMap<>();
			final Map<Commodity, Map<SamgodsConstants.TransportMode, Double>> commodity2mode2tonKmSum = new LinkedHashMap<>();
			final Map<Commodity, Map<SamgodsConstants.TransportMode, Double>> commodity2mode2weightSum = new LinkedHashMap<>();

			for (Commodity commodity : Commodity.values()) {
				commodity2mode2weightedRealizedEfficiencySum.put(commodity, new LinkedHashMap<>());
				commodity2mode2weightedCostSum.put(commodity, new LinkedHashMap<>());
				commodity2mode2weightedRealizedUnitCostSum_avgOfRatio.put(commodity, new LinkedHashMap<>());
				commodity2mode2tonKmSum.put(commodity, new LinkedHashMap<>());
				commodity2mode2weightSum.put(commodity, new LinkedHashMap<>());
			}

			NetworkDataProvider routingData = new NetworkDataProvider(network, fleet);
			ConsolidationCostModel consolidationCostModel = new ConsolidationCostModel(
					PerformanceMeasures.createAllZero());
			HalfLoopConsolidator consolidator = new HalfLoopConsolidator(fleet, consolidationCostModel,
					commodity2serviceInterval, routingData.createNetworkData());
			long cnt = 0;
			for (Map.Entry<ConsolidationUnit, List<ChainAndShipmentSize>> e : signature2choices.entrySet()) {
				cnt++;
				if (cnt % 1000 == 0) {
					log.info("Consolidated " + cnt + " out of " + signature2choices.size() + " signatures.");
				}
				try {
//					TransportEpisode episode = episodeSignature2episodes.get(e.getKey()).get(0);

					ConsolidationUnit signature = e.getKey();
//					log.info("CONSOLIDATING signature: " + signature);

					List<ChainAndShipmentSize> shipments = e.getValue();
					double totalDemand_ton = shipments.stream().mapToDouble(s -> s.annualShipment.getTotalAmount_ton())
							.sum();

					if (signature.linkIds == null || signature.linkIds.stream().mapToInt(l -> l.size()).sum() == 0) {
						// log.warn("Skipping episode without links");
					} else if (totalDemand_ton < 0.001) {
						// log.warn("Skipping episode with too small total demand of " + totalDemand_ton + "ton");
					} else {

//						final List<TransportLeg> legs;
//						if (disaggregateRail && episode.getMode().equals(TransportMode.Rail)
//								&& episode.getLegs().size() > 1) {
//							legs = episode.getLegs();
//						} else {
//							legs = Collections.singletonList(null);
//						}
//
//						for (TransportLeg leg : legs) {

						HalfLoopConsolidator.FleetAssignment assignment = consolidator
								.computeOptimalFleetAssignment(signature, shipments);
//						System.out.println("Demand vs supply: " + totalDemand_ton + " vs " + assignment.supply);

						double weight = assignment.snapshotVehicleCnt();

//						double length_km = (leg == null ? episode.getLegs().stream().mapToDouble(l -> l.getLength_km()).sum()
//								: leg.getLength_km());
//						double length_km = 0.001 * signature.links.stream().flatMap(list -> list.stream())
//								.mapToDouble(l -> l.getLength()).sum();

						final double tonKm = totalDemand_ton * 0.5 * assignment.loopLength_km;

						commodity2mode2weightedRealizedEfficiencySum.get(signature.commodity).compute(signature.mode,
								(m, s) -> s == null ? assignment.transportEfficiency() * weight
										: s + assignment.transportEfficiency() * weight);
						commodity2mode2weightedCostSum.get(signature.commodity).compute(signature.mode,
								(m, s) -> s == null ? tonKm * assignment.unitCost_1_tonKm
										: s + tonKm * assignment.unitCost_1_tonKm);
						commodity2mode2weightedRealizedUnitCostSum_avgOfRatio.get(signature.commodity)
								.compute(signature.mode, (m, s) -> s == null ? assignment.unitCost_1_tonKm * weight
										: s + assignment.unitCost_1_tonKm * weight);
						commodity2mode2tonKmSum.get(signature.commodity).compute(signature.mode,
								(m, c) -> c == null ? tonKm : c + tonKm);
						commodity2mode2weightSum.get(signature.commodity).compute(signature.mode,
								(m, c) -> c == null ? weight : c + weight);

						mode2WeightedEfficiencySum.compute(signature.mode,
								(m, s) -> s == null ? assignment.transportEfficiency() * weight
										: s + assignment.transportEfficiency() * weight);
						mode2CostSum.compute(signature.mode, (m, s) -> s == null ? tonKm * assignment.unitCost_1_tonKm
								: s + tonKm * assignment.unitCost_1_tonKm);
						mode2WeightedUnitCostSum.compute(signature.mode,
								(m, s) -> s == null ? assignment.unitCost_1_tonKm * weight
										: s + assignment.unitCost_1_tonKm * weight);
						mode2TonKmSum.compute(signature.mode, (m, s) -> s == null ? tonKm : s + tonKm);
						mode2WeightSum.compute(signature.mode, (m, c) -> c == null ? weight : c + weight);

						vehicleType2WeightSum.compute(assignment.vehicleType,
								(t, c) -> c == null ? weight : c + weight);

//						if (checkUnitCost) {
//							double assignmentCost = assignment.unitCost_1_tonKm * 0.5 * assignment.loopLength_km;
//							double recoveredCost = consolidationCostModel.computeSignatureCost(
//									FreightVehicleAttributes.getFreightAttributes(assignment.vehicleType),
//									assignment.payload_ton, signature, signature.loadAtStart, signature.unloadAtEnd)
//									.computeUnitCost().monetaryCost;
//							assert (Math.abs(assignmentCost - recoveredCost)
//									/ Math.min(assignmentCost, recoveredCost) < 1e-8);
//						}

						assert (assignment.transportEfficiency() >= 0 && assignment.transportEfficiency() <= 1.001);
						signature2efficiency.compute(signature,
								(sig, eff) -> eff == null ? assignment.transportEfficiency()
										: innoWeight * assignment.transportEfficiency() + (1.0 - innoWeight) * eff);
//						}
					}

				} catch (InsufficientDataException e1) {
					e1.log(TestSamgods.class, "during consolidation");
				}
			}

			effLog.log(signature2efficiency.values());
			log.info("efficiency computed for " + signature2efficiency.size() + " signatures.");
			BasicStatistics containerStats = new BasicStatistics();
			BasicStatistics bulkStats = new BasicStatistics();
			for (Map.Entry<ConsolidationUnit, Double> e : signature2efficiency.entrySet()) {
				if (e.getKey().isContainer) {
					containerStats.add(e.getValue());
				} else {
					bulkStats.add(e.getValue());
				}
			}
			log.info("  container efficiency stats: avg = " + containerStats.getAvg() + ", stddev = "
					+ containerStats.getStddev() + ", cnt = " + containerStats.size());
			log.info("  bulk efficiency stats: avg = " + bulkStats.getAvg() + ", stddev = " + bulkStats.getStddev()
					+ ", cnt = " + bulkStats.size());

			final Map<SamgodsConstants.TransportMode, Double> mode2realizedEfficiency = new LinkedHashMap<>();
			final Map<SamgodsConstants.TransportMode, Double> mode2realizedUnitCost_avgOfRatio = new LinkedHashMap<>();
			final Map<SamgodsConstants.TransportMode, Double> mode2realizedUnitCost_ratioOfAvg = new LinkedHashMap<>();

			for (SamgodsConstants.TransportMode mode : mode2WeightSum.keySet()) {

				mode2realizedUnitCost_avgOfRatio.put(mode,
						mode2WeightedUnitCostSum.get(mode) / mode2WeightSum.get(mode));
				mode2realizedUnitCost_ratioOfAvg.put(mode, mode2CostSum.get(mode) / mode2TonKmSum.get(mode));
				mode2realizedEfficiency.put(mode, mode2WeightedEfficiencySum.get(mode) / mode2WeightSum.get(mode));
				mode2efficiency.compute(mode, (m, e) -> e == null ? mode2realizedEfficiency.get(mode)
						: innoWeight * mode2realizedEfficiency.get(mode) + (1.0 - innoWeight) * e);
			}
			logEfficiency(mode2realizedEfficiency, iteration, "efficiency.txt");
			logCost(mode2realizedUnitCost_avgOfRatio, iteration, "unitcost_avg-of-ratio.txt");
			logCost(mode2realizedUnitCost_ratioOfAvg, iteration, "unitcost_ratio-of-avg.txt");
			logFleet(vehicleType2WeightSum, iteration, fleet);

			for (Commodity commodity : consideredCommodities) {
				logEfficiency(
						commodity2mode2weightedRealizedEfficiencySum.get(commodity).entrySet().stream()
								.collect(Collectors.toMap(e -> e.getKey(),
										e -> e.getValue() / commodity2mode2weightSum.get(commodity).get(e.getKey()))),
						iteration, commodity + "_efficiency.txt");
				logCost(commodity2mode2weightedRealizedUnitCostSum_avgOfRatio.get(commodity).entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey(),
								e -> e.getValue() / commodity2mode2weightSum.get(commodity).get(e.getKey()))),
						iteration, commodity + "_unitcost_avg-of-ratio.txt");
				logCost(commodity2mode2weightedCostSum.get(commodity).entrySet().stream()
						.collect(Collectors.toMap(e -> e.getKey(),
								e -> e.getValue() / commodity2mode2tonKmSum.get(commodity).get(e.getKey()))),
						iteration, commodity + "_unitcost_ratio-of-avg.txt");
			}

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

	static void logEfficiency(Map<SamgodsConstants.TransportMode, Double> mode2realizedEfficiency, int iteration,
			String fileName) {
		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				headerLine += "efficiency(" + mode + ")\t";
			}
			try {
				FileUtils.write(new File(fileName), headerLine + "\n", false);
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
			FileUtils.write(new File(fileName), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void logCost(Map<SamgodsConstants.TransportMode, Double> mode2unitCost_1_tonKm, int iteration,
			String fileName) {
		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				headerLine += "unitCost(" + mode + ")[SEK/tonKm]\t";
			}
			try {
				FileUtils.write(new File(fileName), headerLine + "\n", false);
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
			FileUtils.write(new File(fileName), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	static void logFleet(Map<VehicleType, Double> vehType2cnt, int iteration, VehicleFleet fleet) {

		Map<SamgodsConstants.TransportMode, List<VehicleType>> mode2types = new LinkedHashMap<>();
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			mode2types.put(mode,
					fleet.getVehicles().getVehicleTypes().values().stream()
							.filter(t -> mode.equals(FreightVehicleAttributes.getFreightAttributes(t).mode))
							.collect(Collectors.toList()));
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

	static class EfficiencyLogger {

		final String file;

		Double binSize = null;

		EfficiencyLogger(String file) {
			this.file = file;
		}

		void log(Collection<Double> efficiencies) {

			double maxEff = efficiencies.stream().mapToDouble(e -> e).max().getAsDouble();

			if (this.binSize == null) {
				this.binSize = maxEff / 20;
				try {
					FileUtils.write(new File(this.file),
							IntStream.range(0, 25).boxed().map(b -> Double.toString((0.5 + b) * this.binSize))
									.collect(Collectors.joining("\t")) + "\n",
							false);
				} catch (IOException e1) {
					throw new RuntimeException();
				}
			}

			int[] cnt = new int[1 + (int) Math.ceil(maxEff / this.binSize)];
			for (Double eff : efficiencies) {
				cnt[(int) (eff / this.binSize)]++;
			}

			try {
				FileUtils.write(new File(this.file),
						Arrays.stream(cnt).boxed().map(c -> "" + c).collect(Collectors.joining("\t")) + "\n", true);
			} catch (IOException e1) {
				throw new RuntimeException();
			}
		}

	}

}
