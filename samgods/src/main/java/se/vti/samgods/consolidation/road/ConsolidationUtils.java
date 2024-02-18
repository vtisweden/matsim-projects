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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.logistics.RecurrentShipment;
import se.vti.samgods.transportation.fleet.FreightVehicleTypeAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationUtils {

	public static double getCapacity_ton(Vehicle vehicle) {
		return ((FreightVehicleTypeAttributes) vehicle.getAttributes()
				.getAttribute(FreightVehicleTypeAttributes.ATTRIBUTE_NAME)).getCapacity_ton();
	}

	public static List<Shipment> disaggregate(RecurrentShipment recurrentShipment, int analysisPeriod_days) {

		/*
		 * Translate shipment frequency into shipment period in integer days. Scale
		 * individual shipment size such that total shipment size is recovered.
		 */
		final int shipmentPeriod_days = Math.max(1,
				Math.min(365, (int) Math.round(365.0 / recurrentShipment.getFrequency_1_yr())));
		final double individualShipmentSize_ton = recurrentShipment.getSize_ton() * (shipmentPeriod_days / 365.0);

		/*
		 * Construct one shipment for each shipment period that completely fits into the
		 * analysis period.
		 */
		final int numberOfCertainShipments = analysisPeriod_days / shipmentPeriod_days;
		final List<Shipment> shipments = new ArrayList<>(numberOfCertainShipments + 1);
		for (int i = 0; i < numberOfCertainShipments; i++) {
			shipments.add(new Shipment(individualShipmentSize_ton, recurrentShipment.getCommmodity(), 1.0));
		}

		/*
		 * Consider the remaining (fractional, possibly zero) part of the shipment
		 * period that does not entirely fit into the analysis period. The probability
		 * that the shipment of that period falls into the analysis period is equal to
		 * the ratio of that fractional period over the shipment period.
		 */
		final double additionalShipmentProba = ((double) (analysisPeriod_days % shipmentPeriod_days))
				/ shipmentPeriod_days;
		shipments.add(new Shipment(individualShipmentSize_ton, recurrentShipment.getCommmodity(),
				additionalShipmentProba));

		return shipments;
	}

	public static void main(String[] args) {
		Random rnd = new Random();
		for (int r = 0; r < 10000; r++) {
			RecurrentShipment recurrentShipment = new RecurrentShipment(null, null, 100.0, rnd.nextDouble() * 365.0);
			int analysisPeriod_days = 1 + rnd.nextInt(365);
			List<Shipment> shipments = disaggregate(recurrentShipment, analysisPeriod_days);
			double realized = shipments.stream().mapToDouble(s -> s.getWeight_ton() * s.getProbability()).sum();
			double target = recurrentShipment.getSize_ton() * (analysisPeriod_days / 365.0);
			System.out.println((realized - target) / target);
		}
	}

}
