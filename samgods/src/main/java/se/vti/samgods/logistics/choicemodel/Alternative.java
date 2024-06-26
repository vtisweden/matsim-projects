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

import floetteroed.utilities.math.MathHelpers;
import se.vti.samgods.logistics.AnnualShipment;

/**
 * 
 * @author GunnarF
 *
 */
public class Alternative<C extends AnnualShipmentCost> {

	// -------------------- PUBLIC CONSTANTS --------------------

	public final SizeClass sizeClass;

	public final AnnualShipment shipment;

	public final C cost;

	public final double utility;

	// -------------------- CONSTRUCTION --------------------

	public Alternative(final SizeClass sizeClass, final AnnualShipment shipment, final C cost,
			final double utility) {
		this.sizeClass = sizeClass;
		this.shipment = shipment;
		this.cost = cost;
		this.utility = utility;
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("commodity " + shipment.getCommmodity() + " of total size "
				+ MathHelpers.round(this.shipment.getTotalAmount_ton(), 2) + " ton in relation "
				+ shipment.getTransportChain().getOriginNodeId() + "/"
				+ shipment.getTransportChain().getDestinationNodeId() + ": ");
		result.append("shipmentSizeClass " + sizeClass + ", chain " + shipment.getModeSequence());
		return result.toString();
	}
}
