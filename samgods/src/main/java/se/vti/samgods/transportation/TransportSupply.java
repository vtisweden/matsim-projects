/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import se.vti.samgods.legacy.OD;
import se.vti.samgods.legacy.Samgods;
import se.vti.samgods.legacy.Samgods.Commodity;
import se.vti.samgods.legacy.Samgods.TransportMode;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportLeg;

/**
 * Packages the supply (carrier) side of the freight transport system.
 * 
 * @author GunnarF
 *
 */
public class TransportSupply {

	private static final Map<Samgods.TransportMode, String> samgodsMode2matsimMode;
	static {
		samgodsMode2matsimMode = new LinkedHashMap<>(4);
		samgodsMode2matsimMode.put(Samgods.TransportMode.Road, org.matsim.api.core.v01.TransportMode.car);
		samgodsMode2matsimMode.put(Samgods.TransportMode.Rail, org.matsim.api.core.v01.TransportMode.train);
		samgodsMode2matsimMode.put(Samgods.TransportMode.Sea, org.matsim.api.core.v01.TransportMode.ship);
		samgodsMode2matsimMode.put(Samgods.TransportMode.Air, org.matsim.api.core.v01.TransportMode.airplane);
	}

	private Network network = null;

	private VehicleFleet vehicleFleet = null;
	
	private Map<TransportMode, Network> mode2network = null;

	private Map<Commodity, Map<TransportMode, TransportPrice>> commodity2mode2prices = null;

	public void setNetwork(Network network) {
		this.network = network;
		this.mode2network = new LinkedHashMap<>(samgodsMode2matsimMode.size());
		for (Map.Entry<Samgods.TransportMode, String> entry : samgodsMode2matsimMode.entrySet()) {
			final Network unimodalNetwork = NetworkUtils.createNetwork();
			unimodalNetwork.setCapacityPeriod(3600.0);
			new TransportModeNetworkFilter(this.network).filter(unimodalNetwork,
					Collections.singleton(entry.getValue()));
			this.mode2network.put(entry.getKey(), unimodalNetwork);
		}
	}

	public Network getNetwork() {
		return this.network;
	}
	
	public VehicleFleet getVehicleFleet() {
		return this.vehicleFleet;
	}

	public Network getNetwork(Samgods.TransportMode transportMode) {
		return this.mode2network.get(transportMode);
	}

	public TransportPrice getUnitPrice(Commodity commodity, TransportMode mode) {
		return this.commodity2mode2prices.get(commodity).get(mode);
	}

	public TravelDisutility createTravelDisutilityForAnonymousRouting(Commodity commodity, TransportMode mode) {
		final TransportPrice price = this.getUnitPrice(commodity, mode);
		return new TravelDisutility() {

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return price.getUnitPrice(link.getId()).getTransportPrice_1_ton();
			}

			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return this.getLinkMinimumTravelDisutility(link);
			}
		};
	}

	// TODO parallelize
	public void routeAllLegs(Map<Commodity, Map<OD, List<TransportChain>>> commodity2od2chains) {
		final Map<TransportMode, UnimodalNetworkRouter> mode2router = new LinkedHashMap<>();
		for (Map.Entry<Commodity, Map<OD, List<TransportChain>>> entry : commodity2od2chains.entrySet()) {
			final Commodity commodity = entry.getKey();
			mode2router.clear();
			for (List<TransportChain> chains : entry.getValue().values()) {
				for (TransportChain chain : chains) {
					for (TransportLeg leg : chain.getLegs()) {
						UnimodalNetworkRouter router = mode2router.computeIfAbsent(leg.getMode(),
								l -> new UnimodalNetworkRouter(this.getNetwork(),
										this.createTravelDisutilityForAnonymousRouting(commodity, leg.getMode())));
						leg.setRoute(router.route(leg.getOD()));
					}
				}
			}
		}
	}
}
