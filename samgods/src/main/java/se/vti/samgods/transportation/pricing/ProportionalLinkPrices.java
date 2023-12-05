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

import org.matsim.api.core.v01.network.Link;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportPrices;
import se.vti.samgods.TransportPrices.LinkPrices;

/**
 * 
 * @author GunnarF
 *
 */
public class ProportionalLinkPrices implements TransportPrices.LinkPrices {

	private final Commodity commodity;
	private final TransportMode mode;

	private final double price_1_tonM;
	private final double linkPriceEps; // to avoid zero edge costs in router

	public ProportionalLinkPrices(Commodity commodity, TransportMode mode, double price_1_tonKm, double linkPriceEps) {
		this.commodity = commodity;
		this.mode = mode;
		this.price_1_tonM = 0.001 * price_1_tonKm;
		this.linkPriceEps = linkPriceEps;
	}

	public ProportionalLinkPrices(Commodity commodity, TransportMode mode, double price_1_tonKm) {
		this(commodity, mode, price_1_tonKm, 1e-8);
	}

	@Override
	public double getPrice_1_ton(Link link) {
		return this.linkPriceEps + this.price_1_tonM * link.getLength();
	}

	@Override
	public LinkPrices deepCopy() {
		return new ProportionalLinkPrices(this.commodity, this.mode, 1000.0 * this.price_1_tonM, this.linkPriceEps);
	}

	@Override
	public Commodity getCommodity() {
		return this.commodity;
	}

	@Override
	public TransportMode getMode() {
		return this.mode;
	}

}
