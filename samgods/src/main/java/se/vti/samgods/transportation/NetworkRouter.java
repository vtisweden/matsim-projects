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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

	class RoutingThread implements Runnable {

		private final Map<TransportMode, Network> mode2network;
		private final Map<TransportMode, LeastCostPathCalculator> mode2router;
		private final Iterable<TransportChain> chains;

		private long successes = 0;
		private long failures = 0;
		private long linkCnt = 0;

		RoutingThread(Network network, Map<TransportMode, TravelDisutility> mode2disutility,
				Iterable<TransportChain> chains, int threads) {

			final AStarLandmarksFactory factory = new AStarLandmarksFactory(threads);

			this.mode2network = new LinkedHashMap<>(TransportSupply.samgodsMode2matsimMode.size());
			this.mode2router = new LinkedHashMap<>(TransportSupply.samgodsMode2matsimMode.size());
			for (Map.Entry<SamgodsConstants.TransportMode, String> entry : TransportSupply.samgodsMode2matsimMode
					.entrySet()) {
				final Network subNetwork = NetworkUtils.createNetwork();
				new TransportModeNetworkFilter(network).filter(subNetwork, Collections.singleton(entry.getValue()));
				final LeastCostPathCalculator router = factory.createPathCalculator(subNetwork,
						mode2disutility.get(entry.getKey()), new TravelTime() {
							@Override
							public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
								return 0;
							}
						});
				this.mode2network.put(entry.getKey(), subNetwork);
				this.mode2router.put(entry.getKey(), router);
			}
			this.chains = chains;
		}

		@Override
		public void run() {
			for (TransportChain chain : this.chains) {
				for (TransportLeg leg : chain.getLegs()) {
					Map<Id<Node>, ? extends Node> nodes = this.mode2network.get(leg.getMode()).getNodes();
					Node from = nodes.get(leg.getOrigin());
					Node to = nodes.get(leg.getDestination());
					if ((from != null) && (to != null)) {
						List<Link> links =this.mode2router.get(leg.getMode()).calcLeastCostPath(from, to, 0, null, null).links; 
						leg.setRoute(
								links);
						this.successes++;
						this.linkCnt += links.size();
					} else {
						this.failures++;
					}
				}
			}
		}
	}

	private final Network network;

	private final TransportPrices prices;

	private long failures = 0;
	private long successes = 0;
	private long linkCnt = 0;
	
	public long getFailures() {
		return this.failures;
	}
	
	public long getSuccesses() {
		return this.successes;
	}
	
	public long getLinkCnt() {
		return this.linkCnt;
	}
	
	public NetworkRouter(Network network, TransportPrices prices) {
		this.network = network;
		this.prices = prices;
	}

	public void route(Commodity commodity, Map<OD, List<TransportChain>> od2chains) {

		final int threadCnt = Runtime.getRuntime().availableProcessors();
		final long chainCnt = od2chains.values().stream().flatMap(l -> l.stream()).count();
		final long chainsPerThread = chainCnt / threadCnt;

		final ExecutorService threadPool = Executors.newFixedThreadPool(threadCnt);
		final List<RoutingThread> allThreads = new LinkedList<>();
		
		final Iterator<List<TransportChain>> chainListIterator = od2chains.values().iterator();
		for (int thread = 0; thread < threadCnt; thread++) {

			List<TransportChain> jobs = new LinkedList<>();
			while (chainListIterator.hasNext() && ((jobs.size() < chainsPerThread) || (thread == threadCnt - 1))) {
				jobs.addAll(chainListIterator.next());
			}

			Map<TransportMode, TravelDisutility> mode2disutility = new LinkedHashMap<>();
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

			RoutingThread routingThread = new RoutingThread(this.network, mode2disutility, jobs, threadCnt);
			allThreads.add(routingThread);
			threadPool.execute(routingThread);
		}

		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		
		for (RoutingThread terminatedThread : allThreads) {
			this.failures += terminatedThread.failures;
			this.successes += terminatedThread.successes;
			this.linkCnt += terminatedThread.linkCnt;
		}
	}
}