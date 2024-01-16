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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import floetteroed.utilities.Tuple;
import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class LocationVisitAnalyzer extends SimulatedRoundTripAnalyzer {

	private final MathHelpers math = new MathHelpers();
	
	private final Preferences preferences;

	private final String fileName;

	private List<Map<Location, Double>> timeListOfLocation2visits;

	private List<Map<Location, Double>> timeListOfLocation2chargings_kWh;

	private List<Double> timeListOfDriving;

	private Map<List<Location>, Long> sequence2uses = new LinkedHashMap<>();
	private Map<Location, Long> location2isHomeCnt = new LinkedHashMap<>();

	double chargedDetail_kWh = 0.0;
	double used_kWh = 0.0;
	double charged_kWh = 0.0;

	public LocationVisitAnalyzer(Scenario scenario, Preferences preferences, long burnInIterations,
			long samplingInterval, String fileName) {
		super(scenario, burnInIterations, samplingInterval);
		this.preferences = preferences;
		this.fileName = fileName;
		this.timeListOfLocation2visits = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfLocation2chargings_kWh = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfDriving = new ArrayList<>(scenario.getBinCnt());

		for (int i = 0; i < scenario.getBinCnt(); i++) {
			this.timeListOfLocation2visits.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfLocation2chargings_kWh.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfDriving.add(0.0);
		}
	}

	public LocationVisitAnalyzer(Scenario scenario, long burnInIterations, long samplingInterval, String fileName) {
		this(scenario, new Preferences(), burnInIterations, samplingInterval, fileName);
	}

	@Override
	public void processRelevantState(SimulatedRoundTrip state) {

		if (this.preferences.logWeight(state) < -Double.MIN_VALUE) {
			return;
		}

		this.sequence2uses.compute(state.getLocationsView(), (s, c) -> c == null ? 1 : c + 1);
		this.location2isHomeCnt.compute(state.getLocation(0), (l, c) -> c == null ? 1 : c + 1);

//		List<Episode> episodes = new ArrayList<>();
//		for (int i = 0; i < state.locationCnt(); i++) {
//			episodes.add(state.getEpisodes().get(2 * i));
//			episodes.add(state.getEpisodes().get(2 * i + 1));			
//		}
//		for (int i = 0; i < episodes.size(); i++) {
//			assert(episodes.get(i) == state.getEpisodes().get(i));
//		}
		assert (state.locationCnt() == 1 || state.episodeCnt() == 2 * state.locationCnt());

		for (Episode e : state.getEpisodes()) {

			final ParkingEpisode parking = (e instanceof ParkingEpisode ? (ParkingEpisode) e : null);
			final DrivingEpisode driving = (e instanceof DrivingEpisode ? (DrivingEpisode) e : null);

			final double chargeChangeRate_kW;
			if (e.getDuration_h() > 1e-8) {
				chargeChangeRate_kW = Math.max(0.0, e.getChargeAtEnd_kWh() - e.getChargeAtStart_kWh())
						/ e.getDuration_h();
			} else {
				chargeChangeRate_kW = 0.0;
			}

			final List<Tuple<Double, Double>> intervals = e.effectiveIntervals();

			for (Tuple<Double, Double> interval : intervals) {

				assert (interval.getA() >= 0.0);
				assert (interval.getA() <= 24.0);

				assert (interval.getB() >= 0.0);
				assert (interval.getB() <= 24.0);

				int startBin = (int) (interval.getA() / this.scenario.getBinSize_h());
				int endBin = 1 + (int) (interval.getB() / this.scenario.getBinSize_h());

				for (int bin = startBin; bin <= Math.min(endBin, this.scenario.getBinCnt() - 1); bin++) {

					final double binStart_h = this.scenario.getBinSize_h() * bin;
					final double binEnd_h = binStart_h + this.scenario.getBinSize_h();

					double overlap_h = this.math.overlap(binStart_h, binEnd_h, interval.getA(), interval.getB());

					final double relativeOverlap = overlap_h / this.scenario.getBinSize_h();
					assert (relativeOverlap >= 0.0);
					assert (relativeOverlap <= 1.0);

					if (driving != null) {
						this.timeListOfDriving.set(bin, this.timeListOfDriving.get(bin) + relativeOverlap);
					}

					if (parking != null) {
						this.timeListOfLocation2visits.get(bin).compute(parking.getLocation(),
								(l, c) -> c == null ? relativeOverlap : c + relativeOverlap);

						final double effectiveCharging_kWh = chargeChangeRate_kW * overlap_h;
						this.chargedDetail_kWh += effectiveCharging_kWh;
						this.timeListOfLocation2chargings_kWh.get(bin).compute(parking.getLocation(),
								(l, c) -> c == null ? effectiveCharging_kWh : c + effectiveCharging_kWh);
					}
				}
			}
		}

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

		try {

			List<Location> locations = new ArrayList<>(this.scenario.getLocationsView());
			Collections.sort(locations, new Comparator<Location>() {
				@Override
				public int compare(Location o1, Location o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			PrintWriter writer = new PrintWriter(this.fileName);

			writer.println("VISITS");
			writer.println();
			for (Location l : locations) {
				writer.print(l + "\t");
			}
			writer.println();
			for (Map<Location, Double> location2visits : this.timeListOfLocation2visits) {
				for (Location l : locations) {
					writer.print(location2visits.getOrDefault(l, 0.0) + "\t");
				}
				writer.println();
			}

			writer.println();
			
			writer.println("CHARGINGS");
			writer.println();
			for (Location l : locations) {
				writer.print(l + "\t");
			}
			writer.println();
			for (Map<Location, Double> location2chargings : this.timeListOfLocation2chargings_kWh) {
				for (Location l : locations) {
					writer.print(location2chargings.getOrDefault(l, 0.0) + "\t");
				}
				writer.println();
			}

			writer.println();
			
			writer.println("LOCATION IS HOME");
			writer.println();
			for (Location l : locations) {
				writer.println(l + "\t" + this.location2isHomeCnt.getOrDefault(l, 0l));
			}
			
			writer.println();
			writer.println("DRIVING");
			writer.println();
			for (Double cnt : this.timeListOfDriving) {
				writer.println(cnt);
			}

			writer.println();
			writer.println("ENERGY CONSUMPTION");
			writer.println();
			writer.println("used            " + this.used_kWh);
			writer.println("charged         " + this.charged_kWh);
			writer.println("charged,detail  " + this.chargedDetail_kWh);

			writer.println();
			writer.println("LOCATION SEQUENCES");
			writer.println();
			writer.println();
			for (Map.Entry<List<Location>, Long> e : this.sequence2uses.entrySet()) {
				writer.println(e.getKey() + "\t" + e.getValue());
			}

			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
