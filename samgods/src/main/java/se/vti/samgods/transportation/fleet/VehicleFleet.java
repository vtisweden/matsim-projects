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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;

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

	public FreightVehicleAttributes createVehicleType(final String key, final SamgodsConstants.TransportMode transportMode,
			final double capacity_ton, final double speed_km_h) {

		final Id<VehicleType> typeId = Id.create(key, VehicleType.class);
		final VehicleType type = VehicleUtils.createVehicleType(typeId);
		type.setMaximumVelocity(Units.M_S_PER_KM_H * speed_km_h);
		this.vehicles.addVehicleType(type);

		final FreightVehicleAttributes attributes = new FreightVehicleAttributes(transportMode, capacity_ton);
		type.getAttributes().putAttribute(FreightVehicleAttributes.ATTRIBUTE_NAME, attributes);
		return attributes;
	}

	// -------------------- IMPLEMENTATION --------------------

	public FreightVehicleAttributes getFreightVehicleAttributes(final Id<VehicleType> typeId) {
		return (FreightVehicleAttributes) this.vehicles.getVehicleTypes().get(typeId).getAttributes()
				.getAttribute(FreightVehicleAttributes.ATTRIBUTE_NAME);
	}

	public FreightVehicleAttributes getFreightVehicleAttributes(final String key) {
		return this.getFreightVehicleAttributes(Id.create(key, VehicleType.class));
	}

	public TravelDisutility createEmptyVehicleTravelDisutility(final String vehicleKey) {
		final Id<VehicleType> vehicleTypeId = Id.create(vehicleKey, VehicleType.class);
		final VehicleType vehicleType = this.vehicles.getVehicleTypes().get(vehicleTypeId);

		return new TravelDisutility() {
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				final double length_km = Units.KM_PER_M * link.getLength();
				final double speed_km_h = Units.KM_H_PER_M_S
						* Math.min(link.getFreespeed(), vehicleType.getMaximumVelocity());
				final double traveltime_h = length_km / speed_km_h;
				final FreightVehicleAttributes attrs = getFreightVehicleAttributes(vehicleTypeId);
				return attrs.getFixedCost_1_km() * length_km + attrs.getFixedCost_1_h() * traveltime_h;
			}

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return this.getLinkMinimumTravelDisutility(link);
			}
		};
	}

}
