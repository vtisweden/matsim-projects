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

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;

/**
 * 
 * @author GunnarF
 *
 */
public class FallbackEpisodeCostModel implements EpisodeCostModel {

	private final double teleportationFactor = 1.5;

	private final Map<SamgodsConstants.TransportMode, Double> mode2teleportationSpeed_km_h = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, Double> mode2transshipmentDuration_h = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, Double> mode2transferDuration_h = new LinkedHashMap<>();

	private final Map<SamgodsConstants.TransportMode, Double> mode2teleportationCost_1_ton = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, Double> mode2transshipmentCost_1_ton = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, Double> mode2transferCost_1_ton = new LinkedHashMap<>();

	public FallbackEpisodeCostModel() {
		// TODO Hardcoding here for testing. Should not affect result quality once
		// iterations are run.
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			this.mode2teleportationSpeed_km_h.put(mode, this.teleportationFactor * 0.0);
			this.mode2transshipmentDuration_h.put(mode, this.teleportationFactor * 0.0);
			this.mode2transferDuration_h.put(mode, this.teleportationFactor * 0.0);

			this.mode2teleportationCost_1_ton.put(mode, this.teleportationFactor * 0.0);
			this.mode2transshipmentCost_1_ton.put(mode, this.teleportationFactor * 0.0);
			this.mode2transferCost_1_ton.put(mode, this.teleportationFactor * 0.0);
		}
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
