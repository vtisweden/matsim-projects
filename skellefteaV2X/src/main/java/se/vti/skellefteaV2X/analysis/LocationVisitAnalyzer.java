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

import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.MathHelpers;
import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.RoundTripUtils;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class LocationVisitAnalyzer extends SimulatedRoundTripAnalyzer {

	private final Preferences preferences;

	private List<Map<Location, Double>> timeListOfLocation2visits;

	private List<Map<Location, Double>> timeListOfLocation2chargings_kWh;

	private List<Double> timeListOfDriving;

	private Map<List<Location>, Long> sequence2uses = new LinkedHashMap<>();
	private Map<Location, Long> location2isHomeCnt = new LinkedHashMap<>();

	double chargedDetail_kWh = 0.0;
	double used_kWh = 0.0;
	double charged_kWh = 0.0;

	public LocationVisitAnalyzer(Scenario scenario, Preferences preferences, int burnInIterations,
			int samplingInterval) {
		super(scenario, burnInIterations, samplingInterval);
		this.preferences = preferences;
		this.timeListOfLocation2visits = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfLocation2chargings_kWh = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfDriving = new ArrayList<>(scenario.getBinCnt());

		for (int i = 0; i < scenario.getBinCnt(); i++) {
			this.timeListOfLocation2visits.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfLocation2chargings_kWh.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfDriving.add(0.0);
		}
	}

	public LocationVisitAnalyzer(Scenario scenario, int burnInIterations, int samplingInterval) {
		this(scenario, new Preferences(), burnInIterations, samplingInterval);
	}

	@Override
	public void processRelevantState(SimulatedRoundTrip state) {

		if (this.preferences.logWeight(state) < -Double.MIN_VALUE) {
			return;
		}

		this.sequence2uses.compute(state.getLocationsView(), (s, c) -> c == null ? 1 : c + 1);
		this.location2isHomeCnt.compute(state.getLocation(0), (l, c) -> c == null ? 1 : c + 1);

		for (Episode e : state.getEpisodes()) {

			final ParkingEpisode p = (e instanceof ParkingEpisode ? (ParkingEpisode) e : null);
			final DrivingEpisode d = (e instanceof DrivingEpisode ? (DrivingEpisode) e : null);

			final List<Tuple<Double, Double>> intervals = RoundTripUtils.effectiveIntervals(e.getStartTime_h(),
					e.getEndTime_h());
			final double effectiveDuration_h = RoundTripUtils.effectiveDuration_h(intervals);
			final double effectiveChargingRate_kW;
			if (effectiveDuration_h > 1e-8) {
				effectiveChargingRate_kW = Math.max(0.0, e.getChargeAtEnd_kWh() - e.getChargeAtStart_kWh())
						/ effectiveDuration_h;
			} else {
				effectiveChargingRate_kW = 0.0;
			}

			for (int bin = 0; bin < this.scenario.getBinCnt(); bin++) {
				
				final double binStart_h = this.scenario.getBinSize_h() * bin;
				final double binEnd_h = binStart_h + this.scenario.getBinSize_h();
				double overlap_h = 0.0;
				for (Tuple<Double, Double> interval : intervals) {
					overlap_h += MathHelpers.overlap(binStart_h, binEnd_h, interval.getA(), interval.getB());
				}
				final double relativeOverlap = overlap_h / (binEnd_h - binStart_h);
				assert (relativeOverlap >= 0.0);
				assert (relativeOverlap <= 1.0);

				if (d != null) {
					this.timeListOfDriving.set(bin, this.timeListOfDriving.get(bin) + relativeOverlap);
				}

				if (p != null) {
					this.timeListOfLocation2visits.get(bin).compute(p.getLocation(),
							(l, c) -> c == null ? relativeOverlap : c + relativeOverlap);

					final double effectiveCharging_kWh = effectiveChargingRate_kW * overlap_h;
					this.chargedDetail_kWh += effectiveCharging_kWh;
					this.timeListOfLocation2chargings_kWh.get(bin).compute(p.getLocation(),
							(l, c) -> c == null ? effectiveCharging_kWh : c + effectiveCharging_kWh);
				}
			}

		}

		/*
		 * 
		 * if (state.locationCnt() > 1) { for (int i = 0; i < state.locationCnt(); i++)
		 * {
		 * 
		 * DrivingEpisode d = (DrivingEpisode) state.getEpisodes().get(2 * i + 1); final
		 * List<Tuple<Double, Double>> intervals =
		 * RoundTripUtils.effectiveIntervals(d.getStartTime_h(), d.getEndTime_h());
		 * 
		 * for (int bin = 0; bin < this.scenario.getBinCnt(); bin++) { final double
		 * binStart_h = this.scenario.getBinSize_h() * bin; final double binEnd_h =
		 * binStart_h + this.scenario.getBinSize_h(); double overlap_h = 0.0; for
		 * (Tuple<Double, Double> interval : intervals) { overlap_h +=
		 * MathHelpers.overlap(binStart_h, binEnd_h, interval.getA(), interval.getB());
		 * } // final double overlap_h = MathHelpers.overlap(binStart_h, binEnd_h,
		 * d.getStartTime_h(), // d.getStartTime_h() + d.getDuration_h());
		 * 
		 * final double relativeOverlap = overlap_h / (binEnd_h - binStart_h); assert
		 * (relativeOverlap >= 0.0); assert (relativeOverlap <= 1.0);
		 * this.timeListOfDriving.set(bin, this.timeListOfDriving.get(bin) +
		 * relativeOverlap); } } }
		 * 
		 * for (int i = 0; i < state.locationCnt(); i++) {
		 * 
		 * ParkingEpisode p = (ParkingEpisode) state.getEpisodes().get(2 * i); final
		 * List<Tuple<Double, Double>> intervals =
		 * RoundTripUtils.effectiveIntervals(p.getStartTime_h(), p.getEndTime_h());
		 * 
		 * final double effectiveParkingDuration_h =
		 * RoundTripUtils.effectiveDuration_h(p); final double effectiveChargingRate_kW;
		 * if (effectiveParkingDuration_h > 1e-8) { effectiveChargingRate_kW =
		 * Math.max(0.0, p.getChargeAtEnd_kWh() - p.getChargeAtStart_kWh()) /
		 * effectiveParkingDuration_h; } else { effectiveChargingRate_kW = 0.0; }
		 * 
		 * for (int bin = 0; bin < this.scenario.getBinCnt(); bin++) { final double
		 * binStart_h = this.scenario.getBinSize_h() * bin; final double binEnd_h =
		 * binStart_h + this.scenario.getBinSize_h(); double overlap_h = 0.0; for
		 * (Tuple<Double, Double> interval : intervals) { overlap_h +=
		 * MathHelpers.overlap(binStart_h, binEnd_h, interval.getA(), interval.getB());
		 * } final double relativeOverlap = overlap_h / (binEnd_h - binStart_h); assert
		 * (relativeOverlap >= 0.0); assert (relativeOverlap <= 1.0);
		 * this.timeListOfLocation2visits.get(bin).compute(p.getLocation(), (l, c) -> c
		 * == null ? relativeOverlap : c + relativeOverlap);
		 * 
		 * final double effectiveCharging_kWh = effectiveChargingRate_kW * overlap_h;
		 * this.chargedDetail_kWh += effectiveCharging_kWh;
		 * this.timeListOfLocation2chargings_kWh.get(bin).compute(p.getLocation(), (l,
		 * c) -> c == null ? effectiveCharging_kWh : c + effectiveCharging_kWh); } }
		 * 
		 */

		double myCharged_kWh = 0.0;
		double myUsed_kWh = 0.0;

		for (int i = 0; i < state.getEpisodes().size(); i += 2) {
			ParkingEpisode e = (ParkingEpisode) state.getEpisodes().get(i);
			myCharged_kWh += e.getChargeAtEnd_kWh() - e.getChargeAtStart_kWh();
		}

		for (int i = 1; i < state.getEpisodes().size(); i += 2) {
			DrivingEpisode e = (DrivingEpisode) state.getEpisodes().get(i);
			myUsed_kWh += e.getChargeAtStart_kWh() - e.getChargeAtEnd_kWh();
		}

		this.charged_kWh += myCharged_kWh;
		this.used_kWh += myUsed_kWh;
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

		for (Map<Location, Double> location2visits : this.timeListOfLocation2visits) {
			for (Location l : locations) {
				System.out.print(location2visits.getOrDefault(l, 0.0) + "\t");
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

		for (Map<Location, Double> location2chargings : this.timeListOfLocation2chargings_kWh) {
			for (Location l : locations) {
				System.out.print(location2chargings.getOrDefault(l, 0.0) + "\t");
			}
			System.out.println();
		}

		System.out.println();
		for (Location l : locations) {
			System.out.println(l + "\t" + this.location2isHomeCnt.getOrDefault(l, 0l));
		}

		System.out.println();
		System.out.println("Drivings");
		for (Double cnt : this.timeListOfDriving) {
			System.out.println(cnt);
		}

		System.out.println();
		System.out.println("used            " + this.used_kWh);
		System.out.println("charged         " + this.charged_kWh);
		System.out.println("charged,detail  " + this.chargedDetail_kWh);

//		System.out.println();
//		for (Map.Entry<List<Location>, Long> e : this.sequence2uses.entrySet()) {
//			System.out.println(e.getKey() + "\t" + e.getValue());
//		}

	}

}
