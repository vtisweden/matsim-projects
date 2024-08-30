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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
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
public class NetworkDataProvider {

	// -------------------- MEMBERS --------------------

	private final Network multimodalNetwork;

	private final VehicleFleet fleet;

	// -------------------- CONSTRUCTION --------------------

	public NetworkDataProvider(Network multimodalNetwork, VehicleFleet fleet) {
		this.multimodalNetwork = multimodalNetwork;
		this.fleet = fleet;
	}

	// -------------------- IMPLEMENTATION --------------------

	public NetworkData createNetworkData() {
		return new NetworkData(this);
	}

	// -------------------- PACKAGE PRIVATE SYNCHRONIZED --------------------

	synchronized VehicleType createRepresentativeVehicleType(Commodity commodity, TransportMode mode,
			boolean isContainer) throws InsufficientDataException {
		try {
			return this.fleet.getRepresentativeVehicleType(commodity, mode, isContainer, true);
		} catch (InsufficientDataException e0) {
			return this.fleet.getRepresentativeVehicleType(commodity, mode, isContainer, false);
		}
	}

	synchronized List<VehicleType> createCompatibleVehicleTypes(Commodity commodity, TransportMode mode,
			boolean isContainer) {
		List<VehicleType> resultWithFerry = this.fleet.getCompatibleVehicleTypes(commodity, mode, isContainer, true);
		if (resultWithFerry.size() > 0) {
			return resultWithFerry;
		} else {
			return this.fleet.getCompatibleVehicleTypes(commodity, mode, isContainer, false);
		}
	}

	synchronized Network createNetwork(SamgodsConstants.TransportMode mode, boolean containsFerry) {
		final Network unimodalNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(this.multimodalNetwork).filter(unimodalNetwork, mode.matsimModes);
		if (!containsFerry) {
			for (Id<Link> ferryLinkId : this.createFerryLinkIdSet(unimodalNetwork)) {
				unimodalNetwork.removeLink(ferryLinkId);
			}
		}
		new NetworkCleaner().run(unimodalNetwork);
		return unimodalNetwork;
	}

	synchronized Set<Id<Link>> createFerryLinkIdSet(Network network) {
		return network.getLinks().values().stream().filter(l -> LinkAttributes.isFerry(l)).map(l -> l.getId())
				.collect(Collectors.toSet());
	}

	synchronized Set<Id<Link>> createFerryLinkIdSet() {
		return this.createFerryLinkIdSet(this.multimodalNetwork);
	}

	synchronized BasicTransportCost computeUnitCost(Link link, Commodity commodity,
			FreightVehicleAttributes vehicleAttrs) throws InsufficientDataException {
		final double length_km = Units.KM_PER_M * link.getLength();
		final double duration_h = Units.H_PER_S * vehicleAttrs.travelTimeOnLink_s(link);
		assert (Double.isFinite(length_km));
		assert (Double.isFinite(duration_h));
		if (LinkAttributes.isFerry(link)) {
			return new BasicTransportCost(1.0,
					duration_h * vehicleAttrs.onFerryCost_1_h + length_km * vehicleAttrs.onFerryCost_1_km, duration_h,
					length_km);
		} else {
			return new BasicTransportCost(1.0, duration_h * vehicleAttrs.cost_1_h + length_km * vehicleAttrs.cost_1_km,
					duration_h, length_km);
		}
	}

	synchronized Map<Id<Link>, BasicTransportCost> computeUnitCosts(Network network,
			SamgodsConstants.Commodity commodity, FreightVehicleAttributes vehicleAttrs)
			throws InsufficientDataException {
		final Map<Id<Link>, BasicTransportCost> linkId2cost = new LinkedHashMap<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			linkId2cost.put(link.getId(), this.computeUnitCost(link, commodity, vehicleAttrs));
		}
		return linkId2cost;
	}

	synchronized TravelDisutility createTravelDisutility(Map<Id<Link>, BasicTransportCost> linkId2cost) {
		return new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return this.getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return linkId2cost.get(link.getId()).monetaryCost;
			}
		};
	}

	synchronized TravelTime createTravelTime(Map<Id<Link>, BasicTransportCost> linkId2cost) {
		return new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return Units.S_PER_H * linkId2cost.get(link.getId()).duration_h;
			}
		};
	}

}
