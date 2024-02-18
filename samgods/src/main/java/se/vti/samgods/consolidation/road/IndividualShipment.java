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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.SamgodsConstants.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class IndividualShipment {

	private final double weight_ton;

	private final Commodity commodity;

	private final double probability;

	private final Map<Vehicle, Double> assignedVehicle2tons = new LinkedHashMap<>();

	public IndividualShipment(final double weight_ton, final Commodity commodity, double probability) {
		this.weight_ton = weight_ton;
		this.commodity = commodity;
		this.probability = probability;
	}

	public double allocatedShare_ton() {
		return this.assignedVehicle2tons.values().stream().mapToDouble(t -> t).sum();
	}

	public double getWeight_ton() {
		return this.weight_ton;
	}

	public double getProbability() {
		return this.probability;
	}

	public Commodity getType() {
		return this.commodity;
	}

	public Map<Vehicle, Double> getAssignedVehicle2tons() {
		return this.assignedVehicle2tons;
	}

//	public Map<VehicleType, Integer> getAssignedVehicleTypes() {
//		final Map<VehicleType, Integer> type2cnt = new LinkedHashMap<>();
//		this.assignedVehicle2tons.keySet().stream()
//				.forEach(v -> type2cnt.compute(v.getType(), (t, c) -> c == null ? 1 : c + 1));
//		return type2cnt;
//	}

	public void setAssignedVehicles(final Map<Vehicle, Double> vehicle2tons) {
		this.assignedVehicle2tons.clear();
		this.assignedVehicle2tons.putAll(vehicle2tons);
	}

	public void clearAssignedVehicle2tons() {
		this.assignedVehicle2tons.clear();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(weight=" + this.weight_ton + "ton,type=" + this.commodity + ")";
	}

}
