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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkDataProvider {

	// -------------------- STATIC, SINGLETON --------------------

	private static final Logger log = Logger.getLogger(NetworkDataProvider.class);

	private static NetworkDataProvider instance;

	private NetworkDataProvider(final Network multimodalNetwork) {
		this.multimodalNetwork = multimodalNetwork;
		this.domesticNodeIds.addAll(multimodalNetwork.getNodes().values().stream()
				.filter(l -> ((SamgodsNodeAttributes) l.getAttributes()
						.getAttribute(SamgodsNodeAttributes.ATTRIBUTE_NAME)).isDomestic)
				.map(l -> l.getId()).collect(Collectors.toSet()));
		this.domesticLinkIds.addAll(multimodalNetwork.getLinks().values().stream()
				.filter(l -> this.domesticNodeIds.contains(l.getFromNode().getId())
						&& this.domesticNodeIds.contains(l.getToNode().getId()))
				.map(l -> l.getId()).collect(Collectors.toSet()));
		this.ferryLinkIds.addAll(multimodalNetwork.getLinks().values().stream()
				.filter(l -> ((SamgodsLinkAttributes) l.getAttributes()
						.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME)).samgodsMode.isFerry())
				.map(l -> l.getId()).collect(Collectors.toSet()));
		this.links = new ConcurrentHashMap<>(multimodalNetwork.getLinks());
	}

	public static void initialize(final Network network) {
		instance = new NetworkDataProvider(network);
	}

	public static NetworkDataProvider getInstance() {
		return instance;
	}

	public NetworkData createNetworkData() {
		return new NetworkData(this);
	}

	// --------------- LOCAL, NOT THREADSAFE multimodal Network ---------------

	private final Network multimodalNetwork;

	synchronized Network createNetwork(SamgodsVehicleAttributes vehicleAttrs) {
		assert (!TransportMode.Ferry.equals(vehicleAttrs.samgodsMode));

		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork,
				Collections.singleton(TransportModeMatching.getMatsimModeIgnoreFerry(vehicleAttrs.samgodsMode)));

		final Set<Id<Link>> removeLinkIds = new LinkedHashSet<>();
		for (Link link : this.multimodalNetwork.getLinks().values()) {
			final SamgodsLinkAttributes linkAttrs = (SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			if (vehicleAttrs.networkModes.stream().noneMatch(nm -> linkAttrs.networkModes.contains(nm))) {
				removeLinkIds.add(link.getId());
			}
		}
		removeLinkIds.forEach(id -> unimodalNetwork.removeLink(id));

		log.warn("Not cleaning unimodal network."); // new NetworkCleaner().run(unimodalNetwork);
		return unimodalNetwork;
	}

	// ---------- THREAD-SAFE, LOCALLY CACHED DOMESTIC NODE AND LINK IDS ----------

	private final Set<Id<Node>> domesticNodeIds = ConcurrentHashMap.newKeySet();
	private final Set<Id<Link>> domesticLinkIds = ConcurrentHashMap.newKeySet();
	private final ConcurrentMap<Id<Link>, Link> links;

	Set<Id<Node>> getDomesticNodeIds() {
		return this.domesticNodeIds;
	}

	Set<Id<Link>> getDomesticLinkIds() {
		return this.domesticLinkIds;
	}

	ConcurrentMap<Id<Link>, Link> getLinks() {
		return this.links;
	}

	// --------------- THREAD-SAFE, LOCALLY CACHED FERRY LINK IDS ---------------

	private final Set<Id<Link>> ferryLinkIds = ConcurrentHashMap.newKeySet();

	Set<Id<Link>> getFerryLinkIds() {
		return this.ferryLinkIds;
	}

	// --------------- THREAD-SAFE, LOCALLY CACHED LINK UNIT COSTS ---------------

	private final ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> vehicleType2linkId2unitCost = new ConcurrentHashMap<>();

	ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> getVehicleType2linkId2unitCost() {
		return this.vehicleType2linkId2unitCost;
	}
}
