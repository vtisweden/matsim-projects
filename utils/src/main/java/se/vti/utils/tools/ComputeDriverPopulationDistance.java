/**
 * se.vti.utils
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
package se.vti.utils.tools;

import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import se.vti.utils.matsim.Plans;
import se.vti.utils.matsim.PopulationDistance;

/**
 * 
 * @author GunnarF
 *
 */
public class ComputeDriverPopulationDistance {

	public static void main(String[] args) {

		// four command line parameter: two population files, one network file, one number reflecting the population sampling rate
		Plans plans1 = new Plans(PopulationUtils.readPopulation(args[0]));
		Plans plans2 = new Plans(PopulationUtils.readPopulation(args[1]));
		Network network = NetworkUtils.readNetwork(args[2]);
		double flowCapacityFactor = Double.parseDouble(args[3]);
		
		double kernelHalftime_s = 300.0;
		double kernelThreshold = 0.05;

		Map<String, TravelTime> car2freeTravelTime = Collections.singletonMap("car", new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}
		});

		PopulationDistance dist = new PopulationDistance(plans1, plans2, network, flowCapacityFactor,
				car2freeTravelTime, kernelHalftime_s, kernelThreshold);

		System.out.println("Population distance = " + dist.computeDistance());
	}
}
