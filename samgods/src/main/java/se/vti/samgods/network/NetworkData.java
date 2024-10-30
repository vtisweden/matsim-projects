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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkData {

	// -------------------- CONSTANTS --------------------

	private final NetworkDataProvider dataProvider;

	// -------------------- CONSTRUCTION --------------------

	NetworkData(NetworkDataProvider routingData) {
		this.dataProvider = routingData;
	}

	// --------------- PASS-THROUGH FROM NetworkDataProvider ---------------

	public Set<Id<Node>> getDomesticNodeIds() {
		return this.dataProvider.getDomesticNodeIds();
	}

	public Set<Id<Link>> getDomesticLinkIds() {
		return this.dataProvider.getDomesticLinkIds();
	}

	public Set<Id<Link>> getFerryLinkIds() {
		return this.dataProvider.getFerryLinkIds();
	}

	public ConcurrentMap<Id<Link>, Link> getLinks() {
		return this.dataProvider.getLinks();
	}

	// --------------- PASS-THROUGH FROM NetworkDataProvider ---------------

	private ConcurrentMap<Id<Link>, BasicTransportCost> createLinkId2unitCost(VehicleType vehicleType) {
		final ConcurrentHashMap<Id<Link>, BasicTransportCost> result = new ConcurrentHashMap<>(
				this.dataProvider.getLinks().size());
		final SamgodsVehicleAttributes vehicleAttrs = (SamgodsVehicleAttributes) vehicleType.getAttributes()
				.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
		for (Link link : this.dataProvider.getLinks().values()) {
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

	public Map<Id<Link>, BasicTransportCost> getLinkId2unitCost(VehicleType vehicleType) {
		return this.dataProvider.getVehicleType2linkId2unitCost().computeIfAbsent(vehicleType,
				vt -> this.createLinkId2unitCost(vt));
	}

	// -------------------- LOCALLY CACHED UNIMODAL Network --------------------

	private final Map<VehicleType, Network> vehicleType2network = new LinkedHashMap<>();

	public Network getUnimodalNetwork(VehicleType vehicleType) {
		return this.vehicleType2network.computeIfAbsent(vehicleType, vt -> {
			SamgodsVehicleAttributes vehicleAttrs = (SamgodsVehicleAttributes) vt.getAttributes()
					.getAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME);
			return this.dataProvider.createNetwork(vehicleAttrs);
		});
	}

	// -------------------- LOCALLY CACHED TravelDisutility --------------------

	private final double minMonetaryCost = 1e-3;

	private final Map<VehicleType, TravelDisutility> vehicleType2travelDisutility = new LinkedHashMap<>();

	private TravelDisutility createTravelDisutility(VehicleType vehicleType) {
		final Map<Id<Link>, BasicTransportCost> linkId2representativeUnitCost = this.getLinkId2unitCost(vehicleType);
		return new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return this.getLinkMinimumTravelDisutility(link);
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return Math.max(minMonetaryCost, linkId2representativeUnitCost.get(link.getId()).monetaryCost);
			}
		};
	}

	public TravelDisutility getTravelDisutility(VehicleType vehicleType) {
		return this.vehicleType2travelDisutility.computeIfAbsent(vehicleType, vt -> this.createTravelDisutility(vt));
	}

	// -------------------- LOCALLY CACHED TravelTime --------------------

	private final double minTravelTime_s = 1e-3;

	private final Map<VehicleType, TravelTime> vehicleType2travelTime = new LinkedHashMap<>();

	private TravelTime createTravelTime(VehicleType vehicleType) {
		final Map<Id<Link>, BasicTransportCost> linkId2representativeUnitCost = this.getLinkId2unitCost(vehicleType);
		return new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return Math.max(minTravelTime_s,
						Units.S_PER_H * linkId2representativeUnitCost.get(link.getId()).duration_h);
			}
		};
	}

	public TravelTime getTravelTime(VehicleType vehicleType) {
		return this.vehicleType2travelTime.computeIfAbsent(vehicleType, vt -> this.createTravelTime(vehicleType));
	}
}
