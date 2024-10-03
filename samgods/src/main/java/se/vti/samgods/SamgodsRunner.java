/**
 * se.vti.samgods
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
package se.vti.samgods;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.calibration.FleetCalibrationLogger;
import se.vti.samgods.calibration.FleetCostCalibrator;
import se.vti.samgods.calibration.FleetStatistics;
import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.choice.ChainAndShipmentChoiceStats;
import se.vti.samgods.logistics.choice.ChainAndShipmentSize;
import se.vti.samgods.logistics.choice.ChainAndShipmentSizeUtilityFunction;
import se.vti.samgods.logistics.choice.ChoiceJob;
import se.vti.samgods.logistics.choice.ChoiceJobProcessor;
import se.vti.samgods.logistics.choice.LogisticChoiceData;
import se.vti.samgods.logistics.choice.LogisticChoiceDataProvider;
import se.vti.samgods.logistics.choice.MonetaryChainAndShipmentSizeUtilityFunction;
import se.vti.samgods.logistics.costs.NonTransportCostModel;
import se.vti.samgods.logistics.costs.NonTransportCostModel_v1_22;
import se.vti.samgods.models.TestSamgods;
import se.vti.samgods.network.LinkRegionsReader;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.network.NetworkDataProvider;
import se.vti.samgods.network.NetworkReader;
import se.vti.samgods.network.Router;
import se.vti.samgods.transportation.consolidation.ConsolidationJob;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.FleetDataProvider;
import se.vti.samgods.transportation.fleet.VehiclesReader;

/**
 * TODO public members only while moving this into TestSamgods.
 * 
 * @author GunnarF
 *
 */
public class SamgodsRunner {

	private final static Logger log = Logger.getLogger(SamgodsRunner.class);

	private final long defaultSeed = 4711;

	private final Commodity[] defaultConsideredCommodities = Commodity.values();

	private final int defaultMaxThreads = Integer.MAX_VALUE;

	private final int defautServiceInterval_days = 7;

	private final int defaultMaxIterations = 5;

	private final boolean defaultEnforceReroute = false;

	private final double defaultSamplingRate = 1.0;

	private final Map<Commodity, Double> commodity2scale = new LinkedHashMap<>(
			Arrays.stream(Commodity.values()).collect(Collectors.toMap(c -> c, c -> 1.0)));

	private Random rnd = new Random();

	public List<Commodity> consideredCommodities;

	public int maxThreads;

	private double samplingRate;

	// TODO concurrency?
	public final Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days = new LinkedHashMap<>();

	public int maxIterations;

	public boolean enforceReroute;

	public Vehicles vehicles = null;
	private FleetDataProvider fleetDataProvider = null;

	public Network network = null;
	private NetworkDataProvider networkDataProvider = null;

	private Map<Id<Link>, Double> linkId2domesticWeights = null;

	private TransportDemand transportDemand = null;

	private FleetCostCalibrator fleetCalibrator = null;

	private boolean checkChainConnectivity = false;

	public void setCheckChainConnecivity(boolean checkChainConnectivity) {
		this.checkChainConnectivity = checkChainConnectivity;
	}

	public void setFleetCostCalibrator(FleetCostCalibrator fleetCostCalibrator) {
		this.fleetCalibrator = fleetCostCalibrator;
	}

	public SamgodsRunner() {
		this.setRandomSeed(this.defaultSeed);
		this.setConsideredCommodities(this.defaultConsideredCommodities);
		this.setMaxThreads(this.defaultMaxThreads);
		this.setServiceInterval_days(this.defautServiceInterval_days);
		this.setMaxIterations(this.defaultMaxIterations);
		this.setEnforceReroute(this.defaultEnforceReroute);
		this.setSamplingRate(this.defaultSamplingRate);
	}

	public SamgodsRunner setScale(Commodity commodity, double scale) {
		this.commodity2scale.put(commodity, scale);
		return this;
	}

