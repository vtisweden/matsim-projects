/**
 * se.vti.samgods.transportation.consolidation.uniform
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
package se.vti.samgods.deprecated.logitprocessconsolidation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.deprecated.Shipment;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.SamgodsFleetReader;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class UniformConsolidator {

	private boolean verbose = false;

	/*- TODOs
	 * 
	 * -- Shipments are probabilistic, here we (require to) round to fractional shipments.
	 * 
	 * -- Assuming (for testing) a one-over-capacity vehicle frequency distribution.
	 * 
	 */

	private final VehicleFleet fleet;

	private final int numberOfDays;
	private final double transportEfficiency;

	private final List<VehicleType> vehTypesDescendingCapacity;
	private final double[] vehCaps;
	private final double[] vehFreq;
	private final double[] vehTonFreq;

	// CONSTRUCTION

	public UniformConsolidator(VehicleFleet fleet, int numberOfDays, double transportEfficiency) {
		this.fleet = fleet;
		this.numberOfDays = numberOfDays;
		this.transportEfficiency = transportEfficiency;

		this.vehTypesDescendingCapacity = fleet.getVehicles().getVehicleTypes().values().stream()
				.collect(Collectors.toList());
		Collections.sort(this.vehTypesDescendingCapacity, new Comparator<VehicleType>() {
			@Override
			public int compare(VehicleType t1, VehicleType t2) {
				return -Double.compare(FreightVehicleAttributes.getCapacity_ton(t1),
						FreightVehicleAttributes.getCapacity_ton(t2));
			}
		});
		this.vehCaps = this.vehTypesDescendingCapacity.stream()
				.mapToDouble(t -> FreightVehicleAttributes.getCapacity_ton(t)).toArray();

		{
			this.vehFreq = new double[this.vehTypesDescendingCapacity.size()];
			double sum = 0.0;
			for (int i = 0; i < this.vehTypesDescendingCapacity.size(); i++) {
				this.vehFreq[i] = 1.0
						/ FreightVehicleAttributes.getCapacity_ton(this.vehTypesDescendingCapacity.get(i));
				sum += this.vehFreq[i];
			}
			for (int i = 0; i < this.vehTypesDescendingCapacity.size(); i++) {
				this.vehFreq[i] /= sum;
			}
		}
		{
			this.vehTonFreq = new double[this.vehTypesDescendingCapacity.size()];
			double sum = 0.0;
			for (int i = 0; i < this.vehTypesDescendingCapacity.size(); i++) {
				this.vehTonFreq[i] = this.vehFreq[i]
						* FreightVehicleAttributes.getCapacity_ton(this.vehTypesDescendingCapacity.get(i));
				sum += this.vehTonFreq[i];
			}
			for (int i = 0; i < this.vehTypesDescendingCapacity.size(); i++) {
				this.vehTonFreq[i] /= sum;
			}
		}
	}

	// REMOVE STOCHASTICITY

	public static List<Shipment> createProbabilityScaledShipments(List<Shipment> shipments) {
		final List<Shipment> result = shipments.stream()
				.map(s -> new Shipment(s.getCommodity(), s.getProbability() * s.getWeight_ton(), 1.0))
				.collect(Collectors.toList());
		return result;
	}

	public static List<Shipment> createSimulatedShipments(List<Shipment> shipments, Random rnd) {
		final List<Shipment> result = shipments.stream().filter(s -> rnd.nextDouble() < s.getProbability())
				.map(s -> new Shipment(s.getCommodity(), s.getWeight_ton(), 1.0)).collect(Collectors.toList());
		return result;
	}

	// WEEKLY DISTRIBUTION, DISCRETE

	public List<List<Shipment>> distributeShipmentsOverDays(List<Shipment> shipments) {

		final List<Shipment> sortedShipments = new ArrayList<>(shipments);
		Collections.sort(sortedShipments, new Comparator<Shipment>() {
			@Override
			public int compare(Shipment s1, Shipment s2) {
				return -Double.compare(s1.getWeight_ton(), s2.getWeight_ton());
			}
		});
		shipments = null; // use sortedShipments

		final List<List<Shipment>> shipmentsPerDay = IntStream.range(0, this.numberOfDays).boxed()
				.map(d -> new ArrayList<Shipment>()).collect(Collectors.toList());
		final double[] dailyLoad_ton = new double[this.numberOfDays];

		for (Shipment shipment : sortedShipments) {
			assert (shipment.getProbability() == 1.0);
			int day = IntStream.range(0, dailyLoad_ton.length)
					.reduce((d, e) -> dailyLoad_ton[d] <= dailyLoad_ton[e] ? d : e).getAsInt();
			shipmentsPerDay.get(day).add(shipment);
			dailyLoad_ton[day] += shipment.getWeight_ton();

			if (this.verbose) {
				System.out.println(
						Arrays.stream(dailyLoad_ton).boxed().map(l -> l.toString()).collect(Collectors.joining("\t"))
								+ "\t\t");
			}
		}

		if (this.verbose) {
			System.out.println();
		}

		return shipmentsPerDay;
	}

	// FLEET DISTRIBUTION

	public List<List<Vehicle>> createVehiclesOverDays(List<List<Shipment>> shipmentsPerDay) {

		final double[] minCapacityPerDay_ton = new double[this.numberOfDays];
		for (int day = 0; day < this.numberOfDays; day++) {
			minCapacityPerDay_ton[day] = shipmentsPerDay.get(day).stream().mapToDouble(s -> s.getWeight_ton()).sum();
		}

		final int[][] vehTypesPerDay = new int[this.numberOfDays][this.vehTypesDescendingCapacity.size()];
		final double[] vehTonsPerDay = new double[this.numberOfDays];
		final double[] vehTonsPerType = new double[this.vehTypesDescendingCapacity.size()];
		double totalVehTons = 0.0;

		boolean done = false;
		boolean feasible = false;

		double vehFreqE2 = 0;
		double demandE2 = Arrays.stream(minCapacityPerDay_ton).map(d -> Math.pow(d / this.transportEfficiency, 2.0))
				.sum();

		double vehFreqE2Weight = 1.0 / this.vehTypesDescendingCapacity.size();
		double demandE2Weight = 1.0 / this.numberOfDays;

		while (!done) {

			double bestVehFreqE2 = Double.POSITIVE_INFINITY;
			double bestDemandE2 = Double.POSITIVE_INFINITY;

			int bestDay = -1;
			int bestVehTypeIndex = -1;
			Boolean bestIsFeasible = null;

			for (int vehTypeIndex = 0; vehTypeIndex < this.vehTypesDescendingCapacity.size(); vehTypeIndex++) {
				for (int day = 0; day < this.numberOfDays; day++) {
					vehTypesPerDay[day][vehTypeIndex]++;

					double candVehFreqE2 = 0.0;
					for (int vehTypeIndex2 = 0; vehTypeIndex2 < this.vehTypesDescendingCapacity
							.size(); vehTypeIndex2++) {
						candVehFreqE2 += Math.pow(
								vehTonsPerType[vehTypeIndex2]
										+ (vehTypeIndex2 == vehTypeIndex ? this.vehCaps[vehTypeIndex] : 0.0)
										- this.vehTonFreq[vehTypeIndex2] * (totalVehTons + this.vehCaps[vehTypeIndex]),
								2.0);
					}

					double candDemandE2 = demandE2
							- Math.pow(vehTonsPerDay[day] - minCapacityPerDay_ton[day] / this.transportEfficiency, 2.0)
							+ Math.pow((vehTonsPerDay[day] + this.vehCaps[vehTypeIndex])
									- minCapacityPerDay_ton[day] / this.transportEfficiency, 2.0);

					if (vehFreqE2Weight * candVehFreqE2 + demandE2Weight * candDemandE2 < vehFreqE2Weight
							* bestVehFreqE2 + demandE2Weight * bestDemandE2) {

						bestDay = day;
						bestVehTypeIndex = vehTypeIndex;

						bestVehFreqE2 = candVehFreqE2;
						bestDemandE2 = candDemandE2;

						bestIsFeasible = true;
						for (int day2 = 0; day2 < this.numberOfDays; day2++) {
							bestIsFeasible = bestIsFeasible && (vehTonsPerDay[day2]
									+ (day2 == day ? this.vehCaps[vehTypeIndex] : 0.0) >= minCapacityPerDay_ton[day2]);
						}
					}
					vehTypesPerDay[day][vehTypeIndex]--;
				}
			}

			if (vehFreqE2Weight * bestVehFreqE2 + demandE2Weight * bestDemandE2 >= vehFreqE2Weight * vehFreqE2
					+ demandE2Weight * demandE2 && feasible) {
				done = true;
			} else {
				vehTypesPerDay[bestDay][bestVehTypeIndex]++;
				vehTonsPerDay[bestDay] += this.vehCaps[bestVehTypeIndex];
				vehTonsPerType[bestVehTypeIndex] += this.vehCaps[bestVehTypeIndex];
				totalVehTons += this.vehCaps[bestVehTypeIndex];
				feasible = bestIsFeasible;

				vehFreqE2 = bestVehFreqE2;
				demandE2 = bestDemandE2;

			}

			if (this.verbose) {
				System.out.print(vehFreqE2 + "\t" + demandE2 + "\t" + feasible + "\t\t");
				for (int day = 0; day < this.numberOfDays; day++) {
					double target = minCapacityPerDay_ton[day] / this.transportEfficiency;
					System.out.print((vehTonsPerDay[day] - target) / target);
					System.out.print("\t");
				}
				System.out.print("\t");
				for (int vehTypeIndex = 0; vehTypeIndex < this.vehTypesDescendingCapacity.size(); vehTypeIndex++) {
					double target = this.vehTonFreq[vehTypeIndex] * Arrays.stream(vehTonsPerType).sum();
					System.out.print((vehTonsPerType[vehTypeIndex] - target) / target);
					System.out.print("\t");
				}
				System.out.println();
			}
		}

		final List<List<Vehicle>> vehiclesOverDays = new ArrayList<>(this.numberOfDays);
		for (int day = 0; day < this.numberOfDays; day++) {
			final List<Vehicle> vehicles = new LinkedList<>();
			for (int vehTypeIndex = 0; vehTypeIndex < this.vehTypesDescendingCapacity.size(); vehTypeIndex++) {
				for (int n = 0; n < vehTypesPerDay[day][vehTypeIndex]; n++) {
					vehicles.add(fleet.createAndAddVehicle(this.vehTypesDescendingCapacity.get(vehTypeIndex)));
				}
			}
			vehiclesOverDays.add(vehicles);
		}
		return vehiclesOverDays;
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws InsufficientDataException, IOException {
		VehicleFleet fleet = new VehicleFleet();
		SamgodsFleetReader fleetReader = new SamgodsFleetReader(fleet);
		fleetReader.load_v12("./input_2024/vehicleparameters_road.csv", "./input_2024/transferparameters_road.csv",
				SamgodsConstants.TransportMode.Road);

		Random rnd = new Random();
		UniformConsolidator uc = new UniformConsolidator(fleet, 7, 0.7);

		int shipmentCnt = 1000;
		List<Shipment> shipments = new ArrayList<>(shipmentCnt);
		for (int i = 0; i < shipmentCnt; i++) {
			double tons = 20.0 * rnd.nextExponential();
			double proba = rnd.nextDouble();
			shipments.add(new Shipment(null, tons, proba));
		}

		shipments = UniformConsolidator.createProbabilityScaledShipments(shipments);
		List<List<Shipment>> shipmentsOverDay = uc.distributeShipmentsOverDays(shipments);
		List<List<Vehicle>> vehiclesOverDays = uc.createVehiclesOverDays(shipmentsOverDay);

		Map<Id<VehicleType>, Long> type2cnt = new LinkedHashMap<>();
		for (List<Vehicle> vehicles : vehiclesOverDays) {
			for (Vehicle vehicle : vehicles) {
				type2cnt.compute(vehicle.getType().getId(), (id, cnt) -> cnt == null ? 1 : cnt + 1);
			}
		}
		double totalTons = type2cnt.entrySet().stream().mapToDouble(
				e -> FreightVehicleAttributes.getCapacity_ton(fleet.getVehicles().getVehicleTypes().get(e.getKey()))
						* e.getValue())
				.sum();

		for (VehicleType type : fleet.getVehicles().getVehicleTypes().values()) {
			System.out.print(type2cnt.getOrDefault(type.getId(), 0l) * FreightVehicleAttributes.getCapacity_ton(type)
					/ totalTons);
			System.out.print("\t");
		}
		System.out.println();

	}

}
