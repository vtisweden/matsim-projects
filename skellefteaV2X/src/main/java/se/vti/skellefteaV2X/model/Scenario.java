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

import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.Tuple;

/**
 * 
 * @author GunnarF
 *
 */
public class Scenario {

	private final Map<String, Location> name2location = new LinkedHashMap<>();

	private final Map<Tuple<Location, Location>, Double> od2distance_km = new LinkedHashMap<>();
	
	private final double binSize_h;

	public Scenario(double binSize_h) {
		this.binSize_h = binSize_h;
	}
	
	public Location createAndAddLocation(String name, boolean canCharge) {
		Location result = new Location(name, canCharge);
		this.name2location.put(name, result);
		return result;
	}

	public void setDistance_km(Location from, Location to, double dist_km, boolean symmetric) {
		this.od2distance_km.put(new Tuple<>(from, to), dist_km);
		if (symmetric) {
			this.od2distance_km.put(new Tuple<>(to, from), dist_km);
		}
	}
	
	public Double getDistance_km(Location from, Location to) {
		return this.od2distance_km.get(new Tuple<>(from, to));
	}
	
	public double getBinSize_h() {
		return this.binSize_h;
	}
	

}
