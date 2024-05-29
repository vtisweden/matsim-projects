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
package se.vti.samgods.transportation.consolidation.road;

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.logistics.TransportEpisode;

/**
 * 
 * @author GunnarF
 *
 */
public interface ConsolidationCostModel {

	class RealizedCost {
		public final double amount_ton;
		public final double monetaryCost;
		public final double duration_h;

		public RealizedCost(double amount_ton, double cost, double duration_h) {
			this.amount_ton = amount_ton;
			this.monetaryCost = cost;
			this.duration_h = duration_h;
		}
	}

	RealizedCost getVehicleCost(Vehicle vehicle, double payload_ton, TransportEpisode episode);

	RealizedCost getShipmentCost(Vehicle vehicle, double maxAddedAmount_ton, ShipmentVehicleAssignment assignment);

}
