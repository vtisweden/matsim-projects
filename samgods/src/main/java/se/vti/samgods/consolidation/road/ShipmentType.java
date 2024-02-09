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
public abstract class ShipmentType {

	private final String name;
	
	private final boolean isSplittable;
	
	private final double density_ton_m3;
	
	public ShipmentType(final String name, final boolean isSplittable, final double density_ton_m3) {
		this.name = name;
		this.isSplittable = isSplittable;
		this.density_ton_m3 = density_ton_m3;
	}
	
	public String getName() {
		return this.name;
	}

	public boolean isSplittable() {
		return this.isSplittable;
	}

	public double computeVolume_m3(final double weight_ton) {
		return this.density_ton_m3 * weight_ton;
	}

	public abstract boolean isCompatible(VehicleType type);
	
	public abstract boolean isCompatible(ShipmentType other);

}
