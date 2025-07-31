/**
 * se.vti.atap.examples.parallel_links
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.examples.parallel_links;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.atap.ATAP;
import org.matsim.contrib.atap.ATAPConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.QSimConfigGroup.InflowCapacitySetting;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

import se.vti.utils.misc.Units;

/**
 * 
 * @author GunnarF
 *
 */
public class ParallelLinkScenarioFactory {

	// -------------------- CONSTANTS --------------------

	private final double bottleneckXSpacing_m = 250.0;

	private final double entryNodeYCoord_m = 0.0;
	private final double divergeNodeYCoord_m = 100.0;
	private final double bottleneckTailYCoord_m = 1100.0;
	private final double bottleneckHeadYCoord_m = 1200.0;
	private final double mergeNodeYCoord_m = 2200.0;
	private final double exitNodeYCoord_m = 2300.0;

	// -------------------- MEMBERS --------------------

	private double freeSpeed_m_s = Units.M_S_PER_KM_H * 100.0; // 100 km/h
	private double giganticCapacity_veh_h = 100_000.0; // such that FD takes effect

	private final Map<Integer, Double> bottleneck2capacity_veh_h = new LinkedHashMap<>();
	private final Map<List<Integer>, Integer> od2demand_veh = new LinkedHashMap<>();
	private final Map<List<Integer>, Double> od2duration_s = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public ParallelLinkScenarioFactory() {
	}

	// -------------------- INTERNALS --------------------

	private Id<Link> linkId(Node tailNode, Node headNode) {
		return Id.createLinkId(tailNode.getId() + "->" + headNode.getId());
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setBottleneck(int bottleneckIndex, double capacity_veh_h) {
		this.bottleneck2capacity_veh_h.put(bottleneckIndex, capacity_veh_h);
	}

	public void setOD(int totalDemand_vehs, double inflowDuration_s, int... linkIndices) {
		List<Integer> od = Arrays.stream(linkIndices).boxed().toList();
		this.od2demand_veh.put(od, totalDemand_vehs);
		this.od2duration_s.put(od, inflowDuration_s);
	}

	public Config buildConfig() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setInflowCapacitySetting(InflowCapacitySetting.INFLOW_FROM_FDIAG);
		config.replanning()
				.addStrategySettings(new StrategySettings().setStrategyName(DefaultStrategy.ReRoute).setWeight(1.0));
		config.scoring().addActivityParams(new ActivityParams("start").setScoringThisActivityAtAll(false));
		config.scoring().addActivityParams(new ActivityParams("end").setScoringThisActivityAtAll(false));
		config.scoring().addModeParams(new ModeParams(TransportMode.car).setMarginalUtilityOfTraveling(-1.0 / 60.0));
		return config;
	}

