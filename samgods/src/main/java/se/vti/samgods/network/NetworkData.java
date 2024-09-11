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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import floetteroed.utilities.Units;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.costs.BasicTransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkData {

	// -------------------- CONSTRUCTION --------------------

	NetworkData(NetworkDataProvider routingData) {
		this.dataProvider = routingData;
	}

	// --------------- PASS-THROUGH FROM NetworkDataProvider ---------------

	private final NetworkDataProvider dataProvider;

	public Set<Id<Link>> getFerryLinkIds() {
		return this.dataProvider.getFerryLinkIds();
	}

	public Map<Id<Link>, BasicTransportCost> getLinkId2unitCost(VehicleType vehicleType) {
		return this.dataProvider.getLinkId2unitCost(vehicleType);
	}

	// -------------------- LOCALLY CACHED UNIMODAL Network --------------------

	private final Map<TransportMode, Map<Boolean, Network>> mode2containsFerry2network = new LinkedHashMap<>();

	public Network getUnimodalNetwork(TransportMode samgodsMode, boolean containsFerry) {
		return this.mode2containsFerry2network.computeIfAbsent(samgodsMode, m -> new LinkedHashMap<>())
				.computeIfAbsent(containsFerry, cf -> this.dataProvider.createMATSimNetwork(samgodsMode, cf));
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

	public TravelDisutility getTravelDisutility(VehicleType vehicleType) throws InsufficientDataException {
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

	public TravelTime getTravelTime(VehicleType vehicleType) throws InsufficientDataException {
		return this.vehicleType2travelTime.computeIfAbsent(vehicleType, vt -> this.createTravelTime(vehicleType));
	}
}
