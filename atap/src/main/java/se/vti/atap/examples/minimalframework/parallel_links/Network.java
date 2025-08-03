/**
 * se.vti.atap.examples.minimalframework.parallel_links_agents
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

/**
 * 
 * @author GunnarF
 *
 */
public class Network {

	public final double[] t0_s;
	public final double[] cap_veh;
	public final double[] exponent;

	public Network(int numberOfLinks) {
		this.t0_s = new double[numberOfLinks];
		this.cap_veh = new double[numberOfLinks];
		this.exponent = new double[numberOfLinks];
	}

	public Network setBPRParameters(int link, double t0_s, double cap_veh, double exponent) {
		this.t0_s[link] = t0_s;
		this.cap_veh[link] = cap_veh;
		this.exponent[link] = exponent;
		return this;
	}

	public Network setAllBPRParameters(double t0_s, double cap_veh, double exponent) {
		for (int link = 0; link < this.getNumberOfLinks(); link++) {
			this.setBPRParameters(link, t0_s, cap_veh, exponent);
		}
		return this;
	}

	public int getNumberOfLinks() {
		return this.t0_s.length;
	}
	
	public double computeTravelTime_s(int link, double flow_veh) {
		return this.t0_s[link] * Math.pow(1.0 + flow_veh / this.cap_veh[link], this.exponent[link]);
	}

	public double computeFlow_veh(int link, double travelTime_s) {
		return this.cap_veh[link] * (Math.pow(travelTime_s / this.t0_s[link], 1.0 / this.exponent[link]) - 1.0);
	}

	public double compute_dTravelTime_dFlow_s_veh(int link, double flow_veh) {
		return this.t0_s[link] * this.exponent[link]
				* Math.pow(1.0 + flow_veh / this.t0_s[link], this.exponent[link] - 1.0) / this.cap_veh[link];
	}
}
