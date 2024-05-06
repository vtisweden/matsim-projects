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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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

//	public void addVehicleType(VehicleType type) {
//		this.vehicles.addVehicleType(type);
//	}

//	public Map<Id<VehicleType>, VehicleType> getVehicleTypesView() {
//		return this.vehicles.getVehicleTypes();
//	}

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

	public String createVehicleTypeTable(SamgodsConstants.TransportMode... modes) {
		final Set<SamgodsConstants.TransportMode> modeSet = Arrays.stream(modes).collect(Collectors.toSet());
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Description", "Mode", "Cost[1/km]", "Cost[1/h]", "Capacity[ton]", "FerryCost[1/km]",
				"FerryCost[1/h]", "MaxSpeed[km/h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleTypeAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
			if (modeSet.contains(attrs.mode)) {
				table.addRule();
				table.addRow(attrs.id, attrs.description, attrs.mode, attrs.cost_1_km, attrs.cost_1_h,
						attrs.capacity_ton, this.null2notAvail(attrs.onFerryCost_1_km),
						this.null2notAvail(attrs.onFerryCost_1_h), attrs.maxSpeed_km_h);
			}
		}
		table.addRule();
		return table.render();
	}

	private String null2notAvail(Object c) {
		if (c == null) {
			return "N/A";
		} else {
			return c.toString();
		}
	}

	public String createVehicleTransferCostTable(SamgodsConstants.TransportMode... modes) {
		final Set<SamgodsConstants.TransportMode> modeSet = Arrays.stream(modes).collect(Collectors.toSet());
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Commodity", "LoadCost[1/ton]", "LoadTime[h]", "TransferCost[1/ton]",
				"TransferTime[h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleTypeAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
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
}
