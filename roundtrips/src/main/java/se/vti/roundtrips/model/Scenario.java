/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.single.Location;

/**
 * 
 * @author GunnarF
 *
 */
public class Scenario<L extends Location> {

	private final Random rnd = new Random();
	
	private final LocationFactory<L> locationFactory;

	private int timeBinCnt = 24;
	private int maxParkingEpisodes = 4;

	// private final Set<L> locations = new LinkedHashSet<>();
	private final Map<String, L> name2location = new LinkedHashMap<>();

	private final Map<Tuple<L, L>, Double> od2distance_km = new LinkedHashMap<>();

	private final Map<Tuple<L, L>, Double> od2time_h = new LinkedHashMap<>();

	private List<L> locationsView = Collections.unmodifiableList(new ArrayList<>(0));
	
	public Scenario(LocationFactory<L> locationFactory) {
		this.locationFactory = locationFactory;
	}

	public L createAndAddLocation(String name) {
		L result = this.locationFactory.createLocation(name);
		// this.locations.add(result);
		this.name2location.put(name, result);
		this.locationsView = Collections.unmodifiableList(new ArrayList<>(this.name2location.values()));
		return result;
	}

	public Random getRandom() {
		return this.rnd;
	}

	public List<L> getLocationsView() {
		return this.locationsView;
	}

	public int getLocationCnt() {
//		return this.locations.size();
		return this.name2location.size();
	}

	public L getLocation(String name) {
		return this.name2location.get(name);
	}

	public void setDistance_km(L from, L to, double dist_km) {
		Tuple<L, L> od = new Tuple<>(from, to);
		this.od2distance_km.put(od, dist_km);
	}

	public void setSymmetricDistance_km(L loc1, L loc2, double dist_km) {
		this.setDistance_km(loc1, loc2, dist_km);
		this.setDistance_km(loc2, loc1, dist_km);
	}

	public void setDistance_km(String from, String to, double dist_km) {
		this.setDistance_km(this.name2location.get(from), this.name2location.get(to), dist_km);
	}

	public void setSymmetricDistance_km(String from, String to, double dist_km) {
		this.setDistance_km(from, to, dist_km);
		this.setDistance_km(to, from, dist_km);
	}

	public Double getDistance_km(L from, L to) {
		return this.od2distance_km.get(new Tuple<>(from, to));
	}

	public void setTime_h(L from, L to, double time_h) {
		this.od2time_h.put(new Tuple<>(from, to), time_h);
	}

	public void setSymmetricTime_h(L loc1, L loc2, double time_h) {
		this.setTime_h(loc1, loc2, time_h);
		this.setTime_h(loc2, loc1, time_h);
	}

	public void setTime_h(String from, String to, double time_h) {
		this.od2time_h.put(new Tuple<>(this.name2location.get(from), this.name2location.get(to)), time_h);
	}

	public void setSymmetricTime_h(String from, String to, double time_h) {
		this.setTime_h(from, to, time_h);
		this.setTime_h(to, from, time_h);
	}

	public Double getTime_h(L from, L to) {
		return this.od2time_h.get(new Tuple<>(from, to));
	}

	public double getBinSize_h() {
		return 24.0 / this.getBinCnt();
	}

	public int getBinCnt() {
		return this.timeBinCnt;
	}

	public void setTimeBinCnt(int timeBinCnt) {
		this.timeBinCnt = timeBinCnt;
	}

	public void setMaxParkingEpisodes(int maxParkingEpisodes) {
		this.maxParkingEpisodes = maxParkingEpisodes;
	}

	public int getMaxParkingEpisodes() {
		return this.maxParkingEpisodes;
	}
}
