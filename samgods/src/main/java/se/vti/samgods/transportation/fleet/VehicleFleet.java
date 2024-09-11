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

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleFleet {

	// -------------------- MEMBERS --------------------

	private final Vehicles vehicles;

	private long vehCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	public VehicleFleet() {
		this.vehicles = VehicleUtils.createVehiclesContainer();
	}

	// -------------------- IMPLEMENTATION --------------------

	public Vehicle createAndAddVehicle(VehicleType type) {
		assert (this.vehicles.getVehicleTypes().values().contains(type));
		final Vehicle vehicle = this.vehicles.getFactory().createVehicle(Id.create(this.vehCnt++, Vehicle.class), type);
		this.vehicles.addVehicle(vehicle);
		return vehicle;
	}

	public Vehicles getVehicles() {
		return this.vehicles;
	}

	// -------------------- COMPATIBLE VEHICLE TYPES --------------------

//	public synchronized List<VehicleType> getCompatibleVehicleTypes(SamgodsConstants.Commodity commodity,
//			SamgodsConstants.TransportMode mode, boolean isContainer, boolean containsFerry) {
//		final List<VehicleType> result = new ArrayList<>(this.vehicles.getVehicleTypes().size());
//		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
//			FreightVehicleAttributes attrs = FreightVehicleAttributes.getFreightAttributesSynchronized(type);
//			if (attrs.mode.equals(mode) && (attrs.isContainer == isContainer) && attrs.isCompatible(commodity)
//					&& (!containsFerry || attrs.isFerryCompatible())) {
//				result.add(type);
//			}
//		}
//		return result;
//	}

	// -------------------- REPRESENTATIVE VEHICLE TYPES --------------------

//	public static synchronized VehicleType getRepresentativeVehicleType(List<VehicleType> compatibleTypes)
//			throws InsufficientDataException {
//		VehicleType result = null;
////		final List<VehicleType> compatibleTypes = this.getCompatibleVehicleTypes(commodity, mode, isContainer,
////				containsFerry);
//		if (compatibleTypes.size() > 0) {
//			final double meanCapacity_ton = compatibleTypes.stream()
//					.mapToDouble(t -> FreightVehicleAttributes.getFreightAttributesSynchronized(t).capacity_ton)
//					.average().getAsDouble();
//			double resultDeviation_ton = Double.POSITIVE_INFINITY;
//			for (VehicleType candidate : compatibleTypes) {
//				final double candidateDeviation_ton = Math
//						.abs(FreightVehicleAttributes.getFreightAttributesSynchronized(candidate).capacity_ton
//								- meanCapacity_ton);
//				if (candidateDeviation_ton < resultDeviation_ton) {
//					result = candidate;
//					resultDeviation_ton = candidateDeviation_ton;
//				}
//			}
//		} else {
//			throw new InsufficientDataException(null, "No representative vehicle type.", null, null,
//					null, null, null);
//		}
//		return result;
//	}

//	public FreightVehicleAttributes getRepresentativeVehicleAttributes(SamgodsConstants.Commodity commodity,
//			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry)
//			throws InsufficientDataException {
//		return FreightVehicleAttributes
//				.getFreightAttributesSynchronized(this.getRepresentativeVehicleType(commodity, mode, isContainer, containsFerry));
//	}

//	public FreightVehicleAttributes getRepresentativeVehicleAttributes(TransportEpisode episode)
//			throws InsufficientDataException {
//		
//		FreightVehicleAttributes.getFreightAttributesSynchronized(assignedVehicle.getType()).capacity_ton
//		
//		return this.getRepresentativeVehicleAttributes(episode.getCommodity(), episode.getMode(), episode.isContainer(),
//				episode.getConsolidationUnits().stream().anyMatch(cu -> cu.containsFerry));
//	}

	// -------------------- SUMMARY TABLES --------------------

	private String null2notAvail(Object c) {
		if (c == null) {
			return "N/A";
		} else {
			return c.toString();
		}
	}

	public String createVehicleTypeTable(SamgodsConstants.TransportMode mode) {
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Description", "Mode", "Cost[1/km]", "Cost[1/h]", "Capacity[ton]", "FerryCost[1/km]",
				"FerryCost[1/h]", "MaxSpeed[km/h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleAttributes attrs = FreightVehicleAttributes.getFreightAttributesSynchronized(type);
			if (mode.equals(attrs.samgodsMode)) {
				table.addRule();
				table.addRow(attrs.id, type.getDescription(), attrs.samgodsMode, attrs.cost_1_km, attrs.cost_1_h,
						attrs.capacity_ton, this.null2notAvail(attrs.onFerryCost_1_km),
						this.null2notAvail(attrs.onFerryCost_1_h), this.null2notAvail(attrs.speed_km_h));
			}
		}
		table.addRule();
		return table.render();
	}

	public String createVehicleTransferCostTable(SamgodsConstants.TransportMode mode) {
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Commodity", "LoadCost[1/ton]", "LoadTime[h]", "TransferCost[1/ton]",
				"TransferTime[h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleAttributes attrs = FreightVehicleAttributes.getFreightAttributesSynchronized(type);
			if (mode.equals(attrs.samgodsMode)) {
				for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
					if (attrs.isCompatible(commodity)) {
						table.addRule();
						table.addRow(attrs.id, commodity, attrs.loadCost_1_ton.get(commodity),
								attrs.loadTime_h.get(commodity), attrs.transferCost_1_ton.get(commodity),
								attrs.transferTime_h.get(commodity));
					}
				}
			}
		}
		table.addRule();
		return table.render();
	}
}
