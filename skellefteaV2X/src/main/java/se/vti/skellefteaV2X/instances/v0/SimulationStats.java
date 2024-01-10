/**
 * se.vti.skellefeaV2X
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.skellefteaV2X.instances.v0;

/**
 * 
 * @author GunnarF
 *
 */
public class SimulationStats {

	private Double startTime_h = null;

	private Double endTime_h = null;

	private double minCharge_kWh = 0.0;

	private double maxCharge_kWh = 0.0;

	public void setStartTime_h(double startTime_h) {
		this.startTime_h = startTime_h;
	}

	public void setEndTime_h(double endTime_h) {
		this.endTime_h = endTime_h;
	}

	public Double getStartTime_h() {
		return this.startTime_h;
	}
	
	public Double getEndTime_h() {
		return this.endTime_h;
	}
	
	public void addCharge_kWh(double charge_kWh) {
		this.minCharge_kWh = Math.min(minCharge_kWh, charge_kWh);
		this.maxCharge_kWh = Math.max(maxCharge_kWh, charge_kWh);
	}

	public double getMinCharge_kWh() {
		return this.minCharge_kWh;
	}

	public double getMaxCharge_kWh() {
		return this.maxCharge_kWh;
	}
	
}
