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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 * 
 * TODO continue here. not yet tested. file format? transfer times need to be added.
 *
 */
public class SamgodsFleetReader {

	private SamgodsFleetReader() {
	}

	public static FreightVehicleFleet loadSamgodsFleet_v12(String vehicleTypeFile, String costFile) throws IOException {

		final FreightVehicleFleet fleet = new FreightVehicleFleet();
		final Map<Integer, FreightVehicleFleet.TypeBuilder> vehicleNr2builder = new LinkedHashMap<>();

		final AbstractTabularFileHandlerWithHeaderLine vehicleTypeHandler = new AbstractTabularFileHandlerWithHeaderLine() {
			@Override
			public void startCurrentDataRow() {
				final FreightVehicleFleet.TypeBuilder builder = fleet.createTypeBuilder();
				vehicleNr2builder.put(this.getIntValue("VEH_NR"), builder);
				builder.setName(this.getStringValue("LABEL")).setTransportMode(SamgodsConstants.TransportMode.Road)
						.setFixedCost_1_km(this.getDoubleValue("KM_COST"))
						.setFixedCost_1_h(this.getDoubleValue("HOURS_COST"))
						.setCapacity_ton(this.getDoubleValue("CAPACITY"))
						.setOnFerryCost_1_km(this.getDoubleValue("ONFER_KM_C"))
						.setOnFerryCost_1_h(this.getDoubleValue("ONFER_H_C"))
						.setMaxSpeed_km_h(this.getDoubleValue("MAX_SPEED"));
			}
		};
		final TabularFileParser vehicleTypeParser = new TabularFileParser();
		vehicleTypeParser.setDelimiterRegex("\\t");
		vehicleTypeParser.parse(vehicleTypeFile, vehicleTypeHandler);

		final AbstractTabularFileHandlerWithHeaderLine vehicleCostHandler = new AbstractTabularFileHandlerWithHeaderLine() {
			@Override
			public void startCurrentDataRow() {
				final FreightVehicleFleet.TypeBuilder builder = vehicleNr2builder.get(this.getIntValue("VEH_NR"));
				final SamgodsConstants.Commodity commodity = SamgodsConstants.Commodity
						.values()[this.getIntValue("ID_COM") - 1];
				builder.setLoadCostContainer_1_Ton(commodity, this.getDoubleValue("CONT_LCO"))
						.setLoadCostNoContainer_1_Ton(commodity, this.getDoubleValue("NC_LCO"))
						.setTransferCostContainer_1_ton(commodity, this.getDoubleValue("CONT_LCO_T"))
						.setTransferCostNoContainer_1_ton(commodity, this.getDoubleValue("NC_LCOT"));
			}
		};
		final TabularFileParser vehicleCostParser = new TabularFileParser();
		vehicleCostParser.setDelimiterRegex("\\t");
		vehicleCostParser.parse(costFile, vehicleCostHandler);

		for (FreightVehicleFleet.TypeBuilder builder : vehicleNr2builder.values()) {
			builder.buildAndAddToFleet();
		}
		
		return fleet;
	}

}
