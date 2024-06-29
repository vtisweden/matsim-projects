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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import floetteroed.utilities.Units;
import se.vti.samgods.BasicTransportCost;
import se.vti.samgods.DetailedTransportCost;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.FreightVehicleFleet;
import se.vti.samgods.utils.CommodityModeGrouping;
import se.vti.samgods.utils.TupleGrouping;

/**
 * 
 * @author GunnarF
 *
 */
public class FallbackEpisodeCostModel implements EpisodeCostModel {

	private final ConsolidationCostModel consolidationCostModel;

	private double capacityUsageFactor = 0.6;

	private final Map<SamgodsConstants.TransportMode, FreightVehicleAttributes> mode2representativeContainerVehicleAttributes = new LinkedHashMap<>();
	private final Map<SamgodsConstants.TransportMode, FreightVehicleAttributes> mode2representativeNoContainerVehicleAttributes = new LinkedHashMap<>();

	private final Map<CommodityModeGrouping.Group, FreightVehicleAttributes> group2representativeContainerVehicleAttributes = new LinkedHashMap<>();
	private final Map<CommodityModeGrouping.Group, FreightVehicleAttributes> group2representativeNoContainerVehicleAttributes = new LinkedHashMap<>();

	public FallbackEpisodeCostModel(FreightVehicleFleet fleet, ConsolidationCostModel consolidationCostModel,
			CommodityModeGrouping commodityModeGrouping) {
		this.consolidationCostModel = consolidationCostModel;
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			this.mode2representativeContainerVehicleAttributes.put(mode,
					fleet.getRepresentativeVehicleAttributes(mode, true, a -> a.capacity_ton));
			this.mode2representativeNoContainerVehicleAttributes.put(mode,
					fleet.getRepresentativeVehicleAttributes(mode, false, a -> a.capacity_ton));
		}
		for (CommodityModeGrouping.Group group : commodityModeGrouping.groupsView()) {
			this.group2representativeContainerVehicleAttributes.put(group,
					fleet.getRepresentativeVehicleAttributes(group.getAllSecondView(), true, a -> a.capacity_ton));
			this.group2representativeNoContainerVehicleAttributes.put(group,
					fleet.getRepresentativeVehicleAttributes(group.getAllSecondView(), false, a -> a.capacity_ton));
		}
	}

	@Override
	public DetailedTransportCost computeCost_1_ton(TransportEpisode episode) {
		final FreightVehicleAttributes vehicleAttributes;
		if (episode.isContainer()) {
			vehicleAttributes = this.mode2representativeContainerVehicleAttributes.get(episode.getMode());
		} else {
			vehicleAttributes = this.mode2representativeNoContainerVehicleAttributes.get(episode.getMode());
		}
		return this.consolidationCostModel.getVehicleCost(vehicleAttributes,
				this.capacityUsageFactor * vehicleAttributes.capacity_ton, episode);
	}

	@Override
	public Map<Id<Link>, BasicTransportCost> createLinkTransportCosts(
			TupleGrouping<SamgodsConstants.Commodity, SamgodsConstants.TransportMode>.Group group, Network network,
			boolean container) {
		final FreightVehicleAttributes vehicleAttributes;
		if (container) {
			vehicleAttributes = this.group2representativeContainerVehicleAttributes.get(group);
		} else {
			vehicleAttributes = this.group2representativeNoContainerVehicleAttributes.get(group);
		}

		Map<Id<Link>, BasicTransportCost> link2costs = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			// TODO identify ferry links!
			final double length_km = Units.KM_PER_M * link.getLength();
			final double duration_h = Units.H_PER_S * link.getFreespeed() / link.getLength();
			link2costs.put(link.getId(), new BasicTransportCost(1.0,
					duration_h * vehicleAttributes.cost_1_h + length_km * vehicleAttributes.cost_1_km, duration_h));
		}
		return link2costs;
	}
}
