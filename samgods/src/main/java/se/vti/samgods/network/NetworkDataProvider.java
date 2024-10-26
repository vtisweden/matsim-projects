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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
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
		this.allLinks.addAll(multimodalNetwork.getLinks().values());
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

	public NetworkData createNetworkData() {
		return new NetworkData(this);
	}

	// --------------- LOCAL, NOT THREADSAFE multimodal Network ---------------

	private final Network multimodalNetwork;

	synchronized Network createMATSimNetwork(SamgodsConstants.TransportMode samgodsModeNotFerry, boolean allowFerry) {
		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork,
				Collections.singleton(TransportModeMatching.getMatsimModeIgnoreFerry(samgodsModeNotFerry)));
		if (!allowFerry) {
			this.ferryLinkIds.forEach(id -> unimodalNetwork.removeLink(id));
		}
		Log.warn("Not cleaning unimodal network.");
		// new NetworkCleaner().run(unimodalNetwork);
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

	// --------------- THREAD-SAFE, LOCALLY CACHED LINK REFERENCES ---------------

	private final Set<Link> allLinks = ConcurrentHashMap.newKeySet();

	Set<Link> getAllLinks() {
		return this.allLinks;
	}

	// --------------- THREAD-SAFE, LOCALLY CACHED LINK UNIT COSTS ---------------

	private final ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> vehicleType2linkId2unitCost = new ConcurrentHashMap<>();

	ConcurrentMap<VehicleType, ConcurrentMap<Id<Link>, BasicTransportCost>> getVehicleType2linkId2unitCost() {
		return this.vehicleType2linkId2unitCost;
	}
}
