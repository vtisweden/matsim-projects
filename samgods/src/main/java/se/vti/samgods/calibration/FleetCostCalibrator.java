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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.calibration.ascs.ASCs;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;
import se.vti.samgods.utils.MiscUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetCostCalibrator {

	// -------------------- CONSTANTS --------------------

	public static enum VehicleGroup {
		MGV16_X, MGV24_X, HGV40_X, HGV60_X, RAIL_COMBI, RAIL_SYSTEM, RAIL_WAGON, SEA
	};

	private final Vehicles vehicles;
	private final Map<VehicleType, VehicleGroup> vehicleType2group;
	private final Map<VehicleGroup, TransportMode> group2mode;

	private final ASCTuner<TransportMode> modeAscTuner;
	private final Map<TransportMode, ASCTuner<VehicleGroup>> mode2groupAscTuner;
	private final ASCTuner<Commodity> commodityRailAscTuner;

	private final FleetCalibrationLogger logger;

	// -------------------- MEMBERS --------------------

	final Map<VehicleGroup, Double> group2targetDomesticGTonKm;
	Map<VehicleGroup, Double> group2lastRealizedDomesticGTonKm = null;

	final Map<TransportMode, Double> mode2targetDomesticGTonKm;
	Map<TransportMode, Double> mode2lastRealizedDomesticGTonKm = null;

	final Map<Commodity, Double> commodity2railTargetDomesticGTonKm;
	Map<Commodity, Double> commodity2lastRealizedRailDomesticGTonKm = null;

	// -------------------- CONSTRUCTION --------------------

	public FleetCostCalibrator(Vehicles vehicles, double eta) {

		this.logger = new FleetCalibrationLogger(vehicles);

		this.vehicles = vehicles;
		this.vehicleType2group = new LinkedHashMap<>(vehicles.getVehicleTypes().size());
		this.group2mode = new LinkedHashMap<>();
		this.group2targetDomesticGTonKm = new LinkedHashMap<>();

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

		this.group2mode.put(VehicleGroup.MGV16_X, TransportMode.Road);
		this.group2mode.put(VehicleGroup.MGV24_X, TransportMode.Road);
		this.group2mode.put(VehicleGroup.HGV40_X, TransportMode.Road);
		this.group2mode.put(VehicleGroup.HGV60_X, TransportMode.Road);

		this.group2targetDomesticGTonKm.put(VehicleGroup.MGV16_X, 0.3);
		this.group2targetDomesticGTonKm.put(VehicleGroup.MGV24_X, 0.4);
		this.group2targetDomesticGTonKm.put(VehicleGroup.HGV40_X, 12.2);
		this.group2targetDomesticGTonKm.put(VehicleGroup.HGV60_X, 39.1);

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

		this.group2mode.put(VehicleGroup.RAIL_COMBI, TransportMode.Rail);
		this.group2mode.put(VehicleGroup.RAIL_SYSTEM, TransportMode.Rail);
		this.group2mode.put(VehicleGroup.RAIL_WAGON, TransportMode.Rail);

		this.group2targetDomesticGTonKm.put(VehicleGroup.RAIL_COMBI, 5.96);
		this.group2targetDomesticGTonKm.put(VehicleGroup.RAIL_SYSTEM, 8.61);
		this.group2targetDomesticGTonKm.put(VehicleGroup.RAIL_WAGON, 8.25);

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

		this.group2mode.put(VehicleGroup.SEA, TransportMode.Sea);

		this.group2targetDomesticGTonKm.put(VehicleGroup.SEA, 29.61);

		/*
		 * TARGETS AND ASC TUNERS.
		 */

		this.mode2targetDomesticGTonKm = new LinkedHashMap<>();
		for (Map.Entry<VehicleGroup, Double> e : this.group2targetDomesticGTonKm.entrySet()) {
			final VehicleGroup group = e.getKey();
			final double groupTarget = e.getValue();
			this.mode2targetDomesticGTonKm.compute(this.group2mode.get(group),
					(m, t) -> t == null ? groupTarget : t + groupTarget);
		}

		this.modeAscTuner = new ASCTuner<>(null, eta);
		for (Map.Entry<TransportMode, Double> e : this.mode2targetDomesticGTonKm.entrySet()) {
			final TransportMode mode = e.getKey();
			final Double target_GTonKm = e.getValue();
			this.modeAscTuner.setTarget(mode, target_GTonKm);
		}

		this.mode2groupAscTuner = new LinkedHashMap<>();
		for (Map.Entry<VehicleGroup, Double> e : this.group2targetDomesticGTonKm.entrySet()) {
			final VehicleGroup group = e.getKey();
			final Double target_GTonKm = e.getValue();
			final TransportMode mode = this.group2mode.get(group);
			this.mode2groupAscTuner.computeIfAbsent(mode, m -> new ASCTuner<>(null, eta)).setTarget(group,
					target_GTonKm);
		}

		/*
		 * RAIL TARGETS BY COMMODITY
		 */

		this.commodity2railTargetDomesticGTonKm = new LinkedHashMap<>();
		this.commodity2railTargetDomesticGTonKm.put(Commodity.AGRICULTURE, 0.356);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.COAL, 0.071);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.METAL, 5.195);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.FOOD, 2.307);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.TEXTILES, 0.001 /* null */);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.WOOD, 4.345);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.COKE, 0.345);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.CHEMICALS, 1.001);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.OTHERMINERAL, 0.297);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.BASICMETALS, 3.156);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.MACHINERY, 0.037);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.TRANSPORT, 2.028);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.FURNITURE, 0.001 /* null */);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.SECONDARYRAW, 1.118);
		this.commodity2railTargetDomesticGTonKm.put(Commodity.TIMBER, 2.321);

		this.commodityRailAscTuner = new ASCTuner<>(null, eta);
		for (Map.Entry<Commodity, Double> e : this.commodity2railTargetDomesticGTonKm.entrySet()) {
			final Commodity commodity = e.getKey();
			final Double target_GTonKm = e.getValue();
			this.commodityRailAscTuner.setTarget(commodity, target_GTonKm);
		}
	}

	// -------------------- INTERNALS --------------------

	private VehicleType name2type(String name) {
		final VehicleType type = this.vehicles.getVehicleTypes().get(Id.create(name, VehicleType.class));
		assert (type != null);
		return type;
	}

	private Map<TransportMode, Double> createMode2asc() {
		final Map<TransportMode, Double> mode2asc = new LinkedHashMap<>();
		for (TransportMode mode : TransportMode.values()) {
			mode2asc.put(mode, this.modeAscTuner.getAlternative2asc().getOrDefault(mode, 0.0));
		}
		return mode2asc;
	}

	private Map<VehicleType, Double> createVehicleType2asc() {
		final Map<VehicleType, Double> vehicleType2asc = new LinkedHashMap<>(this.vehicles.getVehicleTypes().size());
		for (VehicleType vehicleType : this.vehicles.getVehicleTypes().values()) {
			final VehicleGroup group = this.vehicleType2group.get(vehicleType);
			if (group == null) {
				vehicleType2asc.put(vehicleType, 0.0);
			} else {
				vehicleType2asc.put(vehicleType, this.mode2groupAscTuner.get(this.group2mode.get(group))
						.getAlternative2asc().getOrDefault(group, 0.0));
			}
		}
		return vehicleType2asc;
	}

	private Map<Commodity, Double> createRailCommodity2asc() {
		final Map<Commodity, Double> railCommodity2asc = new LinkedHashMap<>();
		for (Commodity commodity : Commodity.values()) {
			railCommodity2asc.put(commodity,
					this.commodityRailAscTuner.getAlternative2asc().getOrDefault(commodity, 0.0));
		}
		return railCommodity2asc;
	}

	// -------------------- IMPLEMENTATION --------------------

	boolean updated = false;

	public void updateInternally(
			Map<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> consolidationUnit2fleetAssignment,
			int iteration) {
		if (!this.updated) {
			MiscUtils.ensureEmptyFolder("./results/calibratedASCs");
			this.updated = true;
		}

		final Map<VehicleType, Double> vehicleType2domesticGTonKm = new LinkedHashMap<>();
		this.commodity2lastRealizedRailDomesticGTonKm = new LinkedHashMap<>();
		for (Map.Entry<ConsolidationUnit, HalfLoopConsolidationJobProcessor.FleetAssignment> entry : consolidationUnit2fleetAssignment
				.entrySet()) {
			final ConsolidationUnit consolidationUnit = entry.getKey();
			final FleetAssignment fleetAssignment = entry.getValue();
			final double transportWork_gTonKm = 1e-9 * fleetAssignment.realDemand_ton * 0.5
					* fleetAssignment.domesticLoopLength_km;
			vehicleType2domesticGTonKm.compute(fleetAssignment.vehicleType,
					(vt, tk) -> tk == null ? transportWork_gTonKm : tk + transportWork_gTonKm);
			if (TransportMode.Rail.equals(consolidationUnit.samgodsMode)) {
				this.commodity2lastRealizedRailDomesticGTonKm.compute(consolidationUnit.commodity,
						(c, s) -> s == null ? transportWork_gTonKm : s + transportWork_gTonKm);
			}
		}

		this.group2lastRealizedDomesticGTonKm = new LinkedHashMap<>();
		this.mode2lastRealizedDomesticGTonKm = new LinkedHashMap<>();
		for (VehicleType vehicleType : this.vehicles.getVehicleTypes().values()) {
			final double realized_GTonKm = vehicleType2domesticGTonKm.getOrDefault(vehicleType, 0.0);
			final SamgodsVehicleAttributes attrs = (SamgodsVehicleAttributes) vehicleType.getAttributes()
					.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
			if (this.modeAscTuner.getAlternative2asc().containsKey(attrs.samgodsMode)) {
				this.mode2lastRealizedDomesticGTonKm.compute(attrs.samgodsMode,
						(m, r) -> r == null ? realized_GTonKm : r + realized_GTonKm);
			}
			final VehicleGroup group = this.vehicleType2group.get(vehicleType);
			if (group != null) {
				this.group2lastRealizedDomesticGTonKm.compute(group,
						(g, r) -> r == null ? realized_GTonKm : r + realized_GTonKm);
			}
		}

		this.modeAscTuner.update(m -> this.mode2lastRealizedDomesticGTonKm.getOrDefault(m, 0.0));
		for (ASCTuner<VehicleGroup> groupAscTuner : this.mode2groupAscTuner.values()) {
			groupAscTuner.update(g -> this.group2lastRealizedDomesticGTonKm.get(g));
		}
		this.commodityRailAscTuner.update(c -> this.commodity2lastRealizedRailDomesticGTonKm.getOrDefault(c, 0.0));

		try {
			this.createASCs().writeToFile("./results/calibratedASCs/" + iteration + ".ascs.json");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.logger.log(this);
	}

	public ASCs createASCs() {
		return new ASCs(this.createVehicleType2asc(), this.createMode2asc(), this.createRailCommodity2asc());
	}

	public Map<VehicleGroup, Double> createGroup2asc() {
		final Map<VehicleGroup, Double> group2asc = new LinkedHashMap<>();
		for (ASCTuner<VehicleGroup> ascTuner : this.mode2groupAscTuner.values()) {
			group2asc.putAll(ascTuner.getAlternative2asc());
		}
		return group2asc;
	}

//	public ConcurrentMap<TransportMode, Double> createConcurrentMode2asc() {
//		return new ConcurrentHashMap<>(this.createMode2asc());
//	}
//
//	public ConcurrentMap<VehicleType, Double> createConcurrentVehicleType2asc() {
//		return new ConcurrentHashMap<>(this.createVehicleType2asc());
//	}
//
//	public ConcurrentMap<Commodity, Double> createConcurrentCommodityRailAsc() {
//		return new ConcurrentHashMap<>(this.createRailCommodity2asc());
//	}
}
