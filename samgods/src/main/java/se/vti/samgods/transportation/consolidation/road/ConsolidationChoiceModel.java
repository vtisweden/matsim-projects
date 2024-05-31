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

import java.util.List;
import java.util.Map;

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.TransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public interface ConsolidationChoiceModel {

	class Slot {
		public final Vehicle vehicle;
		public final int day;

		public Slot(Vehicle vehicle, int day) {
			this.vehicle = vehicle;
			this.day = day;
		}
	}

	Slot drawSlot(Shipment shipment, List<Map<Vehicle, TransportCost>> vehicle2costOverDays);

}
