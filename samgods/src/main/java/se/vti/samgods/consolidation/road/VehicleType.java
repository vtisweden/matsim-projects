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
package se.vti.samgods.consolidation.road;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleType {

	private final String name;

	private final double capacity_ton;

	private final double capacity_m3;

	public VehicleType(final String name, final double capacity_ton, final double capacity_m3) {
		this.name = name;
		this.capacity_ton = capacity_ton;
		this.capacity_m3 = capacity_m3;
	}

	// Ignoring volumes
	public VehicleType(final String name, final double capacity_ton) {
		this(name, capacity_ton, Double.POSITIVE_INFINITY);
	}

	public String getName() {
		return this.name;
	}

	public double getCapacity_ton() {
		return this.capacity_ton;
	}

	public double getCapacity_m3() {
		return this.capacity_m3;
	}

	public String toString() {
		return this.name + "(capacity=" + this.getCapacity_ton() + "ton" + this.getCapacity_m3() + "m3)";
	}

}
