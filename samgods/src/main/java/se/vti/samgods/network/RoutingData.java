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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.Units;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.BasicTransportCost;
import se.vti.samgods.transportation.EpisodeCostModel;

/**
 * 
 * @author GunnarF
 *
 */
public class RoutingData {

	// -------------------- MEMBERS --------------------

	private final Network multimodalNetwork;

	private final EpisodeCostModel episodeCostModel;

	private TravelDisutility mostRecentlyCreatedTravelDisutility = null;
	private TravelTime mostRecentyCreatedTravelTime = null;

	// -------------------- CONSTRUCTION --------------------

	public RoutingData(Network multimodalNetwork, EpisodeCostModel episodeCostModel) {
		this.multimodalNetwork = multimodalNetwork;
		this.episodeCostModel = episodeCostModel;
	}

	// -------------------- IMPLEMENTATION --------------------

	public Network getMultimodalNetwork() {
		return this.multimodalNetwork;
	}

	public Network createNetwork(SamgodsConstants.TransportMode mode) {
		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork, mode.matsimModes);
		new NetworkCleaner().run(unimodalNetwork);
		return unimodalNetwork;
	}

	public TravelDisutility getAndClearDisutility() {
		final TravelDisutility result = this.mostRecentlyCreatedTravelDisutility;
		this.mostRecentlyCreatedTravelDisutility = null;
		return result;
	}

	public TravelTime getAndClearTravelTime() {
		final TravelTime result = this.mostRecentyCreatedTravelTime;
		this.mostRecentyCreatedTravelTime = null;
		return result;
	}

	public void createNetworkData(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode,
			Network network, boolean isContainer) throws InsufficientDataException {

		final Map<Link, BasicTransportCost> link2cost = new LinkedHashMap<>(network.getLinks().size());
		this.episodeCostModel.populateLink2transportCost(link2cost, commodity, mode, isContainer, network);

		final Map<Link, Double> link2disutility = link2cost.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().monetaryCost));
		this.mostRecentlyCreatedTravelDisutility = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return this.getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link2disutility.get(link);
			}
		};

		final Map<Link, Double> link2tt = link2cost.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> Units.S_PER_H * e.getValue().duration_h));
		this.mostRecentyCreatedTravelTime = new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return link2tt.get(link);
			}
		};
	}
}
