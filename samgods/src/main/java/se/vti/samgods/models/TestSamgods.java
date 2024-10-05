/**
 * se.vti.samgods.models
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
package se.vti.samgods.models;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsRunner;
import se.vti.samgods.calibration.FleetCostCalibrator;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class TestSamgods {

	static final Logger log = Logger.getLogger(TestSamgods.class);

	static final String consolidationUnitsFileName = "consolidationUnits.json";

	public static void main(String[] args) throws IOException {

		log.info("STARTED ...");

		List<Commodity> allWithoutAir = new ArrayList<>(Arrays.asList(Commodity.values()));
		allWithoutAir.remove(Commodity.AIR);
		allWithoutAir.toArray();

		final double scaleFactor = 1.0;
//		final SamgodsRunner runner = new SamgodsRunner().setServiceInterval_days(7)
//				.setConsideredCommodities(Commodity.AGRICULTURE).setSamplingRate(0.001)
//				.setMaxThreads(1).setScale(Commodity.AGRICULTURE, scaleFactor * 0.0004)
//				.setScale(Commodity.COAL, scaleFactor * 0.0000001).setScale(Commodity.METAL, scaleFactor * 0.0000001
//				/* METAL: using coal parameter because, estimated has wrong sign */)
//				.setScale(Commodity.FOOD, scaleFactor * 0.00006).setScale(Commodity.TEXTILES, scaleFactor * 0.0003)
//				.setScale(Commodity.WOOD, scaleFactor * 0.000003).setScale(Commodity.COKE, scaleFactor * 0.00002)
//				.setScale(Commodity.CHEMICALS, scaleFactor * 0.00002)
//				.setScale(Commodity.OTHERMINERAL, scaleFactor * 0.00003)
//				.setScale(Commodity.BASICMETALS, scaleFactor * 0.00002)
//				.setScale(Commodity.MACHINERY, scaleFactor * 0.00006)
//				.setScale(Commodity.TRANSPORT, scaleFactor * 0.00002)
//				.setScale(Commodity.FURNITURE, scaleFactor * 0.0002)
//				.setScale(Commodity.SECONDARYRAW, scaleFactor * 0.00001)
//				.setScale(Commodity.TIMBER, scaleFactor * 0.00009).setScale(Commodity.AIR, scaleFactor * 0.00005)
//				.setMaxIterations(2).setEnforceReroute(true);
		final SamgodsRunner runner = new SamgodsRunner().setServiceInterval_days(7)
				.setConsideredCommodities(allWithoutAir.toArray(new Commodity[0])).setSamplingRate(1.0)
				.setMaxThreads(Integer.MAX_VALUE).setScale(Commodity.AGRICULTURE, scaleFactor * 0.0004)
				.setScale(Commodity.COAL, scaleFactor * 0.0000001).setScale(Commodity.METAL, scaleFactor * 0.0000001
				/* METAL: using coal parameter because, estimated has wrong sign */)
				.setScale(Commodity.FOOD, scaleFactor * 0.00006).setScale(Commodity.TEXTILES, scaleFactor * 0.0003)
				.setScale(Commodity.WOOD, scaleFactor * 0.000003).setScale(Commodity.COKE, scaleFactor * 0.00002)
				.setScale(Commodity.CHEMICALS, scaleFactor * 0.00002)
				.setScale(Commodity.OTHERMINERAL, scaleFactor * 0.00003)
				.setScale(Commodity.BASICMETALS, scaleFactor * 0.00002)
				.setScale(Commodity.MACHINERY, scaleFactor * 0.00006)
				.setScale(Commodity.TRANSPORT, scaleFactor * 0.00002)
				.setScale(Commodity.FURNITURE, scaleFactor * 0.0002)
				.setScale(Commodity.SECONDARYRAW, scaleFactor * 0.00001)
				.setScale(Commodity.TIMBER, scaleFactor * 0.00009).setScale(Commodity.AIR, scaleFactor * 0.00005)
				.setMaxIterations(1000).setEnforceReroute(false);

		runner.loadVehicles("./input_2024/vehicleparameters_rail.csv", "./input_2024/transferparameters_rail.csv",
				SamgodsConstants.TransportMode.Rail, "WG950", "KOMXL", "SYSXL", "WGEXL")
				.loadVehicles("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
						SamgodsConstants.TransportMode.Road, "HGV74")
				.loadVehicles("./input_2024/vehicleparameters_sea.csv", "./input_2024/transferparameters_sea.csv",
						SamgodsConstants.TransportMode.Sea, "ROF7", "RAF5", "INW", "ROF2", "ROF5");

		runner.setFleetCostCalibrator(new FleetCostCalibrator(runner.vehicles, 1.0));

		runner.loadNetwork("./input_2024/node_parameters.csv", "./input_2024/link_parameters.csv");
		runner.loadLinkRegionalWeights("./input_2024/link_regions_domestic.csv");

		runner.loadTransportDemand("./input_2024/ChainChoi", "XTD.out");
		runner.createOrLoadConsolidationUnits("consolidationUnits.json");

		runner.run();

		log.info("DONE");
	}
}
