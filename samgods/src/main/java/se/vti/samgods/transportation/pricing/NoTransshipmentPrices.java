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

import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.TransportPrices.TransshipmentPrices;

/**
 * 
 * @author GunnarF
 *
 */
public class NoTransshipmentPrices implements TransshipmentPrices {

	private final Commodity commodity;

	public NoTransshipmentPrices(Commodity commodity) {
		this.commodity = commodity;
	}

	@Override
	public Commodity getCommodity() {
		return this.commodity;
	}

	@Override
	public double getTransshipmentPrice_1_ton(Node node, TransportMode fromMode, TransportMode toMode) {
		return 0;
	}

	@Override
	public TransshipmentPrices deepCopy() {
		return new NoTransshipmentPrices(this.commodity);
	}

	@Override
	public double getTransshipmentDuration_min(Node node, TransportMode fromMode, TransportMode toMode) {
		// TODO Auto-generated method stub
		return 0;
	}

}
