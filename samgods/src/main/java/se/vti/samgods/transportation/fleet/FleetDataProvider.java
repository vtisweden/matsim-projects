/**
 * se.vti.samgods.transportation.fleet
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
package se.vti.samgods.transportation.fleet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetDataProvider {

	// populated upon construction
	private final ConcurrentMap<VehicleType, FreightVehicleAttributes> vehicleType2attributes;

	// lazily populated
	private final ConcurrentMap<Commodity, Map<TransportMode, Map<Boolean, Map<Boolean, VehicleType>>>> commodity2transportMode2isContainer2isFerry2representativeVehicleType = new ConcurrentHashMap<>();

	// lazily populated
	private final ConcurrentMap<Commodity, Map<TransportMode, Map<Boolean, Map<Boolean, List<VehicleType>>>>> commodity2transportMode2isContainer2isFerry2representativeVehicleTypes = new ConcurrentHashMap<>();


	public FleetDataProvider(VehicleFleet fleet) {
		this.vehicleType2attributes = new ConcurrentHashMap<>(fleet.getVehicles().getVehicleTypes().values().stream()
				.collect(Collectors.toMap(t -> t, t -> (FreightVehicleAttributes) t.getAttributes()
						.getAttribute(FreightVehicleAttributes.ATTRIBUTE_NAME))));
	}
	

	public FleetData createFleetData() {
		return new FleetData(this);
	}
	
	private synchronized List<VehicleType> createCompatibleVehicleTypes(Commodity commodity, TransportMode mode,
			boolean isContainer, boolean containsFerry) {

		final List<VehicleType> result = new ArrayList<>(this.vehicleType2attributes.size());
		for (Map.Entry<VehicleType, FreightVehicleAttributes> e : this.vehicleType2attributes.entrySet()) {
			VehicleType type = e.getKey();
			FreightVehicleAttributes attrs = e.getValue();
			if (attrs.samgodsMode.equals(mode) && (attrs.isContainer == isContainer) && attrs.isCompatible(commodity)
					&& (!containsFerry || attrs.isFerryCompatible())) {
				result.add(type);
			}
		}
		return result;
	}
	
	private synchronized VehicleType createRepresentativeVehicleType(Commodity commodity, TransportMode mode,
			boolean isContainer, boolean containsFerry) {
		final List<VehicleType> compatibleTypes = this.createCompatibleVehicleTypes(commodity, mode, isContainer,
				containsFerry);
		if (compatibleTypes.size() > 0) {
			VehicleType result = null;
			final double meanCapacity_ton = compatibleTypes.stream()
					.mapToDouble(t -> FreightVehicleAttributes.getFreightAttributesSynchronized(t).capacity_ton)
					.average().getAsDouble();
			double resultDeviation_ton = Double.POSITIVE_INFINITY;
			for (VehicleType candidate : compatibleTypes) {
				final double candidateDeviation_ton = Math
						.abs(FreightVehicleAttributes.getFreightAttributesSynchronized(candidate).capacity_ton
								- meanCapacity_ton);
				if (candidateDeviation_ton < resultDeviation_ton) {
					result = candidate;
					resultDeviation_ton = candidateDeviation_ton;
				}
			}
			return result;
		} else {
			return null;
		}
	}

	// may be null!
	VehicleType getRepresentativeVehicleType(final Commodity commodity, final TransportMode mode,
			final boolean isContainer, final boolean containsFerry) throws InsufficientDataException {
		return this.commodity2transportMode2isContainer2isFerry2representativeVehicleType
				.computeIfAbsent(commodity, c -> new ConcurrentHashMap<>())
				.computeIfAbsent(mode, m -> new ConcurrentHashMap<>())
				.computeIfAbsent(isContainer, ic -> new ConcurrentHashMap<>()).computeIfAbsent(containsFerry,
						cf -> this.createRepresentativeVehicleType(commodity, mode, isContainer, containsFerry));
	}

	List<VehicleType> getCompatibleVehicleTypes(final Commodity commodity, final TransportMode mode,
			final boolean isContainer, final boolean isFerry) {
		return this.commodity2transportMode2isContainer2isFerry2representativeVehicleTypes
				.computeIfAbsent(commodity, c -> new ConcurrentHashMap<>())
				.computeIfAbsent(mode, m -> new ConcurrentHashMap<>())
				.computeIfAbsent(isContainer, ic -> new ConcurrentHashMap<>())
				.computeIfAbsent(isFerry, f -> this.createCompatibleVehicleTypes(commodity, mode, isContainer, f));
	}



}
