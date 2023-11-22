/**
 * se.vti.samgods
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
package se.vti.samgods.transportation;

import se.vti.samgods.legacy.Samgods;

/**
 * 
 * @author GunnarF
 *
 */
public class FreightVehicleAttributes {
	
	// -------------------- CONSTANTS --------------------

	public static final String ATTRIBUTE_NAME = "freight";

	private final Samgods.TransportMode transportMode;

	private final double capacity_ton;

	// -------------------- MEMBERS --------------------

	private Double fixedCost_1_h = null;

	private Double fixedCost_1_km = null;

	private Double unitCost_1_hTon = null;

	private Double unitCost_1_kmTon = null;

	// -------------------- CONSTRUCTION --------------------

	public FreightVehicleAttributes(final Samgods.TransportMode transportMode, final double capacity_ton) {
		this.transportMode = transportMode;
		this.capacity_ton = capacity_ton;
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public Samgods.TransportMode getTransportMode() {
		return this.transportMode;
	}
	
	public double getCapacity_ton() {
		return this.capacity_ton;
	}
	
	public Double getFixedCost_1_h() {
		return fixedCost_1_h;
	}

	public void setFixedCost_1_h(Double fixedCost_1_h) {
		this.fixedCost_1_h = fixedCost_1_h;
	}

	public Double getFixedCost_1_km() {
		return fixedCost_1_km;
	}

	public void setFixedCost_1_km(Double fixedCost_1_km) {
		this.fixedCost_1_km = fixedCost_1_km;
	}

	public Double getUnitCost_1_hTon() {
		return unitCost_1_hTon;
	}

	public void setUnitCost_1_hTon(Double unitCost_1_hTon) {
		this.unitCost_1_hTon = unitCost_1_hTon;
	}

	public Double getUnitCost_1_kmTon() {
		return unitCost_1_kmTon;
	}

	public void setUnitCost_1_kmTon(Double unitCost_1_kmTon) {
		this.unitCost_1_kmTon = unitCost_1_kmTon;
	}
}
