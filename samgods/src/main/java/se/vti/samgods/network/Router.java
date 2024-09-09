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

import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.VehicleFleet;

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
		final ConsolidationUnit consolidationUnit;

		RoutingJob(ConsolidationUnit consolidationUnit) {
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

		private final Commodity commodity;

		private final Iterable<RoutingJob> jobs;

		private final NetworkData2 networkData;

		private final AStarLandmarksFactory routerFactory = new AStarLandmarksFactory(4);

//		private final Map<TransportMode, LeastCostPathCalculator> mode2containerRouter;
//		private final Map<TransportMode, LeastCostPathCalculator> mode2noContainerRouter;
		private final Map<TransportMode, Map<Boolean, Map<Boolean, LeastCostPathCalculator>>> mode2isContainer2containsFerry2router = new LinkedHashMap<>();

		private LeastCostPathCalculator getRouter(TransportMode mode, boolean isContainer, boolean containsFerry)
				throws InsufficientDataException {
			return this.mode2isContainer2containsFerry2router.computeIfAbsent(mode, m -> new LinkedHashMap<>())
					.computeIfAbsent(isContainer, ic -> new LinkedHashMap<>())
					.put(containsFerry, this.routerFactory.createPathCalculator(
							this.networkData.getUnimodalNetwork(this.networkData.getRepresentativeVehicleType(commodity,
									mode, isContainer, containsFerry)),
							this.networkData.getTravelDisutility(this.commodity, mode, isContainer, containsFerry),
							this.networkData.getTravelTime(this.commodity, mode, isContainer, containsFerry)));
		}

		RoutingThread(String name, Commodity commodity, List<RoutingJob> jobs, NetworkData2 networkData) {

			this.name = name;
			this.commodity = commodity;

			// Contents of this datastructure are iterated over and modified (routes added).
			this.jobs = jobs;

			this.networkData = networkData;

//			this.mode2containerRouter = new LinkedHashMap<>();
//			this.mode2noContainerRouter = new LinkedHashMap<>();
//			this.mode2isContainer2containsFerry2router = new LinkedHashMap<>();
//			final Set<TransportMode> allModes = jobs.stream().map(j -> j.mode()).collect(Collectors.toSet());
//			final AStarLandmarksFactory factory = new AStarLandmarksFactory(4);
//			for (TransportMode mode : allModes) {
//				for (boolean isContainer : Arrays.asList(true, false)) {
//					for (boolean containsFerry : Arrays.asList(true, false)) {
//						try {
//							this.mode2isContainer2containsFerry2router.computeIfAbsent(mode, m -> new LinkedHashMap<>())
//									.computeIfAbsent(isContainer, ic -> new LinkedHashMap<>())
//									.put(containsFerry, factory.createPathCalculator(
//											networkData.getUnimodalNetwork(networkData.getRepresentativeVehicleType(
//													commodity, mode, isContainer, containsFerry)),
//											networkData.getTravelDisutility(commodity, mode, isContainer,
//													containsFerry),
//											networkData.getTravelTime(commodity, mode, isContainer, containsFerry)));
//						} catch (InsufficientDataException e) {
//							e.log(this.getClass(), "Could not create router", commodity, null, mode, true, null);
//						}
//					}
//				}
//			}
		}

		private List<List<Link>> computeRoutes(RoutingJob job, boolean containsFerry) {
			final LeastCostPathCalculator router;
			try {
				router = this.getRouter(job.mode(), job.isContainer(), containsFerry);
			} catch (InsufficientDataException e) {
				return null;
			}

			final Network network = this.networkData.getUnimodalNetwork(this.networkData
					.getRepresentativeVehicleType(commodity, job.mode(), job.isContainer(), containsFerry));
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
					final Node from = network.getNodes().get(fromId);
					final Node to = network.getNodes().get(toId);
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
				return routes;
			} else {
				// "all or nothing"
				return null;
			}
		}

		@Override
		public void run() {
			log.info("THREAD STARTED: " + this.name);
			for (RoutingJob job : this.jobs) {

				final List<List<Link>> withFerryRoutes = this.computeRoutes(job, true);
				final Double withFerryCost;
				final Boolean withFerryContainsFerry;
				if (withFerryRoutes != null) {
					final Map<Id<Link>, BasicTransportCost> linkId2withFerryCost = this.networkData
							.getLinkId2representativeUnitCost(this.commodity, job.mode(), job.isContainer(), true);
					withFerryCost = withFerryRoutes.stream().flatMap(list -> list.stream())
							.mapToDouble(l -> linkId2withFerryCost.get(l.getId()).monetaryCost).sum();
					withFerryContainsFerry = withFerryRoutes.stream().flatMap(list -> list.stream())
							.anyMatch(l -> this.networkData.getFerryLinkIds().contains(l.getId()));
				} else {
					withFerryCost = null;
					withFerryContainsFerry = null;
				}

				if ((withFerryRoutes != null) && !withFerryContainsFerry) {
					// Allowing for ferry yields a route that does not contain ferry.
					job.consolidationUnit.setRoutes(withFerryRoutes);
				} else {

					final List<List<Link>> withoutFerryRoutes = this.computeRoutes(job, false);
					final Double withoutFerryCost;
					if (withoutFerryRoutes != null) {
						Map<Id<Link>, BasicTransportCost> link2withoutFerryCost = this.networkData
								.getLinkId2representativeUnitCost(this.commodity, job.mode(), job.isContainer(), false);
						withoutFerryCost = withoutFerryRoutes.stream().flatMap(list -> list.stream())
								.mapToDouble(l -> link2withoutFerryCost.get(l.getId()).monetaryCost).sum();
					} else {
						withoutFerryCost = null;
					}

					if (withFerryRoutes != null) {
						if ((withoutFerryRoutes != null) && (withoutFerryCost < withFerryCost)) {
							job.consolidationUnit.setRoutes(withoutFerryRoutes);
						} else {
							job.consolidationUnit.setRoutes(withFerryRoutes);
						}
					} else {
						if (withoutFerryRoutes != null) {
							job.consolidationUnit.setRoutes(withoutFerryRoutes);
						} else {
							job.consolidationUnit.setRoutes(null);
						}
					}
				}
			}
			log.info("THREAD ENDED: " + this.name);
		}
	}

	// -------------------- CONSTANTS AND MEMBERS --------------------

	private final Network multimodalNetwork;

	private final VehicleFleet fleet;

	private boolean logProgress = false;

	private int maxThreads = Integer.MAX_VALUE;

	// -------------------- CONSTRUCTION --------------------

	public Router(Network multimodalNetwork, VehicleFleet fleet) {
		this.multimodalNetwork = multimodalNetwork;
		this.fleet = fleet;
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

		final NetworkDataProvider2 networkDataProvider = new NetworkDataProvider2(this.multimodalNetwork, this.fleet);

		for (int thread = 0; thread < threadCnt; thread++) {
			final List<RoutingJob> jobs = new LinkedList<>();
			while (jobIterator.hasNext() && ((jobs.size() < jobsPerThread) || (thread == threadCnt - 1))) {
				jobs.add(jobIterator.next());
			}
			final RoutingThread routingThread = new RoutingThread(commodity + "_" + thread + "_" + jobs.size() + "jobs",
					commodity, jobs, networkDataProvider.createNetworkData());
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