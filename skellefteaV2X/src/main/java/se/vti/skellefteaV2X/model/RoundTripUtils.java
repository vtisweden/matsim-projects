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

public class RoundTripUtils {

	private final Scenario scenario;

	public RoundTripUtils(Scenario scenario) {
		this.scenario = scenario;
	}

	public static double effectiveHomeDuration_h(ParkingEpisode h) {
		// wraparound: home activity starts on the day before
		return Math.max(0.0, h.getEndTime_h() - h.getStartTime_h());
	}

//	public static List<Tuple<Double, Double>> withinDayParkingIntervals(ParkingEpisode h) {
//		if (h.getStartTime_h() >= 0) {
//			if (h.getEndTime_h() >= 0) {
//
//				// both within-day
//				
//				if (h.getStartTime_h() <= h.getEndTime_h()) {
//					return Collections.singletonList(new Tuple<>(h.getStartTime_h(), h.getEndTime_h()));
//				} else {
//					return Collections.emptyList();
//				}
//			} else {
//				
//				// end-time outofday
//				
//				return null;
//				
//			}
//		} else {
//			if (h.getEndTime_h() >= 0) {
//				
//				return null;
//				
//			} else {
//				
//				return null;
//				
//			}			
//		}
//		
//	}

}
