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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import se.vti.atap.examples.minimalframework.parallel_links.NetworkConditionsImpl;
import se.vti.atap.minimalframework.Logger;

/**
 * 
 * @author GunnarF
 *
 */
public class LoggerImpl implements Logger<ODPair, NetworkConditionsImpl> {

	private final List<DescriptiveStatistics> gapStatistics = new ArrayList<>();

	public LoggerImpl() {
	}

	@Override
	public final void log(Set<ODPair> odPairs, NetworkConditionsImpl networkConditions, int iteration) {
		double numerator = (-1.0) * odPairs.stream().mapToDouble(od -> od.getCurrentPlan().getUtility()).sum();
		double denominator = 0.0;
		for (int link = 0; link < networkConditions.linkFlows_veh.length; link++) {
			denominator += networkConditions.linkFlows_veh[link] * networkConditions.linkTravelTimes_s[link];
		}
		double relativeEquilibriumGap = numerator / denominator;

		while (this.gapStatistics.size() <= iteration) {
			this.gapStatistics.add(new DescriptiveStatistics());
		}
		this.gapStatistics.get(iteration).addValue(relativeEquilibriumGap);
	}

	public int getNumberOfIterations() {
		return this.gapStatistics.size();
	}

	public DescriptiveStatistics getDataOrNull(int iteration) {
		if (iteration >= this.gapStatistics.size()) {
			return null;
		} else {
			return this.gapStatistics.get(iteration);
		}
	}
}
