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
import java.util.List;
import java.util.Set;

import se.vti.atap.minimalframework.Agent;
import se.vti.atap.minimalframework.Logger;
import se.vti.atap.minimalframework.NetworkConditions;

/**
 * 
 * @author GunnarF
 *
 * @param <A>
 * @param <T>
 */
public class BasicLoggerImpl<A extends Agent<?>, T extends NetworkConditions> implements Logger<A, T> {

	private int logCounter = 0;

	private final List<Double> averageGaps = new ArrayList<>();

	public BasicLoggerImpl() {
	}

	protected int getLogCounter() {
		return this.logCounter;
	}

	@Override
	public final void log(Set<A> agents, T networkConditions, int iteration) {
		assert (this.logCounter == iteration);
		this.averageGaps.add(agents.stream().mapToDouble(a -> a.computeGap()).average().getAsDouble());
		this.logCounter++;
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
