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

import java.util.List;

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public interface VehicleConsolidationCostModel {

	class AssignmentCost {
		public final boolean feasible;
		public final double amount_ton;
		public final double cost;

		public AssignmentCost(boolean feasible, double amount_ton, double cost) {
			this.feasible = feasible;
			this.amount_ton = amount_ton;
			this.cost = cost;
		}
	}

	AssignmentCost getCost(Vehicle vehicle, List<Shipment> alreadyPresentShipments,
			SamgodsConstants.Commodity addedCommodity, double maxAddedAmount_ton, ShipmentVehicleAssignment assignment);

}
