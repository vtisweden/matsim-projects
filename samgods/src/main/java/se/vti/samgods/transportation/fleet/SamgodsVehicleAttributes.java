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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsVehicleAttributes {

	// -------------------- CONSTANTS --------------------

	public static final String ATTRIBUTE_NAME = "freight";

	public final Id<VehicleType> id;

	public final SamgodsConstants.TransportMode samgodsMode;

	public final double cost_1_km;

	public final double cost_1_h;

	public final double capacity_ton;

	// may be null for ferry incompatible vehicles
	public final Double onFerryCost_1_km;

	// may be null for ferry incompatible vehicles
	public final Double onFerryCost_1_h;

	// may be null, in which case link speed limit is used
	public final Double speed_km_h;

	public final boolean isContainer;

	// contains entries only for compatible commodities
	public final ConcurrentHashMap<SamgodsConstants.Commodity, Double> loadCost_1_ton;
	public final ConcurrentHashMap<SamgodsConstants.Commodity, Double> loadTime_h;
	public final ConcurrentHashMap<SamgodsConstants.Commodity, Double> transferCost_1_ton;
	public final ConcurrentHashMap<SamgodsConstants.Commodity, Double> transferTime_h;

	// -------------------- CONSTRUCTION (private, use builder) --------------------

	private SamgodsVehicleAttributes(Id<VehicleType> id, SamgodsConstants.TransportMode samgodsMode, double cost_1_km,
			double cost_1_h, double capacity_ton, Double onFerryCost_1_km, Double onFerryCost_1_h, Double speed_km_h,
			boolean container, Map<SamgodsConstants.Commodity, Double> loadCost_1_ton,
			Map<SamgodsConstants.Commodity, Double> loadTime_h,
			Map<SamgodsConstants.Commodity, Double> transferCost_1_ton,
			Map<SamgodsConstants.Commodity, Double> transferTime_h) {
		this.id = id;
		this.samgodsMode = samgodsMode;
		this.cost_1_km = cost_1_km;
		this.cost_1_h = cost_1_h;
		this.capacity_ton = capacity_ton;
		this.onFerryCost_1_km = onFerryCost_1_km;
		this.onFerryCost_1_h = onFerryCost_1_h;
		this.speed_km_h = speed_km_h;
		this.isContainer = container;
		this.loadCost_1_ton = new ConcurrentHashMap<>(loadCost_1_ton);
		this.loadTime_h = new ConcurrentHashMap<>(loadTime_h);
		this.transferCost_1_ton = new ConcurrentHashMap<>(transferCost_1_ton);
		this.transferTime_h = new ConcurrentHashMap<>(transferTime_h);
	}

	// -------------------- IMPLEMENTATION --------------------

	public boolean isFerryCompatible() {
		return (this.onFerryCost_1_km != null) && (this.onFerryCost_1_h != null);
	}

	public boolean isCompatible(SamgodsConstants.Commodity commodity) {
		return this.loadCost_1_ton.containsKey(commodity) && this.loadTime_h.containsKey(commodity)
				&& this.transferCost_1_ton.containsKey(commodity) && this.transferTime_h.containsKey(commodity);
	}

	// -------------------- BUILDER --------------------

	public static class Builder {

		public final Id<VehicleType> id;

		private String description = null;

		private SamgodsConstants.TransportMode samgodsMode = null;

		private Double cost_1_km = null;

		private Double cost_1_h = null;

		private Double capacity_ton = null;

		private Double onFerryCost_1_km = null;

		private Double onFerryCost_1_h = null;

		private Double speed_km_h = null;

		private Boolean container = null;

		private Map<SamgodsConstants.Commodity, Double> loadCost_1_ton = new LinkedHashMap<>();

		private Map<SamgodsConstants.Commodity, Double> loadTime_h = new LinkedHashMap<>();

		private Map<SamgodsConstants.Commodity, Double> transferCost_1_ton = new LinkedHashMap<>();

		private Map<SamgodsConstants.Commodity, Double> transferTime_h = new LinkedHashMap<>();

		public Builder(String name) {
			this.id = Id.create(name, VehicleType.class);
		}

		private SamgodsVehicleAttributes buildVehicleAttributes() throws InsufficientDataException {
			try {
				return new SamgodsVehicleAttributes(this.id, this.samgodsMode, this.cost_1_km, this.cost_1_h,
						this.capacity_ton, this.onFerryCost_1_km, this.onFerryCost_1_h, this.speed_km_h, this.container,
						this.loadCost_1_ton, this.loadTime_h, this.transferCost_1_ton, this.transferTime_h);
			} catch (Exception e /* Arises when assigning null Double object to primitive double. */) {
				throw new InsufficientDataException(this.getClass(),
						"Insufficient parameter data to build vehicle type " + this.id + ".");
			}
		}

		public VehicleType buildVehicleType() throws InsufficientDataException {
			final VehicleType type = VehicleUtils.createVehicleType(this.id);
			type.setDescription(this.description);
			type.getAttributes().putAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME, this.buildVehicleAttributes());
			return type;
		}

		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder setMode(SamgodsConstants.TransportMode mode) {
			this.samgodsMode = mode;
			return this;
		}

		public Builder setCost_1_km(double fixedCost_1_km) {
			this.cost_1_km = fixedCost_1_km;
			return this;
		}

		public Builder setCost_1_h(double fixedCost_1_h) {
			this.cost_1_h = fixedCost_1_h;
			return this;
		}

		public Builder setCapacity_ton(double capacity_ton) {
			this.capacity_ton = capacity_ton;
			return this;
		}

		public Builder setOnFerryCost_1_km(Double onFerryCost_1_km) {
			this.onFerryCost_1_km = onFerryCost_1_km;
			return this;
		}

		public Builder setOnFerryCost_1_h(Double onFerryCost_1_h) {
			this.onFerryCost_1_h = onFerryCost_1_h;
			return this;
		}

		public Builder setSpeed_km_h(Double speed_km_h) {
			this.speed_km_h = speed_km_h;
			return this;
		}

		public Builder setContainer(boolean container) {
			this.container = container;
			return this;
		}

		public Builder setLoadCost_1_ton(SamgodsConstants.Commodity commodity, Double loadCost_1_Ton) {
			if (loadCost_1_Ton != null) {
				this.loadCost_1_ton.put(commodity, loadCost_1_Ton);
			}
			return this;
		}

		public Builder setLoadTime_h(SamgodsConstants.Commodity commodity, Double loadTime_h) {
			if (loadTime_h != null) {
				this.loadTime_h.put(commodity, loadTime_h);
			}
			return this;
		}

		public Builder setTransferCost_1_ton(SamgodsConstants.Commodity commodity, Double transferCost_1_ton) {
			if (transferCost_1_ton != null) {
				this.transferCost_1_ton.put(commodity, transferCost_1_ton);
			}
			return this;
		}

		public Builder setTransferTime_h(SamgodsConstants.Commodity commodity, Double transferTime_h) {
			if (transferTime_h != null) {
				this.transferTime_h.put(commodity, transferTime_h);
			}
			return this;
		}
	}
}
