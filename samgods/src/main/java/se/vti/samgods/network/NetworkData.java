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
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

public class NetworkData {

	private final NetworkDataProvider dataProvider;

	private final Set<Id<Link>> ferryLinkIds;

//	private Network multimodalNetwork = null;

	private final Map<Commodity, Map<TransportMode, Map<Boolean, VehicleType>>> commodity2transportMode2isContainer2representativeVehicleType = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Map<Boolean, List<VehicleType>>>> commodity2transportMode2isContainer2representativeVehicleTypes = new LinkedHashMap<>();

	private final Map<TransportMode, Map<Boolean, Network>> mode2containsFerry2network = new LinkedHashMap<>();

	private final Map<Commodity, Map<VehicleType, Map<Id<Link>, BasicTransportCost>>> commodity2vehicleType2linkId2unitCost = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Map<Boolean, TravelDisutility>>> commodity2mode2isContainer2travelDisutility = new LinkedHashMap<>();

	private final Map<Commodity, Map<TransportMode, Map<Boolean, TravelTime>>> commodity2mode2isContainer2travelTime = new LinkedHashMap<>();

	NetworkData(NetworkDataProvider routingData) {
		this.dataProvider = routingData;
		this.ferryLinkIds = routingData.createFerryLinkIdSet();
	}

	public Set<Id<Link>> getFerryLinkIds() {
		return this.ferryLinkIds;
	}

	public VehicleType getRepresentativeVehicleType(Commodity commodity, TransportMode mode, boolean isContainer)
			throws InsufficientDataException {
		VehicleType result = this.commodity2transportMode2isContainer2representativeVehicleType
				.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(mode, m -> new LinkedHashMap<>()).get(isContainer);
		if (result == null) {
			result = this.dataProvider.createRepresentativeVehicleType(commodity, mode, isContainer);
			this.commodity2transportMode2isContainer2representativeVehicleType.get(commodity).get(mode).put(isContainer,
					result);
		}
		return result;
	}

	public List<VehicleType> getCompatibleVehicleTypes(Commodity commodity, TransportMode mode, boolean isContainer) {
		List<VehicleType> result = this.commodity2transportMode2isContainer2representativeVehicleTypes
				.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(mode, m -> new LinkedHashMap<>()).get(isContainer);
		if (result == null) {
			result = this.dataProvider.createCompatibleVehicleTypes(commodity, mode, isContainer);
			this.commodity2transportMode2isContainer2representativeVehicleTypes.get(commodity).get(mode)
					.put(isContainer, result);
		}
		return result;
	}

	public Network getUnimodalNetwork(TransportMode mode, boolean containsFerry) {
		Network result = this.mode2containsFerry2network.computeIfAbsent(mode, m -> new LinkedHashMap<>())
				.get(containsFerry);
		if (result == null) {
			result = dataProvider.createNetwork(mode, containsFerry);
			this.mode2containsFerry2network.get(mode).put(containsFerry, result);
		}
		return result;
	}

//	public Network getMultimodalNetwork() {
//		if (this.multimodalNetwork == null) {
//			this.multimodalNetwork = this.dataProvider.createMultimodalNetwork();
//		}
//		return this.multimodalNetwork;
//	}

	public Network getUnimodalNetwork(VehicleType vehicleType) {
		FreightVehicleAttributes attrs = FreightVehicleAttributes.getFreightAttributesSynchronized(vehicleType);
		return this.getUnimodalNetwork(attrs.mode, attrs.isFerryCompatible());
	}

	public Map<Id<Link>, BasicTransportCost> getLinkId2unitCost(Commodity commodity, VehicleType vehicleType)
			throws InsufficientDataException {
		Map<Id<Link>, BasicTransportCost> result = this.commodity2vehicleType2linkId2unitCost
				.computeIfAbsent(commodity, c -> new LinkedHashMap<>()).get(vehicleType);
		if (result == null) {
			result = this.dataProvider.computeUnitCosts(this.getUnimodalNetwork(vehicleType), commodity,
					FreightVehicleAttributes.getFreightAttributesSynchronized(vehicleType));
			this.commodity2vehicleType2linkId2unitCost.get(commodity).put(vehicleType, result);
		}
		return result;
	}

	public Map<Id<Link>, BasicTransportCost> getLinkId2representativeUnitCost(Commodity commodity, TransportMode mode,
			boolean isContainer) throws InsufficientDataException {
		return this.getLinkId2unitCost(commodity, this.getRepresentativeVehicleType(commodity, mode, isContainer));
	}

	public TravelDisutility getTravelDisutility(Commodity commodity, TransportMode mode, boolean isContainer)
			throws InsufficientDataException {
		TravelDisutility result = this.commodity2mode2isContainer2travelDisutility
				.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(mode, m -> new LinkedHashMap<>()).get(isContainer);
		if (result == null) {
			result = this.dataProvider
					.createTravelDisutility(this.getLinkId2representativeUnitCost(commodity, mode, isContainer));
			this.commodity2mode2isContainer2travelDisutility.get(commodity).get(mode).put(isContainer, result);
		}
		return result;
	}

	public TravelTime getTravelTime(Commodity commodity, TransportMode mode, boolean isContainer)
			throws InsufficientDataException {
		TravelTime result = this.commodity2mode2isContainer2travelTime
				.computeIfAbsent(commodity, c -> new LinkedHashMap<>())
				.computeIfAbsent(mode, m -> new LinkedHashMap<>()).get(isContainer);
		if (result == null) {
			result = this.dataProvider
					.createTravelTime(this.getLinkId2representativeUnitCost(commodity, mode, isContainer));
			this.commodity2mode2isContainer2travelTime.get(commodity).get(mode).put(isContainer, result);
		}
		return result;
	}

}
