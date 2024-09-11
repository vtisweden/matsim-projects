/**
 * se.vti.samgods.transportation.fleet
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
package se.vti.samgods.transportation.fleet;

import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetStatsTable {

	private final Vehicles vehicles;

	public FleetStatsTable(Vehicles vehicles) {
		this.vehicles = vehicles;
	}

	private String null2notAvail(Object c) {
		if (c == null) {
			return "N/A";
		} else {
			return c.toString();
		}
	}

	public String createVehicleTypeTable(SamgodsConstants.TransportMode mode) {
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Description", "Mode", "Cost[1/km]", "Cost[1/h]", "Capacity[ton]", "FerryCost[1/km]",
				"FerryCost[1/h]", "MaxSpeed[km/h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final SamgodsVehicleAttributes attrs = (SamgodsVehicleAttributes) type.getAttributes()
					.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
			if (mode.equals(attrs.samgodsMode)) {
				table.addRule();
				table.addRow(attrs.id, type.getDescription(), attrs.samgodsMode, attrs.cost_1_km, attrs.cost_1_h,
						attrs.capacity_ton, this.null2notAvail(attrs.onFerryCost_1_km),
						this.null2notAvail(attrs.onFerryCost_1_h), this.null2notAvail(attrs.speed_km_h));
			}
		}
		table.addRule();
		return table.render();
	}

	public String createVehicleTransferCostTable(SamgodsConstants.TransportMode mode) {
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Commodity", "LoadCost[1/ton]", "LoadTime[h]", "TransferCost[1/ton]",
				"TransferTime[h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final SamgodsVehicleAttributes attrs = (SamgodsVehicleAttributes) type.getAttributes()
					.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
			if (mode.equals(attrs.samgodsMode)) {
				for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
					if (attrs.isCompatible(commodity)) {
						table.addRule();
						table.addRow(attrs.id, commodity, attrs.loadCost_1_ton.get(commodity),
								attrs.loadTime_h.get(commodity), attrs.transferCost_1_ton.get(commodity),
								attrs.transferTime_h.get(commodity));
					}
				}
			}
		}
		table.addRule();
		return table.render();
	}

}
