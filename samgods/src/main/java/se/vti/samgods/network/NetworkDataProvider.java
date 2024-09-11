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

import java.util.Map;
import java.util.Set;
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

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkDataProvider {

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

	Set<Id<Link>> getFerryLinkIds() {
		return this.ferryLinkIds;
	}

	// --------------- THREAD-SAFE, LOCALLY CACHED LINK UNIT COSTS ---------------

	private final ConcurrentMap<VehicleType, Map<Id<Link>, BasicTransportCost>> vehicleType2linkId2unitCost = new ConcurrentHashMap<>();

	private final CopyOnWriteArraySet<Link> allLinks;

	private synchronized Map<Id<Link>, BasicTransportCost> createLinkId2unitCost(VehicleType vehicleType) {
		final ConcurrentHashMap<Id<Link>, BasicTransportCost> result = new ConcurrentHashMap<>(this.allLinks.size());
		final SamgodsVehicleAttributes vehicleAttrs = (SamgodsVehicleAttributes) vehicleType.getAttributes()
				.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
		for (Link link : this.allLinks) {
			final SamgodsLinkAttributes linkAttrs = ((SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME));
			if (linkAttrs.samgodsMode.equals(vehicleAttrs.samgodsMode)
					|| (linkAttrs.samgodsMode.isFerry() && vehicleAttrs.isFerryCompatible())) {
				final double speed_km_h;
				if (vehicleAttrs.speed_km_h != null) {
					speed_km_h = Math.min(vehicleAttrs.speed_km_h, Units.KM_H_PER_M_S * link.getFreespeed());
				} else {
					speed_km_h = Units.KM_H_PER_M_S * link.getFreespeed();
				}
				assert (speed_km_h > 0 && Double.isFinite(speed_km_h));
				final double length_km = Units.KM_PER_M * link.getLength();
				final double duration_h = length_km / speed_km_h;
				if (linkAttrs.samgodsMode.isFerry()) {
					result.put(link.getId(), new BasicTransportCost(1.0,
							duration_h * vehicleAttrs.onFerryCost_1_h + length_km * vehicleAttrs.onFerryCost_1_km,
							duration_h, length_km));
				} else {
					result.put(link.getId(),
							new BasicTransportCost(1.0,
									duration_h * vehicleAttrs.cost_1_h + length_km * vehicleAttrs.cost_1_km, duration_h,
									length_km));
				}
			}
		}
		return result;
	}

	Map<Id<Link>, BasicTransportCost> getLinkId2unitCost(VehicleType vehicleType) {
		return this.vehicleType2linkId2unitCost.computeIfAbsent(vehicleType,
				t -> this.createLinkId2unitCost(vehicleType));
	}

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
}
