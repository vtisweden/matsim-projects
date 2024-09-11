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
public class SamgodsVehicles {

	// -------------------- MEMBERS --------------------

	private final Vehicles vehicles;

	private long vehCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	public SamgodsVehicles() {
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

}