	public SamgodsRunner setRandomSeed(long seed) {
		this.rnd = new Random(seed);
		return this;
	}

	public SamgodsRunner setConsideredCommodities(Commodity... consideredCommodities) {
		this.consideredCommodities = Arrays.asList(consideredCommodities);
		return this;
	}

	public SamgodsRunner setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	public SamgodsRunner setSamplingRate(double samplingRate) {
		this.samplingRate = samplingRate;
		return this;
	}

	public SamgodsRunner setServiceInterval_days(int serviceInterval_days) {
		Arrays.stream(Commodity.values())
				.forEach(c -> this.commodity2serviceInterval_days.put(c, serviceInterval_days));
		return this;
	}

	public SamgodsRunner setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	public SamgodsRunner setEnforceReroute(boolean enforceReroute) {
		this.enforceReroute = enforceReroute;
		return this;
	}

	public SamgodsRunner loadVehicles(String vehicleParametersFileName, String transferParametersFileName,
			TransportMode samgodsMode, String... excludedIds) throws IOException {
		if (this.vehicles == null) {
			this.vehicles = VehicleUtils.createVehiclesContainer();
		}
		final VehiclesReader fleetReader = new VehiclesReader(this.vehicles);
		for (String excludedId : excludedIds) {
			fleetReader.addExcludedId(excludedId);
		}
		fleetReader.load_v12(vehicleParametersFileName, transferParametersFileName, samgodsMode);
		return this;
	}

	public SamgodsRunner loadNetwork(String nodesFile, String linksFile) throws IOException {
		this.network = new NetworkReader().load(nodesFile, linksFile);
		return this;
	}

	// TODO Needed for NTMCalc.
	public SamgodsRunner loadLinkRegionalWeights(String linkRegionFile) throws IOException {
		this.linkId2domesticWeights = new LinkRegionsReader(this.network).read(linkRegionFile);
		return this;
	}

	public SamgodsRunner loadTransportDemand(String demandFilePrefix, String demandFileSuffix) {
		this.transportDemand = new TransportDemand();
		for (Commodity commodity : this.consideredCommodities) {
			new ChainChoiReader(commodity, transportDemand).setSamplingRate(this.samplingRate, new Random(4711))
					.parse(demandFilePrefix + commodity.twoDigitCode() + demandFileSuffix);
		}
		return this;
	}

	public FleetDataProvider getOrCreateFleetDataProvider() {
		if (this.fleetDataProvider == null) {
			this.fleetDataProvider = new FleetDataProvider(this.vehicles);
		}
		return this.fleetDataProvider;
	}

	public NetworkDataProvider getOrCreateNetworkDataProvider() {
		if (this.networkDataProvider == null) {
			this.networkDataProvider = new NetworkDataProvider(this.network);
		}
		return this.networkDataProvider;
	}

	// -------------------- PREPARE CONSOLIDATION UNITS --------------------

