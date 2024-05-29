/**
 * se.vti.samgods.transportation.consolidation
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
package se.vti.samgods.transportation.consolidation;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.vehicles.Vehicle;

import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.consolidation.road.ShipmentVehicleAssignment;

/**
 * TODO This is now about all commodities, which are defining members of
 * transport episodes. Consider splitting this up per commodity.
 * 
 * TODO This is for now specified in ignorance of the rail consolidation
 * formulation.
 * 
 * @author GunnarF
 *
 */
public class EpisodeCostAggregator {

	private final Map<TransportEpisode, Double> episode2costTimesTons = new LinkedHashMap<>();

	private final Map<TransportEpisode, Double> episode2tons = new LinkedHashMap<>();

	public EpisodeCostAggregator() {
	}

	public void add(ShipmentVehicleAssignment assignment) {
		double costTimesTons = 0.0;
		double tons = 0.0;
		
		for (Map.Entry<Vehicle, Double> entry : assignment.getVehicle2payload_ton().entrySet()) {

		};
		
	}

}
