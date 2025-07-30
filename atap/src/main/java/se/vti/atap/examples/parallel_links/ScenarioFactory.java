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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;

import se.vti.utils.misc.Units;

/**
 * 
 * @author GunnarF
 *
 */
public class ScenarioFactory {

	private final int linkCnt;
	private final double linkLength_m;
	private final double linkDistance_m;
	private final double inflowDuration_s;

	private final double originNodeYCoord_m;
	private final double linkFromYCoord_m;
	private final double linkToYCoord_m;
	private final double destinationNodeYCoord_m;

	private Double warmupTime_s = null;

	private final Scenario scenario;

	public ScenarioFactory(Scenario scenario, int linkCnt, double linkLength_m, double inflowDuration_s) {

		this.scenario = scenario;
		scenario.getNetwork().setCapacityPeriod(3600.0);

		this.linkCnt = linkCnt;
		this.linkLength_m = linkLength_m;
		this.linkDistance_m = linkLength_m / (linkCnt - 1.0);
		this.inflowDuration_s = inflowDuration_s;

		this.originNodeYCoord_m = 0.0;
		this.linkFromYCoord_m = 0.5 * linkDistance_m;
		this.linkToYCoord_m = this.linkFromYCoord_m + linkLength_m;
		this.destinationNodeYCoord_m = this.linkToYCoord_m + 0.5 * linkDistance_m;

		for (int linkIndex = 0; linkIndex < linkCnt; linkIndex++) {
			final double xCoord_m = linkIndex * this.linkDistance_m;
			NetworkUtils.createAndAddNode(scenario.getNetwork(), this.linkFromNodeId(linkIndex),
					new Coord(xCoord_m, this.originNodeYCoord_m));
			NetworkUtils.createAndAddNode(scenario.getNetwork(), this.linkToNodeId(linkIndex),
					new Coord(xCoord_m, this.destinationNodeYCoord_m));
		}
	}

	private Id<Node> linkFromNodeId(int linkIndex) {
		return Id.createNodeId(linkIndex + "_from");
	}

	private Id<Node> linkToNodeId(int linkIndex) {
		return Id.createNodeId(linkIndex + "_to");
	}

	private Id<Link> linkId(int linkIndex) {
		return Id.createLinkId(linkIndex);
	}

	private Id<Node> originNodeId(String odId) {
		return Id.createNodeId("origin_" + odId);
	}

	private Id<Node> destinationNodeId(String odId) {
		return Id.createNodeId("destination_" + odId);
	}

	private Id<Link> originConnectorLinkId(String odId, Id<Link> linkId) {
		return Id.createLinkId("originConnector_" + odId + "_toLink" + linkId);
	}

	private Id<Link> destinationConnectorLinkId(String odId, Id<Link> linkId) {
		return Id.createLinkId("destinationConnector_" + odId + "_fromLink" + linkId);
	}

	public void addLink(int linkIndex, double freespeed_km_h, double capacity_veh_h) {
		if (this.warmupTime_s != null) {
			throw new RuntimeException("Warmup time has aready been computed.");
		}
		Node fromNode = this.scenario.getNetwork().getNodes().get(this.linkFromNodeId(linkIndex));
		Node toNode = this.scenario.getNetwork().getNodes().get(this.linkToNodeId(linkIndex));
		NetworkUtils.createAndAddLink(this.scenario.getNetwork(), this.linkId(linkIndex), fromNode, toNode,
				this.linkLength_m, Units.M_S_PER_KM_H * freespeed_km_h, capacity_veh_h, 100, null, null);
	}

	private double getOrComputeWarmupTime_s() {
		if (this.warmupTime_s == null) {
			double maxSpeed_m_s = this.scenario.getNetwork().getLinks().values().stream()
					.mapToDouble(l -> l.getFreespeed()).max().getAsDouble();
			double maxXDistance_m = this.linkDistance_m * (this.linkCnt - 1.0) / 2;
			double maxYDistance_m = 0.5 * this.linkDistance_m;
			double warmupLength_m = Math.sqrt(maxXDistance_m * maxXDistance_m + maxYDistance_m * maxYDistance_m);
			this.warmupTime_s = warmupLength_m / maxSpeed_m_s;
		}
		return this.warmupTime_s;
	}

	private String odId(List<Id<Link>> linkIds) {
		return "OD(" + linkIds.stream().map(id -> id.toString()).collect(Collectors.joining(",")) + ")";
	}

	public void addODFlow(int totalDemand_vehs, int... linkIndices) {
		this.addODFlow(totalDemand_vehs, Arrays.stream(linkIndices).boxed().map(i -> this.linkId(i)).toList());
	}

	public Id<Node> entryNodeId(String odId) {
		return Id.createNodeId("entry_" + odId);
	}

	public Id<Node> exitNodeId(String odId) {
		return Id.createNodeId("exit_" + odId);
	}

	public Id<Link> entryLinkId(String odId) {
		return Id.createLinkId("entry_" + odId);
	}

