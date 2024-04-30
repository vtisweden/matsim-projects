/**
 * se.vti.samgods.transportation.ntmcalc
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
package se.vti.samgods.transportation.ntmcalc;

import org.matsim.vehicles.Vehicle;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import se.vti.samgods.logistics.TransportEpisode;

/**
 * 
 * @author GunnarF
 *
 */
@JsonSerialize(using = VehicleEpisode2NTMCalcSerializer.class)
public class VehicleEpisode {

	private final Vehicle vehicle;
	
	private final double load_ton;

	private final TransportEpisode transportEpisode;
		
	public VehicleEpisode(Vehicle vehicle, double load_ton, TransportEpisode episode) {
		this.vehicle = vehicle;
		this.load_ton = load_ton;
		this.transportEpisode = episode;
	}
	
	public Vehicle getVehicle() {
		return this.vehicle;
	}
	
	public double getLoad_ton() {
		return this.load_ton;
	}
	
	public TransportEpisode getTransportEpisode() {
		return this.transportEpisode;
	}

}
