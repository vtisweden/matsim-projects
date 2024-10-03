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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.FleetDataProvider;

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

	private class RouteProcessor implements Runnable {

		private final Logger log = Logger.getLogger(Router.RouteProcessor.class);

		private final String name;

		private final Commodity commodity;

		private final NetworkData networkData;

		private final FleetData fleetData;

		private final AStarLandmarksFactory routerFactory = new AStarLandmarksFactory(4);

		private final Map<TransportMode, Map<Boolean, Map<Boolean, LeastCostPathCalculator>>> mode2isContainer2containsFerry2router = new LinkedHashMap<>();

		private final BlockingQueue<ConsolidationUnit> jobQueue;

		RouteProcessor(String name, Commodity commodity, NetworkData networkData, FleetData fleetData,
				BlockingQueue<ConsolidationUnit> jobQueue) {
			this.name = name;
			this.commodity = commodity;
			this.networkData = networkData;
			this.fleetData = fleetData;
			this.jobQueue = jobQueue;
		}

//		private LeastCostPathCalculator getRouter(TransportMode mode, boolean isContainer, boolean containsFerry)
//				throws InsufficientDataException {
//			final VehicleType representativeVehicleType = this.fleetData.getRepresentativeVehicleType(this.commodity,
//					mode, isContainer, containsFerry);
//			if (representativeVehicleType != null) {
//				final TravelDisutility travelDisutility = this.networkData
//						.getTravelDisutility(representativeVehicleType);
//				final TravelTime travelTime = this.networkData.getTravelTime(representativeVehicleType);
//				return this.mode2isContainer2containsFerry2router.computeIfAbsent(mode, m -> new LinkedHashMap<>())
//						.computeIfAbsent(isContainer, ic -> new LinkedHashMap<>()).computeIfAbsent(containsFerry,
//								cf -> this.routerFactory.createPathCalculator(
//										this.networkData.getUnimodalNetwork(mode, containsFerry), travelDisutility,
//										travelTime));
//			} else {
//				throw new InsufficientDataException(this.getClass(), "no representative vehicle type available",
//						this.commodity, null, mode, isContainer, containsFerry);
//			}
//		}

		private List<List<Link>> computeRoutes(ConsolidationUnit job, boolean containsFerry)
				throws InsufficientDataException {

			final VehicleType representativeVehicleType = this.fleetData.getRepresentativeVehicleType(this.commodity,
					job.samgodsMode, job.isContainer, containsFerry);
			if (representativeVehicleType == null) {
				throw new InsufficientDataException(this.getClass(), "no representative vehicle type available",
						this.commodity, null, job.samgodsMode, job.isContainer, containsFerry);
			}

			final TravelDisutility travelDisutility = this.networkData.getTravelDisutility(representativeVehicleType);
			if (travelDisutility == null) {
				throw new InsufficientDataException(this.getClass(), "no TravelDisutility available", this.commodity,
						null, job.samgodsMode, job.isContainer, containsFerry);
			}
			final TravelTime travelTime = this.networkData.getTravelTime(representativeVehicleType);
			if (travelTime == null) {
				throw new InsufficientDataException(this.getClass(), "no TravelTime available", this.commodity, null,
						job.samgodsMode, job.isContainer, containsFerry);
			}
			final LeastCostPathCalculator router = this.mode2isContainer2containsFerry2router
					.computeIfAbsent(job.samgodsMode, m -> new LinkedHashMap<>())
					.computeIfAbsent(job.isContainer, ic -> new LinkedHashMap<>()).computeIfAbsent(containsFerry,
							cf -> this.routerFactory.createPathCalculator(
									this.networkData.getUnimodalNetwork(job.samgodsMode, containsFerry),
									travelDisutility, travelTime));
			if (router == null) {
				throw new InsufficientDataException(this.getClass(), "could not create router, reason unknown",
						this.commodity, null, job.samgodsMode, job.isContainer, containsFerry);
			}

			final Network network = this.networkData.getUnimodalNetwork(job.samgodsMode, containsFerry);
			final List<List<Link>> routes = new ArrayList<>(job.nodeIds.size() - 1);
			for (int i = 0; i < job.nodeIds.size() - 1; i++) {
				final Id<Node> fromId = job.nodeIds.get(i);
				final Id<Node> toId = job.nodeIds.get(i + 1);
				if (fromId.equals(toId)) {
					routes.add(new ArrayList<>());
					if (logProgress) {
						registerFoundRoute(this);
					}
				} else {
					final Node from = network.getNodes().get(fromId);
					final Node to = network.getNodes().get(toId);
					if ((from != null) && (to != null) && (router != null)) {
						Path path = router.calcLeastCostPath(from, to, 0, null, null);
						List<Link> links = (path == null ? null : path.links);
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
				// Not finding a route does not necessarily mean insufficient data.
				return null;
			}
		}

		private void process(ConsolidationUnit job) {

			List<List<Link>> withFerryRoutes;
			try {
				withFerryRoutes = this.computeRoutes(job, true);
			} catch (InsufficientDataException e) {
				InsufficientDataException.log(e,
						new InsufficientDataException(this.getClass(), "could not find with-ferry routes"));
				withFerryRoutes = null;
			}
			final Double withFerryCost;
			final Boolean withFerryContainsFerry;
			if (withFerryRoutes != null) {
				final Map<Id<Link>, BasicTransportCost> linkId2withFerryUnitCost = this.networkData
						.getLinkId2unitCost(this.fleetData.getRepresentativeVehicleType(this.commodity, job.samgodsMode,
								job.isContainer, true));
				withFerryCost = withFerryRoutes.stream().flatMap(list -> list.stream())
						.mapToDouble(l -> linkId2withFerryUnitCost.get(l.getId()).monetaryCost).sum();
				withFerryContainsFerry = withFerryRoutes.stream().flatMap(list -> list.stream())
						.anyMatch(l -> this.networkData.getFerryLinkIds().contains(l.getId()));
			} else {
				withFerryCost = null;
				withFerryContainsFerry = null;
			}
			if ((withFerryRoutes != null) && !withFerryContainsFerry) {
				job.setRoutes(withFerryRoutes, networkData);
			} else {

				List<List<Link>> withoutFerryRoutes;
				try {
					withoutFerryRoutes = this.computeRoutes(job, false);
				} catch (InsufficientDataException e) {
					InsufficientDataException.log(e,
							new InsufficientDataException(this.getClass(), "could not find without-ferry routes"));
					withoutFerryRoutes = null;
				}
				final Double withoutFerryCost;
				if (withoutFerryRoutes != null) {
					Map<Id<Link>, BasicTransportCost> link2withoutFerryUnitCost = this.networkData
							.getLinkId2unitCost(this.fleetData.getRepresentativeVehicleType(this.commodity,
									job.samgodsMode, job.isContainer, false));
					withoutFerryCost = withoutFerryRoutes.stream().flatMap(list -> list.stream())
							.mapToDouble(l -> link2withoutFerryUnitCost.get(l.getId()).monetaryCost).sum();
				} else {
					withoutFerryCost = null;
				}

				if (withFerryRoutes != null) {
					if ((withoutFerryRoutes != null) && (withoutFerryCost < withFerryCost)) {
						job.setRoutes(withoutFerryRoutes, networkData);
					} else {
						job.setRoutes(withFerryRoutes, networkData);
					}
				} else {
					if (withoutFerryRoutes != null) {
						job.setRoutes(withoutFerryRoutes, networkData);
					} else {
						job.setRoutes(null, networkData);
					}
				}
			}
		}

		@Override
		public void run() {
			log.info("THREAD STARTED: " + this.name);
			try {
				while (true) {
					ConsolidationUnit job = this.jobQueue.take();
					if (job == ConsolidationUnit.TERMINATE) {
						break;
					}
					this.process(job);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.info("ROUTING THREAD ENDED: " + this.name);
		}

	}

	// -------------------- CONSTANTS AND MEMBERS --------------------

	private final NetworkDataProvider networkDataProvider;

	private final FleetDataProvider fleetDataProvider;

	private boolean logProgress = false;

	private int maxThreads = Integer.MAX_VALUE;

	// -------------------- CONSTRUCTION --------------------

	public Router(NetworkDataProvider networkDataProvider, FleetDataProvider fleetDataProvider) {
		this.networkDataProvider = networkDataProvider;
		this.fleetDataProvider = fleetDataProvider;
	}

	public Router setLogProgress(boolean logProgress) {
		this.logProgress = logProgress;
		return this;
	}

	public Router setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void route(Commodity commodity, Iterable<ConsolidationUnit> allJobs) {
		try {
			final int threadCnt = Math.min(this.maxThreads, Runtime.getRuntime().availableProcessors());
			final BlockingQueue<ConsolidationUnit> jobQueue = new LinkedBlockingQueue<>(10 * threadCnt);
			final List<Thread> routingThreads = new ArrayList<>();

			log.info("Starting " + threadCnt + " routing threads.");
			for (int i = 0; i < threadCnt; i++) {
				final RouteProcessor routeProcessor = new RouteProcessor(commodity + "_" + i, commodity,
						this.networkDataProvider.createNetworkData(), this.fleetDataProvider.createFleetData(),
						jobQueue);
				final Thread routingThread = new Thread(routeProcessor);
				routingThreads.add(routingThread);
				routingThread.start();
			}

			log.info("Starting to populate routing job queue, continuing as threads progress.");
			for (ConsolidationUnit job : allJobs) {
				jobQueue.put(job);
			}

			log.info("Waiting for routing jobs to complete.");
			for (int i = 0; i < routingThreads.size(); i++) {
				jobQueue.put(ConsolidationUnit.TERMINATE);
			}
			for (Thread routingThread : routingThreads) {
				routingThread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}