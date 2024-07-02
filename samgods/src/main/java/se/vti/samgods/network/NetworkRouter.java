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
package se.vti.samgods.network;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
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
public class NetworkRouter {

	private long lastUpdate_ms = System.currentTimeMillis();

	private Set<Runnable> recentlyActiveThreads = new LinkedHashSet<>();

	private long foundRoutes = 0;
	private long failedRoutes = 0;

	private void msgOrNot() {
		if (System.currentTimeMillis() - this.lastUpdate_ms > 5000) {
			Logger.getLogger(this.getClass()).info("found " + this.foundRoutes + ", failed " + this.failedRoutes
					+ ", active threads: " + this.recentlyActiveThreads.size());
			this.lastUpdate_ms = System.currentTimeMillis();
			this.recentlyActiveThreads.clear();
		}
	}

	private synchronized void registerFoundRoute(Runnable thread) {
		this.msgOrNot();
		this.foundRoutes++;
		this.recentlyActiveThreads.add(thread);
	}

	private synchronized void registerFailedRoute(Runnable thread) {
		this.msgOrNot();
		this.failedRoutes++;
		this.recentlyActiveThreads.add(thread);
	}

	/**
	 * Helper class for parallel routing, operates only on its own members.
	 * 
	 * @author GunnarF
	 *
	 */
	class RoutingThread implements Runnable {

		private final String name;

		private final Iterable<TransportChain> chains;

		private final Map<TransportMode, Network> mode2network;
		private final Map<TransportMode, LeastCostPathCalculator> mode2containerRouter;
		private final Map<TransportMode, LeastCostPathCalculator> mode2noContainerRouter;

		RoutingThread(String name, final Iterable<TransportChain> chains,
				final Map<TransportMode, Network> mode2network,
				final Map<TransportMode, TravelDisutility> mode2containerDisutility,
				final Map<TransportMode, TravelDisutility> mode2noContainerDisutility,
				final Map<TransportMode, TravelTime> mode2containerTravelTime,
				final Map<TransportMode, TravelTime> mode2noContainerTravelTime, final int threads) {

			this.name = name;

			// Contents of this datastructure are iterated over and modified (routes added).
			this.chains = chains;

			// Local copies of datastructures used in parallel routing.
			this.mode2network = mode2network;
			this.mode2containerRouter = new LinkedHashMap<>(mode2network.size());
			this.mode2noContainerRouter = new LinkedHashMap<>(mode2network.size());
			final AStarLandmarksFactory factory = new AStarLandmarksFactory(threads);
			for (Map.Entry<SamgodsConstants.TransportMode, Network> e : mode2network.entrySet()) {
				final TransportMode mode = e.getKey();
				final Network unimodalNetwork = e.getValue();
				this.mode2containerRouter.put(mode, factory.createPathCalculator(unimodalNetwork,
						mode2containerDisutility.get(mode), mode2containerTravelTime.get(mode)));
				this.mode2noContainerRouter.put(mode, factory.createPathCalculator(unimodalNetwork,
						mode2noContainerDisutility.get(mode), mode2noContainerTravelTime.get(mode)));
			}
		}

		@Override
		public void run() {

			Logger.getLogger(this.getClass()).info("THREAD STARTED: " + this.name);

			for (TransportChain chain : this.chains) {
				for (TransportEpisode episode : chain.getEpisodes()) {
					for (TransportLeg leg : episode.getLegs()) {
						Map<Id<Node>, ? extends Node> nodes;
						try {
							nodes = this.mode2network.get(leg.getMode()).getNodes();
						} catch (Exception e) {
							e.printStackTrace();
							System.exit(0);
							nodes = null;
						}
						final Node from = nodes.get(leg.getOrigin());
						final Node to = nodes.get(leg.getDestination());
						if ((from != null) && (to != null)) {
							final LeastCostPathCalculator router = episode.isContainer()
									? this.mode2containerRouter.get(leg.getMode())
									: this.mode2noContainerRouter.get(leg.getMode());
							final List<Link> links = router.calcLeastCostPath(from, to, 0, null, null).links;
							leg.setRoute(links);
							if (logProgress) {
								if ((links == null) && !(from == to)) {
									registerFailedRoute(this);
								} else {
									registerFoundRoute(this);

								}
							}
						} else {
							if (logProgress) {
								registerFailedRoute(this);
							}
						}
					}
				}
			}
			Logger.getLogger(this.getClass()).info("THREAD ENDED: " + this.name);
		}
	}

