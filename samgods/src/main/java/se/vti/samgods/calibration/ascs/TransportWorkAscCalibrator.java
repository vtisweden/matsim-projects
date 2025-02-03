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
package se.vti.samgods.calibration.ascs;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.utils.MiscUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportWorkAscCalibrator {

	// -------------------- CONSTANTS --------------------

	public static enum VehicleGroup {
		MGV16_X, MGV24_X, HGV40_X, HGV60_X, RAIL_COMBI, RAIL_SYSTEM, RAIL_WAGON, SEA
	};

	private final Vehicles vehicles;
	private final Map<VehicleType, VehicleGroup> vehicleType2group;
//	private final Map<VehicleGroup, TransportMode> group2mode;

//	private final ASCTuner<TransportMode> modeAscTuner;
//	private final Map<TransportMode, ASCTuner<VehicleGroup>> mode2groupAscTuner;
	private final ASCTuner<VehicleGroup> vehicleGroupAscTuner;
	private final ASCTuner<Commodity> commodityRailAscTuner;

	private final ASCLogger ascLogger;

	// -------------------- MEMBERS --------------------

//	final Map<VehicleGroup, Double> group2targetDomesticGTonKm;
//	final Map<TransportMode, Double> mode2targetDomesticGTonKm;
//	final Map<Commodity, Double> commodity2railTargetDomesticGTonKm;

	// -------------------- CONSTRUCTION --------------------

	public TransportWorkAscCalibrator(Vehicles vehicles, double eta) {

		this.ascLogger = new ASCLogger(vehicles);

		this.vehicles = vehicles;
		this.vehicleType2group = new LinkedHashMap<>(vehicles.getVehicleTypes().size());
//		this.group2mode = new LinkedHashMap<>();
//		this.group2targetDomesticGTonKm = new LinkedHashMap<>();
		this.vehicleGroupAscTuner = new ASCTuner<>(null, eta);

		/*
		 * ROAD
		 */

		this.vehicleType2group.put(this.name2type("MGV16"), VehicleGroup.MGV16_X);
		this.vehicleType2group.put(this.name2type("MGV16_CONTAINER"), VehicleGroup.MGV16_X);
		this.vehicleType2group.put(this.name2type("MGV24"), VehicleGroup.MGV24_X);
		this.vehicleType2group.put(this.name2type("MGV24_CONTAINER"), VehicleGroup.MGV24_X);
		this.vehicleType2group.put(this.name2type("HGV40"), VehicleGroup.HGV40_X);
		this.vehicleType2group.put(this.name2type("HGV40_CONTAINER"), VehicleGroup.HGV40_X);
		this.vehicleType2group.put(this.name2type("HGV60"), VehicleGroup.HGV60_X);
		this.vehicleType2group.put(this.name2type("HGV60_CONTAINER"), VehicleGroup.HGV60_X);

//		this.group2targetDomesticGTonKm.put(VehicleGroup.MGV16_X, 0.3);
//		this.group2targetDomesticGTonKm.put(VehicleGroup.MGV24_X, 0.4);
//		this.group2targetDomesticGTonKm.put(VehicleGroup.HGV40_X, 12.2);
//		this.group2targetDomesticGTonKm.put(VehicleGroup.HGV60_X, 39.1);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.MGV16_X, 0.3);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.MGV24_X, 0.4);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.HGV40_X, 12.2);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.HGV60_X, 39.1);

//		this.group2mode.put(VehicleGroup.MGV16_X, TransportMode.Road);
//		this.group2mode.put(VehicleGroup.MGV24_X, TransportMode.Road);
//		this.group2mode.put(VehicleGroup.HGV40_X, TransportMode.Road);
//		this.group2mode.put(VehicleGroup.HGV60_X, TransportMode.Road);

		/*
		 * RAIL
		 */

		this.vehicleType2group.put(this.name2type("KOMBI"), VehicleGroup.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("KOMBI_CONTAINER"), VehicleGroup.RAIL_COMBI);
		this.vehicleType2group.put(this.name2type("SYS22"), VehicleGroup.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS22_CONTAINER"), VehicleGroup.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS25"), VehicleGroup.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS25_CONTAINER"), VehicleGroup.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS30"), VehicleGroup.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("SYS30_CONTAINER"), VehicleGroup.RAIL_SYSTEM);
		this.vehicleType2group.put(this.name2type("WG550"), VehicleGroup.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG550_CONTAINER"), VehicleGroup.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG750"), VehicleGroup.RAIL_WAGON);
		this.vehicleType2group.put(this.name2type("WG750_CONTAINER"), VehicleGroup.RAIL_WAGON);

