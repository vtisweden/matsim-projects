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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
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

	private final Network multimodalNetwork;

	private final CommodityModeGrouping commodityModeGrouping;

	private final EpisodeCostModel empiricalEpisodeCostModel;

	private final EpisodeCostModel fallbackEpisodeCostModel;

	// -------------------- CONSTRUCTION --------------------

	public NetworkRoutingData(Network multimodalNetwork, CommodityModeGrouping commodityModeGrouping,
			EpisodeCostModel empiricalEpisodeCostModel, EpisodeCostModel fallbackEpisodeCostModel) {
		this.multimodalNetwork = multimodalNetwork;
		this.commodityModeGrouping = commodityModeGrouping;
		this.empiricalEpisodeCostModel = empiricalEpisodeCostModel;
		this.fallbackEpisodeCostModel = fallbackEpisodeCostModel;
	}

	// -------------------- IMPLEMENTATION --------------------

	public Network createNetwork(SamgodsConstants.TransportMode mode) {
		final Set<String> matsimModes = Collections.singleton(SamgodsConstants.samgodsMode2matsimMode.get(mode));
		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork, matsimModes);
		return unimodalNetwork;
	}

	public TravelDisutility createDisutility(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode,
			Network network, boolean container) {
		final CommodityModeGrouping.Group group = this.commodityModeGrouping.getGroup(commodity, mode);
		final Map<Link, Double> link2disutility = this.empiricalEpisodeCostModel
				.createLinkTransportCosts(group, network, container).entrySet().stream().collect(
						Collectors.toMap(e -> network.getLinks().get(e.getKey()), e -> e.getValue().getMonetaryCost()));
		if (link2disutility.size() < network.getLinks().size()) {
			final Map<Id<Link>, BasicTransportCost> link2fallbackDisutility = this.fallbackEpisodeCostModel
					.createLinkTransportCosts(group, network, container);
			for (Link link : network.getLinks().values()) {
				if (!link2disutility.containsKey(link)) {
					link2disutility.put(link, link2fallbackDisutility.get(link).getMonetaryCost());
				}
			}
		}
		return new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return this.getLinkMinimumTravelDisutility(link); // TODO Refine?
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link2disutility.get(link);
			}
		};
	}

	public TravelTime createTravelTime(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode,
			Network network, boolean container) {
		final CommodityModeGrouping.Group group = this.commodityModeGrouping.getGroup(commodity, mode);
		final Map<Link, Double> link2tt = this.fallbackEpisodeCostModel.createLinkTransportCosts(group, network, container)
				.entrySet().stream().collect(Collectors.toMap(e -> network.getLinks().get(e.getKey()),
						e -> Units.S_PER_H * e.getValue().getDuration_h()));
		return new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link2tt.get(link);
			}
		};
	}
}
