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
import se.vti.samgods.logistics.choicemodel.ShipmentCostFunction.ShipmentCost;

/**
 * 
 * @author GunnarF
 *
 */
public class Alternative {

	// -------------------- PUBLIC CONSTANTS --------------------

	public final SizeClass sizeClass;

	public final Shipment shipment;

	public final ShipmentCost cost;

	public final double utility;

	// -------------------- CONSTRUCTION --------------------

	public Alternative(final SizeClass sizeClass, final Shipment shipment, final ShipmentCost cost,
			final double utility) {
		this.sizeClass = sizeClass;
		this.shipment = shipment;
		this.cost = cost;
		this.utility = utility;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public String toString() {
		return "TODO";
	}
}