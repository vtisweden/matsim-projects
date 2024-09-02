/**
 * se.vti.samgods
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation.costs;

/**
 * 
 * @author GunnarF
 *
 */
public class BasicTransportCost {

	public final double amount_ton;
	public final double monetaryCost;
	public final double duration_h;
	public final double length_km;

	public BasicTransportCost(double amount_ton, double cost, double duration_h, double length_km) {
		this.amount_ton = amount_ton;
		this.monetaryCost = cost;
		this.duration_h = duration_h;
		this.length_km = length_km;
	}

//	public BasicTransportCost computeUnitCost_1_ton() {
//		return new BasicTransportCost(1.0, this.monetaryCost / this.amount_ton, this.duration_h, this.length_km);
//	}
}
