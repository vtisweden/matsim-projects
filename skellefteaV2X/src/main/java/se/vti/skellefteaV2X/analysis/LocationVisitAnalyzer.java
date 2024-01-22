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
import java.util.Map.Entry;
import java.util.stream.Collectors;

import floetteroed.utilities.Tuple;
import se.vti.skellefteaV2X.model.DrivingEpisode;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;
import se.vti.skellefteaV2X.preferences.consistency.AllDayBatteryConstraintPreference;
import se.vti.skellefteaV2X.preferences.consistency.AllDayTimeConstraintPreference;
import se.vti.skellefteaV2X.preferences.consistency.NonnegativeBatteryStatePreference;
import se.vti.skellefteaV2X.preferences.consistency.StrategyRealizationConsistency;
import se.vti.utils.misc.math.MathHelpers;

/**
 * 
 * @author GunnarF
 *
 */
public class LocationVisitAnalyzer extends SimulatedRoundTripAnalyzer {

	private final MathHelpers math = new MathHelpers();

	private final String fileName;

	private List<Map<Location, Double>> timeListOfLocation2homeVisits;
	private List<Map<Location, Double>> timeListOfLocation2homeChargings_kWh;

	private List<Map<Location, Double>> timeListOfLocation2enRouteVisits;
	private List<Map<Location, Double>> timeListOfLocation2enRouteChargings_kWh;

	private List<Double> timeListOfDriving;

	private Map<List<Location>, Double> sequence2uses = new LinkedHashMap<>();

	private Map<Location, Double> location2isHomeCnt = new LinkedHashMap<>();
	private Map<Location, Double> location2isOvernightCharging = new LinkedHashMap<>();
	private Map<Location, Double> location2isEnTourCharging = new LinkedHashMap<>();

	private final double[] sizeCnt;
	
	double used_kWh = 0.0;
	double charged_kWh = 0.0;
	
	private AllDayBatteryConstraintPreference batteryWrapAround;
	private AllDayTimeConstraintPreference timeWrapAround;
	private NonnegativeBatteryStatePreference nonnegativeBattery;
	private StrategyRealizationConsistency consistentRealization;
	
	double batteryWrapAroundDiscrepancy_kWh = 0.0;
	double timeWrapAroundDiscrepancy_h = 0.0;
	double nonnegativeBatteryDiscrepancy_kWh = 0.0;
	double ralizationDiscrepancy_h = 0.0;

	public LocationVisitAnalyzer(Scenario scenario, long burnInIterations, long samplingInterval, String fileName,
			Preferences importanceSamplingPreferences) {
		super(scenario, burnInIterations, samplingInterval, importanceSamplingPreferences);
		this.fileName = fileName;
		this.timeListOfLocation2homeVisits = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfLocation2homeChargings_kWh = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfLocation2enRouteVisits = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfLocation2enRouteChargings_kWh = new ArrayList<>(scenario.getBinCnt());
		this.timeListOfDriving = new ArrayList<>(scenario.getBinCnt());

		this.sizeCnt = new double[scenario.getBinCnt() + 1];
		
		for (int i = 0; i < scenario.getBinCnt(); i++) {
			this.timeListOfLocation2homeVisits.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfLocation2homeChargings_kWh.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfLocation2enRouteVisits.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfLocation2enRouteChargings_kWh.add(new LinkedHashMap<>(scenario.getLocationCnt()));
			this.timeListOfDriving.add(0.0);
		}
		
		this.batteryWrapAround = new AllDayBatteryConstraintPreference(scenario);
		this.timeWrapAround = new AllDayTimeConstraintPreference();
		this.nonnegativeBattery = new NonnegativeBatteryStatePreference(scenario);
		this.consistentRealization = new StrategyRealizationConsistency(scenario);
	}

	public LocationVisitAnalyzer(Scenario scenario, long burnInIterations, long samplingInterval, String fileName) {
		this(scenario, burnInIterations, samplingInterval, fileName, new Preferences());
	}

