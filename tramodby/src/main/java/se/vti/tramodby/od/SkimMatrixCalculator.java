/**
 * se.vti.tramodby
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
package se.vti.tramodby.od;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;
import org.matsim.vehicles.Vehicle;

import se.vti.tramodby.module.TramodByConfigGroup;
import se.vti.tramodby.od.ZonalSystem.Zone;
import se.vti.utils.misc.math.MathHelpers;

/**
 * This class handles calculation and printing of distance and travel times
 * between origin zones and destination zones.
 * 
 */
public class SkimMatrixCalculator implements ShutdownListener {
	private static final Logger log = LogManager.getLogger(SkimMatrixCalculator.class);
	private static int done = 0;
	private static Integer total = null;
	private static double nextOutput;

	/**
	 * This method prints the progress of calculation and printing.
	 */
	static synchronized void incDone() {
		done++;
		double progress = MathHelpers.round(100.0 * done / total, 3);
		if (progress >= nextOutput) {
			log.info(progress + "% done");
			nextOutput += 5; // Add 5 %.
		}
	}

	/**
	 * This method resets the progress statistics.
	 * 
	 * @param total the total number of output measurements
	 */
	static synchronized void resetCounter(int total) {
		done = 0;
		nextOutput = 5; // 5 %.
		SkimMatrixCalculator.total = total;
	}

	private class LeastCostPathTreeRunner implements Runnable {

		private final double startTime;

		private final int timeBin;

		private final TravelTime linkTravelTimes;

		private final TravelDisutility linkCosts;

		private final Network network;

		private final Id<Zone> originZoneId;
		private final Set<Zone> allToZones;

		private final Node originNode;

		private final List<InterZonalMatrices> skimMatrices;
		private final InterZonalMatrices matrixUpdateCounts;

		private final List<TravelDisutility> evaluationLinkCosts;

		private LeastCostPathTreeRunner(TravelTime linkTravelTimes, TravelDisutility linkCosts, Network network,
				Node originNode, Id<Zone> originZoneId, final double startTime, final int timeBin,
				final Collection<Zone> allToZones, final List<InterZonalMatrices> skimMatrices,
				List<TravelDisutility> evaluationLinkCosts, InterZonalMatrices matrixUpdateCounts) {
			this.startTime = startTime;
			this.timeBin = timeBin;
			this.linkTravelTimes = linkTravelTimes;
			this.linkCosts = linkCosts;
			this.network = network;
			this.originZoneId = originZoneId;
			this.originNode = originNode;
			this.allToZones = new LinkedHashSet<>(allToZones); // i.e. removing duplicates
			this.skimMatrices = skimMatrices;
			this.evaluationLinkCosts = evaluationLinkCosts;

			this.matrixUpdateCounts = matrixUpdateCounts;
		}

		@Override
		public void run() {

			final LeastCostPathTree lcpt = new LeastCostPathTree(this.linkTravelTimes, this.linkCosts);
			lcpt.calculate(this.network, this.originNode, this.startTime);

			final Map<Node, Link> node2inLinks = new LinkedHashMap<>();

			final Map<Node, Set<Link>> node2outLinks = new LinkedHashMap<>();
			for (Map.Entry<Id<Node>, LeastCostPathTree.NodeData> entry : lcpt.getTree().entrySet()) {
				if (entry.getValue().getPrevNodeId() != null) {
					final Node fromNode = this.network.getNodes().get(entry.getValue().getPrevNodeId());
					final Node toNode = this.network.getNodes().get(entry.getKey());
					final Link link = NetworkUtils.getConnectingLink(fromNode, toNode);
					node2outLinks.computeIfAbsent(fromNode, n -> new LinkedHashSet<>()).add(link);
					node2inLinks.put(toNode, link);
				}
			}

			final List<Map<Node, Double>> listOfNode2Cost = new ArrayList<>();
			for (TravelDisutility evaluationLinkCost : this.evaluationLinkCosts) {

				final Map<Node, Double> node2Cost = new LinkedHashMap<>();
				node2Cost.put(this.originNode, 0.0);
				final Set<Node> activeNodes = new LinkedHashSet<>();
				activeNodes.add(this.originNode);
				while (!activeNodes.isEmpty()) {
					final Node fromNode = activeNodes.iterator().next();
					final double entryTime_s = lcpt.getTree().get(fromNode.getId()).getTime();
					activeNodes.remove(fromNode);
					for (Link outLink : node2outLinks.getOrDefault(fromNode, new LinkedHashSet<>())) {
						final Node toNode = outLink.getToNode();
						node2Cost.put(toNode, node2Cost.get(fromNode)
								+ evaluationLinkCost.getLinkTravelDisutility(outLink, entryTime_s, null, null));
						activeNodes.add(toNode);
					}
				}
				listOfNode2Cost.add(node2Cost);

			}

			for (Zone toZone : this.allToZones) {
				for (Id<Link> toLinkId : toZone.getLinkIds()) {
					final Link toLink = this.network.getLinks().get(toLinkId);
					for (int i = 0; i < listOfNode2Cost.size(); i++) {
						final double val = listOfNode2Cost.get(i).get(toLink.getFromNode());
						this.skimMatrices.get(i).addSynchronized(this.originZoneId, toZone.getId(), this.timeBin, val);
					}
					this.matrixUpdateCounts.addSynchronized(this.originZoneId, toZone.getId(), this.timeBin, 1.0);
				}
			}
			incDone();
		}
	}

