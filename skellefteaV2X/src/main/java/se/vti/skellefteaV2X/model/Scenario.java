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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class Scenario {

	private final double chargingRate_kW;
	private final double maxCharge_kWh;
	private final double consumptionRate_kWh_km;
	private final double defaultSpeed_km_h;
	private final int binCnt;

	private final Set<Location> locations = new LinkedHashSet<>();

	private final Map<Tuple<Location, Location>, Double> od2distance_km = new LinkedHashMap<>();

	private final Map<Tuple<Location, Location>, Double> od2time_h = new LinkedHashMap<>();

	public Scenario(double chargingRate_kW, double maxCharge_kWh, double consumptionRate_kWh_km,
			double defaultSpeed_km_h, int binCnt) {
		this.chargingRate_kW = chargingRate_kW;
		this.maxCharge_kWh = maxCharge_kWh;
		this.consumptionRate_kWh_km = consumptionRate_kWh_km;
		this.defaultSpeed_km_h = defaultSpeed_km_h;
		this.binCnt = binCnt;
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
		return binCnt;
	}

}
