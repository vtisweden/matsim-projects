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

		public final String description;

		public final SamgodsConstants.TransportMode mode;

		public final double cost_1_km;

		public final double cost_1_h;

		public final double capacity_ton;

		public final double onFerryCost_1_km;

		public final double onFerryCost_1_h;

		public final double maxSpeed_km_h;

		public final boolean container;

		public final Map<SamgodsConstants.Commodity, Double> loadCost_1_ton;

		public final Map<SamgodsConstants.Commodity, Double> loadTime_h;

		public final Map<SamgodsConstants.Commodity, Double> transferCost_1_ton;

		public final Map<SamgodsConstants.Commodity, Double> transferTime_h;

		private /* use builder */ TypeAttributes(Id<VehicleType> id, String description,
				SamgodsConstants.TransportMode mode, double cost_1_km, double cost_1_h, double capacity_ton,
				double onFerryCost_1_km, double onFerryCost_1_h, double maxSpeed_km_h, boolean container,
				Map<SamgodsConstants.Commodity, Double> loadCost_1_ton,
				Map<SamgodsConstants.Commodity, Double> loadTime_h,
				Map<SamgodsConstants.Commodity, Double> transferCost_1_ton,
				Map<SamgodsConstants.Commodity, Double> transferTime_h) {
			this.id = id;
			this.description = description;
			this.mode = mode;
			this.cost_1_km = cost_1_km;
			this.cost_1_h = cost_1_h;
			this.capacity_ton = capacity_ton;
			this.onFerryCost_1_km = onFerryCost_1_km;
			this.onFerryCost_1_h = onFerryCost_1_h;
			this.maxSpeed_km_h = maxSpeed_km_h;
			this.container = container;
			this.loadCost_1_ton = loadCost_1_ton;
			this.loadTime_h = loadTime_h;
			this.transferCost_1_ton = transferCost_1_ton;
			this.transferTime_h = transferTime_h;
		}
	}

	// -------------------- INNER CLASS --------------------

	public class TypeBuilder {

		private Id<VehicleType> id = null;

		private String description = null;

		private SamgodsConstants.TransportMode mode = null;

		private Double cost_1_km = null;

		private Double cost_1_h = null;

		private Double capacity_ton = null;

		private Double onFerryCost_1_km = null;

		private Double onFerryCost_1_h = null;

		private Double maxSpeed_km_h = null;

		private Boolean container = null;

		private Map<SamgodsConstants.Commodity, Double> loadCost_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> loadTime_h = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferCost_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferTime_h = new LinkedHashMap<>(16);

		public TypeBuilder() {
		}

		public VehicleType buildAndAddToFleet() {

			final VehicleType type = VehicleUtils.createVehicleType(this.id);
			type.setMaximumVelocity(Units.M_S_PER_KM_H * this.maxSpeed_km_h);

			TypeAttributes attributes = new TypeAttributes(this.id, this.description, this.mode, this.cost_1_km,
					this.cost_1_h, this.capacity_ton, this.onFerryCost_1_km, this.onFerryCost_1_h, this.maxSpeed_km_h,
					this.container, this.loadCost_1_ton, this.loadTime_h, this.transferCost_1_ton, this.transferTime_h);
			type.getAttributes().putAttribute(TypeAttributes.ATTRIBUTE_NAME, attributes);

			vehicles.addVehicleType(type);
			return type;

		}

		public TypeBuilder setName(String name) {
			this.id = Id.create(name, VehicleType.class);
			return this;
		}

		public TypeBuilder setDescription(String description) {
			this.description = description;
			return this;
		}

		public TypeBuilder setTransportMode(SamgodsConstants.TransportMode mode) {
			this.mode = mode;
			return this;
		}

		public TypeBuilder setCost_1_km(double fixedCost_1_km) {
			this.cost_1_km = fixedCost_1_km;
			return this;
		}

		public TypeBuilder setCost_1_h(double fixedCost_1_h) {
			this.cost_1_h = fixedCost_1_h;
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

		public TypeBuilder setContainer(boolean container) {
			this.container = container;
			return this;
		}

		public TypeBuilder setLoadCost_1_ton(SamgodsConstants.Commodity commodity, Double loadCost_1_Ton) {
			this.loadCost_1_ton.put(commodity, loadCost_1_Ton);
			return this;
		}

		public TypeBuilder setLoadTime_h(SamgodsConstants.Commodity commodity, Double loadTime_h) {
			this.loadTime_h.put(commodity, loadTime_h);
			return this;
		}

		public TypeBuilder setTransferCost_1_ton(SamgodsConstants.Commodity commodity, Double transferCost_1_ton) {
			this.transferCost_1_ton.put(commodity, transferCost_1_ton);
			return this;
		}

		public TypeBuilder setTransferTime_h(SamgodsConstants.Commodity commodity, Double transferTime_h) {
			this.transferTime_h.put(commodity, transferTime_h);
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
