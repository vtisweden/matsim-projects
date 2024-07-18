/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

/**
 * 
 * @author GunnarF
 *
 */
public class NonTransportCost {

	public final double annualAmount_ton;

	public final double frequency_1_yr;

	public final double totalOrderCost;

	public final double totalEnRouteLoss;

	public final double totalInventoryCost;

	public NonTransportCost(double annualAmount_ton, double frequency_1_yr, double totalOrderCost,
			double totalEnRouteLoss, double totalInventoryCost) {
		this.annualAmount_ton = annualAmount_ton;
		this.frequency_1_yr = frequency_1_yr;
		this.totalOrderCost = totalOrderCost;
		this.totalInventoryCost = totalInventoryCost;
		this.totalEnRouteLoss = totalEnRouteLoss;
	}
}
