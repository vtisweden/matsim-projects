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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
			final SamgodsVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
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
			final SamgodsVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
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
//
//	private Double medianOrNull(List<Double> values) {
//		final double[] nonNullValues = values.stream().filter(v -> v != null).mapToDouble(v -> v).toArray();
//		if (nonNullValues.length > 0) {
//			return new Median().evaluate(nonNullValues);
//		} else {
//			return null;
//		}
//	}

	public SamgodsVehicleAttributes getRepresentativeVehicleAttributes(SamgodsConstants.TransportMode mode,
			boolean container, Function<SamgodsVehicleAttributes, Double> attributes2property) {
		return this.getRepresentativeVehicleAttributes(Collections.singleton(mode), container, attributes2property);
	}

	public SamgodsVehicleAttributes getRepresentativeVehicleAttributes(Set<SamgodsConstants.TransportMode> modes,
			boolean container, Function<SamgodsVehicleAttributes, Double> attributes2property) {

		final List<SamgodsVehicleAttributes> attributesList = new ArrayList<>();
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final SamgodsVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
			if (modes.contains(attrs.mode) && (container == attrs.container)
					&& (attributes2property.apply(attrs) != null)) {
				attributesList.add(attrs);
			}
		}

		if (attributesList.size() == 0) {
			return null;
		}

		Collections.sort(attributesList, new Comparator<SamgodsVehicleAttributes>() {
			@Override
			public int compare(SamgodsVehicleAttributes attrs1, SamgodsVehicleAttributes attrs2) {
				return Double.compare(attributes2property.apply(attrs1), attributes2property.apply(attrs2));
			}
		});

		final int upperMedianIndex = attributesList.size() / 2;
		if (attributesList.size() % 2 != 0) {
			return attributesList.get(upperMedianIndex);
		} else {
			final double mean = attributesList.stream().mapToDouble(a -> attributes2property.apply(a)).average()
					.getAsDouble();
			final double lower = attributes2property.apply(attributesList.get(upperMedianIndex - 1));
			final double upper = attributes2property.apply(attributesList.get(upperMedianIndex));
			if (Math.abs(lower - mean) <= Math.abs(upper - mean)) {
				return attributesList.get(upperMedianIndex - 1);
			} else {
				return attributesList.get(upperMedianIndex);
			}
		}
	}
}
