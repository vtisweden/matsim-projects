/**
 * se.vti.samgods.transportation
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
package se.vti.samgods.transportation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportChain;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationCostToTransportPriceModel {

	/**
	 * For evaluating an entire transport chain, including
	 * loading/unloading/transshipment.
	 * 
	 * This provides the transport-specific attributes of the alternatives in the
	 * logistics choice model.
	 * 
	 * TODO Only temporarily an inner interface.
	 */
	public static interface TransportChainPrices {

		Commodity getCommodity();

		double getPrice_1_ton(TransportChain chain);

		double getDuration_min(TransportChain chain);

	}

	/**
	 * For evaluating a consolidateable (i.e., unimodal) segment of an entire
	 * transport chain, including initial loading/unloading and possible
	 * intermediate transfer.
	 * 
	 * A TransportChainPrice is the sum of its ConsolidateableTransportChain
	 * segments. TODO This suggests to decompose already the basic TransportChain
	 * structure accordingly.
	 * 
	 * TODO Only temporarily an inner interface.
	 */
	public static interface ConsolidateableTransportChainPrices {

		Commodity getCommodity();

		TransportMode getMode();

		double getPrice_1_ton(TransportChain chain);

		double getDuration_min(TransportChain chain);

	}

	/**
	 * For network routing between two loading/unloading/transfer points, i.e. only
	 * for routing direct shipments. Hence only link-specific costs and travel
	 * times. Commodity- and mode-specific.
	 * 
	 * This is a link-additive aggregation of ConsolidateableTransportChainPrices.
	 * 
	 * TODO Only temporarily an inner interface.
	 */
	public static interface RoutingPrices {

		Commodity getCommodity();

		TransportMode getMode();

		double getPrice_1_ton(Id<Link> link);

		double getDuration_min(Id<Link> link);

	}

}
