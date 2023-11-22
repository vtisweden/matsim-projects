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
package se.vti.samgods.transportation;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.legacy.Samgods;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleFleet {

	// -------------------- MEMBERS --------------------

	private final Vehicles vehicles;

	// -------------------- CONSTRUCTION --------------------

	public VehicleFleet() {
		this.vehicles = VehicleUtils.createVehiclesContainer();
	}

	public void createVehicleType(final String key, final Samgods.TransportMode transportMode,
			final double capacity_ton) {

		final Id<VehicleType> typeId = Id.create(key, VehicleType.class);
		final VehicleType type = VehicleUtils.createVehicleType(typeId);
		this.vehicles.addVehicleType(type);

		final FreightVehicleAttributes freightAttributes = new FreightVehicleAttributes(transportMode, capacity_ton);
		type.getAttributes().putAttribute(FreightVehicleAttributes.ATTRIBUTE_NAME, freightAttributes);
	}
	
	// -------------------- IMPLEMENTATION --------------------

	public FreightVehicleAttributes getFreightVehicleAttributes(final Id<VehicleType> typeId) {
		return (FreightVehicleAttributes) this.vehicles.getVehicleTypes().get(typeId).getAttributes()
				.getAttribute(FreightVehicleAttributes.ATTRIBUTE_NAME);
	}

}
