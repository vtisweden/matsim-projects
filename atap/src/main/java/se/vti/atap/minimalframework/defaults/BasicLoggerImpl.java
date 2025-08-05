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
package se.vti.atap.minimalframework.defaults;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.Logger;
import se.vti.atap.minimalframework.NetworkConditions;

/**
 * 
 * @author GunnarF
 *
 * @param <T>
 * @param <A>
 */
public class BasicLoggerImpl<T extends NetworkConditions, A extends Agent<?>> implements Logger<T, A> {

	private int logCounter = 0;

	private final String header;
	private final List<String> dataRows = new ArrayList<>();

	private final List<Double> averageGaps = new ArrayList<>();
	
	public BasicLoggerImpl() {
		this.header = this.createHeader();
	}

	protected int getLogCounter() {
		return this.logCounter;
	}

	protected double computeAverageGap(Set<A> agents) {
		return agents.stream().mapToDouble(a -> a.getCandidatePlan().getUtility() - a.getCurrentPlan().getUtility())
				.average().getAsDouble();
	}

	@Override
	public final void log(T networkConditions, Set<A> agents) {
		this.averageGaps.add(this.computeAverageGap(agents));
		this.dataRows.add(this.createLine(networkConditions, agents));
		this.logCounter++;
	}
	
	public String createHeader() {
		return "iteration\taverageGap";
	}

	public String createLine(T networkConditions, Set<A> agents) {
		return this.logCounter + "\t" + this.averageGaps.get(this.averageGaps.size() - 1);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer(this.header);
		result.append("\n");
		for (String dataLine : this.dataRows) {
			result.append(dataLine);
			result.append("\n");
		}
		return result.toString();
	}

	public String getHeader() {
		return this.header;
	}

	public List<String> getDataRows() {
		return this.dataRows;
	}
	
	public List<Double> getAverageGaps() {
		return this.averageGaps;
	}

}