	private final ZonalSystem zonalSystem;

	private final Map<Id<Zone>, Set<Id<Link>>> zone2sampledLinks = new LinkedHashMap<>();

	private final List<InterZonalMatrices> skimMatrices = new ArrayList<>();

	public SkimMatrixCalculator(final ZonalSystem zonalSystem) {
		this.zonalSystem = zonalSystem;
	}

	class SynchronizedRoadPricingScheme implements RoadPricingScheme {

		private final RoadPricingScheme unsynchronizedRoadpricingScheme;
		private final Set<Id<Link>> synchronizedTolledLinks;

		SynchronizedRoadPricingScheme(RoadPricingScheme parent) {
			this.unsynchronizedRoadpricingScheme = parent;
			this.synchronizedTolledLinks = Collections
					.synchronizedSet(this.unsynchronizedRoadpricingScheme.getTolledLinkIds());
		}

		@Override
		public String getName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getDescription() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<Id<Link>> getTolledLinkIds() {
			return this.synchronizedTolledLinks;
		}

		@Override
		public Cost getLinkCostInfo(Id<Link> linkId, double time, Id<Person> personId, Id<Vehicle> vehicleId) {
			if (!this.synchronizedTolledLinks.contains(linkId)) {
				return null;
			}
			synchronized (this.unsynchronizedRoadpricingScheme) {
				return this.unsynchronizedRoadpricingScheme.getLinkCostInfo(linkId, time, personId, vehicleId);
			}
		}