	@Override
	public void processRelevantState(SimulatedRoundTrip state, double sampleWeight) {

		this.batteryWrapAroundDiscrepancy_kWh += sampleWeight * this.batteryWrapAround.discrepancy_kWh(state);
		this.timeWrapAroundDiscrepancy_h += sampleWeight * this.timeWrapAround.discrepancy_h(state);
		this.nonnegativeBatteryDiscrepancy_kWh += sampleWeight * this.nonnegativeBattery.discrepancy_kWh(state);
		this.ralizationDiscrepancy_h += sampleWeight * this.consistentRealization.discrepancy_h(state);
		
		this.sizeCnt[state.locationCnt()] += sampleWeight;
		
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
						final double weightedEffectiveCharging_kWh = chargeChangeRate_kW * weightedRelativeOverlap;
						if (parking.equals(home)) {
							this.timeListOfLocation2homeVisits.get(bin).compute(parking.getLocation(),
									(l, c) -> c == null ? weightedRelativeOverlap : c + weightedRelativeOverlap);
							this.timeListOfLocation2homeChargings_kWh.get(bin).compute(parking.getLocation(),
									(l, c) -> c == null ? weightedEffectiveCharging_kWh
											: c + weightedEffectiveCharging_kWh);
						} else {
							this.timeListOfLocation2enRouteVisits.get(bin).compute(parking.getLocation(),
									(l, c) -> c == null ? weightedRelativeOverlap : c + weightedRelativeOverlap);
							this.timeListOfLocation2enRouteChargings_kWh.get(bin).compute(parking.getLocation(),
									(l, c) -> c == null ? weightedEffectiveCharging_kWh
											: c + weightedEffectiveCharging_kWh);
						}
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

			writer.println("Pr[home parking]");
			writer.println();
			for (Location l : locations) {
				writer.print(l + "\t");
			}
			writer.println();
			for (Map<Location, Double> location2visits : this.timeListOfLocation2homeVisits) {
				for (Location l : locations) {
					writer.print(location2visits.getOrDefault(l, 0.0) / acceptedSampleWeightSum() + "\t");
				}
				writer.println();
			}

			writer.println();

			writer.println("E[charging at home] [kW]");
			writer.println();
			for (Location l : locations) {
				writer.print(l + "\t");
			}
			writer.println();
			for (Map<Location, Double> location2chargings : this.timeListOfLocation2homeChargings_kWh) {
				for (Location l : locations) {
					writer.print(location2chargings.getOrDefault(l, 0.0) / acceptedSampleWeightSum() + "\t");
				}
				writer.println();
			}

			writer.println();

			writer.println("Pr[parking en route]");
			writer.println();
			for (Location l : locations) {
				writer.print(l + "\t");
			}
			writer.println();
			for (Map<Location, Double> location2visits : this.timeListOfLocation2enRouteVisits) {
				for (Location l : locations) {
					writer.print(location2visits.getOrDefault(l, 0.0) / acceptedSampleWeightSum() + "\t");
				}
				writer.println();
			}

			writer.println();

			writer.println("E[charging en route] [kW]");
			writer.println();
			for (Location l : locations) {
				writer.print(l + "\t");
			}
			writer.println();
			for (Map<Location, Double> location2chargings : this.timeListOfLocation2enRouteChargings_kWh) {
				for (Location l : locations) {
					writer.print(location2chargings.getOrDefault(l, 0.0) / acceptedSampleWeightSum() + "\t");
				}
				writer.println();
			}

			writer.println();

			writer.println("Pr[location is home]");
			writer.println();
			for (Location l : locations) {
				writer.println(l + "\t" + this.location2isHomeCnt.getOrDefault(l, 0.0) / acceptedSampleWeightSum());
			}

			writer.println();
			writer.println("Pr(driving)");
			writer.println();
			for (Double cnt : this.timeListOfDriving) {
				writer.println(cnt / acceptedSampleWeightSum());
			}

			writer.println();
			writer.println("Pr[charging at location]");
			writer.println();
			writer.println("\tovernight\ten route");
			for (Location l : locations) {
				writer.println(l + "\t"
						+ this.location2isOvernightCharging.getOrDefault(l, 0.0) / acceptedSampleWeightSum() + "\t"
						+ this.location2isEnTourCharging.getOrDefault(l, 0.0) / acceptedSampleWeightSum());
			}

			writer.println();
			writer.println("REL. NUMBER OF CHARGING EPISODES");
			writer.println();
			writer.println(this.location2isOvernightCharging.values().stream().mapToDouble(f -> f).sum()
					/ acceptedSampleWeightSum()
					+ this.location2isEnTourCharging.values().stream().mapToDouble(f -> f).sum()
							/ acceptedSampleWeightSum());

			writer.println();
			writer.println("CONSISTENCY DEVIATIONS");
			writer.println();
			writer.println("battery wrap-around [kWh]\t" + this.batteryWrapAroundDiscrepancy_kWh / acceptedSampleWeightSum());
			writer.println("nonnegative battery [kWh]\t" + this.nonnegativeBatteryDiscrepancy_kWh / acceptedSampleWeightSum());
			writer.println("time wrap-around      [h]\t" + this.timeWrapAroundDiscrepancy_h / acceptedSampleWeightSum());
			writer.println("strategy consistency  [h]\t" + this.ralizationDiscrepancy_h / acceptedSampleWeightSum());
			
			writer.println();
			writer.println("ENERGY CONSUMPTION");
			writer.println();
			writer.println("used            " + this.used_kWh / acceptedSampleWeightSum());
			writer.println("charged         " + this.charged_kWh / acceptedSampleWeightSum());

			writer.println();
			writer.println("ACCEPTANCE RATE");
			writer.println();
			writer.println(this.acceptanceRate());

			writer.println();
			writer.println("LOCATION COUNT DISTRIBUTION");
			writer.println();
			for (int i = 0; i < this.sizeCnt.length; i++) {
				System.out.println(i+"\t" + this.sizeCnt[i] / this.acceptedSampleWeightSum());
			}
			System.out.println();
			
			
			final List<Map.Entry<List<Location>, Double>> usedLocs = this.sequence2uses.entrySet().stream()
					.collect(Collectors.toList());
			Collections.sort(usedLocs, new Comparator<Map.Entry<List<Location>, Double>>() {
				@Override
				public int compare(Entry<List<Location>, Double> e1, Entry<List<Location>, Double> e2) {
					return -e1.getValue().compareTo(e2.getValue());
				}
			});

			writer.println();
			writer.println("LOCATION SEQUENCES");
			writer.println();
			writer.println("locations\tprobability\tcumulative probability");
			double cumulativeProba = 0.0;
			for (Map.Entry<List<Location>, Double> e : usedLocs) {
				final double proba = e.getValue() / acceptedSampleWeightSum();
				cumulativeProba += proba;
				writer.println(e.getKey() + "\t" + proba + "\t" + cumulativeProba);
			}

			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