	public void createOrLoadConsolidationUnits(String consolidationUnitsFileName) throws IOException {

		if (this.enforceReroute || !(new File(consolidationUnitsFileName).exists())) {

			/*
			 * Several episodes may have consolidation units with the same routes. To reduce
			 * routing effort, we collect here, for each possible routing configuration, one
			 * representative consolidation unit to be routed. We store a back-links to the
			 * consolidation units attached to the episodes in order to later replace them
			 * by their equivalent routed instances.
			 */
			final Map<ConsolidationUnit, ConsolidationUnit> consolidationUnitPattern2representativeUnit = new LinkedHashMap<>();
			for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
				for (List<TransportChain> chains : this.transportDemand.getCommodity2od2transportChains().get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							episode.setConsolidationUnits(ConsolidationUnit.createUnrouted(episode));
							for (ConsolidationUnit consolidationUnit : episode.getConsolidationUnits()) {
								consolidationUnitPattern2representativeUnit
										.put(consolidationUnit.createRoutingEquivalentTemplate(), consolidationUnit);
							}
						}
					}
				}
			}

			/*
			 * Route (if possible) the representative consolidation units.
			 * 
			 * Routing changes the behavior of hashcode(..) / equals(..) in
			 * ConsolidationUnit, but this should not affect the *values* of a HashMap.
			 */
			for (Commodity commodity : this.consideredCommodities) {
				log.info(commodity + ": Routing consolidation units.");
				Router router = new Router(this.getOrCreateNetworkDataProvider(), this.getOrCreateFleetDataProvider())
						.setLogProgress(true).setMaxThreads(this.maxThreads);
				router.route(commodity, consolidationUnitPattern2representativeUnit.entrySet().stream()
						.filter(e -> commodity.equals(e.getKey().commodity)).map(e -> e.getValue()).toList());
			}

			/*
			 * Stream routed consolidation units to json file.
			 */
			long routedCnt = 0;
			ObjectMapper mapper = new ObjectMapper();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
			FileOutputStream fos = new FileOutputStream(new File(consolidationUnitsFileName));
			JsonGenerator gen = mapper.getFactory().createGenerator(fos);
			for (ConsolidationUnit consolidationUnit : consolidationUnitPattern2representativeUnit.values()) {
				if (consolidationUnit.linkIds != null) {
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
			 * episodes from now on share consolidation units. TransortChain.isRouted only
			 * produces meaningful behavior after this operation is complete.
			 * 
			 * There must not be redundancies in the consolidation unit file.
			 */
			for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
				for (List<TransportChain> chains : this.transportDemand.getCommodity2od2transportChains().get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							List<ConsolidationUnit> templates = new ArrayList<>(episode.getConsolidationUnits().size());
							for (ConsolidationUnit tmpUnit : episode.getConsolidationUnits()) {
								ConsolidationUnit routingEquivalent = tmpUnit.createRoutingEquivalentTemplate();
								ConsolidationUnit template = consolidationUnitPattern2representativeUnit
										.get(routingEquivalent);
								assert (template != null);
								templates.add(template);
							}
							episode.setConsolidationUnits(templates);
						}
					}
				}
			}

		} else {

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
				unit.computeNetworkCharacteristics(this.network,
						this.getOrCreateNetworkDataProvider().createNetworkData());
				consolidationUnitPattern2representativeUnit.put(unit.createRoutingEquivalentTemplate(), unit);
			}
			parser.close();

			/*
			 * Attach representative consolidation units to episodes.
			 */
			log.info("Attaching consolidation units to episodes.");
			for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
				log.info("... processing commodity: " + commodity);
				for (List<TransportChain> chains : this.transportDemand.getCommodity2od2transportChains().get(commodity)
						.values()) {
					for (TransportChain chain : chains) {
						for (TransportEpisode episode : chain.getEpisodes()) {
							List<ConsolidationUnit> tmpUnits = ConsolidationUnit.createUnrouted(episode);
							List<ConsolidationUnit> representativeUnits = new ArrayList<>(tmpUnits.size());
							for (ConsolidationUnit tmpUnit : tmpUnits) {
								ConsolidationUnit match = consolidationUnitPattern2representativeUnit
										.get(tmpUnit.createRoutingEquivalentTemplate());
								if (match != null) {
									representativeUnits.add(match);
								} else {
									representativeUnits = null;
									break;
								}
							}
							episode.setConsolidationUnits(representativeUnits);
						}
					}
				}
			}
		}

		for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
			long removedChainCnt = 0;
			long totalChainCnt = 0;
			for (Map.Entry<OD, List<TransportChain>> entry : this.transportDemand.getCommodity2od2transportChains()
					.get(commodity).entrySet()) {
				final int chainCnt = entry.getValue().size();
				totalChainCnt += chainCnt;
				entry.setValue(entry.getValue().stream().filter(c -> c.isRouted()).toList());
				removedChainCnt += chainCnt - entry.getValue().size();
			}
			log.warn(commodity + ": Removed " + removedChainCnt + " out of " + totalChainCnt
					+ " chains with incomplete routes.");
		}
	}

	public void run() {

		final FleetCalibrationLogger fleetCalibrationLogger = new FleetCalibrationLogger(this.vehicles);
//		final ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedMoveCost = new ConcurrentHashMap<>();

		final Set<ConsolidationUnit> allConsolidationUnits = new LinkedHashSet<>();
		for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
			log.info(commodity + ": Collecting consolidation units.");
			for (List<TransportChain> transportChains : this.transportDemand.getCommodity2od2transportChains()
					.get(commodity).values()) {
				transportChains.stream().flatMap(c -> c.getEpisodes().stream())
						.forEach(e -> allConsolidationUnits.addAll(e.getConsolidationUnits()));
			}
		}

