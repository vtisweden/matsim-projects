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
 */
public class SamgodsFleetReader {

	// -------------------- CONSTANTS --------------------

	public static final String VEH_NR = "VEH_NR";

	public static final String VEH_LABEL = "LABEL";

	public static final String VEH_DESCRIPTION = "DESCRIPTIO";

	public static final String COST_1_KM = "KM_COST";

	public static final String COST_1_H = "HOURS_COST";

	public static final String CAPACITY_TON = "CAPACITY";

	public static final String ON_FERRY_COST_1_KM = "ONFER_KM_C";

	public static final String ON_FERRY_COST_1_H = "ONFER_H_C";

	public static final String MAX_SPEED_KM_H = "MAX_SPEED";

	public static final String COMMODITY_ID = "ID_COM";

	public static final String NO_CONTAINER_LOAD_COST_1_TON = "NC_LCO";

	public static final String NO_CONTAINER_LOAD_TIME_H = "NC_LTI";

	public static final String NO_CONTAINER_TRANSFER_COST_1_TON = "NC_LCOT";

	public static final String NO_CONTAINER_TRANSFER_TIME_H = "NC_LTIT";

	public static final String CONTAINER_LOAD_COST_1_TON = "CONT_LCO";

	public static final String CONTAINER_LOAD_TIME_H = "CONT_LTI";

	public static final String CONTAINER_TRANSFER_COST_1_TON = "CONT_LCO_T";

	public static final String CONTAINER_TRANSFER_TIME_H = "CONT_LTI_T";

	public static final double MAGIC_NUMBER_INDICATING_IMPOSSIBLE_TRANSFER = 99999;

	public static final double MAGIC_NUMBER_INDICATING_IMPOSSIBLE_FERRY = 0;

	public static final String SUFFIX_INDICATING_CONTAINER = "_CONTAINER";

	// -------------------- MEMBERS --------------------

	private final FreightVehicleFleet fleet;

	// -------------------- CONSTRUCTION --------------------

