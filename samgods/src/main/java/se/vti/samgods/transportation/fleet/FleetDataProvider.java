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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetDataProvider {

	// -------------------- CONSTRUCTION --------------------

	public FleetDataProvider(Vehicles vehicles) {
		this.vehicleType2attributes = new ConcurrentHashMap<>(vehicles.getVehicleTypes().values().stream()
				.collect(Collectors.toMap(t -> t, t -> (SamgodsVehicleAttributes) t.getAttributes()
						.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME))));
		this.vehicleType2asc = new ConcurrentHashMap<>(
				vehicles.getVehicleTypes().values().stream().collect(Collectors.toMap(t -> t, t -> 0.0)));
	}

	public FleetData createFleetData() {
		return new FleetData(this);
	}

	// ---------- THREAD-SAFE LOCALLY CACHED SamgodsVehicleAttributes ----------

	private final ConcurrentMap<VehicleType, SamgodsVehicleAttributes> vehicleType2attributes;

	ConcurrentMap<VehicleType, SamgodsVehicleAttributes> getVehicleType2attributes() {
		return this.vehicleType2attributes;
	}

	// ---------- THREAD-SAFE LOCALLY CACHED COMPATIBLE VEHICLE TYPES ----------

	private final ConcurrentMap<Commodity, ConcurrentMap<TransportMode, ConcurrentMap<Boolean, ConcurrentMap<Boolean, List<VehicleType>>>>> commodity2transportMode2isContainer2isFerry2compatibleVehicleTypes = new ConcurrentHashMap<>();

	ConcurrentMap<Commodity, ConcurrentMap<TransportMode, ConcurrentMap<Boolean, ConcurrentMap<Boolean, List<VehicleType>>>>> getCommodity2transportMode2isContainer2isFerry2compatibleVehicleTypes() {
		return this.commodity2transportMode2isContainer2isFerry2compatibleVehicleTypes;
	}

	// ---------- THREAD-SAFE LOCALLY CACHED REPRESENTATIVE VEHICLE TYPES ----------

	private final ConcurrentMap<Commodity, ConcurrentMap<TransportMode, ConcurrentMap<Boolean, ConcurrentMap<Boolean, VehicleType>>>> commodity2transportMode2isContainer2isFerry2representativeVehicleType = new ConcurrentHashMap<>();

	ConcurrentMap<Commodity, ConcurrentMap<TransportMode, ConcurrentMap<Boolean, ConcurrentMap<Boolean, VehicleType>>>> getCommodity2transportMode2isContainer2isFerry2representativeVehicleType() {
		return this.commodity2transportMode2isContainer2isFerry2representativeVehicleType;
	}

	// ---------- THREAD-SAFE LOCALLY CACHED VEHICLE COST FACTORS ----------

	private ConcurrentMap<VehicleType, Double> vehicleType2asc;

	public void setVehicleType2asc(ConcurrentMap<VehicleType, Double> vehicleType2asc) {
		this.vehicleType2asc = vehicleType2asc;
	}

	ConcurrentMap<VehicleType, Double> getVehicleType2asc() {
		return this.vehicleType2asc;
	}

}
