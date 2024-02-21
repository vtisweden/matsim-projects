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
package se.vti.samgods.consolidation.road;

import java.util.List;
import java.util.Random;

import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class RoadConsolidationExample {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		VehicleFleet fleet = new VehicleFleet();
		VehicleType largeTruck = fleet.createAndAddVehicleType("large truck", SamgodsConstants.TransportMode.Road, 35.0,
				80.0);
		VehicleType smallTruck = fleet.createAndAddVehicleType("small truck", SamgodsConstants.TransportMode.Road, 10.0,
				80.0);

		VehicleConsolidationCostModel costModel = new VehicleConsolidationCostModel() {
			@Override
			public AssignmentCost getCost(Vehicle vehicle, List<Shipment> alreadyPresentShipments,
					Commodity addedCommodity, double maxAddedAmount_ton, ShipmentVehicleAssignment assignment) {
				final double vehicleCapacity_ton = ConsolidationUtils.getCapacity_ton(vehicle);
				final double availableCapacity_ton = vehicleCapacity_ton - assignment.getPayload_ton(vehicle);
				final double assignedWeight_ton = Math.min(maxAddedAmount_ton, availableCapacity_ton);
				final boolean feasible = assignedWeight_ton >= 0.01 * Math.max(maxAddedAmount_ton, vehicleCapacity_ton);
				if (feasible) {
					final double usageFraction = assignedWeight_ton / (assignedWeight_ton + availableCapacity_ton);
					if (largeTruck.getId().equals(vehicle.getType().getId())) {
						return new AssignmentCost(true, assignedWeight_ton, usageFraction * 4.0);
					} else {
						return new AssignmentCost(true, assignedWeight_ton, usageFraction * 4.0);
					}
				} else {
					return new AssignmentCost(false, 0.0, 0.0);
				}
			}
		};

		ConsolidationSlotChoiceModel choiceModel = new LogitConsolidationSlotChoiceModel(costModel, 1.0, new Random());

		Consolidator consolidator = new Consolidator(new Random(), fleet, 2, costModel, choiceModel);

		for (int day = 0; day < 2; day++) {
			for (int i = 1; i <= 6; i++) {
				consolidator.addShipment(new Shipment(SamgodsConstants.Commodity.AGRICULTURE, 0.999 * i, Math.random()));
			}
		}

		consolidator.init();
		System.out.println("----- AFTER INIT -----\n");
		System.out.println(new ConsolidationReport(consolidator.getAssignmentsOverDays()));

		for (int step = 0; step < 3; step++) {
			consolidator.step();
			System.out.println("----- AFTER STEP -----\n");
			System.out.println(new ConsolidationReport(consolidator.getAssignmentsOverDays()));
		}



		System.out.println("... DONE");
	}

}