//		this.group2targetDomesticGTonKm.put(VehicleGroup.RAIL_COMBI, 5.96);
//		this.group2targetDomesticGTonKm.put(VehicleGroup.RAIL_SYSTEM, 8.61);
//		this.group2targetDomesticGTonKm.put(VehicleGroup.RAIL_WAGON, 8.25);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.RAIL_COMBI, 5.96);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.RAIL_SYSTEM, 8.61);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.RAIL_WAGON, 8.25);

//		this.group2mode.put(VehicleGroup.RAIL_COMBI, TransportMode.Rail);
//		this.group2mode.put(VehicleGroup.RAIL_SYSTEM, TransportMode.Rail);
//		this.group2mode.put(VehicleGroup.RAIL_WAGON, TransportMode.Rail);

		/*
		 * SEA
		 */

		this.vehicleType2group.put(this.name2type("CV5"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("CV5_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("CV16"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("CV16_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("CV27"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("CV27_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("CV100"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("CV100_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV1"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV1_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV2"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV2_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV3"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV3_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV5"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV5_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV10"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV10_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV20"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV20_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV40"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV40_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV80"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV80_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV100"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV100_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV250"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("OV250_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("RO3"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("RO3_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("RO6"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("RO6_CONTAINER"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("RO10"), VehicleGroup.SEA);
		this.vehicleType2group.put(this.name2type("RO10_CONTAINER"), VehicleGroup.SEA);

//		this.group2targetDomesticGTonKm.put(VehicleGroup.SEA, 29.61);
		this.vehicleGroupAscTuner.setTarget(VehicleGroup.SEA, 29.61);

//		this.group2mode.put(VehicleGroup.SEA, TransportMode.Sea);

		/*
		 * TARGETS AND ASC TUNERS.
		 */

//		this.mode2targetDomesticGTonKm = new LinkedHashMap<>();
//		for (Map.Entry<VehicleGroup, Double> e : this.group2targetDomesticGTonKm.entrySet()) {
//			final VehicleGroup group = e.getKey();
//			final double groupTarget = e.getValue();
//			this.mode2targetDomesticGTonKm.compute(this.group2mode.get(group),
//					(m, t) -> t == null ? groupTarget : t + groupTarget);
//		}
//
//		this.modeAscTuner = new ASCTuner<>(null, eta);
//		for (Map.Entry<TransportMode, Double> e : this.mode2targetDomesticGTonKm.entrySet()) {
//			final TransportMode mode = e.getKey();
//			final Double target_GTonKm = e.getValue();
//			this.modeAscTuner.setTarget(mode, target_GTonKm);
//		}
//
//		this.mode2groupAscTuner = new LinkedHashMap<>();
//		for (Map.Entry<VehicleGroup, Double> e : this.group2targetDomesticGTonKm.entrySet()) {
//			final VehicleGroup group = e.getKey();
//			final Double target_GTonKm = e.getValue();
//			final TransportMode mode = this.group2mode.get(group);
//			this.mode2groupAscTuner.computeIfAbsent(mode, m -> new ASCTuner<>(null, eta)).setTarget(group,
//					target_GTonKm);
//		}

		/*
		 * RAIL TARGETS BY COMMODITY
		 */

		this.commodityRailAscTuner = new ASCTuner<>(null, eta);

//		this.commodity2railTargetDomesticGTonKm = new LinkedHashMap<>();
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.AGRICULTURE, 0.356);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.COAL, 0.071);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.METAL, 5.195);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.FOOD, 2.307);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.TEXTILES, 0.001 /* null */);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.WOOD, 4.345);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.COKE, 0.345);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.CHEMICALS, 1.001);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.OTHERMINERAL, 0.297);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.BASICMETALS, 3.156);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.MACHINERY, 0.037);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.TRANSPORT, 2.028);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.FURNITURE, 0.001 /* null */);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.SECONDARYRAW, 1.118);
//		this.commodity2railTargetDomesticGTonKm.put(Commodity.TIMBER, 2.321);
		
		this.commodityRailAscTuner.setTarget(Commodity.AGRICULTURE, 0.356);
		this.commodityRailAscTuner.setTarget(Commodity.COAL, 0.071);
		this.commodityRailAscTuner.setTarget(Commodity.METAL, 5.195);
		this.commodityRailAscTuner.setTarget(Commodity.FOOD, 2.307);
		// this.commodityRailAscTuner.setTarget(Commodity.TEXTILES, 0.001 /* null */);
		this.commodityRailAscTuner.setTarget(Commodity.WOOD, 4.345);
		this.commodityRailAscTuner.setTarget(Commodity.COKE, 0.345);
		this.commodityRailAscTuner.setTarget(Commodity.CHEMICALS, 1.001);
		this.commodityRailAscTuner.setTarget(Commodity.OTHERMINERAL, 0.297);
		this.commodityRailAscTuner.setTarget(Commodity.BASICMETALS, 3.156);
		this.commodityRailAscTuner.setTarget(Commodity.MACHINERY, 0.037);
		this.commodityRailAscTuner.setTarget(Commodity.TRANSPORT, 2.028);
		// this.commodityRailAscTuner.setTarget(Commodity.FURNITURE, 0.001 /* null */);
		this.commodityRailAscTuner.setTarget(Commodity.SECONDARYRAW, 1.118);
		this.commodityRailAscTuner.setTarget(Commodity.TIMBER, 2.321);