	public Scenario build(Config config) {

		Scenario scenario = ScenarioUtils.createScenario(config);
		scenario.getNetwork().setCapacityPeriod(3600.0);

		// BOTTLENECKS

		int minIndex = this.bottleneck2capacity_veh_h.keySet().stream().mapToInt(b -> b).min().getAsInt();
		int maxIndex = this.bottleneck2capacity_veh_h.keySet().stream().mapToInt(b -> b).max().getAsInt();
		int bottleneckCnt = this.bottleneck2capacity_veh_h.size();

		if (minIndex != 0 || maxIndex != bottleneckCnt - 1) {
			throw new RuntimeException("Bottlenecks need to correspond to consecutive indices 0,1,2...");
		}

		List<Link> allBottlenecks = new ArrayList<>(bottleneckCnt);
		for (int index = 0; index < bottleneckCnt; index++) {
			double xCoord_m = index * this.bottleneckXSpacing_m;
			Node tail = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("tail" + index),
					new Coord(xCoord_m, this.bottleneckTailYCoord_m));
			Node head = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId("head" + index),
					new Coord(xCoord_m, this.bottleneckHeadYCoord_m));
			allBottlenecks.add(NetworkUtils.createAndAddLink(scenario.getNetwork(), this.linkId(tail, head), tail, head,
					this.bottleneckHeadYCoord_m - this.bottleneckTailYCoord_m, this.freeSpeed_m_s,
					this.bottleneck2capacity_veh_h.get(index), 1.0, null, null));
		}

		// ODs, round 1

		Map<List<Integer>, Node> od2entryNode = new LinkedHashMap<>();
		Map<List<Integer>, Node> od2divergeNode = new LinkedHashMap<>();
		Map<List<Integer>, Node> od2mergeNode = new LinkedHashMap<>();
		Map<List<Integer>, Node> od2exitNode = new LinkedHashMap<>();
		Map<List<Integer>, Link> od2entryLink = new LinkedHashMap<>();
		Map<List<Integer>, Link> od2exitLink = new LinkedHashMap<>();
		double maxApproachTime_s = Double.NEGATIVE_INFINITY;
		for (List<Integer> od : this.od2demand_veh.keySet()) {
			String odName = "OD(" + od.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")) + ")";
			List<? extends Link> usedBottlenecks = od.stream().map(i -> allBottlenecks.get(i)).toList();

			double xCoord_m = usedBottlenecks.stream().mapToDouble(l -> l.getFromNode().getCoord().getX()).average()
					.getAsDouble();
			Node entryNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "entry"),
					new Coord(xCoord_m, this.entryNodeYCoord_m));
			Node divergeNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "diverge"),
					new Coord(xCoord_m, this.divergeNodeYCoord_m));
			Node mergeNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "merge"),
					new Coord(xCoord_m, this.mergeNodeYCoord_m));
			Node exitNode = NetworkUtils.createAndAddNode(scenario.getNetwork(), Id.createNodeId(odName + "exit"),
					new Coord(xCoord_m, this.exitNodeYCoord_m));
			od2entryNode.put(od, entryNode);
			od2divergeNode.put(od, divergeNode);
			od2mergeNode.put(od, mergeNode);
			od2exitNode.put(od, exitNode);

			Link entryLink = NetworkUtils.createAndAddLink(scenario.getNetwork(), this.linkId(entryNode, divergeNode),
					entryNode, divergeNode,
					NetworkUtils.getEuclideanDistance(entryNode.getCoord(), divergeNode.getCoord()), this.freeSpeed_m_s,
					this.giganticCapacity_veh_h, 1, null, null);
			Link exitLink = NetworkUtils.createAndAddLink(scenario.getNetwork(), this.linkId(mergeNode, exitNode),
					mergeNode, exitNode, NetworkUtils.getEuclideanDistance(mergeNode.getCoord(), exitNode.getCoord()),
					this.freeSpeed_m_s, this.giganticCapacity_veh_h, 1, null, null);
			od2entryLink.put(od, entryLink);
			od2exitLink.put(od, exitLink);

			for (Link bottleneck : usedBottlenecks) {
				double approachDist_m = NetworkUtils.getEuclideanDistance(divergeNode.getCoord(),
						bottleneck.getFromNode().getCoord());
				maxApproachTime_s = Math.max(maxApproachTime_s, approachDist_m / this.freeSpeed_m_s);
			}
		}
		
		// ODs, round 2

		for (List<Integer> od : this.od2demand_veh.keySet()) {
			String odName = "OD(" + od.stream().map(i -> Integer.toString(i)).collect(Collectors.joining(",")) + ")";
			List<? extends Link> usedBottlenecks = od.stream().map(i -> allBottlenecks.get(i)).toList();

			Node divergeNode = od2divergeNode.get(od);
			Node mergeNode = od2mergeNode.get(od);

			List<Link> divergeLinks = new ArrayList<>(od.size());
			List<Link> mergeLinks = new ArrayList<>(od.size());
			for (Link bottleneck : usedBottlenecks) {
				double approachDist_m = NetworkUtils.getEuclideanDistance(divergeNode.getCoord(),
						bottleneck.getFromNode().getCoord());
				double approachSpeed_m_s = approachDist_m / maxApproachTime_s;
				divergeLinks.add(NetworkUtils.createAndAddLink(scenario.getNetwork(),
						this.linkId(divergeNode, bottleneck.getFromNode()), divergeNode, bottleneck.getFromNode(),
						NetworkUtils.getEuclideanDistance(divergeNode.getCoord(), bottleneck.getFromNode().getCoord()),
						Units.M_S_PER_KM_H * approachSpeed_m_s, this.giganticCapacity_veh_h, 1, null, null));
				mergeLinks.add(NetworkUtils.createAndAddLink(scenario.getNetwork(),
						this.linkId(bottleneck.getToNode(), mergeNode), bottleneck.getToNode(), mergeNode,
						NetworkUtils.getEuclideanDistance(bottleneck.getToNode().getCoord(), mergeNode.getCoord()),
						Units.M_S_PER_KM_H * approachSpeed_m_s, this.giganticCapacity_veh_h, 1, null, null));
			}

			PopulationFactory factory = scenario.getPopulation().getFactory();

			Id<Link> entryLinkId = od2entryLink.get(od).getId();
			Id<Link> routeLinkId1 = divergeLinks.get(0).getId();
			Id<Link> routeLinkId2 = usedBottlenecks.get(0).getId();
			Id<Link> routeLinkId3 = mergeLinks.get(0).getId();
			Id<Link> exitLinkId = od2exitLink.get(od).getId();

			double delta_s = this.od2duration_s.get(od) / this.od2demand_veh.get(od);
			double time_s = 0.0;
			for (int n = 0; n < this.od2demand_veh.get(od); n++) {
				Plan plan = factory.createPlan();
				Activity start = factory.createActivityFromLinkId("start", entryLinkId);
				start.setEndTime(time_s);
				plan.addActivity(start);
				Leg leg = factory.createLeg(TransportMode.car);
				leg.setDepartureTime(time_s);
				Route route = RouteUtils.createLinkNetworkRouteImpl(entryLinkId,
						Arrays.asList(routeLinkId1, routeLinkId2, routeLinkId3), exitLinkId);
				leg.setRoute(route);
				plan.addLeg(leg);
				plan.addActivity(factory.createActivityFromLinkId("end", exitLinkId));
				Person person = factory.createPerson(Id.createPersonId(odName + "_" + n));
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
				time_s += delta_s;
			}
		}

		return scenario;
	}

	// -------------------- MAIN-FUNCTION, FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		double inflowDuration_s = 1800.0;
		ParallelLinkScenarioFactory factory = new ParallelLinkScenarioFactory();

		factory.setBottleneck(0, 500.0);
		factory.setBottleneck(1, 500.0);
		factory.setBottleneck(2, 500.0);
		factory.setBottleneck(3, 500.0);
		factory.setBottleneck(4, 500.0);

		factory.setOD(1000, inflowDuration_s, 0, 1, 2);
		factory.setOD(1000, inflowDuration_s, 1, 2, 3);
		factory.setOD(1000, inflowDuration_s, 2, 3, 4);

		Config config = factory.buildConfig();
		config.network().setInputFile("network_parallel_links.xml");
		config.plans().setInputFile("population_parallel_links.xml");
		
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		config.addModule(atapConfig);
		config.controller().setLastIteration(100);
		ConfigUtils.writeMinimalConfig(config, "config_parallel_links.xml");

		Scenario scenario = factory.build(config);
		new NetworkWriter(scenario.getNetwork()).write(scenario.getConfig().network().getInputFile());
		new PopulationWriter(scenario.getPopulation()).write(scenario.getConfig().plans().getInputFile());

		ATAP atap = new ATAP();
		atap.meet(scenario.getConfig());
		Controler controler = new Controler(scenario);
		atap.meet(controler);
		controler.run();

		System.out.println("... DONE");
	}
}