		@Override
		public Cost getTypicalLinkCostInfo(Id<Link> linkId, double time) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterable<Cost> getTypicalCosts() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<Id<Link>, List<Cost>> getTypicalCostsForLink() {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {

		final RoadPricingScheme unsynchronizedRoadPricingSchemeSingleton = event.getServices().getInjector()
				.getInstance(RoadPricingScheme.class);

		// Accessing objects that is used for calculating and building up output
		// statistics.
		final TramodByConfigGroup tramodByConfig = ConfigUtils.addOrGetModule(event.getServices().getConfig(),
				TramodByConfigGroup.class);

		final ZonalSystemUtils.LinkFromZoneSampler linkSampler = ZonalSystemUtils.createLinkFromZoneSampler(
				this.zonalSystem, event.getServices().getScenario().getNetwork(), MatsimRandom.getRandom());

		// Setting progress indication variables.
		resetCounter(this.zonalSystem.zoneCnt() * tramodByConfig.getBinCount());

		// Creates matrices for output calculations.
		this.skimMatrices.add(new InterZonalMatrices(tramodByConfig));
		this.skimMatrices.add(new InterZonalMatrices(tramodByConfig));
		this.skimMatrices.add(new InterZonalMatrices(tramodByConfig));
		InterZonalMatrices matrixUpdateCounts = new InterZonalMatrices(
				this.skimMatrices.get(0).getTimeDiscretization());

		log.info("Calculating output statistics, progress information will be printed for each 5% interval.");

		for (int timeBin = 0; timeBin < tramodByConfig.getBinCount(); timeBin++) {

			/*
			 * Calculate skim matrices for this time bin.
			 */

			final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

			final int time = tramodByConfig.getStartTime() + tramodByConfig.getBinSize() * timeBin;
			for (Id<Zone> fromZoneId : this.zonalSystem.getAllZones().keySet()) {

				// Creating one new travel time object per thread.
				final TravelTime linkTravelTimes = event.getServices().getLinkTravelTimes();

				final Set<Id<Link>> fromLinkIds = this.zone2sampledLinks.computeIfAbsent(fromZoneId,
						fromZoneId2 -> new LinkedHashSet<>(
								linkSampler.drawLowVariance(fromZoneId, tramodByConfig.getSampledLinksPerZone())));
				for (Id<Link> linkId : fromLinkIds) {
					final List<TravelDisutility> costFunctions = new ArrayList<>();
					// Adds a cost function for calculating the travel time.
					costFunctions.add(new TravelDisutility() {
						@Override
						public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
							return linkTravelTimes.getLinkTravelTime(link, time, person, vehicle);
						}

						@Override
						public double getLinkMinimumTravelDisutility(Link link) {
							return link.getLength() / link.getFreespeed();
						}
					});

					// Adding a cost function for calculating the distance.
					costFunctions.add(new TravelDisutility() {
						@Override
						public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
							return link.getLength();
						}

						@Override
						public double getLinkMinimumTravelDisutility(Link link) {
							return link.getLength();
						}
					});

					// Adding a cost function for calculating the toll.
					costFunctions.add(new TollTravelDisutility(
							new SynchronizedRoadPricingScheme(unsynchronizedRoadPricingSchemeSingleton)));

					// Creates a LeastCostPathTreeRunner to calculate travel time and distance
					// between
					// the origin and destination and sends the object to the thread pool for
					// execution.
					final LeastCostPathTreeRunner matrixUpdater = new LeastCostPathTreeRunner(linkTravelTimes,
							event.getServices().getTravelDisutilityFactory().createTravelDisutility(linkTravelTimes),
							event.getServices().getScenario().getNetwork(),
							event.getServices().getScenario().getNetwork().getLinks().get(linkId).getFromNode(),
							fromZoneId, time, timeBin, this.zonalSystem.getAllZones().values(), this.skimMatrices,
							costFunctions, matrixUpdateCounts);
					threadPool.execute(matrixUpdater);
				}
			}

			threadPool.shutdown();
			try {
				threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			}
			// Aborting if the any of the threads was interrupted.
			catch (InterruptedException e) {
				log.fatal("LeastCostPathTreeRunner was interruped: " + e.getLocalizedMessage());
				Thread.currentThread().interrupt();
				return;
			}

			/*
			 * Write data for this time bin to file.
			 */

			final InterZonalMatrices.Matrix times = this.skimMatrices.get(0).getMatrixListView().get(timeBin);
			final InterZonalMatrices.Matrix distances = this.skimMatrices.get(1).getMatrixListView().get(timeBin);
			final InterZonalMatrices.Matrix tolls = this.skimMatrices.get(2).getMatrixListView().get(timeBin);
			final InterZonalMatrices.Matrix counts = matrixUpdateCounts.getMatrixListView().get(timeBin);

			try {
				final PrintWriter writer = new PrintWriter(tramodByConfig.getCostFileName(timeBin));
				writer.println("From_BSU\tTO_BSU\tTid\tAvstand\tBomBf");
				for (Id<Zone> fromZoneId : this.zonalSystem.getAllZones().keySet()) {
					for (Id<Zone> toZoneId : this.zonalSystem.getAllZones().keySet()) {
						writer.print(fromZoneId);
						writer.print("\t");
						writer.print(toZoneId);
						writer.print("\t");
						final double cnt = counts.get(fromZoneId, toZoneId);
						writer.print(times.get(fromZoneId, toZoneId) / cnt);
						writer.print("\t");
						writer.print(distances.get(fromZoneId, toZoneId) / cnt);
						writer.print("\t");
						writer.println(tolls.get(fromZoneId, toZoneId) / cnt);
					}
				}
				writer.flush();
				writer.close();
			}
			// Aborting if the any of the output files was not accessible.
			catch (FileNotFoundException e) {
				log.fatal("Could not acces output file (" + tramodByConfig.getCostFileName(timeBin) + "): "
						+ e.getLocalizedMessage());
				Thread.currentThread().interrupt();
				return;
			}
			
			/*
			 *  Clear written matrices to save memory.
			 */
			
			times.clear();
			distances.clear();
			tolls.clear();
			counts.clear();
		}

		/* 
		 
		// Printing the output to the cost file for each bin
		log.info("Storing cost files.");
		for (int bin = 0; bin < tramodByConfig.getBinCount(); bin++) {
			final InterZonalMatrices.Matrix times = this.skimMatrices.get(0).getMatrixListView().get(bin);
			final InterZonalMatrices.Matrix distances = this.skimMatrices.get(1).getMatrixListView().get(bin);
			final InterZonalMatrices.Matrix tolls = this.skimMatrices.get(2).getMatrixListView().get(bin);
			final InterZonalMatrices.Matrix counts = matrixUpdateCounts.getMatrixListView().get(bin);

			try {
				final PrintWriter writer = new PrintWriter(tramodByConfig.getCostFileName(bin));
				writer.println("From_BSU\tTO_BSU\tTid\tAvstand\tBomBf");
				for (Id<Zone> fromZoneId : this.zonalSystem.getAllZones().keySet()) {
					for (Id<Zone> toZoneId : this.zonalSystem.getAllZones().keySet()) {
						writer.print(fromZoneId);
						writer.print("\t");
						writer.print(toZoneId);
						writer.print("\t");
						final double cnt = counts.get(fromZoneId, toZoneId);
						writer.print(times.get(fromZoneId, toZoneId) / cnt);
						writer.print("\t");
						writer.print(distances.get(fromZoneId, toZoneId) / cnt);
						writer.print("\t");
						writer.println(tolls.get(fromZoneId, toZoneId) / cnt);
					}
				}
				writer.flush();
				writer.close();
			}
			// Aborting if the any of the output files was not accessible.
			catch (FileNotFoundException e) {
				log.fatal("Could not acces output file (" + tramodByConfig.getCostFileName(bin) + "): "
						+ e.getLocalizedMessage());
				Thread.currentThread().interrupt();
				return;
			}
		}
		
		*/

	}
}
