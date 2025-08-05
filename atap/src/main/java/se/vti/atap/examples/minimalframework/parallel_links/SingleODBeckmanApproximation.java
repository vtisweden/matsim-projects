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
package se.vti.atap.examples.minimalframework.parallel_links;

import se.vti.atap.examples.minimalframework.parallel_links.ods.ODPair;
import se.vti.atap.examples.minimalframework.parallel_links.ods.Paths;

/**
 * 
 * @author GunnarF
 *
 */
public class SingleODBeckmanApproximation {

	public final double[] s;
	public final double[] c;
	public final double[] g;
	public final double[] v;

	public SingleODBeckmanApproximation(ODPair odPair, NetworkConditionsImpl networkConditions) {
		this.s = new double[odPair.getNumberOfPaths()];
		this.c = new double[odPair.getNumberOfPaths()];
		this.g = new double[odPair.getNumberOfPaths()];
		this.v = new double[odPair.getNumberOfPaths()];
		for (int h = 0; h < odPair.getNumberOfPaths(); h++) {
			int ij = odPair.availableLinks[h];
			this.g[h] = odPair.getCurrentPlan().pathFlows_veh[h];
			this.v[h] = networkConditions.linkTravelTimes_s[ij];
			this.s[h] = networkConditions.dLinkTravelTimes_dLinkFlows_s_veh[ij];
			this.c[h] = this.v[h] - this.s[h] * this.g[h];
		}
	}

	public double compute(Paths paths) {
		double result = 0.0;
		for (int h = 0; h < paths.getNumberOfPaths(); h++) {
			double f = paths.pathFlows_veh[h];
			result += (this.v[h] - this.s[h] * this.g[h]) * f + 0.5 * this.s[h] * f * f;
		}
		return result;
	}

}
