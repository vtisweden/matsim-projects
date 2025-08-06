/**
 * se.vti.atap.minimalframework.common
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import se.vti.atap.examples.minimalframework.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.Logger;

/**
 * 
 * @author GunnarF
 *
 * @param <A>
 * @param <T>
 */
public class LoggerImpl implements Logger<ODPair, NetworkConditionsImpl> {

	private int logCounter = 0;

	private final List<Double> averageGaps = new ArrayList<>();

	private final List<Double> averageNumberOfPaths = new ArrayList<>();
	
	public LoggerImpl() {
	}

	protected int getLogCounter() {
		return this.logCounter;
	}

	@Override
	public final void log(Set<ODPair> agents, NetworkConditionsImpl networkConditions, int iteration) {
		assert (this.logCounter == iteration);

		double numberOfPaths = 0.0;
		for (ODPair odPair : agents) {
			for (int path = 0; path < odPair.getNumberOfPaths(); path++) {
				if (odPair.getCurrentPlan().pathFlows_veh[path] >= 1e-8) {
					numberOfPaths++;
				}
			}
		}
		this.averageNumberOfPaths.add(numberOfPaths / agents.size());

		double num = 0.0;
		for (ODPair odPair : agents) {			
			int bestPath = odPair.computeBestPath(networkConditions);
			double minTT_s = networkConditions.linkTravelTimes_s[odPair.availableLinks[bestPath]];
			num += minTT_s * odPair.demand_veh;			
		}
		double den = 0.0;
		for (int link = 0; link < networkConditions.linkFlows_veh.length; link++) {
			den += networkConditions.linkFlows_veh[link] * networkConditions.linkTravelTimes_s[link];
		}
		double gap = 1.0 - num / den;
		
//		double gap = agents.stream().mapToDouble(od -> od.computeGap()).average().getAsDouble();
				
		this.averageGaps.add(gap);
	}

	public List<Double> getAverageGaps() {
		return this.averageGaps;
	}

	public String toString() {
		StringBuffer result = new StringBuffer("averag gap");
		result.append("\n");
		for (double gap : this.averageGaps) {
			result.append(gap);
			result.append("\n");
		}
		return result.toString();
	}
}
