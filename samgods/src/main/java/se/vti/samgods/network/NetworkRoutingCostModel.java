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

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.consolidation.EpisodeCostModel;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkRoutingCostModel {

	// -------------------- MEMBERS --------------------

	private final Network multimodalNetwork;

	private final Map<SamgodsConstants.TransportMode, Network> mode2unimodalNetwork;

	private final EpisodeCostModel empiricalEpisodeCostModel;

	private final EpisodeCostModel fallbackCostModel;

	private final Map<SamgodsConstants.TransportMode, Map<SamgodsConstants.Commodity, TravelDisutility>> mode2commodity2travelDisutility;

	private final Map<SamgodsConstants.TransportMode, Map<SamgodsConstants.Commodity, TravelTime>> mode2commodity2travelTime;

	// -------------------- CONSTRUCTION --------------------

	public NetworkRoutingCostModel(Network multiModalNetwork, EpisodeCostModel empiricalEpisodeCostModel,
			EpisodeCostModel fallbackCostModel) {
		this.multimodalNetwork = multiModalNetwork;
		this.mode2unimodalNetwork = new LinkedHashMap<>(SamgodsConstants.TransportMode.values().length);

		this.empiricalEpisodeCostModel = empiricalEpisodeCostModel;
		this.fallbackCostModel = fallbackCostModel;

		this.mode2commodity2travelDisutility = new LinkedHashMap<>(SamgodsConstants.TransportMode.values().length);
		this.mode2commodity2travelTime = new LinkedHashMap<>(SamgodsConstants.TransportMode.values().length);
		for (SamgodsConstants.TransportMode mode : SamgodsConstants.TransportMode.values()) {
			
			final Set<String> matsimModes = Collections
					.singleton(SamgodsConstants.samgodsMode2matsimMode.get(mode));
			final Network unimodalNetwork = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(unimodalNetwork).filter(unimodalNetwork, matsimModes);

			final Map<SamgodsConstants.Commodity, TravelDisutility> commodity2disutilities = new LinkedHashMap<>(
					SamgodsConstants.Commodity.values().length);
			final Map<SamgodsConstants.Commodity, TravelTime> commodity2travelTimes = new LinkedHashMap<>(
					SamgodsConstants.Commodity.values().length);
			this.mode2commodity2travelDisutility.put(mode, commodity2disutilities);
			this.mode2commodity2travelTime.put(mode, commodity2travelTimes);

			for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
				
				final Map<Link, Double> link2cost_1_ton = new LinkedHashMap<>();
				final Map<Link, Double> link2travelTime_s = new LinkedHashMap<>();
				
				for (Link link : unimodalNetwork.getLinks().values()) {
					
					
					
					
				}
				
				
				
				
				commodity2disutilities.put(commodity, new TravelDisutility() {
					@Override
					public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
						// TODO Auto-generated method stub
						return 0;
					}

					@Override
					public double getLinkMinimumTravelDisutility(Link link) {
						// TODO Auto-generated method stub
						return 0;
					}
				});
				commodity2travelTimes.put(commodity, new TravelTime() {
					@Override
					public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
						// TODO Auto-generated method stub
						return 0;
					}
				});
			}
		}
	}

	// -------------------- INTERNALS --------------------

	private Network getOrCreateUnimodalNetwork(SamgodsConstants.TransportMode samgodsMode) {
		if (this.mode2unimodalNetwork.containsKey(samgodsMode)) {
			return this.mode2unimodalNetwork.get(samgodsMode);
		} else {
			final Set<String> matsimModes = Collections
					.singleton(SamgodsConstants.samgodsMode2matsimMode.get(samgodsMode));
			final Network unimodalNetwork = NetworkUtils.createNetwork();
			new TransportModeNetworkFilter(unimodalNetwork).filter(unimodalNetwork, matsimModes);
			this.mode2unimodalNetwork.put(samgodsMode, unimodalNetwork);
			return unimodalNetwork;
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	TravelDisutility createLinkDisutility(SamgodsConstants.TransportMode mode, SamgodsConstants.Commodity commodity) {
		return null;
	}

	TravelTime createTravelTime(SamgodsConstants.TransportMode mode, SamgodsConstants.Commodity commodity) {
		return null;
	}

}
