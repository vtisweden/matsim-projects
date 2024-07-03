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
package se.vti.samgods.transportation.fleet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.consolidation.road.ConsolidationUtils;
import se.vti.samgods.transportation.consolidation.road.PrototypeVehicle;

/**
 * 
 * @author GunnarF
 *
 */
public class VehicleFleet {

	// -------------------- MEMBERS --------------------

	private final Vehicles vehicles;

	private long vehCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	public VehicleFleet() {
		this.vehicles = VehicleUtils.createVehiclesContainer();
	}

	// -------------------- IMPLEMENTATION --------------------

	public Vehicles getVehicles() {
		return this.vehicles;
	}

	public Vehicle createAndAddVehicle(VehicleType type) {
		assert (this.vehicles.getVehicleTypes().values().contains(type));
		final Vehicle vehicle = this.vehicles.getFactory().createVehicle(Id.create(this.vehCnt++, Vehicle.class), type);
		this.vehicles.addVehicle(vehicle);
		return vehicle;
	}

	// TODO Move to consolidation once getters of FreightVehicleFleet are decided.
	public Map<VehicleType, Vehicle> createPrototypeVehicles() {
		final Map<VehicleType, Vehicle> type2veh = new LinkedHashMap<>(this.vehicles.getVehicleTypes().size());
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final Vehicle prototype = new PrototypeVehicle(type);
			type2veh.put(type, prototype);
		}
		return type2veh;
	}

	// -------------------- SUMMARY TABLES --------------------

	private String null2notAvail(Object c) {
		if (c == null) {
			return "N/A";
		} else {
			return c.toString();
		}
	}

	public String createVehicleTypeTable(SamgodsConstants.TransportMode... modes) {
		final Set<SamgodsConstants.TransportMode> modeSet = Arrays.stream(modes).collect(Collectors.toSet());
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Description", "Mode", "Cost[1/km]", "Cost[1/h]", "Capacity[ton]", "FerryCost[1/km]",
				"FerryCost[1/h]", "MaxSpeed[km/h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final SamgodsVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
			if (modeSet.contains(attrs.mode)) {
				table.addRule();
				table.addRow(attrs.id, attrs.description, attrs.mode, attrs.cost_1_km, attrs.cost_1_h,
						attrs.capacity_ton, this.null2notAvail(attrs.onFerryCost_1_km),
						this.null2notAvail(attrs.onFerryCost_1_h), this.null2notAvail(attrs.speed_km_h));
			}
		}
		table.addRule();
		return table.render();
	}

	public String createVehicleTransferCostTable(SamgodsConstants.TransportMode... modes) {
		final Set<SamgodsConstants.TransportMode> modeSet = Arrays.stream(modes).collect(Collectors.toSet());
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Commodity", "LoadCost[1/ton]", "LoadTime[h]", "TransferCost[1/ton]",
				"TransferTime[h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final SamgodsVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
			if (modeSet.contains(attrs.mode)) {
				for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
					if (attrs.containsData(commodity)) {
						table.addRule();
						table.addRow(attrs.id, commodity, this.null2notAvail(attrs.loadCost_1_ton.get(commodity)),
								this.null2notAvail(attrs.loadTime_h.get(commodity)),
								this.null2notAvail(attrs.transferCost_1_ton.get(commodity)),
								this.null2notAvail(attrs.transferTime_h.get(commodity)));
					}
				}
			}
		}
		table.addRule();
		return table.render();
	}

	// -------------------- REPRESENTATIVE VEHICLE TYPES --------------------

	private class VehicleClassification {
		final SamgodsConstants.Commodity commodity;
		final SamgodsConstants.TransportMode mode;
		final Boolean isContainer;
		final Boolean containsFerry;

		VehicleClassification(SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode,
				Boolean isContainer, Boolean containsFerry) {
			this.commodity = commodity;
			this.mode = mode;
			this.isContainer = isContainer;
			this.containsFerry = containsFerry;
		}

		@Override
		public boolean equals(Object otherObj) {
			if (this == otherObj) {
				return true;
			} else if (!(otherObj instanceof VehicleClassification)) {
				return false;
			} else {
				final VehicleClassification other = (VehicleClassification) otherObj;
				return Arrays.asList(this.commodity, this.mode, this.isContainer, this.containsFerry)
						.equals(Arrays.asList(other.commodity, other.mode, other.isContainer, other.containsFerry));
			}
		}

		@Override
		public int hashCode() {
			return Arrays.asList(this.commodity, this.mode, this.isContainer, this.containsFerry).hashCode();
		}
	}

	private final Map<VehicleClassification, List<VehicleType>> classification2type = new LinkedHashMap<>();

	private final Map<VehicleClassification, VehicleType> classification2representativeType = new LinkedHashMap<>();

	public List<VehicleType> getCompatibleVehicleTypes(SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {
		final VehicleClassification classification = new VehicleClassification(commodity, mode, isContainer,
				containsFerry);
		List<VehicleType> result = this.classification2type.get(classification);

		if (result == null) {
			result = new ArrayList<>();
			for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
				SamgodsVehicleAttributes attrs = ConsolidationUtils.getFreightAttributes(type);
				if ((commodity == null || attrs.commodityCompatible(commodity))
						&& (mode == null || mode.equals(attrs.mode))
						&& (isContainer == null || isContainer.equals(attrs.isContainer))
						&& (containsFerry == null || !containsFerry || attrs.ferryCompatible())) {
					result.add(type);
				}
			}
			this.classification2type.put(classification, result);
		}

		return result;
	}

	public List<VehicleType> getCompatibleVehicleTypes(TransportEpisode episode) {
		return this.getCompatibleVehicleTypes(episode.getCommodity(), episode.getMode(), episode.isContainer(),
				episode.containsFerry());
	}

	public VehicleType getRepresentativeVehicleType(SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {

		final VehicleClassification classification = new VehicleClassification(commodity, mode, isContainer,
				containsFerry);
		VehicleType result = this.classification2representativeType.get(classification);

		if (result == null) {
			final List<VehicleType> matchingTypes = this.getCompatibleVehicleTypes(commodity, mode, isContainer,
					containsFerry);
			if (matchingTypes != null && matchingTypes.size() > 0) {
				final double meanCapacity_ton = matchingTypes.stream()
						.mapToDouble(t -> ConsolidationUtils.getCapacity_ton(t)).average().getAsDouble();
				double resultDeviation_ton = Double.POSITIVE_INFINITY;
				for (VehicleType candidate : matchingTypes) {
					final double candidateDeviation_ton = Math
							.abs(meanCapacity_ton - ConsolidationUtils.getCapacity_ton(candidate));
					if (candidateDeviation_ton < resultDeviation_ton) {
						result = candidate;
						resultDeviation_ton = candidateDeviation_ton;
					}
				}
			}
			this.classification2representativeType.put(classification, result);
		}

		return result;
	}

	public VehicleType getRepresentativeVehicleType(TransportEpisode episode) {
		return this.getRepresentativeVehicleType(episode.getCommodity(), episode.getMode(), episode.isContainer(),
				episode.containsFerry());
	}
}
