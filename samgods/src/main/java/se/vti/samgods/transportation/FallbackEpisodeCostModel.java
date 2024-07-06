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
package se.vti.samgods.transportation;

import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.LinkAttributes;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;
import se.vti.samgods.transportation.consolidation.road.ConsolidationUtils;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class FallbackEpisodeCostModel implements EpisodeCostModel {

	// -------------------- CONSTANTS --------------------

//	private static final Logger log = Logger.getLogger(FallbackEpisodeCostModel.class);

	// -------------------- MEMBERS --------------------

	private final VehicleFleet fleet;
	private final ConsolidationCostModel consolidationCostModel;

	private double capacityUsageFactor = 0.7;

//	private final Map<SamgodsConstants.TransportMode, SamgodsVehicleAttributes> mode2representativeContainerVehicleAttributes = new LinkedHashMap<>();
//	private final Map<SamgodsConstants.TransportMode, SamgodsVehicleAttributes> mode2representativeNoContainerVehicleAttributes = new LinkedHashMap<>();
//
//	private final Map<CommodityModeGrouping.Group, SamgodsVehicleAttributes> group2representativeContainerVehicleAttributes = new LinkedHashMap<>();
//	private final Map<CommodityModeGrouping.Group, SamgodsVehicleAttributes> group2representativeNoContainerVehicleAttributes = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public FallbackEpisodeCostModel(VehicleFleet fleet, ConsolidationCostModel consolidationCostModel) {
		this.fleet = fleet;
		this.consolidationCostModel = consolidationCostModel;
	}

	public FallbackEpisodeCostModel setCapacityUsageFactor(double factor) {
		this.capacityUsageFactor = factor;
		return this;
	}

	// -------------------- IMPLEMENTATION OF EpisodeCostModel --------------------

	@Override
	public DetailedTransportCost computeUnitCost(TransportEpisode episode) throws InsufficientDataException {
		final VehicleType vehicleType = this.fleet.getRepresentativeVehicleType(episode);
		final SamgodsVehicleAttributes vehicleAttributes = ConsolidationUtils.getFreightAttributes(vehicleType);
		return this.consolidationCostModel.computeEpisodeCost(vehicleAttributes,
				this.capacityUsageFactor * vehicleAttributes.capacity_ton, episode);
	}

	@Override
	public void populateLink2transportCosts(Map<Link, BasicTransportCost> link2cost,
			SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, Boolean isContainer,
			Network network) throws InsufficientDataException {

		final SamgodsVehicleAttributes vehicleAttributes = ConsolidationUtils
				.getFreightAttributes(this.fleet.getRepresentativeVehicleType(commodity, mode, isContainer, false));

		SamgodsVehicleAttributes ferryCompatibleVehicleAttributes;
		try {
			ferryCompatibleVehicleAttributes = ConsolidationUtils
					.getFreightAttributes(this.fleet.getRepresentativeVehicleType(commodity, mode, isContainer, true));
		} catch (InsufficientDataException e) {
			ferryCompatibleVehicleAttributes = vehicleAttributes;
		}

		for (Link link : network.getLinks().values()) {
			if (!link2cost.containsKey(link)) {
				final double length_km = Units.KM_PER_M * link.getLength();
				final double duration_h = Units.H_PER_S * vehicleAttributes.travelTimeOnLink_s(link);
				assert (Double.isFinite(length_km));
				assert (Double.isFinite(duration_h));
				assert (link.getId() != null);
				if (LinkAttributes.isFerry(link)) {
					link2cost.put(link,
							new BasicTransportCost(1.0,
									duration_h * ferryCompatibleVehicleAttributes.onFerryCost_1_h
											+ length_km * ferryCompatibleVehicleAttributes.onFerryCost_1_km,
									duration_h));
				} else {
					link2cost.put(link,
							new BasicTransportCost(1.0,
									duration_h * vehicleAttributes.cost_1_h + length_km * vehicleAttributes.cost_1_km,
									duration_h));
				}
			}
		}
	}
}
