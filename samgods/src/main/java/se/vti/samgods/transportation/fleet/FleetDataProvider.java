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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.calibration.ascs.ASCs;
import se.vti.samgods.network.SamgodsLinkAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetDataProvider {

	// -------------------- CONSTRUCTION --------------------

	public FleetDataProvider(Network network, Vehicles vehicles) {
		this.vehicleType2attributes = new ConcurrentHashMap<>(vehicles.getVehicleTypes().values().stream()
				.collect(Collectors.toMap(t -> t, t -> (SamgodsVehicleAttributes) t.getAttributes()
						.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME))));
		this.vehicleType2asc = new ConcurrentHashMap<>(
				vehicles.getVehicleTypes().values().stream().collect(Collectors.toMap(t -> t, t -> 0.0)));

		final Map<String, Set<VehicleType>> networkMode2vehicleTypes = new LinkedHashMap<>();
		for (VehicleType type : vehicles.getVehicleTypes().values()) {
			final SamgodsVehicleAttributes vehicleAttributes = (SamgodsVehicleAttributes) type.getAttributes()
					.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
			for (String networkMode : vehicleAttributes.networkModes) {
				networkMode2vehicleTypes.computeIfAbsent(networkMode, m -> new LinkedHashSet<>()).add(type);
			}
		}
		this.linkId2allowedVehicleTypes = new ConcurrentHashMap<>();
		for (Link link : network.getLinks().values()) {
			final SamgodsLinkAttributes linkAttributes = (SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			final Set<VehicleType> allowedTypes = new LinkedHashSet<>();
			for (String networkMode : linkAttributes.networkModes) {
				if (networkMode2vehicleTypes.containsKey(networkMode)) {
					allowedTypes.addAll(networkMode2vehicleTypes.get(networkMode));
				}
			}
			this.linkId2allowedVehicleTypes.put(link.getId(), new CopyOnWriteArraySet<>(allowedTypes));
		}
	}

	public FleetData createFleetData() {
		return new FleetData(this);
	}

	// ----- THREAD-SAFE LOCALLY CACHED ALLOWED VEHICLE TYPES PER LINK -----

	private final ConcurrentMap<Id<Link>, CopyOnWriteArraySet<VehicleType>> linkId2allowedVehicleTypes;

	ConcurrentMap<Id<Link>, CopyOnWriteArraySet<VehicleType>> getLinkId2allowedVehicleTypes() {
		return this.linkId2allowedVehicleTypes;
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

	private ConcurrentMap<VehicleType, Double> vehicleType2asc = new ConcurrentHashMap<>();
	private ConcurrentMap<TransportMode, Double> mode2asc = new ConcurrentHashMap<>();
	private ConcurrentMap<Commodity, Double> railCommodity2asc = new ConcurrentHashMap<>();

	public void updateASCs(ASCs ascs) {
		this.vehicleType2asc = new ConcurrentHashMap<>(ascs.getVehicleTyp2ASC());
		this.mode2asc = new ConcurrentHashMap<>(ascs.getMode2ASC());
		this.railCommodity2asc = new ConcurrentHashMap<>(ascs.getRailCommodity2ASC());
	}

	ConcurrentMap<VehicleType, Double> getVehicleType2asc() {
		return this.vehicleType2asc;
	}

	ConcurrentMap<TransportMode, Double> getMode2asc() {
		return this.mode2asc;
	}

	ConcurrentMap<Commodity, Double> getRailCommodity2asc() {
		return this.railCommodity2asc;
	}

}
