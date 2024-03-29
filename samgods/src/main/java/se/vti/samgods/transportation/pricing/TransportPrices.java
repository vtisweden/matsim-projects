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
package se.vti.samgods.transportation.pricing;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.pricing.TransportPrices.ShipmentPrices;
import se.vti.samgods.transportation.pricing.TransportPrices.TransshipmentPrices;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportPrices<S extends ShipmentPrices, T extends TransshipmentPrices> {

	// -------------------- CONSTANTS --------------------

	public interface ShipmentPrices {

		Commodity getCommodity();

		TransportMode getMode();

		double getMovePrice_1_ton(Id<Link> link);

		double getMoveDuration_min(Id<Link> link);

		double getLoadingPrice_1_ton(Id<Node> node);

		double getUnloadingPrice_1_ton(Id<Node> node);

		double getLoadingDuration_min(Id<Node> node);

		double getUnloadingDuration_min(Id<Node> node);

		ShipmentPrices deepCopy();

	}

	public interface TransshipmentPrices {

		Commodity getCommodity();

		double getTransshipmentPrice_1_ton(Id<Node> node, TransportMode fromMode, TransportMode toMode);

		double getTransshipmentDuration_min(Id<Node> node, TransportMode fromMode, TransportMode toMode);

		TransshipmentPrices deepCopy();

	}

	// -------------------- MEMBERS --------------------

	private final Map<Commodity, Map<TransportMode, S>> commodity2mode2shipmentPrices;

	private final Map<Commodity, T> commodity2transshipmentPrices;

	// -------------------- CONSTRUCTION --------------------

	public TransportPrices() {
		this.commodity2mode2shipmentPrices = new LinkedHashMap<>(Commodity.values().length);
		this.commodity2transshipmentPrices = new LinkedHashMap<>(Commodity.values().length);
	}

//	public TransportPrices(Map<Commodity, Map<TransportMode, ShipmentPrices>> commodity2mode2shipmentPrices,
//			Map<Commodity, TransshipmentPrices> commodity2transshipmentPrices) {
//		this.commodity2mode2shipmentPrices = commodity2mode2shipmentPrices;
//		this.commodity2transshipmentPrices = commodity2transshipmentPrices;
//	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void addShipmentPrices(S prices) {
		this.commodity2mode2shipmentPrices
				.computeIfAbsent(prices.getCommodity(), c -> new LinkedHashMap<>(TransportMode.values().length))
				.put(prices.getMode(), prices);
	}

	public S getShipmentPrices(Commodity commodity, TransportMode mode) {
		Map<TransportMode, S> mode2linkMap = this.commodity2mode2shipmentPrices.get(commodity);
		if (mode2linkMap != null) {
			return mode2linkMap.get(mode);
		} else {
			return null;
		}
	}

	public void addTransshipmentPrices(T prices) {
		this.commodity2transshipmentPrices.put(prices.getCommodity(), prices);
	}

	public T getTransshipmentPrices(Commodity commodity) {
		return this.commodity2transshipmentPrices.get(commodity);
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		for (Map<TransportMode, S> mode2shipmentPrices : this.commodity2mode2shipmentPrices.values()) {
			for (S shipmentPrices : mode2shipmentPrices.values()) {
				result.append(shipmentPrices);
			}
		}
		for (TransshipmentPrices transshipmentPrices : this.commodity2transshipmentPrices.values()) {
			result.append(transshipmentPrices);
		}
		return result.toString();
	}
}
