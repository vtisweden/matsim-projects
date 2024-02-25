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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.consolidation.road.PrototypeVehicle;

/**
 * 
 * @author GunnarF
 *
 */
public class FreightVehicleFleet {

	// -------------------- INNER CLASS --------------------

	public class TypeAttributes {

		public static final String ATTRIBUTE_NAME = "freight";

		public final Id<VehicleType> id;

		public final SamgodsConstants.TransportMode mode;

		public final double fixedCost_1_km;

		public final double fixedCost_1_h;

		public final double capacity_ton;

		public final double onFerryCost_1_km;

		public final double onFerryCost_1_h;

		public final double maxSpeed_km_h;

		public final Map<SamgodsConstants.Commodity, Double> loadCostNoContainer_1_ton;

		public final Map<SamgodsConstants.Commodity, Double> loadTimeNoContainer_h;

		public final Map<SamgodsConstants.Commodity, Double> transferCostNoContainer_1_ton;

		public final Map<SamgodsConstants.Commodity, Double> transferTimeNoContainer_h;

		public final Map<SamgodsConstants.Commodity, Double> loadCostContainer_1_ton;

		public final Map<SamgodsConstants.Commodity, Double> loadTimeContainer_h;

		public final Map<SamgodsConstants.Commodity, Double> transferCostContainer_1_ton;

		public final Map<SamgodsConstants.Commodity, Double> transferTimeContainer_h;

		private /* use builder */ TypeAttributes(Id<VehicleType> id, SamgodsConstants.TransportMode mode,
				double fixedCost_1_km, double fixedCost_1_h, double capacity_ton, double onFerryCost_1_km,
				double onFerryCost_1_h, double maxSpeed_km_h,
				Map<SamgodsConstants.Commodity, Double> loadCostNoContainer_1_ton,
				Map<SamgodsConstants.Commodity, Double> loadTimeNoContainer_h,
				Map<SamgodsConstants.Commodity, Double> transferCostNoContainer_1_ton,
				Map<SamgodsConstants.Commodity, Double> transferTimeNoContainer_h,
				Map<SamgodsConstants.Commodity, Double> loadCostContainer_1_ton,
				Map<SamgodsConstants.Commodity, Double> loadTimeContainer_h,
				Map<SamgodsConstants.Commodity, Double> transferCostContainer_1_ton,
				Map<SamgodsConstants.Commodity, Double> transferTimeContainer_h) {
			this.id = id;
			this.mode = mode;
			this.fixedCost_1_km = fixedCost_1_km;
			this.fixedCost_1_h = fixedCost_1_h;
			this.capacity_ton = capacity_ton;
			this.onFerryCost_1_km = onFerryCost_1_km;
			this.onFerryCost_1_h = onFerryCost_1_h;
			this.maxSpeed_km_h = maxSpeed_km_h;
			this.loadCostNoContainer_1_ton = loadCostNoContainer_1_ton;
			this.loadTimeNoContainer_h = loadTimeNoContainer_h;
			this.transferCostNoContainer_1_ton = transferCostNoContainer_1_ton;
			this.transferTimeNoContainer_h = transferTimeNoContainer_h;
			this.loadCostContainer_1_ton = loadCostContainer_1_ton;
			this.loadTimeContainer_h = loadTimeContainer_h;
			this.transferCostContainer_1_ton = transferCostContainer_1_ton;
			this.transferTimeContainer_h = transferTimeContainer_h;
		}
	}

	// -------------------- INNER CLASS --------------------

	public class TypeBuilder {

		private Id<VehicleType> id = null;

		private SamgodsConstants.TransportMode mode = null;

		private Double fixedCost_1_km = null;

		private Double fixedCost_1_h = null;

		private Double capacity_ton = null;

		private Double onFerryCost_1_km = null;

		private Double onFerryCost_1_h = null;

		private Double maxSpeed_km_h = null;

		private Map<SamgodsConstants.Commodity, Double> loadCostNoContainer_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> loadTimeNoContainer_h = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferCostNoContainer_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferTimeNoContainer_h = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> loadCostContainer_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> loadTimeContainer_h = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferCostContainer_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferTimeContainer_h = new LinkedHashMap<>(16);

		public TypeBuilder() {
		}

