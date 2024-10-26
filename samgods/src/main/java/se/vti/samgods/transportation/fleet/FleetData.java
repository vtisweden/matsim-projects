/**
 * se.vti.samgods.transportation.fleet
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
package se.vti.samgods.transportation.fleet;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetData {

	// -------------------- CONSTANTS --------------------

	private final FleetDataProvider dataProvider;

	// -------------------- CONSTRUCTION --------------------

	public FleetData(FleetDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	// -------------------- PASS-THROUGH FROM DATA PROVIDER --------------------

	public Map<Id<Link>, CopyOnWriteArraySet<VehicleType>> getLinkId2allowedVehicleTypes() {
		return this.dataProvider.getLinkId2allowedVehicleTypes();
	}

	public Map<VehicleType, SamgodsVehicleAttributes> getVehicleType2attributes() {
		return this.dataProvider.getVehicleType2attributes();
	}

	// TODO NEW
	public Set<Set<VehicleType>> computeAllVehicleOnLinkGroups() {
		final Set<Set<VehicleType>> allVehicleOnLinkGroups = new LinkedHashSet<>();
		for (Set<VehicleType> vehicleOnLinkGroup : this.dataProvider.getLinkId2allowedVehicleTypes().values()) {
			allVehicleOnLinkGroups.add(vehicleOnLinkGroup);
		}
		return allVehicleOnLinkGroups;
	}

	// TODO NEW
	public Set<Set<VehicleType>> computeAlwaysJointVehicleGroups(Set<Set<VehicleType>> allLinkGroups) {
		final Map<VehicleType, Set<VehicleType>> type2accompanyingGroup = new LinkedHashMap<>();
		for (Set<VehicleType> linkGroup : allLinkGroups) {
			for (VehicleType type : linkGroup) {
				if (type2accompanyingGroup.containsKey(type)) {
					type2accompanyingGroup.get(type).retainAll(linkGroup);
				} else {
					type2accompanyingGroup.put(type, new LinkedHashSet<>(linkGroup));
				}
			}
		}
		return type2accompanyingGroup.values().stream().collect(Collectors.toSet());
	}

	// ----- ACCESS TO & UPDATE OF COMPATIBLE VEHICLE TYPES -----

	private Set<VehicleType> createCompatibleVehicleTypes(Commodity commodity, TransportMode mode, boolean isContainer,
			boolean containsFerry) {
		final Set<VehicleType> result = new LinkedHashSet<>(this.dataProvider.getVehicleType2attributes().size());
		for (ConcurrentMap.Entry<VehicleType, SamgodsVehicleAttributes> e : this.dataProvider
				.getVehicleType2attributes().entrySet()) {
			VehicleType type = e.getKey();
			SamgodsVehicleAttributes attrs = e.getValue();
			if (attrs.samgodsMode.equals(mode) && (attrs.isContainer == isContainer) && attrs.isCompatible(commodity)
					&& (!containsFerry || attrs.isFerryCompatible())) {
				result.add(type);
			}
		}
		return result;
	}

	public Set<VehicleType> getCompatibleVehicleTypes(Commodity commodity, TransportMode mode, boolean isContainer,
			boolean containsFerry) {
		return this.dataProvider.getCommodity2transportMode2isContainer2isFerry2compatibleVehicleTypes()
				.computeIfAbsent(commodity, c -> new ConcurrentHashMap<>())
				.computeIfAbsent(mode, m -> new ConcurrentHashMap<>())
				.computeIfAbsent(isContainer, ic -> new ConcurrentHashMap<>())
				.computeIfAbsent(containsFerry, f -> new CopyOnWriteArraySet<>(
						this.createCompatibleVehicleTypes(commodity, mode, isContainer, f)));
	}

	public Set<VehicleType> computeLinkCompatibleVehicleTypes(ConsolidationUnit consolidationUnit) {
		final LinkedHashSet<VehicleType> result = new LinkedHashSet<>(
				this.getCompatibleVehicleTypes(consolidationUnit.commodity, consolidationUnit.samgodsMode,
						consolidationUnit.isContainer, consolidationUnit.containsFerry));
		for (Id<Link> linkId : consolidationUnit.linkIds) {
			result.retainAll(this.dataProvider.getLinkId2allowedVehicleTypes().get(linkId));
		}
		return result;
	}

	// for testing
	public Map<VehicleType, Integer> computeLinkCompatibleVehicleTypeOccurrences(ConsolidationUnit consolidationUnit) {
		final Map<VehicleType, Integer> result = new LinkedHashMap<>(this
				.getCompatibleVehicleTypes(consolidationUnit.commodity, consolidationUnit.samgodsMode,
						consolidationUnit.isContainer, consolidationUnit.containsFerry)
				.stream().collect(Collectors.toMap(t -> t, t -> 0)));
		for (Id<Link> linkId : consolidationUnit.linkIds) {
			for (VehicleType feasibleType : this.dataProvider.getLinkId2allowedVehicleTypes().get(linkId)) {
				if (result.containsKey(feasibleType)) {
					result.compute(feasibleType, (t, c) -> c + 1);
				}
			}
		}
		return result;
	}

	// -------------------- SINGLE-THREADED FUNCTIONALITY --------------------

	private VehicleType computeRepresentativeVehicleType(Set<VehicleType> compatibleVehicleTypes) {
		if (compatibleVehicleTypes.size() > 0) {
			VehicleType result = null;
			final double meanCapacity_ton = compatibleVehicleTypes.stream()
					.mapToDouble(t -> this.getVehicleType2attributes().get(t).capacity_ton).average().getAsDouble();
			double resultDeviation_ton = Double.POSITIVE_INFINITY;
			for (VehicleType candidate : compatibleVehicleTypes) {
				final double candidateDeviation_ton = Math
						.abs(this.getVehicleType2attributes().get(candidate).capacity_ton - meanCapacity_ton);
				if (candidateDeviation_ton < resultDeviation_ton) {
					result = candidate;
					resultDeviation_ton = candidateDeviation_ton;
				}
			}
			return result;
		} else {
			return null;
		}
	}

	// Uses vehicle/link compatibility data, hence requires a routed consolidation
	// untit. may be null.
	public VehicleType computeRepresentativeVehicleType(ConsolidationUnit consolidationUnit) {
		return this.computeRepresentativeVehicleType(this.computeLinkCompatibleVehicleTypes(consolidationUnit));
	}

	// Does not use vehicle/link compatibility data, can hence be used when
	// consolidation units routes are not yet there. may be null.
	public VehicleType getRepresentativeVehicleType(final Commodity commodity, final TransportMode mode,
			final boolean isContainer, final boolean containsFerry) {
		return this.dataProvider.getCommodity2transportMode2isContainer2isFerry2representativeVehicleType()
				.computeIfAbsent(commodity, c -> new ConcurrentHashMap<>())
				.computeIfAbsent(mode, m -> new ConcurrentHashMap<>())
				.computeIfAbsent(isContainer, ic -> new ConcurrentHashMap<>())
				.computeIfAbsent(containsFerry, cf -> this.computeRepresentativeVehicleType(
						this.getCompatibleVehicleTypes(commodity, mode, isContainer, containsFerry)));
	}

	// -------------------- THREAD SAFE ACCESS TO ASCs --------------------

	public Map<VehicleType, Double> getVehicleType2asc() {
		return this.dataProvider.getVehicleType2asc();
	}

	public Map<TransportMode, Double> getMode2asc() {
		return this.dataProvider.getMode2asc();
	}

	public ConcurrentMap<Commodity, Double> getRailCommodity2asc() {
		return this.dataProvider.getRailCommodity2asc();
	}

}
