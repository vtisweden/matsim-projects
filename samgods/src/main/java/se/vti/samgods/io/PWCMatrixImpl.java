/**
 * org.matsim.contrib.emulation
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
package se.vti.samgods.io;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.Tuple;
import se.vti.samgods.logistics.Location;
import se.vti.samgods.logistics.PWCMatrix;
import se.vti.samgods.logistics.Samgods.Commodity;

public class PWCMatrixImpl implements PWCMatrix {

	// -------------------- MEMBERS --------------------

	private final Commodity commodity;

	private Map<Tuple<Location, Location>, Double> od2amount_ton_yr = new LinkedHashMap<>();

	private Set<Location> locations = new LinkedHashSet<>();

	// -------------------- CONSTRUCTION --------------------

	public PWCMatrixImpl(final Commodity commodity) {
		this.commodity = commodity;
	}

	// -------------------- INTERNALS --------------------

	// -------------------- IMPLEMENTATION --------------------

	public Commodity getCommodity() {
		return this.commodity;
	}
	
	public void add(final Tuple<Location, Location> od, final double amount_ton) {
		this.locations.add(od.getA());
		this.locations.add(od.getB());
		this.od2amount_ton_yr.compute(od,
				(od2, prev_ton_yr) -> (prev_ton_yr == null) ? amount_ton : prev_ton_yr + amount_ton);
	}

	public double computeTotal_ton_yr() {
		return this.od2amount_ton_yr.values().stream().mapToDouble(v -> v).sum();
	}

	// -------------------- IMPLEMENTATION OF PWCMatrix --------------------

	@Override
	public Set<Location> getLocationsView() {
		return Collections.unmodifiableSet(this.locations);
	}

	@Override
	public double getTotalFreightDemand(final Tuple<Location, Location> od) {
		return this.od2amount_ton_yr.getOrDefault(od, 0.0);
	}

}
