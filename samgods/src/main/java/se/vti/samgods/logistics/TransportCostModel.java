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
package se.vti.samgods.logistics;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.legacy.Samgods.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public interface TransportCostModel {

	public interface UnitCost {

		public Double getTransportCost_1_ton();

		public Double getTransportDuration_h();
	}

	public UnitCost getUnitCost(Id<Node> node);

	public UnitCost getUnitCost(TransportLeg leg);

	public double getMonetaryValue_1_ton(Commodity commodity);	
	
	public ShipmentCost computeCost(Shipment shipment);

}
