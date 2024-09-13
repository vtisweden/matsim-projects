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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.costs.BasicTransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkDataProvider {

	// -------------------- CONSTRUCTION --------------------

	public NetworkDataProvider(final Network multimodalNetwork) {
		this.multimodalNetwork = multimodalNetwork;
		this.allLinks = new CopyOnWriteArraySet<>(multimodalNetwork.getLinks().values());
		this.ferryLinkIds = new CopyOnWriteArraySet<>(multimodalNetwork.getLinks().values().stream()
				.filter(l -> ((SamgodsLinkAttributes) l.getAttributes()
						.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME)).samgodsMode.isFerry())
				.map(l -> l.getId()).collect(Collectors.toSet()));
	}

	public NetworkData createNetworkData() {
		return new NetworkData(this);
	}

	// --------------- LOCAL, NOT THREADSAFE multimodal Network ---------------

	private final Network multimodalNetwork;

	synchronized Network createMATSimNetwork(SamgodsConstants.TransportMode samgodsMode, boolean containsFerry) {
		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork, samgodsMode.matsimModes);
		if (!containsFerry) {
			for (Id<Link> ferryLinkId : this.getFerryLinkIds()) {
				if (unimodalNetwork.getLinks().containsKey(ferryLinkId)) {
					unimodalNetwork.removeLink(ferryLinkId);
				}
			}
		}
		new NetworkCleaner().run(unimodalNetwork);
		return unimodalNetwork;
	}

	// --------------- THREAD-SAFE, LOCALLY CACHED FERRY LINK IDS ---------------

	private final CopyOnWriteArraySet<Id<Link>> ferryLinkIds;

	CopyOnWriteArraySet<Id<Link>> getFerryLinkIds() {
		return this.ferryLinkIds;
	}

	// --------------- THREAD-SAFE, LOCALLY CACHED LINK REFERENCES ---------------

	private final CopyOnWriteArraySet<Link> allLinks;

	CopyOnWriteArraySet<Link> getAllLinks() {
		return this.allLinks;
	}

	// --------------- THREAD-SAFE, LOCALLY CACHED LINK UNIT COSTS ---------------

	private final ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> vehicleType2linkId2unitCost = new ConcurrentHashMap<>();

	ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> getVehicleType2linkId2unitCost() {
		return this.vehicleType2linkId2unitCost;
	}
}
