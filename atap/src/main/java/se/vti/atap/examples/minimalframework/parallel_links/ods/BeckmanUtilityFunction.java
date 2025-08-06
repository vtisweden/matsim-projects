/**
 * se.vti.atap.examples.minimalframework.parallel_links.ods
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
package se.vti.atap.examples.minimalframework.parallel_links.ods;

import se.vti.atap.examples.minimalframework.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.UtilityFunction;

/**
 */
public class BeckmanUtilityFunction implements UtilityFunction<Paths, ODPair, NetworkConditionsImpl> {

	public BeckmanUtilityFunction() {
	}

	@Override
	public double compute(Paths paths, ODPair odPair, NetworkConditionsImpl networkConditions) {
//		return (-1.0) * networkConditions.od2beckmanApproximations.get(odPair).compute(paths);
//		return (-1.0) * odPair.beckmanApproximation.computeBeckmanFunctionValue(paths);

		int bestPath = odPair.computeBestPath(networkConditions);
		double minTT_s = networkConditions.linkTravelTimes_s[odPair.availableLinks[bestPath]];
		double min = minTT_s * odPair.demand_veh;

		double avg = 0.0;
		for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
			int link = odPair.availableLinks[path];
			avg += paths.pathFlows_veh[path] * networkConditions.linkTravelTimes_s[link];
		}

		double gap = avg - min;
		return -gap;
	}
}
