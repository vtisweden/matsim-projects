/**
 * se.vti.samgods.network
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
package se.vti.samgods.network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Units;
import se.vti.samgods.BasicTransportCost;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.consolidation.EpisodeCostModel;
import se.vti.samgods.utils.CommodityModeGrouping;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkRoutingData {

	// -------------------- MEMBERS --------------------

	private final CommodityModeGrouping commodityModeGrouping;

	private final Map<SamgodsConstants.TransportMode, Network> mode2unimodalNetwork;

	private final Map<CommodityModeGrouping.Group, TravelDisutility> group2travelDisutility;

	private final Map<CommodityModeGrouping.Group, TravelTime> group2travelTime;

	// -------------------- CONSTRUCTION --------------------

	public NetworkRoutingData(Network multimodalNetwork, CommodityModeGrouping commodityModeGrouping,
			EpisodeCostModel empiricalEpisodeCostModel, EpisodeCostModel fallbackEpisodeCostModel) {
		
		this.commodityModeGrouping = commodityModeGrouping;

		/*
		 * (1) Create unimodal networks. TODO One instance per mode, not synchronized!
		 */

		final Set<SamgodsConstants.TransportMode> consideredModes = commodityModeGrouping.getAllSecond();
		this.mode2unimodalNetwork = new LinkedHashMap<>(consideredModes.size());
		for (SamgodsConstants.TransportMode samgodsMode : consideredModes) {
			final Set<String> matsimModes = Collections
					.singleton(SamgodsConstants.samgodsMode2matsimMode.get(samgodsMode));
			final Network unimodalNetwork = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(unimodalNetwork).filter(unimodalNetwork, matsimModes);
			this.mode2unimodalNetwork.put(samgodsMode, unimodalNetwork);
		}

		/*
		 * (2) Create travel disutilities and times. TODO One instance per group, not
		 * synchronized.
		 */

		this.group2travelDisutility = new LinkedHashMap<>(this.commodityModeGrouping.groupCnt());
		this.group2travelTime = new LinkedHashMap<>(this.commodityModeGrouping.groupCnt());

		for (CommodityModeGrouping.Group group : commodityModeGrouping.groupsView()) {

			final Map<Link, BasicTransportCost> link2empiricalCost = empiricalEpisodeCostModel
					.createLinkTransportCosts(group);
			final Map<Link, BasicTransportCost> link2fallbackCost = fallbackEpisodeCostModel
					.createLinkTransportCosts(group);

			this.group2travelDisutility.put(group, new TravelDisutility() {
				@Override
				public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
					return this.getLinkMinimumTravelDisutility(link); // TODO Refine?
				}

				@Override
				public double getLinkMinimumTravelDisutility(Link link) {
					return link2empiricalCost.getOrDefault(link, link2fallbackCost.get(link)).getMonetaryCost();
				}
			});
			this.group2travelTime.put(group, new TravelTime() {
				@Override
				public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
					return Units.S_PER_H
							* link2empiricalCost.getOrDefault(link, link2fallbackCost.get(link)).getDuration_h();
				}
			});
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	// TODO not synchronized
	public Network getNetwork(SamgodsConstants.TransportMode mode) {
		return this.mode2unimodalNetwork.get(mode);
	}

	// TODO not synchronized
	public TravelDisutility createDisutility(SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode) {
		return this.group2travelDisutility.get(this.commodityModeGrouping.getGroup(commodity, mode));
	}

	// TODO not synchronized
	public TravelTime createTravelTime(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode) {
		return this.group2travelTime.get(this.commodityModeGrouping.getGroup(commodity, mode));
	}
}
