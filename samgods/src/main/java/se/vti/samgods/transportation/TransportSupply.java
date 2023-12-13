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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.network.Network;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.fleet.VehicleFleet;
import se.vti.samgods.transportation.pricing.TransportPrices;

/**
 * Packages the supply (carrier) side of the freight transport system.
 * 
 * @author GunnarF
 *
 */
public class TransportSupply {

	// -------------------- CONSTANTS --------------------

	public static final Map<SamgodsConstants.TransportMode, String> samgodsMode2matsimMode;

	static {
		samgodsMode2matsimMode = new ConcurrentHashMap<>(SamgodsConstants.TransportMode.values().length);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Road, org.matsim.api.core.v01.TransportMode.car);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Rail, org.matsim.api.core.v01.TransportMode.train);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Sea, org.matsim.api.core.v01.TransportMode.ship);
		samgodsMode2matsimMode.put(SamgodsConstants.TransportMode.Air, org.matsim.api.core.v01.TransportMode.airplane);
	}

	// -------------------- MEMBERS --------------------

	private final Network network;

	private final VehicleFleet vehicleFleet;

	private final TransportPrices transportPrices;

	// -------------------- CONSTRUCTION --------------------

	public TransportSupply(Network network, VehicleFleet fleet, TransportPrices transportPrices) {
		this.network = network;
		this.vehicleFleet = fleet;
		this.transportPrices = transportPrices;
	}

	// -------------------- GETTERS --------------------

	public Network getNetwork() {
		return this.network;
	}

	public VehicleFleet getVehicleFleet() {
		return this.vehicleFleet;
	}

	public TransportPrices getTransportPrices() {
		return this.transportPrices;
	}

}
