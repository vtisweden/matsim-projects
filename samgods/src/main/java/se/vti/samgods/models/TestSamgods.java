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

import se.vti.samgods.InsufficientDataException;
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

		InsufficientDataException.setLogDuringRuntime(false);
		InsufficientDataException.setLogUponShutdown(false);

		List<Commodity> allWithoutAir = new ArrayList<>(Arrays.asList(Commodity.values()));
		allWithoutAir.remove(Commodity.AIR);
		allWithoutAir.toArray();

//		final SamgodsRunner runner = new SamgodsRunner().setServiceInterval_days(7)
//				.setConsideredCommodities(Commodity.AGRICULTURE).setSamplingRate(0.001).setMaxThreads(1)
//				.setScale(Commodity.AGRICULTURE, 0.0004).setScale(Commodity.COAL, 0.0000001)
//				.setScale(Commodity.METAL, 0.00000005).setScale(Commodity.FOOD, 0.00006)
//				.setScale(Commodity.TEXTILES, 0.0003).setScale(Commodity.WOOD, 0.000003)
//				.setScale(Commodity.COKE, 0.00002).setScale(Commodity.CHEMICALS, 0.00002)
//				.setScale(Commodity.OTHERMINERAL, 0.00003).setScale(Commodity.BASICMETALS, 0.00002)
//				.setScale(Commodity.MACHINERY, 0.00006).setScale(Commodity.TRANSPORT, 0.00002)
//				.setScale(Commodity.FURNITURE, 0.0002).setScale(Commodity.SECONDARYRAW, 0.00001)
//				.setScale(Commodity.TIMBER, 0.00009).setScale(Commodity.AIR, 0.00005).setMaxIterations(10)
//				.setEnforceReroute(true);
		final SamgodsRunner runner = new SamgodsRunner().setServiceInterval_days(7)
				.setConsideredCommodities(allWithoutAir.toArray(new Commodity[0])).setSamplingRate(1.0)
				.setMaxThreads(Integer.MAX_VALUE).setScale(Commodity.AGRICULTURE, 0.0004)
				.setScale(Commodity.COAL, 0.0000001).setScale(Commodity.METAL, 0.0 /* estimated has wrong sign */)
				.setScale(Commodity.FOOD, 0.00006).setScale(Commodity.TEXTILES, 0.0003)
				.setScale(Commodity.WOOD, 0.000003).setScale(Commodity.COKE, 0.00002)
				.setScale(Commodity.CHEMICALS, 0.00002).setScale(Commodity.OTHERMINERAL, 0.00003)
				.setScale(Commodity.BASICMETALS, 0.00002).setScale(Commodity.MACHINERY, 0.00006)
				.setScale(Commodity.TRANSPORT, 0.00002).setScale(Commodity.FURNITURE, 0.0002)
				.setScale(Commodity.SECONDARYRAW, 0.00001).setScale(Commodity.TIMBER, 0.00009)
				.setScale(Commodity.AIR, 0.00005).setMaxIterations(100).setEnforceReroute(false);

