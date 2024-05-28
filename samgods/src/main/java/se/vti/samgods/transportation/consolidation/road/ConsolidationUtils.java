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
package se.vti.samgods.transportation.consolidation.road;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.choicemodel.SizeClass;
import se.vti.samgods.transportation.fleet.FreightVehicleTypeAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationUtils {

	private ConsolidationUtils() {
	}

	public static FreightVehicleTypeAttributes getFreightAttributes(VehicleType vehicleType) {
		return (FreightVehicleTypeAttributes) vehicleType.getAttributes()
				.getAttribute(FreightVehicleTypeAttributes.ATTRIBUTE_NAME);
	}

	public static FreightVehicleTypeAttributes getFreightAttributes(Vehicle vehicle) {
		return getFreightAttributes(vehicle.getType());
	}

	public static double getCapacity_ton(VehicleType vehicleType) {
		return getFreightAttributes(vehicleType).capacity_ton;
	}

	public static double getCapacity_ton(Vehicle vehicle) {
		return getFreightAttributes(vehicle).capacity_ton;
	}
	
	
	/*
	 * TODO CONTINUE HERE
	 */
	public static List<Shipment> disaggregateIntoAnalysisPeriod(AnnualShipment shipment, int analysisPeriod_days, SizeClass sizeClass) {

		final double analysisPeriodAmount_ton = shipment.getTotalAmount_ton() * analysisPeriod_days / 365.0;
		
		final double shipmentsPerPeriod = analysisPeriodAmount_ton / sizeClass.getMeanValue_ton();
		final double individualShipmentSize_ton = shipment.getTotalAmount_ton() / Math.ceil(shipmentsPerPeriod);
		
		final int integerShipmentsPerPeriod = (int) shipmentsPerPeriod;
		final double fractionalShipmentsPerPeriod = shipmentsPerPeriod - integerShipmentsPerPeriod;

		
		/*
		 * Translate shipment frequency into shipment period in integer days. Scale
		 * individual shipment size such that total shipment size is recovered.
		 */
//		final int shipmentPeriod_days = Math.max(1, Math.min(365, (int) Math.round(365.0 / this.getFrequency_1_yr())));
//		final double individualShipmentSize_ton = shipment.getTotalAmount_ton() * (shipmentPeriod_days / 365.0);

		/*
		 * Construct one shipment for each shipment period that completely fits into the
		 * analysis period.
		 */
		final List<Shipment> shipments = new ArrayList<>(integerShipmentsPerPeriod + 1);
		for (int i = 0; i < integerShipmentsPerPeriod; i++) {
			shipments.add(new Shipment(shipment.getCommmodity(), individualShipmentSize_ton, 1.0));
		}

		/*
		 * Consider the remaining (fractional, possibly zero) part of the shipment
		 * period that does not entirely fit into the analysis period. The probability
		 * that the shipment of that period falls into the analysis period is equal to
		 * the ratio of that fractional period over the shipment period.
		 */
//		final double additionalShipmentProba = ((double) (analysisPeriod_days % shipmentPeriod_days))
//				/ shipmentPeriod_days;
		shipments.add(new Shipment(shipment.getCommmodity(), individualShipmentSize_ton, fractionalShipmentsPerPeriod));

		return shipments;
	}

	// TESTING

	public static void main(String[] args) {
//		Random rnd = new Random();
//		for (int r = 0; r < 10000; r++) {
//			AnnualShipment recurrentShipment = new AnnualShipment(null, null, 100.0); // , rnd.nextDouble() *
//																							// 365.0);
//			int analysisPeriod_days = 1 + rnd.nextInt(365);
//			List<Shipment> shipments = disaggregate(recurrentShipment, analysisPeriod_days);
//			double realized = shipments.stream().mapToDouble(s -> s.getWeight_ton() * s.getProbability()).sum();
//			double target = recurrentShipment.getTotalAmount_ton() * (analysisPeriod_days / 365.0);
//			System.out.println((realized - target) / target);
//		}
	}

}
