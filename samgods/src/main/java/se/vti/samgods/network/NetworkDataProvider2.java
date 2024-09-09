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
import java.util.List;
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
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;
import se.vti.samgods.transportation.fleet.VehicleFleet;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkDataProvider2 {

	// -------------------- INTERNAL MEMBERS --------------------

	private final Network multimodalNetwork;

	private final VehicleFleet fleet;

	// -------------------- SHARED, SYNCHRONIZED MEMBERS --------------------

	private final CopyOnWriteArraySet<Id<Link>> ferryLinkIds;

	private final ConcurrentMap<Commodity, Map<TransportMode, Map<Boolean, VehicleType>>> commodity2transportMode2isContainer2representativeVehicleType = new ConcurrentHashMap<>();

	private final ConcurrentMap<Commodity, Map<TransportMode, Map<Boolean, Map<Boolean, List<VehicleType>>>>> commodity2transportMode2isContainer2isFerry2representativeVehicleTypes = new ConcurrentHashMap<>();

	private final ConcurrentMap<Commodity, Map<VehicleType, Map<Id<Link>, BasicTransportCost>>> commodity2vehicleType2linkId2unitCost = new ConcurrentHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public NetworkDataProvider2(Network multimodalNetwork, VehicleFleet fleet) {
		this.multimodalNetwork = multimodalNetwork;
		this.fleet = fleet;
		this.ferryLinkIds = new CopyOnWriteArraySet<>(multimodalNetwork.getLinks().values().stream().filter(
				l -> ((LinkAttributes) l.getAttributes().getAttribute(LinkAttributes.ATTRIBUTE_NAME)).mode.isFerry())
				.map(l -> l.getId()).collect(Collectors.toSet()));
	}

	public NetworkData2 createNetworkData() {
		return new NetworkData2(this);
	}

	// --------------- SYNCHRONIZED POPULATION OF DATA STRUCTURES ---------------

	private synchronized VehicleType createRepresentativeVehicleType(Commodity commodity, TransportMode mode,
			boolean isContainer, boolean containsFerry) {
		try {
			return this.fleet.getRepresentativeVehicleType(commodity, mode, isContainer, containsFerry);
		} catch (InsufficientDataException e) {
			e.log(this.getClass(), "no representative vehicle type found", commodity, null, mode, isContainer,
					containsFerry);
			return null;
		}
	}

	private synchronized List<VehicleType> createCompatibleVehicleTypes(Commodity commodity, TransportMode mode,
			boolean isContainer, boolean isFerry) {
		return Collections
				.unmodifiableList(this.fleet.getCompatibleVehicleTypes(commodity, mode, isContainer, isFerry));
	}

	private synchronized Map<Id<Link>, BasicTransportCost> createLinkId2unitCost(Network network, Commodity commodity,
			VehicleType vehicleType) {
		final ConcurrentHashMap<Id<Link>, BasicTransportCost> result = new ConcurrentHashMap<>(
				network.getLinks().size());
		final FreightVehicleAttributes vehicleAttrs = FreightVehicleAttributes
				.getFreightAttributesSynchronized(vehicleType);
		for (Link link : network.getLinks().values()) {
			final LinkAttributes linkAttrs = ((LinkAttributes) link.getAttributes()
					.getAttribute(LinkAttributes.ATTRIBUTE_NAME));
			final double speed_km_h;
			if (vehicleAttrs.speed_km_h != null) {
				speed_km_h = Math.min(vehicleAttrs.speed_km_h, Units.KM_H_PER_M_S * link.getFreespeed());
			} else {
				speed_km_h = Units.KM_H_PER_M_S * link.getFreespeed();
			}
			assert (speed_km_h > 0 && Double.isFinite(speed_km_h));
			final double length_km = Units.KM_PER_M * link.getLength();
			final double duration_h = length_km / speed_km_h;
			if (linkAttrs.mode.isFerry()) {
				result.put(link.getId(),
						new BasicTransportCost(1.0,
								duration_h * vehicleAttrs.onFerryCost_1_h + length_km * vehicleAttrs.onFerryCost_1_km,
								duration_h, length_km));
			} else {
				result.put(link.getId(),
						new BasicTransportCost(1.0,
								duration_h * vehicleAttrs.cost_1_h + length_km * vehicleAttrs.cost_1_km, duration_h,
								length_km));
			}
		}
		return result;
	}

	// --------------- (DELIBERATELY PACKAGE PRIVATE) IMPLEMENTATION ---------------

	Set<Id<Link>> getFerryLinkIds() {
		return this.ferryLinkIds;
	}

	VehicleType getRepresentativeVehicleType(final Commodity commodity, final TransportMode mode,
			final boolean isContainer, final boolean containsFerry) {
		return this.commodity2transportMode2isContainer2representativeVehicleType
				.computeIfAbsent(commodity, c -> new ConcurrentHashMap<>())
				.computeIfAbsent(mode, m -> new ConcurrentHashMap<>()).computeIfAbsent(isContainer,
						ic -> this.createRepresentativeVehicleType(commodity, mode, ic, containsFerry));
	}

	List<VehicleType> getCompatibleVehicleTypes(final Commodity commodity, final TransportMode mode,
			final boolean isContainer, final boolean isFerry) throws InsufficientDataException {
		return this.commodity2transportMode2isContainer2isFerry2representativeVehicleTypes
				.computeIfAbsent(commodity, c -> new ConcurrentHashMap<>())
				.computeIfAbsent(mode, m -> new ConcurrentHashMap<>())
				.computeIfAbsent(isContainer, ic -> new ConcurrentHashMap<>())
				.computeIfAbsent(isFerry, f -> this.createCompatibleVehicleTypes(commodity, mode, isContainer, f));
	}

	// This needs to be bookkeept by the data object.
	synchronized Network createNetwork(SamgodsConstants.TransportMode mode, boolean containsFerry) {
		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork, mode.matsimModes);
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

	// Cached costs are consistent if network is result of this.createNetwork(..)
	Map<Id<Link>, BasicTransportCost> getLinkId2unitCost(Network network, Commodity commodity,
			VehicleType vehicleType) {
		return this.commodity2vehicleType2linkId2unitCost.computeIfAbsent(commodity, c -> new ConcurrentHashMap<>())
				.computeIfAbsent(vehicleType, t -> this.createLinkId2unitCost(network, commodity, vehicleType));
	}
}
