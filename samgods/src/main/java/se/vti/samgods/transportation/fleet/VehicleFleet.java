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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import de.vandermeer.asciitable.AsciiTable;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.Signature;
import se.vti.samgods.logistics.TransportEpisode;

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

	public Vehicle createAndAddVehicle(VehicleType type) {
		assert (this.vehicles.getVehicleTypes().values().contains(type));
		final Vehicle vehicle = this.vehicles.getFactory().createVehicle(Id.create(this.vehCnt++, Vehicle.class), type);
		this.vehicles.addVehicle(vehicle);
		return vehicle;
	}

	public Vehicles getVehicles() {
		return this.vehicles;
	}

	// -------------------- COMPATIBLE VEHICLE TYPES --------------------

	private final Map<Signature.ConsolidationUnit, List<VehicleType>> signature2compatibleTypes = new LinkedHashMap<>();

	public List<VehicleType> getCompatibleVehicleTypes(SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {

		final Signature.ConsolidationUnit signature = new Signature.ConsolidationUnit(commodity, mode, isContainer,
				containsFerry);
		List<VehicleType> result = this.signature2compatibleTypes.get(signature);

		if (result == null) {
			result = new ArrayList<>();
			for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
				if (signature.isCompatible(type)) {
					result.add(type);
				}
			}
			this.signature2compatibleTypes.put(signature, result);
		}

		return result;
	}

	// -------------------- REPRESENTATIVE VEHICLE TYPES --------------------

	private final Map<Signature.ConsolidationUnit, VehicleType> signature2representativeType = new LinkedHashMap<>();

	public VehicleType getRepresentativeVehicleType(SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry)
			throws InsufficientDataException {

		final Signature.ConsolidationUnit signature = new Signature.ConsolidationUnit(commodity, mode, isContainer,
				containsFerry);
		VehicleType result = this.signature2representativeType.get(signature);

		if (result == null) {

			final List<VehicleType> compatibleTypes = this.getCompatibleVehicleTypes(commodity, mode, isContainer,
					containsFerry);

			if (compatibleTypes.size() > 0) {
				final double meanCapacity_ton = compatibleTypes.stream()
						.mapToDouble(t -> FreightVehicleAttributes.getCapacity_ton(t)).average().getAsDouble();
				double resultDeviation_ton = Double.POSITIVE_INFINITY;
				for (VehicleType candidate : compatibleTypes) {
					final double candidateDeviation_ton = Math
							.abs(FreightVehicleAttributes.getCapacity_ton(candidate) - meanCapacity_ton);
					if (candidateDeviation_ton < resultDeviation_ton) {
						result = candidate;
						resultDeviation_ton = candidateDeviation_ton;
					}
				}
				this.signature2representativeType.put(signature, result);
			} else {
				throw new InsufficientDataException(this.getClass(), "No representative vehicle type.", commodity, null,
						mode, isContainer, containsFerry);
			}
		}

		return result;
	}

	public FreightVehicleAttributes getRepresentativeVehicleAttributes(SamgodsConstants.Commodity commodity,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry)
			throws InsufficientDataException {
		return FreightVehicleAttributes
				.getFreightAttributes(this.getRepresentativeVehicleType(commodity, mode, isContainer, containsFerry));
	}

	public FreightVehicleAttributes getRepresentativeVehicleAttributes(TransportEpisode episode)
			throws InsufficientDataException {
		return this.getRepresentativeVehicleAttributes(episode.getCommodity(), episode.getMode(), episode.isContainer(),
				episode.containsFerry());
	}

	// -------------------- SUMMARY TABLES --------------------

	private String null2notAvail(Object c) {
		if (c == null) {
			return "N/A";
		} else {
			return c.toString();
		}
	}

	public String createVehicleTypeTable(SamgodsConstants.TransportMode mode) {
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Description", "Mode", "Cost[1/km]", "Cost[1/h]", "Capacity[ton]", "FerryCost[1/km]",
				"FerryCost[1/h]", "MaxSpeed[km/h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleAttributes attrs = FreightVehicleAttributes.getFreightAttributes(type);
			if (mode.equals(attrs.mode)) {
				table.addRule();
				table.addRow(attrs.id, type.getDescription(), attrs.mode, attrs.cost_1_km, attrs.cost_1_h,
						attrs.capacity_ton, this.null2notAvail(attrs.onFerryCost_1_km),
						this.null2notAvail(attrs.onFerryCost_1_h), this.null2notAvail(attrs.speed_km_h));
			}
		}
		table.addRule();
		return table.render();
	}

	public String createVehicleTransferCostTable(SamgodsConstants.TransportMode mode) {
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Vehicle", "Commodity", "LoadCost[1/ton]", "LoadTime[h]", "TransferCost[1/ton]",
				"TransferTime[h]");
		for (VehicleType type : this.vehicles.getVehicleTypes().values()) {
			final FreightVehicleAttributes attrs = FreightVehicleAttributes.getFreightAttributes(type);
			if (mode.equals(attrs.mode)) {
				for (SamgodsConstants.Commodity commodity : SamgodsConstants.Commodity.values()) {
					if (attrs.isCompatible(commodity)) {
						table.addRule();
						table.addRow(attrs.id, commodity, attrs.loadCost_1_ton.get(commodity),
								attrs.loadTime_h.get(commodity), attrs.transferCost_1_ton.get(commodity),
								attrs.transferTime_h.get(commodity));
					}
				}
			}
		}
		table.addRule();
		return table.render();
	}
}