//		for (Map.Entry<Commodity, Double> e : this.commodity2railTargetDomesticGTonKm.entrySet()) {
//			final Commodity commodity = e.getKey();
//			final Double target_GTonKm = e.getValue();
//			this.commodityRailAscTuner.setTarget(commodity, target_GTonKm);
//		}
	}

	// -------------------- INTERNALS --------------------

	private VehicleType name2type(String name) {
		final VehicleType type = this.vehicles.getVehicleTypes().get(Id.create(name, VehicleType.class));
		assert (type != null);
		return type;
	}

//	private Map<TransportMode, Double> createMode2asc() {
//		final Map<TransportMode, Double> mode2asc = new LinkedHashMap<>();
//		for (TransportMode mode : TransportMode.values()) {
//			mode2asc.put(mode, this.modeAscTuner.getAlternative2asc().getOrDefault(mode, 0.0));
//		}
//		return mode2asc;
//	}

	private Map<VehicleType, Double> createVehicleType2asc() {
		final Map<VehicleGroup, Double> vehicleGroup2asc = this.vehicleGroupAscTuner.getAlternative2asc();
		final Map<VehicleType, Double> vehicleType2asc = new LinkedHashMap<>(this.vehicles.getVehicleTypes().size());
		for (VehicleType vehicleType : this.vehicles.getVehicleTypes().values()) {
			final VehicleGroup group = this.vehicleType2group.get(vehicleType);
			if (group == null) {
				vehicleType2asc.put(vehicleType, 0.0);
			} else {
				vehicleType2asc.put(vehicleType, vehicleGroup2asc.getOrDefault(group, 0.0));
			}
		}
		return vehicleType2asc;
	}

	private Map<Commodity, Double> createRailCommodity2asc() {
		final Map<Commodity, Double> commodity2asc = this.commodityRailAscTuner.getAlternative2asc();
		final Map<Commodity, Double> railCommodity2asc = new LinkedHashMap<>();
		for (Commodity commodity : Commodity.values()) {
			railCommodity2asc.put(commodity, commodity2asc.getOrDefault(commodity, 0.0));
		}
		return railCommodity2asc;
	}

	// -------------------- IMPLEMENTATION --------------------

	boolean updated = false;

	public void update(
			Map<VehicleType, Double> vehicleType2lastRealizedDomesticGTonKm,
			Map<TransportMode, Double> mode2lastRealizedDomesticGTonKm,
			Map<Commodity, Double> commodity2lastRealizedRailDomesticGTonKm, int iteration) {

		if (!this.updated) {
			MiscUtils.ensureEmptyFolder("./results/calibratedASCs");
			this.updated = true;
		}

		final Map<VehicleGroup, Double> group2lastRealizedDomesticGTonKm = new LinkedHashMap<>();
		for (VehicleType vehicleType : this.vehicles.getVehicleTypes().values()) {
			final double realized_GTonKm = vehicleType2lastRealizedDomesticGTonKm.getOrDefault(vehicleType, 0.0);
			final VehicleGroup group = this.vehicleType2group.get(vehicleType);
			if (group != null) {
				group2lastRealizedDomesticGTonKm.compute(group,
						(g, r) -> r == null ? realized_GTonKm : r + realized_GTonKm);
			}
		}
		this.vehicleGroupAscTuner.update(g -> group2lastRealizedDomesticGTonKm.getOrDefault(g, 0.0));
		
//		this.modeAscTuner.update(m -> mode2lastRealizedDomesticGTonKm.getOrDefault(m, 0.0));
//		for (ASCTuner<VehicleGroup> groupAscTuner : this.mode2groupAscTuner.values()) {
//			groupAscTuner.update(g -> group2lastRealizedDomesticGTonKm.get(g));
//		}
		
		this.commodityRailAscTuner.update(c -> commodity2lastRealizedRailDomesticGTonKm.getOrDefault(c, 0.0));

		try {
			this.createASCDataProvider().writeToFile("./results/calibratedASCs/" + iteration + ".ascs.json");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.ascLogger.log(this);
	}

	public ASCDataProvider createASCDataProvider() {
//		return new ASCDataProvider(this.createVehicleType2asc(), this.createMode2asc(), this.createRailCommodity2asc());
		return new ASCDataProvider(this.createVehicleType2asc(), this.createRailCommodity2asc());
	}
}
