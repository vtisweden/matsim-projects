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
package se.vti.samgods.transportation.pricing;

import se.vti.samgods.logistics.choicemodel.AnnualShipmentCost;

/**
 * 
 * @author GunnarF
 *
 */
public class BasicShipmentCost implements AnnualShipmentCost {

	private final double duration_h;
	private final double monetaryCost;

	public BasicShipmentCost(double duration_h, double monetaryCost) {
		this.duration_h = duration_h;
		this.monetaryCost = monetaryCost;
	}

	@Override
	public double getMonetaryCost() {
		return this.monetaryCost;
	}

	@Override
	public double getDuration_h() {
		return this.duration_h;
	}

}
