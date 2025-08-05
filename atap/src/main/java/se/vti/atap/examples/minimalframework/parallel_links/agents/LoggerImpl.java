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
package se.vti.atap.examples.minimalframework.parallel_links.agents;

import java.util.Set;

import se.vti.atap.minimalframework.defaults.BasicLoggerImpl;
import se.vti.atap.minimalframework.defaults.DoubleArrayWrapper;

/**
 * 
 * @author GunnarF
 *
 */
public class LoggerImpl extends BasicLoggerImpl<DoubleArrayWrapper, AgentImpl> {

	@Override
	public String createHeader() {
		return "iteration\taverageGap\ttravelTime[0]\ttravelTime[1]";
	}

	@Override
	public String createLine(DoubleArrayWrapper travelTimes, Set<AgentImpl> agents) {
		return super.getLogCounter() + "\t" + super.computeAverageGap(agents) + "\t" + travelTimes.data[0] + "\t"
				+ travelTimes.data[1];
	}

}
