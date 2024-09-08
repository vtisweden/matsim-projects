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
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class NetworkData2 {

	private final NetworkDataProvider2 dataProvider;

	private final Map<TransportMode, Map<Boolean, Network>> mode2containsFerry2network = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Map<Boolean, TravelDisutility>>> commodity2mode2isContainer2travelDisutility = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Map<Boolean, TravelTime>>> commodity2mode2isContainer2travelTime = new LinkedHashMap<>();

	NetworkData2(NetworkDataProvider2 routingData) {
		this.dataProvider = routingData;
	}

	public Set<Id<Link>> getFerryLinkIds() {
		return this.dataProvider.getFerryLinkIds();
	}

	public VehicleType getRepresentativeVehicleType(Commodity commodity, TransportMode mode, boolean isContainer,
			boolean containsFerry) {
		return this.dataProvider.getRepresentativeVehicleType(commodity, mode, isContainer, containsFerry);
	}

	public List<VehicleType> getCompatibleVehicleTypes(Commodity commodity, TransportMode mode, boolean isContainer,
			boolean containsFerry) throws InsufficientDataException {
		return this.dataProvider.getCompatibleVehicleTypes(commodity, mode, isContainer, containsFerry);
	}

	public Network getUnimodalNetwork(TransportMode mode, boolean containsFerry) {
		return this.mode2containsFerry2network.computeIfAbsent(mode, m -> new LinkedHashMap<>())
				.computeIfAbsent(containsFerry, f -> dataProvider.createNetwork(mode, f));
	}

	public Network getUnimodalNetwork(VehicleType vehicleType) {
		FreightVehicleAttributes attrs = FreightVehicleAttributes.getFreightAttributesSynchronized(vehicleType);
		return this.getUnimodalNetwork(attrs.mode, attrs.isFerryCompatible());
	}

	public Map<Id<Link>, BasicTransportCost> getLinkId2unitCost(Commodity commodity, VehicleType vehicleType) {
		return this.dataProvider.getLinkId2unitCost(this.getUnimodalNetwork(vehicleType), commodity, vehicleType);
	}

	public Map<Id<Link>, BasicTransportCost> getLinkId2representativeUnitCost(Commodity commodity, TransportMode mode,
			boolean isContainer, boolean containsFerry) {
		return this.getLinkId2unitCost(commodity,
				this.getRepresentativeVehicleType(commodity, mode, isContainer, containsFerry));
	}

	private TravelDisutility createTravelDisutility(Map<Id<Link>, BasicTransportCost> linkId2cost) {
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

	public TravelDisutility getTravelDisutility(Commodity commodity, TransportMode mode, boolean isContainer,
			boolean containsFerry) {
		return this.commodity2mode2isContainer2travelDisutility.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(mode, m -> new LinkedHashMap<>())
				.computeIfAbsent(isContainer, c -> this.createTravelDisutility(
						this.getLinkId2representativeUnitCost(commodity, mode, isContainer, containsFerry)));
	}

	private TravelTime createTravelTime(Map<Id<Link>, BasicTransportCost> linkId2cost) {
		return new TravelTime() {
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				assert (person == null);
				assert (vehicle == null);
				return Units.S_PER_H * linkId2cost.get(link.getId()).duration_h;
			}
		};
	}

	public TravelTime getTravelTime(Commodity commodity, TransportMode mode, boolean isContainer, boolean containsFerry)
			throws InsufficientDataException {
		return this.commodity2mode2isContainer2travelTime.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(mode, m -> new LinkedHashMap<>())
				.computeIfAbsent(isContainer, c -> this.createTravelTime(
						this.getLinkId2representativeUnitCost(commodity, mode, isContainer, containsFerry)));
	}
}