	public Id<Link> exitLinkId(String odId) {
		return Id.createLinkId("exit_" + odId);
	}

	public void addODFlow(int totalDemand_vehs, List<Id<Link>> usedLinkIds) {
		String odName = this.odId(usedLinkIds);
		List<? extends Link> usedLinks = this.scenario.getNetwork().getLinks().entrySet().stream()
				.filter(e -> usedLinkIds.contains(e.getKey())).map(e -> e.getValue()).toList();
		double xCoord_m = usedLinks.stream().mapToDouble(l -> l.getFromNode().getCoord().getX()).average()
				.getAsDouble();
		Node originNode = NetworkUtils.createAndAddNode(this.scenario.getNetwork(), this.originNodeId(odName),
				new Coord(xCoord_m, this.originNodeYCoord_m));
		Node destinationNode = NetworkUtils.createAndAddNode(this.scenario.getNetwork(), this.destinationNodeId(odName),
				new Coord(xCoord_m, this.destinationNodeYCoord_m));

		for (Link link : usedLinks) {
			double length_m = NetworkUtils.getEuclideanDistance(originNode.getCoord(), link.getFromNode().getCoord());
			double speed_m_s = length_m / this.getOrComputeWarmupTime_s();
			NetworkUtils.createAndAddLink(this.scenario.getNetwork(), this.originConnectorLinkId(odName, link.getId()),
					originNode, link.getFromNode(), length_m, speed_m_s, 10_000, 100, null, null);
			NetworkUtils.createAndAddLink(this.scenario.getNetwork(),
					this.destinationConnectorLinkId(odName, link.getId()), link.getToNode(), destinationNode, length_m,
					speed_m_s, 10_000, 100, null, null);
		}

		Node entryNode = NetworkUtils.createAndAddNode(this.scenario.getNetwork(), this.entryNodeId(odName),
				originNode.getCoord());
		Link entryLink = NetworkUtils.createAndAddLink(this.scenario.getNetwork(), this.entryLinkId(odName), entryNode,
				originNode, 0.0, 60.0, 10_000, 100, null, null);

		Node exitNode = NetworkUtils.createAndAddNode(this.scenario.getNetwork(), this.exitNodeId(odName),
				destinationNode.getCoord());
		Link exitLink = NetworkUtils.createAndAddLink(this.scenario.getNetwork(), this.exitLinkId(odName),
				destinationNode, exitNode, 0.0, 60.0, 10_000, 100, null, null);

		PopulationFactory factory = this.scenario.getPopulation().getFactory();

		Id<Link> chosenLink2Id = usedLinks.get(0).getId();
		Id<Link> chosenLink1Id = this.originConnectorLinkId(odName, chosenLink2Id);
		Id<Link> chosenLink3Id = this.destinationConnectorLinkId(odName, chosenLink2Id);

		double delta_s = this.inflowDuration_s / totalDemand_vehs;
		double time_s = 0;
		for (int n = 0; n < totalDemand_vehs; n++) {
			Plan plan = factory.createPlan();
			plan.addActivity(factory.createActivityFromLinkId("start", entryLink.getId()));
			Leg leg = factory.createLeg(TransportMode.car);
			leg.setDepartureTime(time_s);
			Route route = RouteUtils.createLinkNetworkRouteImpl(entryLink.getId(),
					Arrays.asList(chosenLink1Id, chosenLink2Id, chosenLink3Id), exitLink.getId());
			leg.setRoute(route);
			plan.addLeg(leg);
			plan.addActivity(factory.createActivityFromLinkId("end", exitLink.getId()));

//			Person person = factory.createPerson(Id.createPersonId(odName + "_" + n));			
			Person person = factory.createPerson(Id.createPersonId(odName + "_" + n));
			person.addPlan(plan);
			this.scenario.getPopulation().addPerson(person);

			time_s += delta_s;
		}

	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("network_parallel_links.xml");
		config.plans().setInputFile("population_parallel_links.xml");

		Scenario scenario = ScenarioUtils.createScenario(config);

		int linkCnt = 5;
		double linkLength_m = 1000;
		double inflowDuration_s = 1800.0;
		ScenarioFactory factory = new ScenarioFactory(scenario, linkCnt, linkLength_m, inflowDuration_s);

		factory.addLink(0, 60.0, 500);
		factory.addLink(1, 60.0, 500);
		factory.addLink(2, 60.0, 500);
		factory.addLink(3, 60.0, 500);
		factory.addLink(4, 60.0, 500);

		factory.addODFlow(1000, 0, 1, 2);
		factory.addODFlow(1000, 1, 2, 3);
		factory.addODFlow(1000, 2, 3, 4);

		new ConfigWriter(config).write("config_parallel_links.xml");
		new NetworkWriter(factory.scenario.getNetwork()).write(config.network().getInputFile());
		new PopulationWriter(factory.scenario.getPopulation()).write(config.plans().getInputFile());

		Controler controler = new Controler(scenario);
		controler.run();
		
		System.out.println("... DONE");
	}
}
