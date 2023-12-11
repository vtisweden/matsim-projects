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
package se.vti.samgods;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportPrices {

	// -------------------- CONSTANTS --------------------

	public interface ShipmentPrices {

		Commodity getCommodity();

		TransportMode getMode();

		double getMovePrice_1_ton(Link link);

		default double getMoveDuration_h(Link link) {
			return Units.H_PER_S * Math.max(1.0, (link.getLength() / link.getFreespeed()));
		}

		double getLoadingPrice_1_ton(Node node);

		double getUnloadingPrice_1_ton(Node node);

		double getLoadingDuration_min(Node node);

		double getUnloadingDuration_min(Node node);

		ShipmentPrices deepCopy();

	}

	public interface TransshipmentPrices {

		Commodity getCommodity();

		double getTransshipmentPrice_1_ton(Node node, TransportMode fromMode, TransportMode toMode);

		double getTransshipmentDuration_min(Node node, TransportMode fromMode, TransportMode toMode);

		TransshipmentPrices deepCopy();

	}

	// -------------------- MEMBERS --------------------

	private final Map<Commodity, Map<TransportMode, ShipmentPrices>> commodity2mode2shipmentPrices = new LinkedHashMap<>(
			Commodity.values().length);

	private final Map<Commodity, TransshipmentPrices> commodity2transshipmentPrices = new LinkedHashMap<>(
			Commodity.values().length);

	// -------------------- CONSTRUCTION --------------------

	public TransportPrices() {
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void addShipmentPrices(ShipmentPrices prices) {
		this.commodity2mode2shipmentPrices
				.computeIfAbsent(prices.getCommodity(), c -> new LinkedHashMap<>(TransportMode.values().length))
				.put(prices.getMode(), prices);
	}

	public ShipmentPrices getShipmentPrices(Commodity commodity, TransportMode mode) {
		Map<TransportMode, ShipmentPrices> mode2linkMap = this.commodity2mode2shipmentPrices.get(commodity);
		if (mode2linkMap != null) {
			return mode2linkMap.get(mode);
		} else {
			return null;
		}
	}

	public void addTransshipmentPrices(TransshipmentPrices prices) {
		this.commodity2transshipmentPrices.put(prices.getCommodity(), prices);
	}

	public TransshipmentPrices getTransshipmentPrices(Commodity commodity) {
		return this.commodity2transshipmentPrices.get(commodity);
	}
}