		public VehicleType buildAndAddToFleet() {

			final VehicleType type = VehicleUtils.createVehicleType(this.id);
			type.setMaximumVelocity(Units.M_S_PER_KM_H * this.maxSpeed_km_h);

			TypeAttributes attributes = new TypeAttributes(this.id, this.mode, this.fixedCost_1_km, this.fixedCost_1_h,
					this.capacity_ton, this.onFerryCost_1_km, this.onFerryCost_1_h, this.maxSpeed_km_h,
					this.loadCostNoContainer_1_ton, this.loadTimeNoContainer_h, this.transferCostNoContainer_1_ton,
					this.transferTimeNoContainer_h, this.loadCostContainer_1_ton, this.loadTimeContainer_h,
					this.transferCostContainer_1_ton, this.transferTimeContainer_h);
			type.getAttributes().putAttribute(TypeAttributes.ATTRIBUTE_NAME, attributes);

			vehicles.addVehicleType(type);
			return type;

		}

		public TypeBuilder setName(String name) {
			this.id = Id.create(name, VehicleType.class);
			return this;
		}

		public TypeBuilder setTransportMode(SamgodsConstants.TransportMode mode) {
			this.mode = mode;
			return this;
		}

		public TypeBuilder setFixedCost_1_km(double fixedCost_1_km) {
			this.fixedCost_1_km = fixedCost_1_km;
			return this;
		}

		public TypeBuilder setFixedCost_1_h(double fixedCost_1_h) {
			this.fixedCost_1_h = fixedCost_1_h;
			return this;
		}

		public TypeBuilder setCapacity_ton(double capacity_ton) {
			this.capacity_ton = capacity_ton;
			return this;
		}

		public TypeBuilder setOnFerryCost_1_km(double onFerryCost_1_km) {
			this.onFerryCost_1_km = onFerryCost_1_km;
			return this;
		}

		public TypeBuilder setOnFerryCost_1_h(double onFerryCost_1_h) {
			this.onFerryCost_1_h = onFerryCost_1_h;
			return this;
		}

		public TypeBuilder setMaxSpeed_km_h(double maxSpeed_km_h) {
			this.maxSpeed_km_h = maxSpeed_km_h;
			return this;
		}

		public TypeBuilder setLoadCostNoContainer_1_Ton(SamgodsConstants.Commodity commodity,
				Double loadCostNoContainer_1_Ton) {
			this.loadCostNoContainer_1_ton.put(commodity, loadCostNoContainer_1_Ton);
			return this;
		}

		public TypeBuilder setLoadTimeNoContainer_h(SamgodsConstants.Commodity commodity,
				Double loadTimeNoContainer_h) {
			this.loadTimeNoContainer_h.put(commodity, loadTimeNoContainer_h);
			return this;
		}

		public TypeBuilder setTransferCostNoContainer_1_ton(SamgodsConstants.Commodity commodity,
				Double transferCostNoContainer_1_ton) {
			this.transferCostNoContainer_1_ton.put(commodity, transferCostNoContainer_1_ton);
			return this;
		}

		public TypeBuilder setTransferTimeNoContainer_h(SamgodsConstants.Commodity commodity,
				Double transferTimeNoContainer_h) {
			this.transferTimeNoContainer_h.put(commodity, transferTimeNoContainer_h);
			return this;
		}

		public TypeBuilder setLoadCostContainer_1_Ton(SamgodsConstants.Commodity commodity,
				Double loadCostContainer_1_Ton) {
			this.loadCostContainer_1_ton.put(commodity, loadCostContainer_1_Ton);
			return this;
		}

		public TypeBuilder setLoadTimeContainer_h(SamgodsConstants.Commodity commodity, Double loadTimeContainer_h) {
			this.loadTimeContainer_h.put(commodity, loadTimeContainer_h);
			return this;
		}

		public TypeBuilder setTransferCostContainer_1_ton(SamgodsConstants.Commodity commodity,
				Double transferCostContainer_1_ton) {
			this.transferCostContainer_1_ton.put(commodity, transferCostContainer_1_ton);
			return this;
		}

		public TypeBuilder setTransferTimeContainer_h(SamgodsConstants.Commodity commodity,
				Double transferTimeContainer_h) {
			this.transferTimeContainer_h.put(commodity, transferTimeContainer_h);
			return this;
		}

	}

	// -------------------- MEMBERS --------------------

	private final Vehicles vehicles;

	private long vehCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	public FreightVehicleFleet() {
		this.vehicles = VehicleUtils.createVehiclesContainer();
	}

	// -------------------- IMPLEMENTATION --------------------

	public TypeBuilder createTypeBuilder() {
		return new TypeBuilder();
	}

	public Map<Id<VehicleType>, VehicleType> getVehicleTypes() {
		return this.vehicles.getVehicleTypes();
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

}
