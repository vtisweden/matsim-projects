/**
 * se.vti.atap.examples.minimalframework.parallel_links
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
package se.vti.atap.examples.minimalframework.parallel_links;

import java.util.Random;

/**
 * 
 * @author GunnarF
 *
 */
public class RandomNetworkGenerator {

	private RandomNetworkGenerator() {
	}

	public static Network createRandomNetwork(int numberOfLinks, double minT0_s, double maxT0_s, double minCap_veh,
			double maxCap_veh, Random rnd) {
		Network network = new Network(numberOfLinks);
		for (int link = 0; link < numberOfLinks; link++) {
			network.setBPRParameters(link, rnd.nextDouble(minT0_s, maxT0_s), rnd.nextDouble(minCap_veh, maxCap_veh));
		}
		return network;
	}
}
