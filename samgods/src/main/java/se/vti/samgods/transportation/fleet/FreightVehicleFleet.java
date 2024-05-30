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
package se.vti.samgods.transportation.fleet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.consolidation.road.ConsolidationUtils;
import se.vti.samgods.transportation.consolidation.road.PrototypeVehicle;

/**
 * 
 * @author GunnarF
 *
 */
public class FreightVehicleFleet {

	// -------------------- MEMBERS --------------------

	private final Vehicles vehicles;

	private long vehCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	public FreightVehicleFleet() {
		this.vehicles = VehicleUtils.createVehiclesContainer();
	}

	// -------------------- IMPLEMENTATION --------------------

	public Vehicles getVehicles() {
		return this.vehicles;
	}

	public Vehicle createAndAddVehicle(VehicleType type) {
		assert (this.vehicles.getVehicleTypes().values().contains(type));
		final Vehicle vehicle = this.vehicles.getFactory().createVehicle(Id.create(this.vehCnt++, Vehicle.class), type);
		this.vehicles.addVehicle(vehicle);
		return vehicle;
	}

	// TODO Move to consolidation once getters of FreightVehicleFleet are decided.
	public Map<VehicleType, Vehicle> createPrototypeVehicles() {
		final Map<VehicleType, Vehicle> type2veh = new LinkedHashMap<>(this.vehicles.getVehicleTypes().size());
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final Vehicle prototype = new PrototypeVehicle(type);
			type2veh.put(type, prototype);
		}
		return type2veh;
	}

	// -------------------- SUMMARY TABLES --------------------

	private String null2notAvail(Object c) {
		if (c == null) {
			return "N/A";
		} else {
			return c.toString();
		}
	}

	public String createVehicleTypeTable(SamgodsConstants.TransportMode... modes) {
		final Set<SamgodsConstants.TransportMode> modeSet = Arrays.stream(modes).collect(Collectors.toSet());
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Description", "Mode", "Cost[1/km]", "Cost[1/h]", "Capacity[ton]", "FerryCost[1/km]",
				"FerryCost[1/h]", "MaxSpeed[km/h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
			if (modeSet.contains(attrs.mode)) {
				table.addRule();
				table.addRow(attrs.id, attrs.description, attrs.mode, attrs.cost_1_km, attrs.cost_1_h,
						attrs.capacity_ton, this.null2notAvail(attrs.onFerryCost_1_km),
						this.null2notAvail(attrs.onFerryCost_1_h), this.null2notAvail(attrs.speed_km_h));
			}
		}
		table.addRule();
		return table.render();
	}

	public String createVehicleTransferCostTable(SamgodsConstants.TransportMode... modes) {
		final Set<SamgodsConstants.TransportMode> modeSet = Arrays.stream(modes).collect(Collectors.toSet());
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Commodity", "LoadCost[1/ton]", "LoadTime[h]", "TransferCost[1/ton]",
				"TransferTime[h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
			if (modeSet.contains(attrs.mode)) {
				for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
					if (attrs.containsData(commodity)) {
						table.addRule();
						table.addRow(attrs.id, commodity, this.null2notAvail(attrs.loadCost_1_ton.get(commodity)),
								this.null2notAvail(attrs.loadTime_h.get(commodity)),
								this.null2notAvail(attrs.transferCost_1_ton.get(commodity)),
								this.null2notAvail(attrs.transferTime_h.get(commodity)));
					}
				}
			}
		}
		table.addRule();
		return table.render();
	}

	// -------------------- CONSTRUCT REPRESENTATIVE VEHICLES --------------------

	/*
	 * 
	 * private Double fallbackSpeed_km_h(SamgodsConstants.TransportMode mode) { //
	 * TODO Inventing numbers. One actually should expect all vehicle classes to //
	 * have a speed. if (SamgodsConstants.TransportMode.Air.equals(mode)) { return
	 * 800.0; } else if (SamgodsConstants.TransportMode.Rail.equals(mode)) { return
	 * 80.0; } else if (SamgodsConstants.TransportMode.Road.equals(mode)) { return
	 * 80.0; } else if (SamgodsConstants.TransportMode.Sea.equals(mode)) { return
	 * 16.0; } else { throw new RuntimeException("Unknown transport mode: " + mode);
	 * } }
	 * 
	 * private Double speedOrFallback_km_h(Double speed_km_h,
	 * SamgodsConstants.TransportMode mode) { if (speed_km_h != null) { return
	 * speed_km_h; } else { return this.fallbackSpeed_km_h(mode); } }
	 * 
	 */

	private Double medianOrNull(List<Double> values) {
		final double[] nonNullValues = values.stream().filter(v -> v != null).mapToDouble(v -> v).toArray();
		if (nonNullValues.length > 0) {
			return new Median().evaluate(nonNullValues);
		} else {
			return null;
		}
	}

	public FreightVehicleAttributes createRepresentativeVehicleTypeAttributes(SamgodsConstants.TransportMode mode,
			boolean container) {

		final List<Double> capacities_ton = new ArrayList<>();
		final List<Double> costs_1_h = new ArrayList<>();
		final List<Double> costs_1_km = new ArrayList<>();
		final List<Double> onFerryCosts_1_h = new ArrayList<>();
		final List<Double> onFerryCosts_1_km = new ArrayList<>();
		final List<Double> speeds_km_h = new ArrayList<>();
		final Map<SamgodsConstants.Commodity, List<Double>> commodity2loadCosts_1_ton = new LinkedHashMap<>();
		final Map<SamgodsConstants.Commodity, List<Double>> commodity2loadTimes_h = new LinkedHashMap<>();
		final Map<SamgodsConstants.Commodity, List<Double>> commodity2transferCosts_1_ton = new LinkedHashMap<>();
		final Map<SamgodsConstants.Commodity, List<Double>> commodity2transferTimes_h = new LinkedHashMap<>();

		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
			if (mode.equals(attrs.mode) && (container == attrs.container)) {
				capacities_ton.add(attrs.capacity_ton);
				costs_1_h.add(attrs.cost_1_h);
				costs_1_km.add(attrs.cost_1_km);
				onFerryCosts_1_h.add(attrs.onFerryCost_1_h);
				onFerryCosts_1_km.add(attrs.onFerryCost_1_km);
				speeds_km_h.add(attrs.speed_km_h);
				for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
					commodity2loadCosts_1_ton.computeIfAbsent(commodity, c -> new ArrayList<>())
							.add(attrs.loadCost_1_ton.get(commodity));
					commodity2loadTimes_h.computeIfAbsent(commodity, c -> new ArrayList<>())
							.add(attrs.loadTime_h.get(commodity));
					commodity2transferCosts_1_ton.computeIfAbsent(commodity, c -> new ArrayList<>())
							.add(attrs.transferCost_1_ton.get(commodity));
					commodity2transferTimes_h.computeIfAbsent(commodity, c -> new ArrayList<>())
							.add(attrs.transferTime_h.get(commodity));
				}
			}
		}

		final FreightVehicleAttributes.Builder builder = new FreightVehicleAttributes.Builder();
		builder.setContainer(container);
		builder.setTransportMode(mode);

		builder.setCapacity_ton(this.medianOrNull(capacities_ton));
		builder.setCost_1_h(this.medianOrNull(costs_1_h));
		builder.setCost_1_km(this.medianOrNull(costs_1_km));
		builder.setOnFerryCost_1_h(this.medianOrNull(onFerryCosts_1_h));
		builder.setOnFerryCost_1_km(this.medianOrNull(onFerryCosts_1_km));
		builder.setSpeed_km_h(this.medianOrNull(speeds_km_h));
		for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
			builder.setLoadCost_1_ton(commodity, this.medianOrNull(commodity2loadCosts_1_ton.get(commodity)));
			builder.setLoadTime_h(commodity, this.medianOrNull(commodity2loadTimes_h.get(commodity)));
			builder.setTransferCost_1_ton(commodity, this.medianOrNull(commodity2transferCosts_1_ton.get(commodity)));
			builder.setTransferTime_h(commodity, this.medianOrNull(commodity2transferTimes_h.get(commodity)));
		}

		return builder.buildFreightVehicleAttributes();
	}
}
