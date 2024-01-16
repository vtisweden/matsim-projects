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

import java.util.Arrays;
import java.util.List;

import floetteroed.utilities.Tuple;
import se.vti.utils.misc.math.MathHelpers;

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

	public Double getDuration_h() {
		return duration_h;
	}

	public void setDuration_h(Double duration_h) {
		this.duration_h = duration_h;
	}

	public List<Tuple<Double, Double>> effectiveIntervals() {
		assert (this.duration_h >= 0.0);
		if (this.duration_h > 24.0) {
			return Arrays.asList(new Tuple<>(0.0, 24.0));
		} else {
			double withinDayEnd_h = this.end_h;
			while (withinDayEnd_h < 0.0) {
				withinDayEnd_h += 24.0;
			}
			while (withinDayEnd_h > 24.0) {
				withinDayEnd_h -= 24.0;
			}
			double start_h = withinDayEnd_h - this.duration_h;
			if (start_h < 0.0) {
				return Arrays.asList(new Tuple<>(start_h + 24.0, 24.0), new Tuple<>(0.0, withinDayEnd_h));
			} else {
				return Arrays.asList(new Tuple<>(start_h, withinDayEnd_h));
			}
		}
	}

	public synchronized static List<Tuple<Double, Double>> effectiveIntervals(double duration_h, double end_h) {
		Episode e = new Episode();
		e.setDuration_h(duration_h);
		e.setEndTime_h(end_h);
		return e.effectiveIntervals();
	}

	@Override
	public String toString() {
		MathHelpers math = new MathHelpers();
		return this.getClass().getSimpleName() + ":time(" + math.round(this.end_h - this.duration_h, 2) + ","
				+ math.round(this.end_h, 2) + "),charge(" + math.round(this.chargeAtStart_kWh, 2) + ","
				+ math.round(this.chargeAtEnd_kWh, 2) + ")";
	}
}
