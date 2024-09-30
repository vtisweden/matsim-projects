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

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetCostCalibrator {

	// -------------------- CONSTANTS --------------------

	public static enum Group {
		MGV16_X, MGV24_X, HGV40_X, HGV60_X, RAIL_COMBI, RAIL_SYSTEM, RAIL_WAGON, SEA
//		, CV5, CV16, CV27, CV100, OV1, OV2, OV3, OV5, OV10, OV20, OV40, OV80, OV100, OV250, RO3, RO6, RO10, OTHER
	};

	private final Vehicles vehicles;
	private final Map<VehicleType, Group> vehicleType2group;
	private final Map<Group, TransportMode> group2mode;

//	final Map<Group, Double> group2normalizedTarget;
//	final Map<TransportMode, Double> mode2normalizedTarget;

	private final ASCTuner<TransportMode> modeAscTuner;
	private final Map<TransportMode, ASCTuner<Group>> mode2groupAscTuner;

	// -------------------- MEMBERS --------------------

//	Map<Group, Double> group2lastNormalizedRealized = null;
//	Map<TransportMode, Double> mode2lastNormalizedRealized = null;

	final Map<Group, Double> group2targetDomesticGTonKm;
	Map<Group, Double> group2lastRealizedDomesticGTonKm = null;

	final Map<TransportMode, Double> mode2targetDomesticGTonKm;
	Map<TransportMode, Double> mode2lastRealizedDomesticGTonKm = null;

	// -------------------- CONSTRUCTION --------------------

	public FleetCostCalibrator(Vehicles vehicles, double eta) {

		this.vehicles = vehicles;

		this.vehicleType2group = new LinkedHashMap<>(vehicles.getVehicleTypes().size());
		this.group2mode = new LinkedHashMap<>();

		this.vehicleType2group.put(this.name2type("MGV16"), Group.MGV16_X);
		this.vehicleType2group.put(this.name2type("MGV16_CONTAINER"), Group.MGV16_X);
		this.vehicleType2group.put(this.name2type("MGV24"), Group.MGV24_X);
		this.vehicleType2group.put(this.name2type("MGV24_CONTAINER"), Group.MGV24_X);
		this.vehicleType2group.put(this.name2type("HGV40"), Group.HGV40_X);
		this.vehicleType2group.put(this.name2type("HGV40_CONTAINER"), Group.HGV40_X);
		this.vehicleType2group.put(this.name2type("HGV60"), Group.HGV60_X);
		this.vehicleType2group.put(this.name2type("HGV60_CONTAINER"), Group.HGV60_X);
		this.group2mode.put(Group.MGV16_X, TransportMode.Road);
		this.group2mode.put(Group.MGV24_X, TransportMode.Road);
		this.group2mode.put(Group.HGV40_X, TransportMode.Road);
		this.group2mode.put(Group.HGV60_X, TransportMode.Road);

		this.vehicleType2group.put(this.name2type("KOMBI"), Group.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("KOMBI_CONTAINER"), Group.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("KOMXL"), Group.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("KOMXL_CONTAINER"), Group.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("SYS22"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS22_CONTAINER"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS25"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS25_CONTAINER"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS30"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS30_CONTAINER"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYSXL"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYSXL_CONTAINER"), Group.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("WG550"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG550_CONTAINER"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG750"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG750_CONTAINER"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG950"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG950_CONTAINER"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WGEXL"), Group.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WGEXL_CONTAINER"), Group.RAIL_WAGON);
		this.group2mode.put(Group.RAIL_COMBI, TransportMode.Rail);
		this.group2mode.put(Group.RAIL_SYSTEM, TransportMode.Rail);
		this.group2mode.put(Group.RAIL_WAGON, TransportMode.Rail);

		this.vehicleType2group.put(this.name2type("CV5"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV5_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV16"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV16_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV27"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV27_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV100"), Group.SEA);
		this.vehicleType2group.put(this.name2type("CV100_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV1"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV1_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV2"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV2_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV3"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV3_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV5"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV5_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV10"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV10_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV20"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV20_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV40"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV40_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV80"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV80_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV100"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV100_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV250"), Group.SEA);
		this.vehicleType2group.put(this.name2type("OV250_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO3"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO3_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO6"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO6_CONTAINER"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO10"), Group.SEA);
		this.vehicleType2group.put(this.name2type("RO10_CONTAINER"), Group.SEA);
		this.group2mode.put(Group.SEA, TransportMode.Sea);

		this.group2targetDomesticGTonKm = new LinkedHashMap<>();
		this.group2targetDomesticGTonKm.put(Group.MGV16_X, 0.3);
		this.group2targetDomesticGTonKm.put(Group.MGV24_X, 0.4);
		this.group2targetDomesticGTonKm.put(Group.HGV40_X, 12.2);
		this.group2targetDomesticGTonKm.put(Group.HGV60_X, 39.1);
		this.group2targetDomesticGTonKm.put(Group.RAIL_COMBI, 5.96);
		this.group2targetDomesticGTonKm.put(Group.RAIL_SYSTEM, 8.61);
		this.group2targetDomesticGTonKm.put(Group.RAIL_WAGON, 8.25);
		this.group2targetDomesticGTonKm.put(Group.SEA, 29.61);

		this.mode2targetDomesticGTonKm = new LinkedHashMap<>();
		for (Map.Entry<Group, Double> e : this.group2targetDomesticGTonKm.entrySet()) {
			final Group group = e.getKey();
			final double groupTarget = e.getValue();
			this.mode2targetDomesticGTonKm.compute(this.group2mode.get(group),
					(m, t) -> t == null ? groupTarget : t + groupTarget);
		}

//		this.group2normalizedTarget = this.createNormalized(this.group2targetDomesticGTonKm);
//		this.mode2normalizedTarget = this.createNormalized(this.mode2targetDomesticGTonKm);

		this.modeAscTuner = new ASCTuner<>(this.mode2targetDomesticGTonKm.values().stream().mapToDouble(t -> t).sum(),
				eta);
		for (Map.Entry<TransportMode, Double> e : this.mode2targetDomesticGTonKm.entrySet()) {
			final TransportMode mode = e.getKey();
			final Double target_GTonKm = e.getValue();
			this.modeAscTuner.setTarget(mode, target_GTonKm);
		}

		this.mode2groupAscTuner = new LinkedHashMap<>();
		for (Map.Entry<Group, Double> e : this.group2targetDomesticGTonKm.entrySet()) {
			final Group group = e.getKey();
			final Double target_GTonKm = e.getValue();
			final TransportMode mode = this.group2mode.get(group);
			this.mode2groupAscTuner
					.computeIfAbsent(mode, m -> new ASCTuner<>(this.mode2targetDomesticGTonKm.get(m), eta))
					.setTarget(group, target_GTonKm);
		}
	}

	// -------------------- INTERNALS --------------------

	private VehicleType name2type(String name) {
		final VehicleType type = this.vehicles.getVehicleTypes().get(Id.create(name, VehicleType.class));
		assert (type != null);
		return type;
	}

//	private <K> Map<K, Double> createNormalized(Map<K, Double> group2value) {
//		final double minValue = 1e-8;
//		final double sum = group2value.values().stream().mapToDouble(v -> Math.max(minValue, v)).sum();
//		return group2value.entrySet().stream()
//				.collect(Collectors.toMap(e -> e.getKey(), e -> Math.max(minValue, e.getValue()) / sum));
//	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateInternally(FleetStatistics fleetStats) {
		this.group2lastRealizedDomesticGTonKm = new LinkedHashMap<>();
		this.mode2lastRealizedDomesticGTonKm = new LinkedHashMap<>();
		for (Map.Entry<VehicleType, Group> entry : this.vehicleType2group.entrySet()) {
			final VehicleType vehicleType = entry.getKey();
			final Group group = entry.getValue();
			final TransportMode mode = this.group2mode.get(group);
			final double realized_GTonKm = 1e-9
					* fleetStats.getVehicleType2domesticTonKm().getOrDefault(vehicleType, 0.0);
			this.group2lastRealizedDomesticGTonKm.compute(group,
					(g, r) -> r == null ? realized_GTonKm : r + realized_GTonKm);
			this.mode2lastRealizedDomesticGTonKm.compute(mode,
					(m, r) -> r == null ? realized_GTonKm : r + realized_GTonKm);
		}
//		this.group2lastNormalizedRealized = this.createNormalized(this.group2lastRealizedDomesticGTonKm);
//		this.mode2lastNormalizedRealized = this.createNormalized(this.mode2lastRealizedDomesticGTonKm);

		this.modeAscTuner.update(m -> this.mode2lastRealizedDomesticGTonKm.getOrDefault(m, 0.0));
		for (Map.Entry<Group, Double> e : this.group2lastRealizedDomesticGTonKm.entrySet()) {
			final Group group = e.getKey();
			final TransportMode mode = this.group2mode.get(group);
			this.mode2groupAscTuner.get(mode).update(g -> this.group2lastRealizedDomesticGTonKm.get(g));
		}
	}

	public ConcurrentMap<Group, Double> createConcurrentGroup2asc() {
		final ConcurrentMap<Group, Double> group2asc = new ConcurrentHashMap<>();
		for (ASCTuner<Group> ascTuner : this.mode2groupAscTuner.values()) {
			group2asc.putAll(ascTuner.getAlternative2asc());
		}

		// TODO!!!
		 group2asc.entrySet().stream().forEach(e -> e.setValue(0.0));

		return group2asc;
	}

	public ConcurrentMap<TransportMode, Double> createConcurrentMode2asc() {
		final ConcurrentMap<TransportMode, Double> mode2asc = new ConcurrentHashMap<>();
		for (TransportMode mode : TransportMode.values()) {
			mode2asc.put(mode, this.modeAscTuner.getAlternative2asc().getOrDefault(mode, 0.0));
		}

		// TODO!!!
//		 mode2asc.entrySet().stream().forEach(e -> e.setValue(0.0));

		return mode2asc;
	}

	public ConcurrentMap<VehicleType, Double> createConcurrentVehicleType2asc() {
		final ConcurrentMap<VehicleType, Double> vehicleType2asc = new ConcurrentHashMap<>(
				this.vehicles.getVehicleTypes().size());
		for (VehicleType vehicleType : this.vehicles.getVehicleTypes().values()) {
			final Group group = this.vehicleType2group.get(vehicleType);
			if (group == null) {
				vehicleType2asc.put(vehicleType, 0.0);
			} else {
				vehicleType2asc.put(vehicleType, this.mode2groupAscTuner.get(this.group2mode.get(group))
						.getAlternative2asc().getOrDefault(group, 0.0));
			}
		}

		// TODO!!!
		vehicleType2asc.entrySet().stream().forEach(e -> e.setValue(0.0));

		return vehicleType2asc;
	}
}
