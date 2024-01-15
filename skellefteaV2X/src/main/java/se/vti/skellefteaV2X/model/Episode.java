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
package se.vti.skellefteaV2X.model;

import floetteroed.utilities.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class Episode {

	private Double duration_h = null;
	private Double end_h = null;

	private Double chargeAtStart_kWh = null;
	private Double chargeAtEnd_kWh = null;

	public Episode() {
	}

//	public Double getStartTime_h() {
//		return start_h;
//	}
//
//	public void setStartTime_h(Double start_h) {
//		this.start_h = start_h;
//	}

	public Double getEndTime_h() {
		return end_h;
	}

	public void setEndTime_h(Double end_h) {
		this.end_h = end_h;
	}

	public void setChargeAtStart_kWh(Double chargeAtStart_kWh) {
		this.chargeAtStart_kWh = chargeAtStart_kWh;
	}

	public Double getChargeAtStart_kWh() {
		return this.chargeAtStart_kWh;
	}

	public void setChargeAtEnd_kWh(Double chargeAtEnd_kWh) {
		this.chargeAtEnd_kWh = chargeAtEnd_kWh;
	}

	public Double getChargeAtEnd_kWh() {
		return this.chargeAtEnd_kWh;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ":time(" + MathHelpers.round(this.end_h - this.duration_h, 2) + ","
				+ MathHelpers.round(this.end_h, 2) + "),charge(" + MathHelpers.round(this.chargeAtStart_kWh, 2) + ","
				+ MathHelpers.round(this.chargeAtEnd_kWh, 2) + ")";
	}

	public Double getDuration_h() {
		return duration_h;
	}

	public void setDuration_h(Double duration_h) {
		this.duration_h = duration_h;
	}
}
