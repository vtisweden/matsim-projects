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

import static se.vti.samgods.SamgodsConstants.TransportMode.Air;
import static se.vti.samgods.SamgodsConstants.TransportMode.Rail;
import static se.vti.samgods.SamgodsConstants.TransportMode.Road;
import static se.vti.samgods.SamgodsConstants.TransportMode.Sea;

import java.util.LinkedHashMap;
import java.util.Map;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.fleet.FreightVehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class FallbackEpisodeCostModel implements EpisodeCostModel {

	private final Map<SamgodsConstants.TransportMode, Double> mode2teleportationSpeed_km_h = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, Double> mode2transshipmentDuration_h = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, Double> mode2transferDuration_h = new LinkedHashMap<>();

	private final Map<SamgodsConstants.TransportMode, Double> mode2transshipmentCost_1_tonKm = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, Double> mode2transferCost_1_ton = new LinkedHashMap<>();

	public FallbackEpisodeCostModel(FreightVehicleFleet fleet) {
		final double teleportationFactor = 1.5;

		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			this.mode2transshipmentCost_1_tonKm.put(mode, teleportationFactor * fleet.computeClassMedianCost_1_tonKm(mode));
			this.mode2teleportationSpeed_km_h.put(mode, fleet.computeClassMedianVehicleSpeed_km_h(mode));
		}

		
		this.mode2transshipmentDuration_h.put(Air, teleportationFactor * 0.0);
		this.mode2transferDuration_h.put(Air, teleportationFactor * 0.0);
		this.mode2transferCost_1_ton.put(Air, teleportationFactor * 0.0);

		this.mode2transshipmentDuration_h.put(Rail, teleportationFactor * 0.0);
		this.mode2transferDuration_h.put(Rail, teleportationFactor * 0.0);
		this.mode2transferCost_1_ton.put(Rail, teleportationFactor * 0.0);

		this.mode2transshipmentDuration_h.put(Road, teleportationFactor * 0.0);
		this.mode2transferDuration_h.put(Road, teleportationFactor * 0.0);
		this.mode2transferCost_1_ton.put(Road, teleportationFactor * 0.0);

		this.mode2transshipmentDuration_h.put(Sea, teleportationFactor * 0.0);
		this.mode2transferDuration_h.put(Sea, teleportationFactor * 0.0);
		this.mode2transferCost_1_ton.put(Sea, teleportationFactor * 0.0);

	}

	@Override
	public Double computeMonetaryCost_1_ton(TransportEpisode episode) {
		throw new UnsupportedOperationException("TODO");
	}

	@Override
	public Double computeDuration_h(TransportEpisode episode) {
		throw new UnsupportedOperationException("TODO");
	}

}
