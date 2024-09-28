/**
 * se.vti.samgods.calibration
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.calibration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetCostCalibrator {

	private static final Logger log = Logger.getLogger(FleetCostCalibrator.class);

	final double minCostFactor = 1e-3;

	public static enum Group {
		MGV16, MGV24, HGV40, HGV60, RAIL_COMBI, RAIL_SYSTEM, RAIL_WAGON, SEA
//		, CV5, CV16, CV27, CV100, OV1, OV2, OV3, OV5, OV10, OV20, OV40, OV80, OV100, OV250, RO3, RO6, RO10, OTHER
	};

	private final Vehicles vehicles;
	private final double kP;
	private final double kI;

//	private final Map<Group, Set<VehicleType>> group2vehicleTypes = new LinkedHashMap<>();
	private final Map<VehicleType, Group> vehicleType2group;

	final Map<Group, Double> group2normalizedTarget;

	Map<Group, Double> group2lastNormalizedRealized = null;

	private final Map<Group, Double> group2lastError = new LinkedHashMap<>();
	private final Map<Group, Double> group2errorSum = new LinkedHashMap<>();

	public FleetCostCalibrator(Vehicles vehicles, double kP, double kI) {

		this.vehicles = vehicles;
		this.kP = kP;
		this.kI = kI;

		this.vehicleType2group = new LinkedHashMap<>(vehicles.getVehicleTypes().size());

		this.vehicleType2group.put(this.name2type(Group.MGV16.toString()), Group.MGV16);
		this.vehicleType2group.put(this.name2type(Group.MGV24.toString()), Group.MGV24);
		this.vehicleType2group.put(this.name2type(Group.HGV40.toString()), Group.HGV40);
		this.vehicleType2group.put(this.name2type(Group.HGV60.toString()), Group.HGV60);

		this.vehicleType2group.put(this.name2type("KOMBI"), Group.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("KOMXL"), Group.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("SYS22"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS25"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS30"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYSXL"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("WG550"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG750"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG950"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WGEXL"), Group.RAIL_WAGON);

		this.vehicleType2group.put(this.name2type("CV5"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV16"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV27"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV100"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV1"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV2"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV3"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV5"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV10"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV20"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV40"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV80"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV100"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV250"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO3"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO6"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO10"), Group.SEA);

		final Map<Group, Double> group2target = new LinkedHashMap<>();

		group2target.put(Group.MGV16, 0.3);
		group2target.put(Group.MGV24, 0.4);
		group2target.put(Group.HGV40, 12.2);
		group2target.put(Group.HGV60, 39.1);

		group2target.put(Group.RAIL_COMBI, 5.96);
		group2target.put(Group.RAIL_SYSTEM, 8.61);
		group2target.put(Group.RAIL_WAGON, 8.25);

		group2target.put(Group.SEA, 29.61);

		this.group2normalizedTarget = this.createNormalized(group2target);
	}

	private VehicleType name2type(String name) {
		return this.vehicles.getVehicleTypes().get(Id.create(name, VehicleType.class));
	}

	private Map<Group, Double> createNormalized(Map<Group, Double> group2value) {
		final double minValue = 1e-8;
		final double sum = group2value.values().stream().mapToDouble(v -> Math.max(minValue, v)).sum();
		return group2value.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> Math.max(minValue, e.getValue()) / sum));
	}

	public void updateInternally(FleetStatistics fleetStats) {

		final Map<Group, Double> group2realized = new LinkedHashMap<>();
		for (Map.Entry<VehicleType, Group> entry : this.vehicleType2group.entrySet()) {
			final double realized = fleetStats.getVehicleType2tonKm().getOrDefault(entry.getKey(), 0.0);
			group2realized.compute(entry.getValue(), (g, r) -> r == null ? realized : r + realized);
		}
		this.group2lastNormalizedRealized = this.createNormalized(group2realized);

		this.group2lastError.clear();
		for (Map.Entry<Group, Double> entry : this.group2normalizedTarget.entrySet()) {
			final Group group = entry.getKey();
			final double error = Math.log(this.group2lastNormalizedRealized.get(group)) - Math.log(entry.getValue());
			this.group2lastError.put(group, error);
			this.group2errorSum.compute(group, (g, s) -> s == null ? error : s + error);
		}
	}

	Map<Group, Double> computeGroupASCs() {
		final ConcurrentMap<Group, Double> group2asc = new ConcurrentHashMap<>(Group.values().length);
		for (Group group : Group.values()) {
			final double asc = (-1.0) * (this.kP * this.group2lastError.getOrDefault(group, 0.0)
					+ this.kI * this.group2errorSum.getOrDefault(group, 0.0));
			group2asc.put(group, asc);
		}
		final double ascShift = group2asc.values().stream().mapToDouble(asc -> asc).sum() / group2asc.size();
		group2asc.entrySet().stream().forEach(e -> e.setValue(e.getValue() - ascShift));
		return group2asc;
	}

	public ConcurrentMap<VehicleType, Double> createConcurrentVehicleType2asc() {
		final Map<Group, Double> group2asc = this.computeGroupASCs();
		final ConcurrentMap<VehicleType, Double> vehicleType2asc = new ConcurrentHashMap<>(
				this.vehicles.getVehicleTypes().size());
		for (VehicleType vehicleType : this.vehicles.getVehicleTypes().values()) {
			final Group group = this.vehicleType2group.get(vehicleType);
			if (group == null) {
				vehicleType2asc.put(vehicleType, 0.0);
			} else {
				vehicleType2asc.put(vehicleType, group2asc.getOrDefault(group, 0.0));
			}
		}
		return vehicleType2asc;
	}
}
