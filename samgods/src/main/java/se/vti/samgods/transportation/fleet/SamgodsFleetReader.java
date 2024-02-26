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

import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 * 
 *         TODO continue here. not yet tested. file format? transfer times need
 *         to be added.
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
				builder.setName(this.getStringValue("LABEL")).setDescription(this.getStringValue("DESCRIPTIO"))
						.setTransportMode(SamgodsConstants.TransportMode.Road)
						.setFixedCost_1_km(this.getDoubleValue("KM_COST"))
						.setFixedCost_1_h(this.getDoubleValue("HOURS_COST"))
						.setCapacity_ton(this.getDoubleValue("CAPACITY"))
						.setOnFerryCost_1_km(this.getDoubleValue("ONFER_KM_C"))
						.setOnFerryCost_1_h(this.getDoubleValue("ONFER_H_C"))
						.setMaxSpeed_km_h(this.getDoubleValue("MAX_SPEED"));
			}
		};
		final TabularFileParser vehicleTypeParser = new TabularFileParser();
		vehicleTypeParser.setDelimiterTags(new String[] { ";" });
		vehicleTypeParser.parse(vehicleTypeFile, vehicleTypeHandler);

		final AbstractTabularFileHandlerWithHeaderLine vehicleCostHandler = new AbstractTabularFileHandlerWithHeaderLine() {
			private Double parseSamgodsDouble(String str) {
				final double val = this.getDoubleValue(str);
				if (val == 99988) {
					return null;
				} else {
					return val;
				}
			}

			@Override
			public void startCurrentDataRow() {
				final FreightVehicleFleet.TypeBuilder builder = vehicleNr2builder.get(this.getIntValue("VEH_NR"));
				final SamgodsConstants.Commodity commodity = SamgodsConstants.Commodity
						.values()[this.getIntValue("ID_COM") - 1];
				builder.setLoadCostNoContainer_1_Ton(commodity, parseSamgodsDouble("NC_LCO"))
						.setLoadTimeNoContainer_h(commodity, parseSamgodsDouble("NC_LTI"))
						.setTransferCostNoContainer_1_ton(commodity, parseSamgodsDouble("NC_LCOT"))
						.setTransferTimeNoContainer_h(commodity, parseSamgodsDouble("NC_LTIT"))
						.setLoadCostContainer_1_Ton(commodity, parseSamgodsDouble("CONT_LCO"))
						.setLoadTimeContainer_h(commodity, parseSamgodsDouble("CONT_LTI"))
						.setTransferCostContainer_1_ton(commodity, parseSamgodsDouble("CONT_LCO_T"))
						.setTransferTimeContainer_h(commodity, parseSamgodsDouble("CONT_LTI_T"));
			}
		};
		final TabularFileParser vehicleCostParser = new TabularFileParser();
		vehicleCostParser.setDelimiterTags(new String[] { ";" });
		vehicleCostParser.parse(costFile, vehicleCostHandler);

		for (FreightVehicleFleet.TypeBuilder builder : vehicleNr2builder.values()) {
			builder.buildAndAddToFleet();
		}

		return fleet;
	}

	public static void main(String[] args) throws Exception {
		FreightVehicleFleet fleet = SamgodsFleetReader.loadSamgodsFleet_v12("./2023-06-01_basecase/VehicleTypes.csv",
				"./2023-06-01_basecase/VehicleCosts.csv");

		final boolean includeLoading = false;

		for (VehicleType type : fleet.getVehicleTypes().values()) {
			for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
				System.out.println(type.getId() + " transporting " + commodity + (includeLoading ? ", including (un)loading" : ", move costs only"));
				System.out.println("load factor\tno container price per ton\tcontainer price per ton");
				for (double loadFactor : new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 }) {
					FreightVehicleFleet.TypeAttributes attrs = (FreightVehicleFleet.TypeAttributes) type.getAttributes()
							.getAttribute(FreightVehicleFleet.TypeAttributes.ATTRIBUTE_NAME);
					final double load_ton = loadFactor * attrs.capacity_ton;
					final double fixedCost = 1.0 * attrs.fixedCost_1_km
							+ attrs.fixedCost_1_h * 1.0 / attrs.maxSpeed_km_h;

					final Double pricePerTonNoContainer;
					final Double pricePerTonContainer;
					if (includeLoading) {
						if (attrs.loadCostNoContainer_1_ton.get(commodity) != null) {
							final double loadDependentCost = 2.0 * attrs.loadCostNoContainer_1_ton.get(commodity)
									* load_ton;
							pricePerTonNoContainer = (fixedCost + loadDependentCost) / load_ton;
						} else {
							pricePerTonNoContainer = null;
						}
						if (attrs.loadCostContainer_1_ton.get(commodity) != null) {
							final double loadDependentCost = 2.0 * attrs.loadCostContainer_1_ton.get(commodity)
									* load_ton;
							pricePerTonContainer = (fixedCost + loadDependentCost) / load_ton;
						} else {
							pricePerTonContainer = null;
						}
					} else {
						pricePerTonNoContainer = fixedCost / load_ton;
						pricePerTonContainer = fixedCost / load_ton;
					}

					System.out.println(loadFactor + "\t" + pricePerTonNoContainer + "\t" + pricePerTonContainer);
				}
				System.out.println();
			}
		}

		System.out.println("... DONE");
	}

}
