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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportCost;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.FreightVehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class FallbackEpisodeCostModel implements EpisodeCostModel {

	private final ConsolidationCostModel consolidationCostModel;

	private double capacityUsageFactor = 0.6;

	private final Map<TransportMode, FreightVehicleAttributes> mode2representativeContainerVehicleAttributes = new LinkedHashMap<>();
	private final Map<TransportMode, FreightVehicleAttributes> mode2representativeNoContainerVehicleAttributes = new LinkedHashMap<>();

	public FallbackEpisodeCostModel(FreightVehicleFleet fleet, ConsolidationCostModel consolidationCostModel) {
		this.consolidationCostModel = consolidationCostModel;
		for (TransportMode mode : TransportMode.values()) {
			this.mode2representativeContainerVehicleAttributes.put(mode,
					fleet.getRepresentativeVehicleAttributes(mode, true, a -> a.capacity_ton));
			this.mode2representativeNoContainerVehicleAttributes.put(mode,
					fleet.getRepresentativeVehicleAttributes(mode, false, a -> a.capacity_ton));
		}
	}

	@Override
	public TransportCost computeCost_1_ton(TransportEpisode episode) {
		final FreightVehicleAttributes vehicleAttributes;
		if (episode.isContainer()) {
			vehicleAttributes = this.mode2representativeContainerVehicleAttributes.get(episode.getMode());
		} else {
			vehicleAttributes = this.mode2representativeNoContainerVehicleAttributes.get(episode.getMode());
		}
		final TransportCost representativeVehicleCost = this.consolidationCostModel.getVehicleCost(vehicleAttributes,
				this.capacityUsageFactor * vehicleAttributes.capacity_ton, episode);
		return new TransportCost(1.0, representativeVehicleCost.monetaryCost / representativeVehicleCost.amount_ton,
				representativeVehicleCost.duration_h);
	}
}
