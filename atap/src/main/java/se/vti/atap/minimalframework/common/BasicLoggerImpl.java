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
package se.vti.atap.minimalframework.common;

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

	private final StringBuffer log;

	public BasicLoggerImpl() {
		this.log = new StringBuffer(this.createHeader() + "\n");
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
		this.log.append(this.createLine(networkConditions, agents) + "\n");
		this.logCounter++;
	}

	public String createHeader() {
		return "iteration\taverageGap";
	}

	public String createLine(T networkConditions, Set<A> agents) {
		return this.logCounter + "\t" + computeAverageGap(agents);
	}
	
	public String toString() {
		return this.log.toString();
	}

}
