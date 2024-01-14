/**
 * se.vti.skellefteaV2X
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

public class RoundTripUtils {

	private final Scenario scenario;

	public RoundTripUtils(Scenario scenario) {
		this.scenario = scenario;
	}

	public static double effectiveHomeDuration_h(ParkingEpisode h) {
		// wraparound: home activity starts on the day before
		double result = Math.max(0.0, h.getEndTime_h() - h.getStartTime_h());
		double altResult = effectiveParkingDuration_h(h, true);
		return altResult;
	}

	public static List<Tuple<Double, Double>> effectiveParkingIntervals(ParkingEpisode p, boolean isHome) {
		if (p.getStartTime_h() < 0.0) {
			return Arrays.asList(new Tuple<>(p.getStartTime_h() + 24.0, 24.0), new Tuple<>(0.0, p.getEndTime_h()));
		} else {
			if (isHome) {
				if (p.getStartTime_h() <= p.getEndTime_h()) {
					return Arrays.asList(new Tuple<>(p.getStartTime_h(), p.getEndTime_h()));
				} else {
					return Arrays.asList(new Tuple<>(p.getStartTime_h(), 24.0), new Tuple<>(0.0, p.getEndTime_h()));
				}
			} else {
				return Arrays.asList(new Tuple<>(p.getStartTime_h(), p.getEndTime_h()));
			}
		}
	}

	public static double effectiveParkingDuration_h(ParkingEpisode p, boolean isHome) {
		double result = 0.0;
		for (Tuple<Double, Double> interval : effectiveParkingIntervals(p, isHome)) {
			result += interval.getB() - interval.getA();
		}
		return result;
	}

}
