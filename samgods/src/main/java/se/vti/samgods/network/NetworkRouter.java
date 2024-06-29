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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkRouter {

	/**
	 * Helper class for parallel routing, operates only on its own members.
	 * 
	 * @author GunnarF
	 *
	 */
	class RoutingThread implements Runnable {

		private final Iterable<TransportChain> chains;

		private final Map<TransportMode, Network> mode2network;
		private final Map<TransportMode, LeastCostPathCalculator> mode2containerRouter;
		private final Map<TransportMode, LeastCostPathCalculator> mode2noContainerRouter;

		RoutingThread(final Iterable<TransportChain> chains, final Map<TransportMode, Network> mode2network,
				final Map<TransportMode, TravelDisutility> mode2containerDisutility,
				final Map<TransportMode, TravelDisutility> mode2noContainerDisutility,
				final Map<TransportMode, TravelTime> mode2containerTravelTime,
				final Map<TransportMode, TravelTime> mode2noContainerTravelTime, final int threads) {

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
							routedLegCnt.addAndGet(1);
							routedLinkCnt.addAndGet(links.size());
//						for (Link link : links) {
//							System.out.print(link.getId() + " ");
//						}
//						System.out.println();
						} else {
							if (from == null) {
								mode2LegRoutingFailures.computeIfAbsent(leg.getMode(), m -> new TreeSet<>())
										.add(leg.getOrigin());
//							System.out.println("NO ORIGIN");
							}
							if (to == null) {
								mode2LegRoutingFailures.computeIfAbsent(leg.getMode(), m -> new TreeSet<>())
										.add(leg.getDestination());
//							System.out.println("NO DESTINATION");
							}
						}
					}
				}
			}
		}
	}

	// TODO only for testing
	public AtomicLong routedLegCnt = new AtomicLong(0);
	public AtomicLong routedLinkCnt = new AtomicLong(0);
	public Map<TransportMode, Set<Id<Node>>> mode2LegRoutingFailures = new ConcurrentHashMap<>();

	private final NetworkRoutingData routingData;

	public NetworkRouter(NetworkRoutingData routingData) {
		this.routingData = routingData;
	}

	public void route(Commodity commodity, Map<OD, List<TransportChain>> od2chains) {

		final int threadCnt = Runtime.getRuntime().availableProcessors();
		final long chainCnt = od2chains.values().stream().flatMap(l -> l.stream()).count();
		final long chainsPerThread = chainCnt / threadCnt;

		final ExecutorService threadPool = Executors.newFixedThreadPool(threadCnt);

		final Iterator<List<TransportChain>> chainListIterator = od2chains.values().iterator();
		for (int thread = 0; thread < threadCnt; thread++) {

			final List<TransportChain> jobs = new LinkedList<>();
			while (chainListIterator.hasNext() && ((jobs.size() < chainsPerThread) || (thread == threadCnt - 1))) {
				jobs.addAll(chainListIterator.next());
			}

			final Map<TransportMode, Network> mode2network = new LinkedHashMap<>();
			final Map<TransportMode, TravelDisutility> mode2containerDisutility = new LinkedHashMap<>();
			final Map<TransportMode, TravelDisutility> mode2noContainerDisutility = new LinkedHashMap<>();
			final Map<TransportMode, TravelTime> mode2containerTravelTime = new LinkedHashMap<>();
			final Map<TransportMode, TravelTime> mode2noContainerTravelTime = new LinkedHashMap<>();
			for (TransportMode mode : SamgodsConstants.TransportMode.values()) {
				final Network network = this.routingData.createNetwork(mode);
				mode2network.put(mode, network);
				mode2containerDisutility.put(mode, this.routingData.createDisutility(commodity, mode, network, true));
				mode2noContainerDisutility.put(mode,
						this.routingData.createDisutility(commodity, mode, network, false));
				mode2containerTravelTime.put(mode, this.routingData.createTravelTime(commodity, mode, network, true));
				mode2noContainerTravelTime.put(mode,
						this.routingData.createTravelTime(commodity, mode, network, false));
			}

			final RoutingThread routingThread = new RoutingThread(jobs, mode2network, mode2containerDisutility,
					mode2noContainerDisutility, mode2containerTravelTime, mode2noContainerTravelTime, threadCnt);
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
}