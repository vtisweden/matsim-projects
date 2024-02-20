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
import java.util.Map;

import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author GunnarF
 *
 */
public interface ConsolidationSlotChoiceModel {

	public class Slot {
		public final Vehicle vehicle;
		public final int day;

		public Slot(Vehicle vehicle, int day) {
			this.vehicle = vehicle;
			this.day = day;
		}
	}

	public double getFixedCost(Vehicle vehicle, int day);

	public double getCost_1_ton(Vehicle vehicle, int day, ShipmentVehicleAssignment assignment);

	default public double getUtility(double maxAmount_ton, Vehicle vehicle, int day,
			ShipmentVehicleAssignment assignment) {
		final double potentiallyAssigned_ton = Math.min(maxAmount_ton, assignment.getRemainingCapacity_ton(vehicle));
		return (this.getFixedCost(vehicle, day)
				+ this.getCost_1_ton(vehicle, day, assignment) * potentiallyAssigned_ton)
				/ Math.max(1e-8, potentiallyAssigned_ton);
	}

	public Slot drawSlot(Shipment shipment, List<Map<Vehicle, Double>> vehicle2utilityOverDays);

}
