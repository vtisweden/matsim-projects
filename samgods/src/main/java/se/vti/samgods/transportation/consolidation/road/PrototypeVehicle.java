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

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

/**
 * 
 * Represents one additional, not yet used, vehicle of a given type.
 * 
 * @author GunnarF
 *
 */
public class PrototypeVehicle implements Vehicle {

	private final VehicleType type;
	
	private final Id<Vehicle> id;
	
	public PrototypeVehicle(VehicleType type) {
		this.type = type;
		this.id = Id.create(type.getId(), Vehicle.class);
	}
	
	@Override
	public Id<Vehicle> getId() {
		return this.id;
	}

	@Override
	public VehicleType getType() {
		return this.type;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}


}
