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
package se.vti.samgods.logistics.choicemodel;

import se.vti.samgods.logistics.Shipment;

/**
 * 
 * @author GunnarF
 *
 */
public interface ShipmentCostFunction {

	public class ShipmentCost {

		public final double transportDuration_h;
		public final double transportCost;
		public final double capitalCost;
		public final double valueDensity;

		public ShipmentCost(double transportDuration_h, double transportCost, double capitalCost, double valueDensity) {
			this.transportDuration_h = transportDuration_h;
			this.transportCost = transportCost;
			this.capitalCost = capitalCost;
			this.valueDensity = valueDensity;
		}
	}

	public ShipmentCost computeCost(Shipment shipment);

}
