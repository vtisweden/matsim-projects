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

import java.util.ArrayList;
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.Signature.ConsolidationUnit;

/**
 * 
 * @author GunnarF
 *
 */
public class Router {

	// -------------------- LOGGING, ONLY FOR TESTING --------------------

	private static final Logger log = Logger.getLogger(Router.class);

	private long lastUpdate_ms = System.currentTimeMillis();

	private Set<Runnable> recentlyActiveThreads = new LinkedHashSet<>();

	private long foundRoutes = 0;
	private long failedRoutesNoOD = 0;
	private long failedRoutesNoConnection = 0;
	private long failedRoutesNoRouter = 0;

	private void msgOrNot() {
		if (System.currentTimeMillis() - this.lastUpdate_ms > 500) {
			log.info("found " + this.foundRoutes + ", failed: " + this.failedRoutesNoOD + " (no OD), "
					+ this.failedRoutesNoConnection + " (no connection), " + this.failedRoutesNoRouter + " (no router)"
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

	private synchronized void registerFailedRouteNoOD(Runnable thread) {
		this.msgOrNot();
		this.failedRoutesNoOD++;
		this.recentlyActiveThreads.add(thread);
	}

	private synchronized void registerFailedRouteNoConnection(Runnable thread) {
		this.msgOrNot();
		this.failedRoutesNoConnection++;
		this.recentlyActiveThreads.add(thread);
	}

	private synchronized void registerFailedRouteNoRouter(Runnable thread) {
		this.msgOrNot();
		this.failedRoutesNoRouter++;
		this.recentlyActiveThreads.add(thread);
	}

	// -------------------- INNER CLASSES --------------------

	/**
	 * Helper class to avoid that parallel routing gets problems with
	 * non-synchronized leg/episode/chain references.
	 * 
	 * @author GunnarF
	 *
	 */
	private class RoutingJob {
//		final boolean isContainer;
//		final SamgodsConstants.TransportMode mode;
		final ConsolidationUnit consolidationUnit;

		RoutingJob(ConsolidationUnit consolidationUnit) {
//			this.isContainer = consolidationUnit.isContainer;
//			this.mode = consolidationUnit.mode;
			this.consolidationUnit = consolidationUnit;
		}

		boolean isContainer() {
			return this.consolidationUnit.isContainer;
		}

		TransportMode mode() {
			return this.consolidationUnit.mode;
		}
	}

	/**
	 * Helper class for parallel routing, operates only on its own members.
	 * 
	 * @author GunnarF
	 *
	 */
	private class RoutingThread implements Runnable {

		private final Logger log = Logger.getLogger(Router.RoutingThread.class);

		private final String name;

		private final Iterable<RoutingJob> jobs;

		private final Map<TransportMode, Network> mode2network;
		private final Map<TransportMode, LeastCostPathCalculator> mode2containerRouter;
		private final Map<TransportMode, LeastCostPathCalculator> mode2noContainerRouter;

		RoutingThread(String name, final Iterable<RoutingJob> jobs, final Map<TransportMode, Network> mode2network,
				final Map<TransportMode, TravelDisutility> mode2containerDisutility,
				final Map<TransportMode, TravelDisutility> mode2noContainerDisutility,
				final Map<TransportMode, TravelTime> mode2containerTravelTime,
				final Map<TransportMode, TravelTime> mode2noContainerTravelTime) {

			this.name = name;

			// Contents of this datastructure are iterated over and modified (routes added).
			this.jobs = jobs;

			// Local copies of datastructures used in parallel routing.
			this.mode2network = mode2network;
			this.mode2containerRouter = new LinkedHashMap<>(mode2network.size());
			this.mode2noContainerRouter = new LinkedHashMap<>(mode2network.size());
			final AStarLandmarksFactory factory = new AStarLandmarksFactory(4); // TODO
			for (Map.Entry<SamgodsConstants.TransportMode, Network> e : mode2network.entrySet()) {
				final TransportMode mode = e.getKey();
				final Network unimodalNetwork = e.getValue();
				if (mode2containerDisutility.containsKey(mode) && mode2containerTravelTime.containsKey(mode)) {
					this.mode2containerRouter.put(mode, factory.createPathCalculator(unimodalNetwork,
							mode2containerDisutility.get(mode), mode2containerTravelTime.get(mode)));
				}
				if (mode2noContainerDisutility.containsKey(mode) && mode2noContainerTravelTime.containsKey(mode)) {
					this.mode2noContainerRouter.put(mode, factory.createPathCalculator(unimodalNetwork,
							mode2noContainerDisutility.get(mode), mode2noContainerTravelTime.get(mode)));
				}
			}
		}

		@Override
		public void run() {
			log.info("THREAD STARTED: " + this.name);
			for (RoutingJob job : this.jobs) {

				List<List<Link>> routes = new ArrayList<>(job.consolidationUnit.nodeIds.size() - 1);
				for (int i = 0; i < job.consolidationUnit.nodeIds.size() - 1; i++) {
					Id<Node> fromId = job.consolidationUnit.nodeIds.get(i);
					Id<Node> toId = job.consolidationUnit.nodeIds.get(i + 1);

					if (fromId.equals(toId)) {
						routes.add(new ArrayList<>());
						if (logProgress) {
							registerFoundRoute(this);
						}
					} else {
						final LeastCostPathCalculator router = job.isContainer()
								? this.mode2containerRouter.get(job.mode())
								: this.mode2noContainerRouter.get(job.mode());
						final Map<Id<Node>, ? extends Node> nodes = this.mode2network.get(job.mode()).getNodes();
						final Node from = nodes.get(fromId);
						final Node to = nodes.get(toId);
						if ((from != null) && (to != null) && (router != null)) {
							List<Link> links = router.calcLeastCostPath(from, to, 0, null, null).links; 
							routes.add(links);
							if (links == null) {
								if (logProgress) {
									registerFailedRouteNoConnection(this);
								}
							} else if (logProgress) {
								registerFoundRoute(this);
							}
						} else {
							routes.add(null);
							if (logProgress) {
								if (from == null || to == null) {
									registerFailedRouteNoOD(this);
								} else if (router == null) {
									registerFailedRouteNoRouter(this);
								} else {
									throw new RuntimeException("impossible");
								}
							}
						}
					}

				}
				if (routes.stream().noneMatch(r -> r == null)) {
					job.consolidationUnit.setRoutes(routes);
				}
			}
			log.info("THREAD ENDED: " + this.name);
		}
	}

	// -------------------- CONSTANTS AND MEMBERS --------------------

	private final RoutingData routingData;

	private boolean logProgress = false;

	private int maxThreads = Integer.MAX_VALUE;

	// -------------------- CONSTRUCTION --------------------

	public Router(RoutingData routingData) {
		this.routingData = routingData;
	}

	public Router setLogProgress(boolean logProgress) {
		this.logProgress = logProgress;
		return this;
	}

	public Router setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	// -------------------- INTERNALS --------------------

	private void routeInternally(Commodity commodity, List<RoutingJob> allJobs) {

		final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
		final long jobsPerThread = allJobs.size() / threadCnt;

		final ExecutorService threadPool = Executors.newFixedThreadPool(threadCnt);
		final Iterator<RoutingJob> jobIterator = allJobs.iterator();

		for (int thread = 0; thread < threadCnt; thread++) {

			final List<RoutingJob> jobs = new LinkedList<>();
			while (jobIterator.hasNext() && ((jobs.size() < jobsPerThread) || (thread == threadCnt - 1))) {
				jobs.add(jobIterator.next());
			}

			final Map<TransportMode, Network> mode2network = new LinkedHashMap<>();
			final Map<TransportMode, TravelDisutility> mode2containerDisutility = new LinkedHashMap<>();
			final Map<TransportMode, TravelDisutility> mode2noContainerDisutility = new LinkedHashMap<>();
			final Map<TransportMode, TravelTime> mode2containerTravelTime = new LinkedHashMap<>();
			final Map<TransportMode, TravelTime> mode2noContainerTravelTime = new LinkedHashMap<>();
			for (TransportMode mode : SamgodsConstants.TransportMode.values()) {
				if (!mode.isFerry()) {
					final Network network = this.routingData.createNetwork(mode);
					try {
						this.routingData.createNetworkData(commodity, mode, network, true);
						mode2containerDisutility.put(mode, this.routingData.getAndClearDisutility());
						mode2containerTravelTime.put(mode, this.routingData.getAndClearTravelTime());
						mode2network.put(mode, network);
					} catch (InsufficientDataException e) {
						e.log(this.getClass(), "No travel disutility.", commodity, null, mode, true, null);
					}
					try {
						this.routingData.createNetworkData(commodity, mode, network, false);
						mode2noContainerDisutility.put(mode, this.routingData.getAndClearDisutility());
						mode2noContainerTravelTime.put(mode, this.routingData.getAndClearTravelTime());
						mode2network.put(mode, network);
					} catch (InsufficientDataException e) {
						e.log(this.getClass(), "No travel disutility.", commodity, null, mode, false, null);
					}
				}
			}

			final RoutingThread routingThread = new RoutingThread(commodity + "_" + thread + "_" + jobs.size() + "jobs",
					jobs, mode2network, mode2containerDisutility, mode2noContainerDisutility, mode2containerTravelTime,
					mode2noContainerTravelTime);
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

	// -------------------- IMPLEMENTATION --------------------

	public void route(Commodity commodity, List<ConsolidationUnit> consolidationUnits) {
		final List<RoutingJob> allJobs = consolidationUnits.stream().map(c -> new RoutingJob(c)).toList();
		this.routeInternally(commodity, allJobs);
	}
}