	// TODO only for testing
	private final int maxThreads = Integer.MAX_VALUE;

	private final NetworkRoutingData routingData;

	private boolean logProgress = false;

	public NetworkRouter(NetworkRoutingData routingData) {
		this.routingData = routingData;
	}

	public NetworkRouter setLogProgress(boolean logProgress) {
		this.logProgress = logProgress;
		return this;
	}

	public void route(Commodity commodity, Map<OD, List<TransportChain>> od2chains) {

		final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());

		final List<TransportChain> allJobs = od2chains.values().stream().flatMap(l -> l.stream())
				.collect(Collectors.toList());
		Collections.shuffle(allJobs);
		final long totalLegCnt = allJobs.stream().mapToLong(c -> c.getLegCnt()).sum();
		final long legsPerThread = totalLegCnt / threadCnt;

		final ExecutorService threadPool = Executors.newFixedThreadPool(threadCnt);

		final Iterator<TransportChain> jobIterator = allJobs.iterator();

		for (int thread = 0; thread < threadCnt; thread++) {

			long jobLegCnt = 0;
			final List<TransportChain> jobs = new LinkedList<>();
			while (jobIterator.hasNext() && ((jobLegCnt < legsPerThread) || (thread == threadCnt - 1))) {
				final TransportChain job = jobIterator.next();
				jobLegCnt += job.getLegCnt();
				jobs.add(job);
			}

			final Map<TransportMode, Network> mode2network = new LinkedHashMap<>();
			final Map<TransportMode, TravelDisutility> mode2containerDisutility = new LinkedHashMap<>();
			final Map<TransportMode, TravelDisutility> mode2noContainerDisutility = new LinkedHashMap<>();
			final Map<TransportMode, TravelTime> mode2containerTravelTime = new LinkedHashMap<>();
			final Map<TransportMode, TravelTime> mode2noContainerTravelTime = new LinkedHashMap<>();
			for (TransportMode mode : SamgodsConstants.TransportMode.values()) {
				if (!SamgodsConstants.TransportMode.Ferry.equals(mode)) {
					final Network network = this.routingData.createNetwork(mode);
					mode2network.put(mode, network);
					mode2containerDisutility.put(mode,
							this.routingData.createDisutility(commodity, mode, network, true));
					mode2noContainerDisutility.put(mode,
							this.routingData.createDisutility(commodity, mode, network, false));
					mode2containerTravelTime.put(mode,
							this.routingData.createTravelTime(commodity, mode, network, true));
					mode2noContainerTravelTime.put(mode,
							this.routingData.createTravelTime(commodity, mode, network, false));
				}
			}

			final RoutingThread routingThread = new RoutingThread(
					commodity + "_" + thread + "_" + jobs.stream().mapToLong(c -> c.getLegCnt()).sum() + "jobs", jobs,
					mode2network, mode2containerDisutility, mode2noContainerDisutility, mode2containerTravelTime,
					mode2noContainerTravelTime, threadCnt);
			threadPool.execute(routingThread);
		}

		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

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

		NetworkRoutingData routingData = new NetworkRoutingData(network, grouping, empiricalEpisodeCostModel,
				fallbackEpisodeCostModel);
		NetworkRouter router = new NetworkRouter(routingData).setLogProgress(false);

		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
			ChainChoiReader commodityReader = new ChainChoiReader(commodity).setStoreSamgodsShipments(false)
//					.setSamplingRate(1e-1, new Random(4711))
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
//			for (List<TransportChain> chains : commodityReader.getOD2transportChains().values()) {
//				for (TransportChain chain : chains) {
//					System.out.println("  " + chain.getRoutesView());
//				}
//			}
		}

		System.out.println("... DONE");
	}
}