//		runner.setBackgroundTransportWork(new BackgroundTransportWork().setStepSize(1.0)
//				.setTargetUnitCost_1_tonKm(SamgodsConstants.TransportMode.Road, 1.5)
//				.setTargetUnitCost_1_tonKm(SamgodsConstants.TransportMode.Rail, 0.5)
//				.setTargetUnitCost_1_tonKm(SamgodsConstants.TransportMode.Sea, 0.2)
//				.setTargetUnitCost_1_tonKm(SamgodsConstants.TransportMode.Air, 10.0));

		runner.loadVehicles("./input_2024/vehicleparameters_rail.csv", "./input_2024/transferparameters_rail.csv",
				SamgodsConstants.TransportMode.Rail)
				.loadVehicles("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
						SamgodsConstants.TransportMode.Road)
				.loadVehicles("./input_2024/vehicleparameters_sea.csv", "./input_2024/transferparameters_sea.csv",
						SamgodsConstants.TransportMode.Sea);

		runner.setFleetCostCalibrator(new FleetCostCalibrator(runner.vehicles));

		runner.loadNetwork("./input_2024/node_parameters.csv", "./input_2024/link_parameters.csv");
		runner.loadLinkRegionalWeights("./input_2024/link_regions.csv");
		runner.loadTransportDemand("./input_2024/ChainChoi", "XTD.out");
		runner.createOrLoadConsolidationUnits("consolidationUnits.json");

		runner.run();

		log.info("DONE");
	}

	// ====================================================================================================
	// ====================================================================================================
	// ====================================================================================================

	// LOGGING/TESTING BELOW

	public static void logEfficiency(Map<SamgodsConstants.TransportMode, Double> mode2realizedEfficiency, int iteration,
			String fileName) {
		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				headerLine += "efficiency(" + mode + ")\t";
			}
			try {
				FileUtils.write(new File(fileName), headerLine + "\n", false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		String dataLine = "";
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			if (mode2realizedEfficiency.containsKey(mode)) {
				dataLine += mode2realizedEfficiency.get(mode) + "\t";
			} else {
				dataLine += "\t";
			}
		}
		try {
			FileUtils.write(new File(fileName), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void logCost(Map<SamgodsConstants.TransportMode, Double> mode2unitCost_1_tonKm, int iteration,
			String fileName) {
		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				headerLine += "unitCost(" + mode + ")[SEK/tonKm]\t";
			}
			try {
				FileUtils.write(new File(fileName), headerLine + "\n", false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		String dataLine = "";
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			if (mode2unitCost_1_tonKm.containsKey(mode)) {
				dataLine += mode2unitCost_1_tonKm.get(mode) + "\t";
			} else {
				dataLine += "\t";
			}
		}
		try {
			FileUtils.write(new File(fileName), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void logFleet(Map<VehicleType, Double> vehType2cnt, int iteration, Vehicles vehicles) {

		Map<SamgodsConstants.TransportMode, List<VehicleType>> mode2types = new LinkedHashMap<>();
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			mode2types.put(mode,
					vehicles.getVehicleTypes().values().stream()
							.filter(t -> mode.equals(((SamgodsVehicleAttributes) t.getAttributes()
									.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME)).samgodsMode))
							.collect(Collectors.toList()));
			Collections.sort(mode2types.get(mode), new Comparator<VehicleType>() {
				@Override
				public int compare(VehicleType t1, VehicleType t2) {
					return Double.compare(
							((SamgodsVehicleAttributes) t1.getAttributes()
									.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME)).capacity_ton,
							((SamgodsVehicleAttributes) t2.getAttributes()
									.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME)).capacity_ton);
				}
			});
		}

		if (iteration == 0) {
			String headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				for (VehicleType type : mode2types.get(mode)) {
					headerLine += type.getId().toString() + "\t";
				}
				headerLine += "\t";
			}
			try {
				FileUtils.write(new File("fleet.txt"), headerLine + "\n", false);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			headerLine = "";
			for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
				for (VehicleType type : mode2types.get(mode)) {
					headerLine += ((SamgodsVehicleAttributes) type.getAttributes()
							.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME)).capacity_ton + "\t";
				}
				headerLine += "\t";
			}
			try {
				FileUtils.write(new File("fleet.txt"), headerLine + "\n", true);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}

		double totalCnt = vehType2cnt.values().stream().mapToDouble(c -> c).sum();

		String dataLine = "";
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			for (VehicleType type : mode2types.get(mode)) {
				dataLine += (vehType2cnt.getOrDefault(type, 0.0) / totalCnt) + "\t";
			}
			dataLine += "\t";
		}
		try {
			FileUtils.write(new File("fleet.txt"), dataLine + "\n", true);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class EfficiencyLogger {

		final String file;

		Double binSize = null;

		EfficiencyLogger(String file) {
			this.file = file;
		}

		void log(Collection<Double> efficiencies) {

			double maxEff = efficiencies.stream().mapToDouble(e -> e).max().getAsDouble();

			if (this.binSize == null) {
				this.binSize = maxEff / 20;
				try {
					FileUtils.write(new File(this.file),
							IntStream.range(0, 25).boxed().map(b -> Double.toString((0.5 + b) * this.binSize))
									.collect(Collectors.joining("\t")) + "\n",
							false);
				} catch (IOException e1) {
					throw new RuntimeException();
				}
			}

			int[] cnt = new int[1 + (int) Math.ceil(maxEff / this.binSize)];
			for (Double eff : efficiencies) {
				cnt[(int) (eff / this.binSize)]++;
			}

			try {
				FileUtils.write(new File(this.file),
						Arrays.stream(cnt).boxed().map(c -> "" + c).collect(Collectors.joining("\t")) + "\n", true);
			} catch (IOException e1) {
				throw new RuntimeException();
			}
		}

	}

}
