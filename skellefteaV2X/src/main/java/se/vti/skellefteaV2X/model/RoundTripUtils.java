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

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripUtils {

//	public static double projectOntoDay_h(double time_h) {
//		double result_h = time_h;
//		while (result_h < 0.0) {
//			result_h += 24.0;
//		}
//		while (result_h >= 24.0) {
//			result_h -= 24.0;
//		}
//		return result_h;
//	}

//	public static double selfOverlap_h(List<Tuple<Double, Double>> intervals) {
//		double result_h = 0.0;	
//		for (Tuple<Double, Double> i1 : intervals) {
//			for (Tuple<Double, Double> i2 : intervals) {
//				if (i1 != i2) {
//					result_h += MathHelpers.overlap(i1.getA(), i1.getB(), i2.getA(), i2.getB());
//				}
//			}
//		}		
//		return result_h;
//	}

	public static List<Tuple<Double, Double>> effectiveIntervals(double duration_h, double end_h) {
		assert(duration_h >= 0.0);
		if (duration_h > 24.0) {
			return Arrays.asList(new Tuple<>(0.0, 24.0));
		} else {
			while (end_h < 0.0) {
				end_h += 24.0;
			}
			while (end_h > 24.0) {
				end_h -= 24.0;
			}
			double start_h = end_h - duration_h;
			if (start_h < 0.0) {
				return Arrays.asList(new Tuple<>(start_h + 24.0, 24.0), new Tuple<>(0.0, end_h));
			} else {
				return Arrays.asList(new Tuple<>(start_h, end_h));
			}
		}
//		if (end_h <= start_h || end_h > start_h + 24.0) {
//			return Collections.emptyList();
//		}
//		start_h = projectOntoDay_h(start_h);
//		end_h = projectOntoDay_h(end_h);
//		List<Tuple<Double, Double>> result;
//		if (start_h <= end_h) {
//			result = Arrays.asList(new Tuple<>(start_h, end_h));
//		} else {
//			result = Arrays.asList(new Tuple<>(start_h, 24.0), new Tuple<>(0.0, end_h));
//		}
//		return result;
	}

//	public static double effectiveDuration_h(Episode e) {
//		return effectiveDuration_h(effectiveIntervals(e.getStartTime_h(), e.getEndTime_h()));
////		double result = 0.0;
////		for (Tuple<Double, Double> interval : effectiveIntervals(p.getStartTime_h(), p.getEndTime_h())) {
////			result += interval.getB() - interval.getA();
////		}
////		return result;
//	}
//
//	public static double effectiveDuration_h(List<Tuple<Double, Double>> intervals) {
//		double result = 0.0;
//		for (Tuple<Double, Double> interval : intervals) {
//			result += interval.getB() - interval.getA();
//		}
//		return result;
//	}

}
