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

import java.util.Map;
import java.util.Random;

import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author GunnarF
 *
 */
public class LogitVehicleSampler {
	
//	implements ConsolidationSlotChoiceModel {
//
//	private final double scale;
//
//	private final Random rnd;
//
//	public LogitVehicleSampler(final double scale, final Random rnd) {
//		this.scale = scale;
//		this.rnd = rnd;
//	}
//
//	@Override
//	public Vehicle drawSlot(final Shipment shipment, final Map<Vehicle, Double> vehicle2utility) {
//
//		if (vehicle2utility.size() == 0) {
//			return null;
//		}
//
//		final double maxUtility = vehicle2utility.values().stream().mapToDouble(v -> v).max().getAsDouble();
//		final double denom = vehicle2utility.values().stream().mapToDouble(v -> Math.exp(this.scale * (v - maxUtility)))
//				.sum();
//		final double threshold = this.rnd.nextDouble() * denom;
//		double sum = 0.0;
//		for (Map.Entry<Vehicle, Double> entry : vehicle2utility.entrySet()) {
//			sum += Math.exp(this.scale * (entry.getValue() - maxUtility));
//			if (sum >= threshold) {
//				return entry.getKey();
//			}
//		}
//
//		// should not happen unless for numerical reasons
//		return null;
//	}

}
