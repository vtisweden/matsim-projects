/**
 * se.vti.samgods.calibration
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.utils;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;

import floetteroed.utilities.Units;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.network.NetworkDataProvider;

public class ShipmentPopulationCreator {

	private long id = 0;

	private final NetworkData networkData;
	
	private Population population;
	
	public ShipmentPopulationCreator() {
		this.networkData = NetworkDataProvider.getInstance().createNetworkData();
		this.population = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	}

	public void add(TransportChain chain) {

		double time = Units.S_PER_D * Math.random();
//		for (TransportEpisode episode : chain.getEpisodes()) {
			Plan plan = population.getFactory().createPlan();
			
//			List<? extends Link> links = chain.allLinks(this.networkData);
//			if (links.size() > 1) {
//
//				Activity load = population.getFactory().createActivityFromLinkId("START", links.get(0).getId());
//				load.setEndTime(time);
//				plan.addActivity(load);
//
//				Leg leg = population.getFactory().createLeg("freightmode");
//				Route route = RouteUtils.createNetworkRoute(links.stream().map(l -> l.getId()).toList());
//				double duration = links.stream().mapToDouble(l -> l.getLength() / l.getFreespeed()).sum();
//				double length = links.stream().mapToDouble(l -> l.getLength() / l.getFreespeed()).sum();
//				route.setDistance(length);
//				route.setTravelTime(duration);
//				leg.setRoute(route);
//				leg.setDepartureTime(time);
//				plan.addLeg(leg);
//				time += duration;
//
//				Activity unload = population.getFactory().createActivityFromLinkId("END",
//						links.get(links.size() - 1).getId());
//				unload.setEndTime(time);
//				plan.addActivity(unload);
//			}

			Person shipment = population.getFactory().createPerson(Id.createPersonId(id++));
			shipment.addPlan(plan);
			population.addPerson(shipment);
//		}

	}

	public void writeToFile(String fileName) {
		PopulationUtils.writePopulation(this.population, fileName);
	}

	public static void main(String[] args) {
		new ShipmentPopulationCreator();
		System.out.println("DONE");
	}

}
