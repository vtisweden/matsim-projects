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

	// -------------------- MEMBERS --------------------

	private final NetworkDataProvider dataProvider;

	private final Map<TransportMode, Map<Boolean, Network>> mode2containsFerry2network = new LinkedHashMap<>();

//	private final Map<Commodity, Map<TransportMode, Map<Boolean, Map<Boolean, TravelDisutility>>>> commodity2mode2isContainer2containsFerry2travelDisutility = new LinkedHashMap<>();
	private final Map<VehicleType, TravelDisutility> vehicleType2travelDisutility = new LinkedHashMap<>();

//	private final Map<Commodity, Map<TransportMode, Map<Boolean, Map<Boolean, TravelTime>>>> commodity2mode2isContainer2containsFerry2travelTime = new LinkedHashMap<>();
	private final Map<VehicleType, TravelTime> vehicleType2travelTime = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	NetworkData(NetworkDataProvider routingData) {
		this.dataProvider = routingData;
	}

	// -------------------- IMPLEMENTATION --------------------

	public Set<Id<Link>> getFerryLinkIds() {
		return this.dataProvider.getFerryLinkIds();
	}

	public Network getUnimodalNetwork(TransportMode mode, boolean containsFerry) {
		return this.mode2containsFerry2network.computeIfAbsent(mode, m -> new LinkedHashMap<>())
				.computeIfAbsent(containsFerry, cf -> this.dataProvider.createNetwork(mode, cf));
	}

	public Map<Id<Link>, BasicTransportCost> getLinkId2unitCost(VehicleType vehicleType) {
		return this.dataProvider.getLinkId2unitCost(vehicleType);
	}

//	public Map<Id<Link>, BasicTransportCost> getLinkId2representativeUnitCost(Commodity commodity, TransportMode mode,
//			boolean isContainer, boolean containsFerry) throws InsufficientDataException {
//		return this.getLinkId2unitCost(this.getRepresentativeVehicleType(commodity, mode, isContainer, containsFerry));
//	}

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
				return Math.max(1e-3, linkId2representativeUnitCost.get(link.getId()).monetaryCost);
			}
		};
	}

//	public TravelDisutility getTravelDisutility(Commodity commodity, TransportMode mode, boolean isContainer,
//			boolean containsFerry) throws InsufficientDataException {
//		final Map<Id<Link>, BasicTransportCost> linkId2representativeUnitCost = this
//				.getLinkId2representativeUnitCost(commodity, mode, isContainer, containsFerry);
//		return this.commodity2mode2isContainer2containsFerry2travelDisutility
//				.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
//				.computeIfAbsent(mode, m -> new LinkedHashMap<>())
//				.computeIfAbsent(isContainer, ic -> new LinkedHashMap<>())
//				.computeIfAbsent(containsFerry, cf -> this.createTravelDisutility(linkId2representativeUnitCost));
//	}

	public TravelDisutility getTravelDisutility(VehicleType vehicleType) throws InsufficientDataException {
		return this.vehicleType2travelDisutility.computeIfAbsent(vehicleType, vt -> this.createTravelDisutility(vt));
	}

	private TravelTime createTravelTime(VehicleType vehicleType) {
		final Map<Id<Link>, BasicTransportCost> linkId2representativeUnitCost = this.getLinkId2unitCost(vehicleType);
		return new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return Math.max(1e-3, Units.S_PER_H * linkId2representativeUnitCost.get(link.getId()).duration_h);
			}
		};
	}

	public TravelTime getTravelTime(VehicleType vehicleType) throws InsufficientDataException {
		return this.vehicleType2travelTime.computeIfAbsent(vehicleType, vt -> this.createTravelTime(vehicleType));
	}

}
