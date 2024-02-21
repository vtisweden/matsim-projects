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
import java.util.Random;

import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author GunnarF
 *
 */
public class LogitConsolidationChoiceModel implements ConsolidationChoiceModel {

	private final ConsolidationCostModel vehicleCost;

	private final double scale;

	private final Random rnd;

	public LogitConsolidationChoiceModel(final ConsolidationCostModel vehicleCost, final double scale,
			final Random rnd) {
		this.vehicleCost = vehicleCost;
		this.scale = scale;
		this.rnd = rnd;
	}

	@Override
	public Slot drawSlot(Shipment shipment, List<Map<Vehicle, Double>> vehicle2utilityOverDays) {

		double maxUtility = Double.NEGATIVE_INFINITY;
		for (int day = 0; day < vehicle2utilityOverDays.size(); day++) {
			for (Double utility : vehicle2utilityOverDays.get(day).values()) {
				maxUtility = Math.max(maxUtility, utility);
			}
		}

		double denom = 0.0;
		for (int day = 0; day < vehicle2utilityOverDays.size(); day++) {
			for (Double utility : vehicle2utilityOverDays.get(day).values()) {
				denom += Math.exp(this.scale * (utility - maxUtility));
			}
		}

		final double threshold = this.rnd.nextDouble() * denom;
		double sum = 0.0;
		for (int day = 0; day < vehicle2utilityOverDays.size(); day++) {
			for (Map.Entry<Vehicle, Double> veh2utl : vehicle2utilityOverDays.get(day).entrySet()) {
				sum += Math.exp(this.scale * (veh2utl.getValue() - maxUtility));
				if (sum >= threshold) {
					return new Slot(veh2utl.getKey(), day);
				}
			}
		}

		// TODO should not happen unless for numerical reasons
		return null;
	}
}