	public SamgodsFleetReader(FreightVehicleFleet fleet) {
		this.fleet = fleet;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void load_v12(String vehicleTypeFile, String costFile, SamgodsConstants.TransportMode transportMode)
			throws IOException {

		final Map<String, FreightVehicleTypeAttributes.Builder> vehicleNr2builder = new LinkedHashMap<>();

		final AbstractTabularFileHandlerWithHeaderLine vehicleTypeHandler = new AbstractTabularFileHandlerWithHeaderLine() {

			private Double getSamgodsFerryDouble(String str) {
				final double val = this.getDoubleValue(str);
				if (val == MAGIC_NUMBER_INDICATING_IMPOSSIBLE_FERRY) {
					return null;
				} else {
					return val;
				}
			}

			private void createBuilder(boolean container) {
				final FreightVehicleTypeAttributes.Builder builder = new FreightVehicleTypeAttributes.Builder();
				vehicleNr2builder.put(this.getIntValue(VEH_NR) + (container ? SUFFIX_INDICATING_CONTAINER : ""),
						builder);
				builder.setName(this.getStringValue(VEH_LABEL) + (container ? SUFFIX_INDICATING_CONTAINER : ""))
						.setDescription(this.getStringValue(VEH_DESCRIPTION)).setTransportMode(transportMode)
						.setCost_1_km(this.getDoubleValue(COST_1_KM)).setCost_1_h(this.getDoubleValue(COST_1_H))
						.setCapacity_ton(this.getDoubleValue(CAPACITY_TON))
						.setOnFerryCost_1_km(this.getSamgodsFerryDouble(ON_FERRY_COST_1_KM))
						.setOnFerryCost_1_h(this.getSamgodsFerryDouble(ON_FERRY_COST_1_H))
						.setMaxSpeed_km_h(this.getDoubleValue(MAX_SPEED_KM_H));
			}

			@Override
			public void startCurrentDataRow() {
				this.createBuilder(true);
				this.createBuilder(false);
			}
		};
		final TabularFileParser vehicleTypeParser = new TabularFileParser();
		vehicleTypeParser.setDelimiterTags(new String[] { "," });
		vehicleTypeParser.parse(vehicleTypeFile, vehicleTypeHandler);

		final AbstractTabularFileHandlerWithHeaderLine vehicleCostHandler = new AbstractTabularFileHandlerWithHeaderLine() {

			private Double parseSamgodsTransferDouble(String str) {
				final double val = this.getDoubleValue(str);
				if (val == MAGIC_NUMBER_INDICATING_IMPOSSIBLE_TRANSFER) {
					return null;
				} else {
					return val;
				}
			}

			@Override
			public void startCurrentDataRow() {
				{ // no container
					final FreightVehicleTypeAttributes.Builder builder = vehicleNr2builder
							.get(this.getIntValue(VEH_NR) + "");
					final SamgodsConstants.Commodity commodity = SamgodsConstants.Commodity
							.values()[this.getIntValue(COMMODITY_ID) - 1];
					builder.setContainer(false)
							.setLoadCost_1_ton(commodity, parseSamgodsTransferDouble(NO_CONTAINER_LOAD_COST_1_TON))
							.setLoadTime_h(commodity, parseSamgodsTransferDouble(NO_CONTAINER_LOAD_TIME_H))
							.setTransferCost_1_ton(commodity,
									parseSamgodsTransferDouble(NO_CONTAINER_TRANSFER_COST_1_TON))
							.setTransferTime_h(commodity, parseSamgodsTransferDouble(NO_CONTAINER_TRANSFER_TIME_H));
				}
				{ // container
					final FreightVehicleTypeAttributes.Builder builder = vehicleNr2builder
							.get(this.getIntValue(VEH_NR) + SUFFIX_INDICATING_CONTAINER);
					final SamgodsConstants.Commodity commodity = SamgodsConstants.Commodity
							.values()[this.getIntValue(COMMODITY_ID) - 1];
					builder.setContainer(true)
							.setLoadCost_1_ton(commodity, parseSamgodsTransferDouble(CONTAINER_LOAD_COST_1_TON))
							.setLoadTime_h(commodity, parseSamgodsTransferDouble(CONTAINER_LOAD_TIME_H))
							.setTransferCost_1_ton(commodity, parseSamgodsTransferDouble(CONTAINER_TRANSFER_COST_1_TON))
							.setTransferTime_h(commodity, parseSamgodsTransferDouble(CONTAINER_TRANSFER_TIME_H));
				}
			}
		};

		final TabularFileParser vehicleCostParser = new TabularFileParser();
		vehicleCostParser.setDelimiterTags(new String[] { "," });
		vehicleCostParser.parse(costFile, vehicleCostHandler);

		for (FreightVehicleTypeAttributes.Builder builder : vehicleNr2builder.values()) {
			final VehicleType type = builder.build();
			this.fleet.getVehicles().addVehicleType(type);
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws Exception {
		FreightVehicleFleet fleet = new FreightVehicleFleet();
		SamgodsFleetReader reader = new SamgodsFleetReader(fleet);
		reader.load_v12("./input_2024/vehicleparameters_air.csv", "./input_2024/transferparameters_air.csv",
				SamgodsConstants.TransportMode.Air);
		reader.load_v12("./input_2024/vehicleparameters_rail.csv", "./input_2024/transferparameters_rail.csv",
				SamgodsConstants.TransportMode.Rail);
		reader.load_v12("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
				SamgodsConstants.TransportMode.Road);
		reader.load_v12("./input_2024/vehicleparameters_sea.csv", "./input_2024/transferparameters_sea.csv",
				SamgodsConstants.TransportMode.Sea);

		System.out.println(fleet.createVehicleTypeTable(SamgodsConstants.TransportMode.Rail));
		System.out.println();
		System.out.println(fleet.createVehicleTransferCostTable(SamgodsConstants.TransportMode.Rail));
		System.exit(0);

		final boolean includeLoading = false;

		for (VehicleType type : fleet.getVehicles().getVehicleTypes().values()) {
			for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
				System.out.println(type.getId() + " transporting " + commodity
						+ (includeLoading ? ", including (un)loading" : ", move costs only"));
				System.out.println("load factor\tno container price per ton\tcontainer price per ton");
				for (double loadFactor : new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 }) {
					FreightVehicleTypeAttributes attrs = (FreightVehicleTypeAttributes) type.getAttributes()
							.getAttribute(FreightVehicleTypeAttributes.ATTRIBUTE_NAME);
					final double load_ton = loadFactor * attrs.capacity_ton;
					final double fixedCost = 1.0 * attrs.cost_1_km + attrs.cost_1_h * 1.0 / attrs.maxSpeed_km_h;

					final Double pricePerTonNoContainer;
					final Double pricePerTonContainer;
					if (includeLoading) {
						if (attrs.loadCost_1_ton.get(commodity) != null) {
							final double loadDependentCost = 2.0 * attrs.loadCost_1_ton.get(commodity) * load_ton;
							pricePerTonNoContainer = (fixedCost + loadDependentCost) / load_ton;
						} else {
							pricePerTonNoContainer = null;
						}
						if (attrs.loadCost_1_ton.get(commodity) != null) {
							final double loadDependentCost = 2.0 * attrs.loadCost_1_ton.get(commodity) * load_ton;
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
