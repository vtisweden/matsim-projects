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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.consolidation.road.PrototypeVehicle;

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

	public VehicleType createAndAddVehicleType(final String key, final SamgodsConstants.TransportMode transportMode,
			final double capacity_ton, final double speed_km_h) {

		final Id<VehicleType> typeId = Id.create(key, VehicleType.class);
		final VehicleType type = VehicleUtils.createVehicleType(typeId);
		type.setMaximumVelocity(Units.M_S_PER_KM_H * speed_km_h);

		final FreightVehicleTypeAttributes attributes = new FreightVehicleTypeAttributes(transportMode, capacity_ton);
		type.getAttributes().putAttribute(FreightVehicleTypeAttributes.ATTRIBUTE_NAME, attributes);

		this.vehicles.addVehicleType(type);
		return type;
	}

	public Vehicle createAndAddVehicle(VehicleType type) {
		assert (this.vehicles.getVehicleTypes().values().contains(type));
		final Vehicle vehicle = this.vehicles.getFactory().createVehicle(Id.create(this.vehCnt++, Vehicle.class), type);
		this.vehicles.addVehicle(vehicle);
		return vehicle;
	}

	// TODO Consolidation-specific. Move there once getters of VehicleFleet are
	// decided.
	public Map<VehicleType, Vehicle> createPrototypeVehicles() {
		final Map<VehicleType, Vehicle> type2veh = new LinkedHashMap<>(this.vehicles.getVehicleTypes().size());
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final Vehicle prototype = new PrototypeVehicle(type);
			type2veh.put(type, prototype);
		}
		return type2veh;
	}

}
