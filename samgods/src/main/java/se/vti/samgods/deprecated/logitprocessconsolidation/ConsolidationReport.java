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
package se.vti.samgods.deprecated.logitprocessconsolidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Tuple;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationReport {

	// -------------------- MEMBERS --------------------

	private final List<SamgodsConstants.Commodity> allCommodities;
	private final List<VehicleType> allVehicleTypes;

	private final List<double[][]> vehicleCommodityMatrixOverDays;

	// -------------------- CONSTRUCTION --------------------

	public ConsolidationReport(List<ShipmentVehicleAssignment> assignments) {

//		final Set<SamgodsConstants.Commodity> allCommodities = new LinkedHashSet<>();
//		final Set<VehicleType> allVehicleTypes = new LinkedHashSet<>();
//		for (ShipmentVehicleAssignment assignment : assignments) {
//			allCommodities.addAll(assignment.getShipment2vehicles().keySet().stream().map(s -> s.getCommodity())
//					.collect(Collectors.toSet()));
//			allVehicleTypes.addAll(assignment.getVehicle2shipments().keySet().stream().map(v -> v.getType())
//					.collect(Collectors.toSet()));
//		}
//		this.allCommodities = new ArrayList<>(allCommodities);
//		Collections.sort(this.allCommodities);
//		this.allVehicleTypes = new ArrayList<>(allVehicleTypes);
//		Collections.sort(this.allVehicleTypes, new Comparator<VehicleType>() {
//			@Override
//			public int compare(VehicleType type1, VehicleType type2) {
//				return Double.compare(FreightVehicleAttributes.getCapacity_ton(type1),
//						FreightVehicleAttributes.getCapacity_ton(type2));
//			}
//		});
//
//		this.vehicleCommodityMatrixOverDays = new ArrayList<>(assignments.size());
//		for (ShipmentVehicleAssignment assignment : assignments) {
//			this.vehicleCommodityMatrixOverDays.add(this.createVehicleCommodityMatrix(assignment));
//		}
		throw new UnsupportedOperationException();
	}

	// -------------------- INTERNALS --------------------

	private double[][] createVehicleCommodityMatrix(ShipmentVehicleAssignment assignment) {

		final Map<Tuple<SamgodsConstants.Commodity, VehicleType>, Double> commodityAndVehicleType2tons = assignment
				.getShipmentAndVehicle2tons().entrySet().stream()
				.collect(Collectors.toMap(
						e -> new Tuple<>(e.getKey().getA().getCommodity(), e.getKey().getB().getType()),
						e -> e.getValue(), (oldVal, newVal) -> oldVal + newVal));
		assert (Math.abs(commodityAndVehicleType2tons.values().stream().mapToDouble(t -> t).sum()
				- assignment.getShipmentAndVehicle2tons().values().stream().mapToDouble(t -> t).sum()) <= 1e-8);

		final double[][] result = new double[this.allVehicleTypes.size()][this.allCommodities.size()];
		for (int i = 0; i < this.allVehicleTypes.size(); i++) {
			VehicleType vehicleType = this.allVehicleTypes.get(i);
			double[] row = result[i];
			for (int j = 0; j < this.allCommodities.size(); j++) {
				row[j] += commodityAndVehicleType2tons
						.getOrDefault(new Tuple<>(this.allCommodities.get(j), vehicleType), 0.0);
			}
		}
		return result;
	}

	// -------------------- OVERRIDING OF Object --------------------

	public String vehicleCommodityMatrixToString(double[][] vehicleCommodityMatrix) {
		StringBuffer result = new StringBuffer();

		for (SamgodsConstants.Commodity c : this.allCommodities) {
			result.append("\t");
			result.append(c);
		}
		result.append("\n");

		Iterator<VehicleType> vehTyptIt = allVehicleTypes.iterator();
		for (double[] row : vehicleCommodityMatrix) {
			result.append(vehTyptIt.next().getId());
			for (double val : row) {
				result.append("\t");
				result.append(val);
			}
			result.append("\n");
		}

		return result.toString();
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();

		for (int day = 0; day < this.vehicleCommodityMatrixOverDays.size(); day++) {
			result.append("DAY " + (day + 1) + "/" + this.vehicleCommodityMatrixOverDays.size() + "\n");
			result.append(this.vehicleCommodityMatrixToString(this.vehicleCommodityMatrixOverDays.get(day)));
			result.append("\n");
		}

		result.append("\n");
		result.append("TOTALS OVER DAYS\n");

		result.append("day");
		for (VehicleType type : this.allVehicleTypes) {
			result.append("\t");
			result.append(type.getId());
		}
		for (SamgodsConstants.Commodity commodity : this.allCommodities) {
			result.append("\t");
			result.append(commodity);
		}
		result.append("\n");

		for (int day = 0; day < this.vehicleCommodityMatrixOverDays.size(); day++) {

			final double[][] vehicleCommodityMatrix = this.vehicleCommodityMatrixOverDays.get(day);
			final double[] vehSums = new double[this.allVehicleTypes.size()];
			final double[] comSums = new double[this.allCommodities.size()];
			for (int i = 0; i < this.allVehicleTypes.size(); i++) {
				for (int j = 0; j < this.allCommodities.size(); j++) {
					vehSums[i] += vehicleCommodityMatrix[i][j];
					comSums[j] += vehicleCommodityMatrix[i][j];
				}
			}

			result.append(day + 1);
			for (int i = 0; i < this.allVehicleTypes.size(); i++) {
				result.append("\t");
				result.append(vehSums[i]);
			}
			for (int j = 0; j < this.allCommodities.size(); j++) {
				result.append("\t");
				result.append(comSums[j]);
			}
			result.append("\n");
		}

		return result.toString();
	}

}
