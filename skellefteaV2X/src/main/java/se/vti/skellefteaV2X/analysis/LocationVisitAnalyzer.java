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

	private final String fileName;

	private List<Map<Location, Double>> timeListOfLocation2visits;
	private List<Map<Location, Double>> timeListOfLocation2chargings_kWh;
	private List<Double> timeListOfDriving;

	private Map<List<Location>, Double> sequence2uses = new LinkedHashMap<>();

	private Map<Location, Double> location2isHomeCnt = new LinkedHashMap<>();
	private Map<Location, Double> location2isOvernightCharging = new LinkedHashMap<>();
	private Map<Location, Double> location2isEnTourCharging = new LinkedHashMap<>();

	double used_kWh = 0.0;
	double charged_kWh = 0.0;

	public LocationVisitAnalyzer(Scenario scenario, long burnInIterations, long samplingInterval, String fileName,
			Preferences importanceSamplingPreferences) {
		super(scenario, burnInIterations, samplingInterval, importanceSamplingPreferences);
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
		this(scenario, burnInIterations, samplingInterval, fileName, new Preferences());
	}

	@Override
	public void processRelevantState(SimulatedRoundTrip state, double sampleWeight) {

		final ParkingEpisode home = (ParkingEpisode) state.getEpisodes().get(0);

		this.sequence2uses.compute(state.getLocationsView(), (s, c) -> c == null ? sampleWeight : c + sampleWeight);
		this.location2isHomeCnt.compute(state.getLocation(0), (l, c) -> c == null ? sampleWeight : c + sampleWeight);

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

			if (chargeChangeRate_kW > 0) {
				if (home.equals(e)) {
					this.location2isOvernightCharging.compute(parking.getLocation(),
							(l, c) -> c == null ? sampleWeight : c + sampleWeight);
				} else {
					this.location2isEnTourCharging.compute(parking.getLocation(),
							(l, c) -> c == null ? sampleWeight : c + sampleWeight);
				}
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

					final double weightedRelativeOverlap;
					{
						final double relativeOverlap = overlap_h / this.scenario.getBinSize_h();
						assert (relativeOverlap >= 0.0);
						assert (relativeOverlap <= 1.0);
						weightedRelativeOverlap = relativeOverlap * sampleWeight;
					}

					if (driving != null) {
						this.timeListOfDriving.set(bin, this.timeListOfDriving.get(bin) + weightedRelativeOverlap);
					}

					if (parking != null) {
						this.timeListOfLocation2visits.get(bin).compute(parking.getLocation(),
								(l, c) -> c == null ? weightedRelativeOverlap : c + weightedRelativeOverlap);

						final double weightedEffectiveCharging_kWh = chargeChangeRate_kW * weightedRelativeOverlap;
						this.timeListOfLocation2chargings_kWh.get(bin).compute(parking.getLocation(), (l,
								c) -> c == null ? weightedEffectiveCharging_kWh : c + weightedEffectiveCharging_kWh);
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

		this.charged_kWh += sampleWeight * myCharged_kWh;
		this.used_kWh += sampleWeight * myUsed_kWh;
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
					writer.print(location2visits.getOrDefault(l, 0.0) / sampleWeightSum() + "\t");
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
					writer.print(location2chargings.getOrDefault(l, 0.0) / sampleWeightSum() + "\t");
				}
				writer.println();
			}

			writer.println();

			writer.println("LOCATION IS HOME");
			writer.println();
			for (Location l : locations) {
				writer.println(l + "\t" + this.location2isHomeCnt.getOrDefault(l, 0.0) / sampleWeightSum());
			}

			writer.println();
			writer.println("DRIVING");
			writer.println();
			for (Double cnt : this.timeListOfDriving) {
				writer.println(cnt / sampleWeightSum());
			}

			writer.println();
			writer.println("LOCATION IS CHARGING");
			writer.println();
			writer.println("\tovernight\ten route");
			for (Location l : locations) {
				writer.println(l + "\t" + this.location2isOvernightCharging.getOrDefault(l, 0.0) / sampleWeightSum()
						+ "\t" + this.location2isEnTourCharging.getOrDefault(l, 0.0) / sampleWeightSum());
			}

			writer.println();
			writer.println("REL. NUMBER OF CHARGING EPISODES");
			writer.println();
			writer.println(this.location2isOvernightCharging.values().stream().mapToDouble(f -> f).sum()
					/ sampleWeightSum()
					+ this.location2isEnTourCharging.values().stream().mapToDouble(f -> f).sum() / sampleWeightSum());

			writer.println();
			writer.println("ENERGY CONSUMPTION");
			writer.println();
			writer.println("used            " + this.used_kWh);
			writer.println("charged         " + this.charged_kWh);

			writer.println();
			writer.println("ACCEPTANCE RATE");
			writer.println();
			writer.println(this.acceptanceRate());
			
			writer.println();
			writer.println("LOCATION SEQUENCES");
			writer.println();
			writer.println();
			for (Map.Entry<List<Location>, Double> e : this.sequence2uses.entrySet()) {
				writer.println(e.getKey() + "\t" + e.getValue() / sampleWeightSum());
			}

			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
