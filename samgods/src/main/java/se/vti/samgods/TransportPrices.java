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

	public interface LinkPrices {

		public double getPrice_1_ton(Link link);

		public default double getDuration_h(Link link) {
			return Units.H_PER_S * Math.max(1.0, (link.getLength() / link.getFreespeed()));
		}

	}

	public interface NodePrices {

		public double getPrice_1_ton(Node nodeId, TransportMode fromNode, TransportMode toMode);

		public default double getDuration_h(Node nodeId, TransportMode fromNode, TransportMode toMode) {
			return 0.0;
		}

	}

	// -------------------- MEMBERS --------------------

	private final Map<Commodity, Map<TransportMode, LinkPrices>> commodity2mode2linkPrices = new LinkedHashMap<>(
			Commodity.values().length);

	private final Map<Commodity, NodePrices> commodity2nodePrices = new LinkedHashMap<>(Commodity.values().length);

	// -------------------- CONSTRUCTION --------------------

	public TransportPrices() {
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setLinkPrices(Commodity commodity, TransportMode mode, LinkPrices prices) {
		this.commodity2mode2linkPrices
				.computeIfAbsent(commodity, c -> new LinkedHashMap<>(TransportMode.values().length)).put(mode, prices);
	}

	public LinkPrices getLinkPrices(Commodity commodity, TransportMode mode) {
		Map<TransportMode, LinkPrices> mode2linkMap = this.commodity2mode2linkPrices.get(commodity);
		if (mode2linkMap != null) {
			return mode2linkMap.get(mode);
		} else {
			return null;
		}
	}

	public void setNodePrices(Commodity commodity, NodePrices prices) {
		this.commodity2nodePrices.put(commodity, prices);
	}

	public NodePrices getNodePrices(Commodity commodity) {
		return this.commodity2nodePrices.get(commodity);
	}
}
