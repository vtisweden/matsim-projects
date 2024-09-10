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

import java.util.List;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetData {

	private final FleetDataProvider dataProvider;
	
	public FleetData(FleetDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}
	
	public VehicleType getRepresentativeVehicleType(Commodity commodity, TransportMode mode, boolean isContainer,
			boolean containsFerry) throws InsufficientDataException {
		return this.dataProvider.getRepresentativeVehicleType(commodity, mode, isContainer, containsFerry);
	}

	public List<VehicleType> getCompatibleVehicleTypes(Commodity commodity, TransportMode mode, boolean isContainer,
			boolean containsFerry) {
		return this.dataProvider.getCompatibleVehicleTypes(commodity, mode, isContainer, containsFerry);
	}


}
