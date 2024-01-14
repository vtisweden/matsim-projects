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
package se.vti.skellefteaV2X.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.math.MathHelpers;
import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.RoundTripUtils;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class LocationVisitAnalyzer extends SimulatedRoundTripAnalyzer {

	private List<Map<Location, Long>> timeListOfLocation2visits;

	private List<Map<Location, Long>> timeListOfLocation2chargings;

	private List<Long> timeListOfAtHome;

	private long visitsToHome;
	private long chargingsAtHome;

	private long visitsOffHome;
	private long chargingsOffHome;

	private BasicStatistics homeDurationStats = new BasicStatistics();
	private BasicStatistics homeStartStats = new BasicStatistics();
	private BasicStatistics homeEndStats = new BasicStatistics();

	private BasicStatistics otherDurationStats = new BasicStatistics();

	public LocationVisitAnalyzer(Scenario scenario, int burnInIterations, int samplingInterval) {
		super(scenario, burnInIterations, samplingInterval);
		this.timeListOfLocation2visits = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfLocation2chargings = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfAtHome = new ArrayList<>(scenario.getBinCnt());

		for (int i = 0; i < scenario.getBinCnt(); i++) {
			this.timeListOfLocation2visits.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfLocation2chargings.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfAtHome.add(0l);
		}
	}

	@Override
	public void processRelevantState(SimulatedRoundTrip state) {

//		ParkingEpisode home = (ParkingEpisode) state.getEpisodes().get(0);
//
//		for (int bin = 0; bin < this.scenario.getBinCnt(); bin++) {
//
//			double binStart_h = this.scenario.getBinSize_h() * bin;
//			double binEnd_h = binStart_h + this.scenario.getBinSize_h();
//			
//			if (RoundTripUtils.effectiveHomeDuration_h(home) > 0) {
//				final double withinDayHomeStartTime_h = home.getStartTime_h() + 24.0;
//				final double overlap_h;
//				if (withinDayHomeStartTime_h >= 0) {
//					overlap_h = MathHelpers.overlap(binStart_h, binEnd_h, withinDayHomeStartTime_h, home.getEndTime_h());
//				} else {
//					overlap_h = MathHelpers.overlap(binStart_h, binEnd_h, withinDayHomeStartTime_h, 24.0)
//							+ MathHelpers.overlap(binStart_h, binEnd_h, 0.0, home.getEndTime_h());
//				}
//				//TODO register data
//				
//				for (int i = 2; i < state.getEpisodes().size(); i+=2) {
//					ParkingEpisode p = (ParkingEpisode) state.getEpisodes().get(i);
//					
//				}
//				
//				
//			}
//
//			
//			
//		}
		

		ParkingEpisode home = (ParkingEpisode) state.getEpisodes().get(0);
		boolean homeCharging = home.getChargeAtEnd_kWh() > home.getChargeAtStart_kWh();
		int startBin = (int) ((home.getStartTime_h() + 24.0) / this.scenario.getBinSize_h());
		int endBin = (int) ((home.getEndTime_h()) / this.scenario.getBinSize_h());

		this.visitsToHome++;
		if (homeCharging) {
			this.chargingsAtHome++;
		}

		if (startBin > endBin) {

			for (int parkingBin = Math.max(0, startBin); parkingBin < this.scenario.getBinCnt(); parkingBin++) {
				this.timeListOfLocation2visits.get(parkingBin).compute(home.getLocation(),
						(l, c) -> c == null ? 1 : c + 1);
				this.timeListOfAtHome.set(parkingBin, this.timeListOfAtHome.get(parkingBin) + 1);
				if (homeCharging) {
					this.timeListOfLocation2chargings.get(parkingBin).compute(home.getLocation(),
							(l, c) -> c == null ? 1 : c + 1);
				}
			}
			for (int parkingBin = 0; parkingBin <= Math.min(endBin, this.scenario.getBinCnt() - 1); parkingBin++) {
				this.timeListOfLocation2visits.get(parkingBin).compute(home.getLocation(),
						(l, c) -> c == null ? 1 : c + 1);
				this.timeListOfAtHome.set(parkingBin, this.timeListOfAtHome.get(parkingBin) + 1);
				if (homeCharging) {
					this.timeListOfLocation2chargings.get(parkingBin).compute(home.getLocation(),
							(l, c) -> c == null ? 1 : c + 1);
				}
			}

		} else {

			for (int parkingBin = Math.max(0, startBin); parkingBin < Math.min(endBin,
					this.scenario.getBinCnt() - 1); parkingBin++) {
				this.timeListOfLocation2visits.get(parkingBin).compute(home.getLocation(),
						(l, c) -> c == null ? 1 : c + 1);
				this.timeListOfAtHome.set(parkingBin, this.timeListOfAtHome.get(parkingBin) + 1);
				if (home.getChargeAtEnd_kWh() > home.getChargeAtStart_kWh()) {
					this.timeListOfLocation2chargings.get(parkingBin).compute(home.getLocation(),
							(l, c) -> c == null ? 1 : c + 1);
				}
			}

		}

		if (state.size() > 1) {
			this.homeDurationStats.add(RoundTripUtils.effectiveHomeDuration_h(home));
			this.homeStartStats.add(home.getStartTime_h());
			this.homeEndStats.add(home.getEndTime_h());
		}

		for (int i = 1; i < state.getEpisodes().size(); i++) {
			Episode e = state.getEpisodes().get(i);
			startBin = (int) (e.getStartTime_h() / this.scenario.getBinSize_h());
			endBin = (int) (e.getEndTime_h() / this.scenario.getBinSize_h());
			if ((startBin >= 0) && (endBin < this.scenario.getBinCnt())) {
				if (e instanceof DrivingEpisode) {
					// nothing for now
				} else {
					ParkingEpisode p = (ParkingEpisode) e;
					this.otherDurationStats.add(e.getEndTime_h() - e.getStartTime_h());
					for (int parkingBin = startBin; parkingBin <= endBin; parkingBin++) {
						this.timeListOfLocation2visits.get(parkingBin).compute(p.getLocation(),
								(l, c) -> c == null ? 1 : c + 1);
					}
					this.visitsOffHome++;
					if (p.getChargeAtEnd_kWh() > p.getChargeAtStart_kWh()) {
						for (int parkingBin = startBin; parkingBin <= endBin; parkingBin++) {
							this.timeListOfLocation2chargings.get(parkingBin).compute(p.getLocation(),
									(l, c) -> c == null ? 1 : c + 1);
						}
						this.chargingsOffHome++;
					}
				}
			}
		}
	}

	@Override
	public void end() {

		List<Location> locations = new ArrayList<>(this.scenario.getLocationsView());
		Collections.sort(locations, new Comparator<Location>() {
			@Override
			public int compare(Location o1, Location o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		System.out.println();
		System.out.println("VISITS");
		System.out.println();

		for (Location l : locations) {
			System.out.print(l + "\t");
		}
		System.out.println();

		for (Map<Location, Long> location2visits : this.timeListOfLocation2visits) {
			for (Location l : locations) {
				System.out.print(location2visits.getOrDefault(l, 0l) + "\t");
			}
			System.out.println();
		}

		System.out.println();
		System.out.println("CHARGINGS");
		System.out.println();

		for (Location l : locations) {
			System.out.print(l + "\t");
		}
		System.out.println();

		for (Map<Location, Long> location2chargings : this.timeListOfLocation2chargings) {
			for (Location l : locations) {
				System.out.print(location2chargings.getOrDefault(l, 0l) + "\t");
			}
			System.out.println();
		}

		System.out.println();
		System.out.println("HOME DURATION");
		System.out.println(homeDurationStats.getAvg());
		System.out.println(homeDurationStats.getStddev());
		System.out.println(homeDurationStats.getMin());
		System.out.println(homeDurationStats.getMax());

		System.out.println();
		System.out.println("OTHER DURATION");
		System.out.println(otherDurationStats.getAvg());
		System.out.println(otherDurationStats.getStddev());
		System.out.println(otherDurationStats.getMin());
		System.out.println(otherDurationStats.getMax());

		System.out.println();
		System.out.println("HOME START");
		System.out.println(homeStartStats.getAvg());
		System.out.println(homeStartStats.getStddev());
		System.out.println(homeStartStats.getMin());
		System.out.println(homeStartStats.getMax());

		System.out.println();
		System.out.println("HOME END");
		System.out.println(homeEndStats.getAvg());
		System.out.println(homeEndStats.getStddev());
		System.out.println(homeEndStats.getMin());
		System.out.println(homeEndStats.getMax());

		System.out.println();
		for (Long l : this.timeListOfAtHome) {
			System.out.println(l);
		}

		System.out.println();
		System.out.println("P(chargeAtHome) = " + ((double) this.chargingsAtHome) / this.visitsToHome);
		System.out.println("P(chargeElsewhere) = " + ((double) this.chargingsOffHome) / this.visitsOffHome);

	}

}
