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

	public static List<Shipment> disaggregateIntoAnalysisPeriod(AnnualShipment shipment, int analysisPeriod_days,
			SizeClass sizeClass) {

		final double amountPerPeriod_ton = shipment.getTotalAmount_ton() * analysisPeriod_days / 365.0;
		final double shipmentsPerPeriod = amountPerPeriod_ton / sizeClass.getMeanValue_ton();
		final double singleShipmentSize_ton = amountPerPeriod_ton / Math.ceil(shipmentsPerPeriod);

		final int completeShipmentsPerPeriod = (int) shipmentsPerPeriod;
		final double fractionalShipmentsPerPeriod = shipmentsPerPeriod - completeShipmentsPerPeriod;

		final List<Shipment> shipments = new ArrayList<>(
				completeShipmentsPerPeriod + (fractionalShipmentsPerPeriod > 0 ? 1 : 0));
		for (int i = 0; i < completeShipmentsPerPeriod; i++) {
			shipments.add(new Shipment(shipment.getCommmodity(), singleShipmentSize_ton, 1.0));
		}
		if (fractionalShipmentsPerPeriod > 0) {
			shipments.add(
					new Shipment(shipment.getCommmodity(), singleShipmentSize_ton, fractionalShipmentsPerPeriod));
		}

		return shipments;
	}

// TESTING
//
//	public static void main(String[] args) {
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
//	}

}