//		{
//			final double initialTransportEfficiency = 0.7; // TODO magic number
//			final NetworkData networkData = this.getOrCreateNetworkDataProvider().createNetworkData();
//			final FleetData fleetData = this.getOrCreateFleetDataProvider().createFleetData();
//			final RealizedInVehicleCost realizedInVehicleCost = new RealizedInVehicleCost();
//			log.info("Computing consolidation costs for " + allConsolidationUnits.size() + " consolidation units");
//			for (ConsolidationUnit consolidationUnit : allConsolidationUnits) {
//				final VehicleType vehicleType = fleetData.getRepresentativeVehicleType(consolidationUnit.commodity,
//						consolidationUnit.samgodsMode, consolidationUnit.isContainer, consolidationUnit.containsFerry);
//				if (vehicleType != null) {
//					final SamgodsVehicleAttributes vehicleAttributes = fleetData.getVehicleType2attributes()
//							.get(vehicleType);
//					try {
//						consolidationUnit2realizedMoveCost.put(consolidationUnit,
//								realizedInVehicleCost.compute(vehicleAttributes,
//										initialTransportEfficiency * vehicleAttributes.capacity_ton, consolidationUnit,
//										networkData.getLinkId2unitCost(vehicleType), networkData.getFerryLinkIds()));
//					} catch (InsufficientDataException e) {
//						log.warn("could not initialize unit cost for consolidation unit " + consolidationUnit);
//					}
//				} else {
//					log.warn("could not initialize unit cost for consolidation unit " + consolidationUnit);
//				}
//			}
//		}

		if (this.checkChainConnectivity) {
			for (Map<OD, List<TransportChain>> od2chain : this.transportDemand.getCommodity2od2transportChains()
					.values()) {
				for (List<TransportChain> chains : od2chain.values()) {
					for (TransportChain chain : chains) {
						if (!chain.isConnected(this.network)) {
							throw new RuntimeException("TODO");
						}
					}
				}
			}
		}

		for (int iteration = 0; iteration < this.maxIterations; iteration++) {
			log.info("STARTING ITERATION " + iteration);

			/*
			 * Simulate choices.
			 */

			final LogisticChoiceDataProvider logisticChoiceDataProvider = new LogisticChoiceDataProvider(
//					consolidationUnit2realizedMoveCost, 
					this.getOrCreateNetworkDataProvider(), this.getOrCreateFleetDataProvider());
			logisticChoiceDataProvider.update(null);

			BlockingQueue<ChainAndShipmentSize> allChoices = new LinkedBlockingQueue<>();
			{
				final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
				BlockingQueue<ChoiceJob> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
				List<Thread> choiceThreads = new ArrayList<>();

				log.info("Starting " + threadCnt + " choice simulation threads.");
				try {
					for (int i = 0; i < threadCnt; i++) {
						final FleetData fleetData = this.getOrCreateFleetDataProvider().createFleetData();
						final NonTransportCostModel nonTransportCostModel = new NonTransportCostModel_v1_22();
						final ChainAndShipmentSizeUtilityFunction utilityFunction = new MonetaryChainAndShipmentSizeUtilityFunction(
								new LinkedHashMap<>(this.commodity2scale), fleetData.getMode2asc(),
								fleetData.getRailCommodity2asc());
						final ChoiceJobProcessor choiceSimulator = new ChoiceJobProcessor(
								logisticChoiceDataProvider.createLogisticChoiceData(), nonTransportCostModel,
								utilityFunction, jobQueue, allChoices);
						final Thread choiceThread = new Thread(choiceSimulator);
						choiceThreads.add(choiceThread);
						choiceThread.start();
					}

					log.info("Starting to populate choice job queue, continuing as threads progress.");
					for (SamgodsConstants.Commodity commodity : this.consideredCommodities) {
						for (Map.Entry<OD, List<AnnualShipment>> e : this.transportDemand
								.getCommodity2od2annualShipments().get(commodity).entrySet()) {
							final OD od = e.getKey();
							final List<AnnualShipment> annualShipments = e.getValue();
							final List<TransportChain> transportChains = this.transportDemand
									.getCommodity2od2transportChains().get(commodity).get(od);
							if (transportChains.size() > 0) {
								jobQueue.put(new ChoiceJob(commodity, od, transportChains, annualShipments));
							} else {
								InsufficientDataException.log(
										new InsufficientDataException(TestSamgods.class,
												"No transport chains available.", commodity, od, null, null, null),
										null);
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
			}

			log.info("Collecting data, calculating choice stats.");
			Map<ConsolidationUnit, List<ChainAndShipmentSize>> consolidationUnit2choices = new LinkedHashMap<>();
			ChainAndShipmentChoiceStats stats = new ChainAndShipmentChoiceStats();
			for (ChainAndShipmentSize choice : allChoices) {
				stats.add(choice);
				for (TransportEpisode episode : choice.transportChain.getEpisodes()) {
					for (ConsolidationUnit consolidationUnit : episode.getConsolidationUnits()) {
						consolidationUnit2choices.computeIfAbsent(consolidationUnit, s -> new LinkedList<>())
								.add(choice);
					}
				}
			}
			log.info(allChoices.stream().mapToLong(c -> c.transportChain.getEpisodes().size()).sum() + " episodes.");
			log.info(consolidationUnit2choices.size() + " episode signatures.");
			log.info(consolidationUnit2choices.values().stream().flatMap(l -> l.stream())
					.mapToDouble(c -> c.annualShipment.getTotalAmount_ton() / c.sizeClass.getRepresentativeValue_ton())
					.sum() + " shipments.");
			log.info("\n" + stats.createChoiceStatsTable());

//			ShipmentPopulationCreator populationCreator = new ShipmentPopulationCreator(this.network);
//			for (Map<OD, List<TransportChain>> od2chains : this.transportDemand.getCommodity2od2transportChains().values()) {
//				for (List<TransportChain> chains : od2chains.values()) {
//					for (TransportChain chain : chains) {
//						populationCreator.add(chain);
//					}
//				}
//			}
//			populationCreator.writeToFile("./input_2024/shipmentPlans.xml");

			/*
			 * Consolidate.
			 */
			final ConcurrentHashMap<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2assignment = new ConcurrentHashMap<>();
			{
				final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
				BlockingQueue<ConsolidationJob> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
				List<Thread> consolidationThreads = new ArrayList<>();

				try {

					log.info("Starting " + threadCnt + " consolidation threads.");
					for (int i = 0; i < threadCnt; i++) {
						NetworkData networkData = this.getOrCreateNetworkDataProvider().createNetworkData();
						FleetData fleetData = this.getOrCreateFleetDataProvider().createFleetData();
						LogisticChoiceData choiceData = logisticChoiceDataProvider.createLogisticChoiceData();
						HalfLoopConsolidationJobProcessor consolidationProcessor = new HalfLoopConsolidationJobProcessor(
								networkData, fleetData, choiceData, jobQueue, consolidationUnit2assignment,
								new LinkedHashMap<>(this.commodity2scale));
						Thread choiceThread = new Thread(consolidationProcessor);
						consolidationThreads.add(choiceThread);
						choiceThread.start();
					}

					log.info("Starting to populate consolidation job queue, continuing as threads progress.");
					for (Map.Entry<ConsolidationUnit, List<ChainAndShipmentSize>> entry : consolidationUnit2choices
							.entrySet()) {
						ConsolidationUnit consolidationUnit = entry.getKey();
						List<ChainAndShipmentSize> choices = entry.getValue();
						if ((choices != null) && (choices.size() > 0)) {
							final double totalDemand_ton = choices.stream()
									.mapToDouble(c -> c.annualShipment.getTotalAmount_ton()).sum();
							if (totalDemand_ton >= 1e-3 && consolidationUnit.length_km >= 1e-3) {
								ConsolidationJob job = new ConsolidationJob(consolidationUnit, choices,
										this.commodity2serviceInterval_days.get(consolidationUnit.commodity));
								jobQueue.put(job);
							}
						} else {
							InsufficientDataException.log(
									new InsufficientDataException(TestSamgods.class, "No transport chains available.",
											consolidationUnit.commodity, null, consolidationUnit.samgodsMode,
											consolidationUnit.isContainer, consolidationUnit.containsFerry),
									null);
						}
					}

					log.info("Waiting for choice jobs to complete.");
					for (int i = 0; i < consolidationThreads.size(); i++) {
						jobQueue.put(ConsolidationJob.TERMINATE);
					}
					for (Thread consolidationThread : consolidationThreads) {
						consolidationThread.join();
					}

				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			/*
			 * POSTPROCESSING, SUMMARY STATISTICS.
			 */
//			log.info("Collecting transport statistics");
//			final TransportationStatistics transpStats = new TransportationStatistics(allChoices,
//					consolidationUnit2assignment, this.getOrCreateNetworkDataProvider().createNetworkData(),
//					this.getOrCreateFleetDataProvider().createFleetData(),
//					logisticChoiceDataProvider.createLogisticChoiceData());
//			statisticsLogger.log(transpStats);

			logisticChoiceDataProvider.update(consolidationUnit2assignment);

			log.info("Collecting fleet statistics");
			final FleetStatistics fleetStats = new FleetStatistics(consolidationUnit2assignment,
					this.getOrCreateNetworkDataProvider().createNetworkData());
			if (this.fleetCalibrator != null) {
				this.fleetCalibrator.updateInternally(fleetStats);
				this.fleetDataProvider.updateAscs(this.fleetCalibrator);
				fleetCalibrationLogger.log(this.fleetCalibrator);
			}

//			log.info("Computing transport efficiency and unit cost per consolidation unit.");
//			{
//				final NetworkData networkData = this.getOrCreateNetworkDataProvider().createNetworkData();
//				final FleetData fleetData = this.getOrCreateFleetDataProvider().createFleetData();
//				final RealizedInVehicleCost realizedInVehicleCost = new RealizedInVehicleCost();
//				for (Map.Entry<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> e : consolidationUnit2assignment
//						.entrySet()) {
//					final ConsolidationUnit consolidationUnit = e.getKey();
//					final FleetAssignment assignment = e.getValue();
//					if (assignment.payload_ton >= 1e-3) {
//						try {
//							final SamgodsVehicleAttributes vehicleAttributes = fleetData.getVehicleType2attributes()
//									.get(assignment.vehicleType);
//							consolidationUnit2realizedMoveCost.put(consolidationUnit,
//									realizedInVehicleCost.compute(vehicleAttributes, assignment.payload_ton,
//											consolidationUnit, networkData.getLinkId2unitCost(assignment.vehicleType),
//											networkData.getFerryLinkIds()));
//						} catch (InsufficientDataException e1) {
//							throw new RuntimeException(e1);
//						}
//					}
//				}
//			}
		}
	}

}
