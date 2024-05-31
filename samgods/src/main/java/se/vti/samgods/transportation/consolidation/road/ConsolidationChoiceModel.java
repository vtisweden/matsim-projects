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
import java.util.Random;

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.TransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationChoiceModel {

	public static class Slot {
		public final Vehicle vehicle;
		public final int day;

		public Slot(Vehicle vehicle, int day) {
			this.vehicle = vehicle;
			this.day = day;
		}
	}

	private final double scale;

	private final Random rnd;

	public ConsolidationChoiceModel(final double scale, final Random rnd) {
		this.scale = scale;
		this.rnd = rnd;
	}

	public Slot drawSlot(Shipment shipment, List<Map<Vehicle, TransportCost>> vehicle2costOverDays) {

		double maxUtility = Double.NEGATIVE_INFINITY;
		for (Map<Vehicle, TransportCost> vehicle2cost : vehicle2costOverDays) {
			for (TransportCost cost : vehicle2cost.values()) {
				if (cost != null) { // TODO do not store null values
					maxUtility = Math.max(maxUtility, (-cost.monetaryCost));
				}
			}
		}

		double denom = 0.0;
		for (Map<Vehicle, TransportCost> vehicle2cost : vehicle2costOverDays) {
			for (TransportCost cost : vehicle2cost.values()) {
				if (cost != null) { // TODO do not store null values
					denom += Math.exp(this.scale * ((-cost.monetaryCost) - maxUtility));
				}
			}
		}

		final double threshold = this.rnd.nextDouble() * denom;
		double sum = 0.0;
		for (int day = 0; day < vehicle2costOverDays.size(); day++) {
			for (Map.Entry<Vehicle, TransportCost> veh2cost : vehicle2costOverDays.get(day).entrySet()) {
				if (veh2cost.getValue() != null) {
					sum += Math.exp(this.scale * ((-veh2cost.getValue().monetaryCost) - maxUtility));
					if (sum >= threshold) {
						return new Slot(veh2cost.getKey(), day);
					}
				}
			}
		}

		// Happens at most very rarely and for numerical reasons.
		// Hedge against that in calling function.
		return null;
	}
}
