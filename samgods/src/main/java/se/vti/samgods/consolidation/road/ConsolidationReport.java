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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Tuple;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationReport {

	private final List<SamgodsConstants.Commodity> allCommodities;
	private final List<Id<VehicleType>> allVehicleTypes;

	private final List<DailyRepport> dailyReports;

	public class DailyRepport {

		final List<List<Double>> vehicleCommodityMatrix;

		DailyRepport(ShipmentVehicleAssignment assignment) {
			final Map<Tuple<SamgodsConstants.Commodity, Id<VehicleType>>, Double> commodityAndVehicleTypeId2tons = assignment
					.computeCommodityAndVehicleType2tons();
			this.vehicleCommodityMatrix = new ArrayList<>(allVehicleTypes.size());
			for (Id<VehicleType> vehTypeId : allVehicleTypes) {
				List<Double> comVals = new ArrayList<>(allCommodities.size());
				this.vehicleCommodityMatrix.add(comVals);
				for (SamgodsConstants.Commodity comm : allCommodities) {
					comVals.add(commodityAndVehicleTypeId2tons.getOrDefault(new Tuple<>(comm, vehTypeId), 0.0));
				}
			}
		}

		@Override
		public String toString() {
			StringBuffer result = new StringBuffer();

			for (SamgodsConstants.Commodity c : allCommodities) {
				result.append("\t");
				result.append(c);
			}
			result.append("\n");

			Iterator<Id<VehicleType>> vehTyptIt = allVehicleTypes.iterator();
			for (List<Double> row : this.vehicleCommodityMatrix) {
				result.append(vehTyptIt.next());
				for (Double val : row) {
					result.append("\t");
					result.append(val);
				}
				result.append("\n");
			}

			return result.toString();
		}

	}

	public ConsolidationReport(List<ShipmentVehicleAssignment> assignments) {

		final Set<SamgodsConstants.Commodity> allCommodities = new LinkedHashSet<>();
		final Set<Id<VehicleType>> allVehicleTypes = new LinkedHashSet<>();
		for (ShipmentVehicleAssignment assignment : assignments) {
			allCommodities.addAll(assignment.getShipments().stream().map(s -> s.getType()).collect(Collectors.toSet()));
			allVehicleTypes.addAll(
					assignment.getVehicles().stream().map(v -> v.getType().getId()).collect(Collectors.toSet()));
		}
		this.allCommodities = new ArrayList<>(allCommodities);
		Collections.sort(this.allCommodities);
		this.allVehicleTypes = new ArrayList<>(allVehicleTypes);
		Collections.sort(this.allVehicleTypes, new Comparator<Id<VehicleType>>() {
			@Override
			public int compare(Id<VehicleType> type1, Id<VehicleType> type2) {
				return 0; // TODO
//				return Double.compare(ConsolidationUtils.getCapacity_ton(type1),
//						ConsolidationUtils.getCapacity_ton(type2));
			}
		});

		this.dailyReports = new ArrayList<>(assignments.size());
		for (ShipmentVehicleAssignment assignment : assignments) {
			this.dailyReports.add(new DailyRepport(assignment));
		}
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < this.dailyReports.size(); i++) {
			result.append("DAY " + (i + 1) + "/" + this.dailyReports.size() + "\n");
			result.append(this.dailyReports.get(i));
			result.append("\n");
		}

		return result.toString();
	}

}
