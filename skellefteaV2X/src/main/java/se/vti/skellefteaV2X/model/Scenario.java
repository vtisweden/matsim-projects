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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import floetteroed.utilities.Tuple;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.skellefteaV2X.roundtrips.RoundTripConfiguration;
import se.vti.skellefteaV2X.roundtrips.RoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

/**
 * 
 * @author GunnarF
 *
 */
public class Scenario {

	private final Random rnd;

	private double chargingRate_kW = 11.0;
	private double maxCharge_kWh = 60.0;
	private double consumptionRate_kWh_km = 0.2;

	private double defaultSpeed_km_h = 60.0;
	private int timeBinCnt = 24;

	private final Set<Location> locations = new LinkedHashSet<>();

	private final Map<Tuple<Location, Location>, Double> od2distance_km = new LinkedHashMap<>();

	private final Map<Tuple<Location, Location>, Double> od2time_h = new LinkedHashMap<>();

	public Scenario(Random rnd) {
		this.rnd = rnd;
	}

	public Scenario() {
		this(new Random());
	}

	public Location createAndAddLocation(String name, boolean canCharge) {
		Location result = new Location(name, canCharge);
		this.locations.add(result);
		return result;
	}

	public Set<Location> getLocationsView() {
		return Collections.unmodifiableSet(this.locations);
	}

	public void setDistance_km(Location from, Location to, double dist_km) {
		Tuple<Location, Location> od = new Tuple<>(from, to);
		this.od2distance_km.put(od, dist_km);
		if (!this.od2time_h.containsKey(od)) {
			this.od2time_h.put(od, dist_km / this.defaultSpeed_km_h);
		}
	}

	public void setSymmetricDistance_km(Location loc1, Location loc2, double dist_km) {
		this.setDistance_km(loc1, loc2, dist_km);
		this.setDistance_km(loc2, loc1, dist_km);
	}

	public Double getDistance_km(Location from, Location to) {
		return this.od2distance_km.get(new Tuple<>(from, to));
	}

	public void setTime_h(Location from, Location to, double time_h) {
		this.od2time_h.put(new Tuple<>(from, to), time_h);
	}

	public void setSymmetricTime_h(Location loc1, Location loc2, double time_h) {
		this.setTime_h(loc1, loc2, time_h);
		this.setTime_h(loc2, loc1, time_h);
	}

	public Double getTime_h(Location from, Location to) {
		return this.od2time_h.get(new Tuple<>(from, to));
	}

	public double getBinSize_h() {
		return 24.0 / this.getBinCnt();
	}

	public double getChargingRate_kW() {
		return chargingRate_kW;
	}

	public double getMaxCharge_kWh() {
		return maxCharge_kWh;
	}

	public double getConsumptionRate_kWh_km() {
		return consumptionRate_kWh_km;
	}

	public double getSpeed_km_h() {
		return defaultSpeed_km_h;
	}

	public int getBinCnt() {
		return timeBinCnt;
	}

	public MHAlgorithm<RoundTrip<Location>> createMHAlgorithm(Preferences preferences) {

		int maxLocations = 4;
		double locationProposalWeight = 0.1;
		double departureProposalWeight = 0.45;
		double chargingProposalWeight = 0.45;

		final RoundTripConfiguration<Location> configuration = new RoundTripConfiguration<>(maxLocations, getBinCnt(),
				locationProposalWeight, departureProposalWeight, chargingProposalWeight);
		configuration.addLocations(this.getLocationsView());

		RoundTripProposal<Location> proposal = new RoundTripProposal<>(configuration);

		RoundTrip<Location> initialState = new RoundTrip<>(
				Arrays.asList(new ArrayList<>(this.locations).get(this.rnd.nextInt(this.locations.size()))),
				Arrays.asList(this.rnd.nextInt(this.timeBinCnt)), Arrays.asList(this.rnd.nextBoolean()));

		MHAlgorithm<RoundTrip<Location>> algo = new MHAlgorithm<>(proposal, preferences, new Random());
		algo.setInitialState(initialState);
		return algo;
	}

	public void setChargingRate_kW(double chargingRate_kW) {
		this.chargingRate_kW = chargingRate_kW;
	}

	public void setMaxCharge_kWh(double maxCharge_kWh) {
		this.maxCharge_kWh = maxCharge_kWh;
	}

	public void setConsumptionRate_kWh_km(double consumptionRate_kWh_km) {
		this.consumptionRate_kWh_km = consumptionRate_kWh_km;
	}

	public void setDefaultSpeed_km_h(double defaultSpeed_km_h) {
		this.defaultSpeed_km_h = defaultSpeed_km_h;
	}

	public void setTimeBinCnt(int timeBinCnt) {
		this.timeBinCnt = timeBinCnt;
	}

}
