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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsVehicleAttributes {

	public static final String ATTRIBUTE_NAME = "freight";

	public final Id<VehicleType> id;

	public final String description;

	public final SamgodsConstants.TransportMode mode;

	public final double cost_1_km;

	public final double cost_1_h;

	public final double capacity_ton;

	public final Double onFerryCost_1_km;

	public final Double onFerryCost_1_h;

	public final Double speed_km_h;

	public double speedOnLink_m_s(Link link) {
		if (this.speed_km_h != null) {
			return Math.min(Units.M_S_PER_KM_H * this.speed_km_h, link.getFreespeed());
		} else {
			assert (Double.isFinite(link.getFreespeed()));
			return link.getFreespeed();
		}
	}

	public double travelTimeOnLink_s(Link link) {
		return Math.max(1e-8, link.getLength()) / this.speedOnLink_m_s(link);
	}

	public final boolean container;

	public final Map<SamgodsConstants.Commodity, Double> loadCost_1_ton;

	public final Map<SamgodsConstants.Commodity, Double> loadTime_h;

	public final Map<SamgodsConstants.Commodity, Double> transferCost_1_ton;

	public final Map<SamgodsConstants.Commodity, Double> transferTime_h;

	private SamgodsVehicleAttributes(Id<VehicleType> id, String description, SamgodsConstants.TransportMode mode,
			double cost_1_km, double cost_1_h, double capacity_ton, Double onFerryCost_1_km, Double onFerryCost_1_h,
			Double speed_km_h, boolean container, Map<SamgodsConstants.Commodity, Double> loadCost_1_ton,
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
		this.speed_km_h = speed_km_h;
		this.container = container;
		this.loadCost_1_ton = loadCost_1_ton;
		this.loadTime_h = loadTime_h;
		this.transferCost_1_ton = transferCost_1_ton;
		this.transferTime_h = transferTime_h;
	}

	public boolean containsData(SamgodsConstants.Commodity commodity) {
		return this.loadCost_1_ton.get(commodity) != null || this.loadTime_h.get(commodity) != null
				|| this.transferCost_1_ton.get(commodity) != null || this.transferTime_h.get(commodity) != null;
	}

	public static class Builder {

		private Id<VehicleType> id = null;

		private String description = null;

		private SamgodsConstants.TransportMode mode = null;

		private Double cost_1_km = null;

		private Double cost_1_h = null;

		private Double capacity_ton = null;

		private Double onFerryCost_1_km = null;

		private Double onFerryCost_1_h = null;

		private Double speed_km_h = null;

		private Boolean container = null;

		private Map<SamgodsConstants.Commodity, Double> loadCost_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> loadTime_h = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferCost_1_ton = new LinkedHashMap<>(16);

		private Map<SamgodsConstants.Commodity, Double> transferTime_h = new LinkedHashMap<>(16);

		public Builder() {
		}

		public SamgodsVehicleAttributes buildFreightVehicleAttributes() {
			return new SamgodsVehicleAttributes(this.id, this.description, this.mode, this.cost_1_km, this.cost_1_h,
					this.capacity_ton, this.onFerryCost_1_km, this.onFerryCost_1_h, this.speed_km_h, this.container,
					this.loadCost_1_ton, this.loadTime_h, this.transferCost_1_ton, this.transferTime_h);
		}

		public VehicleType buildVehicleType() {
			final VehicleType type = VehicleUtils.createVehicleType(this.id);
//			type.setMaximumVelocity(Units.M_S_PER_KM_H * this.speed_km_h); // TODO speed may be zero
			type.getAttributes().putAttribute(SamgodsVehicleAttributes.ATTRIBUTE_NAME,
					this.buildFreightVehicleAttributes());
			return type;
		}

		public Builder setName(String name) {
			this.id = Id.create(name, VehicleType.class);
			return this;
		}

		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder setTransportMode(SamgodsConstants.TransportMode mode) {
			this.mode = mode;
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
			this.loadCost_1_ton.put(commodity, loadCost_1_Ton);
			return this;
		}

		public Builder setLoadTime_h(SamgodsConstants.Commodity commodity, Double loadTime_h) {
			this.loadTime_h.put(commodity, loadTime_h);
			return this;
		}

		public Builder setTransferCost_1_ton(SamgodsConstants.Commodity commodity, Double transferCost_1_ton) {
			this.transferCost_1_ton.put(commodity, transferCost_1_ton);
			return this;
		}

		public Builder setTransferTime_h(SamgodsConstants.Commodity commodity, Double transferTime_h) {
			this.transferTime_h.put(commodity, transferTime_h);
			return this;
		}
	}

}
