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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportPrices;

/**
 * 
 * @author GunnarF
 *
 */
public class ProportionalShipmentPrices implements TransportPrices.ShipmentPrices {

	// -------------------- CONSTANTS --------------------

	private final Network network;

	private final Commodity commodity;

	private final TransportMode mode;

	private final double linkPriceEps; // to avoid zero edge costs in router

	// -------------------- MEMBERS --------------------

	private Double movePrice_1_tonM = null;

	private Double loadingPrice_1_ton = null;

	private Double unloadingPrice_1_ton = null;

	private Double loadingDuration_min = null;

	private Double unloadingDuration_min = null;

	// -------------------- CONSTRUCTION --------------------

	public ProportionalShipmentPrices(Network network, Commodity commodity, TransportMode mode) {
		this(network, commodity, mode, 1e-8);
	}

	public ProportionalShipmentPrices(Network network, Commodity commodity, TransportMode mode, double linkPriceEps) {
		this.network = network;
		this.commodity = commodity;
		this.mode = mode;
		this.linkPriceEps = linkPriceEps;
	}

	// -------------------- SETTERS --------------------

	public void setMovePrice_1_kmH(double movePrice_1_tonKm) {
		this.movePrice_1_tonM = 0.001 * movePrice_1_tonKm;
	}

	public void setLoadingPrice_1_ton(double price_1_ton) {
		this.loadingPrice_1_ton = price_1_ton;
	}

	public void setUnloadingPrice_1_ton(double price_1_ton) {
		this.unloadingPrice_1_ton = price_1_ton;
	}

	public void setLoadingDuration_min(double duration_min) {
		this.loadingDuration_min = duration_min;
	}

	public void setUnloadingDuration_min(double duration_min) {
		this.unloadingDuration_min = duration_min;
	}

	// -------------------- INTERNALS --------------------

	// -------------------- IMPLEMENTATION OF ShipmentPrices --------------------

	@Override
	public Commodity getCommodity() {
		return this.commodity;
	}

	@Override
	public TransportMode getMode() {
		return this.mode;
	}

	@Override
	public double getMovePrice_1_ton(Id<Link> linkId) {
		return Math.max(this.linkPriceEps, this.movePrice_1_tonM * this.network.getLinks().get(linkId).getLength());
	}

	@Override
	public double getMoveDuration_min(Id<Link> linkId) {
		final Link link = this.network.getLinks().get(linkId);
		return Units.MIN_PER_S * link.getLength() / link.getFreespeed();
	}

	@Override
	public double getLoadingPrice_1_ton(Id<Node> nodeId) {
		return this.loadingPrice_1_ton;
	}

	@Override
	public double getLoadingDuration_min(Id<Node> nodeId) {
		return this.loadingDuration_min;
	}

	@Override
	public double getUnloadingPrice_1_ton(Id<Node> nodeId) {
		return this.unloadingPrice_1_ton;
	}

	@Override
	public double getUnloadingDuration_min(Id<Node> nodeId) {
		return this.unloadingDuration_min;
	}

	@Override
	public ProportionalShipmentPrices deepCopy() {
		final ProportionalShipmentPrices child = new ProportionalShipmentPrices(this.network, this.commodity, this.mode,
				this.linkPriceEps);
		child.setMovePrice_1_kmH(1000.0 * this.movePrice_1_tonM);
		child.setLoadingPrice_1_ton(this.loadingPrice_1_ton);
		child.setUnloadingPrice_1_ton(this.unloadingPrice_1_ton);
		child.setLoadingDuration_min(this.loadingDuration_min);
		child.setUnloadingDuration_min(this.unloadingDuration_min);
		return child;
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer(
				"Transport prices for commodity " + this.commodity + ", mode " + this.mode + "\n");
		result.append("  move price [per ton] = " + 1000.0 * this.movePrice_1_tonM + "\n");
		result.append("  loading price [per ton] = " + this.loadingPrice_1_ton + "\n");
		result.append("  unloading price [per ton] = " + this.unloadingPrice_1_ton + "\n");
		result.append("  loading duration [min] = " + this.loadingDuration_min + "\n");
		result.append("  unloading duration [min] = " + this.unloadingDuration_min + "\n");
		return result.toString();
	}

	public Double getMovePrice_1_tonM() {
		return movePrice_1_tonM;
	}

	public Double getLoadingPrice_1_ton() {
		return loadingPrice_1_ton;
	}

	public Double getUnloadingPrice_1_ton() {
		return unloadingPrice_1_ton;
	}

	public Double getLoadingDuration_min() {
		return loadingDuration_min;
	}

	public Double getUnloadingDuration_min() {
		return unloadingDuration_min;
	}
}
