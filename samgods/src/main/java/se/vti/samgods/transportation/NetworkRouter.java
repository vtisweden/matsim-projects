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
package se.vti.samgods.transportation;

import java.util.Collections;
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportPrices;
import se.vti.samgods.logistics.TransportChain;
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
		private final Map<TransportMode, LeastCostPathCalculator> mode2router;

		RoutingThread(final Iterable<TransportChain> chains, final Network network,
				final Map<TransportMode, TravelDisutility> mode2disutility, final int threads) {

			// Contents of this datastructure are iterated over and modified (routes added).
			this.chains = chains;

			// Local copies of datastructures used in parallel routing.
			this.mode2network = new LinkedHashMap<>(SamgodsConstants.TransportMode.values().length);
			this.mode2router = new LinkedHashMap<>(SamgodsConstants.TransportMode.values().length);
			final AStarLandmarksFactory factory = new AStarLandmarksFactory(threads);
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				final Network unimodalNetwork = this.createUnimodalNetwork(network, mode);
				this.mode2network.put(mode, unimodalNetwork);
				final LeastCostPathCalculator router = factory.createPathCalculator(unimodalNetwork,
						mode2disutility.get(mode), new TravelTime() {
							@Override
							public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
								return 0;
							}
						});
				this.mode2router.put(mode, router);
			}
		}

		private Network createUnimodalNetwork(final Network network, SamgodsConstants.TransportMode samgodsMode) {
			final Set<String> matsimModes = Collections
					.singleton(TransportSupply.samgodsMode2matsimMode.get(samgodsMode));
			final Network unimodalNetwork = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(network).filter(unimodalNetwork, matsimModes);
			return unimodalNetwork;
		}

		@Override
		public void run() {
			for (TransportChain chain : this.chains) {
				for (TransportLeg leg : chain.getLegs()) {
					final Map<Id<Node>, ? extends Node> nodes = this.mode2network.get(leg.getMode()).getNodes();
					final Node from = nodes.get(leg.getOrigin());
					final Node to = nodes.get(leg.getDestination());
					if ((from != null) && (to != null)) {
						List<Link> links = this.mode2router.get(leg.getMode()).calcLeastCostPath(from, to, 0, null,
								null).links;
						leg.setRoute(links);
						routedLegCnt.addAndGet(1);
						routedLinkCnt.addAndGet(links.size());
					} else {
						if (from == null) {
							mode2LegRoutingFailures.computeIfAbsent(leg.getMode(), m -> new TreeSet<>())
									.add(leg.getOrigin());
						}
						if (to == null) {
							mode2LegRoutingFailures.computeIfAbsent(leg.getMode(), m -> new TreeSet<>())
									.add(leg.getDestination());
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

	private final Network network;

	private final TransportPrices prices;

	public NetworkRouter(Network network, TransportPrices prices) {
		this.network = network;
		this.prices = prices;
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

			final Map<TransportMode, TravelDisutility> mode2disutility = new LinkedHashMap<>();
			for (TransportMode mode : SamgodsConstants.TransportMode.values()) {
				TravelDisutility disutility = new TravelDisutility() {
					private final TransportPrices.LinkPrices lp = prices.getLinkPrices(commodity, mode).deepCopy();

					@Override
					public double getLinkMinimumTravelDisutility(Link link) {
						return this.lp.getPrice_1_ton(link);
					}

					@Override
					public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
						return this.getLinkMinimumTravelDisutility(link);
					}
				};
				mode2disutility.put(mode, disutility);
			}

			RoutingThread routingThread = new RoutingThread(jobs, this.network, mode2disutility, threadCnt);
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